package be.signalsync.core;

/**
 * This interface has to be implemented by a class which is interested in
 * synchronization events emitted by the RealtimeStreamSync class.
 * 
 * @author Ward Van Assche
 *
 */
public interface SyncEventListener {
	void onSyncEvent(SyncData data);
}
