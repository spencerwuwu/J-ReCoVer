// https://searchcode.com/api/result/71732683/

package mapreduce;

import java.util.*;
import java.util.concurrent.*;

public class MapReduce<InputMapKey extends Comparable<InputMapKey> , InputMapValue, IntermediateKey extends Comparable<IntermediateKey> , IntermediateValue, OutputReduceKey, OutputReduceValue>{
	
	private Class<? extends Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>> mapClass;
	private Class<? extends Reducer< IntermediateKey, IntermediateValue, OutputReduceKey, OutputReduceValue>> reduceClass;
	
	private InputData<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue> inputData;
	private OutputData<OutputReduceKey, OutputReduceValue> outputData;
	
	/*
	 * phase_mp
	 * phase_mp"MAP_ONLY"Map
	 * "MAP_SHUFFLE"MapShuffle
	 * "MAP_REDUCE"0MapShuffleReduce
	 * "MAP_REDUCE"
	 */
	
	private String phaseMR;

	//
	private boolean resultOutput;
	
	//The Number of concurrent threads
	private int parallelThreadNum;
	

	
	public MapReduce(
			Class<? extends Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>> map_class, 
			Class<? extends  Reducer< IntermediateKey, IntermediateValue, OutputReduceKey, OutputReduceValue>> reduce_class, 
			String phase_mp
			){
		this.mapClass = map_class;
		this.reduceClass = reduce_class;
		this.inputData = new InputData<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>();
		this.outputData = new OutputData<OutputReduceKey, OutputReduceValue>();
		this.phaseMR = phase_mp;
		this.resultOutput = true;
		this.parallelThreadNum = 1;
	}
	
	public void setPhaseMR(String phaseMR){
		this.phaseMR = phaseMR;
	}
	
	public void setResultOutput(boolean resultOutput){
		this.resultOutput = resultOutput;
	}
	
	public void setParallelThreadNum(int num){
		this.parallelThreadNum = num;
	}
	
	
	/*
	 * addKeyValue
	 * pass data formated as key-value pairs to inputData
	 */
	
	public void addKeyValue(InputMapKey key, InputMapValue value){
		this.inputData.putKeyValue(key, value);
	}
	
	/*
	 * Map
	 * Key-Value
	 * 1.Mapper
	 * 2.1.FutureTask
	 * 3.MapWorkinputData
	 * 4.1.-3.
	 */
	private void startMap(){
		List<Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>> mappers =  new ArrayList<Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>>(this.parallelThreadNum);
		List<FutureTask<Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>>> maptasks = new ArrayList<FutureTask<Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>>>(this.parallelThreadNum);
		ExecutorService executor = Executors.newFixedThreadPool(this.parallelThreadNum);
		
		
		for(int i = 0; i < this.parallelThreadNum; i ++){
			mappers.add(initializeMapper());
			maptasks.add(new FutureTask<Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>>(new MapCallable<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>()));
		}
			
		int x = this.inputData.getMapSize() / this.parallelThreadNum;
		for(int i = 0; i < this.inputData.getMapSize() / this.parallelThreadNum; i++){
			for(int j = 0; j < this.parallelThreadNum; j++){
				mappers.set(j, initializeMapper());
				mappers.get(j).setKeyValue(this.inputData.getMapKey(i+j*x), this.inputData.getMapValue(i+j*x));
				maptasks.set(j, new FutureTask<Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>>(new MapCallable<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>(mappers.get(j))));
			}
			
			try{
				MapWork(mappers, maptasks, executor);
			}catch(OutOfMemoryError e){
				System.out.println(i);
				System.exit(1);
			}
		} 
				
		
		if(this.inputData.getMapSize() % this.parallelThreadNum != 0){
			int finishedsize = (this.inputData.getMapSize() / this.parallelThreadNum) * this.parallelThreadNum;
			for(int i = 0; i < this.inputData.getMapSize() % this.parallelThreadNum; i++){
				mappers.set(i, initializeMapper());
				mappers.get(i).setKeyValue(this.inputData.getMapKey(finishedsize + i), this.inputData.getMapValue(finishedsize + i));
				maptasks.set(i, new FutureTask<Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>>(new MapCallable<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>(mappers.get(i))));
			}
			
			MapWork(mappers, maptasks, executor);
		}
		
		mappers = null;
		maptasks = null;
		//Mapinput_data
		this.inputData.initialRelease();
		executor.shutdown();
	}
	
	private void MapWork(
			List<Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>> mappers,
			List<FutureTask<Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue>>> maptasks,
			ExecutorService executor
			){

		for(int i = 0; i < this.parallelThreadNum; i++){
			executor.submit(maptasks.get(i));
		}
					
		//Map				
		try{
			for(int i = 0; i < this.parallelThreadNum; i++){
			List<IntermediateKey> resultMapKeys = maptasks.get(i).get().getKeys();
			List<IntermediateValue> resultMapValues = maptasks.get(i).get().getValues();
			for(int j = 0; j < resultMapKeys.size(); j++)
				this.inputData.setMap(resultMapKeys.get(j), resultMapValues.get(j));
			}
		}catch(InterruptedException e){
			e.getCause().printStackTrace();
		}catch(ExecutionException e){
			e.getCause().printStackTrace();
		}
	}
	
	
	/**
	 * Mapper
	 * @return Mapper
	 */
	Mapper<InputMapKey, InputMapValue, IntermediateKey, IntermediateValue> initializeMapper(){
		try{
			return mapClass.newInstance();
		}catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	*ReducePhases
	*1.MapKey-ValueKey
	*2.KeyKey-Value	
	*/
	private void startShuffle(){
		this.inputData.cSort();
		this.inputData.grouping();
	}
		
	
	
	/**
	 * Reduce
	 * Key-Value
	 * 1.Reducer
	 * 2.1.FutureTask
	 * 3.ReduceWorkoutputData
	 * 4.1.-3.
	 */
	private void startReduce(){
		List<Reducer<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>> reducers =  new ArrayList<Reducer<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>>(this.parallelThreadNum);
		List<FutureTask<Reducer<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>>> reducetasks = new ArrayList<FutureTask<Reducer<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>>>(this.parallelThreadNum);
		ExecutorService executor = Executors.newFixedThreadPool(this.parallelThreadNum);				
		
		for(int i = 0; i < this.parallelThreadNum; i ++){
			reducers.add(initializeReducer());
			reducetasks.add(new FutureTask<Reducer<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>>(new ReduceCallable<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>()));
		}
		
		int x = this.inputData.getReduceSize() / this.parallelThreadNum;
		for(int i = 0; i < this.inputData.getReduceSize() / this.parallelThreadNum; i++){
			for(int j = 0; j < this.parallelThreadNum; j++){
				reducers.set(j, initializeReducer());
				reducers.get(j).setKeyValue(this.inputData.getReduceKey(i+j*x), this.inputData.getReduceValues(i+j*x));
				reducetasks.set(j, new FutureTask<Reducer<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>>(new ReduceCallable<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>(reducers.get(j))));
			}
			
			ReduceWork(reducers, reducetasks, executor);
		}
		

		if(this.inputData.getReduceSize() % this.parallelThreadNum != 0){
			int finishedsize = (this.inputData.getReduceSize() / this.parallelThreadNum) * this.parallelThreadNum;
			for(int i = 0; i < this.inputData.getReduceSize() % this.parallelThreadNum; i++){
				reducers.set(i, initializeReducer());
				reducers.get(i).setKeyValue(this.inputData.getReduceKey(finishedsize + i), this.inputData.getReduceValues(finishedsize + i));
				reducetasks.set(i, new FutureTask<Reducer<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>>(new ReduceCallable<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>(reducers.get(i))));			}
			
			ReduceWork(reducers, reducetasks, executor);
		}
		
		reducers = null;
		reducetasks = null;
		executor.shutdown();
	}
	
	/**
	 * MapReduce
	 * @param reducers 
	 * @param reducetasks
	 * @param executor
	 */
	private void ReduceWork(
			List<Reducer<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>> reducers,
			List<FutureTask<Reducer<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue>>> reducetasks,
			ExecutorService executor
			){
		
		
		for(int i = 0; i < this.parallelThreadNum; i++){
			executor.submit(reducetasks.get(i));
		}
						
		try{
			for(int j = 0; j < this.parallelThreadNum; j++){
				OutputReduceKey resultMapKey = reducetasks.get(j).get().getKey();
				OutputReduceValue resultMapValue = reducetasks.get(j).get().getValue();
				this.outputData.setKeyValue(resultMapKey, resultMapValue);
			}
		}catch(InterruptedException e){
			e.getCause().printStackTrace();
		}catch(ExecutionException e){
			e.getCause().printStackTrace();
		}
	}
	
	/**
	 * Reducer
	 * @return Reducer
	 */
	Reducer<IntermediateKey, IntermediateValue,OutputReduceKey, OutputReduceValue> initializeReducer(){
		try{
			return reduceClass.newInstance();
		}catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * MapReduce
	 */
	public void run(){
		startMap();
		if(this.phaseMR.equals("MAP_ONLY")){
			if(this.resultOutput)
				inputData.showMap();
			return;
		}

		startShuffle();
		if(this.phaseMR.equals("MAP_SHUFFLE")){
			if(this.resultOutput)
				inputData.showSuffle();
			return;
		}
		startReduce();
		if(this.resultOutput)
			outputData.reduceShow();
	}
	
	/**
	 * MapReduce
	 * getValues()
	 * @return 
	 */
	public List<OutputReduceKey> getKeys(){
		return this.outputData.getOutputKeys();
	}
	
	/**
	 * MapReduce
	 * getKeys()
	 * @return 
	 */
	public List<OutputReduceValue> getValues(){
		return this.outputData.getOutputValues();
	}	
}


