package be.signalsync.app;

import java.util.Map;
import java.util.Map.Entry;

import be.signalsync.stream.StreamGroup;
import be.signalsync.stream.StreamSet;
import be.signalsync.stream.StreamSetFactory;
import be.signalsync.sync.RealtimeSignalSync;
import be.signalsync.sync.SyncEventListener;
import be.signalsync.syncstrategy.LatencyResult;

/**
 * Application class for testing the current SignalSync implementation.
 * @author Ward Van Assche
 */
public class RealtimeSignalSyncTest {
	public static void main(final String[] args) {
		//Creating a streamset from some audio files.
		StreamSet streamSet = StreamSetFactory.createCleanStreamSet();
		
		final RealtimeSignalSync syncer = new RealtimeSignalSync(streamSet);
		syncer.addEventListener(new SyncEventListener() {
			@Override
			public void onSyncEvent(final Map<StreamGroup, LatencyResult> data) {
				for(Entry<StreamGroup, LatencyResult> entry : data.entrySet()) {
					StreamGroup streamGroup = entry.getKey();
					LatencyResult result = entry.getValue();
					System.out.println("Streamgroup: " + streamGroup.getDescription());
					System.out.println(result.toString());
				}
				System.out.println("---------------------------------\n\n");
				
			}
		});
		streamSet.start();
	}
}