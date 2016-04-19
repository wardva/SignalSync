package be.signalsync.max;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import com.cycling74.msp.MSPPerformer;
import com.cycling74.msp.MSPSignal;

import be.signalsync.slicer.StreamSlicer;
import be.signalsync.stream.AudioDispatcherStream;
import be.signalsync.stream.Stream;
import be.signalsync.stream.StreamEvent;
import be.signalsync.stream.StreamGroup;
import be.signalsync.stream.StreamProcessor;
import be.signalsync.sync.RealtimeSignalSync;
import be.signalsync.sync.SyncEventListener;
import be.signalsync.syncstrategy.StreamSet;
import be.signalsync.util.EventsToAudioDispatcher;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;

public class Sync extends MSPPerformer implements SyncEventListener {
	private final static Pattern streamConfigRegex = Pattern.compile("d*ad*(,d*ad*)*");
	private double sampleRate;
	private List<MaxStream> streams;
	private List<StreamGroup> streamGroups;
	private StreamSet streamSet;
	private String[] streamConfig;
	private int numberOfStreams;
	
	private RealtimeSignalSync syncer;
	private ExecutorService syncExecutor;
	
	public Sync() {
		bail("(Sync) Invalid method parameters.");
	}
	
	/*
	 * Streamconfiguration
	 * -------------------
	 * Streamgroups: 	comma seperated string.
	 * String: 			a=audio stream used for synchronisation.
	 *         			d=data stream synchronized with the audio stream.
	 * Preview: 		addddd,add,addd
	 */
	public Sync(String configString) {
		if(!streamConfigRegex.matcher(configString).matches()) {
			bail("Invalid stream configuration.");
			return;
		}
		this.streamConfig = configString.split(",");
		this.numberOfStreams = 0;
		for(String s : streamConfig) {
			StreamGroup group = new StreamGroup();
			for(char c : s.toCharArray()) {
				MaxStream stream = new MaxStream();
				switch(c) {
				case 'a':
					group.setAudioStream(stream);
					break;
				case 'd':
					group.addDataStream(stream);
					break;
				}
				streams.add(stream);
			}
			streamGroups.add(group);
		}
		streamSet = new StreamSet(streamGroups);
		setInlets();
		setOutlets();
		setAssists();
	}
	
	@Override
	protected void dspstate(boolean running) {
		//TODO
	}
	
	private void setInlets() {
		int[] inlets = new int[numberOfStreams];
		Arrays.fill(inlets, SIGNAL);
		declareInlets(inlets);
	}

	private void setOutlets() {
		int[] outlets = new int[numberOfStreams];
		Arrays.fill(outlets, SIGNAL);
		declareOutlets(outlets);
	}
	
	private void setAssists() {
		String[] inletAssists = new String[numberOfStreams];
		String[] outletAssists = new String[numberOfStreams];
		int k = 0;
		for(int i = 0; i<streamConfig.length; i++) {
			String group = streamConfig[i];
			for(int j = 0; j<group.length(); j++) {
				char c = group.charAt(j);
				if(c == 'a') {
					inletAssists[k] = String.format("Stream %d (audio) of streamgroup %d used for synchronization.", j+1, i+1);
					outletAssists[k] = String.format("Synchronized stream (audio) %d of streamgroup %d.", j+1, i+1);
				}
				else if(c == 'd') {
					inletAssists[k] = String.format("Stream %d (data) of streamgroup %d.", j+1, i+1);
					outletAssists[k] = String.format("Synchronized stream (data) %d of streamgroup %d.", j+1, i+1);
				}
				++k;
			}
			
		}
		setInletAssist(inletAssists);
		setOutletAssist(outletAssists);
	}
		
	@Override
	public void perform(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		for(int i = 0; i<numberOfStreams; i++) {
			MaxStream stream = streams.get(i);
			stream.maxPerformed(sigs_in[i]);
		}
	}
	
	@Override
	public void dspsetup(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		sampleRate = sigs_in[0].sr;
	}
	
	@Override
	public void onSyncEvent(Map<StreamGroup, Double> data) {
		for(Entry<StreamGroup, Double> entry : data.entrySet()) {
			post("Latency: " + entry.getValue());
		}
		post("------------");
	}
	
	@Override
	protected void notifyDeleted() {
		syncExecutor.shutdown();
	}
	
	private class MaxStream implements Stream {
		private List<StreamProcessor> processors;
		
		public MaxStream() {
			processors = new ArrayList<>();
		}
		
		public void maxPerformed(MSPSignal s) {
			for(StreamProcessor p : processors) {
				//TODO timestamp
				StreamEvent event = new StreamEvent(s.vec, 0);
				p.process(event);
			}
		}
		
		@Override
		public void addStreamProcessor(StreamProcessor s) {
			processors.add(s);
		}

		@Override
		public double getSampleRate() {
			return Sync.this.sampleRate;
		}

		@Override
		public StreamSlicer getSlicer(int sliceSize, int sliceStep) {
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
}
