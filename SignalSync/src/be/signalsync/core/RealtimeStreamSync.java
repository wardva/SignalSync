package be.signalsync.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import be.panako.strategy.nfft.NFFTFingerprint;
import be.signalsync.util.Config;

public class RealtimeStreamSync {

	/**
	 * This object contains the different streams.
	 */
	private final StreamSupply supply;

	/**
	 * This set contains the objects interested in possible changes of the
	 * latency between the different streams.
	 */
	private final Set<SyncEventListener> listeners;

	/**
	 * Thread pool responsible for executing the stream processing runnable.
	 */
	private final ExecutorService streamsExecutor = Executors.newSingleThreadExecutor();

	/**
	 * Create a new ReatimeStreamSync object.
	 * 
	 * @param supply
	 *            This object contains the different streams which must be
	 *            synchronized.
	 */
	public RealtimeStreamSync(final StreamSupply supply) {
		this.supply = supply;
		listeners = new HashSet<>();
	}

	/**
	 * Add an interested listener. All the listeners will be notified
	 * when new synchronisation data is available.
	 * @param listener
	 */
	public void addEventListener(final SyncEventListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a listener from the interested list.
	 * @param listener
	 */
	public void removeEventListener(final SyncEventListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Start the synchronization process. This method will try to synchronize
	 * the different streams available in the StreamSupply object. The
	 * synchronization process will take place each REFRESH_INTERVAL seconds.
	 * After this time, the listeners will be notified.
	 */
	public void synchronize() {

		
		/*
		 * Iterate over the different streams and attach a
		 * NFFTEventPointProcessor to calculate the fingerprints.
		 */
		for (final Stream s : supply.getStreams()) {
			final RealtimeFingerprinter fingerprinter = new RealtimeFingerprinter(Config.getInt("BUFFER_SIZE"),
					Config.getInt("BUFFER_OVERLAP"), Config.getInt("SAMPLE_RATE"));
			s.addFingerprinter(fingerprinter);
		}

		/*
		 * Run the fingerprinters of all the available streams.
		 */
		streamsExecutor.execute(supply);

		/*
		 * This timer will call the run method each 'REFRESH_INTERVAL' seconds.
		 * This method will process the new fingerprints by delegating the
		 * synchronization process and calling the interested listeners.
		 */
		final Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				
				List<List<NFFTFingerprint>> fingerprints = supply.getNewFingerprints();
				
			}
		}, Config.getInt("FIRST_DELAY"), Config.getInt("REFRESH_INTERVAL"));
	}
}
