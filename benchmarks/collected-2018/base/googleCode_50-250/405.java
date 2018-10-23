// https://searchcode.com/api/result/13667245/

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
public class VarianceNormalized implements DataSetScalar{
  private DataSet dataset;

  double stddevs[];

  public VarianceNormalized(DataSet dataset){
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
    return (float)scaleDim(dataset.getMax(dim), dim);
  }

  @Override
  public float getMin(int dim) {
    return (float)scaleDim(dataset.getMin(dim), dim);
  }

  @Override
  public double getStdDev(int dim) {
    if(stddevs == null){
      findStandardDevs();
    }
    return stddevs[dim];//should be 1
    //return scaleDim(dataset.getStdDev(dim), dim);
  }

  @Override
  public double getMean(int dim) {
    return scaleDim(dataset.getMean(dim), dim); //should be 0
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
    if(dataset.getStdDev(dim) == 0){
      return 0;
    }
    return (val - dataset.getMean(dim)) / dataset.getStdDev(dim);
  }

  private double unscaleDim(double val, int dim){
    if(dataset.getStdDev(dim) == 0){
      return 0;
    }
    return val*dataset.getStdDev(dim) + dataset.getMean(dim);
  }

  @Override
  public DataSet getDataSet(){
    return dataset;
  }


  private void findStandardDevs() {


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
          double distToAvg = vals[k] - getMean(k);
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

