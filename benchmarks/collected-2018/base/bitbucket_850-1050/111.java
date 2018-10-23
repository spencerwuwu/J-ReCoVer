// https://searchcode.com/api/result/63655591/

// Reductor.java

package gov.nasa.jpf.hmi.generation.reduction;

import static java.lang.System.out;

import gov.nasa.jpf.hmi.Config;
import gov.nasa.jpf.hmi.generation.NonFullControlDeterministicModelException;
import gov.nasa.jpf.hmi.models.Action;
import gov.nasa.jpf.hmi.models.ActionType;
import gov.nasa.jpf.hmi.models.LTS;
import gov.nasa.jpf.hmi.models.LTS.Copier;
import gov.nasa.jpf.hmi.models.State;
import gov.nasa.jpf.hmi.models.Transition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class containing utilities method for analyzing HCI
 * based on reduction technique, with three possible variant
 *
 * @author Sebastien Combefis (UCLouvain)
 * @version August 11, 2011
 */
public final class Reductor
{
	// Enumeration
	private enum ReductionMethod { CLASSIC, COMPLETION, HYBRID }
	
	// Constants
	private final Action dcAction = new Action ("_DC()", ActionType.COMMAND);
	
	// Instance variables
	private final LTS<State,Transition> lts;
	private final ReductionMethod reductionMethod;
	private final State dontcare;
	
	/**
	 * Constructor
	 * 
	 * @pre lts != null
	 * @post An instance of this is constructed, and is attached to the specified LTS
	 *       with the specified configuration saved (learning a 3DFA or not, hybrid
	 *       completion for non-transitivity or not)
	 */
	public Reductor (LTS<State,Transition> lts, boolean learn3DFA, boolean hybrid)
	{
		this.lts = lts;
		if (learn3DFA && hybrid)
		{
			reductionMethod = ReductionMethod.HYBRID;
		}
		else if (learn3DFA && ! hybrid)
		{
			reductionMethod = ReductionMethod.COMPLETION;
		}
		else
		{
			reductionMethod = ReductionMethod.CLASSIC;
		}
		dontcare = new State ("DC");
		
		// TODO make a clone of the LTS, because need to be completed
	}
	
	/**
	 * Constructor
	 * 
	 * @pre lts != null
	 * @post An instance of this is constructed, and is attached
	 *       to the specified LTS
	 */
	public Reductor (LTS<State,Transition> lts)
	{
		this (lts, false, false);
	}
	
	/**
	 * Compute the reduction of the LTS
	 * 
	 * @pre -
	 * @post The returned value contains the reduced LTS of the LTS attached to this
	 */
	public LTS<State,Transition> reduce() throws NonFullControlDeterministicModelException
	{
		if (Config.DEBUG) out.println ("********** Reduction of the following LTS :\n\n" + lts);
		
		if (Config.DEBUG) out.println ("********** Completion of the LTS :\n");
		lts.complete (new Copier<State,Transition>() {
			@Override
			public State copyState (State s)
			{
				return new State (s.getName());
			}
			
			@Override
			public Transition copyTransition (Transition t)
			{
				return new Transition (t.getAction());
			}
		});
		
		// Adding the don't care state to the LTS, and a loop with a unique command on it
		if (reductionMethod != ReductionMethod.CLASSIC)
		{
			lts.addState (dontcare);
			lts.addTransition (new Transition (dcAction), dontcare, dontcare);
			
			if (reductionMethod == ReductionMethod.COMPLETION)
			{
				// Complete the LTS with all transitions to DC state
				Set<Action> observations = new HashSet<Action>();
				for (Transition t : lts.transitions())
				{
					if (t.getAction().getType() == ActionType.OBSERVATION)
					{
						observations.add (t.getAction());
					}
				}
				
				for (State s : lts.states())
				{
					Set<Action> actions = new HashSet<Action>();
					Set<Transition> transitions = lts.outTransitions (s);
					for (Transition t : transitions)
					{
						actions.add (t.getAction());
					}
					for (Action obs : observations)
					{
						if (! actions.contains (obs))
						{
							lts.addTransition (new Transition (obs), s, dontcare);
						}
					}
				}
			}
		}
		
		if (Config.DEBUG) out.println (lts);
		
		out.println ("Completion finished");
		out.printf ("%d states and %s transitions\n", lts.statesCount(), lts.transitionsCount());
		
		initialize();
		
		Set<Block> blocks = preprocess();
		if (Config.DEBUG) out.printf ("********** Initial Blocks (%d) :\n\n   %s\n\n", blocks.size(), blocks);
		
		if (Config.DEBUG) out.println ("********** Starting reduction\n");
		blocks = stabilize (blocks);
		out.printf ("********** Stabilized Blocks (%d) :\n\n   %s\n", blocks.size(), blocks);
		
		LTS<State,Transition> reducedLTS = new LTS<State,Transition>();
		Map<State,Integer> origStatesMap = new HashMap<State,Integer>();
		Map<Integer,State> newStatesMap = new HashMap<Integer,State>();
		
		// Finding initial state
		Block initialBlock = null;
		outer: for (Block block : blocks)
		{
			for (State s : block.states)
			{
				if (s.equals (lts.initialState()))
				{
					initialBlock = block;
					State blockState = new State (String.format ("S%d %s", block.id, block.states));
					newStatesMap.put (block.id, blockState);
					reducedLTS.addState (blockState);
					break outer;
				}
			}
		}
		
		// Adding all the other states
		for (Block block : blocks)
		{
			if (! block.equals (initialBlock))
			{
				State blockState = new State (String.format ("S%d %s", block.id, block.states));
				newStatesMap.put (block.id, blockState);
				reducedLTS.addState (blockState);
			}
			
			for (State s : block.states)
			{
				origStatesMap.put (s, block.id);
			}
		}
		
		// Adding the transitions
		for (Transition t : lts.transitions())
		{
			Transition newTransition = new Transition (t.getAction());
			if (! reducedLTS.hasTransition (newTransition) && ! (lts.source (t).equals (lts.destination (t)) && lts.isTauTransition (t)))
			{
				reducedLTS.addTransition (newTransition, newStatesMap.get (origStatesMap.get (lts.source (t))), newStatesMap.get (origStatesMap.get (lts.destination (t))));
			}
		}
		
		return reducedLTS;
	}
	
	/**
	 * Initialize : creates the state informations objects
	 * 
	 * @pre -
	 * @post The state information objects are created for the LTS attached
	 *       to this, and stored in the Block's stateInfos class variable
	 */
	private void initialize()
	{
		Block.stateInfos.clear();
		for (State s : lts.states())
		{
			Block.stateInfos.put (s, new StateInfo (lts, s));
		}
	}
	
	/**
	 * Preprocessing step : compute blocks according to commands
	 * 
	 * @pre -
	 * @post The returned value contains the partition of the states
	 *       of the LTS attached to this, according to the commands
	 */
	private Set<Block> preprocess()
	{
		Set<Block> blocks = new HashSet<Block>();
		
		// For each state of the LTS, find in which block it should go
		for (State s : lts.states())
		{
			// Try each block
			boolean found = false;
			for (Block block : blocks)
			{
				// Take one state from the block, as representative
				State refState = block.states.iterator().next();
				
				if (Block.stateInfos.get (refState).commands.equals (Block.stateInfos.get (s).commands))
				{
					block.addState (s);
					found = true;
					break;
				}
			}
			
			// If no blocks found, should add a new one
			if (! found)
			{
				if (Config.DEBUG) out.println ("New block for " + s + " : " + Block.stateInfos.get (s).commands);
				Block newBlock = new Block();
				newBlock.addState (s);
				blocks.add (newBlock);
			}
		}
		
		return blocks;
	}
	
	// TODO: spec
	private void checkStateNonDeterminism (Block B, Action a) throws NonFullControlDeterministicModelException
	{
		// If one state has multiple times an action, leading on non-equivalent
		// states, the reduction could not succeed, should throw an error
		// TODO: define a specific exception
		for (State s : B.states)
		{
			Set<Transition> outs = outTransitions (lts, s, a);
			if (! outs.isEmpty())
			{
				Transition refTrans = outs.iterator().next();
				Block destBlock = Block.blocksMap.get (lts.destination (refTrans));
				int destId = destBlock.id;
				for (Transition transition : outs)
				{
					if (Block.blocksMap.get (lts.destination (transition)).id != destId)
					{
						out.printf ("%s --%s--> %s\n", lts.source (refTrans), refTrans, lts.destination (refTrans));
						out.printf ("%s --%s--> %s\n", lts.source (transition), transition, lts.destination (transition));
						
						out.println (outActions (lts, lts.destination (refTrans), ActionType.COMMAND));
						out.println (outActions (lts, lts.destination (transition), ActionType.COMMAND));

						throw new NonFullControlDeterministicModelException ("Impossible to compute reduction, due to non-determinism on action " + a + " on state " + s);
					}
				}
			}
		}
	}
	
	/**
	 * Perform the stabilisation of the blocks
	 * 
	 * @pre blocks != null
	 * @post The returned value contains the stabilisation of the
	 *       specified set of blocks, according to full-control equivalence
	 */
	private Set<Block> stabilize (Set<Block> blocks) throws NonFullControlDeterministicModelException
	{
		Map<Integer,Block> toBeProcessed = new HashMap<Integer,Block>();
		Map<Integer,Block> stables = new HashMap<Integer,Block>();
		
		// Initialize the toBeProcessed map
		Block.blocksMap.clear();
		for (Block b : blocks)
		{
			for (State s : b.states)
			{
				Block.blocksMap.put (s, b);
			}
			toBeProcessed.put (b.id, b);
		}
		
		// Loop until nothing to be processed
		while (toBeProcessed.size() > 0)
		{
			if (Config.DEBUG) out.printf ("\n   To be processed (%d) : %s\n", toBeProcessed.size(), toBeProcessed.values());
			if (Config.DEBUG) out.printf ("   Stables (%d) : %s\n", stables.size(), stables.values());
			
			// Take a block to be processed
			Block B = toBeProcessed.values().iterator().next();
			toBeProcessed.remove (B.id);
			
			if (Config.DEBUG) out.println ("   |\n   | Take " + B);
			
			// The block contains only one state, and becomes thus stable
			if (B.states.size() == 1)
			{
				if (Config.DEBUG) out.println ("   |  -> Becomes stable (size = 1)");
				stables.put (B.id, B);
				
				for (Action a : B.unvisited)
				{
					checkStateNonDeterminism (B, a);
				}
			}
			else
			{
				if (B.unvisited.size() > 0)
				{
					// Set of blocks that could need a reprocessing
					Set<Block> needReprocessing = new HashSet<Block>();
					for (State s : B.states)
					{
						Set<Transition> ins = lts.inTransitions (s);
						for (Transition in : ins)
						{
							needReprocessing.add (Block.blocksMap.get (lts.source (in)));
						}
					}
					
					// Split the block into new ones
					Map<Integer,Block> newblocks = new HashMap<Integer,Block>();
					Action a = B.unvisited.iterator().next();
					B.unvisited.remove (a);
					checkStateNonDeterminism (B, a);
					if (Config.DEBUG) out.print ("   |  -> Action " + a);
					
					boolean nondeterministic = false;
					
					// For a command, all the states should go to equivalent states
					if (a.getType() == ActionType.COMMAND)
					{
						if (Config.DEBUG) out.println (" (COMMAND)");
						
						for (State s : B.states)
						{
							Set<Transition> outs = outTransitions (lts, s, a);
							Transition transition = outs.iterator().next();
							State to = lts.destination (transition);
							if (Config.DEBUG) out.println ("   |     " + s + " -" + transition.getAction() + "-> " + to);
							
							Block b;
							if ((b = newblocks.get (Block.blocksMap.get (to).id)) != null)
							{
								b.addState (s);
							}
							else
							{
								b = new Block();
								b.addState (s);
								newblocks.put (Block.blocksMap.get (to).id, b);
							}
						}
					}
					// For an observation, only states having the transition should go to equivalent states
					else if (a.getType() == ActionType.OBSERVATION)
					{
						if (Config.DEBUG) out.println (" (OBSERVATION)");
						Set<State> defaultStates = new HashSet<State>();
						
						for (State s : B.states)
						{
							Set<Transition> outs = outTransitions (lts, s, a);
							if (outs.size() > 0)
							{
								Transition transition = outs.iterator().next();
								State to = lts.destination (transition);
								if (Config.DEBUG) out.println ("   |     " + s + " -" + transition.getAction() + "-> " + to);
								
								Block b;
								if ((b = newblocks.get (Block.blocksMap.get (to).id)) != null)
								{
									b.addState (s);
								}
								else
								{
									b = new Block();
									b.addState (s);
									newblocks.put (Block.blocksMap.get (to).id, b);
								}
							}
							else
							{
								defaultStates.add (s);
							}
						}
						
						// Manage the default states, which are added to one block, chosen randomly
						if (newblocks.size() > 0)
						{
							if (newblocks.size() != 1 && defaultStates.size() > 0)
							{
								nondeterministic = true;
								if (reductionMethod == ReductionMethod.CLASSIC)
								{
									throw new NonFullControlDeterministicModelException ("Non-determinist choice for states " + defaultStates + " for action " + a + " which can go in either of " + newblocks);
								}
							}
							
							Block randBlock = newblocks.get (newblocks.keySet().iterator().next());
							for (State s : defaultStates)
							{
								randBlock.addState (s);
							}
						}
					}
					else
					{
						out.println();
					}
					
					
					if (reductionMethod == ReductionMethod.HYBRID && nondeterministic)
					{
						if (Config.DEBUG) out.println ("   |     -> Non-determinism issue for " + B + " for observation " + a);
						for (State s : B.states)
						{
							if (outTransitions (lts, s, a).size() == 0)
							{
								if (Config.DEBUG) out.println ("   |        Completion " + s + " -" + a + "-> DC");
								lts.addTransition (new Transition (a), s, dontcare);
							}
						}
						B.markUnvisitedAll();
						toBeProcessed.put (B.id, B);
					}
					else
					{
						// Update the structure according to new blocks
						if (Config.DEBUG) out.println ("   |     -> New blocks " + newblocks.values());
						if (newblocks.size() > 1)
						{
							// Add the new blocks to the toBeProcessed list
							for (Block b : newblocks.values())
							{
								toBeProcessed.put (b.id, b);
								Block.refreshStatesMarks (b);
							}
							
							// Requeue blocks that would need a reprocessing
							for (Block b : needReprocessing)
							{
								if (b.id != B.id && b.states.size() > 1)
								{
									b.markUnvisitedAll();
									if (stables.containsKey (b.id))
									{
										stables.remove (b.id);
									}
									toBeProcessed.put (b.id, b);
									Block.refreshStatesMarks (b);
									
									if (Config.DEBUG) out.println ("   |     -> To be reprocessed " + b);
								}
							}
						}
						else
						{
							// Block is of size 1, should check if more action to tests
							if (B.unvisited.size() > 0)
							{
								toBeProcessed.put (B.id, B);
								Block.refreshStatesMarks (B);
							}
							else
							{
								if (Config.DEBUG) out.println ("   |     -> Becomes stable (no more action to test)");
								stables.put (B.id, B);
							}
						}
					}
				}
				else
				{
					if (Config.DEBUG) out.println ("   |  -> Becomes stable (no action to test)");
					stables.put (B.id, B);
				}
			}
			
			
			if (Config.DEBUG) out.println ("   |");
		}
		if (Config.DEBUG) out.println();
		
		// Send back the result
		Set<Block> result = new HashSet<Block>();
		result.addAll (stables.values());
		return result;
	}
	
	// TODO : refactor by adding TransitionFilter to LTS class, for outTransitions method
	private static Set<Transition> outTransitions (LTS<State,Transition> lts, State s, Action a)
	{
		Set<Transition> transitions = new HashSet<Transition>();
		for (Transition t : lts.outTransitions (s))
		{
			if (t.getAction().equals (a))
			{
				transitions.add (t);
			}
		}
		return transitions;
	}
	
	// TODO : spec
	private static Set<Action> outActions (LTS<State,Transition> lts, State s, ActionType type)
	{
		Set<Action> commands = new HashSet<Action>();
		for (Transition t : lts.outTransitions (s))
		{
			if (t.getAction().getType() == type)
			{
				commands.add (t.getAction());
			}
		}
		return commands;
	}
	
	/**
	 * Class representing a block of states (an element of a partition
	 * of the set of states of the LTS attached to this)
	 *
	 * @author Sbastien Combfis
	 * @version June 18, 2010
	 */
	private static class Block
	{
		// Class variables
		private static final Map<State,StateInfo> stateInfos = new HashMap<State,StateInfo>();
		private static final Map<State,Block> blocksMap = new HashMap<State,Block>();
		private static int nextid = 0;
		
		// Instance variables
		private final int id = nextid++;
		private final Set<State> states;
		private final Set<Action> unvisited;
		
		/**
		 * Constructor
		 * 
		 * @pre -
		 * @post An instance of this is created, representing a block
		 *       with no states
		 */
		public Block()
		{
			this (new HashSet<State>());
		}
		
		/**
		 * Constructor
		 * 
		 * @pre states != null
		 * @post An instance of this is created, representing a block
		 *       containing all the states of the specified set
		 */
		public Block (Set<State> states)
		{
			this.states = new HashSet<State>();
			this.states.addAll (states);
			
			unvisited = new HashSet<Action>();
			markUnvisitedAll();
		}
		
		/**
		 * Add a state to this block
		 * 
		 * @pre s != null
		 * @post The specified state is added to this block
		 *       if it was not already in it
		 */
		public void addState (State s)
		{
			states.add (s);
			
			unvisited.addAll (stateInfos.get (s).commands);
			unvisited.addAll (stateInfos.get (s).observations);
		}
		
		/**
		 * Marks all the actions as unvisited
		 * 
		 * @pre -
		 * @post All the actions of the states of this block
		 *       are marked as unvisited
		 */
		public void markUnvisitedAll()
		{
			for (State s : states)
			{
				unvisited.addAll (stateInfos.get (s).commands);
				unvisited.addAll (stateInfos.get (s).observations);
			}	
		}
		
		/**
		 * Refresh the block states'id mark
		 * 
		 * @pre b != null
		 * @post The states of the block are marked as belonging
		 *       to the block in this class's blocksMap
		 */
		public static void refreshStatesMarks (Block b)
		{
			for (State s : b.states)
			{
				blocksMap.put (s, b);
			}
		}
		
		@Override
		public String toString()
		{
			return String.format ("%d:%s%s", id, states, unvisited);
		}
	}
	
	/**
	 * Class representing informations about a state of a LTS
	 *
	 * @author Sbastien Combfis
	 * @version June 18, 2010
	 */
	private static class StateInfo
	{
		// Instance variables
		private final State s;
		private final Set<Action> commands;
		private final Set<Action> observations;
		
		/**
		 * Constructor
		 * 
		 * @pre -
		 * @post An instance of this is created, representing information
		 *       about the specified state of the specified LTS
		 */
		public StateInfo (LTS<State,Transition> lts, State s)
		{
			this.s = s;
			commands = new HashSet<Action>();
			observations = new HashSet<Action>();
			
			for (Transition t : lts.outTransitions (s))
			{
				switch (t.getAction().getType())
				{
					case COMMAND:
						commands.add (t.getAction());
					break;
					
					case OBSERVATION:
						observations.add (t.getAction());
					break;
				}
			}
		}
	}
	
	/**
	 * Check if the LTS is fully controllable by the specified interface
	 * 
	 * @pre mental != null
	 * @post The returned value contains true is this model is fully
	 *       controllable by the specified interface
	 */
	public boolean isFullControl (LTS<State,Transition> mental)
	{
		Set<CompState> explored = new HashSet<CompState>();
		Set<CompState> toexplore = new HashSet<CompState>();
		toexplore.add (new CompState (lts.initialState(), mental.initialState()));
		
		while (! toexplore.isEmpty())
		{
			CompState current = toexplore.iterator().next();
			toexplore.remove (current);
			if (! explored.contains (current))
			{
				explored.add (current);
			}
			
			// Check the composed state
			Set<Action> machineC, mentalC, machineO, mentalO;
			machineC = getActions (lts, current.s1, ActionType.COMMAND);
			mentalC = getActions (mental, current.s2, ActionType.COMMAND);
			machineO = getActions (lts, current.s1, ActionType.OBSERVATION);
			mentalO = getActions (mental, current.s2, ActionType.OBSERVATION);
			
//			out.printf ("Checking %s\n", current);
//			out.printf ("   %s = %s /\\ %s c= %s\n", machineC, mentalC, machineO, mentalO);
			
			if (! (machineC.equals (mentalC) && isSubset (machineO, mentalO)))
			{
				return false;
			}
			
			// Expand the state
			Set<Action> machine = new HashSet<Action>();
			machine.addAll (machineC);
			machine.addAll (machineO);
			for (Action a : machine)
			{
				for (Transition t : outTransitions (lts, current.s1, a))
				{
					for (Transition u : outTransitions (mental, current.s2, a))
					{
						CompState s = new CompState (lts.destination (t), mental.destination (u));
						if (! explored.contains (s) && ! toexplore.contains (s))
						{
							toexplore.add (s);
						}
					}
				}
			}
			
//			out.printf ("   explored : %s\n", explored);
//			out.printf ("   toexplore : %s\n", toexplore);
		}
		
		return true;
	}
	
	/**
	 * Check if the LTS is fully controllable by the specified interface
	 * 
	 * @pre mental != null
	 * @post The returned value contains true is this model is fully
	 *       controllable by the specified interface
	 */
	public boolean isModeConsistent (LTS<State,Transition> mental)
	{
		Set<CompState> explored = new HashSet<CompState>();
		Set<CompState> toexplore = new HashSet<CompState>();
		toexplore.add (new CompState (lts.initialState(), mental.initialState()));
		
		while (! toexplore.isEmpty())
		{
			CompState current = toexplore.iterator().next();
			toexplore.remove (current);
			if (! explored.contains (current))
			{
				explored.add (current);
			}
			
			// Check the composed state
			Set<Action> mentalC, machineO;
			mentalC = getActions (mental, current.s2, ActionType.COMMAND);
			machineO = getActions (lts, current.s1, ActionType.OBSERVATION);
			
			out.printf ("Checking %s\n", current);
			out.printf ("   l(%s) = l(%s)\n", current.s1, current.s2);
			
			if (current.s1.getMode() != current.s2.getMode())
			{
				return false;
			}
			
			// Expand the state
			for (Action a : machineO)
			{
				for (Transition t : outTransitions (lts, current.s1, a))
				{
					Set<Transition> trans = outTransitions (mental, current.s2, a);
					if (trans.isEmpty())
					{
						CompState s = new CompState (lts.destination (t), current.s2);
						if (! explored.contains (s) && ! toexplore.contains (s))
						{
							toexplore.add (s);
						}
					}
					else
					{
						for (Transition u : trans)
						{
							CompState s = new CompState (lts.destination (t), mental.destination (u));
							if (! explored.contains (s) && ! toexplore.contains (s))
							{
								toexplore.add (s);
							}
						}
					}
				}
			}
			for (Action a : mentalC)
			{
				for (Transition t : outTransitions (mental, current.s2, a))
				{
					Set<Transition> trans = outTransitions (lts, current.s1, a);
					if (trans.isEmpty())
					{
						CompState s = new CompState (current.s1, mental.destination (t));
						if (! explored.contains (s) && ! toexplore.contains (s))
						{
							toexplore.add (s);
						}
					}
					else
					{
						for (Transition u : trans)
						{
							CompState s = new CompState (lts.destination (u), mental.destination (t));
							if (! explored.contains (s) && ! toexplore.contains (s))
							{
								toexplore.add (s);
							}
						}
					}
				}
			}
			
			out.printf ("   explored : %s\n", explored);
			out.printf ("   toexplore : %s\n", toexplore);
		}
		
		return true;
	}
	
	// TODO: specs
	private static Set<Action> getActions (LTS<State,Transition> lts, State s, ActionType type)
	{
		Set<Action> actions = new HashSet<Action>();
		for (Transition t : lts.outTransitions (s))
		{
			if (t.getAction().getType() == type)
			{
				actions.add (t.getAction());
			}
		}
		return actions;
	}
	
	/**
	 * Test whether a set is a subset of another one
	 * 
	 * @pre s1, s2 != null
	 * @post The returned value contains true if s1 is a subset of s2 and false otherwise
	 *       The sets s1 and s2 are unchanged
	 */
	private static <T> boolean isSubset (Set<T> s1, Set<T> s2)
	{
		for (T t : s1)
		{
			if (! s2.contains (t))
			{
				return false;
			}
		}
		return true;
	}
	
	// TODO: specs
	private static class CompState
	{
		private final State s1, s2;
		
		public CompState (State s1, State s2)
		{
			this.s1 = s1;
			this.s2 = s2;
		}
		
		@Override
		public String toString()
		{
			return String.format ("(%s, %s)", s1, s2);
		}
		
		@Override
		public boolean equals (Object o)
		{
			if (o instanceof CompState)
			{
				CompState s = (CompState) o;
				return s1.equals (s.s1) && s2.equals (s.s2);
			}
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return toString().hashCode();
		}
	}
}
