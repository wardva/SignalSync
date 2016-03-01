package be.signalsync.streamsets;

public class DummyTwoStreamSet extends FileStreamSet {
	private static String[] streams = new String[] { "./testdata/Sonic Youth - Star Power.wav",
			"./testdata/Sonic Youth - Star Power_2000_100hz.wav" };

	public DummyTwoStreamSet() {
		super(streams);
	}
}
