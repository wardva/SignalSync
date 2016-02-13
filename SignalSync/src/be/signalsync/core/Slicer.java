package be.signalsync.core;

import java.util.HashSet;
import java.util.Set;

public abstract class Slicer<T> implements Runnable
{
	private Set<SliceListener<T>> listeners;
	
	public Slicer() {
		listeners = new HashSet<>();
	}
	
	/**
	 * Add an interested listener. All the listeners will be notified
	 * when new synchronisation data is available.
	 * @param listener
	 */
	public void addEventListener(final SliceListener<T> listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a listener from the interested list.
	 * @param listener
	 */
	public void removeEventListener(final SliceListener<T> listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Send a new sliceresult to the interested listeners
	 * @param result
	 */
	public void emitSliceEvent(T result) {
		for(SliceListener<T> l : listeners) {
			l.onSliceEvent(result);
		}
	}
	
	public abstract T slice();
}
