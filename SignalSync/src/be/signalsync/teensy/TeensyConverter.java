package be.signalsync.teensy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
	private List<Queue<Byte>> buffers;
	private List<InputStream> streams;
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
			Queue<Byte> buffer = new ConcurrentLinkedQueue<Byte>();
			buffers.add(buffer);
			InputStream inputStream = new TeensyInputStream(buffer);
			streams.add(inputStream);
		}
		teensy.addDataHandler(this);
	}
	
	public AudioDispatcher getAudioDispatcher(int index) {
		InputStream input = streams.get(index);
		AudioInputStream audioStream = new AudioInputStream(input, format, AudioSystem.NOT_SPECIFIED);
		TarsosDSPAudioInputStream tarsosStream = new JVMAudioInputStream(audioStream);
		AudioDispatcher dispatcher = new AudioDispatcher(tarsosStream, audioBufferSize, 0);
		return dispatcher;
	}
	
	public InputStream getInputStream(int index) {
		return streams.get(index);
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
			teensy.stop();
			for(InputStream s : streams) {
				s.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static class TeensyInputStream extends InputStream {
		private boolean closed;
		private Queue<Byte> q;
		
		public TeensyInputStream(Queue<Byte> q) {
			super();
			this.q = q;
			this.closed = false;
		}

		@Override
		public int read() throws IOException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int available() throws IOException {
			return q.size();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if(closed) {
				throw new IOException("The teensy input stream has been closed!");
			} else if (b == null) {
	            throw new NullPointerException();
	        } else if (off < 0 || len < 0 || len > b.length - off) {
	            throw new IndexOutOfBoundsException();
	        } else if (len == 0) {
	            return 0;
	        }

			int i = 0;
			while(!q.isEmpty() && i<len) {
				b[off + i++] = q.poll();
			}
			return i;
		}
		
		@Override
		public void close() throws IOException {
			this.closed = true;
			q.clear();
		}
	}
}
