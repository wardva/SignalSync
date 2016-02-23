package be.signalsync.syncstrategy;

import java.util.List;

import be.signalsync.streamsets.StreamSet;
import be.signalsync.util.Config;
import be.signalsync.util.Key;

public abstract class SyncStrategy {
	private static SyncStrategy algorithm;
	private static String currentStrategy;

	public static SyncStrategy getInstance() {
		String name = Config.get(Key.LATENCY_ALGORITHM);
		if(algorithm != null && currentStrategy.equalsIgnoreCase(name)) {
			return algorithm;
		}
		switch (name) {
			case "fingerprint":
				algorithm = new FingerprintSyncStrategy();
				break;
			case "crosscovariance":
				algorithm = new CrossCovarianceSyncStrategy();
				break;
			default:
				throw new IllegalArgumentException("Invalid latency algorithm in config file.");
		}
		currentStrategy = name;
		return algorithm;
	}
		

	public abstract List<Float> findLatencies(StreamSet sliceSet);
}
