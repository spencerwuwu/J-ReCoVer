// https://searchcode.com/api/result/98860640/

/**
 * Copyright 2012 Jee Vang 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *  
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 */
package demo;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Secondary sort reducer.
 * @author Jee Vang
 *
 */
public class SsReducer extends Reducer<StockKey, DoubleWritable, Text, Text> {

	private static final Log _log = LogFactory.getLog(SsReducer.class);
	
	@Override
	public void reduce(StockKey key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
		Text k = new Text(key.toString());
		int count = 0;
		
		Iterator<DoubleWritable> it = values.iterator();
		while(it.hasNext()) {
			Text v = new Text(it.next().toString());
			context.write(k, v);
			_log.debug(k.toString() + " => " + v.toString());
			count++;
		}
		
		_log.debug("count = " + count);
	}
}

