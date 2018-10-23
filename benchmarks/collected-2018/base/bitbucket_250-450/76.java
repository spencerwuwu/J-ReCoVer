// https://searchcode.com/api/result/47560868/

/**
 * This file is part of Mardigras.
 * 
 *  Mardigras is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  Mardigras is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Mardigras. If not, see <http://www.gnu.org/licenses/>.
 *       
 * @author Matteo Camilli <matteo.camilli@unimi.it>
 *
 */

package core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;


import data.Edge;
import data.State;

public class SimpleReducer<E extends State> extends Reducer<Text, E, Text, E> {
	
	private int reduceInOldNodesCounter;
	private int reduceInNewNodesCounter;
	private int reduceOutOldNodesCounter;
	private int reduceOutNewNodesCounter;
//	private int arcsCounter;
	
	private boolean schimmy=false;
	Class<? extends State> stateClass = null;
	private MultipleOutputs<Text, E> mos;
	private SequenceFile.Reader reader = null;
	
	private E prevState = null;
	private Text prevKey = null;
	

	@SuppressWarnings("unchecked")
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		reduceInOldNodesCounter = 0;
		reduceInNewNodesCounter = 0;
		reduceOutOldNodesCounter = 0;
		reduceOutNewNodesCounter = 0;
//		arcsCounter = 0;
		
		
		schimmy = context.getConfiguration().getBoolean(GenericGraphgen.SCHIMMY, false);
		if(schimmy){
			mos = new MultipleOutputs<Text, E>(context);
			String stateClassName = context.getConfiguration().get(GenericGraphgen.STATE_CLASS_NAME);
			int iteration = context.getConfiguration().getInt(GenericGraphgen.ITERATION_NUMBER, 0);
			String[] tokens = context.getTaskAttemptID().toString().split("_");
			String suffix = "old-r-" + tokens[4].substring(1); // e.g. old-r-0001
			
			URI dfsUri = null;
			FileSystem fs=null;
			try {
				dfsUri = new URI(context.getConfiguration().get(GenericGraphgen.DFS_URI));
				fs = FileSystem.get(dfsUri, context.getConfiguration());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			
			try {
				stateClass = (Class<? extends State>) Class.forName(stateClassName);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			
			int j = iteration;
			Path path = new Path(dfsUri + "/input" + j + "/" + suffix); // e.g. input1/old-r-0001
			while(j >= j && !fs.exists(path))
				path = new Path(dfsUri + "/input" + (--j) + "/old" + suffix);
			
			if(j >= j)
				reader=new SequenceFile.Reader(fs, path, context.getConfiguration());
			
		}
	}


	@Override
	public void reduce(Text key, Iterable<E> values, Context context) throws IOException, InterruptedException{
	
		if(!schimmy){
//			if(prevKey != null){
//				if(prevKey.compareTo(key) > 0)
//					throw new IllegalArgumentException("test failed!");
//			}
//			prevKey = new Text(key.toString());
			
			Iterator<E> statesIter = values.iterator();
			E temp = (E)statesIter.next();
			@SuppressWarnings("unchecked")
			E outputState = (E)temp.clone();
			
			// TEMP !!!
//			System.out.println("STATE: " + outputState.getName());
//			for(Edge e: outputState.getIncomingEdges())
//				System.out.println("edge from: " + e.getSource());
		
			if(outputState.isExpanded())
				reduceInOldNodesCounter++;
			else
				reduceInNewNodesCounter++;
		
			while(statesIter.hasNext()){
				E state = statesIter.next();
				
				if(state.isExpanded())
					reduceInOldNodesCounter++;
				else
					reduceInNewNodesCounter++;
				
				// TEMP !!
//				System.out.println("STATE: " + state.getName());
//				for(Edge e: state.getIncomingEdges())
//					System.out.println("edge from: " + e.getSource());
				
				
				outputState.addIncomingEdges(state.getIncomingEdges());
				if(state.isExpanded()){
					outputState.setExpanded();
					outputState.setName(state.getName());
				}
				
				context.progress();
			}
			if(outputState.isExpanded())
				reduceOutOldNodesCounter++;
			else
				reduceOutNewNodesCounter++;
			
//			arcsCounter += outputState.getIncomingEdges().size();
			context.write(key, outputState);
		}
		else{ // PARALLEL MERGE JOIN
			
			Iterator<E> statesIter = values.iterator();
			E temp = (E)statesIter.next();
			@SuppressWarnings("unchecked")
			E outputState = (E)temp.clone();
			while(statesIter.hasNext()){
				E state = statesIter.next();
				outputState.addIncomingEdges(state.getIncomingEdges());
				if(state.isExpanded())
					outputState.setExpanded();
			}
			
			if(reader == null){ // for example the first iteration
				if(outputState.isExpanded()){
					reduceOutOldNodesCounter++;				
					mos.write("old", key, outputState);
				}else{
					reduceOutNewNodesCounter++;
					context.write(key, outputState);
				}
			}else{
				boolean proceed = true;
				boolean written = false;
				
				if(prevKey != null){
					int result = process(key, outputState, prevKey, prevState, context);
					if(result != 2){
						prevKey = null;
						prevState = null;
					}
					proceed = (result == 0);
					written = (result != 0);
				}
				
				if(proceed){
					// scan the old file until you find (or pass) the current key			
					Text k = new Text();
					try {
						E v = (E)stateClass.newInstance();
						while(reader.next(k, v)){
							context.progress();
							int result = process(key, outputState, k, v, context);
							if(result != 0){
								if(result == 2){
									prevKey = new Text(k);
									prevState = (E)v.clone();
								}
								written = true;
								break;
							}	   
						}
					
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
				
				if(!written)
					if(outputState.isExpanded()){
					   reduceOutOldNodesCounter++;
					   mos.write("old", key, outputState);
					}else{
					   reduceOutNewNodesCounter++;
					   context.write(key, outputState);
					}
				
			}
		}
	}
	
	
	private int process(Text inKey, E inState, Text oldKey, E oldState, Context context) throws IOException, InterruptedException {
		int c = oldKey.compareTo(inKey);
		if(c < 0){
			reduceOutOldNodesCounter++;		
			mos.write("old", oldKey, oldState);
			return 0;
		} else if(c == 0){
			reduceOutOldNodesCounter++;
			oldState.addIncomingEdges(inState.getIncomingEdges());
			mos.write("old", oldKey, oldState);
			return 1;
		} else {
			// case (c > 0)
			if(inState.isExpanded()){
				reduceOutOldNodesCounter++;
				mos.write("old", inKey, inState);
			}else{
				reduceOutNewNodesCounter++;
				context.write(inKey, inState);
			}
			return 2;
		}
	}


	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		
		if(schimmy){
			if(prevKey != null){
				reduceOutOldNodesCounter++;
				mos.write("old", prevKey, prevState);
			}
			if(reader != null){
				Text k = new Text();
				try {
					E v = (E)stateClass.newInstance();
					while(reader.next(k, v)){
						context.progress();
						reduceOutOldNodesCounter++;
						mos.write("old", k, v);
					}
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				reader.close();
			}
			mos.close();
		}
		
		context.getCounter(GenericGraphgen.MardigrasCounter.REDUCE_IN_OLD_NODES).increment(reduceInOldNodesCounter);
		context.getCounter(GenericGraphgen.MardigrasCounter.REDUCE_IN_NEW_NODES).increment(reduceInNewNodesCounter);
		context.getCounter(GenericGraphgen.MardigrasCounter.REDUCE_OUT_OLD_NODES).increment(reduceOutOldNodesCounter);
		context.getCounter(GenericGraphgen.MardigrasCounter.REDUCE_OUT_NEW_NODES).increment(reduceOutNewNodesCounter);
//		context.getCounter(GenericGraphgen.MardigrasCounter.ARCS).increment(arcsCounter);
	}

}

