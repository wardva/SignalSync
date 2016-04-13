package be.signalsync.max;

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

import be.signalsync.teensy.DAQDataHandler;
import be.signalsync.teensy.DAQSample;
import be.signalsync.teensy.TeensyDAQ;
import be.tarsos.dsp.resample.Resampler;

public class TeensyReader extends MSPPerformer implements DAQDataHandler {
	//General parameters
	private int startChannel;
	private int numberOfChannels;
	private int audioChannel;
	private int teensySampleRate;
	private int teensyBufferSize;
	
	//Teensy processing attributes
	private TeensyDAQ teensy;
	private Deque<Double> movingAverageWindow;
	private List<BlockingQueue<Float>> buffers;
	private double currentAverageSum;
	private int movingAverageWindowSize;
	
	//Max performing attributes
	private int targetBufferSize;
	private int targetSampleRate;
	private boolean startMessageReceived;
	private boolean processing;
	private boolean dacRunning;
	
	//Resampling attributes
	private double sampleRatio;
	private Resampler[] resamplers;
	private float[] inputBuffer;
	private float[] targetBuffer;
	
	public TeensyReader() {
		bail("(TeensyReader) Invalid method parameters.");
	}
	
	public TeensyReader(String port, int sampleRate, int startChannel, int audioChannel, int numberOfChannels) {
		post("Constructor called");
		//Constants
		this.startChannel = startChannel;
		this.numberOfChannels = numberOfChannels;
		this.audioChannel = audioChannel;
		this.teensySampleRate = sampleRate;
		
		//Teensy processing
		this.movingAverageWindow = new LinkedList<>();
		this.currentAverageSum = 0.0D;
		this.movingAverageWindowSize = 2 * sampleRate;
		this.buffers = new ArrayList<>(numberOfChannels);
		this.resamplers = new Resampler[numberOfChannels];
		
		for(int i = 0; i<numberOfChannels; i++) {
			BlockingQueue<Float> buffer = new LinkedBlockingQueue<>();
			buffers.add(buffer);
		}
		
		//Setting up the max module
		startMessageReceived = processing = dacRunning = false;
		setInlets();
		setOutlets();
		setAssists();
		
		this.teensy = new TeensyDAQ(sampleRate, port, startChannel, numberOfChannels);
		this.teensy.addDataHandler(this);
		this.teensy.start();
	}
	
	public void start() {
		post("Start message received");
		startMessageReceived = true;
		stateChange();
	}
	
	public void stop() {
		post("Stop message received");
		startMessageReceived = false;
		stateChange();
	}
	
	@Override
	protected void dspstate(boolean b) {
		post("dspstate changed: " + b);
		this.dacRunning = b;
		stateChange();
	}
	
	public void stateChange() {
		processing = startMessageReceived && dacRunning;
	}
	
	@Override
	public void dspsetup(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		if(sigs_out.length != numberOfChannels) {
			throw new IllegalArgumentException("Invalid array of output signals.");
		}
		this.targetSampleRate = (int) sigs_out[0].sr;
		this.targetBufferSize = sigs_out[0].n;

		this.sampleRatio = targetSampleRate / teensySampleRate;
		this.teensyBufferSize = (int) Math.round(targetBufferSize / sampleRatio);
		this.inputBuffer = new float[teensyBufferSize];
		this.targetBuffer = new float[targetBufferSize];
		for(int i = 0; i<numberOfChannels; i++) {
			this.resamplers[i] = new Resampler(true, sampleRatio, sampleRatio);
		}
	}
	
	@Override
	public void perform(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		try {
			for(int i = 0; i<numberOfChannels; i++) {
				BlockingQueue<Float> buffer = buffers.get(i);
				MSPSignal out = sigs_out[i];
				if(processing) {
					for(int j = 0; j<teensyBufferSize; j++) {
						Float value = buffer.poll(2, TimeUnit.SECONDS);
						if(value == null) {
							post("Error: polled value is null");
							break;
						}
						else {
							inputBuffer[j] = value;
						}
					}
					resamplers[i].process(sampleRatio, inputBuffer, 0, teensyBufferSize, false, targetBuffer, 0, targetBufferSize);
					System.arraycopy(targetBuffer, 0, out.vec, 0, targetBufferSize);
				}
				else {
					Arrays.fill(out.vec, 0);
				}
			}
		} 
		catch (Exception e) {
			post("Fatal error: ");
			e.printStackTrace();
		}
	}
	
	private void setInlets() {
		declareInlets(new int[] {});
	}

	private void setOutlets() {
		int outlets[] = new int[numberOfChannels];
		Arrays.fill(outlets, SIGNAL);
		declareOutlets(outlets);
	}
	
	private void setAssists() {
		String assists[] = new String[numberOfChannels];
		for(int i = 0; i<numberOfChannels; i++) {
			assists[i] = "Teensy channel " + (i + startChannel);
		}
		setOutletAssist(assists);
	}
	
	@Override
	public void handle(DAQSample sample) {
		if(!processing) {
			return;
		}
		for(int i = 0; i < numberOfChannels; i++) {
			int channelIdx = startChannel + i;
			double sampleData = sample.data[channelIdx];

			double modifiedSampleData;
			if(channelIdx == audioChannel) {
				movingAverageWindow.addLast(sampleData);
				currentAverageSum += sampleData;
				if(movingAverageWindow.size() > movingAverageWindowSize){
					currentAverageSum -= movingAverageWindow.removeFirst();
				}
				double movingAverage = (currentAverageSum/movingAverageWindow.size());
				modifiedSampleData = sampleData - movingAverage;
			}
			else {
				modifiedSampleData = (sampleData - 1.65);
			}
			double normalized = modifiedSampleData / (1.65);
			buffers.get(i).add((float) normalized);
		}
	}
	
	@Override
	protected void notifyDeleted() {
		post("notifyDeleted called");
		teensy.stop();
	}

	@Override
	public void stopDataHandler() {
		
	}
}
