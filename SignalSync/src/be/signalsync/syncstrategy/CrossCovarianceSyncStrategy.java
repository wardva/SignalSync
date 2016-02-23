package be.signalsync.syncstrategy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.UnsupportedAudioFileException;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class CrossCovarianceSyncStrategy extends SyncStrategy {
	private static Logger Log = Logger.getLogger(Config.get(Key.APPLICATION_NAME));
	private final FingerprintSyncStrategy fingerprinter;
	private static final int SAMPLE_RATE = Config.getInt(Key.SAMPLE_RATE);
	private static final int SIZE = Config.getInt(Key.BUFFER_SIZE);
	private static final int STEP_SIZE = Config.getInt(Key.STEP_SIZE);
	private static final int OVERLAP = SIZE - STEP_SIZE;

	protected CrossCovarianceSyncStrategy() {
		fingerprinter = new FingerprintSyncStrategy();
	}

	private int bestCrossCovarianceLag(final float[] reference, final float[] target) {
		double maxCovariance = Double.NEGATIVE_INFINITY;
		int maxCovarianceIndex = -1;
		for (int lag = 0; lag < reference.length; ++lag) {
			final double covariance = covariance(reference, target, lag);
			if (covariance > maxCovariance) {
				maxCovarianceIndex = lag;
				maxCovariance = covariance;
			}
		}
		return maxCovarianceIndex;
	}

	private double covariance(final float[] reference, final float[] target, final int lag) {
		double covariance = 0.0;
		for (int i = 0; i < reference.length; i++) {
			final int targetIndex = (i + lag) % reference.length;
			covariance += reference[i] * target[targetIndex];
		}
		return covariance;
	}

	@Override
	public List<Float> findLatencies(final List<float[]> slices) {
		final List<float[]> others = new ArrayList<>(slices);
		float[] reference = others.remove(0);
		
		final List<Float> results = new ArrayList<>();
		try {
			final List<int[]> fingerprintTimingData = fingerprinter.synchronize(slices);

			final Iterator<int[]> timingDataIterator = fingerprintTimingData.iterator();
			final Iterator<float[]> othersIterator = others.iterator();
			while (timingDataIterator.hasNext() && othersIterator.hasNext()) {
				final int[] timing = timingDataIterator.next();
				if (timing.length < 2) {
					results.add(Float.NaN);
				} 
				else {
					final float[] other = othersIterator.next();
					final double refined = refineMatchWithCrossCovariance(timing[0], timing[1], reference, other);
					results.add((float) refined);
				}
			}
		} 
		catch (UnsupportedAudioFileException e) {
			Log.log(Level.SEVERE, "An error occured while finding latency with crosscovariance, please check this!", e);
		}
		return results;
	}

	private double refineMatchWithCrossCovariance(final int referenceTime, final int otherTime, final float[] reference, final float[] other) throws UnsupportedAudioFileException {
		AudioDispatcher refDispatcher = AudioDispatcherFactory.fromFloatArray(reference, SAMPLE_RATE, SIZE, OVERLAP);
		AudioDispatcher otherDispatcher = AudioDispatcherFactory.fromFloatArray(other, SAMPLE_RATE, SIZE, OVERLAP);
		
		final int sampleRate = Config.getInt(Key.SAMPLE_RATE);
		final int bufferSize = Config.getInt(Key.BUFFER_SIZE);
		final int stepSize = Config.getInt(Key.STEP_SIZE);

		final float sizeS = bufferSize / (float) sampleRate;
		final float fftHopSizesS = stepSize / (float) sampleRate;

		final double referenceAudioToSkip = sizeS + referenceTime * fftHopSizesS;
		final double otherAudioToSkip = sizeS + otherTime * fftHopSizesS;

		final AudioSkipper referenceAudioSkipper = new AudioSkipper(referenceAudioToSkip);
		refDispatcher.addAudioProcessor(referenceAudioSkipper);
		refDispatcher.run();
		final double referenceAudioStart = referenceAudioSkipper.getAudioStart();
		final float[] referenceAudioFrame = referenceAudioSkipper.getAudioFrame();

		final AudioSkipper otherAudioSkipper = new AudioSkipper(otherAudioToSkip);
		otherDispatcher.addAudioProcessor(otherAudioSkipper);
		otherDispatcher.run();
		final double otherAudioStart = otherAudioSkipper.getAudioStart();
		final float[] otherAudioFrame = otherAudioSkipper.getAudioFrame();

		// lag in samples, determines how many samples the other audio frame
		// lags with respect to the reference audio frame.
		final int lag = bestCrossCovarianceLag(referenceAudioFrame, otherAudioFrame);

		// double offsetFramesInSeconds = (referenceTime - otherTime) *
		// fftHopSizesS;
		final double offsetStartEvent = referenceAudioStart - otherAudioStart;

		// lag in seconds
		final double offsetLagInSeconds1 = (bufferSize - lag) / (float) sampleRate;
		final double offsetLagInSeconds2 = lag / (float) sampleRate;

		// Happens when the fingerprint algorithm underestimated the real
		// latency
		final double offsetTotalInSeconds1 = offsetStartEvent + offsetLagInSeconds1;

		// Happens when the fingerprint algorithm overestimated the real latency
		final double offsetTotalInSeconds2 = offsetStartEvent - offsetLagInSeconds2;

		final double offsetFromMatching = (referenceTime - otherTime) * fftHopSizesS;

		// Calculating the difference between the fingerprint match and the
		// covariance results.
		final double dif1 = Math.abs(offsetTotalInSeconds1 - offsetFromMatching);
		final double dif2 = Math.abs(offsetTotalInSeconds2 - offsetFromMatching);

		// Check which results is the closest to the fingerprint match
		final double offsetTotalInSeconds = dif1 < dif2 ? offsetTotalInSeconds1 : offsetTotalInSeconds2;

		return offsetTotalInSeconds;

		 //lag is wrong if lag introduces a larger offset than algorithm:
/*		if(Math.abs(offsetFromMatching-offsetTotalInSeconds) >= 2*fftHopSizesS) { 
			System.err.println("Covariance lag incorrect!");
			return offsetFromMatching; 
		} 
		else { 
			System.err.println("Covariancelag is CORRECT!");
			return offsetTotalInSeconds; 
		}*/
	}
	
	private class AudioSkipper implements AudioProcessor {
		private final float[] audioFrame;
		private final double audioToSkip;
		private double audioStart;

		public AudioSkipper(final double audioToSkip) {
			this.audioToSkip = audioToSkip;
			audioFrame = new float[Config.getInt(Key.BUFFER_SIZE)];
			audioStart = 0;
		}

		public float[] getAudioFrame() {
			return audioFrame;
		}

		public double getAudioStart() {
			return audioStart;
		}

		@Override
		public boolean process(final AudioEvent audioEvent) {
			if (Math.abs(audioEvent.getTimeStamp() - audioToSkip) < 0.00001) {
				audioStart = audioEvent.getTimeStamp();
				final float[] buffer = audioEvent.getFloatBuffer();
				for (int i = 0; i < buffer.length; i++) {
					audioFrame[i] = buffer[i];
				}
				return false;
			}
			return true;
		}

		@Override
		public void processingFinished() {
		}
	}
}
