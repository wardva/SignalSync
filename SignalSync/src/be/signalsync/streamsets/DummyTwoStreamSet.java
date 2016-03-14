package be.signalsync.streamsets;


/**
 * A streamset containing some given streams.
 * @author Ward Van Assche
 */
public class DummyTwoStreamSet extends FileStreamSet {
	private static String[] streams = new String[] { "./testdata/Recorded/opname-reference.wav",
			"./testdata/Recorded/opname-1-7995.wav" };

	public DummyTwoStreamSet() {
		super(streams);
	}
}
