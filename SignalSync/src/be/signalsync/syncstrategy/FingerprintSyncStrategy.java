package be.signalsync.syncstrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.panako.strategy.nfft.NFFTEventPointProcessor;
import be.panako.strategy.nfft.NFFTFingerprint;
import be.panako.util.Key;
import be.signalsync.core.StreamSet;
import be.signalsync.core.SyncData;
import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;

public class FingerprintSyncStrategy extends SyncStrategy {
	private static Logger Log = Logger.getLogger(Config.get("APPLICATION_NAME"));
	
	private static final float MIN_FREQUENCY = 100;
	private static final float MAX_FREQUENCY = 4000;
	
	protected FingerprintSyncStrategy() {
		be.panako.util.Config.set(Key.NFFT_EVENT_POINT_MIN_DISTANCE, 
				Config.get("NFFT_EVENT_POINT_MIN_DISTANCE"));
		be.panako.util.Config.set(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT, 
				Config.get("NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT"));
	}
	
	/**
	 * This method calculates the latencies between the different audio slices
	 * using the existing NFFT fingerprint method. The code inside this method
	 * is copy pasted from the class NFFTStreamSync from the Panako library.
	 * This because the existing methods from the library are not 100% compatible
	 * with this use case.
	 */
	@Override
	public SyncData findLatencies(StreamSet sliceSet) {
		List<NFFTFingerprint> referenceFingerprints = extractFingerprints(sliceSet.getReference());
		filterPrints(referenceFingerprints);
		List<List<NFFTFingerprint>> otherFingerprints = new ArrayList<>();
		for(AudioDispatcher dispatcher : sliceSet.getOthers()) {
			List<NFFTFingerprint> fingerprints = extractFingerprints(dispatcher);
			filterPrints(fingerprints);
			otherFingerprints.add(fingerprints);
		}
		
		return findMatches(referenceFingerprints, otherFingerprints);
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
	
	/**
	 * Filter the fingerprints.
	 * @param prints The fingerprints to filter.
	 */
	private void filterPrints(List<NFFTFingerprint> prints){
		float samplerate = Config.getInt("SAMPLE_RATE");
		float size = Config.getInt("BUFFER_SIZE");
		int minf = (int) Math.ceil(MIN_FREQUENCY / (samplerate/size));
		int maxf = (int) Math.floor(MAX_FREQUENCY / (samplerate/size));
		int numberRemoved = 0;
		int prevSize = prints.size();
		for(int i = 0 ; i< prints.size();i++){
			NFFTFingerprint print = prints.get(i);
			boolean smallerThanMin = print.f1 <= minf || print.f2 <= minf;
			boolean biggerThanMax = print.f1 >= maxf || print.f2 >= maxf;
			
			if(smallerThanMin || biggerThanMax){
				prints.remove(i);
				i--;
				numberRemoved++;
			}
		}
		Log.log(Level.INFO, String.format("Filtered %d of prevSize %d", numberRemoved, prevSize));
	}
	
	private SyncData findMatches(List<NFFTFingerprint> reference, List<List<NFFTFingerprint>> other) {
		//create a map with the fingerprint hash as key, and fingerprint object as value.
		//Warning: only a single object is kept for each hash.
		HashMap<Integer, NFFTFingerprint> referenceHash = fingerprintsToHash(reference);
		SyncData result = new SyncData();
		int otherIndex = 0;
		for(List<NFFTFingerprint> otherPrint: other){
			HashMap<Integer, NFFTFingerprint> otherHash = fingerprintsToHash(otherPrint);
			result.addResult(fingerprintOffset(referenceHash,otherHash,otherIndex));
			otherIndex++;
		}
		return result;
	}
	
	private float[] fingerprintOffset(HashMap<Integer, NFFTFingerprint> referenceHash, HashMap<Integer, NFFTFingerprint> otherHash, int otherIndex) {
		//key is the offset, value a list of fingerprint objects. Offset = time between the two events
		HashMap<Integer,List<NFFTFingerprint>> mostPopularOffsets = new HashMap<Integer,List<NFFTFingerprint>>();
		int minimumAlignedMatchesThreshold = Config.getInt("MIN_ALIGNED_MATCHES");
		int maxAlignedOffsets = 0;
		List<NFFTFingerprint> bestMatchingPairs = null;
		
		//iterate each fingerprint in the reference stream 
		for(Map.Entry<Integer,NFFTFingerprint> entry : referenceHash.entrySet()){
			//if the fingerprint is also present in the other stream
			if(otherHash.containsKey(entry.getKey())){
				
				NFFTFingerprint referenceFingerprint = entry.getValue();
				NFFTFingerprint otherFingerprint = otherHash.get(entry.getKey());
				int offset = referenceFingerprint.t1 - otherFingerprint.t1;
				// add the offset to the tree, if it is not already in the tree.
				if(!mostPopularOffsets.containsKey(offset)){
					mostPopularOffsets.put(offset, new ArrayList<NFFTFingerprint>());
				}
				//add the reference and other fingerprint to the list.
				//add the other fingerprint to the list.
				//the reference fingerprints are at even, the other at odd indexes.
				mostPopularOffsets.get(offset).add(referenceFingerprint);
				mostPopularOffsets.get(offset).add(otherFingerprint);
				
				//keep a max count
				if(mostPopularOffsets.get(offset).size()/2 > maxAlignedOffsets){
					bestMatchingPairs = mostPopularOffsets.get(offset);
					maxAlignedOffsets = bestMatchingPairs.size() / 2;
				}
			}	
		}
		
		float fftHopSizesS = Config.getInt("STEP_SIZE") / (float) Config.getInt("SAMPLE_RATE");
		
		if(maxAlignedOffsets < minimumAlignedMatchesThreshold) {
			return new float[] {};
		} 
		
		int minReferenceFingerprintTimeIndex = Integer.MAX_VALUE;
		int minOtherFingerprintTimeIndex = Integer.MAX_VALUE;
		int maxReferenceFingerprintTimeIndex = Integer.MIN_VALUE;
		int maxOtherFingerprintTimeIndex = Integer.MIN_VALUE;
		//find where the offset matches start and stop
		for(int i = 0 ; i < bestMatchingPairs.size() ;i+=2){
			NFFTFingerprint refFingerprint = bestMatchingPairs.get(i);
			NFFTFingerprint otherFingerprint = bestMatchingPairs.get(i+1);
			minReferenceFingerprintTimeIndex = Math.min(refFingerprint.t1,minReferenceFingerprintTimeIndex);
			minOtherFingerprintTimeIndex = Math.min(otherFingerprint.t1,minOtherFingerprintTimeIndex);
			maxReferenceFingerprintTimeIndex = Math.max(refFingerprint.t1,maxReferenceFingerprintTimeIndex);
			maxOtherFingerprintTimeIndex = Math.max(otherFingerprint.t1,maxOtherFingerprintTimeIndex);
		}			
		
		double startInReference = minReferenceFingerprintTimeIndex * fftHopSizesS;
		double startInMatchingStream = minOtherFingerprintTimeIndex * fftHopSizesS;
		double stopInReference = maxReferenceFingerprintTimeIndex * fftHopSizesS;
		double stopInMatchinStream = maxOtherFingerprintTimeIndex * fftHopSizesS;

		return new float[] { 
			(float) (startInReference - startInMatchingStream), 
			(float) (stopInReference - stopInMatchinStream) 
		};
	}
	
	private HashMap<Integer, NFFTFingerprint> fingerprintsToHash(List<NFFTFingerprint> fingerprints){
		HashMap<Integer, NFFTFingerprint> hash = new HashMap<>();
		for(NFFTFingerprint fingerprint : fingerprints){
			hash.put(fingerprint.hash(),fingerprint);
		}
		return hash;	
	}
}
