// https://searchcode.com/api/result/67831060/

package cluster;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lib.Point;
import lib.VectorWritable;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class CentroidReducer extends Reducer<IntWritable,Point,IntWritable,Point>{
	
	@Override
	public void reduce (IntWritable centerId, Iterable<Point> points, Context context) {
		Point newCenter = createNewCenter(points);
		newCenter.setId(centerId.get());
		
		try {
			context.write(new IntWritable(newCenter.getId()), newCenter);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Point createNewCenter(Iterable<Point> points) {
		Point center = new Point();
		HashMap<Integer,Double> sumMap = new HashMap<Integer,Double>();
		int count=0;
		for (Point point : points) {
			Map<Integer,Double> data = point.getData().getData();
			for (Map.Entry<Integer, Double> entry : data.entrySet()) {
				if (sumMap.containsKey(entry.getKey())) {
					double sum = sumMap.get(entry.getKey()) + entry.getValue();
					sumMap.put(entry.getKey(), sum);
				}
				else {
					sumMap.put(entry.getKey(), entry.getValue());
				}
			}
			count++;
			
		}
		if (count == 0) {
			return center;
		}
		for (Map.Entry<Integer, Double> entry : sumMap.entrySet()) {
			sumMap.put(entry.getKey(), entry.getValue() / count);
		}
		VectorWritable vector = new VectorWritable();
		vector.setData(sumMap);
		center.setData(vector);
		
		return center;
	}
}

