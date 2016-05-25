package be.signalsync.datafilters;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Superclass for any datafilter which calculates the current value
 * from a buffer containing the last results.
 * @author Ward Van Assche
 */
public abstract class BufferedFilter implements DataFilter {
	protected Queue<Double> buffer;
	protected double currentValue;
	protected int bufferSize;
	private boolean initialized;
	
	/**
	 * @param bufferSize The size of the buffer. The buffer
	 * will contain the last n elements where n is the bufferSize.
	 */
	public BufferedFilter(int bufferSize) {
		this.initialized = false;
		this.bufferSize = bufferSize;
		this.buffer = new LinkedList<Double>();
	}
	
	/**
	 * Filter a raw value. This method uses the abstract method 'calculateNext'.
	 * @return The filtered value.
	 */
	@Override
	public double filter(double rawValue) {
		if(!initialized) {
			initializeBuffer(rawValue);
			initialized = true;
		}
		currentValue = calculateNext(rawValue);
		return currentValue;
	}
	
	/**
	 * This method fills the buffer with the rawValue parameter.
	 */
	private void initializeBuffer(double rawValue) {
		currentValue = rawValue;
		for(int i = 0; i<bufferSize; i++) {
			buffer.add(rawValue);
		}
	}
	
	/**
	 * This method should be implemented by a subclass. The implementation
	 * of this method determines how a raw value is filtered.
	 * @param rawValue The raw value
	 * @return The filtered value
	 */
	protected abstract double calculateNext(double rawValue);
}
