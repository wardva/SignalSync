package be.signalsync.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.net.URL;
//import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Properties;
import java.util.prefs.Preferences;

/**
 * Writes and read the configuration values to and from a properties file.
 * 
 * @author Joren Six
 */
public class Config {

	private static Config instance;

	public static String get(final Key key) {
		// re read configuration
		// getInstance().readConfigration();
		final HashMap<Key, String> store = getInstance().configrationStore;
		final String value;
		if (store.get(key) != null) {
			value = store.get(key).trim();
		} else {
			value = key.getDefaultValue();
		}
		return value;
	}

	public static boolean getBoolean(final Key key) {
		return get(key).equalsIgnoreCase("true");
	}

	public static float getFloat(final Key key) {
		return Float.parseFloat(get(key));
	}

	public static Config getInstance() {
		if (instance == null) {
			instance = new Config();
		}
		return instance;
	}

	public static int getInt(final Key key) {
		return Integer.parseInt(get(key));
	}
	
	public static double getDouble(final Key key) {
		return Double.parseDouble(get(key));
	}

	/**
	 * Use preferences to store configuration that changes during runtime and
	 * need to be persisted.
	 * 
	 * @param key
	 *            The key to store.
	 * @return The configured preference value.
	 */
	public static String getPreference(final String key) {
		return getInstance().readPreference(key);
	}

	/**
	 * Sets a configuration value to use during the runtime of the application.
	 * These configuration values are not persisted. To
	 * 
	 * @param key
	 *            The key to set.
	 * @param value
	 *            The value to use.
	 */
	public static void set(final Key key, final String value) {
		final HashMap<Key, String> store = getInstance().configrationStore;
		store.put(key, value);
	}

	/**
	 * Use preferences to store configuration that changes during runtime and
	 * need to be persisted.
	 * 
	 * @param key
	 *            The key to store.
	 * @param value
	 *            The value to store
	 */
	public static void setPreference(final String key, final String value) {
		getInstance().writePreference(key, value);
	}

	/**
	 * The file on disk that is used to store the configuration values. On
	 * Android this can be stored here: res/raw/
	 */
	private final String configrationFileName;

	/**
	 * The values are stored here, in memory.
	 */
	private final HashMap<Key, String> configrationStore;

	private final Preferences preferenceStore;

	/**
	 * Hidden default constructor. Reads the configured values, or stores the
	 * defaults.
	 */
	public Config() {
		//final String path = Config.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		//String decodedPath = "";
		preferenceStore = Preferences.userNodeForPackage(Config.class);

		//try {
		//	decodedPath = URLDecoder.decode(path, "UTF-8");
		//} catch (final UnsupportedEncodingException e) {
		//	e.printStackTrace();
		//}
		//configrationFileName = new File(new File(decodedPath).getParent(), "config.properties").getAbsolutePath();
		//TODO: fixen!!!
		configrationFileName = "D:\\Documenten\\School\\Master\\Masterproef\\Git\\SignalSync\\config.properties";
		
		configrationStore = new HashMap<Key, String>();
		if (!new File(configrationFileName).exists()) {
			writeDefaultConfigration();
		}
		readConfigration();
	}

	/**
	 * Read configuration from properties file on disk.
	 */
	private void readConfigration() {
		final Properties prop = new Properties();
		try {
			// Loads a properties file.
			final FileInputStream inputstream = new FileInputStream(configrationFileName);
			prop.load(inputstream);
			inputstream.close();
			for (final Key key : Key.values()) {
				final String configuredValue = prop.getProperty(key.name());
				configrationStore.put(key, configuredValue);
			}
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	public String readPreference(final String preferenceKey) {
		return preferenceStore.get(preferenceKey, "");
	}

	private void writeDefaultConfigration() {
		final Properties prop = new Properties();
		try {
			// Set the default properties value.
			for (final Key key : Key.values()) {
				prop.setProperty(key.name(), key.defaultValue);
			}
			// Save the properties to the configuration file.
			prop.store(new FileOutputStream(configrationFileName), null);
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	public void writePreference(final String preferenceKey, final String value) {
		preferenceStore.put(preferenceKey, value);
	}

}
