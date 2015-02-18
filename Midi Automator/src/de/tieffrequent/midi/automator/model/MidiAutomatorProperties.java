package de.tieffrequent.midi.automator.model;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public class MidiAutomatorProperties extends Properties {

	private static final long serialVersionUID = 1L;

	private String propertiesFilePath;

	public static final String KEY_MIDI_IN_REMOTE_DEVICE = "MIDI_IN_REMOTE_DEVICE";
	public static final String KEY_MIDI_IN_METRONOM_DEVICE = "MIDI_IN_METRONOM_DEVICE";
	public static final String KEY_MIDI_OUT_REMOTE_DEVICE = "MIDI_OUT_REMOTE_DEVICE";
	public static final String KEY_MIDI_OUT_SWITCH_NOTIFIER_DEVICE = "MIDI_OUT_SWITCH_NOTIFIER_DEVICE";
	public static final String KEY_PREV_MIDI_SIGNATURE = "PREV_MIDI_SIGNATURE";
	public static final String KEY_NEXT_MIDI_SIGNATURE = "NEXT_MIDI_SIGNATURE";
	public static final String KEY_GUI_AUTOMATION_IMAGES = "GUI_AUTOMATION_IMAGE";
	public static final String KEY_GUI_AUTOMATION_TYPES = "GUI_AUTOMATION_TYPE";
	public static final String KEY_GUI_AUTOMATION_TRIGGERS = "GUI_AUTOMATION_TRIGGER";
	public static final String KEY_GUI_AUTOMATION_MIDI_SIGNATURES = "GUI_AUTOMATION_MIDI_SIGNATURE";
	public static final String KEY_GUI_AUTOMATION_MIN_DELAYS = "GUI_AUTOMATION_MIN_DELAY";
	public static final String INDEX_SEPARATOR = "_";
	public static final String VALUE_NULL = "-none-";

	/**
	 * Standard constructor
	 * 
	 * @param path
	 *            The path to the properties file
	 * 
	 */
	public MidiAutomatorProperties(String path) {
		super();
		propertiesFilePath = path;
	}

	/**
	 * Stores properties to the file.
	 */
	public void store() {

		Writer writer = null;
		try {
			writer = new FileWriter(propertiesFilePath);
			store(writer, null);
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Loads properties from the file.
	 */
	public void load() {

		Reader reader = null;
		try {
			reader = new FileReader(propertiesFilePath);
			load(reader);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public Object setProperty(String key, String value) {
		if (value == null) {
			value = "";
		}
		Object obj = super.setProperty(key, value);
		return obj;
	}

	/**
	 * Returns a Set of all key-value pairs where the key contains a given
	 * substring.
	 * 
	 * @param subKey
	 *            The keys substring
	 * @return A filtered Set of all key-value pairs
	 */
	public Set<Entry<Object, Object>> entrySet(String subKey) {
		Set<Entry<Object, Object>> keyValuePairs = entrySet();
		Set<Entry<Object, Object>> filteredKeyValuePairs = new HashSet<Entry<Object, Object>>();

		for (Entry<Object, Object> keyValuePair : keyValuePairs) {

			String key = (String) keyValuePair.getKey();
			String value = (String) keyValuePair.getValue();

			if (key.contains(subKey)) {
				Entry<Object, Object> filteredKeyValuePair = new AbstractMap.SimpleEntry<Object, Object>(
						key, value);
				filteredKeyValuePairs.add(filteredKeyValuePair);
			}
		}

		return filteredKeyValuePairs;
	}

	/**
	 * Removes a key value pair, where the key contains a given sub string.
	 * 
	 * @param subKey
	 *            The substring of the key
	 */
	public void removeKeys(String subKey) {
		Set<Entry<Object, Object>> keyValuePairs = entrySet(subKey);

		for (Entry<Object, Object> keyValuePair : keyValuePairs) {
			String key = (String) keyValuePair.getKey();
			remove(key);
		}
	}

	/**
	 * Gets the index of a property key
	 * 
	 * @param key
	 *            The property key
	 * @return The index of the key, <NULL> if there is no key
	 */
	public static int getIndexOfPropertyKey(String key) {
		String[] splittedKey = key.split(MidiAutomatorProperties.INDEX_SEPARATOR);
		String index = splittedKey[splittedKey.length - 1];
		return Integer.parseInt(index);
	}
}
