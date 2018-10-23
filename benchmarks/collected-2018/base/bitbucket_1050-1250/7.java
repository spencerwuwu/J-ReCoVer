// https://searchcode.com/api/result/53697515/

package de.uni_leipzig.asv.inflection.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;

import de.uni_leipzig.asv.inflection.core.LanguageTagging;
import de.uni_leipzig.asv.inflection.core.LexiconParser;
import de.uni_leipzig.asv.inflection.core.WordLabeling;
import de.uni_leipzig.asv.inflection.core.PatchInterface;
import de.uni_leipzig.asv.inflection.core.PatchQueueInterface;
import de.uni_leipzig.asv.inflection.core.Word;
import de.uni_leipzig.asv.inflection.core.WordInterface;
import de.uni_leipzig.asv.inflection.core.WordLabelingInterface;
import de.uni_leipzig.asv.inflection.core.WordTaggingInterface;
import de.uni_leipzig.asv.inflection.core.XMLSimpleErrorHandler;
import de.uni_leipzig.asv.inflection.de.RightToLeftPositionLabeling;
import de.uni_leipzig.asv.inflection.evaluation.Evaluation;
import de.uni_leipzig.asv.splitting.BaseformReducer;

import org.apache.commons.lang.StringUtils;

/**
 * This is the main application for inflecting words. To see the options run Flexi without any:
 * 
 * Java -jar Flexi.jar
 * 
 * @author Julian Moritz, email [at] julianmoritz [dot] de
 * 
 */
public class Flexi {

	private static Logger logging = Logger.getLogger(Flexi.class);
	private List<WordTaggingInterface> taggingList = new LinkedList<WordTaggingInterface>();
	private List<WordLabelingInterface> labelingList = new LinkedList<WordLabelingInterface>();
	private String lexiconpath;
	private String schemapath;
	private Classifier classifier;
	private Map<String, PatchQueueInterface> patchqueues;
	private Instances instances;
	private FastVector attributes;
	private boolean decisionTree = false;
	private double reduceFactor = -1;
	private double bias = 1.;
	private Map<String, String> lexiconmap = new HashMap<String, String>();
	private boolean lexiconLookup = false;
	private boolean reduceWordToBaseform = false;
	private BaseformReducer baseformreducer;

	/**
	 * Main method to run Flexi. If you want to know wich options can be used, run Flexi without any options:
	 * 
	 * java -jar Flexi.jar
	 * 
	 * @param args Options for Flexi.
	 */
	public static void main(String[] args) {

		Flexi appl = new Flexi();

		OptionParser parser = new OptionParser();
		parser.accepts("config").withRequiredArg().ofType(String.class);
		parser.accepts("decision-tree").withOptionalArg();
		parser.accepts("reduce").withRequiredArg().ofType(Double.class);
		parser.accepts("word").withRequiredArg().ofType(String.class);
		parser.accepts("lookup").withOptionalArg();
		parser.accepts("help").withOptionalArg();
		parser.accepts("input").withRequiredArg().ofType(String.class);
		parser.accepts("output").withRequiredArg().ofType(String.class);
		parser.accepts("baseforms").withOptionalArg();
		parser.accepts("verbose").withOptionalArg();
		parser.accepts("examine").withOptionalArg();

		OptionSet options = parser.parse(args);

		String config;
		boolean verbose = false;

		if (args.length == 0 || options.has("help")) {

			System.out.println("The following options are available:");
			System.out.println("-h, --help: prints this help.");
			System.out
					.println("-c, --config <path>: use file <path> as config-file (default: ./conf/flexi_config.ini).");
			System.out
					.println("-d, --decision-tree: enables use of decision tree to classify. If not given, a naive bayes classifier is used.");
			System.out
					.println("-r, --reduce <number>: Proportion the trainings instances should be reduced to. Argument must be between 0.0 and 1.0. 0.2 causes a reduce to 20% of original size.");
			System.out
					.println("-l, --lookup: if given, before guessing how a word should be inflected, a lookup in the lexicon is made.");
			System.out
					.println("-w, --word <word>: inflects the <word>. If <word> is not a baseform, use option --baseform to let Flexi reduce it.");
			System.out
					.println("-i, --input <file>: use file <path> as a list of baseforms wich should be inflected.");
			System.out
					.println("-o, --output <file>: use file <path> for storing the output of inflecting the baseforms given as input. If this option is missing, the output is printed to stdout.");
			System.out
					.println("-b, --baseforms: try to reduce a word to its baseform before inflecting it.");
			System.out
					.println("-e, --examine: instead of simply inflecting a word, the given word is reduced to its baseform, the baseform is inflected, and if the originally given wordform is in the set of inflected forms, it is marked with a '1' in the output, elsewhise with a '0'. Please use option --input; option --word does not work here.");
			System.out.println("Recommended options for JVM:");
			System.out.println("-Xmx2048m -Xms1024m");

		}

		if (options.has("verbose")) {

			verbose = true;

		}

		if (verbose == true) {

			System.out.println("Welcome to the inflection tool Flexi.");
			System.out
					.println("Author is: Julian Moritz, email [at] julianmoritz [dot] de");
			System.out.println("Looking for a config-file.");

		}

		if ((options.has("help") && args.length == 1) || args.length == 0) {

			System.exit(0);

		}

		if (options.has("config") == true) {

			config = (String) options.valueOf("config");
			if (verbose == true) {
				System.out.println("Config file set to: " + config);
			}

		} else {

			config = "./conf/flexi_config.ini";
			if (verbose == true) {
				System.out
						.println("No paramter 'config' was given, using default:"
								+ config);
			}

		}

		if (options.has("decision-tree") == true) {

			appl.setDecisionTree(true);

		} else {

			appl.setDecisionTree(false);

		}

		if (options.has("reduce") == true) {

			appl.setReduceFactor(Double.valueOf((Double) options
					.valueOf("reduce")));

		}

		if (options.has("lookup") == true) {

			appl.setLexiconLookup(true);

		} else {

			appl.setLexiconLookup(false);

		}

		if (options.has("baseforms") == true) {

			appl.setReduceWord(true);

		} else {

			appl.setReduceWord(false);

		}

		try {
			appl.init(config);
		} catch (Exception e) {

			e.printStackTrace();
			if (verbose == true) {
				System.out.println("An exception ocurred while initialization");
				System.out
						.println("Please send the logfile to the developer's email-address.");
				System.out.println("Aborting now.");

			}
			System.exit(1);
		}

		if (options.has("examine") == true) {

			Map<String, String> revmap = new HashMap<String, String>();
			if (options.has("input") && options.has("output")) {

				try {
					appl.examineWordlistAndStore((String) options
							.valueOf("input"), (String) options
							.valueOf("output"));
				} catch (IOException e) {
					if (verbose == true) {

						System.out
								.println("An error ocurred when trying to access input or output file. Is the output file's location writeable?");
						System.out
								.println("Please send the logfile to the developer's email-address.");
						System.out.println("Aborting now.");

					}
					System.exit(1);
				}

			} else if (options.has("input")) {

				try {
					appl.examineWordlistAndPrint((String) options
							.valueOf("input"));
				} catch (IOException e) {
					if (verbose == true) {

						System.out
								.println("An error ocurred when trying to access input file.");
						System.out
								.println("Please send the logfile to the developer's email-address.");
						System.out.println("Aborting now.");

					}
					System.exit(1);
				}

			} else {

				if (verbose == true) {

					System.out
							.println("Please use option --input to specify an input file.");

				}
				System.exit(1);

			}

			System.exit(0);

		}

		if (options.has("input") == true) {

			try {

				if (options.has("output") == true) {

					appl.inflectWordlistAndStore((String) options
							.valueOf("input"), (String) options
							.valueOf("output"));

				} else {

					appl.inflectWordlistAndPrint((String) options
							.valueOf("input"));

				}
			} catch (Exception e) {
				if (verbose == true) {
					System.out
							.println("An error ocurred when trying to access input or output file.");
					System.out
							.println("If you cannot solve the problem on your own, send the logfile to the developer's email-address.");
					System.out.println("Aborting now.");
					System.exit(1);
				}
			}

		}

		if (options.has("word") == true) {
			
			WordInterface word = appl.inflectWord((String) options.valueOf("word"));
			
			if (options.has("output") == true) {
				
				try {
					
					appl.storeWord(word, (String) options.valueOf("output"));
					
				} catch (IOException e) {
					System.out
							.println("An error ocurred when trying to access output file. Is the output file's location writeable?");
					System.out
							.println("Please send the logfile to the developer's email-address.");
					System.out.println("Aborting now.");
					System.exit(1);
				}

			} else {

				appl.printWord(word);

			}

		}

	}

	/**
	 * Examines wordlist. Each word in the wordlist is reduced to its baseform and then
	 * inflected. If the word is in the set of inflected forms, it is stored with a "1", if not, it is stored with a "0" in the given CSV file.
	 * @param pathToWordlistFile File which contains the words.
	 * @param pathToCSVFile File the result should be written to.
	 * @throws IOException This exception is thrown if any errors occurr concerning the input or the output file.
	 */
	public void examineWordlistAndStore(String pathToWordlistFile,
			String pathToCSVFile) throws IOException {

		logging.info("Examining: " + pathToWordlistFile);

		Set<String> words = new HashSet<String>();

		FileReader freader;
		try {
			freader = new FileReader(new File(pathToWordlistFile));
		} catch (FileNotFoundException e) {
			logging.error(e);
			throw e;
		}
		BufferedReader breader = new BufferedReader(freader);

		String line;

		try {
			while ((line = breader.readLine()) != null) {

				words.add(line.trim());

			}
		} catch (IOException e) {
			logging.error(e);
			throw e;
		}

		File csvfile = new File(pathToCSVFile);
		try {
			csvfile.createNewFile();
		} catch (IOException e) {
			logging.error(e);
			throw e;
		}
		FileWriter fwriter;
		try {
			fwriter = new FileWriter(csvfile);
		} catch (IOException e) {
			logging.error(e);
			throw e;
		}

		BufferedWriter bwriter = new BufferedWriter(fwriter);

		WordInterface iword;

		for (String word : words) {

			iword = this.inflectWord(this.baseformreducer.reduceWord(word));

			if (iword.getInflectedWordforms().keySet().contains(word)) {

				bwriter.write(word + "\t1"
						+ System.getProperty("line.separator"));

			} else {

				// store
				bwriter.write(word + "\t0"
						+ System.getProperty("line.separator"));

			}

		}

		bwriter.flush();
		bwriter.close();

	}

	/**
	 * Examines wordlist. Each word in the wordlist is reduced to its baseform and then
	 * inflected. If the word is in the set of inflected forms, it is stored with a "1", if not, it is stored with a "0" in the returned map.
	 * @param pathToWordlistFile File the result should be written to.
	 * @throws IOException This exception is thrown if any errors occurr concerning the input file.
	 * @return Map with words mapping to "1" (if word is in the set of inflected forms) or "0" (if word is not in the set of inflected forms). 
	 */
	public Map<String, String> examineWordlist(String pathToWordlistFile)
			throws IOException {

		logging.info("Examining: " + pathToWordlistFile);
		Map<String, String> retmap = new HashMap<String, String>();
		Set<String> words = new HashSet<String>();

		FileReader freader;
		try {
			freader = new FileReader(new File(pathToWordlistFile));
		} catch (FileNotFoundException e) {
			logging.error(e);
			throw e;
		}
		BufferedReader breader = new BufferedReader(freader);

		String line;

		try {
			while ((line = breader.readLine()) != null) {

				words.add(line.trim());

			}
		} catch (IOException e) {
			logging.error(e);
			throw e;
		}

		WordInterface iword;

		for (String word : words) {

			iword = this.inflectWord(this.baseformreducer.reduceWord(word));

			if (iword.getInflectedWordforms().keySet().contains(word)) {

				retmap.put(word, "1");

			} else {

				retmap.put(word, "0");

			}

		}

		return retmap;

	}

	/**
	 * Examines wordlist. Each word in the wordlist is reduced to its baseform and then
	 * inflected. If the word is in the set of inflected forms, it is printed with a "1", if not, it is printed with a "0".
	 * @param pathToWordlistFile File the result should be written to.
	 * @throws IOException IOException This exception is thrown if any errors occurr concerning the input file.
	 */
	public void examineWordlistAndPrint(String pathToWordlistFile)
			throws IOException {

		logging.info("Examining: " + pathToWordlistFile);
		Set<String> words = new HashSet<String>();

		FileReader freader;
		try {
			freader = new FileReader(new File(pathToWordlistFile));
		} catch (FileNotFoundException e) {
			logging.error(e);
			throw e;
		}
		BufferedReader breader = new BufferedReader(freader);

		String line;

		try {
			while ((line = breader.readLine()) != null) {

				words.add(line.trim());

			}
		} catch (IOException e) {
			logging.error(e);
			throw e;
		}

		WordInterface iword;

		for (String word : words) {

			iword = this.inflectWord(this.baseformreducer.reduceWord(word));

			if (iword.getInflectedWordforms().keySet().contains(word)) {

				System.out.println(word + "\t1");

			} else {

				System.out.println(word + "\t0");

			}

		}

	}

	/**
	 * Prints word to stdout. Baseform, inflected form and morphosyntactic feature are separated by tab.
	 * @param word Word to be printed. 
	 */
	public void printWord(WordInterface word) {

		for (String wordform : word.getInflectedWordforms().keySet()) {

			System.out.print(wordform + ": ");

			Iterator it = word.getInflectedWordforms().get(wordform).iterator();

			while (it.hasNext()) {

				System.out.print(it.next());
				if (it.hasNext()) {

					System.out.print(", ");

				}

			}
			System.out.println();
		}

	}

	/**
	 * Stores word to file. Baseform, inflected form and morphosyntactic feature are separated by tab.
	 * @param word Word to be stored.
	 * @param pathToCSVFile Path to CSV file the word should be stored into.
	 * @throws IOException Exception is thrown if writing to the file does not work properly.
	 */
	public void storeWord(WordInterface word, String pathToCSVFile)
			throws IOException {

		File csvfile = new File(pathToCSVFile);
		try {
			csvfile.createNewFile();
		} catch (IOException e) {
			logging.error(e);
			throw e;
		}
		FileWriter fwriter;
		try {
			fwriter = new FileWriter(csvfile);
		} catch (IOException e) {
			logging.error(e);
			throw e;
		}

		BufferedWriter bwriter = new BufferedWriter(fwriter);

		this.storeWord(word, bwriter);

	}

	
	private void storeWord(WordInterface word, BufferedWriter writer)
			throws IOException {

		for (String inflform : word.getInflectedWordforms().keySet()) {

			try {
				for (String morphofeature : word.getInflectedWordforms().get(
						inflform)) {
					writer.write(word.toString());
					writer.write("\t");
					writer.write(inflform);
					writer.write("\t");
					writer.write(morphofeature);
					writer.write(System.getProperty("line.separator"));
				}
			} catch (IOException e) {

				logging.error(e);
				throw e;
			}

		}

	}

	/**
	 * Inflects the words in the input file and stores the inflected forms to the output file.
	 * @param pathToWordlistFile Path to a file wich contains words to be inflected.
	 * @param pathToCSVFile Path to CSV file the result should be written to. Baseform, inflected form and morphosyntactic feature are separated by tab.
	 * @throws IOException If an operation on the files does not work properly, this exception is thrown.
	 */
	public void inflectWordlistAndStore(String pathToWordlistFile, String pathToCSVFile)
			throws IOException {

		logging.info("Inflecting: " + pathToWordlistFile);

		FileReader freader;
		try {
			freader = new FileReader(new File(pathToWordlistFile));
		} catch (FileNotFoundException e) {
			logging.error(e);
			throw e;
		}
		BufferedReader breader = new BufferedReader(freader);

		
		File csvfile = new File(pathToCSVFile);
		try {
			csvfile.createNewFile();
		} catch (IOException e) {
			logging.error(e);
			throw e;
		}
		FileWriter fwriter;
		try {
			fwriter = new FileWriter(csvfile);
		} catch (IOException e) {
			logging.error(e);
			throw e;
		}

		BufferedWriter bwriter = new BufferedWriter(fwriter);
		
		String line;

		try {
			while ((line = breader.readLine()) != null) {

				this.storeWord(this.inflectWord(line.trim()), bwriter);

			}
		} catch (IOException e) {
			logging.error(e);
			throw e;
		}
		
		bwriter.flush();
		bwriter.close();


	}

	/**
	 * Inflects the word in the wordlist and prints the result to stdout. Baseform, inflected form and morphosyntactic feature are separated by tab.
	 * @param pathToWordlistFile Path to file wich contains words to be inflected.
	 * @throws IOException If there ocurr any problems with the input file, this exception is thrown.
	 */
	public void inflectWordlistAndPrint(String pathToWordlistFile)
			throws IOException {

		logging.info("Inflecting: " + pathToWordlistFile);

		FileReader freader;
		try {
			freader = new FileReader(new File(pathToWordlistFile));
		} catch (FileNotFoundException e) {
			logging.error(e);
			throw e;
		}
		BufferedReader breader = new BufferedReader(freader);

		String line;

		try {
			while ((line = breader.readLine()) != null) {

				this.printWord(this.inflectWord(line.trim()));

			}
		} catch (IOException e) {
			logging.error(e);
			throw e;
		}

	}

	/**
	 * Initializes Flexi. For proper config files look into the conf directory and read the manual (./manual/manual.html)
	 * @param pathToIniFile Path to  proper config file.
	 * @throws Exception In any problems occurr this exception is thrown. If so please look into the logfile (default: logs/inflection.log).
	 */
	public void init(String pathToIniFile) throws Exception {

		Ini ini = new Ini();
		ini.load(new File(pathToIniFile));

		Ini.Section mainSec = ini.get("main");

		String pathToLoggingConfig = mainSec.get("logging_config_path");
		PropertyConfigurator.configure(pathToLoggingConfig);

		logging.info("Logging initialized");

		this.lexiconpath = mainSec.get("lexicon_path");
		this.schemapath = mainSec.get("lexicon_schema_url");

		if (this.lexiconpath == null) {

			throw new NullPointerException("Source file not given.");

		}

		String[] labeling = mainSec.getAll("wordlabelingclass", String[].class);
		String[] tagging = mainSec.getAll("wordtaggingclass", String[].class);

		Object[] paramargs = new Object[1];
		paramargs[0] = ini;

		Class[] paramtypes = new Class[1];
		paramtypes[0] = Ini.class;

		for (String label : labeling) {

			this.logging.info("Initialize: " + label);
			try {
				Class<WordLabelingInterface> c = (Class<WordLabelingInterface>) Class
						.forName(label);

				Method m;
				try {
					m = c.getDeclaredMethod("createFromConfig", paramtypes);

				} catch (SecurityException exc) {
					this.logging.error("Couldn't create: " + label, exc);
					continue;
				} catch (NoSuchMethodException exc) {
					this.logging.error("Couldn't create: " + label, exc);
					continue;
				}

				try {
					WordLabelingInterface wordlabeling = (WordLabelingInterface) m
							.invoke(null, paramargs);
					this.labelingList.add(wordlabeling);

				} catch (IllegalArgumentException exc) {
					this.logging.error("Couldn't create: " + label, exc);
					continue;
				} catch (IllegalAccessException exc) {

					this.logging.error("Couldn't create: " + label, exc);
					continue;
				} catch (InvocationTargetException exc) {
					this.logging.error("Couldn't create: " + label, exc);
					continue;
				}

			} catch (ClassNotFoundException exc) {
				this.logging.error("Couldn't find: " + label, exc);
			} catch (Exception exc) {
				this.logging.error("Couldn't create: " + label, exc);
			}

		}

		Set<String> tagtypes = new HashSet<String>();

		for (String tag : tagging) {

			this.logging.info("Initialize: " + tag);
			try {
				Class<WordTaggingInterface> c = (Class<WordTaggingInterface>) Class
						.forName(tag);

				Method m;
				try {
					m = c.getDeclaredMethod("createFromConfig", paramtypes);

				} catch (SecurityException exc) {
					this.logging.error("Couldn't create: " + tag, exc);
					continue;
				} catch (NoSuchMethodException exc) {
					this.logging.error("Couldn't create: " + tag, exc);
					continue;
				}

				try {
					WordTaggingInterface wordtagging = (WordTaggingInterface) m
							.invoke(null, paramargs);
					this.taggingList.add(wordtagging);
					tagtypes.add(wordtagging.getType());

				} catch (IllegalArgumentException exc) {
					this.logging.error("Couldn't create: " + tag, exc);
					continue;
				} catch (IllegalAccessException exc) {

					this.logging.error("Couldn't create: " + tag, exc);
					continue;
				} catch (InvocationTargetException exc) {
					this.logging.error("Couldn't create: " + tag, exc);
					continue;
				}

			} catch (ClassNotFoundException exc) {
				this.logging.error("Couldn't find: " + tag, exc);
			} catch (Exception exc) {
				this.logging.error("Couldn't create: " + tag, exc);

			}

		}

		Ini.Section sec = ini.get("tagging");
		String redtree = sec.get("reduction_tree");

		this.baseformreducer = new BaseformReducer(redtree);

		logging.info("Start reading lexicon file: " + this.lexiconpath);

		SAXParserFactory saxfactory = SAXParserFactory.newInstance();

		SAXParser saxParser = saxfactory.newSAXParser();
		LexiconParser lexparser = new LexiconParser(tagtypes);

		lexparser.setReduceFactor(this.reduceFactor);
		lexparser.setDecisionTree(this.decisionTree);

		saxParser.parse(new File(this.lexiconpath), lexparser);

		this.classifier = lexparser.getClassifier();
		this.patchqueues = lexparser.getPatchQueues();
		this.instances = lexparser.getInstances();
		this.lexiconmap = lexparser.getLexiconMap();

		this.attributes = new FastVector(this.instances.numAttributes());

		for (int i = 0; i < this.instances.numAttributes(); i++) {

			this.attributes.addElement(this.instances.attribute(i));

		}

		logging.info("Initialization finished");

	}

	private WordInterface processWord(String word) {

		WordInterface retword = new Word(word);

		return this.processWord(retword);

	}

	/**
	 * Inflects a collection of words.
	 * @param words All words in the list are inflected.
	 * @return Returns a collection containing the inflected words.
	 */
	public Collection<WordInterface> inflectWords(Collection<String> words) {

		List<WordInterface> retlist = new LinkedList<WordInterface>();

		for (String word : words) {

			retlist.add(this.inflectWord(word));

		}

		return (Collection<WordInterface>) retlist;

	}

	/**
	 * Inflects a word.
	 * @param word Word to be inflected.
	 * @return Returns the inflected word.
	 */
	public WordInterface inflectWord(String word) {

		String hashcode;
		WordInterface pword;

		if (this.reduceWordToBaseform == true) {

			pword = this.processWord(this.baseformreducer.reduceWord(word
					.toString()));

		} else {

			pword = this.processWord(word);

		}
		if (this.lexiconLookup == true
				&& this.lexiconmap.containsKey(word.toString())) {

			hashcode = this.lexiconmap.get(word.toString());

		} else {

			Instances trainingsdata = new Instances("tobeclassified",
					this.attributes, 1);

			trainingsdata.setClass(this.instances.classAttribute());

			Instance instance = new Instance(this.instances.numAttributes());
			instance.setDataset(trainingsdata);
			String value;
			Attribute attribute;

			for (String type : pword.getTags().keySet()) {

				attribute = trainingsdata.attribute(type);
				value = pword.getTags().get(type);
				try {
					if (attribute.indexOfValue(value) >= 0) {
						instance.setValue(attribute, value);
					}
				} catch (NullPointerException e) {
					// void
				}
			}

			trainingsdata.add(instance);

			double predicted;
			try {
				predicted = this.classifier.classifyInstance(instance);
			} catch (Exception e) {
				logging.warn("Could not classify word: " + pword.toString());
				logging.error(e);
				return pword;
			}

			hashcode = this.instances.classAttribute().value((int) predicted);
		}

		PatchQueueInterface patchqueue = this.patchqueues.get(hashcode);

		return patchqueue.applyOnWordInterface(pword);

	}

	private WordInterface processWord(WordInterface word) {

		for (WordTaggingInterface tagging : this.taggingList) {

			word = tagging.tagWord(word);

		}

		for (WordLabelingInterface labeling : this.labelingList) {

			word = labeling.labelWord(word);

		}
		return word;

	}

	private boolean validateXML() throws SAXException, IOException,
			ParserConfigurationException {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);

		SchemaFactory schemaFactory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		factory.setSchema(schemaFactory
				.newSchema(new Source[] { new StreamSource(this.schemapath) }));

		SAXParser parser = factory.newSAXParser();

		XMLReader reader = parser.getXMLReader();
		XMLSimpleErrorHandler errorhandler = new XMLSimpleErrorHandler();
		reader.setErrorHandler(errorhandler);
		reader.parse(new InputSource(this.lexiconpath));
		if (errorhandler.getErrorCount() > 0) {
			throw new SAXException("XML file is not valid.");
		}
		return true;

	}

	/**
	 * If you want to use a decision tree to classify, use this method.
	 * @param decisionTree If and only if set to true, a decision tree ist used. 
	 */
	public void setDecisionTree(boolean decisionTree) {
		this.decisionTree = decisionTree;
	}

	/**
	 * If you want to use a Naive Bayes Classifier, use this method.
	 * @param naiveBayes If and only if set to true, a naive bayes is used.
	 */
	public void setNaiveBayes(boolean naiveBayes) {
		this.setDecisionTree(!naiveBayes);
	}

	/**
	 * Check wheter a naive bayes classifier is used.
	 * @return Returns true if and only if a naive bayes is used.
	 */
	public boolean isNaiveBayes() {
		return !this.isDecisionTree();
	}

	/**
	 * Check wheter a decision tree is used.
	 * @return Returns true if and only if a decision tree is used.
	 */
	public boolean isDecisionTree() {
		return decisionTree;
	}

	/**
	 * Sets the reduce factor.
	 * @param reduceFactor If set to 0.15, the training data is reduced to 15 %.
	 */
	public void setReduceFactor(double reduceFactor) {
		this.reduceFactor = reduceFactor;
	}

	/**
	 * Returns the reduce factor.
	 * @return Returns the used reduce factor.
	 */
	public double getReduceFactor() {
		return reduceFactor;
	}

	/**
	 * If a word is in the lexicon, a look up can be made.
	 * @param lexiconLookup Set to true if and only if a lookup should be made.
	 */
	public void setLexiconLookup(boolean lexiconLookup) {
		this.lexiconLookup = lexiconLookup;
	}

	/**
	 * Returns the lookup state.
	 * @return Returns true if and only if a lookup in the lexicon is made.
	 */
	public boolean isLexiconLookup() {
		return lexiconLookup;
	}

	/**
	 * If a word from the input should be reduced to its baseform, use this method.
	 * @param reduceWordToBaseform Set to true if and only if a given word should be reduced to its baseform.
	 */
	public void setReduceWord(boolean reduceWordToBaseform) {
		this.reduceWordToBaseform = reduceWordToBaseform;
	}

	/**
	 * Returns the reduce state.
	 * @return Returns true if and only if a given word should be reduced to its baseform.
	 */
	public boolean isReduceWord() {
		return reduceWordToBaseform;
	}

}

