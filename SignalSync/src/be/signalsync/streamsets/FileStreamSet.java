package be.signalsync.streamsets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class FileStreamSet extends StreamSet {
	private List<File> files;

	public FileStreamSet(String[] filenames) {
		super();
		this.files = new ArrayList<>();
		for(String filename : filenames) {
			files.add(new File(filename));
		}
		this.streamExecutor = Executors.newFixedThreadPool(filenames.length);
		reset();
	}
	
	@Override
	public void reset() {
		try {
			streams.clear();
			for(File f : files) {
				streams.add(AudioDispatcherFactory.fromFile(f, Config.getInt(Key.BUFFER_SIZE), 0));
			}
		} 
		catch (UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
	}
}
