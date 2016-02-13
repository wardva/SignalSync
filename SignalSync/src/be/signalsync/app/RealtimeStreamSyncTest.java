package be.signalsync.app;

import be.signalsync.core.RealtimeStreamSync;
import be.signalsync.core.StreamSupply;
import be.signalsync.core.SyncData;
import be.signalsync.core.SyncEventListener;
//import be.signalsync.dummy.DummySingleStreamSupply;
import be.signalsync.dummy.DummyStreamSupply;

/**
 * Main class for testing the current SignalSync implementation.
 * @author Ward
 *
 */
public class RealtimeStreamSyncTest {
	public static void main(final String[] args) {
		final StreamSupply supply = new DummyStreamSupply();
		final RealtimeStreamSync syncer = new RealtimeStreamSync(supply);
		syncer.addEventListener(new SyncEventListener() {
			@Override
			public void onSyncEvent(final SyncData data) {
				System.out.println("Sync event fired:\n" + data.getData());
			}
		});
		syncer.synchronize();
	}
}
