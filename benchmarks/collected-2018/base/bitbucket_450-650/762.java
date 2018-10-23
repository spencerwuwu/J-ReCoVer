// https://searchcode.com/api/result/126160572/

/**
 * OLAT - Online Learning and Training<br>
 * http://www.olat.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.olat.core.commons.chiefcontrollers.BaseChiefController;
import org.olat.core.logging.LogDelegator;
import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.engine.BaseRenderEngine;
import org.radeox.engine.context.BaseInitialRenderContext;
import org.radeox.engine.context.BaseRenderContext;

/**
 * enclosing_type Description: <br>
 * A formatter to format locale-specific things (mainly dates and times)
 * 
 * @author Felix Jost
 */
public class Formatter extends LogDelegator {

	private Locale locale;
	private static RenderEngine engineWithContext;
	private static BaseRenderContext baseRenderContext;

	static {
		InitialRenderContext initialContext = new BaseInitialRenderContext();
		Locale loc = new Locale("olat", "olat");
		initialContext.set(RenderContext.INPUT_LOCALE, loc);
		initialContext.set(RenderContext.OUTPUT_LOCALE, loc);
		initialContext.set(RenderContext.INPUT_BUNDLE_NAME, "radeox_markup_olat");
		initialContext.set(RenderContext.OUTPUT_BUNDLE_NAME, "radeox_markup_olat");
		engineWithContext = new BaseRenderEngine(initialContext);
		baseRenderContext = new BaseRenderContext();
	}

	/**
	 * Constructor for Formatter.
	 */
	private Formatter(Locale locale) {
		this.locale = locale;
	}

	/**
	 * get an instance of the Formatter given the locale
	 * 
	 * @param locale the locale which the formatter should use in its operations
	 * @return the instance of the Formatter
	 */
	public static Formatter getInstance(Locale locale) {
		return new Formatter(locale);
	}

	/**
	 * formats the given date so it is friendly to read
	 * 
	 * @param d the date
	 * @return a String with the formatted date
	 */
	public String formatDate(Date d) {
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
		df.setLenient(false);
		String da = df.format(d);
		return da;
	}

	/**
	 * formats the given time period so it is friendly to read
	 * 
	 * @param d the date
	 * @return a String with the formatted time
	 */
	public String formatTime(Date d) {
		DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);
		df.setLenient(false);
		String da = df.format(d);
		return da;
	}

	/**
	 * formats the given date so it is friendly to read
	 * 
	 * @param d the date
	 * @return a String with the formatted date and time
	 */
	public String formatDateAndTime(Date d) {
		if (d == null) return null;
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
		df.setLenient(false);
		String da = df.format(d);
		return da;
	}

	/**
	 * Generate a simple date pattern that formats a date using the locale of the formatter
	 * 
	 * @return
	 */
	public String getSimpleDatePatternForDate() {
		Calendar cal = new GregorianCalendar();
		cal.set(1999, Calendar.MARCH, 1, 0, 0, 0);
		Date testDate = cal.getTime();
		String formattedDate = formatDate(testDate);
		formattedDate = formattedDate.replace("1999", "%Y");
		formattedDate = formattedDate.replace("99", "%Y");
		formattedDate = formattedDate.replace("03", "%m");
		formattedDate = formattedDate.replace("3", "%m");
		formattedDate = formattedDate.replace("01", "%d");
		formattedDate = formattedDate.replace("1", "%d");
		return formattedDate;
	}

	/**
	 * Generate a simple date pattern that formats a date with time using the locale of the formatter
	 * 
	 * @return
	 */
	public String getSimpleDatePatternForDateAndTime() {
		Calendar cal = new GregorianCalendar();
		cal.set(1999, Calendar.MARCH, 1, 4, 5, 0);
		Date testDate = cal.getTime();
		String formattedDate = formatDateAndTime(testDate);
		formattedDate = formattedDate.replace("1999", "%Y");
		formattedDate = formattedDate.replace("99", "%Y");
		formattedDate = formattedDate.replace("03", "%m");
		formattedDate = formattedDate.replace("3", "%m");
		formattedDate = formattedDate.replace("01", "%d");
		formattedDate = formattedDate.replace("1", "%d");
		formattedDate = formattedDate.replace("04", "%H");
		formattedDate = formattedDate.replace("4", "%H");
		formattedDate = formattedDate.replace("05", "%M");
		formattedDate = formattedDate.replace("5", "%M");
		if (formattedDate.endsWith("AM")) {
			formattedDate = formattedDate.replace("%H", "%I").replace("AM", "%p");
		}
		return formattedDate;
	}

	/**
	 * Formats the given date with the ISO 8601 standard also known as 'datetime' See http://www.w3.org/TR/NOTE-datetime.html for more info.
	 * 
	 * @param d the date to be formatted
	 * @return a String with the formatted date and time
	 */
	public static String formatDatetime(Date d) {
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return formatter.format(d);
	}

	/**
	 * Use this for naming files or directories with a timestamp. As windows does not like ":" in filenames formatDateAndTime(d) does not work
	 * 
	 * @param d the date to be formatted
	 * @return a String with the formatted date and time
	 */
	public static String formatDatetimeFilesystemSave(Date d) {
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss_SSS");
		return formatter.format(d);
	}

	/**
	 * formats the given time period so it is friendly to read
	 * 
	 * @param d the date
	 * @return a String with the formatted time
	 */
	public String formatTimeShort(Date d) {
		DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
		df.setLenient(false);
		String da = df.format(d);
		return da;
	}

	/**
	 * Formats a duration in millis to "XXh YYm ZZs"
	 * 
	 * @param millis
	 * @return formatted string
	 */
	public static String formatDuration(long millis) {
		long h = millis / (1000 * 60 * 60);
		long hmins = h * 1000 * 60 * 60;
		long m = (millis - hmins) / (1000 * 60);
		long s = (millis - hmins - m * 1000 * 60) / 1000;
		return h + "h " + m + "m " + s + "s";
	}

	/**
	 * Escape " with \" in strings
	 * 
	 * @param source
	 * @return escaped string
	 */
	public static StringBuilder escapeDoubleQuotes(String source) {
		if (source == null) return null;
		StringBuilder sb = new StringBuilder(300);
		int len = source.length();
		char[] cs = source.toCharArray();
		for (int i = 0; i < len; i++) {
			char c = cs[i];
			switch (c) {
				case '"':
					sb.append("&quot;");
					break;
				default:
					sb.append(c);
			}
		}
		return sb;
	}

	/**
	 * Escape " with \" and ' with \' in strings
	 * 
	 * @param source
	 * @return escaped string
	 * @deprecated use org.apache.commons.lang.StringEscapeUtils.escapeJavaScript() instead.
	 */
	public static StringBuilder escapeSingleAndDoubleQuotes(String source) {
		if (source == null) return null;
		StringBuilder sb = new StringBuilder(300);
		int len = source.length();
		char[] cs = source.toCharArray();
		for (int i = 0; i < len; i++) {
			char c = cs[i];
			switch (c) {
				case '"':
					sb.append("\\\"");
					break;
				case '\'':
					sb.append("\\\'");
					break;
				default:
					sb.append(c);
			}
		}
		return sb;
	}

	/**
	 * replace a given String with a different String
	 * 
	 * @param source the source
	 * @param delim the String to replace
	 * @param replacement the replacement String
	 * @return StringBuilder
	 */
	public StringBuilder replace(String source, String delim, String replacement) {
		if (source == null) return null;
		StringBuilder sb = new StringBuilder(300);
		StringTokenizer st = new StringTokenizer(source, delim);
		while (st.hasMoreTokens()) {
			String tok = st.nextToken();
			sb.append(tok);
			sb.append(replacement);
		}
		return sb;
	}

	/**
	 * truncates the supplied string to len-3 and replaces the last three positions with ...
	 * 
	 * @param source
	 * @param len
	 * @return truncated string
	 */
	public static String truncate(String source, int len) {
		if (source == null) return null;
		if (source.length() > len && len > 3) return truncate(source, len - 3, "...");
		else return source;
	}

	/**
	 * This returns a substring of a len length from the input string, as opposite to the <code> truncate </code> method. This should be used for processing strings
	 * before writing.
	 * 
	 * @param source
	 * @param len
	 * @return
	 */
	public static String truncateOnly(String source, int len) {
		if (source == null) return null;
		if (source.length() > len) return source.substring(0, len);
		else return source;
	}

	/**
	 * replaces all non ASCII characters in a string and also the most common special characters like by urlencode it "/" "\" ":" "*" "?" """ ' "<" ">" "|"
	 * 
	 * @param source
	 * @return a string which is OS independant and save for using on any filesystem
	 */
	public static String makeStringFilesystemSave(String source) {
		try {
			source = removeUndesirableSubstrings(source);
			return URLEncoder.encode(source, "utf-8");
		} catch (UnsupportedEncodingException e) {
			// utf-8 should be supported
		}
		return "";
	}

	/**
	 * @param source
	 * @return
	 */
	private static String removeUndesirableSubstrings(String source) {
		String returnString = source;

		// replace successive dots with only one, for now.
		while (returnString != null && returnString.indexOf("..") > -1) {
			returnString = returnString.replaceAll("\\.\\.", "."); // replace recursive 2 dots with one
		}

		return returnString;
	}

	/**
	 * truncates a String: useful to limit in GUI
	 * 
	 * @param source
	 * @param len length of the returned string; if negative, return n chars from the end of the string, otherwise from the beginning of the string
	 * @param delim
	 * @return truncated string
	 */
	public static String truncate(String source, int len, String delim) {
		if (source == null) return null;
		int start, stop;
		int alen = source.length();
		if (len > 0) {
			if (alen <= len) return source;
			start = 0; // TODO effizienter
			stop = (len > alen ? alen : len);
			StringBuilder sb = new StringBuilder(source.substring(start, stop));
			if (alen > len) sb.append(delim);
			return sb.toString();
		}
		start = (len < -alen ? 0 : alen + len);
		stop = alen;
		StringBuilder sb = new StringBuilder(source.substring(start, stop));
		if (-alen <= len) sb.insert(0, delim);
		return sb.toString();
	}

	/**
	 * some old testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("hello");
		// log.debug(linePrepend("asdfsdf. bla and. \n2.line\n3.third",">"));
		// log.debug(escape("bla<>and so on &&\nsecond line").toString());

		System.out.println(":" + StringEscapeUtils.escapeHtml("abcdef&<>") + ":");
		System.out.println(":" + StringEscapeUtils.escapeHtml("&#256;<ba>abcdef&<>") + ":");
		System.out.println(":" + StringEscapeUtils.escapeHtml("&#256;\n<ba>\nabcdef&<>") + ":");

		System.out.println(":" + Formatter.truncate("abcdef", 0) + ":");
		System.out.println(":" + Formatter.truncate("abcdef", 2) + ":");
		System.out.println(":" + Formatter.truncate("abcdef", 4) + ":");
		System.out.println(":" + Formatter.truncate("abcdef", 6) + ":");
		System.out.println(":" + Formatter.truncate("abcdef", 7) + ":");
		System.out.println(":" + Formatter.truncate("abcdef", 8) + ":");

		System.out.println(":" + Formatter.truncate("abcdef", -2) + ":");
		System.out.println(":" + Formatter.truncate("abcdef", -4) + ":");
		System.out.println(":" + Formatter.truncate("abcdef", -6) + ":");
		System.out.println(":" + Formatter.truncate("abcdef", -7) + ":");
		System.out.println(":" + Formatter.truncate("abcdef", -8) + ":");

		Locale loc = new Locale("de");
		Formatter f2 = new Formatter(loc);
		Date d = new Date();
		Calendar cal = Calendar.getInstance(loc);
		cal.setTime(d);
		cal.add(Calendar.HOUR_OF_DAY, 7);
		// so ists 16:36 nachmittags
		d = cal.getTime();
		System.out.println(f2.formatDate(d));
		System.out.println(f2.formatTime(d));
		System.out.println(f2.formatDateAndTime(d));

		System.out.println("Now make String filesystem save");
		// String ugly = "\"/asdf/?._||\"blaoau";
		String ugly = "guido/\\:? .|*\"\"<><guidooau";
		System.out.println("input: " + ugly);
		System.out.println("output: " + Formatter.makeStringFilesystemSave(ugly));
	}

	/**
	 * @return locale of this formatter
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * @param source
	 * @return escaped string
	 */
	public static StringBuilder escWithBR(String source) {
		if (source == null) return null;
		StringBuilder sb = new StringBuilder(300);
		int len = source.length();
		char[] cs = source.toCharArray();
		for (int i = 0; i < len; i++) {
			char c = cs[i];
			switch (c) {
				case '"':
					sb.append("&quot;");
					break;
				case '<':
					sb.append("&lt;");
					break;
				case '>':
					sb.append("&gt;");
					break;
				case '&':
					// check on &# first (entities -> don't escape)
					if (i < len - 1 && cs[i + 1] == '#') { // we have # as next char
						sb.append("&");
					} else {
						sb.append("&amp;");
					}
					break;
				// case '\r':sb.append("<br />"); break;
				case '\n':
					sb.append("<br />");
					break;
				default:
					sb.append(c);
			}
		}
		return sb;
	}

	/**
	 * @param source
	 * @return stripped string
	 */
	public static StringBuilder stripTabsAndReturns(String source) {
		if (source == null) return null;
		StringBuilder sb = new StringBuilder(300);
		int len = source.length();
		char[] cs = source.toCharArray();
		for (int i = 0; i < len; i++) {
			char c = cs[i];
			switch (c) {
				case '\t':
					sb.append("    ");
					break;
				case '\n':
					sb.append("<br />");
					break;
				case '\r':
					sb.append("<br />");
					break;
				case '\u2028': // unicode linebreak
					sb.append("<br />");
					break;
				default:
					sb.append(c);
			}
		}
		return sb;
	}

	/**
	 * renders wiki markup like _italic_ to XHTML see also www.radeox.org
	 * 
	 * @Deprecated The wiki markup area is no longer supported. In the legacy form infrastructure it's still there, but it won't be available in the new flexi forms. In
	 *             flexi forms use the RichTextElement instead. tested during migration and expanded to prevent radeox failures
	 * @param originalText
	 * @return result (rendered originalText) or null if originalText was null
	 */
	@Deprecated
	public static String formatWikiMarkup(String oldValue) {
		if (oldValue != null) {
			String newValue = "";
			// oldValue = oldValue.replaceAll("<>", "&lt;&gt;");
			// oldValue = oldValue.replaceAll(Pattern.quote("[]"),
			// "&#91;&#93;");

			// prevent error with {$} interpreted as regexp
			String marker1 = "piYie6Eigh0phafeiTuk4dahwahvoh7eedoegee2egh8xuj9phah8eop8iuk";
			oldValue = oldValue.replaceAll(Pattern.quote("{$}"), marker1);

			// \{code} will result in an error => convert
			String marker2 = "RohbaeW7xahbohk8iewoo7thocaemaech2pahS8oe1UVohkohJiugaagaeco";
			oldValue = oldValue.replaceAll(Pattern.quote("\\{code}"), marker2);

			// radeox gets an error, if {code} is not a closed tag. prevent at
			// least the case with one single statement.
			int nrOfCodeStatements = countOccurrences(oldValue, "{code}");
			String marker3 = "shagheiph6enieNo0theph9aique0EihoChae6ve2edie4Pohwaok8thaoda";
			if (nrOfCodeStatements == 1) {
				oldValue = oldValue.replaceAll(Pattern.quote("{code}"), marker3);
			}
			if (nrOfCodeStatements % 2 != 0 && nrOfCodeStatements != 1) {
				Formatter fInst = Formatter.getInstance(new Locale("olat"));
				fInst.log("There will be a Warning/NPE from Radeox soon, as there are not enough {code} statements in a text.");
				fInst.log("Old value of text will be kept! " + oldValue);
			}

			// added for compatibility with wikimedia syntax used in the new wiki component. org.olat.core.gui.components.wiki.WikiMarkupComponent
			// filters " ''' " for bold and " ''''' " for bold/italic
			oldValue = oldValue.replaceAll("(^|>|[\\p{Punct}\\p{Space}]+)'{3}(.*?)'{3}([\\p{Punct}\\p{Space}]+|<|$)", "$1*$2*$3");
			oldValue = oldValue.replaceAll("(^|>|[\\p{Punct}\\p{Space}]+)'{5}(.*?)'{5}([\\p{Punct}\\p{Space}]+|<|$)", "$1_*$2*_$3");

			// try-catch not usable, as Radeox doesn't throw an exception,
			// it just prints warnings and returns unconverted value!
			newValue = engineWithContext.render(oldValue, baseRenderContext);

			// convert back
			newValue = newValue.replaceAll(marker1, Matcher.quoteReplacement("{$}"));
			newValue = newValue.replaceAll(marker2, Matcher.quoteReplacement("\\{code}"));
			newValue = newValue.replaceAll(marker3, Matcher.quoteReplacement("{code}"));

			return newValue;
		} else return null;
	}

	private static int countOccurrences(String arg1, String arg2) {
		int count = 0;
		int index = 0;
		while ((index = arg1.indexOf(arg2, index)) != -1) {
			++index;
			++count;
		}
		return count;
	}

	private void log(String msg) {
		logWarn(msg, null);
	}

	/**
	 * Wrapp given html code with a wrapper an add code to transform latex formulas to nice visual characters on the client side. The latex formulas must be within an
	 * HTML element that has the class 'math' attached.
	 * 
	 * @param htmlFragment A html element that might contain an element that has a class 'math' with latex formulas
	 * @return
	 */
	public static String formatLatexFormulas(String htmlFragment) {
		if (htmlFragment == null) return "";
		// optimize, reduce jsmath calls on client
		if (BaseChiefController.isJsMathEnabled() && (htmlFragment.contains("class='math'") || htmlFragment.contains("class=\"math\""))) {
			// add math wrapper
			String domid = "mw_" + CodeHelper.getRAMUniqueID();
			String elem = htmlFragment.contains("<div") ? "div" : "span";
			StringBuffer sb = new StringBuffer();

			sb.append("<").append(elem).append(" id=\"").append(domid).append("\">");
			sb.append(htmlFragment);
			sb.append("</").append(elem).append(">");
			sb.append("<script type='text/javascript'>/* <![CDATA[ */ BFormatter.formatLatexFormulas('").append(domid).append("');/* ]]> */</script>");
			return sb.toString();
		}
		return htmlFragment;
	}

	/**
	 * Round a double value to a double value with given number of figures after comma
	 * 
	 * @param value
	 * @param decimalPlace
	 * @return rounded double value
	 */
	public static double round(double value, int decimalPlace) {
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		value = bd.doubleValue();
		return value;
	}

	/**
	 * Round a float value to a float value with given number of figures after comma
	 * 
	 * @param value
	 * @param decimalPlace
	 * @return rounded float value
	 */
	public static float round(float value, int decimalPlace) {
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		value = bd.floatValue();
		return value;
	}

	/**
	 * Format a float as string with given number of figures after comma
	 * 
	 * @param value
	 * @param decimalPlace
	 * @return formatted string
	 */
	public static String roundToString(float value, int decimalPlace) {
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		return bd.toString();
	}

}

