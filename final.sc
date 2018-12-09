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
						type: \freqAmp, params: [\freq, 813], name: \f1
					),
					(
						type: \freqAmp, inSibling: true, params: [\freq, 1204], name: \f2
					),
					(
						type: \freqAmp, inSibling: true, params: [\freq, 1606], name: \f3
					),
					(
						type: \freqAmp, inSibling: true, params: [\freq, 2023], name: \f4
					),
					(
						type: \freqAmp, inSibling: true, params: [\freq, 2465], name: \f5
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
				type: \reverb,
				params: [\shimmerPitch, 98/99 ,\decayRate, 0.88]
			)
		]
	),
	(
		type: \group,
		name: \two,
		children: [
			(
				type: \mic,
				params: [\delay, 0.0, \amp, 10.dbamp]
			),
			(
				type: \two,
				params: [\gainCtrlOut, KTBus(\gainCtrl), \gainCtrl, KTBus(\f1),
					\shimmerCtrlOut, KTBus(\shimmerCtrl), \shimmerUpCtrl, KTBus(\f4), \shimmerDownCtrl, KTBus(\f5)
				]
			),
			(
				type: \reverb, name: \freezer,
				params: [\mix, 1, \bandwidth, 1, \decayRate, 0.999999, \processMode, 1,
					\processGainBus, KTBus(\gainCtrl), \shimmerPitchBus, KTBus(\shimmerCtrl)
				]
			),
			(
				type: \reverb, inSibling: true, outSibling: true
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
						type: \sin, params: [\freq, 1840]
					),
					(
						type: \sin, outSibling: true, params: [\freq, 1000]
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
// GUI stuff
var winWidth			= 700,
	winHeight			= 500,
	win					= Window(
		name:			"K GUI",
		bounds:			Rect(1200, 200, winWidth, winHeight),
	).background_(Color.grey);

// Sound stuff
var tree				= KTree(~tree);
var audibleGroups		= [\blowey, \two, \testScene];
var fadeTime			= 1;
var selector			= Synth.after(tree.masterGroup, \select, [
	\in, audibleGroups.collect(tree.buses[_]),
	\out, 0,
	\fade, fadeTime
]);

// GUI + Sound stuff
var masterAnalyzer		= Synth.after(selector, \spectro, [\in, 0]);
var micAnalyzer			= Synth.after(selector, \spectro, [\in, tree.buses[\rawMic]]);
var index = 0;

// Function helpers
var focusGroup = { |name|
	audibleGroups.do({ |n|
		if ( (n == name).not ) {
			{ tree.groups[n].run(false) }.defer(fadeTime);
		}
	});
	tree.groups[name].run;
};

// Layout
win.layout = VLayout(
	KSpectro([masterAnalyzer, micAnalyzer], ~fftLength, dbRange: 80),
);

// Event Handlers
win.onClose				= {
	CmdPeriod.run;
};

CmdPeriod.doOnce({
	"Closing".postln;
	if (win.isClosed.not, {win.close});
	s.freeAll;
});

win.view.keyDownAction = { |v, char|
	switch (char,
		$ , {
			index = (index + 1) % audibleGroups.size;
			focusGroup.value(audibleGroups[index]);
			selector.set(\index, index);
			("Switched to Scene " ++ audibleGroups[index]).postln;
		},
		$g, {
			"------------------".postln;
			audibleGroups.do({ |n|
				("Group " ++ n ++ " running: ").post;
				tree.groups[n].isRunning.postln;
			});
		},
		$b, {
			tree.buses.keysValuesDo({ |name, id|
				id.post; Post.tab; name.postln;
			});
		}
	);
};

// Initializations
audibleGroups.do({ |n|
	//tree.groups[n].register(true);
});
focusGroup.value(audibleGroups.first);

// And... Go!
win.alwaysOnTop	= true;
win.front;
)