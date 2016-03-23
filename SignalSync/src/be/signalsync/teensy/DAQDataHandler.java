package be.signalsync.teensy;

public interface DAQDataHandler {
	
	public void handle(DAQSample sample);
	public void stopDataHandler();

}
