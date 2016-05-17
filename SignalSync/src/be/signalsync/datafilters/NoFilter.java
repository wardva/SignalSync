package be.signalsync.datafilters;

/**
 * A filter implementation doing nothing but returning
 * the raw data.
 * @author Ward Van Assche
 *
 */
public class NoFilter implements DataFilter {
	@Override
	public double filter(double rawValue) {
		return rawValue;
	}
}
