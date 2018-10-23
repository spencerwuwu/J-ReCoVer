// https://searchcode.com/api/result/7483246/

package eu.iksproject.fise.stores.persistencestore.jena.search;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.math.stat.StatUtils;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.larq.IndexBuilderString;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.query.larq.LARQ;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.ResultBinding;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;

import eu.iksproject.fise.stores.persistencestore.ISearch;
import eu.iksproject.fise.stores.persistencestore.dbPedia.DBPediaClient;
import eu.iksproject.fise.stores.persistencestore.heuristic.SyntacticStemmed;
import eu.iksproject.fise.stores.persistencestore.jena.JenaPersistenceStore;
import eu.iksproject.fise.stores.persistencestore.rest.ResourceManager;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.ClassResource;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.Document;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.FullTextSearchResult;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.FullTextSearchResultList;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.KeywordList;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.OperatorType;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.ResourceList;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.Result;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.ReturnedDocuments;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.ReturnedOntologyResources;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.ReturnedWordnetResources;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.StructuralQueryPart;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.TopRelatedOntologyResources;
import eu.iksproject.fise.stores.persistencestore.rest.model.search.ResourceList.SelectiveResource;
import eu.iksproject.fise.stores.persistencestore.wordnet.WordnetClient;

public class Search implements ISearch {

	private JenaPersistenceStore jenaPersistenceStoreInstance = null;

	private static String CONTENT_INSTANCE_XPATH_PROPERTY_PREFIX = "http://www.srdc.com.tr/iks/jcr2ont#";
	private static String CONTENT_INSTANCE_XPATH_PROPERTY_URI = "path";

	private static String CONTENT_INSTNACE_PRIMARYTYPE_PROPERTY_PREFIX = "http://www.srdc.com.tr/iks/jcr2ont#";
	private static String CONTENT_INSTNACE_PRIMARYTYPE_PROPERTY_URI = "primaryNodeType";

	private static String QUERY_COLUMN_NAME = "resource";

	private static double DEGRADING_COEFFICIENT = 2.0;
	private static double MAX_SCORE = 50;

	private static double CUT_THRESHOLD = 0.65;
	private static double CLASS_RELEVANCE_THRESHOLD = 0.1;

	private static String JCR_NODETYPE_NAMESPACE = "http://www.jcp.org/jcr/";
	private static String JCR_IN_NAMESPACE = "http://internal";

	private static int WORDNET_EXPANSION_LEVEL = 1;

	private static int TOP_RELATED_ONTOLOGY_RESOURCES_COUNT = 5;

	// private List<String> propertiesToSearchFor = null;
	/** propertiesToSearchForURI --> ontologyURI **/
	// private Hashtable<String, String> propertiesToSearchFor_Ontologies =
	// null;

	private List<String> indexedOntologyURIs = null;

	/** should not be accessed from a function other than getIndexedOntModel() **/
	private Hashtable<String, OntModel> internallyIndexedReasonedOntModels = null;
	private Hashtable<String, OntModel> internallyIndexedAssertedOntModels = null;

	private static boolean REASONING_ENABLED = true;

	private static Logger logger = Logger.getLogger(Search.class);

	private Connection connectionInstance = null;

	/** INDEXES **/
	// ontClassURI --> individualCount in Class
	private Hashtable<String, Integer> individualCountPerClass = null;

	/** END-OF INDEXES **/

	private boolean containsStringInIgnoreList(String uri) {
		if (uri.contains(JCR_NODETYPE_NAMESPACE)
				|| uri.contains(JCR_IN_NAMESPACE)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isClassExcludedForCategorization(String classURI) {
		if (classURI
				.equalsIgnoreCase("http://www.srdc.com.tr/news#newsArticleItem")
				|| classURI
						.equalsIgnoreCase("http://www.srdc.com.tr/news#newsArticle")) {
			return true;
		} else {
			return false;
		}
	}

	private void indexOntModels(List<OntModel> ontModels) {
		individualCountPerClass = new Hashtable<String, Integer>();
		Iterator<OntModel> ontModelsItr = ontModels.iterator();
		while (ontModelsItr.hasNext()) {
			OntModel curOntModel = ontModelsItr.next();
			ExtendedIterator ontClassesItr = curOntModel.listClasses();
			while (ontClassesItr.hasNext()) {
				OntClass ontClass = (OntClass) ontClassesItr.next();
				String ontClassURI = ontClass.getURI();
				int instancesCount = 0;
				if (ontClassURI != null) {
					ExtendedIterator instancesItr = ontClass
							.listInstances(false);
					while (instancesItr.hasNext()) {
						instancesItr.next();
						instancesCount++;
					}
					individualCountPerClass.put(ontClassURI, new Integer(
							instancesCount));
				}
			}
		}
	}

	private String getURIForIndexedOntModel(OntModel ontModel) {
		Enumeration<String> ontModelsItr1 = internallyIndexedReasonedOntModels
				.keys();
		while (ontModelsItr1.hasMoreElements()) {
			String key = ontModelsItr1.nextElement();
			if (ontModel.equals(internallyIndexedReasonedOntModels.get(key))) {
				return key;
			}
		}
		Enumeration<String> ontModelsItr2 = internallyIndexedAssertedOntModels
				.keys();
		while (ontModelsItr2.hasMoreElements()) {
			String key = ontModelsItr2.nextElement();
			if (ontModel.equals(internallyIndexedAssertedOntModels.get(key))) {
				return key;
			}
		}
		return null;
	}

	private OntModel getOntModelForOntologyURI(String uri,
			boolean retrieveReasonedModel) {
		if (retrieveReasonedModel) {
			return internallyIndexedReasonedOntModels.get(uri);
		} else {
			return internallyIndexedAssertedOntModels.get(uri);
		}
	}

	private List<OntModel> getIndexedOntModels(boolean retrieveReasonedModel) {
		ResourceManager rm = ResourceManager.getInstance();
		if (connectionInstance == null) {
			// creates a new connection
			try {
				connectionInstance = rm.obtainConnection();
			} catch (Exception e) {
				logger.error("Can not connect to db", e);
			}

			List<OntModel> ontModelsToIndex = new Vector<OntModel>();
			for (int i = 0; i < indexedOntologyURIs.size(); i++) {
				String ontologyURI = indexedOntologyURIs.get(i);
				Writer wr = new StringWriter();
				jenaPersistenceStoreInstance.getPersistenceProvider().getModel(
						ontologyURI).write(wr);
				Model baseModel = ModelFactory
						.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF);
				baseModel.read(new ByteArrayInputStream(wr.toString()
						.getBytes()), ontologyURI);
				OntModel assertedOntModel = ModelFactory.createOntologyModel(
						jenaPersistenceStoreInstance.getOntModelSpec(false),
						baseModel);
				OntModel reasonedOntModel = ModelFactory.createOntologyModel(
						jenaPersistenceStoreInstance.getOntModelSpec(

						REASONING_ENABLED), baseModel);
				internallyIndexedAssertedOntModels.put(ontologyURI,
						assertedOntModel);
				internallyIndexedReasonedOntModels.put(ontologyURI,
						reasonedOntModel);
				ontModelsToIndex.add(reasonedOntModel);
			}
			indexOntModels(ontModelsToIndex);
		} else {
			try {
				connectionInstance.createStatement().executeQuery(
						"SHOW TABLES;");
			} catch (Exception e) {
				// connection has timed out
				try {
					connectionInstance.close();
				} catch (SQLException e1) {
					logger.fatal("cannot close db connection");
				}
				try {
					connectionInstance = rm.obtainConnection();
				} catch (Exception e1) {
					logger.error("cannot connect to db", e);
				}
				List<OntModel> ontModelsToIndex = new Vector<OntModel>();
				for (int i = 0; i < indexedOntologyURIs.size(); i++) {
					String ontologyURI = indexedOntologyURIs.get(i);
					Writer wr = new StringWriter();
					jenaPersistenceStoreInstance.getPersistenceProvider()
							.getModel(ontologyURI).write(wr);
					Model baseModel = ModelFactory
							.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF);
					baseModel.read(new ByteArrayInputStream(wr.toString()
							.getBytes()), ontologyURI);
					// Model baseModel = jenaPersistenceStoreInstance
					// .getModelMaker(dbConnectionInstance).getModel(
					// ontologyURI);
					OntModel assertedOntModel = ModelFactory
							.createOntologyModel(jenaPersistenceStoreInstance
									.getOntModelSpec(false), baseModel);
					OntModel reasonedOntModel = ModelFactory
							.createOntologyModel(jenaPersistenceStoreInstance
									.getOntModelSpec(REASONING_ENABLED),
									baseModel);
					internallyIndexedAssertedOntModels.put(ontologyURI,
							assertedOntModel);
					internallyIndexedReasonedOntModels.put(ontologyURI,
							reasonedOntModel);
					ontModelsToIndex.add(reasonedOntModel);
				}
				indexOntModels(ontModelsToIndex);
				logger.info("ONT MODEL regenerated from fresh connection");
			}
		}
		List<OntModel> result = new Vector<OntModel>();
		if (retrieveReasonedModel) {
			Enumeration<OntModel> elements = internallyIndexedReasonedOntModels
					.elements();
			while (elements.hasMoreElements()) {
				result.add(elements.nextElement());
			}
		} else {
			Enumeration<OntModel> elements = internallyIndexedAssertedOntModels
					.elements();
			while (elements.hasMoreElements()) {
				result.add(elements.nextElement());
			}
		}
		return result;
	}

	public Search(JenaPersistenceStore jenaPersistenceStoreInstance,
			List<String> ontologyURI) {
		this.jenaPersistenceStoreInstance = jenaPersistenceStoreInstance;
		indexedOntologyURIs = ontologyURI;

		internallyIndexedReasonedOntModels = new Hashtable<String, OntModel>();
		internallyIndexedAssertedOntModels = new Hashtable<String, OntModel>();

		getIndexedOntModels(true);

		/** FIXME:: search should do some prior indexing **/
		/**
		 * for the demo, we just calculate the number of individuals per class
		 * (for calculating classRelevanceFactor)
		 **/

		// closeDBConnection(m_conn);
	}

	private List<OntClass> sortOntClassesBasedOnIndividualCount(
			List<String> classURIs, boolean isAscending) {
		List<OntClass> result = new Vector<OntClass>();
		SortedSet<ScoredOntResource> sortedSet = new TreeSet<ScoredOntResource>();
		Iterator<String> classURIsItr = classURIs.iterator();
		while (classURIsItr.hasNext()) {
			String ontClassURI = classURIsItr.next();
			List<OntModel> ontModels = getIndexedOntModels(true);
			Iterator<OntModel> ontModelsItr = ontModels.iterator();
			while (ontModelsItr.hasNext()) {
				OntClass ontClass = ontModelsItr.next()
						.getOntClass(ontClassURI);
				if (ontClass != null
						&& individualCountPerClass.containsKey(ontClassURI)) {
					int individualCount = individualCountPerClass.get(
							ontClassURI).intValue();
					ScoredOntResource scoredOntResource = new ScoredOntResource(
							ontClass, individualCount);
					sortedSet.add(scoredOntResource);
					// We assume that all classURIs are distinct, even if they
					// exist in different models;
					break;
				}
			}
		}

		if (isAscending) {
			while (!sortedSet.isEmpty()) {
				ScoredOntResource scoredOntResource = sortedSet.first();
				result.add((OntClass) scoredOntResource.getOntResource());
				sortedSet.remove(scoredOntResource);
			}
		} else {
			while (!sortedSet.isEmpty()) {
				ScoredOntResource scoredOntResource = sortedSet.last();
				result.add((OntClass) scoredOntResource.getOntResource());
				sortedSet.remove(scoredOntResource);
			}
		}

		return result;
	}

	// FIXME: Yildiray's solution will replace this function
	private List<ScoredOntResource> getOntClassesByKeyword(
			KeywordList keywordList) {
		// FIXME:: make this more formal!!!
		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		SortedSet<ScoredOntResource> sortedSet = new TreeSet<ScoredOntResource>();

		List<OntModel> ontModels = getIndexedOntModels(true);
		Iterator<OntModel> ontModelsItr = ontModels.iterator();

		while (ontModelsItr.hasNext()) {
			OntModel curOntModel = ontModelsItr.next();
			IndexBuilderString larqBuilder = new IndexBuilderString();
			larqBuilder.indexStatements(curOntModel.listStatements());
			larqBuilder.closeWriter();
			IndexLARQ index = larqBuilder.getIndex();
			LARQ.setDefaultIndex(index);
			// FIXME:: not just owl:Class'es dude!!!
			String queryString = new String(
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
							+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
							+ "SELECT ?x WHERE {\n"
							+ "?x rdf:type owl:Class .\n" + "}\n");
			Query query = QueryFactory.create(queryString);
			QueryExecution qExec = QueryExecutionFactory.create(query,
					curOntModel);
			ResultSet resultSet = qExec.execSelect();
			while (resultSet.hasNext()) {
				ResultBinding resultBinding = ((ResultBinding) resultSet.next());
				RDFNode rdfNode = resultBinding.get("x");
				if (rdfNode.isURIResource()) {
					String uri = rdfNode.toString();
					if (!containsStringInIgnoreList(uri)) {
						OntClass ontClass = curOntModel.getOntClass(uri);
						String uriPostfix = uri
								.substring(uri.lastIndexOf("#") + 1);
						double similarity = computeSimilarity(uriPostfix,
								keywordList)
								* MAX_SCORE;
						if (similarity > 0) {
							sortedSet.add(new ScoredOntResource(ontClass,
									similarity));
						}
					}
				}
			}
		}
		while (!sortedSet.isEmpty()) {
			ScoredOntResource scoredOntResource = sortedSet.last();
			result.add(scoredOntResource);
			sortedSet.remove(scoredOntResource);
		}
		return result;
	}

	/**
	 * FIXME: make this behave like the function getOntClassesByKeyword *
	 * 
	 * @throws SQLException
	 */
	private List<ScoredOntResource> getIndividualsByKeyword(
			KeywordList keywordList) {
		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		for (int i = 0; i < keywordList.getKeywordListOrKeyword().size(); i++) {
			if (keywordList.getKeywordListOrKeyword().get(i) instanceof String) {
				String keyword = (String) keywordList.getKeywordListOrKeyword()
						.get(i);

				List<OntModel> ontModels = getIndexedOntModels(true);
				Iterator<OntModel> ontModelsItr = ontModels.iterator();
				while (ontModelsItr.hasNext()) {
					OntModel curOntModel = ontModelsItr.next();
					Iterator<String> indexedOntologyURIsItr = indexedOntologyURIs
							.iterator();
					while (indexedOntologyURIsItr.hasNext()) {
						String uri = indexedOntologyURIsItr.next();
						if (uri.endsWith("#")) {
							keyword = uri + keyword;
						} else {
							keyword = uri + "#" + keyword;
						}
						Individual individual = curOntModel
								.getIndividual(keyword);
						if (individual != null) {
							try {
								individual.getOntClass();
								result.add(new ScoredOntResource(individual,
										computeSimilarity(individual
												.getLocalName(), keywordList)
												* MAX_SCORE));
							} catch (Exception e) {

							}
						}
					}
				}
			}
		}
		return result;
	}

	private double computeInnerSimilarity(String untokenizedWord,
			String untokenizedKeyword, boolean isExact, double cutThreshold) {
		if (!isExact) {
			String tokenSet1[] = untokenizedWord.split("_");
			String tokenSet2[] = untokenizedKeyword.split("_");
			double innerSimilarity = 0;
			for (int x = 0; x < tokenSet1.length; x++) {
				for (int y = 0; y < tokenSet2.length; y++) {
					SyntacticStemmed ss = new SyntacticStemmed();
					double sim = ss.get(tokenSet1[x], tokenSet2[y]);
					if (sim >= cutThreshold) {
						innerSimilarity += sim;
					}
				}
			}
			innerSimilarity = innerSimilarity
					/ (tokenSet1.length * tokenSet2.length);
			return innerSimilarity;
		} else {
			if (untokenizedWord.equalsIgnoreCase(untokenizedKeyword)) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	private double computeSimilarity(String untokenizedWord,
			KeywordList keywordList) {
		OperatorType operator = keywordList.getOperator();
		List<Object> list = keywordList.getKeywordListOrKeyword();
		// (Cihan) log similarity check
		StringBuilder sb = new StringBuilder();
		sb.append("Check Sim: ").append(untokenizedWord).append(" : ");

		double similarity = 0;

		for (int i = 0; i < list.size(); i++) {
			double maxSimilarity = 0;
			Object item = list.get(i);
			if (item instanceof String) {
				String untokenizedKeyword = (String) item;
				if (operator != null
						&& operator.ordinal() == OperatorType.AND.ordinal()) {
					similarity += computeInnerSimilarity(untokenizedWord,
							untokenizedKeyword, false, CUT_THRESHOLD);
				} else if (operator != null
						&& operator.ordinal() == OperatorType.NOT_SELECTIVE
								.ordinal()) {
					similarity -= computeInnerSimilarity(untokenizedWord,
							untokenizedKeyword, false, CUT_THRESHOLD);
				} else if (operator != null
						&& operator.ordinal() == OperatorType.OR.ordinal()) {
					double innerSimilarity = computeInnerSimilarity(
							untokenizedWord, untokenizedKeyword, false,
							CUT_THRESHOLD);
					if (innerSimilarity > maxSimilarity) {
						similarity += (innerSimilarity - maxSimilarity)
								* list.size();
						maxSimilarity = innerSimilarity;
					}
				} else if (operator != null
						&& operator.ordinal() == OperatorType.EXACT.ordinal()) {
					similarity += computeInnerSimilarity(untokenizedWord,
							untokenizedKeyword, true, CUT_THRESHOLD);
				} else {
					// acts like AND
					similarity += computeInnerSimilarity(untokenizedWord,
							untokenizedKeyword, false, CUT_THRESHOLD);
				}
			} else if (item instanceof KeywordList) {
				KeywordList innerKeywordList = (KeywordList) item;
				if (operator != null
						&& operator.ordinal() == OperatorType.AND.ordinal()) {
					similarity += computeSimilarity(untokenizedWord,
							innerKeywordList);
				} else if (operator != null
						&& operator.ordinal() == OperatorType.NOT_SELECTIVE
								.ordinal()) {
					similarity -= computeSimilarity(untokenizedWord,
							innerKeywordList);
				} else if (operator != null
						&& operator.ordinal() == OperatorType.OR.ordinal()) {
					double innerSimilarity = computeSimilarity(untokenizedWord,
							innerKeywordList);
					if (innerSimilarity > maxSimilarity) {
						similarity += (innerSimilarity - maxSimilarity)
								* list.size();
						maxSimilarity = innerSimilarity;
					}
				} else if (operator != null
						&& operator.ordinal() == OperatorType.EXACT.ordinal()) {
					similarity += computeSimilarity(untokenizedWord,
							innerKeywordList);
				} else {
					// acts like AND
					similarity += computeSimilarity(untokenizedWord,
							innerKeywordList);
				}
			}
		}
		if (similarity > 0) {
			similarity = similarity / (list.size());
		} else {
			similarity = 0;
		}
		sb.append(similarity);
		return similarity;
	}

	/**
	 * 
	 * @param list1
	 * @param operator
	 *            : allowed values OR, NOT_SELECTIVE_ EXCLUDE
	 * @param list2
	 * @return
	 */
	private List<ScoredOntResource> applyOperator(
			List<ScoredOntResource> list1, OperatorType operator,
			List<ScoredOntResource> list2) {
		// list of resources that remain after list1 EXCLUDE list2 is applied
		List<ScoredOntResource> exludedList = new Vector<ScoredOntResource>();

		// first removeDuplicates in lists
		list1 = removeDuplicates(list1);
		list2 = removeDuplicates(list2);

		// internal hashtables used for checking duplicates
		Hashtable<OntResource, Double> resourceTable = new Hashtable<OntResource, Double>();
		Hashtable<String, OntResource> uriTable = new Hashtable<String, OntResource>();

		if (operator.compareTo(OperatorType.EXCLUDE) == 0) {
			// reverse the order of the lists
			List<ScoredOntResource> tempList = list1;
			list1 = list2;
			list2 = tempList;
		}

		// populate resourceTable and uriTable with all instances of list1
		Iterator<ScoredOntResource> list1Itr = list1.iterator();
		while (list1Itr.hasNext()) {
			ScoredOntResource curRankedOntResource = list1Itr.next();
			OntResource ontResource = curRankedOntResource.getOntResource();
			Double score = curRankedOntResource.getScore();
			uriTable.put(ontResource.getURI(), ontResource);
			resourceTable.put(ontResource, score);
			// System.out.println("put in tempList " +
			// curRankedOntResource.getOntResource().getURI());
		}

		// traverse list2 to see if we have duplicate instances
		Iterator<ScoredOntResource> list2Itr = list2.iterator();
		while (list2Itr.hasNext()) {
			ScoredOntResource curRankedOntResource = list2Itr.next();
			OntResource ontResource = curRankedOntResource.getOntResource();
			Double score = curRankedOntResource.getScore();
			if (!uriTable.containsKey(ontResource.getURI())) {
				// this is a new instance
				if (operator.compareTo(OperatorType.EXCLUDE) == 0) {
					// then simply place it in excludedList
					exludedList.add(curRankedOntResource);
					// System.out.println("put in excluded list " +
					// curRankedOntResource.getOntResource().getURI());
				} else if (operator.compareTo(OperatorType.NOT_SELECTIVE) == 0) {
					// if not selective, and list1 does not contain it, then
					// assign it an initial score of 0-score
					uriTable.put(ontResource.getURI(), ontResource);
					resourceTable.put(ontResource, (0 - score));
				} else if (operator.compareTo(OperatorType.OR) == 0) {
					// update internal hashtables
					uriTable.put(ontResource.getURI(), ontResource);
					resourceTable.put(ontResource, score);
				}
			} else {
				// this is a duplicate instance
				Double storedScore = resourceTable.get(ontResource);
				if (operator.compareTo(OperatorType.OR) == 0) {
					// OR implies: take the max score
					if (storedScore.compareTo(score) < 0) {
						uriTable.put(ontResource.getURI(), ontResource);
						resourceTable.put(ontResource, score);
					}
				} else if (operator.compareTo(OperatorType.NOT_SELECTIVE) == 0) {
					// NOT SELECTIVE implies: reduce the score
					double newScore = storedScore - score;
					uriTable.put(ontResource.getURI(), ontResource);
					resourceTable.put(ontResource, newScore);
				}
			}
		}

		if (operator.compareTo(OperatorType.OR) == 0
				|| operator.compareTo(OperatorType.NOT_SELECTIVE) == 0) {
			// in case of OR || NOT SELECTIVE, simply traverse the hashtables to
			// find the scores
			List<ScoredOntResource> result = new Vector<ScoredOntResource>();
			Enumeration<String> keys = uriTable.keys();
			while (keys.hasMoreElements()) {
				String nextKey = keys.nextElement();
				OntResource ontResource = uriTable.get(nextKey);
				double score = resourceTable.get(ontResource);
				result.add(new ScoredOntResource(ontResource, score));
			}
			Collections.sort(result);
			Collections.reverse(result);
			return result;
		} else if (operator.compareTo(OperatorType.EXCLUDE) == 0) {
			// in case of EXCLUDE, use the excludedList
			Collections.sort(exludedList);
			Collections.reverse(exludedList);
			return exludedList;
		}
		return null;
	}

	private List<ScoredOntResource> removeDuplicates(
			List<ScoredOntResource> list) {
		Hashtable<OntResource, Double> resourceTable = new Hashtable<OntResource, Double>();
		Hashtable<String, OntResource> uriTable = new Hashtable<String, OntResource>();
		Iterator<ScoredOntResource> listItr = list.iterator();
		while (listItr.hasNext()) {
			ScoredOntResource curRankedOntResource = listItr.next();
			OntResource ontResource = curRankedOntResource.getOntResource();
			Double score = curRankedOntResource.getScore();
			if (!uriTable.containsKey(ontResource.getURI())) {
				uriTable.put(ontResource.getURI(), ontResource);
				resourceTable.put(ontResource, score);
			} else {
				Double storedScore = resourceTable.get(ontResource);
				if (storedScore.compareTo(score) < 0) {
					uriTable.put(ontResource.getURI(), ontResource);
					resourceTable.put(ontResource, score);
				}
			}
		}

		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		Enumeration<String> keys = uriTable.keys();
		while (keys.hasMoreElements()) {
			String nextKey = keys.nextElement();
			OntResource ontResource = uriTable.get(nextKey);
			double score = resourceTable.get(ontResource);
			result.add(new ScoredOntResource(ontResource, score));
		}
		return result;
	}

	private List<ScoredOntResource> boostDuplicates(List<ScoredOntResource> list) {
		Hashtable<OntResource, Double> resourceTable = new Hashtable<OntResource, Double>();
		Hashtable<String, OntResource> uriTable = new Hashtable<String, OntResource>();
		Iterator<ScoredOntResource> listItr = list.iterator();
		while (listItr.hasNext()) {
			ScoredOntResource curRankedOntResource = listItr.next();
			OntResource ontResource = curRankedOntResource.getOntResource();
			Double score = curRankedOntResource.getScore();
			if (!uriTable.containsKey(ontResource.getURI())) {
				uriTable.put(ontResource.getURI(), ontResource);
				resourceTable.put(ontResource, score);
			} else {
				Double storedScore = resourceTable.get(ontResource);
				uriTable.put(ontResource.getURI(), ontResource);
				resourceTable.put(ontResource, score + storedScore);
			}
		}

		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		Enumeration<String> keys = uriTable.keys();
		while (keys.hasMoreElements()) {
			String nextKey = keys.nextElement();
			OntResource ontResource = uriTable.get(nextKey);
			double score = resourceTable.get(ontResource);
			result.add(new ScoredOntResource(ontResource, score));
		}
		return result;
	}

	private List<ScoredOntResource> removeDuplicatesTakeMinimumForGranted(
			List<ScoredOntResource> list) {
		Hashtable<OntResource, Double> resourceTable = new Hashtable<OntResource, Double>();
		Hashtable<String, OntResource> uriTable = new Hashtable<String, OntResource>();
		Iterator<ScoredOntResource> listItr = list.iterator();
		while (listItr.hasNext()) {
			ScoredOntResource curRankedOntResource = listItr.next();
			OntResource ontResource = curRankedOntResource.getOntResource();
			Double score = curRankedOntResource.getScore();
			if (!uriTable.containsKey(ontResource.getURI())) {
				uriTable.put(ontResource.getURI(), ontResource);
				resourceTable.put(ontResource, score);
			} else {
				Double storedScore = resourceTable.get(ontResource);
				if (storedScore.compareTo(score) > 0) {
					uriTable.put(ontResource.getURI(), ontResource);
					resourceTable.put(ontResource, score);
				}
			}
		}

		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		Enumeration<String> keys = uriTable.keys();
		while (keys.hasMoreElements()) {
			String nextKey = keys.nextElement();
			OntResource ontResource = uriTable.get(nextKey);
			double score = resourceTable.get(ontResource);
			result.add(new ScoredOntResource(ontResource, score));
		}
		return result;
	}

	private List<ScoredOntResource> computeSuperClassClosure(
			ScoredOntResource scoredOntResource, int flexibility,
			double degradingCoefficient) {
		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		if (flexibility >= 0) {

			OntClass ontClass = (OntClass) scoredOntResource.getOntResource();
			double rank = scoredOntResource.getScore();

			result.add(scoredOntResource);
			ExtendedIterator equivalentClassesItr = ontClass
					.listEquivalentClasses();
			while (equivalentClassesItr.hasNext()) {
				OntClass curOntClass = (OntClass) equivalentClassesItr.next();
				result.add(new ScoredOntResource(curOntClass, rank
						/ degradingCoefficient));
			}
			if (flexibility > 0) {
				// get only direct superclasses!???
				ExtendedIterator superClassesItr = ontClass.listSuperClasses();
				while (superClassesItr.hasNext()) {
					OntClass curSuperClass = (OntClass) superClassesItr.next();
					List<ScoredOntResource> closure = computeSuperClassClosure(
							new ScoredOntResource(curSuperClass, rank
									/ degradingCoefficient), (flexibility - 1),
							degradingCoefficient);
					result.addAll(closure);
				}
			}
		}
		return removeDuplicates(result);
	}

	private List<ScoredOntResource> computeSubClassClosure(
			ScoredOntResource scoredOntResource, int flexibility,
			double degradingCoefficient) {
		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		if (flexibility >= 0) {

			OntClass ontClass = (OntClass) scoredOntResource.getOntResource();
			double rank = scoredOntResource.getScore();

			result.add(scoredOntResource);
			ExtendedIterator equivalentClassesItr = ontClass
					.listEquivalentClasses();
			while (equivalentClassesItr.hasNext()) {
				OntClass curOntClass = (OntClass) equivalentClassesItr.next();
				result.add(new ScoredOntResource(curOntClass, rank
						/ degradingCoefficient));
			}
			if (flexibility > 0) {
				// get only direct subclasses!???
				ExtendedIterator subClassesItr = ontClass.listSubClasses();
				while (subClassesItr.hasNext()) {
					OntClass curSubClass = (OntClass) subClassesItr.next();
					List<ScoredOntResource> closure = computeSubClassClosure(
							new ScoredOntResource(curSubClass, rank
									/ degradingCoefficient), (flexibility - 1),
							degradingCoefficient);
					result.addAll(closure);
				}
			}
		}
		return removeDuplicates(result);
	}

	private boolean retrieveRelatedIndividualsHelperFunction(Statement stmt) {
		boolean test = false;
		for (int i = 0; i < indexedOntologyURIs.size(); i++) {
			if (stmt.getPredicate().toString().contains(
					indexedOntologyURIs.get(i))
					|| stmt.getPredicate().toString().contains(
							"http://dbpedia.org/property/")) {
				test = true;
			}
		}
		return test;
	}

	private List<ScoredOntResource> retrieveRelatedIndividuals(
			Individual individual, double initialScore,
			double degradingCoefficient) {
		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		/**
		 * FIXME::
		 * 
		 */

		ObjectProperty objProp = individual
				.getOntModel()
				.getObjectProperty(
						"http://iks-project.org/companyname/repositoryname/workspacename#externalAnnotation");
		if (!individual.hasProperty(objProp)) {
			return result;
		}

		StmtIterator iter1 = individual.getOntModel().listStatements(
				new SimpleSelector(individual, objProp, (RDFNode) (null)));
		while (iter1.hasNext()) {
			Statement curStmt1 = (Statement) iter1.next();
			if (curStmt1.getObject().isURIResource()) {
				String relatedIndividualURI1 = curStmt1.getObject().toString();
				// System.out.println("RETRIEVE_RELATED_INDIVIDUALS in curStmt1="+curStmt1.toString());
				if (!retrieveRelatedIndividualsHelperFunction(curStmt1)) {
					continue;
				}
				
				ResourceManager resourceManager = ResourceManager.getInstance();
				String ontologyURI1 = resourceManager
						.resolveOntologyURIFromResourceURI(relatedIndividualURI1);
				if (ontologyURI1 != null
						&& indexedOntologyURIs.contains(ontologyURI1)) {
					OntModel relatedIndividualOntModel1 = getOntModelForOntologyURI(
							ontologyURI1, REASONING_ENABLED);
					Individual relatedIndividual1 = relatedIndividualOntModel1
							.getIndividual(relatedIndividualURI1);
					if (relatedIndividual1 != null) {
						List<Individual> intermediateIndividuals = new Vector<Individual>();

						intermediateIndividuals.add(relatedIndividual1);

						StmtIterator iter2 = relatedIndividual1.getOntModel()
								.listStatements(
										new SimpleSelector(relatedIndividual1,
												null, (RDFNode) (null)));
						while (iter2.hasNext()) {
							Statement curStmt2 = (Statement) iter2.next();
							if (curStmt2.getObject().isURIResource()) {
								String relatedIndividualURI2 = curStmt2
										.getObject().toString();
								if (!retrieveRelatedIndividualsHelperFunction(curStmt2)) {
									continue;
								}
								String ontologyURI2 = resourceManager
										.resolveOntologyURIFromResourceURI(relatedIndividualURI2);
								if (ontologyURI2 != null
										&& indexedOntologyURIs
												.contains(ontologyURI2)) {
									OntModel relatedIndividualOntModel2 = getOntModelForOntologyURI(
											ontologyURI2, REASONING_ENABLED);
									Individual relatedIndividual2 = relatedIndividualOntModel2
											.getIndividual(relatedIndividualURI2);
									if (relatedIndividual2 != null) {
										intermediateIndividuals
												.add(relatedIndividual2);
									}
								}
							}
						}
						StmtIterator iter3 = relatedIndividual1.getOntModel()
								.listStatements(
										new SimpleSelector(null, null,
												relatedIndividual1));
						while (iter3.hasNext()) {
							Statement curStmt3 = (Statement) iter3.next();
							String relatedIndividualURI3 = curStmt3
									.getSubject().toString();
							if (!retrieveRelatedIndividualsHelperFunction(curStmt3)) {
								continue;
							}
							String ontologyURI3 = resourceManager
									.resolveOntologyURIFromResourceURI(relatedIndividualURI3);
							if (ontologyURI3 != null
									&& indexedOntologyURIs
											.contains(ontologyURI3)) {
								OntModel relatedIndividualOntModel3 = getOntModelForOntologyURI(
										ontologyURI3, REASONING_ENABLED);
								Individual relatedIndividual3 = relatedIndividualOntModel3
										.getIndividual(relatedIndividualURI3);
								if (relatedIndividual3 != null) {
									intermediateIndividuals
											.add(relatedIndividual3);
								}
							}
						}

						for (int i = 0; i < intermediateIndividuals.size(); i++) {
							Individual interIndividual = intermediateIndividuals
									.get(i);
							StmtIterator iter4 = individual.getOntModel()
									.listStatements(
											new SimpleSelector(null, null,
													interIndividual));
							while (iter4.hasNext()) {
								Statement curStmt4 = (Statement) iter4.next();
								String relatedIndividualURI4 = curStmt4
										.getSubject().getURI();
								if (!retrieveRelatedIndividualsHelperFunction(curStmt4)) {
									continue;
								}
								String ontologyURI4 = resourceManager
										.resolveOntologyURIFromResourceURI(relatedIndividualURI4);
								if (ontologyURI4 != null
										&& indexedOntologyURIs
												.contains(ontologyURI4)) {
									OntModel relatedIndividualOntModel4 = getOntModelForOntologyURI(
											ontologyURI4, REASONING_ENABLED);
									Individual relatedIndividual4 = relatedIndividualOntModel4
											.getIndividual(relatedIndividualURI4);
									if (relatedIndividual4 != null) {
										result.add(new ScoredOntResource(
												relatedIndividual4,
												initialScore));
									}
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Finds the relevant classes and individuals based on a given degree of
	 * flexibility The degree of flexibility for classes and individuals can be
	 * separately set
	 * 
	 * @param ontModel
	 * @param ontClass
	 * @param flexibility_Individuals
	 * @param flexibility_Classes
	 * @param initialScore
	 * @param degradingCoefficient
	 * @param usedbPedia
	 * @return
	 */
	private List<ScoredOntResource> computeClosureForOntClass(
			OntModel ontModel, OntClass ontClass, int flexibility_Individuals,
			int flexibility_Classes, double initialScore,
			double degradingCoefficient, boolean usedbPedia) {
		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		if (ontClass != null) {
			ScoredOntResource scoredOntResource = new ScoredOntResource(
					ontClass, initialScore);
			// first do everything for class and its superclasses, here maximum
			// value is replaced in removing duplicates
			List<ScoredOntResource> closure = computeSuperClassClosure(
					scoredOntResource, flexibility_Individuals,
					degradingCoefficient);
			closure = removeDuplicates(closure);
			Iterator<ScoredOntResource> closureItr = closure.iterator();
			while (closureItr.hasNext()) {
				ScoredOntResource curOntResource = closureItr.next();

				if (((OntClass) curOntResource.getOntResource()).getURI()
						.equalsIgnoreCase(OWL.Nothing.getURI())
						|| ((OntClass) curOntResource.getOntResource())
								.getURI().equalsIgnoreCase(OWL.Thing.getURI())) {
					continue;
				}

				curOntResource = new ScoredOntResource(curOntResource
						.getOntResource(), curOntResource.getScore());
				OntClass curOntClass = (OntClass) curOntResource
						.getOntResource();
				double curRank = curOntResource.getScore();
				ExtendedIterator individualsItr = curOntClass
						.listInstances(false);
				while (individualsItr.hasNext()) {
					Individual individual = (Individual) individualsItr.next();
					boolean isDirectParent = false;
					List<OntModel> ontModels = getIndexedOntModels(false);
					Iterator<OntModel> ontModelsItr = ontModels.iterator();
					while (ontModelsItr.hasNext()) {
						OntModel curOntModel = ontModelsItr.next();
						Individual unreasonedIndividual = curOntModel
								.getIndividual(individual.getURI());
						if (unreasonedIndividual != null) {
							ExtendedIterator individualParentsItr = unreasonedIndividual
									.listOntClasses(true);
							while (individualParentsItr.hasNext()) {
								OntClass parentClass = (OntClass) individualParentsItr
										.next();
								if (parentClass.getURI().equalsIgnoreCase(
										curOntClass.getURI())) {
									isDirectParent = true;
									break;
								}
							}
						}
					}
					if (isDirectParent) {
						result.add(new ScoredOntResource(individual, curRank));
					} else {
						result
								.add(new ScoredOntResource(
										individual,
										curRank
												/ (degradingCoefficient * degradingCoefficient)));
					}

					/**
					 * Tuncay Namli
					 * 
					 */
					if (usedbPedia) {
						result.addAll(retrieveRelatedIndividuals(individual,
								curRank, degradingCoefficient));
					}
				}
			}
			result.addAll(computeSuperClassClosure(scoredOntResource,
					flexibility_Classes, degradingCoefficient));
			result = removeDuplicates(result);

			closure = computeSubClassClosure(scoredOntResource,
					flexibility_Individuals, degradingCoefficient);
			closure = removeDuplicates(closure);
			closureItr = closure.iterator();
			while (closureItr.hasNext()) {
				ScoredOntResource curOntResource = closureItr.next();

				if (((OntClass) curOntResource.getOntResource()).getURI()
						.equalsIgnoreCase(OWL.Nothing.getURI())
						|| ((OntClass) curOntResource.getOntResource())
								.getURI().equalsIgnoreCase(OWL.Thing.getURI())) {
					continue;
				}

				curOntResource = new ScoredOntResource(curOntResource
						.getOntResource(), curOntResource.getScore());
				OntClass curOntClass = (OntClass) curOntResource
						.getOntResource();
				double curRank = curOntResource.getScore();
				ExtendedIterator individualsItr = curOntClass
						.listInstances(false);
				while (individualsItr.hasNext()) {
					Individual individual = (Individual) individualsItr.next();
					boolean isDirectParent = false;
					List<OntModel> ontModels = getIndexedOntModels(false);
					Iterator<OntModel> ontModelsItr = ontModels.iterator();
					while (ontModelsItr.hasNext()) {
						OntModel curOntModel = ontModelsItr.next();
						Individual unreasonedIndividual = curOntModel
								.getIndividual(individual.getURI());
						if (unreasonedIndividual != null) {
							ExtendedIterator individualParentsItr = unreasonedIndividual
									.listOntClasses(true);
							while (individualParentsItr.hasNext()) {
								OntClass parentClass = (OntClass) individualParentsItr
										.next();
								if (parentClass.getURI().equalsIgnoreCase(
										curOntClass.getURI())) {
									isDirectParent = true;
									break;
								}
							}
						}
					}
					if (isDirectParent) {
						result.add(new ScoredOntResource(individual, curRank));
					} else {
						// we did not square DEGRADING_COEFFICIENT simply
						// because
						// my grandchildren are more important than my cousins
						result.add(new ScoredOntResource(individual, curRank
								/ degradingCoefficient));
					}
					/**
					 * Tuncay Namli
					 * 
					 */
					if (usedbPedia) {
						result.addAll(retrieveRelatedIndividuals(individual,
								curRank, degradingCoefficient));
					}
				}
			}
			result.addAll(computeSubClassClosure(scoredOntResource,
					flexibility_Classes, degradingCoefficient));
			result = removeDuplicatesTakeMinimumForGranted(result);
		}
		return result;
	}

	private List<ScoredOntResource> getQueryRelatedResources(
			String queryString, int flexibility_Individuals,
			int flexibility_Classes, boolean usedbPedia) {
		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		List<OntModel> ontModels = getIndexedOntModels(true);
		Iterator<OntModel> ontModelsItr = ontModels.iterator();
		while (ontModelsItr.hasNext()) {
			OntModel curOntModel = ontModelsItr.next();
			IndexBuilderString larqBuilder = new IndexBuilderString();
			larqBuilder.indexStatements(curOntModel.listStatements());
			larqBuilder.closeWriter();
			IndexLARQ index = larqBuilder.getIndex();
			LARQ.setDefaultIndex(index);
			Query query = QueryFactory.create(queryString);
			QueryExecution qExec = QueryExecutionFactory.create(query,
					curOntModel);
			ResultSet resultSet = qExec.execSelect();
			List<OntClass> ontClasses = new Vector<OntClass>();
			while (resultSet.hasNext()) {
				ResultBinding resultBinding = ((ResultBinding) resultSet.next());
				RDFNode rdfNode = resultBinding.get(QUERY_COLUMN_NAME);
				if (rdfNode.isURIResource()) {
					// it could be an individual or a class (for now, other
					// solutions are ignored)
					String uri = rdfNode.toString();
					OntClass ontClass = curOntModel.getOntClass(uri);
					Individual individual = curOntModel.getIndividual(uri);
					if (ontClass != null) {
						ontClasses.add(ontClass);
					} else if (individual != null) {
						ontClasses.add(individual.getOntClass());
						// these individuals will definitily be in the result
						// set with some initial rank
						ScoredOntResource scoredOntResource = new ScoredOntResource(
								individual, MAX_SCORE);
						result.add(scoredOntResource);
					}
				}
			}
			// computing closure and stuff
			for (int x = 0; x < ontClasses.size(); x++) {
				OntClass ontClass = ontClasses.get(x);
				List<ScoredOntResource> subResult = computeClosureForOntClass(
						curOntModel, ontClass, flexibility_Individuals,
						flexibility_Classes, (MAX_SCORE),
						DEGRADING_COEFFICIENT, usedbPedia);
				result.addAll(subResult);
			}
		}
		// finally, the duplicates should be removed;
		// remember, in case of duplicates, the highest score is taken
		return removeDuplicates(result);
	}

	private List<ScoredOntResource> getSelectionRelatedSelectiveResources(
			ResourceList resourceList, int flexibility_Individuals,
			int flexibility_Classes, boolean usedbPedia) {
		List<SelectiveResource> selectiveResources = resourceList
				.getSelectiveResource();
		List<ScoredOntResource> OR_List = new Vector<ScoredOntResource>();
		List<OntModel> ontModels = getIndexedOntModels(true);
		Iterator<OntModel> ontModelsItr = ontModels.iterator();
		while (ontModelsItr.hasNext()) {
			OntModel curOntModel = ontModelsItr.next();
			Iterator<SelectiveResource> selectiveResourcesItr = selectiveResources
					.iterator();
			while (selectiveResourcesItr.hasNext()) {
				SelectiveResource curSelectiveResource = selectiveResourcesItr
						.next();
				OntClass ontClass = curOntModel
						.getOntClass(curSelectiveResource.getResourceURI());
				if (ontClass != null) {
					if (curSelectiveResource.getOperator().compareTo(
							OperatorType.OR) == 0) {
						OR_List.addAll(computeClosureForOntClass(curOntModel,
								ontClass, flexibility_Individuals,
								flexibility_Classes, (MAX_SCORE),
								DEGRADING_COEFFICIENT, usedbPedia));
					}
				}
			}
		}
		return OR_List;
	}

	private List<ScoredOntResource> getSelectionRelatedNotSelectiveResources(
			ResourceList resourceList, int flexibility_Individuals,
			int flexibility_Classes, boolean usedbPedia) {
		List<SelectiveResource> selectiveResources = resourceList
				.getSelectiveResource();
		List<ScoredOntResource> NOT_SELECTIVE_List = new Vector<ScoredOntResource>();
		List<OntModel> ontModels = getIndexedOntModels(true);
		Iterator<OntModel> ontModelsItr = ontModels.iterator();
		while (ontModelsItr.hasNext()) {
			OntModel curOntModel = ontModelsItr.next();
			Iterator<SelectiveResource> selectiveResourcesItr = selectiveResources
					.iterator();
			while (selectiveResourcesItr.hasNext()) {
				SelectiveResource curSelectiveResource = selectiveResourcesItr
						.next();
				OntClass ontClass = curOntModel
						.getOntClass(curSelectiveResource.getResourceURI());
				if (ontClass != null) {
					if (curSelectiveResource.getOperator().compareTo(
							OperatorType.NOT_SELECTIVE) == 0) {
						NOT_SELECTIVE_List.addAll(computeClosureForOntClass(
								curOntModel, ontClass, flexibility_Individuals,
								flexibility_Classes, (MAX_SCORE),
								DEGRADING_COEFFICIENT, usedbPedia));
					}
				}
			}
		}
		return NOT_SELECTIVE_List;
	}

	private List<ScoredOntResource> getSelectionRelatedExcludedResources(
			ResourceList resourceList, int flexibility_Individuals,
			int flexibility_Classes, boolean usedbPedia) {
		List<ScoredOntResource> EXCLUDE_List = new Vector<ScoredOntResource>();
		List<SelectiveResource> selectiveResources = resourceList
				.getSelectiveResource();
		List<OntModel> ontModels = getIndexedOntModels(true);
		Iterator<OntModel> ontModelsItr = ontModels.iterator();
		while (ontModelsItr.hasNext()) {
			OntModel curOntModel = ontModelsItr.next();
			Iterator<SelectiveResource> selectiveResourcesItr = selectiveResources
					.iterator();
			while (selectiveResourcesItr.hasNext()) {
				SelectiveResource curSelectiveResource = selectiveResourcesItr
						.next();
				OntClass ontClass = curOntModel
						.getOntClass(curSelectiveResource.getResourceURI());
				if (ontClass != null
						&& curSelectiveResource.getOperator().compareTo(
								OperatorType.EXCLUDE) == 0) {
					EXCLUDE_List
							.addAll(removeDuplicates(computeClosureForOntClass(
									curOntModel, ontClass,
									flexibility_Individuals,
									flexibility_Classes, (MAX_SCORE),
									DEGRADING_COEFFICIENT, usedbPedia)));
				}
			}
		}
		return EXCLUDE_List;
	}

	/**
	 * @param ontologyURI
	 * @param keyword
	 * @param flexibility
	 * @return a list of OntResource(s) -- either OntClass or OntIndividual --
	 *         that are found relevant based on the given search criteria
	 * @throws SQLException
	 */
	// FIXME:: replace String keyword with List<String> keywords
	private List<ScoredOntResource> getKeywordRelatedResources(
			KeywordList keywordList, int flexibility_Individuals,
			int flexibility_Classes, boolean usedbPedia) {
		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		// find all ontology classes that match with the keyword
		List<ScoredOntResource> scoredOntClasses = getOntClassesByKeyword(keywordList);
		// find all individuals that match with the keyword
		List<ScoredOntResource> individualsList = getIndividualsByKeyword(keywordList);
		for (int x = 0; x < individualsList.size(); x++) {
			ScoredOntResource scoredOntResource = individualsList.get(x);
			// these individuals will definitily be in the result set with some
			// initial rank
			result.add(scoredOntResource);

			// you have to compute the closure of the classes these individuals
			// belong to
			/**
			 * Rule of thumb: an individual contributes to each of its container
			 * class inversely proportional to the total number of individuals
			 * in the container class
			 */
			Individual individual = (Individual) scoredOntResource
					.getOntResource();
			ExtendedIterator extendedIterator = individual.listOntClasses(true);
			/**
			 * Rule of thumb: an individual contributes to each of its container
			 * class inversely proportional to the total number of individuals
			 * in the container class
			 */
			List<String> ontClassURIs = new Vector<String>();
			while (extendedIterator.hasNext()) {
				OntClass ontClass = (OntClass) extendedIterator.next();
				if (ontClass != null && ontClass.getURI() != null) {
					ontClassURIs.add(ontClass.getURI());
				}
			}

			List<OntClass> sortedOntClassesBasedOnIndividualCount = sortOntClassesBasedOnIndividualCount(
					ontClassURIs, true);
			Iterator<OntClass> sortedOntClassesBasedOnIndividualCountIterator = sortedOntClassesBasedOnIndividualCount
					.iterator();
			int orderInList = 0;
			while (sortedOntClassesBasedOnIndividualCountIterator.hasNext()) {
				orderInList++;
				OntClass ontClass = sortedOntClassesBasedOnIndividualCountIterator
						.next();
				double classRelevanceFactor = 1 / Math.pow(
						DEGRADING_COEFFICIENT, (orderInList * orderInList));
				if (!isClassExcludedForCategorization(ontClass.getURI())
						&& classRelevanceFactor >= CLASS_RELEVANCE_THRESHOLD) {
					scoredOntClasses
							.add(new ScoredOntResource(ontClass,
									scoredOntResource.getScore()
											* classRelevanceFactor));
				}
			}
		}
		// FIXME:: such optimizations are future work
		// you may want to remove duplicates from ontClasses first, too,

		// computing closure and stuff
		for (int x = 0; x < scoredOntClasses.size(); x++) {
			ScoredOntResource scoredOntClass = scoredOntClasses.get(x);
			OntClass ontClass = (OntClass) scoredOntClass.getOntResource();
			List<ScoredOntResource> subResult = computeClosureForOntClass(
					ontClass.getOntModel(), ontClass, flexibility_Individuals,
					flexibility_Classes, scoredOntClass.getScore(),
					DEGRADING_COEFFICIENT, usedbPedia);
			result.addAll(subResult);
		}
		// finally, the duplicates should be removed;
		// remember, in case of duplicates, the highest score is taken
		return removeDuplicates(result);
	}

	private List<ScoredOntResource> getPathRelatedResources(
			List<FullTextSearchResult> searchResults,
			int flexibility_Individuals, int flexibility_Classes,
			boolean usedbPedia) {
		List<ScoredOntResource> result = new Vector<ScoredOntResource>();
		double maxTotal = 0;
		Hashtable<OntClass, Double> totalIndividualScoreForClasses = new Hashtable<OntClass, Double>();
		for (int x = 0; x < searchResults.size(); x++) {
			FullTextSearchResult searchResult = searchResults.get(x);
			String resourcePath = searchResult.getDocumentXPath();
			double score = searchResult.getScore();
			List<Individual> individuals = findIndividualsGivenContentRepositoryPath(resourcePath);
			for (int y = 0; y < individuals.size(); y++) {
				Individual individual = individuals.get(y);
				result.add(new ScoredOntResource(individual, score));
				ExtendedIterator extendedIterator = individual
						.listOntClasses(true);
				/**
				 * Rule of thumb: an individual contributes to each of its
				 * container class inversely proportional to the total number of
				 * individuals in the container class
				 */
				List<String> ontClassURIs = new Vector<String>();
				while (extendedIterator.hasNext()) {
					OntClass ontClass = (OntClass) extendedIterator.next();
					if (ontClass != null && ontClass.getURI() != null) {
						ontClassURIs.add(ontClass.getURI());
					}
				}

				List<OntClass> sortedOntClassesBasedOnIndividualCount = sortOntClassesBasedOnIndividualCount(
						ontClassURIs, true);
				Iterator<OntClass> sortedOntClassesBasedOnIndividualCountIterator = sortedOntClassesBasedOnIndividualCount
						.iterator();
				int orderInList = 0;
				while (sortedOntClassesBasedOnIndividualCountIterator.hasNext()) {
					orderInList++;
					double classRelevanceFactor = 1 / Math.pow(
							DEGRADING_COEFFICIENT, (orderInList));
					OntClass ontClass = sortedOntClassesBasedOnIndividualCountIterator
							.next();
					if (ontClass == null) {
						logger.info("most relevant container class of individual "
										+ individual.getURI() + " null");
					} else {
						logger.info("most relevant container class of individual "
										+ individual.getURI()
										+ " ("
										+ orderInList
										+ ")::: "
										+ ontClass.getURI());
					}
					if (ontClass != null
							&& !totalIndividualScoreForClasses
									.containsKey(ontClass)) {
						totalIndividualScoreForClasses.put(ontClass,
								new Double(0));
					}
					if (!isClassExcludedForCategorization(ontClass.getURI())
							&& classRelevanceFactor >= CLASS_RELEVANCE_THRESHOLD) {
						int integerCountForClass = individualCountPerClass.get(
								ontClass.getURI()).intValue();
						double addedScore = totalIndividualScoreForClasses.get(
								ontClass).doubleValue()
								+ (score / integerCountForClass)
								* classRelevanceFactor;
						if (addedScore > maxTotal) {
							maxTotal = addedScore;
						}
						totalIndividualScoreForClasses
								.put(ontClass, addedScore);
					}
				}
			}
		}
		// now you have a list of classes as usual
		Enumeration<OntClass> ontClasses = totalIndividualScoreForClasses
				.keys();
		while (ontClasses.hasMoreElements()) {
			OntClass ontClass = ontClasses.nextElement();
			// double initialScore =
			// ((totalIndividualScoreForClasses.get(ontClass).doubleValue())/maxTotal)*;
			double initialScore = totalIndividualScoreForClasses.get(ontClass)
					.doubleValue();
			if (initialScore > 0) {
				List<ScoredOntResource> subResult = computeClosureForOntClass(
						ontClass.getOntModel(), ontClass,
						flexibility_Individuals, flexibility_Classes,
						initialScore, DEGRADING_COEFFICIENT, usedbPedia);
				result.addAll(subResult);
			}
		}
		// finally, the duplicates should be removed;
		// remember, in case of duplicates, the highest score is taken
		return removeDuplicates(result);
	}

	private String getContentRepositoryPathForIndividual(Individual individual) {
		Property property = individual.getOntModel().getProperty(
				CONTENT_INSTANCE_XPATH_PROPERTY_PREFIX
						+ CONTENT_INSTANCE_XPATH_PROPERTY_URI);
		if (property != null && individual.hasProperty(property)) {
			RDFNode rdfNode = individual.getPropertyValue(property);
			if (rdfNode.isLiteral()) {
				// we expect this to be a literal value
				return rdfNode.toString();
			}
		}
		return null;
	}

	private List<Individual> findIndividualsGivenContentRepositoryPath(
			String contentRepositoryPath) {
		List<Individual> result = new Vector<Individual>();
		List<OntModel> ontModels = getIndexedOntModels(true);
		Iterator<OntModel> ontModelsItr = ontModels.iterator();
		while (ontModelsItr.hasNext()) {
			OntModel curOntModel = ontModelsItr.next();
			IndexBuilderString larqBuilder = new IndexBuilderString();
			StmtIterator statements = curOntModel.listStatements();
			larqBuilder.indexStatements(statements);
			larqBuilder.closeWriter();
			IndexLARQ index = larqBuilder.getIndex();
			LARQ.setDefaultIndex(index);
			String queryString = new String(
					"PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n"
							+ "PREFIX jcr2ont: <"
							+ CONTENT_INSTANCE_XPATH_PROPERTY_PREFIX + ">\n"
							+ "SELECT ?doc {\n" + " ?lit pf:textMatch '+"
							+ contentRepositoryPath + "'. \n" + " ?doc "
							+ "jcr2ont:" + CONTENT_INSTANCE_XPATH_PROPERTY_URI
							+ " ?lit\n" + "}\n");
			Query query = QueryFactory.create(queryString);
			QueryExecution qExec = QueryExecutionFactory.create(query,
					curOntModel);
			ResultSet resultSet = qExec.execSelect();
			while (resultSet.hasNext()) {
				ResultBinding resultBinding = ((ResultBinding) resultSet.next());
				RDFNode rdfNode = resultBinding.get("doc");
				if (rdfNode.isURIResource()) {
					Individual individual = curOntModel.getIndividual(rdfNode
							.toString());
					result.add(individual);
					// System.out.println(individual);
				}
			}
		}
		return result;
	}

	private List<ScoredOntResource> normalizeAndMergeSets(
			List<ScoredOntResource> setA_OntResources,
			List<ScoredOntResource> setB_OntResources,
			List<ScoredOntResource> setC_OntResources) {
		// Now, you can safely merge the results
		List<ScoredOntResource> sortedSet = new Vector<ScoredOntResource>();

		// normalize setA and add it to sortedSet
		double[] setA_array = new double[setA_OntResources.size()];
		for (int i = 0; i < setA_OntResources.size(); i++) {
			setA_array[i] = setA_OntResources.get(i).getScore();
		}
		double setA_mean = StatUtils.mean(setA_array);
		double setA_variance = StatUtils.variance(setA_array);
		for (int i = 0; i < setA_OntResources.size(); i++) {
			if (setA_variance != 0) {
				double normalizedScore = (setA_OntResources.get(i).getScore() - setA_mean)
						/ (Math.pow(setA_variance, 0.5)) * MAX_SCORE;
				setA_OntResources.get(i).setScore(normalizedScore);
			} else {
				setA_OntResources.get(i).setScore(0.0);
			}
			sortedSet.add(setA_OntResources.get(i));
		}

		// normalize setB and add it to sortedSet
		double[] setB_array = new double[setB_OntResources.size()];
		for (int i = 0; i < setB_OntResources.size(); i++) {
			setB_array[i] = setB_OntResources.get(i).getScore();
		}
		double setB_mean = StatUtils.mean(setB_array);
		double setB_variance = StatUtils.variance(setB_array);
		for (int i = 0; i < setB_OntResources.size(); i++) {
			if (setB_variance != 0) {
				double normalizedScore = (setB_OntResources.get(i).getScore() - setB_mean)
						/ (Math.pow(setB_variance, 0.5)) * MAX_SCORE;
				setB_OntResources.get(i).setScore(normalizedScore);
			} else {
				setB_OntResources.get(i).setScore(0.0);
			}
			sortedSet.add(setB_OntResources.get(i));
		}

		// normalize setC and add it to sortedSet
		double[] setC_array = new double[setC_OntResources.size()];
		for (int i = 0; i < setC_OntResources.size(); i++) {
			setC_array[i] = setC_OntResources.get(i).getScore();
		}
		double setC_mean = StatUtils.mean(setC_array);
		double setC_variance = StatUtils.variance(setC_array);
		for (int i = 0; i < setC_OntResources.size(); i++) {
			if (setB_variance != 0) {
				double normalizedScore = (setC_OntResources.get(i).getScore() - setC_mean)
						/ (Math.pow(setC_variance, 0.5)) * MAX_SCORE;
				setC_OntResources.get(i).setScore(normalizedScore);
			} else {
				setC_OntResources.get(i).setScore(0.0);
			}
			sortedSet.add(setC_OntResources.get(i));
		}

		sortedSet = boostDuplicates(sortedSet);
		Collections.sort(sortedSet);
		Collections.reverse(sortedSet);

		return sortedSet;
	}

	public eu.iksproject.fise.stores.persistencestore.rest.model.search.Result performQuery(
			eu.iksproject.fise.stores.persistencestore.rest.model.search.Query query) {
		try {
			eu.iksproject.fise.stores.persistencestore.rest.model.search.ObjectFactory objectFactory = new eu.iksproject.fise.stores.persistencestore.rest.model.search.ObjectFactory();
			Result result = objectFactory.createResult();

			// FIXME:: the following is never used;
			int maxResults = -1;
			if (query.getMaxResults() != null) {
				maxResults = query.getMaxResults().intValue();
			}

			int flexibilityClasses = 1;
			if (query.getFlexibilityClasses() != null
					&& query.getFlexibilityClasses().intValue() > 0) {
				flexibilityClasses = query.getFlexibilityClasses().intValue();
			}
			int flexibilityIndividuals = 0;
			if (query.getFlexibilityIndividuals() != null
					&& query.getFlexibilityIndividuals().intValue() > 0) {
				flexibilityIndividuals = query.getFlexibilityIndividuals()
						.intValue();
			}

			// combined keywordList
			KeywordList keywordList = objectFactory.createKeywordList();
			// keywordList coming from query arguments
			KeywordList queryKeywordList = query.getKeywordList();
			StructuralQueryPart structuralQueryPart = query
					.getStructuralQueryPart();
			FullTextSearchResultList fullTextSearchResultList = query
					.getFullTextSearchResultList();

			Vector<String> keywords = new Vector<String>();
			extractKeywordArray(query.getKeywordList(), keywords);
			List<ScoredWordnetResource> scoredWordnetResources = WordnetClient
					.getInstance().getScoredWordnetResources(
							keywords.toArray(new String[keywords.size()]),
							MAX_SCORE, DEGRADING_COEFFICIENT,
							WORDNET_EXPANSION_LEVEL);
			Collections.sort(scoredWordnetResources);
			Collections.reverse(scoredWordnetResources);

			// using synonyms in ontology lookup is optional
			if (query.isUseSynonymsInOntologyLookup() == true) {
				// append the words received from WordNET into keywordList
				KeywordList wordnetKeywordList = objectFactory
						.createKeywordList();
				wordnetKeywordList.setOperator(OperatorType.OR);
				for (int i = 0; i < scoredWordnetResources.size(); i++) {
					wordnetKeywordList.getKeywordListOrKeyword().add(
							scoredWordnetResources.get(i).getWordnetResource()
									.getName());
				}
				// combine queryKeywordList with wordnetKeywordList

				keywordList.setOperator(OperatorType.AND);
				keywordList.getKeywordListOrKeyword().add(wordnetKeywordList);
			}
			keywordList.getKeywordListOrKeyword().add(queryKeywordList);

			List<ScoredOntResource> setA_OntResources = getPathRelatedResources(
					fullTextSearchResultList.getFullTextSearchResult(),
					flexibilityIndividuals, flexibilityClasses, query
							.isUsedbPediaForFindingSimilarContent());
			List<ScoredOntResource> setB_OntResources = getKeywordRelatedResources(
					keywordList, flexibilityIndividuals, flexibilityClasses,
					query.isUsedbPediaForFindingSimilarContent());
			List<ScoredOntResource> setC_SelectiveOntResources = new Vector<ScoredOntResource>();
			List<ScoredOntResource> setC_NotSelectiveOntResources = new Vector<ScoredOntResource>();
			List<ScoredOntResource> setC_ExcludedOntResources = new Vector<ScoredOntResource>();
			if (structuralQueryPart.getSPARQLQuery() != null) {
				// FIXME:: to be implemented
				// getQueryRelatedResources has to be extended with OR, EXCLUDE,
				// NOT_SELECTIVE operators
				setC_SelectiveOntResources.addAll(getQueryRelatedResources(
						structuralQueryPart.getSPARQLQuery(),
						flexibilityIndividuals, flexibilityClasses, query
								.isUsedbPediaForFindingSimilarContent()));

			} else if (structuralQueryPart.getResourceList() != null) {
				ResourceList resourceList = structuralQueryPart
						.getResourceList();
				for (int i = 0; i < resourceList.getSelectiveResource().size(); i++) {
					SelectiveResource sr = resourceList.getSelectiveResource()
							.get(i);
				}
				setC_SelectiveOntResources = getSelectionRelatedSelectiveResources(
						resourceList, flexibilityIndividuals,
						flexibilityClasses, query
								.isUsedbPediaForFindingSimilarContent());
				setC_NotSelectiveOntResources = getSelectionRelatedNotSelectiveResources(
						resourceList, flexibilityIndividuals,
						flexibilityClasses, query
								.isUsedbPediaForFindingSimilarContent());
				setC_ExcludedOntResources = getSelectionRelatedExcludedResources(
						resourceList, 0, 0, query
								.isUsedbPediaForFindingSimilarContent());
			}
			// now take into account the excluded resources
			setA_OntResources = applyOperator(setA_OntResources,
					OperatorType.EXCLUDE, setC_ExcludedOntResources);
			setB_OntResources = applyOperator(setB_OntResources,
					OperatorType.EXCLUDE, setC_ExcludedOntResources);

			// then the not_selective resources
			setA_OntResources = applyOperator(setA_OntResources,
					OperatorType.NOT_SELECTIVE, setC_NotSelectiveOntResources);
			setB_OntResources = applyOperator(setB_OntResources,
					OperatorType.NOT_SELECTIVE, setC_NotSelectiveOntResources);

			// then boost the scores based on selected ont scores
			List<ScoredOntResource> mergedSet = normalizeAndMergeSets(
					setA_OntResources, setB_OntResources,
					setC_SelectiveOntResources);

			// generate the structure TopRelatedOntologyResources
			TopRelatedOntologyResources topRelatedOntologyResources = objectFactory
					.createTopRelatedOntologyResources();

			// generate the structure ReturnedOntologyResources
			ReturnedOntologyResources returnedOntologyResources = objectFactory
					.createReturnedOntologyResources();
			List<ClassResource> listClassResource = returnedOntologyResources
					.getClassResource();
			Hashtable<String, ClassResource> tempHashtable = new Hashtable<String, ClassResource>();
			SortedSet<Double> scoreSet = new TreeSet<Double>();
			for (int i = 0, j = 0; i < mergedSet.size(); i++) {
				double score = mergedSet.get(i).getScore();

				OntResource ontResource = mergedSet.get(i).getOntResource();
				// you will use scoreSet to eliminate the documents/classes
				// that belong to have the minimum score
				if (ontResource instanceof Individual) {
					scoreSet.add(score);
				} else if (ontResource instanceof OntClass
						&& !((OntClass) ontResource).getURI().equalsIgnoreCase(
								OWL.Nothing.getURI())
						&& !((OntClass) ontResource).getURI().equalsIgnoreCase(
								OWL.Thing.getURI())
						&& !containsStringInIgnoreList(((OntClass) ontResource)
								.getURI())) {
					ClassResource classResource = objectFactory
							.createClassResource();
					classResource.setClassURI(ontResource.getURI());
					classResource.setScore((float) score);
					tempHashtable.put(ontResource.getURI(), classResource);
					// System.out.println("put " + ontResource.getURI());
					// listClassResource.add(classResource);

					// Anil
					if (j < TOP_RELATED_ONTOLOGY_RESOURCES_COUNT) {
						ClassResource topClassResource = objectFactory
								.createClassResource();
						topClassResource.setClassURI(ontResource.getURI());
						topClassResource.setScore((float) score);
						topRelatedOntologyResources.getClassResource().add(
								topClassResource);
						j++;
					}
				}
			}

			double scoreThreshold = Double.MIN_VALUE;
			if (scoreSet.size() >= 3) {
				// then it is logical to higher the scoreThreshold to the min
				// score in the list
				scoreThreshold = scoreSet.first().doubleValue();
			}
			result.setTopRelatedOntologyResources(topRelatedOntologyResources);

			// to store those class resources that have parents; we will use it
			// to find those class resources that do not have parents
			// they will form the root resources in ReturnedOntologyResources
			// list
			// List<ClassResource> linkedClassResources = new
			// Vector<ClassResource>();
			Hashtable<ClassResource, ClassResource> parentClassResources = new Hashtable<ClassResource, ClassResource>();
			for (int i = 0; i < mergedSet.size(); i++) {
				OntResource ontResource = mergedSet.get(i).getOntResource();
				double score = mergedSet.get(i).getScore();
				if (ontResource instanceof OntClass
						&& !((OntClass) ontResource).getURI().equalsIgnoreCase(
								OWL.Nothing.getURI())
						&& !((OntClass) ontResource).getURI().equalsIgnoreCase(
								OWL.Thing.getURI())
						&& !containsStringInIgnoreList(((OntClass) ontResource)
								.getURI())) {
					OntClass superOntClass = (OntClass) ontResource;
					ExtendedIterator subOntClasses = superOntClass
							.listSubClasses(true);
					while (subOntClasses.hasNext()) {
						OntClass subOntClass = (OntClass) subOntClasses.next();
						if (subOntClass.getURI() != null
								&& tempHashtable.containsKey(subOntClass
										.getURI())) {
							ClassResource superClassResource = tempHashtable
									.get(superOntClass.getURI());
							ClassResource subClassResource = tempHashtable
									.get(subOntClass.getURI());
							superClassResource.getClassResource().add(
									subClassResource);
							parentClassResources.put(subClassResource,
									superClassResource);
						}
					}
				}
			}

			for (int i = 0; i < mergedSet.size(); i++) {
				OntResource ontResource = mergedSet.get(i).getOntResource();
				double score = mergedSet.get(i).getScore();
				if (ontResource instanceof OntClass
						&& !((OntClass) ontResource).getURI().equalsIgnoreCase(
								OWL.Nothing.getURI())
						&& !((OntClass) ontResource).getURI().equalsIgnoreCase(
								OWL.Thing.getURI())
						&& !containsStringInIgnoreList(((OntClass) ontResource)
								.getURI())) {
					OntClass ontClass = (OntClass) ontResource;
					ClassResource classResource = tempHashtable.get(ontClass
							.getURI());
					while (parentClassResources.containsKey(classResource)) {
						classResource = parentClassResources.get(classResource);
					}
					if (!listClassResource.contains(classResource)) {
						listClassResource.add(classResource);
					}
				}
			}
			result.setReturnedOntologyResources(returnedOntologyResources);
			// System.out.println("returnedOntologyResources");

			// generate structure for ReturnedDocuments
			ReturnedDocuments returnedDocuments = objectFactory
					.createReturnedDocuments();
			for (int i = 0; i < mergedSet.size(); i++) {
				OntResource ontResource = mergedSet.get(i).getOntResource();
				double score = mergedSet.get(i).getScore();
				if (ontResource instanceof Individual
						&& getContentRepositoryPathForIndividual((Individual) ontResource) != null) {
					Individual individual = (Individual) ontResource;
					String documentXPath = getContentRepositoryPathForIndividual(individual);
					Document document = objectFactory.createDocument();
					document.setDocumentXPath(documentXPath);
					document.setScore((float) mergedSet.get(i).getScore());
					Property property = individual
							.getOntModel()
							.getProperty(
									CONTENT_INSTNACE_PRIMARYTYPE_PROPERTY_PREFIX
											+ CONTENT_INSTNACE_PRIMARYTYPE_PROPERTY_URI);
					if (property != null
							&& individual.getPropertyValue(property) != null) {
						RDFNode rdfNode = individual.getPropertyValue(property);
						document.setPrimaryType(rdfNode.toString());
					}
					Document.RelatedTo relatedTo = new Document.RelatedTo();
					ExtendedIterator ontClassesItr = individual
							.listOntClasses(false);
					while (ontClassesItr.hasNext()) {
						OntClass relatedOntClass = (OntClass) ontClassesItr
								.next();
						if (tempHashtable.containsKey(relatedOntClass.getURI())) {
							relatedTo.getClassURI().add(
									relatedOntClass.getURI());
						}
					}
					if (relatedTo.getClassURI().size() > 0) {
						document.setRelatedTo(relatedTo);
					}

					Document.Metadata metadata = new Document.Metadata();
					StmtIterator propertiesItr = individual.listProperties();
					while (propertiesItr.hasNext()) {
						Statement curStmt = (Statement) propertiesItr.next();
						Property curProperty = curStmt.getPredicate();
						RDFNode rdfNode = curStmt.getObject();
						if (curProperty != null && curProperty.getURI() != null
								&& rdfNode != null) {
							Document.Metadata.NameValuePair nameValuePair = new Document.Metadata.NameValuePair();
							nameValuePair.setName(curProperty.getLocalName());
							nameValuePair.setValue(rdfNode.toString());
							metadata.getNameValuePair().add(nameValuePair);
						}
					}
					if (metadata.getNameValuePair().size() > 0) {
						document.setMetadata(metadata);
					}
					returnedDocuments.getDocument().add(document);
				}
			}
			result.setReturnedDocuments(returnedDocuments);
			// System.out.println("returnedDocuments");

			// generate structure for ReturnedWordnetResources
			ReturnedWordnetResources returnedWordnetResources = objectFactory
					.createReturnedWordnetResources();
			for (ScoredWordnetResource scoredWordnetResource : scoredWordnetResources) {
				eu.iksproject.fise.stores.persistencestore.rest.model.search.WordnetResource wordnetResource = objectFactory
						.createWordnetResource();
				wordnetResource.setName(scoredWordnetResource
						.getWordnetResource().getName());
				wordnetResource.setScore((float) scoredWordnetResource
						.getScore());
				returnedWordnetResources.getWordnetResource().add(
						wordnetResource);
			}
			result.setReturnedWordnetResources(returnedWordnetResources);

			// Anil & Tuncay
			result.setReturnedDBPediaResources(DBPediaClient.getInstance()
					.getScoredDBPediaResources(mergeKeywords(keywords),
							MAX_SCORE, DEGRADING_COEFFICIENT));

			return result;
		} catch (Exception e) {
			logger.error("Error ", e);
		}
		return null;
	}

	private String mergeKeywords(Vector<String> keywords) {
		StringBuilder sb = new StringBuilder();
		for (String str : keywords) {
			sb.append(str);
			sb.append(' ');
		}
		return sb.toString().trim();
	}

	private void extractKeywordArray(
			eu.iksproject.fise.stores.persistencestore.rest.model.search.KeywordList keywordList,
			Vector<String> keywords) {

		for (Object obj : keywordList.getKeywordListOrKeyword()) {
			if (obj instanceof String) {
				keywords.add((String) obj);
			} else {
				extractKeywordArray(
						(eu.iksproject.fise.stores.persistencestore.rest.model.search.KeywordList) obj,
						keywords);
			}
		}
	}
}

