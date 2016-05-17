package be.signalsync.test;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import be.signalsync.datafilters.DataFilter;
import be.signalsync.datafilters.MovingAverageFilter;
import be.signalsync.datafilters.MovingMedianFilter;
import be.signalsync.datafilters.NoFilter;

@RunWith(Parameterized.class)
public class DataFilterTest {
	private final static double LATENCY_DATA[] = new double[] {150,150,150,168,150,150,70,149,149,150,148,149,149,149,150,163,170,172,170,170,170,176,170,170,170,170,170,60,80,170,170,170,170,164,170,170};
	private final static double AVG3_DATA[] = new double[] {150,150,150,156,156,156,123.3333333,123,122.6666667,149.3333333,149,149,148.6666667,149,149.3333333,154,161,168.3333333,170.6666667,170.6666667,170,172,172,172,170,170,170,133.3333333,103.3333333,103.3333333,140,170,170,168,168,168};
	private final static double AVG5_DATA[] = new double[] {150,150,150,153.6,153.6,153.6,137.6,137.4,133.6,133.6,133.2,149,149,149,149,152,156.2,160.8,165,169,170.4,171.6,171.2,171.2,171.2,171.2,170,148,130,130,130,130,152,168.8,168.8,168.8};
	private final static double MEDIAN3_DATA[] = new double[] {150,150,150,150,150,150,150,149,149,149,149,149,149,149,149,150,163,170,170,170,170,170,170,170,170,170,170,170,80,80,170,170,170,170,170,170};
	private final static double MEDIAN5_DATA[] = new double[] {150,150,150,150,150,150,150,150,149,149,149,149,149,149,149,149,150,163,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170};
	private final static double DELTA = 0.0001;
	
	private DataFilter filter;
	private double expected[];
	private String method;
	
	
	public DataFilterTest(DataFilter filter, double expected[], String method) {
		this.filter = filter;
		this.expected = expected;
		this.method = method;
	}
	
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{ new NoFilter(), 				LATENCY_DATA, 	"NoFilter" },
			{ new MovingAverageFilter(3), 	AVG3_DATA, 		"Avg 3" },
			{ new MovingAverageFilter(5), 	AVG5_DATA , 	"Avg 5" },
			{ new MovingMedianFilter(3), 	MEDIAN3_DATA, 	"Median 3" },
			{ new MovingMedianFilter(5), 	MEDIAN5_DATA, 	"Median 5" },
		});
	}
	
	@Test
	public void testFilter() {
		Assert.assertEquals(method, LATENCY_DATA.length, expected.length);
		for(int i = 0; i<LATENCY_DATA.length; i++) {
			String message = method + ", i=" + i;
			double rawValue = LATENCY_DATA[i];
			double filtered = filter.filter(rawValue);
			Assert.assertEquals(message, expected[i], filtered, DELTA);
		}
	}
}
