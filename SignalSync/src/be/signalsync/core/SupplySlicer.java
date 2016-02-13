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
public class SupplySlicer extends Slicer<List<AudioDispatcher>> {

	private final StreamSupply supply;
	private final List<StreamSlicer> slicers;

	/**
	 * Creates a new SupplySlicer from a StreamSupply.
	 * 
	 * @param supply
	 */
	public SupplySlicer(final StreamSupply supply) {
		super();
		this.supply = supply;
		slicers = new ArrayList<>(supply.size());
		for (final AudioDispatcher d : this.supply.getStreams()) {
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
	public List<AudioDispatcher> slice() {
		final List<AudioDispatcher> slices = new ArrayList<>();
		for (final StreamSlicer s : slicers) {
			slices.add(s.slice());
		}
		return slices;
	}
}
