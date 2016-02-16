package be.signalsync.dummy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.core.StreamSet;
import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class DummyTwoStreamSet extends StreamSet {
	private static List<AudioDispatcher> streams;
	private static AudioDispatcher reference;

	static {
		try {
			streams = new ArrayList<>();
			final File f1 = new File("./testdata/Sonic Youth - Star Power.wav");
			final File f2 = new File("./testdata/Sonic Youth - Star Power_300.wav");
			final AudioDispatcher d1 = AudioDispatcherFactory.fromFile(f1, Config.getInt("BUFFER_SIZE"), 0);
			final AudioDispatcher d2 = AudioDispatcherFactory.fromFile(f2, Config.getInt("BUFFER_SIZE"), 0);
			streams.add(d1);
			streams.add(d2);
			reference = streams.remove(0);
		} catch (UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
	}

	public DummyTwoStreamSet() {
		super(reference, streams);
	}
}
