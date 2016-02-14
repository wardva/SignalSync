package be.signalsync.syncstrategy;

import java.util.List;

import be.tarsos.dsp.AudioDispatcher;

public class CrossVarianceSyncStrategy extends SyncStrategy {
	
	protected CrossVarianceSyncStrategy() {}
	
	@Override
	public List<Integer> findLatencies(List<AudioDispatcher> slices) {
		return null;
	}
}
