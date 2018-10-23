// https://searchcode.com/api/result/137102440/

package org.scripps.combo.weka;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;


import org.scripps.combo.model.Feature;
import org.scripps.combo.weka.CopyOfWeka.card;
import org.scripps.combo.weka.Weka.execution;
import org.scripps.util.Gene;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.AttributeEvaluator;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.meta.Vote;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
import weka.core.SelectedTag;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.Remove;


public class Weka {

	private Instances train = null;
	Instances test = null;
	Random rand;
	String eval_method;
	Map<String, Feature> features;
	String dataset;
	
	public void buildWeka(InputStream train_stream, InputStream test_stream, String dataset) throws Exception{
		setDataset(dataset);
		//get the data 
		DataSource source = new DataSource(train_stream);
		setTrain(source.getDataSet());
		if (getTrain().classIndex() == -1){
			getTrain().setClassIndex(getTrain().numAttributes() - 1);
		}
		train_stream.close();
		if(test_stream!=null){
			source = new DataSource(test_stream);
			test = source.getDataSet();
			if (test.classIndex() == -1){
				test.setClassIndex(test.numAttributes() - 1);
			} 
			test_stream.close();
		}
		rand = new Random(1);
		//specify how hands evaluated {cross_validation, test_set, training_set}
		eval_method = "cross_validation";//"training_set";
		//assumes that feature table has already been loaded
		//get the features related to this weka dataset
		setFeatures(Feature.getByDataset(dataset));
	} 

	
	public Map<String, Float> getRelief(){
		Map<String, Float> index_value = new HashMap<String, Float>();
		//set the reliefF value for the attribute
		AttributeSelection as = new AttributeSelection();
		Ranker ranker = new Ranker();
		//keep all
		//	String[] options = {"-T","0.0","-N","-1"};
		as.setEvaluator(new ReliefFAttributeEval());
		as.setSearch(ranker);
		try {
			as.setInputFormat(getTrain());
			//ranker.setOptions(options);
			Instances filtered = Filter.useFilter(getTrain(), as); 			
			double[][] ranked = ranker.rankedAttributes();
			//add the scores to the gene cards
			for(int att=0; att<ranked.length; att++){
				int att_id = (int)ranked[att][0];
				float att_value = (float)ranked[att][1];
				Attribute tmp = getTrain().attribute(att_id);
				index_value.put(tmp.name(), att_value);
			}
			setTrain(filtered);
			//	System.out.println(ranked[0][0]+" "+ranked[0][1]);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return index_value;
	}
	
	public void exportArff(Instances dataset, String outfile){
		ArffSaver saver = new ArffSaver();
		saver.setInstances(dataset);
		try {
			saver.setFile(new File(outfile));
			// saver.setDestination(new File("./data/test.arff"));   // **not** necessary in 3.5.4 and later
			saver.writeBatch();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Hold the results of training and testing a FilteredClassifier
	 * @author bgood
	 *
	 */
	public class execution{
		public FilteredClassifier model;
		public Evaluation eval;
		public double avg_percent_correct;
		public execution(FilteredClassifier model, Evaluation eval, double avg_percent_correct) {
			super();
			this.model = model;
			this.eval = eval;
			this.avg_percent_correct = avg_percent_correct;
		}//model.getClassifier().toString()+
		public String toString(){
			return "Tree Accuracy on test set:"+avg_percent_correct;
		}
	}

	public class metaExecution{
		public Classifier model;
		public Evaluation eval;
		public double avg_percent_correct;
		public metaExecution(Classifier model, Evaluation eval, double avg_percent_correct) {
			super();
			this.model = model;
			this.eval = eval;
			this.avg_percent_correct = avg_percent_correct;			
		}
		public metaExecution(Evaluation eval) {

			this.eval = eval;
		}
		public String toString(){
			return "Meta Accuracy on test set:"+eval.pctCorrect();
		}
	}
	/**
	 * unique ids match are used for the keys in the features table
	 * they are external unqiue ids
	 * @param unique_ids
	 * @param wekamodel
	 * @param dataset
	 * @return
	 */
	public execution pruneAndExecuteWithUniqueIds(List<String> unique_ids, Classifier wekamodel, String dataset) {
		String indices = "";
		for(String fid : unique_ids){
			Feature f = features.get(fid);
			for(org.scripps.combo.model.Attribute a : f.getDataset_attributes()){
				if(a.getDataset().equals(dataset)){
					indices+=a.getCol_index()+",";
				}
			}
		}
		return pruneAndExecute(indices, wekamodel);
	}

	public execution pruneAndExecute(String indicesoff1, Classifier wekamodel){
		if(wekamodel==null){
			wekamodel = new J48();
		}
		
		String indices = "";
		for(String a : indicesoff1.split(",")){
			if(!a.equals("")){
				int i = 1+Integer.parseInt(a);
				indices += i+",";
			}
		}
		// set a specific set of attributes to use to train the model
		Remove rm = new Remove();
		//don't remove the class attribute
		rm.setAttributeIndices(indices+"last");
		rm.setInvertSelection(true);
		// build a classifier using only these attributes
		FilteredClassifier fc = new FilteredClassifier();
		fc.setFilter(rm);
		fc.setClassifier(wekamodel);
		// train and evaluate on the test set
		Evaluation eval = null;
		double avg_pct_correct = 0;
		try {
			fc.buildClassifier(getTrain());
			// evaluate classifier and print some statistics
			eval = new Evaluation(getTrain());
			if(eval_method.equals("cross_validation")){
				//this makes the game more stable in terms of scores
				//		for(int r=0; r<10; r++){
				int r = 0;
				Random keep_same = new Random();
				keep_same.setSeed(r);
				eval.crossValidateModel(fc, getTrain(), 10, keep_same);
				avg_pct_correct += eval.pctCorrect();
				//		}
				avg_pct_correct = avg_pct_correct/10;
			}else if(eval_method.equals("test_set")){
				eval.evaluateModel(fc, test);
			}else {
				eval.evaluateModel(fc, getTrain());
			}
			//System.out.println(fc.getClassifier().toString()+"\n\n"+eval.toSummaryString("\nResults\n======\n", false));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//	String tree = fc.getClassifier().toString()+"\n\n"+eval.toSummaryString("\nResults\n======\n", false);
		//	double correct = eval.pctCorrect();
		//	System.out.println("pae "+indices+" "+correct);
		return new execution(fc,eval, avg_pct_correct);
	}

	/***
	 * build and test a meta classifier - a non-random forest in this case
	 * input, a set of feature sets and a classifier model
	 * output an execution result for the whole thing
	 */
	public metaExecution executeNonRandomForestOnUniqueIds(List<List<String>> id_sets){

		//create an array of classifiers that differ from each other based on the features that they use
		Classifier[] classifiers = new Classifier[id_sets.size()];
		int i = 0;
		for(List<String> id_set : id_sets){
			// set a specific set of attributes to use to train the model
			Remove rm = new Remove();
			//don't remove the class attribute
			String indices = "";
			for(String fid : id_set){
				Feature f = features.get(fid);
				if(f!=null&&f.getDataset_attributes()!=null){
				for(org.scripps.combo.model.Attribute a : f.getDataset_attributes()){
					if(a.getDataset().equals(dataset)){
						//note the correction is being made here for the index problem
						indices+=a.getCol_index()+1+",";
						Attribute test = train.attribute(a.getCol_index());
						Attribute test2 = train.attribute(a.getName());
						if(test!=test2){
							System.out.println("indexing problem");
						}
					}
				}
				}else{
					System.out.println("No attribute found for geneid "+fid+" in "+dataset);
				}
			}
			rm.setAttributeIndices(indices+"last");
			rm.setInvertSelection(true);
			// build a classifier using only these attributes
			FilteredClassifier fc = new FilteredClassifier();
			fc.setFilter(rm);
			fc.setClassifier(new J48());
			classifiers[i] = fc;
			i++;
		}
		System.out.println(i+" Classifiers prepared, about to execute");

		//		//build the non-random forest
		Vote voter = new Vote();
		//		//-R <AVG|PROD|MAJ|MIN|MAX|MED>
		String[] options = new String[2];
		options[0] = "-R"; options[1] = "MAJ"; //avg and maj seem to work better..
		try {
			voter.setOptions(options);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		voter.setClassifiers(classifiers);
		voter.setDebug(true);
		// train and evaluate 
		Evaluation eval = null;
		double avg_pct_correct = 0;
		try {
			voter.buildClassifier(getTrain());
			// evaluate classifier and print some statistics
			eval = new Evaluation(getTrain());
			if(eval_method.equals("cross_validation")){
				for(int r=0; r<10; r++){
					Random keep_same = new Random();
					keep_same.setSeed(r);
					eval.crossValidateModel(voter, getTrain(), 10, keep_same);
					avg_pct_correct += eval.pctCorrect();
				}
				avg_pct_correct = avg_pct_correct/10;
				//				//this makes the game more stable in terms of scores
				//				Random keep_same = new Random();
				//				keep_same.setSeed(0);
				//				eval.crossValidateModel(voter, getTrain(), 10, keep_same);
			}
			else if(eval_method.equals("test_set")){
				eval.evaluateModel(voter, test);
			}else {
				eval.evaluateModel(voter, getTrain());
			}
			//	System.out.println(eval.toSummaryString("\nResults\n======\n", false));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new metaExecution(voter,eval,avg_pct_correct);

	}
	
	/***
	 * build and test a meta classifier - a non-random forest in this case
	 * input, a set of feature sets and a classifier model
	 * output an execution result for the whole thing
	 */
	public metaExecution executeNonRandomForest(Set<String> indicesoff1){
		Set<String> indices_set = new HashSet<String>();
		//remap indexes
		for(String indices_ : indicesoff1){
			String indices = "";
			for(String a : indices_.split(",")){
				if(!a.equals("")){
					int i = 1+Integer.parseInt(a);
					indices += i+",";
				}
			}
			indices_set.add(indices);
		}

		//create an array of classifiers that differ from each other based on the features that they use
		Classifier[] classifiers = new Classifier[indices_set.size()];
		int i = 0;
		for(String indices : indices_set){
			// set a specific set of attributes to use to train the model
			Remove rm = new Remove();
			//don't remove the class attribute
			//128,224,91,21,
			rm.setAttributeIndices(indices+"last");
			rm.setInvertSelection(true);
			// build a classifier using only these attributes
			FilteredClassifier fc = new FilteredClassifier();
			fc.setFilter(rm);
			fc.setClassifier(new J48());
			classifiers[i] = fc;
			i++;
		}


		//		//build the non-random forest
		Vote voter = new Vote();
		//		//-R <AVG|PROD|MAJ|MIN|MAX|MED>
		String[] options = new String[2];
		options[0] = "-R"; options[1] = "MAJ"; //avg and maj seem to work better..
		try {
			voter.setOptions(options);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		voter.setClassifiers(classifiers);
		voter.setDebug(true);
		// train and evaluate 
		Evaluation eval = null;
		double avg_pct_correct = 0;
		try {
			voter.buildClassifier(getTrain());
			// evaluate classifier and print some statistics
			eval = new Evaluation(getTrain());
			if(eval_method.equals("cross_validation")){
				for(int r=0; r<10; r++){
					Random keep_same = new Random();
					keep_same.setSeed(r);
					eval.crossValidateModel(voter, getTrain(), 10, keep_same);
					avg_pct_correct += eval.pctCorrect();
				}
				avg_pct_correct = avg_pct_correct/10;
				//				//this makes the game more stable in terms of scores
				//				Random keep_same = new Random();
				//				keep_same.setSeed(0);
				//				eval.crossValidateModel(voter, getTrain(), 10, keep_same);
			}
			else if(eval_method.equals("test_set")){
				eval.evaluateModel(voter, test);
			}else {
				eval.evaluateModel(voter, getTrain());
			}
			//	System.out.println(eval.toSummaryString("\nResults\n======\n", false));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new metaExecution(voter,eval,avg_pct_correct);

	}

	/***
	 * Choose which of the input attribute sets to include in the meta classifier using an evaluation step
	 * included within the cross-validation
	 * @param indicesoff1
	 * @return
	 * @throws Exception 
	 */
	public metaExecution executeNonRandomForestWithInternalCVparamselection(Set<String> indicesoff1) throws Exception{
		Set<String> indices_set = new HashSet<String>();
		//remap indexes
		for(String indices_ : indicesoff1){
			String indices = "";
			for(String a : indices_.split(",")){
				if(!a.equals("")){
					int i = 1+Integer.parseInt(a);
					indices += i+",";
				}
			}
			indices_set.add(indices);
		}

		//start outer cross-validation loop
		int numFolds = 10;
		// Make a copy of the data we can reorder
		Instances data = new Instances(getTrain());
		Evaluation eval = new Evaluation(data);
		data.randomize(getRand());
		if (data.classAttribute().isNominal()) {
			data.stratify(numFolds);
		}

		// Do the folds
		for (int i = 0; i < numFolds; i++) {
			Instances thistrain = data.trainCV(numFolds, i, getRand());
			eval.setPriors(thistrain);
			//execute attribute selection filter here
			int n_trees = 7;
			Classifier voter = getCVSelectedVoterBest(thistrain, indices_set, n_trees);
			Classifier copiedClassifier = Classifier.makeCopy(voter);
			copiedClassifier.buildClassifier(thistrain);
			Instances thistest = data.testCV(numFolds, i);
			eval.evaluateModel(copiedClassifier, thistest);
		}

		return new metaExecution(eval);


	}


	/**
	 * Given a particular training set (e.g. the training set for one fold of a cross-validation run)
	 * generate a voter classifier using only the subclassifiers that perform better than min_pctCorrect
	 * in 10-f cross-validation within this dataset.
	 * 
	 * If no subclassifiers meet the threshold, return the single best tree
	 * @param thistrain
	 * @param classifiers
	 * @return
	 */
	public Classifier getCVSelectedVoterThresholded___(Instances thistrain, Classifier[] classifiers) {
		int min = 69;
		//first select only the finest component trees 
		List<Classifier> selected = new ArrayList<Classifier>();
		int best_index = 0; double best = 0;
		for(int i=0; i<classifiers.length; i++){
			Evaluation e;
			try {
				e = new Evaluation(thistrain);
				e.crossValidateModel(classifiers[i], thistrain, 10, getRand());
				if(e.pctCorrect()>min){
					selected.add(classifiers[i]);
				}
				if(e.pctCorrect()>best){
					best = e.pctCorrect();
					best_index = i;
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		if(selected.size()<1){
			return classifiers[best_index];
		}
		//now build the non-random forest
		Vote voter = new Vote();
		//		//-R <AVG|PROD|MAJ|MIN|MAX|MED>
		String[] options = new String[2];
		options[0] = "-R"; options[1] = "MAJ"; //avg and maj seem to work better..
		try {
			voter.setOptions(options);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		voter.setClassifiers(selected.toArray(new Classifier[selected.size()]));	
		return voter;
	}
	/**
	 * Given a particular training set (e.g. the training set for one fold of a cross-validation run)
	 * generate a voter classifier using only the best n_trees subclassifiers as determined by 10-f 
	 * cross-validation within this dataset.
	 * 
	 * @param thistrain
	 * @param classifiers
	 * @return
	 */
	public Classifier getCVSelectedVoterBest(Instances thistrain, Set<String> indices_set, int n_trees) {
		//create an array of classifiers that differ from each other based on the features that they use
		Classifier[] classifiers = new Classifier[indices_set.size()];
		int ii = 0;
		for(String indices : indices_set){
			// set a specific set of attributes to use to train the model
			Remove rm = new Remove();
			//don't remove the class attribute
			//128,224,91,21,
			rm.setAttributeIndices(indices+"last");
			rm.setInvertSelection(true);
			// build a classifier using only these attributes
			FilteredClassifier fc = new FilteredClassifier();
			fc.setFilter(rm);
			fc.setClassifier(new J48());
			classifiers[ii] = fc;
			ii++;
		}

		if(classifiers.length<n_trees){
			n_trees = classifiers.length;
		}
		//first select only the finest component trees 
		Map<Integer, Double> selected = new HashMap<Integer, Double>();
		for(int i=0; i<classifiers.length; i++){
			Evaluation e;
			try {
				e = new Evaluation(thistrain);
				e.crossValidateModel(classifiers[i], thistrain, 10, getRand());
				selected.put(i, e.pctCorrect());
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		//now build the non-random forest
		Vote voter = new Vote();
		//		//-R <AVG|PROD|MAJ|MIN|MAX|MED>
		String[] options = new String[2];
		options[0] = "-R"; options[1] = "MAJ"; //avg and maj seem to work better..
		try {
			voter.setOptions(options);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		List<Integer> sorted_indexes = org.scripps.util.MapFun.sortMapByValue(selected);
		Collections.reverse(sorted_indexes);
		List<Classifier> keepers = new ArrayList<Classifier>();
		for(int i=0; i<n_trees; i++){
			keepers.add(classifiers[(int)sorted_indexes.get(i)]);
		}
		voter.setClassifiers(keepers.toArray(new Classifier[keepers.size()]));	
		return voter;
	}



	/**
	 * Simple, manual attribute filtering.  Follows basic pattern of Vant'Veer 2002 and other early array processing approaches
	 * Remove attributes that don't have at least n_samples_over_min with min_expression_change.
	 * If outlier_deletion, remove any attributes that contain values over the outlier_threshold.
	 * @param min_expression_change
	 * @param n_samples_over_min
	 * @param outlier_threshold
	 * @param remove_atts_with_outliers
	 */

	public void executeManualAttFiltersTrainTest(float min_expression_change, int n_samples_over_min, int outlier_threshold, boolean remove_atts_with_outliers){
		//reduce N genes by eliminating genes not significantly regulated in at least three samples
		//	System.out.println("Train start n atts = "+getTrain().numAttributes());
		Enumeration<Attribute> atts = getTrain().enumerateAttributes();
		List<Integer> keepers = new ArrayList<Integer>();
		while(atts.hasMoreElements()){
			Attribute att = atts.nextElement();
			//check if we want to keep it
			boolean keep = false;
			Enumeration<Instance> instances = getTrain().enumerateInstances();
			int n_sig_var = 0;
			while(instances.hasMoreElements()){
				Instance instance = instances.nextElement();
				double value = instance.value(att);
				if(value>min_expression_change||value<(-1*min_expression_change)){
					n_sig_var++;
				}
				if(n_sig_var>2){
					keep = true;
				}
				if(value > outlier_threshold||value<(1*-outlier_threshold)){
					keep=false;
					break;
				}
			}
			if(keep){
				keepers.add(att.index());				
			}
		}
		//keep the class index
		keepers.add(getTrain().classIndex());
		//		System.out.println("Manual filter reduces atts to: "+keepers.size());
		//remove the baddies
		Remove remove = new Remove();
		remove.setInvertSelection(true);
		int[] karray = new int[keepers.size()];
		int c = 0;
		for(Integer i : keepers){
			karray[c] = i;
			c++;
		}
		remove.setAttributeIndicesArray(karray);
		try {
			remove.setInputFormat(getTrain());
			setTrain(Filter.useFilter(getTrain(), remove));
			if(getTest()!=null){
				remove.setInputFormat(getTest());
				setTest(Filter.useFilter(getTest(), remove));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

	/**
	 * Convert all continuous variables into binary, nominal attributes (yes or no..)
	 */
	public void binarize(){

	}

	public void setTrain(Instances train) {
		this.train = train;
	}



	public Instances getTrain() {
		return train;
	}



	public Instances getTest() {
		return test;
	}



	public void setTest(Instances test) {
		this.test = test;
	}



	public Random getRand() {
		return rand;
	}



	public void setRand(Random rand) {
		this.rand = rand;
	}



	public String getEval_method() {
		return eval_method;
	}



	public void setEval_method(String eval_method) {
		this.eval_method = eval_method;
	}


	public String getDataset() {
		return dataset;
	}

	public void setDataset(String dataset) {
		this.dataset = dataset;
	}




	public Map<String, Feature> getFeatures() {
		return features;
	}




	public void setFeatures(Map<String, Feature> features) {
		this.features = features;
	}







}

