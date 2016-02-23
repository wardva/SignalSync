package be.signalsync.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import be.signalsync.streamsets.StreamSet;
import be.signalsync.syncstrategy.FingerprintSyncStrategy;
import be.signalsync.syncstrategy.SyncStrategy;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

@RunWith(Parameterized.class)
public class SynchronizationTest {
	
	private final static String REFERENCE_TEMPLATE = "./Slices/Sonic Youth - Star Power - slice - %d";
	private final static String OTHER_TEMPLATE = "./Slices/Sonic Youth - Star Power (%dms delay) - slice - %d";
	private final static int NUMBER_OF_SLICES = 27;
	private final static int[] DELAYS = {20, 80, 90, 300, 2000};
	private final static double MILLIS_TO_SECONDS = 0.001;
	private StreamSet streams;
	private double latency;

    @Parameters
    public static Collection<Object[]> data() {
    	List<Object[]> params = new ArrayList<>();
    	for(int delay : DELAYS) {
    		for(int i = 0; i < NUMBER_OF_SLICES; i++) {
    			String reference = String.format(REFERENCE_TEMPLATE, i);
    			String other = String.format(OTHER_TEMPLATE, delay,i);
    			params.add(new Object[] {reference, other, delay});
    		}
    	}
    	return params;
    }
    
    public SynchronizationTest(String reference, String other, int expectedLatency) {
    	int sampleRate = Config.getInt(Key.SAMPLE_RATE);
		int bufferSize = Config.getInt(Key.BUFFER_SIZE);
		int stepSize = Config.getInt(Key.STEP_SIZE);
		int overlap = bufferSize - stepSize;
    	streams = new StreamSet() {
			@Override
			public void reset() {
				this.streams.clear();
				this.streams.add(AudioDispatcherFactory.fromPipe(reference, sampleRate, bufferSize, overlap));
				this.streams.add(AudioDispatcherFactory.fromPipe(other, sampleRate, bufferSize, overlap));
			}
		};
		streams.reset();
		latency = expectedLatency * MILLIS_TO_SECONDS;
    }
	
	@Test
	public void testCrossCovariance() {
		Config.set(Key.LATENCY_ALGORITHM, "crosscovariance");
		SyncStrategy strategy = FingerprintSyncStrategy.getInstance();
		List<Float> latencies = strategy.findLatencies(streams);
		
		Assert.assertEquals("The result should contain 1 latency", 1, latencies.size());
		Assert.assertEquals("Actual latency should be " + latency, latency, latencies.get(0), 0.00001);
	}
	
	@Test
	public void testFingerprint() {
		Config.set(Key.LATENCY_ALGORITHM, "fingerprint");
		SyncStrategy strategy = FingerprintSyncStrategy.getInstance();
		List<Float> latencies = strategy.findLatencies(streams);
		
		Assert.assertEquals("The result should contain 1 latency", 1, latencies.size());
		Assert.assertEquals("Actual latency should be " + latency, latency, latencies.get(0), 0.032);
	}
}
