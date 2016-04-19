package be.signalsync.sync;

import java.util.Map;

import be.signalsync.stream.StreamGroup;

/**
 * This interface has to be implemented by a class which is interested in
 * synchronization events emitted by the RealtimeStreamSync class.
 * @author Ward Van Assche
 */
public interface SyncEventListener {
	void onSyncEvent(Map<StreamGroup, Double> data);
}
