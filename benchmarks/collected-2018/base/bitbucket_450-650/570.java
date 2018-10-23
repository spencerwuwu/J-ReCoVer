// https://searchcode.com/api/result/39956082/

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Random;
import java.io.*;

/* TODO: 
 * -Define state and reward functions (StateAndReward.java) suitable for your problem 
 * -Define actions
 * -Implement missing parts of Q-learning
 * -Tune state and reward function, and parameters below if the result is not satisfactory */

public class QLearningController extends Controller {

	/* These are the agents senses (inputs) */
	DoubleFeature x; /* Positions */
	DoubleFeature y;
	DoubleFeature vx; /* Velocities */
	DoubleFeature vy;
	DoubleFeature angle; /* Angle */

	/* These are the agents actuators (outputs) */
	RocketEngine leftEngine;
	RocketEngine middleEngine;
	RocketEngine rightEngine;
	
	// strategy pattern
	StateAndReward stateAndReward;
	
	final static int NUM_ACTIONS = 4; /*
									 * The takeAction function must be changed
									 * if this is modified
									 */

	/* Keep track of the previous state and action */
	String previous_state = null;
	double previous_vx = 0;
	double previous_vy = 0;
	double previous_angle = 0;
	int previous_action = 0;

	/* The tables used by Q-learning */
	Hashtable<String, Double> Qtable = new Hashtable<String, Double>(); /*
																		 * Contains
																		 * the
																		 * Q-
																		 * values
																		 * - the
																		 * state
																		 * -
																		 * action
																		 * utilities
																		 */
	Hashtable<String, Integer> Ntable = new Hashtable<String, Integer>(); /*
																		 * Keeps
																		 * track
																		 * of
																		 * how
																		 * many
																		 * times
																		 * each
																		 * state
																		 * -
																		 * action
																		 * combination
																		 * has
																		 * been
																		 * used
																		 */

	/*
	 * PARAMETERS OF THE LEARNING ALGORITHM - THESE MAY BE TUNED BUT THE DEFAULT
	 * VALUES OFTEN WORK REASONABLY WELL
	 */
	static final double GAMMA_DISCOUNT_FACTOR = 0.95; /*
													 * Must be < 1, small values
													 * make it very greedy
													 */
	static final double LEARNING_RATE_CONSTANT = 10; /*
													 * See alpha(), lower values
													 * are good for quick
													 * results in large and
													 * deterministic state
													 * spaces
													 */
	double explore_chance = 0.5; /*
								 * The exploration chance during the exploration
								 * phase
								 */
	final static int REPEAT_ACTION_MAX = 30; /*
											 * Repeat selected action at most
											 * this many times trying reach a
											 * new state, without a max it could
											 * loop forever if the action cannot
											 * lead to a new state
											 */
	private boolean print_log = true;
	
	/* Some internal counters */
	int iteration = 0; /* Keeps track of how many iterations the agent has run */
	int action_counter = 0; /*
							 * Keeps track of how many times we have repeated
							 * the current action
							 */
	int print_counter = 0; /* Makes printouts less spammy */

	/* These are just internal helper variables, you can ignore these */
	boolean paused = false;
	boolean explore = true; /* Will always do exploration by default */

	DecimalFormat df = (DecimalFormat) NumberFormat
			.getNumberInstance(Locale.US);
	public SpringObject object;
	ComposedSpringObject cso;
	long lastPress = 0;

	public void init() {
		cso = (ComposedSpringObject) object;
		x = (DoubleFeature) cso.getObjectById("x");
		y = (DoubleFeature) cso.getObjectById("y");
		vx = (DoubleFeature) cso.getObjectById("vx");
		vy = (DoubleFeature) cso.getObjectById("vy");
		angle = (DoubleFeature) cso.getObjectById("angle");

		previous_vy = vy.getValue();
		previous_vx = vx.getValue();
		previous_angle = angle.getValue();

		leftEngine = (RocketEngine) cso.getObjectById("rocket_engine_left");
		rightEngine = (RocketEngine) cso.getObjectById("rocket_engine_right");
		middleEngine = (RocketEngine) cso.getObjectById("rocket_engine_middle");
		
		this.stateAndReward = new HoverStateAndReward();
	}

	/* Turn off all rockets */
	void resetRockets() {
		leftEngine.setBursting(false);
		rightEngine.setBursting(false);
		middleEngine.setBursting(false);
	}

	/* Performs the chosen action */
	void performAction(int action) {
		this.resetRockets();
		
		/* Fire the rockets! */
		/*
		 * Remember to change NUM_ACTIONS constant to reflect the number
		 * of actions (including 0, no action)
		 */
		// 1-left, 2-middle, 3-right, 4-left & middle, 5-right & middle, 6-left
		// & right
		switch (action) {
		case 0: break;
		case 1:
			leftEngine.setBursting(true);
			break;
		case 2:
			middleEngine.setBursting(true);
			break;
		case 3:
			rightEngine.setBursting(true);
			break;
		case 4:
			leftEngine.setBursting(true);
			middleEngine.setBursting(true);
			break;
		case 5:
			rightEngine.setBursting(true);
			middleEngine.setBursting(true);
			break;
		case 6:
			leftEngine.setBursting(true);
			rightEngine.setBursting(true);
			break;
		}
	}

	/* Main decision loop. Called every iteration by the simulator */
	public void tick(int currentTime) {
		this.iteration++;

		if (!paused) {
			String new_state = this.stateAndReward.getState(angle.getValue(),
					vx.getValue(), vy.getValue());

			/*
			 * Repeat the chosen action for a while, hoping to reach a new
			 * state. This is a trick to speed up learning on this problem.
			 */
			action_counter++;
			if (new_state.equals(previous_state)
					&& action_counter < REPEAT_ACTION_MAX) {
				return;
			}
			double previous_reward = this.stateAndReward.getReward(
					previous_angle, previous_vx, previous_vy);
			action_counter = 0;

			/* The agent is in a new state, do learning and action selection */
			if (previous_state != null) {
				/* Create state-action key */
				String prev_stateaction = previous_state + previous_action;

				/* Increment state-action counter */
				if (Ntable.get(prev_stateaction) == null) {
					Ntable.put(prev_stateaction, 0);
				}
				Ntable.put(prev_stateaction, Ntable.get(prev_stateaction) + 1);

				/* Update Q value */
				if (Qtable.get(prev_stateaction) == null) {
					Qtable.put(prev_stateaction, 0.0);
				}

				// TODO: FIX Q-UPDATE HERE!?
				// In Q-update we won't use model, just actions and rewards
				double Qval = Qtable.get(prev_stateaction);
				double Qmax = this.getMaxActionQValue(new_state);
				double alpha = this.alpha(Ntable.get(prev_stateaction));
				this.Qtable.put(
						prev_stateaction,
						Qval + alpha*(previous_reward + GAMMA_DISCOUNT_FACTOR*Qmax - Qval) 
				); 

				/* See top for constants and below for helper functions */

				int action = selectAction(new_state); /*
													 * Make sure you understand
													 * how it selects an action
													 */

				performAction(action);

				/* Only print every 10th line to reduce spam */
				if(this.print_log ) {
					print_counter++;
					if (print_counter % 10 == 0) {
						System.out.println(
							new StringBuilder()
								.append(String.format("ITERATION: %6d ",iteration))
								.append(String.format("SENSORS: a=%7s ",df.format(angle.getValue())))
								.append(String.format("vx=%7s ", df.format(vx.getValue())))
								.append(String.format("vy=%7s ", df.format(vy.getValue())))
								.append(String.format("P_STATE: %24s P_ACTION: %d ",previous_state, previous_action))
								.append(String.format("P_REWARD: %4s ", df.format(previous_reward)))
								.append(String.format("P_QVAL: %5s ", df.format(Qtable.get(prev_stateaction))))
								.append(String.format("Tested: %5d times", Ntable.get(prev_stateaction)))
								.toString());
					}
				}
				previous_vy = vy.getValue();
				previous_vx = vx.getValue();
				previous_angle = angle.getValue();
				previous_action = action;
			}
			previous_state = new_state;
		}

	}

	/*
	 * Computes the learning rate parameter alpha based on the number of times
	 * the state-action combination has been tested
	 */
	public double alpha(int num_tested) {
		/*
		 * Lower learning rate constants means that alpha will become small
		 * faster and therefore make the agent behavior converge to to a
		 * solution faster, but if the state space is not properly explored at
		 * that point the resulting behavior may be poor. If your state-space is
		 * really huge you may need to increase it.
		 */
		double alpha = (LEARNING_RATE_CONSTANT / (LEARNING_RATE_CONSTANT + num_tested));
		return alpha;
	}

	/* Finds the highest Qvalue of any action in the given state */
	public double getMaxActionQValue(String state) {
		double maxQval = Double.NEGATIVE_INFINITY;

		for (int action = 0; action < NUM_ACTIONS; action++) {
			Double Qval = Qtable.get(state + action);
			if (Qval != null && Qval > maxQval) {
				maxQval = Qval;
			}
		}

		if (maxQval == Double.NEGATIVE_INFINITY) {
			/* Assign 0 as that corresponds to initializing the Qtable to 0. */
			maxQval = 0;
		}
		return maxQval;
	}

	/*
	 * Selects an action in a state based on the registered Q-values and the
	 * exploration chance
	 */
	public int selectAction(String state) {
		Random rand = new Random();

		int action = 0;
		/* May do exploratory move if in exploration mode */
		if (explore && Math.abs(rand.nextDouble()) < explore_chance) {
			/* Taking random exploration action! */
			action = Math.abs(rand.nextInt()) % NUM_ACTIONS;
			return action;
		}

		/* Find action with highest Q-val (utility) in given state */
		double maxQval = Double.MIN_VALUE;
		for (int i = 0; i < NUM_ACTIONS; i++) {
			String test_pair = state + i; /*
										 * Generate a state-action pair for all
										 * actions
										 */
			double Qval = 0;
			if (Qtable.get(test_pair) != null) {
				Qval = Qtable.get(test_pair);
				if (Qval > maxQval) {
					maxQval = Qval;
					action = i;
				}
			}
		}
		if(maxQval == Double.MIN_VALUE) {
			action = rand.nextInt() % NUM_ACTIONS;
		}
		return action;
	}

	/*
	 * The 'E' key will toggle the agents exploration mode. Turn this off to
	 * test its behavior
	 */
	public void toggleExplore() {
		/* Make sure we don't toggle it multiple times */
		if (System.currentTimeMillis() - lastPress < 1000) {
			return;
		}
		if (explore) {
			System.out.println("Turning OFF exploration!");
			explore = false;
		} else {
			System.out.println("Turning ON exploration!");
			explore = true;
		}
		lastPress = System.currentTimeMillis();
	}

	// Key 1 toggles output
	public void toggleCustom1() {
		if (System.currentTimeMillis() - lastPress < 1000) {
			return;
		}
		if (this.print_log) {
			System.out.println("Printing turned OFF");
			this.print_log = false;
		} else {
			System.out.println("Printing turned ON");
			this.print_log = true;
		}
		lastPress = System.currentTimeMillis();
		/*
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(new FileOutputStream("qtable.dat"));
		} catch (IOException e) {
			System.out.println("Cannot write to a file");
			return;
		}
		
		try {
			oos.writeObject(Qtable);
		} catch (IOException e) {}
		
		try {
			oos.close();
		} catch (IOException e) {}
		System.out.println("Qtable saved to qtable.dat");
		
		try {
			oos = new ObjectOutputStream(new FileOutputStream("ntable.dat"));
		} catch (IOException e) {
			System.out.println("Cannot write to a file");
			return;
		}
		
		try {
			oos.writeObject(Ntable);
		} catch (IOException e) {}
		
		try {
			oos.close();
		} catch (IOException e) {}
		System.out.println("Ntable saved to ntable.dat");
		*/
	}

	// Key 2 changes StatesAndRewards
	public void toggleCustom2() {
		if (System.currentTimeMillis() - lastPress < 1000) {
			return;
		}
		
		this.Qtable = new Hashtable<String, Double>();
		this.Ntable = new Hashtable<String, Integer>();

		if (this.stateAndReward instanceof HoverStateAndReward) {
			this.stateAndReward = new AngleStateAndReward();
			System.out.println("Changed to Angle reward");
		} else {
			this.stateAndReward = new HoverStateAndReward();
			System.out.println("Changed to Hover reward");
		}
		this.explore = true;
		this.iteration = 0;
		lastPress = System.currentTimeMillis();
		/*
		ObjectInputStream ois;
		
		try {
			ois = new ObjectInputStream(new FileInputStream("qtable.dat"));
		} catch (IOException e) {
			System.out.println("No saved file found!");
			return;
		}
		try {
			this.Qtable = (Hashtable<String, Double>) ois.readObject();
		} catch (ClassNotFoundException e) {
			//impossible
		} catch (IOException e) {}
		
		try {
			ois.close();
		} catch (IOException e) {}
		System.out.println("Qtable loaded from qtable.dat");
		
		try {
			ois = new ObjectInputStream(new FileInputStream("ntable.dat"));
		} catch (IOException e) {
			System.out.println("No saved file found!");
			return;
		}
		try {
			this.Ntable = (Hashtable<String, Integer>) ois.readObject();
		} catch (ClassNotFoundException e) {
			//impossible
		} catch (IOException e) {
		}
		try {
			ois.close();
		} catch (IOException e) {}
		System.out.println("Ntable loaded from ntable.dat");
		*/
	}

	public void pause() {
		paused = true;
		resetRockets();
	}

	public void run() {
		paused = false;
	}
}

