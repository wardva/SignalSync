package be.signalsync.datafilters;

import java.util.LinkedList;
import java.util.Queue;
import be.signalsync.util.Config;
import be.signalsync.util.Key;

public abstract class BufferedFilter implements DataFilter {
	protected Queue<Double> buffer;
	protected double currentValue;
	protected int bufferSize;
	private boolean initialized;
	
	public BufferedFilter(int bufferSize) {
		this.initialized = false;
		this.bufferSize = bufferSize;
		this.buffer = new LinkedList<Double>();
	}
	
	@Override
	public double filter(double rawValue) {
		if(!initialized) {
			initializeBuffer(rawValue);
			initialized = true;
		}
		currentValue = calculateNext(rawValue);
		return currentValue;
	}
	
	private void initializeBuffer(double rawValue) {
		currentValue = rawValue;
		for(int i = 0; i<bufferSize; i++) {
			buffer.add(rawValue);
		}
	}
	
	protected abstract double calculateNext(double rawValue);
}
