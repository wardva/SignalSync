package be.signalsync.core;

import java.util.List;

import be.panako.strategy.nfft.NFFTFingerprint;
import be.tarsos.dsp.AudioDispatcher;

public interface StreamSupply extends Runnable {
	AudioDispatcher getReference();
	List<AudioDispatcher> getStreams();
	List<AudioDispatcher> getSlices();
}
