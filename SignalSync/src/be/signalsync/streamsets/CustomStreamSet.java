package be.signalsync.streamsets;


/**
 * A streamset containing some given streams.
 * @author Ward Van Assche
 */
public class CustomStreamSet extends FileStreamSet {
	private static String[] streams = new String[] { "./testdata/Clean/Sonic Youth - Star Power_0_0hz.wav",
			"./testdata/Clean/Sonic Youth - Star Power_300_50hz.wav" };

	public CustomStreamSet() {
		super(streams);
	}
}
