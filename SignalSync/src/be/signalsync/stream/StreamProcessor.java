package be.signalsync.stream;

public interface StreamProcessor {
	void process(StreamEvent event);
	void processingFinished();
}
