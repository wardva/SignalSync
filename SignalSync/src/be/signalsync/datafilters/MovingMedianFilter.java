package be.signalsync.datafilters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import be.signalsync.util.Config;
import be.signalsync.util.Key;

public class MovingMedianFilter extends BufferedFilter {
	private final int middle;
	
	public MovingMedianFilter() {
		this(Config.getInt(Key.LATENCY_FILTER_BUFFER_SIZE));
	}
	
	public MovingMedianFilter(int bufferSize) {
		super(bufferSize);
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
