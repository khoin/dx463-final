// Boot Server
(
~fftLength = 2.pow(8);
s.waitForBoot({
	"sdefs.sc".loadRelative;
});
)


// After booting seve
(
// Audio
var bus					= Bus.audio(s, 2);
var freqAmplitude		= Array.fill(3, { |i|
	Synth(\freqAmp, [
		\in, bus,
		\freq, [810,1209,1615][i],
		\out, 99
])});
var pitchShifters		= Synth(\pshift, [
		\in, bus,
		\out, 0,
		\wet, 99
]);
var spectroAnalyzer		= Synth(\spectro, [
		\in, bus
	]);
var microphone			= Synth(\mic, [
		\out, bus,
		\delay, 0
	]);

// GUI
var winWidth			= 700;
var winHeight			= 500;
var win					= Window(
		name:			"K GUI",
		bounds:			Rect(1200, 200, winWidth, winHeight),
	);

win.layout = VLayout(
	KSpectro(spectroAnalyzer, ~fftLength, 80)
	, nil
	, nil
);

win.view.keyDownAction = { |v, char|
	if($ == char, {"Spacebar!".postln; });
};

// Properties
win.background			= Color.grey;

// Event Handlers
CmdPeriod.doOnce({
	"Closing".postln;
	if(win.isClosed.not, {
		win.close
	});
	s.freeAll;
});
win.onClose				= {
	CmdPeriod.run;
};


// And... Go!
win.alwaysOnTop			= true;
win.front;
)