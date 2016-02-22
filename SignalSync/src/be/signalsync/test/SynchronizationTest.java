package be.signalsync.test;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import be.signalsync.core.RealtimeStreamSync;
import be.signalsync.streamsets.FileStreamSet;
import be.signalsync.streamsets.StreamSet;

@RunWith(Parameterized.class)
public class SynchronizationTest {
	
	private final static String referencePath = "./testdata/Sonic Youth - Star Power.wav";
	private StreamSet streams;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
        	 { "./testdata/Sonic Youth - Star Power.wav", 		0.000F }, 
             { "./testdata/Sonic Youth - Star Power_20.wav", 	0.020F }, 
             { "./testdata/Sonic Youth - Star Power_80.wav", 	0.080F }, 
             { "./testdata/Sonic Youth - Star Power_90.wav", 	0.090F }, 
             { "./testdata/Sonic Youth - Star Power_300.wav", 	0.300F }, 
             { "./testdata/Sonic Youth - Star Power_2000.wav", 	2.000F } 
       });
    }
    
    public SynchronizationTest(String otherPath, float expectedLatency) {
    	streams = new FileStreamSet(new String[] {referencePath, otherPath});
    }
	
	@Test
	public void test() {
		RealtimeStreamSync syncer = new RealtimeStreamSync(streams);
		//Mockito.mock(RealtimeStreamSync.class).
	}

}
