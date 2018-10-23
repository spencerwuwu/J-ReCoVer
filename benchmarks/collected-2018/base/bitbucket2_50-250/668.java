// https://searchcode.com/api/result/43427750/

// Copyright (c) 2007 Sun Microsystems
//
// Permission is hereby granted, free of charge, to any person
// obtaining a copy of this software and associated documentation
// files (the "Software"), to deal in the Software without
// restriction, including without limitation the rights to use,
// copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following
// conditions:
//    
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//    
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
// OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
// HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.

package net.java.hulp.measure;

import net.java.hulp.measure.internal.DelegatorFactory;
import net.java.hulp.measure.internal.FactoryFactoryV2;
import net.java.hulp.measure.internal.FactoryV2;

import javax.management.openmbean.TabularData;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Measures a length of time of an operation and aggregates the results with extremely
 * little overhead so that Probes can be left in production code.
 * 
 * Analogous to logging using java.util.logging.Logger, probes can be used at different 
 * levels: FINE and INFO. Probes at FINE level can be turned off to further reduce 
 * overhead. When probes are turned off, the overhead of having a probe in the code is
 * negligible as it only involves calling an empty method without object creation or
 * synchronization.
 * 
 * Typical usage is:<br><br>
 * <code>
 * Probe m = Probe.begin(class, topic, subtopic);<br>
 * ... do something ...<br>
 * d.end();<br>
 * </code>
 * <br><br>
 *
 * Notes:<br>
 * - A measurement has a source, topic and a subtopic. The source class may be null 
 * in which case the resulting source location string is an empty string.
 * - Topic and sub topic can be set afterwards.
 * - A measurement is not reusable.
 * 
 * @author fkieviet
 */
public class Probe {
    private static FactoryV2 sFactory;
    private static FactoryV2 sAlwaysOnFactory;
    private static Probe sVoidMeasurement;
    private static final String FACTORYNAME = "net.sf.hulp.profiler.FactoryFactory";

    /**
     * Constructor; forces callers to use factory method begin()
     */
    protected Probe() {
    }

    // Bootstrap: ensures that the begin() factory method will return a void or
    // concrete measurement
    static {
        try {
            // Try to load factory, taking care of the situation where this class may
            // be loaded in a self-first delegating classloader
            String name = System.getProperty(FACTORYNAME, FACTORYNAME);
            Class cFF = Class.forName(name);
            Class cparentFF = null;
            try {
                cparentFF = Probe.class.getClassLoader().getParent().loadClass(name);
            } catch (Exception e) {
                // ignore
            }
            
            Class cparentProbe = null;
            try {
                cparentProbe = Probe.class.getClassLoader().getParent().loadClass(Probe.class.getName());
            } catch (Exception e) {
                // ignore
            }
            
            if (cparentFF != null && cFF != cparentFF || cparentProbe != null && cparentProbe != Probe.class) {
                // Self-first delegating classloader detected...
                Object factoryfactory = cparentFF.newInstance();
                Object alwaysonfactory = cparentFF.getMethod("newAlwaysOnFactoryV2").invoke(factoryfactory);
                sAlwaysOnFactory = new DelegatorFactory(alwaysonfactory);
                Object factory = cparentFF.getMethod("newFactoryV2").invoke(factoryfactory);
                sFactory = new DelegatorFactory(factory);
                System.out.println("Delegating measurement factory loaded: " + name + " (" + sFactory + ")");
            } else {
                // Ordinary classloading situation
                FactoryFactoryV2 f = (FactoryFactoryV2) cFF.newInstance();
                sAlwaysOnFactory = f.newAlwaysOnFactoryV2();
                sFactory = f.newFactoryV2();
                System.out.println("Measurement factory loaded: " + name + " (" + sFactory + ")");
            }
        } catch (Throwable ex) {
            // Ignore error
        }

        // Factory failed to load? Disable measurements
        if (sFactory == null) {
            sVoidMeasurement = new Probe();
        }
    }

    /**
     * Creates a new Probe at FINE level which means that this Probe can be turned off
     * 
     * @param source source class; may be null
     * @param topic specifies the operation
     * @param subTopic qualifies the operation
     * @return a Probe
     */
    public static Probe fine(Class source, String topic, String subTopic) {
        if (sVoidMeasurement != null) {
            return sVoidMeasurement;
        } else {
            return sFactory.createV2(FactoryV2.FINE, source, topic, subTopic);
        }
    }
    
    /**
     * see {@link #fine(Class, String, String)}
     */
    public static Probe fine(Class source, String topic) {
        return fine(source, topic, null);
    }

    /**
     * Creates a new probe
     * @see #fine(Class, String, String) 
     */
    public static Probe info(Class source, String topic, String subTopic) {
        return sAlwaysOnFactory != null 
        ? sAlwaysOnFactory.createV2(FactoryV2.INFO, source, topic, subTopic) 
            : sVoidMeasurement;
    }
    
    /**
     * @see #fine(Class, String, String)
     */
    public static Probe info(Class source, String topic) {
        return sAlwaysOnFactory != null 
        ? sAlwaysOnFactory.createV2(FactoryV2.INFO, source, topic, null) 
            : sVoidMeasurement;
    }

    /**
     * Returns if there is a measuring infrastructure installed
     * @return true if is installed
     */
    public static boolean isFine() {
        return sVoidMeasurement == null;
    }
    
    /**
     * Returns if there is a measuring infrastructure installed
     * @return true if is installed
     */
    public static boolean isInfo() {
        return sAlwaysOnFactory != null;
    }
    
    /**
     * @param criteria list of critera of which at least one must match to be included
     * in the result. Each criterion is an array of two elements, the first element is 
     * a regular expression for the topic and the second element is a regular expression
     * for the sub topic.
     * 
     * @return CompositeData if the implementation is present, null if not
     */
    public static TabularData getData(List<Pattern[]> criteria) {
        return sAlwaysOnFactory == null ? null : sAlwaysOnFactory.getData(criteria);
    }

    /**
     * Ends the time interval and adds the result to the aggregator
     */
    public void end() {
    }

    /**
     * Resets the topic of the operation
     */
    public void setTopic(String topic) {
    }

    /**
     * Further specifies the operation
     */
    public void setSubtopic(String subTopic) {
    }

    /**
     * Clears the data specified by the criteria
     * @param criteria list of regular expressions defining the probes
     */
    public static void clearData(List<Pattern[]> criteria) {
        if (sAlwaysOnFactory != null) {
            sAlwaysOnFactory.clearData(criteria);
        }
    }
}
