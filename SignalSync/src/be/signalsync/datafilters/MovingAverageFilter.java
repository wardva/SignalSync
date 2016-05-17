package be.signalsync.datafilters;

import be.signalsync.util.Config;
import be.signalsync.util.Key;

public class MovingAverageFilter extends BufferedFilter {
	public MovingAverageFilter() {
		this(Config.getInt(Key.LATENCY_FILTER_BUFFER_SIZE));
	}

	public MovingAverageFilter(int bufferSize) {
		super(bufferSize);
	}
	
	@Override
	protected double calculateNext(double rawValue) {
		buffer.offer(rawValue);
		double lastRawValue = buffer.poll();
		double newValue = currentValue + ((rawValue - lastRawValue) / buffer.size());
		return newValue;
	}
}
