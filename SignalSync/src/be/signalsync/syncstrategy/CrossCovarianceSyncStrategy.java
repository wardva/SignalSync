package be.signalsync.syncstrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
	private static final int BUFFER_SIZE = Config.getInt(Key.BUFFER_SIZE);
	private static final int STEP_SIZE = Config.getInt(Key.STEP_SIZE);
	private static final int OVERLAP = BUFFER_SIZE - STEP_SIZE;

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
					final int numberOfTests = 10;
					final int step = BUFFER_SIZE / numberOfTests;
					final int fingerPrintLatency = timing[1] - timing[0];
					final float fftHopSizesS = STEP_SIZE / (float) SAMPLE_RATE;
					double offsetFromMatching = -fingerPrintLatency * fftHopSizesS;

					Map<Double, Integer> refinedResult = new HashMap<Double, Integer>();
					for(int position = 0; position<BUFFER_SIZE - fingerPrintLatency; position+=step) {
						double refined = refineMatchWithCrossCovariance(position, position+fingerPrintLatency, reference, other);
						int value = refinedResult.get(refined);
						refinedResult.put(refined, value+1);
						
						//TODO: systeem bedenken om waardeloze resultaten (zelfde als fingerprint resultaat) er uit te filteren
						//TODO: verschillende malen testen, beste resultaat er uit halen
						System.out.println(refined);
					}
					results.add((float) refined);
				}
			}
		} 
		catch (UnsupportedAudioFileException e) {
			Log.log(Level.SEVERE, "An error occured while finding latency with crosscovariance, please check this!", e);
		}
		return results;
	}

	private Double refineMatchWithCrossCovariance(final int referenceTime, final int otherTime, final float[] reference, final float[] other) throws UnsupportedAudioFileException {
		AudioDispatcher refDispatcher = AudioDispatcherFactory.fromFloatArray(reference, SAMPLE_RATE, BUFFER_SIZE, OVERLAP);
		AudioDispatcher otherDispatcher = AudioDispatcherFactory.fromFloatArray(other, SAMPLE_RATE, BUFFER_SIZE, OVERLAP);

		final float sizeS = BUFFER_SIZE / (float) SAMPLE_RATE;
		final float fftHopSizesS = STEP_SIZE / (float) SAMPLE_RATE;

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

		final double offsetFromMatching = (referenceTime - otherTime) * fftHopSizesS;
		
		if(lag == 0) {
			System.err.println("Lag is 0!");
			return null;
		}
		
		// double offsetFramesInSeconds = (referenceTime - otherTime) *
		// fftHopSizesS;
		final double offsetStartEvent = referenceAudioStart - otherAudioStart;

		// lag in seconds
		final double offsetLagInSeconds1 = (BUFFER_SIZE - lag) / (float) SAMPLE_RATE;
		final double offsetLagInSeconds2 = lag / (float) SAMPLE_RATE;

		// Happens when the fingerprint algorithm underestimated the real
		// latency
		final double offsetTotalInSeconds1 = offsetStartEvent + offsetLagInSeconds1;

		// Happens when the fingerprint algorithm overestimated the real latency
		final double offsetTotalInSeconds2 = offsetStartEvent - offsetLagInSeconds2;

		// Calculating the difference between the fingerprint match and the
		// covariance results.
		final double dif1 = Math.abs(offsetTotalInSeconds1 - offsetFromMatching);
		final double dif2 = Math.abs(offsetTotalInSeconds2 - offsetFromMatching);

		// Check which results is the closest to the fingerprint match
		final double offsetTotalInSeconds = dif1 < dif2 ? offsetTotalInSeconds1 : offsetTotalInSeconds2;

		if(Math.abs(offsetFromMatching-offsetTotalInSeconds) >= fftHopSizesS) { 
			System.err.println("Covariance lag incorrect!");
			return null; 
		} 
		else { 
			System.err.println("Covariancelag is CORRECT!");
			return offsetTotalInSeconds; 
		}
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
