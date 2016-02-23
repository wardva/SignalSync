package be.signalsync.streamsets;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class FileStreamSet extends StreamSet {
	public FileStreamSet(final String[] filenames) {
		super();
		try {
			streamExecutor = Executors.newFixedThreadPool(filenames.length);
			streams.clear();
			for (final String f : filenames) {
				streams.add(AudioDispatcherFactory.fromFile(new File(f), Config.getInt(Key.BUFFER_SIZE), 0));
			}
		} catch (UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
	}
}