KMap {
	*toLog { |x, factor = 1|
		^((x*factor + 1).log/(factor + 1).log);
	}

	*fromLog { |x, factor = 1|
		^(((factor + 1).pow(x) - 1) / factor);
	}

	// takes in linear
	// returns 0 - 1 mapped to -range dB - 0dB
	*toDb { |x, range = 80|
		^(x.ampdb + range / range);
	}
}


// Displays a spectrum analyzer a given ugen.
// Also displays band amplitudes
// This is an ad-hoc spectrum view. It's not intended for
// modular usage. However, it conforms with Layout views.

KSpectro {
	const logFac = 20;

	var ugen,
		fftSize,
		dbRange;

	var uv,
		fftResult,
		fftOSCFunc,
		freqDict,
		freqOSCFunc;

	*new { | ugen, fftSize, dbRange|
		^super.newCopyArgs(ugen, fftSize, dbRange).init;
	}

	init {
		uv			= UserView();

		fftResult	= Array.fill(fftSize, 0);
		fftOSCFunc	= OSCFunc(
			func:			{ |msg|
				fftResult.put(msg[2], msg[3]);
			},
			path: '/spectro',
			srcID: Server.default.addr
		);

		freqDict	= Dictionary.new;
		freqOSCFunc	= OSCFunc(
			func:			{ |msg|
				freqDict.put(msg[3], msg[4]);
			},
			path: '/freqAmp',
			srcID: Server.default.addr
		);

		uv.minSize		= Size(600, 200);
		uv.background	= Color.black;
		uv.animate		= true;
		uv.frameRate	= 60;

		this.setDrawFunc;
		^this;
	}

	asView { ^uv; }

	setDrawFunc {
		uv.drawFunc = {
			var sR = Server.default.sampleRate;
			var w, h;
			w = uv.bounds.width;
			h = uv.bounds.height;

			Pen.smoothing_(false);

			// Draw frequency guides
			Pen.strokeColor = Color.new(1, 1, 1, 0.3);
			Array.interpolation(12, 0, 1).do({ |i|
				Pen.stringAtPoint(
					((KMap.fromLog(i, logFac) / 2 * sR / 1000).asStringPrec(3)) ++ ' KHz',
					(i * w) @ 0,
					color: Color.white
				);
				Pen.line(
					(i * w) @ 0,
					(i * w) @ h
				);
				Pen.stroke;
			});

			// Draw Spectrogram
			Pen.strokeColor = Color.green;
			Pen.moveTo( 0 @ h );

			fftResult.do({ |val, ind|
				Pen.lineTo(
					(KMap.toLog(ind/fftResult.size, logFac) * w)
					@
					// FFT doesn't seem to be 0-1, I don't know what's the max. 130 for now.
					((1-KMap.toDb(val/130, dbRange)) * h)
				);
			});

			Pen.stroke;

			// Draw Band Detectors
			Pen.strokeColor = Color.red;
			freqDict.keysValuesDo({ |key, val|
				Pen.line(
					(KMap.toLog((key * 2 / sR), logFac) * w )
					@
					((1-KMap.toDb(val, dbRange)) * h)
				,
					(KMap.toLog((key * 2 / sR), logFac) * w )
					@
					(h);
				).stroke;
			});

			// Ask the analyzer for FFT Result.
			ugen.set(\t_trigger, 1);
		}
	}
}