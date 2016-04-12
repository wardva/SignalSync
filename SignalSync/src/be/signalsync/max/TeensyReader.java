package be.signalsync.max;
import java.util.Arrays;
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
public class TeensyReader extends MSPPerformer implements AudioProcessor {
	private TeensyConverter teensy;
	private AudioDispatcher teensyStream;
	private BlockingQueue<float[]> buffer;
	private ExecutorService teensyExecutor;
	
	private String port;
	private int channel;
	private int numberOfChannels;
	
	private boolean startMessageReceived;
	private boolean dacRunning;
	private boolean processing;
	
	private double sampleRatio;
	private int teensySampleRate;
	private double targetSampleRate;
	private int teensyBufferSize;
	private int outputBufferSize;
	
	private Resampler resampler;
	private float[] resampleBuffer;
	
	public TeensyReader(String port, int sampleRate, int channel, int numberOfChannels) {
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
		this.teensyExecutor = Executors.newSingleThreadExecutor();
		setInlets();
		setOutlets();
		setAssists();
	}
	
	private void setInlets() {
		declareInlets(new int[] {});
	}

	private void setOutlets() {
		int outlets[] = new int[numberOfChannels];
		Arrays.fill(outlets, SIGNAL);
		declareOutlets(outlets);
	}
	
	private void setAssists() {
		String assists[] = new String[numberOfChannels];
		Arrays.fill(assists, "A data signal stream.");
		assists[0] = "The audio signal stream.";
		setOutletAssist(assists);
	}
	
	@Override
	public void dspsetup(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		post("dspsetup called");
		this.targetSampleRate = (int) sigs_out[0].sr;
		this.sampleRatio = targetSampleRate / teensySampleRate;
		this.outputBufferSize = sigs_out[0].n;
		this.teensyBufferSize = (int) Math.round(outputBufferSize / sampleRatio);
		this.resampler = new Resampler(true, sampleRatio, sampleRatio);
		this.resampleBuffer = new float[outputBufferSize];
	}
	
	public void start() {
		post("Start message received");
		this.startMessageReceived = true;
		stateChange();
	}
	
	public void stop() {
		post("Stop message received");
		this.startMessageReceived = false;
		stateChange();
	}
	
	public void stateChange() {
		boolean newState = startMessageReceived && dacRunning;
		if(!processing && newState) {
			startProcessing();
		}
		else if(processing && !newState) {
			stopProcessing();
		}
	}
	
	@Override
	protected void dspstate(boolean b) {
		post("dspstate changed: " + b);
		this.dacRunning = b;
		stateChange();
	}
	
	private void startProcessing() {
		processing = true;
		post("Start processing");
		teensy = new TeensyConverter(teensySampleRate, port, channel, numberOfChannels, teensyBufferSize, 1);
		teensy.start();
		teensyStream = teensy.getAudioDispatcher(0);
		teensyStream.addAudioProcessor(this);
		teensyExecutor.execute(teensyStream);
	}
	
	private void stopProcessing() {
		processing = false;
		post("Stop processing");
		teensyStream.stop();
		teensy.stopDataHandler();
	}
	
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		post("Process called");
		if(processing) {
			buffer.offer(audioEvent.getFloatBuffer().clone());
		}
		return true;
	}
	
	@Override
	public void perform(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		try {
			MSPSignal out = sigs_out[0];
			if(processing) {
				float[] buf = buffer.take();
				resampler.process(sampleRatio, buf, 0, buf.length, false, resampleBuffer, 0, resampleBuffer.length);
				System.arraycopy(resampleBuffer, 0, out.vec, 0, outputBufferSize);
			}
			else {
				Arrays.fill(out.vec, 0.0f);
			}
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void processingFinished() {
		post("Processing finished called");
	}
	
	@Override
	protected void notifyDeleted() {
		post("Notify deleted called");
		if(processing) {
			stopProcessing();
		}
		teensyExecutor.shutdown();
	}
}
