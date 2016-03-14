package be.signalsync.syncstrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.panako.strategy.nfft.NFFTEventPointProcessor;
import be.panako.strategy.nfft.NFFTFingerprint;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

/**
 * A synchronization strategy which makes use of the Panako fingerprinting algorithm.
 * Some modifications were made to the algorithm to fit this use case.
 * 
 * @see <a href="http://panako.be">http://panako.be</a>
 * @author Ward Van Assche, Joren Six
 *
 */
public class FingerprintSyncStrategy extends SyncStrategy {
	private static Logger Log = Logger.getLogger(Config.get(Key.APPLICATION_NAME));
	
	private static final float MIN_FREQUENCY = 100;
	private static final float MAX_FREQUENCY = 4000;
	private static final int SAMPLE_RATE = Config.getInt(Key.SAMPLE_RATE);
	private static final int SIZE = Config.getInt(Key.NFFT_BUFFER_SIZE);
	private static final int STEP_SIZE = Config.getInt(Key.NFFT_STEP_SIZE);
	private static final int OVERLAP = SIZE - STEP_SIZE;

	protected FingerprintSyncStrategy() {
		//Changing the Panako configuration entries in the configuration entries from this project.
		be.panako.util.Config.set(be.panako.util.Key.NFFT_EVENT_POINT_MIN_DISTANCE,
				Config.get(Key.NFFT_EVENT_POINT_MIN_DISTANCE));
		be.panako.util.Config.set(be.panako.util.Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT,
				Config.get(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT));
	}

	/**
	 * Extract and return the fingerprints from an audio stream.
	 * @param dispatcher The dispatcher containing the audio stream.
	 * @return A list containing the fingerprints.
	 */
	private List<NFFTFingerprint> extractFingerprints(final AudioDispatcher dispatcher) {
		NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(SIZE, OVERLAP, SAMPLE_RATE);
		dispatcher.addAudioProcessor(minMaxProcessor);
		dispatcher.run();
		dispatcher.removeAudioProcessor(minMaxProcessor);
		List<NFFTFingerprint> fingerprints = new ArrayList<>(minMaxProcessor.getFingerprints());
		return fingerprints;
	}

	/**
	 * Filter a list of fingerprints. This method uses the MIN_FREQUENCY and MAX_FREQUENCY fields and removes
	 * the fingerprints with a frequency above MAX_FREQUENCY and below MIN_FREQUENCY.
	 * 
	 * @param fingerprints The list of fingerprints to filter. This list will not be modified.
	 * @return A list containing the filtered fingerprints.
	 */
	private List<NFFTFingerprint> filterPrints(final List<NFFTFingerprint> fingerprints) {
		List<NFFTFingerprint> filtered = new LinkedList<>(fingerprints);
		final int minf = (int) Math.ceil(MIN_FREQUENCY / (SAMPLE_RATE / SIZE));
		final int maxf = (int) Math.floor(MAX_FREQUENCY / (SAMPLE_RATE / SIZE));
		for(ListIterator<NFFTFingerprint> it = filtered.listIterator(); it.hasNext(); ) {
			NFFTFingerprint print = it.next();
			final boolean smallerThanMin = print.f1 <= minf || print.f2 <= minf;
			final boolean biggerThanMax = print.f1 >= maxf || print.f2 >= maxf;
			if (smallerThanMin || biggerThanMax) {
				it.remove();
			}
		}
		return filtered;
	}

	/**
	 * Get the latencies of a list of slices. A slice is a short part of a stream (as float buffer) from 
	 * approximately 10 seconds (can be changed in the configuration file). The first slice is the reference slice.
	 * When the slices list contains n slices, the latencies list will contain n-1 latencies.
	 * 
	 * @param slices A list of slices, each slice is an array of float values. The first slice
	 * acts as reference slice in the synchronization.
	 * @exception IllegalArgumentException Will be thrown when the slices list is empty.
	 * @return A list of latencies for each (non-reference) slice in comparison with the reference slice. When there is no
	 *         match found, NaN is added to the list.
	 */
	@Override
	public List<Float> findLatencies(List<float[]> slices) {
		if(slices.isEmpty()) {
			throw new IllegalArgumentException("The slices list can not be empty.");
		}
		final float fftHopSizesS = STEP_SIZE / (float) SAMPLE_RATE;
		final List<Float> latencies = new ArrayList<>();
		for (final int[] timing : synchronize(slices)) {
			if (timing.length > 0) {
				// Calculating the time difference from the time index
				latencies.add(timing[0] * fftHopSizesS - timing[1] * fftHopSizesS);
			} else {
				latencies.add(Float.NaN);
			}
		}
		return latencies;
	}
	
	/**
	 * 
	 * @param referenceHash The hashmap representation of the fingerprints from the reference stream (slice).
	 * @param otherHash The hashmap representation of the fingerprints from the other stream (slice).
	 * @return An array of integers containing information about the latencies:
	 * 			   Index 0: contains the time index of the minimum matching reference fingerprint.
	 *             Index 1: contains the time index of the minimum matching other fingerprint.
	 *             Index 2: contains the time index of the maximum matching reference fingerprint.
	 *             Index 3: contains the time index of the maximum matching other fingerprint.
	 *         The array is empty when there is no match found.
	 */
	private int[] fingerprintOffset(final HashMap<Integer, NFFTFingerprint> referenceHash, final HashMap<Integer, NFFTFingerprint> otherHash) {
		// key is the offset, value a list of fingerprint objects. Offset = time
		// between the two events
		HashMap<Integer, List<NFFTFingerprint>> mostPopularOffsets = new HashMap<Integer, List<NFFTFingerprint>>();
		int minimumAlignedMatchesThreshold = Config.getInt(Key.SYNC_MIN_ALIGNED_MATCHES);
		int maxAlignedOffsets = 0;
		List<NFFTFingerprint> bestMatchingPairs = null;

		// iterate each fingerprint in the reference stream
		for (Map.Entry<Integer, NFFTFingerprint> entry : referenceHash.entrySet()) {
			// if the fingerprint is also present in the other stream
			if (otherHash.containsKey(entry.getKey())) {

				NFFTFingerprint referenceFingerprint = entry.getValue();
				NFFTFingerprint otherFingerprint = otherHash.get(entry.getKey());
				int offset = referenceFingerprint.t1 - otherFingerprint.t1;
				// add the offset to the tree, if it is not already in the tree.
				if (!mostPopularOffsets.containsKey(offset)) {
					mostPopularOffsets.put(offset, new ArrayList<NFFTFingerprint>());
				}
				// add the reference and other fingerprint to the list.
				// add the other fingerprint to the list.
				// the reference fingerprints are at even, the other at odd
				// indexes.
				mostPopularOffsets.get(offset).add(referenceFingerprint);
				mostPopularOffsets.get(offset).add(otherFingerprint);

				// keep a max count
				if (mostPopularOffsets.get(offset).size() / 2 > maxAlignedOffsets) {
					bestMatchingPairs = mostPopularOffsets.get(offset);
					maxAlignedOffsets = bestMatchingPairs.size() / 2;
				}
			}
		}

		//The number of matches is under the threshold.
		if (maxAlignedOffsets < minimumAlignedMatchesThreshold) {
			return new int[] {};
		}

		int minReferenceFingerprintTimeIndex = Integer.MAX_VALUE;
		int minOtherFingerprintTimeIndex = Integer.MAX_VALUE;
		int maxReferenceFingerprintTimeIndex = Integer.MIN_VALUE;
		int maxOtherFingerprintTimeIndex = Integer.MIN_VALUE;
		// find where the offset matches start and stop
		for (int i = 0; i < bestMatchingPairs.size(); i += 2) {
			final NFFTFingerprint refFingerprint = bestMatchingPairs.get(i);
			final NFFTFingerprint otherFingerprint = bestMatchingPairs.get(i + 1);
			minReferenceFingerprintTimeIndex = Math.min(refFingerprint.t1, minReferenceFingerprintTimeIndex);
			minOtherFingerprintTimeIndex = Math.min(otherFingerprint.t1, minOtherFingerprintTimeIndex);
			maxReferenceFingerprintTimeIndex = Math.max(refFingerprint.t1, maxReferenceFingerprintTimeIndex);
			maxOtherFingerprintTimeIndex = Math.max(otherFingerprint.t1, maxOtherFingerprintTimeIndex);
		}

		return new int[] { minReferenceFingerprintTimeIndex, minOtherFingerprintTimeIndex,
				maxReferenceFingerprintTimeIndex, maxOtherFingerprintTimeIndex };
	}

	/**
	 * Convert a list of fingerprints to the a hashmap of fingerprints.
	 * @param fingerprints The list of fingerprints.
	 * @return The fingerprints in the hashmap.
	 */
	private HashMap<Integer, NFFTFingerprint> fingerprintsToHash(final List<NFFTFingerprint> fingerprints) {
		final HashMap<Integer, NFFTFingerprint> hash = new HashMap<>();
		for (final NFFTFingerprint fingerprint : fingerprints) {
			hash.put(fingerprint.hash(), fingerprint);
		}
		return hash;
	}

	/**
	 * This helper method extracts the fingerprints, filters the fingerprints and returns the
	 * hashmap from the filtered fingerprints.
	 * @param dispatcher The AudioDispatcher from the stream.
	 * @return A hashmap containing the fingerprints.
	 */
	private HashMap<Integer, NFFTFingerprint> getFingerprintData(final AudioDispatcher dispatcher) {
		return fingerprintsToHash(filterPrints(extractFingerprints(dispatcher)));
	}

	public List<int[]> synchronize(final List<float[]> slices) {
		List<int[]> result = new ArrayList<>();
		try {
			List<AudioDispatcher> dispatchers = new ArrayList<>();
			for(float[] f : slices) {
				dispatchers.add(AudioDispatcherFactory.fromFloatArray(f, SAMPLE_RATE, SIZE, STEP_SIZE));
			}
			final HashMap<Integer, NFFTFingerprint> ref = getFingerprintData(dispatchers.remove(0));
			for (final AudioDispatcher dispatcher : dispatchers) {
				final HashMap<Integer, NFFTFingerprint> other = getFingerprintData(dispatcher);
				final int timingData[] = fingerprintOffset(ref, other);
				result.add(timingData);
			}
		} 
		catch (UnsupportedAudioFileException e) {
			Log.log(Level.SEVERE, "An error occured while fingerprinting, please check this!", e);
		}
		return result;
	}
}
