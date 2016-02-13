package be.signalsync.core;

import java.util.List;

import be.panako.strategy.nfft.NFFTFingerprint;
import be.tarsos.dsp.AudioDispatcher;

public class Stream {
	private final AudioDispatcher dispatcher;
	private RealtimeFingerprinter fingerprinter;

	public Stream(final AudioDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	public Stream(final AudioDispatcher dispatcher, final RealtimeFingerprinter fingerprinter) {
		this(dispatcher);
		addFingerprinter(fingerprinter);
	}

	public void addFingerprinter(final RealtimeFingerprinter fingerprinter) {
		if (this.fingerprinter != null) {
			throw new IllegalArgumentException("Fingerprinter has already been set.");
		}
		dispatcher.addAudioProcessor(fingerprinter);
		this.fingerprinter = fingerprinter;
	}

	public AudioDispatcher getDispatcher() {
		return dispatcher;
	}

	public List<NFFTFingerprint> getNewFingerprints() {
		return fingerprinter.getNewFingerprints();
	}
}
