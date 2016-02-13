package be.signalsync.app;

import be.panako.strategy.nfft.NFFTEventPointProcessor;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;

public class Test {
	private final static String FILEPATH = "C:\\Users\\Ward\\Documents\\Masterproef\\TestData\\TestBestanden\\Sonic Youth - Star Power.wav";

	public static void main(final String[] args) {
		// StreamSupply s = new TestStreamSupply();
		final int samplerate = 8000;
		final int size = 512;
		final int overlap = 256;

		try {
			// AudioDispatcher d = AudioDispatcherFactory.fromFile(f, size,
			// overlap);
			final AudioDispatcher d = AudioDispatcherFactory.fromPipe(FILEPATH, samplerate, size, overlap);
			final NFFTEventPointProcessor minMaxProcessor = new NFFTEventPointProcessor(size, overlap, samplerate);
			d.addAudioProcessor(new AudioPlayer(JVMAudioInputStream.toAudioFormat(d.getFormat())));
			d.addAudioProcessor(new AudioProcessor() {
				@Override
				public boolean process(final AudioEvent audioEvent) {
					System.out.println("Processed buffer");
					return true;
				}

				@Override
				public void processingFinished() {
					System.out.println("Finished!");
				}
			});

			d.addAudioProcessor(minMaxProcessor);
			d.run();
			System.out.println("Fingerprints: " + minMaxProcessor.getFingerprints().size());

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
