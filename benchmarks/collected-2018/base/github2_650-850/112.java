// https://searchcode.com/api/result/69643021/

package ncsa.d2k.modules.core.optimize.ga.nsga;

import java.io.Serializable;
import ncsa.d2k.modules.core.optimize.ga.*;
import ncsa.d2k.modules.core.datatype.table.*;
import ncsa.d2k.modules.core.datatype.table.basic.*;
import ncsa.d2k.modules.core.datatype.table.transformations.*;
import ncsa.d2k.modules.core.optimize.util.*;
import ncsa.d2k.modules.core.optimize.ga.emo.*;

/**
     This is a population object which may serve as a baseclass for all population
     designed to solve multi-objective problem. This implementation is based of the
     work of Deb, Agrawal, Pratap and Meyarivan titled "A fast Elitist Non-Dominated
        Sorting Genetic Algorithm for Multi-Objective Optimization: NSGA-II".
        <p>
     The <code>defineTraits</code> method from <code>NumericPopulation</code> must
     be implemented. Also, the objective traits must be defined by overriding the
        <code>defineObjectiveConstraints </code> method. The <code>ObjectiveConstraint</code>s
        defined in this methodwill provide a context for
     the evaluation of the objectives so that they may be minimumized or maximized.
        <p>
        The <code>evaluteIndividual</code> method will also have to be implemented to evalute all the
     objective values of the members at each generation. Notice there is no one best
        fitness anymore. This class will keep track of the best fitnesses of each objective
        value. This class overrides the evaluateAll method to accumulate the best objective
        values after the members have all been evalutated.
 */
abstract public class NsgaPopulation
    extends Population
    implements Serializable {

  /** number of objectives to optimize on.*/
  protected int numObjectives;

  /** the combined population of parents and progeny. */
  protected NsgaSolution[] combinedPopulation = null;

  /** the Pareto fronts. */
  public ParetoFront fronts;

  /** the maxfitness values. */
  protected double[] maxFitnesses = null;

  /** the minimum fitness values. */
  protected double[] minFitnesses = null;

  /** indices of the individuals possessing the max fitnesses. */
  protected int[] maxFitnessMembers = null;

  /** indices of the individuals possessing the max fitnesses. */
  protected int[] minFitnessMembers = null;

  /** constrains the range and polarity of the obectives, maximizing or minimizing,
   *  and handles the comparison of values of that trait.
   */
  protected ObjectiveConstraints[] objectiveConstraints = null;
  public ObjectiveConstraints[] getObjectiveConstraints() {
    return objectiveConstraints;
  }

  /** this is the target fitness we strive for. */
  protected double target;

      /** this is some measure of the current fitness, averaged best in this case. */
  protected double currentMeasure;

  /** this is the best possible fitness. */
  protected double best;

  /** the worst possible. */
  protected double worst;

  /**
          Given only the number of members, the number of alleles per member
          and the size of the window.
          @param numMembers the number of individuals in the population.
          @param numGenes number of genes in the pool.
   */
  public NsgaPopulation(Range[] ranges, ObjectiveConstraints[] objConstraints,
                        int numMembers, double targ) {
    super(ranges);
    this.target = targ;
    this.objectiveConstraints = objConstraints;

    // Find the best and worst objective values.
    this.worst = objConstraints[0].getMin();
    this.best = objConstraints[0].getMax();
    for (int i = 1; i < objConstraints.length; i++) {
      if (objConstraints[i].getMax() > best) {
        this.best = objConstraints[i].getMax();
      }
      if (objConstraints[i].getMin() < worst) {
        this.worst = objConstraints[i].getMin();
      }
    }

    //. Create a population of the appropriate type.
    if (ranges[0] instanceof BinaryRange) {

      // Set up the members
      members = new MOBinaryIndividual[numMembers];
      nextMembers = new MOBinaryIndividual[numMembers];
      for (int i = 0; i < numMembers; i++) {
        members[i] = new MOBinaryIndividual( (BinaryRange[]) ranges,
                                            objConstraints);
        nextMembers[i] = new MOBinaryIndividual( (BinaryRange[]) ranges,
                                                objConstraints);
      }

    }
    else if (ranges[0] instanceof DoubleRange) {
      // Set up the members
      members = new MONumericIndividual[numMembers];
      nextMembers = new MONumericIndividual[numMembers];
      for (int i = 0; i < numMembers; i++) {
        members[i] = new MONumericIndividual( (DoubleRange[]) ranges,
                                             objConstraints);
        nextMembers[i] = new MONumericIndividual( (DoubleRange[]) ranges,
                                                 objConstraints);
      }
    }
    else if (ranges[0] instanceof IntRange) {

      /*// Set up the members
                              members = new IntIndividual [numMembers];
                              nextMembers = new IntIndividual [numMembers];
                              for (int i = 0 ; i < numMembers ; i++) {
             members[i] = new IntIndividual (traits);
             nextMembers[i] = new IntIndividual (traits);
                              }*/
    }
    else {
      System.out.println("What kind of range is this?");
    }

    // Create an array that will contain the pointers to the combined population.
    int i = 0;
    combinedPopulation = new NsgaSolution[numMembers * 2];
    for (; i < numMembers; i++) {
      combinedPopulation[i] = (NsgaSolution) members[i];
    }
    for (int j = 0; j < numMembers; i++, j++) {
      combinedPopulation[i] = (NsgaSolution) nextMembers[j];

      // Set up the members
    }
    objectiveConstraints = objConstraints;
    numObjectives = objectiveConstraints.length;

    this.maxFitnesses = new double[numObjectives];
    this.maxFitnessMembers = new int[numObjectives];
    this.minFitnesses = new double[numObjectives];
    this.minFitnessMembers = new int[numObjectives];
    this.numObjectives = numObjectives;
  }

  /********
   *   To be used by a subclass that needs to use its own class of
   *   Individuals
   **/
  public NsgaPopulation(Range[] ranges,
                        ObjectiveConstraints[] objConstraints, double targ) {
    super(ranges);
    this.target = targ;
    this.objectiveConstraints = objConstraints;

    // Find the best and worst objective values.
    this.worst = objConstraints[0].getMin();
    this.best = objConstraints[0].getMax();
    for (int i = 1; i < objConstraints.length; i++) {
      if (objConstraints[i].getMax() > best) {
        this.best = objConstraints[i].getMax();
      }
      if (objConstraints[i].getMin() < worst) {
        this.worst = objConstraints[i].getMin();
      }
    }
    this.numObjectives = objectiveConstraints.length;

    this.maxFitnesses = new double[numObjectives];
    this.maxFitnessMembers = new int[numObjectives];
    this.minFitnesses = new double[numObjectives];
    this.minFitnessMembers = new int[numObjectives];
    this.numObjectives = numObjectives;
  }

  /**
   * Compares two individuals returns a value that indicates which individual
   * dominates the other.
   * @param first the first member to compare
   * @param second the other member.
   * @returns 1 if first dominates second, -1 if second dominates first,
   *  0 if neither dominates.
   */
  abstract protected int dominates(NsgaSolution first, NsgaSolution second);

  /**
   * returns the number of objective values generated.
   * @returns the number of objective values generated.
   */
  public int getNumObjectives() {
    return numObjectives;
  }

  /**
   * compute statistics that can be used to measure the success of
   * the population.
   */
  public void computeStatistics() {

    NsgaSolution[] individuals = (NsgaSolution[]) members;
    int length = this.size();
    this.findBestFitnesses(individuals);

    // These values used in selection process.
    this.currentMeasure = 0.0;
    for (int i = 0; i < numObjectives; i++) {
      this.currentMeasure += maxFitnesses[i];
    }
    this.currentMeasure /= numObjectives;
  }

  /**
       * This method does the non dominated sort of the individuals in the population.
   */

  public void doNonDominatedSort() {
    final boolean debug = false;

    // We need space
    NsgaSolution[] theGuys;
    if (currentGeneration > 0) {
      theGuys = (NsgaSolution[]) combinedPopulation;
    }
    else {
      theGuys = (NsgaSolution[]) members;
    }
    int popSize = theGuys.length;
    fronts = new ParetoFront(popSize);

    // for member (i) the number of other members that dominate it.
    int[] domCount = new int[popSize];

    // A list of the individuals dominated by i.
    int[][] indDomByMe = new int[popSize][popSize];

    // count of individuals dominated by member (i).
    int[] numIndDom = new int[popSize];

    // Initialize the domination count, and number dominated counts to zero
    for (int i = 0; i < popSize; i++) {
      domCount[i] = numIndDom[i] = 0;

      // Find the best of each objective values.
    }
    this.findBestFitnesses(theGuys); // find best fitnesses

    // Find the first front, for each member see if anybody dominates
    // and if not, add it to the first front.
    fronts.addFront();
    for (int i = 0; i < popSize - 1; i++) {
      for (int j = i + 1; j < popSize; j++) {
        int idomj = this.dominates(theGuys[i], theGuys[j]);
        if (idomj == 1) {

          // i dominates j
          indDomByMe[i][numIndDom[i]] = j;
          numIndDom[i] += 1;
          domCount[j] += 1;
        }
        else if (idomj == -1) {

          // j dominates i
          indDomByMe[j][numIndDom[j]] = i;
          numIndDom[j] += 1;
          domCount[i] += 1;
        }
      }
      if (domCount[i] == 0) {
        fronts.addMemberToFront(0, i);
        theGuys[i].setRank(0);
      }
    }

    // See if the last one is dominate, if so, add it.
    if (domCount[popSize - 1] == 0) {
      fronts.addMemberToFront(0, popSize - 1);
      theGuys[popSize - 1].setRank(0);
    }

    // Debugging
    if (debug) {
      for (int i = 0; i < popSize; i++) {
        System.out.print("Individual at " + i + "-");
        System.out.println(theGuys[i] + " domCnt = " + domCount[i]);
        for (int j = 0; j < numIndDom[i]; j++) {
          System.out.println("   " + indDomByMe[i][j] + "-" +
                             theGuys[indDomByMe[i][j]]);
        }
      }
      System.out.println();
    }

    // Now we have not only the first front, but a count of how many individuals
    // each member is dominated by. It's all just a counting game from here.
    int fid = 0;

    // As long as we continue to add fronts.
    boolean frontAdded = true;
    while (frontAdded) {
      frontAdded = false;
      int[] currentFront = fronts.getFront(fid);
      int frontSize = fronts.getFrontSize(fid);

      // Debugging
      if (debug) {
        System.out.println("------------------");
        System.out.print("Front at " + fid + " -> ");
        for (int i = 0; i < frontSize; i++) {
          System.out.print(currentFront[i] + ",");
        }
        System.out.println();
      }

      // For each member in the current front,
      for (int i = 0; i < frontSize; i++) {

        // reduce the dominated count for members it dominates...
        int current = currentFront[i];
        int[] membersDominated = indDomByMe[current];

        // Debugging
        if (debug) {
          System.out.print(current + " dominates ");
          for (int tootoo = 0; tootoo < numIndDom[current]; tootoo++) {
            System.out.print(membersDominated[tootoo] + ",");
          }
          System.out.println();
        }

        for (int j = 0; j < numIndDom[current]; j++) {
          domCount[membersDominated[j]]--;

          // and if the count for any of these other members goes to zero,
          // add that member to the next front.
          if (domCount[membersDominated[j]] == 0) {
            if (frontAdded == false) {
              fronts.addFront();
              frontAdded = true;
            }
            fronts.addMemberToFront(fid + 1, membersDominated[j]);
            theGuys[membersDominated[j]].setRank(fid + 1);

            // Debugging
            if (debug) {
              System.out.println("Added " + membersDominated[j] + " to " +
                                 (fid + 1));

            }
          }
        }
      }

      // on the the next front.
      fid += 1;
    }

    // compress the front arrays
    fronts.compress();
  }

  /**
   * returns a reference to the pareto front manager guy.
   * @returns a reference to the pareto front manager guy.
   */
  public ParetoFront getParetoFronts() {
    return fronts;
  }

  /**
   * returns a reference to the pareto front manager guy.
   * @returns a reference to the pareto front manager guy.
   */
  public NsgaSolution[] getCombinedPopulation() {
    return (NsgaSolution[]) combinedPopulation;
  }

  /**
          This is the recursive quicksort procedure.
          @param members the members array to sort.
          @param l the left starting point.
          @param r the right end point.
          @param order the objects to compare.
          @param obj the index of the objective value to sort on.
   */
  protected void quickSortObj(NsgaSolution[] members, int l, int r,
                              int[] order, int obj) {

    // This is the (poorly chosen) pivot value.
    double pivot = members[order[ (r + l) / 2]].getObjective(obj);

    // from position i=l+1 start moving to the right, from j=r-2 start moving
    // to the left, and swap when the fitness of i is more than the pivot
    // and j's fitness is less than the pivot
    int i = l;
    int j = r;

    while (i <= j) {
      while ( (i < r) && (members[order[i]].getObjective(obj) > pivot)) {
        i++;
      }
      while ( (j > l) && (members[order[j]].getObjective(obj) < pivot)) {
        j--;
      }
      if (i <= j) {
        int swap = order[i];
        order[i] = order[j];
        order[j] = swap;
        i++;
        j--;
      }
    }

    // sort the two halves
    if (l < j) {
      quickSortObj(members, l, j, order, obj);
    }
    if (i < r) {
      quickSortObj(members, i, r, order, obj);
    }
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
  public void computeCrowdingDistance(int[] sortListByObj, int frontSize) {
    NsgaSolution[] theGuys = (NsgaSolution[]) combinedPopulation;

    // Clear all the crowding distances for the individuals in this front.
    for (int i = 0; i < frontSize; i++) {
      theGuys[sortListByObj[i]].setCrowdingDistance(0.0);

      // Update the crowding distance for each objective.
    }
    for (int j = 0; j < this.numObjectives; j++) {

      // Sort the front on objective value j
      quickSortObj(theGuys, 0, frontSize - 1, sortListByObj, j);

      // First and last crowding distance is infinity.
      int firstInd = sortListByObj[0];
      int lastInd = sortListByObj[frontSize - 1];
      theGuys[firstInd].setCrowdingDistance(Double.POSITIVE_INFINITY);
      theGuys[lastInd].setCrowdingDistance(Double.POSITIVE_INFINITY);

      // add the crowding distance for each member based on the
      // current objective.
      for (int k = 1; k < frontSize - 1; k++) {
        int indId = sortListByObj[k];
        int indId1 = sortListByObj[k + 1];
        int indId2 = sortListByObj[k - 1];

        double normFit1 = theGuys[indId1].getObjective(j);
        double normFit2 = theGuys[indId2].getObjective(j);

        // LAM-tlr  Why normalize?
        double tt = normFit2 - normFit1;
        theGuys[indId].setCrowdingDistance(
            theGuys[indId].getCrowdingDistance() + tt);
      }
    }
  }

  /**
   * find the best objective value for each objective.
   */
  private void findBestFitnesses(NsgaSolution[] members) {

    // set fitnesses to zero.
    for (int i = 0; i < numObjectives; i++) {
      maxFitnesses[i] = members[0].getObjective(i);
      minFitnesses[i] = members[0].getObjective(i);
      maxFitnessMembers[i] = 0;
      minFitnessMembers[i] = 0;
    }

    // for each objective,
    for (int i = 0; i < numObjectives; i++) {

      // find the best in the population.
      int best = 0;
      int worst = 0;
      for (int j = 1; j < members.length; j++) {
        double obj = members[j].getObjective(i);
        if (objectiveConstraints[i].compare(obj, maxFitnesses[i]) > 0) {
          maxFitnesses[i] = obj;
          maxFitnessMembers[i] = j;
        }
        if (objectiveConstraints[i].compare(obj, minFitnesses[i]) < 0) {
          minFitnesses[i] = obj;
          minFitnessMembers[i] = j;
        }
      }
    }
  }

  /**
   * find the best objective value for each objective.
   */
  public double[] getBestFitnesses() {
    return maxFitnesses;
  }

  /**
          Compares one individual to another on the basis of it's non-dominance
          rank first, and if those are equal, based on crowding distance.
          @returns 1 if member indexed a is greater than b,
                          0 if they are equal,
                          -1 if member indexed by a is less than b.
   */
  final public int compareMembers(Solution a, Solution b) {
    NsgaSolution first = (NsgaSolution) a;
    NsgaSolution second = (NsgaSolution) b;
    int af = first.getRank();
    int bf = second.getRank();
    if (af < bf) {
      return 1;
    }
    if (af > bf) {
      return -1;
    }

    // equal ranks
    double aCrowd = first.getCrowdingDistance();
    double bCrowd = second.getCrowdingDistance();
    if (aCrowd > bCrowd) {
      return 1;
    }
    if (aCrowd < bCrowd) {
      return -1;
    }
    return 0;
  }

  /**
   * Construct a string representing the current status of the population, best members,
   * maybe worst members, whatever.
   */
  public String statusString() {
    StringBuffer sb = new StringBuffer(1024);
    sb.append('\n');
    sb.append("Best Fitnesses at generation ");
    sb.append(currentGeneration);
    sb.append('\n');
    this.computeStatistics();

    // We found the best members, now set the max fitness values from each
    for (int i = 0; i < numObjectives; i++) {
      sb.append("    ");
      sb.append(i);
      sb.append(") ");
      sb.append(members[maxFitnessMembers[i]]);
      sb.append('\n');
    }

    // Now list all top ranked individuals.
    int[] order = new int[combinedPopulation.length];
    for (int i = 0; i < order.length; i++) {
      order[i] = i;
    }
    this.sortIndividuals(combinedPopulation, order);

    sb.append("Top ranked individuals.\n");
    NsgaSolution[] tmp = combinedPopulation;
    for (int i = 0; i < combinedPopulation.length; i++) {

      // Only look at members that are a part of the diverse population.
      sb.append("member " + i + ") ");
      sb.append(tmp[order[i]]);
      sb.append('\n');
    }
    return sb.toString();
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

  public void recompilePopulation(int[] live) {
    int combinedSize = combinedPopulation.length;
    boolean[] liveOrDie = new boolean[combinedSize];

    // Init the live or die array.
    for (int i = 0; i < combinedSize; i++) {
      liveOrDie[i] = DIE;
    }
    for (int i = 0; i < live.length; i++) {
      liveOrDie[live[i]] = LIVE;

      // traverse the entire combined population, moving all individuals
      // that live into the members array, and all that die into the
      // nextMembers array.
    }
    int posLive = 0;
    int posDie = 0;
    for (int i = 0; i < combinedSize; i++) {
      if (liveOrDie[i] == LIVE) {
        members[posLive++] = (Individual) combinedPopulation[i];
      }
      else {
        nextMembers[posDie++] = (Individual) combinedPopulation[i];
      }
    }
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
    NsgaSolution nis = (NsgaSolution) members[0];
    numTraits = this.traits.length;

    int popSize = this.size();
    double[][] dc = new double[numTraits + numObjectives + 2][popSize];

    for (int i = 0; i < popSize; i++) {
      NsgaSolution ni = (NsgaSolution) members[i];
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
   * returns the best possible fitness.
   * @returns the best possible fitness.
   */
  public double getBestFitness() {
    return this.best;
  }

  public double getWorstFitness() {
    return this.worst;
  }

  public double getTargetFitness() {
    return this.target;
  }

  public double getCurrentMeasure() {
    return this.currentMeasure;
  }

}

