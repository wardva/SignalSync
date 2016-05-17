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

/**
 * Max/MSP module for synchronizing signals using audio-to-audio alignment.
 * @author Ward Van Assche
 */
public class Sync extends MSPPerformer implements SyncEventListener {
	//Regular expression for validating the configuration string.
	private final static String STREAM_CONFIG_REGEX = "d*ad*(,d*ad*)*";

	//The Max/MSP samplerate
	private double sampleRate;
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
		//Create the Streamgroup array.
		this.streamGroups = new StreamGroup[streamConfig.length];
		//Create the Stream array.
		this.streams = new MSPStream[numberOfStreams];
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
			//Pass the signal object to the corresponding stream.
			streams[i].maxPerformed(input);
			//input -> output without modifications or synchronization.
			System.arraycopy(input.vec, 0, output.vec, 0, input.n);
		}
	}
	
	/**
	 * Initialization method for the signals called by Max/MSP.
	 */
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
	
	/**
	 * Method called when latency results are available.
	 * Currently this method does nothing but printing the
	 * results to the Max console.
	 */
	@Override
	public void onSyncEvent(Map<StreamGroup, Double> data) {
		for(Entry<StreamGroup, Double> entry : data.entrySet()) {
			post("Latency: " + entry.getValue());
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
