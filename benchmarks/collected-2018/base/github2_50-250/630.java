// https://searchcode.com/api/result/105177235/

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Riot.
 *
 * The Initial Developer of the Original Code is
 * Neteye GmbH.
 * Portions created by the Initial Developer are Copyright (C) 2006
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Felix Gnass [fgnass at neteye dot de]
 *
 * ***** END LICENSE BLOCK ***** */
package org.riotfamily.crawler;


import java.util.List;

import org.riotfamily.common.log.RiotLog;
import org.htmlparser.util.ParserException;
import org.riotfamily.common.web.event.ContentChangedEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

/**
 * Class that crawls web pages.
 */
public class Crawler implements InitializingBean, ApplicationListener, Runnable {

	private RiotLog log = RiotLog.get(Crawler.class);

	private String startPage;

	private PageLoader pageLoader;

	private LinkExtractor linkExtractor;
	
	private LinkFilter linkFilter;

	private List<PageHandler> pageHandlers;

	private long delay;
	
	private volatile boolean running;
	
	private volatile int pageCount;
	
	private int lastPageCount;

	private HrefStack hrefs = new HrefStack();
	
	/**
	 * Sets the URL where the crawler should start crawling. The specified
	 * URL must be absolute, i.e. contain a protocol and host.
	 */
	public void setStartPage(String startPage) {
		this.startPage = startPage;
	}

	/**
	 * Sets the delay between requests in milliseconds. The default value is 
	 * <code>0</code>, which means that the crawler won't pause at all. 
	 * You may want to increase this value in order to reduce the server load. 
	 */
	public void setDelay(long delay) {
		this.delay = delay;
	}

	/**
	 * Sets the PageLoader that is used to retrieve the data. By default a
	 * {@link CommonsHttpClientPageLoader} is used.
	 */
	public void setPageLoader(PageLoader pageLoader) {
		this.pageLoader = pageLoader;
	}

	/**
	 * Sets a list of {@link PageHandler} instances.
	 */
	public void setPageHandlers(List<PageHandler> pageHandlers) {
		this.pageHandlers = pageHandlers;
	}

	/**
	 * Sets the {@link LinkExtractor} that is used to extract the link URLs 
	 * from the parsed HTML document.
	 */
	public void setLinkExtractor(LinkExtractor linkExtractor) {
		this.linkExtractor = linkExtractor;
	}
	
	/**
	 * Sets the {@link LinkFilter} that decides whether a link should be 
	 * followed.
	 */
	public void setLinkFilter(LinkFilter linkFilter) {
		this.linkFilter = linkFilter;
	}
	
	public void setContentEventMulticaster(ApplicationEventMulticaster mc) {
		mc.addApplicationListener(this);
	}

	public void afterPropertiesSet() throws Exception {
		if (startPage == null) {
			log.warn("A startPage must be set in order to index the site.");
		}
		if (pageLoader == null) {
			pageLoader = new CommonsHttpClientPageLoader();
		}
		if (linkExtractor == null) {
			linkExtractor = new DefaultLinkExtractor();
		}
		if (linkFilter == null) {
			linkFilter = new LocalLinkFilter();
		}
	}

	/**
	 * Start crawling. The method first notifies all handlers that the crawler
	 * has been started, it then invokes {@link #crawl()} and finally notifies
	 * the handlers that the crawling has finished.
	 * 
	 * @see PageHandler#crawlerStarted()
	 * @see PageHandler#crawlerFinished()  
	 */
	public void run() {
		if (startPage == null) {
			return;
		}
		if (pageHandlers == null || pageHandlers.isEmpty()) {
			return;
		}
		if (running) {
			log.info("Crawler is already running.");
			return;
		}
		try {
			running = true;
			log.info("Starting to crawl...");
	        for (PageHandler handler : pageHandlers) {
	        	handler.crawlerStarted();
	        }
			crawl();
			for (PageHandler handler : pageHandlers) {
	        	handler.crawlerFinished();
	        }
		}
		finally {
			running = false;
			log.info("Crawler is finished.");
		}
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContentChangedEvent) {
			ContentChangedEvent ce = (ContentChangedEvent) event;
			log.info("Content changed: " + ce.getUrl());
			if (running) {
				hrefs.addAbsolute(ce.getUrl());
			}
			PageData pageData = pageLoader.loadPage(new Href(null, ce.getUrl()));
	        if (pageData.isOk()) {
	        	try {
		        	pageData.parse();
	        	}
	        	catch (ParserException e) {
	        		log.error(e);
	        	}
	        }
	        for (PageHandler handler : pageHandlers) {
	        	handler.handlePage(pageData);
	        }
		}
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public int getProgress() {
		if (lastPageCount > 0 && pageCount > 0) {
			return Math.round((float) pageCount / lastPageCount * 100);
		}
		return 0;
	}

	protected void crawl() {
		long startTime = System.currentTimeMillis();
		hrefs.clear();
		hrefs.addAbsolute(startPage);
		pageCount = 0;
	    while (hrefs.hasNext()) {
	    	Href href = hrefs.next();
	        PageData pageData = pageLoader.loadPage(href);
	        if (pageData.isOk()) {
	        	try {
		        	pageData.parse();
	        		for (String link : linkExtractor.extractLinks(pageData)) { 
	        			if (linkFilter.accept(pageData.getUrl(), link)) {
	        				hrefs.add(pageData.getUrl(), link);
	        			}
		            }
	        	}
	        	catch (ParserException e) {
	        		log.error(e);
	        	}
	        }
	        else if (pageData.isRedirect()) {
	        	log.debug("Redirect: " + pageData.getRedirectUrl());
	        	if (linkFilter.accept(pageData.getUrl(), pageData.getRedirectUrl())) {
	        		hrefs.add(pageData.getUrl(), pageData.getRedirectUrl());
	        	}
	        }

	        for (PageHandler handler : pageHandlers) {
	        	handler.handlePage(pageData);
	        }
	        pageCount++;
	        if (delay > 0) {
	        	log.info("Waiting " + delay + "ms");
        		try {
					Thread.sleep(delay);
				}
				catch (InterruptedException ex) {
					// simply proceed
				}
	        }
	    }
	    
	    lastPageCount = pageCount;
	    log.info(pageCount + " pages crawled in " +
	    		(System.currentTimeMillis() - startTime) + " ms");
	}

}

