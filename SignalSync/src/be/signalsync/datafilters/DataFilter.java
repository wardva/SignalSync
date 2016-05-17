package be.signalsync.datafilters;

/**
 * The interface a data filter has to implement.
 * @author Ward Van Assche
 */
public interface DataFilter {
	double filter(double rawValue);
}
