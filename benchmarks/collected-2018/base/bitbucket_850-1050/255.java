// https://searchcode.com/api/result/47603561/

package org.ben;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;



/**
 * This is the model implementation of BasicGA.
 * 
 *
 * @author 
 */
public class BasicGANodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
	.getLogger(BasicGANodeModel.class);


	public static final String CFG_POPULATION = "Population";

	public static final String CFG_ITERATIONS = "Iterations";

	public static final String CFG_SELPRES = "Selection Pressure";

	public static final String CFG_COL_1 = "Col1";

	public static final String CFG_COL_2 = "Col2";


	public static final String CFG_SFUNCTIONS = "scoring functions";


	public static final String CFG_SCORES = "No. of scores";


	public static final String CFG_PERCENTMUTATE = "percentage of mutations";


	public static final String CFG_TARGET = "minimum length to grow to";

	//public static final String CFG_NOPROPS = "no. of scoring functions";

	private final SettingsModelString m_col_1 = 
		new SettingsModelString(CFG_COL_1, "col1");

	private final SettingsModelString m_col_2 = 
		new SettingsModelString(CFG_COL_2, "col2");

	private SettingsModelInteger m_size_of_pop = new SettingsModelInteger(
			CFG_POPULATION, 2);

	private SettingsModelInteger m_iterations = new SettingsModelInteger(
			CFG_ITERATIONS, 2);

	private SettingsModelDouble m_sel_pres = new SettingsModelDouble(
			CFG_SELPRES, 2);

	private SettingsModelInteger m_scores = new SettingsModelInteger(
			CFG_SCORES, 2);
	
	private SettingsModelStringArray m_sfunctions = 
		new SettingsModelStringArray(CFG_SFUNCTIONS, null);
	
	private SettingsModelDouble m_percentmutate = new SettingsModelDouble(
			CFG_PERCENTMUTATE, 2);
	
	private SettingsModelInteger m_targetsubnet = new SettingsModelInteger(
			CFG_TARGET, 2);
	
	//private SettingsModelInteger m_no_props = new SettingsModelInteger(
	//		CFG_NOPROPS, 1);


	/**
	 * Constructor for the node model.
	 */
	protected BasicGANodeModel() {

		super(1, 1);
	}

	class Node {
		private int idno;
		private String nodeid;
		private int degree;

		public Node(int idno, String nodeid, int degree){
			this.idno=idno;
			this.nodeid=nodeid;
			this.degree=degree;
		}

		public int getidno() {return idno;}
		public String getnodeid() {return nodeid;}
		public int getdegree() {return degree;}

		public void plusdegree() { ++degree; }
	}

	class Connect {
		private int n1;
		private int n2;

		public Connect(int n1, int n2){
			this.n1=n1;
			this.n2=n2;
		}

		public int getn1() {return n1;}
		public int getn2() {return n2;}
	}

	class Subnet {
		private ArrayList<Node> subset;
		private double[] score;
		private int rank;

		public Subnet(ArrayList<Node> subset,int rank){
			this.subset =subset;
			this.rank=rank;
		}

		public int getrank() {return rank;}
		public double[] getscore() {return score;}
		public ArrayList<Node> getsubset() {return subset;}

		public void setrank(int rank) {this.rank=rank;}
		public void setscore(double[] score) {this.score=score;}

	}


	/**
	 * {@inheritDoc}
	 */
	 @Override
	 protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			 final ExecutionContext exec) throws Exception {

		 try{
		

		 //GetNetwork();
		 //GenerateNodeList();
		 //CalculateDegree();

		 int net_col_1 = inData[0].getDataTableSpec().findColumnIndex(
				 m_col_1.getStringValue());
		 int net_col_2 = inData[0].getDataTableSpec().findColumnIndex(
				 m_col_2.getStringValue());
		 
		 System.out.println("column index "+inData[0].getDataTableSpec().findColumnIndex(
				 m_col_2.getStringValue()));

		 ArrayList<Connect> networkArray = new ArrayList<Connect>();
		 ArrayList<Node> nodeArray = new ArrayList<Node>();

		 int nmax=0;

		 SparseGraph<String, String> graph = new SparseGraph<String,String>();
		 
		 for (DataRow rgnRow : inData[0])
		 {
			 exec.checkCanceled();
			 String s1 = rgnRow.getCell(net_col_1).toString();
			 String s2 = rgnRow.getCell(net_col_2).toString();

			 graph.addVertex(s1);
			 graph.addVertex(s2);
			 graph.addEdge(s1+"-"+s2, s1, s2);
			 
			 int n1 =0;
			 int n2= 0;

			 for (Node nodet: nodeArray)
			 {
				 if(nodet.getnodeid().equals(s1)) 
				 {
					 n1=nodet.getidno();
					 nodet.plusdegree();
				 }
				 if(nodet.getnodeid().equals(s2)) 
				 {
					 n2=nodet.getidno();
					 nodet.plusdegree();
				 }
			 }
			 if(n1==0) 
			 {
				 n1=nmax+1;
				 Node node1 = new Node(n1,s1,1);
				 nodeArray.add(node1);
				 ++nmax;
			 }
			 if(n2==0) 
			 {
				 n2=nmax+1;
				 Node node2 = new Node(n2,s2,1);
				 nodeArray.add(node2);
				 ++nmax;
			 }
			 Connect connect = new Connect(n1,n2); 

			 networkArray.add(connect);

		 }
		 
		 System.out.println(graph.toString());
		 
		 System.out.println("no. of nodes is "+nodeArray.size());
		 //for(Node test:nodeArray) System.out.print(test.getnodeid()+" ");

		 //GenerateInitialPopulation();

		 ArrayList<Subnet> population = new ArrayList<Subnet>();

		 int startingsubsetlength=m_targetsubnet.getIntValue();

		 for(int i=0;i<m_size_of_pop.getIntValue();++i)
		 {
			 ArrayList<Node> tempnodeArray = new ArrayList<Node>();
			 for(Node temp :nodeArray) tempnodeArray.add(temp);

			 Random generator1 = new Random();

			 ArrayList<Node> subset = new ArrayList<Node>();

			 for(int j=0;j<startingsubsetlength;++j)
			 {
				 int rand1 = generator1.nextInt(tempnodeArray.size());
				 subset.add(tempnodeArray.get(rand1));
				 tempnodeArray.remove(rand1);
			 }
			
			 Subnet subnet = new Subnet(subset,0);
			 if(IdentityTest(subnet,population,exec))
			 population.add(subnet);
		 }

		 //for(subnet in InitialPopulation)
		 //ScoreSubnet(subnet);

		 String[] sfunctions=m_sfunctions.getStringArrayValue();
		 
		 for(Subnet subnet:population)
		 {
			 double[] score =new double[m_scores.getIntValue()];
			 
			 for(int k=0; k<m_scores.getIntValue();++k)
			 {
				 score[k]=scorefunction(subnet,sfunctions[k],exec);
			 }
			 
			 subnet.setscore(score);
		 }

		 //ParetoRankSubnets(InitialPopulation);
		 

		 ArrayList<double[]> data = new ArrayList<double[]>();

		 for (Subnet subnet:population)
		 {
			 double[]scorearray= subnet.getscore();
			 data.add(scorearray);				    	
		 }

		 int res[] = paretoRank(data, m_scores.getIntValue(), exec);

		 for (int p = 0; p < population.size(); ++p)
		 {
			 population.get(p).setrank(res[p]);		    	
		 }

		 for(Subnet trial:population) {
			 System.out.println(trial.getsubset().size());
			 for(Node test:trial.getsubset()) System.out.print(test.getnodeid()+" ");
			 System.out.println();
			 
		 }
		 //while(GArunning)

		 //GenerateNewSubnet()

		 //	if(mutate)

		 //		RouletteWheelSelect(subnet1);
		 //		MutateSubnet(subnet1);

		 //	else if(crossover)

		 //		RouletteWheelSelect(subnet1);
		 //		RouletteWheelSelect(subnet2);
		 //		Crossover(subnet1,subnet2);


		 //	ScoreSubnet();
		 //	RemovePoorSubnet();

		int GArunning = 0;

		 while(GArunning<m_iterations.getIntValue())
		 {
			 int GeneratePopulation = 0;
			 ArrayList<Subnet> totpopulation = new ArrayList<Subnet>();
			 for(Subnet subnet:population) totpopulation.add(subnet);
			 
			 //generate double size population
			 
			 while(GeneratePopulation<m_size_of_pop.getIntValue())
			 {
				 Random generator = new Random();
			 	double randomOne = generator.nextDouble();
			 	Subnet newNet = null; 
			 	
			 	if(randomOne<m_percentmutate.getDoubleValue())
			 	{
				 	Subnet currentNet = population.get(roulettewheel(population,exec));
				 
				 	newNet = Mutate(currentNet, nodeArray,exec);
				 
			 	}
			 	else
			 	{
				 	Subnet currentNet = population.get(roulettewheel(population,exec));
				 
				 	newNet = Crossover(currentNet,population,exec);
			 	}
			 
			 	if(IdentityTest(newNet,totpopulation,exec))
			 	{
			 		double[] score =new double[m_scores.getIntValue()];
					for(int k=0; k<m_scores.getIntValue();++k)
					 {
						 score[k]=scorefunction(newNet,sfunctions[k],exec);
					 }
					 
					 newNet.setscore(score);
			 		
			 		totpopulation.add(newNet);
				 	++GeneratePopulation;
			 	}
			 }
			 
			  // rank the doubled population
			 
			 ArrayList<double[]> data1 = new ArrayList<double[]>();

			 for (Subnet subnet:totpopulation)
			 {
				  data1.add(subnet.getscore());				    	
			 }

			 int res1[] = paretoRank(data1, m_scores.getIntValue(), exec);

			 for (int p = 0; p < totpopulation.size(); ++p)
			 {
				 totpopulation.get(p).setrank(res1[p]);		    	
			 }
			 
			 //reduce back to single population
			 
			 population.clear();
			 
			 int[] count =new int[totpopulation.size()];
				
			//System.out.println("totpopulation = "+totpopulation.size());
				
				
				for (Subnet novel : totpopulation)
				{
				
					CountLoop:
					for (int i2=0;i2<totpopulation.size();++i2)
					{
						if(novel.getrank()==i2)
						{
						++count[i2];
						break CountLoop;
						}
					}
				}
				
				int finalcount =0;
				int track =0;
				
				TrackLoop:
				for (int i2=0;i2<totpopulation.size();++i2)
				{
					//System.out.println("Rank = "+i2+" and count = "+ count[i2]);
					
					track=track+count[i2];
					if(track>m_size_of_pop.getIntValue()) 
					{
						finalcount=i2;
						break TrackLoop;
					}
					
				}
				
				int residue = m_size_of_pop.getIntValue()+count[finalcount] - track ;
				int rr =0;
				
				
				//System.out.println("track = "+track);
				//System.out.println("finalcount = " +finalcount);
				//System.out.println("residue = " +residue);
				
				Collections.shuffle(totpopulation);
				
											
				for (Subnet novel : totpopulation)
				{
					if(novel.getrank()<finalcount) 
					{
					population.add(novel);
					
					}
					else if(novel.getrank()==finalcount)
						{
						if(rr<residue)
							{
							population.add(novel);
							
							}
						++rr;
						}
				}
							
				totpopulation.clear();
				++GArunning;
		
				
		 }
		 
		 //Output final results set
		 
		 for(Subnet trial:population) {
			 System.out.println(trial.getsubset().size());
			 for(Node test:trial.getsubset()) System.out.print(test.getnodeid()+" ");
			 System.out.println();
			 
		 }
		 	


			 // the data table spec of the single output table, 
			 // the table will have three columns:
			 DataColumnSpec[] allColSpecs = new DataColumnSpec[3];
			 allColSpecs[0] = 
				 new DataColumnSpecCreator("Subnet", StringCell.TYPE).createSpec();
			 allColSpecs[1] = 
				 new DataColumnSpecCreator("Length", IntCell.TYPE).createSpec();
			 allColSpecs[2] = 
				 new DataColumnSpecCreator("Rank", IntCell.TYPE).createSpec();
			 DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
			 // the execution context will provide us with storage capacity, in this
			 // case a data container to which we will add rows sequentially
			 // Note, this container can also handle arbitrary big data tables, it
			 // will buffer to disc if necessary.
			 BufferedDataContainer container = exec.createDataContainer(outputSpec);
			 // let's add m_count rows to it
			 for (int i = 0; i < m_size_of_pop.getIntValue(); i++) {
				 
				 Subnet outputnet = population.get(i);
				 String subsettostring = " ";
				 
				 ArrayList<Node> subset =outputnet.getsubset();
				 
				 for(Node test:subset) System.out.print(test.getnodeid()+" ");
				 System.out.println();	
				 
					for(Node node:subset)
					{
						subsettostring=subsettostring.concat(node.getnodeid()+",");
					}
				 System.out.println("string is " +subsettostring);
				 
				 RowKey key = new RowKey("Row " + i);
				 // the cells of the current row, the types of the cells must match
				 // the column spec (see above)
				 DataCell[] cells = new DataCell[3];
				 cells[0] = new StringCell(subsettostring); 
				 cells[1] = new IntCell(outputnet.getsubset().size()); 
				 cells[2] = new IntCell(outputnet.getrank());
				 DataRow row = new DefaultRow(key, cells);
				 container.addRowToTable(row);

				 // check if the execution monitor was canceled
				 exec.checkCanceled();
				 exec.setProgress(i / (double)3, 
						 "Adding row " + i);
			 }
			 // once we are done, we close the container and return its table
			 container.close();
			 BufferedDataTable out = container.getTable();
			 return new BufferedDataTable[]{out};
			 
	 } catch (Exception ex) {
		            ex.printStackTrace();
		            throw ex;
		        }
	 }
	 
	 
		 /**
		  * {@inheritDoc}
		  */
		 @Override
		 protected void reset() {
			 // TODO Code executed on reset.
			 // Models build during execute are cleared here.
			 // Also data handled in load/saveInternals will be erased here.
		 }

		 /**
		  * {@inheritDoc}
		  */
		 @Override
		 protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
		 throws InvalidSettingsException {

			 // TODO: check if user settings are available, fit to the incoming
			 // table structure, and the incoming types are feasible for the node
			 // to execute. If the node can execute in its current state return
			 // the spec of its output data table(s) (if you can, otherwise an array
			 // with null elements), or throw an exception with a useful user message

			 return new DataTableSpec[]{null};
		 }

		 /**
		  * {@inheritDoc}
		  */
		 @Override
		 protected void saveSettingsTo(final NodeSettingsWO settings) {

			 // TODO save user settings to the config object.


			 m_col_1.saveSettingsTo(settings);
			 m_col_2.saveSettingsTo(settings);
			 m_iterations.saveSettingsTo(settings);
			 m_sel_pres.saveSettingsTo(settings);
			 m_size_of_pop.saveSettingsTo(settings);
			 m_scores.saveSettingsTo(settings);
			 m_sfunctions.saveSettingsTo(settings);
			 m_percentmutate.saveSettingsTo(settings);
			 m_targetsubnet.saveSettingsTo(settings);
		 }

		 /**
		  * {@inheritDoc}
		  */
		 @Override
		 protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
		 throws InvalidSettingsException {

			 // TODO load (valid) settings from the config object.
			 // It can be safely assumed that the settings are valided by the 
			 // method below.


			 m_col_1.loadSettingsFrom(settings);
			 m_col_2.loadSettingsFrom(settings);
			 m_iterations.loadSettingsFrom(settings);
			 m_sel_pres.loadSettingsFrom(settings);
			 m_size_of_pop.loadSettingsFrom(settings);
			 m_scores.loadSettingsFrom(settings);
			 m_sfunctions.loadSettingsFrom(settings);
			 m_percentmutate.loadSettingsFrom(settings);
			 m_targetsubnet.loadSettingsFrom(settings);
		 }

		 /**
		  * {@inheritDoc}
		  */
		 @Override
		 protected void validateSettings(final NodeSettingsRO settings)
		 throws InvalidSettingsException {

			 // TODO check if the settings could be applied to our model
			 // e.g. if the count is in a certain range (which is ensured by the
			 // SettingsModel).
			 // Do not actually set any values of any member variables.


			 m_col_1.validateSettings(settings);
			 m_col_2.validateSettings(settings);
			 m_iterations.validateSettings(settings);
			 m_sel_pres.validateSettings(settings);
			 m_size_of_pop.validateSettings(settings);
			 m_scores.validateSettings(settings);
			 m_sfunctions.validateSettings(settings);
			 m_percentmutate.validateSettings(settings);
			 m_targetsubnet.validateSettings(settings);
		 }

		 /**
		  * {@inheritDoc}
		  */
		 @Override
		 protected void loadInternals(final File internDir,
				 final ExecutionMonitor exec) throws IOException,
				 CanceledExecutionException {

			 // TODO load internal data. 
			 // Everything handed to output ports is loaded automatically (data
			 // returned by the execute method, models loaded in loadModelContent,
			 // and user settings set through loadSettingsFrom - is all taken care 
			 // of). Load here only the other internals that need to be restored
			 // (e.g. data used by the views).

		 }

		 /**
		  * {@inheritDoc}
		  */
		 @Override
		 protected void saveInternals(final File internDir,
				 final ExecutionMonitor exec) throws IOException,
				 CanceledExecutionException {

			 // TODO save internal models. 
			 // Everything written to output ports is saved automatically (data
			 // returned by the execute method, models saved in the saveModelContent,
			 // and user settings saved through saveSettingsTo - is all taken care 
			 // of). Save here only the other internals that need to be preserved
			 // (e.g. data used by the views).

		 }



		 private int[] paretoRank(ArrayList<double[]> data, 
				 int noprop,
				 final ExecutionContext exec) throws CanceledExecutionException
		 {
			 int[] res = new int[data.size()];
			 int[] dominated_by = new int[data.size()]; //solution at i is dominated by dominatedBy[i] other solutions
			 List<Integer> curr_front = new ArrayList<Integer>();
			 Map<Integer,ArrayList<Integer>> dominates = new HashMap<Integer,ArrayList<Integer>>();

			 for (int i = 0; i < data.size(); ++i)
			 {
				 for (int j = i + 1; j < data.size(); ++j)
				 {
					 int dom1 = 0, dom2 = 0;
					 for (int p = 0; p < noprop; ++p)
					 {
						 if (data.get(i)[p] < data.get(j)[p]) dom1 += 1;
						 else if (data.get(i)[p] > data.get(j)[p]) dom2 += 1;
					 }
					 if (dom1 > 0 && dom2 == 0)
					 {
						 dominated_by[j] += 1;
						 if (null == dominates.get(i))
							 dominates.put(i, new ArrayList<Integer>());
						 dominates.get(i).add(j);
					 }
					 else if (dom2 > 0 && dom1 == 0)
					 {
						 dominated_by[i] += 1;
						 if (null == dominates.get(j))
							 dominates.put(j, new ArrayList<Integer>());
						 dominates.get(j).add(i);
					 }
					 exec.setProgress((data.size() + i*data.size() + j)/2);
					 exec.checkCanceled();
				 }
				 if (0 == dominated_by[i]) curr_front.add(i);
			 }

			 int rank = 1;
			 while (0 < curr_front.size())
			 {
				 List<Integer> new_front = new ArrayList<Integer>();
				 for (int i : curr_front)
				 {
					 res[i] = rank;
					 List<Integer> dom_ind = dominates.get(i);
					 if (null != dom_ind)
						 for (int di : dom_ind)
						 {
							 dominated_by[di] -= 1;
							 if (0 == dominated_by[di]) new_front.add(di);
						 }
				 }
				 curr_front = new_front;
				 rank += 1;
			 }

			 return res;
		 }
		 
		 private double scorefunction(Subnet subnet , String scoreid,
				 final ExecutionContext exec) throws CanceledExecutionException
		 {
			double score =0;
			
			if(scoreid.equalsIgnoreCase("SumofDegree"))
			{
				ArrayList<Node> subset =subnet.getsubset();
				for(Node node:subset)
				{
					score =score+node.getdegree();
				}
				score = -score;
			}
			else if(scoreid.equalsIgnoreCase("AverageDegree"))
			{
				ArrayList<Node> subset =subnet.getsubset();
				int count=0;
				for(Node node:subset)
				{
					score =score+node.getdegree();
					++count;
				}
				score=-score/count;
			}
			else if(scoreid.equalsIgnoreCase("MaxDegree"))
			{
				ArrayList<Node> subset =subnet.getsubset();
				
				for(Node node:subset)
				{
					if(node.getdegree()>score) score =node.getdegree();
				}
				score = -score;
			}
			
			
			return score;
		 }
		 
		 private int roulettewheel(ArrayList<Subnet> population,
				 final ExecutionContext exec) throws CanceledExecutionException
		 {
			 int setp =0;
			 Random generator = new Random();

			 int[] countofsol = new int[m_size_of_pop.getIntValue()+1];
			 Arrays.fill(countofsol, 0);
			 for (int p = 0; p < m_size_of_pop.getIntValue(); ++p)
			 {
				 for (int q= 1; q<  m_size_of_pop.getIntValue(); ++q)
				 {
					 if(population.get(p).getrank()==q) ++countofsol[q];
				 }

			 }
			 double[] rcount = new double[m_size_of_pop.getIntValue()+1];
			 for (int p = 0; p < m_size_of_pop.getIntValue(); ++p)
			 {

				 if(population.get(p).getrank()<=m_size_of_pop.getIntValue()) rcount[p]= 1+ countofsol[population.get(p).getrank()];
				 for (int q= 1; q<population.get(p).getrank()-1; ++q)
				 {
					 rcount[p] = rcount[p]+ 2* countofsol[q];
				 }

			 }    



			 double wheel = 0;

			 for (int p = 0; p < m_size_of_pop.getIntValue(); ++p)
			 {
				 wheel = wheel + (m_sel_pres.getDoubleValue()*(m_size_of_pop.getIntValue()+1-rcount[p]) 
						 + countofsol[p] -2)/(m_size_of_pop.getIntValue()*(m_size_of_pop.getIntValue()-1));

				 //writer1.println("Molecule " + p + " is Pareto rank " + molarray.get(p).res);
				 //wheel = wheel + 1/molarray.get(p).res;			    	
			 }

			 double randomOne = generator.nextDouble();

			 double randomWheel = wheel*randomOne;

			 //System.out.println("wheel= "+wheel);
			 //System.out.println("randomwheel= "+randomWheel);

			 double testwheel =0;
			 //muppetry:
			 for (int p = 0; p < m_size_of_pop.getIntValue(); ++p)
			 {
				 testwheel = testwheel + (m_sel_pres.getDoubleValue()*(m_size_of_pop.getIntValue()+1-rcount[p])
						 + countofsol[p] -2)/(m_size_of_pop.getIntValue()*(m_size_of_pop.getIntValue()-1));
				 if (randomWheel <testwheel)
				 {
					 setp = p;

					 p= m_size_of_pop.getIntValue() +1;

				 }

			 }
			 return setp;	

		 }
		 
		 private boolean IdentityTest(Subnet subnet, ArrayList<Subnet> population,
		  		final ExecutionContext exec) throws CanceledExecutionException
		  {
			 boolean it =true;
			 
			 return it;
		  }
		  
		 private Subnet Mutate(Subnet currentNet,ArrayList<Node> nodeArray,
				 final ExecutionContext exec) throws CanceledExecutionException
		  {
			 ArrayList<Node> subset = currentNet.getsubset();
			 
			 Random generator = new Random();
			 int rand = generator.nextInt(subset.size());
			 
			 ArrayList<Node> tempnodeArray = new ArrayList<Node>();
			 for(Node temp :nodeArray) tempnodeArray.add(temp);
			 for(Node temp1 : subset) tempnodeArray.remove(temp1);

			 int rand1 = generator.nextInt(tempnodeArray.size());
			 subset.remove(rand);
			 subset.add(tempnodeArray.get(rand1));
			
			 Subnet newNet = new Subnet(subset,0);
			 
			 return newNet;
		  }
		 
		 private Subnet Crossover(Subnet currentNet,ArrayList<Subnet> population,
				 final ExecutionContext exec) throws CanceledExecutionException
		  {
			
				 
			 ArrayList<Node> subset = currentNet.getsubset();
			 
			 Random generator = new Random();
			 int rand = generator.nextInt(subset.size());
			 
			 ArrayList<Subnet> tempPop = new ArrayList<Subnet>();
				for(Subnet temp:population) tempPop.add(temp); 
				 tempPop.remove(currentNet);
			 
			 int rand1 = generator.nextInt(tempPop.size());
			 
			 Subnet crossNet = tempPop.get(rand1);
			 ArrayList<Node> crossset = crossNet.getsubset();
			 
			 ArrayList<Node> newset = new ArrayList<Node>();
			 
			 for(int i=0;i<rand;++i)newset.add(subset.get(i));
			 for(int j=rand;j<crossset.size();++j)newset.add(crossset.get(j));
			 
			 Subnet newNet= new Subnet(newset,0);
			 
			 return newNet;
		  }
		 
	 }

