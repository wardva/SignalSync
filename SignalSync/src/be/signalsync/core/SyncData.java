package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the synchronization data of the other streams of the synchronized
 * streamset in comparisson with the reference stream of the streamset.
 *
 * @author Ward Van Assche
 *
 */
public class SyncData {
	/**
	 * Contains for each other stream, the start and end latency in comparisson
	 * with the reference stream.
	 */
	private final List<float[]> data;

	public SyncData() {
		data = new ArrayList<>();
	}

	/**
	 * Add a new result.
	 * 
	 * @param result
	 *            An float array containing the start and end latency.
	 */
	public void addResult(final float[] result) {
		data.add(result);
	}

	public List<float[]> getData() {
		return data;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Latencies in comparision with reference stream:\n");
		for (final float[] f : data) {
			if(f.length > 0) {
				sb.append(String.format("Start latency: %.4f\tEnd latency: %.4f\n", f[0], f[1]));
			}
			else {
				sb.append("No match found\n");
			}
			
		}
		return sb.toString();
	}
}
