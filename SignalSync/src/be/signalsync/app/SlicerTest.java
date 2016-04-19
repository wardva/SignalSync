package be.signalsync.app;

import be.signalsync.slicer.SliceEvent;
import be.signalsync.slicer.SliceListener;
import be.signalsync.slicer.Slicer;
import be.signalsync.slicer.StreamSlicer;
import be.signalsync.stream.AudioDispatcherStream;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class SlicerTest {
	public static void main(String[] args) {
		AudioDispatcher d = AudioDispatcherFactory.fromPipe("./testdata/Clean/Sonic Youth - Star Power_2000_50hz.wav", Config.getInt(Key.SAMPLE_RATE),
				Config.getInt(Key.NFFT_BUFFER_SIZE), Config.getInt(Key.NFFT_STEP_SIZE));
		AudioDispatcherStream stream = new AudioDispatcherStream(d);
		StreamSlicer slicer = new StreamSlicer();
		slicer.addEventListener(new SliceListener<float[]>() {
			@Override
			public void onSliceEvent(SliceEvent<float[]> event) {
				System.out.println("slices length: " + event.getSlices().length);
			}
			
			@Override
			public void done(Slicer<float[]> s) {}
		});
		stream.addStreamProcessor(slicer);
		d.run();
	}
}
