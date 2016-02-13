package be.signalsync.core;

import java.util.List;

import be.panako.strategy.nfft.NFFTFingerprint;

public interface SyncStrategy {
	List<Integer> calculateLatency(List<NFFTFingerprint> fingerprints);
}
