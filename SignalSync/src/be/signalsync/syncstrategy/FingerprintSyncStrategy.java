package be.signalsync.syncstrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

public class FingerprintSyncStrategy extends SyncStrategy {
	private static Logger Log = Logger.getLogger(Config.get(Key.APPLICATION_NAME));
	private static final float MIN_FREQUENCY = 100;
	private static final float MAX_FREQUENCY = 4000;
	private static final int SAMPLE_RATE = Config.getInt(Key.SAMPLE_RATE);
	private static final int SIZE = Config.getInt(Key.BUFFER_SIZE);
	private static final int STEP_SIZE = Config.getInt(Key.STEP_SIZE);
	private static final int OVERLAP = SIZE - STEP_SIZE;

	protected FingerprintSyncStrategy() {
		be.panako.util.Config.set(be.panako.util.Key.NFFT_EVENT_POINT_MIN_DISTANCE,
				Config.get(Key.NFFT_EVENT_POINT_MIN_DISTANCE));
		be.panako.util.Config.set(be.panako.util.Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT,
				Config.get(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT));
	}

	private List<NFFTFingerprint> extractFingerprints(final AudioDispatcher d) {
		final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(SIZE, OVERLAP, SAMPLE_RATE);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		d.removeAudioProcessor(minMaxProcessor);
		final List<NFFTFingerprint> fingerprints = new ArrayList<>(minMaxProcessor.getFingerprints());
		filterPrints(fingerprints);
		return fingerprints;
	}

	private List<NFFTFingerprint> filterPrints(final List<NFFTFingerprint> fingerprints) {
		final int minf = (int) Math.ceil(MIN_FREQUENCY / (SAMPLE_RATE / SIZE));
		final int maxf = (int) Math.floor(MAX_FREQUENCY / (SAMPLE_RATE / SIZE));
		for (int i = 0; i < fingerprints.size(); i++) {
			final NFFTFingerprint print = fingerprints.get(i);
			final boolean smallerThanMin = print.f1 <= minf || print.f2 <= minf;
			final boolean biggerThanMax = print.f1 >= maxf || print.f2 >= maxf;
			if (smallerThanMin || biggerThanMax) {
				fingerprints.remove(i);
				i--;
			}
		}
		return fingerprints;
	}

	@Override
	public List<Float> findLatencies(List<float[]> slices) {
		//Making a copy of the list
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

	private int[] fingerprintOffset(final HashMap<Integer, NFFTFingerprint> referenceHash, final HashMap<Integer, NFFTFingerprint> otherHash, final int otherIndex) {
		// key is the offset, value a list of fingerprint objects. Offset = time
		// between the two events
		final HashMap<Integer, List<NFFTFingerprint>> mostPopularOffsets = new HashMap<Integer, List<NFFTFingerprint>>();
		final int minimumAlignedMatchesThreshold = Config.getInt(Key.SYNC_MIN_ALIGNED_MATCHES);
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

	private HashMap<Integer, NFFTFingerprint> fingerprintsToHash(final List<NFFTFingerprint> fingerprints) {
		final HashMap<Integer, NFFTFingerprint> hash = new HashMap<>();
		for (final NFFTFingerprint fingerprint : fingerprints) {
			hash.put(fingerprint.hash(), fingerprint);
		}
		return hash;
	}

	private HashMap<Integer, NFFTFingerprint> getFingerprintData(final AudioDispatcher d) {
		return fingerprintsToHash(filterPrints(extractFingerprints(d)));
	}

	public List<int[]> synchronize(final List<float[]> slices) {
		List<int[]> result = new ArrayList<>();
		try {
			List<AudioDispatcher> dispatchers = new ArrayList<>();
			for(float[] f : slices) {
				dispatchers.add(AudioDispatcherFactory.fromFloatArray(f, SAMPLE_RATE, SIZE, STEP_SIZE));
			}
			final HashMap<Integer, NFFTFingerprint> ref = getFingerprintData(dispatchers.remove(0));
			int i = 0;
			for (final AudioDispatcher dispatcher : dispatchers) {
				final HashMap<Integer, NFFTFingerprint> other = getFingerprintData(dispatcher);
				final int timingData[] = fingerprintOffset(ref, other, i++);
				result.add(timingData);
			}
		} 
		catch (UnsupportedAudioFileException e) {
			Log.log(Level.SEVERE, "An error occured while fingerprinting, please check this!", e);
		}
		return result;
	}
}
