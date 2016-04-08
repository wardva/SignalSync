package be.signalsync.datafilters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MovingMedianFilter extends BufferedFilter {
	private int middle;
	
	
	public MovingMedianFilter(int n) {
		super(n);
		middle = bufferSize/2;
	}

	@Override
	protected void createBuffers() {
		for(int i = 0; i<n; i++) {
			circularBuffers.add(new LinkedList<>());
		}
	}

	@Override
	protected Double calculateNext(int index, Double rawValue) {
		Queue<Double> buffer = circularBuffers.get(index);
		buffer.poll();
		buffer.offer(rawValue);
		List<Double> temp = new ArrayList<>(buffer);
		Collections.sort(temp);
		double newFilteredValue = temp.get(middle);
		return newFilteredValue;
	}
}
