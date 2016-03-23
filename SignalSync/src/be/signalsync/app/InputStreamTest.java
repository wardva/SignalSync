package be.signalsync.app;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamTest {

	public static void main(String[] args) {
		try {
			InputStream input = new DummyInputStream();
			byte[] buffer = new byte[1024];
			int nrRead = input.read(buffer, 0, buffer.length);
			printByteArray(buffer);
			System.out.println("Nr read: " + nrRead);
			input.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static class DummyInputStream extends InputStream {
		private int values[] = new int[] {0,59,254,180,1,51,1,75,254,252,254,180,1,67,254,188,0,171,255,148,0,75,0,227,254,140,0,139,254,173,0,171,254,133,0,180,254,93,1,4,254,213,0,164,254,149,0,132,0,204,254,229,0,188,1,132,0,99,254,149,0,180,254,141,0,204,254,37,0,92,1,28,0,36,0,172,254,53,1,20,254,125,1,76,254,157,1,60,255,45,0,212,254,173,0,172,254,254,0,252,1,12,1,76,0,219,255,37,1,27,254,245,0,235,254,69,0,228,1,83,0,139,255,68,1,35,255,28,1,51,0,251,254,92,1,59,254,164,1,43,254,116,0,251,254,76,1,51,255,36,1,235,255,44,1,170,1,170,254,52,0,66,254,188,0,243,1,90,0,2,254,52,1,50,254,188,1,66,255,155,0,210,1,66,254,147,0,234,255,115,1,34,255,91,0,210,255,11,1,58,254,235,0,90,254,171,0,162,0,194,255,99,1,101,89,0,41,254,243,1,121,255,99,1,89,255,42,1,97,1,113,0,97,254,154,1,1,1,0,254,146,0,137,254,138,1,33,254,146,1,9,254,250,0,168,254,177,0,152,254,217,0,248,0,216,254,177,1,96,254,177,1,16,254,73,0,128,0,248,255,233,254,113,0,88,254,185,0,240,1,32,254,161,1,96,254,153,0,112,-1};
		private int i = 0;
		
		public DummyInputStream() {
			System.out.println("DummyInputStream created, number of values: " + (values.length - 1));
			printIntBuffer(values);
		}
		
		@Override
		public int read() throws IOException {
			return values[i++];
		}
	}
	
	private static void printIntBuffer(int[] array) {
		System.out.print("IntBuffer:\t[");
		for(int i = 0; i<array.length-1; i++) {
			System.out.printf("0x%X,", ((byte)(array[i]-128)));
		}
		System.out.printf("0x%X]\n", array[array.length-1]);
	}
	
	private static void printByteArray(byte[] array) {
		System.out.print("ByteBuffer:\t[");
		for(int i = 0; i<array.length-1; i++) {
			System.out.printf("0x%X,", array[i]);
		}
		System.out.printf("0x%X]\n", array[array.length-1]);
	}
}
