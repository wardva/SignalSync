package be.signalsync.stream;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import com.cycling74.msp.MSPSignal;
import be.signalsync.slicer.StreamSlicer;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.signalsync.util.Util;
import be.tarsos.dsp.resample.Resampler;

/**
 * This class makes it possible to use a sequence of MSPSignal object
 * as a Stream. This only works if the maxPerformed method is called
 * correctly. 
 * @author Ward Van Assche
 */
public class MSPStream implements Stream {
	private List<StreamProcessor> processors;
	private long sampleCtr;
	private double mspSampleRate;
	private double targetSampleRate;
	private int mspBufferSize;
	private int resampleBufferSize;
	private int outputBufferSize;
	private boolean resampling;
	
	//Resampler fields
	private Resampler resampler;
	private LinkedList<Float> buffer;
	private double sampleRatio;
	private float[] resampledBuffer;;
	
	/**
	 * Create a new MSPStream object. If the sampleRate in the config
	 * file is different from the Max/MSP sampleRate the stream
	 * will be resampled.
	 * 
	 * @param mspSampleRate The sampleRate used in Max/MSP
	 * @param mspBufferSize The bufferSize used in Max/MSP
	 */
	public MSPStream(double mspSampleRate, int mspBufferSize) {
		this.mspSampleRate = mspSampleRate;
		this.targetSampleRate = Config.getDouble(Key.SAMPLE_RATE);
		this.outputBufferSize = Config.getInt(Key.NFFT_BUFFER_SIZE);
		this.mspBufferSize = mspBufferSize;
		this.resampling = Math.abs(mspSampleRate - targetSampleRate) > 0.001;
		this.processors = new ArrayList<>();
		this.buffer = new LinkedList<>();
		this.sampleCtr = 0;
		if(resampling) {
			initResampler();
		}
	}

	/**
	 * Initialize everything regarding the resampling process.
	 */
	private void initResampler() {
		sampleRatio = targetSampleRate / mspSampleRate;
		resampler = new Resampler(true, sampleRatio, sampleRatio);
		resampleBufferSize = (int) Math.round(sampleRatio * mspBufferSize);
		resampledBuffer = new float[resampleBufferSize];
		
	}
	
	/**
	 * This method should be called for each MSPSignal object of the stream.
	 * received from Max/MSP.
	 */
	public void maxPerformed(MSPSignal s) {
		if(resampling) {
			//Resample and process the resampled buffer
			resampler.process(sampleRatio, s.vec, 0, mspBufferSize, false, resampledBuffer, 0, resampleBufferSize);
			process(resampledBuffer);
		}
		else {
			//No resampling
			process(s.vec);
		}
	}
	
	/**
	 * This method processes a array containing samples.
	 * The samples are added to the buffer attribute. When there
	 * are enough samples in the buffer, the samples will be sent
	 * to the streamProcessors.
	 * @param vector The array containing the samples
	 */
	private void process(float[] vector) {
		//Add the sampleVector to our total buffer.
		buffer.addAll(Util.floatArrayToList(vector));
		//Fill as many outputBuffers as possible
		while(buffer.size() > outputBufferSize) {
			sampleCtr += outputBufferSize;
			double timestamp = (double) sampleCtr / targetSampleRate;
			float[] outputBuffer = new float[outputBufferSize];
			for(int i = 0; i<outputBufferSize; i++) {
				outputBuffer[i] = buffer.remove();
			}
			//Send the outputBuffer to the processors.
			for(StreamProcessor p : processors) {
				StreamEvent event = new StreamEvent(outputBuffer, timestamp);
				p.process(event);
			}
		}
	}
	
	/**
	 * Add a StreamProcessor to the list of StreamProcessors.
	 */
	@Override
	public void addStreamProcessor(StreamProcessor s) {
		processors.add(s);
	}
	
	/**
	 * Remove a StreamProcessor from the list of StreamProcessors.
	 */
	@Override
	public void removeStreamProcessor(StreamProcessor s) {
		processors.remove(s);
	}

	/**
	 * Create a StreamSlicer from the MSPStream. The slicer is registered
	 * as StreamProcessor.
	 */
	@Override
	public StreamSlicer createSlicer(int sliceSize, int sliceStep) {
		StreamSlicer slicer = new StreamSlicer(sliceSize, sliceStep, getSampleRate());
		addStreamProcessor(slicer);
		return slicer;
	}

	/**
	 * This method does nothing.
	 */
	@Override
	public void start() {}

	/**
	 * This method does nothing.
	 */
	@Override
	public void stop() {}

	/**
	 * This method returns the samplerate used after resampling
	 * from the Max/MSP samplerate.
	 */
	@Override
	public double getSampleRate() {
		return targetSampleRate;
	}
}