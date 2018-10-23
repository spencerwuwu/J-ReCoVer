// https://searchcode.com/api/result/64576662/

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

package core.ctl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;

import data.Model;
import data.State;

public class LocalPropertyReducer<E extends State> extends Reducer<Text, E, Text, E> {
	
	private data.Model model = null;
	private HashMap<String,Integer> pMap = null;
	private MultipleOutputs<Text, E> mos = null;
	
	
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		//super.setup(context);
		mos = new MultipleOutputs<Text, E>(context);
		Path[] localFiles = DistributedCache.getLocalCacheFiles(context.getConfiguration());
		File inputFile = new File(localFiles[0].toString());
		InputStream in = new FileInputStream(inputFile);
		String modelClassName = context.getConfiguration().get(EXLauncher.MODEL_CLASS_NAME);
		try {
			model = (Model) Class.forName(modelClassName).newInstance();			
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		model.buildFromFile(in);
		in.close();
		
		pMap = model.getPMap();
		//tMap = model.getTMap();
	}

	@Override
	protected void reduce(Text key, Iterable<E> list, Context context) throws IOException, InterruptedException {
		for(E e: list){
			if(testSubFormula(e))
				mos.write("true", key, e);
			else
				context.write(key, e);
		}

	}

	private boolean testSubFormula(State state){
		/* (|marking(Active)|!=|marking(Memory)|) | (|marking(Queue)|=|marking(Active)|) */

		short[] marking = state.get();

		short active1 = marking[pMap.get("cId-4690664751711947012104")];
		short active2 = marking[pMap.get("cId-4690664751711947012105")];
		short active3 = marking[pMap.get("cId-4695622201361999396106")];
		short active4 = marking[pMap.get("cId-4690664751711947012107")];
		short active5 = marking[pMap.get("cId-4695622201361999396108")];
		short active6 = marking[pMap.get("cId-4695622201361999396109")];
		short active7 = marking[pMap.get("cId-4693969713850347972110")];
		short active8 = marking[pMap.get("cId-4693969713850347972111")];
		short active9 = marking[pMap.get("cId-4700579651012051780112")];
		short active10 = marking[pMap.get("cId-4692317230633663844113")];

		short queue1 = marking[pMap.get("cId-4692317230633663844114")];
		short queue2 = marking[pMap.get("cId-4692317230633663844115")];
		short queue3 = marking[pMap.get("cId-4692317230633663844116")];
		short queue4 = marking[pMap.get("cId-4692317230633663844117")];
		short queue5 = marking[pMap.get("cId-4692317230633663844118")];
		short queue6 = marking[pMap.get("cId-4692317230633663844119")];
		short queue7 = marking[pMap.get("cId-4700579651012051780120")];
		short queue8 = marking[pMap.get("cId-4700579651012051780121")];
		short queue9 = marking[pMap.get("cId-4700579651012051780122")];
		short queue10 = marking[pMap.get("cId-4700579651012051780123")];

		short memory1 = marking[pMap.get("cId-462952284262986320394")];
		short memory2 = marking[pMap.get("cId-462787035511821177995")];
		short memory3 = marking[pMap.get("cId-468735978098361146096")];
		short memory4 = marking[pMap.get("cId-468901226420029558897")];
		short memory5 = marking[pMap.get("cId-468735978098361146098")];
		short memory6 = marking[pMap.get("cId-468735978098361146099")];
		short memory7 = marking[pMap.get("cId-4626217871901527650100")];
		short memory8 = marking[pMap.get("cId-4626217871901527650101")];
		short memory9 = marking[pMap.get("cId-4624565392979810818102")];
		short memory10 = marking[pMap.get("cId-4624565392979810818103")];


		return eval2(active1, memory1, queue1) &&
				eval2(active2, memory2, queue2) &&
				eval2(active3, memory3, queue3) &&
				eval2(active4, memory4, queue4) &&
				eval2(active5, memory5, queue5) &&
				eval2(active6, memory6, queue6) &&
				eval2(active7, memory7, queue7) &&
				eval2(active8, memory8, queue8) &&
				eval2(active9, memory9, queue9) &&
				eval2(active10, memory10, queue10);
	}

	private boolean eval2(short active, short memory, short queue){
		return active != memory || queue == active;
	}
	
//	private boolean testSubFormula(State state){
//	/* (|marking(Queue)|!=|marking(Active)|) | (|marking(OwnMemAcc)|>|marking(Active)|) */
//	
//	short[] marking = state.get();
//	
//	// just to show...
//	short queue1 = marking[pMap.get("cId-4692317230633663844114")];
//	short queue2 = marking[pMap.get("cId-4692317230633663844115")];
//	short queue3 = marking[pMap.get("cId-4692317230633663844116")];
//	short queue4 = marking[pMap.get("cId-4692317230633663844117")];
//	short queue5 = marking[pMap.get("cId-4692317230633663844118")];
//	short queue6 = marking[pMap.get("cId-4692317230633663844119")];
//	short queue7 = marking[pMap.get("cId-4700579651012051780120")];
//	short queue8 = marking[pMap.get("cId-4700579651012051780121")];
//	short queue9 = marking[pMap.get("cId-4700579651012051780122")];
//	short queue10 = marking[pMap.get("cId-4700579651012051780123")];
//	
//	short active1 = marking[pMap.get("cId-4690664751711947012104")];
//	short active2 = marking[pMap.get("cId-4690664751711947012105")];
//	short active3 = marking[pMap.get("cId-4695622201361999396106")];
//	short active4 = marking[pMap.get("cId-4690664751711947012107")];
//	short active5 = marking[pMap.get("cId-4695622201361999396108")];
//	short active6 = marking[pMap.get("cId-4695622201361999396109")];
//	short active7 = marking[pMap.get("cId-4693969713850347972110")];
//	short active8 = marking[pMap.get("cId-4693969713850347972111")];
//	short active9 = marking[pMap.get("cId-4700579651012051780112")];
//	short active10 = marking[pMap.get("cId-4692317230633663844113")];
//	
//	short ownmemacc1 = marking[pMap.get("cId-4700579651012051780124")];
//	short ownmemacc2 = marking[pMap.get("cId-4700579651012051780125")];
//	short ownmemacc3 = marking[pMap.get("cId-4700579651012051780126")];
//	short ownmemacc4 = marking[pMap.get("cId-4700579651012051780127")];
//	short ownmemacc5 = marking[pMap.get("cId-4702232138523703204128")];
//	short ownmemacc6 = marking[pMap.get("cId-4700579651012051780129")];
//	short ownmemacc7 = marking[pMap.get("cId-4702232138523703204130")];
//	short ownmemacc8 = marking[pMap.get("cId-4702232138523703204131")];
//	short ownmemacc9 = marking[pMap.get("cId-4702232138523703204132")];
//	short ownmemacc10 = marking[pMap.get("cId-4702232138523703204133")];
//	
//	boolean ret = eval(queue1, active1, ownmemacc1) ||
//			eval(queue2, active2, ownmemacc2) ||
//			eval(queue3, active3, ownmemacc3) ||
//			eval(queue4, active4, ownmemacc4) ||
//			eval(queue5, active5, ownmemacc5) ||
//			eval(queue6, active6, ownmemacc6) ||
//			eval(queue7, active7, ownmemacc7) ||
//			eval(queue8, active8, ownmemacc8) ||
//			eval(queue9, active9, ownmemacc9) ||
//			eval(queue10, active10, ownmemacc10);
//	//System.out.println(queue + "!=" + active + " || " + ownmemacc + ">" + active + " -> " + eval);
//	
//	return ret;
//}
//
//private boolean eval(short queue, short active, short ownmemacc){
//	return (queue != active) || (ownmemacc > active);
//}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		mos.close();
	}

}

