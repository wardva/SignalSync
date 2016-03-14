package be.signalsync.app;

import javax.sound.sampled.LineUnavailableException;

import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;

public class MicrophoneTest {
	public static void main(final String[] args) {
		try {
			final AudioDispatcher d = AudioDispatcherFactory.fromDefaultMicrophone(Config.getInt(Key.NFFT_BUFFER_SIZE), 0);
			d.addAudioProcessor(new AudioPlayer(d.getFormat()));
			d.run();
		} catch (final LineUnavailableException e) {
			e.printStackTrace();
		}
	}
}
