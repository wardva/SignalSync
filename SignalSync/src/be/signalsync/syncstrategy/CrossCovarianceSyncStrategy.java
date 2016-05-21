package be.signalsync.syncstrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	private final FingerprintSyncStrategy fingerprinter;
	private final int sampleRate;
	private final int nfftBufferSize;
	private final int stepSize;
	private final int nrOfTests;
	private final int successThreshold;
	private final double FFTHopsize;

	public CrossCovarianceSyncStrategy(FingerprintSyncStrategy fingerprinter, int sampleRate, int nfftBufferSize, int stepSize, int nrOfTests, int succesThreshold) {
		this.fingerprinter = fingerprinter;
		this.sampleRate = sampleRate;
		this.nfftBufferSize = nfftBufferSize;
		this.stepSize = stepSize;
		this.nrOfTests = nrOfTests;
		this.successThreshold = succesThreshold;
		this.FFTHopsize = stepSize / (double) sampleRate;
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
	public List<Double> findLatencies(final List<float[]> slices) {
		if(slices.isEmpty()) {
			throw new IllegalArgumentException("The slices list can not be empty.");
		}
		
		final List<float[]> others = new ArrayList<>(slices);
		float[] reference = others.remove(0);
		
		List<Double> results = new ArrayList<>();
		//Get the timing information using the fingerprinting algorithm.
		List<int[]> fingerprintTimingData = fingerprinter.getResults(slices);

		Iterator<int[]> timingDataIterator = fingerprintTimingData.iterator();
		Iterator<float[]> othersIterator = others.iterator();
		
		//Iterate over the other streams, and the timing information of the streams.
		while (timingDataIterator.hasNext() && othersIterator.hasNext()) {
			int[] timing = timingDataIterator.next();
			if (timing.length < 2) {
				//No timing data available -> no crosscovariance possible.
				results.add(Double.NaN);
			} 
			else {
				//Calculate fingerprint latency
				int fingerPrintLatency = timing[1] - timing[0];
				float[] other = othersIterator.next();
				//Find the best crosscovariance result
				Double refined = findBestCrossCovarianceResult(fingerPrintLatency, reference, other);
				if(refined != null) {
					//A result is found, adding it to the results.
					results.add(refined);
				}
				else {
					//No result found, getting the fingerprint offset and adding it to the resuls.
					double offsetFromMatching = fingerPrintLatency * FFTHopsize;
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
	private Double findBestCrossCovarianceResult(int fingerPrintLatency, float[] reference, float[] other) {
		//The size of 1 slice (=length of reference or other buffer)
		int sliceSize = reference.length;
		//Convert the fingerprintLatency from number of fft steps to number of samples
		int fingerPrintLatencyInSamples = fingerPrintLatency * stepSize;
		//A map containing the results: Key: The found result (in number of samples), Value=The result count
		Map<Integer, Integer> allLags = new HashMap<Integer, Integer>();	
		//The step size we have use to perform `nrOfTests` tests.
		int bufferStepSize = reference.length / nrOfTests;
		
		//Calculate the start position
		int start = fingerPrintLatencyInSamples < 0 ? -fingerPrintLatencyInSamples : 0;
		//Calculate the stop position
		int stop = fingerPrintLatencyInSamples < 0 ? sliceSize - nfftBufferSize : sliceSize - nfftBufferSize - fingerPrintLatencyInSamples;

		//Start iterating. We start at the fingerprint latency value and take steps of the size of partSize.
		for(int position = start; position < stop; position+=bufferStepSize) {
			//Calculate the refined lag using the two streams and the calculated timing data.
			int lag = findCrossCovarianceLag(position, position+fingerPrintLatencyInSamples, reference, other);
			//Check if the result already exists in the hashmap, if so, we increment the value, 
			//else the value becomes 1.
			int value = allLags.get(lag) != null ? allLags.get(lag) : 0;
			allLags.put(lag, value+1);
		}
		
		int max = 0, bestLag = 0;
		//Iterating over the results, keeping the best result and its count
		for(Entry<Integer, Integer> entry : allLags.entrySet())
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
		if(max >= successThreshold) {
			//possible lags in seconds
			double offsetLagInSeconds1 = (nfftBufferSize - bestLag) / (float) sampleRate;
			double offsetLagInSeconds2 = bestLag / (float) sampleRate;
			
			//fingerprint latency in seconds
			double offsetFromMatching = fingerPrintLatency * FFTHopsize;
			
			//possible refined latencies
			double offsetTotalInSeconds1 = offsetFromMatching - offsetLagInSeconds1;
			double offsetTotalInSeconds2 = offsetFromMatching + offsetLagInSeconds2;

			// Calculating the difference between the fingerprint match and the
			// refined results.
			double dif1 = Math.abs(offsetTotalInSeconds1 - offsetFromMatching);
			double dif2 = Math.abs(offsetTotalInSeconds2 - offsetFromMatching);

			// Check which results is the closest to the fingerprint match
			double offsetTotalInSeconds = dif1 < dif2 ? offsetTotalInSeconds1 : offsetTotalInSeconds2;

			//Test if the difference of the crosscovariance result and fingerprint result
			//is not too big, if so, the crosscovariance result is probably wrong -> return null.
			if(Math.abs(offsetFromMatching-offsetTotalInSeconds) < 2*FFTHopsize) { 
				System.err.println("Covariancelag is CORRECT!");
				return offsetTotalInSeconds; 
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
		//Get the reference frame which will be used in the crosscovariance algorithm.
		float[] referenceAudioFrame = skipAudio(reference, referenceTime);
		float[] otherAudioFrame = skipAudio(other, otherTime);
				
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
	 * This method skips a number of samples from an audio buffer and
	 * returns a new buffer containing the first `nfftBufferSize` samples.
	 * @param buffer The initial buffer
	 * @param numberOfSamples The number of samples to skip
	 * @return
	 */
	private float[] skipAudio(float[] buffer, int numberOfSamples) {
		if(numberOfSamples + nfftBufferSize > buffer.length){
			throw new IllegalArgumentException("Can not skip audio");
		}
		float[] audioFrame = new float[nfftBufferSize];
		for(int i = 0; i<nfftBufferSize; i++) {
			audioFrame[i] = buffer[numberOfSamples + i];
		}
		return audioFrame;
	}
}
