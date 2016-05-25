package be.signalsync.sync;

import java.util.ArrayList;
import java.util.List;

import be.signalsync.syncstrategy.LatencyResult;

public class Silencer {
	private double sampleRate;
	private int numberOfStreams;
	private List<Integer> previousLatencies;
	
	public Silencer(double sampleRate, int numberOfStreams) {
		this.sampleRate = sampleRate;
		this.numberOfStreams = numberOfStreams;
		this.previousLatencies = new ArrayList<>(numberOfStreams);
	}
	
	public List<Integer> getCorrections(List<LatencyResult> latencies) {
		if(latencies.size() != numberOfStreams) {
			throw new IllegalArgumentException("Invalid number of streams");
		}
		List<Integer> currentCorrections = new ArrayList<>();
		int highestCorrection = Integer.MIN_VALUE;
		int i = 0;
		for(LatencyResult latency : latencies) {
			int previousLatency = previousLatencies.get(i);
			int latencyInSamples = (int) Math.round(latency.getLatencyInSeconds() * sampleRate);
			int correction = previousLatency - latencyInSamples;
			if(correction > highestCorrection) {
				highestCorrection = correction;
			}
			currentCorrections.add(correction);
			previousLatencies.set(i, latencyInSamples);
			i++;
		}
		List<Integer> relativeCorrections = new ArrayList<>();
		for(int correction : currentCorrections) {
			relativeCorrections.add(highestCorrection - correction);
		}
		return relativeCorrections;
	}
}
