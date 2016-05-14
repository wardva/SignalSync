package be.signalsync.stream;

import be.signalsync.slicer.Sliceable;
import be.signalsync.slicer.StreamSlicer;

/**
 * An interface for any kind of stream which can be processed.
 * @author Ward Van Assche
 */
public interface Stream extends Sliceable<float[], StreamSlicer> {
	void addStreamProcessor(StreamProcessor s);
	void removeStreamProcessor(StreamProcessor s);
	double getSampleRate();
	StreamSlicer createSlicer(int sliceSize, int sliceStep);
	void start();
	void stop();
}
