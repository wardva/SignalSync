package be.signalsync.streamsets;

/**
 * A streamset containing Teensy streams.
 * @author Ward Van Assche
 */
public class TeensyRecordedStreamSet extends FileStreamSet {
	private static String[] streams = new String[] { "./testdata/TeensyRecorded/origineel test soundboard 4.wav", 
			"./testdata/TeensyRecorded/teensy test soundboard 4.wav"};

	public TeensyRecordedStreamSet() {
		super(streams);
	}
}
