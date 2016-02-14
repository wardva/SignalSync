package be.signalsync.core;

import java.util.List;

import be.tarsos.dsp.AudioDispatcher;

/**
 * An interface for a class containing different streams which should be
 * synchronized.
 * 
 * @author Ward Van Assche
 *
 */
public interface StreamSet extends Runnable {
	AudioDispatcher getReference();
	List<AudioDispatcher> getStreams();
	int size();
}
