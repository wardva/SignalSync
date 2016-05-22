package be.signalsync.syncstrategy;

public class LatencyResult {
	public static final LatencyResult NO_RESULT = new LatencyResult(0, false, false);
	private double latency;
	private double timestamp;
	private boolean latencyFound;
	private boolean refined;
	private boolean hasTimestamp;
	
	/**
	 * Create a new LatencyResult without a timestamp containing
	 * a raw latency.
	 * @param latency The raw latency
	 */
	public static LatencyResult rawResult(double latency) {
		return new LatencyResult(latency, true, false);
	}
	
	/**
	 * Create a new LatencyResult without a timestamp containing
	 * a refined latency.
	 * @param latency The refined latency
	 */
	public static LatencyResult refinedResult(double latency) {
		return new LatencyResult(latency, true, true);
	}
	
	/**
	 * Create a new LatencyResult without a timestamp specifying all the fields.
	 */
	public LatencyResult(double result, boolean latencyFound, boolean refined) {
		this.latency = result;
		this.latencyFound = latencyFound;
		this.refined = refined;
		this.hasTimestamp = false;
	}
	
	/**
	 * Create a new LatencyResult with a timestamp specifying all the fields.
	 */
	public LatencyResult(double latency, double timestamp, boolean latencyFound, boolean refined) {
		this.latency = latency;
		this.timestamp = timestamp;
		this.latencyFound = latencyFound;
		this.refined = refined;
		this.hasTimestamp = true;
	}
	
	public double getLatency() {
		return latency;
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
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(this.isLatencyFound()) {
			sb.append(String.format("Latency: %.3f\n", this.getLatency()));
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
