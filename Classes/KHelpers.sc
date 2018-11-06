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

KSpectro {
	const logFac = 20;

	var uvParent,
		uvBounds,
		ugen,
		fftSize,
		dbRange;

	var uv,
		fftResult,
		fftOSCFunc,
		freqDict,
		freqOSCFunc;

	*new { |parent, bounds, ugen, fftSize, dbRange|
		^super.newCopyArgs(parent, bounds, ugen, fftSize, dbRange).init;
	}

	init {
		uv			= UserView(
			parent:		uvParent,
			bounds:		uvBounds
		);

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

		uv.background	= Color.black;
		uv.animate		= true;
		uv.frameRate	= 60;

		this.setDrawFunc;
		^this;
	}

	setDrawFunc {
		uv.drawFunc = {
			var s = Server.default;
			Pen.smoothing_(false);

			// Draw frequency guides
			Pen.strokeColor = Color.new(1, 1, 1, 0.3);
			Array.interpolation(12, 0, 1).do({ |i|
				Pen.stringAtPoint(
					((KMap.fromLog(i, logFac) / 2 * s.sampleRate / 1000).asStringPrec(3)) ++ ' KHz',
					(i * uvBounds.width) @ 0,
					color: Color.white
				);
				Pen.line(
					(i * uvBounds.width) @ 0,
					(i * uvBounds.width) @ uvBounds.height
				);
				Pen.stroke;
			});

			// Draw Spectrogram
			Pen.strokeColor = Color.green;
			Pen.moveTo( 0 @ uvBounds.height );

			fftResult.do({ |val, ind|
				Pen.lineTo(
					(KMap.toLog(ind/fftResult.size, logFac) * uvBounds.width)
					@
					// FFT doesn't seem to be 0-1, I don't know what's the max. 130 for now.
					((1-KMap.toDb(val/130, dbRange)) * uvBounds.height)
				);
			});

			Pen.stroke;

			// Draw Band Detectors
			Pen.strokeColor = Color.red;
			freqDict.keysValuesDo({ |key, val|
				Pen.line(
					(KMap.toLog((key * 2 / s.sampleRate), logFac) * uvBounds.width )
					@
					((1-KMap.toDb(val, dbRange)) * uvBounds.height)
				,
					(KMap.toLog((key * 2 / s.sampleRate), logFac) * uvBounds.width )
					@
					(uvBounds.height);
				).stroke;
			});

			// Ask the analyzer for FFT Result.
			ugen.set(\t_trigger, 1);
		}
	}
}