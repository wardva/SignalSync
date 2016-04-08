package be.signalsync.datafilters;

import java.util.List;

public class NoFilter implements DataFilter {
	@Override
	public List<Double> filter(List<Double> rawLatencies) {
		return rawLatencies;
	}
}
