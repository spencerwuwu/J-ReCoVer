// https://searchcode.com/api/result/1376332/

package beauty.options;

import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.msg.*;

import ise.java.awt.*;

/**
 * An option pane to configure settings for the built-in JSP beautifier.
 */
public class JspOptionPane extends AbstractOptionPane {

    private JCheckBox padSlashEnd;    // put space before />
    private JCheckBox padTagEnd;    // put space before >
    private JCheckBox wrapAttributes;    // put each attribute within a tag on a separate line
    private JCheckBox collapseBlankLines;    // reduce multiple blank lines to a single blank line

    public JspOptionPane() {
        super( "beauty.jsp" );
    }

    // called when this class is first accessed
    public void _init() {
        installComponents();
    }


    // create the user interface components and do the layout
    private void installComponents() {
        setLayout( new KappaLayout() );
        setBorder( BorderFactory.createEmptyBorder(6, 6, 6, 6 ) );

        // create the components
        JLabel description = new JLabel( "<html><b>" + jEdit.getProperty("beauty.msg.JSP_Options", "JSP Options") );

        padSlashEnd = new JCheckBox( jEdit.getProperty("beauty.msg.Pad_tag_slash_end_(/>)", "Pad tag slash end (/>)") );
        padTagEnd = new JCheckBox( jEdit.getProperty("beauty.msg.Pad_tag_end_(>)", "Pad tag end (>)") );
        wrapAttributes = new JCheckBox( jEdit.getProperty("beauty.msg.Wrap_attributes", "Wrap attributes") );
        collapseBlankLines = new JCheckBox( jEdit.getProperty("beauty.msg.Collapse_blank_lines", "Collapse blank lines") );

        padSlashEnd.setSelected( jEdit.getBooleanProperty( "beauty.jsp.padSlashEnd", false ) );
        padTagEnd.setSelected( jEdit.getBooleanProperty( "beauty.jsp.padTagEnd", false ) );
        wrapAttributes.setSelected( jEdit.getBooleanProperty( "beauty.jsp.wrapAttributes", false ) );
        collapseBlankLines.setSelected( jEdit.getBooleanProperty( "beauty.jsp.collapseBlankLines", true ) );

        add( "0, 0, 1, 1, W, w, 3", description );
        add( "0, 1, 1, 1, W, w, 3", padSlashEnd );
        add( "0, 2, 1, 1, W, w, 3", padTagEnd );
        add( "0, 3, 1, 1, W, w, 3", wrapAttributes );
        add( "0, 4, 1, 1, W, w, 3", collapseBlankLines );
    }

    public void _save() {
        jEdit.setBooleanProperty( "beauty.jsp.padSlashEnd", padSlashEnd.isSelected() );
        jEdit.setBooleanProperty( "beauty.jsp.padTagEnd", padTagEnd.isSelected() );
        jEdit.setBooleanProperty( "beauty.jsp.wrapAttributes", wrapAttributes.isSelected() );
        jEdit.setBooleanProperty( "beauty.jsp.collapseBlankLines", collapseBlankLines.isSelected() );
    }
}
