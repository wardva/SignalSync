package be.signalsync.streamsets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private ExecutorService stopExecutor;

	public StreamSet() {
		streams = new ArrayList<>();
		stopExecutor = Executors.newCachedThreadPool();
	}

	protected ExecutorService getStreamExecutor() {
		return streamExecutor;
	}

	public List<AudioDispatcher> getStreams() {
		return streams;
	}
	
	public int size() {
		return streams.size();
	}
	
	public void stop() {
		for(AudioDispatcher d : streams) {
			if(!d.isStopped()) {
				stopExecutor.execute(new Runnable() {
					@Override
					public void run() {
						d.stop();
					}
				});
			}
		}
		stopExecutor.shutdown();
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
