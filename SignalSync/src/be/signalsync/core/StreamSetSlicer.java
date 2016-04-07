package be.signalsync.core;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

import be.signalsync.streamsets.StreamSet;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;

/**
 * This class is a Slicer which is used to take slices of different streams
 * wrapped in a StreamSet.
 * 
 * @author Ward Van Assche
 */
public class StreamSetSlicer extends Slicer<List<float[]>> implements SliceListener<float[]> {
	private static Logger Log = Logger.getLogger(Config.get(Key.APPLICATION_NAME));
	private final StreamSet streamSet;
	
	private final ExecutorService collectExecutor;
	
	/**
	 * This map contains each used slicer, together with a BlockingQueue.
	 * The blockingQueue will contain the slices received from the slicer.
	 */
	private Map<Slicer<float[]>, BlockingQueue<float[]>> slicesMap;
	private List<Slicer<float[]>> slicers;
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
		this.slicesMap = new ConcurrentHashMap<>(streamSet.getStreams().size());
		this.slicers = new ArrayList<>(streamSet.getStreams().size());
		this.timingInfo = new LinkedList<>();
		this.timingLock = new ReentrantLock();
		
		//Iterate over the streams.
		for (final AudioDispatcher d : this.streamSet.getStreams()) {
			//Create and attach a streamSlicer to the current stream.
			final StreamSlicer slicer = new StreamSlicer();
			slicer.addEventListener(this);
			d.addAudioProcessor(slicer);
			//Put the streamslicer into the slicesmap, together with a blockingqueue 
			//which will contain the slices received from this slicer.
			
			slicesMap.put(slicer, new LinkedBlockingQueue<>());
			slicers.add(slicer);
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
			//the list of collected slices.
			private final List<float[]> slices = new ArrayList<>();

			@Override
			public void run() {
				try {
					//Keep doing this while there are streamsets active.
					while (!slicesMap.isEmpty()) {
						for(Slicer<float[]> s : slicers) {
							if(slicesMap.containsKey(s)) {
								BlockingQueue<float[]> q = slicesMap.get(s);
								synchronized (q) {
									float[] d = q.take();
									if(q.isEmpty()){
										q.notify();
									}
									slices.add(d);
								}
							}
						}

						//Finished one iteration, now we have a buffer from each slicer
						//and we can emit a slice event containing the slices.
						timingLock.lock();
						double sliceBeginTime = timingInfo.removeLast();
						timingLock.unlock();
						emitSliceEvent(new ArrayList<float[]>(slices), sliceBeginTime);
						//Clear the slicesList and start a new iteration.
						slices.clear();
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
			BlockingQueue<float[]> q = slicesMap.get(slicer);
			synchronized (q) {
				//While there are still slices in the queue, we have to wait before
				//removing the queue from the slicesMap.
				while(!q.isEmpty()) {
					q.wait();
				}
			}
			slicesMap.remove(slicer);
			//If the size is 0, the slicers are finished and we can shutdown the collectExecutor.
			if (slicesMap.isEmpty()) {
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
			slicesMap.get(event.getSlicer()).put(event.getSlices());

			timingLock.lock();
			double delta = 0.01;
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
