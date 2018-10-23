// https://searchcode.com/api/result/11597313/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 
 * @author Peter Norvig, peter@norvig.com http://www.norvig.com 
 * Copyright 1998 Peter Norvig, see http://www.norvig.com/license.html
 **/

class scheme extends RuntimeException {

	  //////////////// Main Loop

	  /** Create a new Scheme interpreter, passing in the command line args
	   * as files to load, and then enter a read eval write loop. **/
	  public static void main(String[] files) {
		  
		  scheme scheme = Scheme(files);
		  scheme.readEvalWriteLoop();
		  
	  }
	
	static  String EOF = "#!EOF";
	final static byte Scheme = 0;
	final static byte Procedure = 1;
	final static byte Closure = 2;
	final static byte Continuation = 3;
	final static byte Macros = 4;
	final static byte JavaMethod = 5;
	final static byte Primitive = 6;
	final static byte InputPort = 7;
	final static byte Pair = 8;
	final static byte Environment = 9;

	final static byte[] _sizes = new byte[] { 3, -1, 4, 3, 4, 4, 4, 6, 2, 4 };
	final static String[] _names = new String[] { "Scheme", "Procedure",
			"Closure", "Continuation", "Macros", "JavaMethod", "Primitive",
			"InputPort", "Pair", "Environment" };

	byte _ = -1;
	Object[] $ = null;
	
	public synchronized Throwable fillInStackTrace() {
		return null;
	};

	scheme(byte type, Object[] data) {
		try {
			_ = type;
			$ = new Object[_sizes[type]];
			if (data != null)
				for (int i =0; i < data.length; i++) {
					$[i] = data[i];
				}
		} catch (Exception e) {
			fatal("Cannot create " + _name(type), e);
		}
	}

	String _name(byte type) {
		if (type < 0 || type >= _names.length)
			return "Unknown" + type;
		else
			return _names[type];
	}

	void fatal(String message, Object object) {
		System.err.println("**** Fatal error: " + message);
		System.err.println("cause " + object);
		System.exit(-1);
	}

	public String toString() { 
		switch(_) {
		case Closure:
		case Macros:
		case Continuation:
		case JavaMethod:
		case Primitive:
			return "{" + name() + "}";
		case Pair:
			return stringify(this, true);
		default:
			return _name(_);
		}
	}
	
	static boolean is(Object obj, byte type) {

		if (obj instanceof scheme) {
			if (type == Procedure) return 
				((scheme)obj)._ == Closure || ((scheme)obj)._ == Continuation || ((scheme)obj)._ == Macros ||
				((scheme)obj)._ == Primitive || ((scheme)obj)._ == JavaMethod;
			else if (type == Closure) return ((scheme)obj)._ == Closure || ((scheme)obj)._ == Macros;
			else return ((scheme)obj)._ == type;
		}else return false;
	}

	
	/// *********************************** Procedure *********************************
	
    String name() {
    	if ( !is(this, Procedure)) fatal("Expected " + _name(_), _name(_) );
    	return (String) $[0];
    }

    String name(String value) {
    	if ( !is(this, Procedure) ) fatal("Expected " + _name(_), _name(_) );
    	return (String) ($[0] = value);
    }
	
	final static String anonymous = "anonymous procedure";

	Object apply(scheme interp, Object args) {
		switch(_) {
		case Closure:
		case Macros:
			return interp.eval(body(), Environment(parms(), args, env()));
		case JavaMethod:
			try {
		        if (isStatic()) return method().invoke(null, toArray(args));
		        else return method().invoke(first(args), toArray(rest(args)));
		      } catch (IllegalAccessException e) { ; }
		      catch (IllegalArgumentException e) { ; }
		      catch (InvocationTargetException e) { ; }
		      catch (NullPointerException e) { ; }
		      return error("Bad Java Method application:" + this 
		  			+ stringify(args) + ", "); 
		case Primitive:
		      //First make sure there are the right number of arguments. 
		      int nArgs = length(args);
		      if (nArgs < minArgs()) 
			return error("too few args, " + nArgs +
				     ", for " + this.name() + ": " + args);
		      else if (nArgs > maxArgs())
			return error("too many args, " + nArgs +
				     ", for " + this.name() + ": " + args);

		    Object x = first(args);
		    Object y = second(args);

		    switch (idNumber()) {

		      ////////////////  SECTION 6.1 BOOLEANS
		    case NOT:       	return truth(x == FALSE);
		    case BOOLEANQ:  	return truth(x == TRUE || x == FALSE);

		      ////////////////  SECTION 6.2 EQUIVALENCE PREDICATES
		    case EQVQ: 		return truth(eqv(x, y));
		    case EQQ: 		return truth(x == y);
		    case EQUALQ:  	return truth(equal(x,y));

		      ////////////////  SECTION 6.3 LISTS AND PAIRS
		    case PAIRQ:  	return truth( is( x, Pair));
		    case LISTQ:         return truth(isList(x));
		    case CXR:           for (int i = name().length()-2; i >= 1; i--) 
		                          x = (name().charAt(i) == 'a') ? first(x) : rest(x);
		                        return x;
		    case CONS:  	return cons(x, y);
		    case CAR:  	        return first(x);
		    case CDR:  	        return rest(x);
		    case SETCAR:        return setFirst(x, y);
		    case SETCDR:        return setRest(x, y);
		    case SECOND:  	return second(x);
		    case THIRD:  	return third(x);
		    case NULLQ:         return truth(x == null);
		    case LIST:  	return args;
		    case LENGTH:  	return num(length(x));
		    case APPEND:        return (args == null) ? null : append(args);
		    case REVERSE:       return reverse(x);
		    case LISTTAIL: 	for (int k = (int)num(y); k>0; k--) x = rest(x);
		                        return x;
		    case LISTREF:  	for (int k = (int)num(y); k>0; k--) x = rest(x);
		                        return first(x);
		    case MEMQ:      	return memberAssoc(x, y, 'm', 'q');
		    case MEMV:      	return memberAssoc(x, y, 'm', 'v');
		    case MEMBER:    	return memberAssoc(x, y, 'm', ' ');
		    case ASSQ:      	return memberAssoc(x, y, 'a', 'q');
		    case ASSV:      	return memberAssoc(x, y, 'a', 'v');
		    case ASSOC:     	return memberAssoc(x, y, 'a', ' ');

		      ////////////////  SECTION 6.4 SYMBOLS
		    case SYMBOLQ:  	return truth(x instanceof String);
		    case SYMBOLTOSTRING:return sym(x).toCharArray();
		    case STRINGTOSYMBOL:return new String(str(x)).intern();

		      ////////////////  SECTION 6.5 NUMBERS
		    case NUMBERQ:  	return truth(x instanceof Number);
		    case ODDQ:          return truth(Math.abs(num(x)) % 2 != 0);
		    case EVENQ:         return truth(Math.abs(num(x)) % 2 == 0);
		    case ZEROQ:         return truth(num(x) == 0);
		    case POSITIVEQ:     return truth(num(x) > 0);
		    case NEGATIVEQ:     return truth(num(x) < 0);
		    case INTEGERQ:      return truth(isExact(x));
		    case INEXACTQ:      return truth(!isExact(x));
		    case LT:		return numCompare(args, '<');
		    case GT:		return numCompare(args, '>');
		    case EQ:		return numCompare(args, '=');
		    case LE: 		return numCompare(args, 'L');
		    case GE: 		return numCompare(args, 'G');
		    case MAX: 		return numCompute(args, 'X', num(x));
		    case MIN: 		return numCompute(args, 'N', num(x));
		    case PLUS:		return numCompute(args, '+', 0.0);
		    case MINUS:		return numCompute(rest(args), '-', num(x));
		    case TIMES:		return numCompute(args, '*', 1.0);
		    case DIVIDE:	return numCompute(rest(args), '/', num(x));
		    case QUOTIENT:      double d = num(x)/num(y);
		                        return num(d > 0 ? Math.floor(d) : Math.ceil(d));
		    case REMAINDER:     return num((long)num(x) % (long)num(y));
		    case MODULO:        long xi = (long)num(x), yi = (long)num(y), m = xi % yi;
		                        return num((xi*yi > 0 || m == 0) ? m : m + yi);
		    case ABS: 		return num(Math.abs(num(x)));
		    case FLOOR: 	return num(Math.floor(num(x)));
		    case CEILING: 	return num(Math.ceil(num(x))); 
		    case TRUNCATE: 	d = num(x);
		      	                return num((d < 0.0) ? Math.ceil(d) : Math.floor(d)); 
		    case ROUND: 	return num(Math.round(num(x)));
		    case EXP:           return num(Math.exp(num(x)));
		    case LOG:           return num(Math.log(num(x)));
		    case SIN:           return num(Math.sin(num(x)));
		    case COS:           return num(Math.cos(num(x)));
		    case TAN:           return num(Math.tan(num(x)));
		    case ASIN:          return num(Math.asin(num(x)));
		    case ACOS:          return num(Math.acos(num(x)));
		    case ATAN:          return num(Math.atan(num(x)));
		    case SQRT:      	return num(Math.sqrt(num(x)));
		    case EXPT:      	return num(Math.pow(num(x), num(y)));
		    case NUMBERTOSTRING:return numberToString(x, y);
		    case STRINGTONUMBER:return stringToNumber(x, y);
		    case GCD:           return (args == null) ? ZERO : gcd(args);
		    case LCM:           return (args == null) ? ONE  : lcm(args);
		                        
		      ////////////////  SECTION 6.6 CHARACTERS
		    case CHARQ:           return truth(x instanceof Character);
		    case CHARALPHABETICQ: return truth(Character.isLetter(chr(x)));
		    case CHARNUMERICQ:    return truth(Character.isDigit(chr(x)));
		    case CHARWHITESPACEQ: return truth(Character.isWhitespace(chr(x)));
		    case CHARUPPERCASEQ:  return truth(Character.isUpperCase(chr(x)));
		    case CHARLOWERCASEQ:  return truth(Character.isLowerCase(chr(x)));
		    case CHARTOINTEGER:   return new Double((double)chr(x));
		    case INTEGERTOCHAR:   return chr((char)(int)num(x));
		    case CHARUPCASE:      return chr(Character.toUpperCase(chr(x)));
		    case CHARDOWNCASE:    return chr(Character.toLowerCase(chr(x)));
		    case CHARCMP+EQ:      return truth(charCompare(x, y, false) == 0);
		    case CHARCMP+LT:      return truth(charCompare(x, y, false) <  0);
		    case CHARCMP+GT:      return truth(charCompare(x, y, false) >  0);
		    case CHARCMP+GE:      return truth(charCompare(x, y, false) >= 0);
		    case CHARCMP+LE:      return truth(charCompare(x, y, false) <= 0);
		    case CHARCICMP+EQ:    return truth(charCompare(x, y, true)  == 0);
		    case CHARCICMP+LT:    return truth(charCompare(x, y, true)  <  0);
		    case CHARCICMP+GT:    return truth(charCompare(x, y, true)  >  0);
		    case CHARCICMP+GE:    return truth(charCompare(x, y, true)  >= 0);
		    case CHARCICMP+LE:    return truth(charCompare(x, y, true)  <= 0);

		    case ERROR:         return error(stringify(args));

		      ////////////////  SECTION 6.7 STRINGS
		    case STRINGQ:   	return truth(x instanceof char[]);
		    case MAKESTRING:char[] str = new char[(int)num(x)];
		      if (y != null) {
			char c = chr(y);
			for (int i = str.length-1; i >= 0; i--) str[i] = c;
		      }
		      return str;
		    case STRING:    	return listToString(args);
		    case STRINGLENGTH: 	return num(str(x).length);
		    case STRINGREF: 	return chr(str(x)[(int)num(y)]);
		    case STRINGSET: 	Object z = third(args); str(x)[(int)num(y)] = chr(z); 
		                        return z;
		    case SUBSTRING: 	int start = (int)num(y), end = (int)num(third(args));
		                        return new String(str(x), start, end-start).toCharArray();
		    case STRINGAPPEND: 	return stringAppend(args);
		    case STRINGTOLIST:  scheme result = null;
		                        char[] str2 = str(x);
					for (int i = str2.length-1; i >= 0; i--)
					  result = cons(chr(str2[i]), result);
					return result;
		    case LISTTOSTRING:  return listToString(x);
		    case STRINGCMP+EQ:  return truth(stringCompare(x, y, false) == 0);
		    case STRINGCMP+LT:  return truth(stringCompare(x, y, false) <  0);
		    case STRINGCMP+GT:  return truth(stringCompare(x, y, false) >  0);
		    case STRINGCMP+GE:  return truth(stringCompare(x, y, false) >= 0);
		    case STRINGCMP+LE:  return truth(stringCompare(x, y, false) <= 0);
		    case STRINGCICMP+EQ:return truth(stringCompare(x, y, true)  == 0);
		    case STRINGCICMP+LT:return truth(stringCompare(x, y, true)  <  0);
		    case STRINGCICMP+GT:return truth(stringCompare(x, y, true)  >  0);
		    case STRINGCICMP+GE:return truth(stringCompare(x, y, true)  >= 0);
		    case STRINGCICMP+LE:return truth(stringCompare(x, y, true)  <= 0);

		      ////////////////  SECTION 6.8 VECTORS
		    case VECTORQ:	return truth(x instanceof Object[]);
		    case MAKEVECTOR:    Object[] vec = new Object[(int)num(x)];
		                        if (y != null) {
					  for (int i = 0; i < vec.length; i++) vec[i] = y;
					}
					return vec;
		    case VECTOR:        return listToVector(args);
		    case VECTORLENGTH:  return num(vec(x).length);
		    case VECTORREF:	return vec(x)[(int)num(y)];
		    case VECTORSET:     return vec(x)[(int)num(y)] = third(args);
		    case VECTORTOLIST:  return vectorToList(x);
		    case LISTTOVECTOR:  return listToVector(x);

		      ////////////////  SECTION 6.9 CONTROL FEATURES
		    case EVAL:          return interp.eval(x);
		    case FORCE:         return !is(x,Procedure) ? x
					  : proc(x).apply(interp, null);
		    case MACROEXPAND:   return macroExpand(interp, x);
		    case PROCEDUREQ:	return Boolean.valueOf( is(x,Procedure) );
		    case APPLY:  	return proc(x).apply(interp, listStar(rest(args)));
		    case MAP:           return map(proc(x), rest(args), interp, list(null));
		    case FOREACH:       return map(proc(x), rest(args), interp, null);
		    case CALLCC:        RuntimeException cc = new RuntimeException();
		                        scheme proc = Continuation(cc);
			                try { return proc(x).apply(interp, list(proc)); }
					catch (RuntimeException e) { 
					    if (e == cc) return proc.value(); else throw e; 
					}

		      ////////////////  SECTION 6.10 INPUT AND OUPUT
		    case EOFOBJECTQ:         return truth(x == EOF);
		    case INPUTPORTQ:         return truth(is( x,InputPort) );
		    case CURRENTINPUTPORT:   return interp.getInput();
		    case OPENINPUTFILE:      return openInputFile(x);
		    case CLOSEINPUTPORT:     return inPort(x, interp).close(); 
		    case OUTPUTPORTQ:        return truth(x instanceof PrintWriter);
		    case CURRENTOUTPUTPORT:  return interp.getOutput();
		    case OPENOUTPUTFILE:     return openOutputFile(x);
		    case CALLWITHOUTPUTFILE: PrintWriter p = null;
		                             try { p = openOutputFile(x);
		                                   z = proc(y).apply(interp, list(p));
		                             } finally { if (p != null) p.close(); }
		                             return z;
		    case CALLWITHINPUTFILE:  scheme p2 = null;
		                             try { p2 = openInputFile(x);
		                                   z = proc(y).apply(interp, list(p2));
		                             } finally { if (p2 != null) p2.close(); }
		                             return z;
		    case CLOSEOUTPUTPORT:    outPort(x, interp).close(); return TRUE; 
		    case READCHAR:      return inPort(x, interp).readChar();
		    case PEEKCHAR:      return inPort(x, interp).peekChar();
		    case LOAD:          return interp.load(x);
		    case READ:  	return inPort(x, interp).read(); 
		    case EOF_OBJECT:    return truth(isEOF(x));
		    case WRITE:  	return write(x, outPort(y, interp), true);
		    case DISPLAY:       return write(x, outPort(y, interp), false);
		    case NEWLINE:  	outPort(x, interp).println();
		                        outPort(x, interp).flush(); return TRUE;

		      ////////////////  EXTENSIONS
		    case CLASS:         try { return Class.forName(stringify(x, false)); }
		                        catch (ClassNotFoundException e) { return FALSE; }
		    case NEW:           try { return toClass(x).newInstance(); }
		                        catch (ClassCastException e)     { ; }
		                        catch (NoSuchMethodError e)      { ; }
		                        catch (InstantiationException e) { ; }
		                        catch (ClassNotFoundException e) { ; }
		                        catch (IllegalAccessException e) { ; }
		                        return FALSE;
		    case METHOD:        return JavaMethod(stringify(x, false), y,
							      rest(rest(args)));
		    case EXIT:          System.exit((x == null) ? 0 : (int)num(x));
		    case LISTSTAR:      return listStar(args);
		    case TIMECALL:      Runtime runtime = Runtime.getRuntime();
		                        runtime.gc();
		                        long startTime = System.currentTimeMillis();
					long startMem = runtime.freeMemory();
					Object ans = FALSE;
					int nTimes = (y == null ? 1 : (int)num(y));
					for (int i = 0; i < nTimes; i++) {
					  ans = proc(x).apply(interp, null);
					}
		                        long time = System.currentTimeMillis() - startTime;
					long mem = startMem - runtime.freeMemory();
					return cons(ans, list(list(num(time), "msec"),
							      list(num(mem), "bytes")));
		    default:            return error("internal error: unknown primitive: " 
						     + this + " applied to " + args);
		    }
		case Continuation:
			value(first(args));
			throw cc();
		}
		return null;
	}

	  /** Coerces a Scheme object to a procedure. **/
	  static scheme proc(Object x) {
	    if (is(x,Procedure)) return (scheme) x;
	    else return proc(error("Not a procedure: " + stringify(x)));
	  }
	
	 /// ***************************** Scheme ******************************************
	  
	  
	  scheme getInput() {
		return (scheme) $[0];  
	  }
	  
	  scheme setInput(scheme port) {
		  return (scheme) ($[0] = port);
	  }
	  
	  PrintWriter getOutput() {
		  return (PrintWriter) $[1];
	  }
	  
	  PrintWriter setOutput(PrintWriter print) {
		  return (PrintWriter) ($[1] = print);
	  }
	  
	  scheme getGlobalEnvironment() {
		  return (scheme) $[2];
	  }

	  scheme setGlobalEnvironment(scheme port) {
		  return (scheme) ($[2] = port);
	  }
	  
	  /** Create a Scheme interpreter and load an array of files into it.
	   * Also load SchemePrimitives.CODE. **/
	  static scheme Scheme(String[] files) {
		  scheme env = Environment();
		  scheme result = new scheme(Scheme, new Object[]{ InputPort(System.in), new PrintWriter( System.out), env });
		  env = installPrimitives(env);
		  result.setGlobalEnvironment(env);
	    try {

	      result.load( InputPort(new StringReader(CODE)));
	      result.load( prelude(), true);
	      
	    	for (int i = 0; i < (files == null ? 0 : files.length); i++) {
	    	  result.load(files[i]);
	      } 
	    } catch (RuntimeException e) { e.printStackTrace() ; }
	    return result;
	  }

	  static String prelude() {
		  File f = new File("scheme.scm"); 
		  if (f.exists()) return f.getAbsolutePath();
		  f = new File("scheme.ss");
		  if (f.exists()) return f.getAbsolutePath();
		  f = new File(System.getProperty("user.home"),"scheme.scm");
		  if (f.exists()) return f.getAbsolutePath();
		  f = new File(System.getProperty("user.home"),"scheme.ss");
		  if (f.exists()) return f.getAbsolutePath();
		  return null;
	  }
	  

	  /** Prompt, read, eval, and write the result. 
	   * Also sets up a catch for any RuntimeExceptions encountered. **/
	  public void readEvalWriteLoop() {
	    Object x;
	    for(;;) {
	      try {
	    	 getOutput().print("> "); getOutput().flush();
		if (getInput().isEOF(x = getInput().read())) return;
		write(eval(x), getOutput(), true); 
		getOutput().println(); getOutput().flush();
	      } catch (RuntimeException e) { ; }
	    }
	  }

	  /** Eval all the expressions in a file. Calls load(InputPort). **/
	  Object load(Object fileName) {
		  return load(fileName, false);
	  }

	  /** Eval all the expressions in a file. Calls load(InputPort). **/
	  Object load(Object fileName, boolean silent) {
		if (fileName == null && silent) return null;
	    String name = stringify(fileName, false);
	    try { return load( InputPort(new FileInputStream(name))); }
	    catch (IOException e) { 
	    	if (silent) return null;
	    	else return error("can't load " + name); 
	    }
	  }	  
	  
	  /** Eval all the expressions coming from an InputPort. **/
	  Object load(scheme in) {
	    Object x = null;
	    for(;;) {
	      if (in.isEOF(x = in.read())) return TRUE;
	      eval(x); 
	    }
	  }
	      
	  //////////////// Evaluation

	  /** Evaluate an object, x, in an environment. **/
	  public Object eval(Object x, scheme env) {
	    // The purpose of the while loop is to allow tail recursion.
	    // The idea is that in a tail recursive position, we do "x = ..."
	    // and loop, rather than doing "return eval(...)".
	    while (true) {
	      if (x instanceof String) {         // VARIABLE
	    	  return env.lookup((String)x);
	      } else if (!is(x,Pair)) { // CONSTANT
	    	  return x;
	      } else {                           
		Object fn = first(x);
		Object args = rest(x);
		if (fn == "quote") {             // QUOTE
		  return first(args);
		} else if (fn == "begin") {      // BEGIN
		  for (; rest(args) != null; args = rest(args)) {
		    eval(first(args), env);
		  }
		  x = first(args);
		} else if (fn == "define") {     // DEFINE
		  if (is( first(args),  Pair) )
		    return env.define(first(first(args)),
		     eval(cons("lambda", cons(rest(first(args)), rest(args))), env));
		  else return env.define(first(args), eval(second(args), env));
		} else if (fn == "set!") {       // SET!
		  return env.set(first(args), eval(second(args), env));
		} else if (fn == "if") {         // IF
		  x = (truth(eval(first(args), env))) ? second(args) : third(args);
		} else if (fn == "cond") {       // COND
		  x = reduceCond(args, env);
		} else if (fn == "lambda") {     // LAMBDA
		  return Closure(first(args), rest(args), env);
		} else if (fn == "macro") {      // MACRO
			return Macro(first(args), rest(args), env);
		} else {                         // PROCEDURE CALL:
		  fn = eval(fn, env);
		  if (is(fn,Macros)) {          // (MACRO CALL)
		    x = ((scheme)fn).expand(this, (scheme)x, args);
		  } else if (is(fn,Closure)) { // (CLOSURE CALL)
		    scheme f = (scheme)fn;
		    x = f.body();
		    env = Environment(f.parms(), evalList(args, env), f.env());
		  } else {                            // (OTHER PROCEDURE CALL)
		    return proc(fn).apply(this, evalList(args, env));
		  }
		}
	      }
	    }
	  }

	  /** Eval in the global environment. **/
	  public Object eval(Object x) { return eval(x, getGlobalEnvironment() ); }

	  /** Evaluate each of a list of expressions. **/
	  scheme evalList(Object list, scheme env) {
	    if (list == null) 
	      return null;
	    else if (!is(list,Pair)) {
	      error("Illegal arg list: " + list);
	      return null;
	    } else 
	      return cons(eval(first(list), env), evalList(rest(list), env));
	  }

	  /** Reduce a cond expression to some code which, when evaluated,
	   * gives the value of the cond expression.  We do it that way to
	   * maintain tail recursion. **/
	  Object reduceCond(Object clauses, scheme env) {
	    Object result = null;
	    for (;;) {
	      if (clauses == null) return FALSE;
	      Object clause = first(clauses); clauses = rest(clauses);
	      if (first(clause) == "else" 
		  || truth(result = eval(first(clause), env)))
		if (rest(clause) == null) return list("quote", result);
		else if (second(clause) == "=>")
		  return list(third(clause), list("quote", result));
		else return cons("begin", rest(clause));
	    }
	  }

	  public static final String CODE = 
		  "(define call/cc    call-with-current-continuation)\n" +
		  "(define first 	   car)\n" +
		  "(define second     cadr)\n" +
		  "(define third      caddr)\n" +
		  "(define rest 	   cdr)\n" +
		  "(define set-first! set-car!)\n" +
		  "(define set-rest!  set-cdr!)\n" +

		  //;;;;;;;;;;;;;;;; Standard Scheme Macros

		  "(define or\n" +
		    "(macro args\n" +
		      "(if (null? args)\n" +
		  	"#f\n" +
		  	"(cons 'cond (map list args)))))\n" +

		  "(define and\n" +
		    "(macro args\n" +
		      "(cond ((null? args) #t)\n" +
		  	  "((null? (rest args)) (first args))\n" +
		  	  "(else (list 'if (first args) (cons 'and (rest args)) #f)))))\n" +

		  "(define quasiquote\n" +
		    "(macro (x)\n" +
		      "(define (constant? exp)\n" +
		        "(if (pair? exp) (eq? (car exp) 'quote) (not (symbol? exp))))\n" +
		      "(define (combine-skeletons left right exp)\n" +
		        "(cond\n" +
		         "((and (constant? left) (constant? right))\n" +
		  	"(if (and (eqv? (eval left) (car exp))\n" +
		  		 "(eqv? (eval right) (cdr exp)))\n" +
		  	    "(list 'quote exp)\n" +
		  	    "(list 'quote (cons (eval left) (eval right)))))\n" +
		         "((null? right) (list 'list left))\n" +
		         "((and (pair? right) (eq? (car right) 'list))\n" +
		  	"(cons 'list (cons left (cdr right))))\n" +
		         "(else (list 'cons left right))))\n" +
		      "(define (expand-quasiquote exp nesting)\n" +
		        "(cond\n" +
		         "((vector? exp)\n" +
		  	"(list 'apply 'vector (expand-quasiquote (vector->list exp) nesting)))\n" +
		         "((not (pair? exp))\n" +
		  	"(if (constant? exp) exp (list 'quote exp)))\n" +
		         "((and (eq? (car exp) 'unquote) (= (length exp) 2))\n" +
		  	"(if (= nesting 0)\n" +
		  	    "(second exp)\n" +
		  	    "(combine-skeletons ''unquote\n" +
		  			       "(expand-quasiquote (cdr exp) (- nesting 1))\n" +
		  			       "exp)))\n" +
		         "((and (eq? (car exp) 'quasiquote) (= (length exp) 2))\n" +
		  	"(combine-skeletons ''quasiquote\n" +
		  			   "(expand-quasiquote (cdr exp) (+ nesting 1))\n" +
		  			   "exp))\n" +
		         "((and (pair? (car exp))\n" +
		  	     "(eq? (caar exp) 'unquote-splicing)\n" +
		  	     "(= (length (car exp)) 2))\n" +
		  	"(if (= nesting 0)\n" +
		  	    "(list 'append (second (first exp))\n" +
		  		  "(expand-quasiquote (cdr exp) nesting))\n" +
		  	    "(combine-skeletons (expand-quasiquote (car exp) (- nesting 1))\n" +
		  			       "(expand-quasiquote (cdr exp) nesting)\n" +
		  			       "exp)))\n" +
		         "(else (combine-skeletons (expand-quasiquote (car exp) nesting)\n" +
		  				"(expand-quasiquote (cdr exp) nesting)\n" +
		  				"exp))))\n" +
		      "(expand-quasiquote x 0)))\n" +

		  "\n" +
		  "(define let\n" +
		    "(macro (bindings . body)\n" +
		      "(define (named-let name bindings body)\n" +
		        "`(let ((,name #f))\n" +
		  	 "(set! ,name (lambda ,(map first bindings) . ,body))\n" +
		  	 "(,name . ,(map second bindings))))\n" +
		      "(if (symbol? bindings)\n" +
		  	"(named-let bindings (first body) (rest body))\n" +
		  	"`((lambda ,(map first bindings) . ,body) . ,(map second bindings)))))\n" +

		  "(define let*\n" +
		    "(macro (bindings . body)\n" +
		      "(if (null? bindings) `((lambda () . ,body))\n" +
		  	"`(let (,(first bindings))\n" +
		  	   "(let* ,(rest bindings) . ,body)))))\n" +

		  "(define letrec\n" +
		    "(macro (bindings . body)\n" +
		      "(let ((vars (map first bindings))\n" +
		  	  "(vals (map second bindings)))\n" +
		      "`(let ,(map (lambda (var) `(,var #f)) vars)\n" +
		         ",@(map (lambda (var val) `(set! ,var ,val)) vars vals)\n" +
		         ". ,body))))\n" +
		      
		  "(define case\n" +
		    "(macro (exp . cases)\n" +
		      "(define (do-case case)\n" +
		        "(cond ((not (pair? case)) (error \"bad syntax in case\" case))\n" +
		  	    "((eq? (first case) 'else) case)\n" +
		  	    "(else `((member __exp__ ',(first case)) . ,(rest case)))))\n" +
		      "`(let ((__exp__ ,exp)) (cond . ,(map do-case cases)))))\n" +

		  "(define do\n" +
		    "(macro (bindings test-and-result . body)\n" +
		      "(let ((variables (map first bindings))\n" +
		  	  "(inits (map second bindings))\n" +
		  	  "(steps (map (lambda (clause)\n" +
		  			"(if (null? (cddr clause))\n" +
		  			    "(first clause)\n" +
		  			    "(third clause)))\n" +
		  		      "bindings))\n" +
		  	  "(test (first test-and-result))\n" +
		  	  "(result (rest test-and-result)))\n" +
		        "`(letrec ((__loop__\n" +
		  		 "(lambda ,variables\n" +
		  		   "(if ,test\n" +
		  		       "(begin . ,result)\n" +
		  		       "(begin\n" +
		  			 ",@body\n" +
		  			 "(__loop__ . ,steps))))))\n" +
		  	 "(__loop__ . ,inits)))))\n" +

		  "(define delay\n" +
		    "(macro (exp)\n" +
		      "(define (make-promise proc)\n" +
		        "(let ((result-ready? #f)\n" +
		  	    "(result #f))\n" +
		  	"(lambda ()\n" +
		  	  "(if result-ready?\n" +
		  	      "result\n" +
		  	      "(let ((x (proc)))\n" +
		  		"(if result-ready?\n" +
		  		    "result\n" +
		  		    "(begin (set! result-ready? #t)\n" +
		  			   "(set! result x)\n" +
		  			   "result)))))))\n" +
		      "`(,make-promise (lambda () ,exp))))\n" +

		  //;;;;;;;;;;;;;;;; Extensions

		  "(define time\n" +
		    "(macro (exp . rest) `(time-call (lambda () ,exp) . ,rest)))\n"
		  ;
	  
	/// ********************************** SchemeUtils ****************************************
	



		/** Same as Boolean.TRUE. **/
		  public static final Boolean TRUE = Boolean.TRUE;
		  /** Same as Boolean.FALSE. **/
		  public static final Boolean FALSE = Boolean.FALSE;

		  public static Double ZERO = new Double(0.0);
		  public static Double ONE = new Double(1.0);
		  //////////////// Conversion Routines ////////////////

		  // The following convert or coerce objects to the right type.

		  /** Convert boolean to Boolean. **/
		  public static Boolean truth(boolean x) { return x ? TRUE : FALSE; }

		  /** Convert Scheme object to boolean.  Only #f is false, others are true. **/
		  public static boolean truth(Object x) { return x != FALSE; }

		  /** Convert double to Double. Caches 0 and 1; makes new for others. **/
		  public static Double num(double x) { 
		    return (x == 0.0) ? ZERO : (x == 1.0) ? ONE : new Double(x); }

		  /** Converts a Scheme object to a double, or calls error. **/
		  public static double num(Object x) { 
		    if (x instanceof Number) return ((Number)x).doubleValue();
		    else return num(error("expected a number, got: " + x));
		  }

		  /** Converts a Scheme object to a char, or calls error. **/
		  public static char chr(Object x) {
		    if (x instanceof Character) return ((Character)x).charValue();
		    else return chr(error("expected a char, got: " + x));
		  }

		  /** Converts a char to a Character. **/
		  public static Character chr(char ch) {
		    return new Character(ch);
		  }

		  /** Coerces a Scheme object to a Scheme string, which is a char[]. **/
		  public static char[] str(Object x) {
		    if (x instanceof char[]) return (char[])x;
		    else return str(error("expected a string, got: " + x)); 
		  }

		  /** Coerces a Scheme object to a Scheme symbol, which is a string. **/
		  public static String sym(Object x) {
		    if (x instanceof String) return (String)x;
		    else return sym(error("expected a symbol, got: " + x)); 
		  }

		  /** Coerces a Scheme object to a Scheme vector, which is a Object[]. **/
		  public static Object[] vec(Object x) {
		    if (x instanceof Object[]) return (Object[])x;
		    else return vec(error("expected a vector, got: " + x)); 
		  }

		  /** Coerces a Scheme object to a Scheme input port, which is an InputPort.
		   * If the argument is null, returns interpreter.input. **/
		  public static scheme inPort(Object x, scheme interp) {
		    if (x == null) return interp.getInput();
		    else if (is(x,InputPort)) return (scheme)x;
		    else return inPort(error("expected an input port, got: " + x), interp); 
		  }

		  /** Coerces a Scheme object to a Scheme input port, which is a PrintWriter.
		   * If the argument is null, returns System.out. **/
		  public static PrintWriter outPort(Object x, scheme interp) {
		    if (x == null) return interp.getOutput();
		    else if (x instanceof PrintWriter) return (PrintWriter)x;
		    else return outPort(error("expected an output port, got: " + x), interp); 
		  }

		  //////////////// Error Routines ////////////////

		  /** A continuable error. Prints an error message and then prompts for
		   * a value to eval and return. **/
		  public static Object error(String message) {
		    System.err.println("**** ERROR: " + message);
		    throw list(message);  
		  }
		  

		  public static Object warn(String message) {
		    System.err.println("**** WARNING: " + message);
		    return "<warn>";
		  }

		  //////////////// Basic manipulation Routines ////////////////

		  // The following are used throughout the code.

		  /** Like Common Lisp first; car of a Pair, or null for anything else. **/
		  public static Object first(Object x) {
		    return (is(x,Pair)) ? ((scheme)x).car() : null; 
		  }

		  /** Like Common Lisp rest; car of a Pair, or null for anything else. **/
		  public static Object rest(Object x) {
		    return (is(x,Pair)) ? ((scheme)x).cdr() : null; 
		  }

		  /** Like Common Lisp (setf (first ... **/
		  public static Object setFirst(Object x, Object y) {
		    return is(x,Pair) ? ((scheme)x).car(y) 
		      : error("Attempt to set-car of a non-Pair:" + stringify(x));
		  }

		  /** Like Common Lisp (setf (rest ... **/
		  public static Object setRest(Object x, Object y) {
		    return is(x,Pair) ? ((scheme)x).cdr( y) 
		      : error("Attempt to set-cdr of a non-Pair:" + stringify(x));
		  }

		  /** Like Common Lisp second. **/
		  public static Object second(Object x) {
		    return first(rest(x));
		  }

		  /** Like Common Lisp third. **/
		  static Object third(Object x) {
		    return first(rest(rest(x)));
		  }

		  /** Creates a two element list. **/
		  static scheme list(Object a, Object b) {
		    return Pair(a, Pair(b, null));
		  }


		  /** Creates a one element list. **/
		  static scheme list(Object a) {
		    return Pair(a, null);
		  }

		  /** listStar(args) is like Common Lisp (apply #'list* args) **/
		  public static Object listStar(Object args) {
		    if (rest(args) == null) return first(args);
		    else return cons(first(args), listStar(rest(args)));
		  }

		  /** cons(x, y) is the same as new Pair(x, y). **/
		  public static scheme cons(Object a, Object b) {
		    return Pair(a, b);
		  }

		  /** Reverse the elements of a list. **/
		  public static Object reverse(Object x) {
		    Object result = null;
		    while (is(x,Pair)) {
		      result = cons(first(x), result);
		      x = rest(x);
		    }
		    return result;
		  }

		  /** Check if two objects are equal. **/
		  public static boolean equal(Object x, Object y) {
		    if (x == null || y == null) {
		      return x == y;
		    } else if (x instanceof char[]) {
		      if (!(y instanceof char[])) return false;
		      char[] xc = (char[])x, yc = (char[])y;
		      if (xc.length != yc.length) return false;
		      for (int i = xc.length - 1; i >= 0; i--) {
			if (xc[i] != yc[i]) return false;
		      }
		      return true;
		    } else if (x instanceof Object[]) {
		      if (!(y instanceof Object[])) return false;
		      Object[] xo = (Object[])x, yo = (Object[])y;
		      if (xo.length != yo.length) return false;
		      for (int i = xo.length - 1; i >= 0; i--) {
			if (!equal(xo[i],yo[i])) return false;
		      }
		      return true;
		    } else {
		      return x.equals(y);
		    }
		  }

		  /** Check if two objects are == or are equal numbers or characters. **/
		  public static boolean eqv(Object x, Object y) {
		    return x == y 
		      || (x instanceof Double && x.equals(y))
		      || (x instanceof Character && x.equals(y));
		  }

		  /** The length of a list, or zero for a non-list. **/
		  public static int length(Object x) {
		    int len = 0;
		    while (is(x,Pair)) {
		      len++;
		      x = ((scheme)x).cdr();
		    }
		    return len;
		  }

		  /** Convert a list of characters to a Scheme string, which is a char[]. **/
		  public static char[] listToString(Object chars) {
		    char[] str = new char[length(chars)];
		    for (int i = 0; is(chars,Pair); i++) {
		      str[i] = chr(first(chars));
		      chars = rest(chars);
		    }
		    return str;
		  }
		 
		  /** Convert a list of Objects to a Scheme vector, which is a Object[]. **/
		  public static Object[] listToVector(Object objs) {
		    Object[] vec = new Object[length(objs)];
		    for (int i = 0; is(objs,Pair); i++) {
		      vec[i] = first(objs);
		      objs = rest(objs);
		    }
		    return vec;
		  }
		 
		  /** Write the object to a port.  If quoted is true, use "str" and #\c,
		   * otherwise use str and c. **/
		  public static Object write(Object x, PrintWriter port, boolean quoted) {
		    port.print(stringify(x, quoted)); 
		    port.flush();
		    return x;
		  }

		  /** Convert a vector to a List. **/
		  public static scheme vectorToList(Object x) {
		    if (x instanceof Object[]) {
		      Object[] vec = (Object[])x;
		      scheme result = null;
		      for (int i = vec.length - 1; i >= 0; i--) 
			result = cons(vec[i], result);
		      return result;
		    } else {
		      error("expected a vector, got: " + x);
		      return null;
		    }
		  }

		  /** Convert a Scheme object to its printed representation, as
		   * a java String (not a Scheme string). If quoted is true, use "str" and #\c,
		   * otherwise use str and c. You need to pass in a StringBuffer that is used 
		   * to accumulate the results. (If the interface didn't work that way, the
		   * system would use lots of little internal StringBuffers.  But note that
		   * you can still call <tt>stringify(x)</tt> and a new StringBuffer will
		   * be created for you. **/

		  static void stringify(Object x, boolean quoted, StringBuffer buf) { 
		    if (x == null) 
		      buf.append("()");
		    else if (x instanceof Double) {
		      double d = ((Double)x).doubleValue();
		      if (Math.round(d) == d) buf.append((long)d); else buf.append(d);
		    } else if (x instanceof Character) {
		      if (quoted) buf.append("#\\");
		      buf.append(x);
		    } else if (is(x,Pair)) {
		      ((scheme)x).stringifyPair(quoted, buf);
		    } else if (x instanceof char[]) {
		      char[] chars = (char[])x;
		      if (quoted) buf.append('"');
		      for (int i = 0; i < chars.length; i++) {
			if (quoted && chars[i] == '"') buf.append('\\');
			buf.append(chars[i]);
		      }
		      if (quoted) buf.append('"');
		    } else if (x instanceof Object[]) {
			Object[] v = (Object[])x;
			buf.append("#(");
			for (int i=0; i<v.length; i++) {
			    stringify(v[i], quoted, buf);
			    if (i != v.length-1) buf.append(' ');
			}
			buf.append(')');
		    } else if (x == TRUE) {
		      buf.append("#t");
		    } else if (x == FALSE) {
		      buf.append("#f");
		    } else {
		      buf.append(x);
		    }
		  }

		  /** Convert x to a Java String giving its external representation. 
		   * Strings and characters are quoted. **/
		  static String stringify(Object x) { return stringify(x, true); }

		  /** Convert x to a Java String giving its external representation. 
		   * Strings and characters are quoted iff <tt>quoted</tt> is true.. **/
		  static String stringify(Object x, boolean quoted) { 
		    StringBuffer buf = new StringBuffer();
		    stringify(x, quoted, buf);
		    return buf.toString();
		  }

		  /** For debugging purposes, prints output. **/
		  static Object p(Object x) {
		    System.out.println(stringify(x));
		    return x;
		  }

		  /** For debugging purposes, prints output. **/
		  static Object p(String msg, Object x) {
		    System.out.println(msg + ": " + stringify(x));
		    return x;
		  }

    /// ********************************** Closure *********************************************		  

		    Object parms() {
		    	if ( !is(this, Closure)) fatal("Expected " + _name(_), _name(_) );
		    	return (Object) $[1];
		    }

		    Object parms(Object value) {
		    	if ( !is(this, Closure) ) fatal("Expected " + _name(_), _name(_) );
		    	return (Object) ($[1] = value);
		    }

		    Object body() {
		    	if ( !is(this, Closure)) fatal("Expected " + _name(_), _name(_) );
		    	return (Object) $[2];
		    }

		    Object body(Object value) {
		    	if ( !is(this, Closure) ) fatal("Expected " + _name(_), _name(_) );
		    	return (Object) ($[2] = value);
		    }

		    scheme env() {
		    	if ( !is(this, Closure)) fatal("Expected " + _name(_), _name(_) );
		    	return (scheme) $[3];
		    }

		    scheme env(scheme value) {
		    	if ( !is(this, Closure) ) fatal("Expected " + _name(_), _name(_) );
		    	return (scheme) ($[3] = value);
		    }
		    
		    /** Make a closure from a parameter list, body, and environment. **/
		    static scheme Closure (Object parms, Object body, scheme env) {
		    	scheme result = new scheme(Closure, null);
		    	result.name( anonymous );
		        result.parms( parms );
		        result.env( env );
		        result.body( (is(body,Pair) && rest(body) == null)
		        		? first(body) : cons("begin", body) );
		        return result;
		    }

	
    /// ************************************** Continuation **************************************
		    
		    RuntimeException cc() {
		    	if ( !is(this, Continuation)) fatal("Expected " + _name(_), _name(_) );
		    	return (RuntimeException) $[1];
		    }
		    
		    RuntimeException cc(RuntimeException value) {
		    	if ( !is(this, Continuation) ) fatal("Expected " + _name(_), _name(_) );
		    	return (RuntimeException) ($[1] = value);
		    }
		    
		    Object value() {
		    	if ( !is(this, Continuation)) fatal("Expected " + _name(_), _name(_) );
		    	return (Object) $[2];
		    }
		    
		    Object value(Object value) {
		    	if ( !is(this, Continuation) ) fatal("Expected " + _name(_), _name(_) );
		    	return (Object) ($[2] = value);
		    }
		    
		    static scheme Continuation(RuntimeException cc) {
		    	return new scheme(Continuation, new Object[]{anonymous, cc,null});
		    }
		    
    //// ************************************* Macro ***********************************
		    
		    /** Make a macro from a parameter list, body, and environment. **/
		    static scheme Macro(Object parms, Object body, scheme env) {
		    	scheme result = new scheme(Macros,null);
		    	result.name( anonymous );
		        result.parms( parms );
		        result.env( env );
		        result.body( (is(body,Pair) && rest(body) == null)
		        		? first(body) : cons("begin", body) );
		      return result;
		    }

		  /** Replace the old cons cell with the macro expansion, and return it. **/
		  scheme expand(scheme interpreter, scheme oldPair, Object args) {
		    Object expansion = apply(interpreter, args);
		    if (is( expansion,Pair)) {
		      oldPair.car( ((scheme)expansion).car() );
		      oldPair.cdr( ((scheme)expansion).cdr() );
		    } else {
		      oldPair.car( "begin" );
		      oldPair.cdr( cons(expansion, null) );
		    }
		    
		    return oldPair;
		  }

		  /** Macro expand an expression **/
		  static Object macroExpand(scheme interpreter, Object x) {
		    if (!is(x,Pair)) return x;
		    Object fn = interpreter.eval(first(x), interpreter.getGlobalEnvironment());
		    if (!is(fn,Macros)) return x;
		    return ((scheme)fn).expand(interpreter, (scheme)x, rest(x));
		  }
		    
    //// ************************************** JavaMethod ********************************
		    
		    Class[] argClasses() {
		    	return (Class[]) $[0];
		    }
		    
		    Class[] argClasses(Class[] value) {
		    	return (Class[]) ($[0] = value);
		    }

		    Method method() {
		    	return (Method) $[2];
		    }
		    
		    Method method(Method value) {
		    	return (Method) ($[2] = value);
		    }

		    boolean isStatic() {
		    	return  ((Boolean)$[3]).booleanValue();
		    }
		    
		    boolean isStatic(boolean value) {
		    	return ((Boolean) ($[3] = Boolean.valueOf(value))).booleanValue();
		    }

		    
		    static scheme JavaMethod(String methodName, Object targetClassName, 
		  		    Object argClassNames) {
		  	  scheme result = new scheme(JavaMethod,null);
		      result.name( targetClassName + "." + methodName );
		      try {
		        result.argClasses( classArray(argClassNames) );
		        result.method( toClass(targetClassName).getMethod(methodName, result.argClasses() ) );
		        result.isStatic( Modifier.isStatic(result.method().getModifiers()));
		      } catch (ClassNotFoundException e) { 
		        error("Bad class, can't get method " + result.name()); 
		      } catch (NoSuchMethodException e) { 
		        error("Can't get method " + result.name()); 
		      }
		      return result;
		    }


		    public static Class toClass(Object arg) throws ClassNotFoundException { 
		      if      (arg instanceof Class)  return (Class)arg;
		      arg = stringify(arg, false);

		      if (arg.equals("void"))    return java.lang.Void.TYPE;
		      else if (arg.equals("boolean")) return java.lang.Boolean.TYPE;
		      else if (arg.equals("char"))    return java.lang.Character.TYPE;
		      else if (arg.equals("byte"))    return java.lang.Byte.TYPE;
		      else if (arg.equals("short"))   return java.lang.Short.TYPE;
		      else if (arg.equals("int"))     return java.lang.Integer.TYPE;
		      else if (arg.equals("long"))    return java.lang.Long.TYPE;
		      else if (arg.equals("float"))   return java.lang.Float.TYPE;
		      else if (arg.equals("double"))  return java.lang.Double.TYPE;
		      else return Class.forName((String)arg);
		    }

		    /** Convert a list of Objects into an array.  Peek at the argClasses
		     * array to see what's expected.  That enables us to convert between
		     * Double and Integer, something Java won't do automatically. **/
		    public Object[] toArray(Object args) {
		      int n = length(args);
		      int diff = n - argClasses().length;
		      if (diff != 0)
		        error(Math.abs(diff) + " too " + ((diff>0) ? "many" : "few")
		  		   + " args to " + name());
		      Object[] array = new Object[n];
		      for(int i = 0; i < n && i < argClasses().length; i++) {
		        if (argClasses()[i] == java.lang.Integer.TYPE)
		  	array[i] = new Integer((int)num(first(args)));
		        else
		  	array[i] = first(args);
		        args = rest(args);
		      }
		      return array;
		    }

		    /** Convert a list of class names into an array of Classes. **/
		    static Class[] classArray(Object args) throws ClassNotFoundException {
		      int n = length(args);
		      Class[] array = new Class[n];
		      for(int i = 0; i < n; i++) {
		        array[i] = toClass(first(args));
		        args = rest(args);
		      }
		      return array;
		    }		    
		    
    //// ******************************** Pair ****************************************
		    
		    Object car() {
		    	if ( !is(this, Pair)) fatal("Expected " + _name(_), _name(_) );
		    	return (Object) $[0];
		    }

		    Object car(Object value) {
		    	if ( !is(this, Pair) ) fatal("Expected " + _name(_), _name(_) );
		    	return (Object) ($[0] = value);
		    }

		    Object cdr() {
		    	if ( !is(this, Pair)) fatal("Expected " + _name(_), _name(_) );
		    	return (Object) $[1];
		    }

		    Object cdr(Object value) {
		    	if ( !is(this, Pair) ) fatal("Expected " + _name(_), _name(_) );
		    	return (Object) ($[1] = value);
		    }
		    
		    /** Build a pair from two components. **/
		    static scheme Pair(Object first, Object rest) { 
		    	return new scheme(Pair, new Object[]{first,rest}); 
		    }

		    /** Two pairs are equal if their first and rest fields are equal. **/
		    public boolean equals(Object x) {
			if (x == this) return true;
			else if (!( is(x, Pair))) return false;
			else {
			  scheme that = (scheme)x;
			  return equal(this.car(), that.car())
			    && equal(this.cdr(), that.cdr());
			}
		    }


		  /** Build up a String representation of the Pair in a StringBuffer. **/
		  void stringifyPair(boolean quoted, StringBuffer buf) {
		    String special = null;
		    if (( is(car(), Pair)) && rest(cdr()) == null) 
		      special = (car() == "quote") ? "'" : (car() == "quasiquote") ? "`"
			: (car() == "unquote") ? "," : (car() == "unquote-splicing") ? ",@"
			: null;
			
		    if (special != null) {
		      buf.append(special); stringify(second(this), quoted, buf);
		    } else {
		      buf.append('(');
		      stringify(car(), quoted, buf);
		      Object tail = cdr();
		      while ( is(tail,Pair) ) {
			buf.append(' ');
			stringify(((scheme)tail).car(), quoted, buf);
			tail = ((scheme)tail).cdr();
		      }
		      if (tail != null) {
			buf.append(" . ");
			stringify(tail, quoted, buf);
		      }
		      buf.append(')');
		    }
		  }
		  
    //// ********************************** Primitive **************************************
		  
		    
		    int minArgs() {
		    	return ((Integer) $[2]).intValue();
		    }
		    
		    int minArgs(int value) {
		    	return ((Integer) ($[2] = Integer.valueOf(value))).intValue();
		    }

		    int maxArgs() {
		    	return ((Integer) $[3]).intValue();
		    }

		    int maxArgs(int value) {
		    	return ((Integer) ($[3] = Integer.valueOf(value))).intValue();
		    }

		    int idNumber(int value) {
		    	return ((Integer) ($[1] = Integer.valueOf(value))).intValue();
		    }

		    
		    int idNumber() {
		    	return ((Integer) $[1]).intValue();
		    }

		    
		    static scheme Primitive(int id, int minArgs, int maxArgs) {
		    	scheme result = new scheme(Primitive, new Object[]{anonymous,Integer.valueOf(id),
		    			Integer.valueOf(minArgs),Integer.valueOf(maxArgs) });
		    	return result;
		    }

		    private static final int EQ = 0, LT = 1, GT = 2, GE = 3, LE = 4,
		      ABS = 5, EOF_OBJECT = 6, EQQ = 7, EQUALQ = 8, FORCE = 9,
		      CAR = 10, FLOOR = 11,  CEILING = 12, CONS = 13, 
		      DIVIDE= 14, LENGTH = 15, LIST = 16, LISTQ = 17, APPLY = 18,
		      MAX = 19, MIN = 20, MINUS = 21, NEWLINE = 22, 
		      NOT = 23, NULLQ = 24, NUMBERQ = 25, PAIRQ = 26, PLUS = 27, 
		      PROCEDUREQ = 28, READ = 29, CDR = 30, ROUND = 31, SECOND = 32, 
		      SYMBOLQ = 33, TIMES = 34, TRUNCATE = 35, WRITE = 36, APPEND = 37,
		      BOOLEANQ = 38, SQRT = 39, EXPT = 40, REVERSE = 41, ASSOC = 42, 
		      ASSQ = 43, ASSV = 44, MEMBER = 45, MEMQ = 46, MEMV = 47, EQVQ = 48,
		      LISTREF = 49, LISTTAIL = 50, STRINQ = 51, MAKESTRING = 52, STRING = 53,
		      STRINGLENGTH = 54, STRINGREF = 55, STRINGSET = 56, SUBSTRING = 57, 
		      STRINGAPPEND = 58, STRINGTOLIST = 59, LISTTOSTRING = 60, 
		      SYMBOLTOSTRING = 61, STRINGTOSYMBOL = 62, EXP = 63, LOG = 64, SIN = 65,
		      COS = 66, TAN = 67, ACOS = 68, ASIN = 69, ATAN = 70, 
		      NUMBERTOSTRING = 71, STRINGTONUMBER = 72, CHARQ = 73,
		      CHARALPHABETICQ = 74, CHARNUMERICQ = 75, CHARWHITESPACEQ = 76,
		      CHARUPPERCASEQ = 77, CHARLOWERCASEQ = 78, CHARTOINTEGER = 79,
		      INTEGERTOCHAR = 80, CHARUPCASE = 81, CHARDOWNCASE = 82, STRINGQ = 83,
		      VECTORQ = 84, MAKEVECTOR = 85, VECTOR = 86, VECTORLENGTH = 87,
		      VECTORREF = 88, VECTORSET = 89, LISTTOVECTOR = 90, MAP = 91, 
		      FOREACH = 92, CALLCC = 93, VECTORTOLIST = 94, LOAD = 95, DISPLAY = 96,
		      INPUTPORTQ = 98, CURRENTINPUTPORT = 99, OPENINPUTFILE = 100, 
		      CLOSEINPUTPORT = 101, OUTPUTPORTQ = 103, CURRENTOUTPUTPORT = 104,
		      OPENOUTPUTFILE = 105, CLOSEOUTPUTPORT = 106, READCHAR = 107,
		      PEEKCHAR = 108, EVAL = 109, QUOTIENT = 110, REMAINDER = 111,
		      MODULO = 112, THIRD = 113, EOFOBJECTQ = 114, GCD = 115, LCM = 116, 
		      CXR = 117, ODDQ = 118, EVENQ = 119, ZEROQ = 120, POSITIVEQ = 121,
		      NEGATIVEQ = 122, 
		      CHARCMP = 123 /* to 127 */, CHARCICMP = 128 /* to 132 */,
		      STRINGCMP = 133 /* to 137 */, STRINGCICMP = 138 /* to 142 */,
		      EXACTQ = 143, INEXACTQ = 144, INTEGERQ = 145,
		      CALLWITHINPUTFILE = 146, CALLWITHOUTPUTFILE = 147
		    ;

		  //////////////// Extensions ////////////////

		    static final int NEW = -1, CLASS = -2, METHOD = -3, EXIT = -4,
		      SETCAR = -5, SETCDR = -6, TIMECALL = -11, MACROEXPAND = -12,
		      ERROR = -13, LISTSTAR = -14
		    ;


		  public static scheme installPrimitives(scheme env)  {

		    int n = Integer.MAX_VALUE;

		    env
		     .defPrim("*",       	TIMES,     0, n)
		     .defPrim("*",       	TIMES,     0, n)
		     .defPrim("+",       	PLUS,      0, n)
		     .defPrim("-",       	MINUS,     1, n)
		     .defPrim("/",       	DIVIDE,    1, n)
		     .defPrim("<",       	LT,        2, n)
		     .defPrim("<=",      	LE,        2, n)
		     .defPrim("=",       	EQ,        2, n)
		     .defPrim(">",       	GT,        2, n)
		     .defPrim(">=",      	GE,        2, n)
		     .defPrim("abs",     	ABS,       1)
		     .defPrim("acos",    	ACOS,      1)
		     .defPrim("append",         APPEND,    0, n)
		     .defPrim("apply",   	APPLY,     2, n)
		     .defPrim("asin",    	ASIN,      1)
		     .defPrim("assoc",   	ASSOC,     2)
		     .defPrim("assq",    	ASSQ,      2)
		     .defPrim("assv",    	ASSV,      2)
		     .defPrim("atan",    	ATAN,      1)
		     .defPrim("boolean?",	BOOLEANQ,  1)
		     .defPrim("caaaar",         CXR,       1)
		     .defPrim("caaadr",         CXR,       1)
		     .defPrim("caaar",          CXR,       1)
		     .defPrim("caadar",         CXR,       1)
		     .defPrim("caaddr",         CXR,       1)
		     .defPrim("caadr",          CXR,       1)
		     .defPrim("caar",           CXR,       1)
		     .defPrim("cadaar",         CXR,       1)
		     .defPrim("cadadr",         CXR,       1)
		     .defPrim("cadar",          CXR,       1)
		     .defPrim("caddar",         CXR,       1)
		     .defPrim("cadddr",         CXR,       1)
		     .defPrim("caddr",     	THIRD,     1)
		     .defPrim("cadr",  	        SECOND,    1)
		     .defPrim("call-with-current-continuation",        CALLCC,    1)
		     .defPrim("call-with-input-file", CALLWITHINPUTFILE, 2)
		     .defPrim("call-with-output-file", CALLWITHOUTPUTFILE, 2)
		     .defPrim("car",     	CAR,       1)
		     .defPrim("cdaaar",         CXR,       1)
		     .defPrim("cdaadr",         CXR,       1)
		     .defPrim("cdaar",          CXR,       1)
		     .defPrim("cdadar",         CXR,       1)
		     .defPrim("cdaddr",         CXR,       1)
		     .defPrim("cdadr",          CXR,       1)
		     .defPrim("cdar",           CXR,       1)
		     .defPrim("cddaar",         CXR,       1)
		     .defPrim("cddadr",         CXR,       1)
		     .defPrim("cddar",          CXR,       1)
		     .defPrim("cdddar",         CXR,       1)
		     .defPrim("cddddr",         CXR,       1)
		     .defPrim("cdddr",          CXR,       1)
		     .defPrim("cddr",           CXR,       1)
		     .defPrim("cdr",     	CDR,       1)
		     .defPrim("char->integer",  CHARTOINTEGER,      1)
		     .defPrim("char-alphabetic?",CHARALPHABETICQ,      1)
		     .defPrim("char-ci<=?",     CHARCICMP+LE, 2)
		     .defPrim("char-ci<?" ,     CHARCICMP+LT, 2)
		     .defPrim("char-ci=?" ,     CHARCICMP+EQ, 2)
		     .defPrim("char-ci>=?",     CHARCICMP+GE, 2)
		     .defPrim("char-ci>?" ,     CHARCICMP+GT, 2)
		     .defPrim("char-downcase",  CHARDOWNCASE,      1)
		     .defPrim("char-lower-case?",CHARLOWERCASEQ,      1)
		     .defPrim("char-numeric?",  CHARNUMERICQ,      1)
		     .defPrim("char-upcase",    CHARUPCASE,      1)
		     .defPrim("char-upper-case?",CHARUPPERCASEQ,      1)
		     .defPrim("char-whitespace?",CHARWHITESPACEQ,      1)
		     .defPrim("char<=?",        CHARCMP+LE, 2)
		     .defPrim("char<?",         CHARCMP+LT, 2)
		     .defPrim("char=?",         CHARCMP+EQ, 2)
		     .defPrim("char>=?",        CHARCMP+GE, 2)
		     .defPrim("char>?",         CHARCMP+GT, 2)
		     .defPrim("char?",   	CHARQ,     1)
		     .defPrim("close-input-port", CLOSEINPUTPORT, 1)
		     .defPrim("close-output-port", CLOSEOUTPUTPORT, 1)
		     .defPrim("complex?", 	NUMBERQ,   1)
		     .defPrim("cons",    	CONS,      2)
		     .defPrim("cos",     	COS,       1)
		     .defPrim("current-input-port", CURRENTINPUTPORT, 0)
		     .defPrim("current-output-port", CURRENTOUTPUTPORT, 0)
		     .defPrim("display",        DISPLAY,   1, 2)
		     .defPrim("eof-object?",    EOFOBJECTQ, 1)
		     .defPrim("eq?",     	EQQ,       2)
		     .defPrim("equal?",  	EQUALQ,    2)
		     .defPrim("eqv?",    	EQVQ,      2)
		     .defPrim("eval",           EVAL,      1, 2)
		     .defPrim("even?",          EVENQ,     1)
		     .defPrim("exact?",         INTEGERQ,  1)
		     .defPrim("exp",     	EXP,       1)
		     .defPrim("expt",    	EXPT,      2)
		     .defPrim("force",          FORCE,     1)
		     .defPrim("for-each",       FOREACH,   1, n)
		     .defPrim("gcd",            GCD,       0, n)
		     .defPrim("inexact?",       INEXACTQ,  1)
		     .defPrim("input-port?",    INPUTPORTQ, 1)
		     .defPrim("integer->char",  INTEGERTOCHAR,      1)
		     .defPrim("integer?",       INTEGERQ,  1)
		     .defPrim("lcm",            LCM,       0, n)
		     .defPrim("length",  	LENGTH,    1)
		     .defPrim("list",    	LIST,      0, n)
		     .defPrim("list->string", 	LISTTOSTRING, 1)
		     .defPrim("list->vector",   LISTTOVECTOR,      1)
		     .defPrim("list-ref", 	LISTREF,   2)
		     .defPrim("list-tail", 	LISTTAIL,  2)
		     .defPrim("list?",          LISTQ,     1)
		     .defPrim("load",           LOAD,      1)
		     .defPrim("log",     	LOG,       1)
		     .defPrim("macro-expand",   MACROEXPAND,1)
		     .defPrim("make-string", 	MAKESTRING,1, 2)
		     .defPrim("make-vector",    MAKEVECTOR,1, 2)
		     .defPrim("map",            MAP,       1, n)
		     .defPrim("max",     	MAX,       1, n)
		     .defPrim("member",  	MEMBER,    2)
		     .defPrim("memq",    	MEMQ,      2)
		     .defPrim("memv",    	MEMV,      2)
		     .defPrim("min",     	MIN,       1, n)
		     .defPrim("modulo",         MODULO,    2)
		     .defPrim("negative?",      NEGATIVEQ, 1)
		     .defPrim("newline", 	NEWLINE,   0, 1)
		     .defPrim("not",     	NOT,       1)
		     .defPrim("null?",   	NULLQ,     1)
		     .defPrim("number->string", NUMBERTOSTRING,   1, 2)
		     .defPrim("number?", 	NUMBERQ,   1)
		     .defPrim("odd?",           ODDQ,      1)
		     .defPrim("open-input-file",OPENINPUTFILE, 1)
		     .defPrim("open-output-file", OPENOUTPUTFILE, 1)
		     .defPrim("output-port?",   OUTPUTPORTQ, 1)
		     .defPrim("pair?",   	PAIRQ,     1)
		     .defPrim("peek-char",      PEEKCHAR,  0, 1)
		     .defPrim("positive?",      POSITIVEQ, 1)
		     .defPrim("procedure?", 	PROCEDUREQ,1)
		     .defPrim("quotient",       QUOTIENT,  2)
		     .defPrim("rational?",      INTEGERQ, 1)
		     .defPrim("read",    	READ,      0, 1)
		     .defPrim("read-char",      READCHAR,  0, 1)
		     .defPrim("real?", 	        NUMBERQ,   1)
		     .defPrim("remainder",      REMAINDER, 2)
		     .defPrim("reverse", 	REVERSE,   1)
		     .defPrim("round",  	ROUND,     1)
		     .defPrim("set-car!",	SETCAR,    2)
		     .defPrim("set-cdr!",	SETCDR,    2)
		     .defPrim("sin",     	SIN,       1)
		     .defPrim("sqrt",    	SQRT,      1)
		     .defPrim("string", 	STRING,    0, n)
		     .defPrim("string->list", 	STRINGTOLIST, 1)
		     .defPrim("string->number", STRINGTONUMBER,   1, 2)
		     .defPrim("string->symbol", STRINGTOSYMBOL,   1)
		     .defPrim("string-append",  STRINGAPPEND, 0, n)
		     .defPrim("string-ci<=?",   STRINGCICMP+LE, 2)
		     .defPrim("string-ci<?" ,   STRINGCICMP+LT, 2)
		     .defPrim("string-ci=?" ,   STRINGCICMP+EQ, 2)
		     .defPrim("string-ci>=?",   STRINGCICMP+GE, 2)
		     .defPrim("string-ci>?" ,   STRINGCICMP+GT, 2)
		     .defPrim("string-length",  STRINGLENGTH,   1)
		     .defPrim("string-ref", 	STRINGREF, 2)
		     .defPrim("string-set!", 	STRINGSET, 3)
		     .defPrim("string<=?",      STRINGCMP+LE, 2)
		     .defPrim("string<?",       STRINGCMP+LT, 2)
		     .defPrim("string=?",       STRINGCMP+EQ, 2)
		     .defPrim("string>=?",      STRINGCMP+GE, 2)
		     .defPrim("string>?",       STRINGCMP+GT, 2)
		     .defPrim("string?", 	STRINGQ,   1)
		     .defPrim("substring", 	SUBSTRING, 3)
		     .defPrim("symbol->string", SYMBOLTOSTRING,   1)
		     .defPrim("symbol?", 	SYMBOLQ,   1)
		     .defPrim("tan",     	TAN,       1)
		     .defPrim("vector",    	VECTOR,    0, n)
		     .defPrim("vector->list",   VECTORTOLIST, 1)
		     .defPrim("vector-length",  VECTORLENGTH, 1)
		     .defPrim("vector-ref",     VECTORREF, 2)
		     .defPrim("vector-set!",    VECTORSET, 3)
		     .defPrim("vector?",    	VECTORQ,   1)
		     .defPrim("write",   	WRITE,     1, 2)
		     .defPrim("write-char",   	DISPLAY,   1, 2)
		     .defPrim("zero?",          ZEROQ,     1)
			      
		     ///////////// Extensions ////////////////

		     .defPrim("new",     	    NEW,       1)
		     .defPrim("class",   	    CLASS,     1)
		     .defPrim("method",  	    METHOD,    2, n)
		     .defPrim("exit",    	    EXIT,      0, 1)
		     .defPrim("error",    	    ERROR,     0, n)
		     .defPrim("time-call",          TIMECALL,  1, 2)
		     .defPrim("_list*",             LISTSTAR,  0, n)
		       ;

		     return env;
		  }


		  public static char[] stringAppend(Object args) {
		    StringBuffer result = new StringBuffer();
		    for(; is(args, Pair); args = rest(args)) {
		      result.append(stringify(first(args), false));
		    }
		    return result.toString().toCharArray();
		  }

		  public static Object memberAssoc(Object obj, Object list, char m, char eq) {
		    while ( is( list, Pair )) {
		      Object target = (m == 'm') ? first(list) : first(first(list));
		      boolean found;
		      switch (eq) {
		      case 'q': found = (target == obj); break;
		      case 'v': found = eqv(target, obj); break;
		      case ' ': found = equal(target, obj); break;
		      default: warn("Bad option to memberAssoc:" + eq); return FALSE;
		      }
		      if (found) return (m == 'm') ? list : first(list);
		      list = rest(list);
		    }
		    return FALSE;
		  }

		  public static Object numCompare(Object args, char op) {
		    while (is( rest(args), Pair)) {
		      double x = num(first(args)); args = rest(args);
		      double y = num(first(args));
		      switch (op) {
		      case '>': if (!(x >  y)) return FALSE; break;
		      case '<': if (!(x <  y)) return FALSE; break;
		      case '=': if (!(x == y)) return FALSE; break;
		      case 'L': if (!(x <= y)) return FALSE; break;
		      case 'G': if (!(x >= y)) return FALSE; break;
		      default: error("internal error: unrecognized op: " + op); break;
		      }
		    }
		    return TRUE;
		  }

		  public static Object numCompute(Object args, char op, double result) {
		    if (args == null) {
		      switch (op) {
		      case '-': return num(0 - result);
		      case '/': return num(1 / result);
		      default:  return num(result);
		      }
		    } else {
		      while ( is(args, Pair)) {
			double x = num(first(args)); args = rest(args);
			switch (op) {
			case 'X': if (x > result) result = x; break;
			case 'N': if (x < result) result = x; break;
			case '+': result += x; break;
			case '-': result -= x; break;
			case '*': result *= x; break;
			case '/': result /= x; break;
			default: error("internal error: unrecognized op: " + op); break;
			}
		      }
		      return num(result);
		    }
		  }

		  /** Return the sign of the argument: +1, -1, or 0. **/
		  static int sign(int x) { return (x > 0) ? +1 : (x < 0) ? -1 : 0; }

		  /** Return <0 if x is alphabetically first, >0 if y is first,
		   * 0 if same.  Case insensitive iff ci is true.  Error if not both chars. **/
		  public static int charCompare(Object x, Object y, boolean ci) {
		    char xc = chr(x), yc = chr(y);
		    if (ci) { xc = Character.toLowerCase(xc); yc = Character.toLowerCase(yc); }
		    return xc - yc;
		  }

		  /** Return <0 if x is alphabetically first, >0 if y is first,
		   * 0 if same.  Case insensitive iff ci is true.  Error if not strings. **/
		  public static int stringCompare(Object x, Object y, boolean ci) {
		    if (x instanceof char[] && y instanceof char[]) {
		      char[] xc = (char[])x, yc = (char[])y;
		      for (int i = 0; i < xc.length; i+
