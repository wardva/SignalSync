package be.signalsync.core;

import java.util.HashSet;
import java.util.Set;

/**
 * The supertype of the different types of Slicers. This class handles the event
 * emitting and listening, and provides an abstract slice method. A slicer can
 * be used by using the slice method directly, or by using a thread or
 * threadpool. It can be useful to use a threadpool (ScheduledExecutorService)
 * when the slice method has to be called each time interval. If you use the
 * concurrent variant, you only can retrieve the slice result by registring an
 * instance of SliceListener using the addEventListener method.
 *
 * @author Ward Van Assche
 *
 * @param <T>
 *            The return type of the slice method.
 */
public abstract class Slicer<T> implements Runnable {
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
	 * Send a new slice result to the interested listeners
	 * 
	 * @param result
	 *            The slice result.
	 */
	public void emitSliceEvent(final T result) {
		for (final SliceListener<T> l : listeners) {
			l.onSliceEvent(result);
		}
	}

	/**
	 * Remove a listener from the interested set.
	 * 
	 * @param listener
	 */
	public void removeEventListener(final SliceListener<T> listener) {
		listeners.remove(listener);
	}

	/**
	 * Execute the slice result and notify the interested listeners.
	 */
	@Override
	public void run() {
		final T result = slice();
		emitSliceEvent(result);
	}

	/**
	 * This method has to contain the actual slice implementation.
	 * 
	 * @return The slice result. This result is an instance of AudioDispatcher
	 *         when one stream is sliced (AudioDispatcher), and an instance of
	 *         List<AudioDispatcher when multiple streams (StreamSet) is
	 *         sliced.
	 */
	public abstract T slice();
}
