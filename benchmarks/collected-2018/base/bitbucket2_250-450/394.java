// https://searchcode.com/api/result/126461463/

package fri.patterns.interpreter.parsergenerator.lexer;

import java.util.*;
import java.io.IOException;
import fri.patterns.interpreter.parsergenerator.Lexer;
import fri.patterns.interpreter.parsergenerator.Token;
import fri.patterns.interpreter.parsergenerator.syntax.*;
import fri.patterns.interpreter.parsergenerator.syntax.builder.SyntaxSeparation;

/**
	Generates a Lexer from a Syntax. The Syntax can contain also parser rules.
	These will be retrievable (without the removed lexer rules) after build by
	calling <i>lexer.getParserSyntax()</i>.
	<p>
	The syntax rules may not contain '(', ')', '*', '+' or '?', but they may
	contain character set symbols  like ".." (set definitions) and "-" (intersections).
	<p>
	The syntax may contain identifiers enclosed within `backquotes`.
	This marks predefined lexer rules defined in <i>StandardLexerRules</i>.
	That class contains default rules for numbers, identifiers, stringdefinitions,
	characterdefinitions and many other (e.g. for XML), which can be used to build
	lexers.<br>
	<b>CAUTION:</b> Lexer and parser rules have the same namespace, you can not define
	<pre>
		identifier ::= `identifier`;	// wrong!
	</pre>
	Nevertheless you need not to care about the names silently imported from
	<i>StandardLexerRules</i>, they will not reduce the parser syntax namespace,
	only the toplevel rules will.
	<p>
	The syntax may contain (case-sensitive) these nonterminals:
	<ul>
		<li>token</li>
		<li>ignored</li>
	</ul>
	These are lexer-reserved identifiers and can be used to mark top level lexer
	rules (tokens). When <b>token</b> is used, the builder does not try to recognize any
	rule as lexer rule, so this must be good modeled. Be careful: you can read away
	comments only by using <b>ignored</b>. But you can define <b>ignored</b> without <b>token</b>,
	then nevertheless the builder tries to recognize lexer rules.<br>
	When the <b>token</b> marker is not used, the builder tries to separate lexer from
	parser rules.
	<p>
	Example:
	<pre>
		token ::= `identifier` ;	// using StandardLexerRules
		ignored ::= `spaces` ;
		ignored ::= `newline` ;
		ignored ::= comment ;
		comment ::= "//" char_minus_newline_list_opt ;
		char_minus_newline ::= chars - newline;
		char_minus_newline_list ::= char_minus_newline_list char_minus_newline;
		char_minus_newline_list ::= char_minus_newline ;
		char_minus_newline_list_opt ::= char_minus_newline_list;
		char_minus_newline_list_opt ::= ;  // nothing
	</pre>
	Mind that the builder input can not be a text file, it must be wrapped into <i>Syntax</i>.
	Use syntax builder to convert a text into a <i>Syntax</i> object.
	<p>
	Java code fragment:
	<pre>
		SyntaxSeparation separation = new SyntaxSeparation(new Syntax(myRules));
		LexerBuilder builder = new LexerBuilder(separation.getLexerSyntax(), separation.getIgnoredSymbols());
		Lexer lexer = builder.getLexer();
		// when using the lexer standalone (without Parser), you must put the token terminal symbols into it now:
		lexer.setTerminals(separation.getTokenSymbols());
	</pre>
	
	@see fri.patterns.interpreter.parsergenerator.syntax.builder.SyntaxSeparation
	@see fri.patterns.interpreter.parsergenerator.lexer.StandardLexerRules
	@author (c) 2002, Fritz Ritzberger
*/

public class LexerBuilder
{
	protected Map charConsumers;
	protected List ignoredSymbols;
	public static boolean DEBUG;	// defaults to false

	/**
		Creates a LexerBuilder (from lexer rules) that provides a Lexer.
		@param lexerSyntax lexer rule (without token and ignored, use SyntaxSeparation for that)
		@param ignoredSymbols list of ignored symbols, NOT enclosed in backquotes!
	*/
	public LexerBuilder(Syntax lexerSyntax, List ignoredSymbols)
		throws LexerException, SyntaxException
	{
		this.ignoredSymbols = ignoredSymbols;
		build(lexerSyntax);
	}


	/** Returns the built Lexer. */
	public Lexer getLexer()	{
		return new LexerImpl(ignoredSymbols, charConsumers);
	}
		
	/** Returns the built Lexer, loaded with passed input (file, stream, string, ...). */
	public Lexer getLexer(Object input)
		throws IOException
	{
		Lexer lexer = getLexer();
		lexer.setInput(input);
		return lexer;
	}


	private void build(Syntax lexerSyntax)
		throws LexerException, SyntaxException
	{
		SyntaxSeparation.IntArray deleteIndexes = new SyntaxSeparation.IntArray(lexerSyntax.size());
		if (DEBUG)
			System.err.println("Processing lexer rules: \n"+lexerSyntax);

		// resolve scanner rules to Consumers and put it into a hashtable
		this.charConsumers = new Hashtable(lexerSyntax.size());
		for (int i = 0; i < lexerSyntax.size(); i++)
			translateLexerRule(lexerSyntax.getRule(i), i, deleteIndexes);
		deleteIndexes.removeIndexesFrom(lexerSyntax);
		
		// check for unresolved repeatable and nullable rules and delete them from lexer syntax
		for (int i = 0; i < lexerSyntax.size(); i++)	{
			Rule rule = lexerSyntax.getRule(i);
			String nonterm = rule.getNonterminal();
			if (checkNullableRule(nonterm, rule, i, deleteIndexes) == false)
				if (checkRepeatableRule(nonterm, rule, i, deleteIndexes) == false)
					throw new LexerException("Found no character consumer for nullable or repeatable rule "+rule);
		}
		deleteIndexes.removeIndexesFrom(lexerSyntax);
		
		if (lexerSyntax.size() > 0)	{	// not all rules have been resolved to character consumers
			throw new LexerException("Could not process rules in lexer syntax: "+lexerSyntax);
		}

		// resolve all symbolic consumer references after all consumers have been created
		Map done = new Hashtable();	// beware of recursion
		for (Iterator it = charConsumers.entrySet().iterator(); it.hasNext(); )	{
			Consumer cc = (Consumer) ((Map.Entry)it.next()).getValue();
			cc.resolveConsumerReferences(charConsumers, done);
		}
	}


	private void translateLexerRule(Rule rule, int index, SyntaxSeparation.IntArray deleteIndexes)
		throws LexerException
	{
		String nonterm = rule.getNonterminal();
		if (rule.rightSize() <= 0 || rule.getRightSymbol(0).equals(nonterm))	// nullable rules and left recursive rules will be resolved later
			return;
		
		//System.err.println("translating lexer rule: "+rule);

		// ExtendedGrammar should have resolved all parenthesis expressions and wildcards.
		// We take away rules that are:
		// - single character position definitions like
		//     nonterm ::= '0' .. '9'
		//     nonterm ::= 'a' .. 'z' - 'm' .. 'n'
		//     nonterm ::= something - "string"	// "something" must be among scanner rules
		// - single string terminal definitions like
		//     nonterm ::= "string"
		//     nonterm ::= 'c' 'd' 'e' "fgh"	// do concatenation
		// - scanner nonterminals concatenations like
		//     nonterm ::= something1 something2	// "somethingN" must be among scanner rules
		// - scanner nonterminals concatenations like
		//     nonterm ::= "string"

		int CONCATENATION = 0, SET = 1, SUBTRACTION = 2;
		int state = CONCATENATION;
		boolean intersectionHappened = false;
		Consumer consumer = new Consumer(rule);	// master consumer
		Consumer currentConsumer = new Consumer();
		Consumer setConsumer = currentConsumer;	// will host set definitions
		consumer.append(currentConsumer);	// will be resolved when trivial
		
		for (int i = 0; i < rule.rightSize(); i++)	{	// loop all symbols on right side
			String sym = rule.getRightSymbol(i);
			
			if (sym.equals(Token.BUTNOT))	{
				if (i == 0 || state != CONCATENATION)
					throw new LexerException("Missing symbol to subtract from: "+rule);
				state = SUBTRACTION;
			}
			else
			if (sym.equals(Token.UPTO))	{
				if (i == 0 || state != CONCATENATION)
					throw new LexerException("Missing lower limit of set: "+rule);
				state = SET;
			}
			else	{
				String convertedSym = convertSymbol(sym);	// remove quotes or convert number to char
				boolean isNonterm = convertedSym.equals(sym);
				if (isNonterm && state == SET)
					throw new LexerException("Can not append nonterminal to set: "+rule);

				boolean setWillHappen = rule.rightSize() > i + 1 && rule.getRightSymbol(i + 1).equals(Token.UPTO);	// next symbol will be ".."

				if (state == SET)	{
					setConsumer.appendSet(convertedSym);
					setConsumer = currentConsumer;	// reset if intersection happened
				}
				else
				if (state == SUBTRACTION)	{
					intersectionHappened = true;
					if (isNonterm)
						if (setWillHappen)
							throw new LexerException("Nonterminal can not open set after subtraction: "+rule);
						else
							currentConsumer.subtract(new Consumer.Reference(sym));
					else
						if (setWillHappen)
							currentConsumer.subtract(setConsumer = new Consumer(convertedSym));
						else
							currentConsumer.subtract(new Consumer(convertedSym));
				}
				else
				if (state == CONCATENATION)	{
					if (intersectionHappened)	{	// start new consumer
						intersectionHappened = false;
						currentConsumer = new Consumer();
						consumer.append(currentConsumer);
					}
					
					if (isNonterm)
						if (setWillHappen)
							throw new LexerException("Nonterminal can not open set in concatenation: "+rule);
						else
							currentConsumer.append(new Consumer.Reference(sym));
					else
						currentConsumer.append(convertedSym);	// a following set will be recognized by consumer
				}

				state = CONCATENATION;	// reset to normal state
				
			}	// end switch current symbol
		}	// end for right side of rule

		putCharConsumer(nonterm, consumer.optimize());
		deleteIndexes.add(index);
	}


	private void putCharConsumer(String key, Consumer consumer)	{
		//System.err.println("putting character consumer for "+key);
		Object o = charConsumers.get(key);	// test if existing
		
		if (o == null)	{	// not in list
			charConsumers.put(key, consumer);
		}
		else	{
			ConsumerAlternatives ca;

			if (o instanceof ConsumerAlternatives == false)	{
				ca = new ConsumerAlternatives((Consumer)o);
				charConsumers.put(key, ca);	// replace consumer
			}
			else	{
				ca = (ConsumerAlternatives)o;
			}
			
			ca.addAlternate(consumer);	// add a new alternative
		}
	}



	private boolean checkNullableRule(String nonterm, Rule rule, int index, SyntaxSeparation.IntArray deleteIndexes)	{
		// We take away rules that are optional nonterminals like
		//     nonterm ::= something	// "nonterm" is already among scanner rules
		//     nonterm ::= /*nothing*/	// this is the rule to remove now

		if (rule.rightSize() <= 0)	{
			Object o = charConsumers.get(nonterm);
			((Consumer)o).setNullable();
			deleteIndexes.add(index);
			return true;	// do not explore empty rule, return "found nullable"
		}
		return false;
	}


	private boolean checkRepeatableRule(String nonterm, Rule rule, int index, SyntaxSeparation.IntArray deleteIndexes)	{
		// We take away rules that are left recursive like
		//     nonterm ::= nonterm something		// this is the rule to remove now
		//     nonterm ::= something	// "nonterm" must be already among scanner rules

		// check for nonterm in hashtable, set it repeatable if found
		if (rule.rightSize() >= 2 && rule.getRightSymbol(0).equals(nonterm))	{	// left recursive
			Consumer cc = (Consumer) charConsumers.get(nonterm);
			if (cc.matchesRepeatableRule(rule))	{	// check if rest on the right is same
				cc.setRepeatable();
				deleteIndexes.add(index);
				return true;
			}
		}
		return false;
	}

	
	/**
		Converts a character or string definition to its processable form.
		This implementation must be according to "bnf_chardef" in <i>StandardLexerRules</i>.
		<ul>
			<li>0xFFFF   hexadecimal, convert to character</li>
			<li>0777     octal, convert to character</li>
			<li>12345    decimal, convert to character</li>
			<li>'c'      character, remove quotes</li>
			<li>'\n', "\n"      escaped character, decode</li>
			<li>"string"      string, remove quotes</li>
			<li>nonterminal - will stay unchanged</li>
		</ul>
	*/
	private String convertSymbol(String sym)	{
		if (sym.charAt(0) == '\'' || sym.charAt(0) == '"')	{
			String s = sym.substring(1, sym.length() - 1);
			if (s.length() <= 0)
				throw new IllegalArgumentException("Empty character or string definition: "+sym);

			StringBuffer sb = new StringBuffer(s.length());	// convert escape sequences to their real meaning
			for (int i = 0; i < s.length(); i++)	{
				char c = s.charAt(i);
				if (c == '\\')	{
					char c1 = s.length() > i + 1 ? s.charAt(i + 1) : 0;
					switch (c1)	{
						case 'n': sb.append('\n'); i++; break;
						case 'r': sb.append('\r'); i++; break;
						case 't': sb.append('\t'); i++; break;
						case 'f': sb.append('\f'); i++; break;
						case 'b': sb.append('\b'); i++; break;
						case '\'': sb.append('\''); i++; break;
						case '"': sb.append('"'); i++; break;
						case '\\': sb.append('\\'); i++; break;
						default: sb.append(c); break;
					}
				}
				else	{
					sb.append(c);
				}
			}
			return sb.toString();
		}
		else	{	// must be starting with digit or be a nonterminal
			char c;
			if (sym.startsWith("0x") || sym.startsWith("0X"))	// hexadecimal number
				c = (char) Integer.valueOf(sym.substring(2), 16).intValue();
			else
			if (sym.startsWith("0"))	// octal number
				c = (char) Integer.valueOf(sym.substring(1), 8).intValue();
			else
			if (Character.isDigit(sym.charAt(0)))
				c = (char) Integer.valueOf(sym).intValue();	// will throw NumberFormatException when not number
			else
				return sym;	// is a nonterminal
			
			return new String(new char [] { c });
		}
	}
	
}

