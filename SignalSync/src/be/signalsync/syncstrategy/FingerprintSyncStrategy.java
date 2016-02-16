package be.signalsync.syncstrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.panako.strategy.nfft.NFFTEventPointProcessor;
import be.panako.strategy.nfft.NFFTFingerprint;
import be.panako.util.Key;
import be.signalsync.core.StreamSet;
import be.signalsync.core.SyncData;
import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;

/**
 * This class is responsible for generating synchronization data from a
 * StreamSet using the Panako accoustic fingerprinting algorithm. The algorithm
 * is copy-pasted in this class because the existing methods from the Panako
 * library were not 100% compatible with this use case.
 *
 * @author Ward Van Assche
 *
 */
public class FingerprintSyncStrategy extends SyncStrategy {
	private static final float MIN_FREQUENCY = 100;
	private static final float MAX_FREQUENCY = 4000;

	protected FingerprintSyncStrategy() {
		be.panako.util.Config.set(Key.NFFT_EVENT_POINT_MIN_DISTANCE, 
				Config.get("NFFT_EVENT_POINT_MIN_DISTANCE"));
		be.panako.util.Config.set(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT,
				Config.get("NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT"));
	}

	/**
	 * Extract the fingerprints from the given audio file.
	 */
	private List<NFFTFingerprint> extractFingerprints(final AudioDispatcher d) {
		final int samplerate = Config.getInt("SAMPLE_RATE");
		final int size = Config.getInt("BUFFER_SIZE");
		final int overlap = size - Config.getInt("BUFFER_OVERLAP");
		final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size, overlap, samplerate);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		return new ArrayList<NFFTFingerprint>(minMaxProcessor.getFingerprints());
	}

	/**
	 * Filter the fingerprints.
	 * 
	 * @param prints
	 *            The fingerprints to filter.
	 */
	private void filterPrints(final List<NFFTFingerprint> prints) {
		final float samplerate = Config.getInt("SAMPLE_RATE");
		final float size = Config.getInt("BUFFER_SIZE");
		final int minf = (int) Math.ceil(MIN_FREQUENCY / (samplerate / size));
		final int maxf = (int) Math.floor(MAX_FREQUENCY / (samplerate / size));
		int numberRemoved = 0;
		final int prevSize = prints.size();
		for (int i = 0; i < prints.size(); i++) {
			final NFFTFingerprint print = prints.get(i);
			final boolean smallerThanMin = print.f1 <= minf || print.f2 <= minf;
			final boolean biggerThanMax = print.f1 >= maxf || print.f2 >= maxf;

			if (smallerThanMin || biggerThanMax) {
				prints.remove(i);
				i--;
				numberRemoved++;
			}
		}
		//Log.log(Level.INFO, String.format("Filtered %d of prevSize %d", numberRemoved, prevSize));
	}

	/**
	 * This method calculates the latencies between the different audio slices
	 * using the existing NFFT fingerprint method.
	 */
	@Override
	public SyncData findLatencies(final StreamSet sliceSet) {
		final List<NFFTFingerprint> referenceFingerprints = extractFingerprints(sliceSet.getReference());
		filterPrints(referenceFingerprints);
		final List<List<NFFTFingerprint>> otherFingerprints = new ArrayList<>();
		for (final AudioDispatcher dispatcher : sliceSet.getOthers()) {
			final List<NFFTFingerprint> fingerprints = extractFingerprints(dispatcher);
			filterPrints(fingerprints);
			otherFingerprints.add(fingerprints);
		}

		return findMatches(referenceFingerprints, otherFingerprints);
	}

	/**
	 * This method starts the synchronization using the already generated
	 * fingerprints of the different streams.
	 *
	 * @param reference
	 *            A list of fingerprints of the reference stream.
	 * @param other
	 *            A list containing for each other stream a list of
	 *            fingerprints.
	 * @return The synchronization result wrapped in a SyncData object.
	 */
	private SyncData findMatches(final List<NFFTFingerprint> reference, final List<List<NFFTFingerprint>> other) {
		// create a map with the fingerprint hash as key, and fingerprint object
		// as value.
		// Warning: only a single object is kept for each hash.
		final HashMap<Integer, NFFTFingerprint> referenceHash = fingerprintsToHash(reference);
		final SyncData result = new SyncData();
		int otherIndex = 0;
		for (final List<NFFTFingerprint> otherPrint : other) {
			final HashMap<Integer, NFFTFingerprint> otherHash = fingerprintsToHash(otherPrint);
			result.addResult(fingerprintOffset(referenceHash, otherHash, otherIndex));
			otherIndex++;
		}
		return result;
	}

	private float[] fingerprintOffset(final HashMap<Integer, NFFTFingerprint> referenceHash,
			final HashMap<Integer, NFFTFingerprint> otherHash, final int otherIndex) {
		// key is the offset, value a list of fingerprint objects. Offset = time
		// between the two events
		final HashMap<Integer, List<NFFTFingerprint>> mostPopularOffsets = new HashMap<Integer, List<NFFTFingerprint>>();
		final int minimumAlignedMatchesThreshold = Config.getInt("MIN_ALIGNED_MATCHES");
		int maxAlignedOffsets = 0;
		List<NFFTFingerprint> bestMatchingPairs = null;

		// iterate each fingerprint in the reference stream
		for (final Map.Entry<Integer, NFFTFingerprint> entry : referenceHash.entrySet()) {
			// if the fingerprint is also present in the other stream
			if (otherHash.containsKey(entry.getKey())) {

				final NFFTFingerprint referenceFingerprint = entry.getValue();
				final NFFTFingerprint otherFingerprint = otherHash.get(entry.getKey());
				final int offset = referenceFingerprint.t1 - otherFingerprint.t1;
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

		final float fftHopSizesS = Config.getInt("STEP_SIZE") / (float) Config.getInt("SAMPLE_RATE");

		if (maxAlignedOffsets < minimumAlignedMatchesThreshold) {
			return new float[] {};
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

		final double startInReference = minReferenceFingerprintTimeIndex * fftHopSizesS;
		final double startInMatchingStream = minOtherFingerprintTimeIndex * fftHopSizesS;
		final double stopInReference = maxReferenceFingerprintTimeIndex * fftHopSizesS;
		final double stopInMatchinStream = maxOtherFingerprintTimeIndex * fftHopSizesS;

		return new float[] { (float) (startInReference - startInMatchingStream),
				(float) (stopInReference - stopInMatchinStream) };
	}

	private HashMap<Integer, NFFTFingerprint> fingerprintsToHash(final List<NFFTFingerprint> fingerprints) {
		final HashMap<Integer, NFFTFingerprint> hash = new HashMap<>();
		for (final NFFTFingerprint fingerprint : fingerprints) {
			hash.put(fingerprint.hash(), fingerprint);
		}
		return hash;
	}
}
