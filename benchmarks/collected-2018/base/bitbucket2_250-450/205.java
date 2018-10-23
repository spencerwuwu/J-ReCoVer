// https://searchcode.com/api/result/66702701/

package org.easycloud.las.core.cfg;


import org.easycloud.las.core.util.Resources;
import org.easycloud.las.core.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import static org.easycloud.las.core.util.Assert.assertStateHasLength;
import static org.easycloud.las.core.util.Assert.assertStateNotNull;


/**
 * The root class for getting properties operations, it cant be use directly and need to be extended.
 * It's recommended that subclasses use singleton to reduce the I/O cost.
 *
 * Created by IntelliJ IDEA.
 * User: Meng, Ke
 * Date: 13-5-9
 */
public class Configuration {

	private Properties properties;
	protected String resourceName;

	/**
	 * The default constructor
	 */
	public Configuration() {
		properties = new Properties();
	}

	public Configuration(String resourceName) {
		this();
		this.resourceName = resourceName;
	}

	protected void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	/**
	 * Get the value of the <code>name</code> property as a trimmed <code>String</code>,
	 * <code>null</code> if no such property exists.
	 *
	 * @param name the property name.
	 * @return the value of the <code>name</code> or its replacing property,
	 *         or null if no such property exists.
	 */
	public String get(String name) {
		Properties props = getProps();
		return props.getProperty(name);
	}

	/**
	 * Get the value of the <code>name</code> property, <code>defaultValue</code> if
	 * no such property exists.
	 *
	 * @param name the property name.
	 * @param defaultValue the default value.
	 * @return the value of the <code>name</code> or its replacing property,
	 *         or null if no such property exists.
	 */
	public String get(String name, String defaultValue) {
		Properties props = getProps();
		return props.getProperty(name, defaultValue);
	}

	/**
	 * Get the value of the <code>name</code> property as a trimmed <code>String</code>,
	 * <code>null</code> if no such property exists.
	 *
	 * @param name the property name.
	 * @return the value of the <code>name</code> or its replacing property,
	 *         or null if no such property exists.
	 */
	public String getTrimmed(String name) {
		String value = get(name);
		if (null == value) {
			return null;
		} else {
			return value.trim();
		}
	}

	/**
	 * Get the value of the <code>name</code> property as an <code>int</code>.
	 *
	 * If no such property exists, the provided default value is returned,
	 * or if the specified value is not a valid <code>int</code>,
	 * then an error is thrown.
	 *
	 * @param name property name.
	 * @param defaultValue default value.
	 * @throws NumberFormatException when the value is invalid
	 * @return property value as an <code>int</code>,
	 *         or <code>defaultValue</code>.
	 */
	public int getInt(String name, int defaultValue) {
		String valueString = getTrimmed(name);
		if (valueString == null)
			return defaultValue;
		String hexString = getHexDigits(valueString);
		if (hexString != null) {
			return Integer.parseInt(hexString, 16);
		}
		return Integer.parseInt(valueString);
	}

	/**
	 * Get the comma delimited values of the <code>name</code> property as
	 * a collection of <code>String</code>s.
	 * If no such property is specified then empty collection is returned.
	 * <p>
	 * This is an optimized version of {@link #getStrings(String)}
	 *
	 * @param name property name.
	 * @return property value as a collection of <code>String</code>s.
	 */
	public Collection<String> getStringCollection(String name) {
		String valueString = get(name);
		return StringUtil.getStringCollection(valueString);
	}

	/**
	 * Get the comma delimited values of the <code>name</code> property as
	 * an array of <code>String</code>s.
	 * If no such property is specified then <code>null</code> is returned.
	 *
	 * @param name property name.
	 * @return property value as an array of <code>String</code>s,
	 *         or <code>null</code>.
	 */
	public String[] getStrings(String name) {
		String valueString = get(name);
		return StringUtil.getStrings(valueString);
	}

	/**
	 * Get the comma delimited values of the <code>name</code> property as
	 * an array of <code>String</code>s.
	 * If no such property is specified then default value is returned.
	 *
	 * @param name property name.
	 * @param defaultValue The default value
	 * @return property value as an array of <code>String</code>s,
	 *         or default value.
	 */
	public String[] getStrings(String name, String... defaultValue) {
		String valueString = get(name);
		if (valueString == null) {
			return defaultValue;
		} else {
			return StringUtil.getStrings(valueString);
		}
	}

	/**
	 * Get the comma delimited values of the <code>name</code> property as
	 * a collection of <code>String</code>s, trimmed of the leading and trailing whitespace.
	 * If no such property is specified then empty <code>Collection</code> is returned.
	 *
	 * @param name property name.
	 * @return property value as a collection of <code>String</code>s, or empty <code>Collection</code>
	 */
	public Collection<String> getTrimmedStringCollection(String name) {
		String valueString = get(name);
		if (null == valueString) {
			Collection<String> empty = new ArrayList<String>();
			return empty;
		}
		return StringUtil.getTrimmedStringCollection(valueString);
	}

	/**
	 * Get the comma delimited values of the <code>name</code> property as
	 * an array of <code>String</code>s, trimmed of the leading and trailing whitespace.
	 * If no such property is specified then an empty array is returned.
	 *
	 * @param name property name.
	 * @return property value as an array of trimmed <code>String</code>s,
	 *         or empty array.
	 */
	public String[] getTrimmedStrings(String name) {
		String valueString = get(name);
		return StringUtil.getTrimmedStrings(valueString);
	}

	/**
	 * Get the comma delimited values of the <code>name</code> property as
	 * an array of <code>String</code>s, trimmed of the leading and trailing whitespace.
	 * If no such property is specified then default value is returned.
	 *
	 * @param name property name.
	 * @param defaultValue The default value
	 * @return property value as an array of trimmed <code>String</code>s,
	 *         or default value.
	 */
	public String[] getTrimmedStrings(String name, String... defaultValue) {
		String valueString = get(name);
		if (null == valueString) {
			return defaultValue;
		} else {
			return StringUtil.getTrimmedStrings(valueString);
		}
	}

	/**
	 * Get the value of the <code>name</code> property as a set of comma-delimited
	 * <code>int</code> values.
	 *
	 * If no such property exists, an empty array is returned.
	 *
	 * @param name property name
	 * @return property value interpreted as an array of comma-delimited
	 *         <code>int</code> values
	 */
	public int[] getInts(String name) {
		String[] strings = getTrimmedStrings(name);
		int[] ints = new int[strings.length];
		for (int i = 0; i < strings.length; i++) {
			ints[i] = Integer.parseInt(strings[i]);
		}
		return ints;
	}

	/**
	 * Get the value of the <code>name</code> property as a <code>long</code>.
	 * If no such property exists, the provided default value is returned,
	 * or if the specified value is not a valid <code>long</code>,
	 * then an error is thrown.
	 *
	 * @param name property name.
	 * @param defaultValue default value.
	 * @throws NumberFormatException when the value is invalid
	 * @return property value as a <code>long</code>,
	 *         or <code>defaultValue</code>.
	 */
	public long getLong(String name, long defaultValue) {
		String valueString = getTrimmed(name);
		if (valueString == null)
			return defaultValue;
		String hexString = getHexDigits(valueString);
		if (hexString != null) {
			return Long.parseLong(hexString, 16);
		}
		return Long.parseLong(valueString);
	}

	/**
	 * Get the value of the <code>name</code> property as a <code>float</code>.
	 * If no such property exists, the provided default value is returned,
	 * or if the specified value is not a valid <code>float</code>,
	 * then an error is thrown.
	 *
	 * @param name property name.
	 * @param defaultValue default value.
	 * @throws NumberFormatException when the value is invalid
	 * @return property value as a <code>float</code>,
	 *         or <code>defaultValue</code>.
	 */
	public float getFloat(String name, float defaultValue) {
		String valueString = getTrimmed(name);
		if (valueString == null)
			return defaultValue;
		return Float.parseFloat(valueString);
	}


	/**
	 * Get the value of the <code>name</code> property as a <code>double</code>.
	 * If no such property exists, the provided default value is returned,
	 * or if the specified value is not a valid <code>double</code>,
	 * then an error is thrown.
	 *
	 * @param name property name.
	 * @param defaultValue default value.
	 * @throws NumberFormatException when the value is invalid
	 * @return property value as a <code>double</code>,
	 *         or <code>defaultValue</code>.
	 */
	public double getDouble(String name, double defaultValue) {
		String valueString = getTrimmed(name);
		if (valueString == null)
			return defaultValue;
		return Double.parseDouble(valueString);
	}

	/**
	 * Get the value of the <code>name</code> property as a <code>boolean</code>.
	 * If no such property is specified, or if the specified value is not a valid
	 * <code>boolean</code>, then <code>defaultValue</code> is returned.
	 *
	 * @param name property name.
	 * @param defaultValue default value.
	 * @return property value as a <code>boolean</code>,
	 *         or <code>defaultValue</code>.
	 */
	public boolean getBoolean(String name, boolean defaultValue) {
		String valueString = getTrimmed(name);
		if (null == valueString || valueString.isEmpty()) {
			return defaultValue;
		}

		valueString = valueString.toLowerCase();

		if ("true".equals(valueString))
			return true;
		else if ("false".equals(valueString))
			return false;
		else return defaultValue;
	}

	/**
	 * Return the number of keys in the configuration.
	 *
	 * @return number of keys in the configuration.
	 */
	public int size() {
		return getProps().size();
	}

	private String getHexDigits(String value) {
		boolean negative = false;
		String str = value;
		String hexString = null;
		if (value.startsWith("-")) {
			negative = true;
			str = value.substring(1);
		}
		if (str.startsWith("0x") || str.startsWith("0X")) {
			hexString = str.substring(2);
			if (negative) {
				hexString = "-" + hexString;
			}
			return hexString;
		}
		return null;
	}

	private synchronized Properties getProps() {
		assertStateHasLength(resourceName, "Configuration getProps resourceName null or empty");
		if (properties == null || properties.size() == 0) {
			properties = Resources.loadProperties(resourceName);
		}
		assertStateNotNull(properties, "Configuration getProps There's no property in " + resourceName + ". Please check it.");
		return properties;
	}
}

