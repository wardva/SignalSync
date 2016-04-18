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

import be.signalsync.core.RealtimeSignalSync;
import be.signalsync.core.StreamGroup;
import be.signalsync.core.StreamSet;
import be.signalsync.core.SyncEventListener;
import be.signalsync.util.EventsToAudioDispatcher;
import be.tarsos.dsp.AudioDispatcher;

public class Sync extends MSPPerformer implements SyncEventListener {
	private final static Pattern streamConfigRegex = Pattern.compile("d*ad*(,d*ad*)*");
	private double sampleRate;
	private int bufferSize;
	private List<StreamGroup> streamGroups;
	private List<AudioDispatcher> dispatchers;
	private List<EventsToAudioDispatcher> converters;
	private String[] streamConfig;
	private int numberOfStreams;
	private StreamSet streamSet;
	private RealtimeSignalSync syncer;
	private ExecutorService syncExecutor;
	private boolean processing;
	
	public Sync() {
		bail("(Sync) Invalid method parameters.");
	}
	
	public Sync(Object test) {
		post("ooh wat gebeurt er hier!");
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
		try {
			if(!streamConfigRegex.matcher(configString).matches()) {
				bail("Invalid stream configuration.");
				return;
			}
			this.streamConfig = configString.split(",");
			this.numberOfStreams = 0;
			for(String s : streamConfig) {
				numberOfStreams += s.length();
			}
			this.streamGroups = new ArrayList<>(numberOfStreams);
			this.converters = new ArrayList<>(numberOfStreams);
			this.dispatchers = new ArrayList<>(numberOfStreams);
			this.syncExecutor = Executors.newFixedThreadPool(numberOfStreams);
			setInlets();
			setOutlets();
			setAssists();
		}
		catch(Exception ex) {
			post(ex.getMessage());
		}
	}
	
	@Override
	protected void dspstate(boolean running) {
		this.processing = running;
		if(running) {
			startSync();
		}
		else {
			stopSync();
		}
	}
	
	private void startSync() {
		for(String group : streamConfig) {
			StreamGroup streamGroup = new StreamGroup();
			streamGroups.add(streamGroup);
			for(char c : group.toCharArray()) {
				EventsToAudioDispatcher converter = new EventsToAudioDispatcher(sampleRate, bufferSize, false);
				AudioDispatcher dispatcher = converter.createAudioDispatcher();
				dispatchers.add(dispatcher);
				converters.add(converter);
				switch(c) {
					case 'a':
						streamGroup.setAudioStream(dispatcher);
						break;
					case 'd':
						streamGroup.addDataStream(dispatcher);
						break;
				}
			}
		}
		this.streamSet = new StreamSet(streamGroups);
		this.syncer = new RealtimeSignalSync(streamSet);
		this.syncer.addEventListener(this);
		for(AudioDispatcher d : dispatchers) {
			syncExecutor.execute(d);
		}
	}
	
	private void stopSync() {
		/*for(AudioDispatcher d : dispatchers) {
			d.stop();
		}*/
		this.streamGroups.clear();
		this.converters.clear();
		this.syncer = null;
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
				switch(c) {
					case 'a':
						inletAssists[k] = String.format("Stream %d (audio) of streamgroup %d used for synchronization.", j+1, i+1);
						outletAssists[k] = String.format("Synchronized stream (audio) %d of streamgroup %d.", j+1, i+1);
						break;
					case 'd':
						inletAssists[k] = String.format("Stream %d (data) of streamgroup %d.", j+1, i+1);
						outletAssists[k] = String.format("Synchronized stream (data) %d of streamgroup %d.", j+1, i+1);
						break;
				}
				k++;
			}
			
		}
		setInletAssist(inletAssists);
		setOutletAssist(outletAssists);
	}
		
	@Override
	public void perform(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		for(int i = 0; i<numberOfStreams; i++) {
			float[] buffer = sigs_in[i].vec;
			converters.get(i).audioEvent(buffer);
		}
	}
	
	@Override
	public void dspsetup(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		sampleRate = sigs_in[0].sr;
		bufferSize = sigs_in[0].n;
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
		if(processing) {
			stopSync();
		}
		syncExecutor.shutdown();
	}
}
