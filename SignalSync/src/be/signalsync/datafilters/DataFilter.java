package be.signalsync.datafilters;

/**
 * 
 * @author Ward Van Assche
 */
public interface DataFilter {
	double filter(double rawValue);
}
