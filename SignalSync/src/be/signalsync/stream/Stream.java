package be.signalsync.stream;

import be.signalsync.slicer.Sliceable;
import be.signalsync.slicer.StreamSlicer;

public interface Stream extends Sliceable<float[], StreamSlicer> {
	void addStreamProcessor(StreamProcessor s);
	double getSampleRate();
	StreamSlicer createSlicer(int sliceSize, int sliceStep);
	void start();
	void stop();
}
