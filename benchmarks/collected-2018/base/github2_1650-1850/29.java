// https://searchcode.com/api/result/69655920/

package ncsa.d2k.modules.projects.mbabbar.optimize.ga.iga;

import java.io.Serializable;
import ncsa.d2k.modules.core.optimize.ga.*;
import ncsa.d2k.modules.core.optimize.ga.nsga.*;
import ncsa.d2k.modules.core.datatype.table.*;
import ncsa.d2k.modules.core.datatype.table.basic.*;
import ncsa.d2k.modules.core.datatype.table.transformations.*;
import ncsa.d2k.modules.core.optimize.util.*;
import ncsa.d2k.modules.projects.mbabbar.optimize.ga.iga.*;
import java.math.*;

public class IGANsgaPopulation extends Population implements Serializable{

	/** number of objectives to optimize on.*/
	int numObjectives;
        
        public boolean restart = false;

        /////////////////////////////////////////////////////////////
        // Added by Meghna Babbar for Interactive Genetic Algorithms.
        /** Whether some objectives are qualitative in nature or not. */
        public boolean [] igaQualObj;

        /* Number of individuals in the entire population ranked for human evaluation over all the IGA sessions*/
        int totnumIndivsRanked = 0;
        
        int maxGen = 100;

        /* Number of individuals in the population that should be ranked by user in the
         * interactive session for IGA. Recommended value = 20
         */
        int numIndivsRankedByUser = 40;
        
        int totalNumIndivsRankedByUser = 0;

        /* Number of ranking sessions that a user is expecting to go
         * through.
         */
        int numExpectedRankingSessions = 40;

        // Population of all individuals already ranked by User
       // protected MOBinaryIndividual [] humanRankedPopulationArchive ;
        int [][] rankIds = null;
        int [] numRank =  {0,0,0,0,0};
        
          
        protected Individual [] humanRankedPopulationArchive ;

        protected int numArchivedSolutions = 0;

        /////////////////////////////////////////////////////////////

	/** the combined population of parents and progeny. */
	protected IGANsgaSolution [] combinedPopulation = null;

	/** the Pareto fronts. */
	public ParetoFront fronts;

	/** the maxfitness values. */
	double [] maxFitnesses = null;

	/** the minimum fitness values. */
	double [] minFitnesses = null;

	/** indices of the individuals possessing the max fitnesses. */
	int [] maxFitnessMembers = null;

	/** indices of the individuals possessing the max fitnesses. */
	int [] minFitnessMembers = null;

	/** constrains the range and polarity of the obectives, maximizing or minimizing,
	 *  and handles the comparison of values of that trait.
	 */
	ObjectiveConstraints [] objectiveConstraints = null;
	public ObjectiveConstraints [] getObjectiveConstraints () { return objectiveConstraints; }


	/** this is the target fitness we strive for. */
	double target;

	/** this is some measure of the current fitness, averaged best in this case. */
	double currentMeasure;

	/** this is the best possible fitness. */
	double best;

	/** the worst possible. */
	double worst;

        /////////////////////////////////////////////////////////////////////////////
        // Get and Set methods for some variables that will be used by Interactive GAs.
        // Added by Meghna Babbar

        /**
         * returns number of qualitative objectives
         * @return
         */
        public int getnumQualObjs () {
            int numQlObjs = 0;
            for (int i=0; i< igaQualObj.length; i++){
                if (igaQualObj [i] == true)
                  numQlObjs++;
            }
            return numQlObjs;
        }

	/**
	 * whether qualitative objectives exist in the population (to be used by
         * Interactive Genetic Algorithm.
	 * @returns true or false (whatever the case maybe) for a particular objective.
	 */
	public boolean[] getIgaQualObj  () {
		return igaQualObj;
	}

        /**
	 * sets flag true if qualitative objectives exist in the population (to be used by
         * Interactive Genetic Algorithm.
	 */
	public void setIgaQualObj  (boolean[] qualFlags) {
		igaQualObj = qualFlags;
	}

	/**
	 * How many individuals in the population have been ranked by the user (to be used by
         * Interactive Genetic Algorithm.
	 */
	public int getTotalNumIndivsRankedInArchive  () {
                //System.out.println ("NUMBER OF ARCHIVED SOLUTIONS : " + numArchivedSolutions);
                return numArchivedSolutions;
	}

        /** This method returns the array that stores an archive of individuals ranked by
         *  humans in previous IGA sessions.
         */

        public Individual [] getHumanRankedPopulationArchive (){
            return humanRankedPopulationArchive;
        }

        /** This method returns an individual stored in the archive of individuals
         *  previously ranked by humans.
         */

        public Individual getIndInHumanRankedPopulationArchive (int i){
             return humanRankedPopulationArchive [i];
        }
        
        public int getMaxGeneration () {
            return maxGen;
        }
        
        public void setMaxGeneration (int gen){
            maxGen = gen;
        }



        /**
         * This calculates distance between an individual and all human ranked individuals
         * This method uses the genes and quantitative objectives attributes to calculate the
         * euclidean distance
         * @param igaNi
         * @return
         */
        public double[] humanEuclidDistGeneAndObjs (IGANsgaSolution igaNi, double power) {

              // obtain genes from igaNi
              double [] igaNiGenes;
              if (igaNi instanceof MONumericIndividual) {
                       igaNiGenes = (double []) ((Individual) igaNi).getGenes ();
              }
              else{
                       igaNiGenes = (double []) ((MOBinaryIndividual) igaNi).toDouble();
              }
              // obtain numerical objectives from igaNi
              double igaNiQuantObjs [] = new double [numObjectives - getnumQualObjs()];
              int j = 0;
              for (int k = 0 ; k < numObjectives ; k++){
                  if (igaQualObj [k] == false) {
                      igaNiQuantObjs [j] = igaNi.getObjective (k);
                      j++ ;
                  }
              }

              // find total number of human ranked individuals
              int totalHmRdIndivs = this.getTotalNumIndivsRankedInArchive();

              // difference matrix that stores difference between attributes of input individual
              // and human ranked individuals
              double [][] diffMatrix = new double [totalHmRdIndivs][igaNiGenes.length + igaNiQuantObjs.length];

              for (int i = 0; i< totalHmRdIndivs; i++){
                  // obtain genes and quantitative objectives from humanRankedPopulationArchive [i]
                  double [] hmRdGenes = new double [igaNiGenes.length];
                  double [] hmRdQuantObjs = new double [numObjectives - getnumQualObjs()];

                  //if( humanRankedPopulationArchive [i].getRankedIndivFlag() == true) {
                      // obtain genes from humanRankedPopulationArchive [i]
                      if (humanRankedPopulationArchive [i] instanceof MONumericIndividual) {
                               hmRdGenes = (double []) ((Individual) humanRankedPopulationArchive [i]).getGenes ();
                               // obtain numerical objectives from humanRankedPopulationArchive [i]
                                j = 0;
                                for (int k = 0 ; k < numObjectives ; k++){
                                    if (igaQualObj [k] == false) {
                                        hmRdQuantObjs [j] = ((MONumericIndividual)humanRankedPopulationArchive [i]).getObjective (k);
                                        j++ ;
                                    }
                                }
                      }
                      else{

                               hmRdGenes = (double []) ((MOBinaryIndividual) humanRankedPopulationArchive [i]).toDoubleValues();
                               // obtain numerical objectives from humanRankedPopulationArchive [i]
                                j = 0;
                                for (int k = 0 ; k < numObjectives ; k++){
                                    if (igaQualObj [k] == false) {
                                        hmRdQuantObjs [j] = ((MOBinaryIndividual)humanRankedPopulationArchive [i]).getObjective (k);
                                        j++ ;
                                    }
                                }
                      }

                  //}
                  // Difference betweens genes attributes for igaNi and every human ranked individuals
                  for ( j= 0; j < igaNiGenes.length ; j++) {
                    diffMatrix [i][j] = Math.abs(igaNiGenes[j] - hmRdGenes[j]);
                  }

                  // Difference betweens genes attributes for igaNi and every human ranked individuals
                  for (int k = 0; k < igaNiQuantObjs.length; k++){
                    diffMatrix [i][j+k] = Math.abs(igaNiQuantObjs[k] - hmRdQuantObjs [k]);
                  }
              } // for loop "i < totalHmRdIndivs"

              // Scaling diffMatrix
              for (int i = 0; i < igaNiGenes.length + igaNiQuantObjs.length ; i++){
                  double maxDiff = 0;
                  // finding maximum value of attribute difference
                  for ( j = 0; j < totalHmRdIndivs; j++) {
                    if (maxDiff < diffMatrix [j][i]) {
                      maxDiff = diffMatrix [j][i];
                    }
                  }
                  // linear scaling of attribute difference by maximum value
                  for (j = 0; j < totalHmRdIndivs; j++) {
                      if (maxDiff != 0)
                        diffMatrix [j][i] = diffMatrix [j][i]/maxDiff;
                  }
              }

              // Calculating euclidean distance using scaled differences of all attributes
              double [] euclDist = new double [totalHmRdIndivs];
              for (int i = 0; i < totalHmRdIndivs; i++) {
                  double sumSquare = 0;
                  for (j = 0; j < igaNiGenes.length + igaNiQuantObjs.length ; j++){
                    sumSquare = sumSquare + Math.pow(diffMatrix[i][j], power);
                  }
                  euclDist [i] = Math.pow(sumSquare, (1.0/power));
              }

              return euclDist;

        }

        /**
         * This calculates distance between an individual and all human ranked individuals
         * This method uses only the genes attributes to calculate the
         * euclidean distance
         * @param igaNi
         * @return
         */
        public double[] humanEuclidDistGene (IGANsgaSolution igaNi, double power) {

              // obtain genes from igaNi
              double [] igaNiGenes;
              if (igaNi instanceof MONumericIndividual) {
                       igaNiGenes = (double []) ((Individual) igaNi).getGenes ();
              }
              else{
                       igaNiGenes = (double []) ((MOBinaryIndividual) igaNi).toDoubleValues();
              }

              // find total number of human ranked individuals
              int totalHmRdIndivs = this.getTotalNumIndivsRankedInArchive();

              // difference matrix that stores difference between attributes of input individual
              // and human ranked individuals
              double [][] diffMatrix = new double [totalHmRdIndivs][igaNiGenes.length];
              for (int i = 0; i< totalHmRdIndivs; i++){

                  // obtain genes from humanRankedPopulationArchive [i]
                  double [] hmRdGenes = new double [igaNiGenes.length];
                  //if( humanRankedPopulationArchive [i].getRankedIndivFlag() == true) {
                      // obtain genes from humanRankedPopulationArchive [i]
                      if (humanRankedPopulationArchive [i] instanceof MONumericIndividual) {
                               hmRdGenes = (double []) ((Individual) humanRankedPopulationArchive [i]).getGenes ();
                      }
                      else{

                               hmRdGenes = (double []) ((MOBinaryIndividual) humanRankedPopulationArchive [i]).toDoubleValues();
                      }
                  //}
                  // Difference betweens genes attributes for igaNi and every human ranked individuals
                  for (int j= 0; j < igaNiGenes.length ; j++) {
                    diffMatrix [i][j] = Math.abs(igaNiGenes[j] - hmRdGenes[j]);
                  }

              } // for loop "i < totalHmRdIndivs"

              // Scaling diffMatrix
              for (int i = 0; i < igaNiGenes.length ; i++){
                  double maxDiff = 0;
                  // finding maximum value of attribute difference
                  for (int j = 0; j < totalHmRdIndivs; j++) {
                    if (maxDiff < diffMatrix [j][i]) {
                      maxDiff = diffMatrix [j][i];
                    }
                  }
                  // linear scaling of attribute difference by maximum value
                  for (int j = 0; j < totalHmRdIndivs; j++) {
                      if (maxDiff != 0)
                        diffMatrix [j][i] = diffMatrix [j][i]/maxDiff;
                  }
              }

              // Calculating euclidean distance using scaled differences of all attributes
              double [] euclDist = new double [totalHmRdIndivs];
              for (int i = 0; i < totalHmRdIndivs; i++) {
                  double sumSquare = 0;
                  for (int j = 0; j < igaNiGenes.length ; j++){
                    sumSquare = sumSquare + Math.pow(diffMatrix[i][j], power);
                  }
                  euclDist [i] = Math.pow(sumSquare, (1.0/power));
              }

              return euclDist;

        }


        /**
         * This calculates distance between an individual and all human ranked individuals
         * This method uses only the quantitative objectives attributes to calculate the
         * euclidean distance
         * @param igaNi
         * @return
         */
        public double[] humanEuclidDistObjs (IGANsgaSolution igaNi, double power) {

              // obtain numerical objectives from igaNi
              double igaNiQuantObjs [] = new double [numObjectives - getnumQualObjs()];

              int j = 0;
              for (int k = 0 ; k < numObjectives ; k++){
                  if (igaQualObj [k] == false) {
                      igaNiQuantObjs [j] = igaNi.getObjective (k);
                      j++ ;
                  }
              }

              // find total number of human ranked individuals
              int totalHmRdIndivs = this.getTotalNumIndivsRankedInArchive();

              // difference matrix that stores difference between attributes of input individual
              // and human ranked individuals
              double [][] diffMatrix = new double [totalHmRdIndivs][igaNiQuantObjs.length];
              for (int i = 0; i< totalHmRdIndivs; i++){

                  // obtain quantitative objectives from humanRankedPopulationArchive [i]
                  double [] hmRdQuantObjs = new double [numObjectives - getnumQualObjs()];
                 // if( humanRankedPopulationArchive [i].getRankedIndivFlag() == true) {
                      // obtain numerical objectives from humanRankedPopulationArchive [i]
                  if (humanRankedPopulationArchive [i] instanceof MONumericIndividual) {
                        j = 0;
                        for (int k = 0 ; k < numObjectives ; k++){
                          if (igaQualObj [k] == false) {
                              hmRdQuantObjs [j] = ((MONumericIndividual)humanRankedPopulationArchive [i]).getObjective (k);
                              j++ ;
                          }
                      }
                  } else {
                        j = 0;
                        for (int k = 0 ; k < numObjectives ; k++){
                          if (igaQualObj [k] == false) {
                              hmRdQuantObjs [j] = ((MOBinaryIndividual)humanRankedPopulationArchive [i]).getObjective (k);
                              j++ ;
                          }
                      }
                  }

                  // Difference betweens genes attributes for igaNi and every human ranked individuals
                  for (int k = 0; k < igaNiQuantObjs.length; k++){
                    diffMatrix [i][j+k] = Math.abs(igaNiQuantObjs[k] - hmRdQuantObjs [k]);
                  }
              } // for loop "i < totalHmRdIndivs"

              // Scaling diffMatrix
              for (int i = 0; i < igaNiQuantObjs.length ; i++){
                  double maxDiff = 0;
                  // finding maximum value of attribute difference
                  for ( j = 0; j < totalHmRdIndivs; j++) {
                    if (maxDiff < diffMatrix [j][i]) {
                      maxDiff = diffMatrix [j][i];
                    }
                  }
                  // linear scaling of attribute difference by maximum value
                  for ( j = 0; j < totalHmRdIndivs; j++) {
                      if (maxDiff != 0)
                        diffMatrix [j][i] = diffMatrix [j][i]/maxDiff;
                  }
              }

              // Calculating euclidean distance using scaled differences of all attributes
              double [] euclDist = new double [totalHmRdIndivs];
              for (int i = 0; i < totalHmRdIndivs; i++) {
                  double sumSquare = 0;
                  for ( j = 0; j < igaNiQuantObjs.length ; j++){
                    sumSquare = sumSquare + Math.pow(diffMatrix[i][j], power);
                  }
                  euclDist [i] = Math.pow(sumSquare, (1.0/power));
              }

              return euclDist;

        }



        /**
         * this returns the number of individuals that should be ranked during every interactive session
         * by the user in one sitting. To prevent human fatigue this should be a small number
         * like 20.
         */
        public int getNumIndivsForUserRanking () {

            return numIndivsRankedByUser;

        }

        /**
         * this sets the number of individuals that should be ranked during every interactive session
         * by the user in one sitting. To prevent human fatigue this should be a small number
         * like 20.
         */
        public void setNumIndivsForUserRanking (int num) {

            numIndivsRankedByUser = num;

        }
        
        public void setRestart (boolean rest){
            restart = rest;
        }

        /**
         * this returns the number of ranking sessions that a user is expecting to go through
         * To prevent human fatigue this should be a small number
         */
        public int getNumExpectedRankingSessions () {

            return numExpectedRankingSessions;

        }

        /**
         * this sets the number of ranking sessions that a user is expecting to go through.
         * To prevent human fatigue this should be a small number.
         */
        public void setNumExpectedRankingSessions (int num) {

            numExpectedRankingSessions = num;

        }

        /////////////////////////////////////////////////////////////////////////////

	/**
		Given only the number of members, the number of alleles per member
		and the size of the window.
		@param numMembers the number of individuals in the population.
		@param numGenes number of genes in the pool.
                @param randomSeed seed for the random number generator.	
         */
        //CHANGE SIZE OF ARCHIVE
        
	public IGANsgaPopulation (Range [] ranges, ObjectiveConstraints [] objConstraints,
			int numMembers, double targ, long randomSeed) {
                
                            
                
                
		super (ranges);
		this.target = targ;
		this.objectiveConstraints = objConstraints;
                //for (int i = 0; i<5; i++) this.numRank[i]=0;
                rankIds = new int[5][maxGen*numIndivsRankedByUser];

		// Find the best and worst objective values.
		this.worst = objConstraints[0].getMin ();
		this.best = objConstraints [0].getMax ();
		for (int i = 1 ; i < objConstraints.length ; i++) {
			if (objConstraints [i].getMax () > best)
				this.best = objConstraints [i].getMax ();
			if (objConstraints [i].getMin () < worst)
				this.worst = objConstraints [i].getMin ();
		}

		//. Create a population of the appropriate type.
		if (ranges [0] instanceof BinaryRange) {

			// Set up the members
			members = new MOBinaryIndividual [numMembers];
			nextMembers = new MOBinaryIndividual [numMembers];
                        humanRankedPopulationArchive =new MOBinaryIndividual [(5*maxGen)*numIndivsRankedByUser];

			for (int i = 0 ; i < numMembers ; i++) {
				members[i] = new MOBinaryIndividual ((BinaryRange []) ranges,
						objConstraints, (long) (randomSeed + (long)i));
				nextMembers[i] = new MOBinaryIndividual ((BinaryRange []) ranges,
						objConstraints, (long) (randomSeed + (long)i));
                        }
                        for (int i = 0 ; i < (5*maxGen)*numIndivsRankedByUser ; i++) {
                                humanRankedPopulationArchive[i] = new MOBinaryIndividual ((BinaryRange []) ranges,
						objConstraints, (long) (randomSeed + (long)i));
                        }

		} else if (ranges [0] instanceof DoubleRange) {
			// Set up the members
			members = new MONumericIndividual [numMembers];
			nextMembers = new MONumericIndividual [numMembers];
                        humanRankedPopulationArchive = new MONumericIndividual [(5*maxGen)*numIndivsRankedByUser];

			for (int i = 0 ; i < numMembers ; i++) {
				members[i] = new MONumericIndividual ((DoubleRange []) ranges, objConstraints, (long) (randomSeed + (long)i));
				nextMembers[i] = new MONumericIndividual ((DoubleRange []) ranges, objConstraints, (long) (randomSeed + (long)i));
			}
                        for (int i = 0 ; i < (5*maxGen)*numIndivsRankedByUser ; i++) {
                                humanRankedPopulationArchive[i] = new MONumericIndividual ((DoubleRange []) ranges, objConstraints, (long)(randomSeed+(long)i));
                        }

		} else if (ranges [0] instanceof IntRange) {

			/*// Set up the members
			members = new IntIndividual [numMembers];
			nextMembers = new IntIndividual [numMembers];
			for (int i = 0 ; i < numMembers ; i++) {
				members[i] = new IntIndividual (traits);
				nextMembers[i] = new IntIndividual (traits);
			}*/
		} else {
			System.out.println ("What kind of range is this?");
		}

		// Create an array that will contain the pointers to the combined population.
		int i = 0;
		combinedPopulation = new IGANsgaSolution [numMembers*2];
		for (; i < numMembers ; i++)
			combinedPopulation [i] = (IGANsgaSolution) members [i];
		for (int j = 0 ; j < numMembers ; i++, j++)
			combinedPopulation [i] = (IGANsgaSolution) nextMembers [j];

		// Set up the members
		objectiveConstraints = objConstraints;
		numObjectives = objectiveConstraints.length;

		this.maxFitnesses = new double [numObjectives];
		this.maxFitnessMembers = new int [numObjectives];
		this.minFitnesses = new double [numObjectives];
		this.minFitnessMembers = new int [numObjectives];
		this.numObjectives = numObjectives;

                /////////////////////////////////
                // Added by Meghna Babbar for IGA
                igaQualObj = new boolean[numObjectives];
                // Set default initial values to be false
                for (i = 0 ; i < numObjectives ; i++)
                    igaQualObj[i] = false;
                /////////////////////////////////
	}

	/********
	*	A constructor that only initializes the objectiveConstraints info.
	*   To be used by a subclass that needs to use its own class of
	*   Individuals
	*	@param ranges The traits
	*	@param objConstraints The objective constraints array
	*	@param targ
	**/
	public IGANsgaPopulation(Range[] ranges,
		ObjectiveConstraints[] objConstraints, double targ){
                
		super(ranges);
                for (int i = 0; i<5; i++) this.numRank[i]=0;
		this.target = targ;
		this.objectiveConstraints = objConstraints;

		// Find the best and worst objective values.
		this.worst = objConstraints[0].getMin ();
		this.best = objConstraints [0].getMax ();
		for (int i = 1 ; i < objConstraints.length ; i++) {
			if (objConstraints [i].getMax () > best)
				this.best = objConstraints [i].getMax ();
			if (objConstraints [i].getMin () < worst)
				this.worst = objConstraints [i].getMin ();
		}
		this.numObjectives = objectiveConstraints.length;

		this.maxFitnesses = new double [numObjectives];
		this.maxFitnessMembers = new int [numObjectives];
		this.minFitnesses = new double [numObjectives];
		this.minFitnessMembers = new int [numObjectives];
		this.numObjectives = numObjectives;

                /////////////////////////////////
                // Added by Meghna Babbar for IGA
                igaQualObj = new boolean[numObjectives];
                // Set default initial values to be false
                for (int i = 0 ; i < numObjectives ; i++)
                    igaQualObj[i] = false;
	}

	/**
	 * returns the number of objective values generated.
	 * @returns the number of objective values generated.
	 */
	public int getNumObjectives () {
		return numObjectives;
	}

        /**
	 * Compares two individuals returns a value that indicates which individual
	 * dominates the other.
	 * @param first the first member to compare
	 * @param second the other member.
	 * @returns 1 if first dominates second, -1 if second dominates first,
	 *  0 if neither dominates.
	 */
	protected int dominates (IGANsgaSolution first, IGANsgaSolution second) {
		int numObj = this.numObjectives;
                int returnrank = 0;
		boolean firstBetter = false, secondBetter = false;



		// Set the flag if one member has one objective value better than
		// the other.
		for (int i = 0 ; i < numObj; i++) {
		    int compare = objectiveConstraints [i].compare (
				first.getObjective (i), second.getObjective (i));
			if (compare > 0)
				firstBetter = true;
			else if (compare < 0)
				secondBetter = true;
		}

		// Now figure out which is better if either is.
		if (firstBetter == true)
			if (secondBetter == false)  returnrank = 1;
			else                        returnrank = 0;
		else
			if (secondBetter == true)   returnrank = -1;
			else                        returnrank =  0;
                
                
                return returnrank;
	}

	/**
	 * compute statistics that can be used to measure the success of
	 * the population.
	 */
	public void computeStatistics () {

		IGANsgaSolution [] individuals = (IGANsgaSolution []) members;
		int length = this.size ();
		this.findBestFitnesses (individuals);

		// These values used in selection process.
		this.currentMeasure = 0.0;
		for (int i = 0 ; i < numObjectives ; i++)
			this.currentMeasure += maxFitnesses [i];
		this.currentMeasure /= numObjectives;
	}

	/**
	 * This method does the non dominated sort of the individuals in the population.
	 */

	public void doNonDominatedSort () {
		final boolean debug = false;

		// We need space
		IGANsgaSolution [] theGuys;
		if ( (currentGeneration > 0)&&(!restart) )
			theGuys = (IGANsgaSolution []) combinedPopulation;
		else
			theGuys = (IGANsgaSolution []) members;
		int popSize = theGuys.length;
		fronts = new ParetoFront (popSize);

		// for member (i) the number of other members that dominate it.
		int [] domCount = new int [popSize];

		// A list of the individuals dominated by i.
		int [][] indDomByMe = new int [popSize][popSize];

		// count of individuals dominated by member (i).
		int [] numIndDom = new int [popSize];

		// Initialize the domination count, and number dominated counts to zero
		for(int i = 0; i < popSize; i++)
			domCount[i] = numIndDom[i] = 0;

		// Find the best of each objective values.
		this.findBestFitnesses (theGuys);    // find best fitnesses

		// Find the first front, for each member see if anybody dominates
		// and if not, add it to the first front.
		fronts.addFront ();
		for(int i = 0; i < popSize-1; i++) {
			for (int j = i+1; j < popSize; j++) {
				int idomj = this.dominates (theGuys [i], theGuys [j]);
				if (idomj == 1) {

					// i dominates j
					indDomByMe [i][numIndDom [i]] = j;
					numIndDom [i] += 1;
					domCount [j] += 1;
				} else if(idomj == -1) {

					// j dominates i
					indDomByMe[j][numIndDom[j]] = i;
					numIndDom[j] += 1;
					domCount[i] += 1;
				}
			}
                        
			if (domCount [i] == 0) {
				fronts.addMemberToFront (0, i);
				theGuys[i].setRank (0);
			}
		}

		// See if the last one is dominate, if so, add it.
		if (domCount [popSize-1] == 0) {
			fronts.addMemberToFront (0, popSize-1);
			theGuys[popSize-1].setRank (0);
		}

		// Debugging
		//if (debug) {
			for (int i = 0; i < popSize; i++) {
				System.out.print (" domCnt = "+domCount[i]+", Individual at "+i+", ");
				System.out.println (theGuys [i].getObjective(0)+", "+theGuys [i].getObjective(1)+", " + theGuys [i].getObjective(2));
				for (int j = 0 ; j < numIndDom [i] ; j++)
					System.out.println("   "+indDomByMe[i][j]+", " + theGuys[indDomByMe[i][j]].getObjective(0) +", "+ theGuys[indDomByMe[i][j]].getObjective(1)+", "+ theGuys[indDomByMe[i][j]].getObjective(2));
			}
			System.out.println();
		//}

		// Now we have not only the first front, but a count of how many individuals
		// each member is dominated by. It's all just a counting game from here.
		int fid = 0;

		// As long as we continue to add fronts.
		boolean frontAdded = true;
		while (frontAdded) {
			frontAdded = false;
			int [] currentFront = fronts.getFront (fid);
			int frontSize = fronts.getFrontSize (fid);

			// Debugging
			if (debug) {
				System.out.println("------------------");
				System.out.print ("Front at "+fid+" -> ");
				for (int i = 0; i < frontSize; i++)
					System.out.print (currentFront[i]+",");
				System.out.println();
			}

			// For each member in the current front,
			for(int i = 0; i < frontSize; i++) {

				// reduce the dominated count for members it dominates...
				int current = currentFront [i];
				int [] membersDominated = indDomByMe [current];

				// Debugging
				if (debug) {
					System.out.print(current+ " dominates ");
					for (int tootoo = 0; tootoo < numIndDom [current]; tootoo++)
						System.out.print (membersDominated[tootoo]+",");
					System.out.println();
				}

				for(int j = 0; j < numIndDom [current]; j++) {
					domCount [membersDominated [j]]--;

					// and if the count for any of these other members goes to zero,
					// add that member to the next front.
					if(domCount [membersDominated [j]] == 0) {
						if (frontAdded == false) {
							fronts.addFront ();
							frontAdded = true;
						}
						fronts.addMemberToFront (fid+1, membersDominated [j]);
						theGuys [membersDominated [j]].setRank (fid+1);

						// Debugging
						 if (debug)
							System.out.println("Added "+membersDominated [j]+" to "+(fid+1));

					}
				}
			}

			// on the the next front.
			fid += 1;
		}

		// compress the front arrays
		fronts.compress ();
                
	}

	/**
	 * returns a reference to the pareto front manager guy.
	 * @returns a reference to the pareto front manager guy.
	 */
	public ParetoFront getParetoFronts () {
		return fronts;
	}


	/**
	 * returns a reference to the pareto front manager guy.
	 * @returns a reference to the pareto front manager guy.
	 */
	public IGANsgaSolution [] getCombinedPopulation () {
		return (IGANsgaSolution []) combinedPopulation;
	}
	/**
		This is the recursive quicksort procedure.
		@param members the members array to sort.
		@param l the left starting point.
		@param r the right end point.
		@param order the objects to compare.
		@param obj the index of the objective value to sort on.
	*/
	protected void quickSortObj(IGANsgaSolution [] members, int l, int r,
				int [] order, int obj) {

		// This is the (poorly chosen) pivot value.
                
		double pivot = members [order [(r + l) / 2]].getObjective (obj);

		// from position i=l+1 start moving to the right, from j=r-2 start moving
		// to the left, and swap when the fitness of i is more than the pivot
		// and j's fitness is less than the pivot
		int i = l;
		int j = r;

		while (i <= j) {
			while ((i < r) && (members [order [i]].getObjective (obj) > pivot))
				i++;
			while ((j > l) && (members [order [j]].getObjective (obj) < pivot))
				j--;
			if (i <= j) {
				int swap = order [i];
				order [i] = order [j];
				order [j] = swap;
				i++;
				j--;
			}
		}

		// sort the two halves
		if (l < j)
			quickSortObj (members, l, j, order, obj);
		if (i < r)
			quickSortObj (members, i, r, order, obj);
	}
        
/**
		This is the recursive quicksort procedure.
		@param members the members array to sort.
		@param l the left starting point.
		@param r the right end point.
		@param order the objects to compare.
		@param obj the index of the objective value to sort on.
	*/
	protected void quickSortGenes(IGANsgaSolution [] members, int l, int r,
				int [] order, int gene) {

		// This is the (poorly chosen) pivot value.
                //temp1 = (double[]) ((Individual) theGuys[sortListByObj [i]]).getGenes ();
                   double pivot;
                   int numericCheck;
                if ( members[order[(r+l)/2]] instanceof MONumericIndividual){
                   pivot = ((double[]) ((Individual) members[order [(r + l) / 2]]).getGenes())[gene];
                   numericCheck = 1;
                }else{
                   pivot = ( ((MOBinaryIndividual) members[order [(r + l) / 2]]).toDoubleValues())[gene];
                   numericCheck = 0;
                }
                    
                    
                    //                normGene1 = ((double[]) ((Individual) theGuys[indId1]) .getGenes ())[j];
                                       // theGuys[indId1].getObjective(j);
                      //              normGene2 = ((double[]) ((Individual) theGuys[indId2]) .getGenes ())[j];
                    			//double normFit2 = theGuys[indId2].getObjective(j);
                        //            midGene = ((double[]) ((Individual) theGuys[midInd]) .getGenes ())[j];
     
                          //       }else{
                            //         normGene1 = (((MOBinaryIndividual) theGuys[indId1]).toDoubleValues())[j];
                              //       normGene2 = (((MOBinaryIndividual) theGuys[indId2]).toDoubleValues())[j];
                                //     midGene = (((MOBinaryIndividual) theGuys[midInd]).toDoubleValues())[j];
                                 //}
                                    
                
		//double pivot = temp[gene];

		// from position i=l+1 start moving to the right, from j=r-2 start moving
		// to the left, and swap when the fitness of i is more than the pivot
		// and j's fitness is less than the pivot
		int i = l;
		int j = r;
                
                if(numericCheck == 1) {

		while (i <= j) {
                        
			while ((i < r) && ( ((double[]) ((Individual) members [order [i]]).getGenes())[gene] > pivot))
				i++;
			while ((j > l) && ( ((double[]) ((Individual) members [order [j]]).getGenes())[gene] < pivot))
				j--;
			if (i <= j) {
				int swap = order [i];
				order [i] = order [j];
				order [j] = swap;
				i++;
				j--;
			}
		}
                } else {
                 while (i <= j) {
                        
			while ((i < r) && ( ( ((MOBinaryIndividual) members [order [i]]).toDoubleValues())[gene] > pivot))
				i++;
			while ((j > l) && ( ( ((MOBinaryIndividual) members [order [j]]).toDoubleValues())[gene] < pivot))
				j--;
			if (i <= j) {
				int swap = order [i];
				order [i] = order [j];
				order [j] = swap;
				i++;
				j--;
			}
		}   
                }

		// sort the two halves
		if (l < j)
			quickSortGenes (members, l, j, order, gene);
		if (i < r)
			quickSortGenes (members, i, r, order, gene);
	}
        
	/**
	 * Computes the crowding distance of each individual.
	 * Details:
	 * For every front i
	 *   For every objective j
	 *     Sort the individuals in front i in ascending order of objective j
	 *     Set the crowding distance of the first and the last individual to infinity
	 *     For every individual k in the front i except the first and the last individuals
	 *       Normalize the fitness j by dividing the fitness j of individual k-1
	 *  and individual k+1 by maximum jth fitness.
	 *       Add absolute value of (Normalized jth fitness of individual k+1
	 *- Normalized jth fitness of individual k-1) to the crowding distance of kth individual.
	 */
	public void computeCrowdingDistance (int [] sortListByObj, int frontSize) {
	IGANsgaSolution [] theGuys = (IGANsgaSolution []) combinedPopulation;

		// Clear all the crowding distances for the individuals in this front.
		for (int i = 0 ; i < frontSize ; i++)
			theGuys [sortListByObj [i]].setCrowdingDistance(0.0);

		// Update the crowding distance for each objective.
		for (int j = 0; j < this.numObjectives; j++) {

			// Sort the front on objective value j
			quickSortObj (theGuys, 0, frontSize-1, sortListByObj, j);

			// First and last crowding distance is infinity.
			int firstInd = sortListByObj [0];
			int lastInd = sortListByObj [frontSize-1];
                        //Added by Abhishek Singh 3/5/06  so that if there is only one objective value
                        //in the entire front then the solutions are not given infinite distance
                        if(firstInd == lastInd){
                        theGuys [firstInd].setCrowdingDistance (Double.POSITIVE_INFINITY);			
                        }
                        if(theGuys[firstInd].getObjective(j)!= theGuys[lastInd].getObjective(j)){
			theGuys [firstInd].setCrowdingDistance (Double.POSITIVE_INFINITY);
			theGuys [lastInd].setCrowdingDistance (Double.POSITIVE_INFINITY);
                        }

			// add the crowding distance for each member based on the
			// current objective.
			for(int k = 1; k < frontSize-1; k++) {
				int indId = sortListByObj[k];
				int indId1 = sortListByObj[k+1];
				int indId2 = sortListByObj[k-1];

				double normFit1 = theGuys[indId1].getObjective(j);
				double normFit2 = theGuys[indId2].getObjective(j);

				// LAM-tlr  normalize because objectives can have different scales
                                
				double tt = normFit2 - normFit1;
                                if(theGuys[lastInd].getObjective(j)!=0){
                                tt = tt/(theGuys[lastInd].getObjective(j));
                                }
                                tt = Math.abs(tt);
				theGuys[indId].setCrowdingDistance (
					theGuys [indId].getCrowdingDistance () + tt);
			}
		}
	}
	/**
	 * Computes the genotypic crowding distance of each individual.
	 * Details:
	 * For every front i
	 *   For every parameter j
	 *     Sort the individuals in front i in ascending order of objective j
	 *     Set the crowding distance of the first and the last individual to infinity
	 *     For every individual k in the front i except the first and the last individuals
	 *       Normalize the fitness j by dividing the fitness j of individual k-1
	 *  and individual k+1 by maximum jth fitness.
	 *       Add absolute value of (Normalized jth fitness of individual k+1
	 *- Normalized jth fitness of individual k-1) to the crowding distance of kth individual.
	 */
	public void computeCrowdingDistanceGeneAndObj (int [] sortList, int frontSize) {
	IGANsgaSolution [] theGuys = (IGANsgaSolution []) combinedPopulation;

		// Clear all the crowding distances for the individuals in this front.
		for (int i = 0 ; i < frontSize ; i++)
			theGuys [sortList [i]].setCrowdingDistance(0.0);
                        int numpar = this.traits.length;

		// add the crowding distance for each gene.
                for (int j = 0; j < numpar; j++){
                    
                    // Sort the front on gene value j
                    quickSortGenes( theGuys, 0, frontSize-1, sortList, j);
  		    int firstInd = sortList [0];
		    int lastInd = sortList [frontSize-1];
                    int midInd = sortList [(frontSize-1)/2];
                    
                    for(int k =0; k<frontSize;k++ ){
                        
				int indId = sortList[k];
                                
                                int indId1;
                                int indId2;
                                
                                if(frontSize > 1) {
                                if(k == 0){
                                    System.out.println("frontsize "+frontSize+" k "+k);
                                    indId2 = sortList[k];
                                    indId1 = sortList[k+1];
                                }else{
                                    if(k == frontSize-1){
                                    indId2 = sortList[k-1];
                                    indId1 = sortList[k];    
                                    }else{
                                    indId2 = sortList[k-1];
                                    indId1 = sortList[k+1];    
                                    }
                                }
                                } else {
                                    indId2 = sortList[k];
                                    indId1 = sortList[k];
                                }
                                    
                                double normGene1;
                                double normGene2;
                                double midGene;
                                                      
                                 if (theGuys[indId1] instanceof MONumericIndividual){
                                    normGene1 = ((double[]) ((Individual) theGuys[indId1]) .getGenes ())[j];
                                       // theGuys[indId1].getObjective(j);
                                    normGene2 = ((double[]) ((Individual) theGuys[indId2]) .getGenes ())[j];
                    			//double normFit2 = theGuys[indId2].getObjective(j);
                                    midGene = ((double[]) ((Individual) theGuys[midInd]) .getGenes ())[j];
     
                                 }else{
                                     normGene1 = (((MOBinaryIndividual) theGuys[indId1]).toDoubleValues())[j];
                                     normGene2 = (((MOBinaryIndividual) theGuys[indId2]).toDoubleValues())[j];
                                     midGene = (((MOBinaryIndividual) theGuys[midInd]).toDoubleValues())[j];
                                 }
                                     
				
				// LAM-tlr  normalize because objectives can have different scales
                                //divide by the median value
				double tt = normGene2 - normGene1;
                                if(midGene !=0){
                                //if(theGuys[lastInd].getObjective(j)!=0){
                                tt = tt/midGene;
                                }
                                tt = Math.abs(tt);
                                tt = tt + theGuys [indId].getCrowdingDistance();
				theGuys[indId].setCrowdingDistance (tt);
                        
                    }
                }
               
		for (int j = 0; j < this.numObjectives; j++) {

			// Sort the front on objective value j
			quickSortObj (theGuys, 0, frontSize-1, sortList, j);

			// First and last crowding distance is infinity.
			int firstInd = sortList [0];
			int lastInd = sortList [frontSize-1];
                        int midInd = sortList [(frontSize-1)/2];
                        
                        if(firstInd == lastInd){
                        theGuys [firstInd].setCrowdingDistance (Double.POSITIVE_INFINITY);
			
                        }
                        if(theGuys[firstInd].getObjective(j)!= theGuys[lastInd].getObjective(j)){
			theGuys [firstInd].setCrowdingDistance (Double.POSITIVE_INFINITY);
			theGuys [lastInd].setCrowdingDistance (Double.POSITIVE_INFINITY);
                        }

			// add the crowding distance for each member based on the
			// current objective.
			for(int k = 1; k < frontSize-1; k++) {
				int indId = sortList[k];
				int indId1 = sortList[k+1];
				int indId2 = sortList[k-1];

				double normFit1 = theGuys[indId1].getObjective(j);
				double normFit2 = theGuys[indId2].getObjective(j);

				// LAM-tlr  normalize because objectives can have different scales
				double tt = normFit2 - normFit1;
                                if(theGuys[midInd].getObjective(j)!=0){
                                tt = tt/(theGuys[midInd].getObjective(j));
                                }
                                tt = Math.abs(tt);
                                tt = tt + theGuys [indId].getCrowdingDistance();
				theGuys[indId].setCrowdingDistance (tt);
					//theGuys [indId].getCrowdingDistance () + tt);
			}
		}
                        //for(int k = 1; k < frontSize-1; k++) {
                        //    double tt = theGuys[sortList[k]].getCrowdingDistance();
                        //}
	}

	/**
	 * find the best objective value for each objective.
	 */
	private void findBestFitnesses (IGANsgaSolution [] members) {

		// set fitnesses to zero.
		for (int i = 0 ; i < numObjectives ; i++) {
			maxFitnesses [i] = members [0].getObjective (i);
			minFitnesses [i] = members [0].getObjective (i);
			maxFitnessMembers [i] = 0;
			minFitnessMembers [i] = 0;
		}

		// for each objective,
		for (int i = 0 ; i < numObjectives ; i++) {

			// find the best in the population.
			int best = 0;
			int worst = 0;
			for (int j = 1 ; j < members.length; j++) {
				double obj = members [j].getObjective (i);
				if (objectiveConstraints[i].compare (obj, maxFitnesses [i]) > 0) {
					maxFitnesses [i] = obj;
					maxFitnessMembers [i] = j;
				}
				if (objectiveConstraints[i].compare (obj, minFitnesses [i]) < 0){
					minFitnesses [i] = obj;
					minFitnessMembers [i] = j;
				}
			}
		}
	}

	/**
	 * find the best objective value for each objective.
	 */
	public double [] getBestFitnesses () {
		return maxFitnesses;
	}

	/**
		Compares one individual to another on the basis of constraints and non-dominance
		rank first, and if those are equal, based on crowding distance.
		@returns 1 if member indexed a is greater than b,
				0 if they are equal,
				-1 if member indexed by a is less than b.
	*/
	final public int compareMembers (Solution a, Solution b) {
		IGANsgaSolution first = (IGANsgaSolution) a;
		IGANsgaSolution second = (IGANsgaSolution) b;

                
                //check for constraints
                //Abhishek Singh 11/29/05 change to quick implement constraints
                //change this later
                int firstconstr = 0;
                int secondconstr = 0;
                
                if(first.getObjective(1)<=1) firstconstr =firstconstr + 1;
                if(first.getObjective(2)<= 4) firstconstr =firstconstr + 1;
                if(second.getObjective(1)<=1) secondconstr =secondconstr + 1;
                if(second.getObjective(2)<= 4) secondconstr =secondconstr + 1;
                
                if (firstconstr > secondconstr) return 1;
                if (firstconstr < secondconstr) return -1;

                //if both either meet constraints or not meet constraints
		int af = first.getRank ();
		int bf = second.getRank ();
                
		if(af < bf)
			return 1;
		if (af > bf)
			return -1;

		// equal ranks
		double aCrowd = first.getCrowdingDistance ();
		double bCrowd = second.getCrowdingDistance ();
		if (aCrowd > bCrowd)
			return 1;
		if (aCrowd < bCrowd)
			return -1;
		return 0;
	}

	/**
	 * Construct a string representing the current status of the population, best members,
	 * maybe worst members, whatever.
	 */
	public String statusString () {
		StringBuffer sb = new StringBuffer (1024);
		sb.append ('\n');
		sb.append ("Best Fitnesses at generation ");
		sb.append (currentGeneration);
		sb.append ('\n');
                this.computeStatistics();

		// We found the best members, now set the max fitness values from each
		for (int i = 0 ; i < numObjectives ; i++) {
			sb.append ("    ");
			sb.append (i);
			sb.append (") ");
			sb.append (members [maxFitnessMembers [i]]);
			sb.append ('\n');
		}

		// Now list all top ranked individuals.
		int [] order = new int [combinedPopulation.length];
		for (int i = 0 ; i < order.length ; i++) order [i] = i;
		this.sortIndividuals (combinedPopulation, order);

		sb.append ("Top ranked individuals.\n");
		IGANsgaSolution [] tmp = combinedPopulation;
		for (int i = 0 ; i < combinedPopulation.length; i++) {

			// Only look at members that are a part of the diverse population.
			sb.append ("member "+i+") ");
			sb.append (tmp[order [i]]);
			sb.append ('\n');
		}
		return sb.toString ();
	}

	/**
	 * Reformate the parent population by putting all members in the combined
	 * parent child population in their appropriate place. In this way, we can
	 * reorder the population without reallocating any Individual objects.
	 * <p>
	 * When this method is done, the order of the individuals in the populations
	 * should still be random.
	 * @param arry the indices of the indivuals who will live.
	 */
	static final private boolean LIVE = true;
	static final private boolean DIE = false;

	public void recompilePopulation (int [] live) {
		int combinedSize = combinedPopulation.length;
		boolean [] liveOrDie = new boolean [combinedSize];

		// Init the live or die array.
		for (int i = 0 ; i < combinedSize ; i++)
			liveOrDie[i] = DIE;
		for (int i = 0  ; i < live.length ; i++)
			liveOrDie [live [i]] = LIVE;

		// traverse the entire combined population, moving all individuals
		// that live into the members array, and all that die into the
		// nextMembers array.
		int posLive = 0;
		int posDie = 0;
		for (int i = 0; i < combinedSize; i++)
			if (liveOrDie [i] == LIVE)
				members [posLive++] = (Individual) combinedPopulation [i];
			else
				nextMembers [posDie++] = (Individual) combinedPopulation [i];
                
	}

        
	/**
	 * Returns a representation of of the population in the form of a
	 * table, where each row represents one individual, one gene per column, and the last
	 * column containing the objective value.
	 * @returns a table represeting the population.
	 */
  public Table getTable() {
    int numGenes = 0;
    int numTraits;
    IGANsgaSolution nis = (IGANsgaSolution) members[0];
    numTraits = this.traits.length;

    int popSize = this.size();
    double[][] dc = new double[numTraits + numObjectives + 2][popSize];

    for (int i = 0; i < popSize; i++) {
      IGANsgaSolution ni = (IGANsgaSolution) members[i];
      double[] genes = ni.toDoubleValues();
      int j = 0;

      // first do the genes.
      for (; j < numTraits; j++) {
        dc[j][i] = genes[j];

        // Now the objectives.
      }
      for (int k = 0; k < numObjectives; k++, j++) {
        dc[j][i] = ni.getObjective(k);
      }
      dc[j++][i] = ni.getRank();
      dc[j++][i] = ni.getCrowdingDistance();

    }
    // Now make the table
    //BASIC3 TableImpl vt = (TableImpl) DefaultTableFactory.getInstance().createTable(0);
    MutableTableImpl vt =  new MutableTableImpl(0);
    int i = 0;

    for (; i < numTraits; i++) {
      DoubleColumn col = new DoubleColumn(dc[i]);
      // NsgaSolution nis0 = (NsgaSolution) members[0];
      //if (nis instanceof MONumericIndividual) {
        col.setLabel(this.traits[i].getName());
      /*}
      else {
        col.setLabel("Variable " + i);
      }*/
      vt.addColumn(col);
    }

    for (int k = 0; k < numObjectives; k++, i++) {
      DoubleColumn col = new DoubleColumn(dc[i]);
      col.setLabel(this.objectiveConstraints[k].getName());
      vt.addColumn(col);
    }
    DoubleColumn col = new DoubleColumn(dc[i++]);
    col.setLabel("Rank");
    vt.addColumn(col);
    col = new DoubleColumn(dc[i++]);
    col.setLabel("Crowding");
    vt.addColumn(col);
    return vt;
  }
    
  
        /**
         * This method adds a IGANsgaSolution that has been actually ranked by a human
         * to the archive data structure "humanRankedPopulationArchive".
         */
        
        //CHANGE THIS BACCK TO MEGHNA'S MODULE
        public void addHumanRankedIndivToArchive (Individual nis){

                //if (numArchivedSolutions < humanRankedPopulationArchive.length) {
                Individual nis2 = nis;
                
//                  humanRankedPopulationArchive[numArchivedSolutions].copy(nis);
                  int temp;
                  if(nis instanceof MONumericIndividual){
                      temp = ( (int) Math.ceil(((MONumericIndividual)nis).getObjective(2)) );
                      ((MONumericIndividual)nis2).setObjective(2,temp);
                  }
                  else{
                      temp = ( (int) Math.ceil(((MOBinaryIndividual)nis).getObjective(2)) );
                      ((MOBinaryIndividual)nis2).setObjective(2,temp);
                  }
                  
                    // hard copy of individual into the archive
                    humanRankedPopulationArchive[totalNumIndivsRankedByUser].copy(nis2);

                  
                      //rankIds[temp-1][numRank[temp-1]] = numArchivedSolutions;
                      rankIds[temp-1][numRank[temp-1]] = totalNumIndivsRankedByUser;
                      numRank[temp-1]=numRank[temp-1]+1;
                      
             
                          
                  
                  
                  totalNumIndivsRankedByUser ++;
                  int maxlen = numIndivsRankedByUser*numExpectedRankingSessions;
                  if(totalNumIndivsRankedByUser <= maxlen){
                      numArchivedSolutions ++;
                  }
                  //System.out.println("maxlen");
                  //System.out.println(maxlen);
                  //System.out.println("Number in Archive");
                  //System.out.println(numArchivedSolutions);
               //if (totalNumIndivsRankedByUser < numIndivsRankedByUser*numExpectedRankingSessions) {        
               //}
               //else {
                  //start replacing old individuals in the archive
                 // int maxlen = numIndivsRankedByUser*numExpectedRankingSessions;
                 // int index = (totalNumIndivsRankedByUser/maxlen);
                 // index = totalNumIndivsRankedByUser - index*maxlen;
                 // humanRankedPopulationArchive[index].copy(nis);  
                 // totalNumIndivsRankedByUser ++;
                 // System.err.println("Maximum Capacity of Human Ranked Individual Archive Reached !!");
                //}
        }

      private int [] getSelectedIndivs(){
      int [] selected = new int[this.getTotalNumIndivsRankedInArchive()];
       
       //calculate how many of each rank we need to select
       int maxSel = numIndivsRankedByUser*numExpectedRankingSessions;
       
       int id = 0;
       if(totalNumIndivsRankedByUser <= maxSel ){
       for(int i = 0; i<totalNumIndivsRankedByUser; i++){
           selected[i]=i;
           
       }
           
       }else{
           
       for(int i = totalNumIndivsRankedByUser-1 ; id<maxSel; i--){
           selected[id]=i;
           id++;
       }
       
           
           
       }

       
       
       return selected;
      
      }
        
       private int [] getSelectedIndivs1(){
       int [] selected = new int[this.getTotalNumIndivsRankedInArchive()];
       
       //calculate how many of each rank we need to select
       int maxSel = numIndivsRankedByUser*numExpectedRankingSessions;
       int id = 0;
       if(totalNumIndivsRankedByUser <= maxSel ){
       for(int i = 0; i<totalNumIndivsRankedByUser; i++){
           selected[i]=i;
           id++;
       }
           
       }else{
           
       
       int [] numSelRank = new int[5];
       

       int prevMin = 0;
       int totSel = 0;
       int [] numToSelectFromRank = {0,0,0,0,0};
      
       
       while(totSel < maxSel){
           
       int minRank = 0;
       int minNum = 100000;
       int maxRank = 0;
       int maxNum = 0;
       
       int[] sortedListRanks = {0, 1, 2, 3, 4};
       
      //descending sort list based on num remaining in ranks 
       //since this is only with 5 elements bubble sort is used
       for(int i = 0; i < 5; i++){
           for (int j = 0; j < 4; j++){
               if( (numRank[sortedListRanks[j]]-prevMin) < (numRank[sortedListRanks[j+1]]-prevMin) ){
                   //swap j and j+1
                   int temp = sortedListRanks[j];
                   sortedListRanks[j] = sortedListRanks[j+1];
                   sortedListRanks[j+1] = temp;
               }
           }
       }
       
       int lenList = 0;
       //how many ranks have non-zero size
       for(int i = 0; i < 5; i++){
           if(numRank[sortedListRanks[i]]-prevMin > 0 ) {
               minRank = sortedListRanks[i];
               lenList++;
           }
           
       }
       
       maxRank = sortedListRanks[0];
       minNum = numRank[minRank] - prevMin;
       maxNum = numRank[maxRank] - prevMin;
       
       if(maxNum > 0){
           
           //step through each rank in order of elements
           //and select at most minNum elements
           
           for(int i = 1; i <= minNum; i++){
            for(int r = 0; (r < lenList)&&(totSel<maxSel); r++){
                int r2 = sortedListRanks[r];
                numToSelectFromRank[r2]++;
                totSel++;
            }                              
           }                                                       
           
       }
       
       prevMin = minNum;
       
    }
       
       
       if(totSel == maxSel){
           //we are done
           for(int r = 0; r < 5; r++){
               if(numToSelectFromRank[r]> 0){
               int j = 0;
               for(int i = numRank[r]-1; (i >= 0)&&(j<numToSelectFromRank[r]); i--){
                   System.out.println("rank "+ (r+1)+" selected "+rankIds[r][i]);                   
                   selected[id] = rankIds[r][i];
                   id++;
                   j++;
               }
               
               }else{
               System.out.println("rank "+ (r+1) +" selected 0");                   
               }
           }
           
       }else{
           System.out.println("THERE IS A PROBLEM WITH SELECTION");                   
       }
       
        
       }
       
       //while(id<numExpectedRankingSessions*numIndivsRankedByUser){
       //    selected[id]=-1;
       //    id++;
       //}       
       
       return selected;
       }
          

        
       private int [] getSelectedIndivs2(){
       int [] selected = new int[this.getTotalNumIndivsRankedInArchive()];
       
       //calculate how many of each rank we need to select
       int maxSel = numIndivsRankedByUser*numExpectedRankingSessions;
       int id = 0;
       if(totalNumIndivsRankedByUser <= maxSel ){
       for(int i = 0; i<totalNumIndivsRankedByUser; i++){
           selected[i]=i;
           id++;
       }
           
       }else{
           
       
       int [] numSelRank = new int[5];
       
       //select at least one from each rank
       for(int r = 0; r<5; r++){
           if(numRank[r]>0){
               System.out.println("Number "+ id+" selected "+rankIds[r][numRank[r]-1]);
               selected[id] = rankIds[r][numRank[r]-1];    
           id++;
           }    
       }
       
       //how many more do we need to select
       int total = 0;
       int maxNum = 0;
       int max = 0;
       for(int r = 0;r<5;r++){
           double num = ( ((double)(numRank[r]-1))*((double)(maxSel-id)))/((double)(totalNumIndivsRankedByUser-id));
          // double num = ( ((double)(numRank[r]-1))*((double)(maxSel-id)))/((double)(this.getTotalNumIndivsRankedInArchive()-id));
           if(num>0){
           num = Math.round(num);
           //floor(num);
           numSelRank[r] = (int)num;
           total = total + numSelRank[r];
           if(numRank[r]-numSelRank[r] > maxNum) {
               maxNum = numRank[r]-numSelRank[r];
               max = r;
           }
           }
       }
       
       if(total<maxSel-id){
           numSelRank[max]++;
       }
       
       for (int r = 0; r<5; r++){
           for (int i = 2; i <= numSelRank[r]+1; i++){
               int toChoose = numRank[r]-i;
               if((toChoose >= 0)&&(id<this.getTotalNumIndivsRankedInArchive())){
                   System.out.println("Number "+ id+" selected "+rankIds[r][toChoose]);
               selected[id] = rankIds[r][toChoose];
               id++;
               }
           }
       }
       
              

       
         
       }
       
       //while(id<numExpectedRankingSessions*numIndivsRankedByUser){
       //    selected[id]=-1;
       //    id++;
       //}       
       
       return selected;
       }
  
  /**
	 * Returns a representation of of the human ranked archive in the form of a
	 * table, where each row represents one individual, one gene per column, and the last
	 * column containing the objective value.
	 * @returns a table represeting the population.
         * Added by Abhishek Singh 6/15/04
	 */
        
               public ExampleTable getHumanArchiveTable () {

               int numGenes = 0;
               int numTraits;
               int i,j,k,l;
               int [] sel = new int[this.getTotalNumIndivsRankedInArchive()];
               sel = this.getSelectedIndivs();
               
               IGANsgaSolution nis = (IGANsgaSolution) members [0];
               if (nis instanceof MONumericIndividual) {
                       numTraits = this.traits.length;
               }
               else{
                       for(i=0; i<this.traits.length; i++)
                         numGenes += ((BinaryRange) this.traits[i]).getNumBits();
                       numTraits = numGenes;
               }
               //System.out.println("numtraits :" + numTraits);
               int totalHmRdIndivs = this.getTotalNumIndivsRankedInArchive();
               //change totalHmRdIndivs to max expected
               double [][] dc = new double [numTraits+numObjectives][totalHmRdIndivs];
               //double [][] qualArr = new double [getnumQualObjs()][totalHmRdIndivs];
               int [] inputCol = new int [numTraits+numObjectives-getnumQualObjs()];
               int [] outputCol = new int [getnumQualObjs()];
               int [] trainingSet = new int [totalHmRdIndivs];
               
               //calculate how many of rank1, rank2, rank3, rank4, and rank5 indivs need to be selected
               //fill table with each rank sequentially
              

               for (i = 0 ; i < totalHmRdIndivs; i++) {
                   int temp = sel[i];
                   trainingSet[i]=i;
                  double [] hmRdGenes;
                  double [] hmRdQuantObjs = new double [numObjectives - getnumQualObjs()];
                  double [] hmRdQualObjs = new double [getnumQualObjs()];
                  if (humanRankedPopulationArchive [sel[i]] instanceof MONumericIndividual) {
                      hmRdGenes = (double []) ((Individual) humanRankedPopulationArchive [sel[i]]).getGenes ();
                      // obtain numerical objectives from humanRankedPopulationArchive [i]
                      j = 0;
                      l = 0;
                      for (k = 0 ; k < numObjectives ; k++){
                          if (igaQualObj [k] == false) {
                              hmRdQuantObjs [j] = ((MONumericIndividual)humanRankedPopulationArchive [sel[i]]).getObjective (k);
                              j++ ;
                          }
                          else{
                              hmRdQualObjs [l] = ((MONumericIndividual)humanRankedPopulationArchive [sel[i]]).getObjective (k);
                              l++;
                          }
                      }
                  }
                  else{
                      hmRdGenes = (double []) ((MOBinaryIndividual) humanRankedPopulationArchive [sel[i]]).toDoubleValues();
                      // obtain numerical objectives from humanRankedPopulationArchive [i]
                      j = 0;
                      l=0;
                      for (k = 0 ; k < numObjectives ; k++){
                          if (igaQualObj [k] == false) {
                              hmRdQuantObjs [j] = ((MOBinaryIndividual)humanRankedPopulationArchive [sel[i]]).getObjective (k);
                              j++ ;
                          }
                          else{
                              hmRdQualObjs [l] = ((MOBinaryIndividual)humanRankedPopulationArchive [sel[i]]).getObjective (k);
                              l++;
                          }
                      }
                  }

                       j = 0;


                       // first do the genes.
                       for (; j < numTraits ; j++) {
                               dc [j][i] = hmRdGenes [j];
                       inputCol[j]=j;
                       }
                       

                       // Now the objectives.
                       for ( k = 0 ; k < numObjectives-getnumQualObjs() ; k++, j++){
                               dc [j][i] = hmRdQuantObjs [k];
                       inputCol[j]=j;
                       }
                       for (l = 0; l < getnumQualObjs(); l++,j++){
                           dc [j][i] = hmRdQualObjs [l];
                       outputCol[l]=j;
                       }

               }
              // Now make the table
               ExampleTableImpl vt = new ExampleTableImpl (0);
               //(ExampleTableImpl)DefaultTableFactory.getInstance().createTable(0);
               i = 0;

               for (; i < numTraits ; i++) {
                       DoubleColumn col = new DoubleColumn (dc [i]);
                       // NsgaSolution nis0 = (NsgaSolution) members[0];
                       if(nis instanceof MONumericIndividual){
                          col.setLabel (this.traits [i].getName ());
                       }
                       else{
                          col.setLabel ("Variable "+i);
                       }
                       vt.addColumn (col);
               }

               for (k = 0 ; k < numObjectives ; k++, i++) {
                       DoubleColumn col = new DoubleColumn (dc [i]);
                       col.setLabel (this.objectiveConstraints [k].getName ());
                       //set the qual objective as nominal
                       if(k > numObjectives - getnumQualObjs()){
                           boolean value = true;
                           col.setIsNominal(value);
                       }
                       vt.addColumn (col);
               }
               vt.setInputFeatures(inputCol);
               vt.setOutputFeatures(outputCol);
               vt.setColumnIsNominal(true, outputCol[0]);
               vt.setTrainingSet(trainingSet);
               
               return vt;
       /** testing*/
	}
               
        public void setGeneration (int gen) {
            this.currentGeneration = gen;            
        }
        

        
        
      /**  public boolean isDone(){

                // Did we just run out of time?
                if(this.getCurrentGeneration() >= this.getMaxGenerations()) {
                        this.setMaxGenerations(this.getCurrentGeneration()+this.getMaxGenerations()+1);
                        System.out.println ("Max gens.");
                        return true;
                }
                return false;
                
        }*/               
  
	/**
	 * returns the best possible fitness.
	 * @returns the best possible fitness.
	 */
	public double getBestFitness () { return this.best; }
	public double getWorstFitness () { return this.worst; }
	public double getTargetFitness () { return this.target; }
	public double getCurrentMeasure () { return this.currentMeasure; }


}

