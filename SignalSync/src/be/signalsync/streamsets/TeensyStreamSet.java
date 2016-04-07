package be.signalsync.streamsets;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.teensy.TeensyConverter;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.WaveformWriter;

public class TeensyStreamSet extends StreamSet {
	private TeensyConverter converter;
	private AudioDispatcher origineel;
	private AudioDispatcher teensy;
	
	public TeensyStreamSet() {
		super();
		try {
			converter = new TeensyConverter();
			streams.clear();
			
			origineel = AudioDispatcherFactory.fromFile(new File("./testdata/Origineel/starpower11k.wav"), Config.getInt(Key.NFFT_BUFFER_SIZE), 0);
			teensy = converter.getAudioDispatcher(0);
			
			origineel.addAudioProcessor(new WaveformWriter(origineel.getFormat(), "origineel"));
			teensy.addAudioProcessor(new WaveformWriter(teensy.getFormat(), "teensy"));

			streams.add(origineel);
			streams.add(teensy);
			streamExecutor = Executors.newFixedThreadPool(2);
		} 
		catch (UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void startTeensy() {
		converter.start();
	}
	
	@Override
	public void stop() {
		super.stop();
		converter.stop();
	}
}
