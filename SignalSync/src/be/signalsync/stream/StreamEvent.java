package be.signalsync.stream;

/**
 * This class contains a timestamp and a small buffer of 
 * floating point values from a stream. 
 * 
 * @author Ward Van Assche
 */
public class StreamEvent {
	private float[] floatBuffer;
	private double timestamp;
	
	public StreamEvent(float[] floatBuffer, double timeStamp) {
		this.floatBuffer = floatBuffer;
		this.timestamp = timeStamp;
	}
	public float[] getFloatBuffer() {
		return floatBuffer;
	}
	public double getTimeStamp() {
		return timestamp;
	}
	public int getBufferSize() {
		return getFloatBuffer().length;
	}
}
