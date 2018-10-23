// https://searchcode.com/api/result/47560872/

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

import data.Edge;
import data.Relationship;
import data.State;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;


public class GenericReducer<E extends State> extends Reducer<Text, E, Text, E> {
	
	private ArrayList<E> oldReduced=null;
	
	private class NodePair {
		private E a;
		private E b;
		
		public NodePair(E a, E b){
			this.a=a;
			this.b=b;
		}
		
		@Override
		public boolean equals(Object obj){
			@SuppressWarnings("unchecked")
			NodePair pair=(NodePair)obj;
			return (a.equals(pair.a) || a.equals(pair.b)) && (b.equals(pair.b) || b.equals(pair.a));
		}
	}
	
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		oldReduced=new ArrayList<E>();
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<E> fromIterable(Iterable<E> values){
	   ArrayList<E> list=new ArrayList<E>();
	   for(E c: values)
		   list.add((E)c.clone());
	   return list;
	}
	
	@Override
	public void reduce(Text key, Iterable<E> values, Context context) throws IOException, InterruptedException{
		ArrayList<NodePair> reducedPairs=new ArrayList<NodePair>();
		ArrayList<E> nodeList=fromIterable(values);
		   
		if(nodeList.size()==1){
			context.write(key, nodeList.get(0));
		}
		else{
		   ArrayList<E> toWrite=new ArrayList<E>(nodeList);
		   for(E n : nodeList) {
			   if(toWrite.contains(n)){
				   for(E o : toWrite) {
					   context.progress(); // report progress to keep alive this task.
					   
					   NodePair p = new NodePair(n, o);
					   if(!n.equals(o) && !(n.isExpanded() && o.isExpanded()) && !reducedPairs.contains(p)){
						   // case n is new, o is old. Or n is old, o is new. Or n is new, o is new.
						   reducedPairs.add(p);
						   
						   E newNode, oldNode;
						   if(!n.isExpanded()){
							   newNode=n;
							   oldNode=o;
						   }
						   else{
							   newNode=o;
							   oldNode=n;
						   }
						   // reduce(new, old) Or reduce(new, new)
						   Relationship relationship = newNode.identifyRelationship(oldNode);
						   if(relationship != Relationship.NONE){
							   if(relationship == Relationship.INCLUDES){
								   for(Edge e: oldNode.getIncomingEdges()){
										e.setType(e.getBeginType(), 'E');
										newNode.addIncomingEdge(e);
									}
								   toWrite.remove(oldNode);
								   if(oldNode.isExpanded()) // new includes old
									   oldReduced.add(oldNode);
							   }
							   else{
								   if(relationship == Relationship.EQUALS)
									   oldNode.addIncomingEdges(newNode.getIncomingEdges());
								   else{ // INCLUDED
									   for(Edge e: newNode.getIncomingEdges()){
											e.setType(e.getBeginType(), 'E');
											oldNode.addIncomingEdge(e);
										}
								   }  
								   toWrite.remove(newNode);
							   }
							   
							   break;
						   } 
					   }
				   }
			   }
	       }
			   
		   for(E c: toWrite)
			   context.write(key, c);
	   }
	}
	
	
	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		super.cleanup(context);
	   	if(oldReduced.size()>0){
	   		String fileName="reduced-" + context.getTaskAttemptID().toString();
	   		FileSystem fs=null;
			try {
				String currentDfsUri = context.getConfiguration().get(GenericGraphgen.DFS_URI);
				fs = FileSystem.get(new URI(currentDfsUri), context.getConfiguration());
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
			String currentReducedDir = context.getConfiguration().get(GenericGraphgen.REDUCED_DIR_NAME);
   
			SequenceFile.Writer writer = null;
			
				writer = SequenceFile.createWriter(fs,
				   context.getConfiguration(),
				   new Path(currentReducedDir + "/" +fileName),
					   Text.class, Text.class);
	   
		   for(E n: oldReduced){
			   Text t = new Text(n.getName());
			   writer.append(t, t);
		   }
		   writer.close();
	   	}
	   
	   	oldReduced.clear();
	}
}

