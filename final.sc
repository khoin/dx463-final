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
						type: \freqAmp, inSibling: true, params: [\freq, 2023], name: \f4
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
				type: \fork,
				params: [\ctrl1, KTBus(\f1), \ctrl2, KTBus(\f2), \ctrl3, KTBus(\f3), \ctrl4, KTBus(\f4)]
			),
			(
				type: \reverb, name: \rev, params: [\shimmerPitch, 98/99 ,\decayRate, 0.83]
			)
		]
	),
	(
		type: \group,
		name: \bells,
		children: [
			(
				type: \mic,
				params: [\delay, 0.0, \amp, 10.dbamp]
			),
			(
				type: \bells, name: \bell
			),
			(
				type: \reverb, params: [\decayRate, 0.3]
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
			)
		]
	)
];

~fftLength = 2.pow(8);
s.options.memSize = 2.pow(15);
s.options.numWireBufs = 128;
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
	phonePR				= [0, 0],
	phoneListener		;

var tree				= KTree(~tree);
var selector			= Synth.after(tree.masterGroup, \select, [
	\in, [tree.buses[\blowey], tree.buses[\bells], tree.buses[\testScene]],
	\out, 0
]);

var masterAnalyzer		= Synth.after(selector, \spectro, [\in, 0]);
var micAnalyzer			= Synth.after(selector, \spectro, [\in, tree.buses[\rawMic]]);
var index = 0;

win.layout = VLayout(
	KSpectro([masterAnalyzer, micAnalyzer], ~fftLength, dbRange: 80),
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

	// Determining Roll, Pitch from accelerometer:
	// https://stackoverflow.com/a/30195572/2076075
	// Which based on this paper:
	// https://www.nxp.com/files-static/sensors/doc/app_note/AN3461.pdf

	// pitch: -pi to pi
	// roll: -pi/2 to pi/2
	phonePR[1] = (-1 * phoneAccel[0]).atan2(phoneAccel[1].hypot(phoneAccel[2]));
	phonePR[0] = phoneAccel[1].atan2(phoneAccel[2].sign * phoneAccel[2].hypot(0.032*phoneAccel[0]));

	tree.synths[\bell].set(\pitch, phonePR[0], \roll, phonePR[1]);
}, '/accxyz', NetAddr("10.18.254.234", 8000), 9000);

phone.add(Canvas3DItem.cube.transform(Canvas3D.mScale(0.4, 0.8, 0.2)));

phone.animate(40, { |t|
	phone.transforms = [
		Canvas3D.mRotateY(phonePR[1]),
		Canvas3D.mRotateX(pi/2 + phonePR[0])
	];
});

win.view.keyDownAction = { |v, char|
	if($ == char, {
		index = index + 1;
		selector.set(\index, index % 3);
	});
};


// Event Handlers
CmdPeriod.doOnce({
	"Closing".postln;
	if (win.isClosed.not, {win.close});
	s.freeAll;
});


// And... Go!
win.alwaysOnTop	= true;
win.front;
)