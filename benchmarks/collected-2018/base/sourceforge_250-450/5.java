// https://searchcode.com/api/result/1366711/

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
import java.awt.Font;
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
import org.gjt.sp.jedit.textarea.Selection;

// -- for Code2HTML 0.5
// import code2html.Code2HTML;

// -- for Code2HTML 0.6
import code2html.generic.GenericExporter;
import code2html.services.ExporterProvider;

class NavHistoryList extends JPanel {

    private JList list;
    private View view;
    private NavPosition initialPosition = null;
    private Navigator navigator = null;
    private Font textAreaFont = null;

    public NavHistoryList(View view, Navigator navigator, Collection<NavPosition> positions) {
        this(view, navigator, positions, null);
    }

    public NavHistoryList(View view, Navigator navigator, Collection<NavPosition> positions, NavPosition currentPosition) {
        this.navigator = navigator;
        this.view = view;
        initialPosition = currentPosition;
        positions = new ArrayList<NavPosition>(positions);
        Collections.reverse((List) positions);        // it's actually a Stack, so need to reverse it


        // create components
        setLayout(new BorderLayout());
        if (NavigatorPlugin.groupByFile()) {
            positions = groupByFile(positions);
        }
        list = new JList(positions.toArray());
        list.setCellRenderer(new CellRenderer());
        list.setVisibleRowCount(jEdit.getIntegerProperty("navigator.listSize", 10));

        JScrollPane scroller = new JScrollPane(list);
        add(scroller, BorderLayout.CENTER);

        if (view.getEditPane().getTextArea().getPainter().getStyles() != null && view.getEditPane().getTextArea().getPainter().getStyles().length > 0) {
            textAreaFont = view.getEditPane().getTextArea().getPainter().getStyles()[0].getFont();   
        }

        // show components
        list.requestFocus();
        if (currentPosition != null) {
            list.setSelectedValue(currentPosition, true);
        }
    }

    public void addKeyListener(KeyListener listener) {
        list.addKeyListener(listener);
    }

    public void addMouseListener(MouseListener listener) {
        list.addMouseListener(listener);
    }

    public void setModel(NavStack model) {
        Collections.reverse((List) model);
        list.setModel(model);
    }

    // this makes the list recalculate cell dimensions when options change
    public void updateUI() {
        super.updateUI();
        if (list != null) {
            list.updateUI();
        }
    }
    
    public void setToolTipText(String tip) {
        list.setToolTipText(tip);   
    }
    
    private Collection<NavPosition> groupByFile(Collection<NavPosition> positions) {
        List<NavPosition> items = new ArrayList<NavPosition>(positions.size());
        HashSet<String> paths = new HashSet<String>();
        for (NavPosition pos : positions) {
            if (paths.add(pos.path)) {
                items.add(pos);
            }
        }
        return items;
    }

    public void jump() {
        NavPosition item = ((NavPosition) list.getSelectedValue());
        navigator.jump(item);
    }

    // A cell renderer that will show html.  Delegates to the Code2HTML plugin
    // to show the line preview with proper syntax highlighting.
    class CellRenderer extends JLabel implements ListCellRenderer {

        private Border defaultBorder = BorderFactory.createEmptyBorder(1, 1, 6, 1);
        private Border initialPositionBorder = BorderFactory.createCompoundBorder(new LineBorder(getForeground()), defaultBorder);

        public CellRenderer() {
            setBorder(defaultBorder);
            setFont(view.getEditPane().getTextArea().getFont());
        }

        // value to display
        // cell index
        // is the cell selected
        // the list and the cell have the focus
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            NavPosition pos = (NavPosition) value;
            if (pos == null) {
                return null;
            }
            String labelText = pos.plainText();
            if (jEdit.getBooleanProperty("navigator.showLineText", true)) {
                EditPane editPane = null;
                for (EditPane ep : view.getEditPanes()) {
                    if (ep.hashCode() == pos.editPane) {
                        editPane = ep;
                        break;
                    }
                }
                if (editPane == null || !jEdit.getBooleanProperty("navigator.showLineTextSyntax", true)) {
                    labelText = pos.htmlText(Dockable.MAX_LINE_LENGTH);                    // non-syntax highlighted html
                } else {
                    // Have Code2HTML plugin create syntax highlighted html.
                    // First, create a selection for the text of the line
                    Buffer[] buffers = editPane.getBufferSet().getAllBuffers();
                    Buffer buffer = null;
                    for (Buffer b : buffers) {
                        if (b.getPath().equals(pos.path)) {
                            buffer = b;
                            break;
                        }
                    }
                    if (buffer == null) {
                        labelText = pos.htmlText();
                    } else {
                        // protect against pos having an invalid line number
                        int line = pos.lineno >= buffer.getLineCount() ? buffer.getLineCount() - 1 : pos.lineno;
                        int start = buffer.getLineStartOffset(line);
                        int end = buffer.getLineEndOffset(line);
                        if (end - start > Dockable.MAX_LINE_LENGTH) {
                            end = start + Dockable.MAX_LINE_LENGTH;   
                        }
                        Selection selection = new Selection.Rect(line, start, line, end);
                        Selection[] selections = new Selection[1];
                        selections[0] = selection;

                        // Have code2html do the syntax highlighting
                        // set Code2Html properties, don't want to use css, do want to show
                        // the gutter since that gives us line numbers.
                        boolean usecss = jEdit.getBooleanProperty("code2html.use-css", false);
                        boolean showgutter = jEdit.getBooleanProperty("code2html.show-gutter", false);
                        int wrap = jEdit.getIntegerProperty("code2html.wrap", 0);
                        jEdit.setBooleanProperty("code2html.use-css", false);
                        jEdit.setBooleanProperty("code2html.show-gutter", false);
                        jEdit.setIntegerProperty("code2html.wrap", 0);
                        GenericExporter exporter = (GenericExporter) ((ExporterProvider) ServiceManager.getService("code2html.services.ExporterProvider", "html")).getExporter(buffer, editPane.getTextArea().getPainter().getStyles(), selections);
                        labelText = exporter.getContentString();
                        jEdit.setBooleanProperty("code2html.use-css", usecss);
                        jEdit.setBooleanProperty("code2html.show-gutter", showgutter);
                        jEdit.setIntegerProperty("code2html.wrap", wrap);

                        // clean up the output from code2html, it outputs html, head, and body tags,
                        // I just want what is between the pre tags.
                        int preIndex = labelText.indexOf("<pre>");
                        if (preIndex >= 0 ) {
                            labelText = labelText.substring(preIndex + 5);  // 5 = <pre>.length
                            preIndex = labelText.lastIndexOf("</pre>");
                            if (preIndex >= 0) {
                                labelText = labelText.substring(0, preIndex - 1);
                            }
                        }
                        
                        // set the font to be the same as the jEdit text area font
                        labelText = new StringBuilder("<font face=\"").append(textAreaFont.getName()).append("\">").append(labelText).append("</font>").toString();

                        // reduce multiple spaces to single space
                        labelText = labelText.replaceAll("[ ]+", " ");

                        // remove line separators.  Code2HTML only outputs \n, not \r.
                        labelText = labelText.replaceAll("\n", "");

                        // add on the path, followed by the syntax highlighted line.  The line number
                        // is provided by code2html, that's why the useGutter property is set to true.
                        boolean showPath = jEdit.getBooleanProperty("navigator.showPath", true);
                        boolean showLineNumber = jEdit.getBooleanProperty("navigator.showLineNumber", true);
                        boolean showCaretOffset = jEdit.getBooleanProperty("navigator.showCaretOffset", true);
                        StringBuilder sb = new StringBuilder();
                        sb.append("<html><tt>");
                        sb.append(showPath ? pos.path : pos.name);
                        if (showLineNumber) {
                            // lineno is 0-based, but gutter lines are 1-based, so add one
                            sb.append(":").append(line + 1);
                        }
                        if (showCaretOffset) {
                            sb.append(":").append(pos.caret);
                        }
                        if (showPath) {
                            sb.append("<br>");   
                        }
                        sb.append("</tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                        sb.append(labelText.trim());
                        labelText = sb.toString();
                    }
                }
            }
            setText(labelText);
            setEnabled(list.isEnabled());
            setFont(view.getEditPane().getTextArea().getFont());
            setOpaque(true);
            setBackground(view.getBackground());
            if (jEdit.getBooleanProperty("navigator.showStripes", true) && index % 2 == 0) {
                setBackground(getBackground().darker());
            }
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            setBorder(pos.equals(initialPosition) ? initialPositionBorder : defaultBorder);
            return this;
        }
    }
}
