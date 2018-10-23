// https://searchcode.com/api/result/12786165/

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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author bbrown
 */
public interface IStringUtils {
    		
	public Map<String, Integer> sortMapByValue(Map inputMap);
	
	public List<String> buildWordList(final String document);
	
	/**
	 * Return a list of key value (instances of map) pairs.
	 * 
	 * @param inputMap
	 * @return
	 */
	public Set<Map.Entry<String, Integer>> keyValueSet(final Map<String, Integer> inputMap, final int maxnum);
	
	/**
	 * Simple Map Reduce; given a list of keywords, map the terms to a count of how
	 * many times the term occurs in the list.
	 *  
	 * @param allterms
	 * @return
	 */
	public Set<Map.Entry<String, Integer>> mapReduce(final List<String> allterms, final int maxnum);
	   
    /**
     * Simple Map Reduce; given a list of keywords, map the terms to a count of how
     * many times the term occurs in the list.
     *  
     * @param allterms
     * @return
     */
    public Set<Map.Entry<String, Integer>> mapReduceWithStopWords(final List<String> allterms, final int maxnum);
	
    /**
     * Simple Map Reduce; given a list of keywords, map the terms to a count of how
     * many times the term occurs in the list.
     *  
     * @param allterms
     * @return
     */
    public Set<Map.Entry<String, Integer>> mapReduceWithStopWords(final List<String> allterms, final int maxnum, final Map<String, String> stopWords);
    
	/**
     * Simple Map Reduce; given a list of keywords, map the terms to a count of how
     * many times the term occurs in the list.
     *  
     * @param allterms
     * @return
     */
    public Double [] mapReduceCount(final List<String> allterms, final int maxnum); 
    
    public Double [] mapReduceCount(final Set<Map.Entry<String, Integer>> set, final int maxnum);
    
    /**
     * Get the word length
     * 
     * @param allterms
     * @param maxnum
     * @return
     */
    public Double [] mapReduceWordSize(final List<String> allterms, final int maxnum);
    
    public Double [] mapReduceWordSize(final Set<Map.Entry<String, Integer>> set, final int maxnum);
        
    /**
     * @return the stopWords
     */
    public Map<String, String> getStopWords();


    /**
     * @param stopWords the stopWords to set
     */
    public void setStopWords(Map<String, String> stopWords); 
    
} // End of the Class //

