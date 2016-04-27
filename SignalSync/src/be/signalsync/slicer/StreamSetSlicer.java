package be.signalsync.slicer;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.signalsync.stream.Stream;
import be.signalsync.stream.StreamGroup;
import be.signalsync.stream.StreamSet;
import be.signalsync.util.Config;
import be.signalsync.util.Key;

/**
 * This class is a Slicer which is used to take slices of different streams
 * wrapped in a StreamSet.
 * 
 * @author Ward Van Assche
 */
public class StreamSetSlicer extends Slicer<Map<StreamGroup, float[]>> implements SliceListener<float[]> {
	private static Logger Log = Logger.getLogger(Config.get(Key.APPLICATION_NAME));
	private final StreamSet streamSet;
	
	private final ExecutorService collectExecutor;
	
	/**
	 * This map contains each used slicer, together with a BlockingQueue.
	 * The blockingQueue will contain the slices received from the slicer.
	 */
	private Map<Slicer<float[]>, BlockingQueue<float[]>> sliceBuffers;
	private Map<Slicer<float[]>, StreamGroup> slicers;
	private Deque<Double> timingInfo;
	private Lock timingLock;
	
	/**
	 * Creates a new StreamSetSlicer from a StreamSet.
	 * @param streamSet The streamset to extract the slices from.
	 * @param time The time in seconds of each slice.
	 * @param step The step size between each slice in seconds.
	 */
	public StreamSetSlicer(final StreamSet streamSet, final int sliceSize, final int sliceStep) {
		super();
		this.streamSet = streamSet;
		this.collectExecutor = Executors.newSingleThreadExecutor();
		this.sliceBuffers = new ConcurrentHashMap<>(streamSet.size());
		this.slicers = new LinkedHashMap<>(streamSet.size());
		this.timingInfo = new LinkedList<>();
		this.timingLock = new ReentrantLock();
		
		//Iterate over the streams.
		for (StreamGroup group : this.streamSet.getStreamGroups()) {
			Stream audioStream = group.getAudioStream();
			final StreamSlicer slicer = audioStream.createSlicer(sliceSize, sliceStep);
			slicer.addEventListener(this);
			sliceBuffers.put(slicer, new LinkedBlockingQueue<>());
			slicers.put(slicer, group);
		}
		collectSliceSets();
	}

	/**
	 * This method iterates over the SlicesMap and tries to take 1 slice
	 * of each BlockingQueue each iteration. When each iteration is finished
	 * the list of slices (containing 1 slice of each slicer) will be copied
	 * and a sliceEvent will be emitted with the different slices. After this
	 * the slices list will be cleared and a new iteration will start collecting
	 * the next slice of each slicer.
	 * 
	 * This method runs in another thread.
	 */
	private void collectSliceSets() {
		collectExecutor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					//Keep doing this while there are streamsets active.
					while (!sliceBuffers.isEmpty()) {
						Map<StreamGroup, float[]> slices = new HashMap<>();
						for(Entry<Slicer<float[]>, StreamGroup> entry : slicers.entrySet()) {
							Slicer<float[]> slicer = entry.getKey();
							StreamGroup streamGroup = entry.getValue();
							if(sliceBuffers.containsKey(slicer)) {
								BlockingQueue<float[]> q = sliceBuffers.get(slicer);
								synchronized (q) {
									float[] buffer = q.take();
									if(q.isEmpty()){
										q.notify();
									}
									slices.put(streamGroup, buffer);
								}
							}
						}

						//Finished one iteration, now we have a buffer from each slicer
						//and we can emit a slice event containing the slices.
						timingLock.lock();
						double sliceBeginTime = timingInfo.removeLast();
						timingLock.unlock();
						emitSliceEvent(slices, sliceBeginTime);
					}
				} 
				catch (final InterruptedException e) {
					Log.log(Level.SEVERE, "InterruptedExeception thrown in StreamSetSlicer", e);
				}
			}
		});
	}

	/**
	 * This method will be called when a streamSlicer has been finished
	 * collecting slices.
	 * 
	 * @param slicer The slicer which has been finished.
	 */
	@Override
	public void done(final Slicer<float[]> slicer) {
		try {
			//Get the blockingqueue from the slicer.
			BlockingQueue<float[]> q = sliceBuffers.get(slicer);
			synchronized (q) {
				//While there are still slices in the queue, we have to wait before
				//removing the queue from the slicesMap.
				while(!q.isEmpty()) {
					q.wait();
				}
			}
			sliceBuffers.remove(slicer);
			
			//If the size is 0, the slicers are finished and we can shutdown the collectExecutor.
			if (sliceBuffers.isEmpty()) {
				collectExecutor.shutdown();
				final boolean shuttedDown = collectExecutor.awaitTermination(5, TimeUnit.SECONDS);
				if (!shuttedDown) {
					Log.log(Level.SEVERE, "Collector thread wasn't stopped properly. There is a problem somewhere.");
				}
				//Emit the done event to the listeners.
				emitDoneEvent();
			}
		} 
		catch (InterruptedException e) {
			Log.log(Level.SEVERE, "InterruptedExeception thrown in StreamSetSlicer", e);
		} 
	}

	/**
	 * This method will be called when we received a slice from a streamSlicer.
	 * @param slice The float buffer containing the slice data.
	 * @param slicer The slicer which has sent the slice event.
	 */
	@Override
	public void onSliceEvent(SliceEvent<float[]> event) {	
		try {
			//Put the slice into the blockingQueue of the slicer in the slicesMap.
			sliceBuffers.get(event.getSlicer()).put(event.getSlices());

			timingLock.lock();
			final double delta = 0.01;
			if(timingInfo.isEmpty() || event.getBeginTime() - timingInfo.peek() > delta) {
				timingInfo.push(event.getBeginTime());
			}
			timingLock.unlock();
		}
		catch (InterruptedException e) {
			Log.log(Level.SEVERE, "InterruptedExeception thrown in StreamSetSlicer", e);
		}
	}
}
