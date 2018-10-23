// https://searchcode.com/api/result/133128381/

package net.sf.jtmt.clustering;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.SingularValueDecomposition;
import org.apache.commons.math.linear.SingularValueDecompositionImpl;

/**
 * Performs dimensionality reduction using Principal Component Analysis (PCA).
 * Essentially, the n-dimensional term-document matrix is reduced into two 
 * principal components so the results can be visualized on an XY scatter
 * graph.
 * 
 * Algorithm:
 * 1) A = USV'
 * 2) Choose largest two eigenvalues from S. These are the first 2 diagonal
 *    elements on S.
 * 3) Graph the eigenvectors corresponding to the two eigenvalues, ie the 
 *    first two cols of V.
 *
 * @author Sujit Pal
 * @version $Revision$
 */
public class PcaClusterVisualizer {

  private final String PLOT_2D_OUTPUT = "plot2d.dat";
  private final String PLOT_3D_OUTPUT = "plot3d.dat";
  
  public void reduce(RealMatrix tdMatrix, String[] docNames) throws IOException {
    PrintWriter plot2dWriter = new PrintWriter(new FileWriter(PLOT_2D_OUTPUT));
    PrintWriter plot3dWriter = new PrintWriter(new FileWriter(PLOT_3D_OUTPUT));
    SingularValueDecomposition svd = new SingularValueDecompositionImpl(tdMatrix);
    RealMatrix v = svd.getV();
    // we know that the diagonal of S is ordered, so we can take the
    // first 3 cols from V, for use in plot2d and plot3d
    RealMatrix vRed = v.getSubMatrix(0, v.getRowDimension() - 1, 0, 2);
    for (int i = 0; i < v.getRowDimension(); i++) { // rows
      plot2dWriter.printf("%6.4f %6.4f %s%n", 
        Math.abs(vRed.getEntry(i, 0)), Math.abs(vRed.getEntry(i, 1)), docNames[i]);
      plot3dWriter.printf("%6.4f %6.4f %6.4f %s%n", 
        Math.abs(vRed.getEntry(i, 0)), Math.abs(vRed.getEntry(i, 1)), 
        Math.abs(vRed.getEntry(i, 2)), docNames[i]);
    }
    plot2dWriter.flush();
    plot3dWriter.flush();
    plot2dWriter.close();
    plot3dWriter.close();
  }
}

