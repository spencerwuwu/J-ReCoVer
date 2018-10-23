// https://searchcode.com/api/result/53923647/

package de.uni_leipzig.asv.splitting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.uni_leipzig.asv.utils.Pretree;

/**
 * This implements the algorithm of the following paper:
 * 
 * http://wortschatz.uni-leipzig.de/~cbiemann/pub/2005/WitschelBiemannNodalida05.pdf
 * 
 * @author Julian Moritz, email [at] julianmoritz [dot] de
 *
 */
public class CompoundSplitter {

	private static Logger logging = Logger.getLogger(CompoundSplitter.class.getName());
	private Pretree forwardsplittingtree;
	private Pretree backwardsplittingtree;
	private double threshold = 0.3;
	private Map<String, List<String>> cachingMap;
	private static CompoundSplitter instance;
	private BaseformReducer baseformreducer;
	
	/**
	 * Simple constructor with three trees.
	 * @param forwardsplittingtree A tree which maps to the splitting point looked from the left.
	 * @param backwardsplittingtree A tree which maps to the splitting point looked from the right.
	 * @param reductiontree A tree which maps to reduction rules.
	 */
	public CompoundSplitter(Pretree forwardsplittingtree,
			Pretree backwardsplittingtree, Pretree reductiontree) {

		Enumeration<Appender> appenders = Logger.getRootLogger().getAllAppenders();
		int appendercount = 0;
		
		while(appenders.hasMoreElements()){
			
			appendercount += 1;
			appenders.nextElement();
			
		}
		
		if(appendercount == 0){
			
			BasicConfigurator.configure();
			Logger.getRootLogger().setLevel(Level.ERROR);	
			
		}
		
		this.forwardsplittingtree = forwardsplittingtree;
		this.backwardsplittingtree = backwardsplittingtree;
		this.baseformreducer = new BaseformReducer(reductiontree);

		this.setThreshold(this.threshold);
		this.cachingMap = new HashMap<String, List<String>>();
		
	}
	
	/**
	 * Simple constructor using paths to trees.
	 * @param forwardsplittingpath A tree which maps to the splitting point looked from the left.
	 * @param backwardsplittingpath A tree which maps to the splitting point looked from the right.
	 * @param reductionpath A tree which maps to reduction rules.
	 * @throws Exception If any of the files cannot be read, there's an exception thrown.
	 */
	public CompoundSplitter(String forwardsplittingpath,
			String backwardsplittingpath, String reductionpath) throws Exception {

		Enumeration<Appender> appenders = Logger.getRootLogger().getAllAppenders();
		int appendercount = 0;
		
		while(appenders.hasMoreElements()){
			
			appendercount += 1;
			appenders.nextElement();
			
		}
		
		if(appendercount == 0){
			
			BasicConfigurator.configure();
			Logger.getRootLogger().setLevel(Level.ERROR);	
		}
		
		if(forwardsplittingpath == null || backwardsplittingpath == null || reductionpath == null){
			throw new NullPointerException("None of the arguments may be null.");
			
		}
		
		Pretree forw = new Pretree();
		forw.load(forwardsplittingpath);
		Pretree backw = new Pretree();
		backw.load(backwardsplittingpath);
		
		this.forwardsplittingtree = forw;
		this.backwardsplittingtree = backw;
		this.baseformreducer = new BaseformReducer(reductionpath);
		
		this.setThreshold(this.threshold);
		
		this.cachingMap = new HashMap<String, List<String>>();
	}

	/**
	 * Singleton method.
	 * @param forwardsplittingpath A tree which maps to the splitting point looked from the left.
	 * @param backwardsplittingpath A tree which maps to the splitting point looked from the right.
	 * @param reductionpath A tree which maps to reduction rules.
	 * @throws Exception If any of the files cannot be read, there's an exception thrown.
	 * @return Returns a singleton instance.
	 */
	public static CompoundSplitter getInstance(String forwardsplittingpath,
			String backwardsplittingpath, String reductionpath) throws Exception{
		
		if(instance == null){
			instance = new CompoundSplitter(forwardsplittingpath, backwardsplittingpath, reductionpath);			
		}
		
		return instance;
		
	}
	/**
	 * Method to create singelton instance. 
	 * @param forwardsplittingtree A tree which maps to the splitting point looked from the left.
	 * @param backwardsplittingtree A tree which maps to the splitting point looked from the right.
	 * @param reductiontree A tree which maps to reduction rules.
	 * @return Returns a singleton instance.
	 */
	public static CompoundSplitter getInstance(Pretree forwardsplittingtree,
			Pretree backwardsplittingtree, Pretree reductiontree){
		
		if(instance == null){
			instance = new CompoundSplitter(forwardsplittingtree, backwardsplittingtree, reductiontree);			
		}
		
		return instance;
	}
	
	/**
	 * Sets the threshold of the three trees.
	 * @param threshold Threshold to be set.
	 */
	public void setThreshold(double threshold) {

		if (threshold < 0 || threshold > 1) {

			logging.warn("threshold was not between 0 and 1: " + threshold);
			return;

		}

		this.threshold = threshold;
		this.forwardsplittingtree.setThresh(threshold);
		this.backwardsplittingtree.setThresh(threshold);
		this.baseformreducer.setThreshold(threshold);

	}
	
	/**
	 * Returns the threshold.
	 * @return Returns the threshold which was set.
	 */
	public double getThreshold() {

		return this.threshold;

	}

	/**
	 * Split a word into parts.
	 * @param word Word to be split.
	 * @param reduce Set to true if the parts should be reduced to their baseforms.
	 * @return Returns a list with parts.
	 */
	public List<String> split(String word, boolean reduce){
		
		List<String> words = this.iterativeSplit(word);

		if (reduce == true) {
			return this.reduceWords(words);
		} else {

			return words;
		}
		
	}
	
	/**
	 * Split a word into parts.
	 * @param word Wort to be split.
	 * @return REturns a list with parts.
	 */
	public List<String> split(String word) {

		return this.split(word, true);

	}
	
	protected List<String> reduceWords(List<String> words) {

		List<String> retlist = new LinkedList<String>();
		
		for(String inflword : words){
			
			retlist.add(this.reduceWord(inflword));
			
			
		}
		
		return retlist;

	}
	
	protected String reduceWord(String word){
		
		return this.baseformreducer.reduceWord(word);
		
	}
			
	protected List<String> iterativeSplit(String word){
		
		if(this.cachingMap.containsKey(word)){
			
			return this.cachingMap.get(word);
			
		}
		
		List<String> tlist1 = new LinkedList<String>();
		List<String> tlist2 = new LinkedList<String>();
		List<String> tlist3 = new LinkedList<String>();
		tlist1.add(word);
		boolean splitted;
		while(true){
			splitted = false;
			for(String tempword : tlist1){
				
				
				tlist3 = this.splitOnce(tempword);
				
				if(tlist3.size() > 1){
					
					splitted = true;
					
				}
				tlist2.addAll(tlist3);
			
			}
			
			if(splitted == false){
				
				this.storeToCachingMap(word, tlist1);
				return tlist1;
				
			}else{
				
				tlist1.clear();
				tlist1.addAll(tlist2);
				tlist2.clear();
				
			}
			
		}
		
				
	}
	
	protected List<String> splitOnce(String word){
		this.logging.debug("splitting: " + word);
		
		if(this.cachingMap.containsKey(word)){
			
			return this.cachingMap.get(word);
			
		}
		
		List<String> retlist = new LinkedList<String>();
		
		int forw;
		double probforw = -1.;
		int backw;
		double probbackw = -1.;
		try {
			forw = Integer.valueOf(this.forwardsplittingtree.classify(word))
					.intValue();
			probforw = this.forwardsplittingtree.getProbabilityForClass(word, String.valueOf(forw));
			
		} catch (NumberFormatException exc) {

			forw = -1;
			probforw = -1;

		}

		try {
			backw = Integer.valueOf(this.backwardsplittingtree.classify(word))
					.intValue();
			probbackw = this.backwardsplittingtree.getProbabilityForClass(word, String.valueOf(backw));
		} catch (NumberFormatException exc) {

			backw = -1;
			probbackw = -1;

		}

		//if one of the points is out of word boundaries:
		if(backw <= 0 || backw >= word.length()){
			this.logging.debug("backw is out of word boundaries");
			backw = -1;
			probbackw = -1;
			
		}
		
		if(forw <= 0 || forw >= word.length()){
			this.logging.debug("forw is out of word boundaries");
			forw = -1;
			probforw = -1;
			
		}
		
		//both are undecided or segmentation points are out of word bounds
		//don't segment
		if((forw <= 0 && backw <= 0) || (forw >= word.length() && backw >= word.length())){
			this.logging.debug("both are undecided or both are out of bounds");
			retlist.add(word);
			this.storeToCachingMap(word, retlist);
			return retlist;
			
		}
		
		//both predict the same point
		//segment at this point
		if(forw == word.length() - backw){
			this.logging.debug("both predict the same point");
			retlist.add(word.substring(0, forw));
			retlist.add(word.substring(forw));
			return retlist;		
						
		}
		
		//forward tree is undecided
		//segment at backwards trees point, if it is not out of word bounds
		if(forw == -1 && backw > -1){
			this.logging.debug("forw tree is undecided");
			this.logging.debug("backw says: " + backw);
			this.logging.debug("confidence is " + probbackw);
			retlist.add(word.substring(0, word.length()-backw));
			retlist.add(word.substring(word.length()-backw));
			return retlist;
			
		}
		
		//same for backward tree
		if(backw == -1 && forw > -1){
			this.logging.debug("backw is undecided");
			this.logging.debug("forw says: " + forw);
			retlist.add(word.substring(0, forw));
			retlist.add(word.substring(forw));
			return retlist;
			
		}
		
		if(probforw < 0.){
			
			probforw = this.forwardsplittingtree.getProbabilityForClass(word,
					new Integer(forw).toString());
			
		}
		
		if(probbackw < 0.){
			
			probbackw = this.backwardsplittingtree.getProbabilityForClass(word,
					new Integer(backw).toString());
			
		}		
		
		if(probbackw > probforw){
			this.logging.debug("probback is higher than probforw");
			retlist.add(word.substring(0, word.length()-backw));
			retlist.add(word.substring(word.length()-backw));
			return retlist;
			
			
		}
		
		if(probforw > probbackw){
			this.logging.debug("probforw is higher than probbackw");
			retlist.add(word.substring(0, forw));
			retlist.add(word.substring(forw));
			return retlist;
			
						
		}
		
		this.logging.debug("noone of the ones above");
		retlist.add(word);
		this.storeToCachingMap(word, retlist);
		return retlist;
		
		
	}
	
	private void storeToCachingMap(String word, List<String> complist){
		
		if(word.length() <= 8){
			
			this.cachingMap.put(word, complist);
			
		}
		
	}

	

}

