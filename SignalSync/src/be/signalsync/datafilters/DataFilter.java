package be.signalsync.datafilters;

/**
 * Any datafilter has to implement this interface.
 * @author Ward Van Assche
 */
public interface DataFilter {
	double filter(double rawValue);
}
