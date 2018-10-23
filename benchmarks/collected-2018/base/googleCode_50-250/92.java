// https://searchcode.com/api/result/13445857/

package transformations;

import java.util.*;

import statistics.Sample;
import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class PCA {
	public double [][] pc;
	public double [] lambda;
	
	/**
	 * Compute the parameters for the principal components analysis (PCA). Note
	 * that this requires a prior mean normalization (@see Sample.meanSubstract).
	 * @param data 
	 */
	public void computePCA(List<Sample> data) {
		double [][] rawdata = new double [data.size()][data.get(0).x.length];
		
		for (int i = 0; i < data.size(); ++i)
			for (int j = 0; j < data.get(i).x.length; ++j)
				rawdata[i][j] = data.get(i).x[j];
		
		computePCA(rawdata);
	}
	
	/**
	 * Compute the parameters for the principal components analysis (PCA). Note
	 * that this requires a prior mean normalization (@see Sample.meanSubstract).
	 * @param data 
	 */
	public void computePCA(double [][] data) {
		// This stores the samples in rows and the dimensions in columns. Maths people 
		// store it the other way round, but as we need the transposed, we're fine!
		Matrix Y = new Matrix(data);
		
		// normalize for covariance
		Y.times(1./Math.sqrt(data.length-1));
		
		// perform SVD
		SingularValueDecomposition svd = new SingularValueDecomposition(Y);
		
		// save principal components matrix
		pc = svd.getV().transpose().getArray();
		lambda = svd.getSingularValues();
	}
	
	/**
	 * Transform a vector using the pre-computed principal components
	 * @param x
	 * @return new allocated transformed vector
	 */
	public double [] transform(double [] x) {
		return transform(x, x.length);
	}
	
	/**
	 * Transform a vector using the pre-computed principal components
	 * and reduce the dimensions to the given number
	 * @param x
	 * @param dim new (smaller) dimension of the output vector
	 * @return new allocated transformed and reduced vector
	 */
	public double [] transform(double [] x, int dim) {
		double [] ret = new double [dim];
		for (int i = 0; i < dim; ++i)
			for (int j = 0; j < x.length; ++j)
				ret[i] += pc[i][j] * x[j];
		return ret;
	}
	
	/**
	 * Transform a list of samples using the pre-computed principal
	 * components.
	 * @param in
	 * @return new allocated list of (new) samples
	 */
	public ArrayList<Sample> transform(ArrayList<Sample> in) {
		return transform(in, in.get(0).x.length);
	}
	
	/**
	 * Transform a list of samples using the pre-computed principal
	 * components and reduce the dimsions to the given number.
	 * @param in
	 * @param dim new (smaller) dimension of the samples
	 * @return new allocated list of (new) samples
	 */
	public ArrayList<Sample> transform(ArrayList<Sample> in, int dim) {
		ArrayList<Sample> out = new ArrayList<Sample>();
		for (Sample sin : in) {
			double [] tv = transform(sin.x, dim);
			Sample sou = new Sample(sin.c, tv);			
			out.add(sou);
		}
		return out;
	}
}

