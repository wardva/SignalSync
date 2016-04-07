package be.signalsync.streamsets;


/**
 * A streamset containing some streams.
 * @author Ward Van Assche
 */
public class CleanStreamSet extends FileStreamSet {
	private static String[] streams = new String[] { 
			"./testdata/Clean/Sonic Youth - Star Power_90_0hz.wav", 
			"./testdata/Clean/Sonic Youth - Star Power_0_0hz.wav",
			"./testdata/Clean/Sonic Youth - Star Power_2000_0hz.wav", 
			"./testdata/Clean/Sonic Youth - Star Power_300_0hz.wav" };
	public CleanStreamSet() {
		super(streams);
	}
}
