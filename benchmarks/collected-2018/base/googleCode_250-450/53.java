// https://searchcode.com/api/result/12786169/

/**
 * Copyright (c) 2006-2010 Berlin Brown and botnode.com/Berlin Research  All Rights Reserved
 *
 * http://www.opensource.org/licenses/bsd-license.php

 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:

 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * * Neither the name of the Botnode.com (Berlin Brown) nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Date: 1/23/2010 
 * Description: Social Networking Site Document Analysis
 * Home Page: http://botnode.com/
 * 
 * Contact: Berlin Brown <berlin dot brown at gmail.com>
 */
package org.bresearch.websec.utils.botlist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author bbrown
 */
public class BotlistStringUtils implements IStringUtils {

	public static final Map<String, String> STOP_WORDS_MAP;
		
	private Map<String, String> stopWords = STOP_WORDS_MAP;
	
	/** 
	 * inner class to sort map. 
	 */
	private static class ValueComparator implements Comparator<Object> {
	    
		private Map<String, Integer> data = null;
		public ValueComparator(Map<String, Integer> _data) {
			super();
			this.data = _data;
		}
		public int compare(Object o1, Object o2) {
			Integer e1;
			Integer e2;
			e1 = (Integer) this.data.get(o1);
			e2 = (Integer) this.data.get(o2);			
			int res = e1.compareTo(e2);			
			return -(res == 0 ? 1 : res);
		}
	}
	
	public Map<String, Integer> sortMapByValue(Map inputMap) {
		SortedMap sortedMap = new TreeMap(new BotlistStringUtils.ValueComparator(inputMap));		
		sortedMap.putAll(inputMap);		
		return sortedMap;
	}

	
	public List<String> buildWordList(final String document) {
	    
	    if (document == null) {
	        // Return empty list.
	        return new ArrayList<String>();
	    }
	    final String [] words = document.split("\\s");	     	 
	    final List<String> list = new ArrayList<String>();
	    for (String word : words) {
	        list.add(word);
	    } // End of the For //
	    return list;
	}
	
	/**
	 * Return a list of key value (instances of map) pairs.
	 * 
	 * @param inputMap
	 * @return
	 */
	public Set<Map.Entry<String, Integer>> keyValueSet(final Map<String, Integer> inputMap, final int maxnum) {
	    
		Set<Map.Entry<String, Integer>> set = inputMap.entrySet();
		Set<Map.Entry<String, Integer>> newset = new LinkedHashSet<Map.Entry<String, Integer>>();		
		int i = 0;
		for (Iterator<Map.Entry<String, Integer>> it = set.iterator(); it.hasNext(); i++) {		    
			newset.add(it.next());			
			if ((maxnum >= 0) && (i >= (maxnum - 1))) {			    
			    break;
			} // End of the If //		
		} // End of the for //
		
		return newset;
	}
	
	/**
	 * Simple Map Reduce; given a list of keywords, map the terms to a count of how
	 * many times the term occurs in the list.
	 *  
	 * @param allterms
	 * @return
	 */
	public Set<Map.Entry<String, Integer>> mapReduce(final List<String> allterms, final int maxnum) {
	    
		final Map<String, Integer> map = new HashMap<String, Integer>();
		for (Iterator<String> x2it = allterms.iterator(); x2it.hasNext();) {
		    
			final String term = (String) x2it.next();
			if (term.length() == 0) {
			    continue;
			}
			Integer ct = (Integer) map.get(term);
			if (ct == null) {
				map.put(term, new Integer(1));
			} else {
				map.put(term, new Integer(ct.intValue() + 1));
			} // End of if - else
			
		} // End of the for		
		final Map<String, Integer> sortedMap = this.sortMapByValue(map);	
		return this.keyValueSet(sortedMap, maxnum);
	}

	   
    /**
     * Simple Map Reduce; given a list of keywords, map the terms to a count of how
     * many times the term occurs in the list.
     *  
     * @param allterms
     * @return
     */
    public Set<Map.Entry<String, Integer>> mapReduceWithStopWords(final List<String> allterms, final int maxnum) {
        return mapReduceWithStopWords(allterms, maxnum, this.getStopWords());
    }
	
    /**
     * Simple Map Reduce; given a list of keywords, map the terms to a count of how
     * many times the term occurs in the list.
     *  
     * @param allterms
     * @return
     */
    public Set<Map.Entry<String, Integer>> mapReduceWithStopWords(final List<String> allterms, final int maxnum, final Map<String, String> stopWords) {
        
        final Map<String, Integer> map = new HashMap<String, Integer>();
        for (Iterator<String> x2it = allterms.iterator(); x2it.hasNext();) {
            
            final String term = (String) x2it.next();
            if (term.length() == 0) {
                continue;
            }
            
            // Check the stop word //
            if (stopWords.get(term) != null) {
                continue;
            } // End of the if //
            
            Integer ct = (Integer) map.get(term);
            if (ct == null) {
                map.put(term, new Integer(1));
            } else {
                map.put(term, new Integer(ct.intValue() + 1));
            } // End of if - else
            
        } // End of the for     
        final Map<String, Integer> sortedMap = this.sortMapByValue(map);    
        return this.keyValueSet(sortedMap, maxnum);
        
    }
    
	/**
     * Simple Map Reduce; given a list of keywords, map the terms to a count of how
     * many times the term occurs in the list.
     *  
     * @param allterms
     * @return
     */
    public Double [] mapReduceCount(final List<String> allterms, final int maxnum) {
                 
        final Set<Map.Entry<String, Integer>> set = mapReduce(allterms, maxnum);
        return mapReduceCount(set, maxnum);
    }
    
    public Double [] mapReduceCount(final Set<Map.Entry<String, Integer>> set, final int maxnum) {
        
        final List<Double> resList = new ArrayList<Double>();
        // Iterate through the values and convert the values into
        // a set of doubles.        
        for (Map.Entry<String, Integer> entry : set) {
            resList.add(entry.getValue().doubleValue());
        }               
        return resList.toArray(new Double[resList.size()]);
    }

    /**
     * Get the word length
     * 
     * @param allterms
     * @param maxnum
     * @return
     */
    public Double [] mapReduceWordSize(final List<String> allterms, final int maxnum) {
                 
        final Set<Map.Entry<String, Integer>> set = mapReduce(allterms, maxnum);        
        return this.mapReduceWordSize(set, maxnum);
    }
    
    public Double [] mapReduceWordSize(final Set<Map.Entry<String, Integer>> set, final int maxnum) {
        final List<Double> resList = new ArrayList<Double>();
        // Iterate through the values and convert the values into
        // a set of doubles.        
        for (Map.Entry<String, Integer> entry : set) {
            double l = (double) entry.getKey().toString().length();
            resList.add(l);
        }               
        return resList.toArray(new Double[resList.size()]);
    }

    /**
     * @return the stopWords
     */
    public Map<String, String> getStopWords() {
        return stopWords;
    }


    /**
     * @param stopWords the stopWords to set
     */
    public void setStopWords(Map<String, String> stopWords) {
        this.stopWords = stopWords;
    }
    
    public static final String STOP_WORDS [] = {
        "to",
        "the",
        "in",
        "of",
        "am",       
        "is",
        "for",
        "a",
        "on",
        "by",           
        "be",
        "from",
        "too",      
        "and",      
        "i",
        "with",
        "it",
        "all",      
        "at",                       
        "no",
        "this",
        "that",
        "you",
        "my",
        "are",
        "how",
        "do",
        "what",
        "not",
        "any",
        "their",
        "his",
        "we",
        "he",
        "has",
        "was",
        "be",
        "at",
        "one",
        "have",
        "this",
        "from",
        "or",
        "had",
        "by",       
        "but",
        "some",
        "what",
        "there",
        "we",
        "can",
        "out",
        "other",
        "were",
        "all",
        "your",
        "when",
        "or",
        "our",
        "such",
        "which",
        "may",
        "an",
        "also",
        "us",
        "as",
        "so",
        "s",
        "me"
    };
        
    static {
        STOP_WORDS_MAP = new HashMap<String, String>();
        for (int ix = 0; ix < STOP_WORDS.length; ix++) {
            STOP_WORDS_MAP.put(STOP_WORDS[ix], "0");
        }
    } // End of Static Block //
    
} // End of the Class //

