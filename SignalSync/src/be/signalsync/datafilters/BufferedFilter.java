package be.signalsync.datafilters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import be.signalsync.util.Config;
import be.signalsync.util.Key;

public abstract class BufferedFilter implements DataFilter {
	protected List<Queue<Double>> circularBuffers;
	protected List<Double> values;
	protected int circularIndex;
	protected int n;
	protected int bufferSize;
	protected int count;
	
	public BufferedFilter(int bufferSize, int n) {
		this.count = 0;
		this.n = n;
		this.bufferSize = bufferSize;
		this.values = new ArrayList<>(n);
		this.circularBuffers = new ArrayList<>(n);
		createBuffers();
	}
	
	public BufferedFilter(int n) {
		this(Config.getInt(Key.LATENCY_FILTER_BUFFER_SIZE), n);
	}
	
	@Override
	public List<Double> filter(List<Double> data) {
		if(data.size() != circularBuffers.size()) {
			throw new IllegalArgumentException("Invalid list size!");
		}
		if(count++ == 0) {
			initializeBuffers(data);
		}
		List<Double> smoothedLatencies = new ArrayList<>(circularBuffers.size());
		for(int i = 0; i < n; i++) {
			Double filteredValue = calculateNext(i, data.get(i));
			values.set(i, filteredValue);
			smoothedLatencies.add(filteredValue);
		}
		return smoothedLatencies;
	}
	
	protected void initializeBuffers(List<Double> rawValues) {
		if(rawValues.size() != circularBuffers.size()) {
			throw new IllegalArgumentException("Invalid list size!");
		}
		Iterator<Double> rawValuesIt = rawValues.iterator();
		
		for(Queue<Double> buffer : circularBuffers) {
			Double rawValue = rawValuesIt.next();
			values.add(rawValue);
			for(int i = 0; i<bufferSize; i++) {
				buffer.add(rawValue);
			}
		}
	}
	
	protected abstract void createBuffers();
	
	protected abstract Double calculateNext(int index, Double rawValue);
}
