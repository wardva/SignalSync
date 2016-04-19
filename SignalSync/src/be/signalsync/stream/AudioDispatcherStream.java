package be.signalsync.stream;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import be.signalsync.slicer.StreamSlicer;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class AudioDispatcherStream implements Stream {
	private AudioDispatcher dispatcher;
	private ExecutorService executor;
	private boolean started;

	public AudioDispatcherStream(AudioDispatcher dispatcher) {
		this.dispatcher = dispatcher;
		this.executor = Executors.newSingleThreadExecutor();
		this.started = false;
	}
	
	@Override
	public void addStreamProcessor(StreamProcessor processor) {
		dispatcher.addAudioProcessor(new AudioProcessor() {
			@Override
			public void processingFinished() {
				processor.processingFinished();
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				StreamEvent event = new StreamEvent(audioEvent.getFloatBuffer(), audioEvent.getTimeStamp());
				processor.process(event);
				return true;
			}
		});
	}

	@Override
	public StreamSlicer getSlicer(int sliceSize, int sliceStep) {
		StreamSlicer s = new StreamSlicer(sliceSize, sliceStep, getSampleRate());
		this.addStreamProcessor(s);
		return s;
	}
	
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
}
