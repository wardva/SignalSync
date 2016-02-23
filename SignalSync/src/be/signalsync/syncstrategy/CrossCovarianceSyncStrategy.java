package be.signalsync.syncstrategy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import be.signalsync.streamsets.StreamSet;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class CrossCovarianceSyncStrategy extends SyncStrategy {
	private FingerprintSyncStrategy fingerprinter;
	
	protected CrossCovarianceSyncStrategy() {
		fingerprinter = new FingerprintSyncStrategy();
	}
	
	@Override
	public List<Float> findLatencies(StreamSet sliceSet) {
		List<Float> results = new ArrayList<>();
		List<int[]> fingerprintTimingData = fingerprinter.synchronize(sliceSet);
		sliceSet.reset();
		List<AudioDispatcher> others = new ArrayList<AudioDispatcher>(sliceSet.getStreams());
		AudioDispatcher reference = others.remove(0);
		
		Iterator<int[]> timingDataIterator = fingerprintTimingData.iterator();
		Iterator<AudioDispatcher> othersIterator = others.iterator();
		while(timingDataIterator.hasNext() && othersIterator.hasNext()) {
			int[] timing = timingDataIterator.next();
			if(timing.length < 2) {
				results.add(Float.NaN);
			}
			else {
				AudioDispatcher other = othersIterator.next();
				double refined = refineMatchWithCrossCovariance(timing[0], timing[1], reference, other);
				results.add((float) refined);
				sliceSet.reset();
				reference = new ArrayList<>(sliceSet.getStreams()).remove(0);
			}
		}
		return results;
	}
		
	private double refineMatchWithCrossCovariance(int referenceTime, int otherTime, AudioDispatcher reference, AudioDispatcher other){
		int sampleRate = Config.getInt(Key.SAMPLE_RATE);
		int bufferSize = Config.getInt(Key.BUFFER_SIZE);
		int stepSize = Config.getInt(Key.STEP_SIZE);
		
		float sizeS = bufferSize / (float) sampleRate;
		float fftHopSizesS = stepSize / (float) sampleRate;
		
		final double referenceAudioToSkip = sizeS + (referenceTime) * fftHopSizesS;
		final double otherAudioToSkip = sizeS + (otherTime) * fftHopSizesS;
		
		AudioSkipper referenceAudioSkipper = new AudioSkipper(referenceAudioToSkip);
		reference.addAudioProcessor(referenceAudioSkipper);
		reference.run();
		double referenceAudioStart = referenceAudioSkipper.getAudioStart();
		float[] referenceAudioFrame = referenceAudioSkipper.getAudioFrame();
		reference.removeAudioProcessor(referenceAudioSkipper);

		AudioSkipper otherAudioSkipper = new AudioSkipper(otherAudioToSkip);
		other.addAudioProcessor(otherAudioSkipper);	
		other.run();
		double otherAudioStart = otherAudioSkipper.getAudioStart();
		float[] otherAudioFrame = otherAudioSkipper.getAudioFrame();
		other.removeAudioProcessor(otherAudioSkipper);
		
		
		//lag in samples, determines how many samples the other audio frame
		//lags with respect to the reference audio frame.
		int lag = bestCrossCovarianceLag(referenceAudioFrame, otherAudioFrame);
		
		//double offsetFramesInSeconds = (referenceTime - otherTime) * fftHopSizesS;
		double offsetStartEvent = referenceAudioStart - otherAudioStart;

		// lag in seconds
		double offsetLagInSeconds1 = (bufferSize - lag) / (float) sampleRate;
		double offsetLagInSeconds2 = lag / (float) sampleRate;

		// Happens when the fingerprint algorithm underestimated the real latency
		double offsetTotalInSeconds1 = offsetStartEvent + offsetLagInSeconds1; 
		
		// Happens when the fingerprint algorithm overestimated the real latency
		double offsetTotalInSeconds2 = offsetStartEvent - offsetLagInSeconds2; 
		
		double offsetFromMatching = (referenceTime - otherTime) * fftHopSizesS;

		// Calculating the difference between the fingerprint match and the
		// covariance results.
		double dif1 = Math.abs(offsetTotalInSeconds1 - offsetFromMatching);
		double dif2 = Math.abs(offsetTotalInSeconds2 - offsetFromMatching);

		// Check which results is the closest to the fingerprint match
		double offsetTotalInSeconds = dif1 < dif2 ? offsetTotalInSeconds1 : offsetTotalInSeconds2;
		
		return offsetTotalInSeconds;
		
		/*
		//lag is wrong if lag introduces a larger offset than algorithm:
 		if(Math.abs(offsetFromMatching-offsetTotalInSeconds)>= 2*fftHopSizesS) {
			System.err.println("Covariance lag incorrect!");
			return offsetFromMatching;
		} 
		else {
			System.err.println("Covariancelag is CORRECT!");
			return offsetTotalInSeconds;
		}
		*/
	}
	
	private int bestCrossCovarianceLag(float[] reference, float[] target) {
		double maxCovariance = Double.NEGATIVE_INFINITY;
		int maxCovarianceIndex = -1;
		for(int lag = 0; lag<reference.length; ++lag) {
			double covariance = covariance(reference, target, lag);
			if(covariance > maxCovariance) {
				maxCovarianceIndex = lag;
				maxCovariance = covariance;
			}
		}
		return maxCovarianceIndex;
	}
	
	private double covariance(float[] reference, float[] target,int lag) {
		double covariance = 0.0;
		for(int i = 0 ; i < reference.length; i++){
			int targetIndex = (i+lag)%reference.length;
			covariance += reference[i]*target[targetIndex];
		}
		return covariance;
	}

	private class AudioSkipper implements AudioProcessor {
		private float[] audioFrame;
		private double audioToSkip;
		private double audioStart;
		
		public AudioSkipper(double audioToSkip) {
			this.audioToSkip = audioToSkip;
			this.audioFrame = new float[Config.getInt(Key.BUFFER_SIZE)];
			this.audioStart = 0;
		}
		
		@Override
		public boolean process(AudioEvent audioEvent) {
			if(Math.abs(audioEvent.getTimeStamp() - audioToSkip) < 0.00001){
				this.audioStart = audioEvent.getTimeStamp();
				float [] buffer = audioEvent.getFloatBuffer();
				for(int i = 0 ; i < buffer.length; i++){
					audioFrame[i]=buffer[i];
				}
				return false;
			}
			return true;
		}

		public float[] getAudioFrame() {
			return audioFrame;
		}

		public double getAudioStart() {
			return audioStart;
		}

		@Override
		public void processingFinished() {}
	}
}
