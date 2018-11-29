// THIS FILE WILL BE LOADED IN FINAL.sc

// Ultimate Test Sin
SynthDef(\sin, { |out = 0, freq = 440|
	Out.ar(out, 0.1!2 * SinOsc.ar(Lag.kr(freq)));
}).add;

// Selector
SynthDef(\select, { |in = #[1, 1, 1, 1], index = 0, out = 0|

	Out.ar(out, SelectX.ar(Lag.kr(index, 0.1), In.ar(in, 2)));
}).add;


// todo: multiple paths
// --- Spectrogram Analyzer
// When `t_trigger` is triggered,
// a trigger message will be sent (from server) to the client
// where the trigger ID will be the bin number of the FFT
// and the value will be the value of that FFT bin.
// The signal to be analyzed is from `in`.
// -## Note
// After: Fedrick Olofsson f0 https://www.fredrikolofsson.com/f0blog/node/345
// There has to be a better way than sending individual messages per bin like this?
// UnpackFFT might work but need to
// filter out mag from mag + phase interlacing before sending.
SynthDef(\spectro, { |in = 2, t_trigger = 0|
	var fftLength	= ~fftLength;
	var fftChain	= FFT(LocalBuf(fftLength * 2), In.ar(in, 1));

	fftLength.do({ |i|
		// Taking _amplitude_ value at bin `i` from the FFT
		var ugen 	= Unpack1FFT(fftChain, fftLength, i, 0);
		// When fftChain is updated, get value from `ugen` Unpack1FFT.
		var val		= Demand.kr(fftChain > -1 , 0, ugen);
		// When we receive a trigger from outside,
		// we return the value `val` at bin `i`.
		SendReply.kr(t_trigger, '/spectro', val, i);
	});
}).add;

// --- Microphone Input
// Note: mono to stereo
SynthDef(\mic, { |out, amp = 1, delay = 0.5|
	Out.ar(out,
		DelayN.ar(
			SoundIn.ar(0),
			delay,
			delay,
			amp
		) * [1, 1]
	);
}).add;

// -- Amplitude Tracker
// note: rq = bw / f
// ====> bw = f * rq = e.g. 440 * 0.005 = 2 Hz
// ====> e.g. 1220 * 0.005 = 6 Hz (Quite decent/precise bw)
SynthDef(\freqAmp, { |in, out, freq = 440, rq = 0.008|
	var signal = Lag.kr(
			Amplitude.kr(
				BPF.ar(
					In.ar(in),
					freq,
					rq
				)
			),
			lagTime: 0.5
		);

	Out.kr(out, signal);
	SendReply.kr(
		Impulse.kr(60),
		'/freqAmp',
		[freq, signal]
	);
}).add;

// -- Pitchshift
SynthDef(\pshift, { |in, out, ctrl1 = 99, ctrl2 = 99, ctrl3 = 99, ctrl4 = 99|

	var outsig = 0;
	var c1, c2, c3, c4, o1, o2, o3, o4;
	var m, steppy;
	c1 = In.kr(ctrl1);
	c2 = In.kr(ctrl2);
	c3 = In.kr(ctrl3);
	c4 = In.kr(ctrl4);

	o1 = BLowShelf.ar(
			PitchShift.ar(
				In.ar(in, 2),
				pitchRatio: [5/4, 8/36, 3/4, 3/8, 3/16, 1/8] ! 2,
			mul: 5.dbamp * c1),
			300, db: 5
		)
	;

	o2 = Mix.ar(SinOsc.ar(1204 * [2/3, 1/4, 3/8, 3/2, 1/16],
		c2 * In.ar(in, 2) * SinOsc.ar(1, 0, 8, 10),
		-40.dbamp));

	o3 = BLowShelf.ar(
			PitchShift.ar(
				In.ar(in, 2),
				pitchRatio: [1/2, 5/12, 1/6, 1/9, 1/18]*2 ! 2,
			mul: 5.dbamp * c3),
			300, db: 5
		)
	;

	o4 = Silent.ar;

	outsig = SelectX.ar(Lag.kr(Mix.kr([c1, c2, c3, c4]) > -45.dbamp),[
		Silent.ar,
		SelectX.ar(Lag.kr(c1 > c2), [
			SelectX.ar(Lag.kr(c2 > c3), [
				SelectX.ar(Lag.kr(c3 > c4), [
					o4,
					o3,
				]),
				o2,
			]),
			o1
		]);
	]);

	Out.ar(out, outsig * 0.3);
}).add;

// -- Playbacker

SynthDef(\pb, { |out, buf|
	Out.ar(0, PlayBuf.ar(1, buf, doneAction: 2));
}).add;









// REVERB

SynthDef(\reverb, {
	arg in = 99, gain = 0, mix = 0.35, preDelay = 0.001, bandwidth = 0.998, tailDensity = 0.7, decayRate = 0.9, damping = 0.0005, excursionDepth = 0.2, excursionRate = 2, shimmerPitch = 1, out = 0, processGain = 0;

	// funcs
	var sampleRate		= Server.default.sampleRate;
	var equalPower        = {
		arg mix = 0.5;
		[(1-mix).sqrt, mix.sqrt];
	};
	var sampSec           = {
		arg numSamp, sampRate;
		numSamp / sampRate;
	};

	var gFacT60           = {
		arg delay, gFac;
		gFac.sign * (-3 * delay / log10(gFac.abs));

	};
	// some constant values
	var dattorroSampRate = 29761;
	var maxExcursion    = 32; // samples

	// values for prep part
	var preTankVals = [
		[0.75, 0.75, 0.625, 0.625], // gFacs
		sampSec.value([142, 107, 379, 277], dattorroSampRate) // times
	].flop;

	// values for tank part
	// note that Dattorro flipped the sign of gFacs for the decaying APs,
	// I do that here so I don't worry about the signs later.
	var tankAP1GFac = -1 * tailDensity;
	var tankAP1Time = 672;
	var tankDel1    = sampSec.value(4453, dattorroSampRate);
	var tankAP2GFac = (decayRate + 0.15).min(0.5).max(0.25);
	var tankAP2Time = sampSec.value(1800, dattorroSampRate);
	var tankDel2    = sampSec.value(3720, dattorroSampRate);

	var tankAP3GFac = tankAP1GFac;
	var tankAP3Time = 908;
	var tankDel3    = sampSec.value(4217, dattorroSampRate);
	var tankAP4GFac = tankAP2GFac;
	var tankAP4Time = sampSec.value(2656, dattorroSampRate);
	var tankDel4    = sampSec.value(3163, dattorroSampRate);

	// Signals
	var dry     = In.ar(in, 2);
	var preTank = Silent.ar;
	var tank    = Silent.ar;
	var wetL    = Silent.ar;
	var wetR    = Silent.ar;
	var wet     = Silent.ar;
	var outs    = Silent.ar;

	var fback;

	var dryAmp, wetAmp;
	#dryAmp, wetAmp = equalPower.value(mix);

	// proper mappings for params
	damping = (damping + (1 + (8 * damping))).log / (10.log); // somewhat better than linear
	bandwidth = 3.pow(bandwidth) - (1 + bandwidth);

	// ROUTINGS
	// make it mono
	preTank = (dry[0] + dry[1]) / 2;
	// pregain
	preTank = preTank * processGain.dbamp;
	// predelay
	preTank = DelayC.ar(preTank, preDelay, preDelay);
	// lowpass
	preTank = LPF.ar(preTank, sampleRate / 2 * bandwidth);

	// 4 All-passes to diffuse inputs
	preTankVals.do({ arg pair; // 0: gFac, 1: time
		preTank = AllpassC.ar(preTank, pair[1], pair[1], gFacT60.value(pair[1], pair[0]));
	});

	fback = LocalIn.ar(1);

	// // Tank starts here
	// first branch
	tank  = AllpassC.ar(preTank + (decayRate * fback),
		maxdelaytime: sampSec.value(tankAP1Time + maxExcursion, dattorroSampRate),
		delaytime: sampSec.value(tankAP1Time, dattorroSampRate)
		+ (sampSec.value(maxExcursion, dattorroSampRate) * excursionDepth * SinOsc.ar(excursionRate)),
		decaytime: gFacT60.value(sampSec.value(tankAP1Time, dattorroSampRate), tankAP1GFac)
	);

	     wetL = -0.6 * DelayC.ar(tank, sampSec.value(1990, dattorroSampRate), sampSec.value(1990, dattorroSampRate)) + wetL;
	     wetR = 0.6 * tank + wetR;
	     wetR = 0.6 * DelayC.ar(tank, sampSec.value(3300, dattorroSampRate), sampSec.value(3300, dattorroSampRate)) + wetR;
	tank = DelayC.ar(tank, tankDel1, tankDel1);
	tank = LPF.ar(tank, sampleRate / 2 * (1 - damping)) * decayRate;
	     wetL = -0.6 * tank + wetL;
	tank = AllpassC.ar(tank, tankAP2Time, tankAP2Time, gFacT60.value(tankAP2Time, tankAP2GFac));
	     wetR = -0.6 * tank + wetR;
	tank = DelayC.ar(tank, tankDel2, tankDel2);
	     wetR = 0.6 * tank + wetR;

	// // second branch
	tank  = AllpassC.ar((tank * decayRate) + preTank,
		maxdelaytime: sampSec.value(tankAP3Time + maxExcursion, dattorroSampRate),
		delaytime: sampSec.value(tankAP3Time, dattorroSampRate)
		+ (sampSec.value(maxExcursion, dattorroSampRate) * excursionDepth * 0.8 * SinOsc.ar(excursionRate * 0.8)),
		decaytime: gFacT60.value(sampSec.value(tankAP3Time, dattorroSampRate), tankAP3GFac)
	);

	     wetL = 0.6 * tank + wetL;
	     wetL = 0.6 * DelayC.ar(tank, sampSec.value(2700, dattorroSampRate), sampSec.value(2700, dattorroSampRate)) + wetL;
	     wetR = -0.6 * DelayC.ar(tank, sampSec.value(2100, dattorroSampRate), sampSec.value(2100, dattorroSampRate)) + wetR;
	tank = DelayC.ar(tank, tankDel3, tankDel3);
	tank = LPF.ar(tank, sampleRate / 2 * (1 - damping)) * decayRate;
	tank = AllpassC.ar(tank, tankAP4Time, tankAP4Time, gFacT60.value(tankAP4Time, tankAP4GFac));
	     wetL = -0.6 * tank + wetL;
	     wetR = -0.6 * DelayC.ar(tank, sampSec.value(200, dattorroSampRate), sampSec.value(200, dattorroSampRate)) + wetR;

	tank = DelayC.ar(tank, tankDel4, tankDel4);
	     wetL = 0.6 * tank + wetL;

	tank = tank * decayRate;
	// // Sloppy Shimmering
	tank = PitchShift.ar(tank, pitchRatio: shimmerPitch);
	// // Tank ends here
	LocalOut.ar(tank);

    wet = [wetL, wetR];

	outs = (dry * dryAmp) + (wet * wetAmp);
	outs = outs * gain.dbamp;

	Out.ar(out, outs);
}).add;