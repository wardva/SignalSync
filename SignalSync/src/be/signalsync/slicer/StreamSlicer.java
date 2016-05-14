package be.signalsync.slicer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import be.signalsync.stream.StreamEvent;
import be.signalsync.stream.StreamProcessor;
import be.signalsync.util.Config;
import be.signalsync.util.Key;

/**
 * This class is a concrete Slicer which can create float[] slices of a Stream.
 * To receive data from the Stream the slicer object should be registered as
 * StreamProcessor of the Stream.
 * @author Ward Van Assche
 *
 */
public class StreamSlicer extends Slicer<float[]> implements StreamProcessor {
	//The sample counter, used for calculating the time
	private int sampleCtr;
	//A list containing the buffers, the inner List contains all the received float arrays
	//from 1 sec audio. The outer List contains all the buffers containing 1 sec buffers.
	private List<List<float[]>> buffer;
	//The buffer of the current second
	private List<float[]> currentSecBuffer;
	//The current second
	private int currentSec;
	//The size of each slice in seconds
	private final int sliceSize;
	//The size of each slice step in seconds
	private final int sliceStep;
	//The samplerate
	private final double sampleRate;
	
	public StreamSlicer(int sliceSize, int sliceStep, double sampleRate) {
		if(sliceStep <= 0 || sliceSize <= 0) {
			throw new IllegalArgumentException("SliceStep and sliceSize have to be greater than zero.");
		}
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
	
	/**
	 * This method will be called each time there's some streaming data 
	 * available. The data will be added to the currentSecBuffer (the buffer
	 * containing the data of current second). If the timestamp indicates a new
	 * second a test will be performed to check if the slice is finished.
	 * When the slice is finished, a sliceEvent will be emitted with the merged
	 * samples.
	 */
	@Override
	public void process(StreamEvent streamEvent) {
		int second = (int) streamEvent.getTimeStamp();
		//Check if it's a new second
		if(second - currentSec > 0) {
			//If so, change the current second
			currentSec = second;
			//Check if a new slice is available
			if(Math.abs(currentSec-sliceSize) % sliceStep == 0 && currentSec >= sliceSize) {
				//Get a sublist containing the last sliceSize seconds.
				List<List<float[]>> sliceBuffers = buffer.subList(buffer.size() - sliceSize, buffer.size());
				//Flatten the sublist
				float[] merged = flatten(sliceBuffers);
				//Calculate the time of the beginning of the slice
				double beginSliceTime = ((double) (sampleCtr - merged.length)) / sampleRate;
				//Emit a slice event
				emitSliceEvent(merged, beginSliceTime);
				//Clear the buffer parts we won't need anymore for upcomming slices
				buffer.subList(0, sliceStep).clear();
			}
			//Create a new currentSecBuffer...
			currentSecBuffer = new ArrayList<>();
			//...and it to the buffer list
			buffer.add(currentSecBuffer);
		}
		//When it's not a new second, get the buffer from the event, copy it,
		//and add it to the current second buffer.
		final float[] eventBuffer = streamEvent.getFloatBuffer();
		float[] eventBufferCopy = new float[eventBuffer.length];
		System.arraycopy(eventBuffer, 0, eventBufferCopy, 0, eventBuffer.length);
		currentSecBuffer.add(eventBufferCopy);
		//Increment the samplecounter
		sampleCtr += streamEvent.getBufferSize();
	}

	@Override
	public void processingFinished() {
		emitDoneEvent();
	}
	
	/**
	 * This method converts the buffer structure used in this class
	 * to a float array.
	 */
	private float[] flatten(List<List<float[]>> lists) {
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
