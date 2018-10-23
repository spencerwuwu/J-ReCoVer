// https://searchcode.com/api/result/55590446/

/**
 * 
 */
package gov.nasa.ltl.tests;

import gov.nasa.ltl.graph.Degeneralize;
import gov.nasa.ltl.graph.Graph;
import gov.nasa.ltl.graph.SCCReduction;
import gov.nasa.ltl.graph.SFSReduction;
import gov.nasa.ltl.graph.Simplify;
import gov.nasa.ltl.graph.SuperSetReduction;
import gov.nasa.ltl.trans.Formula;
import gov.nasa.ltl.trans.LTL2Buchi;
import gov.nasa.ltl.trans.Rewriter;
import gov.nasa.ltl.trans.Translator;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Iterator;
import java.util.Random;

/**
 * Implementation of the experiment described in section
 * 6.1 of Dimitra Giannakopoulou and Flavio Lerda 2002,
 * <em>From States to Transitions: Improving Translation of
 * LTL Formulae to Buchi Automata</em>. The data are printed
 * to standard output, the gnuplot commands to display them are:<br />
 * <code>set key autotitle columnheader</code><br />
 * <code>plot [0:<em>L</em>] [0:1] for [i in "2 4 6 8"] "data.file" using 1:i:(column(i+1)) with errorbars</code><br />
 * <p>
 * Usage: <code>java gov.nasa.ltl.tests.RandomFormulae [-N n]
 * [-L min max inc] [-F f] [-P p] [-seed s] [-optimise] [-verbose]</code>
 * <p>
 * If the verbose flag is set, extra information is printed to standard
 * error in gnuplot comment format (so redirecting standard output and
 * error to the same file still yields valid input for gnuplot).
 * @author Ewgenij Starostin
 *
 */
public class RandomFormulae {
  /**
   * Infinite iterator which returns a random {@link Formula} of
   * the required length every time.
   * @author Ewgenij Starostin
   *
   */
  private static class FormulaGenerator implements Iterator<Formula<Integer>> {
    private int length, names;
    private double pUorV;
    private Random rand;

    /**
     * Set up the iterator.
     * @param length length of every generated formula
     * @param names number of available atoms
     * @param pUorV probability of choosing a U or V operator
     * @param rand random number generator
     */
    public FormulaGenerator (int length, int names, double pUorV, Random rand) {
      assert 0 <= pUorV && pUorV <= 1 : "bad probability for U or V";
      assert length > 0 : "length must be positive";
      assert names > 0 : "need at least one name";
      assert rand != null : "need a random number generator";
      this.length = length;
      this.names = names;
      this.pUorV = pUorV;
      this.rand = rand;
    }

    /**
     * No-op.
     */
    @Override public boolean hasNext () { return true; }

    /**
     * Returns a new random formula.
     */
    @Override
    public Formula<Integer> next () {
      return randomFormula (length);
    }
    
    /**
     * Recursively generate a random formula.
     * @param length length of the desired subformula
     * @return
     */
    private Formula<Integer> randomFormula (int length) {
      int name = 0, sublength;
      @SuppressWarnings ("unused")
      Formula<Integer> f = null, s1 = null, s2 = null;
      LTL2Buchi.debug = false;
      switch (length) {
      case 0:
        assert false : "formulae cannot have zero length";
        return null;
      case 1:
        name = rand.nextInt (names);
        return f = Formula.Proposition (name);
      case 2:
        s1 = randomFormula (1);
        if (rand.nextDouble () < 0.5)
          return Formula.Not (s1);
        else
          return Formula.Next (s1);
      default:
        sublength = 1 + rand.nextInt (length - 2);
        if (rand.nextDouble () < pUorV) {
          s1 = randomFormula (sublength);
          s2 = randomFormula (length - sublength - 1);
          if (rand.nextDouble () < 0.5)
            return f = Formula.Until (s1, s2);
          else
            return f = Formula.Release (s1, s2);
        } else {
          if (rand.nextDouble () < 0.5) {
            s1 = randomFormula (length - 1);
            if (rand.nextDouble () < 0.5)
              return Formula.Not (s1);
            else
              return Formula.Next (s1);
          } else {
            s1 = randomFormula (sublength);
            s2 = randomFormula (length - sublength - 1);
            if (rand.nextDouble () < 0.5)
              return f = Formula.And (s1, s2);
            else
              return f = Formula.Or (s1, s2);
          }
        }
      }
    }

    /**
     * No-op.
     */
    @Override public void remove () {}
  }

  /**
   * Infinite list of random {@link Formula}e of a specified length.
   * @author Ewgenij Starostin
   *
   */
  public static class FormulaSource implements Iterable<Formula<Integer>> {
    private FormulaGenerator gen;

    /**
     * Set up list.
     * @param length length of each generated formula
     * @param names number of available atoms
     * @param pUorV probability of choosing U and V operators
     * @param rand random number generator
     */
    public FormulaSource (int length, int names, double pUorV, Random rand) {
      gen = new FormulaGenerator (length, names, pUorV, rand);
    }
    
    /**
     * Get a random formula.
     */
    @Override
    public Iterator<Formula<Integer>> iterator () {
      return gen;
    }
  }
  
  public static int N = 3, Lmin = 5, Lmax = 30, Linc = 5,
    F = 100;
  public static long seed = 0;
  public static boolean haveSeed = false, optimise = false, verbose = false;
  public static double P = 1.0/3.0;
  // Save given string representation of P, if any, to avoid rounding issues.
  private static String Pstr = null;
  private static boolean haveCpuTime;

  /**
   * @param args
   */
  public static void main (String[] args) {
    Random rand;
    ThreadMXBean tb = ManagementFactory.getThreadMXBean ();
    parseArgs (args);
    if (!haveSeed)
      seed = System.nanoTime ();
    rand = new Random (seed);
    // Make sure the number we print is the number well use.
    if (Pstr == null) {
      Pstr = "" + P;
      P = Double.valueOf (Pstr);
    }
    if (tb.isCurrentThreadCpuTimeSupported ()) {
      haveCpuTime = true;
      tb.setThreadCpuTimeEnabled (true);
    }
    verbose ("Parameters:\n" +
             "-N " + N + " -L " + Lmin + " " + Lmax + " " + Linc +
             " -P " + Pstr + " -F " + F + " -seed " + seed +
             (optimise ? " -optimise" : "") +
             (verbose ? " -verbose" : "") + "\n");
    verbose ("Plot me with:\n" +
             "set key autotitle columnheader\n" +
             "plot [0:" + (Lmax + Linc) + "] [0:1] for [i in \"2 4 6 8\"] " +
             "\"data.file\" using 1:i:(column(i+1)) with errorbars\n");
    System.out.println ("\"L\" \"GBA states\" \"\" \"GBA transitions\" " +
        "\"\" \"BA states\" \"\" \"BA transitions\" \"\"");
    System.out.flush ();
    for (int L = Lmin; L <= Lmax; L += Linc) {
      FormulaSource formulae = new FormulaSource (L, N, P, rand);
      int i = 0;
      double[] baStates = new double[F], baTrans = new double[F],
        gbaStates = new double[F], gbaTrans = new double[F];
      long[] autTime = new long[F], buTime = new long[F];
      long time = 0;
      for (Formula<Integer> f: formulae) {
        if (i >= F)
          break;
        Graph<Integer> baAut, gbaAut, baBu, gbaBu;
        if (optimise)
          f = new Rewriter<Integer> (f).rewrite ();
        Translator.setAlgorithm (Translator.Algorithm.LTL2AUT);
        if (haveCpuTime)
          time = tb.getCurrentThreadCpuTime ();
        gbaAut = Translator.translate (f);
        if (haveCpuTime)
          autTime[i] = tb.getCurrentThreadCpuTime () - time;
        if (optimise)
          gbaAut = SuperSetReduction.reduce (gbaAut);
        baAut = Degeneralize.degeneralize (gbaAut);
        if (optimise)
          baAut = SFSReduction.reduce (
              Simplify.simplify (SCCReduction.reduce (baAut)));
        /* If baAut ends up empty, the formula was unsatisfiable.
         * Well skip these.
         */
        if (baAut.getNodeCount () == 0 || baAut.getEdgeCount () == 0) {
          Formula.resetStatic ();
          continue;
        }
        // If baAut is non-empty, gbaAut is non-empty too.
        assert gbaAut.getNodeCount () > 0 && gbaAut.getEdgeCount () > 0;
        Translator.setAlgorithm (Translator.Algorithm.LTL2BUCHI);
        if (haveCpuTime)
          time = tb.getCurrentThreadCpuTime ();
        gbaBu = Translator.translate (f);
        if (haveCpuTime)
          buTime[i] = tb.getCurrentThreadCpuTime () - time;
        if (optimise)
          gbaBu = SuperSetReduction.reduce (gbaBu);
        baBu = Degeneralize.degeneralize (gbaBu);
        if (optimise)
          baBu = SFSReduction.reduce (
              Simplify.simplify (SCCReduction.reduce (baBu)));
        gbaStates[i] = gbaBu.getNodeCount () / (double)gbaAut.getNodeCount ();
        gbaTrans[i] = gbaBu.getEdgeCount () / (double)gbaAut.getEdgeCount ();
        baStates[i] = baBu.getNodeCount () / (double)baAut.getNodeCount ();
        baTrans[i] = baBu.getEdgeCount () / (double)baAut.getEdgeCount ();
        Formula.resetStatic ();
        i++;
      }
      output (L, gbaStates, gbaTrans, baStates, baTrans, autTime, buTime);
    }
  }
  
  private static void parseArgs (String[] args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals ("-N")) {
        if (i + 1 >= args.length)
          usage ();
        try {
          N = Integer.parseInt (args[i + 1]);
        } catch (NumberFormatException e) {
          usage ();
        }
        if (N < 1)
          usage ();
        i++;
      } else if (args[i].equals ("-L")) {
        if (i + 3 >= args.length)
          usage ();
        try {
          Lmin = Integer.parseInt (args[i + 1]);
          Lmax = Integer.parseInt (args[i + 2]);
          Linc = Integer.parseInt (args[i + 3]);
        } catch (NumberFormatException e) {
          usage ();
        }
        if (Lmin < 1 || Lmax < 1 || Lmax < Lmin || Linc < 1)
          usage ();
        i += 3;
      } else if (args[i].equals ("-F")) {
        if (i + 1 >= args.length)
          usage ();
        try {
          F = Integer.parseInt (args[i + 1]);
        } catch (NumberFormatException e) {
          usage ();
        }
        i++;
      } else if (args[i].equals ("-P")) {
        if (i + 1 >= args.length)
          usage ();
        try {
          P = Double.parseDouble (args[i + 1]);
        } catch (NumberFormatException e) {
          usage ();
        }
        if (P < 0 || P > 1)
          usage ();
        Pstr = args[i + 1];
        i++;
      } else if (args[i].equals ("-seed")) {
        if (i + 1 >= args.length)
          usage ();
        try {
          seed = Long.parseLong (args[i + 1]);
          haveSeed = true;
        } catch (NumberFormatException e) {
          usage ();
        }
        i++;
      } else if (args[i].equals ("-optimise")) {
        optimise = true;
      } else if (args[i].equals ("-verbose")) {
        verbose = true;
      } else
        usage ();
    }
  }
  
  private static void usage () {
    System.err.println ("usage: gov.nasa.ltl.tests.RandomFormulae " +
          "[-N n] [-L min max inc] [-F f] [-P p] [-seed s] " +
          "[-optimise] [-verbose]");
    System.err.flush ();
    System.exit (1);
  }
  
  private static void verbose (String msg) {
    if (verbose)
      System.err.println ("# " + msg.replaceAll ("\n", "\n# "));
  }
  
  private static void output (int L, double[] gbaStates,
      double[] gbaTrans, double[] baStates, double[] baTrans,
      long[] autTime, long[] buTime) {
    double gbaStatesM = mean (gbaStates), gbaTransM = mean (gbaTrans),
           baStatesM = mean (baStates), baTransM = mean (baTrans),
           gbaStatesD = sigma (gbaStates, gbaStatesM),
           gbaTransD = sigma (gbaTrans, gbaTransM),
           baStatesD = sigma (baStates, baStatesM),
           baTransD = sigma (baTrans, baTransM),
           autM = mean (autTime), buM = mean (buTime);
    if (haveCpuTime)
      verbose ("L = " + L + " average LTL2AUT time = " + (autM / 1E6) +
               " ms, average LTL2Buchi time = " + (buM / 1E6) + " ms");
    System.out.println ("" + L + " " + gbaStatesM + " " +
        gbaStatesD + " " + gbaTransM + " " + gbaTransD + " " +
        baStatesM + " " + baStatesD + " " + baTransM + " " + baTransD);
    System.out.flush ();
  }
  
  private static double mean(double[] values) {
    double s = 0;
    for (int i = 0; i < values.length; i++)
      s += values[i];
    return s / values.length;
  }
  
  private static double mean(long[] values) {
    double s = 0;
    for (int i = 0; i < values.length; i++)
      s += values[i];
    return s / values.length;
  }
  
  private static double sigma(double[] values, double mean) {
    double s = 0;
    for (int i = 0; i < values.length; i++)
      s += (values[i] - mean) * (values[i] - mean);
    return Math.sqrt (s / values.length);
  }
}

