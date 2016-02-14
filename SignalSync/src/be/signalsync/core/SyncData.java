package be.signalsync.core;
import java.util.List;

/**
 * Contains the synchronization data of the different streams.
 * 
 * @author Ward Van Assche
 *
 */
public class SyncData {
	private List<Integer> latencies;

	public SyncData(List<Integer> latencies) {
		this.latencies = latencies;
	}

	public List<Integer> getLatencies() {
		return latencies;
	}

	public void setLatencies(List<Integer> latencies) {
		this.latencies = latencies;
	}
}
