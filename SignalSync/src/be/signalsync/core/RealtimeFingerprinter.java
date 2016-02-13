package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;

import be.panako.strategy.nfft.NFFTEventPointProcessor;
import be.panako.strategy.nfft.NFFTFingerprint;

/**
 * This class adds realtime functionality to the NFFTEventPointProcessor. With
 * this class you can request fingerprints from a stream which is still running.
 * 
 * @author Ward Van Assche
 */
public class RealtimeFingerprinter extends NFFTEventPointProcessor {
	public RealtimeFingerprinter(final int size, final int overlap, final int sampleRate) {
		super(size, overlap, sampleRate);
	}

	/**
	 * This method returns the available fingerprints. After this method, the
	 * returned fingerprints are removed from the fingerprinter.
	 * 
	 * @return A list containing the new fingerprints.
	 */
	public List<NFFTFingerprint> getNewFingerprints() {
		processingFinished();
		final List<NFFTFingerprint> newFingerprints = new ArrayList<>(getFingerprints());
		getFingerprints().clear();
		getEventPoints().clear();
		return newFingerprints;
	}
}
