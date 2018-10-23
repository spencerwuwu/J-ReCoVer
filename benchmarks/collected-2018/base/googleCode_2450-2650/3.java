// https://searchcode.com/api/result/8190092/

package info.bliki.wiki.client.filter;

import info.bliki.wiki.client.filter.WikipediaFilter.InvalidInputException;
import info.bliki.wiki.client.filter.tags.AbstractTag;
import info.bliki.wiki.client.filter.tags.CloseTagToken;
import info.bliki.wiki.client.filter.tags.OpenTagToken;
import info.bliki.wiki.client.filter.tags.SpecialTagToken;
import info.bliki.wiki.client.filter.tags.TagParsingException;
import info.bliki.wiki.client.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A parser for the WikipediaFilter
 * 
 */
public class WikipediaParser extends WikipediaScanner {

	public static final Map<String,String> INTERWIKI_WIKIPEDIA = new HashMap();

	public static final Map<String,String> INTERWIKI_WIKIBOOKS = new HashMap();

	public static final Map<String,String> INTERWIKI_WIKIQUOTE = new HashMap();

	public static final Map<String,String> INTERWIKI_WIKINEWS = new HashMap();

	public static final HashSet STOP_TAGS_SET = new HashSet();

	public static int WIKI_COUNTER = 0;

	// public static final HashSet BBCODE_TAGS_SET = new HashSet();

	public static final String[] STOP_TAGS = { "p", "pre", "dl", "dd", "ul", "ol", "li", "hr", "h1", "h2", "h3", "h4", "h5", "h6",
			"table", "caption", "th", "tr", "td", };

	static {
		for (int i = 0; i < STOP_TAGS.length; i++) {
			STOP_TAGS_SET.add(STOP_TAGS[i]);
		}

		// for (int i = 0; i < BBCODE_TAGS.length; i++) {
		// BBCODE_TAGS_SET.add(BBCODE_TAGS[i]);
		// }
		INTERWIKI_WIKIPEDIA.put("ar", "http://ar.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("ca", "http://ca.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("da", "http://da.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("de", "http://de.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("en", "http://en.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("eo", "http://eo.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("es", "http://es.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("et", "http://et.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("fi", "http://fi.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("fr", "http://fr.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("he", "http://he.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("hr", "http://hr.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("ia", "http://ia.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("it", "http://it.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("ja", "http://ja.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("la", "http://la.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("nl", "http://nl.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("no", "http://no.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("pl", "http://pl.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("pt", "http://pt.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("ro", "http://ro.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("ru", "http://ru.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("sl", "http://sl.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("sv", "http://sv.wikipedia.org/wiki/");
		INTERWIKI_WIKIPEDIA.put("zh", "http://zh.wikipedia.org/wiki/");

		INTERWIKI_WIKIBOOKS.put("ar", "http://ar.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("ca", "http://ca.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("da", "http://da.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("de", "http://de.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("en", "http://en.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("eo", "http://eo.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("es", "http://es.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("et", "http://et.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("fi", "http://fi.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("fr", "http://fr.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("he", "http://he.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("hr", "http://hr.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("ia", "http://ia.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("it", "http://it.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("ja", "http://ja.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("la", "http://la.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("nl", "http://nl.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("no", "http://no.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("pl", "http://pl.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("pt", "http://pt.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("ro", "http://ro.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("ru", "http://ru.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("sl", "http://sl.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("sv", "http://sv.wikibooks.org/wiki/");
		INTERWIKI_WIKIBOOKS.put("zh", "http://zh.wikibooks.org/wiki/");

		INTERWIKI_WIKIQUOTE.put("ar", "http://ar.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("ca", "http://ca.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("da", "http://da.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("de", "http://de.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("en", "http://en.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("eo", "http://eo.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("es", "http://es.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("et", "http://et.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("fi", "http://fi.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("fr", "http://fr.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("he", "http://he.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("hr", "http://hr.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("ia", "http://ia.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("it", "http://it.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("ja", "http://ja.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("nl", "http://nl.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("pl", "http://pl.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("pt", "http://pt.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("ro", "http://ro.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("ru", "http://ru.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("sl", "http://sl.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("sv", "http://sv.wikiquote.org/wiki/");
		INTERWIKI_WIKIQUOTE.put("zh", "http://zh.wikiquote.org/wiki/");

		INTERWIKI_WIKINEWS.put("ar", "http://ar.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("ca", "http://ca.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("da", "http://da.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("de", "http://de.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("en", "http://en.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("eo", "http://eo.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("es", "http://es.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("et", "http://et.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("fi", "http://fi.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("fr", "http://fr.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("he", "http://he.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("hr", "http://hr.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("ia", "http://ia.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("it", "http://it.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("ja", "http://ja.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("nl", "http://nl.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("pl", "http://pl.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("pt", "http://pt.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("ro", "http://ro.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("ru", "http://ru.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("sl", "http://sl.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("sv", "http://sv.wikinews.org/wiki/");
		INTERWIKI_WIKINEWS.put("zh", "http://zh.wikinews.org/wiki/");

	}

	/**
	 * the associated categories for the currrent parsed text
	 */
	private List fCategories;

	// private RenderEngine fWikiEngine = null;

	/**
	 * If the snip contains headings for a &quot;table of content&quot; this
	 * buffer temporarily contains the start of the snip and the &quot;table of
	 * content&quot;
	 */
	private StringBuffer fResultBufferHeader = null;

	/**
	 * The buffer for the resulting HTML rendering from the current snip.
	 */
	private StringBuffer fResultBuffer;

	/**
	 * The corresponding String for the character source array
	 */
	private final String fStringSource;

	/**
	 * The current scanned character
	 */
	private char fCurrentCharacter;

	/**
	 * The current offset in the character source array
	 */
	private int fCurrentPosition;

	/**
	 * The current recursion level for this parser
	 */
	private int fRecursionLevel;

	private TokenStack fTokenStack;

	private boolean fWhiteStart = false;

	private int fWhiteStartPosition = 0;

	/**
	 * Name of the currently rendered domain
	 */
	// private String fDomainName = null;
	/**
	 * Name of the currently rendered wiki text
	 */
	// private String fPageName = null;
	/**
	 * 
	 * &quot;table of content&quot;
	 * 
	 */
	private ArrayList fTableOfContent = null;

	private HashSet fToCSet = null;

	private HashMap fWikiSettings = null;

	private String fWikiBaseURL = null;

	private String fImageBaseURL = null;

	private String fImageLocale = null;

	private String fTemplateLocale = null;

	// private WikiTextReader fTemplateReader = null;

	/**
	 * Enable phpBB like boards tags
	 */
	private boolean fUseBBCodes = false;

	/**
	 * Replace colons in a title with a '/' character
	 * 
	 */
	private boolean fReplaceColon = false;

	/**
	 * Enable HTML tags
	 */
	private boolean fHtmlCodes = true;

	private boolean fNoToC = false;

	public WikipediaParser(String stringSource, StringBuffer result, List categories, int recursionLevel) {
		super(stringSource); // .toCharArray());
		fCategories = categories;
		fResultBuffer = result;
		fStringSource = stringSource;
		fRecursionLevel = recursionLevel;
		fTokenStack = new TokenStack();

	}

	public void setProperties(HashMap wikiSettings) {
		fWikiSettings = wikiSettings;
		if (wikiSettings != null) {
			fWikiBaseURL = (String) wikiSettings.get("wiki_url");
			fImageBaseURL = (String) wikiSettings.get("image_url");
			fImageLocale = (String) wikiSettings.get("image");
			if (fImageLocale == null) {
				fImageLocale = "Image";
			}
			fTemplateLocale = (String) wikiSettings.get("template");
			if (fTemplateLocale == null) {
				fTemplateLocale = "Template";
			}
			// try {
			// fTemplateReader = (WikiTextReader) wikiSettings
			// .get("template_reader");
			// } catch (Exception e) {
			// fTemplateReader = null;
			// }
			try {
				String strReplaceColon = (String) wikiSettings.get("replace_colon");
				fReplaceColon = Boolean.valueOf(strReplaceColon).booleanValue();
			} catch (Exception e) {
				fReplaceColon = false;
			}
			try {
				String strUseBBCodes = (String) wikiSettings.get("bb_codes");
				fUseBBCodes = Boolean.valueOf(strUseBBCodes).booleanValue();
			} catch (Exception e) {
				fUseBBCodes = false;
			}
			try {
				String strHtmlCodes = (String) wikiSettings.get("html_codes");
				if (strHtmlCodes != null) {
					fHtmlCodes = Boolean.valueOf(strHtmlCodes).booleanValue();
				}
			} catch (Exception e) {
				fHtmlCodes = true;
			}
			try {
				String noToC = (String) wikiSettings.get("no_toc");
				fNoToC = Boolean.valueOf(noToC).booleanValue();
			} catch (Exception e) {
				fNoToC = false;
			}
		}
	}

	/**
	 * copy the content in the resulting buffer and escape special html characters
	 * (&lt; &gt; &quot; &amp; &#39;)
	 */
	private void copyWhite(boolean whiteStart, final int whiteStartPosition, final int diff) {
		if (whiteStart) {
			try {
				final int len = fCurrentPosition - diff;
				int currentIndex = whiteStartPosition;
				int lastIndex = currentIndex;
				while (currentIndex < len) {
					switch (charAt(currentIndex++)) {
					case '\r': // special ignore \r - allow only \n
						if (lastIndex < (currentIndex - 1)) {
							StringUtil.append(fResultBuffer, fSource, lastIndex, currentIndex - lastIndex - 1);
							lastIndex = currentIndex;
						} else {
							lastIndex++;
						}
						break;
					case '<': // special html escape character
						if (lastIndex < (currentIndex - 1)) {
							StringUtil.append(fResultBuffer, fSource, lastIndex, currentIndex - lastIndex - 1);
							lastIndex = currentIndex;
						} else {
							lastIndex++;
						}
						fResultBuffer.append("&lt;");
						break;
					case '>': // special html escape character
						if (lastIndex < (currentIndex - 1)) {
							StringUtil.append(fResultBuffer, fSource, lastIndex, currentIndex - lastIndex - 1);
							lastIndex = currentIndex;
						} else {
							lastIndex++;
						}
						fResultBuffer.append("&gt;");
						break;
					case '&': // special html escape character
						if (lastIndex < (currentIndex - 1)) {
							StringUtil.append(fResultBuffer, fSource, lastIndex, currentIndex - lastIndex - 1);
							lastIndex = currentIndex;
						} else {
							lastIndex++;
						}
						fResultBuffer.append("&amp;");
						break;
					case '\'': // special html escape character
						if (lastIndex < (currentIndex - 1)) {
							StringUtil.append(fResultBuffer, fSource, lastIndex, currentIndex - lastIndex - 1);
							lastIndex = currentIndex;
						} else {
							lastIndex++;
						}
						fResultBuffer.append("&#39;");
						break;
					case '\"': // special html escape character
						if (lastIndex < (currentIndex - 1)) {
							StringUtil.append(fResultBuffer, fSource, lastIndex, currentIndex - lastIndex - 1);
							lastIndex = currentIndex;
						} else {
							lastIndex++;
						}
						fResultBuffer.append("&quot;");
						break;
					}
				}
				if (lastIndex < (currentIndex)) {
					StringUtil.append(fResultBuffer, fSource, lastIndex, currentIndex - lastIndex);
				}
			} finally {
				fWhiteStart = false;
			}
		}
	}

	public static String copyWhite(String text) {
		StringBuffer buffer = new StringBuffer(text.length() + 32);
		StringUtil.escape(text, buffer);
		return buffer.toString();
	}

	/**
	 * Copy the text in the resulting buffer and escape special html characters
	 * (&lt; &gt; &quot; &amp; &#39;) Additionally every newline will be replaced
	 * by &lt;br/&gt;
	 */
	private void copyNowikiNewLine(String text) {
		final int len = text.length();
		int currentIndex = 0;
		int lastIndex = currentIndex;
		while (currentIndex < len) {
			switch (text.charAt(currentIndex++)) {
			case '\r':
				if (lastIndex < (currentIndex - 1)) {
					fResultBuffer.append(text.substring(lastIndex, currentIndex - 1));
					lastIndex = currentIndex;
				} else {
					lastIndex++;
				}
				break;
			case '\n':
				if (lastIndex < (currentIndex - 1)) {
					fResultBuffer.append(text.substring(lastIndex, currentIndex - 1));
					lastIndex = currentIndex;
				} else {
					lastIndex++;
				}
				fResultBuffer.append("<br/>");
				break;
			case '<': // special html escape character
				if (lastIndex < (currentIndex - 1)) {
					fResultBuffer.append(text.substring(lastIndex, currentIndex - 1));
					lastIndex = currentIndex;
				} else {
					lastIndex++;
				}
				fResultBuffer.append("&#60;");
				break;
			case '>': // special html escape character
				if (lastIndex < (currentIndex - 1)) {
					fResultBuffer.append(text.substring(lastIndex, currentIndex - 1));
					lastIndex = currentIndex;
				} else {
					lastIndex++;
				}
				fResultBuffer.append("&#62;");
				break;
			// case '&': // special html escape character
			// if (lastIndex < (currentIndex - 1)) {
			// fResultBuffer.append(text.substring(lastIndex, currentIndex -
			// 1));
			// lastIndex = currentIndex;
			// } else {
			// lastIndex++;
			// }
			// fResultBuffer.append("&#38;");
			// break;
			case '\'': // special html escape character
				if (lastIndex < (currentIndex - 1)) {
					fResultBuffer.append(text.substring(lastIndex, currentIndex - 1));
					lastIndex = currentIndex;
				} else {
					lastIndex++;
				}
				fResultBuffer.append("&#39;");
				break;
			case '\"': // special html escape character
				if (lastIndex < (currentIndex - 1)) {
					fResultBuffer.append(text.substring(lastIndex, currentIndex - 1));
					lastIndex = currentIndex;
				} else {
					lastIndex++;
				}
				fResultBuffer.append("&#34;");
				break;
			}
		}
		if (lastIndex < (currentIndex)) {
			fResultBuffer.append(text.substring(lastIndex, currentIndex));
		}
	}

	/**
	 * Copy the text in the resulting buffer and escape special html characters
	 * (&lt; &gt; )
	 */
	private void copyMathLTGT(String text) {
		final int len = text.length();
		int currentIndex = 0;
		int lastIndex = currentIndex;
		while (currentIndex < len) {
			switch (text.charAt(currentIndex++)) {
			case '<': // special html escape character
				if (lastIndex < (currentIndex - 1)) {
					fResultBuffer.append(text.substring(lastIndex, currentIndex - 1));
					lastIndex = currentIndex;
				} else {
					lastIndex++;
				}
				fResultBuffer.append("&lt;");
				break;
			case '>': // special html escape character
				if (lastIndex < (currentIndex - 1)) {
					fResultBuffer.append(text.substring(lastIndex, currentIndex - 1));
					lastIndex = currentIndex;
				} else {
					lastIndex++;
				}
				fResultBuffer.append("&gt;");
				break;
			}
		}
		if (lastIndex < (currentIndex)) {
			fResultBuffer.append(text.substring(lastIndex, currentIndex));
		}
	}

	/**
	 * Render the HTML token which are defined in the OPEN_TAGS and CLOSE_TAGS map
	 * 
	 * @return
	 */
	public int getHTMLToken() {
		int currentHtmlPosition = fCurrentPosition;
		try {
			if (getNextChar('/')) {
				// end tag detected
				currentHtmlPosition++;
				// closing tag
				if (!readTag()) {
					return WikipediaFilter.TokenNotFound;
				}
				String closeTagString = StringUtil.str(fSource, currentHtmlPosition, fCurrentPosition - currentHtmlPosition - 1)
						.toLowerCase();
				// String closeTagString =
				// fSource.substring(currentHtmlPosition, fCurrentPosition
				// - currentHtmlPosition - 1).toLowerCase();
				fCurrentPosition--;
				if (charAt(fCurrentPosition) != '>') {
					copyWhite(fWhiteStart, fWhiteStartPosition, 0);
					if (STOP_TAGS_SET.contains(closeTagString)) {
						reduceTokenStack();
					}
					return WikipediaFilter.TokenIgnore;
				}
				fCurrentPosition++;
				try {
					CloseTagToken token = (CloseTagToken) WikipediaFilter.CLOSE_TAGS.get(closeTagString);
					if (token == null) {
						return WikipediaFilter.TokenNotFound;
					}
					if (!fTokenStack.isEmpty()) {
						Object topToken = fTokenStack.peek();
						if (topToken instanceof OpenTagToken && ((OpenTagToken) topToken).getTagName() == token.getTagName()) {
							fTokenStack.pop();

							copyWhite(fWhiteStart, fWhiteStartPosition, 3 + closeTagString.length());
							if (STOP_TAGS_SET.contains(closeTagString)) {
								reduceTokenStack();
							}
							fResultBuffer.append(token.getCloseTag());
							return WikipediaFilter.TokenIgnore;
						}
					}
					copyWhite(fWhiteStart, fWhiteStartPosition, 0);
					if (STOP_TAGS_SET.contains(closeTagString)) {
						reduceTokenStack();
					}
					return WikipediaFilter.TokenIgnore;
				} catch (NoSuchElementException e) {
					return WikipediaFilter.TokenNotFound;
				}

			} else {
				// start tag
				String tokenString;
				int tagNameStart = fCurrentPosition;
				int tokenLength = 0;
				while (StringUtil.isLetter(charAt(fCurrentPosition))) {
					fCurrentPosition++;
					tokenLength++;
				}
				try {
					tokenString = StringUtil.str(fSource, tagNameStart, fCurrentPosition - tagNameStart);
					
					OpenTagToken token = (OpenTagToken) WikipediaFilter.OPEN_TAGS.get(tokenString);
					if (token == null) {
						return WikipediaFilter.TokenNotFound;
					}
					copyWhite(fWhiteStart, fWhiteStartPosition, (fCurrentPosition - tagNameStart) + 1);
					if (STOP_TAGS_SET.contains(tokenString)) {
						reduceTokenStack();
					}
					if (token instanceof SpecialTagToken) {
						// for <br> <br/> <br /> <hr> <hr/>

						while (StringUtil.isWhitespace(charAt(fCurrentPosition))) {
							fCurrentPosition++;
						}
						if (charAt(fCurrentPosition) == '/') {
							fCurrentPosition++;
						}
						if (charAt(fCurrentPosition) == '>') {
							fCurrentPosition++;
							fWhiteStartPosition = fCurrentPosition;
							// insert the special tag :
							fResultBuffer.append(token.getOpenTag());
							return WikipediaFilter.TokenIgnore;
						}

					} else if (token instanceof OpenTagToken) {
						fTokenStack.push(token);
						// use these buffer because of possible exceptions
						StringBuffer buffer = new StringBuffer();
						fCurrentPosition = token.scanHTMLAttributes(buffer, fSource, fCurrentPosition);
						fResultBuffer.append("<");
						fResultBuffer.append(token.getTagName());

						fResultBuffer.append(buffer);
						fResultBuffer.append(">");
						return WikipediaFilter.TokenIgnore;
					}
					return WikipediaFilter.TokenNotFound;
				} catch (NoSuchElementException e) {
					return WikipediaFilter.TokenNotFound;
				}
			}
		} catch (TagParsingException e) {
			fTokenStack.pop();
		} catch (IndexOutOfBoundsException e) {
			//
		}
		fCurrentPosition = currentHtmlPosition;
		return WikipediaFilter.TokenNotFound;
	}

	public final boolean getNextChar(char testedChar) {
		int temp = fCurrentPosition;
		try {
			fCurrentCharacter = charAt(fCurrentPosition++);
			if (fCurrentCharacter != testedChar) {
				fCurrentPosition = temp;
				return false;
			}
			return true;

		} catch (IndexOutOfBoundsException e) {
			fCurrentPosition = temp;
			return false;
		}
	}

	public final int getNextChar(char testedChar1, char testedChar2) {
		int temp = fCurrentPosition;
		try {
			int result;
			fCurrentCharacter = charAt(fCurrentPosition++);
			if (fCurrentCharacter == testedChar1)
				result = 0;
			else if (fCurrentCharacter == testedChar2)
				result = 1;
			else {
				fCurrentPosition = temp;
				return -1;
			}
			return result;
		} catch (IndexOutOfBoundsException e) {
			fCurrentPosition = temp;
			return -1;
		}
	}

	public final boolean getNextCharAsDigit() {
		int temp = fCurrentPosition;
		try {
			fCurrentCharacter = charAt(fCurrentPosition++);
			if (!StringUtil.isDigit(fCurrentCharacter)) {
				fCurrentPosition = temp;
				return false;
			}
			return true;
		} catch (IndexOutOfBoundsException e) {
			fCurrentPosition = temp;
			return false;
		}
	}

	// public final boolean getNextCharAsDigit(int radix) {
	//
	// int temp = fCurrentPosition;
	// try {
	// fCurrentCharacter = charAt(fCurrentPosition++);
	//
	// if (Character.digit(fCurrentCharacter, radix) == -1) {
	// fCurrentPosition = temp;
	// return false;
	// }
	// return true;
	// } catch (IndexOutOfBoundsException e) {
	// fCurrentPosition = temp;
	// return false;
	// }
	// }

	public final int getNumberOfChar(char testedChar) {
		int number = 0;
		try {
			while ((fCurrentCharacter = charAt(fCurrentPosition++)) == testedChar) {
				number++;
			}
		} catch (IndexOutOfBoundsException e) {

		}
		fCurrentPosition--;
		return number;
	}

	public boolean getNextCharAsWikiPluginIdentifierPart() {
		int temp = fCurrentPosition;
		try {
			fCurrentCharacter = charAt(fCurrentPosition++);

			if (!WikipediaFilter.isWikiPluginIdentifierPart(fCurrentCharacter)) {
				fCurrentPosition = temp;
				return false;
			}
			return true;
		} catch (IndexOutOfBoundsException e) {
			fCurrentPosition = temp;
			return false;
		}
	}

	protected int getNextToken() throws InvalidInputException {
		fWhiteStartPosition = 0;
		fWhiteStart = false;
		try {
			// if (fCurrentPosition == 0 && fRecursionLevel == 1) {
			// handleParagraph(fSource[0]);
			// }
			while (true) {
				fCurrentCharacter = charAt(fCurrentPosition++); // charAt(fCurrentPosition++);

				// if (fCurrentCharacter > 127) {
				// Short i = new Short((short) fCurrentCharacter);
				// EntityTable table = EntityTable.getInstance();
				// String name = table.entityName(i);
				// if (name != null) {
				// copyWhite(fWhiteStart, fWhiteStartPosition, 1);
				// fWhiteStart = false;
				//
				// fResultBuffer.append('&');
				// fResultBuffer.append(name);
				// fResultBuffer.append(';');
				// continue;
				// } else {
				// copyWhite(fWhiteStart, fWhiteStartPosition, 0);
				// fWhiteStart = false;
				// continue;
				// }
				// }

				// ---------Identify the next token-------------
				switch (fCurrentCharacter) {
				case '{': // wikipedia table  
					if (isStartOfLine()) {
						// wiki table ?
						setPosition(fCurrentPosition - 1);
						WPTable table = wpTable();
						if (table != null) {
							copyWhite(fWhiteStart, fWhiteStartPosition, 1);
							reduceTokenStack();
							// set pointer behind: "\n|}"
							fCurrentPosition = getPosition();
							table.filter(fResultBuffer, fSource, fWikiSettings, fRecursionLevel);
							continue;
						}
					}
					break;
				// }
				case '=': // wikipedia header ?
					if (isStartOfLine()) {
						int levelHeader = getNumberOfChar('=') + 1;
						copyWhite(fWhiteStart, fWhiteStartPosition, levelHeader);
						int startHeadPosition = fCurrentPosition;
						if (levelHeader > 6) {
							levelHeader = 6;
						}
						levelHeader--;
						if (readUntilString(WikipediaFilter.HEADER_STRINGS[levelHeader])) {
							reduceTokenStack();
							String head = StringUtil.str(fSource, startHeadPosition, fCurrentPosition - startHeadPosition - (1 + levelHeader));
							head = copyWhite(head);
							levelHeader++;
							handleHead(head, levelHeader);
							continue;
						}
					}
					break;
				case '*': // <ul> list
				case '#': // <ol> list
					// set scanner poiner to '\n' character:
					if (isStartOfLine()) {
						setPosition(fCurrentPosition - 2);
						WPList list = wpList();
						if (list != null && list.size() > 0) {
							copyWhite(fWhiteStart, fWhiteStartPosition, 1);
							reduceTokenStack();
							fCurrentPosition = getPosition() - 1;
							list.filter(fResultBuffer, fSource, fWikiSettings, fRecursionLevel);

							continue;
						}
					}
					break;
				case ':':
					if (isStartOfLine()) {
						copyWhite(fWhiteStart, fWhiteStartPosition, 1);

						int levelHeader = getNumberOfChar(':') + 1;
						int startHeadPosition = fCurrentPosition;
						if (readUntilEOL()) {
							reduceTokenStack();
							String head = StringUtil.str(fSource, startHeadPosition, fCurrentPosition - startHeadPosition);
							// String head =
							// fSource.substring(startHeadPosition,
							// fCurrentPosition
							// - startHeadPosition);
							for (int i = 0; i < levelHeader; i++) {
								fResultBuffer.append("<dl><dd>");
							}
							fResultBuffer.append(WikipediaFilter.filterParser(head.trim(), null, fRecursionLevel));

							for (int i = 0; i < levelHeader; i++) {
								fResultBuffer.append("\n</dd></dl>");
							}
							continue;
						}
					}
					break;
				case ';':
					if (isStartOfLine()) {
						copyWhite(fWhiteStart, fWhiteStartPosition, 1);

						int startHeadPosition = fCurrentPosition;
						if (readUntilEOL()) {
							reduceTokenStack();
							// TODO not correct - improve this
							String head = StringUtil.str(fSource, startHeadPosition, fCurrentPosition - startHeadPosition);
							// String head =
							// fSource.substring(startHeadPosition,
							// fCurrentPosition
							// - startHeadPosition);
							int index = head.indexOf(" : ");
							if (index > 0) {
								fResultBuffer.append("<dl><dt>");
								// fResultBuffer.append();
								fResultBuffer.append(WikipediaFilter.filterParser(head.substring(0, index).trim(), fWikiSettings, null,
										fRecursionLevel));
								fResultBuffer.append("&nbsp;</dt><dd>");
								// fResultBuffer.append(head.substring(index +
								// 2));
								fResultBuffer.append(WikipediaFilter.filterParser(head.substring(index + 2).trim(), fWikiSettings, null,
										fRecursionLevel));
								fResultBuffer.append("\n</dd></dl>");
							} else {
								index = head.indexOf(":");
								if (index > 0) {
									fResultBuffer.append("<dl><dt>");
									fResultBuffer.append(WikipediaFilter.filterParser(head.substring(0, index).trim(), null, fRecursionLevel));
									fResultBuffer.append("</dt><dd>");
									fResultBuffer.append(WikipediaFilter.filterParser(head.substring(index + 1).trim(), null, fRecursionLevel));
									fResultBuffer.append("\n</dd></dl>");
								} else {
									fResultBuffer.append("<dl><dt>");
									fResultBuffer.append(WikipediaFilter.filterParser(head.trim(), null, fRecursionLevel));
									fResultBuffer.append("&nbsp;</dt></dl>");
								}
							}
							continue;
						}
					}
					break;
				case '-':
					if (isStartOfLine()) {
						int tempCurrPosition = fCurrentPosition;
						try {
							if (charAt(tempCurrPosition++) == '-' && charAt(tempCurrPosition++) == '-' && charAt(tempCurrPosition++) == '-') {
								if (charAt(tempCurrPosition) == '\n') {
									copyWhite(fWhiteStart, fWhiteStartPosition, 4);
									reduceTokenStack();
									fCurrentPosition = tempCurrPosition;
									fResultBuffer.append("<hr/>");
									fWhiteStart = false;
									continue;
								} else if (charAt(tempCurrPosition++) == '\r' && charAt(tempCurrPosition++) == '\n') {
									copyWhite(fWhiteStart, fWhiteStartPosition, 6);
									reduceTokenStack();
									fCurrentPosition = tempCurrPosition - 1;
									fResultBuffer.append("<hr/>");
									fWhiteStart = false;
									continue;
								}
							}
						} catch (IndexOutOfBoundsException e) {

						}
						fCurrentPosition = tempCurrPosition;
					}
					break;
				case ' ': // pre-formatted text?
				case '\t':
					if (isStartOfLine() && !isEmptyLine()) {
						if (fTokenStack.size() == 0 || !fTokenStack.get(0).equals(WikipediaFilter.HTML_PRE_OPEN)) {
							copyWhite(fWhiteStart, fWhiteStartPosition, 2);
							reduceTokenStack();
							fResultBuffer.append("<pre>");
							fResultBuffer.append(fCurrentCharacter);
							fTokenStack.push(WikipediaFilter.HTML_PRE_OPEN);
						}
						continue;
					}
					break;
				}

				if (isStartOfLine() && fRecursionLevel == 1) {
					//
					// handle a paragraph
					//
					if (fTokenStack.size() > 0 && fTokenStack.get(0).equals(WikipediaFilter.HTML_P_OPEN)) {
						if (isEmptyLine()) {
							copyWhite(fWhiteStart, fWhiteStartPosition, 2);
							reduceTokenStack();
						}
					} else {
						if (!isEmptyLine()) {
							copyWhite(fWhiteStart, fWhiteStartPosition, 2);
							reduceTokenStack();
							fResultBuffer.append("<p>");
							fTokenStack.push(WikipediaFilter.HTML_P_OPEN);
						}
					}
				}

				// ---------Identify the next token-------------
				switch (fCurrentCharacter) {
				case '[':
					int startLinkPosition = fCurrentPosition;
					if (getNextChar('[')) { // wikipedia link style
						startLinkPosition = fCurrentPosition;
						copyWhite(fWhiteStart, fWhiteStartPosition, 2);

						if (readUntilString("]]")) {
							String name = StringUtil.str(fSource, startLinkPosition, fCurrentPosition - startLinkPosition - 2);
							// String name =
							// fSource.substring(startLinkPosition,
							// fCurrentPosition
							// - startLinkPosition - 2);
							// test for suffix string in Wiki link
							int temp = fCurrentPosition;
							StringBuffer suffixBuffer = new StringBuffer();
							try {
								while (true) {
									fCurrentCharacter = charAt(fCurrentPosition++);
									if (!StringUtil.isLetterOrDigit(fCurrentCharacter)) {
										fCurrentPosition--;
										break;
									}
									suffixBuffer.append(fCurrentCharacter);
								}
								handleWikipediaLink(name, suffixBuffer.toString());
								continue;
							} catch (IndexOutOfBoundsException e) {
								fCurrentPosition = temp;
							}

							handleWikipediaLink(name, "");
							continue;
						}

					} else {
						copyWhite(fWhiteStart, fWhiteStartPosition, 1);
						fWhiteStart = false;

						if (readUntilChar(']')) {
							String name = StringUtil.str(fSource, startLinkPosition, fCurrentPosition - startLinkPosition - 1);
							// String name =
							// fSource.substring(startLinkPosition,
							// fCurrentPosition
							// - startLinkPosition - 1);
							// check bbcode start
							if (fUseBBCodes) {
								if (name.length() > 0) {
									StringBuffer bbCode = new StringBuffer(name.length());
									char ch = name.charAt(0);
									if ('a' <= ch && ch <= 'z') {
										// first character must be a letter
										bbCode.append(ch);
										if (handleBBCode(name, bbCode)) {
											continue;
										}
									}
								}
							}
							// check bbcode end

							if (handleHTTPLink(name)) {
								continue;
							}
							fCurrentPosition = startLinkPosition;
						}
					}
					break;

				case '\'':
					if (getNextChar('\'')) {
						if (!fTokenStack.isEmpty()) {
							Object topToken = fTokenStack.peek();
							if (topToken instanceof AbstractTag && ((AbstractTag) topToken).getToken() == WikipediaFilter.TokenITALIC) {
								copyWhite(fWhiteStart, fWhiteStartPosition, 2);
								return WikipediaFilter.TokenITALIC;
							}
						}
						if (getNextChar('\'')) {
							copyWhite(fWhiteStart, fWhiteStartPosition, 3);
							return WikipediaFilter.TokenBOLD;
						}
						copyWhite(fWhiteStart, fWhiteStartPosition, 2);
						return WikipediaFilter.TokenITALIC;
					}
					break;

				case 'h': // http(s)://
					int urlStartPosition = fCurrentPosition;
					boolean foundUrl = false;
					int diff = 7;
					try {
						String urlString = fStringSource.substring(fCurrentPosition - 1, fCurrentPosition + 3);
						if (urlString.equals("http")) {
							fCurrentPosition += 3;
							fCurrentCharacter = charAt(fCurrentPosition++);
							if (fCurrentCharacter == 's') { // optional
								fCurrentCharacter = charAt(fCurrentPosition++);
								diff++;
							}

							if (fCurrentCharacter == ':' && charAt(fCurrentPosition++) == '/' && charAt(fCurrentPosition++) == '/') {
								copyWhite(fWhiteStart, fWhiteStartPosition, diff);
								fWhiteStart = false;
								foundUrl = true;
								while (WikipediaFilter.isUrlIdentifierPart(charAt(fCurrentPosition++))) {
								}
							}
						}
					} catch (IndexOutOfBoundsException e) {
						if (!foundUrl) {
							// rollback work :-)
							fCurrentPosition = urlStartPosition;
						}
					}
					if (foundUrl) {
						String urlString = StringUtil.str(fSource, urlStartPosition - 1, fCurrentPosition - urlStartPosition);
						// String urlString = fSource.substring(urlStartPosition
						// - 1, fCurrentPosition
						// - urlStartPosition);
						fCurrentPosition--;
						createExternalLink(urlString, urlString);
						continue;
					}
					break;

				case '&':
					int ampersandStart = fCurrentPosition - 1;
					if (getNextChar('#')) {
						try {
							StringBuffer num = new StringBuffer(5);
							char ch = charAt(fCurrentPosition++);
							while (StringUtil.isDigit(ch)) {
								num.append(ch);
								ch = charAt(fCurrentPosition++);
							}
							if (num.length() > 0 && ch == ';') {
								Short i = Short.valueOf(num.toString());
								EntityTable table = EntityTable.getInstance();
								String name = table.entityName(i);

								copyWhite(fWhiteStart, fWhiteStartPosition, 3 + num.length());
								fWhiteStart = false;
								if (name != null) {
									fResultBuffer.append('&');
									fResultBuffer.append(name);
									fResultBuffer.append(';');
								} else {
									StringUtil.append(fResultBuffer, fSource, ampersandStart, fCurrentPosition - ampersandStart);
								}
								continue;
							}
						} catch (IndexOutOfBoundsException e) {
							// ignore exception
						} catch (NumberFormatException e) {
							// ignore exception
						}
					} else {
						try {
							StringBuffer entity = new StringBuffer(10);
							entity.append('&');
							char ch = charAt(fCurrentPosition++);
							while (StringUtil.isLetterOrDigit(ch)) {
								entity.append(ch);
								ch = charAt(fCurrentPosition++);
							}
							if (entity.length() > 0 && ch == ';') {
								EntityTable table = EntityTable.getInstance();
								int code = table.entityCode(entity.toString());
								if (code != 0) {
									copyWhite(fWhiteStart, fWhiteStartPosition, 2 + entity.length() - 1);
									fWhiteStart = false;
									StringUtil.append(fResultBuffer, fSource, ampersandStart, fCurrentPosition - ampersandStart);
									continue;
								}
							}
						} catch (IndexOutOfBoundsException e) {
							// ignore exception
						} catch (NumberFormatException e) {
							// ignore exception
						}
					}
					break;
				case '<':
					if (fHtmlCodes) {
						int htmlStartPosition = fCurrentPosition;
						try {
							switch (fStringSource.charAt(fCurrentPosition)) {
							case '!': // <!-- html comment -->
								String htmlCommentString = fStringSource.substring(fCurrentPosition - 1, fCurrentPosition + 3);

								if (htmlCommentString.equals("<!--")) {
									fCurrentPosition += 3;
									if (readUntilString("-->")) {
										String htmlCommentContent = StringUtil.str(fSource, htmlStartPosition + 3, fCurrentPosition - htmlStartPosition
												- 6);
										// String htmlCommentContent =
										// fSource.substring(htmlStartPosition +
										// 3,
										// fCurrentPosition
										// - htmlStartPosition - 6);
										if (htmlCommentContent != null) {
											copyWhite(fWhiteStart, fWhiteStartPosition, fCurrentPosition - htmlStartPosition + 1);

											// insert html comment for visual
											// checks
											// only:
											/*
											 * fResultBuffer.append(" <!--");
											 * copyWhite(htmlCommentContent);
											 * fResultBuffer.append("--> ");
											 */
											continue;
										}
									}
								}
								break;
							case 'n': // nowiki
								String nowikiString = fStringSource.substring(fCurrentPosition - 1, fCurrentPosition + 7);

								if (nowikiString.equals("<nowiki>")) {
									fCurrentPosition += 7;
									if (readUntilString("</nowiki>")) {
										String nowikiContent = StringUtil
												.str(fSource, htmlStartPosition + 7, fCurrentPosition - htmlStartPosition - 16);
										// String nowikiContent =
										// fSource.substring(htmlStartPosition +
										// 7,
										// fCurrentPosition
										// - htmlStartPosition
										// - 16);
										if (nowikiContent != null) {
											copyWhite(fWhiteStart, fWhiteStartPosition, fCurrentPosition - htmlStartPosition + 1);

											copyNowikiNewLine(nowikiContent);
											continue;
										}
									}
								}
								break;
							case 'm': // math
								String mathString = fStringSource.substring(fCurrentPosition - 1, fCurrentPosition + 5);

								if (mathString.equals("<math>")) {
									fCurrentPosition += 5;
									if (readUntilString("</math>")) {
										String mathContent = StringUtil.str(fSource, htmlStartPosition + 5, fCurrentPosition - htmlStartPosition - 12);
										// String mathContent =
										// fSource.substring(htmlStartPosition +
										// 5,
										// fCurrentPosition
										// - htmlStartPosition
										// - 12);
										if (mathContent != null) {
											copyWhite(fWhiteStart, fWhiteStartPosition, fCurrentPosition - htmlStartPosition + 1);
											fWhiteStart = false;
											handleMathWiki(mathContent);
											continue;
										}
									}
								}
								break;
							case 's': // source
								String sourceString = fStringSource.substring(fCurrentPosition - 1, fCurrentPosition + 7);

								if (sourceString.equals("<source>")) {
									fCurrentPosition += 7;
									if (readUntilString("</source>")) {
										String sourceContent = StringUtil
												.str(fSource, htmlStartPosition + 7, fCurrentPosition - htmlStartPosition - 16);
										if (sourceContent != null) {
											copyWhite(fWhiteStart, fWhiteStartPosition, fCurrentPosition - htmlStartPosition + 1);
											// <textarea> looks better if JavaScript is enabled, <pre> looks better if JavaScript is disabled
											fResultBuffer.append("<pre name=\"code\" class=\"java\">");
											copyMathLTGT(sourceContent);
											fResultBuffer.append("</pre>");
											continue;
										}
									}
								}
								break;
							}
						} catch (IndexOutOfBoundsException e) {
							// do nothing
						}
						fCurrentPosition = htmlStartPosition;
						// detect special html tags
						int htmlToken = getHTMLToken();
						if (htmlToken == WikipediaFilter.TokenIgnore) {
							continue;
						}
						fCurrentPosition = htmlStartPosition;
					}
					break;
				}
				if (!fWhiteStart) {
					fWhiteStart = true;
					fWhiteStartPosition = fCurrentPosition - 1;
				}

			}
			// -----------------end switch while try--------------------
		} catch (IndexOutOfBoundsException e) {
			// end of scanner text
		}
		try {
			copyWhite(fWhiteStart, fWhiteStartPosition, 1);
		} catch (IndexOutOfBoundsException e) {
			// end of scanner text
		}
		return WikipediaFilter.TokenEOF;
	}

	private boolean handleBBCode(String name, StringBuffer bbCode) {
		int index = 1;
		char ch = ' ';

		while (index < name.length()) {
			ch = name.charAt(index++);
			if ('a' <= ch && ch <= 'z') {
				bbCode.append(ch);
			} else {
				break;
			}
		}
		if (ch != '=' && index != name.length()) {
			// no bbcode
			return false;
		}
		String bbStr = bbCode.toString();
		String bbEndStr = "[/" + bbStr + "]";
		int startPos = fCurrentPosition;

		if (!readUntilString(bbEndStr)) {
			return false;
		}
		String bbAttr = null;
		if (ch == '=') {
			bbAttr = name.substring(index, name.length());
		}

		int endPos = fCurrentPosition - bbEndStr.length();
		String innerTag = StringUtil.str(fSource, startPos, endPos - startPos);
		// String innerTag = fSource.substring(startPos, endPos - startPos);

		if (bbStr.equals("code")) {
			fResultBuffer.append("<pre class=\"code\">");
			StringUtil.escape(innerTag, fResultBuffer);
			fResultBuffer.append("</pre>");
			return true;
		} else if (bbStr.equals("color")) {
			if (bbAttr == null) {
				return false;
			}
			fResultBuffer.append("<font color=\"");
			StringUtil.escape(bbAttr, fResultBuffer);
			fResultBuffer.append("\">");
			StringUtil.escape(innerTag, fResultBuffer);
			fResultBuffer.append("</font>");
			return true;
		} else if (bbStr.equals("email")) {
			fResultBuffer.append("<a href=\"emailto:");
			StringUtil.escape(innerTag, fResultBuffer);
			fResultBuffer.append("\">");
			StringUtil.escape(innerTag, fResultBuffer);
			fResultBuffer.append("</a>");
			return true;
		} else if (bbStr.equals("list")) {
			int listStart = 0;
			int listEnd = 0;
			if (bbAttr != null) {
				if (bbAttr.equals("a")) {
					fResultBuffer.append("<ul>");
				} else {
					fResultBuffer.append("<ol>");
				}
			} else {
				fResultBuffer.append("<ul>");
			}
			while (listEnd >= 0) {
				listEnd = innerTag.indexOf("[*]", listStart);
				if (listEnd > listStart) {
					if (listStart == 0) {
						StringUtil.escape(innerTag.substring(0, listEnd), fResultBuffer);
					} else {
						fResultBuffer.append("<li>");
						StringUtil.escape(innerTag.substring(listStart, listEnd), fResultBuffer);
						fResultBuffer.append("</li>");
					}
					listStart = listEnd + 3;
				}
			}
			if (listStart == 0) {
				StringUtil.escape(innerTag, fResultBuffer);
			} else {
				if (listStart < innerTag.length()) {
					fResultBuffer.append("<li>");
					StringUtil.escape(innerTag.substring(listStart, innerTag.length()), fResultBuffer);
					fResultBuffer.append("</li>");
				}
			}
			if (bbAttr != null) {
				if (bbAttr.equals("a")) {
					fResultBuffer.append("</ul>");
				} else {
					fResultBuffer.append("</ol>");
				}
			} else {
				fResultBuffer.append("</ul>");
			}
			return true;
		} else if (bbStr.equals("img")) {
			fResultBuffer.append("<img src=\"");
			StringUtil.escape(innerTag, fResultBuffer);
			fResultBuffer.append("\">");
			StringUtil.escape(innerTag, fResultBuffer);
			fResultBuffer.append("</img>");
			return true;
		} else if (bbStr.equals("quote")) {
			fResultBuffer.append("<blockquote>");
			StringUtil.escape(innerTag, fResultBuffer);
			fResultBuffer.append("</blockquote>");
			return true;
		} else if (bbStr.equals("size")) {
			if (bbAttr == null) {
				return false;
			}
			fResultBuffer.append("<font size=\"");
			StringUtil.escape(bbAttr, fResultBuffer);
			fResultBuffer.append("\">");
			StringUtil.escape(innerTag, fResultBuffer);
			fResultBuffer.append("</font>");
			return true;
		} else if (bbStr.equals("url")) {
			if (bbAttr != null) {
				fResultBuffer.append("<a href=\"");
				StringUtil.escape(bbAttr, fResultBuffer);
				fResultBuffer.append("\">");
				StringUtil.escape(innerTag, fResultBuffer);
				fResultBuffer.append("</a>");
				return true;
			} else {
				fResultBuffer.append("<a href=\"");
				StringUtil.escape(innerTag, fResultBuffer);
				fResultBuffer.append("\">");
				StringUtil.escape(innerTag, fResultBuffer);
				fResultBuffer.append("</a>");
				return true;
			}
		} else if (bbStr.equals("b")) {
			fResultBuffer.append("<b>");
			StringUtil.escape(innerTag, fResultBuffer);
			fResultBuffer.append("</b>");
			return true;
		} else if (bbStr.equals("i")) {
			fResultBuffer.append("<i>");
			StringUtil.escape(innerTag, fResultBuffer);
			fResultBuffer.append("</i>");
			return true;
		} else if (bbStr.equals("u")) {
			fResultBuffer.append("<u>");
			StringUtil.escape(innerTag, fResultBuffer);
			fResultBuffer.append("</u>");
			return true;
		}

		return false;
	}

	/**
	 * Create a map from the parametes defined in a template call
	 * 
	 * @param templateParameters
	 * @return
	 */
	// private HashMap createParameterMap(String templateParameters) {
	// HashMap map = new HashMap();
	// char ch;
	// for (int i = 0; i < templateParameters.length(); i++) {
	// ch = templateParameters.charAt(i);
	// }
	// return map;
	// }
	private Object[] createParameterMap(char[] src, int startOffset, int len) {
		Object[] objs = new Object[2];
		HashMap map = new HashMap();
		objs[0] = map;
		boolean foundPipe = false;
		int currOffset = startOffset;
		int endOffset = startOffset + len;
		char ch;
		while (currOffset < endOffset) {
			ch = src[currOffset++];
			if (ch == '|') {
				// set the templatename
				foundPipe = true;
				objs[1] = new String(src, startOffset, currOffset - startOffset - 1);
				break;
			}
		}
		if (!foundPipe) {
			// set the templatename
			objs[1] = new String(src, startOffset, len);
			return objs;
		}
		String parameter = null;
		String value;
		int parameterCounter = 0;
		int lastOffset = currOffset;
		try {
			while (currOffset < endOffset) {
				ch = src[currOffset++];
				if (ch == '[' && src[currOffset] == '[') {
					currOffset++;
					while (currOffset < endOffset) {
						ch = src[currOffset++];
						if (ch == ']' && src[currOffset] == ']') {
							currOffset++;
							break;
						}
					}
				} else if (ch == '=') {
					parameter = new String(src, lastOffset, currOffset - lastOffset - 1).trim();
					lastOffset = currOffset;
				} else if (ch == '|') {
					parameterCounter++;
					value = new String(src, lastOffset, currOffset - lastOffset - 1);
					map.put(Integer.toString(parameterCounter), value);
					if (parameter != null) {
						map.put(parameter, value);
						parameter = null;
					}
					lastOffset = currOffset;
				}
			}

			if (currOffset > lastOffset) {
				parameterCounter++;
				value = new String(src, lastOffset, currOffset - lastOffset);
				map.put(Integer.toString(parameterCounter), value);
				if (parameter != null) {
					map.put(parameter, value);
				}
			}
		} catch (IndexOutOfBoundsException e) {

		}
		return objs;
	}

	private void handleMathWiki(String mathContent) {
		// we are using jsmath here:
		fResultBuffer.append("<DIV CLASS=\"math\">");
		copyMathLTGT(mathContent);
		fResultBuffer.append("</DIV>");

		// // fWikiEngine is an instance of IMathRenderEngine past this point
		// BufferedImage image = new BufferedImage(640, 480,
		// BufferedImage.TYPE_INT_RGB);
		// Graphics2D g2 = image.createGraphics();
		// g2.setBackground(Color.white);
		// g2.clearRect(0, 0, 640, 480);
		// g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		// RenderingHints.VALUE_ANTIALIAS_ON);
		// g2.setColor(Color.black);
		//
		// String altFormula = mathContent;
		// String encodedContent = Encoder.encode(mathContent);
		// File f = new File(((IWikipediaRenderEngine) fWikiEngine)
		// .getMathImageFilename(encodedContent));
		//
		// if (!f.exists()) {
		// try {
		// TeXImage texImage = new TeXImage(null, null, null);
		// texImage.setInputString(false, "$$" + mathContent + "$$");
		// texImage.paint(g2);
		// image = texImage.crop(image, g2);
		//
		// Iterator writers = ImageIO.getImageWritersByFormatName("png");
		// ImageWriter writer = (ImageWriter) writers.next();
		//
		// ImageOutputStream ios = ImageIO.createImageOutputStream(f);
		// writer.setOutput(ios);
		// writer.write(image);
		// } catch (Exception e) {
		// // tex rendering failed
		// fResultBuffer.append(mathContent);
		// return;
		// }
		// }
		// ((IWikipediaRenderEngine) fWikiEngine).appendMathImageLink(
		// fResultBuffer, encodedContent, altFormula);
	}

	/**
	 * 
	 */
	// private void handleParagraph() {
	// // if (firstCharacterInLine == ' ' || firstCharacterInLine == '\r' ||
	// firstCharacterInLine == '\n') {
	// // preformatted text?
	// if (fTokenStack.size() > 0 &&
	// fTokenStack.get(0).equals(WikipediaFilter.HTML_PRE_OPEN)) {
	// return;
	// }
	// int startIndex = ++fCurrentPosition;
	// try {
	// while (Character.isWhitespace(charAt(fCurrentPosition))) {
	// if (charAt(fCurrentPosition) == '\r' || charAt(fCurrentPosition) ==
	// '\n') {
	// copyWhite(fWhiteStart, fWhiteStartPosition, 0);
	// reduceTokenStack();
	// fResultBuffer.append("<p>");
	// fTokenStack.push(WikipediaFilter.HTML_P_OPEN);
	// return;
	// }
	// fCurrentPosition++;
	// }
	// if (charAt(fCurrentPosition) != '\r' && charAt(fCurrentPosition) !=
	// '\n') {
	// copyWhite(fWhiteStart, fWhiteStartPosition, 0);
	// reduceTokenStack();
	// // fResultBuffer.append("<pre>");
	// // fTokenStack.push(WikipediaFilter.HTML_PRE_OPEN);
	// // fCurrentPosition = startIndex-1;
	// return;
	// }
	// } catch (IndexOutOfBoundsException e) {
	// //
	// }
	// // } else if (firstCharacterInLine != '*' && firstCharacterInLine != '='
	// && firstCharacterInLine != '#'
	// // && firstCharacterInLine != ';' && firstCharacterInLine != ':') {
	// // copyWhite(fWhiteStart, fWhiteStartPosition, 0);
	// // reduceTokenStack();
	// // fResultBuffer.append("<p>");
	// // fTokenStack.push(WikipediaFilter.HTML_P_OPEN);
	// // }
	// }
	// private void handleParagraph_old(char firstCharacterInLine) {
	// if (firstCharacterInLine == ' ' || firstCharacterInLine == '\r' ||
	// firstCharacterInLine == '\n') {
	// // preformatted text?
	// if (fTokenStack.size() > 0 &&
	// fTokenStack.get(0).equals(WikipediaFilter.HTML_PRE_OPEN)) {
	// return;
	// }
	// int startIndex = ++fCurrentPosition;
	// try {
	// while (Character.isWhitespace(charAt(fCurrentPosition))) {
	// if (charAt(fCurrentPosition) == '\r' || charAt(fCurrentPosition) ==
	// '\n') {
	// copyWhite(fWhiteStart, fWhiteStartPosition, 0);
	// reduceTokenStack();
	// fResultBuffer.append("<p>");
	// fTokenStack.push(WikipediaFilter.HTML_P_OPEN);
	// return;
	// }
	// fCurrentPosition++;
	// }
	// if (charAt(fCurrentPosition) != '\r' && charAt(fCurrentPosition) !=
	// '\n') {
	// copyWhite(fWhiteStart, fWhiteStartPosition, 0);
	// reduceTokenStack();
	// // fResultBuffer.append("<pre>");
	// // fTokenStack.push(WikipediaFilter.HTML_PRE_OPEN);
	// // fCurrentPosition = startIndex-1;
	// return;
	// }
	// } catch (IndexOutOfBoundsException e) {
	// //
	// }
	// } else if (firstCharacterInLine != '*' && firstCharacterInLine != '=' &&
	// firstCharacterInLine != '#'
	// && firstCharacterInLine != ';' && firstCharacterInLine != ':') {
	// copyWhite(fWhiteStart, fWhiteStartPosition, 0);
	// reduceTokenStack();
	// fResultBuffer.append("<p>");
	// fTokenStack.push(WikipediaFilter.HTML_P_OPEN);
	// }
	// }
	/**
	 * @return
	 */
	private boolean isStartOfLine() {
		boolean isLineStart = false;
		if (fCurrentPosition >= 2) {
			char beforeChar = charAt(fCurrentPosition - 2);
			// if (beforeChar == '\n' || beforeChar == '\r') {
			if (beforeChar == '\n') {
				isLineStart = true;
			}
		}
		if (fCurrentPosition == 1) {
			isLineStart = true;
		}
		return isLineStart;
	}

	private boolean isEmptyLine() {
		int temp = fCurrentPosition - 1;
		char ch;
		try {
			while (true) {
				ch = charAt(temp);
				if (!StringUtil.isWhitespace(ch)) {
					return false;
				}
				if (ch == '\n') {
					return true;
				}
				temp++;
			}
		} catch (IndexOutOfBoundsException e) {
			// ..
		}
		return true;
	}

	/**
	 * @param levelStar
	 * @param listChars
	 *          TODO
	 */
	// private void appendList(char[] listChars) {
	// int topLevel = 0;
	// int levelStar = listChars.length;
	// copyWhite(fWhiteStart, fWhiteStartPosition, levelStar);
	// fWhiteStart = false;
	//
	// if (!fTokenStack.isEmpty()) {
	// AbstractTag tok = (AbstractTag) fTokenStack.peek();
	// if (tok instanceof ListToken) {
	// ListToken listToken = (ListToken) tok;
	// topLevel = listToken.getLevel();
	//
	// if (levelStar > topLevel) {
	// while (levelStar > topLevel) {
	// if (listChars[topLevel] == '*') {
	// fTokenStack.push(new ListToken(WikipediaFilter.TokenLIST_UL_START,
	// ++topLevel));
	// fResultBuffer.append("<ul><li>");
	// } else {
	// fTokenStack.push(new ListToken(WikipediaFilter.TokenLIST_OL_START,
	// ++topLevel));
	// fResultBuffer.append("<ol><li>");
	// }
	// }
	// } else if (levelStar < topLevel) {
	// while (levelStar < topLevel) {
	// tok = (AbstractTag) fTokenStack.peek();
	// if (tok instanceof ListToken) {
	// fTokenStack.pop();
	// listToken = (ListToken) tok;
	// if (listToken.getToken() == WikipediaFilter.TokenLIST_UL_START) {
	// fResultBuffer.append("</li></ul>\n</li><li>");
	// } else {
	// fResultBuffer.append("</li></ol>\n</li><li>");
	// }
	// topLevel--;
	// } else {
	// break;
	// }
	// }
	// } else {
	// --topLevel;
	// if (listToken.getToken() == WikipediaFilter.TokenLIST_UL_START &&
	// listChars[topLevel] == '#') {
	// fTokenStack.pop();
	// fTokenStack.push(new ListToken(WikipediaFilter.TokenLIST_OL_START,
	// topLevel));
	// fResultBuffer.append("</li></ul><ol><li>");
	// } else if (listToken.getToken() == WikipediaFilter.TokenLIST_OL_START &&
	// listChars[topLevel] == '*') {
	// fTokenStack.pop();
	// fTokenStack.push(new ListToken(WikipediaFilter.TokenLIST_UL_START,
	// topLevel));
	// fResultBuffer.append("</li></ol><ul><li>");
	// } else {
	// fResultBuffer.append("</li><li>");
	// }
	// }
	// return;
	// }
	// }
	// while (levelStar > topLevel) {
	// if (listChars[topLevel] == '*') {
	// fTokenStack.push(new ListToken(WikipediaFilter.TokenLIST_UL_START,
	// ++topLevel));
	// fResultBuffer.append("<ul><li>");
	// } else {
	// fTokenStack.push(new ListToken(WikipediaFilter.TokenLIST_OL_START,
	// ++topLevel));
	// fResultBuffer.append("<ol><li>");
	// }
	// }
	//
	// }
	
	private boolean handleHTTPLink(String name) {
		if (name != null) {
			int index = name.indexOf("http://");

			if (index != -1) {
				// WikipediaFilter.createExternalLink(fResultBuffer,
				// fWikiEngine, name.substring(index));
				String urlString = name.substring(index);
				// Wikipedia like style:
				int pipeIndex = urlString.indexOf(' ');
				String alias = "";
				if (pipeIndex != (-1)) {
					alias = urlString.substring(pipeIndex + 1);
					urlString = urlString.substring(0, pipeIndex);
				} else {
					alias = urlString;
				}

				createExternalLink(urlString, alias);

				return true;
			}
			
		}
		return false;
	}

	/**
	 * 
	 * @param urlString
	 *          the url link
	 * @param alias
	 *          an alias for the url
	 */
	public void createExternalLink(String urlString, String alias) {
		// is it an image?
		int indx = urlString.lastIndexOf(".");
		if (indx > 0 && indx < (urlString.length() - 3)) {
			String ext = urlString.substring(indx + 1);
			if (ext.equalsIgnoreCase("gif") || ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")
					|| ext.equalsIgnoreCase("bmp")) {
				createExternalImage(urlString, alias);
				return;
			}
		}
		fResultBuffer.append("<span class=\"nobr\">");
		fResultBuffer.append("<a href=\"");
		StringUtil.escape(urlString, fResultBuffer);
		fResultBuffer.append("\" class=\"external\" title=\"");
		StringUtil.escape(urlString, fResultBuffer);
		fResultBuffer.append("\">");

		StringUtil.escape(alias, fResultBuffer);
		fResultBuffer.append("</a></span>");
	}

	public void createExternalImage(String urlString, String alias) {
		fResultBuffer.append("<span class=\"image\">");
		fResultBuffer.append("<img src=\"");
		StringUtil.escape(urlString, fResultBuffer);
		fResultBuffer.append("\" alt=\"");
		StringUtil.escape(alias, fResultBuffer);
		fResultBuffer.append("\" /></span>");
	}

	private void handleWikipediaLink(String linkText, String suffix) {
		String name = linkText;
		if (name != null) {
			int index = name.indexOf("http://");
			// Configuration probably wrote [http://plog4u.org] instead of
			// http://plog4u.org
			if (index != -1) {
				String link = name.substring(index);
				createExternalLink(link, link);
				// show error
				// fResult.append("<div class=\"error\">Do not surround URLs
				// with [...].</div>");
			} else {
				// trim the name and unescape it
				name = name.trim(); // Encoder.unescape(name.trim());
				// Is there an alias like [alias|link] ?
				int pipeIndex = name.lastIndexOf('|');
				String alias = "";
				if (-1 != pipeIndex) {
					alias = name.substring(pipeIndex + 1);
					name = name.substring(0, pipeIndex);
				}

				int hashIndex = name.lastIndexOf('#');

				String hash = "";
				if (-1 != hashIndex && hashIndex != name.length() - 1) {
					hash = name.substring(hashIndex + 1);
					name = name.substring(0, hashIndex);
				}

				name = StringUtil.escape(name);
				String view;
				if (-1 != pipeIndex) {
					view = alias + suffix;
				} else {
					view = name + suffix;
				}

				String encodedName = Encoder.encode(name);
				if (handleNamespaceLinks(encodedName, view)) {
					return;
				}

				if (name.startsWith("Image:") || name.startsWith(fImageLocale + ':')) {
					if (fImageBaseURL != null) {
						String imageSrc = fImageBaseURL;
						ImageFormat imgFormat = ImageFormat.getImageFormat(linkText, fImageLocale);

						String imageName = imgFormat.getFilename();
						String sizeStr = imgFormat.getSizeStr();
						if (sizeStr != null) {
							imageName = sizeStr + '-' + imageName;
						}
						if (imageName.endsWith(".svg")) {
							imageName += ".png";
						}
						imageName = Encoder.encode(imageName);
						imageSrc = StringUtil.replace(imageSrc, "${image}", imageName);
						fResultBuffer.append("<img src=\"");
						fResultBuffer.append(imageSrc);
						fResultBuffer.append("\"");

						if (imgFormat.getCaption() != null) {
							fResultBuffer.append(" alt=\"").append(imgFormat.getCaption()).append("\"");
							fResultBuffer.append(" title=\"").append(imgFormat.getCaption()).append("\"");
						}

						fResultBuffer.append(" class=\"location-").append(imgFormat.getLocation());

						if (imgFormat.getType() != null) {
							fResultBuffer.append(" type-").append(imgFormat.getType());
						}
						fResultBuffer.append("\"");

						if (imgFormat.getSize() != -1) {
							fResultBuffer.append(" width=\"").append(imgFormat.getSize()).append("\"");
						}

						fResultBuffer.append(" />");
						// } else {
						// ImageFormat imgFormat = ImageFormat
						// .getImageFormat(linkText);
						//
						// fResultBuffer.append("<img src=\"");
						// fResultBuffer.append(FilterUtil.createServerImage(null,
						// imgFormat.getFilename(), null));
						// fResultBuffer.append("\"");
						//
						// if (imgFormat.getCaption() != null) {
						// fResultBuffer.append(" alt=\"").append(
						// imgFormat.getCaption()).append("\"");
						// fResultBuffer.append(" title=\"").append(
						// imgFormat.getCaption()).append("\"");
						// }
						//
						// fResultBuffer.append(" class=\"location-").append(
						// imgFormat.getLocation());
						//
						// if (imgFormat.getType() != null) {
						// fResultBuffer.append(" type-").append(
						// imgFormat.getType());
						// }
						// fResultBuffer.append("\"");
						//
						// if (imgFormat.getSize() != -1) {
						// fResultBuffer.append(" width=\"").append(
						// imgFormat.getSize()).append("\"");
						// }
						//
						// fResultBuffer.append(" />");
					}
				} else {
					if (fWikiBaseURL != null) {
						if (-1 != hashIndex) {
							appendLink(encodedName, hash, view);
						} else {
							appendLink(encodedName, null, view);
						}
					} else {
						// cannot display/create wiki, so just display the text
						fResultBuffer.append(view);
					}
				}
				// }
			}
		}
	}

	private void appendLink(String name, String hash, String view) {
		String link = fWikiBaseURL;
		if (fReplaceColon) {
			name = StringUtil.replaceAll(name, ":", "/");
		}
		link = StringUtil.replace(link, "${title}", name);
		WIKI_COUNTER++;
		fResultBuffer.append("<a id=\"w" + WIKI_COUNTER + "\" href=\"");
		fResultBuffer.append(link);
		if (hash != null) {
			fResultBuffer.append('#');
			fResultBuffer.append(hash);
		}
		fResultBuffer.append("\">");
		fResultBuffer.append(view);
		fResultBuffer.append("</a>");
	}

	/**
	 * @param name
	 * @param view
	 */
	private boolean handleNamespaceLinks(String name, String view) {
		int interwikiIndex = name.indexOf(':');

		if (interwikiIndex != (-1)) {
			// String interwiki;
			// String link;
			// String page;
			if (interwikiIndex == 1) {
				char ch = name.charAt(0);
				switch (ch) {
				case 'w': // wikipedia
					if (name.length() > 5 && name.charAt(4) == ':') {
						return handleInterwiki(INTERWIKI_WIKIPEDIA, name.substring(2, name.length()), view, 2);
					}
				case 'b': // wikipedia
					if (name.length() > 5 && name.charAt(4) == ':') {
						return handleInterwiki(INTERWIKI_WIKIBOOKS, name.substring(2, name.length()), view, 2);
					}
				case 'q': // wikiquote
					if (name.length() > 5 && name.charAt(4) == ':') {
						return handleInterwiki(INTERWIKI_WIKIQUOTE, name.substring(2, name.length()), view, 2);
					}
				case 'n': // wikinews
					if (name.length() > 5 && name.charAt(4) == ':') {
						return handleInterwiki(INTERWIKI_WIKINEWS, name.substring(2, name.length()), view, 2);
					}
				case 'c': // commons
					if (name.length() > 2) {
						return handleInterwiki("http://commons.wikimedia.org/wiki/", name, view, 1);
					}
				case 'm': // meta
					if (name.length() > 2) {
						return handleInterwiki("http://meta.wikimedia.org/wiki/", name, view, 1);
					}
				case 'd': // deutsches bliki wiki
					if (name.length() > 2) {
						return handleInterwiki("http://www.plog4u.de/index.php/", name, view, 1);
					}
				case 'e': // englisches bliki wiki
					if (name.length() > 2) {
						return handleInterwiki("http://www.plog4u.org/index.php/", name, view, 1);
					}
				case 's': // source
					if (name.length() > 2) {
						return handleInterwiki("http://wikisource.org/wiki/", name, view, 1);
					}
				}
			} else if (interwikiIndex == 2) {
				return handleInterwiki(INTERWIKI_WIKIPEDIA, name, view, interwikiIndex);
			} else {
				String nameSpace = name.substring(0, interwikiIndex);

				if (fCategories != null) {
					if (nameSpace.equals("Category") || nameSpace.equals("Kategorie")) {
						// add the category to this texts metadata
						String category = name.substring(interwikiIndex + 1);
						if (category != null && category.length() > 0) {
							String encodedcategory = Encoder.encodeName(category);
							if (category != null && category.length() > 0) {
								fCategories.add(encodedcategory);
								return true;
							}
						}
					}
				}

			}
		}
		return false;
	}

	/**
	 * @param name
	 * @param alias
	 * @param interwikiIndex
	 */
	private boolean handleInterwiki(Map map, String name, String view, int interwikiIndex) {
		String interwiki;
		String link;
		String page;
		interwiki = name.substring(0, interwikiIndex);
		link = (String) map.get(interwiki);
		if (link != null) {
			page = name.substring(interwikiIndex + 1);
			fResultBuffer.append("<a href=\"");
			fResultBuffer.append(link);
			fResultBuffer.append(page);
			fResultBuffer.append("\">");
			fResultBuffer.append(view);
			fResultBuffer.append("</a>");
			return true;
		}
		return false;
	}

	private boolean handleInterwiki(String link, String name, String alias, int interwikiIndex) {
		String page;
		page = name.substring(interwikiIndex + 1);
		fResultBuffer.append("<a href=\"");
		fResultBuffer.append(link);
		fResultBuffer.append(page);
		fResultBuffer.append("\">");
		if (alias != null && alias.length() > 0) {
			fResultBuffer.append(alias);
		} else {
			fResultBuffer.append(page);
		}
		fResultBuffer.append("</a>");
		return true;
	}

	// public String createAnchor(String head) {
	// StringBuffer result = new StringBuffer(head.length() + 10);
	// char ch;
	// result.append('a');
	// // reduce Anchorstring
	// for (int i = 0; i < head.length(); i++) {
	// ch = head.charAt(i);
	// if ('a' <= ch && 'z' >= ch) {
	// result.append(ch);
	// } else if ('A' <= ch && 'Z' >= ch) {
	// result.append(ch);
	// } else if ('0' <= ch && '9' >= ch) {
	// result.append(ch);
	// }
	//      
	// }
	// return result.toString();
	// }

	/**
	 * append a link to the StringBuffer the name and the view should already
	 * conatin escaped entities for &gt;,&lt;,...
	 */
	public static StringBuffer appendLink(StringBuffer buffer, String name, String view, String target) {
		return appendLinkWithRoot(buffer, null, name + "#" + target, view);
	}

	/**
	 * Create a link with a root and a special view. The name will not be url
	 * encoded!
	 */
	public static StringBuffer appendLinkWithRoot(StringBuffer buffer, String root, String name, String view) {
		buffer.append("<a href=\"");
		if (root != null) {
			buffer.append(root);
			buffer.append("/");
		}
		buffer.append(name);
		buffer.append("\">");
		buffer.append(view);
		buffer.append("</a>");
		return buffer;
	}

	/**
	 * add an entry to the "table of content" TODO refactor this to a class
	 * 
	 * @param toc
	 * @param head
	 * @param anchor
	 * @param headLevel
	 */
	private void addToTableOfContent(ArrayList toc, String head, String anchor, int headLevel) {
		int level = 1;
		if (level == headLevel) {
			String snipName = "";
			// if (fSnip != null) {
			// snipName = fSnip.getName();
			// }

			StringBuffer link = new StringBuffer(snipName.length() + 40 + head.length() + anchor.length());
			link.append("<li>");
			// TODO create link for table of content
			appendLink(link, snipName, head, anchor);
			link.append("\n</li>");
			toc.add(link.toString());
		} else {
			if (toc.size() > 0) {
				if (toc.get(toc.size() - 1) instanceof ArrayList) {
					addToTableOfContent((ArrayList) toc.get(toc.size() - 1), head, anchor, --headLevel);
					return;
				}
			}
			ArrayList list = new ArrayList();
			toc.add(list);
			addToTableOfContent(list, head, anchor, --headLevel);
		}
	}

	/**
	 * handle head for table of content
	 * 
	 * @param head
	 * @param headLevel
	 */
	private void handleHead(String head, int headLevel) {
		if (head != null) {
			String anchor = Encoder.encode(head.trim());

			if (fTableOfContent == null) {
				// create new table of content
				fTableOfContent = new ArrayList();
				fToCSet = new HashSet();
				// copy fResult and new initialization:

				fResultBufferHeader = fResultBuffer;
				fResultBuffer = new StringBuffer();// fResultBuffer.capacity());
			}

			if (fToCSet.contains(anchor)) {
				String newAnchor = anchor;
				for (int i = 2; i < Integer.MAX_VALUE; i++) {
					newAnchor = anchor + '_' + Integer.toString(i);
					if (fToCSet.contains(newAnchor)) {
						break;
					}
				}
				anchor = newAnchor;
			}
			addToTableOfContent(fTableOfContent, head, anchor, headLevel);

			fResultBuffer.append("<a name=\"");
			fResultBuffer.append(anchor);
			fResultBuffer.append("\" id=\"");
			fResultBuffer.append(anchor);
			fResultBuffer.append("\"></a>\n<h");
			fResultBuffer.append(headLevel);
			fResultBuffer.append(">");
			fResultBuffer.append(head);
			fResultBuffer.append("</h");
			fResultBuffer.append(headLevel);
			fResultBuffer.append(">");
			// if (headLevel <= 2) {
			// fResultBuffer.append("<hr/>");
			// }
		}
	}

	// private boolean getList(char listChar, String openTag, String closeTag) {
	// int currentPosition = fCurrentPosition;
	// int level = getNumberOfChar(listChar) + 1;
	// if (getNextChar('.') && getNextChar(' ')) {
	// int tempPosition = checkWhitespaces(fWhiteStartPosition, fCurrentPosition
	// - 3 - level);
	// if (tempPosition >= 0) {
	// copyWhite(fWhiteStart, fWhiteStartPosition, 2 + level);
	// fWhiteStart = false;
	// AbstractTag tok = (AbstractTag) fTokenStack.peek();
	// if (tok instanceof ListToken) {
	// ListToken listToken = (ListToken) tok;
	// int topLevel = listToken.getLevel();
	// if (listToken.getToken() == WikipediaFilter.TokenLIST_OL_START) {
	// if (level > topLevel) {
	// fTokenStack.push(new ListToken(WikipediaFilter.TokenLIST_OL_START,
	// topLevel + 1));
	// fResultBuffer.append(openTag + "<li>");
	// } else if (level < topLevel) {
	// fTokenStack.pop();
	// fResultBuffer.append("\n</li>" + closeTag + "\n</li><li>");
	// } else {
	// fResultBuffer.append("\n</li><li>");
	// }
	// } else {
	// fTokenStack.push(new ListToken(WikipediaFilter.TokenLIST_OL_START,
	// level));
	// fResultBuffer.append(openTag + "<li>");
	// }
	// } else {
	// fTokenStack.push(new ListToken(WikipediaFilter.TokenLIST_OL_START, 1));
	// fResultBuffer.append("\n" + openTag + "<li>");
	// }
	// return true;
	// }
	// }
	// fCurrentPosition = currentPosition;
	// return false;
	// }

	/**
	 * read until the string is found
	 * 
	 * @param name
	 * @return
	 */
	private final boolean readUntilString(String testedString) {
		int index = fStringSource.indexOf(testedString, fCurrentPosition);
		if (index != (-1)) {
			fCurrentPosition = index + testedString.length();
			return true;
		}
		return false;
	}

	/**
	 * read until character is found
	 * 
	 * @param name
	 * @return
	 */
	private final boolean readUntilChar(char testedChar) {
		int temp = fCurrentPosition;
		try {
			while ((fCurrentCharacter = charAt(fCurrentPosition++)) != testedChar) {
			}
			return true;
		} catch (IndexOutOfBoundsException e) {
			fCurrentPosition = temp;
			return false;
		}
	}

	/**
	 * read until character is found or end-of-line is reached
	 * 
	 * @param name
	 * @return -1 - for IndexOutOfBoundsException; 0 - for LF found; 1 - for
	 *         testedChar found
	 */
	private final boolean readTag() {
		int temp = fCurrentPosition;
		try {
			if (!StringUtil.isLetter(charAt(fCurrentPosition++))) {
				fCurrentPosition = temp;
				return false;
			}
			while (StringUtil.isLetterOrDigit(charAt(fCurrentPosition++))) {
			}
			return true;
		} catch (IndexOutOfBoundsException e) {
			fCurrentPosition = temp;
			return false;
		}
	}

	/**
	 * read until character is found or end-of-line is reached
	 * 
	 * @param name
	 * @return -1 - for IndexOutOfBoundsException; 0 - for LF found; 1 - for
	 *         testedChar found
	 */
	private final boolean readUntilEOL() {
		try {
			while (true) {
				fCurrentCharacter = charAt(fCurrentPosition++);
				if (fCurrentCharacter == '\n' || fCurrentCharacter == '\r') {
					return true;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			--fCurrentPosition;
			return true;
		}
	}

	public void parse() {
		int token = WikipediaFilter.TokenSTART;
		// fTokenStack.add(WikipediaFilter.START);
		// fListStack.add(START);
		try {
			while ((token = getNextToken()) != WikipediaFilter.TokenEOF) {
				switch (token) {
				case WikipediaFilter.TokenBOLD:
					if (!fTokenStack.isEmpty() && fTokenStack.peek() == WikipediaFilter.BOLD) {
						fTokenStack.pop();
						fResultBuffer.append("</b>");
					} else {
						fTokenStack.push(WikipediaFilter.BOLD);
						fResultBuffer.append("<b>");
					}
					break;
				case WikipediaFilter.TokenITALIC:
					if (!fTokenStack.isEmpty() && fTokenStack.peek() == WikipediaFilter.ITALIC) {
						fTokenStack.pop();
						fResultBuffer.append("</i>");
					} else {
						fTokenStack.push(WikipediaFilter.ITALIC);
						fResultBuffer.append("<i>");
					}
					break;
				case WikipediaFilter.TokenSTRONG:
					if (!fTokenStack.isEmpty() && fTokenStack.peek() == WikipediaFilter.STRONG) {
						fTokenStack.pop();
						fResultBuffer.append("</strong>");
					} else {
						fTokenStack.push(WikipediaFilter.STRONG);
						fResultBuffer.append("<strong>");
					}
					break;
				case WikipediaFilter.TokenEM:
					if (!fTokenStack.isEmpty() && fTokenStack.peek() == WikipediaFilter.EM) {
						fTokenStack.pop();
						fResultBuffer.append("</em>");
					} else {
						fTokenStack.push(WikipediaFilter.EM);
						fResultBuffer.append("<em>");
					}
					break;
				case WikipediaFilter.TokenSTRIKETHROUGH:
					if (!fTokenStack.isEmpty() && fTokenStack.peek() == WikipediaFilter.STRIKETHROUGH) {
						fTokenStack.pop();
						fResultBuffer.append("</strike>");
					} else {
						fTokenStack.push(WikipediaFilter.STRIKETHROUGH);
						fResultBuffer.append("<strike>");
					}
					break;
				// case TokenLIST_UL_START :
				// if (fTokenStack.peek().equals(LIST_UL_START)) {
				// fResult.append("</li>\n<li>");
				// } else {
				// fTokenStack.push(LIST_UL_START);
				// fResult.append("\n<ul class=\"star\">\n<li>");
				// }
				// break;
				// case TokenLIST_UL_END :
				// fTokenStack.pop();
				// fResult.append("</li>\n</ul>\n");
				// break;
				// case TokenLIST_OL_START :
				// if (fTokenStack.peek().equals(LIST_OL_START)) {
				// fResult.append("</li>\n<li>");
				// } else {
				// fTokenStack.push(LIST_OL_START);
				// fResult.append("\n<ol>\n<li>");
				// }
				// break;
				// case TokenLIST_OL_END :
				// fTokenStack.pop();
				// fResult.append("</li>\n</ol>\n");
				// break;
				}
			}
		} catch (InvalidInputException e) {
			//
		}
		reduceTokenStack();

		if (fResultBufferHeader != null) {
			if (!fNoToC) {
				if (isToC(fTableOfContent) > 3) {
					fResultBufferHeader.append("<table id=\"toc\" border=\"0\"><tr><th>Contents</th></tr><tr><td>");
					// fResultBufferHeader.append("<table
					// id=\"toc\"border=\"0\"><tr><th>Inhalt</th></tr><tr><td>");
					fResultBufferHeader.append("<ol>");
					createToC(fTableOfContent);
					fResultBufferHeader.append("</ol>");
					fResultBufferHeader.append("</td></tr></table><hr/>");
				}
			}

			fResultBufferHeader.append(fResultBuffer);
			fResultBuffer = fResultBufferHeader;
			fResultBufferHeader = null;
			fTableOfContent = null;
		}
	}

	/**
	 * 
	 */
	private void reduceTokenStack() {
		if (fTokenStack.isEmpty()) {
			return;
		}
		// clear rest of stack if necessary (case of error in syntax!?)
		AbstractTag tok;
		while (!fTokenStack.isEmpty()) {
			tok = (AbstractTag) fTokenStack.pop();

			if (tok instanceof OpenTagToken) {

				CloseTagToken closeToken = (CloseTagToken) WikipediaFilter.CLOSE_TAGS.get(tok.getTagName());
				if (closeToken == null) {
					// here is something wrong ???
					fResultBuffer.append("</" + (tok.getTagName()) + ">");
				} else {
					fResultBuffer.append(closeToken.getCloseTag());
				}
			} else if (tok == WikipediaFilter.BOLD) {
				fResultBuffer.append("</b>");
			} else if (tok == WikipediaFilter.ITALIC) {
				fResultBuffer.append("</i>");
			} else if (tok == WikipediaFilter.STRONG) {
				fResultBuffer.append("</strong>");
			} else if (tok == WikipediaFilter.EM) {
				fResultBuffer.append("</em>");
			} else if (tok == WikipediaFilter.STRIKETHROUGH) {
				fResultBuffer.append("</strike>");
			} else if (tok.equals(WikipediaFilter.LIST_UL_START)) {
				fResultBuffer.append("\n</li></ul>");
			} else if (tok.equals(WikipediaFilter.LIST_OL_START)) {
				fResultBuffer.append("\n</li></ol>");
			}
		}
	}

	/**
	 * count the number of wiki headers in this document
	 * 
	 * @param toc
	 * @return
	 */
	private int isToC(ArrayList toc) {

		if (toc.size() == 1 && (toc.get(0) instanceof ArrayList)) {
			return isToC((ArrayList) toc.get(0));
		}
		int result = 0;
		for (int i = 0; i < toc.size(); i++) {
			if (toc.get(i) instanceof ArrayList) {
				result += isToC((ArrayList) toc.get(i));
			} else {
				result++;
			}
		}
		return result;
	}

	private void createToC(ArrayList toc) {
		if (toc.size() == 1 && (toc.get(0) instanceof ArrayList)) {
			createToC((ArrayList) toc.get(0));
			return;
		}
		for (int i = 0; i < toc.size(); i++) {
			if (toc.get(i) instanceof ArrayList) {
				fResultBufferHeader.append("<ol>");
				createToC((ArrayList) toc.get(i));
				fResultBufferHeader.append("</ol>");
			} else {
				fResultBufferHeader.append(toc.get(i));
			}
		}
	}

	// public int readUntil(String testString) throws InvalidInputException {
	// startPosition = currentPosition;
	// int tempPosition;
	// boolean flag;
	// try {
	// while (true) {
	// currentCharacter = source[currentPosition++];
	// if (currentCharacter == testString.charAt(0)) {
	// tempPosition = currentPosition;
	// flag = true;
	// for (int i = 1; i < testString.length(); i++) {
	// currentCharacter = source[currentPosition++];
	// if (currentCharacter != testString.charAt(i)) {
	// flag = false;
	// currentPosition = tempPosition;
	// break;
	// }
	// }
	// if (flag) {
	// return TokenBODY;
	// }
	// }
	// }
	// } catch (IndexOutOfBoundsException e) {
	// // end of scanner text
	// }
	// return TokenEOF;
	// }

	public int scanIdentifierOrKeyword(boolean isVariable) throws InvalidInputException {
		while (getNextCharAsWikiPluginIdentifierPart()) {
		}
		;
		return WikipediaFilter.TokenIdentifier;
	}

	// private final void setSource(char[] source) {
	// // the source-buffer is set to sourceString
	// if (source == null) {
	// this.fSource = new char[0];
	// } else {
	// this.fSource = source;
	// }
	// // this.fEOFPosition = this.fSource.length;
	// // fStartPosition = -1;
	// }

	// private void unexpectedTag(String tag) {
	// fResultBuffer.append("<div class=\"error\">Unexpected end for tag: &lt;"
	// + tag + "&gt;</div>");
	// }

	/**
	 * @return Returns the wikiEngine.
	 */
	// public RenderEngine getWikiEngine() {
	// return fWikiEngine;
	// }
	/**
	 * @param wikiEngine
	 *          The wikiEngine to set.
	 */
	// public void setWikiEngine(RenderEngine wikiEngine) {
	// fWikiEngine = wikiEngine;
	// }
	public boolean isNoToC() {
		return fNoToC;
	}

	public void setNoToC(boolean noToC) {
		fNoToC = noToC;
	}

}
