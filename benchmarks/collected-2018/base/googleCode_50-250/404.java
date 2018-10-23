// https://searchcode.com/api/result/13667244/

/**
 *  Copyright (C) 2011  Kyle Thayer <kyle.thayer AT gmail.com>
 *
 *  This file is part of the IFCSoft project (http://ifcsoft.com)
 *
 *  IFCSoft is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ifcSoft.model.dataSet.dataSetScalar;

import ifcSoft.model.dataSet.DataSet;
import java.util.LinkedList;

/**
 *
 * @author kthayer
 */
public class LogScaleNormalized implements DataSetScalar{

  private DataSet dataset;

  private double stddevs[] = null;
  private double means[] = null;

  public LogScaleNormalized(DataSet dataset){
    this.dataset = dataset;
  }

  @Override
  public String getName() {
    return dataset.getName();
  }

  @Override
  public int length() {
    return dataset.length();
  }

  @Override
  public int getDimensions() {
    return dataset.getDimensions();
  }

  @Override
  public int getUnscaledDimensions() {
    return getDimensions();
  }

  @Override
  public float getMax(int dim) {
    return 1;
  }

  @Override
  public float getMin(int dim) {
    return 0;
  }

  @Override
  public double getStdDev(int dim) {
    if(stddevs == null){
      findStandardDevs();
    }
    return stddevs[dim];
  }

  @Override
  public double getMean(int dim) {
    if(means == null){
      findmeans();
    }
    return means[dim];
  }

  @Override
  public float[] getPoint(int index) {
    return scalePoint(dataset.getVals(index));
  }

  @Override
  public float[] getUnscaledPoint(int index) {
    return dataset.getVals(index);
  }

  @Override
  public float[] scalePoint(float[] weights) {
    float [] scaledpt = new float[weights.length];
    for(int i = 0; i < weights.length; i++){
      scaledpt[i] = (float) scaleDim(weights[i], i);
    }
    return scaledpt;
  }

  @Override
  public float[] unscalePoint(float[] weights) {
    float [] scaledpt = new float[weights.length];
    for(int i = 0; i < weights.length; i++){
      scaledpt[i] = (float) unscaleDim(weights[i], i);
    }
    return scaledpt;
  }

  @Override
  public String[] getColLabels() {
    return dataset.getColLabels();
  }

  @Override
  public LinkedList<String> getRawSetNames() {
    return dataset.getRawSetNames();
  }

  @Override
  public String getPointSetName(int index) {
    return dataset.getPointSetName(index);
  }

  private double scaleDim(double val, int dim){
    double returnval;
    float delta = dataset.getMax(dim)- dataset.getMin(dim);
    if(delta == 0){
      returnval = 0;
    }else{
      double scaled = ( val - getMin(dim)) / delta;
      if(9*scaled+1 < 1){
        returnval = 0;
      }else{
        returnval = Math.log10((9*scaled+1)); //9*scales+1 gives range 1-10
      }
    }

    return returnval;
  }

  private double unscaleDim(double val, int dim){
    if(dataset.getMax(dim) == dataset.getMin(dim)){
      return dataset.getMax(dim);
    }
    return Math.pow(10, val) / 9 * (dataset.getMax(dim) - dataset.getMin(dim)) + dataset.getMin(dim);
  }

  @Override
  public DataSet getDataSet(){
    return dataset;
  }

  private void findmeans() {
    means = new double[getDimensions()];
    for(int k = 0; k < getDimensions(); k++){
      means[k] = 0; //for now we'll use it to keep sums
    }

    for(int i = 0; i < length(); i++){
      for(int k = 0; k < getDimensions(); k++){
        //compute averages(at each step it's the current avg. of pts given)
        double weight = getPoint(i)[k];
        means[k] = weight / (i+1) + (means[k]*i)/(i+1);
      }
    }
  }


  private void findStandardDevs() {
    //find means first
    if (means == null){
      findmeans();
    }

    stddevs = new double[getDimensions()];

    for(int i = 0; i < getDimensions(); i++){
      stddevs[i] =0;
    }

    // find the variance (avg of [dist to mean]^2)
    //do this on SegSize at a time, then average those values together, just to reduce rounding errors
    int numSegs = (int) Math.ceil(length() / (double) DataSet.SEGSIZE);
    int segLengths[] = new int[numSegs];
    for(int i = 0; i < numSegs; i++){
      if(i != numSegs-1){
        segLengths[i] = DataSet.SEGSIZE;
      }else{ //last one is remainder
        segLengths[i] = length() -  DataSet.SEGSIZE*(numSegs-1);
      }
    }


    double [][] segVars = new double[numSegs][getDimensions()];
    //0 it out
    for(int i = 0; i < numSegs; i++){
      for(int k = 0; k < getDimensions(); k++){
        segVars[i][k] = 0;
      }
    }
    int sofar = 0;
    for(int i = 0; i < numSegs; i++){
      for(int j = 0; j < segLengths[i]; j++){
        float[] vals = getPoint(sofar);
        sofar++;
        for(int k = 0; k < getDimensions(); k++){
          //compute average of the squares (at each step it's the current avg. of pts given)
          double distToAvg = vals[k] - means[k];
          segVars[i][k] = distToAvg*distToAvg / (j+1) + (segVars[i][k]*j)/(j+1);
        }
      }
    }

    //compute actual varience from the segments
    double variance[] = new double[getDimensions()];
    for(int k = 0; k < getDimensions(); k++){
      variance[k] = 0;
    }
    for(int i = 0; i < numSegs; i++){
      for(int k = 0; k < getDimensions(); k++){
        variance[k] += segVars[i][k] * (segLengths[i] / (double) length());
      }
    }

    //find actual standard devs from the variance we computed
    for(int k = 0; k < getDimensions(); k++){
      stddevs[k] = Math.sqrt(variance[k]);
    }
  }

}

