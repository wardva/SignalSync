package be.signalsync.util;

/**
 * Defines which values can be configured and their respective default values.
 * 
 * @author Joren Six
 */
public enum Key {
	/**
	 * Name used in log messages.
	 */
	APPLICATION_NAME("StreamSync"),
	/**
	 * The sample rate of the input signal.
	 */
	SAMPLE_RATE(8000),
	/**
	 * The size of the audio block and consequently the size (in samples) of the
	 * FFT.
	 */
	NFFT_BUFFER_SIZE(512),
	/**
	 * The size of the audio block step size (in samples).
	 */
	NFFT_STEP_SIZE(256),
	/**
	 * The size in seconds between each slice.
	 */
	SLICE_SIZE_S(10),
	/**
	 * The step size between each slice in seconds.
	 */
	SLICE_STEP_S(1),
	/**
	 * Which synchronization algorithm should be used. Possible values:
	 * crosscovariance or fingerprint
	 */
	LATENCY_ALGORITHM("crosscovariance"),

	/**
	 * Minium euclidean distance between event points. The value is expressed in
	 * milliseconds cents
	 */
	NFFT_EVENT_POINT_MIN_DISTANCE(600),

	/**
	 * The maximum number of fingerpints per event points (fan-out).
	 */
	NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT(2),

	/**
	 * The synchronization algorithm only considers the match as valid if this
	 * number of aligning matches are found.
	 */
	SYNC_MIN_ALIGNED_MATCHES(7),
	
	/**
	 * The number of tests that should be performed each crosscovariance run.
	 */
	CROSS_COVARIANCE_NUMBER_OF_TESTS(10),
	
	/**
	 * The number of tests that should give the same results before the result value
	 * is retained.
	 */
	CROSS_COVARIANCE_THRESHOLD(1),
	
	/**
	 * The name of the Teensy port to read.
	 */
	TEENSY_PORT("COM3"),
	
	/**
	 * The index of the first Teensy channel.
	 */
	TEENSY_START_CHANNEL(0),
	
	/**
	 * The number of Teensy channels.
	 */
	TEENSY_CHANNEL_COUNT(2),
	
	/**
	 * The received Teensy audio is multiplicated with this value.
	 */
	TEENSY_GAIN(1), 

	/**
	 * The size of the circular buffer used in the low pass filter
	 * for filtering the latencies.
	 */
	LATENCY_FILTER_BUFFER_SIZE(3),
	
	/**
	 * The type of the latency filter
	 */
	LATENCY_FILTER_TYPE("median");

	String defaultValue;

	private Key(final float defaultValue) {
		this(String.valueOf(defaultValue));
	}

	private Key(final int defaultValue) {
		this(String.valueOf(defaultValue));
	}

	private Key(final String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDefaultValue() {
		return defaultValue;
	}
}
