// https://searchcode.com/api/result/47560869/

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
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import data.State;

public class SimpleCombiner<E extends State> extends Reducer<Text, E, Text, E> {
	
	@Override
	public void reduce(Text key, Iterable<E> values, Context context) throws IOException, InterruptedException{
		
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
		context.write(key, outputState);
	}


}

