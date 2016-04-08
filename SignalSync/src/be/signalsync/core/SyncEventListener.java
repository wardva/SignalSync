package be.signalsync.core;

import java.util.Map;

/**
 * This interface has to be implemented by a class which is interested in
 * synchronization events emitted by the RealtimeStreamSync class.
 *
 * @author Ward Van Assche
 *
 */
public interface SyncEventListener {
	void onSyncEvent(Map<StreamGroup, Double> data);
}
