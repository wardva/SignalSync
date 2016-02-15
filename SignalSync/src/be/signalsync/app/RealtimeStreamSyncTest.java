package be.signalsync.app;

import be.signalsync.core.RealtimeStreamSync;
import be.signalsync.core.StreamSet;
import be.signalsync.core.SyncData;
import be.signalsync.core.SyncEventListener;
import be.signalsync.dummy.DummyStreamSet;

/**
 * Main class for testing the current SignalSync implementation.
 * 
 * @author Ward
 */
public class RealtimeStreamSyncTest {
	public static void main(final String[] args) {
		final StreamSet streamSet = new DummyStreamSet();
		final RealtimeStreamSync syncer = new RealtimeStreamSync(streamSet);
		syncer.addEventListener(new SyncEventListener() {
			@Override
			public void onSyncEvent(final SyncData data) {
				System.out.println(data.toString());
			}
		});
		syncer.synchronize();
	}
}
