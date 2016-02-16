package be.signalsync.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.signalsync.syncstrategy.SyncStrategy;
import be.signalsync.util.Config;

/**
 * This class is used for starting and managing the realtime stream
 * synchronization algorithm.
 *
 * @author Ward Van Assche
 *
 */
public class RealtimeStreamSync implements SliceListener<StreamSet> {
	private static Logger Log = Logger.getLogger(Config.get("APPLICATION_NAME"));

	/**
	 * This object contains the different streams. This object is a runnable and
	 * will be executed on a different thread. When this object is executed the
	 * available streams are executed as well.
	 */
	private final StreamSet streamSet;

	/**
	 * This set contains the objects interested in possible changes of the
	 * latency between the different streams.
	 */
	private final Set<SyncEventListener> listeners;

	/**
	 * The threadpool used for executing the stream streamSet.
	 */
	private final ExecutorService streamExecutor = Executors.newSingleThreadExecutor();

	private final SyncStrategy syncer;

	/**
	 * This method will be executed when the streamSet streams have been sliced.
	 * It's called each refresh interval from the scheduled threadpool.
	 */
	int nr = 0;

	/**
	 * Create a new ReatimeStreamSync object.
	 *
	 * @param streamSet
	 *            This object contains the different streams which must be
	 *            synchronized.
	 */
	public RealtimeStreamSync(final StreamSet streamSet) {
		this.streamSet = streamSet;
		listeners = new HashSet<>();
		syncer = SyncStrategy.getInstance();
	}

	/**
	 * Add an interested listener. The listeners will be notified when new
	 * synchronization data is available.
	 *
	 * @param listener
	 */
	public void addEventListener(final SyncEventListener listener) {
		listeners.add(listener);
	}

	/**
	 * Emit a synchronization event.
	 *
	 * @param data
	 *            The data to send to the interested listeners.
	 */
	public void emitSyncEvent(final SyncData data) {
		Log.log(Level.INFO, "Sync data generated.");
		for (final SyncEventListener l : listeners) {
			l.onSyncEvent(data);
		}
	}

	@Override
	public void onSliceEvent(final StreamSet sliceSet, Slicer<StreamSet> s) {
		Log.log(Level.INFO, "Slices of streams received.");
		final SyncData data = syncer.findLatencies(sliceSet);
		emitSyncEvent(data);
	}

	/**
	 * Remove a listener from the interested set.
	 *
	 * @param listener
	 *            The listener to remove.
	 */
	public void removeEventListener(final SyncEventListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Start the synchronization process. This method will try to synchronize
	 * the different streams available in the StreamSet object. The
	 * synchronization process will take place each REFRESH_INTERVAL seconds.
	 * After this time, the listeners will be notified.
	 */
	public void synchronize() {
		Log.log(Level.INFO, "Starting the synchronization.");
		StreamSetSlicer slicer = new StreamSetSlicer(streamSet, Config.getInt("REFRESH_INTERVAL"));
		slicer.addEventListener(this);
		streamExecutor.execute(streamSet);
	}

	@Override
	public void done(Slicer<StreamSet> s) {
		System.out.println("DONE");
		System.out.println("-----------------------------------------------------------------");
	}
}
