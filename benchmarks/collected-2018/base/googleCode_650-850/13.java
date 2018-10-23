// https://searchcode.com/api/result/8441395/

/*
 * RDV
 * Real-time Data Viewer
 * http://rdv.googlecode.com/
 * 
 * Copyright (c) 2008 Palta Software
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * $URL: http://rdv.googlecode.com/svn/trunk/src/org/rdv/viz/spectrum/SpectrumAnalyzerPanel.java $
 * $Revision: 1329 $
 * $Date: 2008-12-08 17:07:06 +0100 (Mon, 08 Dec 2008) $
 * $Author: jason@paltasoftware.com $
 */

package org.rdv.viz.spectrum;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;
import org.rdv.viz.chart.ChartPanel;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * A panel for displaying a spectrum analyzer.
 * 
 * @author Jason P. Hanley
 * 
 */
public class SpectrumAnalyzerPanel extends ChartPanel {

  /** serialization version identifier */
  private static final long serialVersionUID = -4965300280813579540L;

  /** the sample rate of the data */
  private double sampleRate;

  /** the number of samples to use as input */
  private int numberOfSamples;

  /** the window function to use on the data */
  private WindowFunction windowFunction;

  /** the size of the segments used to FFT */
  private int segmentSize;

  /** the overlap of the segments */
  private int overlap;

  /** the data */
  private double[] inputData;

  /** the offset to the data */
  private int oldestDataIndex;
  
  /** the window function coefficients */
  private double[] window;

  /** the FFT class */
  private DoubleFFT_1D fft;

  /** the FFT executor */
  private Executor fftExecutor;

  /** the chart */
  private JFreeChart chart;

  /** the XY series to hold the chart data */
  private SpectrumXYSeries xySeries;
  
  /** the sample rate property */
  public static final String SAMPLE_RATE_PROPERTY = "sampleRate";
  
  /** the number of samples property */
  public static final String NUMBER_OF_SAMPLES_PROPERTY = "numberOfSamples";
  
  /** the window function property */
  public static final String WINDOW_FUNCTION_PROPERTY = "windowFunction";
  
  /** the segment size property */
  public static final String SEGMENT_SIZE_PROPERTY = "segmentSize";
  
  /** the overlap property */
  public static final String OVERLAP_PROPERTY = "overlap";

  /**
   * Creates a spectrum analyzer panel.
   */
  public SpectrumAnalyzerPanel() {
    super(null, true);

    sampleRate = 256;
    numberOfSamples = 256;
    windowFunction = WindowFunction.Hamming;
    segmentSize = 256;
    overlap = 0;

    inputData = new double[numberOfSamples];
    oldestDataIndex = 0;
    
    createWindow();

    fft = new DoubleFFT_1D(segmentSize);

    fftExecutor = Executors.newSingleThreadExecutor();

    initChart();
  }

  /**
   * Initializes the chart.
   */
  private void initChart() {
    xySeries = new SpectrumXYSeries("", false, false);
    
    XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
    xySeriesCollection.addSeries(xySeries);

    chart = ChartFactory.createXYLineChart(null, "Frequency (Hz)", null,
        xySeriesCollection, PlotOrientation.VERTICAL, false, true, false);
    chart.setAntiAlias(false);

    XYPlot xyPlot = (XYPlot) chart.getPlot();
    NumberAxis xAxis = (NumberAxis) xyPlot.getDomainAxis();
    xAxis.setRange(0, sampleRate / 2);

    setChart(chart);
  }

  /**
   * Gets the sample rate.
   * 
   * @return  the sample rate
   */
  public double getSampleRate() {
    return sampleRate;
  }

  /**
   * Sets the sample rate. The sample rate must be positive.
   * 
   * @param sampleRate  the new sample rate
   */
  public void setSampleRate(double sampleRate) {
    if (sampleRate <= 0 ) {
      throw new IllegalArgumentException("Sample rate must be positive.");
    }
    
    if (this.sampleRate == sampleRate) {
      return;
    }
    
    double oldSampleRate = this.sampleRate;
    this.sampleRate = sampleRate;
    
    firePropertyChange(SAMPLE_RATE_PROPERTY, oldSampleRate, sampleRate);

    // set the x-axis range
    XYPlot xyPlot = (XYPlot) chart.getPlot();
    NumberAxis xAxis = (NumberAxis) xyPlot.getDomainAxis();
    xAxis.setRange(0, sampleRate / 2);

    plotSpectrum();
  }

  /**
   * Gets the number of samples. This is amount of data that will be used in the
   * analysis.
   * 
   * @return  the number of samples
   */
  public int getNumberOfSamples() {
    return numberOfSamples;
  }

  /**
   * Sets the number of samples. The number of samples must be greater then 1.
   * If the segment size is greater then the number of samples it will be set to
   * the next lower (or equal) power of 2. The overlap also may be reduced
   * because of this.
   * 
   * @param numberOfSamples  the new number of samples
   */
  public void setNumberOfSamples(int numberOfSamples) {
    if (numberOfSamples <= 1) {
      throw new IllegalArgumentException("Number of samples must be greater then 1.");
    }
    
    if (this.numberOfSamples == numberOfSamples) {
      return;
    }

    double[] newInputData = new double[numberOfSamples];
    int offset = numberOfSamples < this.numberOfSamples ? this.numberOfSamples
        - numberOfSamples : 0;
    for (int i = 0; i < Math.min(this.numberOfSamples, numberOfSamples); i++) {
      int index = (oldestDataIndex + offset + i) % this.numberOfSamples;
      newInputData[i] = inputData[index];
    }
    inputData = newInputData;
    oldestDataIndex = Math.min(this.numberOfSamples, numberOfSamples)
        % numberOfSamples;

    int oldNumberOfSamples = this.numberOfSamples;
    this.numberOfSamples = numberOfSamples;
    
    firePropertyChange(NUMBER_OF_SAMPLES_PROPERTY, oldNumberOfSamples, numberOfSamples);
    
    // reduce the segment size if needed
    if (segmentSize > numberOfSamples) {
      setSegmentSize(numberOfSamples, false);
    }

    plotSpectrum();
  }

  /**
   * Gets the window function.
   * 
   * @return  the window function
   */
  public WindowFunction getWindowFunction() {
    return windowFunction;
  }

  /**
   * Sets the window function.
   * 
   * @param windowFunction  the new window function
   */
  public void setWindowFunction(WindowFunction windowFunction) {
    if (windowFunction == null) {
      windowFunction = WindowFunction.Rectangular;
    }

    if (this.windowFunction == windowFunction) {
      return;
    }

    WindowFunction oldWindowFunction = this.windowFunction;
    this.windowFunction = windowFunction;
    
    firePropertyChange(WINDOW_FUNCTION_PROPERTY, oldWindowFunction, windowFunction);

    createWindow();

    plotSpectrum();
  }

  /**
   * Gets the segment size. This is the size of the data that will be FFT'ed.
   * 
   * @return  the segment size
   */
  public int getSegmentSize() {
    return segmentSize;
  }

  /**
   * Sets the segment size. The segment size must at least 2, less then or equal
   * to the number of samples and a power of 2. If the overlap is greater then
   * or equal to the segment size it will be reduced to 1 less then the segment
   * size.
   * 
   * @param segmentSize  the new segment size
   */
  public void setSegmentSize(int segmentSize) {
    setSegmentSize(segmentSize, true);
  }

  /**
   * Sets the segment size and optionally redraws the chart. The segment size
   * must at least 2, less then or equal to the number of samples and a power of
   * 2. If the overlap is greater then or equal to the segment size it will be
   * reduced to 1 less then the segment size.
   * 
   * @param segmentSize  the new segment size
   * @param redraw       if true, redraw the chart, otherwise don't
   */
  private void setSegmentSize(int segmentSize, boolean redraw) {
    if (segmentSize <= 1 || segmentSize > numberOfSamples) {
      throw new IllegalArgumentException("Segment size must be greater then 1 and less then or equal to the number of samples.");
    }
    
    if (this.segmentSize == segmentSize) {
      return;
    }
    
    int oldSegmentSize = this.segmentSize;
    this.segmentSize = segmentSize;
    
    createWindow();

    fft = new DoubleFFT_1D(segmentSize);
    
    firePropertyChange(SEGMENT_SIZE_PROPERTY, oldSegmentSize, segmentSize);
    
    // reduce the overlap if needed
    if (overlap >= segmentSize) {
      setOverlap(segmentSize-1, false);
    }

    if (redraw) {
      plotSpectrum();
    }
  }

  /**
   * Gets the overlap. The overlap is the number of points shared by each
   * successive segment.
   * 
   * @return
   */
  public int getOverlap() {
    return overlap;
  }

  /**
   * Sets the overlap. The overlap must be less then the segment size. If the
   * overlap is less then 0, it will be set to half the segment size.
   * 
   * @param overlap  the new overlap
   */
  public void setOverlap(int overlap) {
    setOverlap(overlap, true);
  }
  
  /**
   * Sets the overlap and optionally redraws the chart. The overlap must be less
   * then the segment size. If the overlap is less then 0, it will be set to
   * half the segment size.
   * 
   * @param overlap  the new overlap
   * @param redraw   if true, redraw the chart, otherwise don't
   */
  private void setOverlap(int overlap, boolean redraw) {
    if (overlap < 0) {
      overlap = segmentSize/2;
    }

    if (overlap >= segmentSize) {
      throw new IllegalArgumentException("Overlap must be less then the segment size.");
    }
    
    if (this.overlap == overlap) {
      return;
    }
    
    int oldOverlap = this.overlap;
    this.overlap = overlap;
    
    firePropertyChange(OVERLAP_PROPERTY, oldOverlap, overlap);

    plotSpectrum();
  }
  
  /**
   * Adds data. This will not trigger a replot. Call
   * {@link #finishedAddingData()} when finished adding data.
   * 
   * @param data  the data point to add
   * @see         #finishedAddingData()
   */
  public void addData(double data) {
    inputData[oldestDataIndex++] = data;
    if (oldestDataIndex == numberOfSamples) {
      oldestDataIndex = 0;
    }
  }
  
  /**
   * Called when finished adding data and replots the chart.
   * 
   * @see  #addData(double)
   */
  public void finishedAddingData() {
    plotSpectrum();
  }

  /**
   * Adds data and replots the chart.
   * 
   * @param data        the new data
   * @param startIndex  the start index for the data
   * @param endIndex    the end index for the data
   */
  public void addData(float[] data, int startIndex, int endIndex) {
    // if there is too much data, only add the most recent
    if (endIndex - startIndex > numberOfSamples) {
      startIndex = endIndex - numberOfSamples;
    }

    // add the data overwritting the oldest data
    for (int i = startIndex; i <= endIndex; i++) {
      inputData[oldestDataIndex++] = data[i];
      if (oldestDataIndex == numberOfSamples) {
        oldestDataIndex = 0;
      }
    }

    plotSpectrum();
  }
  
  /**
   * Adds data and replots the chart.
   * 
   * @param data        the new data
   * @param startIndex  the start index for the data
   * @param endIndex    the end index for the data
   */
  public void addData(double[] data, int startIndex, int endIndex) {
    // if there is too much data, only add the most recent
    if (endIndex - startIndex > numberOfSamples) {
      startIndex = endIndex - numberOfSamples;
    }

    // add the data overwritting the oldest data
    for (int i = startIndex; i <= endIndex; i++) {
      inputData[oldestDataIndex++] = data[i];
      if (oldestDataIndex == numberOfSamples) {
        oldestDataIndex = 0;
      }
    }

    plotSpectrum();
  }

  /**
   * Clears the data.
   */
  public void clearData() {
    Arrays.fill(inputData, 0);
    oldestDataIndex = 0;

    xySeries.clear();
  }

  /**
   * Gets the data as an array with a 0 offset.
   * 
   * @return  the data
   */
  private double[] getData() {
    double[] array = new double[numberOfSamples];
    for (int i = 0; i < numberOfSamples; i++) {
      int index = (oldestDataIndex + i) % numberOfSamples;
      array[i] = inputData[index];
    }
    return array;
  }

  /**
   * Plots the power spectrum. This does the analysis in a seperate thread and
   * when done adds updates the chart on the EDT.
   */
  private void plotSpectrum() {
    final double[] x = getData();
    
    fftExecutor.execute(new Runnable() {
      public void run() {
        final double[] X = psdWelch(fft, x, numberOfSamples, window, segmentSize, overlap);

        try {
          SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
              updateChart(X, sampleRate, segmentSize);
            }
          });
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
  
  /**
   * Calculate the PSD using Whelch's Method.
   * 
   * @param fft              the FFT class
   * @param x                the data
   * @param numberOfSamples  the number of samples to use
   * @param window           the window coefficients
   * @param segmentSize      the size of the segments
   * @param overlap          the overlap of the segments
   * @return                 the PSD
   */
  private static double[] psdWelch(DoubleFFT_1D fft, double x[], int numberOfSamples, double window[], int segmentSize, int overlap) {
    // the final PSD aray
    double X[] = new double[segmentSize / 2 + 1];

    // the data array's for the segments
    double xx[] = new double[segmentSize];
    double XX[] = new double[segmentSize / 2 + 1];

    // loop through each segment and calculate their PSD
    int segments = 0;
    int offset = 0;
    while (offset + segmentSize <= numberOfSamples) {
      // fill the data array
      for (int i = 0; i < segmentSize; i++) {
        xx[i] = x[offset + i] * window[i];
      }

      // compute the FFT
      fft.realForward(xx);

      // calculate the magnitude
      for (int i = 0; i <= segmentSize / 2; i++) {
        // get the data out according to the FFT's data format
        double re = (i == segmentSize / 2) ? xx[1] : xx[2 * i];
        double im = (i == 0 || i == segmentSize / 2) ? 0 : xx[2 * i + 1];
        double mag = Math.sqrt(Math.pow(re, 2) + Math.pow(im, 2));

        XX[i] = mag / segmentSize;
        XX[i] = Math.pow(XX[i], 2);
        if (i != 0 || i != segmentSize)
          XX[i] = 2 * XX[i];

        // add to the final PSD
        X[i] += XX[i];
      }

      segments++;
      offset += segmentSize - overlap;
    }

    // compute the average of the segments PSD's
    for (int i = 0; i <= segmentSize / 2; i++) {
      X[i] = X[i] / segments;
    }

    return X;
  }

  /**
   * Update the chart with new data.
   * 
   * @param X            the new PSD data
   * @param sampleRate   the sample rate
   * @param segmentSize  the segment size
   */
  private void updateChart(double[] X, double sampleRate, int segmentSize) {
    xySeries.clear(false);

    double period = sampleRate / segmentSize;
    double frequency = 0;
    for (int i = 0; i < X.length; i++) {
      xySeries.add(frequency, X[i], false);
      frequency += period;
    }

    xySeries.fireSeriesChanged();
  }

  /**
   * Create the window by calculating the window coefficients.
   */
  private void createWindow() {
    window = new double[segmentSize];

    switch (windowFunction) {
    case Bartlett:
      bartlett();
      break;
    case Blackman:
      blackman();
      break;
    case Hamming:
      hamming();
      break;
    case Hann:
      hann();
      break;
    case Rectangular:
      rectangular();
      break;
    }
  }

  /**
   * Create a Bartlett window.
   */
  private void bartlett() {
    int N = window.length;
    window[0] = 0;
    for (int n = 1; n < N - 1; n++) {
      if (n < N / 2) {
        window[n] = 2 * n / (N - 1);
      } else {
        window[n] = 2 - (2 * n / (N - 1));
      }
    }
    window[N - 1] = 0;
  }

  /**
   * Create a Blackman window.
   */
  private void blackman() {
    int N = window.length;
    double pi = Math.PI;
    for (int n = 0; n < N; n++) {
      window[n] = 0.42 - (0.5 * Math.cos(2 * pi * n / (N - 1)))
          + (0.08 * Math.cos(4 * pi * n / (N - 1)));
    }
  }

  /**
   * Create a Hamming window.
   */
  private void hamming() {
    int N = window.length;
    double pi = Math.PI;
    for (int n = 0; n < window.length; n++) {
      window[n] = 0.54 - (0.46 * Math.cos(2 * pi * n / (N - 1)));
    }
  }

  /**
   * Create a Hann window.
   */
  private void hann() {
    int N = window.length;
    double pi = Math.PI;
    for (int n = 0; n < window.length; n++) {
      window[n] = 0.5 - (0.5 * Math.cos(2 * pi * n / (N - 1)));
    }
  }

  /**
   * Create a rectangular window. This is the same as no window.
   */
  private void rectangular() {
    for (int n = 0; n < window.length; n++) {
      window[n] = 1;
    }
  }

}
