package be.signalsync.test;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SuccessTest {
	private int t, a, b;
	
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			new Object[] {2,1},
			new Object[] {4,2},
			new Object[] {6,3},
			new Object[] {8,4},
			new Object[] {10,5},
			new Object[] {12,6},
			new Object[] {14,7},
			new Object[] {16,8},
			new Object[] {18,9},
			new Object[] {20,10},
		});
	}
	
	public SuccessTest(int a, int b) {
		this.a = a;
		this.b = b;
	}
	
	@Before
	public void before() {
		t = 5;
	}
	
	@Test
	public void test() {
		for(int i = 0; i<10; ++i) {
			Assert.assertEquals(5, 5);
		}
		Assert.assertTrue(true);
	}
	
	@Test
	public void test2() {
		Assert.assertEquals(t, 5);
		Assert.assertFalse(false);
	}
	
	public void test3() {
		Assert.assertEquals(a, 2*b);
	}
}
