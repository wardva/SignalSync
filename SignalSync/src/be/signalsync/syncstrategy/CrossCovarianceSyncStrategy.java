package be.signalsync.syncstrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class CrossCovarianceSyncStrategy extends SyncStrategy {
	private static Logger Log = Logger.getLogger(Config.get(Key.APPLICATION_NAME));
	private final FingerprintSyncStrategy fingerprinter;
	private static final int SAMPLE_RATE = Config.getInt(Key.SAMPLE_RATE);
	private static final int BUFFER_SIZE = Config.getInt(Key.BUFFER_SIZE);
	private static final int STEP_SIZE = Config.getInt(Key.STEP_SIZE);
	private static final int OVERLAP = BUFFER_SIZE - STEP_SIZE;
	private static final float FFT_HOPSIZE_S = STEP_SIZE / (float) SAMPLE_RATE;

	protected CrossCovarianceSyncStrategy() {
		fingerprinter = new FingerprintSyncStrategy();
	}

	/**
	 * This method returns a list with all the calculated latencies for each audiostream
	 * in comparison with the (first) reference stream.
	 */
	@Override
	public List<Float> findLatencies(final List<float[]> slices) {
		final List<float[]> others = new ArrayList<>(slices);
		float[] reference = others.remove(0);
		
		List<Float> results = new ArrayList<>();
		List<int[]> fingerprintTimingData = fingerprinter.synchronize(slices);

		Iterator<int[]> timingDataIterator = fingerprintTimingData.iterator();
		Iterator<float[]> othersIterator = others.iterator();
		
		//Iterate over the streams and their fingerprint timing data.
		while (timingDataIterator.hasNext() && othersIterator.hasNext()) {
			int[] timing = timingDataIterator.next();
			if (timing.length < 2) {
				//No timing data available -> no crosscovariance possible.
				results.add(Float.NaN);
			} 
			else {
				int fingerPrintLatency = timing[1] - timing[0];
				float[] other = othersIterator.next();
				//Find the best crosscovariance result
				Float refined = findBestCrossCovarianceResult(fingerPrintLatency, reference, other);
				if(refined != null) {
					//A result is found, adding it to the results.
					results.add(refined);
				}
				else {
					//No result found, getting the fingerprint offset data and adding it to the resuls.
					float offsetFromMatching = -fingerPrintLatency * FFT_HOPSIZE_S;
					results.add(offsetFromMatching);
				}
			}
		}
		return results;
	}
	
	/**
	 * This method returns the best crosscovariance result, or null if the crosscovariance result is not valid.
	 * This method executes the findCrossCovariance method n times (this depends on a value in the config file).
	 * The frequency of each lag is kept, if the frequency is above a given threshold (also in config file),
	 * this method will return the calculated latency, otherwise this method will return null.
	 * @return The latency in seconds, or null.
	 */
	private Float findBestCrossCovarianceResult(int fingerPrintLatency, float[] reference, float[] other) {
		Map<Integer, Integer> refinedResult = new HashMap<Integer, Integer>();
		
		int numberOfTests = Config.getInt(Key.CROSS_COVARIANCE_NUMBER_OF_TESTS);
		int succesThreshold = Config.getInt(Key.CROSS_COVARIANCE_THRESHOLD);
		int steps = reference.length / STEP_SIZE;
		int step = steps / numberOfTests;
		int fp = Math.abs(fingerPrintLatency);
		
		for(int position = fp; position<=BUFFER_SIZE-step; position+=step) {
			int lag = findCrossCovarianceLag(position, position+fingerPrintLatency, reference, other);
			Integer value = refinedResult.get(lag);
			if(value == null) value = 0;
			refinedResult.put(lag, value+1);
		}
		
		int max = 0;
		int bestLag = 0;
		for(Entry<Integer, Integer> entry : refinedResult.entrySet())
		{
		   int lag = entry.getKey();
		   int n = entry.getValue();
		   if(lag != 0 && n > max) {
			   max = n;
			   bestLag = lag;
		   }
		}
		
		if(max > succesThreshold) {
			// lag in seconds
			final double offsetLagInSeconds1 = (BUFFER_SIZE - bestLag) / (float) SAMPLE_RATE;
			final double offsetLagInSeconds2 = bestLag / (float) SAMPLE_RATE;
			
			double offsetFromMatching = -fingerPrintLatency * FFT_HOPSIZE_S;
			
			final double offsetTotalInSeconds1 = offsetFromMatching + offsetLagInSeconds1;
			// Happens when the fingerprint algorithm overestimated the real latency
			final double offsetTotalInSeconds2 = offsetFromMatching - offsetLagInSeconds2;

			// Calculating the difference between the fingerprint match and the
			// covariance results.
			final double dif1 = Math.abs(offsetTotalInSeconds1 - offsetFromMatching);
			final double dif2 = Math.abs(offsetTotalInSeconds2 - offsetFromMatching);

			// Check which results is the closest to the fingerprint match
			final double offsetTotalInSeconds = dif1 < dif2 ? offsetTotalInSeconds1 : offsetTotalInSeconds2;

			
			if(Math.abs(offsetFromMatching-offsetTotalInSeconds) < 2*FFT_HOPSIZE_S) { 
				System.err.println("Covariancelag is CORRECT!");
				return (float) offsetTotalInSeconds; 
			} 
			System.err.println("Covariancelag is incorrect!");
		}

		else {
			System.err.println("All lags under threshold!");
		}
		return null;
	}

	/**
	 * This method returns the cross covariance lag of two buffers.
	 */
	private int findCrossCovarianceLag(int referenceTime, int otherTime, float[] reference, float[] other) {
		AudioDispatcher refDispatcher;
		AudioDispatcher otherDispatcher;
		try {
			refDispatcher = AudioDispatcherFactory.fromFloatArray(reference, SAMPLE_RATE, BUFFER_SIZE, OVERLAP);
			otherDispatcher = AudioDispatcherFactory.fromFloatArray(other, SAMPLE_RATE, BUFFER_SIZE, OVERLAP);
		} 
		catch (UnsupportedAudioFileException e) {
			Log.log(Level.SEVERE, "Audio file problem, check this!", e);
			return 0;
		}

		final float sizeS = BUFFER_SIZE / (float) SAMPLE_RATE;

		final double referenceAudioToSkip = sizeS + referenceTime * FFT_HOPSIZE_S;
		final double otherAudioToSkip = sizeS + otherTime * FFT_HOPSIZE_S;
		
		AudioSkipper referenceAudioSkipper = new AudioSkipper(referenceAudioToSkip);
		refDispatcher.addAudioProcessor(referenceAudioSkipper);
		refDispatcher.run();
		//double referenceAudioStart = referenceAudioSkipper.getAudioStart();
		float[] referenceAudioFrame = referenceAudioSkipper.getAudioFrame();

		final AudioSkipper otherAudioSkipper = new AudioSkipper(otherAudioToSkip);
		otherDispatcher.addAudioProcessor(otherAudioSkipper);
		otherDispatcher.run();
		//double otherAudioStart = otherAudioSkipper.getAudioStart();
		float[] otherAudioFrame = otherAudioSkipper.getAudioFrame();

		// lag in samples, determines how many samples the other audio frame
		// lags with respect to the reference audio frame.
		int lag = bestCrossCovarianceLag(referenceAudioFrame, otherAudioFrame);
		return lag;
	}
	
	/**
	 * This method returns the lag of the best crosscovariance value
	 * found in the current buffers.
	 * @return The found lag.
	 */
	private int bestCrossCovarianceLag(final float[] reference, final float[] target) {
		double maxCovariance = Double.NEGATIVE_INFINITY;
		int maxCovarianceIndex = -1;
		for (int lag = 0; lag < reference.length; ++lag) {
			final double covariance = covariance(reference, target, lag);
			if (covariance > maxCovariance) {
				maxCovarianceIndex = lag;
				maxCovariance = covariance;
			}
		}
		return maxCovarianceIndex;
	}

	/**
	 * This method calculates the covariance between the two buffers
	 * using the given lag.
	 * 
	 * @return The covariance value.
	 */
	private double covariance(final float[] reference, final float[] target, final int lag) {
		double covariance = 0.0;
		for (int i = 0; i < reference.length; i++) {
			final int targetIndex = (i + lag) % reference.length;
			covariance += reference[i] * target[targetIndex];
		}
		return covariance;
	}
	
	
	private class AudioSkipper implements AudioProcessor {
		private final float[] audioFrame;
		private final double audioToSkip;

		public AudioSkipper(final double audioToSkip) {
			this.audioToSkip = audioToSkip;
			audioFrame = new float[Config.getInt(Key.BUFFER_SIZE)];
		}

		public float[] getAudioFrame() {
			return audioFrame;
		}

		@Override
		public boolean process(final AudioEvent audioEvent) {
			if (Math.abs(audioEvent.getTimeStamp() - audioToSkip) < 0.00001) {
				final float[] buffer = audioEvent.getFloatBuffer();
				for (int i = 0; i < buffer.length; i++) {
					audioFrame[i] = buffer[i];
				}
				return false;
			}
			return true;
		}

		@Override
		public void processingFinished() {
		}
	}
}
