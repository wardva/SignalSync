package be.signalsync.app;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.core.SliceListener;
import be.signalsync.core.Slicer;
import be.signalsync.core.StreamSlicer;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.WaveformWriter;

public class SlicerApp {
	public static void main(final String[] args) {
		final int sampleRate = Config.getInt(Key.SAMPLE_RATE);
		final int bufferSize = Config.getInt(Key.BUFFER_SIZE);
		final int stepSize = Config.getInt(Key.STEP_SIZE);
		final int overlap = bufferSize - stepSize;

		final StreamSlicer s = new StreamSlicer(10, new SliceListener<float[]>() {
			private int i = 0;

			@Override
			public void done(final Slicer<float[]> s) {
				System.out.println("Finished processing the slices.");
			}

			@Override
			public void onSliceEvent(final float[] slices, final Slicer<float[]> s) {
				try {
					final AudioDispatcher writer = AudioDispatcherFactory.fromFloatArray(slices, sampleRate, bufferSize,
							overlap);
					final String filename = "./Slices/Sonic Youth - Star Power (2000ms delay) - slice - " + i++;
					writer.addAudioProcessor(new WaveformWriter(writer.getFormat(), filename));
					writer.run();
					System.out.printf("Wrote slice %s to disk.\n", filename);
				} catch (final UnsupportedAudioFileException e) {
					e.printStackTrace();
				}
			}
		});

		final AudioDispatcher d = AudioDispatcherFactory.fromPipe("./testdata/Sonic Youth - Star Power_2000.wav",
				sampleRate, bufferSize, 0);
		d.addAudioProcessor(s);
		d.run();
	}
}
