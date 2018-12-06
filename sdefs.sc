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
SynthDef(\freqAmp, { |in, out, freq = 440, rq = 0.005|
	var signal = Lag.kr(
			Amplitude.kr(
				BPF.ar(
					In.ar(in),
					freq,
					rq
				)
			),
			lagTime: 0.1
		);

	Out.kr(out, signal);
	SendReply.kr(
		Impulse.kr(60),
		'/freqAmp',
		[freq, signal]
	);
}).add;

// -- Satie
// I've decided against this because of feedback issues.
// 1090
// 1212
// 1461
// 1343
// 1630
// 1831
// 2083
// 2171
SynthDef(\satie, { |in, out, c1 = 9, c2 = 9, c3 = 9, c4 = 9, c5 = 9, c6 = 9, c7 = 9, c8 = 9|
	var outsig = 0;
	// do re fa mi sol la ti do'; Satie - Chorale inappétissant
	// first variables represent the detection of pitch (control rate)
	// second variables represent the output.
	var i = In.ar(in, 2);
	var h;
	var d, r, f, m, s, l, t, dd;
	var od, or, of, om, os, ol, ot, odd;
	d = In.kr(c1);
	r = In.kr(c2);
	f = In.kr(c3);
	m = In.kr(c4);
	s = In.kr(c5);
	l = In.kr(c6);
	t = In.kr(c7);
	dd = In.kr(c8);

	h = 2.pow([
		[24, 0, -2, -5, -9],
		[24, 0, -5, -9, -16],
		[12, 0, -10, -15, -17]*2,
		[12, 0, -4, -10, -16]*2,
		[24, 0, -2, -7, -9],
		[24, 0, -5, -9, -11],
		[24, 0, -2, -7, -9],
		[24, 0, -5, -8, -10]
	]/12)/4;

	od = PitchShift.ar(i * d, pitchRatio: h[0] ! 2);
	or = PitchShift.ar(i * r, pitchRatio: h[1] ! 2)
		+ DelayL.ar(PitchShift.ar(i * r, pitchRatio: 2.pow(-1/2)/4 ! 2), 0.8, 0.8, mul: 3.dbamp);
	of = PitchShift.ar(i * f, pitchRatio: h[2] ! 2);
	om = PitchShift.ar(i * m, pitchRatio: h[3] ! 2)
		+ DelayL.ar(PitchShift.ar(i * m, pitchRatio: 2.pow(-9/2)/4 ! 2), 0.8, 0.8, mul: 2.dbamp);
	os = PitchShift.ar(i * s, pitchRatio: h[4] ! 2);
	ol = PitchShift.ar(i * l, pitchRatio: h[5] ! 2)
		+ DelayL.ar(PitchShift.ar(i * l, pitchRatio: 2.pow(-1)/4 ! 2), 0.8, 0.8, mul: -8.dbamp);
	ot = PitchShift.ar(i * t * -20.dbamp, pitchRatio: h[6] ! 2, mul: -5.dbamp);
	odd = PitchShift.ar(i * dd * -20.dbamp, pitchRatio: h[7] ! 2, mul: -15.dbamp)
	+ DelayL.ar(PitchShift.ar(i * dd, pitchRatio: 2.pow(-1)/4 ! 2), 0.8, 0.8, mul: -5.dbamp);

	outsig = SelectX.ar(Lag.kr(Mix.kr([d, r, f, m, s, l, t, dd]) > -40.dbamp),[
		Silent.ar,
		SelectX.ar(
			Lag.kr(ArrayMax.kr([d, r, f, m, s, l, t, dd])[1], 0.5),
			[od, or, of, om, os, ol, ot, odd]
		)
	]);

	outsig = HPF.ar(outsig, 120);

	Out.ar(out, outsig);

});

// -- Fork
SynthDef(\fork, { |in, out, ctrl1 = 99, ctrl2 = 99, ctrl3 = 99, ctrl4 = 99|

	var outsig = 0;
	var c1, c2, c3, c4, o1, o2, o3, o4, m;
	c1 = In.kr(ctrl1);
	c2 = In.kr(ctrl2);
	c3 = In.kr(ctrl3);
	c4 = In.kr(ctrl4);

	m = ArrayMax.kr([c1, c2, c3, c4])[1];

	// f: 813
	o1 = BLowShelf.ar(
			PitchShift.ar(
				In.ar(in, 2),
				pitchRatio: [5/4, 8/36, 3/4, 3/8, 3/16, 1/8] ! 2,
			mul: 5.dbamp * c1),
			300, db: 5
		)
	;

	// f: 1204
	o2 = Mix.ar(SinOsc.ar(1204 * [2/3, 1/4, 3/8, 3/2, 1/16],
		c2 * In.ar(in, 2) * SinOsc.ar(1, 0, 8, 10),
		-30.dbamp));

	// f: 1606
	o3 = BLowShelf.ar(PitchShift.ar(
			In.ar(in, 2),
			pitchRatio: [1/2, 5/12, 1/6, 1/9]*2 ! 2,
			mul: 5.dbamp * c3
	), 800, 1, 5);

	// f: 2023
	o4 = BLowShelf.ar(
			PitchShift.ar(
				In.ar(in, 2),
			pitchRatio: [8/36, 3/8, 3/16, 1/8] *
			TRand.kr(0.25, 5, m >=3) *
			EnvGen.kr(Env.perc(0.01, TRand.kr(1, 4, m >=3)), m >= 3) ! 2,
			mul: 5.dbamp * c4),
			300, db: -5, mul: 0.dbamp
		)
	;

	outsig = SelectX.ar(Lag.kr(Mix.kr([c1, c2, c3, c4]) > -45.dbamp),[
		Silent.ar,
		SelectX.ar(
			Lag.kr(m, 0.5),
			[o1, o2, o3, o4]
		)
	]);

	Out.ar(out, outsig * 0.3);
}).add;


// -- bells

SynthDef(\bells, { |in, out, pitch = 0, roll = 0|
	var outsig = 0;

	//[pitch, roll].poll;

	outsig = PitchShift.ar(
		In.ar(in, 2), pitchRatio: [0.5 + ((pi + pitch)/2pi), 0.5 + (pi/2 + roll)/(pi/2)] ! 2,
		mul: -10.dbamp
	);

	Out.ar(out, outsig);
}).add;

// -- Playbacker

SynthDef(\pb, { |out, buf|
	Out.ar(out, PlayBuf.ar(1, buf, doneAction: 2));
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