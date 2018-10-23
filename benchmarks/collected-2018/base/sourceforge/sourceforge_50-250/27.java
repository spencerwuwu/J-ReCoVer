// https://searchcode.com/api/result/14208379/


/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.decoder.linguist;
import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.decoder.linguist.SentenceHMMState;
import edu.cmu.sphinx.decoder.linguist.Grammar;



/**
 * Provides language model services. 
 *
 */
public interface Linguist {
    /**
     * Prefix for search.Linguist SphinxProperties.
     */
    public final static String PROP_PREFIX =
	"edu.cmu.sphinx.decoder.linguist.Linguist.";

    /**
      * Word insertion probability property
      */
    public final static String PROP_WORD_INSERTION_PROBABILITY
	    = PROP_PREFIX + "wordInsertionProbability";
    /**
      * Unit insertion probability property
      */
    public final static String PROP_UNIT_INSERTION_PROBABILITY
	    = PROP_PREFIX + "unitInsertionProbability";

    /**
      * Silence insertion probability property
      */
    public final static String PROP_SILENCE_INSERTION_PROBABILITY
		= PROP_PREFIX + "silenceInsertionProbability";


    /**
      * Property to control whether or not alternative states are
      * inserted into the sentence hmm or not.
      */
    public final static String PROP_INSERT_ALTERNATIVE_STATES
		= PROP_PREFIX + "insertAlternativeStates";

    /**
     * Property to control the maximum number of right contexts to
     * consider before switching over to using composite hmms
     */
    public final static String PROP_COMPOSITE_THRESHOLD = PROP_PREFIX +
	"compositeThreshold";

    /**
     * Property to control whether pronunciations subtrees are
     * re-joined to reduce fan-out
     */
    public final static String PROP_JOIN_PRONUNCIATIONS = PROP_PREFIX +
	"joinPronunciations";

    /**
     * Property that controls whether word probabilities are spread
     * across all pronunciations.
     */
    public final static 
        String PROP_SPREAD_WORD_PROBABILTIES_ACROSS_PRONUNCIATIONS =
            PROP_PREFIX + "spreadWordProbabilitiesAcrossPronunciations";

    /**
     * Property to control whether silence units are automatically
     * looped to allow for longer silences
     */
    public final static String PROP_AUTO_LOOP_SILENCES = PROP_PREFIX +
	"autoLoopSilences";

    /**
     * Property to control the the dumping of the sentence HMM
     */
    public final static String PROP_SHOW_SENTENCE_HMM = PROP_PREFIX +
	"showSentenceHMM";

    /**
     * Property to control the the validating of the sentence HMM
     */
    public final static String PROP_VALIDATE_SENTENCE_HMM = PROP_PREFIX +
	"validateSentenceHMM";

    /**
     * Property to control whether contexts are considered across
     * grammar node boundaries
     */
    public final static String PROP_EXPAND_INTER_NODE_CONTEXTS = PROP_PREFIX +
	"expandInterNodeContexts";

    /**
     * Property to control whether compilation progress is displayed
     * on stdout. If this property is true, a 'dot' is displayed for
     * every 1000 sentence hmm states added to the SentenceHMM
     */
    public final static String PROP_SHOW_COMPILATION_PROGRESS = PROP_PREFIX +
	"showCompilationProgress";

    /**
     * Initializes this linguist
     *
     * @param context the context to associate this linguist with
     * @param languageModel the language model
     * @param grammar the grammar for this linguist
     * @param models the acoustic model(s) used by this linguist,
     *    normally there is only one AcousticModel, but it is possible
     *    for the Linguist to use multiple AcousticModel(s)
     */
    public void initialize(String context,
			   LanguageModel languageModel,
			   Grammar grammar,
			   AcousticModel[] models) ;

    /**
     * Retrieves initial SentenceHMMState
     * 
     * @return the set of initial SentenceHMMState
     */
    public SentenceHMMState getInitialState();


    /**
     * Called before a recognitino
     */
    public void start();

    /**
     * Called after a recognition
     */
    public void stop();


    /**
     * Retrieves the language model for this linguist
     *
     * @return the language model (or null if there is none)
     */
    public LanguageModel getLanguageModel();
}


