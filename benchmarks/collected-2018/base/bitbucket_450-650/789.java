// https://searchcode.com/api/result/56435090/

// TestExtendedLTS.java

package gov.nasa.jpf.hmi.tests;

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.hmi.Config;
import gov.nasa.jpf.hmi.generation.NonFullControlDeterministicModelException;
import gov.nasa.jpf.hmi.generation.reduction.Reductor;
import gov.nasa.jpf.hmi.models.Action;
import gov.nasa.jpf.hmi.models.ActionType;
import gov.nasa.jpf.hmi.models.ExtendedState;
import gov.nasa.jpf.hmi.models.ExtendedTransition;
import gov.nasa.jpf.hmi.models.LTS;
import gov.nasa.jpf.hmi.models.State;
import gov.nasa.jpf.hmi.models.Transition;
import gov.nasa.jpf.hmi.models.util.LTSStats;

/**
 * TODO
 *
 * @author Sebastien Combefis (UCLouvain)
 * @version May 8, 2012
 */
public final class TestExtendedLTS
{
	public static void main (String[] args) throws NonFullControlDeterministicModelException, IOException
	{
		// Original system (HMI-SM)
		LTS<ExtendedState,Transition> system = autopilotExample();
		out.println ("=== Original system");
		out.printf ("  States: %d\n", system.statesCount());
		out.printf ("  Transitions: %d\n", system.transitionsCount());
		int[] transitions = new int[3];
		Set<Action> alphabet = new HashSet<Action>();
		for (Transition t : system.transitions())
		{
			alphabet.add (t.getAction());
			switch (t.getAction().getType())
			{
				case COMMAND: transitions[0]++; break;
				case OBSERVATION: transitions[1]++; break;
				case TAU: transitions[2]++; break;
			}
		}
		out.printf ("    -> Commands: %d, Observations: %d, Taus: %d\n", transitions[0], transitions[1], transitions[2]);
		out.printf ("  Alphabet (%d): %s\n", alphabet.size(), alphabet);
		int deadlocks = 0;
		int unreachable = 0;
		for (ExtendedState s : system.states())
		{
			if (system.outTransitions (s).size() == 0)
			{
				deadlocks++;
			}
			if (system.inTransitions (s).size() == 0)
			{
				unreachable++;
			}
		}
		out.printf ("  Deadlock states: %d\n", deadlocks);
		out.printf ("  Unreachable states: %d\n", unreachable);
		
		
		// Transformed system (HMI-LTS)
		LTS<State,Transition> transformedSystem = transformSystem (system);
		out.println ("\n=== Transformed system");
		out.printf ("  States: %d\n", transformedSystem.statesCount());
		out.printf ("  Transitions: %d\n", transformedSystem.transitionsCount());
		transitions = new int[3];
		alphabet = new HashSet<Action>();
		for (Transition t : transformedSystem.transitions())
		{
			alphabet.add (t.getAction());
			switch (t.getAction().getType())
			{
				case COMMAND: transitions[0]++; break;
				case OBSERVATION: transitions[1]++; break;
				case TAU: transitions[2]++; break;
			}
		}
		out.printf ("    -> Commands: %d, Observations: %d, Taus: %d\n", transitions[0], transitions[1], transitions[2]);
		out.printf ("  Alphabet (%d): %s\n", alphabet.size(), alphabet);
		deadlocks = 0;
		unreachable = 0;
		for (State s : transformedSystem.states())
		{
			if (transformedSystem.outTransitions (s).size() == 0)
			{
				deadlocks++;
			}
			if (transformedSystem.inTransitions (s).size() == 0)
			{
				unreachable++;
			}
		}
		out.printf ("  Deadlock states: %d\n", deadlocks);
		out.printf ("  Unreachable states: %d\n", unreachable);
		
		
		// Reduced system abstraction (HMI-LTS)
		Reductor reductor = new Reductor (transformedSystem);
		LTS<State,Transition> reduced = reductor.reduce();
		reduced.saveAsDotFile ("/Users/combefis/Desktop/result.dot");
		out.println ("\n=== Reduced system");
		out.printf ("  States: %d\n", reduced.statesCount());
		out.printf ("  Transitions: %d\n", reduced.transitionsCount());
		
		
		// Transformed back system abstraction (HMI-MM)
		LTS<State,ExtendedTransition> mental = transformMentalBack (reduced);
		mental.saveAsDotFile ("/Users/combefis/Desktop/mental.dot");
		out.println ("\n=== Transformed back system");
		out.printf ("  States: %d\n", mental.statesCount());
		out.printf ("  Transitions: %d\n", mental.transitionsCount());
		
		
//		
////		LTS<ExtendedState,Transition> system = systemExample1();
////		system.saveAsDotFile ("/Users/combefis/Desktop/system.dot");
////		System.out.println (system);
////		System.out.println (transformSystem (system));
////		transformSystem (system).saveAsDotFile ("/Users/combefis/Desktop/transformed-system.dot");
//		
//		
//		
////		LTSStats.printStats (transformSystem (system), "/Users/combefis/Desktop/system.htm");
//		
////		LTS<State,ExtendedTransition> mental = mentalExample1();
////		System.out.println (mental);
////		System.out.println (transformMental (mental));
//		
////		Config.DEBUG = true;
//		System.out.println (reduced);
////		LTSStats.printStats (reduced, "/Users/combefis/Desktop/mental.htm");
//		
//		LTSStats.printStats (mental, "/Users/combefis/Desktop/mental.htm");
	}
	
	private static LTS<State,Transition> transformSystem (LTS<ExtendedState,Transition> system)
	{
		LTS<State,Transition> lts = new LTS<State,Transition>();
		
		// Add actions
		Map<String,Action> actionsMap = new Hashtable<String,Action>();
		actionsMap.put ("tau", new Action ("tau", ActionType.TAU));
		actionsMap.put ("actionState", new Action ("actionState", ActionType.COMMAND));
		actionsMap.put ("observationState", new Action ("observationState", ActionType.COMMAND));
		for (ExtendedState s : system.states())
		{
			Action a = s.getStateObservation();
			if (! actionsMap.containsKey (s.getStateObservation().getName()))
			{
				actionsMap.put (a.getName(), new Action (a.getName(), ActionType.OBSERVATION));
			}
		}
		for (Transition t : system.transitions())
		{
			Action a = t.getAction();
			if (! actionsMap.containsKey (a.getName()))
			{
				actionsMap.put (a.getName(), new Action (a.getName(), a.getType()));
			}
		}
		
		// Add states
		Map<String,State> statesMap = new Hashtable<String,State>();
		for (ExtendedState s : system.states())
		{
			statesMap.put (s.getName(), new State (s.getName()));
			lts.addState (statesMap.get (s.getName()));
			lts.addTransition (new Transition (actionsMap.get ("observationState")), statesMap.get (s.getName()), statesMap.get (s.getName()));
			
			if (hasObservableTransition (system.outTransitions (s)))
			{
				String name = s.getName() + "'";
				statesMap.put (name, new State (name));
				lts.addState (statesMap.get (name));
				lts.addTransition (new Transition (actionsMap.get ("actionState")), statesMap.get (name), statesMap.get (name));
			}
		}
		lts.setInitialState (statesMap.get (system.initialState().getName()));
		
		// Add transitions
		for (Transition t : system.transitions())
		{
			if (system.isTauTransition (t))
			{
				lts.addTauTransition (new Transition (actionsMap.get ("tau")), statesMap.get (system.source (t).getName()), statesMap.get (system.destination (t).getName()));
			}
			else
			{
				lts.addTransition (new Transition (actionsMap.get (system.source (t).getStateObservation().getName())), statesMap.get (system.source (t).getName()), statesMap.get (system.source (t).getName() + "'"));
				lts.addTransition (new Transition (actionsMap.get (t.getAction().getName())), statesMap.get (system.source (t).getName() + "'"), statesMap.get (system.destination (t).getName()));
			}
		}
		
		return lts;
	}
	
	private static LTS<State,Transition> transformMental (LTS<State,ExtendedTransition> mental)
	{
		LTS<State,Transition> lts = new LTS<State,Transition>();
		
		// Add actions
		Map<String,Action> actionsMap = new Hashtable<String,Action>();
		Set<Action> guards = new HashSet<Action>();
		actionsMap.put ("tau", new Action ("tau", ActionType.TAU));
		for (ExtendedTransition t : mental.transitions())
		{
			Action a = t.getAction();
			if (! actionsMap.containsKey (a.getName()))
			{
				actionsMap.put (a.getName(), new Action (a.getName(), a.getType()));
			}
			Action g = t.getGuard();
			guards.add (g);
			if (! actionsMap.containsKey (g.getName()))
			{
				actionsMap.put (g.getName(), new Action (g.getName(), ActionType.OBSERVATION));
			}
		}
		
		// Add states
		Map<String,State> statesMap = new Hashtable<String,State>();
		for (State s : mental.states())
		{
			statesMap.put (s.getName(), new State (s.getName()));
			lts.addState (statesMap.get (s.getName()));
			
			for (Action g : guards)
			{
				String name = s.getName() + "[" + g.getName() + "]";
				statesMap.put (name, new State (name));
				lts.addState (statesMap.get (name));
			}
		}
		lts.setInitialState (statesMap.get (mental.initialState().getName()));
		
		// Add transitions
		for (State s : mental.states())
		{
			for (Action g : guards)
			{
				lts.addTransition (new Transition (actionsMap.get (g.getName())), statesMap.get (s.getName()), statesMap.get (s.getName() + "[" + g.getName() + "]"));
			}
		}
		for (ExtendedTransition t : mental.transitions())
		{
			lts.addTransition (new Transition (actionsMap.get (t.getAction().getName())), statesMap.get (mental.source (t).getName() + "[" + t.getGuard().getName() + "]"), statesMap.get (mental.destination (t).getName()));
		}
		
		return lts;
	}
	
	private static boolean hasObservableTransition (Set<Transition> transitions)
	{
		for (Transition t : transitions)
		{
			ActionType type = t.getAction().getType();
			if (type == ActionType.COMMAND || type == ActionType.OBSERVATION)
			{
				return true;
			}
		}
		return false;
	}
	
	private static LTS<State,ExtendedTransition> transformMentalBack (LTS<State,Transition> mental)
	{
		LTS<State,ExtendedTransition> lts = new LTS<State,ExtendedTransition>();
		
		// Add actions
		Map<String,Action> actionsMap = new Hashtable<String,Action>();
		Set<Action> guards = new HashSet<Action>();
		actionsMap.put ("tau", new Action ("tau", ActionType.TAU));
		for (Transition t : mental.transitions())
		{
			Action a = t.getAction();
			if (! actionsMap.containsKey (a.getName()) && ! "observationState".equals (a.getName()) && ! "actionState".equals (a.getName()))
			{
				Action newAction = new Action (a.getName(), a.getType());
				if (isObservationState (mental, mental.source (t)))
				{
					newAction = new Action (a.getName(), ActionType.STATE_OBSERVATION);
					guards.add (newAction);
				}
				actionsMap.put (newAction.getName(), newAction);
			}
		}
		
		// Add states
		Map<String,State> statesMap = new Hashtable<String,State>();
		for (State s : mental.states())
		{
			if (isObservationState (mental, s))
			{
				statesMap.put (s.getName(), new State (s.getName()));
				lts.addState (statesMap.get (s.getName()));
			}
		}
		lts.setInitialState (statesMap.get (mental.initialState().getName()));
		
		// Add transitions
		for (State s : mental.states())
		{
			if (isObservationState (mental, s))
			{
				for (Transition t : mental.outTransitions (s))
				{
					Action g = t.getAction();
					if (! "observationState".equals (g.getName()) && ! "actionState".equals (g.getName()))
					{
						for (Transition t2 : mental.outTransitions (mental.destination (t)))
						{
							Action a = t2.getAction();
							if (! "observationState".equals (a.getName()) && ! "actionState".equals (a.getName()))
							{
//								System.out.printf ("%s --%s--> %s\n", mental.source (t2), t2, mental.destination (t2));
								lts.addTransition (new ExtendedTransition (actionsMap.get (t2.getAction().getName()), actionsMap.get (g.getName())), statesMap.get (s.getName()), statesMap.get (mental.destination (t2).getName()));
							}
						}
					}
				}
			}
		}
		
		return lts;
	}
	
	private static boolean isObservationState (LTS<State,Transition> lts, State s)
	{
		for (Transition t : lts.outTransitions (s))
		{
			if (lts.destination (t).equals (s) && t.getAction().getName().equals ("observationState"))
			{
				return true;
			}
		}
		return false;
	}
	
	private static LTS<ExtendedState,Transition> systemExample1()
	{
		Action cancel = new Action ("cancel", ActionType.COMMAND);
		Action click = new Action ("click", ActionType.COMMAND);
		Action reset = new Action ("reset", ActionType.COMMAND);
		Action start = new Action ("start", ActionType.COMMAND);
		
		Action ring = new Action ("ring", ActionType.OBSERVATION);
		
		Action tau = new Action ("tau", ActionType.TAU);
		
		Action c0 = new Action ("c=0", ActionType.STATE_OBSERVATION);
		Action c1 = new Action ("c=1", ActionType.STATE_OBSERVATION);
		Action c2 = new Action ("c=2", ActionType.STATE_OBSERVATION);
		
		ExtendedState A = new ExtendedState ("A", c0);
		ExtendedState B = new ExtendedState ("B", c1);
		ExtendedState C = new ExtendedState ("C", c2);
		ExtendedState D = new ExtendedState ("D", c2);
		ExtendedState E = new ExtendedState ("E", c1);
		ExtendedState F = new ExtendedState ("F", c0);
		
		LTS<ExtendedState,Transition> system = new LTS<ExtendedState,Transition>();
		system.addState (A);
		system.addState (B);
		system.addState (C);
		system.addState (D);
		system.addState (E);
		system.addState (F);
		system.addTransition (new Transition (click), A, B);
		system.addTransition (new Transition (reset), B, A);
		system.addTransition (new Transition (click), B, C);
		system.addTransition (new Transition (start), B, E);
		system.addTransition (new Transition (click), C, A);
		system.addTransition (new Transition (reset), C, A);
		system.addTransition (new Transition (start), C, D);
		system.addTauTransition (new Transition (tau), D, E);
		system.addTransition (new Transition (cancel), E, A);
		system.addTauTransition (new Transition (tau), E, F);
		system.addTransition (new Transition (ring), F, A);
		
		return system;
	}
	
	private static LTS<State,ExtendedTransition> mentalExample1()
	{
		Action cancel = new Action ("cancel", ActionType.COMMAND);
		Action click = new Action ("click", ActionType.COMMAND);
		Action reset = new Action ("reset", ActionType.COMMAND);
		Action start = new Action ("start", ActionType.COMMAND);
		
		Action ring = new Action ("ring", ActionType.OBSERVATION);
		
		Action tau = new Action ("tau", ActionType.TAU);
		
		Action c0 = new Action ("c=0", ActionType.STATE_OBSERVATION);
		Action c1 = new Action ("c=1", ActionType.STATE_OBSERVATION);
		Action c2 = new Action ("c=2", ActionType.STATE_OBSERVATION);
		
		State S0 = new State ("S0");
		State S1 = new State ("S1");
		
		LTS<State,ExtendedTransition> mental = new LTS<State,ExtendedTransition>();
		mental.addState (S0);
		mental.addState (S1);
		mental.addTransition (new ExtendedTransition (click, c0), S0, S0);
		mental.addTransition (new ExtendedTransition (click, c1), S0, S0);
		mental.addTransition (new ExtendedTransition (click, c2), S0, S0);
		mental.addTransition (new ExtendedTransition (reset, c1), S0, S0);
		mental.addTransition (new ExtendedTransition (reset, c2), S0, S0);
		mental.addTransition (new ExtendedTransition (start, c1), S0, S1);
		mental.addTransition (new ExtendedTransition (start, c2), S0, S1);
		mental.addTransition (new ExtendedTransition (cancel, c1), S1, S0);
		mental.addTransition (new ExtendedTransition (ring, c0), S1, S0);
		mental.addTransition (new ExtendedTransition (ring, c1), S1, S0);
		mental.addTransition (new ExtendedTransition (ring, c2), S1, S0);
		
		return mental;
	}
	
	private static LTS<ExtendedState,Transition> autopilotExample() throws IOException
	{
		String prefix = "./";
		
		List<Action> transitionActions = new ArrayList<Action>();
		BufferedReader reader = new BufferedReader (new FileReader (prefix + "transitionAction.txt"));
		String line = null;
		while ((line = reader.readLine()) != null)
		{
			transitionActions.add (new Action (line, ActionType.COMMAND));
		}
		reader.close();
		
		List<Action> stateObservationActions = new ArrayList<Action>();
		reader = new BufferedReader (new FileReader (prefix + "stateObservationAction.txt"));
		int nbStates = Integer.parseInt (reader.readLine());
		for (int i = 0; i < nbStates; i++)
		{
			stateObservationActions.add (new Action ("O" + i, ActionType.STATE_OBSERVATION));
		}
		reader.close();
		
		Action tau = new Action ("tau", ActionType.TAU);
		LTS<ExtendedState,Transition> system = new LTS<ExtendedState,Transition>();
		
		List<ExtendedState> states = new ArrayList<ExtendedState>();
		reader = new BufferedReader (new FileReader (prefix + "state.txt"));
		line = null;
		int i = 0;
		while ((line = reader.readLine()) != null)
		{
			states.add (new ExtendedState ("S" + i, stateObservationActions.get (Integer.parseInt (line.split (":")[0]))));
			system.addState (states.get (i));
			i++;
		}
		reader.close();
		
		reader = new BufferedReader (new FileReader (prefix + "transition.txt"));
		line = null;
		while ((line = reader.readLine()) != null)
		{
			String[] tokens = line.split (";");
			if ("tau".equals (tokens[0]))
			{
				system.addTauTransition (new Transition (tau), states.get (Integer.parseInt (tokens[1])), states.get (Integer.parseInt (tokens[2])));
			}
			else
			{
				system.addTransition (new Transition (transitionActions.get (Integer.parseInt (tokens[0]))), states.get (Integer.parseInt (tokens[1])), states.get (Integer.parseInt (tokens[2])));
			}
		}
		reader.close();
		
		return system;
	}
}
