package be.signalsync.syncstrategy;

import be.signalsync.core.StreamSet;
import be.signalsync.core.SyncData;

public class CrossVarianceSyncStrategy extends SyncStrategy {

	protected CrossVarianceSyncStrategy() {
	}

	@Override
	public SyncData findLatencies(final StreamSet sliceSet) {
		return null;
	}
}
