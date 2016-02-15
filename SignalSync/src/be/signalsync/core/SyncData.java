package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the synchronization data of the different streams.
 * 
 * @author Ward Van Assche
 *
 */
public class SyncData {
	private List<float[]> data;

	public SyncData() {
		this.data = new ArrayList<>();
	}

	public List<float[]> getData() {
		return data;
	}

	public void addResult(float[] result) {
		data.add(result);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Latencies in comparision with reference stream:\n");
		for(float[] f : data) {
			sb.append(String.format("Start latency: %.4f\tEnd latency: %.4f\n", f[0], f[1]));
		}
		return sb.toString();
	}
}
