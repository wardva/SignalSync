package be.signalsync.sync;

import java.util.Map;

import be.signalsync.stream.StreamGroup;
import be.signalsync.syncstrategy.LatencyResult;

/**
 * This interface has to be implemented by a class which is interested in
 * synchronization events emitted by the RealtimeStreamSync class.
 * @author Ward Van Assche
 */
public interface SyncEventListener {
	void onSyncEvent(Map<StreamGroup, LatencyResult> data);
}
