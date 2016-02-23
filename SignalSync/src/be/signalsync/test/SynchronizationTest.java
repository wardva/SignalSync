package be.signalsync.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
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
public class SynchronizationTest {

	private final static String REFERENCE_TEMPLATE = "./Slices/Sonic Youth - Star Power - slice - %d";
	private final static String OTHER_TEMPLATE = "./Slices/Sonic Youth - Star Power (%dms delay) - slice - %d";
	private final static int NUMBER_OF_SLICES = 27;
	private final static int[] DELAYS = { 20, 80, 90, 300, 2000 };
	private final static double MILLIS_TO_SECONDS = 0.001;
	private final List<float[]> streams;
	private final double latency;

	@Parameters
	public static Collection<Object[]> data() {
		final List<Object[]> params = new ArrayList<>();
		for (final int delay : DELAYS) {
			for (int i = 0; i < NUMBER_OF_SLICES; i++) {
				final String reference = String.format(REFERENCE_TEMPLATE, i);
				final String other = String.format(OTHER_TEMPLATE, delay, i);
				params.add(new Object[] { reference, other, delay });
			}
		}
		return params;
	}


	public SynchronizationTest(final String reference, final String other, final int expectedLatency) {
		final int sampleRate = Config.getInt(Key.SAMPLE_RATE);
		final int bufferSize = Config.getInt(Key.BUFFER_SIZE);
		this.latency = expectedLatency * MILLIS_TO_SECONDS;
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
		Assert.assertEquals("Actual latency should be " + latency, latency, latencies.get(0), 0.00001);
	}

	@Test
	public void testFingerprint() {
		Config.set(Key.LATENCY_ALGORITHM, "fingerprint");
		final SyncStrategy strategy = FingerprintSyncStrategy.getInstance();
		final List<Float> latencies = strategy.findLatencies(streams);

		Assert.assertEquals("The result should contain 1 latency", 1, latencies.size());
		Assert.assertEquals("Actual latency should be " + latency, latency, latencies.get(0), 0.032);
	}
}
