// https://searchcode.com/api/result/126899715/

package edu.scripps.mwsync;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import com.google.common.base.Preconditions;



import edu.scripps.genewiki.common.Serialize;
import edu.scripps.genewiki.common.Wiki;
import edu.scripps.genewiki.common.Wiki.Revision;
import edu.scripps.mwsync.Keys;
/**
 * Sync objects represent the operation of pulling changes from a source
 * MediaWiki and pushing them to a target MediaWiki. They are repeatedly 
 * executed by a Sync.Runner object.
 * <br /><br />
 * To instantiate, the client can either inject the dependencies through
 * the constructor, or use the static methods to create a Sync object from
 * options specified in a configuration file.
 * <br /><br />
 * To run, the client must create a Runner object and pass the Sync to the 
 * Runner's start() method.
 * <br /><br />
 * To create custom rewriting behavior, the client can create an object that
 * implements the Rewriter interface and attach it using mySync.setRewriter().
 * <br /><br />
 * Usage example:
 * <pre>
 * Sync sync = Sync.newFromConfigFile();
 * Rewriter r = new MyCustomRewriter();
 * sync.setRewriter(r);
 * Runner runner = new Runner();
 * runner.start(sync);
 * </pre>
 * @author eclarke@scripps.edu
 *
 */
public class Sync implements Runnable {

	private final static String DEFAULT_PATH = "/etc/mwsync/";
	private final static String DEFAULT_CFG_NAME = "mwsync.conf";
	private final static String LAST_CHECK_TIME = DEFAULT_PATH+"lastcheck.ser";
	private final static String	SYNC_SUMMARY_FMT = "{[SYNC | user = %s | revid = %s | summary = %s]}";
	
	private final Wiki 		source;
	private final Wiki 		target;
	private final long 		period;
	private Rewriter	 	rewriter;
		
	public Wiki 	source() 	{ return this.source; }
	public Wiki 	target() 	{ return this.target; }
	public long 	period() 	{ return this.period; }
	public Rewriter	rewriter() 	{ return this.rewriter; }
	
	/**
	 * Appending a rewriter allows the Sync to modify contents before writing
	 * to the target.
	 * @param r rewriter
	 */
	public void setRewriter(Rewriter r)	{ this.rewriter = r; } 
	
	/**
	 * Create a new Sync between the source and target MediaWiki with a  
	 * given period (in seconds) and a Rewriter module to optionally alter
	 * content before posting it to the target.
	 * @param source Wiki to pull from
	 * @param target Wiki to write to
	 * @param periodInSeconds the frequency of updates
	 * @throws IllegalArgumentException if src/tgt Wikis are null, or period
	 * less than 60s
	 */
	public Sync(Wiki source, Wiki target, long periodInSeconds)
	{
		try {
			this.source = Preconditions.checkNotNull(source);
			this.target = Preconditions.checkNotNull(target);
		} catch (NullPointerException npe) {
			throw new IllegalArgumentException("Source/Target Wiki instance cannot be null.");
		}
		
		Preconditions.checkArgument(periodInSeconds >= 60);
		this.period = periodInSeconds;
		this.rewriter = null;
	}
	
	/**
	 * Returns a new Sync created from options specified in the default configuration
	 * file location (usually '/etc/mwsync/mwsync.conf').
	 * @return default Sync object
	 * @throws FailedLoginException
	 * @throws IOException
	 */
	public static Sync newFromConfigFile() 
			throws FailedLoginException, IOException
	{
		return Sync.newFromConfigFile(DEFAULT_PATH+DEFAULT_CFG_NAME);
	}

	/**
	 * Returns a new Sync created from options specified in the configuration file.
	 * @param conf_filename the filename
	 * @return Sync object created from options in specified config file
	 * @throws FailedLoginException
	 * @throws IOException
	 */
	public static Sync newFromConfigFile(String conf_filename) 
			throws FailedLoginException, IOException 
	{
		Properties props = new Properties();
		props.load(new FileReader(conf_filename));
		
		Wiki source = new Wiki(props.getProperty(Keys.SRC_LOCATION, null));
		source.setMaxLag(0);
		source.login(
				props.getProperty(Keys.SRC_USERNAME), 
				props.getProperty(Keys.SRC_PASSWORD).toCharArray());
		
		Wiki target = new Wiki(props.getProperty(Keys.TGT_LOCATION, null));
		target.setMaxLag(0);
		target.setThrottle(0);
		target.setUsingCompressedRequests(false);
		target.login(
				props.getProperty(Keys.TGT_USERNAME), 
				props.getProperty(Keys.TGT_PASSWORD).toCharArray());
		
		long period = Long.parseLong(props.getProperty(Keys.SYNC_PERIOD));
		
		return new Sync(source, target, period);	
	}
	
	/**
	 * Runs one iteration of a synchronization operation: find the changes
	 * since the last check from the source, and write them to the target.
	 * This method is usually called by a Sync.Runner, not manually.
	 */
	public void run()
	{
		try {
			Calendar lastChecked;
			try { 
				lastChecked = (Calendar) Serialize.in(LAST_CHECK_TIME); 
			} catch (FileNotFoundException e) {
				lastChecked = Calendar.getInstance();
				lastChecked.add(Calendar.HOUR, -2);
			}
			
			List<String> changed = getRecentChanges(lastChecked);
			Serialize.out(LAST_CHECK_TIME, lastChecked);
			writeChangedArticles(changed);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private List<String> getRecentChanges(Calendar since) 
			throws IOException
	{
		List<Revision> live = source.getChangesFromWatchlist(since, true);
		Set<String> changed = new HashSet<String>(live.size());
		for (Revision rev : live) {
			changed.add(rev.getTitle());
		}
		return new ArrayList<String>(changed);
	}
	
	
	private void writeChangedArticles(List<String> changed)
	{
		for (String title : changed) {
			try {
				String text = source.getPageText(title);
				Revision rev = source.getTopRevision(title);
				String summary = rev.getSummary();
				String author = rev.getUser();
				String revid = rev.getRevid()+"";
				summary = String.format(SYNC_SUMMARY_FMT, author, revid, summary); 
				
				/** 
				 * If we have a rewriter object, feed the text through first
				 * before posting to target
				 */
				if (rewriter != null) {
					text = rewriter.process(text, title, source, target);
				}
				
				target.edit(title, text, summary, false);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (LoginException e) {
				e.printStackTrace();
			}
			
		}
	}

	
	/**
	 * A Runner controls the repeating execution of a single Sync operation. It
	 * is responsible for re-attempting the sync should it fail, while also 
	 * ensuring that multiple, rapid failures result in program termination.
	 * 
	 * @author eclarke
	 */
	public static class Runner {
		
		/**
		 * Starts the synchronization using the provided Sync object.
		 * @param sync the Sync object to use
		 */
		public void start(Sync sync)
		{
			ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
			ScheduledFuture<?> future = executor.scheduleAtFixedRate(sync, 0, sync.period, TimeUnit.SECONDS);
			long 	launchtime 	= Calendar.getInstance().getTimeInMillis();
			int		failcount 	= 0;
			
			// TODO logging - log something here
			
			while (true) {
				if (future.isDone()) {
					try { 
						future.get(); 
					} catch (ExecutionException e) {
						// TODO log exception
						long failtime = Calendar.getInstance().getTimeInMillis();
						
						if (failtime - launchtime < 1200) {
							if (++failcount > 7) {
								// too many failures, abort
								// TODO log decision to exit
								return;
							} else {
								// throttle restart rate +5 seconds each time
								// TODO log decision to sleep
								sleep(5000*failcount);
							}
						} else {
							failcount = 0;
						}
						future = executor.scheduleWithFixedDelay(sync, 0, sync.period, TimeUnit.SECONDS);
						launchtime = Calendar.getInstance().getTimeInMillis();
					} catch (InterruptedException e) {
						// We don't need to handle this.
					}
				} else {
					// reduce polling frequency
					sleep(500);
				}
			}
		}
		
		private void sleep(long ms) 
		{
			try { Thread.sleep(ms); }
			catch (InterruptedException e) { /** pass **/ }
		}
	}
	
}

