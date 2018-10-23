// https://searchcode.com/api/result/12361642/

package tripleo.histore.j3;

import java.io.*;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tripleo.amemu.*;
import tripleo.curr.MonitoredRunner;
import tripleo.framework.svr.storage.*;
import tripleo.histore.*;
import tripleo.util.UT;

// $Id: HiStore_J2.java,v 1.5 2005/10/11 19:22:08 olu Exp $

public class HiStore_J3 implements HiStore {

	private int cs;
	IStorageServer s;
	ds1 d;
	ds1 ds;

	public HiStore_J3 (String startdir) throws ds_exception {
		backing=new File(startdir);
		backing.mkdirs();
		//
		ds=new ds1(startdir+"/tmp");
		s=new ss1(startdir+"/st");
		d=new ds1(startdir+"/ds");
		cs=0; // TODO conflict
		//
		ds.mkdb();
		ds.start();
		//
		d.mkdb();
		d.start();
		
	}
	
//	public InputStream readStreamFor(long identifier, char aFor) throws ConsistencyFailure {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	public OutputStream writeStreamFor(long identifier, char aFor) throws ConsistencyFailure {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	public String mangle_name(long identifier, char aFor) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public HiStoreEntry alloc(String aKey) throws AllocationFailure, ConsistencyFailure {
		try {
			ds.enter(aKey,cs++);
		} catch (ds_exception e) {
			throw new AllocationFailure(Constants.UNKNOWN, e); // TODO
		}
		return new HiStoreEntry_J3(cs,aKey,this);
	}

	public HiStoreEntry insert(String aKey) throws AllocationFailure, ConsistencyFailure {
		try {
			int b = ds.lookup(aKey);
			throw new ConsistencyFailure( Constants.UNIQ);
		} catch (ds_exception e) {
//			log.info("hise-insert: key= "+aKey, e);
			// TODO assume we got here b/c not_found @see ds_exception
			return alloc(aKey);
		}
	}

	public void removeEntryForKey(long identifier) throws ConsistencyFailure {
		// TODO Auto-generated method stub
		
	}

	transient private static Log log = LogFactory.getLog(HiStore_J3.class);


//	private long dummyident;
//	private J3_System.lazyEntry first;

	File backing;

	String getFileName(long identifier, char aFor) {
		final String mangled = mangle_name(identifier, aFor);
		final File file = new File(backing,mangled);
		final String R = file.getPath();
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

//	/**
//	 * return an the entry associated with akey. it may exist already in entrymap
//	 *
//	 * @param aKey goes into entries field.
//	 * @throws tripleo.histore.AllocationFailure  if the system rejects our request
//	 * @throws tripleo.histore.ConsistencyFailure if the filesystem reports an error
//	 */
//	public HiStoreEntry alloc(String aKey) throws AllocationFailure, ConsistencyFailure {
//		//%nevernull
//		log.trace("alloc "+aKey);
//		synchronized (entries()) {
//			if(!psystem().has_entry_for_key(aKey)) {
//				_insert(aKey);
//			}
//			return open(aKey);
//		}
//	}
//
//	/**
//	 *
//	 * @param aKey
//	 * @return null
//	 * @throws tripleo.histore.ConsistencyFailure if #aKey does not exist
//	 */
//	private HiStoreEntry open(String aKey) throws ConsistencyFailure {
//		//%nevernull
//		if(!psystem().has_entry_for_key(aKey)) {
//			log.trace("open failure for "+aKey);
//			throw new ConsistencyFailure(Constants.NON_EXISTANCE);
//		}
//		final HiStoreEntry entry = psystem().entry_for_key(aKey, this);
//		log.trace("open sucess for "+aKey);
//		return entry;
//	}
//
//	/**
//	 * Answers a fresh entry to have #get{Meta,Content}Writer called,
//	 * with empty content and metadata
//	 *
//	 * @param aKey a unique key which will be the entry's identifier
//	 * @return an entry for #aKey,
//	 *
//	 * @throws tripleo.histore.AllocationFailure  @see #_insert
//	 * @throws tripleo.histore.ConsistencyFailure The reason will be UNIQ if the key already exists
//	 */
//	public HiStoreEntry insert(String aKey) throws AllocationFailure, ConsistencyFailure {
//		synchronized (entries()) {
//			if (psystem().has_entry_for_key(aKey))
//				throw new ConsistencyFailure(Constants.UNIQ);
//			//
//			return _insert(aKey);
//		}
//    }
//
//	public void removeEntryForKey(long identifier) throws ConsistencyFailure {
//		int y=2;
//	}
//
//	/**
//	 * Worker function. undocumented.
//	 *
//	 * @param aKey
//	 * @return
//	 * @throws tripleo.histore.AllocationFailure @see #nextIdent
//	 */
//	private HiStoreEntry _insert(String aKey) throws AllocationFailure {
//		final long    identifier = nextIdent();
//		final HiStoreEntry_J3 entry = new HiStoreEntry_J3(this, identifier);
//		//
//		queue_add_entry(aKey, entry);
//		//
//		return entry;
//	}
//
//	/**
//	 * Tell the system to update its entry index
//	 * Our prevalent system will allocate #r for the callers' exclusive usage
//	 * <p/>
//	 * NOTE: This also adds entry to the dirty list, which will be flushed later
//	 * <p/>
//	 * @param aKey   ...
//	 * @param entry  ...
//	 *
//	 * @throws tripleo.histore.AllocationFailure if the prevayler fails
//	 */
//	private void queue_add_entry(final String aKey, final HiStoreEntry_J3 entry) throws AllocationFailure {
//		try {
//			prevayler.executeCommand(new J3_System.EntryStorageCommand(aKey, entry));
//		} catch (Exception e) {
//			// TODO: allocation is overused
//			throw new AllocationFailure(Constants.PREVALENT_REJECTION, e);
//		}
//	}
//
//	/**
//	 * Tell the system that we wish to use this number #r
//	 * Our prevalent system will allocate #r for the callers' exclusive usage
//	 *
//	 * @param r the number we want to use
//	 * @throws tripleo.histore.AllocationFailure if the system rejects our request
//	 */
//	private void allocate(long r) throws AllocationFailure {
//		try {
//			prevayler.executeCommand(new J3_System.NumberStorageCommand(r));
//		} catch (Exception e) {
//			throw new AllocationFailure(Constants.PREVALENT_REJECTION, e);
//		}
//	}
//
//	/**
//	 * Ask the system if it already has this number
//	 */
//	private boolean allocated(final long r) {
//		//return entrymap.containsValue(new Long(r));
//		return psystem().allocated(r);
//	}
//
//	/**
//	 * Currently used only by insert
//	 * @return
//	 * @throws tripleo.histore.AllocationFailure
//	 */
//	private long nextIdent() throws AllocationFailure {
//		// TODO: use system nodes. prevayler for now. also use a lock...
//		long R = dummyident = dummyident + 1;
//		while (allocated(R)) R = dummyident = dummyident + 1;
//		allocate(R);
//		return R;
//	}
//
//	private HiStore_J3() {
////		dirty = new Vector<HiStoreEntry_J2>();
//		dummyident = 0x1000L;
//		first = null;
//	}
//
//	public static HiStore_J3 New(final String aBacking) throws InitializationFailure	{
//		return New(aBacking, new MonitoredRunner() { // TODO:refactor
//			public boolean stillRunning() {return true;}
//		});
//	}
//
//	/**
//	 * 
//	 * @param aBacking a directory name to create files
//	 * @param m if this is not null, start a cleaner thread
//	 *          note that this behavoir is undefined (untested)
//	 * @return
//	 * @throws tripleo.histore.InitializationFailure ...
//	 */
//	public static HiStore_J3 New(String aBacking, final MonitoredRunner m) throws InitializationFailure	{
//		try {
//			HiStore_J3 R = new HiStore_J3();
//			//
//			R.backing = new File(aBacking);
//			R.backing.mkdirs();
//			//
//			R.first = new J3_System.serialEntry(R, 0L);
//			final String prevail_path = R.backing.getPath()+"/prevail";
//			final J3_System newSystem = new J3_System(R);
//			R.prevayler = new /*Snapshot*/Prevayler(newSystem, prevail_path);
////			if (R.prevayler instanceof SnapshotPrevayler) {
////				// snapshot as soon as possible
////				// reduce state space
////				((SnapshotPrevayler) R.prevayler).takeSnapshot();
////			}
//			R.read();
//			//
//			if (m!=null) R.psystem().start_cleaner(m);
//			//
//			return R;
////		} catch (IOException e) {
////			throw new InitializationFailure(e);
//		} catch (SecurityException e) {
//			throw new InitializationFailure(e);
////		} catch (ClassNotFoundException e) {
////			throw new InitializationFailure(e);
//		}
//	}
//
//	/**
//	 * initialize #entrymap from the store on disk
//	 */
//	public void read() {
//		psystem().entrymap.load("entries");
//	}
//
////	public void manualRead() {
////		Assert.not_implemented();
////	}
//
//	public void write() {
//		psystem().entrymap.store("entries");
//	}
//
//
//	public Map getEntrymap() {
//		return entries().unmodifiable();// TODO: used in RCache
//	}
//
//	/**
//	 * @deprecated
//	 */
//	public EntryMap entries() {
//		return psystem().entrymap;
//	}
//
//	public void remove(String aKey) {
//		psystem().remove_entry_for_key(aKey);
//	}
//
//	public static interface IExceptionHandler {
//		void handleException(Throwable throwable);
//	}
//
//	/**
//	 * The default entry is used for metadata
//	 * 
//	 * @return the default entry
//	 */
////	public J2_System.lazyEntry first() {
////		return first;
////	}
//
//	/**
//	 * Used by #ViewerFrame and #J2_System. Internal.
//	 * @return
//	 */
//	public File backing() {
//		return backing;
//	}


}


