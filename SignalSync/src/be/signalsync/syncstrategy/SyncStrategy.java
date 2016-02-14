package be.signalsync.syncstrategy;

import java.util.List;

import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;

public abstract class SyncStrategy {
	public abstract List<Integer> findLatencies(List<AudioDispatcher> slices);
	
	private static SyncStrategy algorithm;	
	public static SyncStrategy getInstance(){
		if(algorithm == null){
			switch(Config.get("LATENCY_ALGORITHM")) {
				case "fingerprint" : 
					algorithm = new FingerprintSyncStrategy();
				case "crossvariance" :
					algorithm = new CrossVarianceSyncStrategy();
				default : 
					throw new IllegalArgumentException("Invalid latency algorithm in config file.");
			}
		}
		return algorithm;
	}
}
