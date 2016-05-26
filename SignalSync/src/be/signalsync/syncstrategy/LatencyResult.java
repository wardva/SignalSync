package be.signalsync.syncstrategy;

/**
 * This class represents a latency between two streams. The class contains
 * the latency in seconds and in samples. The conversion between these formats is not
 * handled in this class.
 * 
 * @author Ward Van Assche
 */
public class LatencyResult {
	private double latencyInSeconds;
	private int latencyInSamples;
	private double timestamp;
	private boolean latencyFound;
	private boolean refined;
	private boolean hasTimestamp;
	public final static LatencyResult NO_RESULT = new LatencyResult(0, 0, false, false);
	
	/**
	 * Create a new LatencyResult without a timestamp containing
	 * a raw latency.
	 * @param latency The raw latency
	 */
	public static LatencyResult rawResult(double latencyInSeconds, int latencyInSamples) {
		return new LatencyResult(latencyInSeconds, latencyInSamples, true, false);
	}
	
	/**
	 * Create a new LatencyResult without a timestamp containing
	 * a refined latency.
	 * @param latency The refined latency
	 */
	public static LatencyResult refinedResult(double latencyInSeconds, int latencyInSamples) {
		return new LatencyResult(latencyInSeconds, latencyInSamples, true, true);
	}
	
	/**
	 * Create a new LatencyResult without a timestamp specifying all the fields.
	 */
	public LatencyResult(double latencyInSeconds, int latencyInSamples, boolean latencyFound, boolean refined) {
		this(latencyInSeconds, latencyInSamples, 0, latencyFound, refined, false);
	}
	
	/**
	 * Create a new LatencyResult with a timestamp specifying all the fields.
	 */
	public LatencyResult(double latencyInSeconds, int latencyInSamples, double timestamp, boolean latencyFound, boolean refined) {
		this(latencyInSeconds, latencyInSamples, timestamp, latencyFound, refined, true);
	}
	
	/**
	 * Create a new LatencyResult with a timestamp specifying all the fields.
	 */
	public LatencyResult(double latencyInSeconds, int latencyInSamples, double timestamp, boolean latencyFound, boolean refined, boolean hasTimestamp) {
		this.latencyInSeconds = latencyInSeconds;
		this.latencyInSamples = latencyInSamples;
		this.timestamp = timestamp;
		this.latencyFound = latencyFound;
		this.refined = refined;
		this.hasTimestamp = hasTimestamp;
	}

	public boolean isLatencyFound() {
		return latencyFound;
	}
	public boolean isRefined() {
		return refined;
	}
	public boolean hasTimestamp () {
		return hasTimestamp;
	}
	public double getTimestamp() {
		return timestamp;
	}	
	public double getLatencyInSeconds() {
		return latencyInSeconds;
	}
	public int getLatencyInSamples() {
		return latencyInSamples;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(this.isLatencyFound()) {
			sb.append(String.format("Latency in seconds: %.3f\n", this.getLatencyInSeconds()));
			sb.append(String.format("Latency in samples: %d\n", this.getLatencyInSamples()));
		}
		else {
			sb.append("No latency found!");
		}
		if(this.hasTimestamp()) {
			sb.append(String.format("Timestamp: %.3f\n", this.getTimestamp()));
		}
		if(this.isRefined()) {
			sb.append("Latency refined\n");
		}
		else {
			sb.append("Latency not refined\n");
		}
		sb.append("\n");
		return sb.toString();
	}
}
