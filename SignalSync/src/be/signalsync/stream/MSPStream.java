package be.signalsync.stream;

import java.util.ArrayList;
import java.util.List;
import com.cycling74.msp.MSPSignal;
import be.signalsync.slicer.StreamSlicer;

/**
 * This class makes it possible to use a sequence of MSPSignal object
 * as a Stream. This only works if the maxPerformed method is called
 * correctly. 
 * @author Ward Van Assche
 */
public class MSPStream implements Stream {
	private List<StreamProcessor> processors;
	private double sampleRate;
	private long sampleCtr;
	
	public MSPStream(double sampleRate) {
		this.sampleRate = sampleRate;
		this.processors = new ArrayList<>();
		this.sampleCtr = 0;
	}
	
	/**
	 * This method should be called for each MSPSignal object
	 * received from Max/MSP.
	 */
	public void maxPerformed(MSPSignal s) {
		sampleCtr += s.n;
		double timestamp = (double) sampleCtr / sampleRate;
		for(StreamProcessor p : processors) {
			StreamEvent event = new StreamEvent(s.vec, timestamp);
			p.process(event);
		}
	}
	
	@Override
	public void addStreamProcessor(StreamProcessor s) {
		processors.add(s);
	}
	
	@Override
	public void removeStreamProcessor(StreamProcessor s) {
		processors.remove(s);
	}

	@Override
	public double getSampleRate() {
		return sampleRate;
	}

	@Override
	public StreamSlicer createSlicer(int sliceSize, int sliceStep) {
		StreamSlicer slicer = new StreamSlicer(sliceSize, sliceStep, getSampleRate());
		addStreamProcessor(slicer);
		return slicer;
	}

	@Override
	public void start() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void stop() {
		throw new UnsupportedOperationException();
	}
}