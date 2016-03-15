package be.signalsync.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import be.signalsync.syncstrategy.FingerprintSyncStrategy;
import be.signalsync.syncstrategy.SyncStrategy;
import be.signalsync.util.Config;
import be.signalsync.util.FloatBufferGenerator;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

@RunWith(Parameterized.class)
public class RecordedTest {

	private final static String REFERENCE_TEMPLATE = "./Slices/Recorded/opname-reference.wav - slice - %d.wav";
	private final static String OTHER_TEMPLATE = "./Slices/Recorded/opname-%d.wav - slice - %d.wav";
	private final static double[] LATENCIES = { -2.391D, -1.847D, -4.052D };
	private final static String[] RECORDING_TYPE = {"goeie microfoon via geluidskaart", 
			"goeie microfoon zonder geluidskaart", "slechte microfoon"};
	
	private final static int NUMBER_OF_SLICES = 27;
	private final static int NUMBER_OF_RECORDINGS = 3;
	
	private List<float[]> streams;
	private double latency;
	private String type;
	
	@Before
	public void setParameters() {
		//The optimal parameters for this test.
		Config.set(Key.NFFT_MAX_FINGERPRINTS_PER_EVENT_POINT, "10");
		Config.set(Key.NFFT_EVENT_POINT_MIN_DISTANCE, "100");
		Config.set(Key.CROSS_COVARIANCE_NUMBER_OF_TESTS, "20");
		Config.set(Key.CROSS_COVARIANCE_THRESHOLD, "1");
	}

	@Parameters
	public static Collection<Object[]> data() {
		final List<Object[]> params = new ArrayList<>();
		for(int nr = 1; nr<=NUMBER_OF_RECORDINGS; nr++) {
			for(int j = 0; j<NUMBER_OF_SLICES; j++) {
				final String reference = String.format(REFERENCE_TEMPLATE, j);
				final String other = String.format(OTHER_TEMPLATE, nr, j);
				final double latency = LATENCIES[nr-1];
				final String type = RECORDING_TYPE[nr-1];
				params.add(new Object[] { reference, other, latency, type });
			}
		}
		return params;
	}


	public RecordedTest(String reference, String other, double expectedLatency, String type) {
		final int sampleRate = Config.getInt(Key.SAMPLE_RATE);
		final int bufferSize = Config.getInt(Key.NFFT_BUFFER_SIZE);
		this.latency = expectedLatency;
		this.type = type;
		this.streams = new ArrayList<>();
		
		AudioDispatcher refDispatcher = AudioDispatcherFactory.fromPipe(reference, sampleRate, bufferSize, 0);
		FloatBufferGenerator refBufferGen = new FloatBufferGenerator();
		refDispatcher.addAudioProcessor(refBufferGen);
		refDispatcher.run();
		streams.add(refBufferGen.getTotalBuffer());
		
		AudioDispatcher otherDispatcher = AudioDispatcherFactory.fromPipe(other, sampleRate, bufferSize, 0);
		FloatBufferGenerator otherBufferGen = new FloatBufferGenerator();
		otherDispatcher.addAudioProcessor(otherBufferGen);
		otherDispatcher.run(); 
		streams.add(otherBufferGen.getTotalBuffer());
	}
	
	@Test
	public void testCrossCovariance() {
		Config.set(Key.LATENCY_ALGORITHM, "crosscovariance");
		final SyncStrategy strategy = FingerprintSyncStrategy.getInstance();
		final List<Float> latencies = strategy.findLatencies(streams);
		Assert.assertEquals("The result should contain 1 latency", 1, latencies.size());
		Assert.assertEquals(String.format("Crosscovariance failed when latency: %.4f, type: %s", latency, type), 
				latency, latencies.get(0), 0.002);
	}

	@Test
	public void testFingerprint() {
		Config.set(Key.LATENCY_ALGORITHM, "fingerprint");
		final SyncStrategy strategy = FingerprintSyncStrategy.getInstance();
		final List<Float> latencies = strategy.findLatencies(streams);
		Assert.assertEquals("The result should contain 1 latency", 1, latencies.size());
		Assert.assertEquals(String.format("Fingerprinting failed when latency: %.4f, type: %s", latency, type), 
				latency, latencies.get(0), 0.032);
	}
}
