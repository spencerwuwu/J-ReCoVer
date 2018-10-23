// https://searchcode.com/api/result/71570736/

package me.hardtack.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtendableFormatter extends Object {
	private class Replace {
		public int start;
		public int end;
		public String string;
	}

	private static final String regex = "%" // Percent
			+ "(\\d+\\$)?" // Argument index
			+ "([" + Pattern.quote("-#+\\s0,(") + "]+)?" // Flag
			+ "(\\d+)?" // Width
			+ "(\\.\\d+)?" // Precision
			+ "((t[^\\d\\s]|[^\\d\\s]))"; // Conversion
	private static final Pattern formatPattern = Pattern.compile(regex);

	private Formatter formatter;
	private List<Converter> converters;

	public List<Converter> getConverters() {
		return converters;
	}

	public void setConverters(List<Converter> converters) {
		this.converters = converters;
	}

	public String preprocessFormat(Locale l, String format, Object... args) {
		Matcher matcher = ExtendableFormatter.formatPattern.matcher(format);
		List<Replace> replaces = new LinkedList<ExtendableFormatter.Replace>();
		int index = 0;
		while (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end();

			FormatSpecifier specifier = new FormatSpecifier(matcher.group(1),
					matcher.group(2), matcher.group(3), matcher.group(4),
					matcher.group(5));
			boolean handled = false;
			for (Converter converter : this.converters) {
				if (converter.shouldHandle(specifier)) {
					Replace r = new Replace();
					r.start = start;
					r.end = end;
					r.string = converter.format(l, specifier, index, args);
					replaces.add(r);
					handled = true;
					break;
				}
			}
			if (!handled && specifier.getArgumentIndex() == null) {
				Replace r = new Replace();
				r.start = start;
				r.end = end;
				specifier.setArgumentIndex(index + 1);
				r.string = specifier.toString();
				replaces.add(r);
			}
			index++;
		}
		int reduce = 0;
		for (Replace replace : replaces) {
			int len = replace.end - replace.start;
			format = format.substring(0, replace.start - reduce)
					+ replace.string
					+ format.substring(replace.end - reduce, format.length());
			reduce += len - replace.string.length();
		}
		return format;
	}

	public String preprocessFormat(String format, Object... args) {
		return this.preprocessFormat(this.formatter.locale(), format, args);
	}

	public ExtendableFormatter format(Locale l, String format, Object... args) {
		format = this.preprocessFormat(format, args);
		this.formatter.format(l, format, args);
		return this;
	}

	public ExtendableFormatter format(String format, Object... args) {
		format = this.preprocessFormat(format, args);
		this.formatter.format(format, args);
		return this;
	}

	public Formatter getFormatter() {
		return formatter;
	}

	public ExtendableFormatter() {
		this.formatter = new Formatter();
	}

	public ExtendableFormatter(Appendable a) {
		this.formatter = new Formatter(a);
	}

	public ExtendableFormatter(Appendable a, Locale l) {
		this.formatter = new Formatter(a, l);
	}

	public ExtendableFormatter(File file) throws FileNotFoundException {
		this.formatter = new Formatter(file);
	}

	public ExtendableFormatter(File file, String csn)
			throws FileNotFoundException, UnsupportedEncodingException {
		this.formatter = new Formatter(file, csn);
	}

	public ExtendableFormatter(File file, String csn, Locale l)
			throws FileNotFoundException, UnsupportedEncodingException {
		this.formatter = new Formatter(file, csn, l);
	}

	public ExtendableFormatter(Locale l) {
		this.formatter = new Formatter(l);
	}

	public ExtendableFormatter(OutputStream os) {
		this.formatter = new Formatter(os);
	}

	public ExtendableFormatter(OutputStream os, String csn)
			throws UnsupportedEncodingException {
		this.formatter = new Formatter(os, csn);
	}

	public ExtendableFormatter(OutputStream os, String csn, Locale l)
			throws UnsupportedEncodingException {
		this.formatter = new Formatter(os, csn, l);
	}

	public ExtendableFormatter(PrintStream ps) {
		this.formatter = new Formatter(ps);
	}

	public ExtendableFormatter(String fileName) throws FileNotFoundException {
		this.formatter = new Formatter(fileName);
	}

	public ExtendableFormatter(String fileName, String csn)
			throws FileNotFoundException, UnsupportedEncodingException {
		this.formatter = new Formatter(fileName, csn);
	}

	public ExtendableFormatter(String fileName, String csn, Locale l)
			throws FileNotFoundException, UnsupportedEncodingException {
		this.formatter = new Formatter(fileName, csn, l);
	}

	public void close() {
		this.formatter.close();
	}

	public void flush() {
		this.formatter.flush();
	}

	public IOException ioException() {
		return this.formatter.ioException();
	}

	public Locale locale() {
		return this.formatter.locale();
	}

	public Appendable out() {
		return this.formatter.out();
	}

	public String toString() {
		return this.formatter.toString();
	}
}

