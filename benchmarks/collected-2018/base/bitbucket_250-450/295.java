// https://searchcode.com/api/result/127095145/

package org.archive.crawler.frontier;

import static org.archive.crawler.frontier.WsdlCostElement.CONTENT_KEYWORDS_REWARD;
import static org.archive.crawler.frontier.WsdlCostElement.DEFAULT_COST;
import static org.archive.crawler.frontier.WsdlCostElement.PARENT_CONTENT_REWARD;
import static org.archive.crawler.frontier.WsdlCostElement.PATH_SEGMENT_PENALTY;
import static org.archive.crawler.frontier.WsdlCostElement.RECURRING_ELEMENTS_PENALTY;
import static org.archive.crawler.frontier.WsdlCostElement.SUBDOMAIN_PENALTY;
import static org.archive.crawler.frontier.WsdlCostElement.URL_ENDS_WITH_WSDL_REWARD;
import static org.archive.crawler.frontier.WsdlCostElement.URL_KEYWORDS_REWARD;
import static org.archive.crawler.frontier.WsdlCostElement.URL_QUERY_PENALTY;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.logging.Logger;

import org.archive.crawler.framework.CrawlController;
import org.archive.modules.CrawlURI;
import org.archive.net.UURI;
import org.springframework.beans.factory.annotation.Autowired;

public class WsdlCostAssignmentPolicy extends CostAssignmentPolicy{

	private static final long serialVersionUID = 1L;
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	private final String KEYWORD_DELIMITER = ";";
	
	private int defaultCost;
	
	private int subDomainPenaltyWeight;
	
	private int queryStringPenaltyWeight;
	
    private int pathSegmentPenaltyWeight;
        
    private int recurringElementsPenaltyWeight;
    
    private int endsWithWsdlReward;
    
    private int urlKeywordsReward;
	
	private int parentKeywordsRewardWeight;
    
	private String urlKeywords;
	
	private String contentKeywords;
	
	protected CrawlController controller;
	
	{
        setDefaultCost(20);
        setSubDomainPenaltyWeight(1);
        setQueryStringPenaltyWeight(1);
        setPathSegmentPenaltyWeight(1);
        setRecurringElementsPenaltyWeight(3);
        setEndsWithWsdlReward(-5);
        setUrlKeywordsReward(-2);
        setParentKeywordsRewardWeight(1);
        setUrlKeywords("/webservice;/service;api;/ws");
        setContentKeywords("wsdl;webservice");
    }
	
    
    public CrawlController getCrawlController() {
        return this.controller;
    }
    @Autowired
    public void setCrawlController(CrawlController controller) {
        this.controller = controller;
    }
	
	@Override
	public Integer[] getCostElements(CrawlURI curi) {
		Integer[] costElements = new Integer[WsdlCostElement.values().length];
		
		// initial cost
		costElements[DEFAULT_COST.getValue()] = defaultCost;
		
		// Negative features:
		
		/*
		 *We penalize any subdomain different than www. E.g., neither example.com 
		 *nor www.example.com would get penalized, but ws.example.com would be penalized by +1
		 *and test.ws.example.com would even be penalized by +2.
		 */
		costElements[SUBDOMAIN_PENALTY.getValue()] = subdomainPenalty(curi);
		
		/*
		 * We penalize all URLs that have more than one path segment
		 * string. E.g. example.com/somePath/some would be penalized by +1. More path
		 * segments would accordingly be penalized by the number of path segments in
		 * the URL minus one.
		 */
		costElements[PATH_SEGMENT_PENALTY.getValue()] = pathSegmentPenalty(curi);
		
		/*
		 * We penalize all URLs that have more than one query string. E.g.
		 * ?a=b would not be penalized, whereas ?a=b&c=d and ?a=b&c=d&e=f would be
		 * penalized by +1 or +2, respectively.
		 */
		costElements[URL_QUERY_PENALTY.getValue()] = urlQueryPenalty(curi);
		
		/*
		 *To awoid some crawler traps, we add penalty for recurring url elements. 
		 *E.g.  example.com/somePath/some/example.com/somePath would receive a penalty
		 */
		costElements[RECURRING_ELEMENTS_PENALTY.getValue()] = recurringElementsPenalty(curi);
		
		// Positive features:
		
		//We reduce the costs by 5 for URLs that promise to be WSDL service descriptions,
		//i.e. URLs that contain the string ?wsdl.
		costElements[URL_ENDS_WITH_WSDL_REWARD.getValue()] = urlEndsWithWsdlReward(curi);
		
		//We reduce the costs by 2 for URLs that promise to be related to Web Services
		costElements[URL_KEYWORDS_REWARD.getValue()] = urlKeywordsReward(curi);
				
		// Rewards from parent's features 
				
		// check for keywords in parent's content
		costElements[PARENT_CONTENT_REWARD.getValue()] = parentsContentKeywordReward(curi);
				

		return costElements;
	}
   
	@Override
	public int costOf(CrawlURI curi) {
		//initial cost
		Integer cost = 0;
		for(Integer element : getCostElements(curi)){
			if(element!=null){
				cost += element;
			}
		}
		return cost;
	}
	
	/**
	 * Calculate parent's cost bonus
	 */
	protected Integer parentsUrlReward(CrawlURI curi){
		 Integer reward=null;
		 if(curi.getFullVia()==null){
			 return reward;
		 }
		 Integer[] parentCostElements = curi.getFullVia().getCostElements();
	     return parentCostElements[URL_KEYWORDS_REWARD.getValue()];
	}
	
	/**
	 * TODO: KeyWord search should be done in Extractor class during data extracting. 
	 * @param curi
	 * @return
	 */
	protected Integer parentsContentKeywordReward(CrawlURI curi){
		Integer reward=null;
		if(curi.getFullVia()==null){
			return reward;
		}
		CrawlURI parent = curi.getFullVia();
		Integer parentContentKeywordsReward = parent.getCostElements()[CONTENT_KEYWORDS_REWARD.getValue()];
		if(parentContentKeywordsReward!=null){
			return parentContentKeywordsReward;
		}
		if(parent.getRecorder()==null){
			return 0;
		}
		
		InputStreamReader reader=null;		
		try {
			reader = new InputStreamReader(parent.getRecorder().getContentReplayInputStream());
			int keywordcount = searchFromStream(reader);
			Integer parentKeywordsReward = -keywordcount * parentKeywordsRewardWeight;
			parent.getCostElements()[CONTENT_KEYWORDS_REWARD.getValue()]=parentKeywordsReward;
			return parentKeywordsReward;
		} catch (IOException e) {
			logger.throwing(this.getClass().getName(), "parentsContentKeywordsReward(CrawlURI curi)", e);
        } finally {
        	if (reader!=null){
        		try {
					reader.close();
				} catch (IOException e) {
					logger.throwing(this.getClass().getName(), "parentsContentKeywordsReward(CrawlURI curi)", e);
				}
        	}
        }
		return 0;
	}
	
	protected int urlKeywordsReward(CrawlURI curi){
	    String[] urlKeywords = getUrlKeywords().split(KEYWORD_DELIMITER);
		for(String keyword:urlKeywords){
	        if(curi.getURI().contains(keyword)){
	           return urlKeywordsReward;
	        }
	    }
	    return 0;
	}
		 
	protected Integer urlEndsWithWsdlReward(CrawlURI curi) {
		if(curi.getURI().toLowerCase().endsWith("?wsdl")){
			return endsWithWsdlReward;
		}
		return 0;
	}
	
	
	protected int subdomainPenalty(CrawlURI curi){
	    String uri = curi.getBaseURI().toString();
	    //no penalty for www subdomain
	    if(uri.indexOf("www")<8){
	    	return 0;
	    }
	    	    
	    int subDomains = countOccurence(uri, ".");
        
        //exception for uk top level domain
        if(!uri.endsWith("uk")){
            subDomains --;
        }
        int penalty = subDomains --;      
	    if(penalty > 0){
	    	return penalty * subDomainPenaltyWeight;
	    }
        return 0;
	}
	
	protected int pathSegmentPenalty(CrawlURI curi){
		String uri = curi.getBaseURI().toString();
	    if(uri.startsWith("http://")){
			uri = uri.substring(7);
		}
		int pathSegments = countOccurence(uri, "/");
		
		int result = (pathSegments-1)*pathSegmentPenaltyWeight;
		if(result > 0){
			return result;
		}
		return 0;
	}
	
	protected int recurringElementsPenalty(CrawlURI curi){
		if(curi==null || curi.getBaseURI()==null){
			return 0;
		}
		String uri = curi.getBaseURI().toString().toLowerCase();
	    if(uri.startsWith("http://")){
			uri = uri.substring(7);
		}
	    String[] elements = uri.split("/|\\.");
	    HashSet<String> set = new HashSet<String>();
	    int penatly = 0;
	    for(String element:elements){
	    	if(set.contains(element)){
	    		penatly += recurringElementsPenaltyWeight;
	    	}else{
	    		set.add(element);
	    	}
	    }
	    return penatly;
	}
		
	protected int urlQueryPenalty(CrawlURI curi) {
        int penalty = 0;
        UURI uuri = curi.getUURI();
        if (uuri.hasQuery()) {
            // has query string
            int qIndex = uuri.toString().indexOf('?');
            
            // more than 2 query-string attributes
            String pathQuery = uuri.toString().substring(qIndex);
            int attributesCount = countOccurence(pathQuery, "&");
            penalty+=attributesCount*queryStringPenaltyWeight;
        }
        return penalty;
    }
	
	/**
     * Utils method to count string element occurences in another string
     * efficiently. 
     * @param input
     * @param element
     * @return
     */
    private int countOccurence(String input, String element){
        if(input == null || element == null){
            return -1;
        }
        int count = 0;
        int index = 0;
        do {
            index = input.indexOf(element, index) +1;
            if(index>0){
                count ++; 
            }
        } while (index>0);
        return count;
    }
    
    /**
     * use java lang string search implementation
     * @throws IOException 
     */
    protected int searchFromStream(Reader reader) throws IOException{
    	String[] contentKeywords = getContentKeywords().split(KEYWORD_DELIMITER);
    	int count = 0;
        char[] buffer = new char[2048];
        String breakBuffer = ""; 
        while(reader.read(buffer) > 0) {
            String string = new String(buffer);
            string = string.toLowerCase();
            string = breakBuffer + string;
            for(String keyword : contentKeywords){
                int result = countOccurence(string, keyword);
                if(result>=0){
                    count+=result;
                    if(count>=5){
                    	return 5;
                    }
                }
            }
            breakBuffer = string.substring(string.length()-10);
        }
        return count;
    }
	
	public int getDefaultCost() {
		return defaultCost;
	}
	public void setDefaultCost(int defaultCost) {
		this.defaultCost = defaultCost;
	}
	public int getSubDomainPenaltyWeight() {
		return subDomainPenaltyWeight;
	}
	public void setSubDomainPenaltyWeight(int subDomainPenaltyWeight) {
		this.subDomainPenaltyWeight = subDomainPenaltyWeight;
	}
	public int getQueryStringPenaltyWeight() {
		return queryStringPenaltyWeight;
	}
	public void setQueryStringPenaltyWeight(int queryStringPenaltyWeight) {
		this.queryStringPenaltyWeight = queryStringPenaltyWeight;
	}
	public int getPathSegmentPenaltyWeight() {
		return pathSegmentPenaltyWeight;
	}
	public void setPathSegmentPenaltyWeight(int pathSegmentPenaltyWeight) {
		this.pathSegmentPenaltyWeight = pathSegmentPenaltyWeight;
	}
	public int getRecurringElementsPenaltyWeight() {
		return recurringElementsPenaltyWeight;
	}
	public void setRecurringElementsPenaltyWeight(int recurringElementsPenaltyWeight) {
		this.recurringElementsPenaltyWeight = recurringElementsPenaltyWeight;
	}
	public int getParentKeywordsRewardWeight() {
		return parentKeywordsRewardWeight;
	}
	public void setParentKeywordsRewardWeight(int parentKeywordsRewardWeight) {
		this.parentKeywordsRewardWeight = parentKeywordsRewardWeight;
	}
	public int getEndsWithWsdlReward() {
		return endsWithWsdlReward;
	}
	public void setEndsWithWsdlReward(int endsWithWsdlReward) {
		this.endsWithWsdlReward = endsWithWsdlReward;
	}
	public int getUrlKeywordsReward() {
		return urlKeywordsReward;
	}
	public void setUrlKeywordsReward(int urlKeywordsReward) {
		this.urlKeywordsReward = urlKeywordsReward;
	}
	public String getUrlKeywords() {
		return urlKeywords;
	}
	public void setUrlKeywords(String urlKeywords) {
		this.urlKeywords = urlKeywords;
	}
	public String getContentKeywords() {
		return contentKeywords;
	}
	public void setContentKeywords(String contentKeywords) {
		this.contentKeywords = contentKeywords;
	}

}

