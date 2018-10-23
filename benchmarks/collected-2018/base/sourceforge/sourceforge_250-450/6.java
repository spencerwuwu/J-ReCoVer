// https://searchcode.com/api/result/1366725/

/*
* ChooseTagListPopup.java
* Copyright (c) 2001, 2002 Kenrick Drew, Slava Pestov
*
* This file is part of TagsPlugin
*
* TagsPlugin is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or any later version.
*
* TagsPlugin is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*
* $Id: ChooseTagListPopup.java,v 1.10 2004/11/07 15:52:34 orutherfurd Exp $
*/

/* This is pretty much ripped from gui/CompleteWord.java */

package ise.plugin.nav;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.jedit.gui.KeyEventWorkaround;

// -- for Code2HTML 0.5
//import code2html.Code2HTML;

// -- for Code2HTML 0.6
import code2html.generic.GenericExporter;
import code2html.services.ExporterProvider;

class NavHistoryPopup extends JPopupMenu {

    private JList list;
    private View view;
    private NavPosition initialPosition = null;
    private boolean numberKeyProcessed = false;
    private Navigator navigator = null;

    private boolean useCSS = false;
    private boolean showGutter = false;

    public NavHistoryPopup( View view, Navigator navigator, Collection<NavPosition> positions ) {
        this( view, navigator, positions, null );
    }

    public NavHistoryPopup( View view, Navigator navigator, Collection<NavPosition> positions, NavPosition currentPosition ) {
        if (positions == null || positions.size() == 0) {
            return;   
        }
        this.navigator = navigator;
        this.view = view;
        initialPosition = currentPosition;

        // positions is a Stack, so need to reverse the order
        positions = new ArrayList<NavPosition>( positions );
        Collections.reverse( ( List ) positions );

        // create components
        JPanel contents = new JPanel( new BorderLayout() );
        if ( NavigatorPlugin.groupByFile() ) {
            positions = groupByFile( positions );
        }
        list = new JList( positions.toArray() );
        list.setCellRenderer( new CellRenderer() );
        list.setVisibleRowCount( jEdit.getIntegerProperty( "navigator.listSize", 10 ) );
        list.addMouseListener( new MouseHandler() );

        JScrollPane scroller = new JScrollPane( list );
        contents.add( scroller, BorderLayout.CENTER );

        // place components
        add( scroller );

        // add listeners
        KeyHandler keyHandler = new KeyHandler();
        addKeyListener( keyHandler );
        list.addKeyListener( keyHandler );
        this.view.setKeyEventInterceptor( keyHandler );

        // set Code2Html properties, don't want to use css, do want to show
        // the gutter since that gives us line numbers.
        useCSS = jEdit.getBooleanProperty( "code2html.use-css", false );
        showGutter = jEdit.getBooleanProperty( "code2html.show-gutter", true );
        jEdit.setBooleanProperty( "code2html.use-css", false );
        jEdit.setBooleanProperty( "code2html.show-gutter", true );

        // show components
        pack();
        setLocation();
        setVisible( true );
        list.requestFocus();
        if ( currentPosition != null ) {
            list.setSelectedValue( currentPosition, true );
        }
    }

    private Collection<NavPosition> groupByFile( Collection<NavPosition> positions ) {
        List<NavPosition> items = new ArrayList<NavPosition>( positions.size() );
        HashSet<String> paths = new HashSet<String>();
        for ( NavPosition pos: positions ) {
            if ( paths.add( pos.path ) ) {
                items.add( pos );
            }
        }
        return items;
    }

    /**
     * Set the location of the popup on the screen.
     */
    public void setLocation() {
        JEditTextArea textArea = view.getTextArea();

        int caretLine = textArea.getCaretLine();
        textArea.getLineStartOffset( caretLine );

        Rectangle rect = view.getGraphicsConfiguration().getBounds();
        Dimension d = getSize();
        Point location = new Point( rect.x + ( rect.width - d.width ) / 2,
                rect.y + ( rect.height - d.height ) / 2 );
        // make sure it fits on screen
        Dimension screenSize = rect.getSize();
        if ( location.x + d.width > screenSize.width ) {
            if ( d.width >= screenSize.width ) {
                /* In this instance we should actually resize the number of columns in
                 * the tag index filename, but for now just position it so that you
                 * can at least read the left side of the dialog
                 */
                location.x = rect.x;
            }
            else {
                location.x = rect.x + rect.width - d.width - 200;
            }
        }
        if ( location.y + d.height > screenSize.height ) {
            location.y = screenSize.height - d.height;
        }

        setLocation( location );

        textArea = null;
        location = null;
        d = null;
        screenSize = null;
    }


    public void dispose() {
        // restore Code2Html properties to original values
        jEdit.setBooleanProperty( "code2html.use-css", useCSS );
        jEdit.setBooleanProperty( "code2html.show-gutter", showGutter );

        view.setKeyEventInterceptor( null );
        setVisible( false );
        view.getTextArea().requestFocus();
    }


    private void selected() {
        NavPosition item = ( ( NavPosition ) list.getSelectedValue() );
        navigator.jump( item );
        dispose();
    }


    class KeyHandler extends KeyAdapter {

        public void keyTyped( KeyEvent evt ) {
            evt = KeyEventWorkaround.processKeyEvent( evt );
            if ( evt == null )
                return ;

            switch ( evt.getKeyChar() ) {
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if ( numberKeyProcessed )                        // Since many components have this handler
                        return ;

                    /* There may actually be more than 9 items in the list, but since
                     * the user would have to scroll to see them either with the mouse
                     * or with the arrow keys, then they can select the item they want
                     * with those means.
                     */
                    int selected = Character.getNumericValue( evt.getKeyChar() ) - 1;
                    if ( selected >= 0 &&
                            selected < list.getModel().getSize() ) {
                        list.setSelectedIndex( selected );
                        selected();
                        numberKeyProcessed = true;
                    }
                    evt.consume();
            }

            evt = null;
        }


        public void keyPressed( KeyEvent evt ) {
            evt = KeyEventWorkaround.processKeyEvent( evt );
            if ( evt == null )
                return ;

            switch ( evt.getKeyCode() ) {
                case KeyEvent.VK_TAB:
                case KeyEvent.VK_ENTER:
                    selected();
                    evt.consume();
                    break;
                case KeyEvent.VK_ESCAPE:
                    dispose();
                    evt.consume();
                    break;
                case KeyEvent.VK_UP:
                    int selected = list.getSelectedIndex();
                    if ( selected == 0 )
                        selected = list.getModel().getSize() - 1;
                    //else if ( getFocusOwner() == list )
                    //    return ; // Let JList handle the event
                    else
                        selected = selected - 1;

                    list.setSelectedIndex( selected );
                    list.ensureIndexIsVisible( selected );

                    evt.consume();
                    break;
                case KeyEvent.VK_DOWN:
                    selected = list.getSelectedIndex();
                    if ( selected == list.getModel().getSize() - 1 )
                        selected = 0;
                    //else if ( getFocusOwner() == list )
                    //    return ; // Let JList handle the event
                    else
                        selected = selected + 1;

                    list.setSelectedIndex( selected );
                    list.ensureIndexIsVisible( selected );

                    evt.consume();
                    break;
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_1:
                case KeyEvent.VK_2:
                case KeyEvent.VK_3:
                case KeyEvent.VK_4:
                case KeyEvent.VK_5:
                case KeyEvent.VK_6:
                case KeyEvent.VK_7:
                case KeyEvent.VK_8:
                case KeyEvent.VK_9:
                    evt.consume();  /* so that we don't automatically dismiss */
                    break;

                case KeyEvent.VK_PAGE_UP:
                case KeyEvent.VK_PAGE_DOWN:
                    break;

                default:
                    dispose();
                    evt.consume();
                    break;
            }
            evt = null;
        }
    }

    class MouseHandler extends MouseAdapter {
        public void mouseClicked( MouseEvent evt ) {
            selected();
        }
    }

    // A cell renderer that will show html.  Delegates to the Code2HTML plugin
    // to show the line preview with proper syntax highlighting.
    class CellRenderer extends JLabel implements ListCellRenderer {

        private Border defaultBorder = BorderFactory.createEmptyBorder( 1, 1, 6, 1 );
        private Border initialPositionBorder = BorderFactory.createCompoundBorder( new LineBorder( getForeground() ) , defaultBorder );

        public CellRenderer() {
            setBorder( defaultBorder );
        }

        public Component getListCellRendererComponent(
            JList list,
            Object value,                            // value to display
            int index,                               // cell index
            boolean isSelected,                      // is the cell selected
            boolean cellHasFocus )                   // the list and the cell have the focus
        {
            NavPosition pos = ( NavPosition ) value;
            if ( pos == null ) {
                return null;
            }
            String labelText = pos.toString();
            if ( jEdit.getBooleanProperty( "navigator.showLineText", true ) ) {
                EditPane editPane = null;
                for ( EditPane ep : view.getEditPanes() ) {
                    if ( ep.hashCode() == pos.editPane ) {
                        editPane = ep;
                        break;
                    }
                }
                if ( editPane == null || !jEdit.getBooleanProperty( "navigator.showLineTextSyntax", true ) ) {
                    labelText = pos.toHtml();       // non-syntax highlighted html
                }
                else {
                    // Have Code2HTML plugin create syntax highlighted html.
                    // First, create a selection for the text of the line
                    Buffer[] buffers = editPane.getBufferSet().getAllBuffers();
                    Buffer buffer = null;
                    for ( Buffer b : buffers ) {
                        if ( b.getPath().equals( pos.path ) ) {
                            buffer = b;
                            break;
                        }
                    }
                    if ( buffer == null ) {
                        labelText = pos.toHtml();
                    }
                    else {
                        int start = buffer.getLineStartOffset( pos.lineno );
                        int end = buffer.getLineEndOffset( pos.lineno );
                        Selection selection = new Selection.Rect( pos.lineno, start, pos.lineno, end );
                        Selection[] selections = new Selection[ 1 ];
                        selections[ 0 ] = selection;

                        // Have code2html do the syntax highlighting
                        // -- this is for Code2HTML 0.5:
                        /*
                        Code2HTML c2h = new Code2HTML(
                                    buffer,
                                    editPane.getTextArea().getPainter().getStyles(),
                                    selections
                                );
                        labelText = c2h.getHtmlString();
                        */

                        // -- this is for Code2HTML 0.6:
                        GenericExporter exporter = ( GenericExporter ) ( ( ExporterProvider ) ServiceManager.getService( "code2html.services.ExporterProvider", "html" ) ).getExporter(
                                    buffer,
                                    editPane.getTextArea().getPainter().getStyles(),
                                    selections
                                );
                        labelText = exporter.getContentString();

                        // clean up the output from code2html, it outputs html, head, and body tags,
                        // I just want what is between the pre tags
                        // -- next line can be removed with Code2HTML 0.6 since the getContentString method
                        // will return only the <pre>...</pre> content.
                        //labelText = labelText.substring( labelText.indexOf( "<pre>" ), labelText.lastIndexOf( "</pre>" ) + "</pre>".length() );

                        // reduce multiple spaces to single space
                        while ( labelText.indexOf( "  " ) > -1 ) {
                            labelText = labelText.replaceAll( "  ", " " );
                        }

                        // remove line separators.  Code2HTML only outputs \n, not \r.
                        labelText = labelText.replaceAll( "\n", "" );

                        // add on the path, followed by the syntax highlighted line.  The line number
                        // is provided by code2html, that's why the useGutter property is set to true.
                        labelText = "<html><tt>" + pos.path + ":</tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + labelText.trim() ;
                    }
                }
            }
            setText( labelText );
            setEnabled( list.isEnabled() );
            setFont( list.getFont() );
            setOpaque( true );
            setBackground( view.getBackground() );
            if ( jEdit.getBooleanProperty( "navigator.showStripes", true ) && index % 2 == 0 ) {
                setBackground( getBackground().darker() );
            }
            if ( isSelected ) {
                setBackground( list.getSelectionBackground() );
                setForeground( list.getSelectionForeground() );
            }
            setBorder( pos.equals( initialPosition ) ? initialPositionBorder : defaultBorder );
            return this;
        }
    }
}
