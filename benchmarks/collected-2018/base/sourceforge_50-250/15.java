// https://searchcode.com/api/result/10637727/

package net.sf.colossus.util;


import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import net.sf.colossus.client.IOptions;
import net.sf.colossus.client.SaveWindow;
import net.sf.colossus.webcommon.InstanceTracker;


/** KDialog adds some generally useful functions to JDialog.
 * 
 *  @version $Id: KDialog.java 3243 2008-02-27 02:22:35Z peterbecker $
 *  @author David Ripton
 */
public class KDialog extends JDialog implements MouseListener, WindowListener
{
    private SaveWindow kSaveWindow;

    /** 
     * Only support one of JDialog's many constructor forms.
     */
    public KDialog(Frame owner, String title, boolean modal)
    {
        super(owner, title, modal);
        InstanceTracker.register(this, "KDialog-for-?");
    }

    /** 
     * Place dialog relative to parentFrame's origin, offset by 
     * point, and fully on-screen.
     */
    public void placeRelative(JFrame parentFrame, Point point, JScrollPane pane)
    {

        JViewport viewPort = pane.getViewport();

        // Absolute coordinate in the screen since the window is toplevel
        Point parentOrigin = parentFrame.getLocation();

        // Relative coordinate of the view, change when scrolling
        Point viewOrigin = viewPort.getViewPosition();

        Point origin = new Point(point.x + parentOrigin.x - viewOrigin.x,
            point.y + parentOrigin.y - viewOrigin.y);

        setLocation(origin);
    }

    /**
     * Center this dialog on the screen.
     * 
     * Must be called after the dialog size has been set.
     */
    public void centerOnScreen()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
            - getSize().height / 2));
    }

    /**
     * 
     * Center this dialog on the screen, with an additional offset.
     * 
     * Must be called after the dialog size has been set.
     */
    public void centerOnScreen(int xoffset, int yoffset)
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point((d.width / 2 - getSize().width / 2) + xoffset,
            (d.height / 2 - getSize().height / 2) + yoffset));
    }

    public Point getUpperRightCorner(int width)
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        Point location = new Point(d.width - width, 0);
        return location;
    }

    public void upperRightCorner()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        Point location = new Point(d.width - getSize().width, 0);
        setLocation(location);
    }

    // Move up a few pixels from the bottom, to help avoid taskbars.
    public void lowerRightCorner()
    {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width - getSize().width, d.height
            - getSize().height - 30));
    }

    /**
     * If, and only if, the extending class calls this useSaveWindow,
     * then the KDialog will handle the SaveWindow work:
     * creating it when useSaveWindow is called, and saving back
     * always when setVisible(false) is called (and useSaveWindow was
     * called before, of course).
     * 
     * TODO maybe we should enforce this by calling it through the 
     *      constructor
     * 
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

    @Override
    public void setVisible(boolean val)
    {
        if (!val && kSaveWindow != null)
        {
            kSaveWindow.save(this);
        }
        super.setVisible(val);
    }

    @Override
    public void dispose()
    {
        if (kSaveWindow != null)
        {
            kSaveWindow.save(this);
        }
        super.dispose();
        kSaveWindow = null;
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

