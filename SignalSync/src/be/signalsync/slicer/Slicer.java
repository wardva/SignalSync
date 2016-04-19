package be.signalsync.slicer;

import java.util.HashSet;
import java.util.Set;

/**
 * The supertype of the different types of Slicers. This class handles the event
 * emitting and listening.
 * 
 * @author Ward Van Assche
 * @param <T> The return type of the slice method.
 */
public abstract class Slicer<T> {
	private final Set<SliceListener<T>> listeners;

	public Slicer() {
		listeners = new HashSet<>();
	}

	/**
	 * Add an interested listener. All the listeners will be notified when a new
	 * slice(s) is available.
	 *
	 * @param listener
	 */
	public void addEventListener(final SliceListener<T> listener) {
		listeners.add(listener);
	}

	/**
	 * Execute the done method of the interested listeners.
	 */
	public void emitDoneEvent() {
		for (final SliceListener<T> l : listeners) {
			l.done(this);
		}
	}

	/**
	 * Send a new slice result to the interested listeners
	 * @param result The slice result.
	 */
	public void emitSliceEvent(final T result, double beginTime) {
		SliceEvent<T> event = new SliceEvent<T>(this, result, beginTime);
		for (final SliceListener<T> l : listeners) {
			l.onSliceEvent(event);
		}
	}

	/**
	 * Remove a listener from the interested set.
	 * @param listener
	 */
	public void removeEventListener(final SliceListener<T> listener) {
		listeners.remove(listener);
	}
}
