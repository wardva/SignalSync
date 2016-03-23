package be.signalsync.teensy;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * Copy pasted from the TeensyDAQ project.
 * More information: https://github.com/JorenSix/TeensyDAQ
 * @author Joren Six
 *
 */
public class SerialPortReader {

	private final SerialPort serialPort;
	private final SerialDataLineHandler handler;
	
	public SerialPortReader(String portName,SerialDataLineHandler handler){
		serialPort = new SerialPort(portName);
		this.handler = handler;
	}
	
	public void open(){
		try {
            serialPort.openPort();//Open port
            serialPort.setParams(921600, 8, 1, 0);//Set params
            int mask = SerialPort.MASK_RXCHAR;//Prepare mask
        	//Set mask
			serialPort.setEventsMask(mask);
        }
        catch (SerialPortException ex) {
            System.out.println(ex);
        }
	}
	
	public void start(){
		//Add SerialPortEventListener
		try {
			serialPort.addEventListener(new SerialPortLineReader(serialPort,handler));
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}
	
	public void stop(){
		try {
            serialPort.closePort();//close port
        }
        catch (SerialPortException ex) {
            System.out.println(ex);
        }
	}
	
	public static interface SerialDataLineHandler{
		
		void handleSerialDataLine(int lineNumber, String lineData);
	}
	
	public void write(String line){
		if(!line.endsWith("\n")){
			line = line + "\n";
		}
		try {
			serialPort.writeString(line);
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}
    
    /*
     * In this class must implement the method serialEvent, through it we learn about 
     * events that happened to our port. But we will not report on all events but only 
     * those that we put in the mask. In this case the arrival of the data and change the 
     * status lines CTS and DSR
     */
    public static class SerialPortLineReader implements SerialPortEventListener {
    	private final SerialPort serialPort;
    	private final StringBuilder message = new StringBuilder();
    	private boolean receivingMessage = false;
    	private int lineNumber = 0;
    	private final SerialDataLineHandler handler;
    	
    	public SerialPortLineReader(SerialPort serialPort,SerialDataLineHandler handler){
    		this.serialPort = serialPort;
    		this.handler = handler;
    	}
       
        public void serialEvent(SerialPortEvent event) {
            if(event.isRXCHAR() && event.getEventValue() > 0){//If data is available 
                //Read data, if 10 bytes available 
                try {
            	  byte buffer[] = serialPort.readBytes();
                  for (byte b: buffer) {
                      if (b == 'T') {
                          receivingMessage = true;
                          message.setLength(0);
                      }
                      else if (receivingMessage == true) {
                          if (b == '\n') {
                              receivingMessage = false;
                              lineNumber++;
                              String lineData = message.toString();
                              if(handler!=null){
                            	  handler.handleSerialDataLine(lineNumber, lineData);
                              }
                          }else {
                              message.append((char)b);
                          }
                      }
                  }
                }catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
    }
    
    public static String[] getSerialPorts(){
    	return SerialPortList.getPortNames();
    }
}
