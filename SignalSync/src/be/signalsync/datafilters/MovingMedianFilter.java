package be.signalsync.datafilters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MovingMedianFilter extends BufferedFilter {
	private int middle;
	
	public MovingMedianFilter() {
		super();
		middle = bufferSize/2;
	}

	@Override
	protected double calculateNext(double rawValue) {
		buffer.poll();
		buffer.offer(rawValue);
		List<Double> temp = new ArrayList<>(buffer);
		Collections.sort(temp);
		double newFilteredValue = temp.get(middle);
		return newFilteredValue;
	}
}
