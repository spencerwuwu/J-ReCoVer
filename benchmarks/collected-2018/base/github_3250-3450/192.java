// https://searchcode.com/api/result/17110457/

/*
 * Copyright 1998-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package javax.swing.text.html;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.HeadlessException;
import java.awt.Image;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Locale;
import javax.swing.ImageIcon;
import javax.swing.SizeRequirements;
import javax.swing.text.*;

/**
 * Defines a set of
 * <a href="http://www.w3.org/TR/REC-CSS1">CSS attributes</a>
 * as a typesafe enumeration.  The HTML View implementations use
 * CSS attributes to determine how they will render. This also defines
 * methods to map between CSS/HTML/StyleConstants. Any shorthand
 * properties, such as font, are mapped to the intrinsic properties.
 * <p>The following describes the CSS properties that are suppored by the
 * rendering engine:
 * <ul><li>font-family
 *   <li>font-style
 *   <li>font-size (supports relative units)
 *   <li>font-weight
 *   <li>font
 *   <li>color
 *   <li>background-color (with the exception of transparent)
 *   <li>background-image
 *   <li>background-repeat
 *   <li>background-position
 *   <li>background
 *   <li>background-repeat
 *   <li>text-decoration (with the exception of blink and overline)
 *   <li>vertical-align (only sup and super)
 *   <li>text-align (justify is treated as center)
 *   <li>margin-top
 *   <li>margin-right
 *   <li>margin-bottom
 *   <li>margin-left
 *   <li>margin
 *   <li>padding-top
 *   <li>padding-right
 *   <li>padding-bottom
 *   <li>padding-left
 *   <li>border-style (only supports inset, outset and none)
 *   <li>list-style-type
 *   <li>list-style-position
 * </ul>
 * The following are modeled, but currently not rendered.
 * <ul><li>font-variant
 *   <li>background-attachment (background always treated as scroll)
 *   <li>word-spacing
 *   <li>letter-spacing
 *   <li>text-indent
 *   <li>text-transform
 *   <li>line-height
 *   <li>border-top-width (this is used to indicate if a border should be used)
 *   <li>border-right-width
 *   <li>border-bottom-width
 *   <li>border-left-width
 *   <li>border-width
 *   <li>border-top
 *   <li>border-right
 *   <li>border-bottom
 *   <li>border-left
 *   <li>border
 *   <li>width
 *   <li>height
 *   <li>float
 *   <li>clear
 *   <li>display
 *   <li>white-space
 *   <li>list-style
 * </ul>
 * <p><b>Note: for the time being we do not fully support relative units,
 * unless noted, so that
 * p { margin-top: 10% } will be treated as if no margin-top was specified.
 *
 * @author  Timothy Prinzing
 * @author  Scott Violet
 * @see StyleSheet
 */
public class CSS implements Serializable {

    /**
     * Definitions to be used as a key on AttributeSet's
     * that might hold CSS attributes.  Since this is a
     * closed set (i.e. defined exactly by the specification),
     * it is final and cannot be extended.
     */
    public static final class Attribute {

        private Attribute(String name, String defaultValue, boolean inherited) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.inherited = inherited;
        }

        /**
         * The string representation of the attribute.  This
         * should exactly match the string specified in the
         * CSS specification.
         */
        public String toString() {
            return name;
        }

        /**
         * Fetch the default value for the attribute.
         * If there is no default value (such as for
         * composite attributes), null will be returned.
         */
        public String getDefaultValue() {
            return defaultValue;
        }

        /**
         * Indicates if the attribute should be inherited
         * from the parent or not.
         */
        public boolean isInherited() {
            return inherited;
        }

        private String name;
        private String defaultValue;
        private boolean inherited;


        public static final Attribute BACKGROUND =
            new Attribute("background", null, false);

        public static final Attribute BACKGROUND_ATTACHMENT =
            new Attribute("background-attachment", "scroll", false);

        public static final Attribute BACKGROUND_COLOR =
            new Attribute("background-color", "transparent", false);

        public static final Attribute BACKGROUND_IMAGE =
            new Attribute("background-image", "none", false);

        public static final Attribute BACKGROUND_POSITION =
            new Attribute("background-position", null, false);

        public static final Attribute BACKGROUND_REPEAT =
            new Attribute("background-repeat", "repeat", false);

        public static final Attribute BORDER =
            new Attribute("border", null, false);

        public static final Attribute BORDER_BOTTOM =
            new Attribute("border-bottom", null, false);

        public static final Attribute BORDER_BOTTOM_WIDTH =
            new Attribute("border-bottom-width", "medium", false);

        public static final Attribute BORDER_COLOR =
            new Attribute("border-color", "black", false);

        public static final Attribute BORDER_LEFT =
            new Attribute("border-left", null, false);

        public static final Attribute BORDER_LEFT_WIDTH =
            new Attribute("border-left-width", "medium", false);

        public static final Attribute BORDER_RIGHT =
            new Attribute("border-right", null, false);

        public static final Attribute BORDER_RIGHT_WIDTH =
            new Attribute("border-right-width", "medium", false);

        public static final Attribute BORDER_STYLE =
            new Attribute("border-style", "none", false);

        public static final Attribute BORDER_TOP =
            new Attribute("border-top", null, false);

        public static final Attribute BORDER_TOP_WIDTH =
            new Attribute("border-top-width", "medium", false);

        public static final Attribute BORDER_WIDTH =
            new Attribute("border-width", "medium", false);

        public static final Attribute CLEAR =
            new Attribute("clear", "none", false);

        public static final Attribute COLOR =
            new Attribute("color", "black", true);

        public static final Attribute DISPLAY =
            new Attribute("display", "block", false);

        public static final Attribute FLOAT =
            new Attribute("float", "none", false);

        public static final Attribute FONT =
            new Attribute("font", null, true);

        public static final Attribute FONT_FAMILY =
            new Attribute("font-family", null, true);

        public static final Attribute FONT_SIZE =
            new Attribute("font-size", "medium", true);

        public static final Attribute FONT_STYLE =
            new Attribute("font-style", "normal", true);

        public static final Attribute FONT_VARIANT =
            new Attribute("font-variant", "normal", true);

        public static final Attribute FONT_WEIGHT =
            new Attribute("font-weight", "normal", true);

        public static final Attribute HEIGHT =
            new Attribute("height", "auto", false);

        public static final Attribute LETTER_SPACING =
            new Attribute("letter-spacing", "normal", true);

        public static final Attribute LINE_HEIGHT =
            new Attribute("line-height", "normal", true);

        public static final Attribute LIST_STYLE =
            new Attribute("list-style", null, true);

        public static final Attribute LIST_STYLE_IMAGE =
            new Attribute("list-style-image", "none", true);

        public static final Attribute LIST_STYLE_POSITION =
            new Attribute("list-style-position", "outside", true);

        public static final Attribute LIST_STYLE_TYPE =
            new Attribute("list-style-type", "disc", true);

        public static final Attribute MARGIN =
            new Attribute("margin", null, false);

        public static final Attribute MARGIN_BOTTOM =
            new Attribute("margin-bottom", "0", false);

        public static final Attribute MARGIN_LEFT =
            new Attribute("margin-left", "0", false);

        public static final Attribute MARGIN_RIGHT =
            new Attribute("margin-right", "0", false);

        /*
         * made up css attributes to describe orientation depended
         * margins. used for <dir>, <menu>, <ul> etc. see
         * 5088268 for more details
         */
        static final Attribute MARGIN_LEFT_LTR =
            new Attribute("margin-left-ltr",
                          Integer.toString(Integer.MIN_VALUE), false);

        static final Attribute MARGIN_LEFT_RTL =
            new Attribute("margin-left-rtl",
                          Integer.toString(Integer.MIN_VALUE), false);

        static final Attribute MARGIN_RIGHT_LTR =
            new Attribute("margin-right-ltr",
                          Integer.toString(Integer.MIN_VALUE), false);

        static final Attribute MARGIN_RIGHT_RTL =
            new Attribute("margin-right-rtl",
                          Integer.toString(Integer.MIN_VALUE), false);


        public static final Attribute MARGIN_TOP =
            new Attribute("margin-top", "0", false);

        public static final Attribute PADDING =
            new Attribute("padding", null, false);

        public static final Attribute PADDING_BOTTOM =
            new Attribute("padding-bottom", "0", false);

        public static final Attribute PADDING_LEFT =
            new Attribute("padding-left", "0", false);

        public static final Attribute PADDING_RIGHT =
            new Attribute("padding-right", "0", false);

        public static final Attribute PADDING_TOP =
            new Attribute("padding-top", "0", false);

        public static final Attribute TEXT_ALIGN =
            new Attribute("text-align", null, true);

        public static final Attribute TEXT_DECORATION =
            new Attribute("text-decoration", "none", true);

        public static final Attribute TEXT_INDENT =
            new Attribute("text-indent", "0", true);

        public static final Attribute TEXT_TRANSFORM =
            new Attribute("text-transform", "none", true);

        public static final Attribute VERTICAL_ALIGN =
            new Attribute("vertical-align", "baseline", false);

        public static final Attribute WORD_SPACING =
            new Attribute("word-spacing", "normal", true);

        public static final Attribute WHITE_SPACE =
            new Attribute("white-space", "normal", true);

        public static final Attribute WIDTH =
            new Attribute("width", "auto", false);

        /*public*/ static final Attribute BORDER_SPACING =
            new Attribute("border-spacing", "0", true);

        /*public*/ static final Attribute CAPTION_SIDE =
            new Attribute("caption-side", "left", true);

        // All possible CSS attribute keys.
        static final Attribute[] allAttributes = {
            BACKGROUND, BACKGROUND_ATTACHMENT, BACKGROUND_COLOR,
            BACKGROUND_IMAGE, BACKGROUND_POSITION, BACKGROUND_REPEAT,
            BORDER, BORDER_BOTTOM, BORDER_BOTTOM_WIDTH, BORDER_COLOR,
            BORDER_LEFT, BORDER_LEFT_WIDTH, BORDER_RIGHT, BORDER_RIGHT_WIDTH,
            BORDER_STYLE, BORDER_TOP, BORDER_TOP_WIDTH, BORDER_WIDTH,
            CLEAR, COLOR, DISPLAY, FLOAT, FONT, FONT_FAMILY, FONT_SIZE,
            FONT_STYLE, FONT_VARIANT, FONT_WEIGHT, HEIGHT, LETTER_SPACING,
            LINE_HEIGHT, LIST_STYLE, LIST_STYLE_IMAGE, LIST_STYLE_POSITION,
            LIST_STYLE_TYPE, MARGIN, MARGIN_BOTTOM, MARGIN_LEFT, MARGIN_RIGHT,
            MARGIN_TOP, PADDING, PADDING_BOTTOM, PADDING_LEFT, PADDING_RIGHT,
            PADDING_TOP, TEXT_ALIGN, TEXT_DECORATION, TEXT_INDENT, TEXT_TRANSFORM,
            VERTICAL_ALIGN, WORD_SPACING, WHITE_SPACE, WIDTH,
            BORDER_SPACING, CAPTION_SIDE,
            MARGIN_LEFT_LTR, MARGIN_LEFT_RTL, MARGIN_RIGHT_LTR, MARGIN_RIGHT_RTL
        };

        private static final Attribute[] ALL_MARGINS =
                { MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT };
        private static final Attribute[] ALL_PADDING =
                { PADDING_TOP, PADDING_RIGHT, PADDING_BOTTOM, PADDING_LEFT };
        private static final Attribute[] ALL_BORDER_WIDTHS =
                { BORDER_TOP_WIDTH, BORDER_RIGHT_WIDTH, BORDER_BOTTOM_WIDTH,
                  BORDER_LEFT_WIDTH };

    }

    static final class Value {

        private Value(String name) {
            this.name = name;
        }

        /**
         * The string representation of the attribute.  This
         * should exactly match the string specified in the
         * CSS specification.
         */
        public String toString() {
            return name;
        }

        static final Value INHERITED = new Value("inherited");
        static final Value NONE = new Value("none");
        static final Value DOTTED = new Value("dotted");
        static final Value DASHED = new Value("dashed");
        static final Value SOLID = new Value("solid");
        static final Value DOUBLE = new Value("double");
        static final Value GROOVE = new Value("groove");
        static final Value RIDGE = new Value("ridge");
        static final Value INSET = new Value("inset");
        static final Value OUTSET = new Value("outset");
        // Lists.
        static final Value BLANK_LIST_ITEM = new Value("none");
        static final Value DISC = new Value("disc");
        static final Value CIRCLE = new Value("circle");
        static final Value SQUARE = new Value("square");
        static final Value DECIMAL = new Value("decimal");
        static final Value LOWER_ROMAN = new Value("lower-roman");
        static final Value UPPER_ROMAN = new Value("upper-roman");
        static final Value LOWER_ALPHA = new Value("lower-alpha");
        static final Value UPPER_ALPHA = new Value("upper-alpha");
        // background-repeat
        static final Value BACKGROUND_NO_REPEAT = new Value("no-repeat");
        static final Value BACKGROUND_REPEAT = new Value("repeat");
        static final Value BACKGROUND_REPEAT_X = new Value("repeat-x");
        static final Value BACKGROUND_REPEAT_Y = new Value("repeat-y");
        // background-attachment
        static final Value BACKGROUND_SCROLL = new Value("scroll");
        static final Value BACKGROUND_FIXED = new Value("fixed");

        private String name;

        static final Value[] allValues = {
            INHERITED, NONE, DOTTED, DASHED, SOLID, DOUBLE, GROOVE,
            RIDGE, INSET, OUTSET, DISC, CIRCLE, SQUARE, DECIMAL,
            LOWER_ROMAN, UPPER_ROMAN, LOWER_ALPHA, UPPER_ALPHA,
            BLANK_LIST_ITEM, BACKGROUND_NO_REPEAT, BACKGROUND_REPEAT,
            BACKGROUND_REPEAT_X, BACKGROUND_REPEAT_Y,
            BACKGROUND_FIXED, BACKGROUND_FIXED
        };
    }

    public CSS() {
        baseFontSize = baseFontSizeIndex + 1;
        // setup the css conversion table
        valueConvertor = new Hashtable();
        valueConvertor.put(CSS.Attribute.FONT_SIZE, new FontSize());
        valueConvertor.put(CSS.Attribute.FONT_FAMILY, new FontFamily());
        valueConvertor.put(CSS.Attribute.FONT_WEIGHT, new FontWeight());
        valueConvertor.put(CSS.Attribute.BORDER_STYLE, new BorderStyle());
        Object cv = new ColorValue();
        valueConvertor.put(CSS.Attribute.COLOR, cv);
        valueConvertor.put(CSS.Attribute.BACKGROUND_COLOR, cv);
        valueConvertor.put(CSS.Attribute.BORDER_COLOR, cv);
        Object lv = new LengthValue();
        valueConvertor.put(CSS.Attribute.MARGIN_TOP, lv);
        valueConvertor.put(CSS.Attribute.MARGIN_BOTTOM, lv);
        valueConvertor.put(CSS.Attribute.MARGIN_LEFT, lv);
        valueConvertor.put(CSS.Attribute.MARGIN_LEFT_LTR, lv);
        valueConvertor.put(CSS.Attribute.MARGIN_LEFT_RTL, lv);
        valueConvertor.put(CSS.Attribute.MARGIN_RIGHT, lv);
        valueConvertor.put(CSS.Attribute.MARGIN_RIGHT_LTR, lv);
        valueConvertor.put(CSS.Attribute.MARGIN_RIGHT_RTL, lv);
        valueConvertor.put(CSS.Attribute.PADDING_TOP, lv);
        valueConvertor.put(CSS.Attribute.PADDING_BOTTOM, lv);
        valueConvertor.put(CSS.Attribute.PADDING_LEFT, lv);
        valueConvertor.put(CSS.Attribute.PADDING_RIGHT, lv);
        Object bv = new BorderWidthValue(null, 0);
        valueConvertor.put(CSS.Attribute.BORDER_WIDTH, lv);
        valueConvertor.put(CSS.Attribute.BORDER_TOP_WIDTH, bv);
        valueConvertor.put(CSS.Attribute.BORDER_BOTTOM_WIDTH, bv);
        valueConvertor.put(CSS.Attribute.BORDER_LEFT_WIDTH, bv);
        valueConvertor.put(CSS.Attribute.BORDER_RIGHT_WIDTH, bv);
        Object nlv = new LengthValue(true);
        valueConvertor.put(CSS.Attribute.TEXT_INDENT, nlv);
        valueConvertor.put(CSS.Attribute.WIDTH, lv);
        valueConvertor.put(CSS.Attribute.HEIGHT, lv);
        valueConvertor.put(CSS.Attribute.BORDER_SPACING, lv);
        Object sv = new StringValue();
        valueConvertor.put(CSS.Attribute.FONT_STYLE, sv);
        valueConvertor.put(CSS.Attribute.TEXT_DECORATION, sv);
        valueConvertor.put(CSS.Attribute.TEXT_ALIGN, sv);
        valueConvertor.put(CSS.Attribute.VERTICAL_ALIGN, sv);
        Object valueMapper = new CssValueMapper();
        valueConvertor.put(CSS.Attribute.LIST_STYLE_TYPE,
                           valueMapper);
        valueConvertor.put(CSS.Attribute.BACKGROUND_IMAGE,
                           new BackgroundImage());
        valueConvertor.put(CSS.Attribute.BACKGROUND_POSITION,
                           new BackgroundPosition());
        valueConvertor.put(CSS.Attribute.BACKGROUND_REPEAT,
                           valueMapper);
        valueConvertor.put(CSS.Attribute.BACKGROUND_ATTACHMENT,
                           valueMapper);
        Object generic = new CssValue();
        int n = CSS.Attribute.allAttributes.length;
        for (int i = 0; i < n; i++) {
            CSS.Attribute key = CSS.Attribute.allAttributes[i];
            if (valueConvertor.get(key) == null) {
                valueConvertor.put(key, generic);
            }
        }
    }

    /**
     * Sets the base font size. <code>sz</code> is a CSS value, and is
     * not necessarily the point size. Use getPointSize to determine the
     * point size corresponding to <code>sz</code>.
     */
    void setBaseFontSize(int sz) {
        if (sz < 1)
          baseFontSize = 0;
        else if (sz > 7)
          baseFontSize = 7;
        else
          baseFontSize = sz;
    }

    /**
     * Sets the base font size from the passed in string.
     */
    void setBaseFontSize(String size) {
        int relSize, absSize, diff;

        if (size != null) {
            if (size.startsWith("+")) {
                relSize = Integer.valueOf(size.substring(1)).intValue();
                setBaseFontSize(baseFontSize + relSize);
            } else if (size.startsWith("-")) {
                relSize = -Integer.valueOf(size.substring(1)).intValue();
                setBaseFontSize(baseFontSize + relSize);
            } else {
                setBaseFontSize(Integer.valueOf(size).intValue());
            }
        }
    }

    /**
     * Returns the base font size.
     */
    int getBaseFontSize() {
        return baseFontSize;
    }

    /**
     * Parses the CSS property <code>key</code> with value
     * <code>value</code> placing the result in <code>att</code>.
     */
    void addInternalCSSValue(MutableAttributeSet attr,
                             CSS.Attribute key, String value) {
        if (key == CSS.Attribute.FONT) {
            ShorthandFontParser.parseShorthandFont(this, value, attr);
        }
        else if (key == CSS.Attribute.BACKGROUND) {
            ShorthandBackgroundParser.parseShorthandBackground
                               (this, value, attr);
        }
        else if (key == CSS.Attribute.MARGIN) {
            ShorthandMarginParser.parseShorthandMargin(this, value, attr,
                                           CSS.Attribute.ALL_MARGINS);
        }
        else if (key == CSS.Attribute.PADDING) {
            ShorthandMarginParser.parseShorthandMargin(this, value, attr,
                                           CSS.Attribute.ALL_PADDING);
        }
        else if (key == CSS.Attribute.BORDER_WIDTH) {
            ShorthandMarginParser.parseShorthandMargin(this, value, attr,
                                           CSS.Attribute.ALL_BORDER_WIDTHS);
        }
        else {
            Object iValue = getInternalCSSValue(key, value);
            if (iValue != null) {
                attr.addAttribute(key, iValue);
            }
        }
    }

    /**
     * Gets the internal CSS representation of <code>value</code> which is
     * a CSS value of the CSS attribute named <code>key</code>. The receiver
     * should not modify <code>value</code>, and the first <code>count</code>
     * strings are valid.
     */
    Object getInternalCSSValue(CSS.Attribute key, String value) {
        CssValue conv = (CssValue) valueConvertor.get(key);
        Object r = conv.parseCssValue(value);
        return r != null ? r : conv.parseCssValue(key.getDefaultValue());
    }

    /**
     * Maps from a StyleConstants to a CSS Attribute.
     */
    Attribute styleConstantsKeyToCSSKey(StyleConstants sc) {
        return (Attribute)styleConstantToCssMap.get(sc);
    }

    /**
     * Maps from a StyleConstants value to a CSS value.
     */
    Object styleConstantsValueToCSSValue(StyleConstants sc,
                                         Object styleValue) {
        Object cssKey = styleConstantsKeyToCSSKey(sc);
        if (cssKey != null) {
            CssValue conv = (CssValue)valueConvertor.get(cssKey);
            return conv.fromStyleConstants(sc, styleValue);
        }
        return null;
    }

    /**
     * Converts the passed in CSS value to a StyleConstants value.
     * <code>key</code> identifies the CSS attribute being mapped.
     */
    Object cssValueToStyleConstantsValue(StyleConstants key, Object value) {
        if (value instanceof CssValue) {
            return ((CssValue)value).toStyleConstants((StyleConstants)key,
                                                      null);
        }
        return null;
    }

    /**
     * Returns the font for the values in the passed in AttributeSet.
     * It is assumed the keys will be CSS.Attribute keys.
     * <code>sc</code> is the StyleContext that will be messaged to get
     * the font once the size, name and style have been determined.
     */
    Font getFont(StyleContext sc, AttributeSet a, int defaultSize, StyleSheet ss) {
        ss = getStyleSheet(ss);
        int size = getFontSize(a, defaultSize, ss);

        /*
         * If the vertical alignment is set to either superscirpt or
         * subscript we reduce the font size by 2 points.
         */
        StringValue vAlignV = (StringValue)a.getAttribute
                              (CSS.Attribute.VERTICAL_ALIGN);
        if ((vAlignV != null)) {
            String vAlign = vAlignV.toString();
            if ((vAlign.indexOf("sup") >= 0) ||
                (vAlign.indexOf("sub") >= 0)) {
                size -= 2;
            }
        }

        FontFamily familyValue = (FontFamily)a.getAttribute
                                            (CSS.Attribute.FONT_FAMILY);
        String family = (familyValue != null) ? familyValue.getValue() :
                                  Font.SANS_SERIF;
        int style = Font.PLAIN;
        FontWeight weightValue = (FontWeight) a.getAttribute
                                  (CSS.Attribute.FONT_WEIGHT);
        if ((weightValue != null) && (weightValue.getValue() > 400)) {
            style |= Font.BOLD;
        }
        Object fs = a.getAttribute(CSS.Attribute.FONT_STYLE);
        if ((fs != null) && (fs.toString().indexOf("italic") >= 0)) {
            style |= Font.ITALIC;
        }
        if (family.equalsIgnoreCase("monospace")) {
            family = Font.MONOSPACED;
        }
        Font f = sc.getFont(family, style, size);
        if (f == null
            || (f.getFamily().equals(Font.DIALOG)
                && ! family.equalsIgnoreCase(Font.DIALOG))) {
            family = Font.SANS_SERIF;
            f = sc.getFont(family, style, size);
        }
        return f;
    }

    static int getFontSize(AttributeSet attr, int defaultSize, StyleSheet ss) {
        // PENDING(prinz) this is a 1.1 based implementation, need to also
        // have a 1.2 version.
        FontSize sizeValue = (FontSize)attr.getAttribute(CSS.Attribute.
                                                         FONT_SIZE);

        return (sizeValue != null) ? sizeValue.getValue(attr, ss)
                                   : defaultSize;
    }

    /**
     * Takes a set of attributes and turn it into a color
     * specification.  This might be used to specify things
     * like brighter, more hue, etc.
     * This will return null if there is no value for <code>key</code>.
     *
     * @param key CSS.Attribute identifying where color is stored.
     * @param a the set of attributes
     * @return the color
     */
    Color getColor(AttributeSet a, CSS.Attribute key) {
        ColorValue cv = (ColorValue) a.getAttribute(key);
        if (cv != null) {
            return cv.getValue();
        }
        return null;
    }

    /**
     * Returns the size of a font from the passed in string.
     *
     * @param size CSS string describing font size
     * @param baseFontSize size to use for relative units.
     */
    float getPointSize(String size, StyleSheet ss) {
        int relSize, absSize, diff, index;
        ss = getStyleSheet(ss);
        if (size != null) {
            if (size.startsWith("+")) {
                relSize = Integer.valueOf(size.substring(1)).intValue();
                return getPointSize(baseFontSize + relSize, ss);
            } else if (size.startsWith("-")) {
                relSize = -Integer.valueOf(size.substring(1)).intValue();
                return getPointSize(baseFontSize + relSize, ss);
            } else {
                absSize = Integer.valueOf(size).intValue();
                return getPointSize(absSize, ss);
            }
        }
        return 0;
    }

    /**
     * Returns the length of the attribute in <code>a</code> with
     * key <code>key</code>.
     */
    float getLength(AttributeSet a, CSS.Attribute key, StyleSheet ss) {
        ss = getStyleSheet(ss);
        LengthValue lv = (LengthValue) a.getAttribute(key);
        boolean isW3CLengthUnits = (ss == null) ? false : ss.isW3CLengthUnits();
        float len = (lv != null) ? lv.getValue(isW3CLengthUnits) : 0;
        return len;
    }

    /**
     * Convert a set of HTML attributes to an equivalent
     * set of CSS attributes.
     *
     * @param AttributeSet containing the HTML attributes.
     * @return AttributeSet containing the corresponding CSS attributes.
     *        The AttributeSet will be empty if there are no mapping
     *        CSS attributes.
     */
    AttributeSet translateHTMLToCSS(AttributeSet htmlAttrSet) {
        MutableAttributeSet cssAttrSet = new SimpleAttributeSet();
        Element elem = (Element)htmlAttrSet;
        HTML.Tag tag = getHTMLTag(htmlAttrSet);
        if ((tag == HTML.Tag.TD) || (tag == HTML.Tag.TH)) {
            // translate border width into the cells
            AttributeSet tableAttr = elem.getParentElement().
                                     getParentElement().getAttributes();
            translateAttribute(HTML.Attribute.BORDER, tableAttr, cssAttrSet);
            String pad = (String)tableAttr.getAttribute(HTML.Attribute.CELLPADDING);
            if (pad != null) {
                LengthValue v =
                    (LengthValue)getInternalCSSValue(CSS.Attribute.PADDING_TOP, pad);
                v.span = (v.span < 0) ? 0 : v.span;
                cssAttrSet.addAttribute(CSS.Attribute.PADDING_TOP, v);
                cssAttrSet.addAttribute(CSS.Attribute.PADDING_BOTTOM, v);
                cssAttrSet.addAttribute(CSS.Attribute.PADDING_LEFT, v);
                cssAttrSet.addAttribute(CSS.Attribute.PADDING_RIGHT, v);
            }
        }
        if (elem.isLeaf()) {
            translateEmbeddedAttributes(htmlAttrSet, cssAttrSet);
        } else {
            translateAttributes(tag, htmlAttrSet, cssAttrSet);
        }
        if (tag == HTML.Tag.CAPTION) {
            /*
             * Navigator uses ALIGN for caption placement and IE uses VALIGN.
             */
            Object v = htmlAttrSet.getAttribute(HTML.Attribute.ALIGN);
            if ((v != null) && (v.equals("top") || v.equals("bottom"))) {
                cssAttrSet.addAttribute(CSS.Attribute.CAPTION_SIDE, v);
                cssAttrSet.removeAttribute(CSS.Attribute.TEXT_ALIGN);
            } else {
                v = htmlAttrSet.getAttribute(HTML.Attribute.VALIGN);
                if (v != null) {
                    cssAttrSet.addAttribute(CSS.Attribute.CAPTION_SIDE, v);
                }
            }
        }
        return cssAttrSet;
    }

    private static final Hashtable attributeMap = new Hashtable();
    private static final Hashtable valueMap = new Hashtable();

    /**
     * The hashtable and the static initalization block below,
     * set up a mapping from well-known HTML attributes to
     * CSS attributes.  For the most part, there is a 1-1 mapping
     * between the two.  However in the case of certain HTML
     * attributes for example HTML.Attribute.VSPACE or
     * HTML.Attribute.HSPACE, end up mapping to two CSS.Attribute's.
     * Therefore, the value associated with each HTML.Attribute.
     * key ends up being an array of CSS.Attribute.* objects.
     */
    private static final Hashtable htmlAttrToCssAttrMap = new Hashtable(20);

    /**
     * The hashtable and static initialization that follows sets
     * up a translation from StyleConstants (i.e. the <em>well known</em>
     * attributes) to the associated CSS attributes.
     */
    private static final Hashtable styleConstantToCssMap = new Hashtable(17);
    /** Maps from HTML value to a CSS value. Used in internal mapping. */
    private static final Hashtable htmlValueToCssValueMap = new Hashtable(8);
    /** Maps from CSS value (string) to internal value. */
    private static final Hashtable cssValueToInternalValueMap = new Hashtable(13);

    static {
        // load the attribute map
        for (int i = 0; i < Attribute.allAttributes.length; i++ ) {
            attributeMap.put(Attribute.allAttributes[i].toString(),
                             Attribute.allAttributes[i]);
        }
        // load the value map
        for (int i = 0; i < Value.allValues.length; i++ ) {
            valueMap.put(Value.allValues[i].toString(),
                             Value.allValues[i]);
        }

        htmlAttrToCssAttrMap.put(HTML.Attribute.COLOR,
                                 new CSS.Attribute[]{CSS.Attribute.COLOR});
        htmlAttrToCssAttrMap.put(HTML.Attribute.TEXT,
                                 new CSS.Attribute[]{CSS.Attribute.COLOR});
        htmlAttrToCssAttrMap.put(HTML.Attribute.CLEAR,
                                 new CSS.Attribute[]{CSS.Attribute.CLEAR});
        htmlAttrToCssAttrMap.put(HTML.Attribute.BACKGROUND,
                                 new CSS.Attribute[]{CSS.Attribute.BACKGROUND_IMAGE});
        htmlAttrToCssAttrMap.put(HTML.Attribute.BGCOLOR,
                                 new CSS.Attribute[]{CSS.Attribute.BACKGROUND_COLOR});
        htmlAttrToCssAttrMap.put(HTML.Attribute.WIDTH,
                                 new CSS.Attribute[]{CSS.Attribute.WIDTH});
        htmlAttrToCssAttrMap.put(HTML.Attribute.HEIGHT,
                                 new CSS.Attribute[]{CSS.Attribute.HEIGHT});
        htmlAttrToCssAttrMap.put(HTML.Attribute.BORDER,
                                 new CSS.Attribute[]{CSS.Attribute.BORDER_TOP_WIDTH, CSS.Attribute.BORDER_RIGHT_WIDTH, CSS.Attribute.BORDER_BOTTOM_WIDTH, CSS.Attribute.BORDER_LEFT_WIDTH});
        htmlAttrToCssAttrMap.put(HTML.Attribute.CELLPADDING,
                                 new CSS.Attribute[]{CSS.Attribute.PADDING});
        htmlAttrToCssAttrMap.put(HTML.Attribute.CELLSPACING,
                                 new CSS.Attribute[]{CSS.Attribute.BORDER_SPACING});
        htmlAttrToCssAttrMap.put(HTML.Attribute.MARGINWIDTH,
                                 new CSS.Attribute[]{CSS.Attribute.MARGIN_LEFT,
                                                     CSS.Attribute.MARGIN_RIGHT});
        htmlAttrToCssAttrMap.put(HTML.Attribute.MARGINHEIGHT,
                                 new CSS.Attribute[]{CSS.Attribute.MARGIN_TOP,
                                                     CSS.Attribute.MARGIN_BOTTOM});
        htmlAttrToCssAttrMap.put(HTML.Attribute.HSPACE,
                                 new CSS.Attribute[]{CSS.Attribute.PADDING_LEFT,
                                                     CSS.Attribute.PADDING_RIGHT});
        htmlAttrToCssAttrMap.put(HTML.Attribute.VSPACE,
                                 new CSS.Attribute[]{CSS.Attribute.PADDING_BOTTOM,
                                                     CSS.Attribute.PADDING_TOP});
        htmlAttrToCssAttrMap.put(HTML.Attribute.FACE,
                                 new CSS.Attribute[]{CSS.Attribute.FONT_FAMILY});
        htmlAttrToCssAttrMap.put(HTML.Attribute.SIZE,
                                 new CSS.Attribute[]{CSS.Attribute.FONT_SIZE});
        htmlAttrToCssAttrMap.put(HTML.Attribute.VALIGN,
                                 new CSS.Attribute[]{CSS.Attribute.VERTICAL_ALIGN});
        htmlAttrToCssAttrMap.put(HTML.Attribute.ALIGN,
                                 new CSS.Attribute[]{CSS.Attribute.VERTICAL_ALIGN,
                                                     CSS.Attribute.TEXT_ALIGN,
                                                     CSS.Attribute.FLOAT});
        htmlAttrToCssAttrMap.put(HTML.Attribute.TYPE,
                                 new CSS.Attribute[]{CSS.Attribute.LIST_STYLE_TYPE});
        htmlAttrToCssAttrMap.put(HTML.Attribute.NOWRAP,
                                 new CSS.Attribute[]{CSS.Attribute.WHITE_SPACE});

        // initialize StyleConstants mapping
        styleConstantToCssMap.put(StyleConstants.FontFamily,
                                  CSS.Attribute.FONT_FAMILY);
        styleConstantToCssMap.put(StyleConstants.FontSize,
                                  CSS.Attribute.FONT_SIZE);
        styleConstantToCssMap.put(StyleConstants.Bold,
                                  CSS.Attribute.FONT_WEIGHT);
        styleConstantToCssMap.put(StyleConstants.Italic,
                                  CSS.Attribute.FONT_STYLE);
        styleConstantToCssMap.put(StyleConstants.Underline,
                                  CSS.Attribute.TEXT_DECORATION);
        styleConstantToCssMap.put(StyleConstants.StrikeThrough,
                                  CSS.Attribute.TEXT_DECORATION);
        styleConstantToCssMap.put(StyleConstants.Superscript,
                                  CSS.Attribute.VERTICAL_ALIGN);
        styleConstantToCssMap.put(StyleConstants.Subscript,
                                  CSS.Attribute.VERTICAL_ALIGN);
        styleConstantToCssMap.put(StyleConstants.Foreground,
                                  CSS.Attribute.COLOR);
        styleConstantToCssMap.put(StyleConstants.Background,
                                  CSS.Attribute.BACKGROUND_COLOR);
        styleConstantToCssMap.put(StyleConstants.FirstLineIndent,
                                  CSS.Attribute.TEXT_INDENT);
        styleConstantToCssMap.put(StyleConstants.LeftIndent,
                                  CSS.Attribute.MARGIN_LEFT);
        styleConstantToCssMap.put(StyleConstants.RightIndent,
                                  CSS.Attribute.MARGIN_RIGHT);
        styleConstantToCssMap.put(StyleConstants.SpaceAbove,
                                  CSS.Attribute.MARGIN_TOP);
        styleConstantToCssMap.put(StyleConstants.SpaceBelow,
                                  CSS.Attribute.MARGIN_BOTTOM);
        styleConstantToCssMap.put(StyleConstants.Alignment,
                                  CSS.Attribute.TEXT_ALIGN);

        // HTML->CSS
        htmlValueToCssValueMap.put("disc", CSS.Value.DISC);
        htmlValueToCssValueMap.put("square", CSS.Value.SQUARE);
        htmlValueToCssValueMap.put("circle", CSS.Value.CIRCLE);
        htmlValueToCssValueMap.put("1", CSS.Value.DECIMAL);
        htmlValueToCssValueMap.put("a", CSS.Value.LOWER_ALPHA);
        htmlValueToCssValueMap.put("A", CSS.Value.UPPER_ALPHA);
        htmlValueToCssValueMap.put("i", CSS.Value.LOWER_ROMAN);
        htmlValueToCssValueMap.put("I", CSS.Value.UPPER_ROMAN);

        // CSS-> internal CSS
        cssValueToInternalValueMap.put("none", CSS.Value.NONE);
        cssValueToInternalValueMap.put("disc", CSS.Value.DISC);
        cssValueToInternalValueMap.put("square", CSS.Value.SQUARE);
        cssValueToInternalValueMap.put("circle", CSS.Value.CIRCLE);
        cssValueToInternalValueMap.put("decimal", CSS.Value.DECIMAL);
        cssValueToInternalValueMap.put("lower-roman", CSS.Value.LOWER_ROMAN);
        cssValueToInternalValueMap.put("upper-roman", CSS.Value.UPPER_ROMAN);
        cssValueToInternalValueMap.put("lower-alpha", CSS.Value.LOWER_ALPHA);
        cssValueToInternalValueMap.put("upper-alpha", CSS.Value.UPPER_ALPHA);
        cssValueToInternalValueMap.put("repeat", CSS.Value.BACKGROUND_REPEAT);
        cssValueToInternalValueMap.put("no-repeat",
                                       CSS.Value.BACKGROUND_NO_REPEAT);
        cssValueToInternalValueMap.put("repeat-x",
                                       CSS.Value.BACKGROUND_REPEAT_X);
        cssValueToInternalValueMap.put("repeat-y",
                                       CSS.Value.BACKGROUND_REPEAT_Y);
        cssValueToInternalValueMap.put("scroll",
                                       CSS.Value.BACKGROUND_SCROLL);
        cssValueToInternalValueMap.put("fixed",
                                       CSS.Value.BACKGROUND_FIXED);

        // Register all the CSS attribute keys for archival/unarchival
        Object[] keys = CSS.Attribute.allAttributes;
        try {
            for (int i = 0; i < keys.length; i++) {
                StyleContext.registerStaticAttributeKey(keys[i]);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        // Register all the CSS Values for archival/unarchival
        keys = CSS.Value.allValues;
        try {
            for (int i = 0; i < keys.length; i++) {
                StyleContext.registerStaticAttributeKey(keys[i]);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Return the set of all possible CSS attribute keys.
     */
    public static Attribute[] getAllAttributeKeys() {
        Attribute[] keys = new Attribute[Attribute.allAttributes.length];
        System.arraycopy(Attribute.allAttributes, 0, keys, 0, Attribute.allAttributes.length);
        return keys;
    }

    /**
     * Translates a string to a <code>CSS.Attribute</code> object.
     * This will return <code>null</code> if there is no attribute
     * by the given name.
     *
     * @param name the name of the CSS attribute to fetch the
     *  typesafe enumeration for
     * @return the <code>CSS.Attribute</code> object,
     *  or <code>null</code> if the string
     *  doesn't represent a valid attribute key
     */
    public static final Attribute getAttribute(String name) {
        return (Attribute) attributeMap.get(name);
    }

    /**
     * Translates a string to a <code>CSS.Value</code> object.
     * This will return <code>null</code> if there is no value
     * by the given name.
     *
     * @param name the name of the CSS value to fetch the
     *  typesafe enumeration for
     * @return the <code>CSS.Value</code> object,
     *  or <code>null</code> if the string
     *  doesn't represent a valid CSS value name; this does
     *  not mean that it doesn't represent a valid CSS value
     */
    static final Value getValue(String name) {
        return (Value) valueMap.get(name);
    }


    //
    // Conversion related methods/classes
    //

    /**
     * Returns a URL for the given CSS url string. If relative,
     * <code>base</code> is used as the parent. If a valid URL can not
     * be found, this will not throw a MalformedURLException, instead
     * null will be returned.
     */
    static URL getURL(URL base, String cssString) {
        if (cssString == null) {
            return null;
        }
        if (cssString.startsWith("url(") &&
            cssString.endsWith(")")) {
            cssString = cssString.substring(4, cssString.length() - 1);
        }
        // Absolute first
        try {
            URL url = new URL(cssString);
            if (url != null) {
                return url;
            }
        } catch (MalformedURLException mue) {
        }
        // Then relative
        if (base != null) {
            // Relative URL, try from base
            try {
                URL url = new URL(base, cssString);
                return url;
            }
            catch (MalformedURLException muee) {
            }
        }
        return null;
    }

    /**
     * Converts a type Color to a hex string
     * in the format "#RRGGBB"
     */
    static String colorToHex(Color color) {

      String colorstr = new String("#");

      // Red
      String str = Integer.toHexString(color.getRed());
      if (str.length() > 2)
        str = str.substring(0, 2);
      else if (str.length() < 2)
        colorstr += "0" + str;
      else
        colorstr += str;

      // Green
      str = Integer.toHexString(color.getGreen());
      if (str.length() > 2)
        str = str.substring(0, 2);
      else if (str.length() < 2)
        colorstr += "0" + str;
      else
        colorstr += str;

      // Blue
      str = Integer.toHexString(color.getBlue());
      if (str.length() > 2)
        str = str.substring(0, 2);
      else if (str.length() < 2)
        colorstr += "0" + str;
      else
        colorstr += str;

      return colorstr;
    }

     /**
      * Convert a "#FFFFFF" hex string to a Color.
      * If the color specification is bad, an attempt
      * will be made to fix it up.
      */
    static final Color hexToColor(String value) {
        String digits;
        int n = value.length();
        if (value.startsWith("#")) {
            digits = value.substring(1, Math.min(value.length(), 7));
        } else {
            digits = value;
        }
        String hstr = "0x" + digits;
        Color c;
        try {
            c = Color.decode(hstr);
        } catch (NumberFormatException nfe) {
            c = null;
        }
         return c;
     }

    /**
     * Convert a color string such as "RED" or "#NNNNNN" or "rgb(r, g, b)"
     * to a Color.
     */
    static Color stringToColor(String str) {
      Color color = null;

      if (str.length() == 0)
        color = Color.black;
      else if (str.startsWith("rgb(")) {
          color = parseRGB(str);
      }
      else if (str.charAt(0) == '#')
        color = hexToColor(str);
      else if (str.equalsIgnoreCase("Black"))
        color = hexToColor("#000000");
      else if(str.equalsIgnoreCase("Silver"))
        color = hexToColor("#C0C0C0");
      else if(str.equalsIgnoreCase("Gray"))
        color = hexToColor("#808080");
      else if(str.equalsIgnoreCase("White"))
        color = hexToColor("#FFFFFF");
      else if(str.equalsIgnoreCase("Maroon"))
        color = hexToColor("#800000");
      else if(str.equalsIgnoreCase("Red"))
        color = hexToColor("#FF0000");
      else if(str.equalsIgnoreCase("Purple"))
        color = hexToColor("#800080");
      else if(str.equalsIgnoreCase("Fuchsia"))
        color = hexToColor("#FF00FF");
      else if(str.equalsIgnoreCase("Green"))
        color = hexToColor("#008000");
      else if(str.equalsIgnoreCase("Lime"))
        color = hexToColor("#00FF00");
      else if(str.equalsIgnoreCase("Olive"))
        color = hexToColor("#808000");
      else if(str.equalsIgnoreCase("Yellow"))
        color = hexToColor("#FFFF00");
      else if(str.equalsIgnoreCase("Navy"))
        color = hexToColor("#000080");
      else if(str.equalsIgnoreCase("Blue"))
        color = hexToColor("#0000FF");
      else if(str.equalsIgnoreCase("Teal"))
        color = hexToColor("#008080");
      else if(str.equalsIgnoreCase("Aqua"))
        color = hexToColor("#00FFFF");
      else
          color = hexToColor(str); // sometimes get specified without leading #
      return color;
    }

    /**
     * Parses a String in the format <code>rgb(r, g, b)</code> where
     * each of the Color components is either an integer, or a floating number
     * with a % after indicating a percentage value of 255. Values are
     * constrained to fit with 0-255. The resulting Color is returned.
     */
    private static Color parseRGB(String string) {
        // Find the next numeric char
        int[] index = new int[1];

        index[0] = 4;
        int red = getColorComponent(string, index);
        int green = getColorComponent(string, index);
        int blue = getColorComponent(string, index);

        return new Color(red, green, blue);
    }

    /**
     * Returns the next integer value from <code>string</code> starting
     * at <code>index[0]</code>. The value can either can an integer, or
     * a percentage (floating number ending with %), in which case it is
     * multiplied by 255.
     */
    private static int getColorComponent(String string, int[] index) {
        int length = string.length();
        char aChar;

        // Skip non-decimal chars
        while(index[0] < length && (aChar = string.charAt(index[0])) != '-' &&
              !Character.isDigit(aChar) && aChar != '.') {
            index[0]++;
        }

        int start = index[0];

        if (start < length && string.charAt(index[0]) == '-') {
            index[0]++;
        }
        while(index[0] < length &&
                         Character.isDigit(string.charAt(index[0]))) {
            index[0]++;
        }
        if (index[0] < length && string.charAt(index[0]) == '.') {
            // Decimal value
            index[0]++;
            while(index[0] < length &&
                  Character.isDigit(string.charAt(index[0]))) {
                index[0]++;
            }
        }
        if (start != index[0]) {
            try {
                float value = Float.parseFloat(string.substring
                                               (start, index[0]));

                if (index[0] < length && string.charAt(index[0]) == '%') {
                    index[0]++;
                    value = value * 255f / 100f;
                }
                return Math.min(255, Math.max(0, (int)value));
            } catch (NumberFormatException nfe) {
                // Treat as 0
            }
        }
        return 0;
    }

    static int getIndexOfSize(float pt, int[] sizeMap) {
        for (int i = 0; i < sizeMap.length; i ++ )
                if (pt <= sizeMap[i])
                        return i + 1;
        return sizeMap.length;
    }

    static int getIndexOfSize(float pt, StyleSheet ss) {
        int[] sizeMap = (ss != null) ? ss.getSizeMap() :
            StyleSheet.sizeMapDefault;
        return getIndexOfSize(pt, sizeMap);
    }


    /**
     * @return an array of all the strings in <code>value</code>
     *         that are separated by whitespace.
     */
    static String[] parseStrings(String value) {
        int         current, last;
        int         length = (value == null) ? 0 : value.length();
        Vector      temp = new Vector(4);

        current = 0;
        while (current < length) {
            // Skip ws
            while (current < length && Character.isWhitespace
                   (value.charAt(current))) {
                current++;
            }
            last = current;
            while (current < length && !Character.isWhitespace
                   (value.charAt(current))) {
                current++;
            }
            if (last != current) {
                temp.addElement(value.substring(last, current));
            }
            current++;
        }
        String[] retValue = new String[temp.size()];
        temp.copyInto(retValue);
        return retValue;
    }

    /**
     * Return the point size, given a size index. Legal HTML index sizes
     * are 1-7.
     */
    float getPointSize(int index, StyleSheet ss) {
        ss = getStyleSheet(ss);
        int[] sizeMap = (ss != null) ? ss.getSizeMap() :
            StyleSheet.sizeMapDefault;
        --index;
        if (index < 0)
          return sizeMap[0];
        else if (index > sizeMap.length - 1)
          return sizeMap[sizeMap.length - 1];
        else
          return sizeMap[index];
    }


    private void translateEmbeddedAttributes(AttributeSet htmlAttrSet,
                                             MutableAttributeSet cssAttrSet) {
        Enumeration keys = htmlAttrSet.getAttributeNames();
        if (htmlAttrSet.getAttribute(StyleConstants.NameAttribute) ==
            HTML.Tag.HR) {
            // HR needs special handling due to us treating it as a leaf.
            translateAttributes(HTML.Tag.HR, htmlAttrSet, cssAttrSet);
        }
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof HTML.Tag) {
                HTML.Tag tag = (HTML.Tag)key;
                Object o = htmlAttrSet.getAttribute(tag);
                if (o != null && o instanceof AttributeSet) {
                    translateAttributes(tag, (AttributeSet)o, cssAttrSet);
                }
            } else if (key instanceof CSS.Attribute) {
                cssAttrSet.addAttribute(key, htmlAttrSet.getAttribute(key));
            }
        }
    }

    private void translateAttributes(HTML.Tag tag,
                                            AttributeSet htmlAttrSet,
                                            MutableAttributeSet cssAttrSet) {
        Enumeration names = htmlAttrSet.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();

            if (name instanceof HTML.Attribute) {
                HTML.Attribute key = (HTML.Attribute)name;

                /*
                 * HTML.Attribute.ALIGN needs special processing.
                 * It can map to to 1 of many(3) possible CSS attributes
                 * depending on the nature of the tag the attribute is
                 * part off and depending on the value of the attribute.
                 */
                if (key == HTML.Attribute.ALIGN) {
                    String htmlAttrValue = (String)htmlAttrSet.getAttribute(HTML.Attribute.ALIGN);
                    if (htmlAttrValue != null) {
                        CSS.Attribute cssAttr = getCssAlignAttribute(tag, htmlAttrSet);
                        if (cssAttr != null) {
                            Object o = getCssValue(cssAttr, htmlAttrValue);
                            if (o != null) {
                                cssAttrSet.addAttribute(cssAttr, o);
                            }
                        }
                    }
                } else {

                    /*
                     * The html size attribute has a mapping in the CSS world only
                     * if it is par of a font or base font tag.
                     */

                    if (key == HTML.Attribute.SIZE && !isHTMLFontTag(tag)) {
                        continue;
                    }

                    translateAttribute(key, htmlAttrSet, cssAttrSet);
                }
            } else if (name instanceof CSS.Attribute) {
                cssAttrSet.addAttribute(name, htmlAttrSet.getAttribute(name));
            }
        }
    }

    private void translateAttribute(HTML.Attribute key,
                                           AttributeSet htmlAttrSet,
                                           MutableAttributeSet cssAttrSet) {
        /*
         * In the case of all remaining HTML.Attribute's they
         * map to 1 or more CCS.Attribute.
         */
        CSS.Attribute[] cssAttrList = getCssAttribute(key);

        String htmlAttrValue = (String)htmlAttrSet.getAttribute(key);

        if (cssAttrList == null || htmlAttrValue == null) {
            return;
        }
        for (int i = 0; i < cssAttrList.length; i++) {
            Object o = getCssValue(cssAttrList[i], htmlAttrValue);
            if (o != null) {
                cssAttrSet.addAttribute(cssAttrList[i], o);
            }
        }
    }

    /**
     * Given a CSS.Attribute object and its corresponding HTML.Attribute's
     * value, this method returns a CssValue object to associate with the
     * CSS attribute.
     *
     * @param the CSS.Attribute
     * @param a String containing the value associated HTML.Attribtue.
     */
    Object getCssValue(CSS.Attribute cssAttr, String htmlAttrValue) {
        CssValue value = (CssValue)valueConvertor.get(cssAttr);
        Object o = value.parseHtmlValue(htmlAttrValue);
        return o;
    }

    /**
     * Maps an HTML.Attribute object to its appropriate CSS.Attributes.
     *
     * @param HTML.Attribute
     * @return CSS.Attribute[]
     */
    private CSS.Attribute[] getCssAttribute(HTML.Attribute hAttr) {
        return (CSS.Attribute[])htmlAttrToCssAttrMap.get(hAttr);
    }

    /**
     * Maps HTML.Attribute.ALIGN to either:
     *     CSS.Attribute.TEXT_ALIGN
     *     CSS.Attribute.FLOAT
     *     CSS.Attribute.VERTICAL_ALIGN
     * based on the tag associated with the attribute and the
     * value of the attribute.
     *
     * @param AttributeSet containing HTML attributes.
     * @return CSS.Attribute mapping for HTML.Attribute.ALIGN.
     */
    private CSS.Attribute getCssAlignAttribute(HTML.Tag tag,
                                                   AttributeSet htmlAttrSet) {
        return CSS.Attribute.TEXT_ALIGN;
/*
        String htmlAttrValue = (String)htmlAttrSet.getAttribute(HTML.Attribute.ALIGN);
        CSS.Attribute cssAttr = CSS.Attribute.TEXT_ALIGN;
        if (htmlAttrValue != null && htmlAttrSet instanceof Element) {
            Element elem = (Element)htmlAttrSet;
            if (!elem.isLeaf() && tag.isBlock() && validTextAlignValue(htmlAttrValue)) {
                return CSS.Attribute.TEXT_ALIGN;
            } else if (isFloater(htmlAttrValue)) {
                return CSS.Attribute.FLOAT;
            } else if (elem.isLeaf()) {
                return CSS.Attribute.VERTICAL_ALIGN;
            }
        }
        return null;
        */
    }

    /**
     * Fetches the tag associated with the HTML AttributeSet.
     *
     * @param  AttributeSet containing the HTML attributes.
     * @return HTML.Tag
     */
    private HTML.Tag getHTMLTag(AttributeSet htmlAttrSet) {
        Object o = htmlAttrSet.getAttribute(StyleConstants.NameAttribute);
        if (o instanceof HTML.Tag) {
            HTML.Tag tag = (HTML.Tag) o;
            return tag;
        }
        return null;
    }


    private boolean isHTMLFontTag(HTML.Tag tag) {
        return (tag != null && ((tag == HTML.Tag.FONT) || (tag == HTML.Tag.BASEFONT)));
    }


    private boolean isFloater(String alignValue) {
        return (alignValue.equals("left") || alignValue.equals("right"));
    }

    private boolean validTextAlignValue(String alignValue) {
        return (isFloater(alignValue) || alignValue.equals("center"));
    }

    /**
     * Base class to CSS values in the attribute sets.  This
     * is intended to act as a convertor to/from other attribute
     * formats.
     * <p>
     * The CSS parser uses the parseCssValue method to convert
     * a string to whatever format is appropriate a given key
     * (i.e. these convertors are stored in a map using the
     * CSS.Attribute as a key and the CssValue as the value).
     * <p>
     * The HTML to CSS conversion process first converts the
     * HTML.Attribute to a CSS.Attribute, and then calls
     * the parseHtmlValue method on the value of the HTML
     * attribute to produce the corresponding CSS value.
     * <p>
     * The StyleConstants to CSS conversion process first
     * converts the StyleConstants attribute to a
     * CSS.Attribute, and then calls the fromStyleConstants
     * method to convert the StyleConstants value to a
     * CSS value.
     * <p>
     * The CSS to StyleConstants conversion process first
     * converts the StyleConstants attribute to a
     * CSS.Attribute, and then calls the toStyleConstants
     * method to convert the CSS value to a StyleConstants
     * value.
     */
    static class CssValue implements Serializable {

        /**
         * Convert a CSS value string to the internal format
         * (for fast processing) used in the attribute sets.
         * The fallback storage for any value that we don't
         * have a special binary format for is a String.
         */
        Object parseCssValue(String value) {
            return value;
        }

        /**
         * Convert an HTML attribute value to a CSS attribute
         * value.  If there is no conversion, return null.
         * This is implemented to simply forward to the CSS
         * parsing by default (since some of the attribute
         * values are the same).  If the attribute value
         * isn't recognized as a CSS value it is generally
         * returned as null.
         */
        Object parseHtmlValue(String value) {
            return parseCssValue(value);
        }

        /**
         * Converts a <code>StyleConstants</code> attribute value to
         * a CSS attribute value.  If there is no conversion,
         * returns <code>null</code>.  By default, there is no conversion.
         *
         * @param key the <code>StyleConstants</code> attribute
         * @param value the value of a <code>StyleConstants</code>
         *   attribute to be converted
         * @return the CSS value that represents the
         *   <code>StyleConstants</code> value
         */
        Object fromStyleConstants(StyleConstants key, Object value) {
            return null;
        }

        /**
         * Converts a CSS attribute value to a
         * <code>StyleConstants</code>
         * value.  If there is no conversion, returns
         * <code>null</code>.
         * By default, there is no conversion.
         *
         * @param key the <code>StyleConstants</code> attribute
         * @param v the view containing <code>AttributeSet</code>
         * @return the <code>StyleConstants</code> attribute value that
         *   represents the CSS attribute value
         */
        Object toStyleConstants(StyleConstants key, View v) {
            return null;
        }

        /**
         * Return the CSS format of the value
         */
        public String toString() {
            return svalue;
        }

        /**
         * The value as a string... before conversion to a
         * binary format.
         */
        String svalue;
    }

    /**
     * By default CSS attributes are represented as simple
     * strings.  They also have no conversion to/from
     * StyleConstants by default. This class represents the
     * value as a string (via the superclass), but
     * provides StyleConstants conversion support for the
     * CSS attributes that are held as strings.
     */
    static class StringValue extends CssValue {

        /**
         * Convert a CSS value string to the internal format
         * (for fast processing) used in the attribute sets.
         * This produces a StringValue, so that it can be
         * used to convert from CSS to StyleConstants values.
         */
        Object parseCssValue(String value) {
            StringValue sv = new StringValue();
            sv.svalue = value;
            return sv;
        }

        /**
         * Converts a <code>StyleConstants</code> attribute value to
         * a CSS attribute value.  If there is no conversion
         * returns <code>null</code>.
         *
         * @param key the <code>StyleConstants</code> attribute
         * @param value the value of a <code>StyleConstants</code>
         *   attribute to be converted
         * @return the CSS value that represents the
         *   <code>StyleConstants</code> value
         */
        Object fromStyleConstants(StyleConstants key, Object value) {
            if (key == StyleConstants.Italic) {
                if (value.equals(Boolean.TRUE)) {
                    return parseCssValue("italic");
                }
                return parseCssValue("");
            } else if (key == StyleConstants.Underline) {
                if (value.equals(Boolean.TRUE)) {
                    return parseCssValue("underline");
                }
                return parseCssValue("");
            } else if (key == StyleConstants.Alignment) {
                int align = ((Integer)value).intValue();
                String ta;
                switch(align) {
                case StyleConstants.ALIGN_LEFT:
                    ta = "left";
                    break;
                case StyleConstants.ALIGN_RIGHT:
                    ta = "right";
                    break;
                case StyleConstants.ALIGN_CENTER:
                    ta = "center";
                    break;
                case StyleConstants.ALIGN_JUSTIFIED:
                    ta = "justify";
                    break;
                default:
                    ta = "left";
                }
                return parseCssValue(ta);
            } else if (key == StyleConstants.StrikeThrough) {
                if (value.equals(Boolean.TRUE)) {
                    return parseCssValue("line-through");
                }
                return parseCssValue("");
            } else if (key == StyleConstants.Superscript) {
                if (value.equals(Boolean.TRUE)) {
                    return parseCssValue("super");
                }
                return parseCssValue("");
            } else if (key == StyleConstants.Subscript) {
                if (value.equals(Boolean.TRUE)) {
                    return parseCssValue("sub");
                }
                return parseCssValue("");
            }
            return null;
        }

        /**
         * Converts a CSS attribute value to a
         * <code>StyleConstants</code> value.
         * If there is no conversion, returns <code>null</code>.
         * By default, there is no conversion.
         *
         * @param key the <code>StyleConstants</code> attribute
         * @return the <code>StyleConstants</code> attribute value that
         *   represents the CSS attribute value
         */
        Object toStyleConstants(StyleConstants key, View v) {
            if (key == StyleConstants.Italic) {
                if (svalue.indexOf("italic") >= 0) {
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            } else if (key == StyleConstants.Underline) {
                if (svalue.indexOf("underline") >= 0) {
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            } else if (key == StyleConstants.Alignment) {
                if (svalue.equals("right")) {
                    return new Integer(StyleConstants.ALIGN_RIGHT);
                } else if (svalue.equals("center")) {
                    return new Integer(StyleConstants.ALIGN_CENTER);
                } else if  (svalue.equals("justify")) {
                    return new Integer(StyleConstants.ALIGN_JUSTIFIED);
                }
                return new Integer(StyleConstants.ALIGN_LEFT);
            } else if (key == StyleConstants.StrikeThrough) {
                if (svalue.indexOf("line-through") >= 0) {
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            } else if (key == St
