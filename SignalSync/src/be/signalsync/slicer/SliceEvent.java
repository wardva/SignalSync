package be.signalsync.slicer;

public class SliceEvent<T> {
	private Slicer<T> slicer;
	private T slice;
	private double beginTime;
	
	public SliceEvent(Slicer<T> slicer, T slices, double beginTime) {
		this.slicer = slicer;
		this.slice = slices;
		this.beginTime = beginTime;
	}
	public Slicer<T> getSlicer() {
		return slicer;
	}
	public T getSlice() {
		return slice;
	}
	public double getBeginTime() {
		return beginTime;
	}
}
