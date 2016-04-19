package be.signalsync.stream;

import be.signalsync.slicer.StreamSlicer;

public interface Stream {
	void addStreamProcessor(StreamProcessor s);
	double getSampleRate();
	StreamSlicer getSlicer(int sliceSize, int sliceStep);
	void start();
	void stop();
}
