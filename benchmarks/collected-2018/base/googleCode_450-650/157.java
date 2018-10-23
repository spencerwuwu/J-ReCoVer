// https://searchcode.com/api/result/12203857/

/*
 * This file is part of Cadmium.
 * Copyright (C) 2007-2010 Xavier Clerc.
 *
 * Cadmium is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cadmium is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.x9c.cadmium.primitives.stdlib;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * Implements all primitives from 'parsing.c'.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.2
 * @since 1.0
 */
@PrimitiveProvider
public final class Parsing {

    /** Base for error code. */
    private static final int ERRCODE = 256;

    /** Index of 'actions' field in 'parser_tables'. */
    private static final int ACTIONS = 0;

    /** Index of 'transl_const' field in 'parser_tables'. */
    private static final int TRANSL_CONST = 1;

    /** Index of 'transl_block' field in 'parser_tables'. */
    private static final int TRANSL_BLOCK = 2;

    /** Index of 'lhs' field in 'parser_tables'. */
    private static final int LHS = 3;

    /** Index of 'len' field in 'parser_tables'. */
    private static final int LEN = 4;

    /** Index of 'defred' field in 'parser_tables'. */
    private static final int DEFRED = 5;

    /** Index of 'dgoto' field in 'parser_tables'. */
    private static final int DGOTO = 6;

    /** Index of 'sindex' field in 'parser_tables'. */
    private static final int SINDEX = 7;

    /** Index of 'rindex' field in 'parser_tables'. */
    private static final int RINDEX = 8;

    /** Index of 'gindex' field in 'parser_tables'. */
    private static final int GINDEX = 9;

    /** Index of 'tablesize' field in 'parser_tables'. */
    private static final int TABLESIZE = 10;

    /** Index of 'table' field in 'parser_tables'. */
    private static final int TABLE = 11;

    /** Index of 'check' field in 'parser_tables'. */
    private static final int CHECK = 12;

    /** Index of 'error_function' field in 'parser_tables'. */
    private static final int ERROR_FUNCTION = 13;

    /** Index of 'names_const' field in 'parser_tables'. */
    private static final int NAMES_CONST = 14;

    /** Index of 'names_block' field in 'parser_tables'. */
    private static final int NAMES_BLOCK = 15;

    /** Index of 's_stack' field in 'parser_env'. */
    private static final int S_STACK = 0;

    /** Index of 'v_stack' field in 'parser_env'. */
    private static final int V_STACK = 1;

    /** Index of 'symb_start_stack' field in 'parser_env'. */
    private static final int SYMB_START_STACK = 2;

    /** Index of 'symb_end_stack' field in 'parser_env'. */
    private static final int SYMB_END_STACK = 3;

    /** Index of 'stacksize' field in 'parser_env'. */
    private static final int STACKSIZE = 4;

    /** Index of 'stackbase' field in 'parser_env'. */
    private static final int STACKBASE = 5;

    /** Index of 'curr_char' field in 'parser_env'. */
    private static final int CURR_CHAR = 6;

    /** Index of 'lval' field in 'parser_env'. */
    private static final int LVAL = 7;

    /** Index of 'symb_start' field in 'parser_env'. */
    private static final int SYMB_START = 8;

    /** Index of 'symb_end' field in 'parser_env'. */
    private static final int SYMB_END = 9;

    /** Index of 'asp' field in 'parser_env'. */
    private static final int ASP = 10;

    /** Index of 'rule_len' field in 'parser_env'. */
    private static final int RULE_LEN = 11;

    /** Index of 'rule_number' field in 'parser_env'. */
    private static final int RULE_NUMBER = 12;

    /** Index of 'sp' field in 'parser_env'. */
    private static final int SP = 13;

    /** Index of 'state' field in 'parser_env'. */
    private static final int STATE = 14;

    /** Index of 'errflag' field in 'parser_env'. */
    private static final int ERRFLAG = 15;

    /** Input code / command. */
    private static final int START = 0;

    /** Input code / command. */
    private static final int TOKEN_READ = 1;

    /** Input code / command. */
    private static final int STACKS_GROWN_1 = 2;

    /** Input code / command. */
    private static final int STACKS_GROWN_2 = 3;

    /** Input code / command. */
    private static final int SEMANTIC_ACTION_COMPUTED = 4;

    /** Input code / command. */
    private static final int ERROR_DETECTED = 5;

    /** Output code. */
    private static final Value READ_TOKEN = Value.ZERO;

    /** Output code. */
    private static final Value RAISE_PARSE_ERROR = Value.ONE;

    /** Output code. */
    private static final Value GROW_STACKS_1 = Value.TWO;

    /** Output code. */
    private static final Value GROW_STACKS_2 = Value.createFromLong(3);

    /** Output code. */
    private static final Value COMPUTE_SEMANTIC_ACTION = Value.createFromLong(4);

    /** output code. */
    private static final Value CALL_ERROR_FUNCTION = Value.createFromLong(5);

    /** To fake/emulate gotos ... */
    private static final int NO_GOTO = 0;

    /** To fake/emulate gotos ... */
    private static final int GOTO_LOOP = 1;

    /** To fake/emulate gotos ... */
    private static final int GOTO_REDUCE = 2;

    /** To fake/emulate gotos ... */
    private static final int GOTO_TESTSHIFT = 3;

    /** To fake/emulate gotos ... */
    private static final int GOTO_SHIFT = 4;

    /** To fake/emulate gotos ... */
    private static final int GOTO_RECOVER = 5;

    /** To fake/emulate gotos ... */
    private static final int GOTO_SHIFT_RECOVER = 6;

    /** To fake/emulate gotos ... */
    private static final int GOTO_PUSH = 7;

    /** To fake/emulate gotos ... */
    private static final int GOTO_SEMANTIC_ACTION = 8;

    /**
     * No instance of this class.
     */
    private Parsing() {
    } // end empty constructor

    /**
     * Runs a step of the parsing engine automaton.
     * @param ctxt context
     * @param parserTables parser tables
     * @param parserEnv parser environment
     * @param cmd command (what parsing engine just did)
     * @param arg argument
     * @return what parsing engine should do next
     */
    @Primitive
    public static Value caml_parse_engine(final CodeRunner ctxt,
                                          final Value parserTables,
                                          final Value parserEnv,
                                          final Value cmd,
                                          final Value arg) {
        final Context cnt = ctxt.getContext();
        final PrintStream err;
        if (cnt.isParserTraceEnabled()) {
            err = new PrintStream(cnt.getChannel(Channel.STDERR).asOutputStream());
        } else {
            err = null;
        } // end if/else
        final Block tables = parserTables.asBlock();
        final Block env = parserEnv.asBlock();
        int state = 0;
        int sp = 0;
        int asp = 0;
        int errFlag = 0;
        int n = 0;
        int n1 = 0;
        int n2 = 0;
        int m = 0;
        int state1 = 0;
        int gto = Parsing.NO_GOTO;
        switch (cmd.asLong()) {
        case Parsing.START:
            state = 0;
            sp = env.get(SP).asLong();
            errFlag = 0;
            gto = Parsing.GOTO_LOOP;
            break;
        case Parsing.TOKEN_READ:
            { /* RESTORE */
                sp = env.get(Parsing.SP).asLong();
                state = env.get(Parsing.STATE).asLong();
                errFlag = env.get(Parsing.ERRFLAG).asLong();
            }
            if (arg.isBlock()) {
                final Block bl = arg.asBlock();
                env.set(Parsing.CURR_CHAR,
                        tables.get(Parsing.TRANSL_BLOCK).asBlock().get(bl.getTag()));
                env.set(Parsing.LVAL, bl.get(0));
            } else {
                env.set(Parsing.CURR_CHAR,
                        tables.get(Parsing.TRANSL_CONST).asBlock().get(arg.asLong()));
                env.set(Parsing.LVAL, Value.ZERO);
            } // end if/else
            if (err != null) printToken(err, tables, state, arg);
            gto = Parsing.GOTO_TESTSHIFT;
            break;
        case Parsing.ERROR_DETECTED:
            { /* RESTORE */
                sp = env.get(Parsing.SP).asLong();
                state = env.get(Parsing.STATE).asLong();
                errFlag = env.get(Parsing.ERRFLAG).asLong();
            }
            gto = Parsing.GOTO_RECOVER;
            break;
        case Parsing.STACKS_GROWN_1:
            { /* RESTORE */
                sp = env.get(Parsing.SP).asLong();
                state = env.get(Parsing.STATE).asLong();
                errFlag = env.get(Parsing.ERRFLAG).asLong();
            }
            gto = Parsing.GOTO_PUSH;
            break;
        case Parsing.STACKS_GROWN_2:
            { /* RESTORE */
                sp = env.get(Parsing.SP).asLong();
                state = env.get(Parsing.STATE).asLong();
                errFlag = env.get(Parsing.ERRFLAG).asLong();
            }
            gto = Parsing.GOTO_SEMANTIC_ACTION;
            break;
        case Parsing.SEMANTIC_ACTION_COMPUTED:
            { /* RESTORE */
                sp = env.get(Parsing.SP).asLong();
                state = env.get(Parsing.STATE).asLong();
                errFlag = env.get(Parsing.ERRFLAG).asLong();
            }
            env.get(Parsing.S_STACK).asBlock().set(sp, Value.createFromLong(state));
            env.get(Parsing.V_STACK).asBlock().set(sp, arg);
            asp = env.get(Parsing.ASP).asLong();
            env.get(Parsing.SYMB_END_STACK).asBlock().set(sp, env.get(Parsing.SYMB_END_STACK).asBlock().get(asp));
            if (sp > asp) {
                env.get(Parsing.SYMB_START_STACK).asBlock().set(sp, env.get(Parsing.SYMB_END_STACK).asBlock().get(asp));
            } // end if
            gto = Parsing.GOTO_LOOP;
            break;
        default:
            assert false : "invalid command";
            return Parsing.RAISE_PARSE_ERROR;
        } // end switch

        while (true) {
            switch (gto) {
            case Parsing.GOTO_LOOP:
                n = Lexing.getShort(tables.get(Parsing.DEFRED), state);
                if (n != 0) {
                    gto = Parsing.GOTO_REDUCE;
                    break;
                } // end if
                if (env.get(Parsing.CURR_CHAR).asLong() >= 0) {
                    gto = Parsing.GOTO_TESTSHIFT;
                    break;
                } // end if
                { /* SAVE */
                    env.set(Parsing.SP, Value.createFromLong(sp));
                    env.set(Parsing.STATE, Value.createFromLong(state));
                    env.set(Parsing.ERRFLAG, Value.createFromLong(errFlag));
                }
                return Parsing.READ_TOKEN;
            case Parsing.GOTO_REDUCE:
                if (err != null) err.printf("State %d: reduce by rule %d\n", state, n);
                m = Lexing.getShort(tables.get(Parsing.LEN), n);
                env.set(Parsing.ASP, Value.createFromLong(sp));
                env.set(Parsing.RULE_NUMBER, Value.createFromLong(n));
                env.set(Parsing.RULE_LEN, Value.createFromLong(m));
                sp = sp - m + 1;
                m = Lexing.getShort(tables.get(Parsing.LHS), n);
                state1 = env.get(Parsing.S_STACK).asBlock().get(sp - 1).asLong();
                n1 = Lexing.getShort(tables.get(Parsing.GINDEX), m);
                n2 = n1 + state1;
                if ((n1 != 0) && (n2 >= 0)
                    && (n2 <= tables.get(Parsing.TABLESIZE).asLong())
                    && (Lexing.getShort(tables.get(Parsing.CHECK), n2) == state1)) {
                    state = Lexing.getShort(tables.get(Parsing.TABLE), n2);
                } else {
                    state = Lexing.getShort(tables.get(Parsing.DGOTO), m);
                } // end if/else
                if (sp < env.get(Parsing.STACKSIZE).asLong()) {
                    gto = Parsing.GOTO_SEMANTIC_ACTION;
                    break;
                } // end if
                { /* SAVE */
                    env.set(Parsing.SP, Value.createFromLong(sp));
                    env.set(Parsing.STATE, Value.createFromLong(state));
                    env.set(Parsing.ERRFLAG, Value.createFromLong(errFlag));
                }
                return Parsing.GROW_STACKS_2;
            case Parsing.GOTO_TESTSHIFT:
                n1 = Lexing.getShort(tables.get(Parsing.SINDEX), state);
                n2 = n1 + env.get(Parsing.CURR_CHAR).asLong();
                if ((n1 != 0) && (n2 >= 0)
                    && (n2 <= tables.get(Parsing.TABLESIZE).asLong())
                    && (Lexing.getShort(tables.get(Parsing.CHECK), n2) == env.get(Parsing.CURR_CHAR).asLong())) {
                    gto = Parsing.GOTO_SHIFT;
                    break;
                } // end if
                n1 = Lexing.getShort(tables.get(Parsing.RINDEX), state);
                n2 = n1 + env.get(Parsing.CURR_CHAR).asLong();
                if ((n1 != 0) && (n2 >= 0)
                    && (n2 <= tables.get(TABLESIZE).asLong())
                    && (Lexing.getShort(tables.get(Parsing.CHECK), n2) == env.get(Parsing.CURR_CHAR).asLong())) {
                    n = Lexing.getShort(tables.get(Parsing.TABLE), n2);
                    gto = Parsing.GOTO_REDUCE;
                    break;
                } // end if
                if (errFlag > 0) {
                    gto = Parsing.GOTO_RECOVER;
                    break;
                } // end if
                { /* SAVE */
                    env.set(Parsing.SP, Value.createFromLong(sp));
                    env.set(Parsing.STATE, Value.createFromLong(state));
                    env.set(Parsing.ERRFLAG, Value.createFromLong(errFlag));
                }
                return Parsing.CALL_ERROR_FUNCTION;
            case Parsing.GOTO_SHIFT:
                env.set(Parsing.CURR_CHAR, Value.MINUS_ONE);
                if (errFlag > 0) {
                    errFlag--;
                } // end if
                gto = Parsing.GOTO_SHIFT_RECOVER;
                break;
            case Parsing.GOTO_RECOVER:
                if (errFlag < 3) {
                    errFlag = 3;
                    while (true) {
                        state1 = env.get(Parsing.S_STACK).asBlock().get(sp).asLong();
                        n1 = Lexing.getShort(tables.get(Parsing.SINDEX), state1);
                        n2 = n1 + Parsing.ERRCODE;
                        if ((n1 != 0) && (n2 >= 0)
                            && (n2 <= tables.get(Parsing.TABLESIZE).asLong())
                            && (Lexing.getShort(tables.get(Parsing.CHECK), n2) == Parsing.ERRCODE)) {
                            if (err != null) err.printf("Recovering in state %d\n", state1);
                            gto = Parsing.GOTO_SHIFT_RECOVER;
                            break;
                        } else {
                            if (err != null) err.printf("Discarding state %d\n", state1);
                            if (sp <= env.get(Parsing.STACKBASE).asLong()) {
                            if (err != null) err.printf("No more states to discard\n");
                                return Parsing.RAISE_PARSE_ERROR;
                            } // end if/else
                            sp--;
                        } // end if/else
                    } // end while
                } else {
                    if (env.get(Parsing.CURR_CHAR).asLong() == 0) {
                        return Parsing.RAISE_PARSE_ERROR;
                    } // end if
                    if (err != null) err.printf("Discarding last token read\n");
                    env.set(Parsing.CURR_CHAR, Value.MINUS_ONE);
                    gto = Parsing.GOTO_LOOP;
                    break;
                } // end if/else
                gto = Parsing.GOTO_SHIFT;
                break;
            case Parsing.GOTO_SHIFT_RECOVER:
                if (err != null) err.printf("State %d: shift to state %d\n",
                                            state,
                                            Lexing.getShort(tables.get(Parsing.TABLE), n2));
                state = Lexing.getShort(tables.get(Parsing.TABLE), n2);
                sp++;
                if (sp < env.get(Parsing.STACKSIZE).asLong()) {
                    gto = Parsing.GOTO_PUSH;
                    break;
                } // end if
                { /* SAVE */
                    env.set(Parsing.SP, Value.createFromLong(sp));
                    env.set(Parsing.STATE, Value.createFromLong(state));
                    env.set(Parsing.ERRFLAG, Value.createFromLong(errFlag));
                }
                return Parsing.GROW_STACKS_1;
            case Parsing.GOTO_PUSH:
                env.get(Parsing.S_STACK).asBlock().set(sp, Value.createFromLong(state));
                env.get(Parsing.V_STACK).asBlock().set(sp, env.get(Parsing.LVAL));
                env.get(Parsing.SYMB_START_STACK).asBlock().set(sp, env.get(Parsing.SYMB_START));
                env.get(Parsing.SYMB_END_STACK).asBlock().set(sp, env.get(Parsing.SYMB_END));
                gto = Parsing.GOTO_LOOP;
                break;
            case Parsing.GOTO_SEMANTIC_ACTION:
                { /* SAVE */
                    env.set(Parsing.SP, Value.createFromLong(sp));
                    env.set(Parsing.STATE, Value.createFromLong(state));
                    env.set(Parsing.ERRFLAG, Value.createFromLong(errFlag));
                }
                return Parsing.COMPUTE_SEMANTIC_ACTION;
            default:
                assert false : "invalid goto";
                return Parsing.RAISE_PARSE_ERROR;
            } // end switch
        } // end while
    } // end method 'caml_parse_engine(CodeRunner, Value, Value, Value, Value)'

    /**
     * Sets the state of the parser trace.
     * @param ctxt context
     * @param v new state of parser trace
     * @return old state of parser trace
     */
    @Primitive
    public static Value caml_set_parser_trace(final CodeRunner ctxt,
                                              final Value v) {
        final Context c = ctxt.getContext();
        final Value old = c.isParserTraceEnabled() ? Value.TRUE : Value.FALSE;
        c.setParserTrace(v != Value.FALSE);
        return old;
    } // end method 'caml_set_parser_trace(Value)'

    /**
     * Prints the passed token.
     * @param err where token should be printed
     * @param tables parser tables
     * @param state parser state
     * @param tok parser token
     */
    private static void printToken(final PrintStream err,
                                   final Block tables,
                                   final int state,
                                   final Value tok) {
        if (tok.isLong()) {
            err.printf("State %d: read token %s\n",
                       state,
                       getTokenName(tables.get(Parsing.NAMES_CONST), tok.asLong()));
        } else {
            err.printf("State %d: read token %s(",
                       state,
                       getTokenName(tables.get(Parsing.NAMES_BLOCK), tok.asBlock().getTag()));
            final Value v = tok.asBlock().get(0);
            if (v.isLong()) {
                err.printf("%d", v.asLong());
            } else {
                final Block b = v.asBlock();
                switch (b.getTag()) {
                case Block.STRING_TAG:
                    err.print(b.asString());
                    break;
                case Block.DOUBLE_TAG:
                    final StringBuilder sb = new StringBuilder();
                    final Formatter fmt = new Formatter(sb, Locale.US);
                    fmt.format("%g", b.asDouble());
                    final boolean exp = (sb.indexOf("e") >= 0) || (sb.indexOf("E") >= 0);
                    if (!exp) {
                        while (sb.charAt(sb.length() - 1) == '0') {
                            sb.deleteCharAt(sb.length() - 1);
                        } // end while
                    } // end if
                    if (sb.charAt(sb.length() - 1) == '.') {
                        sb.deleteCharAt(sb.length() - 1);
                    } // end if
                    err.print(sb.toString());
                    break;
                default:
                    err.print("_");
                    break;
                } // end switch
            } // end if/else
            err.printf(")\n");
        } // end if/else
    } // end method 'printToken(PrintStream, Block, int, Value)'

    /**
     * Returns the token name at a given index.
     * @param names names as a string
     * @param idx token index
     * @return the token name at a given index
     */
    private static String getTokenName(final Value names, final int idx) {
        final byte[] bytes = names.asBlock().getBytes();
        final int len = bytes.length;
        int p = 0;
        final int n = idx <= 0 ? 0 : idx;
        if (n == 0) {
            int j = 1;
            while  ((j < len) && (bytes[j] != 0)) {
                j++;
            } // end while
            final Block b = Block.createString(Arrays.copyOfRange(bytes, 0, j));
            return b.asString();
        } // end if
        for (int i = 0; i < len; i++) {
            if (bytes[i] == 0) {
                p++;
                if (n == p) {
                    int j = i + 1;
                    while ((j < len) && (bytes[j] != 0)) {
                        j++;
                    } // end while
                    final Block b = Block.createString(Arrays.copyOfRange(bytes, i + 1, j));
                    return b.asString();
                } // end if
            } // end if
        } // end for
        return "<unknown token>";
    } // end method 'getTokenName(Value, int)'

} // end class 'Parsing'

