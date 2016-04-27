package be.signalsync.msp;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import com.cycling74.msp.MSPPerformer;
import com.cycling74.msp.MSPSignal;
import be.signalsync.stream.MSPStream;
import be.signalsync.stream.StreamGroup;
import be.signalsync.stream.StreamSet;
import be.signalsync.sync.RealtimeSignalSync;
import be.signalsync.sync.SyncEventListener;

public class Sync extends MSPPerformer implements SyncEventListener {
	private final static Pattern streamConfigRegex = Pattern.compile("d*ad*(,d*ad*)*");
	private double sampleRate;
	private MSPStream[] streams;
	private StreamGroup[] streamGroups;
	private StreamSet streamSet;
	private String[] streamConfig;
	private int numberOfStreams;
	
	private RealtimeSignalSync syncer;
	
	public Sync() {
		bail("(Sync) Invalid method parameters.");
	}
	
	/**
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
		for(String s : streamConfig) {
			numberOfStreams += s.length();
		}
		this.streamGroups = new StreamGroup[streamConfig.length];
		this.streams = new MSPStream[numberOfStreams];
		setInlets();
		setOutlets();
		setAssists();
	}
	
	@Override
	protected void dspstate(boolean running) {
		//Change state
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
		for(int i = 0; i<streamConfig.length; ++i) {
			String group = streamConfig[i];
			for(int j = 0; j<group.length(); ++j) {
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
		for(int i = 0; i<numberOfStreams; ++i) {
			MSPSignal input = sigs_in[i];
			MSPSignal output = sigs_out[i];
			streams[i].maxPerformed(input);
			//input -> output without modifications or synchronization.
			System.arraycopy(input.vec, 0, output.vec, 0, input.n);
		}
	}
	
	@Override
	public void dspsetup(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		sampleRate = sigs_in[0].sr;
		int ctr = 0;
		for(int i = 0; i<streamConfig.length; ++i) {
			String s = streamConfig[i];
			StreamGroup group = new StreamGroup();
			for(int j = 0; j<s.length(); j++) {
				char c = s.charAt(j);
				MSPStream stream = new MSPStream(sampleRate);
				switch(c) {
				case 'a':
					group.setAudioStream(stream);
					break;
				case 'd':
					group.addDataStream(stream);
					break;
				}
				streams[ctr++] = stream;
			}
			streamGroups[i] = group;
		}
		streamSet = new StreamSet(Arrays.asList(streamGroups));
		syncer = new RealtimeSignalSync(streamSet);
		syncer.addEventListener(this);
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
		//Cleanup
	}
}
