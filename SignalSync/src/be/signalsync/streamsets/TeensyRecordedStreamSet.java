package be.signalsync.streamsets;

/**
 * A streamset containing Teensy streams.
 * @author Ward Van Assche
 */
public class TeensyRecordedStreamSet extends FileStreamSet {
	private static String[] streams = new String[] { "./testdata/TeensyRecorded/origineel test soundboard 3.wav", 
			"./testdata/TeensyRecorded/teensy test soundboard 3.wav"};

	public TeensyRecordedStreamSet() {
		super(streams);
	}
}
