// https://searchcode.com/api/result/49457194/

package classifier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import knowledgeBase.RelationInstance;
import knowledgeBase.Relationship;
import trainingDataGenerator.AnnotatedCorpusReader;
import trainingDataGenerator.AnnotatedSentenceParserAdapter;
import trainingDataGenerator.AnnotationConstant;
import trainingDataGenerator.PreprocessFromCandidate;
import trainingDataGenerator.PreprocessFromCandidate.PreprocessWriter;
import trainingDataGenerator.TrainingCandidateGenerator.EntityPairLocation;
import trainingDataGenerator.TrainingConfig;
import trainingDataGenerator.TrainingDataDriver;
import util.Logger;
import cc.factorie.protobuf.DocumentProtos.Relation;
import cc.factorie.protobuf.DocumentProtos.Relation.RelationMentionRef;
import edu.uw.cs.multir.learning.algorithm.AveragedPerceptron;
import edu.uw.cs.multir.learning.algorithm.Model;
import edu.uw.cs.multir.learning.algorithm.Parameters;
import edu.uw.cs.multir.learning.data.Dataset;
import edu.uw.cs.multir.learning.data.MemoryDataset;
import edu.uw.cs.multir.preprocess.Mappings;
import features.extractor.OverallFeatureExtractor;
import features.oldstuff.FeatureExtractor;

/**
 * Evaluate how well the classifier is doing
 * Input: stuff from TrainingCandidate generator - this should be a bunch of folders according to relationships
 * Output: Precision and Recall of classifier
 * 
 * General strategy:
 * Uses K cross validation
 * Divide the entire candidate set into training set and test set K times, each time with differing partition
 * Trains classifier on train set, then run classifier against test set to find precision and recall.
 * Average is taken out of the K runs
 * @author sjonany
 *
 */
public class ClassifierEvaluator {
	private static final String EVALUATOR_TEMP_DIR = TrainingConfig.BASE_DIR + "eval";
	private static final String TEMP_TRAIN_PATH = EVALUATOR_TEMP_DIR + File.separatorChar + "train";
	private static final String TEMP_TEST_PATH = EVALUATOR_TEMP_DIR + File.separatorChar + "test";
	private static String EVALUATION_RESULT_PATH = null;
	private static String CANDIDATE_DIR = null;
	
	public static void main(String[] args) throws Exception{
		// TODO add additional Argument for Output directoy
		if(args.length != 7){
			printUsage();
			return;
		}
		
		int k = Integer.parseInt(args[0]);
		double startRatio = Double.parseDouble(args[1]);
		double endRatio = Double.parseDouble(args[2]);
		double increment = Double.parseDouble(args[3]);
		FeatureExtractor featureExtractor = OverallFeatureExtractor.getFeatureExtractor(args[4]);
		CANDIDATE_DIR = args[5].endsWith("" + File.separatorChar) ? args[5] : args[5] + File.separatorChar;
		EVALUATION_RESULT_PATH = args[6];
		
		if(increment <= 0){
			throw new IllegalArgumentException("Increment must be positive");
		}
		
		double maxF1 = 0;
		double bestNARatio = -1.0;
		
		double curRatio = startRatio;
		while(curRatio <= endRatio){
			double curF1 = performKEval(curRatio, k, featureExtractor);
			if(curF1 > maxF1){
				maxF1 = curF1;
				bestNARatio = curRatio;
			}
			curRatio += increment;
		}

		PrintWriter evalWriter = new PrintWriter(new BufferedWriter(new FileWriter(EVALUATION_RESULT_PATH, true)));
		String argLine = "";
		for(String arg : args){
			argLine += arg + " ";
		}
		evalWriter.write("Arguments : " + argLine + "\n");
		evalWriter.write("Start Ratio : " + startRatio + "\n");
		evalWriter.write("End Ratio : " + endRatio + "\n");
		evalWriter.write("Increment : " + increment + "\n");
		evalWriter.write("Best configuration - NA_TO_NA_RATIO = " +  bestNARatio + ", with average F1 = " + maxF1 + "\n");
		evalWriter.write("-------------------------------------------------------");
		evalWriter.close();
	}
	
	private static void printUsage(){
		// TODO add additional argument to usage
		System.out.println("Input sequence = K(int) Start_NA_Ratio(double) " +
				"End_NA_Ratio(double) Increment(double) FeatureExtractorargs candidateDir outputLogFile");
	}
	
	/**
	 * performs K cross validation using the specified ratio
	 * @param NA_TO_NON_NA_RATIO how many samples of NA against the entire non-NA samples. If 1:1, num NA = num non NA
	 * 		Higher value means more NA, and hence likely to reduce recall, but increase precision.
	 * @return average f1 score of all k iterations
	 * @throws Exception
	 */
	public static double performKEval(double NA_TO_NON_NA_RATIO, int K, FeatureExtractor featureExtractor) throws Exception{
		PrintWriter evalWriter = new PrintWriter(new BufferedWriter(new FileWriter(EVALUATION_RESULT_PATH, true)));
		
		evalWriter.write("NA_TO_NON_NA_RATIO = " + NA_TO_NON_NA_RATIO + "\n");
		evalWriter.write("K = " + K + "\n");
		evalWriter.flush();
		
		//key = relationship, val = how many samples of that relationship exist as candidates
		HashMap<Relationship, Long> sampleCount = getCandidatesSampleCount();
		
		//undersample NA
		long nonNACount = 0;
		for(Relationship r : sampleCount.keySet()){
			if(r.equals(Relationship.NA)){
				continue;
			}
			nonNACount += sampleCount.get(r);
		}
		long maxNACount = (long)(nonNACount * NA_TO_NON_NA_RATIO);
		long totalNACount = sampleCount.get(Relationship.NA);
		sampleCount.put(Relationship.NA, Math.min(maxNACount, totalNACount));
		
		double totalPrecision = 0.0;
		double totalRecall  = 0.0;
		double totalF1 = 0.0;
		
		//run k cross evaluation
		for(long evalIteration = 1; evalIteration <= K; evalIteration++){
			initDirForEval();
			PreprocessWriter trainWriter = new PreprocessWriter(TEMP_TRAIN_PATH);
			
			//go through candidates are partitioned into test and training set
			for(Relationship r : Relationship.values()){
				Logger.writeLine("Loading all annotations for " + r);
				Map<Integer, Map<AnnotationConstant, String>> annotationCache = TrainingDataDriver.initAnnotationCache(r, sampleCount.get(r),CANDIDATE_DIR);
				Logger.writeLine("Finished Loading all annotations for " + r);
				PrintWriter mentionWriter = new PrintWriter(TEMP_TEST_PATH + "/mention_" + r.name());
				
				//the size of the test set for this relationship
				long chunkSize = (long) (1.0 * sampleCount.get(r) / K);
			
				String relInstanceFile = TrainingConfig.getAbsoluteAnnotationPathForEntityPairMention(r, CANDIDATE_DIR);
				BufferedReader relationInstanceReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(relInstanceFile), "UTF-8"));
				String relInstanceLine  = relationInstanceReader.readLine();
				
				long mentionCount = 0;
				long testChunkObtained = 0;
				//for each relation instance, we look at all the mentions in the corpus,
				//and output relationship
				while(relInstanceLine != null){
					if(mentionCount > sampleCount.get(r)){
						break;
					}
					relInstanceLine = relInstanceLine.trim();
					if(relInstanceLine.length() == 0){
						relInstanceLine = relationInstanceReader.readLine();
						continue;
					}
					//parse relInstanceLine
					String[] tokens = relInstanceLine.split("\\|");
					RelationInstance relationInstance = RelationInstance.fromStringDump(tokens[0]);
					List<EntityPairLocation> mentions = new ArrayList<EntityPairLocation>();
					//first token is relation instance, the rest are entity pair locations
					for(int i=1; i<tokens.length; i++){
						mentions.add(EntityPairLocation.deserialize(tokens[i]));
						mentionCount++;
						if(mentionCount > sampleCount.get(r)){
							break;
						}
					}

					//check if this is part of the chunk reserved for test set
					if(mentionCount >= (evalIteration-1) * chunkSize && testChunkObtained < chunkSize ){
						testChunkObtained += mentions.size();
						mentionWriter.write(relInstanceLine + "\n");
					}else{
						PreprocessFromCandidate.writeRelationToDisk(relationInstance, mentions, annotationCache, featureExtractor, trainWriter);
					}
					
					relInstanceLine = relationInstanceReader.readLine();
				}
				mentionWriter.close();
				relationInstanceReader.close();
			}//for each relationship
			trainWriter.close();
			//train.pb and test.pb are in disk
			//preprocess train.pb to get mapping, model and train file on disk
			Mappings mappings = new Mappings();
			mappings.read(TEMP_TRAIN_PATH +"/mapping");
			
			//training- generate params file, but don't serialize
			Random random = new Random(1);
			
			Model model = new Model();
			model.read(TEMP_TRAIN_PATH + File.separatorChar + "model");
			
			AveragedPerceptron ct = new AveragedPerceptron(model, random);
			
			Dataset train = new MemoryDataset(TEMP_TRAIN_PATH  + File.separatorChar + "train");

			System.out.println("starting training");
			
			long start = System.currentTimeMillis();
			Parameters params = ct.train(train);
			long end = System.currentTimeMillis();
			System.out.println("training time " + (end-start)/1000.0 + " seconds");
			
			//now we have the params (aka the brain) file of the current training set
			Classifier classifier = new Classifier(params, mappings);
			
			//Read test set and calculate performance of current classifier
			long tp = 0;
			long fp = 0;
			long fn = 0;
			long tn = 0;
			
			for(Relationship r: Relationship.values()){
				//TODO: do this up there
				Map<Integer, Map<AnnotationConstant, String>> annotCache = initAnnotationCache(r, CANDIDATE_DIR,TEMP_TEST_PATH + "/mention_" + r.name());
				BufferedReader relationInstanceReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(TEMP_TEST_PATH + "/mention_" + r.name()), "UTF-8"));
				String relInstanceLine  = relationInstanceReader.readLine();
				while(relInstanceLine != null){
					
					//parse relInstanceLine
					String[] tokens = relInstanceLine.split("\\|");
					RelationInstance relationInstance = RelationInstance.fromStringDump(tokens[0]);
					List<EntityPairLocation> mentions = new ArrayList<EntityPairLocation>();
					//first token is relation instance, the rest are entity pair locations
					for(int i=1; i<tokens.length; i++){
						mentions.add(EntityPairLocation.deserialize(tokens[i]));
					}
					
					Relation relation = getRelation(relationInstance, mentions, annotCache, featureExtractor);
					
			    	String expectedRelationship = relation.getRelType();
			    	List<List<String>> featureVectors = new ArrayList<List<String>>();
			    	for(RelationMentionRef sentence : relation.getMentionList()){
			    		featureVectors.add(sentence.getFeatureList());
			    	}
			    	String prediction = classifier.classifyMultipleSentences(featureVectors).getRelationship();
					
			    	if(expectedRelationship.equals(Relationship.NA.toString())){
			    		if(prediction.equals(Relationship.NA.toString())){
			    			//true negative does not matter for precision/recall actually
			    			tn++;
			    		}else{
			    			fp++;
			    		}
			    	}else{
			    		if(prediction.equals(expectedRelationship)){
			    			tp++;
			    		}else{
			    			//TODO : false negative calculation
			    			//if prediction = NA, and expected is not NA, im sure it's fn
			    			//but what if prediction = birth place, and expected = death place?
			    			//fn too? 
			    			fn++;
			    		}
			    	}	
					relInstanceLine = relationInstanceReader.readLine();
				}					
				relationInstanceReader.close();
			}
		    
		    double precision = 1.0  * tp / (tp+fp);
		    double recall = 1.0 * tp / (tp+ fn);
		    double f1 = 2.0 * precision*recall /(precision + recall);
		    
		    totalPrecision += precision;
		    totalRecall += recall;
		    totalF1 += f1;
		    
			evalWriter.write("Iteration " +  evalIteration + "\n");
			evalWriter.write("\tTN = " + tn + ", TP = " + tp + ", FN = " + fn + ", FP = " + fp + "\n");
			evalWriter.write("\tPrecision " + precision + "\n");
			evalWriter.write("\tRecall " + recall + "\n");
			evalWriter.write("\tF1-Measure " + f1 + "\n");
			evalWriter.flush();
		}//for eval iteration
		
		evalWriter.write("Summary\n");
		evalWriter.write("\tPrecision " + (totalPrecision/K) + "\n");
		evalWriter.write("\tRecall " + (totalRecall/K) + "\n");
		evalWriter.write("\tF1-Measure " + (totalF1/K) + "\n");
		evalWriter.write("------------------------------------------------------\n");
	
		evalWriter.close();
		return (totalF1/K);
	}
	
	/**
	 * initialize the directory structure needed for temporary dumps of 
	 * training result for a single evaluation iteration
	 * @throws IOException 
	 */
	private static void initDirForEval() throws IOException{
		File f = new File(EVALUATOR_TEMP_DIR);
		//remove the dump from previous iteration
		if (f.exists()) {
		    for (File c : f.listFiles()){
		    	c.delete();
		  	}
		}	
		f.mkdir();
		new File(TEMP_TRAIN_PATH).mkdir();
		new File(TEMP_TEST_PATH).mkdir();
	}
	
	/**
	 * @requires TrainingCandidateGenerator has already been run
	 * @return key = relationship, val = how many samples of that relationship exist as candidates
	 * note that this is the number of mentions/feature vectors, NOT entity pairs
	 * @throws FileNotFoundException 
	 * @throws UnsupportedEncodingException 
	 */
	private static HashMap<Relationship, Long> getCandidatesSampleCount() throws Exception{
		HashMap<Relationship, Long> sampleCount = new HashMap<Relationship, Long>();
		
		for(Relationship r : Relationship.values()){
			long mentionCount = 0;
			BufferedReader relationInstanceReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(TrainingConfig.getAbsoluteAnnotationPathForEntityPairMention(r,CANDIDATE_DIR)), "UTF-8"));
			String relInstanceLine  = relationInstanceReader.readLine();
			
			while(relInstanceLine != null){
				//parse relInstanceLine
				String[] tokens = relInstanceLine.split("\\|");
				//first token is relation instance, the rest are entity pair locations
				//and we only care about the entity pair locations. hence the -1
				mentionCount += tokens.length-1;
				relInstanceLine = relationInstanceReader.readLine();
			}
			sampleCount.put(r, mentionCount);
			relationInstanceReader.close();
		}
		
		return sampleCount;
	}
	
	private static Relation getRelation(RelationInstance relationInstance, List<EntityPairLocation> mentions, 
			Map<Integer, Map<AnnotationConstant,String>> annotationCache, FeatureExtractor featureExtractor) throws Exception{
		Relation.Builder relationBuilder = Relation.newBuilder();
		
		relationBuilder.setSourceGuid(relationInstance.getEntity1().getMid());
		relationBuilder.setDestGuid(relationInstance.getEntity2().getMid());
		relationBuilder.setRelType(relationInstance.getRelationship().toString());
		
		
		for(EntityPairLocation location : mentions){
			int sentenceId = Integer.parseInt(location.getGlobalSentenceId());
			if(!annotationCache.containsKey(sentenceId)){
				throw new IllegalArgumentException("Missing annotation for sentenceID = " + sentenceId + ", relation = " + relationInstance);
			}
			
			AnnotatedSentenceParserAdapter annotations = new AnnotatedSentenceParserAdapter(annotationCache.get(sentenceId));
			
			RelationMentionRef.Builder refMention = RelationMentionRef.newBuilder();
			refMention.setFilename(annotations.getMeta().originalFileName);
			//Congle said these id's do not matter
			refMention.setDestId(-123214);
			refMention.setSourceId(-12234314);
			refMention.setSentence(annotations.getText().sentence);
			
			try{
				for(String feature : getFeatures(annotations, relationInstance, location, featureExtractor)){
					refMention.addFeature(feature);
				}
			}catch(Exception e){
				//This is a short hack, just because there are some misformatting in the preprocessed annotations
				//I assume that any exception thrown here is because the annotations are misformatted, which renders this mention unusable
				continue;
			}
			
			relationBuilder.addMention(refMention);
		}
		
		return relationBuilder.build();
	}
	
	private static List<String> getFeatures(AnnotatedSentenceParserAdapter annotations, 
			RelationInstance relInstance, EntityPairLocation location, FeatureExtractor featureExtractor) throws Exception{
		try{
			List<String> features =  featureExtractor.getFeatures(annotations, location.getRange1(), location.getRange2());
			return features;
		}catch(Exception e){
			Logger.writeLine(annotations.toString());
			Logger.writeLine(relInstance.toStringDump());
			Logger.writeLine(location.serialize());
			throw e;
		}
	}
	
	/**
	 * load all the relevant annotations needed to train r to memory
	 * @param mention file - entity pair mention file, we will only get annot cache which mention the id
	 * @return annotationCache - key = sentence id, value = annotation related to sentence
	 * @throws Exception 
	 */
	public static Map<Integer, Map<AnnotationConstant, String>> initAnnotationCache(Relationship r, String baseDir, String mentionFile) throws Exception{
		Map<Integer, Map<AnnotationConstant, String>> annotCache = new HashMap<Integer, Map<AnnotationConstant, String>>();
		AnnotatedCorpusReader candidates = new AnnotatedCorpusReader(
				TrainingConfig.getAnnotationFolderForTrainingCandidateOutput(r, baseDir)
				,TrainingConfig.TRAINING_DATA_CONSTANTS);
		
		BufferedReader relationInstanceReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(TEMP_TEST_PATH + "/mention_" + r.name()), "UTF-8"));

		Set<Integer> sentenceIds = new HashSet<Integer>();
		String line = relationInstanceReader.readLine();
		while(line != null){
			//parse relInstanceLine
			String[] tokens = line.split("\\|");
			//first token is relation instance, the rest are entity pair locations
			for(int i=1; i<tokens.length; i++){
				sentenceIds.add(Integer.parseInt(EntityPairLocation.deserialize(tokens[i]).getGlobalSentenceId()));
			}
			
			line = relationInstanceReader.readLine();
		}
		relationInstanceReader.close();
		
		//go through the candidates, and pick out annotations
		while(candidates.hasNext()){
			Map<AnnotationConstant, String> sentenceAnnot = candidates.next();
			AnnotatedSentenceParserAdapter annotations = new AnnotatedSentenceParserAdapter(sentenceAnnot);
			int sentenceId = Integer.parseInt(annotations.getText().globalSentenceId);
			
			if(sentenceIds.contains(sentenceId)){
				//WARNING: If I don't make this copy, values in hashtable will keep changing value!!
				Map<AnnotationConstant, String> shallowCopy = new HashMap<AnnotationConstant, String>();	
				shallowCopy.putAll(sentenceAnnot);
				annotCache.put(sentenceId, shallowCopy);
			}
		}
		
		candidates.close();
		return annotCache;
	}
	
}

