// https://searchcode.com/api/result/67830977/

package unitTest;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lib.VectorWritable;

import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import org.apache.hadoop.mrunit.mapreduce.MapDriver;
import org.apache.hadoop.mrunit.mapreduce.MapReduceDriver;
import org.junit.Before;
import org.junit.Test;

import diatance.EuclideanDistance;


public class VectorWritableTest {

	private Mapper1 mapper;
	private Mapper2 mapper2;
	private Reducer1 reducer;
	private MapReduceDriver<Text,DoubleWritable,Text,VectorWritable,Text,VectorWritable> mapreduceDriver;
	MapDriver<Text,DoubleWritable,Text,VectorWritable> mapDriver;
	MapDriver<Text,DoubleWritable,Text,ArrayWritable> mapDriver2;
	
	public static class Mapper1 extends Mapper<Text,DoubleWritable,Text,VectorWritable> {
		@Override
		public void map(Text key, DoubleWritable value, Context context) {
			try {
				List<Double> list = new ArrayList<Double>();
				VectorWritable vector = new VectorWritable();
				list.add(1.1);
				list.add(12.1);
				vector.setData(list);
				context.write(key, vector);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static class Mapper2 extends Mapper<Text,DoubleWritable,Text,ArrayWritable> {
		@Override
		public void map(Text key, DoubleWritable value, Context context) {
			try {
				ArrayWritable array = new ArrayWritable(DoubleWritable.class);
				array.set(new DoubleWritable[]{new DoubleWritable(1.1)});
				context.write(key, array);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static class Reducer1 extends Reducer<Text,VectorWritable,Text,VectorWritable> {
		@Override
		public void reduce (Text key, Iterable<VectorWritable> values, Context context) {
			for (VectorWritable vector : values) {
				System.out.println(vector);
				try {
					context.write(key, vector);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	}
	
	@Before
	public void setUp() {
		mapper = new Mapper1();
		mapper2 = new Mapper2();
		reducer = new Reducer1();
		mapreduceDriver = new MapReduceDriver<Text,DoubleWritable,Text,VectorWritable,Text,VectorWritable>();
		mapDriver = new MapDriver<Text,DoubleWritable,Text,VectorWritable>();
		mapDriver2 = new MapDriver<Text,DoubleWritable,Text,ArrayWritable>();
		mapreduceDriver.setMapper(mapper);
		mapreduceDriver.setReducer(reducer);
		mapDriver.withMapper(mapper);
		mapDriver2.setMapper(mapper2);
	}

	
	@Test
	public void testMap() {

		mapDriver.withInput(new Text("a"), new DoubleWritable(2.2));
		List<Double> list = new ArrayList<Double>();
		VectorWritable vector = new VectorWritable();
		list.add(1.1);
		list.add(12.1);
		vector.setData(list);
		mapDriver.withOutput(new Text("a"), vector);
//		
		mapDriver.runTest();
//		mapreduceDriver.runTest();
	}
	
	@Test
	public void testMapReducer() {
		
		mapreduceDriver.withInput(new Text("a"), new DoubleWritable(2.2));
		List<Double> list = new ArrayList<Double>();
		VectorWritable vector = new VectorWritable();
		list.add(1.1);
		list.add(12.1);
		vector.setData(list);
		
		VectorWritable vector1 = new VectorWritable();
		list.add(1.1);
		list.add(12.1);
		vector1.setData(list);

		mapreduceDriver.withOutput(new Text("a"), vector);
//		
		mapreduceDriver.runTest();
	}
	

	@Test
	public void testEuclideanDistance() {
		List<Double> list1 = new ArrayList<Double>();
		VectorWritable vector1 = new VectorWritable(5);
		list1.add(1.1);
		list1.add(0.);
		list1.add(12.1);
		vector1.setData(list1);
		
		List<Double> list2 = new ArrayList<Double>();
		VectorWritable vector2 = new VectorWritable(5);
		list2.add(0.);
		list2.add(1.1);
		list2.add(12.1);
		vector2.setData(list2);
		
		double result = new EuclideanDistance().computeDistance(vector1, vector2);
		assertEquals(2.42, result,0.001);
	}
	
	public static void main (String[] args) {
		List<Double> list = new ArrayList<Double>();
		VectorWritable vector = new VectorWritable();
		list.add(1.1);
		list.add(12.1);
		vector.setData(list);
		
		List<Double> list1 = new ArrayList<Double>();
		VectorWritable vector1 = new VectorWritable();
		list1.add(1.1);
		list1.add(12.1);
		vector1.setData(list1);
		
		System.out.println(vector.hashCode()+":"+vector1.hashCode());
		
		if (vector.equals(vector1)) {
			System.out.println("okok");
		}
		System.out.println(Integer.MAX_VALUE+100);
		String a = "";
		
	}
}

