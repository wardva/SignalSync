package be.signalsync.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.signalsync.streamsets.StreamSet;
import be.signalsync.syncstrategy.SyncStrategy;
import be.signalsync.util.Config;
import be.signalsync.util.Key;

/**
 * This class is used for starting and managing the realtime stream
 * synchronization algorithm.
 * @author Ward Van Assche
 */
public class RealtimeStreamSync implements SliceListener<List<float[]>>, Runnable {
	private static Logger Log = Logger.getLogger("signalSync");

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

	private final StreamSetSlicer slicer;

	private final SyncStrategy syncer;

	/**
	 * Create a new ReatimeStreamSync object.
	 * @param streamSet This object contains the different streams which must be synchronized.
	 * @param slicer The slicer object which should be used to slice the
	 */
	public RealtimeStreamSync(final StreamSet streamSet) {
		this.streamSet = streamSet;
		this.listeners = new HashSet<>();
		this.syncer = SyncStrategy.getDefault();
		
		this.slicer = new StreamSetSlicer(streamSet, Config.getInt(Key.SLICE_SIZE_S), Config.getInt(Key.SLICE_STEP_S));
		this.slicer.addEventListener(this);
	}

	/**
	 * Add an interested listener. The listeners will be notified when new
	 * synchronization data is available.
	 * @param listener The listener to add.
	 */
	public void addEventListener(final SyncEventListener listener) {
		listeners.add(listener);
	}

	/**
	 * This method will be called when the slicing is done.
	 */
	@Override
	public void done(final Slicer<List<float[]>> s) {
		Log.log(Level.INFO, "Done in realtime stream sync, application should exit.");
	}

	/**
	 * Emit a synchronization event.
	 * @param data The data to send to the interested listeners.
	 */
	public void emitSyncEvent(final List<Float> data) {
		for (final SyncEventListener l : listeners) {
			l.onSyncEvent(data);
		}
	}

	/**
	 * This method will be executed when the streamSet streams have been sliced.
	 * It's called each refresh interval from the scheduled threadpool.
	 */
	@Override
	public void onSliceEvent(SliceEvent<List<float[]>> event) {
		final List<Float> data = syncer.findLatencies(event.getSlices());
		emitSyncEvent(data);
	}

	/**
	 * Remove a listener from the interested set.
	 * @param listener The listener to remove.
	 */
	public void removeEventListener(final SyncEventListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void run() {
		Log.log(Level.INFO, "Starting the synchronization.");
		streamExecutor.execute(streamSet);
		streamExecutor.shutdown();
	}
}
