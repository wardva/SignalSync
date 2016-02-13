package be.signalsync.app;

import java.io.File;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;

public class MusicPlayerTest {
	public static void main(final String[] args) {
		new MusicPlayerTest();
	}

	private final int samplerate = 8000;
	private final int size = 512;

	private final int overlap = 256;

	public MusicPlayerTest() {
		try {
			final File f = new File("./testdata/Sonic Youth - Star Power.wav");
			final AudioDispatcher d = AudioDispatcherFactory.fromPipe(f.getAbsolutePath(), samplerate, size, overlap);
			// AudioDispatcher d = AudioDispatcherFactory.fromFile(f, size,
			// overlap);
			d.addAudioProcessor(new AudioPlayer(d.getFormat()));
			d.run();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
