package be.signalsync.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.signalsync.slicer.Sliceable;
import be.signalsync.slicer.StreamSetSlicer;

/**
 * This class contains any number of streamGroups and some methods.
 * @author Ward Van Assche
 */
public class StreamSet implements Sliceable<Map<StreamGroup, float[]>, StreamSetSlicer> {
	
	private List<StreamGroup> streamGroups;
	
	public StreamSet() {
		this.streamGroups = new ArrayList<>();
	}
	
	public StreamSet(List<StreamGroup> streamGroups) {
		this.streamGroups = streamGroups;
	}
	
	public List<StreamGroup> getStreamGroups() {
		return this.streamGroups;
	}
	
	/**
	 * @return The audiostreams of the streamGroups.
	 */
	public List<Stream> getAudioStreams() {
		List<Stream> audioStreams = new ArrayList<>();
		for(StreamGroup g : streamGroups) {
			audioStreams.add(g.getAudioStream());
		}
		return audioStreams;
	}
	
	public void addStreamGroup(StreamGroup group) {
		streamGroups.add(group);
	}
	
	public int size() {
		return streamGroups.size();
	}
	
	/**
	 * Start running the streamGroups.
	 */
	public void start() {
		for(StreamGroup group : streamGroups) {
			group.start();
		}
	}
	
	/**
	 * Stop running the streamGroups.
	 */
	public void stop() {
		for(StreamGroup group : streamGroups) {
			group.stop();
		}
	}
	
	/**
	 * Create a streamSetSlicer from this streamSet.
	 */
	public StreamSetSlicer createSlicer(int sliceSize, int sliceStep) {
		return new StreamSetSlicer(this, sliceSize, sliceStep);
	}
}
