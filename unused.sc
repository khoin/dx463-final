(
var winWidth			= 200,
	winHeight			= 300,
	win					= Window(
		name:			"Phone Accelerometer",
		bounds:			Rect(1200, 200, winWidth, winHeight),
	)	.background_	(Color.grey);
var phone				= Canvas3D().distance_(2),
	phoneAccel			= [0, 0, 0],
	phonePR				= [0, 0],
	phoneListener		;

win.layout = HLayout(phone);
win.onClose				= {
	CmdPeriod.run;
};

phoneListener = OSCFunc({ |msg|
	phoneAccel = msg.copyRange(1, 3);

	// TouchOSC only provides accelerometer data /accxyz,
	// so it is impossible to figure out all Euler angles.
	// Determining Roll, Pitch from accelerometer:
	// https://stackoverflow.com/a/30195572/2076075
	// Which based on this paper:
	// https://www.nxp.com/files-static/sensors/doc/app_note/AN3461.pdf

	// PhoneP[itch]R[oll]
	// pitch: -pi to pi
	// roll: -pi/2 to pi/2
	phonePR[1] = (-1 * phoneAccel[0]).atan2(phoneAccel[1].hypot(phoneAccel[2]));
	phonePR[0] = phoneAccel[1].atan2(phoneAccel[2].sign * phoneAccel[2].hypot(0.032*phoneAccel[0]));

	// tree.synths[\bell].set(\pitch, phonePR[0], \roll, phonePR[1]);


}, '/accxyz', NetAddr("10.18.254.234", 8000), 9000);

phone.add(Canvas3DItem.cube.transform(Canvas3D.mScale(0.4, 0.8, 0.2)));

phone.animate(40, { |t|
	phone.transforms = [
		Canvas3D.mRotateY(phonePR[1]),
		Canvas3D.mRotateX(pi/2 + phonePR[0])
	];
});

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