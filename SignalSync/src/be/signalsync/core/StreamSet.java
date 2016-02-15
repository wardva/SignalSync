package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import be.tarsos.dsp.AudioDispatcher;

/**
 * A class containing different streams.
 * 
 * @author Ward Van Assche
 *
 */
public class StreamSet implements Runnable {
	private AudioDispatcher reference;
	private List<AudioDispatcher> others;
	private ExecutorService streamExecutor;
	
	public StreamSet(AudioDispatcher reference, List<AudioDispatcher> streams) {
		this.others = new ArrayList<>(streams);
		this.reference = reference;
		this.streamExecutor = Executors.newFixedThreadPool(streams.size() + 1);
	}
	
	public List<AudioDispatcher> getOthers() {
		return others;
	}

	public ExecutorService getStreamExecutor() {
		return streamExecutor;
	}

	public AudioDispatcher getReference() {
		return reference;
	}

	@Override
	public void run() { 
		streamExecutor.execute(reference);
		for (final AudioDispatcher d : others) {
			streamExecutor.execute(d);
		}
	}
}
