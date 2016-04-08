package be.signalsync.datafilters;

import java.util.LinkedList;
import java.util.Queue;

public class MovingAverageFilter extends BufferedFilter {
	public MovingAverageFilter(int n) {
		super(n);
	}

	@Override
	public void createBuffers() {
		for(int i = 0; i<n; i++) {
			circularBuffers.add(new LinkedList<>());
		}
	}

	@Override
	protected Double calculateNext(int index, Double rawValue) {
		Queue<Double> buffer = circularBuffers.get(index);
		buffer.offer(rawValue);
		double lastRawValue = buffer.poll();
		double previousFilteredValue = values.get(index);
		double newValue = previousFilteredValue + ((rawValue - lastRawValue) / buffer.size());
		return newValue;
	}
}
