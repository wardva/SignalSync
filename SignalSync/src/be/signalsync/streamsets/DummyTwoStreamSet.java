package be.signalsync.streamsets;


/**
 * A streamset containing some given streams.
 * @author Ward Van Assche
 *
 */
public class DummyTwoStreamSet extends FileStreamSet {
	private static String[] streams = new String[] { "./testdata/opname-reference.wav",
			"./testdata/opname-1.wav", "./testdata/opname-2.wav", "./testdata/opname-3.wav" };

	public DummyTwoStreamSet() {
		super(streams);
	}
}
