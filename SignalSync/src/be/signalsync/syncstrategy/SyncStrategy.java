package be.signalsync.syncstrategy;

import java.util.List;

import be.signalsync.streamsets.StreamSet;
import be.signalsync.util.Config;

public abstract class SyncStrategy {
	private static SyncStrategy algorithm;

	public static SyncStrategy getInstance() {
		if (algorithm == null) {
			switch (Config.get("LATENCY_ALGORITHM")) {
			case "fingerprint":
				algorithm = new FingerprintSyncStrategy();
				break;
			case "crosscovariance":
				algorithm = new CrossCovarianceSyncStrategy();
				break;
			default:
				throw new IllegalArgumentException("Invalid latency algorithm in config file.");
			}
		}
		return algorithm;
	}

	public abstract List<Float> findLatencies(StreamSet sliceSet);
}
