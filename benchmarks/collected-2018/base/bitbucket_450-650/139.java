// https://searchcode.com/api/result/53697559/

/**
 * 
 */
package de.uni_leipzig.asv.inflection.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesSimple;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.Id3;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.instance.RemoveMisclassified;

/**
 * Parser to parse a lexicon. Use by the main application.
 * 
 * @author Julian Moritz, email [at] julianmoritz [dot] de
 * @see de.uni_leipzig.asv.inflection.main.Flexi
 */
public class LexiconParser extends DefaultHandler {

	private Set<String> types;
	private StringBuffer charbuff = new StringBuffer();
	private WordInterface currword;
	private String currwordform;
	private Set<String> currfeatureset = new HashSet<String>();
	private String currtype;
	private String currtag;
	private int currposition;
	private String currlabel;
	private Map<String, PatchQueueInterface> patchmap = new HashMap<String, PatchQueueInterface>();
	private Instances trainingsdata;
	private static Logger logging = Logger.getLogger(LexiconParser.class);
	private Classifier classifier;
	private Attribute classattribute;
	private String gh;
	private double reduceFactor = -1.;
	private boolean decisionTree = false;
	private FastVector attributes;
	private Map<String, String> lexiconmap = new HashMap<String, String>();
	private Map<String, Integer> patchqueuecounting = new HashMap<String, Integer>();

	public static void main(String[] args) {

		SAXParserFactory saxfactory = SAXParserFactory.newInstance();

		SAXParser saxParser;
		try {
			saxParser = saxfactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			logging.error(e);
			return;
		} catch (SAXException e) {
			logging.error(e);
			return;
		}

		Set<String> typeset = new HashSet<String>();
		typeset.add("de_pos");
		typeset.add("de_word");
		typeset.add("de_gender");
		typeset.add("de_being");
		typeset.add("de_end_of_word");
		typeset.add("de_hyphen_count");
		typeset.add("language");
		typeset.add("de_root");
		typeset.add("de_last_hyphen");
		typeset.add("de_stem_vowel");
		typeset.add("de_prefix");

		LexiconParser lexParser = new LexiconParser(typeset);

		try {
			saxParser.parse(new File("./resources/de_lexicon.xml"), lexParser);
		} catch (SAXException e) {
			
			logging.error(e);
			return;
		} catch (IOException e) {
			
			logging.error(e);
			return;
		}

		Classifier cl = lexParser.getClassifier();

	}

	/**
	 * Creates a new LexiconParser. This is a subclass of a DefaultHandler
	 * reading xml files event based.
	 * 
	 * @param allowedTypes
	 *            Specifies the allowed types of tags. E.g. you want to the tag
	 *            "de_stem_vowel" to be read, the set must contain it as a
	 *            string.
	 * @see de.uni_leipzig.asv.inflection.main.Flexi
	 */
	public LexiconParser(Set<String> allowedTypes) {

		Enumeration<Appender> appenders = Logger.getRootLogger()
				.getAllAppenders();
		int appendercount = 0;

		while (appenders.hasMoreElements()) {

			appendercount += 1;
			appenders.nextElement();

		}

		if (appendercount == 0) {

			BasicConfigurator.configure();
			Logger.getRootLogger().setLevel(Level.INFO);
		}

		this.currword = new Word("");

		if (allowedTypes != null) {

			this.types = allowedTypes;
		} else {

			this.types = new HashSet<String>();

		}

		this.attributes = new FastVector(this.types.size() + 1);

		Attribute attribute;

		for (String type : this.types) {

			attribute = new Attribute(type, (FastVector) null);
			// attribute.setWeight(allowedTypes.get(type));
			attributes.addElement(attribute);

		}

		attribute = new Attribute("class", (FastVector) null);
		this.attributes.addElement(attribute);

		this.trainingsdata = new Instances("lexicon", this.attributes, 1000);

		this.trainingsdata.setClass(attribute);

	}

	/**
	 * Method to specifiy the factor the trainingsdata should be reduced.
	 * 
	 * @param factor
	 *            If this parameter is 0.1, the trainingsdata will be reduced to
	 *            10 %, so it must be between 0.0 and 1.0. The distribution of
	 *            the classes will be kept.
	 */
	public void setReduceFactor(double factor) {

		if (factor >= 0. && factor <= 1.) {
			this.reduceFactor = factor;
			return;
		}

	}

	/**
	 * Method to retriev the reduce factor.
	 * 
	 * @return Returns the reduce factor.
	 * @see de.uni_leipzig.asv.inflection.core.LexiconParser#setReduceFactor(double)
	 */
	public double getReduceFactor() {

		return this.reduceFactor;

	}

	/**
	 * (Non-Javadoc) @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	public void startDocument() {

		// init

	}

	private void reduceTrainingsData() {

		if (this.reduceFactor < 0. || this.reduceFactor > 1.) {

			return;

		}

		Resample resamplefilter = new Resample();
		logging.info("reducing: " + 100. * this.reduceFactor);
		resamplefilter.setSampleSizePercent(100. * this.reduceFactor);

		try {
			resamplefilter.setInputFormat(this.trainingsdata);
		} catch (Exception e1) {
			logging.error(
					"Error when setting input format for resample filter", e1);
		}
		try {
			this.trainingsdata = Filter.useFilter(this.trainingsdata,
					resamplefilter);
		} catch (Exception e1) {
			logging
					.error("Error when applying resample filter to trainingsdata");
		}

		logging.info("Number of reduced instances: "
				+ this.trainingsdata.numInstances());

	}

	private void calculateID3() {

		Id3 id3 = new Id3();

		logging.info("Building decision tree classifier with "
				+ this.trainingsdata.numInstances() + " instances");
		logging.info("Class attribute contains: "
				+ this.trainingsdata.classAttribute().numValues() + " values");
		try {
			id3.buildClassifier(this.trainingsdata);
		} catch (Exception e) {
			this.logging.error("Error when training classifier.", e);
		}
		logging.info("Decision-tree was successfully built.");
		this.classifier = id3;

	}

	private void calculateC45() {

		J48 j48 = new J48();
		j48.setUnpruned(true);
		j48.setMinNumObj(1);

		logging.info("Building decision tree classifier with "
				+ this.trainingsdata.numInstances() + " instances");
		logging.info("Class attribute contains: "
				+ this.trainingsdata.classAttribute().numValues() + " values");
		try {
			j48.buildClassifier(this.trainingsdata);
		} catch (Exception e) {
			this.logging.error("Error when training classifier.", e);
		}
		logging.info("Decision-tree was successfully built.");
		this.classifier = j48;

	}

	public void calculateDecisionTree() {

		// choose this method call for C45:
		// this.calculateC45();
		// choose this method call for ID3:
		this.calculateID3();

	}

	public void calculateNaiveBayes() {

		NaiveBayesUpdateable bayes = new NaiveBayesUpdateable();
		logging.info("Building bayes classifier with "
				+ this.trainingsdata.numInstances() + " instances");
		logging.info("Class attribute contains: "
				+ this.trainingsdata.classAttribute().numValues() + " values");

		Instances someinstances = new Instances("some", this.attributes, 1);
		someinstances.setClass(this.trainingsdata.classAttribute());

		for (int i = 0; i < 1; i++) {

			someinstances.add(this.trainingsdata.instance(i));

		}

		someinstances = this.nominalize(someinstances);

		try {
			bayes.buildClassifier(someinstances);
		} catch (Exception e1) {
			logging.warn("Could not build Naive Bayes Classifier");
			logging.error(e1);
			return;
		}

		for (int i = 0; i < this.trainingsdata.numInstances(); i++) {

			try {
				bayes.updateClassifier(this.trainingsdata.instance(i));
			} catch (Exception e) {
				this.logging.warn("Could not update Naive Bayes Classifier");
				this.logging.info(this.trainingsdata.instance(i));
				this.logging.error(e);
				break;
			}

		}

		logging.info("Bayes classifier was successfully built.");
		this.classifier = bayes;

	}

	private Instances nominalize(Instances instances) {

		StringToNominal strtonomfilter = new StringToNominal();
		strtonomfilter.setAttributeRange("1-" + (this.types.size() + 1));
		try {
			strtonomfilter.setInputFormat(this.trainingsdata);
		} catch (Exception e2) {
			logging
					.error(
							"Could not set input format for string to nominal filter. Aborting.",
							e2);
			return null;
		}
		try {
			return Filter.useFilter(instances, strtonomfilter);
		} catch (Exception e2) {
			logging
					.error(
							"Could not convert string attributes to nominal ones, throwing exception. Aborting.",
							e2);
			return null;
		}

	}

	/**
	 * (Non-Javadoc) @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	@Override
	public void endDocument() {

		// creating a filter for converting string attributes to nominal ones
		this.trainingsdata = this.nominalize(this.trainingsdata);

		this.reduceTrainingsData();

		if (this.isDecisionTree()) {

			this.calculateDecisionTree();

		} else {

			this.calculateNaiveBayes();

		}

		this.classattribute = this.trainingsdata.classAttribute();

	}

	/**
	 * Returns the learned trainings instances.
	 * @return The instances wich are stored in the lexicon wich was parsed by this lexicon parser.
	 */
	public Instances getInstances() {

		return this.trainingsdata;

	}

	/**
	 * Returns the classifier.
	 * @return Returns a classifier, e.g. a DecisionTree or a Naive Bayes Classifier.
	 */
	public Classifier getClassifier() {

		return this.classifier;

	}

	/**
	 * Returns a map with ids (as strings) pointing on patchqueues.
	 * @return If you want to make a lookup up in the lexicon for a word,
	 * first you have to retrieve the key with {@link de.uni_leipzig.asv.inflection.core.LexiconParser#getLexiconMap()}
	 * and then you are able to retrieve the matching patchqueue.
	 * @return Map to retrieve patchqueues. 
	 */
	public Map<String, PatchQueueInterface> getPatchQueues() {

		return patchmap;

	}

	/**
	 * (Non-Javadoc) @see org.xml.sax.helpers.DefaultHandler#startElement(String, String, String, Attributes)
	 */
	@Override
	public void startElement(String uri, String localname, String qname,
			Attributes attributes) {


	}

	/**
	 * (Non-Javadoc) @see org.xml.sax.helpers.DefaultHandler#endElement(String, String, String)
	 */
	@Override
	public void endElement(String uri, String localname, String qname) {

		if (qname.equals("word")) {

			Instance instance = new Instance(this.types.size() + 1);
			instance.setDataset(this.trainingsdata);
			Attribute attribute;
			String value;

			for (String type : this.currword.getTags().keySet()) {

				attribute = this.trainingsdata.attribute(type);
				value = this.currword.getTags().get(type);
				instance.setValue(attribute, attribute.addStringValue(value));

			}

			PatchQueueInterface patchqueue = PatchQueue
					.createFromWordInterface(this.currword);

			String hashcode = String.valueOf(patchqueue.hashCode());

			this.lexiconmap.put(this.currword.toString(), hashcode);

			this.patchmap.put(hashcode, patchqueue);

			Attribute classattr = this.trainingsdata.attribute("class");
			instance.setValue(classattr, classattr.addStringValue(hashcode));

			this.trainingsdata.add(instance);

			this.currword.clear("");
			this.currlabel = "";
			this.currposition = -1;
			this.currtype = "";
			this.currwordform = "";
			this.currfeatureset.clear();

		} else if (qname.equals("basicform")) {

			this.currword.clear(this.charbuff.toString().trim());

		} else if (qname.equals("wordform")) {

			this.currwordform = this.charbuff.toString().trim();

		} else if (qname.equals("morphofeature")) {

			this.currfeatureset.add(this.charbuff.toString().trim());

		} else if (qname.equals("inflectedform")) {

			this.currword.addInflectedWordform(this.currwordform,
					this.currfeatureset);
			this.currfeatureset.clear();

		} else if (qname.equals("type")) {

			this.currtype = this.charbuff.toString().trim();

		} else if (qname.equals("value")) {

			this.currtag = this.charbuff.toString().trim();
			this.currlabel = this.charbuff.toString().trim();

		} else if (qname.equals("tag")) {

			if (this.types.contains(this.currtype)) {
				this.currword.addTag(this.currtype, this.currtag);
			}

		} else if (qname.equals("position")) {

			this.currposition = new Integer(this.charbuff.toString().trim())
					.intValue();

		} else if (qname.equals("label")) {

			this.currword.addLabel(this.currposition, this.currlabel);

		}

		this.charbuff.setLength(0);

	}

	/**
	 * (Non-Javadoc) @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] ch, int start, int length) {

		this.charbuff.append(ch, start, length);

	}

	/**
	 * Method to specifiy the usage of a decisiontree.
	 * @param decisionTree True if a decisiontree should be used, false if not.
	 */
	public void setDecisionTree(boolean decisionTree) {
		this.decisionTree = decisionTree;
	}

	/**
	 * Method to check if a decision is used.
	 * @return Returns true if a decisiontree is used, false if not.
	 */
	public boolean isDecisionTree() {
		return decisionTree;
	}
	
	/**
	 * Returns the lexicon map for making lookups.
	 * @return Returns a map with words pointing on strings, wich are
	 * used by {@link de.uni_leipzig.asv.inflection.core.LexiconParser#getPatchQueues()} to 
	 * retrieve the patchqueues.
	 */
	public Map<String, String> getLexiconMap(){
		
		return this.lexiconmap;
		
	}

}

