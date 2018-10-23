// https://searchcode.com/api/result/56435067/

// Reductor.java

package util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import util.LTS.Copier;

/**
 * Class containing utilities method for analyzing HCI
 *
 * @author S&#x17D;bastien Comb&#x17D;fis
 * @version July 24, 2010
 */
public final class Reductor
{
	// Instance variables
	private final LTS<State,Transition> lts;
	
	/**
	 * Constructor
	 * 
	 * @pre lts != null
	 * @post An instance of this is constructed, and is attached
	 *       to the specified LTS
	 */
	public Reductor (LTS<State,Transition> lts)
	{
		this.lts = lts;
		
		// TODO make a clone of the LTS, because need to be completed
	}
	
	/**
	 * Compute the reduction of the LTS
	 * 
	 * @pre -
	 * @post The returned value contains the equivalence classes of
	 *       the states of the LTS attached to this
	 */
	public Set<Set<State>> reduce()
	{
		System.out.println ("********** Reduction of the following LTS :\n\n" + lts);
		initialize();
		
		System.out.println ("********** Completion of the LTS :\n");
		lts.complete (new Copier<State,Transition>() {
			@Override
			public State copyState (State s)
			{
				return new State (s.name);
			}
			
			@Override
			public Transition copyTransition (Transition t)
			{
				return new Transition (t.action);
			}
		});
		System.out.println (lts);
		
		Set<Block> blocks = preprocess();
		System.out.printf ("********** Initial Blocks (%d) :\n\n   %s\n\n", blocks.size(), blocks);
		
		System.out.println ("********** Starting reduction\n");
		blocks = stabilize (blocks);
		System.out.printf ("********** Stabilized Blocks (%d) :\n\n   %s\n", blocks.size(), blocks);
		
		return null;
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
				Block newBlock = new Block();
				newBlock.addState (s);
				blocks.add (newBlock);
			}
		}
		
		return blocks;
	}
	
	// TODO: spec
	private void checkStateNonDeterminism (Block B, Action a)
	{
		// If one state has multiple times an action, leading on non-equivalent
		// states, the reduction could not succeed, should throw an error
		// TODO: define a specific exception
		for (State s : B.states)
		{
			Set<Transition> outs = outTransitions (lts, s, a);
			if (! outs.isEmpty())
			{
				int destId = Block.blocksMap.get (lts.destination (outs.iterator().next())).id;
				for (Transition out : outs)
				{
					if (Block.blocksMap.get (lts.destination (out)).id != destId)
					{
						throw new RuntimeException ("Impossible to compute reduction, due to non-determinism on action " + a + " on state " + s);
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
	private Set<Block> stabilize (Set<Block> blocks)
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
			System.out.printf ("\n   To be processed (%d) : %s\n", toBeProcessed.size(), toBeProcessed.values());
			System.out.printf ("   Stables (%d) : %s\n", stables.size(), stables.values());
			
			// Take a block to be processed
			Block B = toBeProcessed.values().iterator().next();
			toBeProcessed.remove (B.id);
			
			System.out.println ("   |\n   | Take " + B);
			
			// The block contains only one state, and becomes thus stable
			if (B.states.size() == 1)
			{
				System.out.println ("   |  -> Becomes stable (size = 1)");
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
					System.out.print ("   |  -> Action " + a);
					
					// For a command, all the states should go to equivalent states
					if (a.type == ActionType.COMMAND)
					{
						System.out.println (" (COMMAND)");
						
						for (State s : B.states)
						{
							Set<Transition> outs = outTransitions (lts, s, a);
							Transition out = outs.iterator().next();
							State to = lts.destination (out);
							System.out.println ("   |     " + s + " -" + out.action + "-> " + to);
							
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
					else if (a.type == ActionType.OBSERVATION)
					{
						System.out.println (" (OBSERVATION)");
						Set<State> defaultStates = new HashSet<State>();
						
						for (State s : B.states)
						{
							Set<Transition> outs = outTransitions (lts, s, a);
							if (outs.size() > 0)
							{
								Transition out = outs.iterator().next();
								State to = lts.destination (out);
								System.out.println ("   |     " + s + " -" + out.action + "-> " + to);
								
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
								throw new RuntimeException ("Non-determinist choice for states " + defaultStates + " for action " + a + " which can go in either of " + newblocks);
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
						System.out.println();
					}
					
					
					// Update the structure according to new blocks
					System.out.println ("   |     -> New blocks " + newblocks.values());
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
								
								System.out.println ("   |     -> To be reprocessed " + b);
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
							System.out.println ("   |     -> Becomes stable (no more action to test)");
							stables.put (B.id, B);
						}
					}
				}
				else
				{
					System.out.println ("   |  -> Becomes stable (no action to test)");
					stables.put (B.id, B);
				}
			}
			
			
			System.out.println ("   |");
		}
		System.out.println();
		
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
			if (t.action.equals (a))
			{
				transitions.add (t);
			}
		}
		return transitions;
	}
	
	/**
	 * Class representing a block of states (an element of a partition
	 * of the set of states of the LTS attached to this)
	 *
	 * @author S&#x17D;bastien Comb&#x17D;fis
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
	 * @author S&#x17D;bastien Comb&#x17D;fis
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
				switch (t.action.type)
				{
					case COMMAND:		commands.add (t.action);
					case OBSERVATION:	observations.add (t.action);
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
			
			System.out.printf ("Checking %s\n", current);
			System.out.printf ("   %s = %s /\\ %s c %s\n", machineC, mentalC, machineO, mentalO);
			
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
			
			System.out.printf ("   explored : %s\n", explored);
			System.out.printf ("   toexplore : %s\n", toexplore);
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
			
			System.out.printf ("Checking %s\n", current);
			System.out.printf ("   ?(%s) = ?(%s)\n", current.s1, current.s2);
			
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
			
			System.out.printf ("   explored : %s\n", explored);
			System.out.printf ("   toexplore : %s\n", toexplore);
		}
		
		return true;
	}
	
	// TODO: specs
	private static Set<Action> getActions (LTS<State,Transition> lts, State s, ActionType type)
	{
		Set<Action> actions = new HashSet<Action>();
		for (Transition t : lts.outTransitions (s))
		{
			if (t.action.type == type)
			{
				actions.add (t.action);
			}
		}
		return actions;
	}
	
	// TODO: specs
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
	
	/**
	 * Enumeration for the type of action
	 * 
	 * @author S&#x17D;bastien Comb&#x17D;fis
	 * @version June 18, 2010
	 */
	public static enum ActionType {COMMAND, OBSERVATION, TAU};
	
	/**
	 * Class representing a state of the LTS
	 * 
	 * @author S&#x17D;bastien Comb&#x17D;fis
	 * @version June 18, 2010
	 */
	public static class State
	{
		// Instance variables
		private final String name;
		private final int mode;
		
		/**
		 * Constructor
		 * 
		 * @pre name != null, mode >= 0
		 * @post An instance of this is created, representing a state
		 *       with specified name and mode
		 */
		public State (String name, int mode)
		{
			this.name = name;
			this.mode = mode;
		}
		
		/**
		 * Constructor
		 * 
		 * @pre name != null
		 * @post An instance of this is created, representing a state
		 *       with the specified name
		 */
		public State (String name)
		{
			this (name, 0);
		}
		
		/**
		 * Get the mode of the state
		 * 
		 * @pre -
		 * @post The returned value contains the mode's value of this state
		 */
		public int getMode()
		{
			return mode;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		@Override
		public boolean equals (Object o)
		{
			if (o instanceof State)
			{
				State s = (State) o;
				return name.equals (s.name) && mode == s.mode;
			}
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return name.hashCode();
		}
	}
	
	/**
	 * Class representing an action
	 * 
	 * @author S&#x17D;bastien Comb&#x17D;fis
	 * @version June 18, 2010
	 */
	public static class Action
	{
		// Instance variables
		private final String name;
		private final ActionType type;
		
		/**
		 * Constructor
		 * 
		 * @pre name, type != null
		 * @post An instance of this is created, representing an action with
		 *       specified name and type
		 */
		public Action (String name, ActionType type)
		{
			this.name = name;
			this.type = type;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		@Override
		public boolean equals (Object o)
		{
			if (o instanceof Action)
			{
				Action a = (Action) o;
				return name.equals (a.name) && type.equals (a.type);
			}
			return false;
		}
	}
	
	/**
	 * Class representing a transition
	 * 
	 * @author S&#x17D;bastien Comb&#x17D;fis
	 * @version June 18, 2010
	 */
	public static class Transition
	{
		// Instance variables
		private final Action action;
		
		/**
		 * Constructor
		 * 
		 * @pre action != null
		 * @post An instance of this is created, representing a transition
		 *       with specified action
		 */
		public Transition (Action action)
		{
			this.action = action;
		}
		
		@Override
		public String toString()
		{
			return action != null ? action.toString() : "tau";
		}
		
		@Override
		public boolean equals (Object o)
		{
			if (o instanceof Transition)
			{
				Transition t = (Transition) o;
				if (action == null && t.action != null)
				{
					return false;
				}
				return action.equals (t.action);
			}
			return false;
		}
	}
}
