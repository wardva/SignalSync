package be.signalsync.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import be.signalsync.teensy.TeensyConverter;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

public class Test {
	public static void main(String[] args) {
		ExecutorService teensyExecutor = Executors.newSingleThreadExecutor();
		TeensyConverter teensy = new TeensyConverter(11025, "COM5", 0, 1, 64, 1);
		teensy.start();
		AudioDispatcher teensyStream = teensy.getAudioDispatcher(0);
		teensyStream.addAudioProcessor(new AudioProcessor() {
			@Override
			public void processingFinished() {
				System.out.println("Processing finished");
			}
			
			@Override
			public boolean process(AudioEvent audioEvent) {
				System.out.println("Processing: " + audioEvent.getBufferSize());
				return true;
			}
		});
		teensyExecutor.execute(teensyStream);
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
