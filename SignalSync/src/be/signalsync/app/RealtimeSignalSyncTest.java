package be.signalsync.app;

import java.util.Map;
import java.util.Map.Entry;

import be.signalsync.stream.StreamGroup;
import be.signalsync.stream.StreamSet;
import be.signalsync.stream.StreamSetFactory;
import be.signalsync.sync.RealtimeSignalSync;
import be.signalsync.sync.SyncEventListener;

/**
 * Application class for testing the current SignalSync implementation.
 * @author Ward Van Assche
 */
public class RealtimeSignalSyncTest {
	public static void main(final String[] args) {
		//Creating a streamset from some audio files.
		StreamSet streamSet = StreamSetFactory.createFromFiles(
				"./testdata/Clean/Sonic Youth - Star Power_-90_0hz.wav", 
				"./testdata/Clean/Sonic Youth - Star Power_0_0hz.wav");
		
		final RealtimeSignalSync syncer = new RealtimeSignalSync(streamSet);
		syncer.addEventListener(new SyncEventListener() {
			@Override
			public void onSyncEvent(final Map<StreamGroup, Double> data) {
				for(Entry<StreamGroup, Double> entry : data.entrySet()) {
					StreamGroup streamGroup = entry.getKey();
					double latency = entry.getValue();
					System.out.printf("%-40s: %.3f\n", streamGroup.getDescription(), latency);
				}
				System.out.println("---------------------------------");
			}
		});
		streamSet.start();
	}
}