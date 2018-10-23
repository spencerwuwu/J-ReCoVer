// https://searchcode.com/api/result/12095568/

/**
 * DependencyAnalysis ant task.  Copyright 2008, Three Rings Design inc.
 * Author: Robin Barooah
 * Released under a BSD license.
 * 
 * see: http://code.google.com/p/as3-dependency-tracer/
 */
package com.threerings.tools.flex.dependency;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.*;

public class DependencyAnalysis extends Task
{    
    /**
     * A set of SWC files containing symbols that are known to be
     * available.
     */
    public void addConfiguredAvailable (FileSet available)
    {
        _available = available;
    }
    
    /**
     * A set of SWC files containing symbols that are known to be
     * unavailable.
     */
    public void addConfiguredUnavailable (FileSet unavailable)
    {
        _unavailable = unavailable;
    }

    /**
     * The location of the file containing the link report.
     */
    public void setLinkReport(File linkReport)
    {
        _linkReport = linkReport;
    }
    
    /**
     * The root symbol that we wish to analyse.
     */
    public void setRoot(String root)
    {
        _root = root;
    }
    
    /**
     * The list of fake classes.
     */
    public void addConfiguredFake (ListSpace fake) 
    {
        _fake = fake;
    }
    
    /**
     * The list of immune classes.
     */
    public void addConfiguredImmune (ListSpace immune)
    {
        _immune = immune;
    }

    /**
     * Switch on reporting of traces into unknown classes.
     */
     public void setUnknown (boolean unknown)
     {
         _unknown = unknown;
     }

    /**
     * Switch on diagnostics (prints the parameter lists as understood by the ant task)
     */
    public void setDiagnostic (boolean diagnostic)
    {
        _diagnostic = diagnostic;
    }

    /**
     * Switch on tracing (prints out detailed steps taken by the analyser)
     */
    public void setTrace (boolean trace)
    {
        _trace = trace;
    }

    /**
     * This is called by ant to execute the task when everything is ready.
     */     
    public void execute () throws BuildException
    {
        if (_diagnostic) {
            dumpParameters();
        }
        try {
            run();
        } catch (Exception e) {
            throw new BuildException(e);
        }        
    }
    
    /**
     * Run the analysis (the scala class).
     */ 
    public void run () throws IOException
    {
        AntAnalyser analyser = new AntAnalyser(stringPaths(_available), stringPaths(_unavailable), 
            _root, _linkReport.getCanonicalPath(), _fake.list(), _immune.list(), _unknown, _trace);

        analyser.run();
    }
    
    /**
     * Convert the paths from a fileset into a list of strings.
     */
    public ArrayList<String> stringPaths (FileSet set)
    {
        ArrayList<String> list = new ArrayList<String>();
        Iterator i = set.iterator();
        while (i.hasNext()) {
            Resource r = (Resource) i.next();
            list.add(r.toString());
        }
        return list;
    }
    
    /**
     * Dump parameters passed to the ant task - used for diagnostic purposes.
     */
    public void dumpParameters () 
    {
        println("Available libraries:");
        dumpResources(_available);
        println();        
        println("Unavailable libraries:");
        dumpResources(_unavailable);
        println();        
        println("Dummy classes:");
        dumpList(_fake.list());
        println();
        println("Immune classes:");
        dumpList(_immune.list());
        println();
    }
    
    /**
     * Dump the the contents of a list.
     */
    protected void dumpList (List<String> list)
    {
        if (list == null) {
            println("  no list specified");
            return;
        }
        for (String element: list) {
            println("  "+element);
        }        
    }
 
    /**
     * Dump a resource collection.
     */
    protected void dumpResources (ResourceCollection resources)
    {
        if (resources == null) {
            println("  no resource specified");
            return;
        }
        Iterator i = resources.iterator();
        while (i.hasNext()) {
            Resource r = (Resource) i.next();
            println("  "+r.toString());
        }
    }
    
    /**
     * Reduce verbosity.
     */
    protected void println ()
    {
        println("");
    }
    
    /**
     * Reduce verbosity.
     */
    protected void println (String toPrint) 
    {
        System.out.println(toPrint);
    }
    
    /**
     * Define a type (to use in the ant task) for providing a comma separated list of stuff in
     * the body text.
     */ 
    public static class ListSpace
    {
        public void addText(String text) 
        {
            for (String s: text.split(",")) {
                final String trimmed = s.trim();
                if (trimmed.length() > 0) {
                    _list.add(s.trim());
                }
            }
        }
        
        public List<String> list() {
            return _list;
        }
        
        protected ArrayList<String> _list = new ArrayList<String>();        
    }

    protected boolean _diagnostic;
    protected boolean _trace;
    protected boolean _unknown;
    protected FileSet _available;
    protected FileSet _unavailable;
    protected String _root;
    protected File _linkReport;
    protected ListSpace _fake;
    protected ListSpace _immune;
}
