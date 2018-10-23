// https://searchcode.com/api/result/60406371/

package org.scripps.combo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.scripps.combo.weka.Weka;
import org.scripps.combo.weka.Weka.metaExecution;
import org.scripps.ontologies.go.Annotations;
import org.scripps.ontologies.go.GOowl;
import org.scripps.ontologies.go.GOterm;
import org.scripps.util.Gene;
import org.scripps.util.MapFun;
import org.scripps.util.MyGeneInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import weka.attributeSelection.AttributeEvaluator;
import weka.attributeSelection.ChiSquaredAttributeEval;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.OneRAttributeEval;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.rules.JRip;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.Remove;
import weka.gui.graphvisualizer.BIFParser;
import weka.gui.graphvisualizer.DotParser;
import weka.gui.graphvisualizer.GraphEdge;
import weka.gui.graphvisualizer.GraphNode;



public class Scratch {
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception  {

		//buildrankedListofGenesForEnrichmentTesting();
		//		String out = "/Users/bgood/genegames/go_group_trees_unfiltered_10_10cv.txt";
		//		testAllGoClassesAsFeatureSets(out);
		//testAllGOForest();
		//makeAndTest70geneClassifier();
		//crossvalidateTest();
		//geneSetSearch();
//		String train_file = "/Users/bgood/workspace/athecure/WebContent/WEB-INF/data/dream/Exprs_CNV_2500genes.arff" ;
//		String metadatafile = "/Users/bgood/workspace/athecure/WebContent/WEB-INF/data/dream/id_map.txt"; 
//		Weka weka = new Weka();
//		weka.buildWeka(new FileInputStream(train_file), null, "dream_breast_cancer");

		String train_file = "/Users/bgood/workspace/aacure/WebContent/WEB-INF/pubdata/griffith/griffith_breast_cancer_2.arff" ;		
		Weka weka = new Weka();
		weka.buildWeka(new FileInputStream(train_file), null, "griffith_breast_cancer_1");
//		//run a basic rule-based attribute filter
		float min_expression_change = (float)4;
		int n_samples_over_min = 50; int outlier_threshold = 15; boolean remove_atts_with_outliers = true;
		System.out.println("before "+weka.getTrain().numAttributes());
		weka.executeManualAttFiltersTrainTest(min_expression_change, n_samples_over_min, outlier_threshold, remove_atts_with_outliers);
		System.out.println("after "+weka.getTrain().numAttributes());
//
		
	}
	
	
	
	
//
//	public static void buildrankedListofGenesForEnrichmentTesting() throws Exception {
//		String train_data = "/usr/local/data/vantveer/breastCancer-train-filtered.arff";
//		String test_data = "/usr/local/data/vantveer/breastCancer-test.arff";
//		String meta = "/usr/local/data/vantveer/breastCancer-train_meta.txt";
//		String annotations = "/usr/local/data/go2gene_3_51.txt";
//		GoWeka gow = new GoWeka(train_data, test_data, meta, annotations);
//		//weka.filters.Filter.
//		AttributeSelection as = new AttributeSelection();
//		//	InfoGainAttributeEval infogain = new InfoGainAttributeEval();
//		ChiSquaredAttributeEval infogain = new ChiSquaredAttributeEval();
//		Ranker ranker = new Ranker();
//		String[] options = {"-T","0.0","-N","-1"};
//		as.setEvaluator(infogain);
//		as.setSearch(ranker);
//		try {
//			as.setInputFormat(gow.getTrain());
//			ranker.setOptions(options);
//			Instances filtered = Filter.useFilter(gow.getTrain(), as); 			
//			double[][] ranked = ranker.rankedAttributes();
//			//add the scores to the gene cards
//			for(int att=0; att<ranked.length; att++){
//				int att_id = (int)ranked[att][0];
//				float att_value = (float)ranked[att][1];
//				Attribute tmp = gow.getTrain().attribute(att_id);
//				card c = gow.getAtt_meta().get(tmp.name());
//				//				if(c==null){
//				//					c = new Weka.card(0, tmp.name(), "_", "_");
//				//				}
//				//				c.setPower(att_value);
//				gow.getAtt_meta().put(tmp.name(), c);
//				if(att_value > 0){
//					if(c!=null){
//						System.out.println(c.unique_id+"\t"+c.name+"\t"+att_value);
//					}else{
//						System.out.println("\tatt:"+att_value+"\t"+att_value);
//					}
//				}
//			}
//			//train = filtered;
//			System.out.println(ranked[0][0]+" "+ranked[0][1]);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	public static void addTermInfoToGOtreeData(String infile, String outfile){
//		String gos = infile;
//		String gos2 = outfile;
//		GOowl gowl = new GOowl();
//		String goowlfile = "file:/users/bgood/data/ontologies/5-12-2012/go_daily-termdb.owl";
//		gowl.init(goowlfile, false);
//		BufferedReader f = null;
//		try {
//			FileWriter w = new FileWriter(gos2);
//			f = new BufferedReader(new FileReader(gos));			
//			String line = f.readLine();
//			while(line!=null){
//				String[] item = line.split("\t");
//				GOterm t = gowl.makeGOterm(item[0]);
//				if(t!=null){
//					w.write(t.getRoot()+"\t"+t.getTerm()+"\t"+line+"\n");
//				}else{
//					w.write("\t\t"+line+"\n");
//				}
//				line = f.readLine();
//			}
//			w.close();
//			f.close();
//
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 * check to see how well the GO categories segregate useful, independent feature groups	
//	 * @param out
//	 * @throws IOException
//	 */
//	public static void testAllGOForest() throws Exception{
//		String train_data = "/usr/local/data/vantveer/breastCancer-train-filtered.arff";
//		String test_data = "/usr/local/data/vantveer/breastCancer-test.arff";
//		String meta = "/usr/local/data/vantveer/breastCancer-train_meta.txt";
//		String annotations = "/usr/local/data/go2gene_3_51.txt";
//		GoWeka baseweka = new GoWeka(train_data, test_data, meta, annotations);
//		GOowl gowl = new GOowl();
//		String goowlfile = "file:/users/bgood/data/ontologies/5-12-2012/go_daily-termdb.owl";
//		gowl.init(goowlfile, false);
//
//		//	System.out.println(s.pruneAndExecute("1,2,"));
//		int ngo = 0; 
//		Set<String> missing_geneids = new HashSet<String>();
//		Set<String> found_geneids = new HashSet<String>();
//
//		float best_score = 0;
//		List<String> keys = new ArrayList<String>(baseweka.acc2name.keySet());
//		//only use go that map to at least one gene in the filtered dataset
//		keys = baseweka.getGoAccsWithMapInFilteredData(keys);
//
//		int p = keys.size();
//		//		int p = s.go2genes.keySet().size();
//		System.out.println(p+" usable go terms to test");
//		//		System.out.println(ngo+"\t"+go+"\t"+got.getTerm()+"\t"+e.eval.pctCorrect()+"\t"+e.avg_percent_correct+"\t"+etestset.eval.pctCorrect()+"\t"+em.eval.pctCorrect()+"\t"+emtestset.avg_percent_correct+"\t"+emtestset.eval.pctCorrect());
//
//		System.out.println("ngo\tgo\tterm\tgo_pctCorrect()_cv\tavg_cv_pct_correct_10_10\tgo_pctCorrect()_test\tforest_pctCorrect()_cv\tforest_10-10dv\tforest_pctCorrect()_test");
//
//		//track the sets of features for the forest
//		Set<String> indices = new HashSet<String>();
//		Set<String> used_go = new HashSet<String>();
//
//		keys = new ArrayList<String>();
//		keys.add("GO:0016796");
//		keys.add("GO:0005753");
//		keys.add("GO:0043297");
//		keys.add("GO:0048702");
//		keys.add("GO:0022601");
//		keys.add("GO:0071942");
//		keys.add("GO:0070830");
//
//		for(String go : keys){
//			p--;
//			Set<String> genes = baseweka.go2genes.get(go);
//			String atts = "";
//			//	System.out.println(genes.length+" genes in set");
//			int found = 0; int n_atts = 0;
//			String names = "";
//			String geness = "";
//			for(String gene : genes){
//				String id = gene;
//				List<Weka.card> cards = baseweka.geneid_cards.get(id);
//				if(cards!=null){
//					found_geneids.add(gene);
//					found++;
//					for(Weka.card card : cards){
//						atts+=card.getAtt_index()+",";
//						n_atts++;
//						geness+=gene+",";
//						names+=card.getAtt_name()+",";
//					}
//				}else{
//					missing_geneids.add(gene);
//				}
//			} 
//
//			if(found>1&&found<25){
//				J48 wekamodel = new J48();
//				wekamodel.setUnpruned(false); 
//				baseweka.setEval_method("cross_validation");
//				Weka.execution e = baseweka.pruneAndExecute(atts, wekamodel);
//				double correct = e.eval.pctCorrect();
//				if(correct>0){
//					ngo++;
//					indices.add(atts);
//					used_go.add(go);
//					//run the forest in cv
//					metaExecution em = baseweka.executeNonRandomForest(indices);
//					//now test both on the test set
//					baseweka.setEval_method("test_set");
//					metaExecution emtestset = baseweka.executeNonRandomForest(indices);
//					Weka.execution etestset = baseweka.pruneAndExecute(atts, wekamodel);
//					GOterm got = gowl.makeGOterm(go);
//					//					System.out.println(ngo+"\t"+go+"\t"+got.getTerm()+"\t"+e.eval.pctCorrect()+"\t"+e.avg_percent_correct+"\t"+etestset.eval.pctCorrect()+"\t"+em.eval.pctCorrect()+"\t"+emtestset.eval.pctCorrect());
//					System.out.println(ngo+"\t"+go+"\t"+got.getTerm()+"\t"+e.eval.pctCorrect()+"\t"+e.avg_percent_correct+"\t"+etestset.eval.pctCorrect()+"\t"+em.eval.pctCorrect()+"\t"+em.avg_percent_correct+"\t"+emtestset.eval.pctCorrect());
//
//				}			
//			}
//			//runs the forest using only internal cross-validation for GO-based attribute selection
//			//(no cheating by peaking ahead...)
//			//						if(found>1&&found<25){
//			//							ngo++;
//			//							indices.add(atts);
//			//							used_go.add(go);
//			//							//run the forest
//			//							//metaExecution em = baseweka.executeNonRandomForest(indices);
//			//							if(ngo%10==0){
//			//								metaExecution cross_validated;
//			//								try {
//			//									cross_validated = baseweka.executeNonRandomForestWithInternalCVparamselection(indices);
//			//									if(cross_validated!=null){
//			//										//train and test on test set
//			//										int n_trees = 7;
//			//										Classifier voter = baseweka.getCVSelectedVoterBest(baseweka.getTrain(), indices, n_trees);
//			//										voter.buildClassifier(baseweka.getTrain());
//			//										Evaluation test_set = new Evaluation(baseweka.getTrain());
//			//										test_set.evaluateModel(voter, baseweka.getTest());
//			//										Evaluation training_set = new Evaluation(baseweka.getTrain());
//			//										training_set.evaluateModel(voter, baseweka.getTrain());
//			//										System.out.println(ngo+"\t"+go+"\t"+cross_validated.eval.pctCorrect()+"\t"+test_set.pctCorrect()+"\t"+training_set.pctCorrect());
//			//									}else{
//			//										System.out.println(ngo+"\tno model");
//			//									}
//			//								} catch (Exception e1) {
//			//									// TODO Auto-generated catch block
//			//									e1.printStackTrace();
//			//								}
//			//							}
//			//						}			
//		}
//
//
//	}
//
//	/** 
//	 * estimate value of cross-validation on selected dataset
//	 * @throws FileNotFoundException 
//	 */
//	public static void crossvalidateTest() throws Exception{
//		//load weka with full training and testing set
//		//		String train_file = "/Users/bgood/data/arrays/Golub/leukemia_train_38x7129.arff"; String test_file = "/Users/bgood/data/arrays/Golub/leukemia_test_34x7129.arff";
//		String train_file = "/usr/local/data/vantveer/breastCancer-train.arff";
//		String test_file = "/usr/local/data/vantveer/breastCancer-test.arff";
//		String meta = "/usr/local/data/vantveer/breastCancer-train_meta.txt";
//		String annotations = "/usr/local/data/go2gene_3_51.txt";
//		System.out.println("Method\tloop\tweka.getTrain().numAttributes()\trsquare_cv\trsquare_train");
//		float min_expression_change = (float)0.3;
//		int ngenes = 0;
//		for(int outer=0; outer<20; outer++){
//			Weka weka = new Weka(train_file, test_file, meta);
//			//run a basic rule-based attribute filter
//			//min_expression_change+=0.1;
//			int n_samples_over_min = outer; int outlier_threshold = 10; boolean remove_atts_with_outliers = true;
//			weka.executeManualAttFiltersTrainTest(min_expression_change, n_samples_over_min, outlier_threshold, remove_atts_with_outliers);
//
//			Instances realtrain = new Instances(weka.getTrain());
//			Instances realtest = new Instances(weka.getTest());
//
//			//		weka.setCardPower(new OneRAttributeEval());
//			//		System.out.println("eval_test.pctCorrect()\teval_train.pctCorrect()\teval_cv.pctCorrect()\tmean_info_gain\tmax_info_gain\tsum_info_gain");
//			double[][] train_test = new double[200][2];
//			double[][] cv_test = new double[200][2];
//			ngenes+=1;
//			DescriptiveStatistics test_set_scores = new DescriptiveStatistics();
//			for(int r=0;r<200;r++){
//				weka.setTrain(new Instances(realtrain));
//				weka.setTest(new Instances(realtest));
//				DescriptiveStatistics gene_power = new DescriptiveStatistics();
//				List<Integer> keepers = new ArrayList<Integer>();
//				for(int g=0; g<ngenes; g++){
//					int randomNum = weka.getRand().nextInt(weka.getTrain().numAttributes()-1);
//					keepers.add(randomNum);
//					//				gene_power.addValue((double)weka.getAtt_meta().get(weka.getTrain().attribute(randomNum).name()).power);
//				}
//				//keep the class index
//				keepers.add(weka.getTrain().classIndex());
//				//remove the rest
//				Remove remove = new Remove();
//				remove.setInvertSelection(true);
//				int[] karray = new int[keepers.size()];
//				int c = 0;
//
//				for(Integer i : keepers){
//					karray[c] = i;
//					c++;
//				}
//				remove.setAttributeIndicesArray(karray);
//				try {
//					remove.setInputFormat(weka.getTrain());
//					weka.setTrain(Filter.useFilter(weka.getTrain(), remove));
//					remove.setInputFormat(weka.getTest());
//					weka.setTest(Filter.useFilter(weka.getTest(), remove));
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//				Classifier classifier = new J48();
//
//				//now evaluate it in cv and test set
//				try {
//					//cross-validation
//					Evaluation eval_cv = new Evaluation(weka.getTrain());
//					eval_cv.crossValidateModel(classifier, weka.getTrain(), 10, weka.getRand());
//					//System.out.println("10f cross-validation\n"+eval_cv.toSummaryString());
//					//test set
//					Evaluation eval_test = new Evaluation(weka.getTrain());
//					classifier.buildClassifier(weka.getTrain());
//					eval_test.evaluateModel(classifier, weka.getTest());
//					test_set_scores.addValue(eval_test.pctCorrect());
//					//System.out.println("\nTest Set\n"+eval_test.toSummaryString());
//					//training set 
//					Evaluation eval_train = new Evaluation(weka.getTrain());
//					classifier.buildClassifier(weka.getTrain());
//					eval_train.evaluateModel(classifier, weka.getTrain());
//					//System.out.println("\nTraining set\n"+eval_train.toSummaryString());
//					//				System.out.println(+eval_test.pctCorrect()+"\t"+eval_train.pctCorrect()+"\t"+eval_cv.pctCorrect()+"\t"+gene_power.getMean()+"\t"+gene_power.getMax()+"\t"+gene_power.getSum());
//					train_test[r][0] = eval_train.pctCorrect();
//					train_test[r][1] = eval_test.pctCorrect();
//					cv_test[r][0] = eval_cv.pctCorrect();
//					cv_test[r][1] = eval_test.pctCorrect();
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			SimpleRegression cv_regression = new SimpleRegression();
//			cv_regression.addData(cv_test);
//			double rsquare_cv = cv_regression.getRSquare();
//
//			SimpleRegression train_regression = new SimpleRegression();
//			train_regression.addData(train_test);
//			double rsquare_train = train_regression.getRSquare();
//			System.out.println("SMO\t"+outer+"\t"+ngenes+"\t"+rsquare_cv+"\t"+rsquare_train+"\t"+test_set_scores.getMax()+"\t"+test_set_scores.getMean());
//		}
//	}	
//
//	/** 
//	 * Search through the dataset for gene sets that result in trees that score well on the training set.
//	 * @throws FileNotFoundException 
//	 */
//	public static void geneSetSearch() throws FileNotFoundException{
//		int n_genes_in_set = 5;
//		int population_size = 300000;
//		String outfile = "/Users/bgood/data/arrays/breastcancer/ngf_processed_vandevijver_5trees.txt";
//		//load weka with full training and testing set
//		//		String train_file = "/Users/bgood/data/arrays/Golub/leukemia_train_38x7129.arff"; String test_file = "/Users/bgood/data/arrays/Golub/leukemia_test_34x7129.arff";
//	//	String train_file = "/usr/local/data/vantveer/breastCancer-train.arff";
//		String train_file = "/Users/bgood/data/arrays/breastcancer/ngf_processed_vandevijver.arff";
//		System.out.println("Step\tN_kept\tAttributes\tAtt_indexes\ttraining_set_score\tauc\ttree_size\tleaves");
//		FileWriter f = null;
//		try {
//			f = new FileWriter(outfile);
//			f.write("Step\tN_kept\tAttributes\tAtt_indexes\ttraining_set_score\ttree_size\tleave\n");
//			f.close();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		float min_expression_change = (float)0.3;		
//		Weka weka = new Weka(train_file);
//		//run a basic rule-based attribute filter
//	//	int n_samples_over_min = 3; int outlier_threshold = 10; boolean remove_atts_with_outliers = true;
//	//	weka.executeManualAttFiltersTrainTest(min_expression_change, n_samples_over_min, outlier_threshold, remove_atts_with_outliers);
//
//		//run a binarize filter
//		
//		
//		Instances realtrain = new Instances(weka.getTrain());
//		long t = System.currentTimeMillis();
//		int n_kept = 0;
//		for(int r=0;r<population_size;r++){
//			weka.setTrain(new Instances(realtrain));
//			List<Integer> keepers = new ArrayList<Integer>();
//			String names = ""; String ids = "";
//			for(int g=0; g<n_genes_in_set; g++){
//				int randomNum = weka.getRand().nextInt(weka.getTrain().numAttributes()-1);
//				keepers.add(randomNum);
//				names+=realtrain.attribute(randomNum).name()+",";
//				ids+=(randomNum+1)+","; // for easy weka inspecting
//			}
//			//keep the class index
//			keepers.add(weka.getTrain().classIndex());
//			//remove the rest
//			Remove remove = new Remove();
//			remove.setInvertSelection(true);
//			int[] karray = new int[keepers.size()];
//			int c = 0;
//
//			for(Integer i : keepers){
//				karray[c] = i;
//				c++;
//			}
//			remove.setAttributeIndicesArray(karray);
//			try {
//				remove.setInputFormat(weka.getTrain());
//				weka.setTrain(Filter.useFilter(weka.getTrain(), remove));
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//			J48 classifier = new J48();
//
//			//now evaluate it on the training set
//			try {
//				//cross-validation
////				Evaluation eval_cross = new Evaluation(weka.getTrain());
////				eval_cross.crossValidateModel(classifier, realtrain, 5, weka.getRand());
//				//training set 
//				Evaluation eval_train = new Evaluation(weka.getTrain());
//				classifier.buildClassifier(weka.getTrain());
//				double leaves = classifier.measureNumLeaves();
//				double tree_size = classifier.measureTreeSize();
//				if(tree_size>1){
//					eval_train.evaluateModel(classifier, weka.getTrain());
//					if(eval_train.pctCorrect()>75){
//						n_kept++;
//						if(r%1==0){
//							System.out.println(r+"\t"+n_kept+"\t"+names+"\t"+ids+"\t"+eval_train.pctCorrect()+"\t"+eval_train.areaUnderROC(0)+"\t"+tree_size+"\t"+leaves);
//						}
//						f = new FileWriter(outfile, true);					
//						f.write(r+"\t"+n_kept+"\t"+names+"\t"+ids+"\t"+eval_train.pctCorrect()+"\t"+tree_size+"\t"+leaves+"\n");
//						f.close();
//					}
//				}
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//
//
//	}
//
//
//
//	/** try to reproduce the result
//	 *  from the 2002 VantVeer paper
//	 * @throws FileNotFoundException 
//	 */
//	public static void makeAndTest70geneClassifier() throws Exception{
//		//load weka with full training and testing set
//		String meta = "/usr/local/data/vantveer/breastCancer-train_meta.txt";
//
//		Weka weka = new Weka("/usr/local/data/vantveer/breastCancer-train.arff","/usr/local/data/vantveer/breastCancer-test.arff", meta);
//		//reduce to about 5,000 genes by eliminating genes not significantly regulated in at least three samples
//		System.out.println("Train start n atts = "+weka.getTrain().numAttributes());
//		Enumeration<Attribute> atts = weka.getTrain().enumerateAttributes();
//		List<Integer> keepers = new ArrayList<Integer>();
//		int with_gene = 0;
//		while(atts.hasMoreElements()){
//			Attribute att = atts.nextElement();
//			//check if we want to keep it
//			boolean keep = false;
//			Enumeration<Instance> instances = weka.getTrain().enumerateInstances();
//			int n_sig_var = 0;
//			while(instances.hasMoreElements()){
//				Instance instance = instances.nextElement();
//				double value = instance.value(att);
//				if(value>0.3||value<-0.3){
//					n_sig_var++;
//				}
//				if(n_sig_var>2){
//					keep = true;
//				}
//				if(value > 10||value<-10){
//					keep=false;
//					break;
//				}
//			}
//			if(keep){
//				keepers.add(att.index());
//				//check for gene, go annotations
//				card card = weka.att_meta.get(att.name());
//				if(card!=null){
//					if(card.getUnique_id()!=null){
//						with_gene++;	
//					}
//				}
//			}
//		}
//		//keep the class index
//		keepers.add(weka.getTrain().classIndex());
//		System.out.println("First filter reduces atts to: "+keepers.size()+" with gene "+with_gene);
//		with_gene = 0;
//		//remove the baddies
//		Remove remove = new Remove();
//		remove.setInvertSelection(true);
//		int[] karray = new int[keepers.size()];
//		int c = 0;
//		for(Integer i : keepers){
//			karray[c] = i;
//			c++;
//		}
//		remove.setAttributeIndicesArray(karray);
//		try {
//			remove.setInputFormat(weka.getTrain());
//			weka.setTrain(Filter.useFilter(weka.getTrain(), remove));
//			remove.setInputFormat(weka.getTest());
//			weka.setTest(Filter.useFilter(weka.getTest(), remove));
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println("Train/test filtered n atts = "+weka.getTrain().numAttributes()+"/"+weka.getTest().numAttributes());
//
//		//lump the following into one attribute selected classifier
//		//calculate the correlation coefficient between each gene and disease outcome 
//		//find 231 genes with correlation <-0.3 or >0.3
//		//sort the 231 genes by amount of correlation
//		//test top 70 genes (they used the correlations of the expression profile of the 'leave-one-out' sample with the mean expression levels of the remaining samples from the good and the poor prognosis patients, respectively)
//
//		AttributeSelectedClassifier classifier = new AttributeSelectedClassifier();
//		ChiSquaredAttributeEval evaluator = new ChiSquaredAttributeEval();
//		Ranker search = new Ranker();
//		search.setNumToSelect(70);
//		classifier.setEvaluator(evaluator);
//		classifier.setSearch(search);
//		classifier.setClassifier(new SMO());
//
//		//now evaluate it in cv and test set
//		try {
//			//cross-validation
//			Evaluation eval_cv = new Evaluation(weka.getTrain());
//			eval_cv.crossValidateModel(classifier, weka.getTrain(), 10, weka.getRand());
//			System.out.println("10f cross-validation\n"+eval_cv.toSummaryString());
//			//test set
//			Evaluation eval_test = new Evaluation(weka.getTrain());
//			classifier.buildClassifier(weka.getTrain());
//			eval_test.evaluateModel(classifier, weka.getTest());
//			System.out.println("\nTest Set\n"+eval_test.toSummaryString());
//			//training set 
//			Evaluation eval_train = new Evaluation(weka.getTrain());
//			classifier.buildClassifier(weka.getTrain());
//			eval_train.evaluateModel(classifier, weka.getTrain());
//			System.out.println("\nTraining set\n"+eval_train.toSummaryString());
//
//			//output selected attribute and metadata
//			Map<String, Set<String>> go2genes = Annotations.readCachedGoAcc2Genes("/usr/local/data/go2gene_3_51.txt");
//			Map<String, Set<String>> gene2gos = MapFun.flipMapStringSetStrings(go2genes);
//			AttributeSelection as = new AttributeSelection();
//			as.setEvaluator(evaluator);
//			as.setInputFormat(weka.getTrain());
//			as.setSearch(search);
//			Instances filtered = Filter.useFilter(weka.getTrain(), as); 					
//			Enumeration<Attribute> filtered_atts = filtered.enumerateAttributes();
//			while(filtered_atts.hasMoreElements()){
//				Attribute f = filtered_atts.nextElement();
//				card card = weka.att_meta.get(f.name());
//				if(card!=null){
//					if(card.getUnique_id()!=null){
//						with_gene++;	
//						System.out.println(card.att_name+"\t"+card.name+"\t"+card.unique_id+"\t"+gene2gos.get(card.getUnique_id()));
//					}
//				}else{
//					System.out.println(f.name());
//				}
//			}
//			System.out.println("NUm of 70 with gene id: "+with_gene);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//
//	}
//
//	public static void testAllGoClassesAsFeatureSets(String out) throws Exception{
//		String train_data = "/usr/local/data/vantveer/breastCancer-train-filtered.arff";
//		String test_data = "/usr/local/data/vantveer/breastCancer-test.arff";
//		String meta = "/usr/local/data/vantveer/breastCancer-train_meta.txt";
//		String annotations = "/usr/local/data/go2gene_3_51.txt";
//		GoWeka s = new GoWeka(train_data, test_data, meta, annotations);
//		//filters the training and test sets based on the good parts of the training data
//		//will over-estimate performance of subsequent cross-validation.
//		//s.filterTrainAndTestSetForNonZeroInfoGainAttsInTrain();
//		//s.remapAttNames();
//		//	System.out.println(s.pruneAndExecute("1,2,"));
//		int ngo = 0; 
//		Set<String> missing_geneids = new HashSet<String>();
//		Set<String> found_geneids = new HashSet<String>();
//		Set<String> keepers = new HashSet<String>();
//		J48 wekamodel = new J48();
//		wekamodel.setUnpruned(true); 
//		float best_score = 0;
//		List<String> keys = new ArrayList<String>(s.acc2name.keySet());
//		//only use go that map to at least one gene in the filtered dataset
//		keys = s.getGoAccsWithMapInFilteredData(keys);
//		int p = keys.size();
//		//		int p = s.go2genes.keySet().size();
//		System.out.println(p+" usable go terms to test");
//		FileWriter f = new FileWriter(out);
//		f.write("go	pctcorrect_cv_10-10	pctcorrect_cv_1_10	pctcorrect_test	genes	att name	att id");
//		//		for(String go : s.go2genes.keySet()){
//		for(String go : keys){
//			p--;
//			Set<String> genes = s.go2genes.get(go);
//			String atts = "";
//			//	System.out.println(genes.length+" genes in set");
//			int found = 0; int n_atts = 0;
//			String names = "";
//			String geness = "";
//			for(String gene : genes){
//				String id = gene;
//				List<Weka.card> cards = s.geneid_cards.get(id);
//				if(cards!=null){
//					found_geneids.add(gene);
//					found++;
//					for(Weka.card card : cards){
//						atts+=card.getAtt_index()+",";
//						n_atts++;
//						geness+=gene+",";
//						names+=card.getAtt_name()+",";
//					}
//				}else{
//					missing_geneids.add(gene);
//				}
//			} 
//			if(found>0&&found<10){
//				ngo++;
//				//System.out.println(ngo+" go "+go+" had "+genes.length+" found "+found+" atts: "+atts+" genes: "+geness);
//				//run the tree on the set and score it
//				s.setEval_method("cross_validation");
//				Weka.execution e = s.pruneAndExecute(atts, wekamodel);
//				s.setEval_method("test_set");
//				Weka.execution e_test = s.pruneAndExecute(atts, wekamodel);
//				f.write(go+"\t"+e.avg_percent_correct+"\t"+e.eval.pctCorrect()+"\t"+e_test.eval.pctCorrect()+"\t"+n_atts+"\t"+geness+"\t"+names+"\t"+atts+"\n");
//				if(e.avg_percent_correct>70){
//					keepers.add(go);
//				}
//				if(e.avg_percent_correct> best_score){
//					best_score = (float)e.avg_percent_correct;
//					System.out.println(go+"\t"+e.avg_percent_correct+"\t"+e.eval.pctCorrect()+"\t"+n_atts+"\t"+geness+"\t"+names+"\t"+atts);
//				}
//			}
//			if(p%100==0){
//				System.out.println(1-((float)p/(float)s.go2genes.keySet().size())+" percent done "+p);
//			}
//		}
//		//check combos
//		//		p = keepers.size()*keepers.size();
//		//		System.out.println(p+" usable combo go terms to test");
//		//		Set<String> done = new HashSet<String>();
//		//		for(String go1 : keepers){
//		//			for(String go2 : new HashSet<String>(keepers)){
//		//				p--;
//		//				String go2go = go1+go2; String go2go2 = go2+go1;
//		//				if(done.contains(go2go)||done.contains(go2go2)){
//		//					continue;
//		//				}
//		//				done.add(go2go); done.add(go2go2);
//		//				Set<String> genes = s.go2genes.get(go1);
//		//				genes.addAll(s.go2genes.get(go2));
//		//
//		//				String atts = "";
//		//				//	System.out.println(genes.length+" genes in set");
//		//				int found = 0; int n_atts = 0;
//		//				String names = "";
//		//				String geness = "";
//		//				for(String gene : genes){
//		//					String id = gene;
//		//					List<Weka.card> cards = s.geneid_cards.get(id);
//		//					if(cards!=null){
//		//						found_geneids.add(gene);
//		//						found++;
//		//						for(Weka.card card : cards){
//		//							atts+=card.getAtt_index()+",";
//		//							n_atts++;
//		//							geness+=gene+",";
//		//							names+=card.getAtt_name()+",";
//		//						}
//		//					}else{
//		//						missing_geneids.add(gene);
//		//					}
//		//				} 
//		//				if(found>0&&found<21){
//		//					ngo++;
//		//					//System.out.println(ngo+" go "+go+" had "+genes.length+" found "+found+" atts: "+atts+" genes: "+geness);
//		//					//run the tree on the set and score it
//		//					s.setEval_method("cross_validation");
//		//					Weka.execution e = s.pruneAndExecute(atts, wekamodel);
//		//					s.setEval_method("test_set");
//		//					Weka.execution e_test = s.pruneAndExecute(atts, wekamodel);								
//		//					f.write(go2go+"\t"+e.eval.pctCorrect()+"\t"+e_test.eval.pctCorrect()+"\t"+n_atts+"\t"+geness+"\t"+names+"\t"+atts+"\n");
//		//					if(e.eval.pctCorrect()> best_score){
//		//						best_score = (float)e.eval.pctCorrect();
//		//						System.out.println(go2go+"\t"+e.eval.pctCorrect()+"\t"+n_atts+"\t"+geness+"\t"+names+"\t"+atts);
//		//					}
//		//				}
//		//				if(p%100==0){
//		//					System.out.println(1-((float)p/(float)(keepers.size()*keepers.size()))+" percent done of"+p);
//		//				}
//		//			}
//		//		}
//		f.close();
//		//		System.out.println("missing genes\t"+missing_geneids.size()+"\tfound ids\t"+found_geneids.size());
//
//	}
//
	public static void cacheExpandedGoAnnotations(){
		//cache go2genes
		String goowlfile = "file:/users/bgood/data/ontologies/5-12-2012/go_daily-termdb.owl";
		String anno_file = "/users/bgood/data/ontologies/5-12-2012/gene2go";
		String outputfile = "/usr/local/data/go2gene_3_51.txt";// "/users/bgood/data/ontologies/5-12-2012/go2gene_3_51.txt";
		int min_set_size = 3; int max_set_size = 50;
		try {
			//Annotations.cacheGo2Genes(anno_file, "9606", false, true, goowlfile, outputfile, min_set_size, max_set_size);
			//read them in
			Map<String, Set<String>> go2genes = Annotations.readCachedGoAcc2Genes(outputfile);
			System.out.println(go2genes.get("GO:0070830")); //\tProcess\ttight junction assembly
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}

