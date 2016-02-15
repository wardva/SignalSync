package be.signalsync.dummy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.core.StreamSet;
import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;

public class DummyStreamSet extends StreamSet {
	private static List<AudioDispatcher> streams;
	private static AudioDispatcher reference;
	static {
		try {
			streams = new ArrayList<>();
			final File dir = new File("./testdata");
			final File files[] = dir.listFiles();
			for (final File f : files) {
				System.out.println(f.getAbsolutePath());
				final AudioDispatcher d = AudioDispatcherFactory.fromFile(f, 
						Config.getInt("BUFFER_SIZE"), 
						0);
				d.addAudioProcessor(new AudioPlayer(d.getFormat()));
				streams.add(d);
			}
			reference = streams.remove(0);
		}
		catch(LineUnavailableException | UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public DummyStreamSet() {
		super(reference, streams);
	}
}
