package be.signalsync.app;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.core.StreamSlicer;
import be.signalsync.util.Config;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;


public class SlicerTest {
	public SlicerTest() {
		try {
			File f = new File("./testdata/Sonic Youth - Star Power.wav");
			AudioDispatcher initialDispatcher = AudioDispatcherFactory.fromFile(f, Config.getInt("BUFFER_SIZE"), 0);
			initialDispatcher.addAudioProcessor(new AudioPlayer(initialDispatcher.getFormat()));
			StreamSlicer slicer = new StreamSlicer(initialDispatcher);
			
			slicer.start();
			Thread.sleep(1000);
			AudioDispatcher oneSecondSonicYouth = slicer.slice();
			slicer.stop();
			Thread.sleep(1000);
			oneSecondSonicYouth.addAudioProcessor(new AudioPlayer(oneSecondSonicYouth.getFormat()));
			oneSecondSonicYouth.run();
		} 
		catch (InterruptedException | UnsupportedAudioFileException | LineUnavailableException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new SlicerTest();
	}
}
