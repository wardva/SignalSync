package be.signalsync.util;

import java.util.ArrayList;
import java.util.List;

public class Util {
	
	/**
	 * Convert a float array to an ArrayList containing.
	 * the float values.
	 * @param array The float array.
	 * @return An ArrayList containing the float values. The order
	 * is maintained.
	 */
	public static List<Float> floatArrayToList(float[] array) {
		List<Float> list = new ArrayList<Float>(array.length);
	    for (float value : array) {
	        list.add(value);
	    }
	    return list;
	}
}
