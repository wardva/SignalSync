package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;

/**
 * This class is a Slicer which is used to take slices of different streams
 * wrapped in a StreamSuppy.
 *
 * @author Ward Van Assche
 *
 */
public class StreamSetSlicer extends Slicer<StreamSet> {

	private final StreamSet streamSet;
	private final List<StreamSlicer> slicers;

	/**
	 * Creates a new StreamSetSlicer from a StreamSet.
	 *
	 * @param supply
	 */
	public StreamSetSlicer(final StreamSet streamSet) {
		super();
		this.streamSet = streamSet;
		slicers = new ArrayList<>();
		// Reference stream slicer at index 0 of slicers.
		slicers.add(new StreamSlicer(streamSet.getReference()));
		for (final AudioDispatcher d : this.streamSet.getOthers()) {
			final StreamSlicer s = new StreamSlicer(d);
			slicers.add(s);
		}
	}

	/**
	 * To create the different slices this method makes use of the StraemSlicer
	 * class.
	 *
	 * @return List<AudioDispatcher> A list of slices (AudioDispatchers).
	 */
	@Override
	public StreamSet slice() {
		final List<AudioDispatcher> slices = new ArrayList<>();
		for (final StreamSlicer s : slicers) {
			slices.add(s.slice());
		}
		final StreamSet sliceSet = new StreamSet(slices.remove(0), slices);
		return sliceSet;
	}
}
