package be.signalsync.core;

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
public class StreamSet implements Runnable {
	private final AudioDispatcher reference;
	private final List<AudioDispatcher> others;
	private final ExecutorService streamExecutor;

	/**
	 * Create a new StreamSet.
	 * 
	 * @param reference
	 *            The stream which should be used as reference for the other
	 *            streams.
	 * @param streams
	 *            The other streams which should be synchronized with the
	 *            reference stream.
	 */
	public StreamSet(final AudioDispatcher reference, final List<AudioDispatcher> streams) {
		others = new ArrayList<>(streams);
		this.reference = reference;
		streamExecutor = Executors.newFixedThreadPool(streams.size() + 1);
	}

	public List<AudioDispatcher> getOthers() {
		return others;
	}

	public AudioDispatcher getReference() {
		return reference;
	}

	/**
	 * Execute the streams (reference and other streams).
	 */
	@Override
	public void run() {
		streamExecutor.execute(reference);
		for (final AudioDispatcher d : others) {
			streamExecutor.execute(d);
		}
	}
}
