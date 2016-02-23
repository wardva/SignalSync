package be.signalsync.streamsets;

import java.util.List;
import java.util.concurrent.Executors;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.util.Config;
import be.signalsync.util.Key;
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
		int sampleRate = Config.getInt(Key.SAMPLE_RATE);
		int bufferSize = Config.getInt(Key.BUFFER_SIZE);
		int stepSize = Config.getInt(Key.STEP_SIZE);
		int overlap = bufferSize - stepSize;
		
		try {
			streams.clear();
			for(float[] streamData : data) {
				streams.add(AudioDispatcherFactory.fromFloatArray(streamData, sampleRate, bufferSize, overlap));
			}
		} 
		catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}
}
