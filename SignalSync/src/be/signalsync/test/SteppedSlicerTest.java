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

import be.signalsync.slicer.SliceEvent;
import be.signalsync.slicer.SliceListener;
import be.signalsync.slicer.Slicer;
import be.signalsync.slicer.StreamSlicer;
import be.signalsync.stream.StreamEvent;

@RunWith(Parameterized.class)
public class SteppedSlicerTest {
	
	private List<StreamEvent> streamEvents;
	private int sliceSize;
	private int sliceStep;
	private int limit;
	private float sampleRate = 1f/0.015f;
	private int expectedSliceLengths[];
	
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			/*			   --------------------------------------------------------
						   | SliceSize | SliceStep | limit | ExpectedSliceLengths |
			  			   --------------------------------------------------------				*/
			new Object[] {	1, 			1,			 4, 	new int[] {67,  67,  66,  67 }		},
			new Object[] {	5,			5,			 20,	new int[] {334, 333, 333, 334}		},
			new Object[] {	5,			1,			 8, 	new int[] {334, 333, 333, 334}		},
			new Object[] {	5,			2,			 11, 	new int[] {334, 333, 333, 334}		},
			new Object[] {	4,			3,			 13, 	new int[] {267, 267, 267, 267}		}
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
		streamEvents = new ArrayList<>();
		for(double i = 0; i<=100; i += 0.015) {
			streamEvents.add(createDummyStreamEvent(i));
		}
	}
	
	@Test
	public void test() {
		SliceListener<float[]> listener = new SliceListener<float[]>() {
			private int eventNr = 0;
			
			@Override
			public void onSliceEvent(SliceEvent<float[]> event) {
				Assert.assertEquals(expectedSliceLengths[eventNr], event.getSlices().length, 1);
				DoubleStream ds = IntStream.range(0, event.getSlices().length).mapToDouble(i -> event.getSlices()[i]);
				Assert.assertTrue(ds.allMatch(x -> x >= eventNr * sliceStep && x < eventNr * sliceStep + sliceSize));
				++eventNr;
			}
			
			@Override
			public void done(Slicer<float[]> s) {
				Assert.assertEquals(4, eventNr);
			}
		};
		
		StreamSlicer slicer = new StreamSlicer(sliceSize, sliceStep, sampleRate);
		slicer.addEventListener(listener);
		
		for(StreamEvent a : streamEvents) {
			slicer.process(a);
			if(a.getTimeStamp() >= limit) {
				break;
			}
		}
		
		slicer.processingFinished();
	}
	
	private StreamEvent createDummyStreamEvent(double timestamp) {
		return new StreamEvent(new float[] { (float) Math.floor(timestamp) }, timestamp);
	}
}
