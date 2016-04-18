package be.signalsync.app;

import be.signalsync.teensy.TeensyConverter;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioPlayer;

public class TeensyTester {
	public static void main(String[] args) {
		try {
			TeensyConverter converter = new TeensyConverter(11025, "COM5", 0, 0, 1, 512);
			AudioDispatcher d = converter.getAudioDispatcher(0);
			d.addAudioProcessor(new AudioPlayer(d.getFormat()));
			d.addAudioProcessor(new AudioProcessor() {
				
				@Override
				public void processingFinished() {
					System.out.println("finished");
				}
				
				@Override
				public boolean process(AudioEvent audioEvent) {
					System.out.println("Hello world");
					return true;
				}
			});
			Thread t = new Thread(d);
			t.start();
			converter.start();
			Thread.sleep(15000);
			d.stop();
			converter.stopDataHandler();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
