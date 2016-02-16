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
public class StreamSetSlicer extends Slicer<StreamSet> implements SliceListener<AudioDispatcher>  {

	private final StreamSet streamSet;
	private int size;
	private AudioDispatcher refSlice;
	private List<AudioDispatcher> slices;
	private StreamSlicer refSlicer;

	/**
	 * Creates a new StreamSetSlicer from a StreamSet.
	 *
	 * @param supply
	 */
	public StreamSetSlicer(final StreamSet streamSet, long interval) {
		super();
		this.streamSet = streamSet;
		this.size = streamSet.getOthers().size();
		this.refSlicer = new StreamSlicer(interval, this);
		this.slices = new ArrayList<>();
		this.streamSet.getReference().addAudioProcessor(refSlicer);
		for (final AudioDispatcher d : this.streamSet.getOthers()) {
			d.addAudioProcessor(new StreamSlicer(interval, this));
		}
	}

	@Override
	public void onSliceEvent(AudioDispatcher slice, Slicer<AudioDispatcher> s) {
		if(s == refSlicer) {
			refSlice = slice;
		}
		else {
			slices.add(slice);
		}
		
		if(slices.size() == size && refSlice != null) {
			emitSliceEvent(new StreamSet(refSlice, slices));
			reset();
		}
	}
	
	private void reset() {
		slices.clear();
		refSlice = null;
	}

	@Override
	public void done(Slicer<AudioDispatcher> s) {
		size--;
		emitDoneEvent();
	}
}
