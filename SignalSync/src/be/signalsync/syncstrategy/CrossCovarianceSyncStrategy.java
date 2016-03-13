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


/**
 * A synchronization strategy which makes use of the Panako fingerprinting algorithm and
 * the crosscovariance algorithm.
 * Some modifications were made to the algorithm to fit this use case.
 * 
 * @see <a href="http://panako.be">http://panako.be</a>
 * @author Joren Six, Ward Van Assche
 *
 */
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
	 * 
	 * @param slices A list of slices, each slice is an array of float values. The first slice
	 * acts as reference slice in the synchronization.
	 * @exception IllegalArgumentException Will be thrown when the slices list is empty.
	 * @return A list of latencies for each (non-reference) slice in comparison with the reference slice. When there is no
	 *         match found, NaN is added to the list.
	 */
	@Override
	public List<Float> findLatencies(final List<float[]> slices) {
		if(slices.isEmpty()) {
			throw new IllegalArgumentException("The slices list can not be empty.");
		}
		
		final List<float[]> others = new ArrayList<>(slices);
		float[] reference = others.remove(0);
		
		List<Float> results = new ArrayList<>();
		//Get the timing information using the fingerprinting algorithm.
		List<int[]> fingerprintTimingData = fingerprinter.synchronize(slices);

		Iterator<int[]> timingDataIterator = fingerprintTimingData.iterator();
		Iterator<float[]> othersIterator = others.iterator();
		
		//Iterate over the other streams, and the timing information of the streams.
		while (timingDataIterator.hasNext() && othersIterator.hasNext()) {
			int[] timing = timingDataIterator.next();
			if (timing.length < 2) {
				//No timing data available -> no crosscovariance possible.
				results.add(Float.NaN);
			} 
			else {
				//Calculate fingerprint latency
				int fingerPrintLatency = timing[1] - timing[0];
				float[] other = othersIterator.next();
				//Find the best crosscovariance result
				Float refined = findBestCrossCovarianceResult(fingerPrintLatency, reference, other);
				if(refined != null) {
					//A result is found, adding it to the results.
					results.add(refined);
				}
				else {
					//No result found, getting the fingerprint offset and adding it to the resuls.
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
	 * @param fingerPrintLatency The approximated latency calculated with the fingerprinting algorithm.
	 * @param reference A float buffer containing the reference stream data.
	 * @param other	A float buffer containing the other stream data.
	 * @return The latency in seconds, or null.
	 */
	private Float findBestCrossCovarianceResult(int fingerPrintLatency, float[] reference, float[] other) {
		//A map containing the results: Key: The found result (in sample), Value=The result count
		Map<Integer, Integer> refinedResult = new HashMap<Integer, Integer>();
		
		//The number of cross covariance to execute. The data will be split in equally divided parts.
		//The cross covariance algorithm will be executed on each part.
		int numberOfTests = Config.getInt(Key.CROSS_COVARIANCE_NUMBER_OF_TESTS);
		int succesThreshold = Config.getInt(Key.CROSS_COVARIANCE_THRESHOLD);
		
		//The number of nfft steps in the buffers.
		int steps = reference.length / STEP_SIZE;
		//The size of each divided part from the buffers.
		int partSize = steps / numberOfTests;
		//The start position
		int fp = Math.abs(fingerPrintLatency);
		
		//Start iterating. We start at the fingerprint latency value and take steps of the size of partSize.
		for(int position = fp; position<=BUFFER_SIZE-partSize; position+=partSize) {
			//Calculate the refined lag using the two streams and the calculated timing data.
			int lag = findCrossCovarianceLag(position, position+fingerPrintLatency, reference, other);
			//Check if the result already exists in the hashmap, if so, we increment the value, 
			//else the value becomes 1.
			Integer value = refinedResult.get(lag) != null ? refinedResult.get(lag) : 0;
			refinedResult.put(lag, value+1);
		}
		
		int max = 0, bestLag = 0;
		//Iterating over the results, we keep the best result and its occurence.
		for(Entry<Integer, Integer> entry : refinedResult.entrySet())
		{
		   int lag = entry.getKey();
		   int n = entry.getValue();
		   //If the lag is 0, it's probably wrong so we reject it.
		   if(lag != 0 && n > max) {
			   max = n;
			   bestLag = lag;
		   }
		}
		
		//Test if the occurence of the best lag is above the required threshold. If not: return null,
		//else: calculate the offset in seconds.
		if(max > succesThreshold) {
			// lag in seconds
			double offsetLagInSeconds1 = (BUFFER_SIZE - bestLag) / (float) SAMPLE_RATE;
			double offsetLagInSeconds2 = bestLag / (float) SAMPLE_RATE;
			
			double offsetFromMatching = -fingerPrintLatency * FFT_HOPSIZE_S;
			
			double offsetTotalInSeconds1 = offsetFromMatching + offsetLagInSeconds1;
			// Happens when the fingerprint algorithm overestimated the real latency
			double offsetTotalInSeconds2 = offsetFromMatching - offsetLagInSeconds2;

			// Calculating the difference between the fingerprint match and the
			// covariance results.
			double dif1 = Math.abs(offsetTotalInSeconds1 - offsetFromMatching);
			double dif2 = Math.abs(offsetTotalInSeconds2 - offsetFromMatching);

			// Check which results is the closest to the fingerprint match
			double offsetTotalInSeconds = dif1 < dif2 ? offsetTotalInSeconds1 : offsetTotalInSeconds2;

			//Test if the difference of the crosscovariance result and fingerprint result
			//is not too big, if so, the crosscovariance result is probably wrong -> return null.
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
	 * @param referenceTime The matching fingerprint time of the reference stream.
	 * @param otherTime The matching fingerprint time of the other stream.
	 * @param reference The float buffer of the reference stream.
	 * @param other The float buffer of the other stream.
	 * @return The lag between the reference stream and the other stream in samples.
	 */
	private int findCrossCovarianceLag(int referenceTime, int otherTime, float[] reference, float[] other) {
		AudioDispatcher refDispatcher;
		AudioDispatcher otherDispatcher;
		try {
			//Create audiodispatchers from the float buffers.
			refDispatcher = AudioDispatcherFactory.fromFloatArray(reference, SAMPLE_RATE, BUFFER_SIZE, OVERLAP);
			otherDispatcher = AudioDispatcherFactory.fromFloatArray(other, SAMPLE_RATE, BUFFER_SIZE, OVERLAP);
		} 
		catch (UnsupportedAudioFileException e) {
			Log.log(Level.SEVERE, "Audio file problem, check this!", e);
			return 0;
		}

		final float sizeS = BUFFER_SIZE / (float) SAMPLE_RATE;

		//Calculating how much we have to skip each audiodispatcher before we can start
		//aligning the streams using the crosscovariance algorithm.
		final double referenceAudioToSkip = sizeS + referenceTime * FFT_HOPSIZE_S;
		final double otherAudioToSkip = sizeS + otherTime * FFT_HOPSIZE_S;
		
		AudioSkipper referenceAudioSkipper = new AudioSkipper(referenceAudioToSkip);
		refDispatcher.addAudioProcessor(referenceAudioSkipper);
		refDispatcher.run();
		//Get the reference frame which will be used in the crosscovariance algorithm.
		float[] referenceAudioFrame = referenceAudioSkipper.getAudioFrame();

		final AudioSkipper otherAudioSkipper = new AudioSkipper(otherAudioToSkip);
		otherDispatcher.addAudioProcessor(otherAudioSkipper);
		otherDispatcher.run();
		//Get the other frame which will be used in the crosscovariance algorithm.
		float[] otherAudioFrame = otherAudioSkipper.getAudioFrame();

		// lag in samples, determines how many samples the other audio frame
		// lags with respect to the reference audio frame.
		int lag = bestCrossCovarianceLag(referenceAudioFrame, otherAudioFrame);
		return lag;
	}
	
	/**
	 * This method returns the lag of the best crosscovariance value
	 * found in the reference and other audio frame.
	 * @param reference The reference audio frame
	 * @param other The other audio frame
	 * @return The found lag.
	 */
	private int bestCrossCovarianceLag(final float[] reference, final float[] other) {
		System.out.println(reference.length);
		double maxCovariance = Double.NEGATIVE_INFINITY;
		int maxCovarianceIndex = -1;
		for (int lag = 0; lag < reference.length; ++lag) {
			final double covariance = covariance(reference, other, lag);
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
	 * @param reference The reference audio frame.
	 * @param other The other audio frame.
	 * @param lag The lag to use in the calculation.
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
	
	/**
	 * A helper class used for skipping a certain amount of time from
	 * a AudioDispatcher and returning an audio buffer after the number
	 * of frames is skipped.
	 * @author Ward Van Assche
	 *
	 */
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
		public void processingFinished() {}
	}
}
