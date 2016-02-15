package be.signalsync.syncstrategy;

import be.signalsync.core.StreamSet;
import be.signalsync.core.SyncData;
import be.signalsync.util.Config;

public abstract class SyncStrategy {
	private static SyncStrategy algorithm;

	public static SyncStrategy getInstance() {
		if (algorithm == null) {
			switch (Config.get("LATENCY_ALGORITHM")) {
			case "fingerprint":
				algorithm = new FingerprintSyncStrategy();
				break;
			case "crossvariance":
				algorithm = new CrossVarianceSyncStrategy();
				break;
			default:
				throw new IllegalArgumentException("Invalid latency algorithm in config file.");
			}
		}
		return algorithm;
	}

	public abstract SyncData findLatencies(StreamSet sliceSet);
}
