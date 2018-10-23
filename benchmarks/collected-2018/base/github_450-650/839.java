// https://searchcode.com/api/result/71565017/

package com.chintux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Simplex {
	
	double[][] A;
	double[][] b;
	double[][] c;
	int[][] x;
	int[][] s;
	
	static int[][] VB;
	static int[][] VNB;
	
	int num_var = 0;
	int num_rest = 0;
	
	static int iteracion = 1;

	static double[][] B;
	
	static int pivPos;
	static double[][] piv;
	static double[][] ld;
	static int x2;
	static double [][] extA;
	
/*
	public static void main(String[] args){

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String raw_input = "";
		System.out.println("Cuantas variables son?");
		
		try {
			raw_input = reader.readLine();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		num_var = Integer.parseInt(raw_input);
		
		System.out.println("Cuantas restricciones son?");
		
		try {
			raw_input = reader.readLine();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		num_rest = Integer.parseInt(raw_input);

		System.out.println("Introduzca los coeficientes de la funcion objetivo (separados por espacios)");
		
		try {
			raw_input = reader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] fo_coef2 = raw_input.split(" ");
		c = new double[1][num_var];
		for(int i = 0; i < num_var; i++) {
			c[0][i] = Double.parseDouble(fo_coef2[i]);
		}

		A = new double[num_rest][num_var];
		b = new double[num_rest][1];
		for(int i = 0; i < num_rest; i++) {
			System.out.println("Introduzca los coeficientes de la restriccion " + (i+1));
			System.out.println("Para 3*x1 - 2*x2 <= 40, teclee 3 -2 40");
			try {
				raw_input = reader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String[] coef = raw_input.split(" ");
			for(int j = 0; j < num_var; j++) {
				A[i][j] = Double.parseDouble(coef[j]);
			}
			b[i][0] = Double.parseDouble(coef[num_var]);
		}
		
		x = new int[num_var][1];
		s = new int[num_rest][1];
		for(int i = 0; i < num_var; i++) {
			x[i][0] = i+1;
		}
		for(int i = 0; i < num_rest; i++) {
			s[i][0] = i + num_var+1;
		}
		
		VB = copy(s);
		VNB = copy(x);
		
		System.out.println("\nITERACION " + iteracion);
		pivPos = isOptimum();

		while(pivPos != -1) {
			piv = getPiv(A, pivPos);
			ld = b;
			x2 = pivPosRow(piv, ld);
			extA = extendA(A);
			B = genB(extA, VB);
			if(x2 > -1) {
				swap(VB, VNB, x2, pivPos);
			} else {
				System.out.println("No hay variable basica que salga, region factible no acotada.");
				break;
			}
			iteracion++;
			System.out.println("\nITERACION " + iteracion);
			pivPos = isOptimum();
		}
		
		System.out.println("\nFINAL");
		double[][] cb = gen_cb(extend_c(c), VB);
		double[][] Binv = inverse(genB(extendA(A),VB));
		double z = mult(mult(cb, Binv), b)[0][0];
		System.out.println("z = " + z);
		double[][] ladod = mult(Binv, b);
		System.out.println("lado derecho");
		show(ladod);
		System.out.println("Variables basicas");
		show_vars(VB, num_var);
		System.out.println("Variables no basicas");
		show_vars(VNB, num_var);
		
	}
	*/

	public Simplex(double[][] c, double[][] A, double[][] b) {
		this.c = c;
		this.A = A;
		this.b = b;
		this.num_var = c[0].length;
		this.num_rest = b.length;
	}
	
	public double solve() {
		x = new int[num_var][1];
		s = new int[num_rest][1];
		for(int i = 0; i < num_var; i++) {
			x[i][0] = i+1;
		}
		for(int i = 0; i < num_rest; i++) {
			s[i][0] = i + num_var+1;
		}
		
		VB = copy(s);
		VNB = copy(x);
		
		pivPos = isOptimum();

		while(pivPos != -1) {
			piv = getPiv(A, pivPos);
			ld = b;
			x2 = pivPosRow(piv, ld);
			extA = extendA(A);
			B = genB(extA, VB);
			if(x2 > -1) {
				swap(VB, VNB, x2, pivPos);
			} else {
				//System.out.println("No hay variable basica que salga, region factible no acotada.");
				break;
			}
			iteracion++;
			//System.out.println("\nITERACION " + iteracion);
			pivPos = isOptimum();
		}
		
		//System.out.println("\nFINAL");
		double[][] cb = gen_cb(extend_c(c), VB);
		double[][] Binv = inverse(genB(extendA(A),VB));
		
		double z = mult(mult(cb, Binv), b)[0][0];
		System.out.println("z = " + z);
		
		double[][] ladod = mult(Binv, b);
		System.out.println("lado derecho");
		show(ladod);
		
		System.out.println("Variables basicas");
		show_vars(VB, num_var);
		
		System.out.println("Variables no basicas");
		show_vars(VNB, num_var);
		
		return z;
	}
	
	private double[][] inverse(double[][] a) {
		if(a.length == a[0].length) {
			if(a.length == 2) {
				double[][] x = {
						{+a[1][1],-a[0][1]},
						{-a[1][0],+a[0][0]}
				};
				return scalarProduct(x, 1.0/(a[0][0] * a[1][1] - a[0][1] * a[1][0]));
			} else {
				return invert(a);
			}
		} else {
			throw new RuntimeException("Illegal matrix dimensions for inversion.");
		}
	}
	
	
	private double[][] extend_c(double[][] c) {
		int vars = x.length;
		int slacks = s.length;
		double[][] mat = new double[c.length][c[0].length + slacks];
		for(int i = 0; i < mat.length; i++) {
			for(int j = 0; j < mat[0].length; j++) {
				if(j < vars) {
					mat[i][j] = c[i][j];
				} else {
					mat[i][j] = 0.0;
				}
			}
		}
		return mat;
	}
	
	
	private double[][] gen_cb(double[][] c, int[][] vb) {
		int len = vb.length;
		double[][] mat = new double[c.length][len];
		for(int j = 0; j < vb.length; j++) {
			int col = vb[j][0]-1;
			for(int i = 0; i < c.length; i++) {
				mat[i][j] = c[i][col];				
			}
		}
		return mat;
	}
	
	
	private double[][] extendA(double[][] a) {
		int vars = x.length;
		int slacks = s.length;
		double[][] mat = new double[a.length][a[0].length + slacks];
		for(int i = 0; i < mat.length; i++) {
			for(int j = 0; j < mat[0].length; j++) {
				if(j < vars) {
					mat[i][j] = a[i][j];
				} else {
					if(i == j - vars) {
						mat[i][j] = 1.0;
					} else {
						mat[i][j] = 0.0;
					}
				}
			}
		}
		return mat;
	}

	
	private double[][] genB(double[][] extA, int[][] vb) {
		int len = vb.length;
		double[][] matrix = new double[len][len];
		for(int j = 0; j < vb.length; j++) {
			int col = vb[j][0]-1;
			for(int i = 0; i < extA.length; i++) {
				matrix[i][j] = extA[i][col];
			}
		}
		return matrix;
	}

	
	private double[][] mult(double[][] a, double[][] b) {
		double[][] c;
		if(a[0].length == b.length) { 		// si se puede multiplicar
			c = new double[a.length][b[0].length];
			for(int i = 0; i < c.length; i++) {
				for(int j = 0; j < c[0].length; j++) {
					for(int k = 0; k < a[0].length; k++) {
						c[i][j] += a[i][k] * b[k][j];
					}
				}
			}
		} else {
			throw new RuntimeException("Illegal matrix dimensions.");
		}
		return c;
	}

	
	private void swap(int[][] a, int[][] b, int posa, int posb) {
		int temp = b[posb][0];
		b[posb][0] = a[posa][0];
		a[posa][0] = temp;
	}

	
	private int pivPosRow(double[][] a, double[][] b) {
		double min = Double.POSITIVE_INFINITY;
		int pos = -1;
		for(int i = 0; i < a.length; i++) {
			if(a[i][0] > 0.0) {
				double div = b[i][0] / a[i][0];
				if(div < min) {
					min = div;
					pos = i;
				}
			} else {
				continue;
			}
		}
		return pos;
	}

	
	private int[][] copy(int[][] matrix) {
		int M = matrix.length;
		int N = matrix[0].length;
		int[][] matrix2 = new int[M][N];
		for(int i = 0; i < M; i++) {
			for(int j = 0; j < N; j++) {
				matrix2[i][j] = matrix[i][j];
			}
		}
		return matrix2;
	}

	
	private void show(double[][] a){
		for(int i = 0; i < a.length; i++) {
			for(int j = 0; j < a[0].length; j++) {
				System.out.print(a[i][j] + " ");
			}
			System.out.println();
		}
	}

	
	private void show(int[][] a){
		for(int i = 0; i < a.length; i++) {
			for(int j = 0; j < a[0].length; j++) {
				System.out.print(a[i][j] + " ");
			}
			System.out.println();
		}
	}
	
	
	private void show2(double[][] a){
		for(int i = 0; i < a.length; i++) {
			for(int j = 0; j < a[0].length; j++) {
				System.out.print(a[i][j] + " ");
			}
		}
	}

	
	private void show_vars(int[][] a, int offset) {
		for(int i = 0; i < a.length; i++) {
			for(int j = 0; j < a[0].length; j++) {
				if(a[i][j] <= offset){ 
					System.out.print("x" + (a[i][j]) + " ");
				} else {
					System.out.print("s" + (a[i][j]-offset) + " ");
				}
			}
			System.out.println();
		}
	}

	private double[][] getPiv(double[][] A, int pos) {
		if(pos < A[0].length) {
			double[][] piv = new double[A.length][1];
			for(int i = 0; i < piv.length; i++) {
				piv[i][0] = A[i][pos];
			}
			return piv;
		} 
		return null;
	}

	
	private int isOptimum() {

		double min = 0.0;
		int place = -1;
		double[][] minusc;
		double[][] shadow;
		double[][] B_inv;
		double[][] cb_Binv;
		double z;
		double[][] ladod;
		if(iteracion == 1) {
			minusc = scalarProduct(c, -1);
			shadow = new double[1][num_rest];
			for(int i = 0; i < shadow.length; i++){
				for(int j = 0; j < shadow[0].length; j++){
					shadow[i][j] = 0.0;
				}
			}
			z = 0.0;
			ladod = b;
		} else {
			minusc = scalarProduct(c, -1);
			double[][] extc = extend_c(c);
			System.out.println("basicas:");
			show_vars(VB, num_var);
			double[][] cb = gen_cb(extc, VB);
			B = genB(extA, VB);
			System.out.println("MATRIZ B:");
			show(B);
			B_inv = inverse(B);
			cb_Binv = mult(cb, B_inv);
			minusc = sum(mult(cb_Binv, A), scalarProduct(c, -1));
			shadow = cb_Binv;
			z = mult(cb_Binv, b)[0][0];
			ladod = mult(B_inv, b);
		}
		
		System.out.println("Renglon 0");
		show2(minusc);
		show2(shadow);
		System.out.println(z);
		System.out.println("LADO DER.");
		show(ladod);
		
		for(int j = 0; j < minusc[0].length; j++) {
			if(minusc[0][j] < 0.0) {
				if(minusc[0][j] < min) {
					min = minusc[0][j];
					place = j;
				}
			} else {
				continue;
			}
		}
		return place;
	}
	
	
	private double[][] sum(double[][] a, double[][] b) {
		int am, an, bm, bn;
		am = a.length;
		an = a[0].length;
		bm = b.length;
		bn = b[0].length;
		double[][] mat;
		if(am == bm && an == bn) {
			mat = new double[am][an];
			for(int i = 0; i < am; i++) {
				for(int j = 0; j < an; j++) {
					mat[i][j] = a[i][j] + b[i][j];
				}
			}
			return mat;
		}
		return null;
	}

	
	private double[][] scalarProduct(double[][] matrix, double scalar) {
		int M = matrix.length;
		int N = matrix[0].length;
		double[][] matrix2 = new double[M][N];
		for(int i = 0; i < M; i++) {
			for(int j = 0; j < N; j++) {
				matrix2[i][j] = scalar * matrix[i][j];
			}
		}
		return matrix2;
	}
	
	
	/*
	 * taken from:
	 * http://www.csee.umbc.edu/~squire/download/Matrix.java
	 * but modified so it returns a new matrix, instead of calculating it in place
	 */
	private double[][] invert(double in[][])
	  {
		double[][] A = new double[in.length][in[0].length];
		for(int i = 0; i < in.length; i++) {
			for(int j = 0; j < in[0].length; j++) {
				A[i][j] = in[i][j];
			}
		}
	    int n = A.length;
	    int row[] = new int[n];
	    int col[] = new int[n];
	    double temp[] = new double[n];
	    int hold , I_pivot , J_pivot;
	    double pivot, abs_pivot;

	    if(A[0].length!=n)
	    {
	      System.out.println("Error in Matrix.invert, inconsistent array sizes.");
	    }
	    // set up row and column interchange vectors
	    for(int k=0; k<n; k++)
	    {
	      row[k] = k ;
	      col[k] = k ;
	    }
	    // begin main reduction loop
	    for(int k=0; k<n; k++)
	    {
	      // find largest element for pivot
	      pivot = A[row[k]][col[k]] ;
	      I_pivot = k;
	      J_pivot = k;
	      for(int i=k; i<n; i++)
	      {
	        for(int j=k; j<n; j++)
	        {
	          abs_pivot = Math.abs(pivot) ;
	          if(Math.abs(A[row[i]][col[j]]) > abs_pivot)
	          {
	            I_pivot = i ;
	            J_pivot = j ;
	            pivot = A[row[i]][col[j]] ;
	          }
	        }
	      }
	      if(Math.abs(pivot) < 1.0E-10)
	      {
	        System.out.println("Matrix is singular !");
	        return null;
	      }
	      hold = row[k];
	      row[k]= row[I_pivot];
	      row[I_pivot] = hold ;
	      hold = col[k];
	      col[k]= col[J_pivot];
	      col[J_pivot] = hold ;
	       // reduce about pivot
	      A[row[k]][col[k]] = 1.0 / pivot ;
	      for(int j=0; j<n; j++)
	      {
	        if(j != k)
	        {
	          A[row[k]][col[j]] = A[row[k]][col[j]] * A[row[k]][col[k]];
	        }
	      }
	      // inner reduction loop
	      for(int i=0; i<n; i++)
	      {
	        if(k != i)
	        {
	          for(int j=0; j<n; j++)
	          {
	            if( k != j )
	            {
	              A[row[i]][col[j]] = A[row[i]][col[j]] - A[row[i]][col[k]] *
	                                   A[row[k]][col[j]] ;
	            }
	          }
	          A[row[i]][col [k]] = - A[row[i]][col[k]] * A[row[k]][col[k]] ;
	        }
	      }
	    }
	    // end main reduction loop

	    // unscramble rows
	    for(int j=0; j<n; j++)
	    {
	      for(int i=0; i<n; i++)
	      {
	        temp[col[i]] = A[row[i]][j];
	      }
	      for(int i=0; i<n; i++)
	      {
	        A[i][j] = temp[i] ;
	      }
	    }
	    // unscramble columns
	    for(int i=0; i<n; i++)
	    {
	      for(int j=0; j<n; j++)
	      {
	        temp[row[j]] = A[i][col[j]] ;
	      }
	      for(int j=0; j<n; j++)
	      {
	        A[i][j] = temp[j] ;
	      }
	    }
	    return A;
	  } // end invert

}
