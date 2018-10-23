// https://searchcode.com/api/result/7102455/

package dovetaildb.dbservice;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;


import dovetaildb.api.ApiBuffer;
import dovetaildb.bagindex.BagIndex;
import dovetaildb.iter.Iter;
import dovetaildb.util.Dirty;

public interface DbService extends Serializable, Dirty.Able {

	public void initialize(); // guaranteed to be called synchronously before anything else
	
	/**
	 * 
	 * @param bag
	 * @param txnId
	 * @param query The query filtering constraints, expressed as a json object.
	 *   Each item must be an instance of List, Number, or String.
	 *   The first item in any list is an operation string, one of the following:
	 *   ["AND", <query1>, ...]
	 *   ["OR", <query1>, ...]
	 *   ["NOT", <query>]
	 *   ["=", <field>, <value1>, ...]  (extra values interpreted as meaning that the field can match any)
	 *   ["!=", <field>, <value1>, ...]  (extra values interpreted as meaning that the field cannot match any)
	 *   ["<", <field>, <value>]
	 *   [">", <field>, <value>]
	 *   ["<=", <field>, <value>]
	 *   [">=", <field>, <value>]
	 * @param options
	 *   map,reduce,score,offset,limit,bookmark,diversity
	 * @return
	 *   Iter
	 * 
	 * Should every option be implementable at the DbService level?
	 *  - Does full text search work at the DbService level?
	 *  - Does sorting/weighting work at the DbService level?  Bookmarks?
	 *  - Efficient implementation of computed fields (SAX like result processing?)
	 *  
	 *  Should txnIds be Bytes (or Strings) instead of longs?  It makes rebuilding
	 *  slightly easier (because you can namespace the transactions over the different
	 *  generations).  But it means that txns are not ordered in any fashion (except,
	 *  perhaps, via the coordinator)
	 */
	public Iter query(String bag, long txnId, Object query, Map<String, Object> options);

	/**
	 * 
	 * @return Capacity measurement: -1 to 1000, higher means more capacity is available.  -1 means out of commission.
	 */
	public long capacity();
	
	public Collection<String> getBags(long txnId);

	public ApiBuffer createBufferedData(String bagName);

	public long commit(long fromTxnId, Map<String, ApiBuffer> batch);

	public void rollback(long txnId) throws TxnNotFoundException;

	public void dropBag(String bagName);
	
	public BagIndex getBag(String bagName);
	
	public void drop();

	public File getHomeDir();

	public long getHighestCompletedTxnId();
	
	public Map<String,Object> getMetrics(int detailLevel);

}

