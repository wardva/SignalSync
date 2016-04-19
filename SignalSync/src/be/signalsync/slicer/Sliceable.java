package be.signalsync.slicer;

public interface Sliceable<T, S extends Slicer<T>> {
	S createSlicer(int sliceStep, int sliceSize);
}
