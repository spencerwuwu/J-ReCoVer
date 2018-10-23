// https://searchcode.com/api/result/50857857/

import java.util.Stack;
import java.util.Iterator; // remove

import java.util.HashMap;
import java.util.Map;

/* *** This file is given as part of the programming assignment. *** */

public class Interpreter {

    // a scanner...
    private Scan scanner;

    // contains variables; current implementation is just built-in functions
    private Map<String,Expression> variables;

    // tok is global to all these parsing methods;
    // scan just calls the scanner's scan method and saves the result in tok.
    private Token tok; // the current token
    private void scan() {
        tok = scanner.scan();
    }

    // note on the first sets:
    // we cheat -- there is only a single token in the set,
    // so we just compare tkrep with the first token.

    // level at which parsing;
    // used to handle the problem of scan-ahead, given interactive system.
    private int level;

    // for error handling
    // to make this a bit friendlier in interactive environment;
    // handle parse errors by jumping back to main loop.

    class ParsingExpressionException extends Exception {
        public ParsingExpressionException(String msg) {
//            super(msg); // call constructor in superclass (i.e., base class);
            // it outputs message and a bit more.
        }
    }


    public Interpreter(String list[]) {
        scanner = new Scan(list);

        variables = new HashMap<String,Expression>();
        init_builtins();

        while (true) {
            System.out.print("> ");
            System.out.flush();
            // always reset since previous might have failed
            level = 0;
            scan();
            if (is(TK.EOF)) break;
            try {
                // read and parse expression.
                Expression expression = expr();
                // in later parts:
                //   print out expression
                System.out.println(expression.toString());
                //   evaluate expression
                //   print out value of evaluated expression
                System.out.println(expression.evaluate(variables));
                // note that an error in evaluating (at any level) will
                // return List.nil and evaluation will continue.
            }
            catch (ParsingExpressionException e) {
                System.out.println( "trying to recover from parse error");
                gobble();
            }
            //System.out.println("");
        }
    }

    private Expression expr() throws ParsingExpressionException {
        if (is(TK.LPAREN))
            return list();
        else if (is(TK.ID) || is(TK.NUM))
            return atom();
        else if (is(TK.QUOTE)) {
            /*
            added handling of quote by simply adding a new recursive function
              which simply creates a list (quote expr) where expr is gotten
              from an expr() call. almost nothing changes.
            */
            return quote();
        }
        else {
            parse_error("bad start of expression");
            /*NOTREACHED*/
        }
        return null; // is there a better way to make Java not complain?
    }

    private List list() throws ParsingExpressionException {
        level++;
        mustbe(TK.LPAREN);

        // we use a list that we will initialize when we first come
        //  across something (which we either will, or will find a
        //  close paren and return List.nil)
        // i wonder if there a better way to do this
        //  at first, i was trying to just make functionally nil lists
        //  which i would call append on and would set the first value
        //  if it were null, but that seemed even worse than this
        //  the idea of building a vector/list and iterating through it
        //  was also quite unappealing
        List l = null;

        if (is(TK.RPAREN))
            l = List.nil;
        else {
            // first gets set to false at the end of the loop, once
            //  it's been gone through once already
            boolean first = true;
            while (!is(TK.RPAREN)) {
                if (is(TK.LPAREN))
                    if (first)
                        l = new List(list());
                    else
                        l.append(new List(list()));
                else if (is(TK.NUM) || is(TK.ID))
                    if (first)
                        l = new List(atom());
                    else
                        l.append(atom());
                else if (is(TK.QUOTE))
                    if (first)
                        l = new List(quote());
                    else
                        l.append(new List(quote()));
                else
                    parse_error("oops -- bad atom " + tok.kind);

                first = false;
            }
        }

        level--;
        mustbe(TK.RPAREN);

        return l;
    }

    private Atom atom() throws ParsingExpressionException {
        Token this_tok = tok;
        if (is(TK.ID)) {
            mustbe(TK.ID);
            return new Identifier(this_tok.string);
        }
        else if (is(TK.NUM)) {
            mustbe(TK.NUM);
            return new Number(Integer.parseInt(this_tok.string));
        }
        else {
            parse_error("oops -- bad atom");
            /*NOTREACHED*/
        }
        return null; // is there a better way to make Java not complain?
    }

    private List quote() throws ParsingExpressionException {
        scan(); // throw away the quote
        return new List(new Identifier("quote"), new List(expr()));
    }

    // is current token what we want?
    private boolean is(TK tk) {
        return tk == tok.kind;
    }

    // ensure current token is tk and skip over it.
    void mustbe(TK tk) throws ParsingExpressionException {
        if( !is(tk) ) {
            System.err.println( "mustbe: want " + tk + ", got " + tok );
            parse_error( "missing token (mustbe)" );
        }
        // read ahead to next token only if not at top level.
        // this enables returning to main loop after parse entire expression;
        // otherwise would need to wait for user to type first
        // part of next expression before evaluating current expression,
        // which wouldn't be so good in interactive environment.
        // (so main loop always calls scan before calling expr)
        if (level > 0) scan();
    }
    
    void parse_error(String msg) throws ParsingExpressionException {
        System.err.println( "can't parse: " + msg );
        throw new ParsingExpressionException("problem parsing");
    }
    
    // used in recovering from errors.
    // gobble up all tokens up until something that could start an expression.
    // obviously, not entirely effective...
    // another possibility would be to gobble up to matching ) or ]
    // but that's not 100% effective either.
    void gobble() {
        while( level > 0 &&
               !is(TK.LPAREN) &&
               !is(TK.ID) &&
               !is(TK.NUM) &&
               !is(TK.EOF) ) {
            scan();
        }
    }

    private void init_builtins() {
        // eventually reduce redudancy here

        variables.put("nil", List.nil);

        variables.put("show", new Function("show", 0) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables))
                    return List.nil;

                System.out.println("           show      special    0      builtin");
                System.out.println("           cons  non-special    2      builtin");
                System.out.println("            car  non-special    1      builtin");
                System.out.println("            cdr  non-special    1      builtin");
                System.out.println("          quote      special    1      builtin");
                System.out.println("           list  non-special   -1      builtin");
                System.out.println("         append  non-special   -1      builtin");
                System.out.println("         length  non-special    1      builtin");
                System.out.println("              +  non-special    2      builtin");
                System.out.println("              -  non-special    2      builtin");
                System.out.println("              *  non-special    2      builtin");
                System.out.println("              /  non-special    2      builtin");
                System.out.println("              =  non-special    2      builtin");
                System.out.println("             /=  non-special    2      builtin");
                System.out.println("              <  non-special    2      builtin");
                System.out.println("              >  non-special    2      builtin");
                System.out.println("             <=  non-special    2      builtin");
                System.out.println("             >=  non-special    2      builtin");
                System.out.println("           null  non-special    1      builtin");
                System.out.println("           atom  non-special    1      builtin");
                System.out.println("          listp  non-special    1      builtin");
                System.out.println("       integerp  non-special    1      builtin");
                System.out.println("           cond      special   -1      builtin");
                return List.nil;
            }
        });


        variables.put("quote", new Function("quote", 1) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables))
                    return List.nil;
                return list.getCar();
            }
        });

        variables.put("list", new Function("list", -1) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null)
                    return List.nil;
                return list.evaluate_elements(variables);
            }
        });

        variables.put("null", new Function("null", 1) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables))
                    return List.nil;
                if (list.getCar() == List.nil)
                    return Number.t;
                else
                    return List.nil;
            }
        });

        variables.put("atom", new Function("atom", 1) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables))
                    return List.nil;
                if (list.getCar() == List.nil)
                    return Number.t;
                if (list.length() == 1)
                    if (list.getCar().evaluate(variables) instanceof Atom)
                        return Number.t;
                return List.nil;
            }
        });

        variables.put("listp", new Function("listp", 1) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables))
                    return List.nil;
                if (list.length() == 1)
                    if(list.getCar().evaluate(variables) instanceof List)
                        return Number.t;
                return List.nil;
            }
        });

        variables.put("integerp", new Function("integerp", 1) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables))
                    return List.nil;
                if (list.length() == 1) {
                    Expression test = list.getCar().evaluate(variables);
                    if (test instanceof Number)
                        return Number.t;
                }
                return List.nil;
            }
        });

        variables.put("cons", new Function("cons", 2) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables))
                    return List.nil;

                Expression cdr = list.getCdr().getCar().evaluate(variables);
                if (cdr instanceof List) {
                    List consed = new List(list.getCar().evaluate(variables));
                    if (cdr != List.nil)
                        consed.append((List) cdr);
                    return consed;
                }
                System.out.println("cons's 2nd argument is non-list");
                return List.nil;
            }
        });

        variables.put("car", new Function("car", 1) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (list == List.nil)
                    return List.nil;
                if (!checkArity(list, variables))
                    return List.nil;

                Expression car = list.getCar().evaluate(variables);
                if (car instanceof List)
                    return ((List) car).getCar();
                return List.nil;
            }
        });

        variables.put("cdr", new Function("cdr", 1) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (list == List.nil)
                    return List.nil;
                if (!checkArity(list, variables))
                    return List.nil;

                Expression car = list.getCar().evaluate(variables);
                if (car instanceof List)
                    return ((List) car).getCdr();
                return List.nil;
            }
        });

        variables.put("length", new Function("length", 1) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables))
                    return List.nil;

                Expression car = list.getCar().evaluate(variables);
                if (!(car instanceof List)) {
                    System.out.println("length given a non-list or an impure list (dotted pair at end of list)");
                    return List.nil;
                }
                return new Number(((List) car).length());
            }
        });

        variables.put("append", new Function("append", -1) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null || list == List.nil)
                    return List.nil;
                list = list.evaluate_elements(variables);

                try {
                    return List.chain_append(list);
                }
                catch (List.AppendGivenNonListException e) {
                    return List.nil;
                }
            }
        });

        variables.put("+", new Function("+", 2) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null || list == List.nil)
                    return List.nil;
                list = list.evaluate_elements(variables);
                try {
                    return ((Number) list.getCar()).add((Number) list.getCdr().getCar());
                } catch (ClassCastException e) {
                    System.out.println("builtin arithmetic rel op given non-number");
                    return List.nil;
                }
            }
        });

        variables.put("-", new Function("-", 2) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null || list == List.nil)
                    return List.nil;
                list = list.evaluate_elements(variables);
                try {
                    return ((Number) list.getCar()).subtract((Number) list.getCdr().getCar());
                } catch (ClassCastException e) {
                    System.out.println("builtin arithmetic rel op given non-number");
                    return List.nil;
                }
            }
        });

        variables.put("*", new Function("*", 2) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null || list == List.nil)
                    return List.nil;
                list = list.evaluate_elements(variables);
                try {
                    return ((Number) list.getCar()).mult((Number) list.getCdr().getCar());
                } catch (ClassCastException e) {
                    System.out.println("builtin arithmetic rel op given non-number");
                    return List.nil;
                }
            }
        });

        variables.put("/", new Function("/", 2) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null || list == List.nil)
                    return List.nil;
                list = list.evaluate_elements(variables);
                try {
                    return ((Number) list.getCar()).div((Number) list.getCdr().getCar());
                } catch (ClassCastException e) {
                    System.out.println("builtin arithmetic rel op given non-number");
                    return List.nil;
                }
            }
        });

        variables.put("=", new Function("=", 2) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null || list == List.nil)
                    return List.nil;
                list = list.evaluate_elements(variables);
                try {
                    return ((Number) list.getCar()).equal((Number) list.getCdr().getCar());
                } catch (ClassCastException e) {
                    System.out.println("builtin arithmetic rel op given non-number");
                    return List.nil;
                }
            }
        });

        variables.put("/=", new Function("/=", 2) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null || list == List.nil)
                    return List.nil;
                list = list.evaluate_elements(variables);
                try {
                    return ((Number) list.getCar()).notEqual((Number) list.getCdr().getCar());
                } catch (ClassCastException e) {
                    System.out.println("builtin arithmetic rel op given non-number");
                    return List.nil;
                }
            }
        });

        variables.put("<", new Function("<", 2) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null || list == List.nil)
                    return List.nil;
                list = list.evaluate_elements(variables);
                try {
                    return ((Number) list.getCar()).lessThan((Number) list.getCdr().getCar());
                } catch (ClassCastException e) {
                    System.out.println("builtin arithmetic rel op given non-number");
                    return List.nil;
                }
            }
        });

        variables.put(">", new Function(">", 2) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null || list == List.nil)
                    return List.nil;
                list = list.evaluate_elements(variables);
                try {
                    return ((Number) list.getCar()).greaterThan((Number) list.getCdr().getCar());
                } catch (ClassCastException e) {
                    System.out.println("builtin arithmetic rel op given non-number");
                    return List.nil;
                }
            }
        });

        variables.put("<=", new Function("<=", 2) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null || list == List.nil)
                    return List.nil;
                list = list.evaluate_elements(variables);
                try {
                    return ((Number) list.getCar()).lessThanOrEqual((Number) list.getCdr().getCar());
                } catch (ClassCastException e) {
                    System.out.println("builtin arithmetic rel op given non-number");
                    return List.nil;
                }
            }
        });

        variables.put(">=", new Function(">=", 2) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (!checkArity(list, variables) || list == null || list == List.nil)
                    return List.nil;
                list = list.evaluate_elements(variables);
                try {
                    return ((Number) list.getCar()).greaterThanOrEqual((Number) list.getCdr().getCar());
                } catch (ClassCastException e) {
                    System.out.println("builtin arithmetic rel op given non-number");
                    return List.nil;
                }
            }
        });

        variables.put("cond", new Function("cond", -1) {
            public Expression call(List list, Map<String,Expression> variables) {
                if (list == List.nil || list == null)
                    return List.nil;
                if (list.getCar() == List.nil)
                    return List.nil;
                
                return Function.chain_cond(list, variables);
            }
        });
    }
}

