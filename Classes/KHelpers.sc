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

	var synths,
		fftSize,
		dbRange;

	var uv,
		fftResults,
		fftOSCFunc,
		freqDict,
		freqOSCFunc;

	*new { | synth, fftSize, dbRange|
		^super.newCopyArgs(synth, fftSize, dbRange).init;
	}

	init {
		uv			= UserView();

		fftResults	= Dictionary.new;
		fftOSCFunc	= OSCFunc(
			func:			{ |msg|
				fftResults.atFail(msg[1], {
					fftResults.put(msg[1], Array.fill(fftSize, 0));
				});
				fftResults[msg[1]].put(msg[2], msg[3]);

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

			// Draw Spectrograms
			fftResults.values.do({ |result, i|
				Pen.strokeColor = Color.hsv(0.3+i*0.2%1, 1, 1);
				Pen.moveTo( 0 @ h );

				result.do({ |val, ind|
					Pen.lineTo(
						(KMap.toLog(ind/fftSize, logFac) * w)
						@
						// FFT doesn't seem to be 0-1, I don't know what's the max. 130 for now.
						((1-KMap.toDb(val/130, dbRange)) * h)
					);
				});
				Pen.stroke;
			});


			// Draw Band Detectors
			Pen.strokeColor = Color.cyan;
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
			synths.do(_.set(\t_trigger, 1));
		}
	}
}