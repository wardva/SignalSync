package be.signalsync.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;

public class EventsToAudioDispatcher {
	private int bufferSize;
	private BlockingQueue<Byte> buffer;
	private AudioDispatcherStream stream;
	private AudioFormat format;
	
	//Moving average attributes
	private boolean takeAverage;
	private Deque<Double> movingAverageWindow;
	private int movingAverageWindowSize;
	private double currentAverageSum;
	
	public EventsToAudioDispatcher(double sampleRate, int bufferSize, boolean takeAverage) {
		this.bufferSize = bufferSize;
		this.format = new AudioFormat((float) sampleRate, 16, 1, true, true);
		this.buffer = new LinkedBlockingQueue<>();
		this.stream = new AudioDispatcherStream(buffer);
		this.movingAverageWindow = new LinkedList<>();
		this.movingAverageWindowSize = (int) (2 * sampleRate);
		this.currentAverageSum = 0.0D;
	}
	
	public AudioDispatcher createAudioDispatcher() {
		AudioInputStream audioStream = new AudioInputStream(stream, format, AudioSystem.NOT_SPECIFIED);
		TarsosDSPAudioInputStream tarsosStream = new JVMAudioInputStream(audioStream);
		AudioDispatcher dispatcher = new AudioDispatcher(tarsosStream, bufferSize, 0);
		return dispatcher;
	}
	
	public void audioEvent(float buffer[]) {
		for(float f : buffer) {
			audioEvent(f);
		}
	}
	
	public void audioEvent(double sample) {
		int x = (int) (sample * 32767.0);
        byte out1 = (byte) (x >>> 8);
        byte out2 = (byte) x;
		buffer.add(out1);
		buffer.add(out2);
	}
	
	public void stop() {
		try {
			this.stream.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void voltageEvent(double sample, double max) {
		double modifiedSampleData;
		if(takeAverage) {
			movingAverageWindow.addLast(sample);
			currentAverageSum += sample;
			if(movingAverageWindow.size() > movingAverageWindowSize){
				currentAverageSum -= movingAverageWindow.removeFirst();
			}
			double movingAverage = (currentAverageSum/movingAverageWindow.size());
			double corrected = sample - movingAverage;
			modifiedSampleData = corrected/(max/2);
		}
		else {
			modifiedSampleData = sample-max/2;
		}
		audioEvent(modifiedSampleData);
	}
	
	private static class AudioDispatcherStream extends InputStream {
		private boolean closed;
		private BlockingQueue<Byte> buffer;
		
		public AudioDispatcherStream(BlockingQueue<Byte> buffer) {
			super();
			this.buffer = buffer;
			this.closed = false;
		}

		@Override
		public int read() throws IOException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int available() throws IOException {
			return buffer.size();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if(closed) {
				throw new IOException("The max input stream has been closed!");
			} else if (b == null) {
	            throw new NullPointerException();
	        } else if (off < 0 || len < 0 || len > b.length - off) {
	            throw new IndexOutOfBoundsException();
	        } else if (len == 0) {
	            return 0;
	        }
			try {
				int i = 0;
				boolean stopped = false;
				do {
					Byte value = buffer.poll(1, TimeUnit.SECONDS);
					if(value == null) {
						System.err.println("Events to audio: value = null");
						stopped = true;
					} 
					else {
						b[off + i++] = value;
					}
				}
				while(!buffer.isEmpty() && i<len && !stopped && !closed);
				return i;
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			return -1;
		}
		
		@Override
		public void close() throws IOException {
			this.closed = true;
			buffer.clear();
		}
	}
}
