// https://searchcode.com/api/result/102497683/

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

import java.util.logging.Level;
import gov.nasa.jpf.shell.*;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.util.LogManager;


import java.util.List;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import gov.nasa.jpf.hmi.models.LTS;
import gov.nasa.jpf.hmi.models.Model;
import gov.nasa.jpf.hmi.models.Models;
import gov.nasa.jpf.hmi.models.util.LTSLoader;
import gov.nasa.jpf.hmi.models.State;
import gov.nasa.jpf.hmi.models.Transition;
import gov.nasa.jpf.hmi.models.LTS.Copier;

import gov.nasa.jpf.hmi.generation.learning.Learner;
import gov.nasa.jpf.hmi.generation.learning.Teacher;
import gov.nasa.jpf.hmi.generation.NonFullControlDeterministicModelException;
import gov.nasa.jpf.hmi.generation.reduction.Reductor;

// import gov.nasa.jpf.learn.basic.SETException;
// import gov.nasa.jpf.learn.basic.Learner;
// import gov.nasa.jpf.learn.TDFA.TDFALearner;

import gov.nasa.jpf.jvm.AnnotationInfo;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;

import java.io.IOException;


/**
 * The command responsible for starting and running JPF and also for canceling
 * it.
 */
public class HMICommand extends ShellCommand {
    private static boolean running = false;

    @Override
    public String getName(){
        return "HMI";
    }

    @Override
    public String getToolTip(){
        return "Validate the loaded HMI";
    }

    @Override
    public Icon getIcon(){
        String name;
        if (running) {
            name = "working.png";
        } else {
            name = "hmi.png";
        }
        URL url = getClass().getResource("images/" + name);
        return new ImageIcon(url);
    }

    // @Override
    // public boolean prepare(){
    //  if (running){
    //      if (JOptionPane
    //      .showConfirmDialog(ShellManager.getManager().getShell(),
    //                 "Are you sure that you want to cancel JPF?")
    //      == JOptionPane.YES_OPTION)
    //      //cancelVerify();
    //      return false;
    //  }
    //  return true;
    // }

    private LTS<State,Transition> getSystemLTS(Config config) {
        String ltsFile = config.getProperty("hmi.lts_file");
        String system = config.getProperty("hmi.system");
        LTS<State,Transition> systemModel;
        if (ltsFile != null) {
            systemModel = LTSLoader.loadLTS(ltsFile);
        } else {
            systemModel = Models.getLTS(config, system);
            String systemOutput = config.getProperty ("hmi.system_output");
            if (systemOutput != null) {
                try {
                    LTSLoader.saveLTS(systemModel, systemOutput + ".lts");
                    systemModel.saveAsDotFile(systemOutput + ".dot");
                } catch (IOException exception) {
                    System.err.printf("! Failed to save the system model LTS in the %s.lts file\n", systemOutput);
                }
            }
        }
        return systemModel;
    }

    /**
     * Responsible for starting HMI analysis. <b>DO NOT CALL THIS
     * DIRECTLY</b> instead use one of the
     * <code>ShellManager.getManager().fireCommand()</code> methods.
     *
     * The HMICommand has an extra step between pre and post command, some
     * listeners need the jpf instance after jpf's init() but before the run
     * (Specifically to add publishers). Therefore, before jpf.run() can be called
     * the {@link gov.nasa.jpf.shell.HMICommandListener#afterJPFInit(gov.nasa.jpf.JPF) }
     * method needs to be called on all of the HMICommandListener that are
     * registered with the ShellManager. That is all handled here. This method
     * will get all of the HMICommandListeners and execute the afterJPFInit
     * after the jpf init takes place.
     */
    public void execute() {
        List<HMICommandListener> listeners = ShellManager.getManager()
            .getCommandListeners(getClass(), HMICommandListener.class);
        for (HMICommandListener vcl :listeners) {
            vcl.afterJPFInit(this);
        }

        running = true;
        requestShellUpdate();
        Config config = ShellManager.getManager().getConfig();
        String system = config.getProperty("hmi.system");

        LTS<State,Transition> systemModel = getSystemLTS(config);
        if (systemModel == null) {
            System.err.println("System model is not loaded. Aborting");
            running = false;
            requestShellUpdate();
            return;
        }
        for (HMICommandListener vcl :listeners) {
            vcl.loadedSystemModel(this, systemModel);
        }
        String algo = config.getProperty("hmi.algo");
        LTS<State,Transition> mentalModel = null;
        if ("reduction".equals(algo)) {
            boolean learn3DFA = config.getBoolean("hmi.algo.reduction.learn3DFA");
            boolean hybrid = config.getBoolean("hmi.algo.reduction.hybrid");

            Reductor reductor = new Reductor(systemModel, learn3DFA, hybrid);
            try {
                mentalModel = reductor.reduce();
            } catch (NonFullControlDeterministicModelException ex) {
                for (HMICommandListener vcl : listeners) {
                    vcl.reducerFailed(this, ex);
                }
            }
        } else if ("learning".equals(algo)) {
            systemModel.complete(new Copier<State,Transition>() {
                    @Override
                        public State copyState (State s) {
                        return new State(s.getName());
                    }

                    @Override
                        public Transition copyTransition (Transition t) {
                        return new Transition (t.getAction());
                    }
                });
            Teacher teacher = new Teacher(systemModel);
            Learner learner = new Learner(teacher);
            try {
                mentalModel = learner.getMentalModel();
            } catch (NonFullControlDeterministicModelException setx) {
                for (HMICommandListener vcl :listeners) {
                    vcl.learnerFailed(this, setx);
                }
            }
        }
        if (mentalModel != null) {
            for (HMICommandListener vcl :listeners) {
                vcl.producedMentalModel(this, mentalModel);
            }
        }
        running = false;
        requestShellUpdate();
    }
}

