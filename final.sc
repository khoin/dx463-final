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
var buf = Buffer.read(s, "sounds/subject.wav".resolveRelative);
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
var phone				= Canvas3D().distance_(2);
var phoneListener		;
var phoneAccel			= [0, 0, 0];

win.layout = VLayout(
	KSpectro(spectroAnalyzer, ~fftLength, 80),
	HLayout(
		[phone, s: 1],
		[nil, s: 2]
	)
);

phoneListener = OSCFunc({ |msg|
	phoneAccel = msg.copyRange(1, 3);
}, '/accxyz', NetAddr("192.168.1.10", 8000), 9000);

phone.add(
	Canvas3DItem.cube.transform(Canvas3D.mScale(0.4, 0.8, 0.2))
);

phone.animate(40, { |t|

	// Determining Roll, Pitch from accelerometer:
	// https://stackoverflow.com/a/30195572/2076075
	var roll = phoneAccel[1].atan2(phoneAccel[2].sign * phoneAccel[2].hypot(0.032*phoneAccel[0]));
	var pitch = (-1 * phoneAccel[0]).atan2(phoneAccel[1].hypot(phoneAccel[2]));

	phone.transforms = [
		Canvas3D.mRotateZ(0),
		Canvas3D.mRotateY(pitch),
		Canvas3D.mRotateX(pi/2 + roll )
	];
});

win.view.keyDownAction = { |v, char|
	if($ == char, {
		Synth(\pb, [
			\out, bus,
			\buf, buf
		]);
	});
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