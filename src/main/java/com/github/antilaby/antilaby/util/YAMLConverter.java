package com.github.antilaby.antilaby.util;

import com.github.antilaby.antilaby.main.AntiLaby;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Does all Converting Magic.
 */
public final class YAMLConverter {

	/**
	 * Flats the given Map by joining keys
	 *
	 * @param source
	 * 		the nested Map
	 *
	 * @return the flattened Map
	 */
	@SuppressWarnings("unchecked")
	@Nonnull
	public static SortedMap<String, Object> flatten(Map<String, Object> source) {
		SortedMap<String, Object> result = new TreeMap<>();
		for(String key : source.keySet()) {
			Object value = source.get(key);
			if(value instanceof Map) {
				Map<String, Object> subMap = flatten((Map<String, Object>) value);
				for(String subkey : subMap.keySet()) result.put(key + '.' + subkey, subMap.get(subkey));
			} else if(value instanceof Collection) {
				StringBuilder joiner = new StringBuilder();
				String separator = "";
				for(Object element : ((Collection) value)) {
					Map<String, Object> subMap = flatten(Collections.singletonMap(key, element));
					joiner.append(separator).append(subMap.entrySet().iterator().next().getValue().toString());

					separator = ",";
				}
				result.put(key, joiner.toString());
			} else result.put(key, value);
		}
		return result;
	}

	/**
	 * Converts the given Yaml String-only Configuration to Properties.<p>Usage:</p> <p><code> for(Map.Entry&lt;String,
	 * String&gt; e : convertToLocale (yaml).entrySet())<br> yourWritingMethod(e.getKey() + '=' +
	 * e.getValue());</code></p><i>Warning: Non-String entries (like Collections) will be lost</i>
	 *
	 * @param yaml
	 * 		the YamlFile to convert
	 *
	 * @return the resulting Map
	 */
	@Nonnull
	public static SortedMap<String, String> convertYmlToProperties(File yaml) {
		SortedMap<String, String> result = new TreeMap<>();
		try(FileInputStream fileInputStream = new FileInputStream(yaml)) {
			for(Map.Entry<String, Object> entry : flatten(new Yaml().load(fileInputStream)).entrySet())
				if(entry.getValue() instanceof String) result.put(entry.getKey(), (String) entry.getValue());
			fileInputStream.close();
		} catch(IOException e) {
			AntiLaby.LOG.error(e.getMessage());
		}
		return result;
	}

	/**
	 * Private constructor, no need to instantiate this class
	 */
	private YAMLConverter() {throw new UnsupportedOperationException();}
}