package be.signalsync.streamsets;

public class DummySingleStreamSet extends FileStreamSet {
	private static String[] files = new String[] { "./testdata/Sonic Youth - Star Power.wav" };

	public DummySingleStreamSet() {
		super(files);
	}
}
