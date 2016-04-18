package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;

/**
 * 
 * @author Ward Van Assche
 *
 */
public class StreamGroup {
	private AudioDispatcher audioStream;
	private List<AudioDispatcher> dataStreams;
	private String description;
	
	public StreamGroup() {
		this.dataStreams = new ArrayList<>();
	}
	
	public void setAudioStream(AudioDispatcher audioStream) {
		this.audioStream = audioStream;
	}
	
	public void addDataStream(AudioDispatcher dataStream) {
		dataStreams.add(dataStream);
	}

	public AudioDispatcher getAudioStream() {
		return audioStream;
	}

	public List<AudioDispatcher> getDataStreams() {
		return dataStreams;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public void stop() {
		if(audioStream != null) {
			audioStream.stop();
		}
		for(AudioDispatcher d : dataStreams) {
			d.stop();
		}
	}
}
