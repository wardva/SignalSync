package be.signalsync.core;

public interface SliceListener<T> {
	void onSliceEvent(T slices);
}
