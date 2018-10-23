// https://searchcode.com/api/result/39115966/

/**
 * A library to interact with Virtual Worlds such as OpenSim
 * Copyright (C) 2012  Jitendra Chauhan, Email: jitendra.chauhan@gmail.com
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package jj2000.j2k.image.input;

import jj2000.j2k.image.*;
import jj2000.j2k.*;
import java.io.*;

/**
 * This is the generic interface to be implemented by all image file (or other
 * resource) readers for different image file formats.
 *
 * <p>An ImgReader behaves as an ImgData object. Whenever image data is
 * requested through the getInternCompData() or getCompData() methods, the
 * image data will be read (if it is not buffered) and returned. Implementing
 * classes should not buffer large amounts of data, so as to reduce memory
 * usage.</p>
 *
 * <p>This class sets the image origin to (0,0). All default implementations
 * of the methods assume this.</p>
 *
 * <p>This class provides default implementations of many methods. These
 * default implementations assume that there is no tiling (i.e., the only tile
 * is the entire image), that the image origin is (0,0) in the canvas system
 * and that there is no component subsampling (all components are the same
 * size), but they can be overloaded by the implementating class if need
 * be.</p>
 * */
public abstract class ImgReader implements BlkImgDataSrc {

    /** The width of the image */
    protected int w;

    /** The height of the image */
    protected int h;

    /** The number of components in the image */
    protected int nc;

    /**
     * Closes the underlying file or network connection from where the
     * image data is being read.
     *
     * @exception IOException If an I/O error occurs.
     */
    public abstract void close() throws IOException;

    /**
     * Returns the width of the current tile in pixels, assuming there is
     * no-tiling. Since no-tiling is assumed this is the same as the width of
     * the image. The value of <tt>w</tt> is returned.
     *
     * @return The total image width in pixels.
     * */
    public int getTileWidth() {
        return w;
    }

    /**
     * Returns the overall height of the current tile in pixels, assuming
     * there is no-tiling. Since no-tiling is assumed this is the same as the
     * width of the image. The value of <tt>h</tt> is returned.
     *
     * @return The total image height in pixels.  */
    public int getTileHeight() {
        return h;
    }

    /** Returns the nominal tiles width */
    public int getNomTileWidth() {
        return w;
    }

    /** Returns the nominal tiles height */
    public int getNomTileHeight() {
        return h;
    }

    /**
     * Returns the overall width of the image in pixels. This is the image's
     * width without accounting for any component subsampling or tiling. The
     * value of <tt>w</tt> is returned.
     *
     * @return The total image's width in pixels.
     * */
    public int getImgWidth() {
        return w;
    }

    /**
     * Returns the overall height of the image in pixels. This is the image's
     * height without accounting for any component subsampling or tiling. The
     * value of <tt>h</tt> is returned.
     *
     * @return The total image's height in pixels.
     * */
    public int getImgHeight() {
        return h;
    }

    /**
     * Returns the number of components in the image. The value of <tt>nc</tt>
     * is returned.
     *
     * @return The number of components in the image.
     * */
    public int getNumComps() {
        return nc;
    }

    /**
     * Returns the component subsampling factor in the horizontal direction,
     * for the specified component. This is, approximately, the ratio of
     * dimensions between the reference grid and the component itself, see the
     * 'ImgData' interface desription for details.
     *
     * @param c The index of the component (between 0 and C-1)
     *
     * @return The horizontal subsampling factor of component 'c'
     *
     * @see jj2000.j2k.image.ImgData
     * */
    public int getCompSubsX(int c) {
        return 1;
    }

    /**
     * Returns the component subsampling factor in the vertical direction, for
     * the specified component. This is, approximately, the ratio of
     * dimensions between the reference grid and the component itself, see the
     * 'ImgData' interface desription for details.
     *
     * @param c The index of the component (between 0 and C-1)
     *
     * @return The vertical subsampling factor of component 'c'
     *
     * @see jj2000.j2k.image.ImgData
     * */
    public int getCompSubsY(int c) {
        return 1;
    }

    /**
     * Returns the width in pixels of the specified tile-component. This
     * default implementation assumes no tiling and no component subsampling
     * (i.e., all components, or components, have the same dimensions in
     * pixels).
     *
     * @param t Tile index
     *
     * @param c The index of the component, from 0 to C-1.
     *
     * @return The width in pixels of component <tt>c</tt> in tile<tt>t</tt>.
     * */
    public int getTileCompWidth(int t,int c) {
        if(t!=0) {
            throw new Error("Asking a tile-component width for a tile index"+
                            " greater than 0 whereas there is only one tile");
        }
        return w;
    }

    /**
     * Returns the height in pixels of the specified tile-component. This
     * default implementation assumes no tiling and no component subsampling
     * (i.e., all components, or components, have the same dimensions in
     * pixels).
     *
     * @param t The tile index
     *
     * @param c The index of the component, from 0 to C-1.
     *
     * @return The height in pixels of component <tt>c</tt> in tile
     * <tt>t</tt>.
     * */
    public int getTileCompHeight(int t,int c) {
        if(t!=0) {
            throw new Error("Asking a tile-component width for a tile index"+
                            " greater than 0 whereas there is only one tile");
        }
        return h;
    }

    /**
     * Returns the width in pixels of the specified component in the overall
     * image. This default implementation assumes no component, or component,
     * subsampling (i.e. all components have the same dimensions in pixels).
     *
     * @param c The index of the component, from 0 to C-1.
     *
     * @return The width in pixels of component <tt>c</tt> in the overall
     * image.
     * */
    public int getCompImgWidth(int c) {
        return w;
    }

    /**
     * Returns the height in pixels of the specified component in the overall
     * image. This default implementation assumes no component, or component,
     * subsampling (i.e. all components have the same dimensions in pixels).
     *
     * @param c The index of the component, from 0 to C-1.
     *
     * @return The height in pixels of component <tt>c</tt> in the overall
     * image.
     * */
    public int getCompImgHeight(int c) {
        return h;
    }

    /**
     * Changes the current tile, given the new coordinates. An
     * IllegalArgumentException is thrown if the coordinates do not correspond
     * to a valid tile. This default implementation assumes no tiling so the
     * only valid arguments are x=0, y=0.
     *
     * @param x The horizontal coordinate of the tile.
     *
     * @param y The vertical coordinate of the new tile.
     * */
    public void setTile(int x, int y) {
        if (x!=0 || y != 0) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Advances to the next tile, in standard scan-line order (by rows then
     * columns). A NoNextElementException is thrown if the current tile is the
     * last one (i.e. there is no next tile). This default implementation
     * assumes no tiling, so NoNextElementException() is always thrown.
     * */
    public void nextTile() {
        throw new NoNextElementException();
    }

    /**
     * Returns the coordinates of the current tile. This default
     * implementation assumes no-tiling, so (0,0) is returned.
     *
     * @param co If not null this object is used to return the information. If
     * null a new one is created and returned.
     *
     * @return The current tile's coordinates.
     * */
    public Coord getTile(Coord co) {
        if (co != null) {
            co.x = 0;
            co.y = 0;
            return co;
        }
        else {
            return new Coord(0,0);
        }
    }

    /**
     * Returns the index of the current tile, relative to a standard scan-line
     * order. This default implementations assumes no tiling, so 0 is always
     * returned.
     *
     * @return The current tile's index (starts at 0).
     * */
    public int getTileIdx() {
        return 0;
    }

    /**
     * Returns the horizontal coordinate of the upper-left corner of the
     * specified component in the current tile.
     *
     * @param c The component index.
     * */
    public int getCompULX(int c) {
        return 0;
    }

    /**
     * Returns the vertical coordinate of the upper-left corner of the
     * specified component in the current tile.
     *
     * @param c The component index.
     * */
    public int getCompULY(int c) {
        return 0;
    }

    /** Returns the horizontal tile partition offset in the reference grid */
    public int getTilePartULX() {
        return 0;
    }

    /** Returns the vertical tile partition offset in the reference grid */
    public int getTilePartULY() {
        return 0;
    }

    /**
     * Returns the horizontal coordinate of the image origin, the top-left
     * corner, in the canvas system, on the reference grid.
     *
     * @return The horizontal coordinate of the image origin in the canvas
     * system, on the reference grid.
     * */
    public int getImgULX() {
        return 0;
    }

    /**
     * Returns the vertical coordinate of the image origin, the top-left
     * corner, in the canvas system, on the reference grid.
     *
     * @return The vertical coordinate of the image origin in the canvas
     * system, on the reference grid.
     * */
    public int getImgULY() {
        return 0;
    }

    /**
     * Returns the number of tiles in the horizontal and vertical
     * directions. This default implementation assumes no tiling, so (1,1) is
     * always returned.
     *
     * @param co If not null this object is used to return the information. If
     * null a new one is created and returned.
     *
     * @return The number of tiles in the horizontal (Coord.x) and vertical
     * (Coord.y) directions.
     * */
    public Coord getNumTiles(Coord co) {
        if (co != null) {
            co.x = 1;
            co.y = 1;
            return co;
        }
        else {
            return new Coord(1,1);
        }
    }

    /**
     * Returns the total number of tiles in the image. This default
     * implementation assumes no tiling, so 1 is always returned.
     *
     * @return The total number of tiles in the image.
     * */
    public int getNumTiles() {
        return 1;
    }

    /**
     * Returns true if the data read was originally signed in the specified
     * component, false if not.
     *
     * @param c The index of the component, from 0 to C-1.
     *
     * @return true if the data was originally signed, false if not.
     * */
    public abstract boolean isOrigSigned(int c);

}

