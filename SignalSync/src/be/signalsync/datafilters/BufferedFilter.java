package be.signalsync.datafilters;

import java.util.LinkedList;
import java.util.Queue;
import be.signalsync.util.Config;
import be.signalsync.util.Key;

public abstract class BufferedFilter implements DataFilter {
	protected Queue<Double> buffer;
	protected double currentValue;
	protected int bufferSize;
	protected int count;
	
	public BufferedFilter(int bufferSize) {
		this.count = 0;
		this.bufferSize = bufferSize;
		this.buffer = new LinkedList<Double>();
	}
	
	public BufferedFilter() {
		this(Config.getInt(Key.LATENCY_FILTER_BUFFER_SIZE));
	}
	
	@Override
	public double filter(double rawValue) {
		if(count++ == 0) {
			initializeBuffer(rawValue);
		}
		currentValue = calculateNext(rawValue);
		return currentValue;
	}
	
	protected void initializeBuffer(double rawValue) {
		currentValue = rawValue;
		for(int i = 0; i<bufferSize; i++) {
			buffer.add(rawValue);
		}
	}
	
	protected abstract double calculateNext(double rawValue);
}
