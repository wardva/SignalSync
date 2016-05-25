package be.signalsync.stream;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import be.signalsync.slicer.StreamSlicer;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

/**
 * Adapter class for converting an AudioDispatcher into an 
 * implementation of the Stream interface.
 * @author Ward Van Assche
 */
public class AudioDispatcherStream implements Stream {
	private Map<StreamProcessor, AudioProcessor> processorMap;
	private AudioDispatcher dispatcher;
	private ExecutorService executor;
	private boolean started;

	public AudioDispatcherStream(AudioDispatcher dispatcher) {
		this.dispatcher = dispatcher;
		this.executor = Executors.newSingleThreadExecutor();
		this.processorMap = new HashMap<>();
		this.started = false;
	}
	
	/**
	 * Create a new TarsosDSP StreamProcessor. This method is responsible
	 * for mapping the events between the libraries.
	 */
	@Override
	public void addStreamProcessor(StreamProcessor streamProcessor) {
		AudioProcessor audioProcessor = new AudioProcessor() {
			@Override
			public void processingFinished() {
				streamProcessor.processingFinished();
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				StreamEvent event = new StreamEvent(audioEvent.getFloatBuffer(), audioEvent.getTimeStamp());
				streamProcessor.process(event);
				return true;
			}
		};
		processorMap.put(streamProcessor, audioProcessor);
		dispatcher.addAudioProcessor(audioProcessor);
	}

	/**
	 * Create a StreamSlicer instance of this stream.
	 * Caution: this method adds the slicer as an AudioDispatcher to the
	 * stream, don't do it a second time yourself.
	 */
	@Override
	public StreamSlicer createSlicer(int sliceSize, int sliceStep) {
		StreamSlicer s = new StreamSlicer(sliceSize, sliceStep, getSampleRate());
		this.addStreamProcessor(s);
		return s;
	}
	
	/**
	 * Starts the AudioDispatcher.
	 */
	@Override
	public void start() {
		if(executor.isShutdown()) {
			throw new RuntimeException("Can't run an AudioDispatcher more than once.");
		}
		else if(started) {
			throw new RuntimeException("AudioDispatcher is running already.");
		}
		started = true;
		executor.execute(dispatcher);
		executor.shutdown();
	}
	
	/**
	 * Stops the AudioDispatcher.
	 */
	@Override
	public void stop() {
		if(!dispatcher.isStopped()) {
			throw new RuntimeException("The AudioDispatcher is not running!");
		}
		dispatcher.stop();
		boolean terminated;
		try {
			terminated = executor.awaitTermination(1, TimeUnit.SECONDS);
		} 
		catch (InterruptedException e) {
			terminated = false;
		}
		if(!terminated) {
			throw new RuntimeException("An error occured while trying to stop an AudioDispatcher");
		}
	}

	@Override
	public double getSampleRate() {
		return dispatcher.getFormat().getSampleRate();
	}

	/**
	 * Remove a StreamProcessor from the list of StreamProcessors.
	 */
	@Override
	public void removeStreamProcessor(StreamProcessor s) {
		AudioProcessor audioProcessor = processorMap.get(s);
		dispatcher.removeAudioProcessor(audioProcessor);
		processorMap.remove(s);
	}
}
