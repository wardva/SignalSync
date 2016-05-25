package be.signalsync.datafilters;

import be.signalsync.util.Config;
import be.signalsync.util.Key;

/**
 * An implementation of a buffered filter.
 * The filtered value is the average of the buffer values.
 * @author Ward Van Assche
 */
public class MovingAverageFilter extends BufferedFilter {
	public MovingAverageFilter() {
		this(Config.getInt(Key.LATENCY_FILTER_BUFFER_SIZE));
	}

	public MovingAverageFilter(int bufferSize) {
		super(bufferSize);
	}
	
	/**
	 * Calculate the next value by taking the average
	 * of the current buffer.
	 */
	@Override
	protected double calculateNext(double rawValue) {
		buffer.offer(rawValue);
		double lastRawValue = buffer.poll();
		double newValue = currentValue + ((rawValue - lastRawValue) / buffer.size());
		return newValue;
	}
}
