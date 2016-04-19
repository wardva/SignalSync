package be.signalsync.stream;

public class StreamEvent {
	private float[] floatBuffer;
	private double timeStamp;
	
	public StreamEvent(float[] floatBuffer, double timeStamp) {
		this.floatBuffer = floatBuffer;
		this.timeStamp = timeStamp;
	}
	public float[] getFloatBuffer() {
		return floatBuffer;
	}
	public void setFloatBuffer(float[] data) {
		this.floatBuffer = data;
	}
	public double getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(double timeStamp) {
		this.timeStamp = timeStamp;
	}
	public int getBufferSize() {
		return getFloatBuffer().length;
	}
}
