package be.signalsync.datafilters;

public class NoFilter implements DataFilter {
	@Override
	public double filter(double rawValue) {
		return rawValue;
	}
}
