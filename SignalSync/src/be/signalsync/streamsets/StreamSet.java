package be.signalsync.streamsets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import be.tarsos.dsp.AudioDispatcher;

/**
 * This class is a container for different streams in a synchronization context.
 * The streams can be executed concurrently using the run method.
 *
 * @author Ward Van Assche
 *
 */
public abstract class StreamSet implements Runnable {
	protected List<AudioDispatcher> streams;
	protected ExecutorService streamExecutor;

	public StreamSet() {
		streams = new ArrayList<>();
	}

	public AudioDispatcher first() {
		return streams.get(0);
	}

	protected ExecutorService getStreamExecutor() {
		return streamExecutor;
	}

	public List<AudioDispatcher> getStreams() {
		return streams;
	}

	/**
	 * Execute the streams (reference and other streams).
	 */
	@Override
	public void run() {
		for (final AudioDispatcher d : streams) {
			streamExecutor.execute(d);
		}
		streamExecutor.shutdown();
	}
}
