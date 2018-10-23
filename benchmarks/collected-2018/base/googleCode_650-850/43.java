// https://searchcode.com/api/result/3833992/

/* 
	@Copyright (c) 2007 by Denis Riabtchik. All rights reserved. See license.txt and http://jgrouse.com for details@
	$Id: DocParser.java 600 2013-04-15 01:24:07Z denis.riabtchik@gmail.com $
 */

package jgrouse.jgdoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgrouse.jgdoc.api.IComment;
import jgrouse.jgdoc.api.ICommentTag;
import jgrouse.jgdoc.api.IContent;
import jgrouse.jgdoc.api.ILink;
import jgrouse.jgdoc.elements.Comment;
import jgrouse.jgdoc.elements.CommentTag;
import jgrouse.jgdoc.elements.Content;
import jgrouse.jgdoc.elements.ContentSection;
import jgrouse.jgdoc.elements.DocManager;
import jgrouse.jgdoc.elements.Link;

public class DocParser {
	private static Pattern RE_RESERVED_WORDS = Pattern.compile("(^|[^\\w$.])(" + "break|case|catch|continue|default|"
			+ "delete|do|else|finally|for|" + "function|if|in|instanceof|new|" + "return|switch|this|throw|try|"
			+ "typeof|var|void|while|with" + ")([^\\w$.]|$)");

	private static Pattern RE_IDENTIFIER_PATH = Pattern.compile("[^a-zA-Z\\$_.]*([\\w\\$.]+).*");

	private static final String NAME_PLACEHOLDER = "?";

	private DocManager m_docManager;
	private int m_lineNo;
	private String m_currentFile;
	private String m_srcEncoding;
	private List<String> m_htmlErrors = new LinkedList<String>();
	private String m_privatePrefix;

	private static String s_invalidNameChars = "\"<>&";

	public DocParser(DocManager pDocManager) {
		m_docManager = pDocManager;
	}

	public void doExtract(String fileName, String baseDirectory) throws IOException, JGDocException {
		m_currentFile = fileName;
		String fn = baseDirectory + fileName;
		File f = new File(fn);
		InputStream fis = null;
		try {
			try {
				m_docManager.startFile(fileName);
			} catch (JGDocException e) {
				m_docManager.handleException(e);
			}
			fis = new FileInputStream(f);
			byte strBuf[] = new byte[fis.available()];
			fis.read(strBuf);
			String s = m_srcEncoding == null ? new String(strBuf) : new String(strBuf, m_srcEncoding);
			s = s.replace("\r\n", "\n");
			List<Integer> lineIndices = new ArrayList<Integer>();
			int pos = 0;
			while (pos >= 0) {
				lineIndices.add(Integer.valueOf(pos));
				pos = s.indexOf('\n', pos + 1);
			}
			Integer[] indices = lineIndices.toArray(new Integer[lineIndices.size()]);
			boolean isInComment = false;
			StringBuffer buf = new StringBuffer();
			int i = 0;
			int begin = 0;
			int end = 0;
			while (i >= 0) {
				if (!isInComment) {
					i = s.indexOf("/**", i);
					if (i >= 0) {
						isInComment = true;
						begin = i;
						i += 2;
						if (i >= s.length()) {
							i = -1;
						}
					}
				} else {
					int linePos = Arrays.binarySearch(indices, Integer.valueOf(begin));
					if (linePos < 0) {
						linePos = -linePos - 1;
					}
					m_lineNo = linePos;

					end = s.indexOf("*/", i);
					if (end >= 0) {
						buf.append(s.substring(begin, end + 2));
						i = end < s.length() - 2 ? end + 2 : -1;
						try {
							IComment c = parseComment(buf);
							if (c != null && i >= 0) {
								Scanner scanner = new Scanner(s.substring(i));
								String id = parseIdentifier(scanner, false);
								if (id != null) {
									String name = c.getName();
									if (name != null && name.length() > 0) {
										String[] nameArr = name.split("\\.");
										if (nameArr[nameArr.length - 1].equals(NAME_PLACEHOLDER)) {
											nameArr[nameArr.length - 1] = id;
											StringBuffer b = new StringBuffer();
											for (String n : nameArr) {
												if (b.length() > 0) {
													b.append(".");
												}
												b.append(n);
											}
											c.setName(b.toString());
											c.resetPrivateModifier(m_privatePrefix);
										} else {
											TokenType tt = c.getTokenType();
											if (!(tt == TokenType.LOGICAL_CONTAINER || tt == TokenType.PHYS_CONTAINER)) {
												if (!nameArr[nameArr.length - 1].equals(id)) {
													m_docManager.warn(m_currentFile, m_lineNo, "\"" + name
															+ "\" in comment does not match \"" + id + "\" in source");
												}
											}
										}
									}
								}
								m_docManager.addComment(c);
							}
						} catch (JGDocException e) {
							m_docManager.handleException(e);
						}
						isInComment = false;
						buf = new StringBuffer();
					} else {
						buf.append(s.substring(begin));
						i = -1;
					}
				}
			}
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}

	private void checkName(String name, IComment pComment, boolean wasQuoted, Vocabulary token) throws JGDocException {
		if (NAME_PLACEHOLDER.equals(name) || isExternalHyperlink(name, token)) {
			return;
		}
		boolean inWarning = false;
		if (name != null) {
			String[] parts = name.split("\\.");
			if (parts.length > 0) {
				if (NAME_PLACEHOLDER.equals(parts[parts.length - 1])) {
					name = DocManager.getParentName(parts);
				}
			}
			for (int i = 0; i < s_invalidNameChars.length(); i++) {
				char c = s_invalidNameChars.charAt(i);
				if (name.indexOf(c) >= 0) {
					throw new JGDocException("Invalid character " + c + " in object's name " + name,
							pComment.getFile(), pComment.getLine());
				}
			}
			if (!wasQuoted) {
				for (int i = 0; i < name.length(); i++) {
					char c = name.charAt(i);
					if (pComment.getCommentType() != null
							&& pComment.getCommentType().getTokenType() == TokenType.PHYS_CONTAINER) {
						continue;
					}
					if (c == '.') {
						continue;
					}
					if (!inWarning && isValidIdentiferCharacter(i, c)) {
						inWarning = true;
					}
				}
			}

		}
		if (inWarning) {
			if (token != Vocabulary.FILE) {
				m_docManager.warn(m_currentFile, m_lineNo, "\"" + name + "\" is not a valid JavaScript identifier");
			}
		}
	}

	private boolean isExternalHyperlink(String name, Vocabulary token) {
		return token.isAllowExternalLink() && ILink.Util.isExternalLink(name);
	}

	private boolean isValidIdentiferCharacter(int i, char c) {
		return (i == 0 && !Character.isJavaIdentifierStart(c)) || (i > 0 && !Character.isJavaIdentifierPart(c));
	}

	private ICommentTag createCommentTag(Vocabulary token, String content, IComment pComment) throws JGDocException {
		if (token == null) {
			token = Vocabulary.CONTENT;
		}
		String type = null;
		String name = null;
		Set<String> modifiers = null;
		int wasQuoted = 0;
		if (token.isAllowsType() || token.isRequiresName()) {
			String origContent = content;
			content = content.trim();
			if (content.length() > 0) {
				if (token.isAllowsType() || token.getModifiers() != null) {
					if (content.charAt(0) == '{') {
						int pos = content.indexOf('}', 1);
						if (pos > 1) {
							String strType = content.substring(1, pos);
							String[] data = strType.split("\\s");
							boolean addToType = false;
							StringBuffer strTypeBuf = new StringBuffer();
							Set<String> mods = new LinkedHashSet<String>();
							for (int i = 0; i < data.length; i++) {
								String s = data[i];
								if (s.length() > 0) {
									if (!addToType) {
										if (Modifiers.s_allModifiersList.indexOf(s) >= 0) {
											if (token.getModifiers().indexOf(s) < 0) {
												throw new JGDocException(
														"Invalid modifier " + s + " in " + origContent,
														pComment.getFile(), pComment.getLine());
											}
											mods.add(s);
										} else {
											addToType = true;
										}
									}
									if (addToType) {
										if (strTypeBuf.length() != 0) {
											strTypeBuf.append(' ');
										}
										strTypeBuf.append(s);
									}
								}
							}
							modifiers = mods;
							type = strTypeBuf.toString();
						} else {
							throw new JGDocException("Cannot extract type from " + content, pComment.getFile(),
									pComment.getLine());
						}
						content = content.substring(pos + 1).trim();
					}
				}
				if (token.isAllowsName()) {
					int pos = -1;
					boolean inQuotedName = false;
					boolean isParam = token == Vocabulary.PARAM || token == Vocabulary.PARAMOPTION;
					for (int i = 0; i < content.length(); i++) {
						char c = content.charAt(i);
						if (!inQuotedName) {
							if (isParam && c == '\"') {
								inQuotedName = true;
							} else if (Character.isWhitespace(c)) {
								pos = i;
								break;
							}
						} else {
							if (c == '\"') {
								pos = i;
								inQuotedName = false;
								break;
							}
						}
					}
					if (inQuotedName) {
						throw new JGDocException("Unterminated quoted name", pComment.getFile(), pComment.getLine());
					}
					if (pos > 0) {
						wasQuoted = content.charAt(0) == '\"' ? 1 : 0;
						name = content.substring(wasQuoted, pos);
						content = content.substring(pos + wasQuoted);
					} else {
						name = content;
						content = "";
					}
				}
				if ((name == null || name.length() == 0) && token.isRequiresName()) {
					throw new JGDocException("Name not specified for " + token.toString().toLowerCase(),
							pComment.getFile(), pComment.getLine());
				}
				checkName(name, pComment, wasQuoted == 1, token);
			}
		} else if (token.isNameAsDesc()) {
			name = content;
			content = "";
		}
		checkTypeBrackets(type, pComment);
		ICommentTag ct = new CommentTag(token, parseContent(content, pComment), type, name, modifiers, pComment);
		ct.resetPrivateModifier(m_privatePrefix);
		return ct;
	}

	private void checkTypeBrackets(String pType, IComment pComment) throws JGDocException {
		if (pType == null) {
			return;
		}
		int count = 0;
		for (int i = 0; i < pType.length(); i++) {
			char c = pType.charAt(i);
			if (c == '[') {
				count++;
			}
			if (c == ']') {
				count--;
			}
		}
		if (count < 0) {
			throw new JGDocException("Unexpected ] in type " + pType, pComment.getFile(), pComment.getLine());
		} else if (count > 0) {
			throw new JGDocException("Missing ] in type " + pType, pComment.getFile(), pComment.getLine());
		}

	}

	private ILink createLink(String pString, IComment pComment) {
		int pos = -1;
		for (int i = 0; i < pString.length(); i++) {
			if (Character.isWhitespace(pString.charAt(i))) {
				pos = i;
				break;
			}
		}
		if (pos < 0) {
			return new Link(pString, pString, pComment);
		} else {
			return new Link(pString.substring(0, pos), pString.substring(pos).trim(), pComment);
		}

	}

	private IContent parseContent(String pContent, IComment pComment) throws JGDocException {
		pContent = trimLeadingAndTrailingEmptyLines(pContent);
		int prevStart = 0;
		int i = 0;
		IContent c = new Content();
		HTMLChecker htc = new HTMLChecker();
		try {
			htc.validateHTML(pContent);
		} catch (IOException e) {
			throw new JGDocException(e.getMessage(), pComment.getFile(), pComment.getLine());
		}
		if (htc.getError() != null) {
			String msg = htc.getError();
			// Or to show error and comment text...
			// String msg = htc.getError() + pContent;
			//
			if (m_htmlErrors.indexOf(msg) < 0) {
				// Get the line # error by adding the DocParser and HTMLChecker
				// line
				// #'s together... then subtract one because they both start at
				// 1.
				int line = pComment.getLine() + htc.getLine() - 1;
				m_htmlErrors.add(msg);
				m_docManager.warn(pComment.getFile(), line, msg);
			}
		}

		c.setOriginalString(pContent);
		while (i >= 0) {
			i = pContent.indexOf("{@link", i);
			if (i >= 0) {
				String prevContent = pContent.substring(prevStart, i);
				if (prevContent.length() > 0) {
					c.addContentSection(new ContentSection(prevContent));
				}
				int end = pContent.indexOf('}', i);
				if (end <= 0) {
					throw new JGDocException("Missing } for @link", pComment.getFile(), pComment.getLine());
				}
				String linkContent = pContent.substring(i + 6, end);
				ILink l = createLink(linkContent.trim(), pComment);
				c.addContentSection(l);
				i = end;
				prevStart = i + 1;
			}
		}
		String content = pContent.substring(prevStart);
		if (content.length() > 0) {
			c.addContentSection(new ContentSection(content));
		}
		return c;
	}

	private String trimLeadingAndTrailingEmptyLines(String pContent) {
		String[] split = pContent.split("\\n");
		for (int i = split.length - 1; i>= 0; i--) {
			String s = split[i];
			if (s.trim().length() != 0) {
				break;
			}
			split[i] = null;
		}
		for (int i = 0; i < split.length; i++) {
			String s = split[i];
			if (s == null || s.trim().length() > 0) {
				break;
			}
			split[i] = null;
		}
		StringBuilder sb = new StringBuilder();
		for (String s : split) {
			if (s != null) {
				if (sb.length() > 0) {
					sb.append("\n");
				}
				sb.append(s);
			}
		}
		return sb.toString();
	}

	private void addContent(IComment comment, String pContent) throws JGDocException {
		if (comment.getContent() != null) {
			throw new JGDocException("Content already defined", comment.getFile(), comment.getLine());
		}
		int pos = 0;
		while (pos >= 0 && pos < pContent.length()) {
			pos = pContent.indexOf('.', pos);
			if (pos >= 0) {
				if ((pos == pContent.length() - 1 || Character.isWhitespace(pContent.charAt(pos + 1)))) {
					break;
				}
				pos++;
			}
		}
		String summary = pos > 0 ? pContent.substring(0, pos + 1) : pContent;
		IContent summaryContent = parseContent(summary, comment);
		IContent maintContent = parseContent(pContent, comment);
		comment.setContent(maintContent);
		comment.setSummary(summaryContent);
	}

	private void parseCommentBuffer(IComment comment, StringBuffer buf) throws JGDocException {
		String s = new String(buf);
		int i = 0;
		Vocabulary currentToken = null;
		int prevStart = 0;
		while (i >= 0) {
			i = s.indexOf('@', i);
			if (i >= 0) {
				i++;
				Vocabulary foundToken = null;
				int whiteSpacePos = i;
				if (i == 1 || Character.isWhitespace(s.charAt(i - 2))) {
					do {
						char c = s.charAt(whiteSpacePos);
						if (Character.isWhitespace(c)) {
							break;
						}
						whiteSpacePos++;
					} while (whiteSpacePos < s.length());
					String strToken = s.substring(i, whiteSpacePos);
					if ("...".equals(strToken)) {
						foundToken = Vocabulary.PARAMOPTION;
					} else {
						try {
							foundToken = Vocabulary.valueOf(strToken.toUpperCase());
						} catch (IllegalArgumentException e) {
							m_docManager.warn(m_currentFile, m_lineNo, "Unknown tag \"@" + strToken + "\"");
						}
					}
				}
				if (foundToken != null) {
					String prevContent = s.substring(prevStart, i - 1);
					if (prevContent.length() > 0) {
						if (currentToken == null) {
							addContent(comment, prevContent);
						} else {
							makeCommentTag(currentToken, prevContent, comment);
						}
					}
					currentToken = foundToken;
					prevStart = whiteSpacePos;
					i = prevStart;
				}
			}
		}
		String content = s.substring(prevStart);
		if (currentToken == null) {
			addContent(comment, content);
		} else {
			makeCommentTag(currentToken, content, comment);
		}

	}

	private void makeCommentTag(Vocabulary currentToken, String content, IComment comment) throws JGDocException {
		ICommentTag ct = createCommentTag(currentToken, content, comment);
		if (currentToken.getTokenType().isTagType()) {
			String orig = ct.getContent().getOriginalString().trim();
			if (orig.length() != 0) {
				IContent summaryContent = parseContent(orig, comment);
				IContent maintContent = parseContent(orig + "\n" + comment.getContent().getOriginalString(), comment);
				comment.setContent(maintContent);
				comment.setSummary(summaryContent);
			}
		}
		comment.addCommentTag(ct);
	}

	private IComment parseComment(StringBuffer buf) throws JGDocException {
		if (buf.length() <= 4) {
			return null;
		}
		buf.delete(0, 3);
		buf.delete(buf.length() - 2, buf.length());
		boolean isBeginning = true;
		int bol = 0;
		for (int i = 0; i < buf.length(); i++) {
			char c = buf.charAt(i);
			switch (c) {
			case '\r':
			case '\n': {
				isBeginning = true;
				bol = i;
				break;
			}
			case ' ':
			case '\t': {
				break;
			}
			case '*': {
				if (isBeginning) {
					buf.delete(bol + 1, i + 1);
					i = bol;
					// buf.setCharAt(i, ' ');
				}
				break;
			}
			default: {
				isBeginning = false;
			}
			}
		}
		Comment c = new Comment(m_currentFile, m_lineNo);
		parseCommentBuffer(c, buf);

		if (m_docManager.getSrcUrl() != null) {
			String s = m_docManager.getSrcUrl().replace("${jg_filePath}", m_currentFile)
					.replace("${jg_lineNum}", Integer.toString((m_lineNo + 1)));
			c.setSrcUrl(s);
		}
		return c;
	}

	public String getSrcEncoding() {
		return m_srcEncoding;
	}

	public void setSrcEncoding(String pSrcEncoding) {
		m_srcEncoding = pSrcEncoding;
	}

	/**
	 * <p>
	 * Find the first javascript identifier referenced in source code. The parse
	 * logic is fairly simple - it returns the first identifier it finds,
	 * stripped of any leading "."-delimited path. For example, this will return
	 * "foobar" in all of the following cases:
	 * </p>
	 * <ul>
	 * <li>foobar: ...</li>
	 * <li>function foobar(...</li>
	 * <li>if (!Some.Namespace.foobar) ...</li>
	 * <li>Some.Namespace.foobar = ...</li>
	 * <li>this.foobar = ...</li>
	 * <li>window.foobar = ...</li>
	 * </ul>
	 * 
	 * @param scanner
	 * @param inComment
	 *            True if the jgd comment that triggered this call was not
	 *            closed (i.e. the parser needs to scan to the end of the
	 *            current comment before looking for an identifier)
	 */
	public static String parseIdentifier(Scanner scanner, boolean inComment) throws IOException {
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine().trim();
			// lineNum++;

			// Scan to the end of the current [jgd] comment?
			if (inComment) {
				if (line.indexOf("*/") < 0)
					continue;
				inComment = false;
			}

			// Skip the line if it looks like a comment
			if (line.matches("^(//|/\\*[^\\*]|\\*|\\*/).*"))
				continue;

			// If it's another jgd comment, stop looking (i.e. no identifier
			// found)
			if (line.indexOf("/**") >= 0)
				return null;

			// Basic tests passed, so reduce it down to the identifier
			String id = line;

			// Remove all of javascript's reserved words
			id = RE_RESERVED_WORDS.matcher(id).replaceAll("");

			// Extract anything that looks like a full name. e.g.
			// "path.to.some.identifier"
			Matcher matcher = RE_IDENTIFIER_PATH.matcher(id);
			if (matcher.matches()) {
				id = matcher.group(1);
				if (id == null)
					continue;
			} else {
				id = "";
			}

			// Stripping off leading path
			id = id.replaceAll(".*\\.", "");

			// Anything left at this point must be the identifier
			if (id.length() > 0) {
				return id;
			}
		}

		// Nothing found
		return null;
	}

	public String getPrivatePrefix() {
		return m_privatePrefix;
	}

	public void setPrivatePrefix(String pPrivatePrefix) {
		m_privatePrefix = pPrivatePrefix;
	}

}

