// https://searchcode.com/api/result/11990030/

/**
 * Alexander Conrad
 * gelations.Prioritizer.java
 */
package gelations;

import java.util.ArrayList;
import java.util.Random;

/**
 * Core class of the evolutionary prioritizer.
 * 
 * @author conrada
 *
 */
public class Prioritizer {
	
    int repetition, generations, popSize, maxStag;
    double targetFitness, maxTime;
    long startTime, runTime, seed;
	
    Random rng;
    Configuration config;
    ArrayList<Chromosome> initChromosomes;
    Population initPopulation, population;
    CrossoverOperator crossoverOperator;
    MutationOperator mutationOperator;
    MetricCalculator fitnessCalculator;
    ParentSelector parentSelector;
    Individual bestIndividual;
    ScalingTransform scalingTransform;
	
    /**
     * Creates a new instance of Prioritizer.
     * 
     * @param _config - Configuration object containing all 
     *  config info and CaseTest data
     */
    public Prioritizer(Configuration _config) {
	this(_config,0);
    }
	
    /**
     * Creates a new instance of Prioritizer.
     * 
     * @param _config - Configuration object containing all 
     *  config info and CaseTest data
     * @param _repetition - current repetition of this config, 
     *  for data recording purposes
     */
    public Prioritizer(Configuration _config, int _repetition) {
		
	repetition = _repetition;
	config = _config;
	seed = config.getSeeds().get(repetition);
	targetFitness = config.getTargetFitness();
	maxTime = config.getMaxTime();
	maxStag = config.getMaxStagnancy();
		
	rng = new Random(seed);
		
	initChromosomes 
	    = CreateInitialChromosomes.makeChromosomes
	    (config.getCaseTests());
		
	//debug
	/*
	  for (int i=0; i<initChromosomes.size(); i++) {
			
	  System.out.println("chromosome: "
	  +initChromosomes.get(i).getId());
			
	  }
	*/
		
	population 
	    = CreateRandomInitialPopulation.makePopulation
	    (initChromosomes, config.getPopSize(), 
	     rng.nextLong());
	
	initPopulation 
	    = CreateRandomInitialPopulation
	    .getInitialPopulation();
	
	//debug
	/*
	  crossoverOperator = InitializeCrossoverOperators
	  .getCrossoverOperator(2, rng.nextLong());
	*/
	crossoverOperator 
	    = InitializeCrossoverOperators
	    .getCrossoverOperator
	    (config.getCrossoverOperator(), rng.nextLong());
		
	//debug
	/*
	  mutationOperator = InitializeMutationOperators
	  .getMutationOperator(5, rng.nextLong());
	*/
		
	mutationOperator 
	    = InitializeMutationOperators
	    .getMutationOperator
	    (config.getMutationOperator(), rng.nextLong());
		
		
	fitnessCalculator = MetricSelector.getFitnessCalculator
	    (config.getMetric());
		
	//debug
	/*
	  scalingTransform = InitializeScalingTransform
	  .getScalingTransform(2, rng.nextLong());
	*/
	scalingTransform = InitializeScalingTransform
	    .getScalingTransform
	    (config.getFitnessTransform(), rng.nextLong());
		
		
	// compute fitness
	fitnessCalculator.computeFitness(population);
		
	// modify fitness for scaling transform
	//	note: this should no longer be performed yet
	//scalingTransform.transformPopulation(population);
		
	//debug
	/*
	  parentSelector = InitializeParentSelector
	  .getParentSelector(1, rng.nextLong());
	*/
		
	parentSelector 
	    = InitializeParentSelector.getParentSelector
	    (config.getSelectionOperator(), rng.nextLong());
		
		
	//debug
	/*
	  for (int i=0; i
	  <population.getIndividuals().size(); i++) {
			
	  System.out.println("individual "+i+": "
	  +population.getIndividuals().get(i)
	  .getStringRepresentation());
			
	  }
	*/
		
	//debug
	//System.out.println(population.getMaxFitness()+" "
	// +population.getBestIndividual().getFitness() +" "
	// +population.getBestIndividual()
	// .getStringRepresentation()+" "+ targetFitness);
		
    }
	
    /**
     * Execute the prioritization program for the configuration 
     * and data currently reflected in this Prioritizer's state.
     * 
     * @return the best Individual as discovered by the 
     *  Prioritizer
     */
    public Individual runPrioritization() {
		
	int stagnancy = 0;
	double bestFitness = 0;
	double tempFitness;
		
	generations = 0;
	startTime = System.currentTimeMillis();
	ArrayList<Individual> parents, children;
	Population lastPopulation, childPopulation;
		
	//debug
	//maxStag = 500;
	/*
	  System.out.println((population.getMaxFitness() 
	  < targetFitness) +" "+
	  ((System.currentTimeMillis()-startTime) < maxTime) 
	  +" "+(stagnancy < maxStag));
	  
	  System.out.println(population.getMaxFitness()+" "
	  +population.getBestIndividual().getFitness() +" "
	  +population.getBestIndividual()
	  .getStringRepresentation()+" "+ targetFitness);
	*/
	
	/*
	while(population.getMaxFitness() < targetFitness && 
	      System.currentTimeMillis()-startTime < maxTime &&
	      stagnancy < maxStag) {
		*/
	// temporary condition: just do 400 generations
	while(generations < 400) {
		
	    generations++;
			
	    // make a copy of the current population
	    lastPopulation = Population.copy(population);
			
	    // modify the fitnesses by scaling
	    lastPopulation = scalingTransform
		.transformPopulation(lastPopulation);
			
	    // select a subpopulation of parents
	    parents = parentSelector.chooseParents
		(lastPopulation, config.getChildRepresentation());
			
	    // create children
	    children = crossoverOperator.createChildren(parents); 
			
	    // mutate children
	    childPopulation = new Population(children);
			
	    //debug
	    /*
	      childPopulation 
	      = mutationOperator
	      .mutatePopulation(childPopulation, 0.2);
	    */
			
	    childPopulation = mutationOperator.mutatePopulation
		(childPopulation, config.getMutationRate());
			
	    // compute fitnesses for children
	    fitnessCalculator.computeFitness(childPopulation);
			
	    // reduce the previous population and merge with 
	    //  children
	    population 
		= Population.randomlyReducePopulation
		(population, config.getPopSize()-childPopulation
		 .getPopSize(), rng);
	    
	    population = Population.randomlyCombinePopulations
		(population, childPopulation, rng);
			
	    tempFitness = population.getMaxFitness();
	    if (tempFitness <= bestFitness) {
				
		stagnancy++;
				
	    } else {
				
		bestIndividual = population.getBestIndividual();
		bestFitness = tempFitness;
		stagnancy = 0;
				
	    }
			
	    //debug
	    //System.out.println("stagnancy: "+stagnancy
	    // +", pop max fitness: "+population.getMaxFitness()
	    // +", bestFitness: "+bestFitness+", bestOrdering: "
	    // +population.getBestIndividual()
	    // .getStringRepresentation());
	    //System.out.println("popsize: "
	    // +population.getIndividuals().size());
	    /*
	      for (int i=0; 
	      i<population.getIndividuals().size(); i++) {
				
	      System.out.println("  current population: "
	      +"individual: "+i+", ordering: "
	      +population.getIndividuals().get(i).
	      getStringRepresentation()+", fitness: "
	      +population.getIndividuals().get(i).getFitness());
				
	      }
	    */
			
	    // write stagnancy info
	    /*
	      WriteResults.toFileStag(config, generations, 
	      System.currentTimeMillis()-startTime, repetition, 
	      stagnancy, tempFitness, bestFitness, 
	      bestIndividual);
	    */
	}
	
	runTime = System.currentTimeMillis()-startTime;
	
	// recompute the best individual's fitness to negate 
	// scaling
	//	(this step shouldn't be necessary anymore, but 
	//       it can't hurt)
	//fitnessCalculator.computeFitness(bestIndividual);
	System.out.println("best fitness: "+bestFitness);
	return bestIndividual;
	
    }
    
    /**
     * Write a line to the relevant datafile to record this 
     * execution of the prioritizer.
     *
     */
    public void writeResults() {
	
	WriteResults.toFile(config, bestIndividual, runTime, 
			    repetition, generations);
	
    }
    
    /**
     * Deletes the datafile specified in config and recreates an 
     * empty version, containing only the header line.
     *
     */
    public void resetDataFile() {
	
	WriteResults.writeHeader(config);
	
    }
    
}

