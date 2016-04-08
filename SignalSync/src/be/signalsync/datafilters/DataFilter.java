package be.signalsync.datafilters;

import java.util.List;

public interface DataFilter {
	List<Double> filter(List<Double> rawLatencies);
}
