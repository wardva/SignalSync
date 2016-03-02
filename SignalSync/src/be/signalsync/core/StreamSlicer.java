package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

/**
 * This class is a Slicer which is used to slice a single stream.
 *
 * @author Ward Van Assche
 */
public class StreamSlicer extends Slicer<float[]> implements AudioProcessor {
	/**
	 * A list of float buffers, used to buffer the newly received data from the
	 * stream. The list is cleared each slice operation.
	 */
	private final List<float[]> floatBuffers;

	/**
	 * The timestamp in seconds when the previous slice occured.
	 */
	private double previousSliceTimestamp;

	/**
	 * The interval between each slice.
	 */
	private final double interval;

	public StreamSlicer(final double interval, final SliceListener<float[]> listener) {
		super();
		floatBuffers = new ArrayList<>();
		previousSliceTimestamp = 0;
		this.interval = interval;
		addEventListener(listener);
	}

	@Override
	public boolean process(final AudioEvent audioEvent) {
		final float[] current = audioEvent.getFloatBuffer();
		final float[] bufCopy = new float[current.length];
		System.arraycopy(current, 0, bufCopy, 0, current.length);
		floatBuffers.add(bufCopy);
		if (audioEvent.getTimeStamp() - previousSliceTimestamp > interval) {
			previousSliceTimestamp = audioEvent.getTimeStamp();
			slice();
		}
		return true;
	}

	@Override
	public void processingFinished() {
		emitDoneEvent();
	}

	private void slice() {
		final float totalBuf[] = new float[floatBuffers.size() * floatBuffers.get(0).length];
		int i = 0;
		for (final float[] buf : floatBuffers) {
			System.arraycopy(buf, 0, totalBuf, i, buf.length);
			i += buf.length;
		}
		floatBuffers.clear();
		emitSliceEvent(totalBuf);
	}
}
