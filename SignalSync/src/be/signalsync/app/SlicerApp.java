package be.signalsync.app;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.core.SliceListener;
import be.signalsync.core.Slicer;
import be.signalsync.core.StreamSlicer;
import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.WaveformWriter;

public class SlicerApp {
	public static void main(String[] args) {
		StreamSlicer s = new StreamSlicer(10, new SliceListener<float[]>() {
			private int i = 0;
			@Override
			public void done(Slicer<float[]> s) {
				System.out.println("Finished processing the slices.");
			}

			@Override
			public void onSliceEvent(float[] slices, Slicer<float[]> s) {
				try {
					AudioDispatcher writer = AudioDispatcherFactory.fromFloatArray(slices, 
							Config.getInt("SAMPLE_RATE"), Config.getInt("BUFFER_SIZE"), Config.getInt("BUFFER_OVERLAP"));
					String filename = "./Slices/Sonic Youth - Star Power (2000ms delay) - slice - " + i++;
					writer.addAudioProcessor(new WaveformWriter(writer.getFormat(), filename));
					writer.run();
					System.out.printf("Wrote slice %s to disk.\n", filename);
				} 
				catch (UnsupportedAudioFileException e) {
					e.printStackTrace();
				}
			}
		});
		
		AudioDispatcher d = AudioDispatcherFactory.fromPipe("./testdata/Sonic Youth - Star Power_2000.wav", 
				Config.getInt("SAMPLE_RATE"), Config.getInt("BUFFER_SIZE"), 0);
		d.addAudioProcessor(s);
		d.run();
	}
}
