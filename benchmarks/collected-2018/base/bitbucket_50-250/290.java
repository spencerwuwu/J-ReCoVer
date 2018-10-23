// https://searchcode.com/api/result/129122560/

package hv.parser;

import hv.TextPosition;
import hv.ast.ASTFixedData;
import hv.message.HVErrorMessageException;
import hv.message.MSGGen;
import hv.util.HVAssertionError;
import hv.util.MyArrays;
import hv.util.MyLogger;
import hv.util.MyLoggerFactory;

/**
 * State of a compiled parser. This class contains all input-dependent state.
 * 
 * @author hans
 * 
 */
public class Parser {
	private final static MyLogger log = MyLoggerFactory.getLogger(Parser.class);
	boolean accepted = false;
	private final CompiledGrammar grammar;
	private Node[] objectStack = new Node[1];
	private int sp; // first free element
	private int[] stateStack = new int[objectStack.length];

	public Parser(CompiledGrammar grammar) {
		this.grammar = grammar;
		sp = 0;
		stateStack[sp] = 0;
		objectStack[sp] = new ASTFixedData(null, Location.DUMMY);
		sp++;
	}

	private int findAction(Terminal t, int state, int id)
			throws HVErrorMessageException {
		for (int i = grammar.actionIndex[state]; grammar.mapping[i] != Short.MAX_VALUE; i += 2)
			if (grammar.mapping[i] == id)
				return grammar.mapping[i + 1];
		// Not found. Find out possible actions
		StringBuffer s = new StringBuffer();
		for (int j = grammar.actionIndex[state]; grammar.mapping[j] != Short.MAX_VALUE; j += 2) {
			if (s.length() != 0)
				s.append(',');
			s.append(grammar.terminalNames[grammar.mapping[j]]);
		}
		throw new HVErrorMessageException(MSGGen.eParser(t, s.toString()));
	}

	public Object getResult() {
		if (sp != 1)
			throw new HVAssertionError("Bad stack size ", sp);
		return objectStack[1];
	}

	private void grow() {
		if (sp >= stateStack.length) {
			stateStack = MyArrays.copyOf(stateStack, sp * 2);
			objectStack = MyArrays.copyOf(objectStack, sp * 2);
		}
	}

	/**
	 * @return true if finished with valid input
	 */
	public boolean isAccepted() {
		return accepted;
	}

	private void reduce(int next, TextPosition pos)
			throws HVErrorMessageException {
		int r = -next - 1;
		int body = grammar.ruleBodyLength[r];
		final Action action = grammar.ruleAction[r];
		if (log.isDebugEnabled()) {
			final String actionName = action.getClass().getSimpleName();
			log.debug("reduce({},{},{})", new Object[] { Integer.valueOf(next),
					actionName, Integer.valueOf(body) });
		}
		sp = sp - body;
		if (body > 0) {
			Node tp1 = objectStack[sp];
			Node tp2 = objectStack[sp + body - 1];
			TextPosition start = tp1.getLocation().getStart();
			TextPosition end = tp2.getLocation().getEnd();
			action.reduce(this, objectStack, sp, new Location(start, end));
		} else {
			grow();
			action.reduce(this, objectStack, sp, new Location(pos, pos));
		}
		if (objectStack[sp] == null)
			throw new HVAssertionError("reduce to null!");
		if (isAccepted())
			return;
		int top = stateStack[sp - 1];
		stateStack[sp] = grammar.findSym(grammar.gotoIndex[top], -1 - r);
		sp++;
	}

	/**
	 * shift the input on the stack
	 * 
	 * @param t
	 *            input token
	 * @param next
	 *            next state
	 */
	private void shift(Terminal t, int next) {
		log.debug("shift {} ", next);
		grow();
		if (t == null)
			throw new HVAssertionError("shifting null!");
		stateStack[sp] = next;
		objectStack[sp] = t;
		sp++;
	}

	public void step(Terminal t) throws HVErrorMessageException {
		log.debug("input {}", t);
		int id = t.getIndex();
		for (;;) {
			if (log.isDebugEnabled()) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < sp; i++) {
					sb.append(stateStack[i]);
					sb.append(':');
					sb.append(objectStack[i]);
					sb.append(' ');
				}
				log.debug("{}", sb.toString());
			}
			int state = stateStack[sp - 1];
			int next = findAction(t, state, id);
			if (next == 0)
				throw new HVErrorMessageException(MSGGen.eParser(t, "???"));
			else if (next > 0) {
				// shift
				shift(t, next);
				return;
			} else {
				// reduce
				reduce(next, t.getLocation().getStart());
				if (isAccepted())
					return;
			}
		}
	}
}

