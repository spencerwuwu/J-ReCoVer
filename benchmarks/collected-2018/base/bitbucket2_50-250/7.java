// https://searchcode.com/api/result/64576633/

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

package core.deadlock;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import data.State;

public class DeadlockReducer<E extends State> extends Reducer<Text, E, Text, Text> {

	@Override
	protected void reduce(Text key, Iterable<E> list, Context context) throws IOException, InterruptedException {
		//super.reduce(key, list, context);
		//System.out.println("REDUCE CALL*******");
		boolean fakeFound = false;
		for(E e: list){
			//System.out.println(e.getName());
			if(e.getName().equals("F")){ //optimization: check just the first element (if list is ordered)
				fakeFound = true;
				break;
			}
		}
		if(!fakeFound)
			context.write(key, new Text());
		
		//context.write(key, new Text("asd"));
	}


}

