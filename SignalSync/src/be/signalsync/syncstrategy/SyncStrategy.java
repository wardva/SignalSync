package be.signalsync.syncstrategy;

import java.util.List;
import be.signalsync.util.Config;
import be.signalsync.util.Key;

public abstract class SyncStrategy {
	
	public static SyncStrategy getDefault() {
		final String name = Config.get(Key.LATENCY_ALGORITHM);
		switch (name) {
		case "fingerprint":
			return getDefaultFingerprintStrategy();
		case "crosscovariance":
			return getDefaultCrossCovariance();
		default:
			throw new IllegalArgumentException("Invalid latency algorithm in config file.");
		}
	}
	
	public static FingerprintSyncStrategy getDefaultFingerprintStrategy() {
		FingerprintSyncStrategy strategy = new FingerprintSyncStrategy(
			 Config.getInt(Key.SAMPLE_RATE), 
			 Config.getInt(Key.NFFT_BUFFER_SIZE), 
			 Config.getInt(Key.NFFT_STEP_SIZE), 
			 Config.getInt(Key.NFFT_EVENT_POINT_MIN_DISTANCE), 
			 Config.getInt(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT),
			 Config.getInt(Key.SYNC_MIN_ALIGNED_MATCHES));
		return strategy;
	}
	
	public static CrossCovarianceSyncStrategy getDefaultCrossCovariance() {
		return new CrossCovarianceSyncStrategy(
			 getDefaultFingerprintStrategy(),
			 Config.getInt(Key.SAMPLE_RATE), 
			 Config.getInt(Key.NFFT_BUFFER_SIZE), 
			 Config.getInt(Key.NFFT_STEP_SIZE),
			 Config.getInt(Key.CROSS_COVARIANCE_NUMBER_OF_TESTS),
			 Config.getInt(Key.CROSS_COVARIANCE_THRESHOLD));
	}

	public abstract List<Double> findLatencies(List<float[]> sliceSet);
}
