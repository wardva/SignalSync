package be.signalsync.app;

import be.signalsync.teensy.TeensyConverter;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.WaveformWriter;

public class TeensyTester {
	public static void main(String[] args) {
		try {
			TeensyConverter converter = new TeensyConverter();
			AudioDispatcher d = converter.getAudioDispatcher(0);
			d.addAudioProcessor(new WaveformWriter(d.getFormat(), "testfile"));
			converter.start();
			Thread t = new Thread(d);
			t.start();
			Thread.sleep(15000);
			d.stop();
			System.out.println("Stopped");
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
