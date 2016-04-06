package be.signalsync.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

/**
 * This slicer can slice a stream using a sliceSize and a sliceStep.
 * The sliceSize is the size of the windows, the sliceStep is the size of
 * each windows shift. The slice size and stepsize are given in seconds.
 * @author Ward Van Assche
 *
 */
public class StreamSlicer extends Slicer<float[]> implements AudioProcessor {
	
	private final int sliceSize;
	private final int sliceStep;
	private final int sampleRate;
	private int sampleCtr;
	
	private List<List<float[]>> buffer;
	private List<float[]> currentSecBuffer;
	private int currentSec;
	
	public StreamSlicer(int sliceSize, int sliceStep, int sampleRate) {
		this.sliceSize = sliceSize;
		this.sliceStep = sliceStep;
		this.sampleRate = sampleRate;
		this.buffer = new LinkedList<>();
		this.sampleCtr = 0;
		this.currentSec = -1;
	}
	
	
	public StreamSlicer(int sliceSize, int sliceStep) {
		this(sliceSize, sliceStep, Config.getInt(Key.SAMPLE_RATE));
	}
	
	public StreamSlicer() {
		this(Config.getInt(Key.SLICE_SIZE_S), Config.getInt(Key.SLICE_STEP_S), Config.getInt(Key.SAMPLE_RATE));
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		int second = (int) audioEvent.getTimeStamp();
		if(second - currentSec > 0) {
			currentSec = second;
			if(Math.abs(currentSec-sliceSize) % sliceStep == 0 && currentSec >= sliceSize) {
				List<List<float[]>> sliceBuffers = buffer.subList(buffer.size() - sliceSize, buffer.size());
				float[] merged = flatten(sliceBuffers);
				double beginSliceTime = ((double) (sampleCtr - merged.length)) / sampleRate;
				emitSliceEvent(merged, beginSliceTime);
				buffer.subList(0, sliceStep).clear();
			}
			currentSecBuffer = new ArrayList<>();
			buffer.add(currentSecBuffer);
		}
		final float[] eventBuffer = audioEvent.getFloatBuffer();
		float[] eventBufferCopy = new float[eventBuffer.length];
		System.arraycopy(eventBuffer, 0, eventBufferCopy, 0, eventBuffer.length);
		currentSecBuffer.add(eventBufferCopy);
		sampleCtr += audioEvent.getBufferSize();
		return true;
	}

	@Override
	public void processingFinished() {
		emitDoneEvent();
	}
	
	public float[] flatten(List<List<float[]>> lists) {
	    int size = 0;
	    for (int i = 0; i < lists.size(); i++) {
	        List<float[]> list = lists.get(i);
	        for (int j = 0; j < list.size(); j++) {
	            float[] floats = list.get(j);
	            size += floats.length;
	        }
	    }
	    float[] ret = new float[size];
	    int pos = 0;
	    for (int i = 0; i < lists.size(); i++) {
	        List<float[]> list = lists.get(i);
	        for (int j = 0; j < list.size(); j++) {
	            float[] floats = list.get(j);
	            System.arraycopy(floats, 0, ret, pos, floats.length);
	            pos += floats.length;
	        }
	    }
	    return ret;
	}
}
