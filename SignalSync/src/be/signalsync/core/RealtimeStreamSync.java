package be.signalsync.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;

public class RealtimeStreamSync implements SliceListener<List<AudioDispatcher>> {

	/**
	 * This object contains the different streams.
	 */
	private final StreamSupply supply;

	/**
	 * This set contains the objects interested in possible changes of the
	 * latency between the different streams.
	 */
	private final Set<SyncEventListener> listeners;

	private final ExecutorService streamExecutor = Executors.newSingleThreadExecutor();
	private final ScheduledExecutorService slicerExecutor = Executors.newScheduledThreadPool(1);

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
	
	public void emitSyncEvent(SyncData data) {
		for(SyncEventListener l : listeners) {
			l.onSyncEvent(data);
		}
	}

	/**
	 * Start the synchronization process. This method will try to synchronize
	 * the different streams available in the StreamSupply object. The
	 * synchronization process will take place each REFRESH_INTERVAL seconds.
	 * After this time, the listeners will be notified.
	 */
	public void synchronize() {
		streamExecutor.execute(supply);
		SupplySlicer slicer = new SupplySlicer(supply);
		slicer.addEventListener(this);
		slicerExecutor.scheduleWithFixedDelay(slicer, 
				Config.getInt("FIRST_DELAY"), 
				Config.getInt("REFRESH_INTERVAL"), 
				TimeUnit.MILLISECONDS);
	}

	@Override
	public void onSliceEvent(List<AudioDispatcher> slices) {
		//emitSyncEvent(new SyncData(slices.toString()));
	}
}
