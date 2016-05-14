package be.signalsync.slicer;

/**
 * This interface has to be implemented when an instance is interested in slice
 * data of a Stream or StreamSet.
 * @author Ward Van Assche
 * @param <T>
 *            This type defines the slicing result. When a single stream
 *            is sliced, then this type will be float[]. When a StreamSet is sliced, 
 *            then this type will be Map<StreamGroup, float[]>.
 */
public interface SliceListener<T> {
	void done(Slicer<T> s);
	void onSliceEvent(SliceEvent<T> event);
}
