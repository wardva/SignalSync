package be.signalsync.stream;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents 1 audiostream used for synchronisation
 * and any number of datastreams with the same latency.
 * @author Ward Van Assche
 */
public class StreamGroup {
	private Stream audioStream;
	private List<Stream> dataStreams;
	private String description;
	
	public StreamGroup() {
		this.dataStreams = new ArrayList<>();
	}
	
	public void setAudioStream(Stream audioStream) {
		this.audioStream = audioStream;
	}
	
	public void addDataStream(Stream dataStream) {
		dataStreams.add(dataStream);
	}

	public Stream getAudioStream() {
		return audioStream;
	}

	public List<Stream> getDataStreams() {
		return dataStreams;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public void start() {
		audioStream.start();
		for(Stream s : dataStreams) {
			s.start();
		}
	}
	
	public void stop() {
		audioStream.stop();
		for(Stream s : dataStreams) {
			s.stop();
		}
	}
}
