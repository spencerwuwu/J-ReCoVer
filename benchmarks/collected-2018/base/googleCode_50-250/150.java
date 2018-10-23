// https://searchcode.com/api/result/4332687/

package it.unina.cloudclusteringnaive;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

import com.google.appengine.tools.mapreduce.AppEngineMapper;

import org.apache.hadoop.io.NullWritable;
import org.datanucleus.store.appengine.query.JDOCursorHelper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

public class JoinResultMapper extends
		AppEngineMapper<Key, Entity, NullWritable, NullWritable> {
	private static final Logger log = Logger.getLogger(JoinResultMapper.class
			.getName());

	//private DatastoreService datastore;

	// private long count = 1;

	public JoinResultMapper() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void taskSetup(Context context) {
	//	this.datastore = DatastoreServiceFactory.getDatastoreService();
	}

	@Override
	public void taskCleanup(Context context) {
	//	log.warning("Doing per-task cleanup");
	}

	@Override
	public void setup(Context context) {
		//log.warning("Doing per-worker setup");
	}

	@Override
	public void cleanup(Context context) {
		//log.warning("Doing per-worker cleanup");
	}

	// TODO: modificare in modo che scriva qualcosa ogni ora
	@Override
	public void map(Key key, Entity value, Context context) {
		

		String paramKey=(String) value.getProperty("params");
		
		log.warning("Mapping key: " + paramKey);
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		
		Map<String, Object> extensionMap= new HashMap<String, Object>();
		Cursor cursor;
		
		try {
			
			log.warning("STO FACENDO LA QUERY");
			
			Query query = pm.newQuery("select from "+ SvmTestResult.class.getName()
			+ " "+ "where params == paramKey parameters String paramKey ");
			
			query.setRange(0, 500);

			//Recupero la lista di coppie (valore predetto, valore reale) che condividono 
			//la stessa stringa di parametri
			List<SvmTestResult> eList=(List<SvmTestResult>) query.execute(paramKey);
			log.warning("dim lista "+ eList.size());
			//Fusione delle liste dei risultati
			String resultList ="";
			
			while(eList.size()!=0){
				
				//Crea la lista parziale dei primi 500 risultati
				for (SvmTestResult r : eList) {
					resultList+=r.getResults()+" ";
				}
				
				cursor = JDOCursorHelper.getCursor(eList);
				
				extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
	
				query.setExtensions(extensionMap);
				
				query.setRange(0, 500);
				
				//Prende i 500 risultati successivi
				eList=(List<SvmTestResult>) query.execute(paramKey);
			
			}
			
			//Creazione entit?  di input per la fase di reduce
			SvmReduceInput ri= new SvmReduceInput(paramKey, resultList);
			
			pm.makePersistent(ri);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} finally {
			pm.close();
		}

	}

}
