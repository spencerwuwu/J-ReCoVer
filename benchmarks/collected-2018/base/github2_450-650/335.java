// https://searchcode.com/api/result/104266651/

/*
 *																		
 *							SimpleDM										
 *																		
 *					Illinois Institute of Technology
 *					Information Retrieval Laboratory
 *		
 *					CS422 Introduction To Data Mining					
 *					Professor: Dr. Nazli Goharian						
 *																		
 *					Copyright (c) 2003									
 *					@author		Mi Sun Chung							
 *					@version	1.3										
 *																		
 *					SVM.java											
 */

 


package edu.iit.ir.simpledm.model.svm;

import edu.iit.ir.simpledm.model.*;
import edu.iit.ir.simpledm.preprocess.*;
import edu.iit.ir.simpledm.util.*;
import edu.iit.ir.simpledm.data.*;
import edu.iit.ir.simpledm.exception.*;

import java.text.DecimalFormat;
import java.util.*; 
import javax.swing.JOptionPane;
import java.io.*;



 /**
 *	Support Vector Machine.						
 *  It extends Model abstract class.<br> 
 *  The implemented SVM is scalable model. It builds a model for each training SampleSet. 
 *  Then it votes the majority of models with testing SampleSets.   				 					 
 */
public class SVM extends Model
{
	/** congifure file path for parameters such as kernel function and learning rate */
	private String m_param_file = "lib/svm_config.txt";

	/** Represents polynomial kernel function. */
	public static final int POLYNOMIAL = 1;

	/** Represents radio basis kernel function. */
	public static final int RADIO_BASIS = 2;



	/** Name of kernel function */
	protected int kernel_int = RADIO_BASIS;
	
	/** Kernel function */
	protected KernelFunction kernel; 


	/**  Learning rate		                */
	protected double learningRate=0.3;


	private	SampleSet sampleSet;        // Sample set
	private Sample[] samples;			// samples
	private	int numAttributes;       // number of attributes

	private Vector assembledSV=new Vector();  // saves the support vectors of each models
	private Vector assembledAlpha = new Vector();  // saves the alphas of the support vectors 
	private Vector assembledBias = new Vector();   // saves the bias of each models

	private int[]result ;			// voting results of models
	private int noFold=0;			// number of folds in cross-validation
	private double b;				// bias	
	
	private	double correctL;	// number of correct low class
	private double correctH;    // number of correct high class
	private double noL;			// number of low class
	private double noH;			// number of high class
	



	/** Default Constructor */
	public SVM(DataSet dataset)
	{
		super(dataset);
		correctL=0;
		correctH=0;
		noL=0;
		noH=0;
		readParam();
	}


	/** 
	 * Builds a Support Vector model with the given SampleSet.
	 * The built model such as support vectors, its alpha and bias value 
	 * will be stored for voting the majority of the models later.
	 */
	public void buildModel(SampleSet set) 
	{
		sampleSet = set;



		// check if there are two classes. if not,terminate building SVM model.
		if(!isTwoClasses())
		{
			System.out.println("ERROR!!!! The number of classes is over two. This data file cannot be applied in this model.");
			System.out.println("\n-----------------------------------------------------"+
							"\nERROR!!! THE NUMBER OF CLASSES IS NOT TWO."+
							"\nBUILDING SVM WAS TERMINATED WITH DATA FILE ERROR."+
							"\n-------------------------------------------------------");
/*			JOptionPane.showMessageDialog(null,
					"The number of classes is over two.\nThis data file cannot be applied in this model.",
					"Improper data file",
					JOptionPane.ERROR_MESSAGE);
*/		
//			super.m_panel.stopModel();
			return;
		}
		

		// < necessary preprocessing >
		if(m_pre.isTransformationSelected() == false){
			m_pre.transformToContinuous(sampleSet);
		}
		else if(m_pre.isTransformationSelected() == true 
			&& m_pre.getTypeOfTransformation() != Preprocessor.TO_CONTINUOUS){
			m_pre.transformToContinuous(sampleSet);
		}
		
		
		missingValue(sampleSet);  // replace missing value with averages..
		modifyClasses(sampleSet);  // 0-> -1
		
		
		numAttributes = m_data.getNumAttributes();
		samples = sampleSet.getSamples();

		// generate kernel adatron
		generateKernelAdatron();

	}



	/**
	 * Tests the SVM on a SampleSet by voting the majority of models.
	 * 
	 * @param set -SampleSet of testing data.
	 */
	public void testModel(SampleSet set)
	{
		sampleSet = set;
		System.out.println("Testing Size of Buffer: "+set.size() );
				
		// preprocessing
		if(m_pre.isTransformationSelected() == false)
			m_pre.transformToContinuous(sampleSet );
		else if(m_pre.isTransformationSelected() == true
			&& m_pre.getTypeOfTransformation() != Preprocessor.TO_CONTINUOUS)
			m_pre.transformToContinuous(sampleSet );


		missingValue(sampleSet);  // replace missing value with averages..
		if(!modifyClasses(sampleSet))  // 0-> -1
			return; 
		

		samples = sampleSet.getSamples();
		result = new int [samples.length];

		Enumeration sv_enum = assembledSV.elements();
		Enumeration alpha_enum = assembledAlpha.elements();
		Enumeration bias_enum = assembledBias.elements();
		// for each SVM model...
		while(sv_enum.hasMoreElements())
		{
			Vector supportVectors = (Vector)sv_enum.nextElement();
			Vector alphasSV = (Vector)alpha_enum.nextElement();
			double bias = ((Double)bias_enum.nextElement()).doubleValue();
			testKernelAdatron(supportVectors, alphasSV,bias);
		}

		// vote the majority
		votingResults();
	}





	/**
	 * Write information about SVM, kernel function and parameters on resul text.
	 * It will be called first at the beginning of modeling.
	 */
	public void writeIntro()
	{
		StringBuffer sb = new StringBuffer();
		
		sb.append("\n /**************************************************************");
		sb.append("\n                   Support Vector Machine				       ");
		sb.append("\n  															");
		sb.append("\n       Learning rate: "+learningRate);
		if(kernel_int == POLYNOMIAL){
			sb.append("\n       Polynomial function  Degree = "
					+((Polynomial)kernel).getDegree() +" ");
		}
		else{
			sb.append("\n       Radio Basis function  Sigma = "
					+((RadioBasis)kernel).getSigma()+" ");
		}
		sb.append("\n ****************************************************************");
		sb.append("");
//		sb.append("\nNote. Only the data set, which has two classes, can be applied in this SVM model.");
//		sb.append("\nNote. Very small number of records can build an incorrect model.");
//		sb.append("\nNote. Normalization should be processed for SVM.");
//		sb.append("\nNote. 'Avoiding over-fit' preprocessing can reduce the execution time.");
//		sb.append("\nNote. Defualt parameters were set for the 'adult.arff' file.\n\n\n");

		String s = sb.toString();
		writeFile(s);
		System.out.println(s);
	}




	/**
	 * Write resulting accuracy of testing.
	 * It will be called every folds.
	 */
	public void showResults()
	{
		double accuracyL = correctL/noL;
		double accuracyH = correctH/noH;
		double accuracy = (correctL+correctH)/(noL+noH);
		int numAttributes = m_data.getNumAttributes();		


		DecimalFormat df=new DecimalFormat(".0000");
		StringBuffer sb = new StringBuffer();

		sb.append("\n\n	Class Name	Count	Accuracy");
		sb.append("\n	-------------------------------------------------------");
		sb.append("\n	"+m_data.getAttribute(numAttributes-1).getItemInfo().getLabel(0));
		sb.append("		"+noL+"	"+df.format(accuracyL));
		sb.append("\n	"+m_data.getAttribute(numAttributes-1).getItemInfo().getLabel(1));
		sb.append("		"+noH+"	"+df.format(accuracyH));
		sb.append("\n\n		Total Accuracy : "+ df.format(accuracy)+"\n");

		String s = sb.toString();
		writeFile(s);
		System.out.println(s);

		++noFold;
		noH=0;
		noL=0;
		correctL =0;
		correctH =0;
	}



	/**
	 * The Kernel-Adatron Algorithm.
     */
	private void generateKernelAdatron()
	{
		double z=0;
		double gamma = 0;
		double temp =0;
		double y=0;
		double maxZi=0,minZi=0;
		boolean first = false;
		boolean first2 = false;
		int numRecords = samples.length;


		// initialized alpha as 1
		double[]alphas = new double[numRecords];
		for(int i=0; i<numRecords; ++i)
			alphas[i] = 1;

		for(int indexRcd=0; indexRcd<numRecords; ++indexRcd)
		{
			y = ((ContinuousAttributeValue)samples[indexRcd].get(numAttributes-1)).doubleValue();
			double sum=0;
			for(int j = 0; j<numRecords; ++j)
			{
				sum = sum+ (alphas[j] * ((ContinuousAttributeValue)samples[j].get(numAttributes-1)).doubleValue()
					* kernel.calculate(samples[indexRcd],samples[j]));
			}
			z = y * sum;

			//System.out.println("z: "+z);
			gamma = y * z;
			temp = learningRate * ( 1 - gamma );

			if((alphas[indexRcd]+temp) <= 0 ){
				alphas[indexRcd] = 0;
			}
			else
				alphas[indexRcd] += temp;
				
//			System.out.println("alpha: "+alphas[indexRcd]);
			if(y==-1 && first == false)
			{
				maxZi = z;
				first = true;
			}
			else if(y==1 && first2 == false)
			{
				minZi = z;
				first2 = true;
			}
			else
			{
				if(y <0)
				{
					if(maxZi < z)
						maxZi = z;
				}
				else
				{
					if(minZi > z )
						minZi = z;
				}
			}
		}  // end of for loop
		
		b = ( minZi + maxZi ) /2;
		System.out.println("b:  "+b);
		System.out.println("minZi:  "+minZi);
		System.out.println("maxZi:  "+maxZi);

		saveModel(alphas,b);
	}




	/**
	 * Save support vectors, corresponding alphas and bias for each model.
	 */
	private void saveModel(double [] alphas,double bias)
	{
		Vector supportVectors = new Vector();
		Vector alphasSV = new Vector();
		int numRecords = samples.length;

		for(int rcd=0; rcd<numRecords; ++rcd)
		{
			if( alphas[rcd] != 0)
			{
				supportVectors.addElement(samples[rcd]);
				alphasSV.addElement(new Double(alphas[rcd]));
			}
		}
		
		// assemble this support vectors and its alphas
		assembledSV.addElement(supportVectors);
		assembledAlpha.addElement(alphasSV);
		assembledBias.addElement(new Double(bias));

		System.out.println("Number of support vectors"+supportVectors.size());
	}


	

	/**
	 * Tests a Kernel Adatron model, and save the result.
	 *
	 * @param supportVectors One of Kernel Adatron model that will be tested.
	 * @param alphasSv Alphas of the corresponding support vectors.
	 */
	private void testKernelAdatron(Vector supportVectors, Vector alphasSV,double bias)
	{
		double sum=0;
		int numRecords = samples.length;
		
		for(int rcd=0; rcd<numRecords; ++rcd) // for each testing sample set
		{
			sum=0;
			Enumeration sv_enum = supportVectors.elements();
			Enumeration alpha_enum = alphasSV.elements();
			while(sv_enum.hasMoreElements())  // for each support vectors
			{
				Sample sv = (Sample)sv_enum.nextElement();
				double alpha = ((Double)alpha_enum.nextElement()).doubleValue();
				sum += (((ContinuousAttributeValue)sv.get(numAttributes-1)).doubleValue()
					* alpha * kernel.calculate(sv,samples[rcd]));
			}
			sum = sum - b;
		//	System.out.println("sum: "+sum);

			if(sum>0)
				result[rcd] += 1;
			else
				result[rcd] += -1;
		}
	}





	/**
	 * Votes the majority.
	 */
	private void votingResults()
	{
		double correct=0;
		double y;

		for(int rcd =0; rcd<result.length; ++rcd)
		{
			y = ((ContinuousAttributeValue)samples[rcd].get(numAttributes-1)).intValue();
			if(y>0)
				++noH;
			else
				++noL;

			if(result[rcd] > 0){
				if(y>0)
					++correctH;
			}
			else{
				if(y<0)
					++correctL;
			}
		}
	}
			




	/**
	 * Sets parameters from SVM_Param.
	 */
/*	public void setParam(SVM_ParamFrame svmFrame)
	{
		try {
			 learningRate= Double.parseDouble(svmFrame.lrateField.getText());

			 if(svmFrame.polyButton.isSelected())
			{
				 kernel_int = POLYNOMIAL;
				 kernel = new Polynomial();
				 ((Polynomial)kernel).setDegree(Double.parseDouble(svmFrame.degreeField.getText()));
			}
			else
			{
				kernel_int = RADIO_BASIS;
				kernel = new RadioBasis();
				((RadioBasis)kernel).setSigma(Double.parseDouble(svmFrame.sigmaField.getText()));
			}
		}
		catch (Exception ex) {
		 	ex.printStackTrace();
			System.out.println("Number Input Error: "+ex.toString());
		}
	}
*/





	/**
	 * Modify Classes as -1 or 1.
	 */
	private boolean modifyClasses(SampleSet set) 
	{
		int size = set.size();
		Sample [] samples = set.getSamples();
		int classNo = m_data.getNumAttributes() - 1;

		// should be only two classes for this svm algorithm
/*		if(m_data.getAttribute(classNo).getItemInfo().size()!=2){
			return false;
		}
*/
		for(int i=0; i<size;++i){
			if(((ContinuousAttributeValue)samples[i].get(classNo)).intValue() == 0)
				((ContinuousAttributeValue)samples[i].get(classNo)).setIntValue(-1);
		}
		return true;
	}


	
	
	
	
	private boolean isTwoClasses()
	{
		int classNo = m_data.getNumAttributes() - 1;
		// should be only two classes for this svm algorithm
		if(m_data.getAttribute(classNo).getItemInfo().size()!=2){
			return false;
		}
		return true;
	}






	/**
	 * Replace a missing value as the average.
	 */
	private void missingValue(SampleSet sampleSet)
	{
		Sample [] samples = sampleSet.getSamples();
		for(int i=0; i<samples.length;++i){  // for each sample
			for(int j=0; j<numAttributes;++j){  // for each attribute
				if(((ContinuousAttributeValue)samples[i].get(j)).doubleValue() == Double.NaN){
					((ContinuousAttributeValue)samples[i].get(j))
						.setDoubleValue(m_data.getAttribute(j).getAttributeStats().getAvg());
				}
			}
		}
	}




	
	
	/**
	 * Reads some parameters for SVM in the "svm_config.txt" file.
	 * The parameters includes the learning rate, the name of kernel function
	 * and its parameter.
	 * Each model can have its own configure file, and the implementation
	 * is dependent on a developer.
	 */
	private void readParam()
	{
		  
		try {
			// open a configure file
			BufferedReader br = new BufferedReader(new FileReader(m_param_file));
			String s = br.readLine();
	
			// read the first line which represents the learning rate
			learningRate= Double.parseDouble(s);
			
			// read the next line which represents the name of kernel function
			// then read the next line which is the degree or sigma
			s = br.readLine();
			if(s.equals("POLYNOMIAL"))
			{
				kernel_int = POLYNOMIAL;
				kernel = new Polynomial();
				((Polynomial)kernel).setDegree(Double.parseDouble(br.readLine()));
			}			
			else if(s.equals("RADIO_BASIS"))
			{
				kernel_int = RADIO_BASIS;
				kernel = new RadioBasis();
				((RadioBasis)kernel).setSigma(Double.parseDouble(br.readLine()));
			}
			else
				System.out.println("Improper parameters for SVM.");
		}catch (Exception ex) {
			System.out.println("Problem to read configure file in which the parameters for svm are stored.");
		}
	}

}

