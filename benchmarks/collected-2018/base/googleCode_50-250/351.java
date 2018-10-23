// https://searchcode.com/api/result/12103406/

package nl.jamiecraane.mover;

import java.math.BigDecimal;
import java.util.*;

import org.apache.log4j.Logger;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.DeltaFitnessEvaluator;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.SwappingMutationOperator;

/**
 * In the mover example all things that should be moved from one location to the
 * other is put in boxes. The boxes vary in size and all have a different
 * volume. The mover also has a couple of vans in which the boxes are stored.
 * All vans have the same volume. Given is a number of boxes
 * with varying sizes. To reduce transport costs, it is crucial for the moving company to use as minimal vans as possible.
 * Given the size of the vans, what is the optimal
 * distribution of the boxes so that a minimal number of vans is needed?
 * 
 * Although one can calculate the total volume of the boxes and divide this with the volume of the vans,
 * this calculation says nothing about the arrangement of the boxes in the vans. The arrangement is
 * what this genetic program solves.
 *
 * The constants can be modified to experiment with different program settings.
 */
public class MoverExample {
	private static final Logger LOG = Logger.getLogger(MoverExample.class);
	// No need to have this much evolutions but we do want the optimal solution at the cost of cpu
	private static final int NUMBER_OF_EVOLUTIONS = 5000;
	// The amount of boxes used to move things from one location to the other. The number of boxes determines the number of genes.
	private static final int NUMBER_OF_BOXES = 20;
	// The volume of the vans in cubic meters
	private static final double VOLUME_OF_VANS = 4.33;
	
	private static final int TOTAL_KILOMETERS = 75;
	private static final BigDecimal COSTS_KILOMETER = new BigDecimal("2.50");

	// The minimum size of the width,height and depth of a box
	private static final double MINIMUM_VOLUME = 0.25;
	// The maximum random size. The maximum size of a box is thus MINIMUM_VOLUME + MAXIMUM_VOLUME
	private static final double MAXIMUM_VOLUME = 2.75;
    // The size of the population (number of chromosomes in the genotype)    
    private static final int SIZE_OF_POPULATION = 50;

    private Box[] boxes;
	private double totalVolumeOfBoxes = 0.0D;

    public MoverExample(int seed) throws Exception {
		this.createBoxes(seed);
		Genotype genotype = this.configureJGAP();
		this.evolve(genotype);
	}

    /**
     * Setup JGAP.
     */
    private Genotype configureJGAP() throws InvalidConfigurationException {
		Configuration gaConf = new DefaultConfiguration();
		// Here we specify a fitness evaluator where lower values means a better fitness
		Configuration.resetProperty(Configuration.PROPERTY_FITEVAL_INST);
		gaConf.setFitnessEvaluator(new DeltaFitnessEvaluator());

		// Only use the swapping operator. Other operations makes no sense here
		// and the size of the chromosome must remain constant
		gaConf.getGeneticOperators().clear();
		SwappingMutationOperator swapper = new SwappingMutationOperator(gaConf);
		gaConf.addGeneticOperator(swapper);

        // We are only interested in the most fittest individual
        gaConf.setPreservFittestIndividual(true);
		gaConf.setKeepPopulationSizeConstant(false);

		gaConf.setPopulationSize(SIZE_OF_POPULATION);
        // The number of chromosomes is the number of boxes we have. Every chromosome represents one box.
        int chromeSize = this.boxes.length;
		Genotype genotype;

		// Setup the structure with which to evolve the solution of the problem.
        // An IntegerGene is used. This gene represents the index of a box in the boxes array.
		IChromosome sampleChromosome = new Chromosome(gaConf, new IntegerGene(gaConf), chromeSize);
		gaConf.setSampleChromosome(sampleChromosome);
        // Setup the fitness function
		MoverFitnessFunction fitnessFunction = new MoverFitnessFunction();
		fitnessFunction.setBoxes(this.boxes);
		fitnessFunction.setVanCapacity(VOLUME_OF_VANS);
		gaConf.setFitnessFunction(fitnessFunction);

		// Because the IntegerGenes are initialized randomly, it is neccesary to set the values to the index. Values range from 0..boxes.length
		genotype = Genotype.randomInitialGenotype(gaConf);
		List chromosomes = genotype.getPopulation().getChromosomes();
        for (Object chromosome : chromosomes) {
            IChromosome chrom = (IChromosome) chromosome;
            for (int j = 0; j < chrom.size(); j++) {
                Gene gene = chrom.getGene(j);
                gene.setAllele(j);
            }
        }

		return genotype;
	}

	/**
	 * Creates the boxes which are needed for the move from one location to the
	 * other.
	 */
	private void createBoxes(int seed) {
		Random r = new Random(seed);
		this.boxes = new Box[NUMBER_OF_BOXES];
		for (int i = 0; i < NUMBER_OF_BOXES; i++) {
			Box box = new Box(MINIMUM_VOLUME + (r.nextDouble() * MAXIMUM_VOLUME));
            box.setId(i);
            this.boxes[i] = box;
        }

        double[] volumes = new double[this.boxes.length];
        for (int i = 0; i < this.boxes.length; i++) {
			LOG.debug("Box [" + i + "]: " + this.boxes[i]);
			this.totalVolumeOfBoxes += this.boxes[i].getVolume();
            volumes[i] = this.boxes[i].getVolume(); 
        }
		LOG.info("The total volume of the [" + NUMBER_OF_BOXES + "] boxes is [" + this.totalVolumeOfBoxes + "] cubic metres.");
    }

	/**
	 * Evolves the population.
	 */
	private void evolve(Genotype a_genotype) {
		int optimalNumberOfVans = (int) Math.ceil(this.totalVolumeOfBoxes / VOLUME_OF_VANS);
		LOG.info("The optimal number of vans needed is [" + optimalNumberOfVans + "]");
		
		double previousFittest = a_genotype.getFittestChromosome().getFitnessValue();
		int numberOfVansNeeded = Integer.MAX_VALUE;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < NUMBER_OF_EVOLUTIONS; i++) {
			if (i % 250 == 0) {
				LOG.info("Number of evolutions [" + i + "]");
			}
			a_genotype.evolve();
			double fittness = a_genotype.getFittestChromosome().getFitnessValue();
			int vansNeeded = this.numberOfVansNeeded(a_genotype.getFittestChromosome().getGenes()).size();
			if (fittness < previousFittest && vansNeeded < numberOfVansNeeded) {
				this.printSolution(a_genotype.getFittestChromosome());
				previousFittest = fittness;
				numberOfVansNeeded = vansNeeded;
			}
			
			// No more optimal solutions
			if (numberOfVansNeeded == optimalNumberOfVans) {
				break;
			}
		}
        long endTime = System.currentTimeMillis();
        System.out.println("computation time = " + (endTime - startTime));
        IChromosome fittest = a_genotype.getFittestChromosome();

        List<Van> vans = numberOfVansNeeded(fittest.getGenes());
        printVans(vans);
        this.printSolution(fittest);
	}

	private void printSolution(IChromosome fittest) {
		// The optimal genes of the most optimal population
		Gene[] genes = fittest.getGenes();
		List<Van> vans = numberOfVansNeeded(genes);

        System.out.println("Fitness value [" + fittest.getFitnessValue() + "]");
		System.out.println("The total number of vans needed is [" + vans.size() + "]");
//		System.out.println("The total costs are [" + (TOTAL_KILOMETERS * COSTS_KILOMETER.doubleValue()) * vans.size() + "]");
	}

    private void printVans(List<Van> vans) {
        int index = 1;
        for (Van van : vans) {
            System.out.println("Van [" + index + "] has contents with a total volume of [" + van.getVolumeOfContents() + "] and contains the following boxes:");
            List<Box> boxes = van.getContents();
            for (Box box : boxes) {
                System.out.println("    " + box);
            }
            index++;
        }
    }

	private List<Van> numberOfVansNeeded(Gene[] genes) {
        List<Van> vans = new ArrayList();
        Van van = new Van(VOLUME_OF_VANS);
        for (Gene gene : genes) {
            int index = (Integer) gene.getAllele();
            if (!van.addBox(this.boxes[index])) {
                // A new van is needed
                vans.add(van);
                van = new Van(VOLUME_OF_VANS);
                van.addBox(this.boxes[index]);
            }
        }
		return vans;
	}

    /**
     * Starts the moving example. A seed can be specified on the command line which generates a particular sequence of boxes. When no seed
     * is specified the seed 37 is used.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
		int seed = 37;
		if (args.length == 1) {
			seed = Integer.parseInt(args[0]);
		}
		new MoverExample(seed);
	}
}

