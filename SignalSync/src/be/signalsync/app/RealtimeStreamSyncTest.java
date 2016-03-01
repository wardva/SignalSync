package be.signalsync.app;

import java.util.List;

import be.signalsync.core.RealtimeStreamSync;
import be.signalsync.core.SyncEventListener;
import be.signalsync.streamsets.DummyTwoStreamSet;
import be.signalsync.streamsets.StreamSet;

/**
 * Main class for testing the current SignalSync implementation.
 * 
 * @author Ward
 */
public class RealtimeStreamSyncTest {
	public static void main(final String[] args) {
		final StreamSet streamSet = new DummyTwoStreamSet();
		final RealtimeStreamSync syncer = new RealtimeStreamSync(streamSet);
		syncer.addEventListener(new SyncEventListener() {
			@Override
			public void onSyncEvent(final List<Float> data) {
				data.forEach(d -> System.out.printf("%.4f\n", d));
				System.out.println("---------------------------------");
			}
		});
		syncer.run();
	}
}