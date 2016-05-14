package be.signalsync.stream;

/**
 * This interface should be implemented by classes
 * which are interested in the data from streams.
 * @author Ward Van Assche
 */
public interface StreamProcessor {
	void process(StreamEvent event);
	void processingFinished();
}
