package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;


public class StreamSlicer extends Slicer<AudioDispatcher> {
	private AudioDispatcher dispatcher;
	private List<float[]> floatBuffers;
	
	public StreamSlicer(AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		this.floatBuffers = new ArrayList<>();
		
		this.dispatcher.addAudioProcessor(new AudioProcessor() {
			@Override
			public void processingFinished() {}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				float[] current = audioEvent.getFloatBuffer();
				float[] bufCopy = new float[current.length];
				System.arraycopy(current, 0, bufCopy, 0, current.length);
				floatBuffers.add(bufCopy);
				return true;
			}
		});
	}

	public AudioDispatcher slice() {
		float totalBuf[] = new float[floatBuffers.size() * floatBuffers.get(0).length];
		int i = 0;
		for(float[] buf : floatBuffers) {
			System.arraycopy(buf, 0, totalBuf, i, buf.length);
			i+=buf.length;
		}
		floatBuffers.clear();
		AudioDispatcher dispatcher = null;
		try {
			dispatcher = AudioDispatcherFactory.fromFloatArray(totalBuf, 
					Config.getInt("SAMPLE_RATE"), 
					Config.getInt("BUFFER_SIZE"), 
					Config.getInt("BUFFER_OVERLAP"));
		}
		catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}		
		return dispatcher;
	}

	@Override
	public void run() {
		AudioDispatcher dispatcher = slice();
		emitSliceEvent(dispatcher);
	}
}
