package be.signalsync.core;

import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;

public class SupplySlicer extends Slicer<List<AudioDispatcher>> {
	
	private StreamSupply supply;
	private List<StreamSlicer> slicers;
	
	public SupplySlicer(StreamSupply supply) {
		super();
		this.supply = supply;
		this.slicers = new ArrayList<>(supply.size());
		for(AudioDispatcher d : this.supply.getStreams()) {
			StreamSlicer s = new StreamSlicer(d);
			slicers.add(s);
		}
	}
	
	@Override
	public List<AudioDispatcher> slice() {
		List<AudioDispatcher> slices = new ArrayList<>();
		for(StreamSlicer s : slicers) {
			slices.add(s.slice());
		}
		return slices;
	}
	
	@Override
	public void run() {
		List<AudioDispatcher> slices = slice();
		emitSliceEvent(slices);
	}
}
