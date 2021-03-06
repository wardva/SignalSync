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

import be.signalsync.syncstrategy.CrossCovarianceSyncStrategy;
import be.signalsync.syncstrategy.FingerprintSyncStrategy;
import be.signalsync.syncstrategy.LatencyResult;
import be.signalsync.util.FloatBufferGenerator;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

@RunWith(Parameterized.class)
public class SynchronizationTest {

	private final static int SAMPLE_RATE = 8000;
	private final static int NFFT_BUFFER_SIZE = 512;
	private final static int NFFT_STEP_SIZE = 256;
	
	private final static String REFERENCE_TEMPLATE = "./Slices/Clean/Sonic Youth - Star Power_0_0hz.wav - slice - %d.wav";
	private final static String OTHER_TEMPLATE = "./Slices/Clean/Sonic Youth - Star Power_%d_%dhz.wav - slice - %d.wav";
	private final static int NUMBER_OF_SLICES = 27;
	
	//The latencies in milliseconds, latency in samples: multiply by 8
	private final static int[] LATENCIES = {20, 80, 90, 300, 2000, 6000, -20, -80, -90, -300, -2000, -6000};
	private final static int[] FREQUENCIES = {50,100};
	private final static double MILLIS_TO_SECONDS = 0.001;
	private final List<float[]> streams;
	private final double latencyInSeconds;
	private final int latencyInSamples;
	private final int frequency;
	
	private CrossCovarianceSyncStrategy crossCovarianceStrategy;
	private FingerprintSyncStrategy fingerprintSyncStrategy;
	
	@Before
	public void before() {
		this.fingerprintSyncStrategy = new FingerprintSyncStrategy(
				SAMPLE_RATE, 		//Sample rate
				NFFT_BUFFER_SIZE, 	//Buffer size
				NFFT_STEP_SIZE, 	//Buffer step size
				10, 				//Minimum distance between fingerprints
				50,					//Max number of fingerprints for each event point
				2);					//Minimum aligned matchess
		
		this.crossCovarianceStrategy = new CrossCovarianceSyncStrategy(this.fingerprintSyncStrategy, 
				SAMPLE_RATE, 		//Sample rate
				NFFT_BUFFER_SIZE, 	//Buffer size
				NFFT_STEP_SIZE, 	//Buffer step size
				30, 				//Number of tests
				1);					//Success threshold
	}

	@Parameters
	public static Collection<Object[]> data() {
		final List<Object[]> params = new ArrayList<>();
		for (final int latency : LATENCIES) {
			for(final int frequency : FREQUENCIES) {
				for (int i = 0; i < NUMBER_OF_SLICES; i++) {
					final String reference = String.format(REFERENCE_TEMPLATE, i);
					final String other = String.format(OTHER_TEMPLATE, latency, frequency, i);
					params.add(new Object[] { reference, other, latency, frequency });
				}
			}
		}
		return params;
	}

	public SynchronizationTest(final String reference, final String other, final int expectedLatency, final int currentFrequency) {
		this.latencyInSeconds = expectedLatency * MILLIS_TO_SECONDS;
		this.latencyInSamples = expectedLatency * 8;
		this.frequency = currentFrequency;
		this.streams = new ArrayList<>();
		
		AudioDispatcher refDispatcher = AudioDispatcherFactory.fromPipe(reference, SAMPLE_RATE, NFFT_BUFFER_SIZE, 0);
		FloatBufferGenerator refBufferGen = new FloatBufferGenerator();
		refDispatcher.addAudioProcessor(refBufferGen);
		refDispatcher.run();
		streams.add(refBufferGen.getTotalBuffer());
		
		AudioDispatcher otherDispatcher = AudioDispatcherFactory.fromPipe(other, SAMPLE_RATE, SAMPLE_RATE, 0);
		FloatBufferGenerator otherBufferGen = new FloatBufferGenerator();
		otherDispatcher.addAudioProcessor(otherBufferGen);
		otherDispatcher.run(); 
		streams.add(otherBufferGen.getTotalBuffer());
	}
	
	@Test
	public void testCrossCovariance() {
		final List<LatencyResult> latencies = crossCovarianceStrategy.findLatencies(streams);
		Assert.assertEquals("The result should contain 1 latency", 1, latencies.size());
		Assert.assertTrue("The latency should be found", latencies.get(0).isLatencyFound());
		Assert.assertTrue("The latency should be refined", latencies.get(0).isRefined());
		Assert.assertEquals(String.format("Crosscovariance failed when latency (in seconds): %.4f, frequency: %d", latencyInSeconds, frequency), 
				latencyInSeconds, latencies.get(0).getLatencyInSeconds(), 0.0001);
		Assert.assertEquals(String.format("Crosscovariance failed when latency (in samples): %d, frequency: %d", latencyInSamples, frequency), 
				latencyInSamples, latencies.get(0).getLatencyInSamples(), 1);
	}

	/*@Test
	public void testFingerprint() {
		final List<LatencyResult> latencies = fingerprintSyncStrategy.findLatencies(streams);
		Assert.assertEquals("The result should contain 1 latency", 1, latencies.size());
		Assert.assertTrue("The latency should be found", latencies.get(0).isLatencyFound());
		Assert.assertFalse("The latency should not be refined", latencies.get(0).isRefined());
		Assert.assertEquals(String.format("Fingerprinting failed when latency (in seconds): %.4f, frequency: %d", latencyInSeconds, frequency), 
				latencyInSeconds, latencies.get(0).getLatencyInSeconds(), 0.032);
		Assert.assertEquals(String.format("Fingerprinting failed when latency (in samples): %d, frequency: %d", latencyInSamples, frequency), 
				latencyInSamples, latencies.get(0).getLatencyInSamples(), 256);
	}*/
}
