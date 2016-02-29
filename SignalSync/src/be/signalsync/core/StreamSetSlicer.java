package be.signalsync.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.signalsync.streamsets.StreamSet;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;

/**
 * This class is a Slicer which is used to take slices of different streams
 * wrapped in a StreamSuppy.
 *
 * @author Ward Van Assche
 *
 */
public class StreamSetSlicer extends Slicer<List<float[]>> implements SliceListener<float[]> {
	private static Logger Log = Logger.getLogger(Config.get(Key.APPLICATION_NAME));
	private final StreamSet streamSet;
	private int size;
	private final Map<Slicer<float[]>, BlockingQueue<float[]>> slicesMap;
	private final ExecutorService collectExecutor;

	/**
	 * Creates a new StreamSetSlicer from a StreamSet.
	 *
	 * @param supply
	 */
	public StreamSetSlicer(final StreamSet streamSet, final long interval) {
		super();
		this.streamSet = streamSet;
		this.size = streamSet.getStreams().size();
		this.slicesMap = Collections.synchronizedMap(new LinkedHashMap<>(size));
		this.collectExecutor = Executors.newSingleThreadExecutor();
		for (final AudioDispatcher d : this.streamSet.getStreams()) {
			final StreamSlicer slicer = new StreamSlicer(interval, this);
			d.addAudioProcessor(slicer);
			slicesMap.put(slicer, new ArrayBlockingQueue<>(5));
		}
		collectSliceSets();
	}

	private void collectSliceSets() {
		collectExecutor.execute(new Runnable() {
			private final List<float[]> slices = new ArrayList<>();

			@Override
			public void run() {
				try {
					while (size > 0) {
						for (final BlockingQueue<float[]> q : slicesMap.values()) {
							synchronized(q) {
								final float[] d = q.take();
								if(q.isEmpty()){
									q.notify();
								}
								slices.add(d);
							}
						}
						emitSliceEvent(new ArrayList<float[]>(slices));
						slices.clear();
					}
				} catch (final InterruptedException e) {
					Log.log(Level.SEVERE, "InterruptedExeception thrown in StreamSetSlicer", e);
				}
			}
		});
	}

	@Override
	public void done(final Slicer<float[]> slicer) {
		try {
			BlockingQueue<float[]> q = slicesMap.get(slicer);
			synchronized(q) {
				while(!q.isEmpty()) {
					q.wait();
				}
			}
			slicesMap.remove(slicer);
			size--;
			if (size == 0) {
				collectExecutor.shutdown();
				final boolean shuttedDown = collectExecutor.awaitTermination(5, TimeUnit.SECONDS);
				if (!shuttedDown) {
					Log.log(Level.SEVERE, "Collector thread wasn't stopped properly. There is a problem somewhere.");
				}
				emitDoneEvent();
			}
		} 
		catch (InterruptedException e) {
			Log.log(Level.SEVERE, "InterruptedExeception thrown in StreamSetSlicer", e);
		}
	}

	@Override
	public void onSliceEvent(final float[] slice, final Slicer<float[]> slicer) {
		try {
			slicesMap.get(slicer).put(slice);
		}
		catch (InterruptedException e) {
			Log.log(Level.SEVERE, "InterruptedExeception thrown in StreamSetSlicer", e);
		}
	}
}
