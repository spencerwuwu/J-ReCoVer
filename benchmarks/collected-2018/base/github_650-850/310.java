// https://searchcode.com/api/result/112654535/

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
 *    AdaBoostM1.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package weka.classifiers.meta;

import weka.classifiers.*;
import weka.classifiers.Classifier;
import weka.classifiers.DistributionClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.Sourcable;
import weka.classifiers.rules.ZeroR;
import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for boosting a classifier using Freund & Schapire's Adaboost 
 * M1 method. For more information, see<p>
 *
 * Yoav Freund and Robert E. Schapire
 * (1996). <i>Experiments with a new boosting algorithm</i>.  Proc
 * International Conference on Machine Learning, pages 148-156, Morgan
 * Kaufmann, San Francisco.<p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Turn on debugging output.<p>
 *
 * -W classname <br>
 * Specify the full class name of a classifier as the basis for 
 * boosting (required).<p>
 *
 * -I num <br>
 * Set the number of boost iterations (default 10). <p>
 *
 * -P num <br>
 * Set the percentage of weight mass used to build classifiers
 * (default 100). <p>
 *
 * -Q <br>
 * Use resampling instead of reweighting.<p>
 *
 * -S seed <br>
 * Random number seed for resampling (default 1). <p>
 *
 * Options after -- are passed to the designated classifier.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $ 
 */
public class AdaBoostM1 extends EnsembleClassifier 
  implements OptionHandler, WeightedInstancesHandler, Sourcable {

  /** Max num iterations tried to find classifier with non-zero error. */ 
  private static int MAX_NUM_RESAMPLING_ITERATIONS = 10;

  /** The model base classifier to use */
  protected Classifier m_Classifier = new weka.classifiers.rules.ZeroR();
  
  /** Array for storing the generated base classifiers. */
  protected Classifier [] m_Classifiers;
  
  /** Array for storing the weights for the votes. */
  protected double [] m_Betas;

  /** The maximum number of boost iterations */
  protected int m_MaxIterations = 10;

  /** The number of successfully generated base classifiers. */
  protected int m_NumIterations;

  /** Weight Threshold. The percentage of weight mass used in training */
  protected int m_WeightThreshold = 100;

  /** Debugging mode, gives extra output if true */
  protected boolean m_Debug;

  /** Use boosting with reweighting? */
  protected boolean m_UseResampling;

  /** Seed for boosting with resampling. */
  protected int m_Seed = 1;

  /** The number of classes */
  protected int m_NumClasses;

  /**
   * Select only instances with weights that contribute to 
   * the specified quantile of the weight distribution
   *
   * @param data the input instances
   * @param quantile the specified quantile eg 0.9 to select 
   * 90% of the weight mass
   * @return the selected instances
   */
  protected Instances selectWeightQuantile(Instances data, double quantile) { 

    int numInstances = data.numInstances();
    Instances trainData = new Instances(data, numInstances);
    double [] weights = new double [numInstances];

    double sumOfWeights = 0;
    for(int i = 0; i < numInstances; i++) {
      weights[i] = data.instance(i).weight();
      sumOfWeights += weights[i];
    }
    double weightMassToSelect = sumOfWeights * quantile;
    int [] sortedIndices = Utils.sort(weights);

    // Select the instances
    sumOfWeights = 0;
    for(int i = numInstances - 1; i >= 0; i--) {
      Instance instance = (Instance)data.instance(sortedIndices[i]).copy();
      trainData.add(instance);
      sumOfWeights += weights[sortedIndices[i]];
      if ((sumOfWeights > weightMassToSelect) && 
	  (i > 0) && 
	  (weights[sortedIndices[i]] != weights[sortedIndices[i - 1]])) {
	break;
      }
    }
    if (m_Debug) {
      System.err.println("Selected " + trainData.numInstances()
			 + " out of " + numInstances);
    }
    return trainData;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(6);

    newVector.addElement(new Option(
	      "\tTurn on debugging output.",
	      "D", 0, "-D"));
    newVector.addElement(new Option(
	      "\tMaximum number of boost iterations.\n"
	      +"\t(default 10)",
	      "I", 1, "-I <num>"));
    newVector.addElement(new Option(
	      "\tPercentage of weight mass to base training on.\n"
	      +"\t(default 100, reduce to around 90 speed up)",
	      "P", 1, "-P <num>"));
    newVector.addElement(new Option(
	      "\tFull name of classifier to boost.\n"
	      +"\teg: weka.classifiers.bayes.NaiveBayes",
	      "W", 1, "-W <class name>"));
    newVector.addElement(new Option(
	      "\tUse resampling for boosting.",
	      "Q", 0, "-Q"));
    newVector.addElement(new Option(
	      "\tSeed for resampling. (Default 1)",
	      "S", 1, "-S <num>"));
    

    if ((m_Classifier != null) &&
	(m_Classifier instanceof OptionHandler)) {
      newVector.addElement(new Option(
	     "",
	     "", 0, "\nOptions specific to classifier "
	     + m_Classifier.getClass().getName() + ":"));
      Enumeration enum = ((OptionHandler)m_Classifier).listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    }
    return newVector.elements();
  }


  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Turn on debugging output.<p>
   *
   * -W classname <br>
   * Specify the full class name of a classifier as the basis for 
   * boosting (required).<p>
   *
   * -I num <br>
   * Set the number of boost iterations (default 10). <p>
   *
   * -P num <br>
   * Set the percentage of weight mass used to build classifiers
   * (default 100). <p>
   *
   * -Q <br>
   * Use resampling instead of reweighting.<p>
   *
   * -S seed <br>
   * Random number seed for resampling (default 1).<p>
   *
   * Options after -- are passed to the designated classifier.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    setDebug(Utils.getFlag('D', options));
    
    String boostIterations = Utils.getOption('I', options);
    if (boostIterations.length() != 0) {
      setMaxIterations(Integer.parseInt(boostIterations));
    } else {
      setMaxIterations(10);
    }

    String thresholdString = Utils.getOption('P', options);
    if (thresholdString.length() != 0) {
      setWeightThreshold(Integer.parseInt(thresholdString));
    } else {
      setWeightThreshold(100);
    }
      
    setUseResampling(Utils.getFlag('Q', options));
    if (m_UseResampling && (thresholdString.length() != 0)) {
      throw new Exception("Weight pruning with resampling"+
			  "not allowed.");
    }

    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      setSeed(Integer.parseInt(seedString));
    } else {
      setSeed(1);
    }

    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }
    setClassifier(Classifier.forName(classifierName,
				     Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) && 
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }

    String [] options = new String [classifierOptions.length + 10];
    int current = 0;
    if (getDebug()) {
      options[current++] = "-D";
    }
    if (getUseResampling()) {
      options[current++] = "-Q";
    } else {
      options[current++] = "-P"; 
      options[current++] = "" + getWeightThreshold();
    }
    options[current++] = "-I"; options[current++] = "" + getMaxIterations();
    options[current++] = "-S"; options[current++] = "" + getSeed();

    if (getClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getClassifier().getClass().getName();
    }
    options[current++] = "--";

    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Set the classifier for boosting. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the classifier used as the classifier
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }


  /**
   * Set the maximum number of boost iterations
   */
  public void setMaxIterations(int maxIterations) {

    m_MaxIterations = maxIterations;
  }

  /**
   * Get the maximum number of boost iterations
   *
   * @return the maximum number of boost iterations
   */
  public int getMaxIterations() {

    return m_MaxIterations;
  }

  /**
   * Set weight threshold
   *
   * @param thresholding the percentage of weight mass used for training
   */
  public void setWeightThreshold(int threshold) {

    m_WeightThreshold = threshold;
  }

  /**
   * Get the degree of weight thresholding
   *
   * @return the percentage of weight mass used for training
   */
  public int getWeightThreshold() {

    return m_WeightThreshold;
  }

  /**
   * Set seed for resampling.
   *
   * @param seed the seed for resampling
   */
  public void setSeed(int seed) {

    m_Seed = seed;
  }

  /**
   * Get seed for resampling.
   *
   * @return the seed for resampling
   */
  public int getSeed() {

    return m_Seed;
  }

  /**
   * Set debugging mode
   *
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {

    m_Debug = debug;
  }

  /**
   * Get whether debugging is turned on
   *
   * @return true if debugging output is on
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Set resampling mode
   *
   * @param resampling true if resampling should be done
   */
  public void setUseResampling(boolean r) {

    m_UseResampling = r;
  }

  /**
   * Get whether resampling is turned on
   *
   * @return true if resampling output is on
   */
  public boolean getUseResampling() {

    return m_UseResampling;
  }

  /**
   * Boosting method.
   *
   * @param data the training data to be used for generating the
   * boosted classifier.
   * @exception Exception if the classifier could not be built successfully
   */

  public void buildClassifier(Instances data) throws Exception {
      
      //Initialize measures
      initMeasures();
      
    if (data.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }
    data = new Instances(data);
    data.deleteWithMissingClass();
    if (data.numInstances() == 0) {
      throw new Exception("No train instances without class missing!");
    }
    if (data.classAttribute().isNumeric()) {
      throw new UnsupportedClassTypeException("AdaBoostM1 can't handle a numeric class!");
    }
    if (m_Classifier == null) {
      throw new Exception("A base classifier has not been specified!");
    }
    m_NumClasses = data.numClasses();
    m_Classifiers = Classifier.makeCopies(m_Classifier, getMaxIterations());
    if ((!m_UseResampling) && 
	(m_Classifier instanceof WeightedInstancesHandler)) {
      buildClassifierWithWeights(data);
    } else {
      buildClassifierUsingResampling(data);
    }

    //=============== BEGIN EDIT melville ===============
    m_EnsembleWts = new double [m_NumIterations];
    if(m_NumIterations==1)
	m_EnsembleWts[0] = 1.0;
    else{
	for(int i=0; i<m_NumIterations; i++)
	    m_EnsembleWts[i] =  m_Betas[i];
    } 
    computeEnsembleMeasures(data);
    //=============== END EDIT melville ===============
  }

  /**
   * Boosting method. Boosts using resampling
   *
   * @param data the training data to be used for generating the
   * boosted classifier.
   * @exception Exception if the classifier could not be built successfully
   */
  protected void buildClassifierUsingResampling(Instances data) 
    throws Exception {

    Instances trainData, sample, training;
    double epsilon, reweight, beta = 0, sumProbs;
    double oldSumOfWeights, newSumOfWeights;
    Evaluation evaluation;
    int numInstances = data.numInstances();
    Random randomInstance = new Random(m_Seed);
    double[] probabilities;
    int resamplingIterations = 0;
    int k, l;

    // Initialize data
    m_Betas = new double [m_Classifiers.length];
    m_NumIterations = 0;
    // Create a copy of the data so that when the weights are diddled
    // with it doesn't mess up the weights for anyone else
    training = new Instances(data, 0, numInstances);
    sumProbs = training.sumOfWeights();
    for (int i = 0; i < training.numInstances(); i++) {
      training.instance(i).setWeight(training.instance(i).
				      weight() / sumProbs);
    }
    
    // Do boostrap iterations
    for (m_NumIterations = 0; m_NumIterations < m_Classifiers.length; 
	 m_NumIterations++) {
      if (m_Debug) {
	System.err.println("Training classifier " + (m_NumIterations + 1));
      }

      // Select instances to train the classifier on
      if (m_WeightThreshold < 100) {
	trainData = selectWeightQuantile(training, 
					 (double)m_WeightThreshold / 100);
      } else {
	trainData = new Instances(training);
      }
      
      // Resample
      resamplingIterations = 0;
      double[] weights = new double[trainData.numInstances()];
      for (int i = 0; i < weights.length; i++) {
	weights[i] = trainData.instance(i).weight();
      }
      do {
	sample = trainData.resampleWithWeights(randomInstance, weights);

	// Build and evaluate classifier
	m_Classifiers[m_NumIterations].buildClassifier(sample);
	evaluation = new Evaluation(data);
	evaluation.evaluateModel(m_Classifiers[m_NumIterations], 
				 training);
	epsilon = evaluation.errorRate();
	resamplingIterations++;
      } while (Utils.eq(epsilon, 0) && 
	      (resamplingIterations < MAX_NUM_RESAMPLING_ITERATIONS));
      	
      // Stop if error too big or 0
      if (Utils.grOrEq(epsilon, 0.5) || Utils.eq(epsilon, 0)) {
	if (m_NumIterations == 0) {
	  m_NumIterations = 1; // If we're the first we have to to use it
	}
	break;
      }
      
      // Determine the weight to assign to this model
      m_Betas[m_NumIterations] = beta = Math.log((1 - epsilon) / epsilon);
      reweight = (1 - epsilon) / epsilon;
      if (m_Debug) {
	System.err.println("\terror rate = " + epsilon
			   +"  beta = " + m_Betas[m_NumIterations]);
      }
 
      // Update instance weights
      oldSumOfWeights = training.sumOfWeights();
      Enumeration enum = training.enumerateInstances();
      while (enum.hasMoreElements()) {
	Instance instance = (Instance) enum.nextElement();
	if (!Utils.eq(m_Classifiers[m_NumIterations].classifyInstance(instance), 
		     instance.classValue()))
	  instance.setWeight(instance.weight() * reweight);
      }
      // Renormalize weights
      newSumOfWeights = training.sumOfWeights();
      enum = training.enumerateInstances();
      while (enum.hasMoreElements()) {
	Instance instance = (Instance) enum.nextElement();
	instance.setWeight(instance.weight() * oldSumOfWeights 
			   / newSumOfWeights);
      }
    }
  }

  /**
   * Boosting method. Boosts any classifier that can handle weighted
   * instances.
   *
   * @param data the training data to be used for generating the
   * boosted classifier.
   * @exception Exception if the classifier could not be built successfully
   */
  protected void buildClassifierWithWeights(Instances data) 
    throws Exception {

    Instances trainData, training;
    double epsilon, reweight, beta = 0;
    double oldSumOfWeights, newSumOfWeights;
    Evaluation evaluation;
    int numInstances = data.numInstances();

    // Initialize data
    m_Betas = new double [m_Classifiers.length];
    m_NumIterations = 0;

    // Create a copy of the data so that when the weights are diddled
    // with it doesn't mess up the weights for anyone else
    training = new Instances(data, 0, numInstances);
    
    // Do boostrap iterations
    for (m_NumIterations = 0; m_NumIterations < m_Classifiers.length; 
	 m_NumIterations++) {
      if (m_Debug) {
	System.err.println("Training classifier " + (m_NumIterations + 1));
      }
      // Select instances to train the classifier on
      if (m_WeightThreshold < 100) {
	trainData = selectWeightQuantile(training, 
					 (double)m_WeightThreshold / 100);
      } else {
	trainData = new Instances(training, 0, numInstances);
      }

      // Build the classifier
      m_Classifiers[m_NumIterations].buildClassifier(trainData);

      // Evaluate the classifier
      evaluation = new Evaluation(data);
      evaluation.evaluateModel(m_Classifiers[m_NumIterations], training);
      epsilon = evaluation.errorRate();

      // Stop if error too small or error too big and ignore this model
      if (Utils.grOrEq(epsilon, 0.5) || Utils.eq(epsilon, 0)) {
	  if (m_NumIterations == 0) {
	  m_NumIterations = 1; // If we're the first we have to to use it
	}
	break;
      }
      // Determine the weight to assign to this model
      m_Betas[m_NumIterations] = beta = Math.log((1 - epsilon) / epsilon);
      reweight = (1 - epsilon) / epsilon;
      if (m_Debug) {
	System.err.println("\terror rate = " + epsilon
			   +"  beta = " + m_Betas[m_NumIterations]);
      }
 
      // Update instance weights
      oldSumOfWeights = training.sumOfWeights();
      Enumeration enum = training.enumerateInstances();
      while (enum.hasMoreElements()) {
	Instance instance = (Instance) enum.nextElement();
	if (!Utils.eq(m_Classifiers[m_NumIterations]
		      .classifyInstance(instance), 
		      instance.classValue()))
	  instance.setWeight(instance.weight() * reweight);
      }
      // Renormalize weights
      newSumOfWeights = training.sumOfWeights();
      enum = training.enumerateInstances();
      while (enum.hasMoreElements()) {
	Instance instance = (Instance) enum.nextElement();
	instance.setWeight(instance.weight() * oldSumOfWeights
			   / newSumOfWeights);
      }
    }
  }
  
  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double [] distributionForInstance(Instance instance) 
    throws Exception {
      if (m_NumIterations == 0) {
      throw new Exception("No model built");
    }
    double [] sums = new double [instance.numClasses()]; 
    
    if (m_NumIterations == 1) {
      if (m_Classifiers[0] instanceof DistributionClassifier) {
	return ((DistributionClassifier)m_Classifiers[0]).
	distributionForInstance(instance);
      } else {
	sums[(int)m_Classifiers[0].classifyInstance(instance)] ++;
      }
    } else {
      for (int i = 0; i < m_NumIterations; i++) {
	sums[(int)m_Classifiers[i].classifyInstance(instance)] += 
	m_Betas[i];
      }
    }
    Utils.normalize(sums);
    return sums;
  }

    
    //=============== BEGIN EDIT melville ===============
    /** Returns class predictions of each ensemble member */
    public double []getEnsemblePredictions(Instance instance) throws Exception{
	double preds[] = new double [m_NumIterations];
	for(int i=0; i<m_NumIterations; i++)
	    preds[i] = m_Classifiers[i].classifyInstance(instance);
	
	return preds;
    }
    
    /** 
     * Returns vote weights of ensemble members.
     *
     * @return vote weights of ensemble members
     */
    public double []getEnsembleWts(){
	return m_EnsembleWts;
    }
    
    /** Returns size of ensemble */
    public double getEnsembleSize(){
	return m_NumIterations;
    }
    //=============== END EDIT melville ===============
    
   /**
   * Returns the boosted model as Java source code.
   *
   * @return the tree as Java source code
   * @exception Exception if something goes wrong
   */
  public String toSource(String className) throws Exception {

    if (m_NumIterations == 0) {
      throw new Exception("No model built yet");
    }
    if (!(m_Classifiers[0] instanceof Sourcable)) {
      throw new Exception("Base learner " + m_Classifier.getClass().getName()
			  + " is not Sourcable");
    }

    StringBuffer text = new StringBuffer("class ");
    text.append(className).append(" {\n\n");

    text.append("  public static double classify(Object [] i) {\n");

    if (m_NumIterations == 1) {
      text.append("    return " + className + "_0.classify(i);\n");
    } else {
      text.append("    double [] sums = new double [" + m_NumClasses + "];\n");
      for (int i = 0; i < m_NumIterations; i++) {
	text.append("    sums[(int) " + className + '_' + i 
		    + ".classify(i)] += " + m_Betas[i] + ";\n");
      }
      text.append("    double maxV = sums[0];\n" +
		  "    int maxI = 0;\n"+
		  "    for (int j = 1; j < " + m_NumClasses + "; j++) {\n"+
		  "      if (sums[j] > maxV) { maxV = sums[j]; maxI = j; }\n"+
		  "    }\n    return (double) maxI;\n");
    }
    text.append("  }\n}\n");

    for (int i = 0; i < m_Classifiers.length; i++) {
	text.append(((Sourcable)m_Classifiers[i])
		    .toSource(className + '_' + i));
    }
    return text.toString();
  }

  /**
   * Returns description of the boosted classifier.
   *
   * @return description of the boosted classifier as a string
   */
  public String toString() {
    
    StringBuffer text = new StringBuffer();
    
    if (m_NumIterations == 0) {
      text.append("AdaBoostM1: No model built yet.\n");
    } else if (m_NumIterations == 1) {
      text.append("AdaBoostM1: No boosting possible, one classifier used!\n");
      text.append(m_Classifiers[0].toString() + "\n");
    } else {
      text.append("AdaBoostM1: Base classifiers and their weights: \n\n");
      for (int i = 0; i < m_NumIterations ; i++) {
	text.append(m_Classifiers[i].toString() + "\n\n");
	text.append("Weight: " + Utils.roundDouble(m_Betas[i], 2) + "\n\n");
      }
      text.append("Number of performed Iterations: " 
		  + m_NumIterations + "\n");
    }
    
    return text.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new AdaBoostM1(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}


  

