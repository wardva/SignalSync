package be.signalsync.core;

import be.tarsos.dsp.AudioDispatcher;

public class SpliceResult {
	private AudioDispatcher splice;
	private AudioDispatcher remainder;
	public AudioDispatcher getSplice() {
		return splice;
	}
	public void setSplice(AudioDispatcher splice) {
		this.splice = splice;
	}
	public AudioDispatcher getRemainder() {
		return remainder;
	}
	public void setRemainder(AudioDispatcher remainder) {
		this.remainder = remainder;
	}
	public SpliceResult(AudioDispatcher splice, AudioDispatcher remainder) {
		this.splice = splice;
		this.remainder = remainder;
	}
}
