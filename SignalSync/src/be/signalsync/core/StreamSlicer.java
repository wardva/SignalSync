package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

/**
 * This class is a Slicer which is used to slice a single stream.
 * 
 * @author Ward Van Assche
 *
 */
public class StreamSlicer extends Slicer<AudioDispatcher> {
	/**
	 * The AudioDispatcher which contains the stream.
	 */
	private AudioDispatcher dispatcher;

	/**
	 * A list of float buffers, used to buffer the newly received data from the
	 * stream. The list is cleared each slice operation.
	 */
	private List<float[]> floatBuffers;

	/**
	 * Lock used for avoiding interference between AudioProcessor's process
	 * method and the slice method.
	 */
	private final ReentrantLock processingLock = new ReentrantLock();

	/**
	 * Creates a new StreamSlicer from an AudioDispatcher.
	 * 
	 * @param dispatcher
	 */
	public StreamSlicer(final AudioDispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
		floatBuffers = new ArrayList<>();

		/*
		 * Add an AudioProcessor to the dispatcher. The processor will buffer to
		 * data into the floatBuffers field.
		 */
		this.dispatcher.addAudioProcessor(new AudioProcessor() {
			@Override
			public boolean process(final AudioEvent audioEvent) {
				final float[] current = audioEvent.getFloatBuffer();
				final float[] bufCopy = new float[current.length];
				System.arraycopy(current, 0, bufCopy, 0, current.length);
				processingLock.lock();
				floatBuffers.add(bufCopy);
				processingLock.unlock();
				return true;
			}

			@Override
			public void processingFinished() {
			}
		});
	}

	/**
	 * This method copies the data in the floatBuffers field to a new buffer and
	 * clears the floatBuffers field. A new AudioDispatcher is created with the
	 * new buffer and is returned.
	 *
	 * @return AudioDispatcher The dispatcher created from the current float
	 *         buffer.
	 */
	@Override
	public AudioDispatcher slice() {
		processingLock.lock();
		final float totalBuf[] = new float[floatBuffers.size() * floatBuffers.get(0).length];
		int i = 0;
		for (final float[] buf : floatBuffers) {
			System.arraycopy(buf, 0, totalBuf, i, buf.length);
			i += buf.length;
		}
		floatBuffers.clear();
		processingLock.unlock();
		AudioDispatcher dispatcher = null;
		try {
			dispatcher = AudioDispatcherFactory.fromFloatArray(totalBuf, Config.getInt("SAMPLE_RATE"),
					Config.getInt("BUFFER_SIZE"), Config.getInt("BUFFER_OVERLAP"));
		} catch (final UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
		return dispatcher;
	}
}
