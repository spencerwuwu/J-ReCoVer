// https://searchcode.com/api/result/10653757/

package net.sf.colossus.util;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

import net.sf.colossus.client.IOptions;
import net.sf.colossus.client.SaveWindow;


/** KFrame adds some generally useful functions to JFrame.
 * 
 * TODO SaveWindow handling should be on this level
 * 
 *  @version $Id: KFrame.java 2952 2008-01-03 05:26:26Z cleka $
 *  @author Clemens Katzer */

public class KFrame extends JFrame implements MouseListener, WindowListener
{

    private SaveWindow kSaveWindow;

    /** Only support the simple constructor forms of JFrame. */

    public KFrame()
    {
        super();
        net.sf.colossus.webcommon.InstanceTracker.register(this, "<no title>");
    }

    public KFrame(String title)
    {
        super(title);
        net.sf.colossus.webcommon.InstanceTracker.register(this, title);
    }

    /**
     * If, and only if, the extending class calls this useSaveWindow,
     * then the KFrame will handle the SaveWindow work:
     * creating it when useSaveWindow is called, and saving back
     * always when setVisible(false) is called (and useSaveWindow was
     * called before, of course).
     * @param options IOptions reference to the client for saving window 
     *        size+pos in the Options data
     * @param windowName name/title of the window, 
     *        window size+pos are stored for that name 
     * @param defaultLocation to be used if no location was earlier stored: 
     *        place there; give null to center on screen.
     */
    public void useSaveWindow(IOptions options, String windowName,
        Point defaultLocation)
    {
        kSaveWindow = new SaveWindow(options, windowName);
        if (defaultLocation == null)
        {
            kSaveWindow.restoreOrCenter(this);
        }
        else
        {
            kSaveWindow.restore(this, defaultLocation);
        }
    }

    public void setVisible(boolean val)
    {
        if (!val && kSaveWindow != null)
        {
            kSaveWindow.save(this);
        }
        super.setVisible(val);
    }

    public void dispose()
    {
        super.dispose();
        kSaveWindow = null;
    }

    /** Center this dialog on the screen.  Must be called after the dialog
     *  size has been set. */
    public void centerOnScreen()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
            - getSize().height / 2));
    }

    // Add the do-nothing mouse and window listener methods here, rather 
    // than using Adapters, to reduce the number of useless little inner
    // class files we generate.

    // Note the potential for error if a subclass tries to override
    // one of these methods, but fails due to a typo, and the compiler
    // no longer flags the error because the interface is legally implemented.
    // (Adapters have the same problem.)

    public void mouseClicked(MouseEvent e)
    {
        // nothing to do
    }

    public void mouseEntered(MouseEvent e)
    {
        // nothing to do
    }

    public void mouseExited(MouseEvent e)
    {
        // nothing to do
    }

    public void mousePressed(MouseEvent e)
    {
        // nothing to do
    }

    public void mouseReleased(MouseEvent e)
    {
        // nothing to do
    }

    public void windowClosed(WindowEvent e)
    {
        // nothing to do
    }

    public void windowActivated(WindowEvent e)
    {
        // nothing to do
    }

    public void windowClosing(WindowEvent e)
    {
        // nothing to do
    }

    public void windowDeactivated(WindowEvent e)
    {
        // nothing to do
    }

    public void windowDeiconified(WindowEvent e)
    {
        // nothing to do
    }

    public void windowIconified(WindowEvent e)
    {
        // nothing to do
    }

    public void windowOpened(WindowEvent e)
    {
        // nothing to do
    }
}

