// https://searchcode.com/api/result/69643465/

package ncsa.d2k.modules.core.optimize.ga.selection;


/**
        This will select individuals on the basis of an unranked evaluation function, namely
        the stochastic universal selection without replacement also known as expected value model,
        aka Stochastic Sampling without Replacement.<p>

        In effect it works like this. Imagine that each of the individuals of the current population
        is assigned a start and end point on a line where the length betwee the start
        and end point is proportional to their fitness relative to the total fitness of the
        population. Then assign N equidistant points along the line such that the points are evenly
        distributed all the way along the line. The only variable is where the first point is placed,
        this is determined by a role of the dice. For each of the N points, it is determined which
        individual resides there on the line, and that individual is selected.
*/
public class StochasticUniversalSampling extends SelectionModule
         {
           protected void createSelection() {
             selection = new StochasticUniversalSamplingObj();
           }

        //////////////////////////////////
        // Info methods
        //////////////////////////////////
        /**
                This method returns the description of the various inputs.

                @return the description of the indexed input.
        */
/*	public String getOutputInfo (int index) {
                switch (index) {
                        case 0: return "      the population after selection   ";
                        default: return "No such output";
                }
        }*/

        /**
                This method returns the description of the various inputs.
                @return the description of the indexed input.
        */
/*	public String getInputInfo (int index) {
                switch (index) {
                        case 0: return "      the input population   ";
                        default: return "No such input";
                }
        }*/

        /**
                This method returns the description of the module.
                @return the description of the module.
        */
        public String getModuleInfo () {
                return "<html>  <head>      </head>  <body>    This will select individuals on the basis of an unranked evaluation     function, namely the stochastic universal Sampling (J. E. Baker, &quot;Reducing     bias and inefficiency in the selection algorithm&quot;, 1987). Imagine that     each of the individuals of the current population is assigned a start and     end point on a line where the length betwee the start and end point is     perportional to their fitness relative to the total fitness of the     population. Then assign N equidistant points along the line such that the     points are evenly distributed all the way along the line. The only     variable is where the first point is placed, this is determined by a role     of the dice. For each of the N points, it is determined which individual     resides there on the line, and that individual is selected. The advantage     of this mechanism is that it will reduce the chance fluctuations between     the expected number of individuals to be selected and the actual number of     copies actually allocated to the new population when fliping a coin.  </body></html>";
        }

        //////////////////////////////////
        // Type definitions.
        //////////////////////////////////

/*	public String[] getInputTypes () {
                String[] types = {"ncsa.d2k.modules.core.optimize.ga.Population"};
                return types;
        }*/

/*	public String[] getOutputTypes () {
                String[] types = {"ncsa.d2k.modules.core.optimize.ga.Population"};
                return types;
        }*/

        /**
                This will select individuals on the basis of an unranked evaluation function, namely
                the stochastic universal selection without replacement also known as expected value model.
                @param population is the population of individuals.
        */
/*	protected void compute (Population population) {
                int popSize = population.size();
                double worst = population.getWorstFitness ();

                // normalizer for proportional selection probabilities
                double avg = ((SOPopulation)population).getAverageFitness ();
                boolean mf = worst < avg;
                double factor =  mf ?
                                        1.0 / (avg - worst) :
                                        1.0 / (worst - avg);

                //
                // Now the stochastic universal sampling algorithm by James E. Baker!
                //
                int k = 0; 						// index of the next selected sample.
                double ptr = Math.random ();	// role the dice.
                double sum = 0;					// control for selection loop.
                double expected;
                int i = 0;

                for (; i < popSize; i++) {

                        // Get the member to test.
                        SOSolution member = (SOSolution) population.getMember (i);
                        if (mf)
                                expected = (member.getObjective () - worst) * factor;
                        else
                                expected = (worst - member.getObjective ()) * factor;

                        // the magnitude of expected will determine the number of
                        // progeny of the individual to survive.
                        for (sum += expected; sum > ptr; ptr++) {
                                this.sample[k++] = i;
                        }
                }
        }*/

        /**
         * Return the human readable name of the module.
         * @return the human readable name of the module.
         */
        public String getModuleName() {
                return "Expected Value";
        }

        /**
         * Return the human readable name of the indexed input.
         * @param index the index of the input.
         * @return the human readable name of the indexed input.
         */
/*	public String getInputName(int index) {
                switch(index) {
                        case 0:
                                return "population";
                        default: return "NO SUCH INPUT!";
                }
        }*/

        /**
         * Return the human readable name of the indexed output.
         * @param index the index of the output.
         * @return the human readable name of the indexed output.
         */
/*	public String getOutputName(int index) {
                switch(index) {
                        case 0:
                                return "population";
                        default: return "NO SUCH OUTPUT!";
                }
        }*/
}

