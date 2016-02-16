package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

/**
 * This class is a Slicer which is used to slice a single stream.
 *
 * @author Ward Van Assche
 *
 */
public class StreamSlicer extends Slicer<AudioDispatcher> implements AudioProcessor {
	/**
	 * A list of float buffers, used to buffer the newly received data from the
	 * stream. The list is cleared each slice operation.
	 */
	private List<float[]> floatBuffers;
	
	/**
	 * The timestamp in seconds when the previous slice occured.
	 */
	private double previousSliceTimestamp;
	
	/**
	 * The interval between each slice.
	 */
	private double interval;

	public StreamSlicer(double interval, SliceListener<AudioDispatcher> listener) {
		super();
		this.floatBuffers = new ArrayList<>();
		this.previousSliceTimestamp = 0;
		this.interval = interval;
		this.addEventListener(listener);
	}

	private void slice() {
		try {
			final float totalBuf[] = new float[floatBuffers.size() * floatBuffers.get(0).length];
			int i = 0;
			for (final float[] buf : floatBuffers) {
				System.arraycopy(buf, 0, totalBuf, i, buf.length);
				i += buf.length;
			}
			floatBuffers.clear();
			AudioDispatcher dispatcher = null;
			dispatcher = AudioDispatcherFactory.fromFloatArray(totalBuf, Config.getInt("SAMPLE_RATE"), Config.getInt("BUFFER_SIZE"), Config.getInt("BUFFER_OVERLAP"));
			emitSliceEvent(dispatcher);
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		final float[] current = audioEvent.getFloatBuffer();
		final float[] bufCopy = new float[current.length];
		System.arraycopy(current, 0, bufCopy, 0, current.length);
		floatBuffers.add(bufCopy);
		if((audioEvent.getTimeStamp() - previousSliceTimestamp) > interval) {
			previousSliceTimestamp = audioEvent.getTimeStamp();
			slice();
		}
		return true;
	}

	@Override
	public void processingFinished() {
		emitDoneEvent();
	}
}
