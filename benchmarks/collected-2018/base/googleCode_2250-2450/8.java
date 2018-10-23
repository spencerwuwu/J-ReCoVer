// https://searchcode.com/api/result/11599203/

/*
 * Copyright 2011 yura.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opu.db_vdumper.ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JDialog;

/*
 * @(#)SquareBoard.java
 *
 * This work is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (c) 2003 Per Cederberg. All rights reserved.
 *//**
 * The main class of the Tetris game. This class contains the
 * necessary methods to run the game either as a stand-alone
 * application or as an applet inside a web page.
 *
 * @version  1.2
 * @author   yura
 */
public class Tetris {

//    /**
//     * @return an array describing the parameters to this applet
//     */
//    private static final String PARAMETER[][] = {
//        { "tetris.color.background", "color",
//            "The overall background color." },
//        { "tetris.color.label", "color",
//            "The text color of the labels." },
//        { "tetris.color.button", "color",
//            "The start and pause button bolor." },
//        { "tetris.color.board.background", "color",
//            "The background game board color." },
//        { "tetris.color.board.message", "color",
//            "The game board message color." },
//        { "tetris.color.figure.square", "color",
//            "The color of the square figure." },
//        { "tetris.color.figure.line", "color",
//            "The color of the line figure." },
//        { "tetris.color.figure.s", "color",
//            "The color of the 's' curved figure." },
//        { "tetris.color.figure.z", "color",
//            "The color of the 'z' curved figure." },
//        { "tetris.color.figure.right", "color",
//            "The color of the right angle figure." },
//        { "tetris.color.figure.left", "color",
//            "The color of the left angle figure." },
//        { "tetris.color.figure.triangle", "color",
//            "The color of the triangle figure." }
//    };
    /**
     * The Tetris game being played (in applet mode).
     */
    private Game game = null;

    /**
     * The stand-alone main routine.
     *
     * @param args      the command-line arguments
     */
    public static void main(String[] args) {
        run(null);
    }

    public static void run(JDialog parent) throws HeadlessException {
        System.out.println("starting");
        final JDialog frame = new JDialog(parent, "Tetris", true);
        final Game game = new Game();

        game.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                //System.out.println("PCE " + evt.getPropertyName() + " " + evt.getNewValue());
            }
        });

//        final TextArea taHiScores = new TextArea("", 10, 10, TextArea.SCROLLBARS_NONE);
//
//        taHiScores.setBackground(Color.black);
//        taHiScores.setForeground(Color.white);
//        taHiScores.setFont(new Font("monospaced", 0, 11));
//        taHiScores.setText(" High Scores                  \n"
//                + " -----------------------------\n\n"
//                + " PLAYER     LEVEL    SCORE    \n\n"
//                + " Lorenzo       12 1  50280     \n"
//                + " Lorenzo       12 1  50280     \n");
//        taHiScores.setEditable(false);


        final TextField txt = new TextField();
        txt.setEnabled(false);

        game.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("state")) {
                    int state = ((Integer) evt.getNewValue()).intValue();
                    if (state == Game.STATE_GAMEOVER) {
                        txt.setEnabled(true);
                        txt.requestFocus();
                        txt.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                txt.setEnabled(false);
                                game.init();
                            }
                        });
                        // show score...
                    }
                }
            }
        });


        Button btnStart = new Button("Start");
        btnStart.setFocusable(false);
        btnStart.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                game.start();
            }
        });

        final Container c = new Container();
        c.setLayout(new BorderLayout());
        c.add(txt, BorderLayout.NORTH);
        c.add(game.getSquareBoardComponent(), BorderLayout.CENTER);
        c.add(btnStart, BorderLayout.SOUTH);

        final Container c2 = new Container();
        c2.setLayout(new GridLayout(1, 1));
        c2.add(c);
//        c2.add(taHiScores);

        frame.add(c2);

        System.out.println("packing");

        frame.pack();

        // Add frame window listener
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                frame.dispose();
            }
        });
        frame.setLocationRelativeTo(null);

        // Show frame (and start game)
        frame.setVisible(true);
    }
}


/**
 * A Tetris square board. The board is rectangular and contains a grid
 * of colored squares. The board is considered to be constrained to
 * both sides (left and right), and to the bottom. There is no
 * constraint to the top of the board, although colors assigned to
 * positions above the board are not saved.
 *
 * @version  1.2
 * @author   Per Cederberg, per@percederberg.net
 */
class SquareBoard extends Object {

    /**
     * The board width (in squares)
     */
    private final int width;
    /**
     * The board height (in squares).
     */
    private final int height;
    /**
     * The square board color matrix. This matrix (or grid) contains
     * a color entry for each square in the board. The matrix is
     * indexed by the vertical, and then the horizontal coordinate.
     */
    private Color[][] matrix = null;
    /**
     * An optional board message. The board message can be set at any
     * time, printing it on top of the board.
     */
    private String message = null;
    /**
     * The number of lines removed. This counter is increased each
     * time a line is removed from the board.
     */
    private int removedLines = 0;
    /**
     * The graphical sqare board component. This graphical
     * representation is created upon the first call to
     * getComponent().
     */
    private final SquareBoardComponent component;

    /**
     * Creates a new square board with the specified size. The square
     * board will initially be empty.
     *
     * @param width     the width of the board (in squares)
     * @param height    the height of the board (in squares)
     */
    public SquareBoard(int width, int height) {
        this.width = width;
        this.height = height;
        this.matrix = new Color[height][width];
        this.component = new SquareBoardComponent();
        clear();
    }

    /**
     * Checks if a specified square is empty, i.e. if it is not
     * marked with a color. If the square is outside the board,
     * false will be returned in all cases except when the square is
     * directly above the board.
     *
     * @param x         the horizontal position (0 <= x < width)
     * @param y         the vertical position (0 <= y < height)
     *
     * @return true if the square is emtpy, or
     *         false otherwise
     */
    public boolean isSquareEmpty(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return x >= 0 && x < width && y < 0;
        } else {
            return matrix[y][x] == null;
        }
    }

    /**
     * Checks if a specified line is empty, i.e. only contains
     * empty squares. If the line is outside the board, false will
     * always be returned.
     *
     * @param y         the vertical position (0 <= y < height)
     *
     * @return true if the whole line is empty, or
     *         false otherwise
     */
    public boolean isLineEmpty(int y) {
        if (y < 0 || y >= height) {
            return false;
        }
        for (int x = 0; x < width; x++) {
            if (matrix[y][x] != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a specified line is full, i.e. only contains no empty
     * squares. If the line is outside the board, true will always be
     * returned.
     *
     * @param y         the vertical position (0 <= y < height)
     *
     * @return true if the whole line is full, or
     *         false otherwise
     */
    public boolean isLineFull(int y) {
        if (y < 0 || y >= height) {
            return true;
        }
        for (int x = 0; x < width; x++) {
            if (matrix[y][x] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the board contains any full lines.
     *
     * @return true if there are full lines on the board, or
     *         false otherwise
     */
    public boolean hasFullLines() {
        for (int y = height - 1; y >= 0; y--) {
            if (isLineFull(y)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a graphical component to draw the board. The component
     * returned will automatically be updated when changes are made to
     * this board. Multiple calls to this method will return the same
     * component, as a square board can only have a single graphical
     * representation.
     *
     * @return a graphical component that draws this board
     */
    public Component getComponent() {
        return component;
    }

    /**
     * Returns the board height (in squares). This method returns,
     * i.e, the number of vertical squares that fit on the board.
     *
     * @return the board height in squares
     */
    public int getBoardHeight() {
        return height;
    }

    /**
     * Returns the board width (in squares). This method returns, i.e,
     * the number of horizontal squares that fit on the board.
     *
     * @return the board width in squares
     */
    public int getBoardWidth() {
        return width;
    }

    /**
     * Returns the number of lines removed since the last clear().
     *
     * @return the number of lines removed since the last clear call
     */
    public int getRemovedLines() {
        return removedLines;
    }

    /**
     * Returns the color of an individual square on the board. If the
     * square is empty or outside the board, null will be returned.
     *
     * @param x         the horizontal position (0 <= x < width)
     * @param y         the vertical position (0 <= y < height)
     *
     * @return the square color, or null for none
     */
    public Color getSquareColor(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        } else {
            return matrix[y][x];
        }
    }

    /**
     * Changes the color of an individual square on the board. The
     * square will be marked as in need of a repaint, but the
     * graphical component will NOT be repainted until the update()
     * method is called.
     *
     * @param x         the horizontal position (0 <= x < width)
     * @param y         the vertical position (0 <= y < height)
     * @param color     the new square color, or null for empty
     */
    public void setSquareColor(int x, int y, Color color) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }
        matrix[y][x] = color;
        if (component != null) {
            component.invalidateSquare(x, y);
        }
    }

    /**
     * Sets a message to display on the square board. This is supposed
     * to be used when the board is not being used for active drawing,
     * as it slows down the drawing considerably.
     *
     * @param message  a message to display, or null to remove a
     *                 previous message
     */
    public void setMessage(String message) {
        this.message = message;
        if (component != null) {
            component.redrawAll();
        }
    }

    /**
     * Clears the board, i.e. removes all the colored squares. As
     * side-effects, the number of removed lines will be reset to
     * zero, and the component will be repainted immediately.
     */
    public void clear() {
        removedLines = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.matrix[y][x] = null;
            }
        }
        if (component != null) {
            component.redrawAll();
        }
    }

    /**
     * Removes all full lines. All lines above a removed line will be
     * moved downward one step, and a new empty line will be added at
     * the top. After removing all full lines, the component will be
     * repainted.
     *
     * @see #hasFullLines
     */
    public void removeFullLines() {
        boolean repaint = false;

        // Remove full lines
        for (int y = height - 1; y >= 0; y--) {
            if (isLineFull(y)) {
                removeLine(y);
                removedLines++;
                repaint = true;
                y++;
            }
        }

        // Repaint if necessary
        if (repaint && component != null) {
            component.redrawAll();
        }
    }

    /**
     * Removes a single line. All lines above are moved down one step,
     * and a new empty line is added at the top. No repainting will be
     * done after removing the line.
     *
     * @param y         the vertical position (0 <= y < height)
     */
    private void removeLine(int y) {
        if (y < 0 || y >= height) {
            return;
        }
        for (; y > 0; y--) {
            for (int x = 0; x < width; x++) {
                matrix[y][x] = matrix[y - 1][x];
            }
        }
        for (int x = 0; x < width; x++) {
            matrix[0][x] = null;
        }
    }

    /**
     * Updates the graphical component. Any squares previously changed
     * will be repainted by this method.
     */
    public void update() {
        component.redraw();
    }

    /**
     * The graphical component that paints the square board. This is
     * implemented as an inner class in order to better abstract the
     * detailed information that must be sent between the square board
     * and its graphical representation.
     */
    private class SquareBoardComponent extends JComponent {

        /**
         * The component size. If the component has been resized, that
         * will be detected when the paint method executes. If this
         * value is set to null, the component dimensions are unknown.
         */
        private Dimension size = null;
        /**
         * The component insets. The inset values are used to create a
         * border around the board to compensate for a skewed aspect
         * ratio. If the component has been resized, the insets values
         * will be recalculated when the paint method executes.
         */
        private Insets insets = new Insets(0, 0, 0, 0);
        /**
         * The square size in pixels. This value is updated when the
         * component size is changed, i.e. when the <code>size</code>
         * variable is modified.
         */
        private Dimension squareSize = new Dimension(0, 0);
        /**
         * An image used for double buffering. The board is first
         * painted onto this image, and that image is then painted
         * onto the real surface in order to avoid making the drawing
         * process visible to the user. This image is recreated each
         * time the component size changes.
         */
        private Image bufferImage = null;
        /**
         * A clip boundary buffer rectangle. This rectangle is used
         * when calculating the clip boundaries, in order to avoid
         * allocating a new clip rectangle for each board square.
         */
        private Rectangle bufferRect = new Rectangle();
        /**
         * The board message color.
         */
        private Color messageColor = Color.white;
        /**
         * A lookup table containing lighter versions of the colors.
         * This table is used to avoid calculating the lighter
         * versions of the colors for each and every square drawn.
         */
        private Hashtable lighterColors = new Hashtable();
        /**
         * A lookup table containing darker versions of the colors.
         * This table is used to avoid calculating the darker
         * versions of the colors for each and every square drawn.
         */
        private Hashtable darkerColors = new Hashtable();
        /**
         * A flag set when the component has been updated.
         */
        private boolean updated = true;
        /**
         * A bounding box of the squares to update. The coordinates
         * used in the rectangle refers to the square matrix.
         */
        private Rectangle updateRect = new Rectangle();

        /**
         * Creates a new square board component.
         */
        public SquareBoardComponent() {
            setBackground(Configuration.getColor("board.background",
                    "#000000"));
            messageColor = Configuration.getColor("board.message",
                    "#ffffff");
        }

        /**
         * Adds a square to the set of squares in need of redrawing.
         *
         * @param x     the horizontal position (0 <= x < width)
         * @param y     the vertical position (0 <= y < height)
         */
        public void invalidateSquare(int x, int y) {
            if (updated) {
                updated = false;
                updateRect.x = x;
                updateRect.y = y;
                updateRect.width = 0;
                updateRect.height = 0;
            } else {
                if (x < updateRect.x) {
                    updateRect.width += updateRect.x - x;
                    updateRect.x = x;
                } else if (x > updateRect.x + updateRect.width) {
                    updateRect.width = x - updateRect.x;
                }
                if (y < updateRect.y) {
                    updateRect.height += updateRect.y - y;
                    updateRect.y = y;
                } else if (y > updateRect.y + updateRect.height) {
                    updateRect.height = y - updateRect.y;
                }
            }
        }

        /**
         * Redraws all the invalidated squares. If no squares have
         * been marked as in need of redrawing, no redrawing will
         * occur.
         */
        public void redraw() {
            Graphics g;

            if (!updated) {
                updated = true;
                g = getGraphics();
                if (g == null) {
                    return;
                }
                g.setClip(insets.left + updateRect.x * squareSize.width,
                        insets.top + updateRect.y * squareSize.height,
                        (updateRect.width + 1) * squareSize.width,
                        (updateRect.height + 1) * squareSize.height);
                paint(g);
            }
        }

        /**
         * Redraws the whole component.
         */
        public void redrawAll() {
            Graphics g;

            updated = true;
            g = getGraphics();
            if (g == null) {
                return;
            }
            g.setClip(insets.left,
                    insets.top,
                    width * squareSize.width,
                    height * squareSize.height);
            paint(g);
        }

        /**
         * Returns true as this component is double buffered.
         *
         * @return true as this component is double buffered
         */
        @Override
        public boolean isDoubleBuffered() {
            return true;
        }

        /**
         * Returns the preferred size of this component.
         *
         * @return the preferred component size
         */
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(width * 20, height * 20);
        }

        /**
         * Returns the minimum size of this component.
         *
         * @return the minimum component size
         */
        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        /**
         * Returns the maximum size of this component.
         *
         * @return the maximum component size
         */
        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        /**
         * Returns a lighter version of the specified color. The
         * lighter color will looked up in a hashtable, making this
         * method fast. If the color is not found, the ligher color
         * will be calculated and added to the lookup table for later
         * reference.
         *
         * @param c     the base color
         *
         * @return the lighter version of the color
         */
        private Color getLighterColor(Color c) {
            Color lighter;

            lighter = (Color) lighterColors.get(c);
            if (lighter == null) {
                lighter = c.brighter().brighter();
                lighterColors.put(c, lighter);
            }
            return lighter;
        }

        /**
         * Returns a darker version of the specified color. The
         * darker color will looked up in a hashtable, making this
         * method fast. If the color is not found, the darker color
         * will be calculated and added to the lookup table for later
         * reference.
         *
         * @param c     the base color
         *
         * @return the darker version of the color
         */
        private Color getDarkerColor(Color c) {
            Color darker;

            darker = (Color) darkerColors.get(c);
            if (darker == null) {
                darker = c.darker().darker();
                darkerColors.put(c, darker);
            }
            return darker;
        }

        /**
         * Paints this component indirectly. The painting is first
         * done to a buffer image, that is then painted directly to
         * the specified graphics context.
         *
         * @param g     the graphics context to use
         */
        @Override
        public synchronized void paint(Graphics g) {
            Graphics bufferGraphics;
            Rectangle rect;

            // Handle component size change
            if (size == null || !size.equals(getSize())) {
                size = getSize();
                squareSize.width = size.width / width;
                squareSize.height = size.height / height;

                //if (squareSize.width <= squareSize.height) {
                //    squareSize.height = squareSize.width;
                //} else {
                //    squareSize.width = squareSize.height;
                //}

                insets.left = (size.width - width * squareSize.width) / 2;
                insets.right = insets.left;
                insets.top = 0;
                insets.bottom = size.height - height * squareSize.height;
                bufferImage = createImage(width * squareSize.width,
                        height * squareSize.height);
            }

            // Paint component in buffer image
            rect = g.getClipBounds();
            bufferGraphics = bufferImage.getGraphics();
            bufferGraphics.setClip(rect.x - insets.left,
                    rect.y - insets.top,
                    rect.width,
                    rect.height);
            doPaintComponent(bufferGraphics);

            // Paint image buffer
            g.drawImage(bufferImage,
                    insets.left,
                    insets.top,
                    getBackground(),
                    null);
        }

        /**
         * Paints this component directly. All the squares on the
         * board will be painted directly to the specified graphics
         * context.
         *
         * @param g     the graphics context to use
         */
        private void doPaintComponent(Graphics g) {

            // Paint background
            g.setColor(getBackground());
            g.fillRect(0,
                    0,
                    width * squareSize.width,
                    height * squareSize.height);

            // Paint squares
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (matrix[y][x] != null) {
                        paintSquare(g, x, y);
                    }
                }
            }

            // Paint message
            if (message != null) {
                paintMessage(g, message);
            }
        }

        /**
         * Paints a single board square. The specified position must
         * contain a color object.
         *
         * @param g     the graphics context to use
         * @param x     the horizontal position (0 <= x < width)
         * @param y     the vertical position (0 <= y < height)
         */
        private void paintSquare(Graphics g, int x, int y) {
            Color color = matrix[y][x];
            int xMin = x * squareSize.width;
            int yMin = y * squareSize.height;
            int xMax = xMin + squareSize.width - 1;
            int yMax = yMin + squareSize.height - 1;
            int i;

            // Skip drawing if not visible
            bufferRect.x = xMin;
            bufferRect.y = yMin;
            bufferRect.width = squareSize.width;
            bufferRect.height = squareSize.height;
            if (!bufferRect.intersects(g.getClipBounds())) {
                return;
            }

            // Fill with base color
            g.setColor(color);
            g.fillRect(xMin, yMin, squareSize.width, squareSize.height);

            // Draw brighter lines
            g.setColor(getLighterColor(color));
            for (i = 0; i < squareSize.width / 10; i++) {
                g.drawLine(xMin + i, yMin + i, xMax - i, yMin + i);
                g.drawLine(xMin + i, yMin + i, xMin + i, yMax - i);
            }

            // Draw darker lines
            g.setColor(getDarkerColor(color));
            for (i = 0; i < squareSize.width / 10; i++) {
                g.drawLine(xMax - i, yMin + i, xMax - i, yMax - i);
                g.drawLine(xMin + i, yMax - i, xMax - i, yMax - i);
            }
        }

        /**
         * Paints a board message. The message will be drawn at the
         * center of the component.
         *
         * @param g     the graphics context to use
         * @param msg   the string message
         */
        private void paintMessage(Graphics g, String msg) {
            int fontWidth;
            int offset;
            int x;
            int y;

            // Find string font width
            g.setFont(new Font("SansSerif", Font.BOLD, squareSize.width + 4));
            fontWidth = g.getFontMetrics().stringWidth(msg);

            // Find centered position
            x = (width * squareSize.width - fontWidth) / 2;
            y = height * squareSize.height / 2;

            // Draw black version of the string
            offset = squareSize.width / 10;
            g.setColor(Color.black);
            g.drawString(msg, x - offset, y - offset);
            g.drawString(msg, x - offset, y);
            g.drawString(msg, x - offset, y - offset);
            g.drawString(msg, x, y - offset);
            g.drawString(msg, x, y + offset);
            g.drawString(msg, x + offset, y - offset);
            g.drawString(msg, x + offset, y);
            g.drawString(msg, x + offset, y + offset);

            // Draw white version of the string
            g.setColor(messageColor);
            g.drawString(msg, x, y);
        }
    }
}
/*
 * @(#)Game.java
 *
 * This work is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (c) 2003 Per Cederberg. All rights reserved.
 */

/**
 * The Tetris game. This class controls all events in the game and
 * handles all the game logics. The game is started through user
 * interaction with the graphical game component provided by this
 * class.
 *
 * @version  1.2
 * @author   Per Cederberg, per@percederberg.net
 */
class Game extends Object {

    public static final int STATE_GETREADY = 1;
    public static final int STATE_PLAYING = 2;
    public static final int STATE_PAUSED = 3;
    public static final int STATE_GAMEOVER = 4;
    /**
     * The PropertyChangeSupport Object able to register listener and dispatch events to them.
     */
    private final PropertyChangeSupport PCS = new PropertyChangeSupport(this);
    /**
     * The main square board. This board is used for the game itself.
     */
    private final SquareBoard board;
    /**
     * The preview square board. This board is used to display a
     * preview of the figures.
     */
    private final SquareBoard previewBoard = new SquareBoard(5, 5);
    /**
     * The figures used on both boards. All figures are reutilized in
     * order to avoid creating new objects while the game is running.
     * Special care has to be taken when the preview figure and the
     * current figure refers to the same object.
     */
    private Figure[] figures = {
        new Figure(Figure.SQUARE_FIGURE),
        new Figure(Figure.LINE_FIGURE),
        new Figure(Figure.S_FIGURE),
        new Figure(Figure.Z_FIGURE),
        new Figure(Figure.RIGHT_ANGLE_FIGURE),
        new Figure(Figure.LEFT_ANGLE_FIGURE),
        new Figure(Figure.TRIANGLE_FIGURE)
    };
    /**
     * The thread that runs the game. When this variable is set to
     * null, the game thread will terminate.
     */
    private final GameThread thread;
    /**
     * The game level. The level will be increased for every 20 lines
     * removed from the square board.
     */
    private int level = 1;
    /**
     * The current score. The score is increased for every figure that
     * is possible to place on the main board.
     */
    private int score = 0;
    /**
     * The current figure. The figure will be updated when
     */
    private Figure figure = null;
    /**
     * The next figure.
     */
    private Figure nextFigure = null;
    /**
     * The rotation of the next figure.
     */
    private int nextRotation = 0;
    /**
     * The figure preview flag. If this flag is set, the figure
     * will be shown in the figure preview board.
     */
    private boolean preview = true;
    /**
     * The move lock flag. If this flag is set, the current figure
     * cannot be moved. This flag is set when a figure is moved all
     * the way down, and reset when a new figure is displayed.
     */
    private boolean moveLock = false;
    /**
     *
     */
    private int state;

    /**
     * Creates a new Tetris game. The square board will be given
     * the default size of 10x20.
     */
    public Game() {
        this(10, 20);
    }

    /**
     * Creates a new Tetris game. The square board will be given
     * the specified size.
     *
     * @param width     the width of the square board (in positions)
     * @param height    the height of the square board (in positions)
     */
    public Game(int width, int height) {
        board = new SquareBoard(width, height);
        thread = new GameThread();
        handleGetReady();
        board.getComponent().setFocusable(true);
        board.getComponent().addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyEvent(e);
            }
        });
    }

    /**
     * Adds a PropertyChangeListener to this Game.
     *
     * This is the list the Events that can be fired:
     *
     * name: "state"
     * value: new current state (int) one of those: STATE_OVER,STATE_PLAYING,STATE_PAUSED
     * when: fired when the state changes.
     *
     * name: "level"
     * value: current level (int)
     * when: fired when the player moves to the next level.
     *
     * name: "score"
     * value: current score (int)
     * when: fired when the player increases his/her score.
     *
     * name: "lines"
     * value: number of 'removed' lines (int)
     * when: fired when the player removes one or more lines.
     *
     * @param l the property change listener which is going to be notified.
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        PCS.addPropertyChangeListener(l);
    }

    /**
     * Removes this propertyChangeListener
     * @param l the PropertyChangeListener object to remove.
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        PCS.removePropertyChangeListener(l);
    }

    /**
     * Gets the current 'state'.
     * One of the following:
     * STATE_GETREADY,STATE_PLAYING,STATE_PAUSED,STATE_GAMEOVER.
     * @return the current state.
     */
    public int getState() {
        return state;
    }

    /**
     * Gets the current level.
     * @return the current level.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the current score.
     * @return the current score.
     **/
    public int getScore() {
        return score;
    }

    /**
     * Gets the number of lines that have been removed since the game started.
     * @return the number of removed lines.
     */
    public int getRemovedLines() {
        return board.getRemovedLines();
    }

    /**
     * Gets the java.awt.Component for the board.
     * @return the gui component for the board.
     */
    public Component getSquareBoardComponent() {
        return board.getComponent();
    }

    /**
     * Gets the java.awt.Component for the preview board (5x5)
     * @return the gui component for the board.
     */
    public Component getPreviewBoardComponent() {
        return previewBoard.getComponent();
    }

    /**
     * Initializes the game ready if the state is on STATE_GAMEOVER
     * otherwise it does nothing.
     **/
    public void init() {
        if (state == STATE_GAMEOVER) {
            handleGetReady();
        }
    }

    /**
     * Starts the game. (No matter what the current state is)
     **/
    public void start() {
        handleStart();
    }

    /**
     * Pauses the game  if the state is on STATE_PLAYING
     * otherwise it does nothing.
     **/
    public void pause() {
        if (state == STATE_PLAYING) {
            handlePause();
        }
    }

    /**
     * Resumes the game  if the state is on STATE_PAUSED
     * otherwise it does nothing.
     **/
    public void resume() {
        if (state == STATE_PAUSED) {
            handleResume();
        }
    }

    /**
     * Terminates the game. (No matter what the current state is)
     **/
    public void terminate() {
        handleGameOver();
    }

    /**
     * Handles a game start event. Both the main and preview square
     * boards will be reset, and all other game parameters will be
     * reset. Finally the game thread will be launched.
     */
    private void handleStart() {

        // Reset score and figures
        level = 1;
        score = 0;
        figure = null;
        nextFigure = randomFigure();
        nextFigure.rotateRandom();
        nextRotation = nextFigure.getRotation();

        // Reset components
        state = STATE_PLAYING;
        board.setMessage(null);
        board.clear();
        previewBoard.clear();
        handleLevelModification();
        handleScoreModification();

        PCS.firePropertyChange("state", -1, STATE_PLAYING);

        // Start game thread
        thread.reset();
    }

    /**
     * Handles a game over event. This will stop the game thread,
     * reset all figures and print a game over message.
     */
    private void handleGameOver() {

        // Stop game thred
        thread.setPaused(true);

        // Reset figures
        if (figure != null) {
            figure.detach();
        }
        figure = null;
        if (nextFigure != null) {
            nextFigure.detach();
        }
        nextFigure = null;

        // Handle components
        state = STATE_GAMEOVER;
        board.setMessage("Game Over");
        PCS.firePropertyChange("state", -1, STATE_GAMEOVER);
    }

    /**
     * Handles a getReady event.
     * This will print a 'get ready' message on the game board.
     */
    private void handleGetReady() {
        board.setMessage("Get Ready");
        board.clear();
        previewBoard.clear();
        state = STATE_GETREADY;
        PCS.firePropertyChange("state", -1, STATE_GETREADY);
    }

    /**
     * Handles a game pause event. This will pause the game thread and
     * print a pause message on the game board.
     */
    private void handlePause() {
        thread.setPaused(true);
        state = STATE_PAUSED;
        board.setMessage("Paused");
        PCS.firePropertyChange("state", -1, STATE_PAUSED);
    }

    /**
     * Handles a game resume event. This will resume the game thread
     * and remove any messages on the game board.
     */
    private void handleResume() {
        state = STATE_PLAYING;
        board.setMessage(null);
        thread.setPaused(false);
        PCS.firePropertyChange("state", -1, STATE_PLAYING);
    }

    /**
     * Handles a level modification event. This will modify the level
     * label and adjust the thread speed.
     */
    private void handleLevelModification() {
        PCS.firePropertyChange("level", -1, level);
        thread.adjustSpeed();
    }

    /**
     * Handle a score modification event. This will modify the score
     * label.
     */
    private void handleScoreModification() {
        PCS.firePropertyChange("score", -1, score);
    }

    /**
     * Handles a figure start event. This will move the next figure
     * to the current figure position, while also creating a new
     * preview figure. If the figure cannot be introduced onto the
     * game board, a game over event will be launched.
     */
    private void handleFigureStart() {
        int rotation;

        // Move next figure to current
        figure = nextFigure;
        moveLock = false;
        rotation = nextRotation;
        nextFigure = randomFigure();
        nextFigure.rotateRandom();
        nextRotation = nextFigure.getRotation();

        // Handle figure preview
        if (preview) {
            previewBoard.clear();
            nextFigure.attach(previewBoard, true);
            nextFigure.detach();
        }

        // Attach figure to game board
        figure.setRotation(rotation);
        if (!figure.attach(board, false)) {
            previewBoard.clear();
            figure.attach(previewBoard, true);
            figure.detach();
            handleGameOver();
        }
    }

    /**
     * Handles a figure landed event. This will check that the figure
     * is completely visible, or a game over event will be launched.
     * After this control, any full lines will be removed. If no full
     * lines could be removed, a figure start event is launched
     * directly.
     */
    private void handleFigureLanded() {

        // Check and detach figure
        if (figure.isAllVisible()) {
            score += 10;
            handleScoreModification();
        } else {
            handleGameOver();
            return;
        }
        figure.detach();
        figure = null;

        // Check for full lines or create new figure
        if (board.hasFullLines()) {
            board.removeFullLines();
            PCS.firePropertyChange("lines", -1, board.getRemovedLines());
            if (level < 9 && board.getRemovedLines() / 20 > level) {
                level = board.getRemovedLines() / 20;
                handleLevelModification();
            }
        } else {
            handleFigureStart();
        }
    }

    /**
     * Handles a timer event. This will normally move the figure down
     * one step, but when a figure has landed or isn't ready other
     * events will be launched. This method is synchronized to avoid
     * race conditions with other asynchronous events (keyboard and
     * mouse).
     */
    private synchronized void handleTimer() {
        if (figure == null) {
            handleFigureStart();
        } else if (figure.hasLanded()) {
            handleFigureLanded();
        } else {
            figure.moveDown();
        }
    }

    /**
     * Handles a button press event. This will launch different events
     * depending on the state of the game, as the button semantics
     * change as the game changes. This method is synchronized to
     * avoid race conditions with other asynchronous events (timer and
     * keyboard).
     */
    private synchronized void handlePauseOnOff() {
        if (nextFigure == null) {
            handleStart();
        } else if (thread.isPaused()) {
            handleResume();
        } else {
            handlePause();
        }
    }

    /**
     * Handles a keyboard event. This will result in different actions
     * being taken, depending on the key pressed. In some cases, other
     * events will be launched. This method is synchronized to avoid
     * race conditions with other asynchronous events (timer and
     * mouse).
     *
     * @param e         the key event
     */
    private synchronized void handleKeyEvent(KeyEvent e) {
        // Handle start (any key to start !!!)
        if (state == STATE_GETREADY) {
            handleStart();
            return;
        }

        // pause and resume
        if (e.getKeyCode() == KeyEvent.VK_P) {
            handlePauseOnOff();
            return;
        }

        // Don't proceed if stopped or paused
        if (figure == null || moveLock || thread.isPaused()) {
            return;
        }

        // Handle remaining key events
        switch (e.getKeyCode()) {

            case KeyEvent.VK_LEFT:
                figure.moveLeft();
                break;

            case KeyEvent.VK_RIGHT:
                figure.moveRight();
                break;

            case KeyEvent.VK_DOWN:
                figure.moveAllWayDown();
                moveLock = true;
                break;

            case KeyEvent.VK_UP:
            case KeyEvent.VK_SPACE:
                if (e.isControlDown()) {
                    figure.rotateRandom();
                } else if (e.isShiftDown()) {
                    figure.rotateClockwise();
                } else {
                    figure.rotateCounterClockwise();
                }
                break;

            case KeyEvent.VK_S:
                if (level < 9) {
                    level++;
                    handleLevelModification();
                }
                break;

            case KeyEvent.VK_N:
                preview = !preview;
                if (preview && figure != nextFigure) {
                    nextFigure.attach(previewBoard, true);
                    nextFigure.detach();
                } else {
                    previewBoard.clear();
                }
                break;
        }
    }

    /**
     * Returns a random figure. The figures come from the figures
     * array, and will not be initialized.
     *
     * @return a random figure
     */
    private Figure randomFigure() {
        return figures[(int) (Math.random() * figures.length)];
    }

    /**
     * The game time thread. This thread makes sure that the timer
     * events are launched appropriately, making the current figure
     * fall. This thread can be reused across games, but should be set
     * to paused state when no game is running.
     */
    private class GameThread extends Thread {

        /**
         * The game pause flag. This flag is set to true while the
         * game should pause.
         */
        private boolean paused = true;
        /**
         * The number of milliseconds to sleep before each automatic
         * move. This number will be lowered as the game progresses.
         */
        private int sleepTime = 500;

        /**
         * Creates a new game thread with default values.
         */
        public GameThread() {
        }

        /**
         * Resets the game thread. This will adjust the speed and
         * start the game thread if not previously started.
         */
        public void reset() {
            adjustSpeed();
            setPaused(false);
            if (!isAlive()) {
                this.start();
            }
        }

        /**
         * Checks if the thread is paused.
         *
         * @return true if the thread is paused, or
         *         false otherwise
         */
        public boolean isPaused() {
            return paused;
        }

        /**
         * Sets the thread pause flag.
         *
         * @param paused     the new paused flag value
         */
        public void setPaused(boolean paused) {
            this.paused = paused;
        }

        /**
         * Adjusts the game speed according to the current level. The
         * sleeping time is calculated with a function making larger
         * steps initially an smaller as the level increases. A level
         * above ten (10) doesn't have any further effect.
         */
        public void adjustSpeed() {
            sleepTime = 4500 / (level + 5) - 250;
            if (sleepTime < 50) {
                sleepTime = 50;
            }
        }

        /**
         * Runs the game.
         */
        @Override
        public void run() {
            while (thread == this) {
                // Make the time step
                handleTimer();

                // Sleep for some time
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignore) {
                    // Do nothing
                }

                // Sleep if paused
                while (paused && thread == this) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                        // Do nothing
                    }
                }
            }
        }
    }
}
/*
 * @(#)Configuration.java
 *
 * This work is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (c) 2003 Per Cederberg. All rights reserved.
 */

/**
 * A program configuration. This class provides static methods for
 * simplifying the reading of configuration parameters. It also
 * provides some methods for transforming string values into more
 * useful objects.
 *
 * @author   Per Cederberg, per@percederberg.net
 * @version  1.2
 */
class Configuration extends Object {

    /**
     * The internal configuration property values. This lookup table
     * is used to avoid setting configuration parameters in the system
     * properties, as some programs (applets) do not have the security
     * permissions to set system properties.
     */
    private static Hashtable config = new Hashtable();

    /**
     * Returns a configuration parameter value.
     *
     * @param key       the configuration parameter key
     *
     * @return the configuration parameter value, or
     *         null if not set
     */
    public static String getValue(String key) {
        if (config.containsKey(key)) {
            return config.get(key).toString();
        } else {
            try {
                return System.getProperty(key);
            } catch (SecurityException ignore) {
                return null;
            }
        }
    }

    /**
     * Returns a configuration parameter value. If the configuration
     * parameter is not set, a default value will be returned instead.
     *
     * @param key       the configuration parameter key
     * @param def       the default value to use
     *
     * @return the configuration parameter value, or
     *         the default value if not set
     */
    public static String getValue(String key, String def) {
        String value = getValue(key);

        return (value == null) ? def : value;
    }

    /**
     * Sets a configuration parameter value.
     *
     * @param key       the configuration parameter key
     * @param value     the configuration parameter value
     */
    public static void setValue(String key, String value) {
        config.put(key, value);
    }

    /**
     * Returns the color configured for the specified key. The key
     * will be prepended with "tetris.color." and the value will be
     * read from the system properties. The color value must be
     * specified in hexadecimal web format, i.e. in the "#RRGGBB"
     * format. If the default color isn't in a valid format, white
     * will be returned.
     *
     * @param key       the configuration parameter key
     * @param def       the default value
     *
     * @return the color specified in the configuration, or
     *         a default color value
     */
    public static Color getColor(String key, String def) {
        String value = getValue("tetris.color." + key, def);
        Color color;

        color = parseColor(value);
        if (color != null) {
            return color;
        }
        color = parseColor(def);
        if (color != null) {
            return color;
        } else {
            return Color.white;
        }
    }

    /**
     * Parses a web color string. If the color value couldn't be
     * parsed correctly, null will be returned.
     *
     * @param value     the color value to parse
     *
     * @return the color represented by the string, or
     *         null if the string was malformed
     */
    private static Color parseColor(String value) {
        if (!value.startsWith("#")) {
            return null;
        }
        try {
            return new Color(Integer.parseInt(value.substring(1), 16));
        } catch (NumberFormatException ignore) {
            return null;
        }
    }
}
/*
 * @(#)Figure.java
 *
 * This work is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (c) 2003 Per Cederberg. All rights reserved.
 */

/**
 * A class representing a Tetris square figure. Each figure consists
 * of four connected squares in one of seven possible constellations.
 * The figures may be rotated in 90 degree steps and have sideways and
 * downwards movability.<p>
 *
 * Each figure instance can have two states, either attached to a
 * square board or not. When attached, all move and rotation
 * operations are checked so that collisions do not occur with other
 * squares on the board. When not attached, any rotation can be made
 * (and will be kept when attached to a new board).
 *
 * @version  1.2
 * @author   Per Cederberg, per@percederberg.net
 */
class Figure extends Object {

    /**
     * A figure constant used to create a figure forming a square.
     */
    public static final int SQUARE_FIGURE = 1;
    /**
     * A figure constant used to create a figure forming a line.
     */
    public static final int LINE_FIGURE = 2;
    /**
     * A figure constant used to create a figure forming an "S".
     */
    public static final int S_FIGURE = 3;
    /**
     * A figure constant used to create a figure forming a "Z".
     */
    public static final int Z_FIGURE = 4;
    /**
     * A figure constant used to create a figure forming a right angle.
     */
    public static final int RIGHT_ANGLE_FIGURE = 5;
    /**
     * A figure constant used to create a figure forming a left angle.
     */
    public static final int LEFT_ANGLE_FIGURE = 6;
    /**
     * A figure constant used to create a figure forming a triangle.
     */
    public static final int TRIANGLE_FIGURE = 7;
    /**
     * The square board to which the figure is attached. If this
     * variable is set to null, the figure is not attached.
     */
    private SquareBoard board = null;
    /**
     * The horizontal figure position on the board. This value has no
     * meaning when the figure is not attached to a square board.
     */
    private int xPos = 0;
    /**
     * The vertical figure position on the board. This value has no
     * meaning when the figure is not attached to a square board.
     */
    private int yPos = 0;
    /**
     * The figure orientation (or rotation). This value is normally
     * between 0 and 3, but must also be less than the maxOrientation
     * value.
     *
     * @see #maxOrientation
     */
    private int orientation = 0;
    /**
     * The maximum allowed orientation number. This is used to reduce
     * the number of possible rotations for some figures, such as the
     * square figure. If this value is not used, the square figure
     * will be possible to rotate around one of its squares, which
     * gives an erroneous effect.
     *
     * @see #orientation
     */
    private int maxOrientation = 4;
    /**
     * The horizontal coordinates of the figure shape. The coordinates
     * are relative to the current figure position and orientation.
     */
    private int[] shapeX = new int[4];
    /**
     * The vertical coordinates of the figure shape. The coordinates
     * are relative to the current figure position and orientation.
     */
    private int[] shapeY = new int[4];
    /**
     * The figure color.
     */
    private Color color = Color.white;

    /**
     * Creates a new figure of one of the seven predefined types. The
     * figure will not be attached to any square board and default
     * colors and orientations will be assigned.
     *
     * @param type      the figure type (one of the figure constants)
     *
     * @see #SQUARE_FIGURE
     * @see #LINE_FIGURE
     * @see #S_FIGURE
     * @see #Z_FIGURE
     * @see #RIGHT_ANGLE_FIGURE
     * @see #LEFT_ANGLE_FIGURE
     * @see #TRIANGLE_FIGURE
     *
     * @throws IllegalArgumentException if the figure type specified
     *             is not recognized
     */
    public Figure(int type) throws IllegalArgumentException {
        initialize(type);
    }

    /**
     * Initializes the instance variables for a specified figure type.
     *
     * @param type      the figure type (one of the figure constants)
     *
     * @see #SQUARE_FIGURE
     * @see #LINE_FIGURE
     * @see #S_FIGURE
     * @see #Z_FIGURE
     * @see #RIGHT_ANGLE_FIGURE
     * @see #LEFT_ANGLE_FIGURE
     * @see #TRIANGLE_FIGURE
     *
     * @throws IllegalArgumentException if the figure type specified
     *             is not recognized
     */
    private void initialize(int type) throws IllegalArgumentException {

        // Initialize default variables
        board = null;
        xPos = 0;
        yPos = 0;
        orientation = 0;

        // Initialize figure type variables
        switch (type) {
            case SQUARE_FIGURE:
                maxOrientation = 1;
                color = Configuration.getColor("figure.square", "#ffd8b1");
                shapeX[0] = -1;
                shapeY[0] = 0;
                shapeX[1] = 0;
                shapeY[1] = 0;
                shapeX[2] = -1;
                shapeY[2] = 1;
                shapeX[3] = 0;
                shapeY[3] = 1;
                break;
            case LINE_FIGURE:
                maxOrientation = 2;
                color = Configuration.getColor("figure.line", "#ffb4b4");
                shapeX[0] = -2;
                shapeY[0] = 0;
                shapeX[1] = -1;
                shapeY[1] = 0;
                shapeX[2] = 0;
                shapeY[2] = 0;
                shapeX[3] = 1;
                shapeY[3] = 0;
                break;
            case S_FIGURE:
                maxOrientation = 2;
                color = Configuration.getColor("figure.s", "#a3d5ee");
                shapeX[0] = 0;
                shapeY[0] = 0;
                shapeX[1] = 1;
                shapeY[1] = 0;
                shapeX[2] = -1;
                shapeY[2] = 1;
                shapeX[3] = 0;
                shapeY[3] = 1;
                break;
            case Z_FIGURE:
                maxOrientation = 2;
                color = Configuration.getColor("figure.z", "#f4adff");
                shapeX[0] = -1;
                shapeY[0] = 0;
                shapeX[1] = 0;
                shapeY[1] = 0;
                shapeX[2] = 0;
                shapeY[2] = 1;
                shapeX[3] = 1;
                shapeY[3] = 1;
                break;
            case RIGHT_ANGLE_FIGURE:
                maxOrientation = 4;
                color = Configuration.getColor("figure.right", "#c0b6fa");
                shapeX[0] = -1;
                shapeY[0] = 0;
                shapeX[1] = 0;
                shapeY[1] = 0;
                shapeX[2] = 1;
                shapeY[2] = 0;
                shapeX[3] = 1;
                shapeY[3] = 1;
                break;
            case LEFT_ANGLE_FIGURE:
                maxOrientation = 4;
                color = Configuration.getColor("figure.left", "#f5f4a7");
                shapeX[0] = -1;
                shapeY[0] = 0;
                shapeX[1] = 0;
                shapeY[1] = 0;
                shapeX[2] = 1;
                shapeY[2] = 0;
                shapeX[3] = -1;
                shapeY[3] = 1;
                break;
            case TRIANGLE_FIGURE:
                maxOrientation = 4;
                color = Configuration.getColor("figure.triangle", "#a4d9b6");
                shapeX[0] = -1;
                shapeY[0] = 0;
                shapeX[1] = 0;
                shapeY[1] = 0;
                shapeX[2] = 1;
                shapeY[2] = 0;
                shapeX[3] = 0;
                shapeY[3] = 1;
                break;
            default:
                throw new IllegalArgumentException("No figure constant: "
                        + type);
        }
    }

    /**
     * Checks if this figure is attached to a square board.
     *
     * @return true if the figure is already attached, or
     *         false otherwise
     */
    public boolean isAttached() {
        return board != null;
    }

    /**
     * Attaches the figure to a specified square board. The figure
     * will be drawn either at the absolute top of the board, with
     * only the bottom line visible, or centered onto the board. In
     * both cases, the squares on the new board are checked for
     * collisions. If the squares are already occupied, this method
     * returns false and no attachment is made.<p>
     *
     * The horizontal and vertical coordinates will be reset for the
     * figure, when centering the figure on the new board. The figure
     * orientation (rotation) will be kept, however. If the figure was
     * previously attached to another board, it will be detached from
     * that board before attaching to the new board.
     *
     * @param board     the square board to attach to
     * @param center    the centered position flag
     *
     * @return true if the figure could be attached, or
     *         false otherwise
     */
    public boolean attach(SquareBoard board, boolean center) {
        int newX;
        int newY;
        int i;

        // Check for previous attachment
        if (isAttached()) {
            detach();
        }

        // Reset position (for correct controls)
        xPos = 0;
        yPos = 0;

        // Calculate position
        newX = board.getBoardWidth() / 2;
        if (center) {
            newY = board.getBoardHeight() / 2;
        } else {
            newY = 0;
            for (i = 0; i < shapeX.length; i++) {
                if (getRelativeY(i, orientation) - newY > 0) {
                    newY = -getRelativeY(i, orientation);
                }
            }
        }

        // Check position
        this.board = board;
        if (!canMoveTo(newX, newY, orientation)) {
            this.board = null;
            return false;
        }

        // Draw figure
        xPos = newX;
        yPos = newY;
        paint(color);
        board.update();

        return true;
    }

    /**
     * Detaches this figure from its square board. The figure will not
     * be removed from the board by this operation, resulting in the
     * figure being left intact.
     */
    public void detach() {
        board = null;
    }

    /**
     * Checks if the figure is fully visible on the square board. If
     * the figure isn't attached to a board, false will be returned.
     *
     * @return true if the figure is fully visible, or
     *         false otherwise
     */
    public boolean isAllVisible() {
        if (!isAttached()) {
            return false;
        }
        for (int i = 0; i < shapeX.length; i++) {
            if (yPos + getRelativeY(i, orientation) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the figure has landed. If this method returns true,
     * the moveDown() or the moveAllWayDown() methods should have no
     * effect. If no square board is attached, this method will return
     * true.
     *
     * @return true if the figure has landed, or false otherwise
     */
    public boolean hasLanded() {
        return !isAttached() || !canMoveTo(xPos, yPos + 1, orientation);
    }

    /**
     * Moves the figure one step to the left. If such a move is not
     * possible with respect to the square board, nothing is done. The
     * square board will be changed as the figure moves, clearing the
     * previous cells. If no square board is attached, nothing is
     * done.
     */
    public void moveLeft() {
        if (isAttached() && canMoveTo(xPos - 1, yPos, orientation)) {
            paint(null);
            xPos--;
            paint(color);
            board.update();
        }
    }

    /**
     * Moves the figure one step to the right. If such a move is not
     * possible with respect to the square board, nothing is done. The
     * square board will be changed as the figure moves, clearing the
     * previous cells. If no square board is attached, nothing is
     * done.
     */
    public void moveRight() {
        if (isAttached() && canMoveTo(xPos + 1, yPos, orientation)) {
            paint(null);
            xPos++;
            pa
