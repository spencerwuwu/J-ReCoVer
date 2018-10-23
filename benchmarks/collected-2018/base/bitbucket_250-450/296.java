// https://searchcode.com/api/result/129122563/

package hv.parser;

import hv.util.HVAssertionError;
import hv.util.MyArrays;
import hv.util.MyLogger;
import hv.util.MyLoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

/**
 * A generic LALR parser generator.
 * 
 * @author hans
 * 
 */
public class Grammar {
	private static MyLogger log = MyLoggerFactory.getLogger(Grammar.class);
	private static MyLogger logGrammar = MyLoggerFactory
			.getLogger(Grammar.class.getName() + ".grammar");
	private ArrayList<State> allStates = null;
	private Terminal endOfFile;
	private final Map<Symbol, Set<Terminal>> firstSet = new HashMap<Symbol, Set<Terminal>>();
	private ItemCache itemCache;
	private final Set<NonTerminal> nonterminals = new HashSet<NonTerminal>();
	private final NonTerminal ntStart;
	private Set<NonTerminal> nullable;
	private int productionCount = 0;
	private Map<Object, State> stateForCore = new HashMap<Object, State>();
	private Set<Terminal> terminals = null;

	public Grammar() {
		ntStart = nonterminal("START");
	}

	public Production add(NonTerminal head, Action action, Object[] body) {
		Symbol[] s = new Symbol[body.length];
		for (int i = 0; i < body.length; i++)
			s[i] = toSymbol(body[i]);
		Production p = new Production(head, s, action, productionCount);
		productionCount++;
		head.productions.add(p);
		log.debug("Add production {}:{}", productionCount, p);
		return p;
	}

	private short addMapping(CompiledGrammar compiled,
			SortedMap<Integer, Integer> values) {
		// convert the map<a,b> to an array a1 b1 a2 b2 a3 b3 ... 0
		short[] required = new short[values.size() * 2 + 1];
		int r = 0;
		for (Entry<Integer, Integer> e : values.entrySet()) {
			if (e.getKey() == Integer.MAX_VALUE)
				throw new HVAssertionError("0 value in ", e);
			required[r++] = e.getKey().shortValue();
			required[r++] = e.getValue().shortValue();
		}
		required[r] = Short.MAX_VALUE;
		// find or make an index in compiled.mapping which has a copy of
		// 'required'
		final short mapLen = (short) compiled.mapping.length;
		final int reqLen = required.length;
		for (short i = 0; i < mapLen - reqLen + 1; i++) {
			boolean found = true;
			for (int j = 0; j < reqLen; j++)
				if (compiled.mapping[i + j] != required[j]) {
					found = false;
					break;
				}
			if (found)
				return i;
		}
		compiled.mapping = MyArrays.copyOf(compiled.mapping, mapLen + reqLen);
		System.arraycopy(required, 0, compiled.mapping, mapLen, reqLen);
		return mapLen;
	}

	private boolean addToResultSet(State result, List<Item> todo, Production p,
			Symbol s) {
		for (Terminal t : firstSet.get(s)) {
			// add a new item B->.C,t
			Item ni = itemCache.create(p, 0, t);
			if (result.add(ni))
				todo.add(ni);
		}
		return !nullable.contains(s);
	}

	private void buildRuleInfo(CompiledGrammar compiled) {
		compiled.ruleAction = new Action[productionCount];
		compiled.ruleBodyLength = new int[productionCount];
		for (NonTerminal n : nonterminals)
			for (Production p : n.productions) {
				if (compiled.ruleAction[p.index] != null)
					throw new HVAssertionError("Double action", p);
				compiled.ruleAction[p.index] = p.action;
				compiled.ruleBodyLength[p.index] = p.body.length;
			}
	}

	private void calculateActions(CompiledGrammar compiled) {
		compiled.actionIndex = new short[allStates.size()];
		for (int i = 0; i < allStates.size(); i++) {
			SortedMap<Integer, Integer> actions = new TreeMap<Integer, Integer>();
			State state = allStates.get(i);
			for (Item j : state.items())
				if (j.atEnd()) {
					int reduce = -1 - j.prod.index;
					Integer id = j.lookahead.getIndex();
					setAction(actions, id, reduce, i, j.lookahead, j.prod);
				} else {
					Symbol s = j.prod.body[j.pos];
					if (s instanceof Terminal) {
						Terminal t = (Terminal) s;
						State target = state.gotos.get(s);
						int shift = allStates.indexOf(target);
						if (shift < 0)
							throw new HVAssertionError("Can't shift to state",
									allStates.get(i), t, target);
						Integer id = t.getIndex();
						setAction(actions, id, shift, i, t, null);
					}
				}
			log.debug("Actions {}:{}", i, actions);
			compiled.actionIndex[i] = addMapping(compiled, actions);
		}
	}

	void calculateFirst() {
		// Every Terminal is its own first set
		for (Terminal s : terminals)
			firstSet.put(s, Collections.singleton(s));
		// Every nonterminal starts with empty set
		for (NonTerminal n : nonterminals)
			firstSet.put(n, new HashSet<Terminal>());
		// iterate until all targets stop modifying
		for (boolean modified = true; modified;) {
			modified = false;
			for (NonTerminal head : nonterminals) {
				Set<Terminal> target = firstSet.get(head);
				for (Production p : head.productions)
					for (Symbol s : p.body) {
						Set<Terminal> f = firstSet.get(s);
						if (!target.containsAll(f)) {
							target.addAll(f);
							modified = true;
						}
						if (!nullable.contains(s))
							break;
					}
			}
		}
	}

	private void calculateGoto(Stack<State> todo, State in, Symbol s) {
		State result = null;
		for (Item i : in.items())
			if (!i.atEnd() && i.prod.body[i.pos] == s) {
				if (result == null)
					result = new State();
				result.add(itemCache.advance(i));
			}
		if (result == null)
			return;
		closure(result);
		// Check if an existing result is identical
		State state = stateForCore.get(result.getCore());
		if (state != null) {
			// state with same core as existing state -> merge
			boolean hasChanged = false;
			for (Item i : result.items())
				hasChanged |= state.add(i);
			if (hasChanged)
				todo.push(state);
			in.gotos.put(s, state);
		} else {
			// completely new state
			stateForCore.put(result.getCore(), result);
			result.example = in.example + " " + s;
			in.gotos.put(s, result);
			allStates.add(result);
			todo.push(result);
			log.debug("State {}:{}", allStates.size() - 1, result);
		}
	}

	private void calculateGotos(CompiledGrammar compiled) {
		compiled.gotoIndex = new short[allStates.size()];
		for (int i = 0; i < allStates.size(); i++) {
			SortedMap<Integer, Integer> gotos = new TreeMap<Integer, Integer>();
			for (Entry<Symbol, State> e : allStates.get(i).gotos.entrySet())
				if (e.getKey() instanceof NonTerminal) {
					NonTerminal symbol = (NonTerminal) e.getKey();
					int stateIndex = allStates.indexOf(e.getValue());
					for (Production p : symbol.productions)
						gotos.put(-1 - p.index, stateIndex);
				}
			log.debug("goto({})={}", i, gotos);
			compiled.gotoIndex[i] = addMapping(compiled, gotos);
		}
		log.debug("Symtab is {}:{}", compiled.mapping.length,
				Arrays.toString(compiled.mapping));
	}

	void calculateNullable() {
		nullable = new HashSet<NonTerminal>();
		boolean changed;
		do {
			changed = false;
			for (NonTerminal n : nonterminals) {
				if (isNullable(n))
					continue;
				for (Production p : n.productions) {
					// A production is nullable if all terms are nullable
					boolean isNullable = true;
					for (Symbol s : p.body)
						if (!nullable.contains(s)) {
							isNullable = false;
							break;
						}
					if (isNullable) {
						changed = true;
						nullable.add(p.head);
						break;
					}
				}
			}
		} while (changed);
	}

	void calculateStates() {
		Stack<State> todo = new Stack<State>();
		allStates = new ArrayList<State>();
		// start with closure(item(realHead->startsymbol,EOF))
		State initialSet = makeInitialState();
		todo.add(initialSet);
		allStates.add(initialSet);
		log.debug("initial set:{}", initialSet);
		Set<Symbol> allSymbols = new HashSet<Symbol>();
		allSymbols.addAll(terminals);
		allSymbols.addAll(nonterminals);
		while (!todo.isEmpty()) {
			State in = todo.pop();
			for (Symbol s : allSymbols)
				calculateGoto(todo, in, s);
		}
	}

	void closure(State result) {
		// for each item of the form I->a.Bb,l in result
		List<Item> todo = new LinkedList<Item>(result.items());
		while (!todo.isEmpty()) {
			Item i = todo.remove(0);
			if (i.pos >= i.prod.body.length)
				continue;
			// for each Item kernel B->.C
			Symbol head = i.prod.body[i.pos];
			if (!(head instanceof NonTerminal))
				continue;
			NonTerminal ntHead = (NonTerminal) head;
			for (Production p : ntHead.productions) {
				// for each terminal t in first(bl)
				boolean done = false;
				for (int j = i.pos + 1; j < i.prod.body.length; j++) {
					Symbol s = i.prod.body[j];
					if (addToResultSet(result, todo, p, s)) {
						done = true;
						break;
					}
				}
				if (!done)
					addToResultSet(result, todo, p, i.lookahead);
			}
		}
	}

	public CompiledGrammar compile() {
		log.debug("Grammar.compile()");
		itemCache = new ItemCache(productionCount);
		calculateNullable();
		calculateFirst();
		calculateStates();
		CompiledGrammar compiled = new CompiledGrammar();
		calculateActions(compiled);
		setTerminalNames(compiled);
		calculateGotos(compiled);
		buildRuleInfo(compiled);
		return compiled;
	}

	Set<Terminal> first(List<Symbol> in) {
		HashSet<Terminal> result = new HashSet<Terminal>();
		for (Symbol s : in) {
			result.addAll(firstSet.get(s));
			if (!nullable.contains(s))
				break;
		}
		return result;
	}

	public NonTerminal getStartSymbol() {
		return ntStart;
	}

	boolean isNullable(Symbol s) {
		return nullable.contains(s);
	}

	// Create an item start-real: . start,EOF
	private State makeInitialState() {
		NonTerminal realHead = nonterminal("start-real");
		Production initialProd = realHead.rule(ntStart, new AcceptAction());
		Item initialItem = itemCache.create(initialProd, 0, endOfFile);
		State initialState = new State();
		initialState.add(initialItem);
		closure(initialState);
		return initialState;
	}

	/**
	 * Create a new nonterminal.
	 * 
	 * @param name
	 * @return
	 */
	public NonTerminal nonterminal(String name) {
		for (NonTerminal nt : nonterminals)
			if (nt.getName().equals(name))
				throw new HVAssertionError("Redeclaration of nonterminal", name);
		NonTerminal nt = new NonTerminal(this, name);
		nonterminals.add(nt);
		return nt;
	}

	public void setAction(Map<Integer, Integer> actions, Integer id,
			Integer action, int stateNum, Terminal t, Production rule) {
		Integer lastAction = actions.put(id, action);
		if (!action.equals(lastAction)) {
			if (action > 0) {
				logGrammar.debug(stateNum + " on  {} :shift {}", t, action);
			} else {
				logGrammar.debug(stateNum + " on {} :reduce {}", t, rule);
			}
		}
		if (lastAction != null && !action.equals(lastAction)) {
			throw new GrammarException("CONFLICT! " + action + " vs "
					+ lastAction + ",state " + allStates.get(stateNum));
		}
	}

	public void setEndOfFile(Terminal endOfFile) {
		this.endOfFile = endOfFile;
	}

	private void setTerminalNames(CompiledGrammar compiled) {
		List<String> result = new ArrayList<String>();
		for (Terminal t : terminals) {
			int index = t.getIndex();
			while (result.size() <= index)
				result.add("??BAD??");
			result.set(index, t.getName());
		}
		compiled.terminalNames = result.toArray(new String[result.size()]);
	}

	public void setTerminals(Set<Terminal> terminals) {
		this.terminals = terminals;
	}

	/**
	 * Convert to symbol: The input can be a Symbol or a String naming a
	 * Terminal from terminals
	 * 
	 * If a.equals(b) then toSymbol(a)==toSymbol(b)
	 * 
	 * @param o
	 * @return
	 */
	public Symbol toSymbol(Object o) {
		if (o instanceof Symbol)
			return (Symbol) o;
		for (Terminal t : terminals)
			if (t.getName().equals(o.toString()))
				return t;
		throw new HVAssertionError("No terminal ", o);
	}
}

