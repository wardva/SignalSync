package be.signalsync.app;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.slicer.SliceEvent;
import be.signalsync.slicer.SliceListener;
import be.signalsync.slicer.Slicer;
import be.signalsync.slicer.StreamSlicer;
import be.signalsync.stream.AudioDispatcherStream;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.WaveformWriter;

/**
 * Class used for generating slices of testdata and saving them to disk.
 * @author Ward Van Assche
 *
 */
public class RecordedSlicerApp {
	public static void main(final String[] args) {
		RecordedSlicerApp app = new RecordedSlicerApp();
		
		final String directory = "./testdata/Recorded/";
		final String[] filenames = { "opname-reference", "opname-1", "opname-2", "opname-3" };
		
		for(String filename : filenames) {
			app.generateSlices(directory, filename + ".wav");
		}
	}
	
	private void generateSlices(String directory, String filename) {
		final int sampleRate = Config.getInt(Key.SAMPLE_RATE);
		final int bufferSize = Config.getInt(Key.NFFT_BUFFER_SIZE);
		final int stepSize = Config.getInt(Key.NFFT_STEP_SIZE);
		final int overlap = bufferSize - stepSize;
		
		final StreamSlicer slicer = new StreamSlicer();
		slicer.addEventListener(new SliceListener<float[]>() {
			private int i = 0;

			@Override
			public void done(final Slicer<float[]> s) {
				System.out.println("Finished processing the slices.");
			}

			@Override
			public void onSliceEvent(SliceEvent<float[]> event) {
				try {
					final AudioDispatcher writer = AudioDispatcherFactory.fromFloatArray(event.getSlices(), sampleRate, bufferSize, overlap);
					final String newname = "./Slices/Recorded/" + filename + " - slice - " + i++ + ".wav";
					writer.addAudioProcessor(new WaveformWriter(writer.getFormat(), newname));
					writer.run();
					System.out.printf("Wrote slice %s to disk.\n", newname);
				} catch (final UnsupportedAudioFileException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		});

		final AudioDispatcher d = AudioDispatcherFactory.fromPipe(directory + filename, sampleRate, bufferSize, 0);
		AudioDispatcherStream stream = new AudioDispatcherStream(d);
		stream.addStreamProcessor(slicer);
		d.run();
	}
}
