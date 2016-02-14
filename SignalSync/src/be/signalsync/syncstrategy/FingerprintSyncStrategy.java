package be.signalsync.syncstrategy;

import java.util.ArrayList;
import java.util.List;

import be.panako.strategy.nfft.NFFTEventPointProcessor;
import be.panako.strategy.nfft.NFFTFingerprint;
import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class FingerprintSyncStrategy extends SyncStrategy {
	private static final float MIN_FREQUENCY = 100;//hz
	private static final float MAX_FREQUENCY = 4000;//hz
	
	protected FingerprintSyncStrategy() {}
	
	/**
	 * This method calculates the latencies between the different audio slices
	 * using the existing NFFT fingerprint method. The code inside this method
	 * is copy pasted from the class NFFTStreamSync from the Panako library.
	 * This because the existing methods from the library are not 100% compatible
	 * with this use case.
	 */
	@Override
	public List<Integer> findLatencies(List<AudioDispatcher> slices) {
		return null;
	}
	
	/**
	 * Extract the fingerprints from the given audio file.
	 */
	private List<NFFTFingerprint> extractFingerprints(AudioDispatcher d){
		int samplerate = Config.getInt("SAMPLE_RATE");
		int size = Config.getInt("BUFFER_SIZE");
		int overlap = size - Config.getInt("BUFFER_OVERLAP");
		final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size,overlap,samplerate);
		d.addAudioProcessor(minMaxProcessor);
		d.run();
		return new ArrayList<NFFTFingerprint>(minMaxProcessor.getFingerprints());
	}
}
