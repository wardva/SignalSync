package be.signalsync.teensy;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.signalsync.teensy.SerialPortReader.SerialDataLineHandler;


public class TeensyDAQ implements SerialDataLineHandler {
	private static final Logger LOG = Logger.getLogger(TeensyDAQ.class.getName());
	
	private static final  float REFERENCE_VOLTAGE = 3.3f;//v
	private static final float STEPS = 8192f;//2^13
	private static final float SCALER  = REFERENCE_VOLTAGE/STEPS;
	
	private float timeScaler; //period in seconds (1/sample rate)
	
	private SerialPortReader reader;
	private final List<DAQDataHandler> dataHandlers;
	private final int startChannel;
	private final int numberOfChannels;
	
	private final DAQValueRange[] ranges;
	private int sampleRate;
	
	private long firstTimeIndex = -100;
	private long prevTimeIndex = -100;
	
	
	public TeensyDAQ(int sampleRate,String portName,int startChannel,int numberOfChannels){
		dataHandlers = new ArrayList<DAQDataHandler>();
		this.numberOfChannels = numberOfChannels;
		this.startChannel = startChannel;
		ranges = new DAQValueRange[numberOfChannels];
		for(int i = 0 ; i < ranges.length ; i++){
			ranges[i] = DAQValueRange.ZERO_TO_3_POINT_3_VOLTS;
		}
		reader = new SerialPortReader(portName, this);
		this.sampleRate = sampleRate;
	}
	
	
	public void start(){
		try {
			reader.open();
			reader.write(String.format("SET SR %04d\n", sampleRate));
			//wait for the sample rate change to finish
			Thread.sleep(10);
		} 
		catch (InterruptedException e) {
			LOG.log(Level.SEVERE, "Concurrency problem, please check this out!", e);
		}
		
		timeScaler = 1.0f/this.sampleRate;
		reader.start();
	}
	
	public void stop(){
		reader.stop();
		firstTimeIndex = -100;
	}
	
	public void addDataHandler(DAQDataHandler handler){
		dataHandlers.add(handler);
	}
	
	@Override
	public void handleSerialDataLine(int lineNumber, String lineData) {
		//first lines are unreliable
		if(lineNumber < 60)
		return;
		
		String[] lineDataValues = lineData.split(" ");
		long timeIndex = Long.parseLong(lineDataValues[0].trim(),16);
		if(firstTimeIndex == -100){
			firstTimeIndex = timeIndex;
		}
		
		timeIndex = timeIndex - firstTimeIndex;
		double timeStampInS = timeIndex * timeScaler;
		
		//check if the timeIndex is equal to the prev time index +1
		//if not samples have been dropped!
		if(prevTimeIndex>0 && prevTimeIndex + 1 != timeIndex){
			long jumpSize = (timeIndex - prevTimeIndex);
			LOG.severe("Unexpected time index jump " + jumpSize + " from " + prevTimeIndex + " to " + timeIndex + ". Samples dropped?");
			LOG.severe("  Parsed line (" +  lineNumber + ") :" + lineData);
		}

		double[] measurements = new double[numberOfChannels];
		
		for(int i = 0; i < numberOfChannels ; i++){
			measurements[i] = Integer.parseInt(lineDataValues[i+1+startChannel].trim(),16) * SCALER;//voltage
		}
		final DAQSample sample = new DAQSample(timeStampInS, measurements, ranges);
		for(DAQDataHandler handler : dataHandlers){
			handler.handle(sample);
		}
		prevTimeIndex = timeIndex;
	}
}
