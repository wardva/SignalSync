package be.signalsync.app;

import javax.sound.sampled.LineUnavailableException;

import be.signalsync.teensy.TeensyConverter;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioPlayer;
//import be.tarsos.dsp.io.jvm.WaveformWriter;

public class TeensyTester {
	public static void main(String[] args) {
		try {
			TeensyConverter converter = new TeensyConverter();
			AudioDispatcher d = converter.getAudioDispatcher(0);
			d.addAudioProcessor(new AudioPlayer(d.getFormat()));
	//		d.addAudioProcessor(new WaveformWriter(d.getFormat(), "WaveFile"));
			converter.start();
			Thread t = new Thread(d);
	//		Thread.sleep(2000);
			t.start();
	//		Thread.sleep(5000);
	//		d.stop();
		} 
		catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
}
