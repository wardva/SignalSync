package be.signalsync.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import be.signalsync.util.Config;
import be.signalsync.util.Key;

public class LatencyFilter {
	private List<double[]> circularBuffers;
	private List<Double> averages;
	private int circularIndex;
	private int count;
	
	public LatencyFilter(int buffer_size, int n) {
		this.count = 0;
		this.circularBuffers = new ArrayList<>(n);
		this.averages = new ArrayList<>(n);
		for(int i = 0; i<n; i++) {
			circularBuffers.add(new double[buffer_size]);
		}
	}
	
	public LatencyFilter(int n) {
		this(Config.getInt(Key.LATENCY_FILTER_BUFFER_SIZE), n);
	}
	
	public List<Double> push(List<Double> rawLatencies) {
		if(rawLatencies.size() != circularBuffers.size()) {
			throw new IllegalArgumentException("Invalid list size!");
		}
		if(count++ == 0) {
			initializeBuffers(rawLatencies);
		}
		List<Double> smoothedLatencies = new ArrayList<>(circularBuffers.size());
		Iterator<Double> latencyIt = rawLatencies.iterator();
		ListIterator<Double> averageIt = averages.listIterator();
		for(double[] buffer : circularBuffers) {
			double latency = latencyIt.next();
			double average = averageIt.next();
			double lastValue = buffer[circularIndex];
			average = average + ((latency - lastValue) / buffer.length);
			averageIt.set(average);
			buffer[circularIndex] = latency;
			circularIndex = (circularIndex + 1) % buffer.length;
			smoothedLatencies.add(average);
		}
		return smoothedLatencies;
	}
	
	private void initializeBuffers(List<Double> values) {
		if(values.size() != circularBuffers.size()) {
			throw new IllegalArgumentException("Invalid list size!");
		}
		Iterator<Double> valuesIt = values.iterator();
		for(double[] buffer : circularBuffers) {
			double value = valuesIt.next();
			Arrays.fill(buffer, value);
			averages.add(value);
		}
	}
}
