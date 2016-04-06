package be.signalsync.core;

public class SliceEvent<T> {
	private Slicer<T> slicer;
	private T slices;
	private double beginTime;
	
	public SliceEvent(Slicer<T> slicer, T slices, double beginTime) {
		this.slicer = slicer;
		this.slices = slices;
		this.beginTime = beginTime;
	}
	public Slicer<T> getSlicer() {
		return slicer;
	}
	public T getSlices() {
		return slices;
	}
	public double getBeginTime() {
		return beginTime;
	}
}
