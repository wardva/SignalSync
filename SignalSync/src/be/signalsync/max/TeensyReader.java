package be.signalsync.max;
import java.nio.FloatBuffer;
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
	
	private int teensySampleRate;
	private double targetSampleRate;
	private double sampleRatio;
	private int bufferSize;
	
	private Resampler resampler;
	private FloatBuffer inputBuffer;
	private FloatBuffer resampledBuffer;
	
	
	public TeensyReader() {
		bail("(TeensyReader) Please provide the required arguments!");
	}
	
	public void start() {
		this.startMessageReceived = true;
		stateChange();
	}
	
	public void stop() {
		this.startMessageReceived = false;
		stateChange();
	}
	
	@Override
	protected void dspstate(boolean b) {
		post("Running: " + b);
		this.dacRunning = b;
		stateChange();
	}
	
	public void stateChange() {
		boolean newState = startMessageReceived && dacRunning;
		if(!processing && newState) {
			processing = true;
			startProcessing();
		}
		else if(processing && !newState) {
			processing = false;
			stopProcessing();
		}
	}
	
	private void startProcessing() {
		post("Start processing");
		teensyExecutor = Executors.newSingleThreadExecutor();
		teensy = new TeensyConverter(teensySampleRate, port, channel, numberOfChannels, bufferSize, 1);
		teensy.start();
		teensyStream = teensy.getAudioDispatcher(0);
		teensyStream.addAudioProcessor(this);
		teensyExecutor.execute(teensyStream);
	}
	
	private void stopProcessing() {
		post("Stop processing");
		teensyStream.stop();
		teensy.stopDataHandler();
		teensyExecutor.shutdown();
	}
	
	public TeensyReader(String port, int sampleRate, int channel, int numberOfChannels) {
		super();
		if(channel < 0 || numberOfChannels < 1 || sampleRate < 0) {
			bail("(TeensyReader) Invalid argument(s).");
		}
		this.startMessageReceived = this.dacRunning = this.processing = false;
		this.port = port;
		this.channel = channel;
		this.numberOfChannels = numberOfChannels;
		this.teensySampleRate = sampleRate;
		this.buffer = new LinkedBlockingQueue<>();
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
		this.bufferSize = sigs_out[0].n;
		this.targetSampleRate = (int) sigs_out[0].sr;
		this.sampleRatio = targetSampleRate / teensySampleRate;
		this.resampler = new Resampler(true, sampleRatio, sampleRatio);
		this.inputBuffer = FloatBuffer.allocate(bufferSize);
		this.resampledBuffer = FloatBuffer.allocate((int) Math.round(bufferSize * sampleRatio));
	}
	
	@Override
	public void perform(MSPSignal[] sigs_in, MSPSignal[] sigs_out) {
		try {
			MSPSignal out = sigs_out[0];
			if(processing) {
				float[] buf = buffer.take();
				System.arraycopy(buf, 0, out.vec, 0, out.n);
			}
			else {
				Arrays.fill(out.vec, 0.0f);
			}
		}
		catch (InterruptedException e) {
			post("Error: " + e.getMessage());
		}
	}
	
	@Override
	public boolean process(AudioEvent audioEvent) {
		if(processing) {
			inputBuffer.put(audioEvent.getFloatBuffer());
			resampler.process(sampleRatio, inputBuffer, false, resampledBuffer);
			buffer.offer(resampledBuffer.array().clone());
			inputBuffer.clear();
			resampledBuffer.clear();
		}
		return true;
	}

	@Override
	public void processingFinished() {
		post("Processing finished!");
	}
}
