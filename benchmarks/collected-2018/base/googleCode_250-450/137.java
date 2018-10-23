// https://searchcode.com/api/result/12361651/

package tripleo.histore.j2;

import java.io.*;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tripleo.curr.MonitoredRunner;
import tripleo.histore.*;
import tripleo.util.UT;

// $Id: HiStore_J2.java,v 1.5 2005/10/11 19:22:08 olu Exp $

public class HiStore_J2 implements HiStore {

	transient private static Log log = LogFactory.getLog(HiStore_J2.class);

	static class Prevayler {
		PrevalentSystem syst;

		public Prevayler(PrevalentSystem aSyst, String aPrevail_path) {
			syst = aSyst;
		}

		public void executeCommand(Command aCommand) throws Exception {
			aCommand.execute(syst);
		}

		public PrevalentSystem system() {
			return syst;
		}
	}

	private long dummyident;
	private Prevayler prevayler;
	private J2_System.lazyEntry first;
	private File backing;

	String getFileName(long identifier, char aFor) {
		final String R = backing+"/"+mangle_name(identifier, aFor);
		return R;
	}

	/**
	 *
	 * @param identifier
	 * @param aFor
	 * @return
	 * @throws tripleo.histore.ConsistencyFailure if the filesystem fails
	 */
	public InputStream readStreamFor(long identifier, char aFor) throws ConsistencyFailure {
		final File file = new File(getFileName(identifier, aFor));
		try {
			FileInputStream R =	new FileInputStream(file);
			return R;
		} catch (FileNotFoundException e) {
			log.debug("FileNotFound " + file);
			throw new ConsistencyFailure(Constants.ALLOC_READ_STREAM);
		}
	}

	/**
	 *
	 * @param identifier
	 * @param aFor
	 * @return
	 * @throws tripleo.histore.ConsistencyFailure if the filesystem fails
	 */
	public OutputStream writeStreamFor(long identifier, char aFor) throws ConsistencyFailure {
		final String mangled = getFileName(identifier, aFor);
		final File file = new File(mangled);
		FileOutputStream R;
		try {
			R = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			throw new ConsistencyFailure(Constants.ALLOC_WRITE_STREAM);
//			UT.errW.println("-~ [HiStore] FileNotFound " + file);
//			Assert.not_reached();
//			R = null;
		}
		return R;
	}

	public String mangle_name(long identifier, char aFor) {
		String R;
		String s1 = Long.toString(identifier, 16);
		R = UT.repeatChar('0', 8-s1.length()) + s1;
		R += new String(new char[]{'-',Character.toUpperCase(aFor)});
		return R;
	}

	/**
	 * return an the entry associated with akey. it may exist already in entrymap
	 *
	 * @param aKey goes into entries field.
	 * @throws tripleo.histore.AllocationFailure  if the system rejects our request
	 * @throws tripleo.histore.ConsistencyFailure if the filesystem reports an error
	 */
	public HiStoreEntry alloc(String aKey) throws AllocationFailure, ConsistencyFailure {
		//%nevernull
		log.trace("alloc "+aKey);
		synchronized (entries()) {
			if(!psystem().has_entry_for_key(aKey)) {
				_insert(aKey);
			}
			return open(aKey);
		}
	}

	/**
	 *
	 * @param aKey
	 * @return null
	 * @throws tripleo.histore.ConsistencyFailure if #aKey does not exist
	 */
	private HiStoreEntry open(String aKey) throws ConsistencyFailure {
		//%nevernull
		if(!psystem().has_entry_for_key(aKey)) {
			log.trace("open failure for "+aKey);
			throw new ConsistencyFailure(Constants.NON_EXISTANCE);
		}
		final HiStoreEntry entry = psystem().entry_for_key(aKey, this);
		log.trace("open sucess for "+aKey);
		return entry;
	}

	/**
	 * Answers a fresh entry to have #get{Meta,Content}Writer called,
	 * with empty content and metadata
	 *
	 * @param aKey a unique key which will be the entry's identifier
	 * @return an entry for #aKey,
	 *
	 * @throws tripleo.histore.AllocationFailure  @see #_insert
	 * @throws tripleo.histore.ConsistencyFailure The reason will be UNIQ if the key already exists
	 */
	public HiStoreEntry insert(String aKey) throws AllocationFailure, ConsistencyFailure {
		synchronized (entries()) {
			if (psystem().has_entry_for_key(aKey))
				throw new ConsistencyFailure(Constants.UNIQ);
			//
			return _insert(aKey);
		}
    }

	public void removeEntryForKey(long identifier) throws ConsistencyFailure {
		int y=2;
	}

	/**
	 * Worker function. undocumented.
	 *
	 * @param aKey
	 * @return
	 * @throws tripleo.histore.AllocationFailure @see #nextIdent
	 */
	private HiStoreEntry _insert(String aKey) throws AllocationFailure {
		final long    identifier = nextIdent();
		final HiStoreEntry_J2 entry = new HiStoreEntry_J2(this, identifier);
		//
		queue_add_entry(aKey, entry);
		//
		return entry;
	}

	/**
	 * Tell the system to update its entry index
	 * Our prevalent system will allocate #r for the callers' exclusive usage
	 * <p/>
	 * NOTE: This also adds entry to the dirty list, which will be flushed later
	 * <p/>
	 * @param aKey   ...
	 * @param entry  ...
	 *
	 * @throws tripleo.histore.AllocationFailure if the prevayler fails
	 */
	private void queue_add_entry(final String aKey, final HiStoreEntry_J2 entry) throws AllocationFailure {
		try {
			prevayler.executeCommand(new J2_System.EntryStorageCommand(aKey, entry));
		} catch (Exception e) {
			// TODO: allocation is overused
			throw new AllocationFailure(Constants.PREVALENT_REJECTION, e);
		}
	}

	/**
	 * Tell the system that we wish to use this number #r
	 * Our prevalent system will allocate #r for the callers' exclusive usage
	 *
	 * @param r the number we want to use
	 * @throws tripleo.histore.AllocationFailure if the system rejects our request
	 */
	private void allocate(long r) throws AllocationFailure {
		try {
			prevayler.executeCommand(new J2_System.NumberStorageCommand(r));
		} catch (Exception e) {
			throw new AllocationFailure(Constants.PREVALENT_REJECTION, e);
		}
	}

	/**
	 * Ask the system if it already has this number
	 */
	private boolean allocated(final long r) {
		//return entrymap.containsValue(new Long(r));
		return psystem().allocated(r);
	}

	private J2_System psystem() {
		return (J2_System)prevayler.system();
	}

	/**
	 * Currently used only by insert
	 * @return
	 * @throws tripleo.histore.AllocationFailure
	 */
	private long nextIdent() throws AllocationFailure {
		// TODO: use system nodes. prevayler for now. also use a lock...
		long R = dummyident = dummyident + 1;
		while (allocated(R)) R = dummyident = dummyident + 1;
		allocate(R);
		return R;
	}

	private HiStore_J2() {
//		dirty = new Vector<HiStoreEntry_J2>();
		dummyident = 0x1000L;
		first = null;
	}

	public static HiStore_J2 New(final String aBacking) throws InitializationFailure	{
		return New(aBacking, new MonitoredRunner() { // TODO:refactor
			public boolean stillRunning() {return true;}
		});
	}

	/**
	 * 
	 * @param aBacking a directory name to create files
	 * @param m if this is not null, start a cleaner thread
	 *          note that this behavoir is undefined (untested)
	 * @return
	 * @throws tripleo.histore.InitializationFailure ...
	 */
	public static HiStore_J2 New(String aBacking, final MonitoredRunner m) throws InitializationFailure	{
		try {
			HiStore_J2 R = new HiStore_J2();
			//
			R.backing = new File(aBacking);
			R.backing.mkdirs();
			//
			R.first = new J2_System.serialEntry(R, 0L);
			final String prevail_path = R.backing.getPath()+"/prevail";
			final J2_System newSystem = new J2_System(R);
			R.prevayler = new /*Snapshot*/Prevayler(newSystem, prevail_path);
//			if (R.prevayler instanceof SnapshotPrevayler) {
//				// snapshot as soon as possible
//				// reduce state space
//				((SnapshotPrevayler) R.prevayler).takeSnapshot();
//			}
			R.read();
			//
			if (m!=null) R.psystem().start_cleaner(m);
			//
			return R;
//		} catch (IOException e) {
//			throw new InitializationFailure(e);
		} catch (SecurityException e) {
			throw new InitializationFailure(e);
//		} catch (ClassNotFoundException e) {
//			throw new InitializationFailure(e);
		}
	}

	/**
	 * initialize #entrymap from the store on disk
	 */
	public void read() {
		psystem().entrymap.load("entries");
	}

//	public void manualRead() {
//		Assert.not_implemented();
//	}

	public void write() {
		psystem().entrymap.store("entries");
	}


	public Map getEntrymap() {
		return entries().unmodifiable();// TODO: used in RCache
	}

	/**
	 * @deprecated
	 */
	public EntryMap entries() {
		return psystem().entrymap;
	}

	public void remove(String aKey) {
		psystem().remove_entry_for_key(aKey);
	}

	public static interface IExceptionHandler {
		void handleException(Throwable throwable);
	}

	/**
	 * The default entry is used for metadata
	 * 
	 * @return the default entry
	 */
//	public J2_System.lazyEntry first() {
//		return first;
//	}

	// in the same tradition as alloc/insert
	public HiStoreEntry walk_open(String aPath) {
		//% nullerr

		// TODO: implement me

		// 0) convert arg to a path-object
		// 1) open the meta reader
		// 2) find if it is a dir entry
		// 3) parse entries
		// 4) lookup
		// 5) rinse, lather, repeat (ok just repeat)
		HiStoreEntry R = null;
		return R;
	}

	/**
	 * Used by #ViewerFrame and #J2_System. Internal.
	 * @return
	 */
	public File backing() {
		return backing;
	}
}


