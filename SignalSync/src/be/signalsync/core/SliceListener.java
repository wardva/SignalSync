package be.signalsync.core;

/**
 * This interface has to be implemented when an instance is interested in slice
 * data of a stream or a StreamSet.
 *
 * @author Ward Van Assche
 *
 * @param <T>
 *            This type defines the slicing result. When a single stream
 *            (instance of AudioDispatcher) is sliced, then this type has to be
 *            AudioDispatcher. When a StreamSet is sliced, then this type has
 *            to be List<AudioDispatcher>.
 */
public interface SliceListener<T> {
	void onSliceEvent(T slices);
}
