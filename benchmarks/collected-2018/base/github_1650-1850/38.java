// https://searchcode.com/api/result/103557545/

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.LayoutManager;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Scrollbar;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;

public class MDApplet extends Applet implements Runnable {

    /**
     * Serial version.
     */
    private static final long serialVersionUID = 1L;
    
    int Nmax = 1000; // maximum number of molecules
    int N = 0; // current number of molecules
    int canvasWidth = 500; // canvas width in pixels (html must set compatible
                           // applet size)
    int pixelsPerUnit; // number of pixels in one distance unit
    double boxWidth; // box width in natural units
    double t = 0; // clock time in natural units
    double dt = 0.02;
    double wallStiffness = 50; // "spring constant" for bouncing off walls
    double forceCutoff = 3; // distance beyond which we don't bother to compute
                            // the force
    double forceCutoff2 = forceCutoff * forceCutoff;
    double pEatCutoff = 4 * (Math.pow(forceCutoff, -12) - Math.pow(forceCutoff, -6));
    double kE, pE, tempPE, gE, totalE; // kinetic, potential, gravitational, and
                                       // total energy
    double xcm, ycm, px, py, Lcm, Icm; // cm coordinates, momentum, angular
                                       // momentum, moment of inertia
    double pressure;
    double totalT, totalP; // accumulated temperature and pressure
    long sampleCount; // number of values accumulated in totalT and totalP
    double gravity = 0; // local gravitational constant in natural units

    // here are the arrays of molecule positions, velocities, and accelerations:
    double[] x = new double[Nmax];
    double[] y = new double[Nmax];
    double[] vx = new double[Nmax];
    double[] vy = new double[Nmax];
    double[] ax = new double[Nmax];
    double[] ay = new double[Nmax];
    Color[] mColor = new Color[Nmax]; // colors for "random" setting

    int fixedCount = 0; // number of molecules that are fixed in space
    int[] fixedList = new int[Nmax]; // list of indices of fixed molecules

    int maxBonds = Nmax * 3; // maximum number of bonds
    int[] bondList = new int[maxBonds * 2]; // list of bonded pairs, with entry
                                            // 0 bonded to 1,
                                            // 2 to 3, 4 to 5, etc.
    int bondCount = 0; // current number of bonds
    double bondStrength = 5.0; // spring constant for bonds (way too low to be
                               // realistic--
                               // a realistic value would be about 50,000 but
                               // this would
                               // make the time scale for vibrations way too
                               // short)

    double bondEnergyOffset = 1.97246; // we subtract this so min. energy of
                                       // bonded pair is 0

    double pullStrength = 1.0; // spring constant when we're manually pulling a
                               // molecule

    MDCanvas theCanvas;
    Button startButton;
    Button presetButton;
    PopupMenu presetMenu;
    MDScroller nScroll, sizeScroll, gravityScroll, dtScroll, speedScroll;
    Canvas dataCanvas;
    Frame dataFrame, detailFrame, alertFrame; // auxilliary windows that come up
                                              // as needed
    TextArea dataPane, detailPane;
    Choice autoRecordChoice; // popup menu for auto-record interval
    double lastAutoRecordTime = 0; // simulation time when stats were last
                                   // auto-recorded
    Checkbox safetyCheck;

    int presetMax = 100; // maximum number of preset configurations (100 is
                         // actually too many)
    int presetCount = 0; // number of preset configurations
    String[] presetName = new String[presetMax];
    MenuItem[] presetItem = new MenuItem[presetMax];
    StringBuffer[] presetData = new StringBuffer[presetMax]; // text data for
                                                             // preset
                                                             // configurations

    Choice mColorChoice;
    String[] mColorName = { "Purple ", "Green", "Yellow", "Orange", "Red", "Magenta", "Cyan", "Blue", "Forest",
            "Black", "White" };
    // trailing space in first entry is to ensure enough space for others
    Color[] mColorList = { new Color(100, 0, 200), Color.GREEN, Color.YELLOW, new Color(255, 128, 0), Color.RED,
            Color.MAGENTA, Color.CYAN, Color.BLUE, new Color(0, 100, 50), Color.BLACK, Color.WHITE };

    Choice bgChoice;
    String[] bgColorName = { "White", "Beige", "Pink", "Sky", "Navy", "Brown", "Gray", "Black" };
    Color[] bgColor = { Color.WHITE, new Color(255, 245, 230), new Color(255, 225, 245), new Color(230, 230, 255),
            new Color(0, 0, 60), new Color(50, 0, 0), new Color(60, 60, 60), Color.BLACK };
    // (Both of the preceding color lists can be expanded as desired, with no
    // further changes needed.)

    final DecimalFormat twoPlaces = new DecimalFormat("0.00");
    final DecimalFormat threePlaces = new DecimalFormat("0.000");
    final DecimalFormat fourPlaces = new DecimalFormat("0.0000");
    final DecimalFormat sixPlaces = new DecimalFormat("0.000000");
    final String lineSeparator = System.getProperty("line.separator");

    boolean running = false; // true when simulation is running
    boolean reinit = false; // true if "Initialize" button has been pressed
    boolean pulling = false; // true when mouse is pulling on an atom
    int pulledMolecule; // index of atom being pulled
    boolean mouseInCanvas; // true if mouse location is within canvas
    int moleculeUnderMouse = -1; // index of molecule under mouse (-1 if none)
    double mouseX, mouseY; // mouse location in simulation units
    int learningLevel = 0; // used for startup messages
    int selectedMolecule = 0; // index of selected molecule
    boolean selectedDifferent = false; // true if selected molecule gets a
                                       // different color
    int mColorIndex = 0; // index of molecule color in popup menu
    Label mColorChoiceLabel = new Label("Molecules:"); // label on popup menu
                                                       // (can change)
    boolean testing = false; // true when we're in testing/debugging mode
    double stepsPerSecond = 0; // to measure performance
    int stepsPerFrame = 0; // to measure performance

    // int pairsInteracting = 0; // (diagnostic)

    // Exceedingly long init method, mostly to set up the GUI. Note that the
    // html file must
    // specify dimensions for the applet that are big enough to hold everything.
    public void init() {

        // main elements and layout:
        setLayout(new BorderLayout()); // we'll use only CENTER, EAST, and SOUTH
                                       // positions
        Color marginColor = Color.lightGray;
        Panel leftPanel = new Panel();
        leftPanel.setBackground(marginColor);
        add(leftPanel, BorderLayout.CENTER);
        theCanvas = new MDCanvas();
        leftPanel.add(theCanvas);
        Panel controlPanel = new Panel();
        add(controlPanel, BorderLayout.EAST);
        controlPanel.setBackground(marginColor);
        controlPanel.setLayout(new MDControlLayout());

        // narrow canvas across the bottom for displaying data:
        dataCanvas = new Canvas() {
            public Dimension getPreferredSize() {
                return new Dimension(0, 18);
            }

            public void paint(Graphics g) {
                if ((!running) && mouseInCanvas) {
                    if (moleculeUnderMouse > -1) {
                        g.drawString(
                                "Atom " + (moleculeUnderMouse) + ":  x = " + fourPlaces.format(x[moleculeUnderMouse])
                                        + ", y = " + fourPlaces.format(y[moleculeUnderMouse]) + ", vx = "
                                        + fourPlaces.format(vx[moleculeUnderMouse]) + ", vy = "
                                        + fourPlaces.format(vy[moleculeUnderMouse]), 10, 12);
                    } else {
                        g.drawString(
                                "Mouse is at x = " + fourPlaces.format(mouseX) + ", y = " + fourPlaces.format(mouseY),
                                10, 12);
                    }
                } else {
                    g.drawString("t = " + threePlaces.format(t) + ", T = " + fourPlaces.format(totalT / sampleCount)
                            + ", P = " + fourPlaces.format(totalP / sampleCount) + ", E = " + fourPlaces.format(totalE)
                            + ", KE = " + fourPlaces.format(kE) + ", PE = " + fourPlaces.format(pE) + ", GE = "
                            + fourPlaces.format(gE), 10, 12);
                }
            }
        };
        add(dataCanvas, BorderLayout.SOUTH);
        dataCanvas.setBackground(marginColor);

        // testing/debugging kludge: shift-click at left end of dataCanvas to
        // toggle testing mode
        dataCanvas.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if ((e.isShiftDown()) && (e.getX() < 10)) {
                    testing = !testing;
                    theCanvas.repaint();
                }
            }
        });

        // here come the buttons...
        Panel startPanel = new Panel();
        startPanel.setLayout(new GridLayout(1, 2));
        controlPanel.add(startPanel);

        startButton = new Button("Start");
        startPanel.add(startButton);
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                running = !running;
                if (running)
                    startButton.setLabel("Pause");
                else
                    startButton.setLabel("Resume");
                learningLevel++;
            }
        });

        Button stepButton = new Button("Step");
        startPanel.add(stepButton);
        stepButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                step();
            }
        });

        Panel slowfastPanel = new Panel();
        slowfastPanel.setLayout(new GridLayout(1, 2));
        controlPanel.add(slowfastPanel);

        Button slowButton = new Button("Slower");
        slowfastPanel.add(slowButton);
        // horrible kludge to work around bug that ActionEvent.getModifiers
        // always returns 0:
        slowButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.isShiftDown()) {
                    speedMultiply(1.0 / 1.01);
                } else {
                    speedMultiply(1.0 / 1.1);
                }
            }
        });
        slowButton.addKeyListener(new KeyAdapter() { // if this button has
                                                     // focus, typing space
                                                     // activates
                    public void keyTyped(KeyEvent e) {
                        if (e.getKeyChar() == ' ')
                            speedMultiply(1.0 / 1.1);
                    }
                });
        /*
         * slowButton.addActionListener(new ActionListener() { public void
         * actionPerformed(ActionEvent e) { if ((e.getModifiers() &
         * ActionEvent.SHIFT_MASK) != 0) { // doesn't work!
         * speedMultiply(1.0/1.02); } else { speedMultiply(1.0/1.1); } }});
         */

        Button fastButton = new Button("Faster");
        slowfastPanel.add(fastButton);
        // same kludge as above:
        fastButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.isShiftDown()) {
                    speedMultiply(1.01);
                } else {
                    speedMultiply(1.1);
                }
            }
        });
        fastButton.addKeyListener(new KeyAdapter() { // if this button has
                                                     // focus, typing space
                                                     // activates
                    public void keyTyped(KeyEvent e) {
                        if (e.getKeyChar() == ' ')
                            speedMultiply(1.1);
                    }
                });
        /*
         * fastButton.addActionListener(new ActionListener() { public void
         * actionPerformed(ActionEvent e) { if ((e.getModifiers() &
         * ActionEvent.SHIFT_MASK) != 0) { speedMultiply(1.02); } else {
         * speedMultiply(1.1); } }});
         */

        Panel freezereversePanel = new Panel();
        freezereversePanel.setLayout(new GridLayout(1, 2));
        controlPanel.add(freezereversePanel);

        Button freezeButton = new Button("Freeze");
        freezereversePanel.add(freezeButton);
        freezeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                speedMultiply(0);
            }
        });

        Button reverseButton = new Button("Reverse");
        freezereversePanel.add(reverseButton);
        reverseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                speedMultiply(-1);
            }
        });

        Panel initPanel = new Panel();
        initPanel.setLayout(new GridLayout(1, 2));
        controlPanel.add(initPanel);

        presetButton = new Button("Presets");
        initPanel.add(presetButton);
        presetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                presetMenu.show(presetButton, -80, 0); // i don't see how to
                                                       // determine the menu's
                                                       // width!
                learningLevel = 2;
            }
        });
        presetMenu = new PopupMenu();        
        try {
            // Read in the preset data. This code is not robust and will fail if
            // the file isn't
            // there or if it's formatted incorrectly. There must be at least
            // one preset configuration
            // in the file, and there can't be more than presetMax. Each preset
            // configuration begins
            // with a line containing a colon. Text before the colon is ignored,
            // and a unique name
            // must come immediately after the colon. This name is used as the
            // menu entry. The data
            // then follows, in the same format as displayed in the
            // configuration (detail) window.
            URL fileAddr = new URL(getCodeBase().toString() + "MDPresets.txt");
            BufferedReader presetFile = new BufferedReader(new InputStreamReader(fileAddr.openStream()));
            String nextLine;
            while ((nextLine = presetFile.readLine()) != null) {
                int colonPosition = nextLine.indexOf(":");
                if (colonPosition > -1) { // if it's a header line for a new
                                          // item...
                    presetName[presetCount] = nextLine.substring(colonPosition + 1);
                    presetItem[presetCount] = new MenuItem(presetName[presetCount]);
                    presetMenu.add(presetItem[presetCount]);                    
                    presetData[presetCount] = new StringBuffer("Header line\n");
                    presetCount++;
                } else { // otherwise it's a data line so just append the line
                         // for later parsing
                    presetData[presetCount - 1].append(nextLine + "\n");
                }
            }
            presetFile.close();
        } catch (IOException e) {
            presetButton.setEnabled(false);
        }
        add(presetMenu);

        Button initButton = new Button("Restart");
        initPanel.add(initButton);
        initButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reinit = true;
                learningLevel = 2;
            }
        });

        Panel bondAndSavePanel = new Panel();
        bondAndSavePanel.setLayout(new GridLayout(1, 2));
        controlPanel.add(bondAndSavePanel);

        Button bondButton = new Button("Make bonds");
        bondAndSavePanel.add(bondButton);
        // same kludge as for Slower and Faster buttons:
        bondButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.isShiftDown()) {
                    bondCount = 0; // delete all bonds if shift key is down
                    reset();
                    theCanvas.repaint();
                } else {
                    makeNeighborBonds();
                }
            }
        });
        bondButton.addKeyListener(new KeyAdapter() { // if this button has
                                                     // focus, typing space
                                                     // activates
                    public void keyTyped(KeyEvent e) {
                        if (e.getKeyChar() == ' ')
                            makeNeighborBonds();
                    }
                });

        Button detailButton = new Button("Save state");
        bondAndSavePanel.add(detailButton);
        detailButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {                
                learningLevel = 2;
            }
        });

        Panel statPanel = new Panel();
        statPanel.setLayout(new GridLayout(1, 2));
        controlPanel.add(statPanel);

        Button resetButton = new Button("Reset stats");
        statPanel.add(resetButton);
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });

        Button writeButton = new Button("Write stats");
        statPanel.add(writeButton);
        writeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dataFrame.setVisible(true);
                writeStats();
            }
        });

        // Statistics window, activated by "Write stats" button:
        dataFrame = new Frame("MD Statistics");
        dataFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dataFrame.setVisible(false);
            }
        });
        dataPane = new TextArea("", 10, 72);
        dataPane.setEditable(false);
        dataFrame.add(dataPane);
        Panel statControlPanel = new Panel();
        dataFrame.add(statControlPanel, BorderLayout.NORTH);
        statControlPanel.add(new Label("Auto-record interval:"));
        autoRecordChoice = new Choice();
        autoRecordChoice.addItem("None ");
        autoRecordChoice.addItem("1    "); // these values are hard-coded in
                                           // computeStats method
        autoRecordChoice.addItem("10   ");
        autoRecordChoice.addItem("100  ");
        autoRecordChoice.addItem("1000 ");
        autoRecordChoice.addItem("10000");
        statControlPanel.add(autoRecordChoice);
        statControlPanel.add(new Label("    "));
        Button clearButton = new Button("Clear");
        statControlPanel.add(clearButton);
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearStats();
            }
        });
        dataFrame.pack();

        // Configuration window, activated by "Show configuration" button:
        detailFrame = new Frame("MD Configuration");
        detailFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                detailFrame.setVisible(false);
            }
        });
        detailPane = new TextArea("", 25, 50);
        detailPane.setEditable(true);
        detailFrame.add(detailPane);
        Panel readButtonPanel = new Panel();
        detailFrame.add(readButtonPanel, BorderLayout.NORTH);
        Button readButton = new Button("Read data into simulation");
        readButtonPanel.add(readButton);
        readButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {                
            }
        });
        detailFrame.pack();

        // Alert window to handle run-away instability:
        alertFrame = new Frame("MD Alert");
        alertFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                alertFrame.setVisible(false);
            }
        });
        alertFrame.setResizable(false);
        Panel alertTextPanel = new Panel();
        alertTextPanel.setLayout(new GridLayout(0, 1));
        alertFrame.add(alertTextPanel);
        alertTextPanel.add(new Label("")); // blank line
        Label theBadNews = new Label("Oops!  The calculation has become unstable.");
        theBadNews.setForeground(Color.red);
        alertTextPanel.add(theBadNews);
        alertTextPanel.add(new Label(""));
        alertTextPanel.add(new Label("Avoid placing or resizing the molecules"));
        alertTextPanel.add(new Label("so they overlap.  Avoid over-stretching bonds."));
        alertTextPanel.add(new Label(""));
        alertTextPanel.add(new Label("The higher the temperature, the smaller the"));
        alertTextPanel.add(new Label("time step needed to run stably.  Safety mode"));
        alertTextPanel.add(new Label("tries to reduce the time step as needed, but"));
        alertTextPanel.add(new Label("can't always keep up with sudden changes."));
        alertTextPanel.add(new Label(""));
        alertFrame.add(new Label("   "), BorderLayout.WEST); // space at left
        alertFrame.add(new Label("   "), BorderLayout.EAST); // space at right
        Panel alertButtonPanel = new Panel();
        alertFrame.add(alertButtonPanel, BorderLayout.SOUTH);
        Button alertRestartButton = new Button("Restart");
        alertButtonPanel.add(alertRestartButton);
        alertRestartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reinit = true;
                alertFrame.setVisible(false);
            }
        });
        alertFrame.pack();

        // now for the scrollbars:
        nScroll = new MDScroller("Number of atoms = ", 1, 1000, 1, 200);
        controlPanel.add(nScroll);

        sizeScroll = new MDScroller("Atom size = ", 1, 100, 1, 16);
        controlPanel.add(sizeScroll);

        gravityScroll = new MDScroller("Gravity = ", 0, 1.0, 0.001, 0);
        controlPanel.add(gravityScroll);

        dtScroll = new MDScroller("Time step = ", 0.001, 0.050, 0.001, dt);
        controlPanel.add(dtScroll);

        speedScroll = new MDScroller("Animation speed = ", 0, 100, 1, 50);
        controlPanel.add(speedScroll);

        // kludge to add a bit of vertical space:
        Component spacer = new Component() {
            public Dimension getPreferredSize() {
                return new Dimension(2, 2);
            }
        };
        controlPanel.add(spacer);

        // popup menus to select the colors:
        Panel mColorChoicePanel = new Panel();
        mColorChoicePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 1));
        controlPanel.add(mColorChoicePanel);
        // Label mColorChoiceLabel = new Label("Molecules:"); // promoted to
        // global variable
        mColorChoicePanel.add(mColorChoiceLabel);
        // hidden feature: clicking on this label changes it (and its function):
        mColorChoiceLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                selectedDifferent = !selectedDifferent;
                if (selectedDifferent) {
                    mColorChoiceLabel.setText("Selected:");
                } else {
                    mColorChoiceLabel.setText("Molecules:");
                    mColorChoice.select(mColorIndex); // set popup back to
                                                      // remembered value
                    theCanvas.repaint();
                }
            }
        });
        mColorChoice = new Choice();
        for (int i = 0; i < mColorList.length; i++) {
            mColorChoice.addItem(mColorName[i]);
        }
        mColorChoice.addItem("Random"); // these must be the last 2 entries; see
        mColorChoice.addItem("By speed"); // drawOffScreenImage method in
                                          // MDCanvas class
        mColorChoice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (!selectedDifferent)
                    mColorIndex = mColorChoice.getSelectedIndex();
                // save index value only if we're talking about the rest of the
                // molecules
                theCanvas.repaint();
            }
        });
        mColorChoicePanel.add(mColorChoice);

        Panel bgChoicePanel = new Panel();
        bgChoicePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 1));
        controlPanel.add(bgChoicePanel);
        bgChoicePanel.add(new Label("Background:"));
        bgChoice = new Choice();
        for (int i = 0; i < bgColor.length; i++) {
            bgChoice.addItem(bgColorName[i]);
        }
        bgChoice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                theCanvas.repaint();
            }
        });
        bgChoicePanel.add(bgChoice);

        // safety mode check box:
        Panel safetyCheckPanel = new Panel();
        safetyCheckPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 1));
        controlPanel.add(safetyCheckPanel);
        safetyCheck = new Checkbox("Safety mode", true);
        safetyCheckPanel.add(safetyCheck);
        // GUI is done!

        // place the initial molecules:
        initialize();

        // write headings in the statistics window:
        clearStats();

        // start a new thread to run the simulation:
        Thread t = new Thread(this);
        t.start();

    } // end of init method

    // initialize the simulation by adding molecules one at a time:
    void initialize() {
        pixelsPerUnit = (int) Math.round(sizeScroll.getValue());
        boxWidth = canvasWidth * 1.0 / pixelsPerUnit;
        int n = (int) Math.round(nScroll.getValue());
        N = 0;
        for (int i = 0; i < n; i++)
            addMolecule();
        nScroll.setValue(N); // in case there wasn't room for them all
        dt = 0.02;
        dtScroll.setValue(dt);
        bondCount = 0;
        fixedCount = 0;
        computeAccelerations();
        reset();
        theCanvas.repaint();
    }

    // clear the statistics window and write the headings:
    void clearStats() {
        dataPane.setText("Molecular Dynamics Statistics" + lineSeparator);
        dataPane.append("t\tN\tV\tT\tP\tE\tKE\tPE\tGE\tpx\tpy\tLcm\tIcm" + lineSeparator);
    }

    // write one line of statistics in the statistics window:
    synchronized void writeStats() {
        dataPane.append(threePlaces.format(t) + "\t" + N + "\t" + twoPlaces.format(boxWidth * boxWidth) + "\t"
                + fourPlaces.format(totalT / sampleCount) + "\t" + fourPlaces.format(totalP / sampleCount) + "\t"
                + twoPlaces.format(totalE) + "\t" + twoPlaces.format(kE) + "\t" + twoPlaces.format(pE) + "\t"
                + twoPlaces.format(gE) + "\t" + twoPlaces.format(px) + "\t" + twoPlaces.format(py) + "\t"
                + twoPlaces.format(Lcm) + "\t" + twoPlaces.format(Icm) + lineSeparator);
    }

    // generate a random color for a molecule (actually a random hue with
    // maximum
    // saturation and brightness):
    Color randomColor() {
        return Color.getHSBColor((float) Math.random(), 1, 1);
    }

    // add a molecule at the first available space (if any):
    void addMolecule() {
        final double buffer = 1.3; // minimum space required between molecule
                                   // centers
        final double epsilon = 0.01; // small distance
        if (N == Nmax)
            return; // quit if max number of molecules is already reached
        double xTest = buffer / 2; // start looking for space at lower-left
                                   // corner
        double yTest = buffer / 2;
        while (true) { // this loop actually does terminate, when one or the
                       // other "return" statement is reached
            boolean spaceOpen = true; // temporarily assume this space is
                                      // available
            for (int i = 0; i < N; i++) { // check all other molecules to see if
                                          // any are too close
                if ((Math.abs(x[i] - xTest) < buffer - epsilon) && (Math.abs(y[i] - yTest) < buffer - epsilon)) {
                    spaceOpen = false;
                    break;
                }
            }
            if (spaceOpen) {
                x[N] = xTest + (Math.random() - .5) * epsilon; // random nudge
                                                               // is to avoid
                                                               // too much
                                                               // symmetry
                y[N] = yTest + (Math.random() - .5) * epsilon;
                vx[N] = 0;
                vy[N] = 0;
                ax[N] = 0;
                ay[N] = 0;
                mColor[N] = randomColor();
                N++;
                return;
            } else { // if this space isn't open, try the next...
                xTest += buffer;
                if (xTest + buffer / 2 > boxWidth) {
                    xTest = buffer / 2;
                    yTest += buffer;
                    if (yTest + buffer / 2 > boxWidth)
                        return;
                }
            }
        }
    }

    // delete the molecule with index mIndex:
    // (is it necessary to preserve the order of the other molecules?)
    synchronized void deleteMolecule(int mIndex) {
        deleteMoleculeBonds(mIndex); // delete any bonds to this molecule
        for (int i = 0; i < fixedCount; i++) { // search fixed list for it and
                                               // delete if found
            if (fixedList[i] == mIndex) {
                fixedCount--;
                fixedList[i] = fixedList[fixedCount];
                break;
            }
        }
        N--;
        for (int i = mIndex; i < N; i++) { // bump remaining molecules down the
                                           // list
            x[i] = x[i + 1];
            y[i] = y[i + 1];
            vx[i] = vx[i + 1];
            vy[i] = vy[i + 1];
            ax[i] = ax[i + 1];
            ay[i] = ay[i + 1];
            mColor[i] = mColor[i + 1];
        }
        for (int i = 0; i < bondCount * 2; i++) { // update bond list for new
                                                  // molecule numbering
            if (bondList[i] > mIndex)
                bondList[i]--;
        }
        for (int i = 0; i < fixedCount; i++) { // update fixed list similarly
            if (fixedList[i] > mIndex)
                fixedList[i]--;
        }
        nScroll.setValue(N);
        if (selectedMolecule >= N)
            selectedMolecule = 0;
        theCanvas.repaint();
    }

    // make a molecule fixed if it isn't, or free it if it's fixed:
    synchronized void toggleFixedStatus(int mIndex) {
        boolean wasAlreadyFixed = false;
        for (int i = 0; i < fixedCount; i++) {
            if (fixedList[i] == mIndex) {
                wasAlreadyFixed = true;
                fixedCount--;
                fixedList[i] = fixedList[fixedCount];
                break;
            }
        }
        if (!wasAlreadyFixed) {
            fixedList[fixedCount] = mIndex;
            fixedCount++;
            vx[mIndex] = 0;
            vy[mIndex] = 0;
            ax[mIndex] = 0;
            ay[mIndex] = 0;
        }
        reset();
    }

    // Create a new bond between molecules m1 and m2, or delete all bonds if
    // m1==m2.
    // This method is not robust: m1 and m2 must be valid indices.
    void createBond(int m1, int m2) {
        if (m1 == m2) {
            deleteMoleculeBonds(m1);
        } else {
            if (m1 > m2) { // put lower index first for efficiency
                int swap = m1;
                m1 = m2;
                m2 = swap;
            }
            for (int i = 0; i < bondCount * 2; i += 2) {
                if ((bondList[i] == m1) && (bondList[i + 1] == m2))
                    return; // quit if bond already exists
            }
            if (bondCount == maxBonds)
                return; // quit if bond list is already full
            bondList[2 * bondCount] = m1;
            bondList[2 * bondCount + 1] = m2;
            bondCount++;
        }
        reset();
    }

    // delete all bonds to molecule mIndex from bond list, compressing list as
    // we go:
    void deleteMoleculeBonds(int mIndex) {
        int bIndex = 0;
        while (bIndex < 2 * bondCount) {
            if ((bondList[bIndex] == mIndex) || (bondList[bIndex + 1] == mIndex)) {
                bondCount--;
                for (int i = bIndex; i < bondCount * 2; i++)
                    bondList[i] = bondList[i + 2];
            } else {
                bIndex += 2;
            }
        }
    }

    synchronized void makeNeighborBonds() {
        for (int i = 1; i < N; i++) {
            for (int j = 0; j < i; j++) {
                double dx = x[i] - x[j];
                double dy = y[i] - y[j];
                if (dx * dx + dy * dy < 1.3 * 1.3) {
                    createBond(i, j);
                }
            }
        }
        theCanvas.repaint();
    }

    // check whether N or pixelsPerUnit or gravity or dt has been changed,
    // or if whole system is to be re-initialized:
    synchronized void checkControls() {

        // first check if "Initialize" button was pressed:
        if (reinit) {
            reinit = false;
            initialize();
            return;
        }

        // next check if "size" control has been changed:
        int newSize = (int) Math.round(sizeScroll.getValue());
        if (newSize != pixelsPerUnit) {
            if ((newSize > pixelsPerUnit) && safetyCheck.getState()) {
                newSize = pixelsPerUnit + 1; // increase by only 1 at a time in
                                             // safety mode
                sizeScroll.setValue(newSize);
            }
            double sizeRatio = pixelsPerUnit * 1.0 / newSize;
            for (int i = 0; i < N; i++) {
                x[i] *= sizeRatio;
                y[i] *= sizeRatio;
            }
            pixelsPerUnit = newSize;
            boxWidth = canvasWidth * 1.0 / pixelsPerUnit;
            theCanvas.repaint();
            reset();
        }

        // check time step:
        double newdt = dtScroll.getValue();
        if ((newdt > dt + 0.001) && safetyCheck.getState()) {
            newdt = dt + 0.001; // only minimal increase allowed in safety mode
            dtScroll.setValue(newdt);
        } else {
            dt = newdt;
        }

        // Now remove or add molecules if necessary:
        int newN = (int) Math.round(nScroll.getValue());
        if (newN < N) {
            N = newN;
            // delete all bonds to removed molecules:
            int bIndex = 0;
            while (bIndex < 2 * bondCount) {
                if ((bondList[bIndex] >= N) || (bondList[bIndex + 1] >= N)) {
                    bondCount--;
                    for (int i = bIndex; i < bondCount * 2; i++)
                        bondList[i] = bondList[i + 2];
                } else {
                    bIndex += 2;
                }
            }
            // delete removed molecules from fixed list:
            int fIndex = 0;
            while (fIndex < fixedCount) {
                if (fixedList[fIndex] >= N) {
                    fixedCount--;
                    fixedList[fIndex] = fixedList[fixedCount];
                } else {
                    fIndex++;
                }
            }
            computeAccelerations();
            if (selectedMolecule >= N)
                selectedMolecule = 0;
            theCanvas.repaint();
            reset();
        }
        if (newN > N) {
            for (int i = N; i < newN; i++)
                addMolecule();
            nScroll.setValue(N); // in case there wasn't room
            computeAccelerations();
            theCanvas.repaint();
            reset();
        }

        // finally, check gravity value:
        double newg = gravityScroll.getValue();
        if (newg != gravity) {
            gravity = newg;
            reset();
        }
    }

    // Here's the method that actually runs the simulation
    // (in the thread started at end of init):
    public void run() {
        long lastRealTime = System.currentTimeMillis(); // initialize
                                                        // performance counters
        int stepCount = 0;

        while (true) {
            checkControls(); // check for changes to N, size, reinitialization

            // Here's where we determine the animation speed:
            // (numbers work well on 2GHz dual-core machine)
            int speed = (int) Math.round(speedScroll.getValue());

            // sleep duration is 1 ms when speed >= 50, increasing linearly to
            // 50 ms at speed = 0:
            int sleepDuration;
            if (speed < 50)
                sleepDuration = 50 - speed;
            else
                sleepDuration = 1;

            // max real time between repaints (in milliseconds) ranges from 0 to
            // 100:
            // (for 25<speed<50, speed is controlled by sleepDuration)
            int maxRealTime;
            if (speed < 25) {
                maxRealTime = speed; // linear increase up to 25
            } else {
                if (speed < 50) {
                    maxRealTime = 25; // constant between 25 and 50
                } else {
                    maxRealTime = speed * speed / 100; // quadratic rise when
                                                       // speed > 50
                }
            }

            // max simulation time between repaints ranges from 0 to 2.0:
            double maxSimTime = maxRealTime / 50.0;

            // Now for the action:
            if (running) {
                long realTimeLimit = System.currentTimeMillis() + maxRealTime;
                double simTimeLimit = t + maxSimTime;
                int previousSteps = stepCount;
                while ((t < simTimeLimit) && (System.currentTimeMillis() < realTimeLimit)) {
                    singleStep();
                    stepCount++;
                }
                stepsPerFrame = stepCount - previousSteps;
                computeStats();
                theCanvas.repaint();
            }

            try {
                Thread.sleep(sleepDuration);
            } catch (InterruptedException e) {
            }

            // calculate steps per second:
            long realTime = System.currentTimeMillis();
            if ((realTime - lastRealTime) > 1000) {
                stepsPerSecond = stepCount * 1000.0 / (realTime - lastRealTime);
                lastRealTime = realTime;
                stepCount = 0;
            }
        }
    }

    // Run for one "step" (actually several, based on speed scroller):
    synchronized void step() {
        int nSteps = (int) Math.round(speedScroll.getValue());
        for (int step = 0; step < nSteps; step++) {
            singleStep();
        }
        computeStats();
        theCanvas.repaint();
    }

    // Execute one time step using the Verlet algorithm (from Gould and
    // Tobochnik):
    // (The physics is all in this method and the next two.)
    synchronized void singleStep() {
        double dtOver2 = 0.5 * dt;
        double dtSquaredOver2 = 0.5 * dt * dt;
        for (int i = 0; i < N; i++) {
            x[i] += (vx[i] * dt) + (ax[i] * dtSquaredOver2); // update position
            y[i] += (vy[i] * dt) + (ay[i] * dtSquaredOver2);
            vx[i] += (ax[i] * dtOver2); // update velocity halfway
            vy[i] += (ay[i] * dtOver2);
        }
        computeAccelerations();
        for (int i = 0; i < N; i++) {
            vx[i] += (ax[i] * dtOver2); // finish updating velocity with new
                                        // acceleration
            vy[i] += (ay[i] * dtOver2);
        }
        for (int i = 0; i < fixedCount; i++) { // force v = 0 for fixed
                                               // molecules
            vx[fixedList[i]] = 0;
            vy[fixedList[i]] = 0;
        }
        t += dt;
    } // end of method singleStep

    // Compute accelerations of all atoms from current positions:
    void computeAccelerations() {

        double dx, dy; // separations in x and y directions
        double dx2, dy2, rSquared, rSquaredInv, attract, repel, fOverR, fx, fy;

        tempPE = 0; // we'll also compute the potential energy
        double wallForce = 0.0; // and the pressure

        // first check for bounces off walls, and include gravity (if any):
        for (int i = 0; i < N; i++) {
            if (x[i] < 0.5) {
                ax[i] = wallStiffness * (0.5 - x[i]);
                wallForce += ax[i];
                tempPE += 0.5 * wallStiffness * (0.5 - x[i]) * (0.5 - x[i]);
            } else if (x[i] > (boxWidth - 0.5)) {
                ax[i] = wallStiffness * (boxWidth - 0.5 - x[i]);
                wallForce -= ax[i];
                tempPE += 0.5 * wallStiffness * (boxWidth - 0.5 - x[i]) * (boxWidth - 0.5 - x[i]);
            } else
                ax[i] = 0.0;
            if (y[i] < 0.5) {
                ay[i] = (wallStiffness * (0.5 - y[i]));
                wallForce += ay[i];
                tempPE += 0.5 * wallStiffness * (0.5 - y[i]) * (0.5 - y[i]);
            } else if (y[i] > (boxWidth - 0.5)) {
                ay[i] = (wallStiffness * (boxWidth - 0.5 - y[i]));
                wallForce -= ay[i];
                tempPE += 0.5 * wallStiffness * (boxWidth - 0.5 - y[i]) * (boxWidth - 0.5 - y[i]);
            } else
                ay[i] = 0;
            ay[i] -= gravity;
        }

        pressure = wallForce / (4 * boxWidth);

        // Now compute interaction forces (Lennard-Jones potential).
        // This is where the program spends most of its time (when N is
        // reasonably large),
        // so we carefully avoid unnecessary calculations and array lookups.
        // int pairCount = 0; // (diagnostic)
        for (int i = 1; i < N; i++) {
            for (int j = 0; j < i; j++) { // loop over all distinct pairs
                dx = x[i] - x[j];
                dx2 = dx * dx;
                if (dx2 < forceCutoff2) { // make sure they're close enough to
                                          // bother
                    dy = y[i] - y[j];
                    dy2 = dy * dy;
                    if (dy2 < forceCutoff2) {
                        rSquared = dx2 + dy2;
                        if (rSquared < forceCutoff2) {
                            // pairCount++;
                            rSquaredInv = 1.0 / rSquared;
                            attract = rSquaredInv * rSquaredInv * rSquaredInv;
                            repel = attract * attract;
                            tempPE += (4.0 * (repel - attract)) - pEatCutoff;
                            fOverR = 24.0 * ((2.0 * repel) - attract) * rSquaredInv;
                            fx = fOverR * dx;
                            fy = fOverR * dy;
                            ax[i] += fx; // add this force on to i's
                                         // acceleration (mass = 1)
                            ay[i] += fy;
                            ax[j] -= fx; // Newton's 3rd law
                            ay[j] -= fy;
                        }
                    }
                }
            }
        }
        // pairsInteracting = pairCount;

        // Add an attractive linear force for each bonded pair:
        for (int i = 0; i < bondCount * 2; i += 2) {
            int m1 = bondList[i];
            int m2 = bondList[i + 1];
            dx = x[m1] - x[m2];
            dy = y[m1] - y[m2];
            tempPE += 0.5 * bondStrength * (dx * dx + dy * dy) - bondEnergyOffset;
            fx = bondStrength * dx;
            fy = bondStrength * dy;
            ax[m1] -= fx;
            ay[m1] -= fy;
            ax[m2] += fx;
            ay[m2] += fy;
        }

        // fixed atoms don't accelerate:
        for (int i = 0; i < fixedCount; i++) {
            ax[fixedList[i]] = 0;
            ay[fixedList[i]] = 0;
        }

        // if we're pulling an atom it feels a spring force based on mouse
        // location (even if fixed)
        if (pulling) {
            dx = mouseX - x[pulledMolecule];
            dy = mouseY - y[pulledMolecule];
            tempPE += 0.5 * pullStrength * (dx * dx + dy * dy);
            ax[pulledMolecule] += pullStrength * dx;
            ay[pulledMolecule] += pullStrength * dy;
            // reset(); // pulling adds energy so reset averages
        }

    } // end of method computeAccelerations

    // Compute the energies, momenta, angular quantities, temperature, and
    // pressure.
    // This method gets called once for each animation frame (repaint).
    // These calls don't necessarily occur at regular intervals of simulation
    // time,
    // so the average T and P aren't necessarily strict averages over time.
    // However, I don't think this will matter for most purposes.
    synchronized void computeStats() {
        kE = 0;
        gE = 0;
        px = 0;
        py = 0;
        double xsum = 0;
        double ysum = 0;
        for (int i = 0; i < N; i++) {
            xsum += x[i];
            ysum += y[i];
            px += vx[i];
            py += vy[i];
            double speedSquared = vx[i] * vx[i] + vy[i] * vy[i];
            kE += 0.5 * speedSquared;
        }
        for (int i = 0; i < fixedCount; i++) { // fixed atoms don't count!
            xsum -= x[fixedList[i]];
            ysum -= y[fixedList[i]];
        }
        gE = gravity * ysum;
        xcm = xsum / (N - fixedCount);
        ycm = ysum / (N - fixedCount);
        Lcm = 0;
        Icm = 0;
        for (int i = 0; i < N; i++) {
            double dx = x[i] - xcm;
            double dy = y[i] - ycm;
            Lcm += dx * vy[i] - dy * vx[i];
            Icm += dx * dx + dy * dy;
        }
        for (int i = 0; i < fixedCount; i++) { // fixed atoms don't count for
                                               // Icm either
            double dx = x[fixedList[i]] - xcm;
            double dy = y[fixedList[i]] - ycm;
            Icm -= dx * dx + dy * dy;
        }
        pE = tempPE;
        totalE = kE + pE + gE;
        double currentT = kE / (N - fixedCount);
        if (currentT > 1000) { // handle run-away instability
            running = false;
            Point canvasLocation = theCanvas.getLocationOnScreen();
            alertFrame.setLocation(canvasLocation.x + 50, canvasLocation.y + 50);
            alertFrame.setVisible(true);
        }
        // try to prevent instability when safety mode is checked:
        final double safetyFactor = 4000;
        if ((currentT > 1.0 / (safetyFactor * dt * dt)) && safetyCheck.getState()) {
            dtScroll.setValue(Math.sqrt(1.0 / (safetyFactor * currentT)) - .001);
            dt = dtScroll.getValue();
        }
        totalT += currentT;
        totalP += pressure;
        sampleCount++;
        dataCanvas.repaint(); // display stats at bottom of applet
        // auto-record stats in stat window if enabled:
        int autoRecordMenuIndex = autoRecordChoice.getSelectedIndex();
        if (autoRecordMenuIndex > 0) {
            double interval = Math.pow(10, autoRecordMenuIndex - 1); // menu
                                                                     // choices
                                                                     // are
                                                                     // powers
                                                                     // of 10
            if (t >= lastAutoRecordTime + interval) {
                writeStats();
                lastAutoRecordTime = t;
            }
        }
    }

    public synchronized void speedMultiply(double factor) { // mulitiply all
                                                            // velocities by
                                                            // factor
        for (int i = 0; i < N; i++) { // (or slow down if factor < 1)
            vx[i] *= factor;
            vy[i] *= factor;
        }
        if (learningLevel > 0)
            learningLevel++; // get rid of helpful message
        reset();
        theCanvas.repaint();
    }

    public synchronized void reset() { // reset time and start over with average
                                       // T, P
        t = 0.0;
        totalT = 0.0;
        totalP = 0.0;
        sampleCount = 0;
        lastAutoRecordTime = 0.0;
        dataCanvas.repaint();
        if (!running)
            startButton.setLabel("Start");
    }

    // This inner class provides the drawing space and and does all the drawing:
    class MDCanvas extends Canvas implements MouseListener, MouseMotionListener {

        // off-screen image for double-buffering, to prevent flicker:
        Image offScreenImage;
        Graphics offScreenGraphics;

        Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
        Cursor crosshairCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
        Font bigFont = new Font(null, Font.BOLD, 20);
        int circleSizeCorrection = 0; // see constructor and drawOffScreenImage
                                      // method
        boolean dragging = false; // true when a molecule is being dragged
        boolean drawingBond = false; // true when we're drawing a new bond
        int newBondIndex; // index of molecule from which we're drawing a bond
        int bondEndX, bondEndY; // pixel coordinates of mouse location while
                                // drawing a new bond

        // carefully constructed list of colors for indicating speeds:
        final Color[] speedColorList = { new Color(20, 0, 180), new Color(60, 0, 170), new Color(80, 0, 160),
                new Color(100, 0, 150), new Color(120, 0, 120), new Color(140, 0, 80), new Color(160, 0, 0),
                new Color(180, 0, 0), new Color(200, 0, 0), new Color(230, 0, 0), Color.RED, new Color(255, 60, 0),
                new Color(255, 90, 0), new Color(255, 120, 0), new Color(255, 150, 0), new Color(255, 180, 0),
                new Color(255, 210, 0), new Color(255, 230, 0), Color.YELLOW, new Color(255, 255, 120) };

        // canvas constructor method:
        MDCanvas() {
            setSize(canvasWidth, canvasWidth);
            addMouseListener(this);
            addMouseMotionListener(this);
            setCursor(handCursor);
            if (System.getProperty("os.name").equals("Mac OS X") && (System.getProperty("os.version").charAt(3) < '5')
                    && (System.getProperty("java.vm.version").charAt(2) >= '4'))
                circleSizeCorrection = -1;
            // Java 1.4+ on Mac OS X 10.4- draws circles one pixel too big!
        }

        // draw the off-screen image, for later copying to screen:
        // (This is where we check the popup menus for the selected colors. Note
        // that the last
        // two entries in the molecule color menu are hard-coded for "random"
        // and "by speed".)
        void drawOffScreenImage(Graphics g) {
            g.setColor(bgColor[bgChoice.getSelectedIndex()]);
            g.fillRect(0, 0, canvasWidth, canvasWidth);
            int circleSize = pixelsPerUnit + circleSizeCorrection;
            int colorIndex = mColorIndex; // use remembered value since popup
                                          // may be set for selected
            if (colorIndex < mColorList.length)
                g.setColor(mColorList[colorIndex]);
            for (int i = 0; i < N; i++) {
                int screenx = xToScreen(x[i] - 0.5); // circle is drawn from
                                                     // upper-left corner
                int screeny = yToScreen(y[i] + 0.5);
                if (colorIndex == mColorList.length)
                    g.setColor(mColor[i]);
                if (colorIndex > mColorList.length)
                    g.setColor(speedColor(i));
                if (pixelsPerUnit < 5) {
                    g.fillRect(screenx, screeny, pixelsPerUnit, pixelsPerUnit);
                } else {
                    g.fillOval(screenx, screeny, circleSize, circleSize);
                }
            }
            // redraw fixed atoms in light gray:
            g.setColor(Color.lightGray);
            for (int i = 0; i < fixedCount; i++) {
                int screenx = xToScreen(x[fixedList[i]] - 0.5);
                int screeny = yToScreen(y[fixedList[i]] + 0.5);
                if (pixelsPerUnit < 5) {
                    g.fillRect(screenx, screeny, pixelsPerUnit, pixelsPerUnit);
                } else {
                    g.fillOval(screenx, screeny, circleSize, circleSize);
                }
            }
            // redraw selected atom if its color is different:
            if (selectedDifferent) {
                colorIndex = mColorChoice.getSelectedIndex();
                if (colorIndex < mColorList.length)
                    g.setColor(mColorList[colorIndex]);
                int screenx = xToScreen(x[selectedMolecule] - 0.5);
                int screeny = yToScreen(y[selectedMolecule] + 0.5);
                if (colorIndex == mColorList.length)
                    g.setColor(mColor[selectedMolecule]);
                if (colorIndex > mColorList.length)
                    g.setColor(speedColor(selectedMolecule));
                if (pixelsPerUnit < 5) {
                    g.fillRect(screenx, screeny, pixelsPerUnit, pixelsPerUnit);
                } else {
                    g.fillOval(screenx, screeny, circleSize, circleSize);
                }
            }
            // draw bonds as gray lines:
            g.setColor(Color.gray);
            int x1, y1, x2, y2; // pixel coordinates for line representing bond
            for (int i = 0; i < bondCount * 2; i += 2) {
                int m1 = bondList[i];
                int m2 = bondList[i + 1];
                x1 = xToScreen(x[m1]);
                y1 = yToScreen(y[m1]);
                x2 = xToScreen(x[m2]);
                y2 = yToScreen(y[m2]);
                g.drawLine(x1, y1, x2, y2);
            }
            if (drawingBond) {
                x1 = xToScreen(x[newBondIndex]);
                y1 = yToScreen(y[newBondIndex]);
                x2 = bondEndX;
                y2 = bondEndY;
                g.drawLine(x1, y1, x2, y2); // draw the bond currently being
                                            // created
            }
            if (pulling) {
                x1 = xToScreen(x[pulledMolecule]);
                y1 = yToScreen(y[pulledMolecule]);
                x2 = bondEndX;
                y2 = bondEndY;
                g.drawLine(x1, y1, x2, y2); // draw line from pulled molecule to
                                            // mouse
            }
            // when in testing mode, show all speed-determined colors:
            if (testing) {
                g.setColor(Color.gray);
                g.drawString("Steps per second: " + twoPlaces.format(stepsPerSecond), 5, canvasWidth - 5);
                g.drawString("Steps per frame: " + stepsPerFrame, 5, canvasWidth - 21);
                // g.drawString("Pairs interacting: " +
                // pairsInteracting,5,canvasWidth-37);
                for (int i = 0; i < speedColorList.length; i++) {
                    g.setColor(speedColorList[i]);
                    g.fillOval(20 * (i + 1), 10, 13, 13);
                    g.fillRect(20 * (i + 1), 40, 20, 20);
                }
            }
            // when simulation is first launched, give some helpful messages:
            if (learningLevel < 2) {
                g.setColor(Color.black);
                Font defaultFont = g.getFont();
                g.setFont(bigFont);
                if (learningLevel == 0) {
                    g.drawString("Press the Start button -->", 30, 22);
                } else {
                    g.drawString("Now try the Slower and Faster buttons -->", 30, 55);
                }
                g.setFont(defaultFont);
            }
        } // end of method drawOffScreenImage

        // return a color to indicate the speed of molecule i:
        Color speedColor(int i) {
            int colorCount = speedColorList.length;
            double speed = Math.sqrt(vx[i] * vx[i] + vy[i] * vy[i]);
            final double speedLimit = 3.0; // above this (squared) speed, all
                                           // get the same color
            if (speed >= speedLimit)
                return speedColorList[colorCount - 1];
            else
                return speedColorList[(int) (speed * colorCount / speedLimit)];
        }

        // override update to skip painting background (improves performance and
        // reduces flicker)
        public void update(Graphics g) {
            paint(g);
        }

        // paint method draws the off-screen image first, then blasts it to
        // screen:
        public void paint(Graphics g) {
            if (offScreenImage == null) {
                offScreenImage = createImage(canvasWidth, canvasWidth); // can't
                                                                        // do
                                                                        // this
                                                                        // until
                                                                        // paint
                                                                        // is
                                                                        // first
                                                                        // called
                offScreenGraphics = offScreenImage.getGraphics();
            }
            drawOffScreenImage(offScreenGraphics);
            g.drawImage(offScreenImage, 0, 0, null);
        }

        // When mouse button is pressed, check if it's over a molecule and take
        // appropriate action:
        public void mousePressed(MouseEvent e) {
            dragging = false; // should already be false but let's make sure
            drawingBond = false; // ditto
            pulling = false; // ditto ditto
            int clickedMolecule = findMolecule(e.getX(), e.getY()); // see if
                                                                    // there's a
                                                                    // molecule
                                                                    // there
            if (clickedMolecule > -1) {
                if (e.isAltDown()) { // alt down means toggling fixed status
                    toggleFixedStatus(clickedMolecule);
                } else if (e.isShiftDown()) { // shift down means creating or
                                              // deleting bond
                    drawingBond = true;
                    newBondIndex = clickedMolecule;
                    bondEndX = e.getX();
                    bondEndY = e.getY();
                } else if (running) { // otherwise pull molecule by elastic
                                      // "cord" if running
                    pulling = true;
                    pulledMolecule = clickedMolecule;
                    bondEndX = e.getX();
                    bondEndY = e.getY();
                    mouseX = xToActual(bondEndX);
                    mouseY = yToActual(bondEndY);
                } else { // or just select/drag it if not running
                    selectedMolecule = clickedMolecule;
                    dragging = true;
                }
                repaint();
            }
        }

        // Dragging mouse can mean several things...
        public void mouseDragged(MouseEvent e) {
            // setCursor(handCursor); // this shouldn't be necessary but is in
            // Safari (at least)
            if (dragging) {
                x[selectedMolecule] = xToActual(e.getX());
                y[selectedMolecule] = yToActual(e.getY());
                repaint();
                dataCanvas.repaint();
            } else if (drawingBond) {
                bondEndX = e.getX();
                bondEndY = e.getY();
                repaint();
                if (!running) {
                    mouseX = xToActual(bondEndX);
                    mouseY = yToActual(bondEndY);
                    moleculeUnderMouse = findMolecule(bondEndX, bondEndY);
                    dataCanvas.repaint();
                }
            } else if (pulling) {
                bondEndX = e.getX();
                bondEndY = e.getY();
                mouseX = xToActual(bondEndX);
                mouseY = yToActual(bondEndY);
                reset();
                repaint();
            }
        }

        // Quit dragging/pulling or create/delete bond when mouse is released:
        public void mouseReleased(MouseEvent e) {
            // setCursor(handCursor); // this shouldn't be necessary but is in
            // Safari (at least)
            if (dragging) {
                dragging = false;
                if ((x[selectedMolecule] < 0) || (x[selectedMolecule] > boxWidth) || (y[selectedMolecule] < 0)
                        || (y[selectedMolecule] > boxWidth)) {
                    deleteMolecule(selectedMolecule); // delete if out of bounds
                }
                reset();
            } else if (drawingBond) {
                int endMolecule = findMolecule(e.getX(), e.getY());
                if (endMolecule > -1) {
                    createBond(newBondIndex, endMolecule);
                }
                drawingBond = false;
                repaint();
            } else if (pulling) {
                pulling = false;
                reset();
            }
        }

        public void mouseEntered(MouseEvent e) {
            mouseInCanvas = true;
            updateMouseInfo(e);
        }

        public void mouseExited(MouseEvent e) {
            mouseInCanvas = false;
            dataCanvas.repaint();
        }

        public void mouseMoved(MouseEvent e) {
            updateMouseInfo(e);
        }

        void updateMouseInfo(MouseEvent e) {
            if (running) {
                moleculeUnderMouse = -1; // don't waste time looking if sim is
                                         // running
                setCursor(handCursor);
            } else {
                mouseX = xToActual(e.getX());
                mouseY = yToActual(e.getY());
                moleculeUnderMouse = findMolecule(e.getX(), e.getY());
                if (moleculeUnderMouse > -1)
                    setCursor(handCursor);
                else
                    setCursor(crosshairCursor);
                dataCanvas.repaint();
            }
        }

        // Look for a molecule at given pixel coordinates and return its index,
        // or -1 if not found:
        int findMolecule(int pixelX, int pixelY) {
            double mX = xToActual(pixelX); // convert coordinates to simulation
                                           // units
            double mY = yToActual(pixelY);
            final double radius2 = 0.55 * 0.55; // square of molecular radius
                                                // (with a little extra)
            boolean found = false; // no molecule found yet
            int i = 0; // molecule index
            while (i < N) {
                double dx = mX - x[i];
                double dx2 = dx * dx;
                if (dx2 < radius2) {
                    double dy = mY - y[i];
                    double dy2 = dy * dy;
                    if (dy2 < radius2) {
                        double r2 = dx2 + dy2;
                        if (r2 < radius2) {
                            found = true;
                            break;
                        }
                    }
                }
                i++;
            }
            if (found)
                return i;
            else
                return -1;
        }

        // conversions from simulation coordinates to screen coordinates:
        int xToScreen(double xActual) {
            return (int) Math.round(xActual * pixelsPerUnit);
        }

        int yToScreen(double yActual) {
            return canvasWidth - (int) Math.round(yActual * pixelsPerUnit);
        }

        // conversions from screen coordinates to simulation coordinates:
        double xToActual(int xScreen) {
            return xScreen * 1.0 / pixelsPerUnit;
        }

        double yToActual(int yScreen) {
            return (canvasWidth - yScreen) * 1.0 / pixelsPerUnit;
        }

        public void mouseClicked(MouseEvent e) {
        } // not needed since we handle press & release separately

    } // end of inner class MDCanvas

} // end of class MDApplet

// This class provides a scrollbar with a label and numerical readout:
class MDScroller extends Panel implements AdjustmentListener {

    double theValue, minValue, maxValue, stepSize; // scrollbar parameters
    String labelText; // explanatory text to display
    Label theLabel; // includes explanatory text and the numerical value
    DecimalFormat labelFormat; // format for displaying the value
    Scrollbar theScrollbar;

    /**
     * Construct a new MDScroller given the minimum value, maximum value, step
     * size, initial value, and text label to display to the left of the current
     * value.
     */
    MDScroller(String label, double min, double max, double step, double initial) {
        setLayout(new GridLayout(2, 1)); // label and readout on top; scrollbar
                                         // on bottom
        minValue = min;
        maxValue = max;
        stepSize = step;
        theValue = initial;
        labelText = label;
        if (decimalPlaces(stepSize) <= 0) {
            labelFormat = new DecimalFormat("0");
        } else {
            StringBuffer pattern = new StringBuffer().append("0.");
            for (int i = 0; i < decimalPlaces(stepSize); i++) {
                pattern.append("0");
            }
            labelFormat = new DecimalFormat(pattern.toString());
        }
        theLabel = new Label(labelText + labelFormat.format(theValue) + "  ");
        // append a couple of spaces to leave room in case the number grows
        // later
        add(theLabel); // the label goes on top
        int scaledInitial = (int) Math.round((initial - min) / step);
        int scaledMax = (int) Math.round((max - min) / step);
        theScrollbar = new Scrollbar(Scrollbar.HORIZONTAL, scaledInitial, 1, 0, scaledMax + 1);
        add(theScrollbar); // the scrollbar goes on the bottom
        theScrollbar.addAdjustmentListener(this);
    }

    /* Returns the decimal place of the first sig fig in x. */
    int decimalPlaces(double x) {
        return -(int) Math.floor(Math.log(x) / Math.log(10));
    }

    /* Implement AdjustmentListener to respond to scrollbar adjustment events. */
    public void adjustmentValueChanged(AdjustmentEvent e) {
        int scaledValue = theScrollbar.getValue();
        theValue = scaledValue * stepSize + minValue;
        theLabel.setText(labelText + labelFormat.format(theValue));
    }

    /** Return the current value of the parameter when asked. */
    public double getValue() {
        return theValue;
    }

    /** Set the current value of the scrollbar. */
    public void setValue(double newValue) {
        if (newValue < minValue)
            newValue = minValue;
        if (newValue > maxValue)
            newValue = maxValue;
        int scaledValue = (int) Math.round((newValue - minValue) / stepSize);
        theScrollbar.setValue(scaledValue);
        theValue = scaledValue * stepSize + minValue; // now it's rounded
                                                      // appropriately
        theLabel.setText(labelText + labelFormat.format(theValue));
    }
} // end of class MDScroller

// This class does the layout for the control panel:
// (based very loosely on code from www.falstad.com)
class MDControlLayout implements LayoutManager {

    int margin = 5; // size of margins around all sides, in pixels

    // unused methods:
    public MDControlLayout() {
    }

    public void addLayoutComponent(String name, Component c) {
    }

    public void removeLayoutComponent(Component c) {
    }

    // calculate needed width and height of container contents:
    Dimension totalContentSize(Container target) {
        int width = 0;
        int height = 0;
        for (int i = 0; i < target.getComponentCount(); i++) {
            Component c = target.getComponent(i);
            if (c.isVisible()) {
                Dimension d = c.getPreferredSize();
                if (d.width > width)
                    width = d.width;
                height += d.height;
            }
        }
        return new Dimension(width + 2 * margin, height + 2 * margin);
    }

    public Dimension preferredLayoutSize(Container target) {
        Dimension contentSize = totalContentSize(target);
        Insets insets = target.getInsets();
        return new Dimension(contentSize.width + insets.left + insets.right, contentSize.height + insets.top
                + insets.bottom);
    }

    public Dimension minimumLayoutSize(Container target) {
        return preferredLayoutSize(target);
    }

    // this method does the actual layout:
    public void layoutContainer(Container target) {

        Dimension contentSize = totalContentSize(target);
        Insets insets = target.getInsets(); // frame borders
        int vertSpace = target.getSize().height - (insets.top + insets.bottom); // available
                                                                                // space
        int padding = (vertSpace - contentSize.height) / target.getComponentCount();

        int leftEdge = insets.left + margin;

        int nextY = insets.top + margin; // y value at top of next component
        for (int i = 0; i < target.getComponentCount(); i++) { // loop over
                                                               // components
            Component c = target.getComponent(i);
            if (c.isVisible()) {
                Dimension d = c.getPreferredSize();
                d.width = contentSize.width - 2 * margin;
                c.setLocation(leftEdge, nextY);
                c.setSize(d.width, d.height);
                nextY += d.height + padding;
            }
        }
    }
} // end of class MDControlLayout
