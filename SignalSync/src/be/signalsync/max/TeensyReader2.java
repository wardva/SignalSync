package be.signalsync.max;
import java.nio.FloatBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import com.cycling74.msp.MSPPerformer;
import com.cycling74.msp.MSPSignal;
import be.signalsync.teensy.TeensyConverter;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.resample.Resampler;

/**
 * This Max/MSP module is used for reading, handling and converting the data read
 * from a Teensy microcontroller into a Max/MSP patch.
 * 
 * @author Ward Van Assche
 *
 */
public class TeensyReader2 extends MSPPerformer implements AudioProcessor {
	private TeensyConverter teensy;
	private AudioDispatcher teensyStream;
	private BlockingQueue<float[]> buffer;
	private ExecutorService teensyExecutor;
	
	private String port;
	private int channel;
	private int numberOfChannels;
	
	private boolean processing;
	
	private int teensySampleRate;
	private double sampleRatio;
	private int bufferSize;
	
	@Override
	protected void dspstate(boolean b) {
		if(b) {
			startProcessing();
		} 
		else {
			stopProcessing();
		}
	}
	
	private void startProcessing() {
		post("Start processing");
		processing = true;
		teensy = new TeensyConverter(teensySampleRate, port, channel, numberOfChannels, bufferSize, 1);
		teensy.start();
		teensyStream = teensy.getAudioDispatcher(0);
		teensyStream.addAudioProcessor(this);
		teensyExecutor.execute(teensyStream);
	}
	
	private void stopProcessing() {
		post("Stop processing");
		processing = false;
		teensyStream.stop();
		teensy.stopDataHandler();
	}
	
	public TeensyReader2(String port, int sampleRate, int channel, int numberOfChannels) {
		super();
		post("constructor called");
		if(channel < 0 || numberOfChannels < 1 || sampleRate < 0) {
			bail("(TeensyReader) Invalid argument(s).");
		}
		this.port = port;
		this.channel = channel;
		this.numberOfChannels = numberOfChannels;
		this.teensySampleRate = sampleRate;
		this.buffer = new LinkedBlockingQueue<>();
		this.bufferSize = 64;
		this.teensyExecutor = Executors.newSingleThreadExecutor();
	}
	
	@Override
	public void dspsetup(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		post("dspsetup called");
	}
	
	@Override
	public void perform(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		try {
			post("Perform starting");
			buffer.take();
			post("Perform finished");
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		post("Process called");
		if(processing) {
			post("Processing: Length: " + audioEvent.getFloatBuffer().length);
			buffer.offer(audioEvent.getFloatBuffer());
		}
		return true;
	}

	@Override
	public void processingFinished() {
		post("Processing finished called");
	}
	
	@Override
	protected void notifyDeleted() {
		post("Notify deleted called");
		if(processing) {
			processing = false;
			stopProcessing();
		}
		teensyExecutor.shutdown();
	}
}
