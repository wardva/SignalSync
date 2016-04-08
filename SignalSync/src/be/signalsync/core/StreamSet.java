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
	
	/*protected List<AudioDispatcher> streams;
	protected ExecutorService streamExecutor;
	private ExecutorService stopExecutor;

	public StreamSet() {
		streams = new ArrayList<>();
		stopExecutor = Executors.newCachedThreadPool();
	}

	protected ExecutorService getStreamExecutor() {
		return streamExecutor;
	}

	public List<AudioDispatcher> getStreams() {
		return streams;
	}
	
	public int size() {
		return streams.size();
	}
	
	public void stop() {
		for(AudioDispatcher d : streams) {
			if(!d.isStopped()) {
				stopExecutor.execute(new Runnable() {
					@Override
					public void run() {
						d.stop();
					}
				});
			}
		}
		stopExecutor.shutdown();
	}


	/**
	 * Execute the streams (reference and other streams).
	 */
	/*@Override
	public void run() {
		for (final AudioDispatcher d : streams) {
			streamExecutor.execute(d);
		}
		streamExecutor.shutdown();
	}*/
}
