package be.signalsync.msp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import com.cycling74.msp.MSPPerformer;
import com.cycling74.msp.MSPSignal;

import be.signalsync.stream.MSPStream;
import be.signalsync.stream.StreamGroup;
import be.signalsync.stream.StreamSet;
import be.signalsync.sync.RealtimeSignalSync;
import be.signalsync.sync.SyncEventListener;
import be.signalsync.syncstrategy.LatencyResult;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.signalsync.util.Util;

/**
 * Max/MSP module for synchronizing signals using audio-to-audio alignment.
 * @author Ward Van Assche
 */
public class Sync extends MSPPerformer implements SyncEventListener {
	//Regular expression for validating the configuration string.
	private final static String STREAM_CONFIG_REGEX = "d*ad*(,d*ad*)*";
	//Max/MSP samplerate
	private double sampleRate;
	//Max/MSP bufferSize
	private int bufferSize;
	//Array containing the MSPStream objects. The method maxPerformed has to be called on
	//these objects with an MSPSignal as parameter in order to activate the streams.
	private MSPStream[] streams;
	//Array of the StreamGroup objects. Each StreamGroup contains 1 audioStream and several
	//datastreams who are already synchronized with the audiostream.
	private StreamGroup[] streamGroups;
	//The StreamSet object containing the StreamGroups.
	private StreamSet streamSet;
	//Array of the StreamGroup configuration (splitted from the 
	//comma-delimitted configuration String).
	private String[] streamConfig;
	//The number of streams.
	private int numberOfStreams;
	//The object which actually performs the synchronization.
	private RealtimeSignalSync syncer;
	//A treemap containing the input buffers, key is the timestamp
	private List<LinkedList<Float>> buffers;
	//The list of corrections (in samples) of each stream
	private int[] corrections;
	//The initial latency of each stream is the sliceSize.
	private int sliceSize;
	//Previous latency in samplenumbers
	private Map<StreamGroup, Integer> previousLatencies;
	//Lock for modifying the corrections array (Shared between the onSyncEvent method
	//and the perform method).
	private Lock syncLock;
	
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
		post("Current running algorithm: " + Config.get(Key.LATENCY_ALGORITHM));
		//Check the configuration String.
		if(!Pattern.compile(STREAM_CONFIG_REGEX).matcher(configString).matches()) {
			bail("Invalid stream configuration.");
			return;
		}
		//Split the string into parts.
		this.streamConfig = configString.split(",");
		//Calculate the number of streams.
		for(String s : streamConfig) {
			numberOfStreams += s.length();
		}
		this.syncLock = new ReentrantLock();
		//Create the Streamgroup array.
		this.streamGroups = new StreamGroup[streamConfig.length];
		//Create the Stream array.
		this.streams = new MSPStream[numberOfStreams];
		//Get the slice size
		this.sliceSize = Config.getInt(Key.SLICE_SIZE_S)/2;
		//Initialize the inlets, outlets and assists.
		setInlets();
		setOutlets();
		setAssists();
	}
	
	@Override
	protected void dspstate(boolean running) {
		//State changed
	}
	
	/**
	 * Initialize the inlets.
	 */
	private void setInlets() {
		int[] inlets = new int[numberOfStreams];
		Arrays.fill(inlets, SIGNAL);
		declareInlets(inlets);
	}

	/**
	 * Initialize the outlets.
	 */
	private void setOutlets() {
		int[] outlets = new int[numberOfStreams];
		Arrays.fill(outlets, SIGNAL);
		declareOutlets(outlets);
	}
	
	/**
	 * Initialize the assist messages.
	 */
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

	/**
	 * Called by the Max/MSP context when samples are available.
	 */
	@Override
	public void perform(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		for(int i = 0; i<numberOfStreams; ++i) {
			//Do for each channel:
			MSPSignal input = sigs_in[i];
			MSPSignal output = sigs_out[i];
			LinkedList<Float> buffer = buffers.get(i);
			//Adding the samples to the corresponding buffer
			buffer.addAll(Util.floatArrayToList(input.vec));
			
			int currentCorrection;
			syncLock.lock();
			try {
				//Get the current correction in samples, the max current correction
				//is the number of samples in 1 Max/MSP buffer.
				int correction = corrections[i];
				currentCorrection = correction > bufferSize ? bufferSize : correction;
				corrections[i] -= currentCorrection;
			}
			finally {
				syncLock.unlock();
			}
			
			float[] currentBuffer = new float[bufferSize];
			//Fill the current buffer with empty sample values.
			Arrays.fill(currentBuffer, 0, currentCorrection, 0);
			//If there is more space, add audio samples to the current buffer
			for(int j = currentCorrection; j<bufferSize; j++) {
				currentBuffer[j] = buffer.remove();
			}
			//Pass the signal object to the corresponding MSPStream object.
			streams[i].maxPerformed(input);
			//Copy the current buffer to the MSP output vector
			System.arraycopy(currentBuffer, 0, output.vec, 0, bufferSize);
		}
	}
	
	/**
	 * Initialization method for the signals called by Max/MSP.
	 */
	@Override
	public void dspsetup(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		sampleRate = sigs_in[0].sr;
		bufferSize = sigs_in[0].n;
		previousLatencies = new HashMap<>();
		int ctr = 0;
		for(int i = 0; i<streamConfig.length; ++i) {
			String s = streamConfig[i];
			StreamGroup group = new StreamGroup();
			group.setDescription("inset " + i);
			for(int j = 0; j<s.length(); j++) {
				char c = s.charAt(j);
				MSPStream stream = new MSPStream(sampleRate, bufferSize);
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
			previousLatencies.put(group, 0);
		}
		streamSet = new StreamSet(Arrays.asList(streamGroups));
		syncer = new RealtimeSignalSync(streamSet);
		syncer.addEventListener(this);
		
		//Create a buffer for each stream
		buffers = new ArrayList<>(numberOfStreams);
		for(int i = 0; i<numberOfStreams; i++) {
			buffers.add(new LinkedList<Float>());
		}
		//Initialize the list of corrections
		this.corrections = new int[numberOfStreams];
		//The initial latency of each stream is the sliceSize
		Arrays.fill(corrections, (int) (sliceSize * sampleRate));
	}
	
	/**
	 * Method called when latency results are available.
	 * Currently this method does nothing but printing the
	 * results to the Max console.
	 */
	@Override
	public void onSyncEvent(Map<StreamGroup, LatencyResult> data) {
		printLatencies(data);
		int highestCorrection = Integer.MIN_VALUE;
		Map<StreamGroup, Integer> currentCorrections = new HashMap<>();
		for(Entry<StreamGroup, LatencyResult> entry : data.entrySet()) {
			LatencyResult latency = entry.getValue();
			StreamGroup group = entry.getKey();
			int previousLatency = previousLatencies.get(group);
			
			//Latency in seconds to latency in samples. 
			int latencyInSamples = (int) Math.round(latency.getLatencyInSeconds() * sampleRate);
			post("MSP latency in samples: " + latencyInSamples);
			//Calculate the correction: the difference between the current latency 
			//and the previous latency in  number of samples.
			int correction = latencyInSamples - previousLatency;
			if(correction > highestCorrection) {
				highestCorrection = correction;
			}
			currentCorrections.put(group, correction);
			previousLatencies.put(group, latencyInSamples);
		}
		//Change each correction so it's relative to the highest correction
		for(Entry<StreamGroup, Integer> entry : currentCorrections.entrySet()) {
			entry.setValue(highestCorrection - entry.getValue());
		}
		int streamCounter = 0;
		syncLock.lock();
		try {
			//Iterate over the streamgroups in the order of the insets.
			for(StreamGroup group : streamGroups) {
				int correction = currentCorrections.get(group);
				for(int i = 0; i<group.size(); i++) {
					//Set the correction for the corresponding streamNumber
					corrections[streamCounter++] = correction;
				}
			}
		}
		finally {
			syncLock.unlock();
		}
	}
	
	/**
	 * Print the latency information to the Max console
	 */
	private void printLatencies(Map<StreamGroup, LatencyResult> data) {
		for(Entry<StreamGroup, LatencyResult> entry : data.entrySet()) {
			StreamGroup streamGroup = entry.getKey();
			LatencyResult latency = entry.getValue();
			post("Streamgroup " + streamGroup.getDescription());
			post(latency.toString());
		}
		post("------------");
	}
	
	/**
	 * Method called when the module is removed from the Max/MSP patchboard.
	 */
	@Override
	protected void notifyDeleted() {
		//Cleanup
	}
}
