package be.signalsync.app;

import java.util.List;
import java.util.Scanner;

import be.signalsync.core.RealtimeStreamSync;
import be.signalsync.core.SyncEventListener;
import be.signalsync.streamsets.StreamSet;
import be.signalsync.streamsets.TeensyRecordedStreamSet;

/**
 * Main class for testing the current SignalSync implementation.
 * 
 * @author Ward
 */
public class RealtimeStreamSyncTest {
	public static void main(final String[] args) {
		StreamSet streamSet = new TeensyRecordedStreamSet();
		
		final RealtimeStreamSync syncer = new RealtimeStreamSync(streamSet);
		syncer.addEventListener(new SyncEventListener() {
			@Override
			public void onSyncEvent(final List<Double> data) {
				data.forEach(d -> System.out.printf("%.3f\n", d));
				System.out.println("---------------------------------");
			}
		});

		new Thread(new Runnable() {
			private Scanner input = new Scanner(System.in);
			@Override
			public void run() {
				while(!input.nextLine().trim().equalsIgnoreCase("stop"));
				streamSet.stop();
			}
		}).start();
		syncer.run();
	}
}