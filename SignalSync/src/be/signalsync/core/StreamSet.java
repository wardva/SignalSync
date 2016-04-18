package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;

public class StreamSet {
	
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
	
	public List<AudioDispatcher> getAudioStreams() {
		List<AudioDispatcher> audioStreams = new ArrayList<>();
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
	
	public void stop() {
		for(StreamGroup group : streamGroups) {
			group.stop();
		}
	}
}
