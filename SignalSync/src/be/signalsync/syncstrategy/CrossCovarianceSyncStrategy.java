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
 * @author Joren Six, Ward Van Assche
 */
public class CrossCovarianceSyncStrategy extends SyncStrategy {
	private final FingerprintSyncStrategy fingerprinter;
	private final int sampleRate;
	private final int nfftBufferSize;
	private final int stepSize;
	private final int nrOfTests;
	private final int successThreshold;

	public CrossCovarianceSyncStrategy(FingerprintSyncStrategy fingerprinter, int sampleRate, int nfftBufferSize, int stepSize, int nrOfTests, int succesThreshold) {
		this.fingerprinter = fingerprinter;
		this.sampleRate = sampleRate;
		this.nfftBufferSize = nfftBufferSize;
		this.stepSize = stepSize;
		this.nrOfTests = nrOfTests;
		this.successThreshold = succesThreshold;
	}

	/**
	 * This method returns a list with all the calculated latencies for each audiostream
	 * in comparison with the (first) reference stream.
	 * 
	 * @param slices A list of slices, each slice is an array of float values. The first slice
	 * acts as reference slice in the synchronization.
	 * @exception IllegalArgumentException Will be thrown when the slices list is empty.
	 * @return A list of latency results for each (non-reference) slice in comparison with the reference slice.
	 */
	@Override
	public List<LatencyResult> findLatencies(final List<float[]> slices) {
		if(slices.isEmpty()) {
			throw new IllegalArgumentException("The slices list can not be empty.");
		}
		
		final List<float[]> others = new ArrayList<>(slices);
		float[] reference = others.remove(0);
		
		List<LatencyResult> results = new ArrayList<>();
		//Get the timing information using the fingerprinting algorithm.
		List<LatencyResult> fingerprintlatencies = fingerprinter.findLatencies(slices);
		Iterator<LatencyResult> latenciesIt = fingerprintlatencies.iterator();
		Iterator<float[]> othersIterator = others.iterator();
		
		//Iterate over the other streams, and the fingerprint latencies.
		while (latenciesIt.hasNext() && othersIterator.hasNext()) {
			LatencyResult fingerprintLatency = latenciesIt.next();
			if(!fingerprintLatency.isLatencyFound()) {
				results.add(LatencyResult.NO_RESULT);
			}
			else {
				//Calculate fingerprint latency
				int latencyInSamples = fingerprintLatency.getLatencyInSamples();
				float[] other = othersIterator.next();
				//Find the best crosscovariance result
				Integer refined = findBestCrossCovarianceResult(latencyInSamples, reference, other);
				if(refined != null) {
					//A result is found, adding it to the results.
					results.add(LatencyResult.refinedResult(((double) refined)/sampleRate, refined));
				}
				else {
					//No result found, getting the fingerprint offset and adding it to the resuls.
					int offsetFromMatching = latencyInSamples;
					results.add(LatencyResult.rawResult(offsetFromMatching/sampleRate, offsetFromMatching));
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
	 * @return The latency in samples, or null.
	 */
	private Integer findBestCrossCovarianceResult(int fingerPrintLatencyInSamples, float[] reference, float[] other) {
		//The size of 1 slice (=length of reference or other buffer)
		int sliceSize = reference.length;
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
		
		if(max >= successThreshold) {
			//possible lags in samples
			int offsetLag1 = nfftBufferSize - bestLag;
			int offsetLag2 = bestLag;

			//possible refined latencies
			int offsetTotal1 = fingerPrintLatencyInSamples - offsetLag1;
			int offsetTotal2 = fingerPrintLatencyInSamples + offsetLag2;

			// Calculating the difference between the fingerprint match and the
			// refined results.
			int dif1 = Math.abs(offsetTotal1 - fingerPrintLatencyInSamples);
			int dif2 = Math.abs(offsetTotal2 - fingerPrintLatencyInSamples);

			// Check which results is the closest to the fingerprint match
			int offsetTotal = dif1 < dif2 ? offsetTotal1 : offsetTotal2;

			//Test if the difference of the crosscovariance result and fingerprint result
			//is not too big, if so, the crosscovariance result is probably wrong -> return null.
			if(Math.abs(fingerPrintLatencyInSamples-offsetTotal) < 2*stepSize) { 
				System.err.println("Covariancelag is CORRECT!");
				return offsetTotal; 
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
	 * @param referenceTime The matching fingerprint time of the reference stream in samples.
	 * @param otherTime The matching fingerprint time of the other stream in samples.
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
