package be.signalsync.dummy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.core.StreamSupply;
import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;

public class DummySingleStreamSupply implements StreamSupply {
	private List<AudioDispatcher> streams;
	private ExecutorService streamExecutor;

	public DummySingleStreamSupply() {
		try {
			streams = new ArrayList<>();
			final File f = new File("./testdata/Sonic Youth - Star Power.wav");
			final AudioDispatcher d = AudioDispatcherFactory.fromFile(f, 
					Config.getInt("BUFFER_SIZE"), 
					0);
			d.addAudioProcessor(new AudioPlayer(d.getFormat()));
			streams.add(d);
			streamExecutor = Executors.newFixedThreadPool(streams.size());
		} catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public AudioDispatcher getReference() {
		return streams.size() > 0 ? streams.get(0) : null;
	}

	@Override
	public List<AudioDispatcher> getStreams() {
		return streams;
	}

	@Override
	public void run() {
		for (final AudioDispatcher d : streams) {
			streamExecutor.execute(d);
		}
	}

	@Override
	public int size() {
		return streams.size();
	}
}
