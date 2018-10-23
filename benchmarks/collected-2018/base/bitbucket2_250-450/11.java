// https://searchcode.com/api/result/102497684/

/* Copyright (C) 2007 United States Government as represented by the
 * Administrator of the National Aeronautics and Space Administration
 * (NASA).  All Rights Reserved.
 *
 * This software is distributed under the NASA Open Source Agreement
 * (NOSA), version 1.3.  The NOSA has been approved by the Open Source
 * Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
 * directory tree for the complete NOSA document.
 *
 * THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
 * KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
 * LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
 * SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 * A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
 * THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
 * DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
 */
package gov.nasa.jpf.hmi.shell;

import gov.nasa.jpf.shell.ShellPanel;
import gov.nasa.jpf.shell.ShellManager;
import gov.nasa.jpf.shell.ShellCommand;
import gov.nasa.jpf.shell.ShellCommandListener;

import gov.nasa.jpf.shell.panels.PropertiesPanel;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.ConfigChangeListener;

import gov.nasa.jpf.hmi.generation.NonFullControlDeterministicModelException;
import gov.nasa.jpf.hmi.models.LTS;
import gov.nasa.jpf.hmi.models.Transition;
import gov.nasa.jpf.hmi.models.ActionType;
import gov.nasa.jpf.hmi.models.Action;
import gov.nasa.jpf.hmi.models.State;

import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.text.BadLocationException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HMIPanel extends ShellPanel {
    private JTextArea system, mental;
    private JPanel gSys, gMent;
    private JRadioButton learn, reduce;
    private JCheckBox dfa,  hybrid;
    private Box reduceOptions;
    private boolean guiInit;
    private boolean hasDot;
    private Pattern
        // pattern = Pattern.compile("^ *\\+?[a-z]+(\\.[a-z_0-9]+)+",
        //                           Pattern.CASE_INSENSITIVE),
        property = Pattern.compile("^ *\\+?([a-z]+(\\.[a-z_0-9]+)+) *=(.*)$",
                                   Pattern.CASE_INSENSITIVE);

    private JEditorPane editor;
    private Map<String, String> unsetProperties = new HashMap<String, String>();


    public HMIPanel() {
        super("HMI Analysis", null, "Click me!");
        guiInit = false;
        HMICommandListener listener =
            new HMICommandListener() {
                public void preCommand(HMICommand command) {
                    if (!hasDot) {
                        mental.setText("");
                        system.setText("");
                    }
                    requestShellFocus();
                }

                public void postCommand(HMICommand command) {
                    validate();
                }
                public void afterJPFInit(HMICommand command) {}

                public void reducerFailed(HMICommand cmd, Exception ex) {
                    error("Failed to reduce the model with error message:"
                          + ex.getMessage());
                }

                public void learnerFailed(HMICommand cmd, Exception ex) {
                    error("Model is incorrect:"
                          + ex.getMessage());
                }

                private void produceGraphOrText (LTS <State, Transition> lts,
                                                 JTextArea text, JPanel graph) {
                    if (!hasDot) {
                        text.setText(lts.toString());
                    } else {
                        try {
                            produceGraph(lts, graph);
                        } catch (IOException ex) {
                            error(ex.getMessage());
                        }
                    }
                }

                public void loadedSystemModel(HMICommand cmd, LTS<State,
                                              Transition> sys) {
                    produceGraphOrText(sys, system, gSys);
                }
    
                public void producedMentalModel(HMICommand cmd,
                                                LTS<State, Transition> mntl) {
                    produceGraphOrText(mntl, mental, gMent);
                }
            };
        ShellCommandListener sm =
            new ShellCommandListener() {
                public void postCommand(ShellCommand command) {
                    String name = ShellManager.getManager()
                        .getConfig().getProperty("hmi.system");
                    setEditorProperty("hmi.system", name);
                }

                public void preCommand(ShellCommand command) {}
            };
        ShellManager.getManager().addCommandListener(HMICommand.class,
                                                     listener);
        ShellManager.getManager().addCommandListener(StateMachineLoadCommand.class,
                                                     sm);
        try {
            Runtime.getRuntime().exec("dot -h");
        } catch (IOException io) {
            warning("Couldn't locate graphviz. Include it's path in the PATH system property or install it from http://www.graphviz.org");
        }
    }
    
    private void produceGraph(LTS lts, JComponent c) throws IOException {
        Map<String, Integer> states = new HashMap<String, Integer>();
        Map<String, Set<String>> transitions = new HashMap<String, Set<String>>();
        int index = 1;
        StringBuffer out = new StringBuffer("digraph G {\n");

        Object init = lts.initialState();
        for (Object  st : lts.states()) {
            if (init != null && init.equals(st)) {
                //continue;
                out.append("init [shape=plaintext, label=\"\"];\n");
            }
            Integer pos =  new Integer(index++);
            states.put(st.toString(), pos);
            out.append(pos).append("[label=\"")
                .append(st.toString())
                .append("\"];\n");
            if (init != null && init.equals(st)) {
                out.append("init -> ").append(pos).append(";\n");
            }
        }
        for (Object o : lts.transitions()) {
            Transition tr = (Transition) o;
            Integer from = states.get(lts.source(tr).toString());
            Integer to   = states.get(lts.destination(tr).toString());
            if (lts.isTauTransition(tr)) {
                out.append(from).append("->").append(to)
                    .append("[style=dotted];");
            } else {
                String key = from.toString() + "-" + to;
                Set<String> b = transitions.get(key);
                if (b == null) {
                    b = new TreeSet<String>();
                    transitions.put(key, b);
                }
                b.add(tr.getAction().toString());
            }
        }
        for (String key : transitions.keySet()) {
            out.append(key.replace("-", "->"))
                .append("[label=\"");
            Set<String> labels = transitions.get(key);
            boolean first = true;
            for (String label : labels) {
                if (!first) {
                    out.append("\\n");
                }
                out.append(label);
                first = false;
            }
            out.append("\"];\n");
        }
        out.append("};\n");

        File dot = File.createTempFile("ltsdot", ".dot", null);
        File png = File.createTempFile("ltspng", ".png", null);
        try {
            FileOutputStream fs = new FileOutputStream(dot);
            fs.write(out.toString().getBytes());
            fs.flush();
            fs.close();

            String[] args = {"dot", "-Tpng", dot.getAbsolutePath(),
                             "-o" + png.getAbsolutePath()};
            Process proc = Runtime.getRuntime().exec(args);
            try {
                proc.waitFor();
            } catch (InterruptedException ie) {
                error("graphviz failed");
            }
            if (proc.exitValue() == 0) {
                BufferedImage bi = ImageIO.read(png);
                c.removeAll();
                c.add(new JLabel(new ImageIcon(bi)));
            }
        } catch (IOException ioex) {
            error("Failed to produce image:"  + ioex.getMessage());
        } finally {
            dot.delete();
            png.delete();
        }
    }
    
    protected void addedToShell(){
        if (guiInit) {
            return;
        }
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Config config = ShellManager.getManager().getConfig();

        JRadioButton produceModel = new JRadioButton("Generate mental model");
        produceModel.setSelected(true);

        JRadioButton modelConfussion =
            new JRadioButton(new AbstractAction("Model confusion") {
                    public void actionPerformed(ActionEvent e) {
                        warning("This feature is not supported");
                    }
                });
        modelConfussion.setEnabled(false);

        ButtonGroup sel = new ButtonGroup();
        sel.add(produceModel);
        sel.add(modelConfussion);

        Box selBox = Box.createVerticalBox();
        selBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Operation mode"));
        selBox.add(produceModel);
        selBox.add(modelConfussion);
            
        learn = new JRadioButton(new AbstractAction("Learning") {
                public void actionPerformed(ActionEvent e) {
                    reduceOptions.setEnabled(false);
                    for (Component c : reduceOptions.getComponents()) {
                        c.setEnabled(false);
                    }
                    setEditorProperty("hmi.algo", "learning");
                }
            });
        learn.setSelected("learning".equals(config.getProperty("hmi.algo"))
                          || config.getProperty("hmi.algo") == null);
    
        reduce = new JRadioButton(new AbstractAction("Reduction") {
                public void actionPerformed(ActionEvent e) {
                    reduceOptions.setEnabled(true);
                    for (Component c : reduceOptions.getComponents()) {
                        c.setEnabled(true);
                    }
                    setEditorProperty("hmi.algo", "reduction");
                }
            });
        reduce.setSelected("reduction".equals(config.getProperty("hmi.algo")));
        ButtonGroup algo = new ButtonGroup();
        algo.add(learn);
        algo.add(reduce);

        Box algorithms = Box.createVerticalBox();
        algorithms.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Algorithms"));
        algorithms.add(learn);
        algorithms.add(reduce);
        
        
        dfa = new JCheckBox(new AbstractAction("Three valued automaton") {
                public void actionPerformed(ActionEvent e) {
                    JCheckBox source = (JCheckBox) e.getSource();
                    setEditorProperty("hmi.algo.reduction.learn3DFA",
                                      source.isSelected() ? "true" : "false");
                }
            });
        dfa.setSelected(config.getBoolean("hmi.algo.reduction.learn3DFA"));

        hybrid = new JCheckBox(new AbstractAction("Hybrid") {
                public void actionPerformed(ActionEvent e) {
                    JCheckBox source = (JCheckBox) e.getSource();
                    setEditorProperty("hmi.algo.reduction.hybrid",
                                      source.isSelected() ? "true" : "false");
                }
            });
        hybrid.setSelected(config.getBoolean("hmi.algo.reduction.hybrid"));
        
        reduceOptions = Box.createVerticalBox();
        reduceOptions
            .setBorder(BorderFactory
                       .createTitledBorder(
                                           BorderFactory.createLineBorder(Color.BLACK),
                                           "Settings for reduce algorithm"));
        reduceOptions.add(dfa);
        reduceOptions.add(hybrid);
        reduceOptions.add(Box.createHorizontalStrut(50));
        learn.getAction().actionPerformed(null);
        
        Box options = Box.createHorizontalBox();
        options.add(Box.createHorizontalStrut(50));
        options.add(selBox);
        options.add(Box.createHorizontalStrut(50));
        options.add(algorithms);
        options.add(Box.createHorizontalStrut(50));
        options.add(reduceOptions);
        options.setMaximumSize(new Dimension(100000, 300));
        
        if (!hasDot) {
            system = new JTextArea(10, 20);
            mental = new JTextArea(10, 20);
        } else {
            gSys = new JPanel();
            gMent = new JPanel();
        }
    
        Box sys = Box.createVerticalBox();
        sys.add(new JLabel("Sytem model"));
        JScrollPane pane = new JScrollPane(!hasDot ? system : gSys);
        sys.add(pane);

        Box ment = Box.createVerticalBox();
        ment.add(new JLabel("Mental model"));
        pane = new JScrollPane(!hasDot ? mental : gMent);
        ment.add(pane);

        Box texts = Box.createHorizontalBox();
        texts.add(sys);
        texts.add(ment);

        add(options);
        add(texts);

        guiInit = true;
        final HMIPanel me = this;
        SwingUtilities.invokeLater( new Runnable(){
                public void run(){
                    PropertiesPanel p = ShellManager.getManager()
                        .findPanel(PropertiesPanel.class);
                    if (p != null) {
                        editor = p.getEditor();
                        editor.setEditable(false);
                        for (String key : unsetProperties.keySet()) {
                            setEditorProperty(key, unsetProperties.get(key));
                        }
                        unsetProperties.clear();
                        // try {
                        //     String text = editor.getDocument().getText(0, editor.getDocument().getLength());
                        //     boolean found = Pattern.compile("\\+*native_classpath\\+* *=")
                        //                  .matcher(text).find();
                        //     if (!found) {
                        //  setEditorProperty("+native_classpath", "../build/examples;build/examples;examples;.");
                        //     }
                        // } catch (BadLocationException be) {
                        //     setEditorProperty("+classpath", "../build/examples;");
                        // }
                    }
                }
            });
    }
    
    public void configChanged(Config cfg) {
        super.configChanged(cfg);
        learn.setSelected("learning".equals(cfg.getProperty("hmi.algo")));
        reduce.setSelected("reduction".equals(cfg.getProperty("hmi.algo")));
        dfa.setSelected(cfg.getBoolean("hmi.algo.reduction.learn3DFA"));
        hybrid.setSelected(cfg.getBoolean("hmi.algo.reduction.hybrid"));
        learn.getAction().actionPerformed(null);
        if (gSys != null) {
            gSys.removeAll();
            gMent.removeAll();
            validate();
        } else {
            system.setText("");
            mental.setText("");
        }
    }

    private void setEditorProperty(String name, String value) {
        if (editor == null) {
            unsetProperties.put(name, value);
            return;
        }
        Config config = ShellManager.getManager().getConfig();
        Matcher matcher = null, propMatcher = null;
        config.setProperty(name, value);
        int offset = 0;
        try {
            String[] lines = editor.getDocument()
                .getText(0, editor.getDocument().getLength()).split("\n");
            boolean found = false;
            for (String line : lines) {
                int lineLength = line.length();
                line = line.trim();
                if (line.length() == 0) {
                    offset += lineLength + 1;
                    continue;
                }
                matcher = matcher == null
                    ? property.matcher(line) : matcher.reset(line);
                if (matcher.find()) {
                    String propName = matcher.group(1);
                    String propVal = matcher.group(3).trim();
                    if (propName.equals(name)) {
                        if (!propVal.equals(value)) {
                            editor.getDocument().remove(offset, lineLength);
                            editor.getDocument().insertString(offset,
                                                              name + "=" + value,
                                                              null);
                        }
                        return;
                    }
                }
                offset += lineLength + 1;
            }
            int last = editor.getDocument().getLength();
            editor.getDocument().insertString(last,
                                              name + "=" + value + "\n",
                                              null);
        } catch (BadLocationException ble) {
            error(ble.toString());
        }
    }
}

