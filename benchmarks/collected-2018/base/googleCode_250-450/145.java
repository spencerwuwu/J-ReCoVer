// https://searchcode.com/api/result/12907989/

package jist.editor;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Shape;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.WrappedPlainView;

/**
 * A collection of styles used to render java text. This class also acts as a
 * factory for the views used to represent the java documents. Since the
 * rendering styles are based upon view preferences, the views need a way to
 * gain access to the style settings which is facilitated by implementing the
 * factory in the style storage. Both functionalities can be widely shared
 * across java document views.
 */
public class SchemeContext extends StyleContext implements ViewFactory {

	/**
	 * Constructs a set of styles to represent java lexical tokens. By default
	 * there are no colors or fonts specified.
	 */
	public SchemeContext() {
		super();

		Style root = getStyle(DEFAULT_STYLE), parent;
		tokenStyles = new Style[Token.MAXINSTANCE + 1];

		Token[] tokens = Token.all;
		for (int i = 0, n = tokens.length; i < n; i++) {
			Token t = tokens[i];
			if ((parent = getStyle(t.getCategory())) == null)
				parent = addStyle(t.getCategory(), root);
			Style s = addStyle(null, parent);
			s.addAttribute(Token.TokenAttribute, t);
			tokenStyles[t.getScanValue()] = s;
		}
	}

	/**
	 * Fetch the foreground color to use for a lexical token with the given
	 * value.
	 */
	public Color getForeground(int code) {
		if (tokenColors == null)
			tokenColors = new Color[Token.MAXINSTANCE + 1];
		if ((code >= 0) && (code < tokenColors.length)) {
			Color c = tokenColors[code];
			// cache token color in the hashtable
			if (c == null)
				c = (tokenColors[code] = StyleConstants
						.getForeground(tokenStyles[code]));
			return c;
		}
		return Color.black;
	}

	/**
	 * Fetch the font to use for a lexical token with the given scan value.
	 */
	public Font getFont(int code) {
		if (tokenFonts == null)
			tokenFonts = new Font[Token.MAXINSTANCE + 1];
		if (code < tokenFonts.length) {
			Font f = tokenFonts[code];
			if (f == null) {
				// cache token font in the hashtable
				String fontFamily = StyleConstants
						.getFontFamily(tokenStyles[code]);
				int fontSize = StyleConstants.getFontSize(tokenStyles[code]);
				int fontStyle = Font.PLAIN;
				fontStyle |= StyleConstants.isBold(tokenStyles[code]) ? Font.BOLD
						: 0;
				fontStyle |= StyleConstants.isItalic(tokenStyles[code]) ? Font.ITALIC
						: 0;
				f = (tokenFonts[code] = new Font(fontFamily, fontStyle,
						fontSize));
			}
			return f;
		}
		return null;
	}

	/*
	 * Resets the token font. When a key is pressed the Scanner will call
	 * getFont(int code) above. This method fills in the tokenFonts array with
	 * new font styles.
	 */

	public void resetTokenFont() {
		tokenFonts = new Font[Token.MAXINSTANCE + 1];
	}

	/**
	 * Fetches the attribute set to use for the given scan code. The set is
	 * stored in a table to facilitate relatively fast access to use in
	 * conjunction with the scanner.
	 */
	public Style getStyleForScanValue(int code) {
		if (code < tokenStyles.length)
			return tokenStyles[code];
		else
			return null;
	}

	public View create(Element elem) {
		return (view = new SchemeView(elem));
	}

	public TokenScanner getScanner() {
		return lexer;
	}

	/**
	 * The view of the document
	 */
	public SchemeView view;

	/**
	 * The styles representing the actual token types
	 */
	protected Style[] tokenStyles;

	/**
	 * Cache of foreground colors to represent the various tokens
	 */
	protected transient Color[] tokenColors;

	/**
	 * Cache of fonts to represent the various tokens
	 */
	protected transient Font[] tokenFonts;

	/**
	 * View that uses the lexical information to determine the style
	 * characteristics of the text that it renders. This simply colorizes the
	 * various tokens and assumes a constant font family and size.
	 */
	public class SchemeView extends WrappedPlainView {

		/**
		 * Construct a simple colorized view of java text.
		 */
		public SchemeView(Element elem) {
			super(elem);
			lexer = ((SchemeDocument) getDocument()).createScanner();
			lexerValid = false;
		}

		/*
		 * Invalidate the lexer when the document changes
		 */
		public void changedUpdate(javax.swing.event.DocumentEvent e, Shape a,
				ViewFactory f) {
			super.insertUpdate(e, a, f);
			lexerValid = false;
		}

		/*
		 * Invalidate the lexer to reflect document insertion
		 */
		public void insertUpdate(javax.swing.event.DocumentEvent e, Shape a,
				ViewFactory f) {
			super.insertUpdate(e, a, f);
			lexerValid = false;
		}

		/*
		 * Invalidate the lexer to reflect document removal
		 */
		public void removeUpdate(javax.swing.event.DocumentEvent e, Shape a,
				ViewFactory f) {
			super.removeUpdate(e, a, f);
			lexerValid = false;
		}

		/*
		 * Force the lexer to update immediately
		 */
		public void forceUpdateScanner() {
			lexerValid = false;
			updateScanner();
		}

		/**
		 * Renders the given range in the model as normal unselected text. This
		 * is implemented to paint colors based upon the token-to-color
		 * translations. To reduce the number of calls to the Graphics object,
		 * text is batched up until a color change is detected or the entire
		 * requested range has been reached.
		 */
		protected int drawUnselectedText(Graphics g, int x, int y, int p0,
				int p1) throws BadLocationException {
			int mark = p0;

			for (; p0 < p1;) {
				updateScanner();
				lexer.getToken(p0);
				mark = Math.min(mark + lexer.getEndOffset(), p1);
				// draw the segment token by token
				g.setColor(getForeground(lexer.getScanValue()));
				g.setFont(getFont(lexer.getScanValue()));
				Segment text = getLineBuffer();
				getDocument().getText(p0, mark - p0, text);
				x = Utilities.drawTabbedText(text, x, y, g, this, p0);

				p0 = mark;
			}
			return x;
		}

		/**
		 * Update the scanner if necessary
		 */
		protected void updateScanner() {
			if (!lexerValid) {
				// text has been changed
				lexer = ((SchemeDocument) getDocument()).createScanner();
				lexer.scan();
				lexerValid = true;
			}
		}

		protected boolean lexerValid;
	}

	protected TokenScanner lexer = null;
}

/*
 * Copyright (c) 2004 Regents of the University of California (Regents). Created
 * by Graduate School of Education, University of California at Berkeley.
 * 
 * This software is distributed under the GNU General Public License, v2.
 * 
 * Permission is hereby granted, without written agreement and without license
 * or royalty fees, to use, copy, modify, and distribute this software and its
 * documentation for any purpose, provided that the above copyright notice and
 * the following two paragraphs appear in all copies of this software.
 * 
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE SOFTWAREAND ACCOMPANYING DOCUMENTATION, IF ANY, PROVIDED
 * HEREUNDER IS PROVIDED "AS IS". REGENTS HAS NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 * REGENTS HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

