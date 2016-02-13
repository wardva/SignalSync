package be.signalsync.core;

/**
 * Contains the synchronization data of the different streams.
 * 
 * @author Ward Van Assche
 *
 */
public class SyncData {
	private String data;

	public SyncData(final String data) {
		setData(data);
	}

	public String getData() {
		return data;
	}

	public void setData(final String data) {
		this.data = data;
	}

}
