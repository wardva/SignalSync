package be.signalsync.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.LineUnavailableException;

import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioPlayer;

/**
 * This class is used for starting and managing the realtime stream
 * synchronization algorithm.
 * 
 * @author Ward Van Assche
 *
 */
public class RealtimeStreamSync implements SliceListener<List<AudioDispatcher>> {

	/**
	 * This object contains the different streams. This object is a runnable and
	 * will be executed on a different thread. When this object is executed the
	 * available streams are executed as well.
	 */
	private final StreamSupply supply;

	/**
	 * This set contains the objects interested in possible changes of the
	 * latency between the different streams.
	 */
	private final Set<SyncEventListener> listeners;

	/**
	 * The threadpool used for executing the stream supply.
	 */
	private final ExecutorService streamExecutor = Executors.newSingleThreadExecutor();

	/**
	 * The threadpool used for running the supplySlicer each time interval.
	 */
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
		for (final SyncEventListener l : listeners) {
			l.onSyncEvent(data);
		}
	}

	/**
	 * This method will be executed when the supply streams have been sliced.
	 * It's called each refresh interval from the scheduled threadpool.
	 */
	@Override
	public void onSliceEvent(final List<AudioDispatcher> slices) {
		try {
			supply.getStreams().forEach(x -> x.stop());
			Thread.sleep(2000);
			final AudioDispatcher d = slices.get(0);
			d.addAudioProcessor(new AudioPlayer(d.getFormat()));
			d.run();
		} catch (InterruptedException | LineUnavailableException e) {
			e.printStackTrace();
		}
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
	 * the different streams available in the StreamSupply object. The
	 * synchronization process will take place each REFRESH_INTERVAL seconds.
	 * After this time, the listeners will be notified.
	 */
	public void synchronize() {
		streamExecutor.execute(supply);
		final SupplySlicer slicer = new SupplySlicer(supply);
		slicer.addEventListener(this);
		slicerExecutor.scheduleWithFixedDelay(slicer, Config.getInt("FIRST_DELAY"), Config.getInt("REFRESH_INTERVAL"),
				TimeUnit.MILLISECONDS);
	}
}
