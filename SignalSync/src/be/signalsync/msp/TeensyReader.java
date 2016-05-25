package be.signalsync.msp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.cycling74.msp.MSPPerformer;
import com.cycling74.msp.MSPSignal;

import be.tarsos.dsp.resample.Resampler;
import be.ugent.ipem.teensydaq.DAQDataHandler;
import be.ugent.ipem.teensydaq.DAQSample;
import be.ugent.ipem.teensydaq.TeensyDAQ;
import be.ugent.ipem.teensydaq.util.SerialPortReader;
import jssc.SerialPortException;

/**
 * Max/MSP module for reading signals from a Teensy microcontroller.
 * More information can be found in my thesis.
 * 
 * @author Ward Van Assche
 */
public class TeensyReader extends MSPPerformer implements DAQDataHandler {
	//General attributes
	//------------------
	
	//First Teensy analog output channel, e.g. 5 => pin A5.
	private int startChannel;
	//The number of channels to read, starting at the startChannel
	private int numberOfChannels;
	//Channel which contains audio data for synchronisation, number starts at 0.
	private int audioChannel;
	//Sample rate of the Teensy (Hz), in most case 8000 or 11025.
	private int teensySampleRate;
	//The buffer size of the Teensy data buffer
	private int teensyBufferSize;
	
	//Teensy attributes
	//------------------
	
	//The Teensy input port.
	private String port;
	//The Teensy data acquisition object.
	private TeensyDAQ teensy;
	//The Teensy moving average window for optimizing the audio channel.
	private Deque<Double> movingAverageWindow;
	//The size of the moving average window.
	private int movingAverageWindowSize;
	//Used for optimizing the audio channel.
	private double currentAverageSum;
	//List of buffers for each Teensy channel.
	private List<BlockingQueue<Float>> buffers;	
				
	
	
	//Max performing attributes
	//-------------------------
	
	//The buffer size of the Max/MSP context.
	private int targetBufferSize;
	//The sample rate of the Max/MSP context.
	private int targetSampleRate;
	//A flag indicating if the module is running.
	private boolean processing;
	
	//Resampling attributes
	//------------------
	
	//The ratio of the Teensy and Max sample rate
	private double sampleRatio;
	//One resampler for each channel
	private Resampler[] resamplers;
	//Buffer for the input values, size: teensyBufferSize
	private float[] inputBuffer;
	//Buffer for the resampled values: size: targetBufferSize
	private float[] targetBuffer;
	
	//Invalid parameters
	public TeensyReader() {
		bail("(TeensyReader) Invalid method parameters.");
	}
	
	/**
	 * Create a TeensyReader with a port index. Useful when the Teensy portname is unknown.
	 */
	public TeensyReader(int portIdx, int sampleRate, int startChannel, int audioChannel, int numberOfChannels) {
		this(findPortAtIndex(portIdx), sampleRate, startChannel, audioChannel, numberOfChannels);
	}
	
	/**
	 * Create a TeensyReader reading from the first available port.
	 */
	public TeensyReader(int sampleRate, int startChannel, int audioChannel, int numberOfChannels) {
		this(findFirstOpenPort(), sampleRate, startChannel, audioChannel, numberOfChannels);
	}
	
	public TeensyReader(String port, int sampleRate, int startChannel, int audioChannel, int numberOfChannels) {
		//Check if the port is valid
		if(!validPort(port)) {
			bail("Teensy port is not open");
			return;
		}
		//Check the number of channels and startChannel for invalid values
		//AudioChannel can be invalid, invalid value => no audio channel
		if(numberOfChannels < 1 || startChannel < 0) {
			bail("Invalid channel parameter");
			return;
		}
		
		//Constants
		this.startChannel = startChannel;
		this.numberOfChannels = numberOfChannels;
		this.audioChannel = audioChannel;
		this.teensySampleRate = sampleRate;
		this.port = port;
		
		//Teensy processing
		this.movingAverageWindow = new LinkedList<>();
		this.currentAverageSum = 0.0D;
		this.movingAverageWindowSize = 2 * sampleRate;
		this.buffers = new ArrayList<>(numberOfChannels);
		this.resamplers = new Resampler[numberOfChannels];
		
		for(int i = 0; i<numberOfChannels; i++) {
			//Create a blocking queue for each channel
			BlockingQueue<Float> buffer = new LinkedBlockingQueue<>();
			buffers.add(buffer);
		}
		
		//Setting up the max module
		this.processing = false;
		setOutlets();
		setAssists();
		
		//Create a TeensyDAQ object for accessing the Teensy
		this.teensy = new TeensyDAQ(sampleRate, port, startChannel, numberOfChannels);
		
		try {
			this.teensy.start();
		} 
		catch (SerialPortException e) {
			post("Error while trying to start teensy from port  " + port + ": " + e.getMessage());
		}
		this.teensy.addDataHandler(this);
	}
	
	/**
	 * This method checks if a portName is valid.
	 */
	private boolean validPort(String port1) {
		String[] serialPorts = SerialPortReader.getSerialPorts();
		for(String port2 : serialPorts) {
			if(port1.equals(port2)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method is called when the DSP running state
	 * is changed.
	 */
	@Override
	protected void dspstate(boolean running) {
		post("(TeensyReader) dspstate changed: " + running);
		processing = running;
	}
	
	/**
	 * This method is called before the module will start running. 
	 * The buffers and sample rates are initialized in this method.
	 */
	@Override
	public void dspsetup(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		//Get the Max/MSP sample rate
		this.targetSampleRate = (int) sigs_out[0].sr;
		//Get the Max/MSP buffer size
		this.targetBufferSize = sigs_out[0].n;
		//Calculate the sample ratio
		this.sampleRatio = ((double) targetSampleRate) / teensySampleRate;
		//Calculate the Teensy buffer size
		this.teensyBufferSize = (int) Math.round(targetBufferSize / sampleRatio);
		//Create a new input buffer using the Teensy buffer size.
		this.inputBuffer = new float[teensyBufferSize];
		//Create a new target buffer, the resampled samples will be put
		//in this buffer.
		this.targetBuffer = new float[targetBufferSize];
		//Create new Resampler object.
		for(int i = 0; i<numberOfChannels; i++) {
			this.resamplers[i] = new Resampler(true, sampleRatio, sampleRatio);
		}
		
		post("(TeensyReader) targetSampleRate: " + targetSampleRate);
		post("(TeensyReader) targetBufferSize: " + targetBufferSize);
		post("(TeensyReader) sampleRatio: " + sampleRatio);
		post("(TeensyReader) teensyBufferSize: " + teensyBufferSize);
	}
	
	/**
	 * This method will be called by the Max/MSP context when new samples
	 * are available.
	 */
	@Override
	public void perform(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		try {
			for(int i = 0; i<numberOfChannels; i++) {
				//Get the i'th buffer with Teensy data
				BlockingQueue<Float> buffer = buffers.get(i);
				//Get the i'th Max output signal
				MSPSignal out = sigs_out[i];
				//Take the needed amount of samples from the buffer.
				for(int j = 0; j<teensyBufferSize; j++) {
					Float value = buffer.poll(2, TimeUnit.SECONDS);
					if(value == null) {
						post("(TeensyReader) No value available in buffer. "
								+ "This can be fixed by removing inserting the USB cable and recreating this module.");
						break;
					}
					else {
						inputBuffer[j] = value;
					}
				}
				//Resample the input buffer
				resamplers[i].process(sampleRatio, inputBuffer, 0, teensyBufferSize, false, targetBuffer, 0, targetBufferSize);
				//Copy the resampled buffer to the MSPSignal object.
				System.arraycopy(targetBuffer, 0, out.vec, 0, targetBufferSize);
			}
		} 
		catch (InterruptedException e) {
			post("(TeensyReader) Fatal error while performing teensy port " + port + ": " + e.getMessage());
		}
	}

	/**
	 * Initialize the outlets of the Max/MSP module
	 */
	private void setOutlets() {
		int outlets[] = new int[numberOfChannels];
		Arrays.fill(outlets, SIGNAL);
		declareOutlets(outlets);
	}
	
	/**
	 * Initialize the assists of the outlets.
	 */
	private void setAssists() {
		String assists[] = new String[numberOfChannels];
		for(int i = 0; i<numberOfChannels; i++) {
			assists[i] = "Teensy channel " + (i + startChannel);
		}
		setOutletAssist(assists);
	}
	
	/**
	 * This method is called by the TeensyDAQ object when new Teensy samples are available.
	 * The samples will be put in buffers.
	 */
	@Override
	public void handle(DAQSample sample) {
		if(!processing) {
			//If the module is not running, we don't need the samples.
			return;
		}
		//Iterate over the channels.
		for(int i = 0; i < numberOfChannels; i++) {
			//Calculate the correct index of the channel.
			int channelIdx = startChannel + i;
			//Get the sample (voltage between 0V and 3.3V).
			double sampleData = sample.data[channelIdx];
			double modifiedSampleData;
			//If the channel is the audio channel, the data will be filtered with a moving 
			//average window.
			if(channelIdx == audioChannel) {
				movingAverageWindow.addLast(sampleData);
				currentAverageSum += sampleData;
				if(movingAverageWindow.size() > movingAverageWindowSize){
					currentAverageSum -= movingAverageWindow.removeFirst();
				}
				//Calculate the average from the last n samples
				//where n = movingAverageWindowSize.
				double movingAverage = (currentAverageSum/movingAverageWindow.size());
				//Subtract the moving average from the sample data.
				modifiedSampleData = sampleData - movingAverage;
			}
			else {
				//Subtract half of the max. voltage from the sample so the value.
				//is between -1.65 and 1.65
				modifiedSampleData = (sampleData - 1.65);
			}
			//Normalize the modified sample so the value is between -1 and 1.
			double normalized = modifiedSampleData / (1.65);
			//Add the normalized value to the buffer so it can be processed by Max/MSP.
			buffers.get(i).add((float) normalized);
		}
	}
	
	//Called when the module is deleted from Max/MSP
	@Override
	protected void notifyDeleted() {
		post("(TeensyReader) Module deleted from patchboard.");
		teensy.stop();
	}

	@Override
	public void stopDataHandler() {
		
	}
	
	/**
	 * Helper method used when a constructor is called with a port index instead of a 
	 * port name. This is useful when the name of a Teensy port is unknown.
	 * @param portIdx The index of the port, starting at 0.
	 * @return The portname
	 */
	public static String findPortAtIndex(int portIdx) {
		String[] serialPorts = SerialPortReader.getSerialPorts();
		if(portIdx >= 0 && serialPorts.length > 0 && portIdx < serialPorts.length) {
			String portName = serialPorts[portIdx];
			post("Found an open COM-port at index " + portIdx + ": " + portName);
			return portName;
		}
		else {
			bail("(TeensyReader) No port found at index " + portIdx + ".");
			return null;
		}
	}
	
	/**
	 * Helper method to find the first open Teensy port.
	 * @return The portname
	 */
	public static String findFirstOpenPort() {
		return findPortAtIndex(0);
	}
}
