package be.signalsync.datafilters;

import be.signalsync.util.Config;
import be.signalsync.util.Key;

public class DataFilterFactory {
	public static DataFilter getDefault(int n) {
		String type = Config.get(Key.LATENCY_FILTER_TYPE);
		switch(type) {
			case "average" : 
				return new MovingAverageFilter(n);
			case "median" : 
				return new MovingMedianFilter(n);
			case "none" : 
				return new NoFilter();
			default:
				throw new IllegalArgumentException("Invalid latency filter type in config file");
		}
	}
}
