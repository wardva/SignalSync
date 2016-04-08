package be.signalsync.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.signalsync.datafilters.DataFilter;
import be.signalsync.datafilters.DataFilterFactory;
import be.signalsync.syncstrategy.SyncStrategy;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;

/**
 * This class is used for starting and managing the realtime stream
 * synchronization algorithm.
 * @author Ward Vasn Assche
 */
public class RealtimeStreamSync implements SliceListener<Map<StreamGroup, float[]>>, Runnable {
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
	private final ExecutorService streamExecutor;

	private final StreamSetSlicer slicer;

	private final SyncStrategy syncer;
	
	private final Map<StreamGroup, DataFilter> latencyFilters;

	/**
	 * Create a new ReatimeStreamSync object.
	 * @param streamSet This object contains the different streams which must be synchronized.
	 * @param slicer The slicer object which should be used to slice the
	 */
	public RealtimeStreamSync(final StreamSet streamSet) {
		this.streamSet = streamSet;
		this.listeners = new HashSet<>();
		this.syncer = SyncStrategy.createDefault();
		this.latencyFilters = new HashMap<>(streamSet.size());
		this.slicer = new StreamSetSlicer(streamSet, Config.getInt(Key.SLICE_SIZE_S), Config.getInt(Key.SLICE_STEP_S));
		this.streamExecutor = Executors.newFixedThreadPool(streamSet.size());
		this.slicer.addEventListener(this);
		
		for(StreamGroup streamGroup : streamSet.getStreamGroups()) {
			latencyFilters.put(streamGroup, DataFilterFactory.createDefault());
		}
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
	public void done(final Slicer<Map<StreamGroup, float[]>> s) {
		Log.log(Level.INFO, "Done in realtime stream sync, application should exit.");
	}

	/**
	 * Emit a synchronization event.
	 * @param data The data to send to the interested listeners.
	 */
	public void emitSyncEvent(final Map<StreamGroup, Double> data) {
		for (final SyncEventListener l : listeners) {
			l.onSyncEvent(data);
		}
	}

	/**
	 * This method will be executed when the streamSet streams have been sliced.
	 */
	@Override
	public void onSliceEvent(SliceEvent<Map<StreamGroup, float[]>> event) {
		List<StreamGroup> streams = new ArrayList<>(event.getSlices().keySet());
		List<float[]> slices = new ArrayList<>(event.getSlices().values());
		
		List<Double> rawLatencies = new ArrayList<>(streams.size());
		rawLatencies.add(0.0D); //Reference stream latency
		rawLatencies.addAll(syncer.findLatencies(slices));
		
		Map<StreamGroup, Double> filteredLatencies = new HashMap<>(streams.size());
		for(int i = 0; i<streams.size(); i++) {
			StreamGroup streamGroup = streams.get(i);
			DataFilter filter = latencyFilters.get(streamGroup);
			double rawLatency = rawLatencies.get(i);
			double filteredLatency = filter.filter(rawLatency);
			filteredLatencies.put(streamGroup, filteredLatency);
		}
		
		/*List<Float> timing = new ArrayList<>();
		for(float latency : latencies) {
			timing.add((float) (latency + event.getBeginTime()));
		}
		emitSyncEvent(timing);*/
		
		emitSyncEvent(filteredLatencies);
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
		for(AudioDispatcher d : streamSet.getAudioStreams()) {
			streamExecutor.execute(d);
		}
		streamExecutor.shutdown();
	}
}
