package be.signalsync.util;

import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class FloatBufferGenerator implements AudioProcessor {
	private List<float[]> buffers;
	private float[] totalBuf;
	private boolean finished = false;
	
	public FloatBufferGenerator() {
		buffers = new ArrayList<>();
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		final float[] current = audioEvent.getFloatBuffer();
		final float[] bufCopy = new float[current.length];
		System.arraycopy(current, 0, bufCopy, 0, current.length);
		buffers.add(bufCopy);
		return true;
	}

	public float[] getTotalBuffer() {
		if(finished) {
			return totalBuf;
		}
		throw new RuntimeException("The audioProcessor is not finished yet!");
	}

	@Override
	public void processingFinished() {
		finished = true;
		if(buffers.size() > 0) {
			totalBuf = new float[buffers.size() * buffers.get(0).length];
			int i = 0;
			for (final float[] buf : buffers) {
				System.arraycopy(buf, 0, totalBuf, i, buf.length);
				i += buf.length;
			}
		}
		else {
			totalBuf = new float[0];
		}
	}
}
