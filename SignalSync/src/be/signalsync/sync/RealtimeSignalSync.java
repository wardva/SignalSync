package be.signalsync.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.signalsync.datafilters.DataFilter;
import be.signalsync.datafilters.DataFilterFactory;
import be.signalsync.slicer.SliceEvent;
import be.signalsync.slicer.SliceListener;
import be.signalsync.slicer.Slicer;
import be.signalsync.slicer.StreamSetSlicer;
import be.signalsync.stream.StreamGroup;
import be.signalsync.stream.StreamSet;
import be.signalsync.syncstrategy.LatencyResult;
import be.signalsync.syncstrategy.SyncStrategy;
import be.signalsync.util.Config;
import be.signalsync.util.Key;

/**
 * This class is used for starting and managing the real time stream
 * synchronization algorithm.
 * @author Ward Van Assche
 */
public class RealtimeSignalSync implements SliceListener<Map<StreamGroup, float[]>> {
	private static Logger Log = Logger.getLogger("signalSync");

	/**
	 * This set contains the objects interested in possible changes of the
	 * latency between the different streams.
	 */
	private final Set<SyncEventListener> listeners;

	private final StreamSetSlicer slicer;

	private final SyncStrategy strategy;
	
	private final Map<StreamGroup, DataFilter> latencyFilters;
	
	/**
	 * Create a new ReatimeStreamSync object.
	 * @param streamSet This object contains the different streams which must be synchronized.
	 * @param slicer The slicer object which should be used to slice the
	 */
	public RealtimeSignalSync(final StreamSet streamSet) {
		this.listeners = new HashSet<>();
		this.strategy = SyncStrategy.createDefault();
		this.latencyFilters = new HashMap<>(streamSet.size());
		this.slicer = streamSet.createSlicer(Config.getInt(Key.SLICE_SIZE_S), Config.getInt(Key.SLICE_STEP_S));
		this.slicer.addEventListener(this);
		
		for(StreamGroup streamGroup : streamSet.getStreamGroups()) {
			latencyFilters.put(streamGroup, DataFilterFactory.createDefault());
		}
	}

	/**
	 * Add an interested listener. The listeners will be notified when new
	 * Synchronization data is available.
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
	 * This method will be executed when the streamSet streams have been sliced.
	 */
	@Override
	public void onSliceEvent(SliceEvent<Map<StreamGroup, float[]>> event) {
		List<StreamGroup> streams = new ArrayList<>(event.getSlice().keySet());
		List<float[]> slices = new ArrayList<>(event.getSlice().values());
		List<LatencyResult> unfilteredLatencies = new ArrayList<>(streams.size());
		unfilteredLatencies.add(LatencyResult.refinedResult(0.0D, 0)); //Reference stream latency is 0.0
		unfilteredLatencies.addAll(strategy.findLatencies(slices));
		Map<StreamGroup, LatencyResult> filteredLatencies = new HashMap<>(streams.size());
		for(int i = 0; i<streams.size(); i++) {
			StreamGroup streamGroup = streams.get(i);
			DataFilter filter = latencyFilters.get(streamGroup);
			LatencyResult rawLatency = unfilteredLatencies.get(i);
			int filteredLatencyInSamples = (int) filter.filter(rawLatency.getLatencyInSamples());
			double filteredLatencyInSeconds = filteredLatencyInSamples / Config.getDouble(Key.SAMPLE_RATE);
			//Add the (potentiel) filtered latencies in a new LatencyResult object. Also add
			//the event begintime.
			LatencyResult filteredResult = new LatencyResult(filteredLatencyInSeconds, 
															 filteredLatencyInSamples, 
															 rawLatency.isLatencyFound(), 
															 rawLatency.isRefined());
			filteredLatencies.put(streamGroup, filteredResult);
		}
		emitSyncEvent(filteredLatencies);
	}

	/**
	 * Remove a listener from the interested set.
	 * @param listener The listener to remove.
	 */
	public void removeEventListener(final SyncEventListener listener) {
		listeners.remove(listener);
	}
	

	/**
	 * Emit a synchronization event.
	 * @param data The data to send to the interested listeners.
	 */
	private void emitSyncEvent(final Map<StreamGroup, LatencyResult> data) {
		for (final SyncEventListener l : listeners) {
			l.onSyncEvent(data);
		}
	}
}
