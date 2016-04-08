package be.signalsync.datafilters;

public class MovingAverageFilter extends BufferedFilter {
	public MovingAverageFilter() {
		super();
	}

	@Override
	protected double calculateNext(double rawValue) {
		buffer.offer(rawValue);
		double lastRawValue = buffer.poll();
		double newValue = currentValue + ((rawValue - lastRawValue) / buffer.size());
		return newValue;
	}
}
