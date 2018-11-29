// Boot Server
(
// These will be used to build the Node Tree and keep track of things.
// tree contains a list of things. If type is \group, it's group;
// otherwise, it's the Synth's name.
~tree = [
	(
		type: \group,
		name: \monitor,
		children: [
			(
				type: \mic, name: \rawMic,
				params: [\delay, 0]
			),
			(
				type: \group,
				children: [
					(
						type: \freqAmp, inSibling: true, params: [\freq, 813], name: \f1
					),
					(
						type: \freqAmp, inSibling: true, params: [\freq, 1204], name: \f2
					),
					(
						type: \freqAmp, inSibling: true, params: [\freq, 1606], name: \f3
					),
					(
						type: \freqAmp, inSibling: true, params: [\freq, 2032], name: \f4
					)
				]
			)
		]
	),
	(
		type: \group,
		name: \blowey,
		children: [
			(
				type: \mic,
				params: [\delay, 0.0, \amp, 10.dbamp]
			),
			(
				type: \pshift, name: \ps1
			),
			(
				type: \reverb, name: \rev, params: [\shimmerPitch, 99/100, \decayRate, 0.09]
			)
		]
	),
	(
		type: \group,
		name: \realTime1,
		children: [
			(
				type: \sin,
				params: [\freq, 450]
			)
		]
	),
	(
		type: \group,
		name: \testScene,
		children: [
			(
				type: \group,
				children: [
					(
						type: \sin, name: \s1, params: [\freq, 1840]
					),
					(
						type: \sin, name: \s2, outSibling: true, params: [\freq, 1000]
					)
				]
			),
			(
				type: \reverb
			)
		]
	)
];

~fftLength = 2.pow(8);
s.waitForBoot({
	"sdefs.sc".loadRelative;
});
)

// After booting seve
(
// GUI
var winWidth			= 700,
	winHeight			= 500,
	win					= Window(
		name:			"K GUI",
		bounds:			Rect(1200, 200, winWidth, winHeight),
	).background_(Color.grey);
var phone				= Canvas3D().distance_(2),
	phoneAccel			= [0, 0, 0],
	phoneListener		;

var tree				= KTree(~tree);
var selector			= Synth.after(tree.masterGroup, \select, [
	\in, [tree.buses[\blowey], tree.buses[\realTime1], tree.buses[\testScene]],
	\out, 0
]);

var masterAnalyzer		= Synth.after(selector, \spectro, [\in, 0]);
var micAnalyzer			= Synth.after(selector, \spectro, [\in, tree.buses[\rawMic]]);
var index = 0;

tree.synths[\ps1].set(
	\ctrl1, tree.buses[\f1],
	\ctrl2, tree.buses[\f2],
	\ctrl3, tree.buses[\f3],
	\ctrl4, tree.buses[\f4]
);

win.layout = VLayout(
	KSpectro([masterAnalyzer, micAnalyzer], ~fftLength, 80),
	HLayout(
		[phone, s: 1],
		[nil, s: 2]
	)
);
win.onClose				= {
	CmdPeriod.run;
};


phoneListener = OSCFunc({ |msg|
	phoneAccel = msg.copyRange(1, 3);
}, '/accxyz', NetAddr("10.18.254.234", 8000), 9000);

phone.add(Canvas3DItem.cube.transform(Canvas3D.mScale(0.4, 0.8, 0.2)));

phone.animate(40, { |t|

	// Determining Roll, Pitch from accelerometer:
	// https://stackoverflow.com/a/30195572/2076075
	// Which based on this paper:
	// https://www.nxp.com/files-static/sensors/doc/app_note/AN3461.pdf

	var roll = phoneAccel[1].atan2(phoneAccel[2].sign * phoneAccel[2].hypot(0.032*phoneAccel[0]));
	var pitch = (-1 * phoneAccel[0]).atan2(phoneAccel[1].hypot(phoneAccel[2]));

	tree.synths[\s1].set(\freq, 1840 + (sin(roll) * 1020));
	tree.synths[\s2].set(\freq, 1840*1.49 + (sin(pitch) * 920));

	//tree.synths[\rev].set(\shimmerPitch, 1 + ((pitch)/2pi));

	phone.transforms = [
		Canvas3D.mRotateY(pitch),
		Canvas3D.mRotateX(pi/2 + roll)
	];
});

win.view.keyDownAction = { |v, char|
	if($ == char, {
		selector.set(\index, index % 3);
		index = index + 1;
	});
};

// Properties


// Event Handlers
CmdPeriod.doOnce({
	"Closing".postln;
	if (win.isClosed.not, {win.close});
	s.freeAll;
});


// And... Go!
win.alwaysOnTop			= true;
win.front;
)