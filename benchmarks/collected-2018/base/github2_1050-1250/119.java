// https://searchcode.com/api/result/112652509/

/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    MatlabNMF.java
 *    Copyright (C) 2002 Sugato Basu, Mikhail Bilenko and Yuk Wah Wong
 *
 */

package weka.attributeSelection;

import  java.io.*;
import  java.util.*;
import  weka.core.*;
import  weka.filters.unsupervised.attribute.ReplaceMissingValues;
import  weka.filters.unsupervised.attribute.Normalize;
import  weka.filters.unsupervised.attribute.NominalToBinary;
import  weka.filters.unsupervised.attribute.Remove;
import  weka.filters.Filter;

/**
 * Class for performing non-negative matrix factorization/transformation. <p>
 *
 * Valid options are:<p>
 * -D <br>
 * Don't normalize the input data. <p>
 *
 * -R <rank> <br>
 * Set rank of the basis matrix.  Default=5. <p>
 *
 * -n <iterations> <br>
 * Set number of iterations.  Default=20. <p>
 *
 * -O <objective function> <br>
 * Set objective function.  Default=1. <br>
 * [1: sum_{i,u}[V_{iu}log(WH)_{iu}-(WH)_{iu}]; 2: norm(V-WH); 3: D(V||WH)] <p>
 *
 * -E <evaluator spec> <br>
 * Set attribute evaluator.  Evaluator spec should contain the full
 * class name of an attribute evaluator followed by any options. <p>
 *
 * 
 * @author Misha Bilenko (mbilenko@cs.utexas.edu)
 * @author Sugato Basu (sugato@cs.utexas.edu)
 * @author Yuk Wah Wong (ywwong@cs.utexas.edu)
 * @version $Revision: 1.1.1.1 $
 */
public class MatlabNMF extends AttributeEvaluator 
  implements AttributeTransformer, OptionHandler {
  
  /** The data to transform analyse/transform */
  private Instances m_trainInstances;

  /** Keep a copy for the class attribute (if set) */
  private Instances m_trainCopy;

  /** The header for the transformed data format */
  private Instances m_transformedFormat;

  /** Data has a class set */
  private boolean m_hasClass;

  /** Number of attributes */
  private int m_numAttribs;

  /** Number of instances */
  private int m_numInstances;

  /** Name of temp directory */
  private String m_tmpDir = new String("/tmp/");

  /** Name of the Matlab program file that computes NMF */
  protected String m_mFile = new String(m_tmpDir+"MatlabNMF.m");

  /** Will hold the orthogonal basis of the original data */
  private double [][] m_basis;

  /** A timestamp suffix for matching vectors with attributes */
  String m_timestamp = null;

  /** Name of the file where attribute names will be stored */
  String m_nmfAttributeFilenameBase = new String(m_tmpDir+"NMFattributes");
  
  /** Name of the file where original data V (or v) will be stored */
  String m_dataFilename = new String(m_tmpDir+"NMFdata.txt");

  /** Name of the file where other parameters will be stored */
  String m_paramFilename = new String(m_tmpDir+"NMFparam.txt");

  /** Name of the file where the basis W will be stored */
  String m_basisFilename = new String(m_tmpDir+"NMFbasis.txt");

  /** Name of the file where the basis vectors in decreasing order of
      ranking will be stored */
  String m_rankedBasisFilenameBase = new String(m_tmpDir+"NMFrankedbasis");

  /** Name of the file where the encoding H (or h) of V (or v) will be
      stored */
  String m_encodingFilename = new String(m_tmpDir+"NMFencoding.txt");

  /** Filters for original data */
  private ReplaceMissingValues m_replaceMissingFilter;
  private Normalize m_normalizeFilter;
  private Remove m_attributeFilter;
  
  /** normalize the input data? */
  private boolean m_normalize = false;

  /** The number of attributes in the NMF transformed data (i.e. rank
      of the basis matrix) */
  private int m_rank = 5;
  
  /** The number of iterations for gradient descent */
  private int m_iter = 20;

  /** The objective function */
  private int m_obj = 1;

  /** The attribute evaluator to use */
  private ASEvaluation m_eval =
    new weka.attributeSelection.ChiSquaredAttributeEval();

  /**
   * Returns a string describing this attribute transformer
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Performs non-negative matrix factorization. "
      +"Use in conjunction with a Ranker search.";
  }

  /**
   * Returns an enumeration describing the available options. <p>
   *
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(5);
    newVector.addElement(new Option("\tDon't normalize input data." 
                                    , "D", 0, "-D"));

    newVector.addElement
        (new Option("\tSet rank of the basis matrix.  Default=5."
                    , "R", 1, "-R <rank>"));

    newVector.addElement
        (new Option("\tSet number of iterations.  Default=20."
                    , "n", 1, "-n <iterations>"));

    newVector.addElement
        (new Option("\tSet objective function.  Default=1.\n"
                    +"\t[1: sum_{i,u}[V_{iu}log(WH)_{iu}-(WH)_{iu}]; "
                    +"2: norm(V-WH); 3: D(V||WH)]"
                    , "O", 1, "-O <objection function>"));

    newVector.addElement
      (new Option("\tSet attribute evaluator.  Full class name of attribute\n"
                  +"\tevaluator, followed by its options.\n"
                  +"\teg: \"weka.attributeSelection.ChiSquaredAttributeEval\"",
                  "E", 1, "-E <evaluator spec>"));

    return  newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   * Valid options are:<p>
   * -D <br>
   * Don't normalize the input data. <p>
   *
   * -R <rank> <br>
   * Set rank of the basis matrix.  Default=5. <p>
   *
   * -n <iterations> <br>
   * Set number of iterations.  Default=20. <p>
   *
   * -O <objective function> <br>
   * Set objective function.  Default=1. <br>
   * [1: sum_{i,u}[V_{iu}log(WH)_{iu}-(WH)_{iu}]; 2: norm(V-WH);
   *  3: D(V||WH)] <p>
   *
   * -E <evaluator spec> <br>
   * Set attribute evaluator.  Evaluator spec should contain the full
   * class name of an attribute evaluator followed by any options. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions (String[] options)
    throws Exception
  {
    resetOptions();
    String optionString;

    setNormalize(!Utils.getFlag('D', options));

    optionString = Utils.getOption('R', options);
    if (optionString.length() != 0) {
      Integer temp;
      temp = Integer.valueOf(optionString);
      setRank(temp.intValue());
    }

    optionString = Utils.getOption('n', options);
    if (optionString.length() != 0) {
      Integer temp;
      temp = Integer.valueOf(optionString);
      setIterations(temp.intValue());
    }

    optionString = Utils.getOption('O', options);
    if (optionString.length() != 0) {
      Integer temp;
      temp = Integer.valueOf(optionString);
      setObjectiveFunction(temp.intValue());
    }

    optionString = Utils.getOption('E', options);
    if (optionString.length() == 0) {
      throw new Exception("An attribute evaluator must be specified"
			  + " with the -E option.");
    }
    String[] evaluatorSpec = Utils.splitOptions(optionString);
    if (evaluatorSpec.length == 0) {
      throw new Exception("Invalid attribute evaluator specification string");
    }
    String evaluatorName = evaluatorSpec[0];
    evaluatorSpec[0] = "";
    setEvaluator(ASEvaluation.forName(evaluatorName, evaluatorSpec));
  }

  /**
   * Reset to defaults
   */
  private void resetOptions() {
    m_normalize = false;
    m_rank = 5;
    m_iter = 20;
    m_obj = 1;
    m_eval = new weka.attributeSelection.ChiSquaredAttributeEval();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String normalizeTipText() {
    return "Normalize input data.";
  }

  /**
   * Set whether input data will be normalized.
   * @param n true if input data is to be normalized
   */
  public void setNormalize(boolean n) {
    m_normalize = n;
  }

  /**
   * Gets whether or not input data is to be normalized
   * @return true if input data is to be normalized
   */
  public boolean getNormalize() {
    return m_normalize;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String rankTipText() {
    return "Set rank of the basis matrix.";
  }

  /**
   * Sets the rank of the basis matrix W.
   * @param r the rank of the basis matrix
   */
  public void setRank(int r) {
    m_rank = r;
  }

  /**
   * Gets the rank of the basis matrix W.
   * @return the rank of the basis matrix
   */
  public int getRank() {
    return m_rank;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String iterationsTipText() {
    return "Set number of iterations.";
  }

  /**
   * Sets the number of iterations for gradient descent.
   * @param i the number of iterations
   */
  public void setIterations(int i) {
    m_iter = i;
  }

  /**
   * Gets the number of iterations for gradient descent.
   * @return the nubmer of iterations
   */
  public int getIterations() {
    return m_iter;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String objectiveFunctionTipText() {
    return "Set objective function. "
      +"[1:sum(V*log(WH)-WH); 2:norm; 3:divergence]";
  }

  /**
   * Sets the objective function.
   * @param i the objective function
   */
  public void setObjectiveFunction(int i) {
    m_obj = i;
  }

  /**
   * Gets the objective function.
   * @return the objective function
   */
  public int getObjectiveFunction() {
    return m_obj;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String evaluatorTipText() {
    return "Set the attribute evaluator to use. This evaluator is used "
      +"during the attribute selection phase before the classifier is "
      +"invoked.";
  }

  /**
   * Sets the attribute evaluator
   *
   * @param evaluator the evaluator with all options set.
   */
  public void setEvaluator(ASEvaluation evaluator) {
    m_eval = evaluator;
  }

  /**
   * Gets the attribute evaluator used
   *
   * @return the attribute evaluator
   */
  public ASEvaluation getEvaluator() {
    return m_eval;
  }

  /**
   * Gets the evaluator specification string, which contains the class
   * name of the attribute evaluator and any options to it
   *
   * @return the evaluator string.
   */
  protected String getEvaluatorSpec() {
    ASEvaluation e = getEvaluator();
    if (e instanceof OptionHandler) {
      return e.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)e).getOptions());
    }
    return e.getClass().getName();
  }

  /**
   * Gets the current settings of MatlabNMF
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {

    String[] options = new String[9];
    int current = 0;

    if (!getNormalize()) {
      options[current++] = "-D";
    }

    options[current++] = "-R"; options[current++] = ""+getRank();

    options[current++] = "-n"; options[current++] = ""+getIterations();

    options[current++] = "-O"; options[current++] = ""+getObjectiveFunction();

    options[current++] = "-E"; options[current++] = ""+getEvaluatorSpec();
    
    while (current < options.length) {
      options[current++] = "";
    }
    
    return  options;
  }

  /**
   * Initializes NMF.
   * @param data the instances to analyze
   * @exception Exception if analysis fails
   */
  public void buildEvaluator(Instances data) throws Exception {
    buildAttributeConstructor(data);
  }

  private void buildAttributeConstructor (Instances data) throws Exception {
    m_basis = null;
    m_attributeFilter = null;

    if (data.checkForStringAttributes()) {
      throw  new UnsupportedAttributeTypeException("Can't handle string attributes!");
    }
    m_trainInstances = data;

    // make a copy of the training data so that we can get the class
    // column to append to the transformed data (if necessary)
    m_trainCopy = new Instances(m_trainInstances);
    System.out.println("Copied instances");
    m_replaceMissingFilter = new ReplaceMissingValues();
    m_replaceMissingFilter.setInputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, 
                                        m_replaceMissingFilter);
    System.out.println("Replaced missing values");

    if (m_normalize) {
      m_normalizeFilter = new Normalize();
      m_normalizeFilter.setInputFormat(m_trainInstances);
      m_trainInstances = Filter.useFilter(m_trainInstances, m_normalizeFilter);
      System.out.println("Normalized");
    }

    // delete any attributes with only one distinct value or are all missing
    Vector deleteCols = new Vector();
    for (int i=0;i<m_trainInstances.numAttributes();i++) {
      if (m_trainInstances.numDistinctValues(i) <=1) {
        deleteCols.addElement(new Integer(i));
      }
    }
    System.out.println("Deleted single-value attributes");
    
    if (m_trainInstances.classIndex() >=0) {
      // get rid of the class column
      m_hasClass = true;
      deleteCols.addElement(new Integer(m_trainInstances.classIndex()));
    }

    // remove columns from the data if necessary
    if (deleteCols.size() > 0) {
      m_attributeFilter = new Remove();
      int [] todelete = new int [deleteCols.size()];
      for (int i=0;i<deleteCols.size();i++) {
        todelete[i] = ((Integer)(deleteCols.elementAt(i))).intValue();
      }
      m_attributeFilter.setAttributeIndicesArray(todelete);
      m_attributeFilter.setInvertSelection(false);
      m_attributeFilter.setInputFormat(m_trainInstances);
      m_trainInstances = Filter.useFilter(m_trainInstances, m_attributeFilter);
    }
    System.out.println("Removed attributes filtered above");
    
    m_numInstances = m_trainInstances.numInstances();
    m_numAttribs = m_trainInstances.numAttributes();  // w/o class index

    // Reduce output rank when # attributes falls below that
    if (getRank() > m_numAttribs) {
      System.out.println("Reduce rank to number of attributes");
      setRank(m_numAttribs);
    }
    
    System.out.println("About to run NMF in matlab with " + m_numAttribs + " attributes");
    dumpScripts();
    dumpInstances(m_dataFilename);
    int[] params = new int[3];
    params[0] = getRank();
    params[1] = getIterations();
    params[2] = getObjectiveFunction();
    dumpVector(m_paramFilename, params, 3);
    runMatlab(m_mFile);
    System.out.println("Done training");

    m_basis = readVectors(m_basisFilename, -1);
    System.out.println("Successfully parsed matlab output files");

    m_transformedFormat = setOutputFormat();

    // Build the attribute evaluator
    if (!(m_eval instanceof AttributeEvaluator)) {
      throw new Exception("Invalid attribute evaluator!");
    }
    m_eval.buildEvaluator(transformedData());

    // Save the basis vectors in decreasing order of ranking
    double[] merit = new double[m_rank];
    for (int i = 0; i < m_rank; ++i)
      merit[i] = ((AttributeEvaluator) m_eval).evaluateAttribute(i);
    int[] pos = Utils.sort(merit);
    int[] bestToWorst = new int[m_rank];
    for (int i = 0; i < m_rank; ++i)
      bestToWorst[m_rank-pos[i]-1] = i;
    try {
      // Save attribute names first
      m_timestamp = MatlabPCA.getLogTimestamp();
      MatlabPCA.dumpAttributeNames
        (m_trainInstances, m_nmfAttributeFilenameBase+m_timestamp+".txt");
      // Then save the basis vectors in order
      PrintWriter writer =
        new PrintWriter
        (new BufferedOutputStream
         (new FileOutputStream(m_rankedBasisFilenameBase+m_timestamp+".txt")));
      for (int i = 0; i < m_numAttribs; ++i) {
        for (int j = 0; j < m_rank; ++j)
          writer.print(m_basis[i][bestToWorst[j]] + " ");
        writer.println();
      }
      writer.close();
    } catch (Exception e) {
      System.err.println("Could not create a temporary file for dumping basis vectors: " + e);
    }
  }


  /** Read column vectors from a text file
   * @param name file name
   * @param maxVectors max number of vectors to read, -1 to read all
   * @return double[][] array corresponding to vectors
   */
  public double[][] readVectors(String name, int maxVectors) throws Exception {
    BufferedReader r = new BufferedReader(new FileReader(name));
    int numAttributes=-1, numVectors=-1;

    // number of attributes
    String s =  r.readLine();
    try {
      numAttributes = (int) Double.parseDouble(s);
    } catch (Exception e) {
      System.err.println("Couldn't parse " + s + " as int");
    }
     
    // number of vectors
    s = r.readLine();
    try { 
      numVectors = (int) Double.parseDouble(s);
    } catch (Exception e) {
      System.err.println("Couldn't parse " + s + " as int");
    }

    double[][] vectors = new double[numAttributes][numVectors];
    int i = 0;
    while ((s = r.readLine()) != null) {
      StringTokenizer tokenizer = new StringTokenizer(s);
      int j = 0;
      while (tokenizer.hasMoreTokens()) {
        String value = tokenizer.nextToken();
        try { 
          vectors[i][j] = Double.parseDouble(value);
        } catch (Exception e) {
          System.err.println("Couldn't parse " + value + " as double");
        }
        j++;
        if (j > numVectors) {
          System.err.println("Too many vectors in line: " + s);
        }
      }
      if (j != numVectors) {
        System.err.println("Too few vectors in line: " + s);
      }
      i++;
      if (i > numAttributes) {
        System.err.println("Too many attributes: " + i + " expecting " + numAttributes + " attributes");
      }
    }
    if (i != numAttributes) {
      System.err.println("Too few attributes: " + i + " expecting " + numAttributes + " attributes");
    }
    return vectors;
  }


   /** Read a column vector from a text file
    * @param vector array into which the column vector is stored
    * @param name file name
    * @returns double[] array corresponding to a vector
    */
  public void readVector(double[] vector, String name) throws Exception {
    // Determine the dimensionality from the first line
     BufferedReader r = new BufferedReader(new FileReader(name));
     int numAttributes = -1;
     
     // Read the number of attributes
     String s =  r.readLine();
     try { 
       numAttributes = (int) Double.parseDouble(s);
     } catch (Exception e) {
       System.err.println("Couldn't parse " + s + " as int");
     }

     // Assume vector has enough space
     int i = 0; 
     while ((s = r.readLine()) != null) {
       try { 
         vector[i] = Double.parseDouble(s);
       } catch (Exception e) {
         System.err.println("Couldn't parse " + s + " as double");
       }
       i++;
       if (i > numAttributes) {
         System.err.println("Too many attributes: " + i + " expecting " + numAttributes + " attributes");
       }
     }
     if (i != numAttributes) {
       System.err.println("Too few attributes: " + i + " expecting " + numAttributes + " attributes");
     }
  }

  /**
   * Returns just the header for the transformed data (ie. an empty
   * set of instances. This is so that AttributeSelection can
   * determine the structure of the transformed data without actually
   * having to get all the transformed data through getTransformedData().
   * @return the header of the transformed data.
   * @exception Exception if the header of the transformed data can't
   * be determined.
   */
  public Instances transformedHeader() throws Exception {
    if (m_basis == null) {
      throw new Exception("Basis hasn't been formed yet");
    }
    return m_transformedFormat;
  }

  /**
   * Gets the transformed training data.
   * @return the transformed training data
   * @exception Exception if transformed data can't be returned
   */
  public Instances transformedData() throws Exception {
    if (m_basis == null) {
      throw new Exception("Basis hasn't been formed yet");
    }
    Instances output;

    output = new Instances(m_transformedFormat);
    double[][] encoding = readVectors(m_encodingFilename, -1);
    for (int i = 0; i < m_trainCopy.numInstances(); ++i) {
      Instance inst = m_trainCopy.instance(i);
      double[] h = null;
      if (m_hasClass) {
        h = new double[m_rank+1];
        h[m_rank] = inst.value(inst.classIndex());
      } else
        h = new double[m_rank];
      for (int j = 0; j < m_rank; ++j)
        h[j] = encoding[j][i];
      output.add(new Instance(inst.weight(), h));
    }
    return output;
  }

  /**
   * Evaluates the merit of a transformed attribute.
   * @param att the attribute to be evaluated
   * @return the merit of a transformed attribute
   * @exception Exception if attribute can't be evaluated
   */
  public double evaluateAttribute(int att) throws Exception {
    if (m_basis == null) {
      throw new Exception("Basis hasn't been formed yet!");
    }
    if (!(m_eval instanceof AttributeEvaluator)) {
      throw new Exception("Invalid attribute evaluator!");
    }
    return ((AttributeEvaluator)m_eval).evaluateAttribute(att);
  }

  /**
   * Dump scripts into temporary files
   */
  private void dumpScripts() {
    try {
      PrintWriter nmf = new PrintWriter(new BufferedOutputStream(new FileOutputStream(m_mFile)));
      nmf.print
        ("V=load('" + m_dataFilename + "');\n"+
         "param=load('" + m_paramFilename + "');\n"+
         "r=param(1);\n"+
         "maxiter=param(2);\n"+
         "obj=param(3);\n"+
         "[n m]=size(V);\n"+
         "W=rand(n,r);\n"+
         "H=rand(r,m);\n"+
         "eps=1e-9;\n"+
         "if obj == 1\n"+
         "W=W./(ones(n,1)*sum(W));\n"+
         "end\n"+
         "for iter=1:maxiter\n"+
         "switch obj\n"+
         "case 1\n"+
         "H=H.*(W'*((V+eps)./(W*H+eps)));\n"+
         "W=W.*(((V+eps)./(W*H+eps))*H');\n"+
         "W=W./(ones(n,1)*sum(W));\n"+
         "case 2\n"+
         "H=H.*((W'*V+eps)./(W'*W*H+eps));\n"+
         "W=W.*((V*H'+eps)./(W*H*H'+eps));\n"+
         "case 3\n"+
         "H=H.*((W'*((V+eps)./(W*H+eps)))./((sum(W))'*ones(1,m)));\n"+
         "W=W.*((((V+eps)./(W*H+eps))*H')./(ones(n,1)*(sum(H,2))'));\n"+
         "end\n"+
         "end\n"+
         "save '" + m_basisFilename + "' n r W -ASCII -DOUBLE\n"+
         "save '" + m_encodingFilename + "' r m H -ASCII -DOUBLE\n");
      nmf.close();
    } catch (Exception e) {
      System.err.println("Could not create temporary files for dumping the scripts: " + e);
    }
  }

  /**
   * Dump covariance matrix into a file
   */
  private void dumpInstances(String tempFile) {
    try { 
      PrintWriter writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(tempFile)));
      for (int j = 0; j < m_numAttribs; j++) {
        for (int k = 0; k < m_numInstances; k++) {
          Instance instance = m_trainInstances.instance(k);
          writer.print(instance.value(j) + " ");
        }
        writer.println();
      }
      writer.close();
    } catch (Exception e) {
      System.err.println("Could not create a temporary file for dumping the covariance matrix: " + e);
    }
  }

  /**
   * Dump a column vector of size n into a file
   */
  private void dumpInstance(String tempFile, Instance instance) {
    try { 
      PrintWriter writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(tempFile)));
      for (int j = 0; j < m_numAttribs; j++) {
        writer.println(instance.value(j));
      }
      writer.close();
    } catch (Exception e) {
      System.err.println("Could not create a temporary file for dumping the column vector: " + e);
    }
  }

  /**
   * Dump an integer array into a file
   */
  private void dumpVector(String tempFile, int[] a, int n) {
    try { 
      PrintWriter writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(tempFile)));
      for (int j = 0; j < n; j++) {
        writer.println(a[j]);
      }
      writer.close();
    } catch (Exception e) {
      System.err.println("Could not create a temporary file for dumping the column vector: " + e);
    }
  }

  /** Run matlab in command line with a given argument
   * @param inFile file to be input to Matlab
   */
  public static void runMatlab(String inFile) {
    // call matlab to do the dirty work
    try {
      int exitValue;
      do {
        Process proc = Runtime.getRuntime().exec("matlab -tty < " + inFile);
        exitValue = proc.waitFor();
	if (exitValue != 0) {
	  System.err.println("Abnormal termination, trying again later!");
	  Thread.sleep(300000);
	}
      } while (exitValue != 0);
    } catch (Exception e) {
      System.err.println("Problems running matlab: " + e);
    }
  }

  /**
   * Return a summary of the analysis
   * @return a summary of the analysis.
   */
  private String NMFSummary() {
    StringBuffer result = new StringBuffer();
    Instances output = null;
    int numVectors=0;

    try {
      output = setOutputFormat();
      numVectors = (output.classIndex() < 0) 
        ? output.numAttributes()
        : output.numAttributes()-1;
    } catch (Exception ex) {
    }
    System.out.println("Sanity check:  numVectors=" + numVectors);

    result.append("Basis vectors:\n");
    for (int j = 1;j <= numVectors;j++) {
      result.append(" V"+j+"\t");
    }
    result.append("\n");
    for (int j = 0; j < m_numAttribs; j++) {
      for (int i = 0; i < numVectors; i++)
        result.append(Utils.doubleToString(m_basis[j][i],7,4)+"\t");
      result.append(m_trainInstances.attribute(j).name()+"\n");
    }
    result.append("\nAttribute ranking filter:\n");
    result.append(m_eval.toString());

    return result.toString();
  }

  /**
   * Returns a description of this attribute transformer
   * @return a String describing this attribute transformer
   */
  public String toString() {
    if (m_basis == null) {
      return "Basis hasn't been formed yet!";
    } else {
      return "\tNMF Attribute Transformer\n\n"
        + NMFSummary();
    }
  }

  /**
   * Return a matrix as a String
   * @param matrix that is decribed as a string
   * @return a String describing a matrix
   */
  private String matrixToString(double [][] matrix) {
    StringBuffer result = new StringBuffer();
    int last = matrix.length - 1;

    for (int i = 0; i <= last; i++) {
      for (int j = 0; j <= last; j++) {
        result.append(Utils.doubleToString(matrix[i][j],6,2)+" ");
        if (j == last) {
          result.append('\n');
        }
      }
    }
    return result.toString();
  }

  /**
   * Transform an instance in original (unnormalized) format. Convert back
   * to the original space if requested.
   * @param instance an instance in the original (unnormalized) format
   * @return a transformed instance
   * @exception Exception if instance cant be transformed
   */
  public Instance convertInstance(Instance instance) throws Exception {

    if (m_basis == null) {
      throw new Exception("convertInstance: Basis not formed yet");
    }

    Instance tempInst = (Instance)instance.copy();
    if (!instance.equalHeaders(m_trainCopy.instance(0))) {
      throw new Exception("Can't convert instance: headers don't match: "
                          +"MatlabNMF");
    }

    m_replaceMissingFilter.input(tempInst);
    m_replaceMissingFilter.batchFinished();
    tempInst = m_replaceMissingFilter.output();

    if (m_normalize) {
      m_normalizeFilter.input(tempInst);
      m_normalizeFilter.batchFinished();
      tempInst = m_normalizeFilter.output();
    }

    if (m_attributeFilter != null) {
      m_attributeFilter.input(tempInst);
      m_attributeFilter.batchFinished();
      tempInst = m_attributeFilter.output();
    }

    // Wanted to do it on Matlab but it's too expensive
    double[] v = new double[m_numAttribs];
    double eps = 1e-9;
    for (int i = 0; i < m_numAttribs; ++i)
      v[i] = tempInst.value(i);
    double[][] W = m_basis;
    double[] h = null;
    if (m_hasClass)
      h = new double[m_rank + 1];
    else
      h = new double[m_rank];
    for (int i = 0; i < m_rank; ++i)
      h[i] = Math.random();
    double[] t1 = new double[m_numAttribs];
    double[] t2 = new double[m_rank];
    double[] t3 = new double[m_rank];
    for (int i = 0; i < m_iter; ++i) {
      switch (m_obj) {
      case 1:
        // t1 = W*h+eps
        for (int j = 0; j < m_numAttribs; ++j) {
          t1[j] = eps;
          for (int k = 0; k < m_rank; ++k)
            t1[j] += W[j][k] * h[k];
        }
        // t1 = (v+eps)./t1
        for (int j = 0; j < m_numAttribs; ++j)
          t1[j] = (v[j]+eps) / t1[j];
        // t2 = W'*t1
        for (int j = 0; j < m_rank; ++j) {
          t2[j] = 0.0;
          for (int k = 0; k < m_numAttribs; ++k)
            t2[j] += W[k][j] * t1[k];
        }
        break;
      case 2:
        // t2 = W'*v
        for (int j = 0; j < m_rank; ++j) {
          t2[j] = 0.0;
          for (int k = 0; k < m_numAttribs; ++k)
            t2[j] += W[k][j] * v[k];
        }
        // t1 = W*h
        for (int j = 0; j < m_numAttribs; ++j) {
          t1[j] = 0.0;
          for (int k = 0; k < m_rank; ++k)
            t1[j] += W[j][k] * h[k];
        }
        // t3 = W'*t1
        for (int j = 0; j < m_rank; ++j) {
          t3[j] = 0.0;
          for (int k = 0; k < m_numAttribs; ++k)
            t3[j] += W[k][j] * t1[k];
        }
        // t2 = (t2+eps)./(t3+eps)
        for (int j = 0; j < m_rank; ++j)
          t2[j] = (t2[j]+eps) / (t3[j]+eps);
        break;
      case 3:
        // t1 = W*h
        for (int j = 0; j < m_numAttribs; ++j) {
          t1[j] = 0.0;
          for (int k = 0; k < m_rank; ++k)
            t1[j] += W[j][k] * h[k];
        }
        // t1 = (v+eps)./(t1+eps)
        for (int j = 0; j < m_numAttribs; ++j)
          t1[j] = (v[j]+eps) / (t1[j]+eps);
        // t2 = W'*t1
        for (int j = 0; j < m_rank; ++j) {
          t2[j] = 0.0;
          for (int k = 0; k < m_numAttribs; ++k)
            t2[j] += W[k][j] * t1[k];
        }
        // t3 = (sum(W))'
        for (int j = 0; j < m_rank; ++j) {
          t3[j] = 0.0;
          for (int k = 0; k < m_numAttribs; ++k)
            t3[j] += W[k][j];
        }
        // t2 = t2./t3
        for (int j = 0; j < m_rank; ++j)
          t2[j] /= t3[j];
        break;
      }
      // h = h.*t2
      for (int j = 0; j < m_rank; ++j)
        h[j] *= t2[j];
    }

    if (m_hasClass) {
      h[m_rank] = instance.value(instance.classIndex());
    }

    System.err.print(".");
    if (instance instanceof SparseInstance) {
      return new SparseInstance(instance.weight(), h);
    } else {
      return new Instance(instance.weight(), h);
    }
  }

  /**
   * Set the format for the transformed data
   * @return a set of empty Instances (header only) in the new format
   * @exception Exception if the output format can't be set
   */
  private Instances setOutputFormat() throws Exception {
    if (m_basis == null) {
      return null;
    }

    FastVector attributes = new FastVector();

    // add attribute names
    for (int i = 1; i <= m_basis[0].length; ++i) {
      attributes.addElement(new Attribute("enc-" + Integer.toString(i)));
    }

    if (m_hasClass) {
      attributes.addElement(m_trainCopy.classAttribute().copy());
    }

    Instances outputFormat = 
      new Instances(m_trainInstances.relationName()+"->NMF",
                    attributes, 0);

    // set the class to be the last attribute if necessary
    if (m_hasClass) {
      outputFormat.setClassIndex(outputFormat.numAttributes()-1);
    }
     
    return outputFormat;
  }


  /**
   * Main method for testing this class
   * @param argv should contain the command line arguments to the
   * evaluator/transformer (see AttributeSelection)
   */
  public static void main(String [] argv) {
    try {
      System.out.println(AttributeSelection.
                         SelectAttributes(new MatlabNMF(), argv));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
  
}



