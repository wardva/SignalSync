package be.signalsync.core;

import java.io.InputStream;
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
	private List<InputStream> dataStreams;
	private String description;
	
	public StreamGroup() {
		this.dataStreams = new ArrayList<>();
	}
	
	public void setAudioStream(AudioDispatcher audioStream) {
		this.audioStream = audioStream;
	}
	
	public void addDataStream(InputStream dataStream) {
		dataStreams.add(dataStream);
	}

	public AudioDispatcher getAudioStream() {
		return audioStream;
	}

	public List<InputStream> getDataStreams() {
		return dataStreams;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
