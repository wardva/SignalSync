package be.signalsync.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import be.signalsync.core.SliceListener;
import be.signalsync.core.Slicer;
import be.signalsync.core.SteppedStreamSlicer;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

@RunWith(Parameterized.class)
public class SteppedSlicerTest {
	
	private List<AudioEvent> audioEvents;
	private int sliceSize;
	private int sliceStep;
	private int limit;
	private int expectedSliceLengths[];
	
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			/*			   --------------------------------------------------------
						   | SliceSize | SliceStep | limit | ExpectedSliceLengths |
			  			   --------------------------------------------------------				*/
			new Object[] {	1, 			1,			 4, 	new int[]{67,  67,  66,  67 }		},
			new Object[] {	5,			5,			 20,	new int[]{334, 333, 333, 334}		},
			new Object[] {	5,			1,			 8, 	new int[]{334, 333, 333, 334}		},
			new Object[] {	5,			2,			 11, 	new int[]{334, 333, 333, 334}		},
			new Object[] {	4,			3,			 13, 	new int[]{267, 267, 267, 267}		}
		});
	}
	
	public SteppedSlicerTest(int sliceSize, int sliceStep, int limit, int[] expectedSliceLengths) {
		this.sliceSize = sliceSize;
		this.sliceStep = sliceStep;
		this.limit = limit;
		this.expectedSliceLengths = expectedSliceLengths;
	}
	
	@Before
	public void before() {
		audioEvents = new ArrayList<>();
		for(double i = 0; i<=100; i += 0.015) {
			audioEvents.add(new DummyAudioEvent(i));
		}
	}
	
	@Test
	public void test() {
		SteppedStreamSlicer slicer = new SteppedStreamSlicer(sliceSize, sliceStep);

		SliceListener<float[]> listener = new SliceListener<float[]>() {
			private int eventNr = 0;
			
			@Override
			public void onSliceEvent(float[] slice, Slicer<float[]> s) {
				Assert.assertEquals(expectedSliceLengths[eventNr], slice.length, 1);
				DoubleStream ds = IntStream.range(0, slice.length).mapToDouble(i -> slice[i]);
				int boundary = eventNr * sliceStep;
				Assert.assertTrue(ds.allMatch(x -> Math.floor(x) >= boundary && Math.floor(x) < boundary + sliceSize));
				++eventNr;
			}
			
			@Override
			public void done(Slicer<float[]> s) {
				Assert.assertEquals(4, eventNr);
			}
		};
		
		slicer.addEventListener(listener);
		
		for(AudioEvent a : audioEvents) {
			slicer.process(a);
			if(a.getTimeStamp() >= limit) {
				break;
			}
		}
		slicer.processingFinished();
	}
	
	private class DummyAudioEvent extends AudioEvent {
		private double timestamp;
		public DummyAudioEvent(double timestamp) {
			super(new TarsosDSPAudioFormat(8000, 0, 1, false, false));
			this.timestamp = timestamp;
			this.setFloatBuffer(new float[] { (float) Math.floor(timestamp) });
		}
		
		@Override
		public double getTimeStamp() {
			return timestamp;
		}
	}
}
