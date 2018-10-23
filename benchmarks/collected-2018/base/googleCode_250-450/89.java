// https://searchcode.com/api/result/3972082/

/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.guzz.service.core.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.guzz.Guzz;
import org.guzz.GuzzContext;
import org.guzz.exception.GuzzException;
import org.guzz.exception.ORMException;
import org.guzz.jdbc.ObjectBatcher;
import org.guzz.orm.mapping.ObjectMappingManager;
import org.guzz.orm.mapping.POJOBasedObjectMapping;
import org.guzz.service.AbstractService;
import org.guzz.service.ServiceConfig;
import org.guzz.service.core.SlowUpdateService;
import org.guzz.transaction.TransactionManager;
import org.guzz.transaction.WriteTranSession;
import org.guzz.util.StringUtil;
import org.guzz.util.thread.DemonQueuedThread;
import org.guzz.web.context.GuzzContextAware;

/**
 * 
 * ???????????update???
 * <p/>
 * ???????????Map??????????????????????????????????????????????????????????????????
 * <p/>
 * ????????????????????????
 * <p/>
 * ?????
 * <lo>
 * <li>???????????key?Map????????</li>
 * <li>??????????????count??????????????Map??</li>
 * <li>????????Map??????Map???update????????????Map?????</li>
 * <li>??????Map?????????????</li>
 * <li>??????@param updateInterval ????????</li>
 * </lo>
 * 
 * @author liukaixuan(liukaixuan@gmail.com)
 */
public class SuperSlowUpdateServiceImpl extends AbstractService implements GuzzContextAware, SlowUpdateService {
	private static transient final Log log = LogFactory.getLog(SuperSlowUpdateServiceImpl.class) ;
		
	private Map updateOperations = new HashMap(2048) ;
		
	private TransactionManager tm ;
	
	private ObjectMappingManager omm ;
		
	protected UpdateToDBThread updateThread ;
	
	private int batchSize = 2048 ;
	
	/**?????????500???*/
	private int updateInterval = 500 ;
	
	private Object insertLock = new Object() ;
	
	public void updateCount(String businessName, Object tableCondition, String propToUpdate, Serializable pkValue, int countToInc){
		tableCondition = tableCondition == null ? Guzz.getTableCondition() : tableCondition ;
		POJOBasedObjectMapping mapping = (POJOBasedObjectMapping) omm.getObjectMapping(businessName, tableCondition) ;
		
		if(mapping == null){
			throw new ORMException("unknown business:[" + businessName + "]") ;
		}
		
		String columnToUpdate = mapping.getColNameByPropNameForSQL(propToUpdate) ;
		
		if(columnToUpdate == null){
			throw new ORMException("unknown property:[" + propToUpdate + "], business name:[" + businessName + "]") ;
		}
		
		updateCount(mapping.getDbGroup().getPhysicsGroupName(tableCondition), mapping.getTable().getTableName(tableCondition), columnToUpdate, mapping.getTable().getPKColumn().getColNameForSQL(), pkValue, countToInc) ;
	}
	
	public void updateCount(Class domainClass, Object tableCondtion, String propToUpdate, Serializable pkValue, int countToInc){
		updateCount(domainClass.getName(), tableCondtion, propToUpdate, pkValue, countToInc) ;
	}
	
	public void updateCount(String dbGroup, String tableName, String columnToUpdate, String pkColName, Serializable pkValue, int countToInc) {
		if(!isAvailable()){
			throw new GuzzException("superSlowUpdateService is not available. use the config server's [" + FAMOUSE_SERVICE.SLOW_UPDATE + "] to active this service.") ;
		}
		
		//key
		StringBuffer sb = new StringBuffer(32) ;		
		sb.append(pkValue)
		  .append(columnToUpdate) 
		  .append(tableName)
		  .append(dbGroup) ;
		
		String key = sb.toString() ;
		
		IncUpdateBusiness ut = (IncUpdateBusiness) this.updateOperations.get(key) ;
		if(ut != null){
			ut.incCount(countToInc) ;
		}else{
			synchronized(insertLock){
				//read again
				ut = (IncUpdateBusiness) this.updateOperations.get(key) ;
				
				if(ut != null){//already created while waiting for the lock
					//do nothing
				}else{
					ut = new IncUpdateBusiness(dbGroup) ;
					
					//put it into the map as soon as possible. 
					this.updateOperations.put(key, ut) ;
				}
			}
			
			//release lock as soon as possible.
			ut.setTableName(tableName) ;
			ut.setPkColunName(pkColName) ;
			
			ut.setColumnToUpdate(columnToUpdate) ;
			ut.setPkValue(pkValue.toString()) ;
			
			//use thread safe method to inc count. Other threads may have already changed the count.
			ut.incCount(countToInc) ;
		}
		
		//just let it sleep. The delay is fine, a big batch is preferred.
//		if(this.updateThread.isSleeping()){
//			synchronized (updateThread) {
//				try{
//					this.updateThread.notify() ;
//				}catch(Exception e){}
//			}
//		}
	}

	public boolean configure(ServiceConfig[] scs) {
		if(scs == null || scs.length == 0){
			//TODO: ??????delegate?????????????????????
			
			//???????????
			return false;
		}
		
		ServiceConfig sc = scs[0] ;
		
		String m_batchSize = (String) sc.getProps().get("batchSize") ;
		String m_updateInterval = (String) sc.getProps().get("updateInterval") ;
		
		this.batchSize = StringUtil.toInt(m_batchSize, this.batchSize) ;
		this.updateInterval = StringUtil.toInt(m_updateInterval, this.updateInterval) ;
		
		return true ;
	}
	
	public void startup() {		
		//??????
		if(updateThread == null){
			updateThread = new UpdateToDBThread() ;
			updateThread.start() ;
			
			log.info("super slow update service started.") ;
		}
	}
	
	public void shutdown() {		
		if(updateThread != null){
			updateThread.shutdown() ;
			updateThread = null ;
		}
	}

	public boolean isAvailable() {
		return this.tm != null && updateThread != null;
	}
	
	class UpdateToDBThread extends DemonQueuedThread{
		
		public UpdateToDBThread(){
			super("superSlowUpdateThread", 0) ;
		}
		
		protected boolean doWithTheQueue() throws Exception{
			Map oldOperations = updateOperations ;
			
			if(oldOperations == null || oldOperations.isEmpty()){
				return false ;
			}
			
			//???Map????????1???Map?update???????
			//?????Map??????Map?count != 0???????????????????????update?????????????????????????????????
			HashMap newMap = new HashMap(2048) ;
		
			/*
			 * 20091231 BUG?????????????????? ?????????????????interval?????????????????????????????
			 * ?????????????????????ConcurrentModifyException??????ConcurrentModifyException???????????????CPU???????
			 * CPU???150%???????????????????????????JDK HashMap?bug?
			 */
			synchronized(insertLock){
				Iterator it = oldOperations.entrySet().iterator() ;
				
				while(it.hasNext()){
					Map.Entry e = (Entry) it.next() ; //
					String key = (String) e.getKey() ;				
					IncUpdateBusiness value = (IncUpdateBusiness) e.getValue() ;
					
					if(value.getCountToInc() != 0){
						IncUpdateBusiness newValue = new IncUpdateBusiness() ;
						newValue.setColumnToUpdate(value.getColumnToUpdate()) ;
						newValue.setCountToInc(0) ;
						newValue.setDbGroup(value.getDbGroup()) ;
						newValue.setId(value.getId()) ;
						newValue.setPkColunName(value.getPkColunName()) ;
						newValue.setPkValue(value.getPkValue()) ;
						newValue.setTableName(value.getTableName()) ;
						
						newMap.put(key, newValue) ;
					}
				}
			}
			
			//????Map
			WriteTranSession tran = tm.openRWTran(false) ; //????????????????????????????
			updateOperations = newMap ;
			
			
			//??Map???????????????????CPU???????incCount???????????????????????
			Iterator i = oldOperations.values().iterator() ;			
			ObjectBatcher batcher = null ;
			int addedCount = 0 ;
			IncUpdateBusiness ut = null ;
			
			try{
				while(i.hasNext()){
					ut = (IncUpdateBusiness) i.next() ;
					if(ut == null) continue ;
					
					//??????????????????CPU??????????????
					ut.incCount(0) ;
					
					if(ut.getCountToInc() == 0) continue ;
					
					if(batcher == null){
						batcher = tran.createObjectBatcher() ;
					}
					
					batcher.insert(ut) ;
					addedCount++ ;
					
					if(addedCount >= batchSize){
						batcher.executeBatch() ;
						tran.commit() ;
						
						addedCount = 0 ;
					}
				}
				
				if(addedCount > 0){
					batcher.executeBatch() ;
					tran.commit() ;
				}
			}catch(Exception e){
				log.error(ut, e) ;
				//??1??1????????
			}finally{
				if(tran != null){
					tran.close() ;
				}
			}
			
			oldOperations.clear() ;
			oldOperations = null ;
			
			//force sleep to reduce conflict.
			return false ;
		}

		protected int getMillSecondsToSleep() {
			return updateInterval ;
		}
		
	}

	public void setGuzzContext(GuzzContext guzzContext) {
		this.tm = guzzContext.getTransactionManager() ;
		this.omm = guzzContext.getObjectMappingManager() ;
	}
}

