package be.signalsync.datafilters;

import be.signalsync.util.Config;
import be.signalsync.util.Key;

public class DataFilterFactory {
	public static DataFilter createDefault() {
		String type = Config.get(Key.LATENCY_FILTER_TYPE);
		switch(type) {
			case "average" : 
				return new MovingAverageFilter();
			case "median" : 
				return new MovingMedianFilter();
			case "none" : 
				return new NoFilter();
			default:
				throw new IllegalArgumentException("Invalid latency filter type in config file");
		}
	}
}
