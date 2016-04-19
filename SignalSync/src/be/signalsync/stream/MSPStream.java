package be.signalsync.stream;

import java.util.ArrayList;
import java.util.List;
import com.cycling74.msp.MSPSignal;
import be.signalsync.slicer.StreamSlicer;

public class MSPStream implements Stream {
	private List<StreamProcessor> processors;
	private double sampleRate;
	private long sampleCtr;
	
	public MSPStream(double sampleRate) {
		this.sampleRate = sampleRate;
		this.processors = new ArrayList<>();
		this.sampleCtr = 0;
	}
	
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
		//TODO: slicer constructie nog eens bekijken
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