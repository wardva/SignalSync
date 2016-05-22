package be.signalsync.syncstrategy;

import java.util.List;
import be.signalsync.util.Config;
import be.signalsync.util.Key;

/**
 * Superclass for the synchronization algorithms.
 * This class contains an abstract method to start the synchronization
 * and two static methods for creation of the algorithms.
 * @author Ward Van Assche
 */
public abstract class SyncStrategy {
	
	/**
	 * Create the default synchronization algorithm object.
	 * This method uses the configuration file to determine
	 * the prefered algorithm (key: LATENCY_ALGORITHM).
	 * @return
	 */
	public static SyncStrategy createDefault() {
		final String name = Config.get(Key.LATENCY_ALGORITHM);
		switch (name) {
		case "fingerprint":
			return createFingerprint();
		case "crosscovariance":
			return createCrossCovariance();
		default:
			throw new IllegalArgumentException("Invalid latency algorithm in config file.");
		}
	}
	
	/**
	 * This method creates a FingerPrintSyncStrategy object by
	 * using the default parameters. The default parameters are
	 * determined by using the configuration file.
	 */
	public static FingerprintSyncStrategy createFingerprint() {
		FingerprintSyncStrategy strategy = new FingerprintSyncStrategy(
			 Config.getInt(Key.SAMPLE_RATE), 
			 Config.getInt(Key.NFFT_BUFFER_SIZE), 
			 Config.getInt(Key.NFFT_STEP_SIZE), 
			 Config.getInt(Key.NFFT_EVENT_POINT_MIN_DISTANCE), 
			 Config.getInt(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT),
			 Config.getInt(Key.SYNC_MIN_ALIGNED_MATCHES));
		return strategy;
	}
	
	/**
	 * This method creates a CrossCovarianceSyncStrategy object by
	 * using the default parameters. The default parameters are
	 * determined by using the configuration file.
	 */
	public static CrossCovarianceSyncStrategy createCrossCovariance() {
		return new CrossCovarianceSyncStrategy(
			 createFingerprint(),
			 Config.getInt(Key.SAMPLE_RATE), 
			 Config.getInt(Key.NFFT_BUFFER_SIZE), 
			 Config.getInt(Key.NFFT_STEP_SIZE),
			 Config.getInt(Key.CROSS_COVARIANCE_NUMBER_OF_TESTS),
			 Config.getInt(Key.CROSS_COVARIANCE_THRESHOLD));
	}

	/**
	 * This method has to be implemented by a any algorithm subclass and
	 * should return a List containing the latency object of non-reference slice.
	 * 
	 * @param sliceSet A List of sample arrays. Each sample array contains
	 * the samples of a stream slice. The first sample array is the reference slice.
	 * @return A List of timestamps in seconds where the slices are synchronized.
	 */
	public abstract List<LatencyResult> findLatencies(List<float[]> sliceSet);
}
