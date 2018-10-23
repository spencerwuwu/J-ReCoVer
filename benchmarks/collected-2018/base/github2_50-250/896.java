// https://searchcode.com/api/result/65552214/

package com.magnetstreet.swt.beanwidget.datagrid2.reflective.header;

import com.magnetstreet.swt.beanwidget.datagrid2.header.ColumnHeaderProvider;
import org.eclipse.swt.graphics.Image;

/**
 * TemplatedColumnHeaderProvider
 *
 * Designed to be used for the templated datagrid utils which abstract many of the
 * creation steps but in tern reduce the customization ability of the library.
 * @author Martin Dale Lyness <martin.lyness@gmail.com>
 * @since 5/5/11
 */
public class TemplatedColumnHeaderProvider<T> extends ColumnHeaderProvider {
    public final int AVG_CHAR_WIDTH = 10;

    protected String title = "";
    protected String tooltip = "";
    protected int width = 1;
    protected boolean resizable, moveable;
    protected Image image;

    public TemplatedColumnHeaderProvider(String title) {
        this.title = title;
        this.tooltip = "Sort by '"+title+"' column";
        this.width = title.length() * AVG_CHAR_WIDTH;
        this.resizable = true;
        this.moveable = false;
        this.image = null;
    }
    public TemplatedColumnHeaderProvider(String title, String tooltip, int width, boolean resizable, boolean moveable, Image image) {
        super();
        this.title = title;
        this.tooltip = tooltip;
        this.width = width;
        this.resizable = resizable;
        this.moveable = moveable;
        this.image = image;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getTooltip() {
        return tooltip;
    }
    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }
    public int getWidth() {
        return width;
    }
    public void setWidth(int width) {
        this.width = width;
    }
    public boolean isResizable() {
        return resizable;
    }
    public void setResizable(boolean resizable) {
        this.resizable = resizable;
    }
    public boolean isMoveable() {
        return moveable;
    }
    public void setMoveable(boolean moveable) {
        this.moveable = moveable;
    }
    public Image getImage() {
        return image;
    }
    public void setImage(Image image) {
        this.image = image;
    }
}

