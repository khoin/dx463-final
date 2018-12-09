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

(
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
)