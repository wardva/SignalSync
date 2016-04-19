package be.signalsync.slicer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import be.signalsync.stream.StreamEvent;
import be.signalsync.stream.StreamProcessor;
import be.signalsync.util.Config;
import be.signalsync.util.Key;

public class StreamSlicer extends Slicer<float[]> implements StreamProcessor {
	private int sampleCtr;
	private List<List<float[]>> buffer;
	private List<float[]> currentSecBuffer;
	private int currentSec;
	private final int sliceSize;
	private final int sliceStep;
	private final double sampleRate;
	
	public StreamSlicer(int sliceSize, int sliceStep, double sampleRate) {
		this.sliceSize = sliceSize;
		this.sliceStep = sliceStep;
		this.sampleRate = sampleRate;
		this.buffer = new LinkedList<>();
		this.sampleCtr = 0;
		this.currentSec = -1;
	}
	public StreamSlicer() {
		this(Config.getInt(Key.SLICE_SIZE_S), 
			 Config.getInt(Key.SLICE_STEP_S), 
			 Config.getInt(Key.SAMPLE_RATE));
	}
	
	
	@Override
	public void process(StreamEvent streamEvent) {
		int second = (int) streamEvent.getTimeStamp();
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
		final float[] eventBuffer = streamEvent.getFloatBuffer();
		float[] eventBufferCopy = new float[eventBuffer.length];
		System.arraycopy(eventBuffer, 0, eventBufferCopy, 0, eventBuffer.length);
		currentSecBuffer.add(eventBufferCopy);
		sampleCtr += streamEvent.getBufferSize();
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
