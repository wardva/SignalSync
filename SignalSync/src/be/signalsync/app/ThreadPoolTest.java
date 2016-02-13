package be.signalsync.app;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ThreadPoolTest  {
	private ScheduledExecutorService executor;
	private Runnable task;
	
	
	public ThreadPoolTest(long firstLatency, long delay) {
		executor = Executors.newScheduledThreadPool(1);
		
		task = new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
		};
	}
}
