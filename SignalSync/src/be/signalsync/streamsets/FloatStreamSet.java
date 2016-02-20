package be.signalsync.streamsets;

import java.util.List;
import java.util.concurrent.Executors;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.util.Config;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class FloatStreamSet extends StreamSet {
	
	private List<float[]> data;

	public FloatStreamSet(List<float[]> data) {
		super();
		this.data = data;
		this.streamExecutor = Executors.newFixedThreadPool(data.size());
		reset();
	}
	
	@Override
	public void reset() {
		try {
			streams.clear();
			for(float[] streamData : data) {
				streams.add(AudioDispatcherFactory.fromFloatArray(streamData, Config.getInt("SAMPLE_RATE"), Config.getInt("BUFFER_SIZE"), Config.getInt("BUFFER_OVERLAP")));
			}
		} 
		catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}
}
