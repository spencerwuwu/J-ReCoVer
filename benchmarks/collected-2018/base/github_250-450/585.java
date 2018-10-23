// https://searchcode.com/api/result/69655826/

package ncsa.d2k.modules.projects.mbabbar.optimize.ga.util;

import ncsa.d2k.modules.core.optimize.ga.*;
import ncsa.d2k.modules.core.optimize.ga.emo.*;
import ncsa.d2k.modules.core.optimize.ga.nsga.*;
import ncsa.d2k.modules.core.optimize.util.*;
import ncsa.d2k.core.modules.*;
import ncsa.d2k.modules.projects.mbabbar.optimize.ga.iga.*;
import java.io.Serializable;
import ncsa.d2k.modules.core.datatype.table.*;
import ncsa.d2k.modules.core.datatype.table.ColumnTypes;
import java.util.*;

/**
		ExportGenesIntoPopulFromTable.java

*/
public class ExportGenesIntoPopulFromTable extends ncsa.d2k.core.modules.DataPrepModule
                                            implements Serializable {

        /////////////////////////
        // Module Fields
        Population pop;
        Table geneTable;
        protected long randomSeed = 1000;
        public Random randNum = new Random(randomSeed);
        // randomSelection is true when data from table (i.e. the genes) is randomly selected into the population
        boolean randomSelection = false;
        // numClassesForRandomSelection is a non-zero positive integer used for dividing all the rows in the table into
        // this constant number of sampling classes. The genes are then randomly selected one by one from each sampling class. A gene once selected is not
        // selected again, hence repetitive selection of genes is avoided. This is similar to latin hypercube sampling.
        int numClassesForRandomSelection = 0;
        // numRandomGenesSelected is the total number of individuals to be selected randomly from the table.
        int numRandomGenesSelected = 0;
        /////////////////////////
        // get set methods for module fields
        public boolean getRandomSelection (){
          return randomSelection;
        }
        public void setRandomSelection (boolean rsFlag) {
          randomSelection = rsFlag;
        }
        public int getNumClassesForRandomSelection (){
          return numClassesForRandomSelection;
        }
        public void setNumClassesForRandomSelection (int numClasses) {
          numClassesForRandomSelection = numClasses;
        }
        public int getNumRandomGenesSelected (){
          return numRandomGenesSelected;
        }
        public void setNumRandomGenesSelected (int num) {
          numRandomGenesSelected = num;
        }
        // get random number seed
        public long getRandomSeed (){
          return randomSeed;
        }

       // set random number seed
        public void setRandomSeed (long seed){
          randomSeed = seed;
        }
	//////////////////////////////////
	// Info methods
	//////////////////////////////////
	/**
		This method returns the description of the various inputs.
		@return the description of the indexed input.
	*/
	public String getInputInfo (int index) {
		switch (index) {
			case 0: return " Population   ";
                        case 1: return " Genes Input Table ";
			default: return "No such output";
		}
	}

	/**
		This method returns the description of the various inputs.
		@return the description of the indexed input.
	*/
	public String getOutputInfo (int index) {
		switch (index) {
			case 0: return " Population   ";
			default: return "No such input";
		}
	}
	/**
		This method returns the description of the module.

		@return the description of the module.
	*/
	public String getModuleInfo () {
		return "<html>  <head>      </head>  <body> This module exports initial set of genes into a population from a table. The table could be read from a file. </body></html>";
	}

	/**
		Create the initial population. In this case we have chosen to override the doit method,
		though it was probably not necessary

		@param outV the array to contain output object.
	*/




	public void doit () throws Exception {

                pop = (Population) this.pullInput(0);
                geneTable = (Table) this.pullInput(1);

                // Random number generator


                // read genes from Table and assign them to individuals in the population.
                int npopsize = pop.size();
                int numGenesInTable = geneTable.getNumRows();

                // indices of genes in the Table that are chosen for the population, either randomly or serially
                int numGenesSelected = 0;
                int [] selectedGenesIds;

                if (randomSelection == true){  // random selection

                    numGenesSelected = numRandomGenesSelected;
                    selectedGenesIds = new int [numRandomGenesSelected];

                    if (numClassesForRandomSelection > numGenesInTable){
                      System.out.println("Error : There are more classes requested than the number of data in the table."+
                                        " Please reduce the numClassesForRandomSelection variable to a value equal or lesser than "+
                                        " the number of data in the Table.") ;
                      System.exit(0);
                    }
                    else{
                      if (numRandomGenesSelected > npopsize){
                          System.out.println("Error : There are more number of genes requested from the table than the population size."+
                                        " Please reduce the numRandomGenesSelected variable to a value equal or lesser than "+
                                        " the population size.") ;
                          System.exit(0);
                      }
                      else{
                          // number of genes in each sampling class
                          int numIndsInEachClass = (int) (numGenesInTable / numClassesForRandomSelection);
                          // min id of genes in each sampling class
                          int classMinId [] = new int [numClassesForRandomSelection];
                          // max id of genes in each sampling class
                          int classMaxId [] = new int [numClassesForRandomSelection];
                          // assigning min and max ids of the genes for each sampling class
                          for (int i =0; i < numClassesForRandomSelection; i++) {
                              classMinId [i] = i * numIndsInEachClass;
                              classMaxId [i] = ((i+1) * numIndsInEachClass) - 1 ;
                          }
                          // making sure that the last class has the id of the last gene in the gene Table.
                          classMaxId [numClassesForRandomSelection-1] = numGenesInTable -1;

                          // initializing the selectedGenesIds to a negative integer
                          for (int i =0; i < numRandomGenesSelected; i++){
                              selectedGenesIds [i] = -1;
                          }

                          // random picking of individuals from each sampling class
                          if ( numRandomGenesSelected < numClassesForRandomSelection ){
                              // when number of indivs to be randomly selected are lesser than number of classes.
                              for (int i =0; i < numRandomGenesSelected; i++){
                                 int count;
                                 count = randNum.nextInt(classMaxId [i] - classMinId [i] + 1) ;
                                 selectedGenesIds [i] = classMinId [i] + count;
                              }
                          }
                          else{
                              // when there are lesser number of classes than the number of indivs being selected.
                              int numFullLoops = (int) (numRandomGenesSelected / numClassesForRandomSelection);
                              int numRemainingClasses = numRandomGenesSelected - (numFullLoops * numClassesForRandomSelection);

                              // looping through all the classes 'numFullLoops' times.
                              int selecIndCounter = 0;
                              for (int j =0; j < numFullLoops; j++){
                                // picking a random id from each class.
                                for (int i =0; i < numClassesForRandomSelection; i++){
                                     int count, number;
                                     count = randNum.nextInt(classMaxId [i] - classMinId [i] + 1) ;
                                     number = classMinId [i] + count;
                                     // search for another random number till we obtain a id that was not previously chosen.
                                     while ( intSearchResult (number , selectedGenesIds)){
                                        count = randNum.nextInt(classMaxId [i] - classMinId [i] + 1) ;
                                        number = classMinId [i] + count;
                                     }
                                     selectedGenesIds [selecIndCounter] = number;
                                     selecIndCounter ++;
                                 }
                              }
                              // looping through remaining classes
                              for (int i =0; i < numRemainingClasses; i++){
                                     int count, number;
                                     count = randNum.nextInt(classMaxId [i] - classMinId [i] + 1) ;
                                     number = classMinId [i] + count;
                                     // search for another random number till we obtain a id that was not previously chosen.
                                     while ( intSearchResult (number , selectedGenesIds)){
                                        count = randNum.nextInt(classMaxId [i] - classMinId [i] + 1) ;
                                        number = classMinId [i] + count;
                                     }
                                     selectedGenesIds [selecIndCounter] = number;
                                     selecIndCounter ++;
                              }
                          }

                      }
                    }

                }
                else {  // serial selection
                    if (npopsize < numGenesInTable ){
                      numGenesSelected = npopsize;
                    }
                    else {
                      numGenesSelected = numGenesInTable;
                    }
                    // assign ids of genes in the table to selectedGenesIds array. Genes with these ids
                    // will be assigned to population later.
                    selectedGenesIds = new int [numGenesSelected];
                    for (int i = 0; i < numGenesSelected; i++){
                      selectedGenesIds[i] = i;
                    }
                }

                // assign genes from Table to Population
                for (int i=0; i < numGenesSelected; i++){
                        if (geneTable.getColumnType(0) == ColumnTypes.BOOLEAN){
                            boolean [] geneArray = new boolean [geneTable.getNumColumns()];
                            for (int j=0; j < geneTable.getNumColumns(); j++){
                              geneArray [j] = geneTable.getBoolean(selectedGenesIds[i],j);
                            }
                            pop.getMember(i).setParameters(geneArray);
                        }
                        else {
                            double [] geneArray = new double [geneTable.getNumColumns()];
                            for (int j=0; j < geneTable.getNumColumns(); j++){
                              geneArray [j] = geneTable.getDouble(selectedGenesIds[i],j);
                            }
                            pop.getMember(i).setParameters(geneArray);
                        }
                }

		this.pushOutput (pop, 0);
    	}

        /**
         * This method searches for an integer in an array and returns true or false as the case maybe.
         */
        public boolean intSearchResult (int ii, int [] intArray){
            boolean result = false;
            for (int i = 0; i < intArray.length; i++){
              if (ii == intArray [i]){
                  result = true;
              }
            }
            return result;
        }

	/**
	 * Return the human readable name of the module.
	 * @return the human readable name of the module.
	 */
	public String getModuleName() {
		return "ExportGenesIntoPopulFromTable";
	}

	/**
	 * Return the human readable name of the indexed input.
	 * @param index the index of the input.
	 * @return the human readable name of the indexed input.
	 */
	public String getOutputName(int index) {
		switch(index) {
			case 0:
				return "Population";
			default: return "NO SUCH OUTPUT!";
		}
	}

	/**
	 * Return the human readable name of the indexed output.
	 * @param index the index of the output.
	 * @return the human readable name of the indexed output.
	 */
	public String getInputName(int index) {
		switch(index) {
			case 0:
				return "Population";
                        case 1:
				return "Genes Input Table";
			default: return "NO SUCH OUTPUT!";
		}
	}

        	//////////////////////////////////
	// Type definitions.
	//////////////////////////////////

	public String[] getOutputTypes () {
		String[] types = {"ncsa.d2k.modules.core.optimize.ga.Population"};
		return types;
	}

	public String[] getInputTypes () {
		String[] types = {"ncsa.d2k.modules.core.optimize.ga.Population", "ncsa.d2k.modules.core.datatype.table.basic.TableImpl"};
		return types;
	}
}

