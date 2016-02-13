package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

/**
 * This class is used to split up audio streams (instance of AudioDispatcher) into
 * several shorter audio streams.
 * @author Ward Van Assche
 *
 */
public class StreamSlicer implements Callable<SpliceResult> {
	private AudioDispatcher dispatcher;
	private ExecutorService executor;
	private List<float[]> floatBuffers;
	private final ReentrantLock lock = new ReentrantLock();
	
	/**
	 * Create a new StreamSlicer
	 * @param dispatcher The dispatcher of the stream to slice. This dispatcher should have buffer overlap 0.
	 */
	public StreamSlicer(AudioDispatcher dispatcher) {
		this.dispatcher = dispatcher;
		this.executor = Executors.newSingleThreadExecutor();
		this.floatBuffers = new ArrayList<>();
	}
	
	/**
	 * Start the stream and buffer the processed data.
	 */
	public void start() {
		dispatcher.addAudioProcessor(new AudioProcessor() {
			
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				lock.lock();
				float[] current = audioEvent.getFloatBuffer();
				float[] bufCopy = new float[current.length];
				System.arraycopy(current, 0, bufCopy, 0, current.length);
				floatBuffers.add(bufCopy);
				lock.unlock();
				return true;
			}
		});
		executor.execute(dispatcher);
	}
	
	/**
	 * Stop the stream.
	 */
	public void stop() {
		this.dispatcher.stop();
	}
	
	/**
	 * Return the last slice of the stream.
	 * @return This method returns an AudioDispatcher object containing the stream data
	 * from the previous slice (or the start) until the moment this method has been called.
	 */
	public AudioDispatcher slice() {
		lock.lock();
		float totalBuf[] = new float[floatBuffers.size() * floatBuffers.get(0).length];
		int i = 0;
		for(float[] buf : floatBuffers) {
			System.arraycopy(buf, 0, totalBuf, i, buf.length);
			i+=buf.length;
		}
		floatBuffers.clear();
		lock.unlock();
		AudioDispatcher d = null;
		try {
			d = AudioDispatcherFactory.fromFloatArray(totalBuf, 
					Config.getInt("SAMPLE_RATE"), 
					Config.getInt("BUFFER_SIZE"), 
					Config.getInt("BUFFER_OVERLAP"));
		} 
		catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}		
		return d;
	}

	@Override
	public SpliceResult call() throws Exception {
		dispatcher.addAudioProcessor(new AudioProcessor() {
			@Override
			public void processingFinished() {
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				lock.lock();
				float[] current = audioEvent.getFloatBuffer();
				float[] bufCopy = new float[current.length];
				System.arraycopy(current, 0, bufCopy, 0, current.length);
				floatBuffers.add(bufCopy);
				lock.unlock();
				return true;
			}
		});
		executor.execute(dispatcher);
		return null;
	}
}
