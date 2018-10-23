// https://searchcode.com/api/result/73230017/

/*******************************************************************************
 * Copyright (c) 2009, 2011 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 *     EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.swt.custom;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.custom.IStyledTextAdapter;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * A StyledText is an editable user interface object that displays lines of
 * text. The following style attributes can be defined for the text:
 * <ul>
 * <li>foreground color
 * <li>background color
 * <li>font style (bold, italic, bold-italic, regular)
 * <li>underline
 * <li>strikeout
 * </ul>
 * </p>
 * <p>
 * <dl>
 * <dt><b>Events:</b>
 * <dd>Selection
 * </dl>
 * </p>
 * <p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 */
@SuppressWarnings("all")
public class StyledText extends Canvas {

  private final class StyledTextAdapter implements IStyledTextAdapter {
    public String getHtml() {
      return StyledText.this.html;
    }
  }

  private String content;
  private String html;
  private String charStyle;
  private Point selection;
  private int caretOffset;
  private int selectionAnchor;
  private boolean editable;
  private StyledTextRenderer renderer;
  private transient IStyledTextAdapter styledTextAdapter;

  /**
   * Constructs a new instance of this class given its parent and a style value
   * describing its behavior and appearance.
   * <p>
   * The style value is either one of the style constants defined in class
   * <code>SWT</code> which is applicable to instances of this class, or must be
   * built by <em>bitwise OR</em>'ing together (that is, using the
   * <code>int</code> "|" operator) two or more of those <code>SWT</code> style
   * constants. The class description lists the style constants that are
   * applicable to the class. Style bits are also inherited from superclasses.
   * </p>
   *
   * @param parent a widget which will be the parent of the new instance (cannot
   *          be null)
   * @param style the style of widget to construct
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the parent</li>
   *              </ul>
   */
  public StyledText( final Composite parent, final int style ) {
    super( parent, checkStyle( style ) );
    editable = false;
    content = "";
    html = "";
    charStyle = "";
    selection = new Point( 0, 0 );
    renderer = new StyledTextRenderer( this );
  }

  /**
   * Sets whether the widget content can be edited.
   * </p>
   *
   * @param editable if true content can be edited, if false content can not be
   *  edited
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   */
  public void setEditable( final boolean editable ) {
      checkWidget();
      // RAP
      // The RAP implementation is a non editable
      // This method is for single-source only.
      if( editable ) {
        SWT.error( SWT.ERROR_INVALID_ARGUMENT );
      }
      this.editable = editable;
  }

  /**
   * Returns whether the widget content can be edited.
   *
   * @return true if content can be edited, false otherwise
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   */
  public boolean getEditable() {
      checkWidget();
      return editable;
  }

  /**
   * Sets the widget content. If the widget has the SWT.SINGLE style and "text"
   * contains more than one line, only the first line is rendered but the text
   * is stored unchanged. A subsequent call to getText will return the same text
   * that was set.
   * <p>
   * <b>Note:</b> Only a single line of text should be set when the SWT.SINGLE
   * style is used.
   * </p>
   *
   * @param text new widget content. Replaces existing content. Line styles that
   *          were set using StyledText API are discarded. The current selection
   *          is also discarded.
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT when string is null</li>
   *              </ul>
   */
  public void setText( final String text ) {
    checkWidget();
    if( text == null ) {
      SWT.error( SWT.ERROR_NULL_ARGUMENT );
    }
    this.content = text;
    setStyleRange( null );
    selection = new Point( 0, 0 );
    caretOffset = 0;
    this.html = generateHtml();
  }

  /**
   * Returns a copy of the widget content.
   *
   * @return copy of the widget content
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public String getText() {
    checkWidget();
    return content;
  }

  /**
   * Sets the selection to the given position and scrolls it into view.
   * Equivalent to setSelection(start,start).
   *
   * @param start new caret position
   * @see #setSelection(int,int)
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_INVALID_ARGUMENT when either the start or the end of
   *              the selection range is inside a multi byte line delimiter (and
   *              thus neither clearly in front of or after the line delimiter)
   *              </ul>
   */
  public void setSelection( final int start ) {
    // checkWidget test done in setSelectionRange
    setSelection( start, start );
  }

  /**
   * Sets the selection and scrolls it into view.
   * <p>
   * Indexing is zero based. Text selections are specified in terms of caret
   * positions. In a text widget that contains N characters, there are N+1 caret
   * positions, ranging from 0..N
   * </p>
   *
   * @param point x=selection start offset, y=selection end offset The caret
   *          will be placed at the selection start when x > y.
   * @see #setSelection(int,int)
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT when point is null</li>
   *              <li>ERROR_INVALID_ARGUMENT when either the start or the end of
   *              the selection range is inside a multi byte line delimiter (and
   *              thus neither clearly in front of or after the line delimiter)
   *              </ul>
   */
  public void setSelection( final Point point ) {
    checkWidget();
    if( point == null ) {
      SWT.error( SWT.ERROR_NULL_ARGUMENT );
    }
    setSelection( point.x, point.y );
  }

  /**
   * Selects all the text.
   *
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   */
  public void selectAll() {
    checkWidget();
    setSelection( 0, Math.max( getCharCount(), 0 ) );
  }

  /**
   * Sets the selection and scrolls it into view.
   * <p>
   * Indexing is zero based. Text selections are specified in terms of caret
   * positions. In a text widget that contains N characters, there are N+1 caret
   * positions, ranging from 0..N
   * </p>
   *
   * @param start selection start offset. The caret will be placed at the
   *          selection start when start > end.
   * @param end selection end offset
   * @see #setSelectionRange(int,int)
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_INVALID_ARGUMENT when either the start or the end of
   *              the selection range is inside a multi byte line delimiter (and
   *              thus neither clearly in front of or after the line delimiter)
   *              </ul>
   */
  public void setSelection( final int start, final int end ) {
    setSelectionRange( start, end - start );
  }

  /**
   * Sets the selection.
   * <p>
   * The new selection may not be visible. Call showSelection to scroll the
   * selection into view.
   * </p>
   *
   * @param start offset of the first selected character, start >= 0 must be
   *          true.
   * @param length number of characters to select, 0 <= start + length <=
   *          getCharCount() must be true. A negative length places the caret at
   *          the selection start.
   * @param sendEvent a Selection event is sent when set to true and when the
   *          selection is reset.
   */
  void setSelection( final int selStart,
                     final int selLength,
                     final boolean sendEvent )
  {
    int start = selStart;
    int length = selLength;
    int end = start + length;
    if( start > end ) {
      int temp = end;
      end = start;
      start = temp;
    }
    // is the selection range different or is the selection direction
    // different?
    if( selection.x != start
        || selection.y != end
        || ( length > 0 && selectionAnchor != selection.x )
        || ( length < 0 && selectionAnchor != selection.y ) )
    {
      clearSelection( sendEvent );
      if( length < 0 ) {
        selectionAnchor = selection.y = end;
        caretOffset = selection.x = start;
      } else {
        selectionAnchor = selection.x = start;
        caretOffset = selection.y = end;
      }
    }
    this.html = generateHtml();
  }

  /**
   * Returns the selection.
   * <p>
   * Text selections are specified in terms of caret positions. In a text widget
   * that contains N characters, there are N+1 caret positions, ranging from
   * 0..N
   * </p>
   *
   * @return start and end of the selection, x is the offset of the first
   *         selected character, y is the offset after the last selected
   *         character. The selection values returned are visual (i.e., x will
   *         always always be <= y). To determine if a selection is
   *         right-to-left (RtoL) vs. left-to-right (LtoR), compare the
   *         caretOffset to the start and end of the selection (e.g.,
   *         caretOffset == start of selection implies that the selection is
   *         RtoL).
   * @see #getSelectionRange
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public Point getSelection() {
    checkWidget();
    return new Point( selection.x, selection.y );
  }

  /**
   * Returns the selected text.
   *
   * @return selected text, or an empty String if there is no selection.
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   */
  public String getSelectionText() {
      checkWidget();
      return content.substring(selection.x, selection.y);
  }

  /**
   * Sets the selection.
   * <p>
   * The new selection may not be visible. Call showSelection to scroll the
   * selection into view. A negative length places the caret at the visual start
   * of the selection.
   * </p>
   *
   * @param start offset of the first selected character
   * @param length number of characters to select
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_INVALID_ARGUMENT when either the start or the end of
   *              the selection range is inside a multi byte line delimiter (and
   *              thus neither clearly in front of or after the line delimiter)
   *              </ul>
   */
  public void setSelectionRange( final int start, final int length ) {
    checkWidget();
    int contentLength = getCharCount();
    int newStart = Math.max( 0, Math.min( start, contentLength ) );
    int newLength = length;
    int end = newStart + newLength;
    if( end < 0 ) {
      newLength = -newStart;
    } else {
      if( end > contentLength ) {
        newLength = contentLength - newStart;
      }
    }
    setSelection( newStart, newLength, false );
  }

  /**
   * Returns the selection.
   *
   * @return start and length of the selection, x is the offset of the
   *  first selected character, relative to the first character of the
   *  widget content. y is the length of the selection.
   *  The selection values returned are visual (i.e., length will always always be
   *  positive).  To determine if a selection is right-to-left (RtoL) vs. left-to-right
   *  (LtoR), compare the caretOffset to the start and end of the selection
   *  (e.g., caretOffset == start of selection implies that the selection is RtoL).
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   */
  public Point getSelectionRange() {
    checkWidget();
    return new Point( selection.x, selection.y - selection.x );
  }

  /**
   * Adds the specified style.
   * <p>
   * The new style overwrites existing styles for the specified range. Existing
   * style ranges are adjusted if they partially overlap with the new style. To
   * clear an individual style, call setStyleRange with a StyleRange that has
   * null attributes.
   * </p>
   * <p>
   * Should not be called if a LineStyleListener has been set since the listener
   * maintains the styles.
   * </p>
   *
   * @param range StyleRange object containing the style information. Overwrites
   *          the old style in the given range. May be null to delete all
   *          styles.
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_INVALID_RANGE when the style range is outside the
   *              valid range (> getCharCount())</li>
   *              </ul>
   */
  public void setStyleRange( final StyleRange range ) {
    checkWidget();
    if( range != null ) {
      if( range.isUnstyled() ) {
        setStyleRanges( range.start, range.length, null, null, false );
      } else {
        setStyleRanges( range.start, 0, null, new StyleRange[]{
          range
        }, false );
      }
    } else {
      setStyleRanges( 0, 0, null, null, true );
    }
  }

  /**
   * Sets styles to be used for rendering the widget content. All styles in the
   * widget will be replaced with the given set of styles.
   * <p>
   * Note: Because a StyleRange includes the start and length, the same instance
   * cannot occur multiple times in the array of styles. If the same style
   * attributes, such as font and color, occur in multiple StyleRanges,
   * <code>setStyleRanges(int[], StyleRange[])</code> can be used to share
   * styles and reduce memory usage.
   * </p>
   * <p>
   * Should not be called if a LineStyleListener has been set since the listener
   * maintains the styles.
   * </p>
   *
   * @param ranges StyleRange objects containing the style information. The
   *          ranges should not overlap. The style rendering is undefined if the
   *          ranges do overlap. Must not be null. The styles need to be in
   *          order.
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT when the list of ranges is null</li>
   *              <li>ERROR_INVALID_RANGE when the last of the style ranges is
   *              outside the valid range (> getCharCount())</li>
   *              </ul>
   * @see #setStyleRanges(int[], StyleRange[])
   */
  public void setStyleRanges( final StyleRange[] ranges ) {
    checkWidget();
    if( ranges == null ) {
      SWT.error( SWT.ERROR_NULL_ARGUMENT );
    }
    setStyleRanges( 0, 0, null, ranges, true );
  }

  /**
   * Returns the styles.
   *
   * @return the styles or an empty array.
   *
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   *
   * @see #getStyleRanges(boolean)
   */
  public StyleRange[] getStyleRanges() {
    checkWidget();
    return getStyleRanges( 0, content.length(), true );
  }

  /**
   * Returns the styles for the given text range.
   *
   * @param start the start offset of the style ranges to return
   * @param length the number of style ranges to return
   * @param includeRanges whether the start and length field of the StyleRanges should be set.
   *
   * @return the styles or an empty array
   *
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   * @exception IllegalArgumentException <ul>
   *   <li>ERROR_INVALID_RANGE when start and/or end are outside the widget content</li>
   * </ul>
   *
   */
  public StyleRange[] getStyleRanges( final int start,
                                      final int length,
                                      final boolean includeRanges )
  {
    checkWidget();
    int contentLength = getCharCount();
    int end = start + length;
    if( start > end || start < 0 || end > contentLength ) {
      SWT.error( SWT.ERROR_INVALID_RANGE );
    }

    StyleRange[] ranges
      = renderer.getStyleRanges( start, length, includeRanges );
    if( ranges == null ) {
      ranges = new StyleRange[0];
    }

    return ranges;
  }

  void setStyleRanges( final int start,
                       final int length,
                       final int[] ranges,
                       final StyleRange[] styles,
                       final boolean reset )
  {
    int charCount = content.length();
    int end = start + length;
    if( start > end || start < 0 ) {
      SWT.error( SWT.ERROR_INVALID_RANGE );
    }
    if( styles != null ) {
      if( end > charCount ) {
        SWT.error( SWT.ERROR_INVALID_RANGE );
      }
      if( ranges != null ) {
        if( ranges.length != styles.length << 1 ) {
          SWT.error( SWT.ERROR_INVALID_ARGUMENT );
        }
      }
      int lastOffset = 0;
      for( int i = 0; i < styles.length; i++ ) {
        if( styles[ i ] == null ) {
          SWT.error( SWT.ERROR_INVALID_ARGUMENT );
        }
        int rangeStart, rangeLength;
        if( ranges != null ) {
          rangeStart = ranges[ i << 1 ];
          rangeLength = ranges[ ( i << 1 ) + 1 ];
        } else {
          rangeStart = styles[ i ].start;
          rangeLength = styles[ i ].length;
        }
        if( rangeLength < 0 ) {
          SWT.error( SWT.ERROR_INVALID_ARGUMENT );
        }
        if( !( 0 <= rangeStart && rangeStart + rangeLength <= charCount ) ) {
          SWT.error( SWT.ERROR_INVALID_ARGUMENT );
        }
        if( lastOffset > rangeStart ) {
          SWT.error( SWT.ERROR_INVALID_ARGUMENT );
        }
        lastOffset = rangeStart + rangeLength;
      }
    }
    if( reset ) {
      renderer.setStyleRanges( null, null );
    } else {
      renderer.updateRanges( start, length, length );
    }
    if( styles != null && styles.length > 0 ) {
      renderer.setStyleRanges( ranges, styles );
    }
    this.html = generateHtml();
  }

  // /////////////////////////////////////
  // Listener registration/deregistration

  /**
   * Adds a selection listener. A Selection event is sent by the widget when the
   * user changes the selection.
   * <p>
   * When <code>widgetSelected</code> is called, the event x and y fields
   * contain the start and end caret indices of the selection.
   * <code>widgetDefaultSelected</code> is not called for StyledTexts.
   * </p>
   *
   * @param listener the listener which should be notified when the user changes
   *          the receiver's selection
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see SelectionListener
   * @see #removeSelectionListener
   * @see SelectionEvent
   */
  public void addSelectionListener( final SelectionListener listener ) {
    SelectionEvent.addListener( this, listener );
  }

  /**
   * Removes the listener from the collection of listeners who will be notified
   * when the user changes the receiver's selection.
   *
   * @param listener the listener which should no longer be notified
   * @exception IllegalArgumentException <ul>
   *              <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
   *              </ul>
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   * @see SelectionListener
   * @see #addSelectionListener
   */
  public void removeSelectionListener( final SelectionListener listener ) {
    SelectionEvent.removeListener( this, listener );
  }

  /**
   * Gets the number of characters.
   *
   * @return number of characters in the widget
   * @exception SWTException <ul>
   *              <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *              <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
   *              thread that created the receiver</li>
   *              </ul>
   */
  public int getCharCount() {
    checkWidget();
    return content.length();
  }

  /**
   * Sets the caret offset.
   *
   * @param offset caret offset, relative to the first character in the text.
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   */
  public void setCaretOffset( final int offset ) {
    checkWidget();
    int length = getCharCount();
    if( length > 0 && offset != caretOffset ) {
      if( offset < 0 ) {
        caretOffset = 0;
      } else if( offset > length ) {
        caretOffset = length;
      } else {
        caretOffset = offset;
      }
      clearSelection( false );
      // [ev] this generates html on each click
      // this.html = generateHtml();
    }
  }

  /**
   * Returns the caret position relative to the start of the text.
   *
   * @return the caret position relative to the start of the text.
   * @exception SWTException <ul>
   *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
   *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
   * </ul>
   */
  public int getCaretOffset() {
    checkWidget();
    return caretOffset;
  }

  /**
   * Hides the scroll bars if widget is created in single line mode.
   */
  static int checkStyle( int style ) {
    if( ( style & SWT.SINGLE ) != 0 ) {
      style &= ~( SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI );
    } else {
      style |= SWT.MULTI;
      if( ( style & SWT.WRAP ) != 0 ) {
        style &= ~SWT.H_SCROLL;
      }
    }
    return style;
  }

  /**
   * Removes the widget selection.
   *
   * @param sendEvent a Selection event is sent when set to true and when the
   *          selection is actually reset.
   */
  void clearSelection( final boolean sendEvent ) {
    resetSelection();
    if( sendEvent ) {
      sendSelectionEvent();
    }
  }

  /**
   * Resets the selection.
   */
  void resetSelection() {
    selection.x = selection.y = caretOffset;
    selectionAnchor = -1;
  }

  /**
   * Sends the specified selection event.
   */
  void sendSelectionEvent() {
    Rectangle bounds = new Rectangle( selection.x, selection.y, 0, 0 );
    SelectionEvent event = new SelectionEvent( this,
                                               null,
                                               SelectionEvent.WIDGET_SELECTED,
                                               bounds,
                                               0,
                                               null,
                                               true,
                                               SWT.NONE );
    event.processEvent();
  }

  private String createCharStyle( final StyleRange styleRange ) {
    StringBuffer result = new StringBuffer();
    result.append( " style='" );
    if( styleRange.foreground != null ) {
        String foreground = toHtmlString( styleRange.foreground );
        result.append( "color:" + foreground + ";" );
    }
    if( styleRange.background != null ) {
        String background = toHtmlString( styleRange.background );
        result.append( "background-color:" + background + ";" );
    }
    if( styleRange.fontStyle != SWT.NORMAL ) {
        switch( styleRange.fontStyle ) {
        case SWT.BOLD:
            result.append( "font-weight:bold;" );
            break;
        case SWT.ITALIC:
            result.append( "font-style:italic;" );
            break;
        case SWT.BOLD | SWT.ITALIC:
            result.append( "font-weight:bold;font-style:italic;" );
            break;
        default:
        }
    }
    boolean underline = styleRange.underline;
    boolean strikeout = styleRange.strikeout;
    if( underline && strikeout ) {
        result.append( "text-decoration: underline line-through;" );
    } else if( underline ) {
        result.append( "text-decoration: underline;" );
    } else if( strikeout ) {
        result.append( "text-decoration: line-through;" );
    }
    result.append( "'" );
    return result.toString();
  }

  private String generateHtml() {
    StringBuffer html = new StringBuffer();
    charStyle = "";
    html.append( "<span id=sr0" + charStyle + ">" );
    StyleRange[] styleRanges = getStyleRanges(); // this is expensive
    int lastMatch = 0;
    for( int i = 0; i < content.length(); i++ ) {
      /*
       * Optimizations to reduce the cost of searching the style ranges and
       * generating the spans from from O(content)*O(ranges) to O(content)*2:
       *
       * - the ranges are sorted from low to high and do not overlap
       *   (this is guaranteed by the renderer)
       * - begin by looking at the range that was matched previously
       *   (range #0 the first time)
       * - exit from generateStyleTag(...) when a range is encountered that is
       *   after charId (because all others will be after too)
       * - generateStyleTag(...) returns the number of the lastMatch; begin the
       *   next search with that style range
       */
      lastMatch = generateStyleTag( html, styleRanges, i, lastMatch );
      generateSelectionTag( html, i );
      if( content.charAt( i ) == '\n' ) {
        html.append( "</span>" );
        html.append( "<br/>" );
        html.append( "<span id=sr" + ( i + 1 ) + charStyle + ">" );
      } else {
        html.append( escape( content.charAt( i ) ) );
      }
    }
    html.append( "</span>" );
    return html.toString();
  }

  private int generateStyleTag( final StringBuffer html,
                                 final StyleRange[] styleRanges,
                                 final int charId,
                                 final int lastMatch ) {
    int result = lastMatch;
    int start = 0;
    for( int i = lastMatch; i < styleRanges.length && start <= charId; i++ ) {
      StyleRange styleRange = styleRanges[ i ];
      start = styleRange.start;
      int length = styleRange.length;

      if( start + length == charId ) {
        html.append( "</span>" );
        charStyle = "";
        html.append( "<span id=sr" + charId + charStyle + ">" );
        result = i;
      }
      if( start == charId ) {
        html.append( "</span>" );
        charStyle = createCharStyle( styleRange );
        html.append( "<span id=sr" + charId + charStyle + ">" );
        result = i;
      }
    }
    return result;
  }

  private void generateSelectionTag( final StringBuffer html,
                                     final int charId )
  {
    if( selection.x != selection.y ) {
      if( selection.x == charId ) {
        html.append( "</span>" );
        html.append( "<span id=sel>" );
        html.append( "<span id=sr" + charId + charStyle + ">" );
      }
      if( selection.y == charId ) {
        html.append( "</span>" );
        html.append( "</span>" );
        html.append( "<span id=sr" + charId + charStyle + ">" );
      }
    } else {
      if( selection.x == charId ) {
        html.append( "</span>" );
        html.append( "<span id=sel>" );
        html.append( "</span>" );
        html.append( "<span id=sr" + charId + charStyle + ">" );
      }
    }
  }

  private String escape( final char chr ) {
    String res = "";
    switch( chr ) {
      case ' ':
        res = "&nbsp;";
        break;
      case '<':
        res = "&lt;";
        break;
      case '>':
        res = "&gt;";
        break;
      case '&':
        res = "&amp;";
        break;
      default:
        res = Character.toString( chr );
    }
    return res;
  }

  private static String toHtmlString( final Color color ) {
    int red = color.getRed();
    int green = color.getGreen();
    int blue = color.getBlue();
    StringBuffer sb = new StringBuffer();
    sb.append( "#" );
    sb.append( getHexStr( red ) );
    sb.append( getHexStr( green ) );
    sb.append( getHexStr( blue ) );
    return sb.toString();
  }

  private static String getHexStr( final int value ) {
    String hex = Integer.toHexString( value );
    return hex.length() == 1 ? "0" + hex : hex;
  }

  @SuppressWarnings("rawtypes")
  public Object getAdapter( final Class adapter ) {
    Object result;
    if( adapter == IStyledTextAdapter.class ) {
      if( styledTextAdapter == null ) {
        styledTextAdapter = new StyledTextAdapter();
      }
      result = styledTextAdapter;
    } else {
      result = super.getAdapter( adapter );
    }
    return result;
  }
}

