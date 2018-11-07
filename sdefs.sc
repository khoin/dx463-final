// THIS FILE WILL BE LOADED IN FINAL.sc

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
SynthDef(\spectro, { |in = 0, t_trigger = 0|
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

// --- Microphone Inputc
// Note: mono to stereo
SynthDef(\mic, { |out, delay = 0.5|
	Out.ar(out,
		DelayN.ar(
			SoundIn.ar(0),
			delay,
			delay
		) * [1, 1]
	);
}).add;

// -- Amplitude Tracker
// note: rq = bw / f
// ====> bw = f * rq = e.g. 440 * 0.005 = 2 Hz
// ====> e.g. 1220 * 0.005 = 6 Hz (Quite decent/precise bw)
SynthDef(\freqAmp, { |in, out, freq = 440, rq = 0.007|
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
SynthDef(\pshift, { |in, out, wet|
	Out.ar(0,
		PitchShift.ar(In.ar(in, 2), pitchRatio: 0.25*[3/2, 4/3, 5/4, 5], mul: (15.dbamp) * In.kr(wet));
	)
}).add;