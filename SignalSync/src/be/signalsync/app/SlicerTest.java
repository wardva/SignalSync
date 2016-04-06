package be.signalsync.app;

import be.signalsync.core.SliceEvent;
import be.signalsync.core.SliceListener;
import be.signalsync.core.Slicer;
import be.signalsync.core.StreamSlicer;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class SlicerTest {
	public static void main(String[] args) {
		AudioDispatcher d = AudioDispatcherFactory.fromPipe("./testdata/Clean/Sonic Youth - Star Power_2000_50hz.wav", Config.getInt(Key.SAMPLE_RATE),
				Config.getInt(Key.NFFT_BUFFER_SIZE), Config.getInt(Key.NFFT_STEP_SIZE));
		StreamSlicer slicer = new StreamSlicer();
		slicer.addEventListener(new SliceListener<float[]>() {
			@Override
			public void onSliceEvent(SliceEvent<float[]> event) {
				System.out.println("slices length: " + event.getSlices().length);
			}
			
			@Override
			public void done(Slicer<float[]> s) {}
		});
		d.addAudioProcessor(slicer);
		d.run();
	}
}
