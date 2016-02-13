
package be.signalsync.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple configuration class. The keys and values are read from a file. The
 * name of the file has to be specified in the FILE_NAME attribute. Values can
 * be requested with the get and getInt methods.
 * 
 * @author Ward Van Assche
 */
public class Config {

	private static final String FILE_NAME = "config.properties";
	private static Properties properties;

	static {
		try {
			final InputStream input = new FileInputStream(FILE_NAME);
			properties = new Properties();
			properties.load(input);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public static String get(final String key) {
		if (properties.containsKey(key)) {
			return properties.getProperty(key);
		}
		throw new IllegalArgumentException(String.format("Key %s does not exist.", key));
	}

	public static int getInt(final String key) {
		return Integer.parseInt(get(key));
	}
}
