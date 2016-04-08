package be.signalsync.app;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import be.signalsync.core.RealtimeStreamSync;
import be.signalsync.core.StreamGroup;
import be.signalsync.core.StreamSet;
import be.signalsync.core.StreamSetFactory;
import be.signalsync.core.SyncEventListener;

/**
 * Main class for testing the current SignalSync implementation.
 * 
 * @author Ward
 */
public class RealtimeStreamSyncTest {
	public static void main(final String[] args) {
		StreamSet streamSet = StreamSetFactory.createRecordedTeensyStreamSet();
		
		final RealtimeStreamSync syncer = new RealtimeStreamSync(streamSet);
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

		new Thread(new Runnable() {
			private Scanner input = new Scanner(System.in);
			@Override
			public void run() {
				while(!input.nextLine().trim().equalsIgnoreCase("stop"));
				streamSet.getAudioStreams().forEach(d -> d.stop());
			}
		}).start();
		syncer.run();
	}
}