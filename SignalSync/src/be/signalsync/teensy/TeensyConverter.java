package be.signalsync.teensy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;

public class TeensyConverter implements DAQDataHandler {

	private int audioBufferSize;
	private double gain;
	private TeensyDAQ teensy;
	private List<BlockingQueue<Byte>> buffers;
	private List<TarsosDSPAudioInputStream> streams;
	private AudioFormat format;
	
	private final int movingAverageWindowSize = 1000;
	private Deque<Double> movingAverageWindow;
	private double currentAverageSum;
	
	public TeensyConverter(int sampleRate,String portName,int startChannel,int numberOfChannels,int bufferSize,double gain) {
		this.audioBufferSize = bufferSize;
		this.gain = gain;
		this.teensy = new TeensyDAQ(sampleRate, portName, startChannel, numberOfChannels);
		this.format = new AudioFormat(sampleRate, 16, 1, true, true);
		this.buffers = new ArrayList<>(numberOfChannels);
		this.streams = new ArrayList<>(numberOfChannels);
		this.movingAverageWindow = new LinkedList<>();
		for(int i = 0; i<numberOfChannels; i++) {
			BlockingQueue<Byte> buffer = new LinkedBlockingQueue<>();
			InputStream inputStream = new TeensyInputStream(buffer);
			AudioInputStream audioStream = new AudioInputStream(inputStream, format, AudioSystem.NOT_SPECIFIED);
			TarsosDSPAudioInputStream tarsosStream = new JVMAudioInputStream(audioStream);
			buffers.add(buffer);
			streams.add(tarsosStream);
		}
		teensy.addDataHandler(this);
	}
	
	public AudioDispatcher getAudioDispatcher(int nr) {
		TarsosDSPAudioInputStream stream = streams.get(nr);
		AudioDispatcher dispatcher = new AudioDispatcher(stream, audioBufferSize, 0);
		return dispatcher;
	}
	
	public TeensyConverter() {
		this(Config.getInt(Key.SAMPLE_RATE), 
			 Config.get(Key.TEENSY_PORT), 
			 Config.getInt(Key.TEENSY_START_CHANNEL), 
			 Config.getInt(Key.TEENSY_CHANNEL_COUNT), 
			 Config.getInt(Key.NFFT_BUFFER_SIZE),
			 Config.getDouble(Key.TEENSY_GAIN));
	}
	
	public void start() {
		teensy.start();
	}
	
	public void stop() {
		teensy.stop();
		stopDataHandler();
	}
	
	@Override
	public void handle(DAQSample sample) {
		double sampleData = sample.data[0];
		
		movingAverageWindow.addLast(sampleData);
		currentAverageSum += sampleData;
		if(movingAverageWindow.size() > movingAverageWindowSize){
			currentAverageSum -= movingAverageWindow.removeFirst();
		}
		double movingAverage = (currentAverageSum/movingAverageWindow.size());
		double corrected = sampleData - movingAverage;
		double normalized = (corrected/(1.65)) * gain;
		
		int x = (int) (normalized * 32767.0);
        byte out1 = (byte) (x >>> 8);
        byte out2 = (byte) x;
        buffers.get(0).offer(out1);
        buffers.get(0).offer(out2);
	}

	@Override
	public void stopDataHandler() {
		try {
			for(TarsosDSPAudioInputStream s : streams) {
				s.close();
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static class TeensyInputStream extends InputStream {
		private boolean running;
		private BlockingQueue<Byte> q;
		
		public TeensyInputStream(BlockingQueue<Byte> q) {
			super();
			this.q = q;
			this.running = true;
		}

		@Override
		public int read() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (b == null) {
	            throw new NullPointerException();
	        } else if (off < 0 || len < 0 || len > b.length - off) {
	            throw new IndexOutOfBoundsException();
	        } else if(!running && q.size() == 0) {
	        	return -1;
	        } 
	        else if (len == 0) {
	            return 0;
	        }
			
			try {
				byte first = q.take();
				b[off] = first;
				
				int i = 1;
				while(!q.isEmpty() && i<len) {
					b[off + i++] = q.take();
				}
				return i;
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
				return -1;
			}
		}
		
		@Override
		public void close() throws IOException {
			super.close();
			this.running = false;
		}
	}
}
