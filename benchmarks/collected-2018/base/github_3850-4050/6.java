// https://searchcode.com/api/result/5685282/

// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "DefaultRubyParser.y"
/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2007 Mirko Stocker <me@misto.ch>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.parser;

import java.io.IOException;

import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BlockAcceptingNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNoArgBlockNode;
import org.jruby.ast.FCallNoArgNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LiteralNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilImplicitNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RestArgNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.TypedArgumentNode;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.ZYieldNode;
import org.jruby.ast.ZeroArgNode;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.ISourcePositionHolder;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.lexer.yacc.RubyYaccLexer;
import org.jruby.lexer.yacc.RubyYaccLexer.LexState;
import org.jruby.lexer.yacc.StrTerm;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.SyntaxException.PID;
import org.jruby.lexer.yacc.Token;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;

public class DefaultRubyParser implements RubyParser {
    protected ParserSupport support;
    protected RubyYaccLexer lexer;

    public DefaultRubyParser() {
        this(new ParserSupport());
    }

    public DefaultRubyParser(ParserSupport support) {
        this.support = support;
        lexer = new RubyYaccLexer();
        lexer.setParserSupport(support);
        support.setLexer(lexer);
    }

    public void setWarnings(IRubyWarnings warnings) {
        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
					// line 156 "-"
  // %token constants
  public static final int kCLASS = 257;
  public static final int kMODULE = 258;
  public static final int kDEF = 259;
  public static final int kUNDEF = 260;
  public static final int kBEGIN = 261;
  public static final int kRESCUE = 262;
  public static final int kENSURE = 263;
  public static final int kEND = 264;
  public static final int kIF = 265;
  public static final int kUNLESS = 266;
  public static final int kTHEN = 267;
  public static final int kELSIF = 268;
  public static final int kELSE = 269;
  public static final int kCASE = 270;
  public static final int kWHEN = 271;
  public static final int kWHILE = 272;
  public static final int kUNTIL = 273;
  public static final int kFOR = 274;
  public static final int kBREAK = 275;
  public static final int kNEXT = 276;
  public static final int kREDO = 277;
  public static final int kRETRY = 278;
  public static final int kIN = 279;
  public static final int kDO = 280;
  public static final int kDO_COND = 281;
  public static final int kDO_BLOCK = 282;
  public static final int kRETURN = 283;
  public static final int kYIELD = 284;
  public static final int kSUPER = 285;
  public static final int kSELF = 286;
  public static final int kNIL = 287;
  public static final int kTRUE = 288;
  public static final int kFALSE = 289;
  public static final int kAND = 290;
  public static final int kOR = 291;
  public static final int kNOT = 292;
  public static final int kIF_MOD = 293;
  public static final int kUNLESS_MOD = 294;
  public static final int kWHILE_MOD = 295;
  public static final int kUNTIL_MOD = 296;
  public static final int kRESCUE_MOD = 297;
  public static final int kALIAS = 298;
  public static final int kDEFINED = 299;
  public static final int klBEGIN = 300;
  public static final int klEND = 301;
  public static final int k__LINE__ = 302;
  public static final int k__FILE__ = 303;
  public static final int k__ENCODING__ = 304;
  public static final int kDO_LAMBDA = 305;
  public static final int tIDENTIFIER = 306;
  public static final int tFID = 307;
  public static final int tGVAR = 308;
  public static final int tIVAR = 309;
  public static final int tCONSTANT = 310;
  public static final int tCVAR = 311;
  public static final int tLABEL = 312;
  public static final int tCHAR = 313;
  public static final int tUPLUS = 314;
  public static final int tUMINUS = 315;
  public static final int tUMINUS_NUM = 316;
  public static final int tPOW = 317;
  public static final int tCMP = 318;
  public static final int tEQ = 319;
  public static final int tEQQ = 320;
  public static final int tNEQ = 321;
  public static final int tGEQ = 322;
  public static final int tLEQ = 323;
  public static final int tANDOP = 324;
  public static final int tOROP = 325;
  public static final int tMATCH = 326;
  public static final int tNMATCH = 327;
  public static final int tDOT = 328;
  public static final int tDOT2 = 329;
  public static final int tDOT3 = 330;
  public static final int tAREF = 331;
  public static final int tASET = 332;
  public static final int tLSHFT = 333;
  public static final int tRSHFT = 334;
  public static final int tCOLON2 = 335;
  public static final int tCOLON3 = 336;
  public static final int tOP_ASGN = 337;
  public static final int tASSOC = 338;
  public static final int tLPAREN = 339;
  public static final int tLPAREN2 = 340;
  public static final int tRPAREN = 341;
  public static final int tLPAREN_ARG = 342;
  public static final int tLBRACK = 343;
  public static final int tRBRACK = 344;
  public static final int tLBRACE = 345;
  public static final int tLBRACE_ARG = 346;
  public static final int tSTAR = 347;
  public static final int tSTAR2 = 348;
  public static final int tAMPER = 349;
  public static final int tAMPER2 = 350;
  public static final int tTILDE = 351;
  public static final int tPERCENT = 352;
  public static final int tDIVIDE = 353;
  public static final int tPLUS = 354;
  public static final int tMINUS = 355;
  public static final int tLT = 356;
  public static final int tGT = 357;
  public static final int tPIPE = 358;
  public static final int tBANG = 359;
  public static final int tCARET = 360;
  public static final int tLCURLY = 361;
  public static final int tRCURLY = 362;
  public static final int tBACK_REF2 = 363;
  public static final int tSYMBEG = 364;
  public static final int tSTRING_BEG = 365;
  public static final int tXSTRING_BEG = 366;
  public static final int tREGEXP_BEG = 367;
  public static final int tWORDS_BEG = 368;
  public static final int tQWORDS_BEG = 369;
  public static final int tSTRING_DBEG = 370;
  public static final int tSTRING_DVAR = 371;
  public static final int tSTRING_END = 372;
  public static final int tLAMBDA = 373;
  public static final int tLAMBEG = 374;
  public static final int tNTH_REF = 375;
  public static final int tBACK_REF = 376;
  public static final int tSTRING_CONTENT = 377;
  public static final int tINTEGER = 378;
  public static final int tFLOAT = 379;
  public static final int tREGEXP_END = 380;
  public static final int tLOWEST = 381;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 512
    -1,   102,     0,    32,    31,    33,    33,    33,    33,   105,
    34,    34,    34,    34,    34,    34,    34,    34,    34,    34,
   106,    34,    34,    34,    34,    34,    34,    34,    34,    34,
    34,    34,    34,    34,    34,    35,    35,    35,    35,    35,
    35,    39,    30,    30,    30,    30,    30,    55,    55,    55,
   107,    90,    38,    38,    38,    38,    38,    38,    38,    38,
    91,    91,    93,    93,    92,    92,    92,    92,    92,    92,
    63,    63,    78,    78,    64,    64,    64,    64,    64,    64,
    64,    64,    71,    71,    71,    71,    71,    71,    71,    71,
     7,     7,    29,    29,    29,     8,     8,     8,     8,     8,
    96,    96,    97,    97,    59,   109,    59,     9,     9,     9,
     9,     9,     9,     9,     9,     9,     9,     9,     9,     9,
     9,     9,     9,     9,     9,     9,     9,     9,     9,     9,
     9,     9,     9,   108,   108,   108,   108,   108,   108,   108,
   108,   108,   108,   108,   108,   108,   108,   108,   108,   108,
   108,   108,   108,   108,   108,   108,   108,   108,   108,   108,
   108,   108,   108,   108,   108,   108,   108,   108,   108,   108,
   108,   108,   108,   108,    36,    36,    36,    36,    36,    36,
    36,    36,    36,    36,    36,    36,    36,    36,    36,    36,
    36,    36,    36,    36,    36,    36,    36,    36,    36,    36,
    36,    36,    36,    36,    36,    36,    36,    36,    36,    36,
    36,    36,    36,    36,    36,    36,    36,    36,    65,    68,
    68,    68,    68,    68,    68,    49,    49,    49,    49,    53,
    53,    45,    45,    45,    45,    45,    45,    45,    45,    45,
    46,    46,    46,    46,    46,    46,    46,    46,    46,    46,
    46,    46,   112,    51,    47,   113,    47,   114,    47,    84,
    83,    83,    77,    77,    62,    62,    62,    37,    37,    37,
    37,    37,    37,    37,    37,    37,    37,   115,    37,    37,
    37,    37,    37,    37,    37,    37,    37,    37,    37,    37,
    37,    37,    37,    37,    37,   117,   119,    37,   120,   121,
    37,    37,    37,    37,   122,   123,    37,   124,    37,   126,
   127,    37,   128,    37,   129,    37,   130,   131,    37,    37,
    37,    37,    37,    40,   116,   116,   116,   116,   118,   118,
   118,    43,    43,    41,    41,    98,    98,    99,    99,    69,
    69,    69,    69,    69,    69,    69,    69,    69,    69,    69,
    69,    70,    70,    70,    70,   132,    89,    54,    54,    54,
    22,    22,    22,    22,    22,    22,   133,    88,   134,    88,
    66,    82,    82,    82,    42,    42,    94,    94,    67,    67,
    67,    44,    44,    48,    48,    26,    26,    26,    14,    15,
    15,    16,    17,    18,    23,    23,    74,    74,    25,    25,
    24,    24,    73,    73,    19,    19,    20,    20,    21,   135,
    21,   136,    21,    60,    60,    60,    60,     3,     2,     2,
     2,     2,    28,    27,    27,    27,    27,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,    52,    95,
    61,    61,    50,   137,    50,    50,    56,    56,    57,    57,
    57,    57,    57,    57,    57,    57,    57,   100,   100,   100,
   100,   100,    75,    75,    75,    75,    58,    76,    76,    11,
    11,   101,   101,    12,    12,    87,    86,    86,    13,   138,
    13,    81,    81,    81,    79,    79,    80,     4,     4,     4,
     5,     5,     5,     5,     6,     6,     6,    10,    10,   103,
   103,   110,   110,   111,   111,   111,   125,   125,   104,   104,
    72,    85,
    }, yyLen = {
//yyLen 512
     2,     0,     2,     4,     2,     1,     1,     3,     2,     0,
     4,     3,     3,     3,     2,     3,     3,     3,     3,     3,
     0,     5,     4,     3,     3,     3,     6,     5,     5,     5,
     3,     3,     3,     3,     1,     1,     3,     3,     2,     2,
     1,     1,     1,     1,     2,     2,     2,     1,     4,     4,
     0,     5,     2,     3,     4,     5,     4,     5,     2,     2,
     1,     3,     1,     3,     1,     2,     3,     2,     2,     1,
     1,     3,     2,     3,     1,     4,     3,     3,     3,     3,
     2,     1,     1,     4,     3,     3,     3,     3,     2,     1,
     1,     1,     2,     1,     3,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     0,     4,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     3,     5,     3,     6,     5,     5,
     5,     5,     4,     3,     3,     3,     3,     3,     3,     3,
     3,     3,     4,     4,     2,     2,     3,     3,     3,     3,
     3,     3,     3,     3,     3,     3,     3,     3,     3,     2,
     2,     3,     3,     3,     3,     3,     5,     1,     1,     1,
     2,     2,     5,     2,     3,     3,     4,     4,     6,     1,
     1,     1,     2,     5,     2,     5,     4,     7,     3,     1,
     4,     3,     5,     7,     2,     5,     4,     6,     7,     9,
     3,     1,     0,     2,     1,     0,     3,     0,     4,     2,
     2,     1,     1,     3,     3,     4,     2,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     3,     0,     5,     3,
     3,     2,     4,     3,     3,     1,     4,     3,     1,     5,
     2,     1,     2,     6,     6,     0,     0,     7,     0,     0,
     7,     5,     4,     5,     0,     0,     9,     0,     6,     0,
     0,     8,     0,     5,     0,     6,     0,     0,     9,     1,
     1,     1,     1,     1,     1,     1,     1,     2,     1,     1,
     1,     1,     5,     1,     2,     1,     1,     1,     3,     1,
     2,     4,     7,     6,     4,     3,     5,     4,     2,     1,
     2,     1,     2,     1,     3,     0,     5,     2,     4,     4,
     2,     4,     4,     3,     2,     1,     0,     5,     0,     5,
     5,     1,     4,     2,     1,     1,     6,     0,     1,     1,
     1,     2,     1,     2,     1,     1,     1,     1,     1,     1,
     2,     3,     3,     3,     3,     3,     0,     3,     1,     2,
     3,     3,     0,     3,     0,     2,     0,     2,     1,     0,
     3,     0,     4,     1,     1,     1,     1,     2,     1,     1,
     1,     1,     3,     1,     1,     2,     2,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     0,     4,     2,     4,     2,     6,     4,
     4,     2,     4,     2,     2,     1,     0,     1,     1,     1,
     1,     1,     3,     1,     5,     3,     3,     1,     3,     1,
     1,     2,     1,     1,     1,     2,     2,     0,     1,     0,
     5,     1,     2,     2,     1,     3,     3,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     0,
     1,     0,     1,     0,     1,     1,     1,     1,     1,     2,
     0,     0,
    }, yyDefRed = {
//yyDefRed 914
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   295,   298,     0,     0,     0,   321,   322,     0,
     0,     0,   433,   432,   434,   435,     0,     0,     0,    20,
     0,   437,   436,     0,     0,   429,   428,     0,   431,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   404,   406,   406,     0,     0,   440,   441,   423,   424,
     0,   386,     0,   268,     0,   389,   269,   270,     0,   271,
   272,   267,   385,   387,    35,     2,     0,     0,     0,     0,
     0,     0,     0,   273,     0,    43,     0,     0,    70,     0,
     5,     0,     0,    60,     0,     0,   319,   320,   285,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   438,     0,
    93,     0,   323,     0,   274,   312,   142,   153,   143,   166,
   139,   159,   149,   148,   164,   147,   146,   141,   167,   151,
   140,   154,   158,   160,   152,   145,   161,   168,   163,     0,
     0,     0,     0,   138,   157,   156,   169,   170,   171,   172,
   173,   137,   144,   135,   136,     0,     0,     0,    97,     0,
   128,   129,   126,   110,   111,   112,   115,   117,   113,   130,
   131,   118,   119,   479,   123,   122,   109,   127,   125,   124,
   120,   121,   116,   114,   107,   108,   132,   314,    98,     0,
   478,    99,   162,   155,   165,   150,   133,   134,    95,    96,
   101,   100,   103,     0,   102,   104,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   507,   506,     0,
     0,     0,   508,     0,     0,     0,     0,     0,     0,   335,
   336,     0,     0,     0,     0,     0,   231,    45,     0,     0,
     0,   484,   239,    46,    44,     0,    59,     0,     0,   364,
    58,    38,     0,     9,   502,     0,     0,     0,   194,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   219,     0,     0,   481,     0,     0,     0,     0,
     0,     0,     0,    68,   210,    39,   209,   420,   419,   421,
   417,   418,     0,     0,     0,     0,     0,     0,     0,     0,
   368,   366,   360,     0,   290,   390,   292,     4,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   355,   357,     0,     0,     0,     0,     0,     0,
    72,     0,     0,     0,     0,     0,     0,     0,   425,   426,
     0,    90,     0,    92,     0,   443,   307,   442,     0,     0,
     0,     0,     0,     0,   497,   498,   316,   105,     0,     0,
   276,     0,   326,   325,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   509,     0,     0,
     0,     0,     0,     0,   304,     0,   259,     0,     0,   232,
   261,     0,   234,   287,     0,     0,   254,   253,     0,     0,
     0,     0,     0,    11,    13,    12,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   279,     0,     0,
     0,   220,   283,     0,   504,   221,     0,   223,     0,   483,
   482,   284,     0,     0,     0,     0,   411,   409,   422,   408,
   407,   391,   405,   392,   393,   394,   395,   398,     0,   400,
   401,     0,     0,     0,    50,    53,     0,    15,    16,    17,
    18,    19,    36,    37,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   492,     0,     0,   493,     0,     0,     0,     0,
   363,     0,     0,   490,   491,     0,     0,    30,     0,     0,
    23,     0,    31,   262,     0,     0,    66,    73,    24,    33,
     0,    25,     0,     0,   445,     0,     0,     0,     0,     0,
     0,    94,     0,     0,     0,     0,   459,   458,   457,   460,
     0,   470,   469,   474,   473,     0,     0,     0,     0,   467,
     0,     0,   455,     0,     0,     0,     0,   379,     0,     0,
   380,     0,     0,   333,     0,   327,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   302,   330,
   329,   296,   328,   299,     0,     0,     0,     0,     0,     0,
     0,   238,   486,     0,     0,     0,   260,     0,     0,   485,
   286,     0,     0,   257,     0,     0,   251,     0,     0,     0,
     0,     0,   225,     0,    10,     0,     0,    22,     0,     0,
     0,     0,     0,   224,     0,   263,     0,     0,     0,     0,
     0,     0,     0,   397,   399,   403,   353,     0,     0,   351,
     0,     0,     0,     0,     0,     0,   230,     0,   361,   229,
     0,     0,   362,     0,     0,    48,   358,    49,   359,   266,
     0,     0,    71,   310,     0,     0,   282,   313,     0,     0,
     0,   471,   475,     0,   447,     0,   451,     0,   453,     0,
     0,   454,   317,   106,     0,     0,   382,   334,     0,     3,
   384,     0,   331,     0,     0,     0,     0,     0,     0,   301,
   303,   373,     0,     0,     0,     0,     0,     0,     0,     0,
   236,     0,     0,     0,     0,     0,   244,   256,   226,     0,
     0,   227,     0,     0,   289,    21,   278,     0,     0,     0,
   413,   414,   415,   410,   416,     0,     0,   352,   337,     0,
     0,     0,     0,     0,     0,     0,    27,     0,    28,     0,
    55,    29,     0,     0,    57,     0,     0,     0,     0,     0,
   444,   308,   480,   466,     0,   315,     0,   476,     0,     0,
     0,   468,     0,   462,     0,     0,     0,     0,     0,   381,
     0,   383,     0,   293,     0,   294,     0,     0,     0,     0,
   305,   233,     0,   235,   250,   258,     0,     0,     0,   241,
     0,     0,   222,   412,     0,     0,   350,   354,     0,   369,
   367,     0,   356,    26,     0,   265,     0,   446,     0,   449,
     0,   450,   452,     0,     0,     0,     0,     0,     0,     0,
   372,   374,   370,   375,   297,   300,     0,     0,     0,     0,
   240,     0,   246,     0,   228,     0,     0,     0,     0,   338,
    51,   311,     0,   464,     0,     0,     0,     0,     0,     0,
   376,     0,     0,   237,   242,     0,     0,     0,   245,   347,
     0,     0,     0,   341,   448,   318,     0,   332,   306,     0,
     0,   247,     0,   346,     0,     0,   243,     0,   248,   343,
     0,     0,   342,   249,
    }, yyDgoto = {
//yyDgoto 139
     1,   209,   290,    61,   109,   547,   520,   110,   201,   515,
   376,   565,   566,   189,    63,    64,    65,    66,    67,   293,
   292,   460,    68,    69,    70,   468,    71,    72,    73,   111,
    74,   206,   207,    76,    77,    78,    79,    80,    81,   211,
   259,   712,   852,   713,   705,   237,   623,   417,   709,   666,
   366,   246,    83,   668,    84,    85,   567,   568,   569,   203,
   753,   213,   532,    87,    88,   238,   396,   579,   271,   759,
   658,   214,    90,   299,   297,   570,   571,   273,    91,   274,
   241,   278,   597,   409,   616,   410,   696,   787,   304,   343,
   475,    92,    93,   267,   379,   215,   204,   205,   231,   760,
   573,   574,     2,   220,   221,   426,   256,   661,   191,   576,
   255,   445,   247,   627,   733,   439,   384,   223,   601,   724,
   224,   725,   609,   856,   546,   385,   543,   779,   371,   373,
   575,   794,   510,   473,   472,   652,   651,   545,   372,
    }, yySindex = {
//yySindex 914
     0,     0,  5296, 13585, 17029, 17398, 17983, 17875,  5296, 15430,
 15430,  6893,     0,     0, 17152, 13954, 13954,     0,     0, 13954,
  -259,  -231,     0,     0,     0,     0, 15430, 17767,   127,     0,
  -185,     0,     0,     0,     0,     0,     0,     0,     0, 16660,
 16660,   -19,  -104, 13462, 15430, 15553, 16660, 17521, 16660, 16783,
 18090,     0,     0,     0,   158,   188,     0,     0,     0,     0,
     0,     0,   306,     0,  -118,     0,     0,     0,  -193,     0,
     0,     0,     0,     0,     0,     0,   140,   333,   257,  4344,
     0,   -33,    -5,     0,   -93,     0,   -62,   239,     0,   266,
     0, 17275,   278,     0,   -35,   333,     0,     0,     0,  -259,
  -231,   127,     0,     0,   194, 15430,   -16,  5296,     0,   306,
     0,    66,     0,   113,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   -51,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   274,     0,     0,   201,   108,   122,     0,
   257,    88,   157,   146,   426,   183,    88,     0,     0,   140,
   -38,   431,     0, 15430, 15430,   212,     0,   282,     0,     0,
     0,   237, 16660, 16660, 16660,  4344,     0,     0,   191,   482,
   489,     0,     0,     0,     0, 13708,     0, 14077, 13954,     0,
     0,     0,  -175,     0,     0, 15676,   199,  5296,     0,   423,
   225,   271,   276,   219, 13462,   250,     0,   256,   257, 16660,
   127,   265,     0,   151,   173,     0,   229,   173,   243,   311,
     0,   448,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   326,   440,   694,   304,   264,   704,   272,  -174,
     0,     0,     0,   292,     0,     0,     0,     0, 13338, 15430,
 15430, 15430, 15430, 13585, 15430, 15430, 16660, 16660, 16660, 16660,
 16660, 16660, 16660, 16660, 16660, 16660, 16660, 16660, 16660, 16660,
 16660, 16660, 16660, 16660, 16660, 16660, 16660, 16660, 16660, 16660,
 16660, 16660,     0,     0, 18252, 18307, 15553, 18362, 18362, 16783,
     0, 15799, 13462, 17521,   567, 15799, 16783,   312,     0,     0,
   257,     0,     0,     0,   140,     0,     0,     0, 18362, 18417,
 15553,  5296, 15430,  1468,     0,     0,     0,     0, 15922,   391,
     0,   219,     0,     0,  5296,   399, 18472, 18527, 15553, 16660,
 16660, 16660,  5296,   406,  5296, 16045,   415,     0,   126,   126,
     0, 18582, 18637, 15553,     0,   636,     0, 16660, 14200,     0,
     0, 14323,     0,     0,   353, 13831,     0,     0,   -33,   127,
   142,   358,   657,     0,     0,     0, 17875, 15430,  4344,  5296,
   342, 18472, 18527, 16660, 16660, 16660,   364,     0,     0,   127,
   127,     0,     0, 16168,     0,     0, 16660,     0, 16660,     0,
     0,     0,     0, 18692, 18747, 15553,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    -6,     0,
     0,   679,  -207,  -207,     0,     0,   333,     0,     0,     0,
     0,     0,     0,     0,   225,  1910,  1910,  1910,  1910,  1700,
  1700,  3293,  2807,  1910,  1910,  2406,  2406,   690,   690,   225,
   767,   225,   225,   -24,   -24,  1700,  1700,   875,   875,  3379,
  -207,   375,     0,   377,  -231,     0,   378,     0,   380,  -231,
     0,     0,   382,     0,     0,  -231,  -231,     0,  4344, 16660,
     0,  3858,     0,     0,   684,   389,     0,     0,     0,     0,
     0,     0,  4344,   140,     0, 15430,  5296,  -231,     0,     0,
  -231,     0,   388,   472,    89,   676,     0,     0,     0,     0,
   824,     0,     0,     0,     0,   432,   458,  5296,   140,     0,
   702,   718,     0,   427,   725, 18197, 17875,     0,     0,   434,
     0,  5296,   512,     0,   315,     0,   442,   450,   459,   380,
   449,  3858,   391,   535,   536, 16660,   759,    88,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   463, 15430,
   470,     0,     0, 16660,   191,   776,     0, 16660,   191,     0,
     0, 16660,  4344,     0,    29,   777,     0,   483,   486, 18362,
 18362,   493,     0, 14446,     0,  -132,   461,     0,   225,   225,
  4344,     0,   494,     0, 16660,     0,     0,     0,     0,     0,
   484,  5296,  -115,     0,     0,     0,     0,  4808,  5296,     0,
  5296,  -207, 16660,  5296, 16783, 16783,     0,   292,     0,     0,
 16783, 16660,     0,   292,   499,     0,     0,     0,     0,     0,
 16660, 16291,     0,     0,   140,   575,     0,     0,   505, 16660,
   127,     0,     0,   588,     0,   824,     0,   275,     0, 16660,
   301,     0,     0,     0, 17644,    88,     0,     0,  5296,     0,
     0, 15430,     0,   591, 16660, 16660, 16660,   519,   595,     0,
     0,     0, 16414,  5296,  5296,  5296,     0,   126,   636, 14569,
     0,   636,   636,   520, 14692, 14815,     0,     0,     0,  -231,
  -231,     0,   -33,   142,     0,     0,     0,   127,     0,   498,
     0,     0,     0,     0,     0, 13092, 17644,     0,     0,   506,
   823,   607,   510,  5296,  4344,   609,     0,  4344,     0,  4344,
     0,     0,  4344,  4344,     0, 16783,  4344, 16660,     0,  5296,
     0,     0,     0,     0,   538,     0,   841,     0,   548,   725,
   676,     0,   725,     0,  1468,   579,     0,   454,     0,     0,
  5296,     0,    88,     0, 16660,     0, 16660,    63,   630,   639,
     0,     0, 16660,     0,     0,     0, 16660,   861,   867,     0,
 16660,   572,     0,     0,   565,   872,     0,     0, 16906,     0,
     0,   555,     0,     0,  4344,     0,   658,     0,   275,     0,
 16660,     0,     0,  5296,     0, 18802, 18857, 15553,   201,  5296,
     0,     0,     0,     0,     0,     0,  5296,  2886,   636, 14938,
     0, 15061,     0,   636,     0, 17644,   574, 13215, 17644,     0,
     0,     0,   725,     0,   660,     0,     0,     0,     0,   581,
     0,   315,   672,     0,     0, 16660,   893, 16660,     0,     0,
 17644,   594,   900,     0,     0,     0,     0,     0,     0,   636,
 15184,     0,   636,     0, 17644,   601,     0, 16660,     0,     0,
 17644,   636,     0,     0,
    }, yyRindex = {
//yyRindex 914
     0,     0,   181,     0,     0,     0,     0,     0,   261,     0,
     0,   227,     0,     0,     0,  8473,  8602,     0,     0,  8713,
  4595,  3986,     0,     0,     0,     0,     0,     0, 16537,     0,
     0,     0,     0,  2041,  3137,     0,     0,  2165,     0,     0,
     0,     0,     0,    94,     0,   608,   592,   110,     0,     0,
   719,     0,     0,     0,   741,  -170,     0,     0,     0,     0,
  9673,     0, 15307,     0,  7753,     0,     0,     0,  7882,     0,
     0,     0,     0,     0,     0,     0,   241,   456,  4713,  3256,
  7993,  3742,     0,     0,  4228,     0,  9802,     0,     0,     0,
     0,   152,     0,     0,     0,   580,     0,     0,     0,  8122,
  7033,   615,  5837,  5979,     0,     0,     0,    94,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  2286,
  2773,  3259,  3745,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  4231,  4693,  5201,     0,  6250,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  5197,     0,     0,    36,     0,     0,  7162,
  1895,     0,     0,  7402,     0,     0,     0,     0,     0,   687,
     0,   231,     0,     0,     0,     0,   487,     0,   648,     0,
     0,     0,     0,     0,     0, 12094,     0,     0, 12922,  1728,
  1728,     0,     0,     0,     0,     0,     0,     0,   619,     0,
     0,     0,     0,     0,     0,     0,     0,    61,     0,     0,
  8842,  8233,  8362,  9913,    94,     0,    35,     0,    64,     0,
   617,     0,     0,   621,   621,     0,   604,   604,     0,     0,
   945,     0,  1594,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,  2283,     0,     0,     0,     0,   569,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   608,     0,     0,     0,
     0,     0,    94,   154,   195,     0,     0,     0,     0,     0,
   164,     0,  6366,     0,     0,     0,     0,     0,     0,     0,
   608,   261,     0,   179,     0,     0,     0,     0,    96,   409,
     0,  7513,     0,     0,   545,  6495,     0,     0,   608,     0,
     0,     0,   627,     0,   160,     0,     0,     0,     0,     0,
   649,     0,     0,   608,     0,  1728,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   629,     0,     0,    85,   641,
   641,     0,   118,     0,     0,     0,     0,     0, 12179,    61,
     0,     0,     0,     0,     0,     0,     0,     0,   121,   641,
   617,     0,     0,   644,     0,     0,  -157,     0,   618,     0,
     0,     0,  1598,     0,     0,   608,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  6624,  6764,     0,     0,   747,     0,     0,     0,
     0,     0,     0,     0,  8953,  1857, 11356, 11462, 11547, 10909,
 11024, 11632, 11887, 11717, 11802, 11972, 12009, 10360, 10466,  9082,
 10577,  9193,  9322, 10134, 10245, 11130, 11241, 10692, 10798,     0,
  6624,  4956,     0,  5079,  4109,     0,  5442,  3500,  5565, 15307,
     0,  3623,     0,     0,     0,  5688,  5688,     0, 12264,     0,
     0,   700,     0,     0,     0,     0,     0,     0,     0,     0,
 11372,     0, 12301,     0,     0,     0,   261,  7273,  6108,  6237,
     0,     0,     0,     0,   641,    80,     0,     0,     0,     0,
   130,     0,     0,     0,     0,    53,     0,   261,     0,     0,
   116,   116,     0,    75,   116,     0,     0,     0,   120,   153,
     0,   215,   721,     0,   721,     0,  2528,  2651,  3014,  4472,
     0, 12959,   721,     0,     0,     0,   178,     0,     0,     0,
     0,     0,     0,     0,   522,  1404,  1433,   428,     0,     0,
     0,     0,     0,     0, 13044,  1728,     0,     0,     0,     0,
     0,     0,   170,     0,     0,   656,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,  9433,  9562,
 12386,    17,     0,     0,     0,     0,   598,  1190,  1228,   699,
     0,    61,     0,     0,     0,     0,     0,     0,   160,     0,
    61,  6764,     0,   160,     0,     0,     0,  2411,     0,     0,
     0,     0,     0,  2770, 10024,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   641,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   160,     0,
     0,     0,     0,     0,     0,     0,     0,  7642,     0,     0,
     0,     0,     0,   220,   160,   160,   651,     0,  1728,     0,
     0,  1728,   656,     0,     0,     0,     0,     0,     0,   134,
   134,     0,     0,   641,     0,     0,     0,   617,  1603,     0,
     0,     0,     0,     0,     0,   634,     0,     0,     0,     0,
   640,     0,     0,    61, 12423,     0,     0, 12508,     0, 12593,
     0,     0, 12630, 12715,     0,     0, 12752,     0,  1708,   261,
     0,     0,     0,     0,     0,     0,   116,     0,    97,   116,
     0,     0,   116,     0,   179,     0,   541,     0,   860,     0,
   261,     0,     0,     0,     0,     0,     0,   721,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   656,   656,     0,
     0,     0,     0,     0,     0,   642,     0,     0,   643,     0,
     0,     0,     0,     0, 12837,     0,     0,     0,     0,     0,
     0,     0,     0,   261,  1126,     0,     0,   608,    36,   545,
     0,     0,     0,     0,     0,     0,   160,  1728,   656,     0,
     0,     0,     0,   656,     0,     0,     0,   650,     0,     0,
     0,     0,   116,     0,     0,   200,  1085,  1226,   206,     0,
     0,   721,     0,     0,     0,     0,   656,     0,     0,     0,
     0,     0,   655,     0,     0,     0,  1157,     0,     0,   656,
     0,     0,   656,     0,     0,     0,     0,     0,     0,     0,
     0,   656,     0,     0,
    }, yyGindex = {
//yyGindex 139
     0,    25,     0,    11,  1188,  -294,     0,   -12,    14,    95,
     0,     0,     0,     0,     0,     0,   935,     0,     0,     0,
   638,  -120,     0,     0,     0,     0,     0,     0,    39,   997,
   -30,  1035,  -349,     0,   139,  1059,  1007,    30,    98,     5,
    -2,  -368,     0,   133,     0,   150,     0,     0,     0,    48,
     0,    49,  1011,  -222,  -223,     0,   226,   465,  -562,     0,
     0,   249,    -3,   -87,    13,  1394,  -361,     0,  -288,     0,
  -324,   400,  1135,     0,     0,     0,   324,    33,     0,    21,
  -323,     0,     0,   735,    40,     0,  -491,  -352,   953,     0,
  -274,  1015,    22,  -180,   182,   224,     0,   -22,     0,     0,
   336,  -566,     0,    15,   956,     0,     0,     0,     0,     0,
   -78,   371,     0,     0,     0,     0,  -206,     0,  -342,     0,
     0,     0,     0,     0,     0,    51,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,
    };
    protected static final short[] yyTable = YyTables.yyTable();
    protected static final short[] yyCheck = YyTables.yyCheck();

  /** maps symbol value to printable name.
      @see #yyExpecting
    */
  protected static final String[] yyNames = {
    "end-of-file",null,null,null,null,null,null,null,null,null,"'\\n'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,"' '",null,null,null,null,null,
    null,null,null,null,null,null,"','",null,null,null,null,null,null,
    null,null,null,null,null,null,null,"':'","';'",null,"'='",null,"'?'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,
    "'['",null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,
    "kCLASS","kMODULE","kDEF","kUNDEF","kBEGIN","kRESCUE","kENSURE",
    "kEND","kIF","kUNLESS","kTHEN","kELSIF","kELSE","kCASE","kWHEN",
    "kWHILE","kUNTIL","kFOR","kBREAK","kNEXT","kREDO","kRETRY","kIN",
    "kDO","kDO_COND","kDO_BLOCK","kRETURN","kYIELD","kSUPER","kSELF",
    "kNIL","kTRUE","kFALSE","kAND","kOR","kNOT","kIF_MOD","kUNLESS_MOD",
    "kWHILE_MOD","kUNTIL_MOD","kRESCUE_MOD","kALIAS","kDEFINED","klBEGIN",
    "klEND","k__LINE__","k__FILE__","k__ENCODING__","kDO_LAMBDA",
    "tIDENTIFIER","tFID","tGVAR","tIVAR","tCONSTANT","tCVAR","tLABEL",
    "tCHAR","tUPLUS","tUMINUS","tUMINUS_NUM","tPOW","tCMP","tEQ","tEQQ",
    "tNEQ","tGEQ","tLEQ","tANDOP","tOROP","tMATCH","tNMATCH","tDOT",
    "tDOT2","tDOT3","tAREF","tASET","tLSHFT","tRSHFT","tCOLON2","tCOLON3",
    "tOP_ASGN","tASSOC","tLPAREN","tLPAREN2","tRPAREN","tLPAREN_ARG",
    "tLBRACK","tRBRACK","tLBRACE","tLBRACE_ARG","tSTAR","tSTAR2","tAMPER",
    "tAMPER2","tTILDE","tPERCENT","tDIVIDE","tPLUS","tMINUS","tLT","tGT",
    "tPIPE","tBANG","tCARET","tLCURLY","tRCURLY","tBACK_REF2","tSYMBEG",
    "tSTRING_BEG","tXSTRING_BEG","tREGEXP_BEG","tWORDS_BEG","tQWORDS_BEG",
    "tSTRING_DBEG","tSTRING_DVAR","tSTRING_END","tLAMBDA","tLAMBEG",
    "tNTH_REF","tBACK_REF","tSTRING_CONTENT","tINTEGER","tFLOAT",
    "tREGEXP_END","tLOWEST",
    };

  /** printable rules for debugging.
    */
  protected static final String [] yyRule = {
    "$accept : program",
    "$$1 :",
    "program : $$1 compstmt",
    "bodystmt : compstmt opt_rescue opt_else opt_ensure",
    "compstmt : stmts opt_terms",
    "stmts : none",
    "stmts : stmt",
    "stmts : stmts terms stmt",
    "stmts : error stmt",
    "$$2 :",
    "stmt : kALIAS fitem $$2 fitem",
    "stmt : kALIAS tGVAR tGVAR",
    "stmt : kALIAS tGVAR tBACK_REF",
    "stmt : kALIAS tGVAR tNTH_REF",
    "stmt : kUNDEF undef_list",
    "stmt : stmt kIF_MOD expr_value",
    "stmt : stmt kUNLESS_MOD expr_value",
    "stmt : stmt kWHILE_MOD expr_value",
    "stmt : stmt kUNTIL_MOD expr_value",
    "stmt : stmt kRESCUE_MOD stmt",
    "$$3 :",
    "stmt : klBEGIN $$3 tLCURLY compstmt tRCURLY",
    "stmt : klEND tLCURLY compstmt tRCURLY",
    "stmt : lhs '=' command_call",
    "stmt : mlhs '=' command_call",
    "stmt : var_lhs tOP_ASGN command_call",
    "stmt : primary_value '[' aref_args tRBRACK tOP_ASGN command_call",
    "stmt : primary_value tDOT tIDENTIFIER tOP_ASGN command_call",
    "stmt : primary_value tDOT tCONSTANT tOP_ASGN command_call",
    "stmt : primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_call",
    "stmt : backref tOP_ASGN command_call",
    "stmt : lhs '=' mrhs",
    "stmt : mlhs '=' arg_value",
    "stmt : mlhs '=' mrhs",
    "stmt : expr",
    "expr : command_call",
    "expr : expr kAND expr",
    "expr : expr kOR expr",
    "expr : kNOT expr",
    "expr : tBANG command_call",
    "expr : arg",
    "expr_value : expr",
    "command_call : command",
    "command_call : block_command",
    "command_call : kRETURN call_args",
    "command_call : kBREAK call_args",
    "command_call : kNEXT call_args",
    "block_command : block_call",
    "block_command : block_call tDOT operation2 command_args",
    "block_command : block_call tCOLON2 operation2 command_args",
    "$$4 :",
    "cmd_brace_block : tLBRACE_ARG $$4 opt_block_var compstmt tRCURLY",
    "command : operation command_args",
    "command : operation command_args cmd_brace_block",
    "command : primary_value tDOT operation2 command_args",
    "command : primary_value tDOT operation2 command_args cmd_brace_block",
    "command : primary_value tCOLON2 operation2 command_args",
    "command : primary_value tCOLON2 operation2 command_args cmd_brace_block",
    "command : kSUPER command_args",
    "command : kYIELD command_args",
    "mlhs : mlhs_basic",
    "mlhs : tLPAREN mlhs_entry tRPAREN",
    "mlhs_entry : mlhs_basic",
    "mlhs_entry : tLPAREN mlhs_entry tRPAREN",
    "mlhs_basic : mlhs_head",
    "mlhs_basic : mlhs_head mlhs_item",
    "mlhs_basic : mlhs_head tSTAR mlhs_node",
    "mlhs_basic : mlhs_head tSTAR",
    "mlhs_basic : tSTAR mlhs_node",
    "mlhs_basic : tSTAR",
    "mlhs_item : mlhs_node",
    "mlhs_item : tLPAREN mlhs_entry tRPAREN",
    "mlhs_head : mlhs_item ','",
    "mlhs_head : mlhs_head mlhs_item ','",
    "mlhs_node : variable",
    "mlhs_node : primary_value '[' aref_args tRBRACK",
    "mlhs_node : primary_value tDOT tIDENTIFIER",
    "mlhs_node : primary_value tCOLON2 tIDENTIFIER",
    "mlhs_node : primary_value tDOT tCONSTANT",
    "mlhs_node : primary_value tCOLON2 tCONSTANT",
    "mlhs_node : tCOLON3 tCONSTANT",
    "mlhs_node : backref",
    "lhs : variable",
    "lhs : primary_value '[' aref_args tRBRACK",
    "lhs : primary_value tDOT tIDENTIFIER",
    "lhs : primary_value tCOLON2 tIDENTIFIER",
    "lhs : primary_value tDOT tCONSTANT",
    "lhs : primary_value tCOLON2 tCONSTANT",
    "lhs : tCOLON3 tCONSTANT",
    "lhs : backref",
    "cname : tIDENTIFIER",
    "cname : tCONSTANT",
    "cpath : tCOLON3 cname",
    "cpath : cname",
    "cpath : primary_value tCOLON2 cname",
    "fname : tIDENTIFIER",
    "fname : tCONSTANT",
    "fname : tFID",
    "fname : op",
    "fname : reswords",
    "fsym : fname",
    "fsym : symbol",
    "fitem : fsym",
    "fitem : dsym",
    "undef_list : fitem",
    "$$5 :",
    "undef_list : undef_list ',' $$5 fitem",
    "op : tPIPE",
    "op : tCARET",
    "op : tAMPER2",
    "op : tCMP",
    "op : tEQ",
    "op : tEQQ",
    "op : tMATCH",
    "op : tGT",
    "op : tGEQ",
    "op : tLT",
    "op : tLEQ",
    "op : tLSHFT",
    "op : tRSHFT",
    "op : tPLUS",
    "op : tMINUS",
    "op : tSTAR2",
    "op : tSTAR",
    "op : tDIVIDE",
    "op : tPERCENT",
    "op : tPOW",
    "op : tTILDE",
    "op : tUPLUS",
    "op : tUMINUS",
    "op : tAREF",
    "op : tASET",
    "op : tBACK_REF2",
    "reswords : k__LINE__",
    "reswords : k__FILE__",
    "reswords : klBEGIN",
    "reswords : klEND",
    "reswords : kALIAS",
    "reswords : kAND",
    "reswords : kBEGIN",
    "reswords : kBREAK",
    "reswords : kCASE",
    "reswords : kCLASS",
    "reswords : kDEF",
    "reswords : kDEFINED",
    "reswords : kDO",
    "reswords : kELSE",
    "reswords : kELSIF",
    "reswords : kEND",
    "reswords : kENSURE",
    "reswords : kFALSE",
    "reswords : kFOR",
    "reswords : kIN",
    "reswords : kMODULE",
    "reswords : kNEXT",
    "reswords : kNIL",
    "reswords : kNOT",
    "reswords : kOR",
    "reswords : kREDO",
    "reswords : kRESCUE",
    "reswords : kRETRY",
    "reswords : kRETURN",
    "reswords : kSELF",
    "reswords : kSUPER",
    "reswords : kTHEN",
    "reswords : kTRUE",
    "reswords : kUNDEF",
    "reswords : kWHEN",
    "reswords : kYIELD",
    "reswords : kIF_MOD",
    "reswords : kUNLESS_MOD",
    "reswords : kWHILE_MOD",
    "reswords : kUNTIL_MOD",
    "reswords : kRESCUE_MOD",
    "arg : lhs '=' arg",
    "arg : lhs '=' arg kRESCUE_MOD arg",
    "arg : var_lhs tOP_ASGN arg",
    "arg : primary_value '[' aref_args tRBRACK tOP_ASGN arg",
    "arg : primary_value tDOT tIDENTIFIER tOP_ASGN arg",
    "arg : primary_value tDOT tCONSTANT tOP_ASGN arg",
    "arg : primary_value tCOLON2 tIDENTIFIER tOP_ASGN arg",
    "arg : primary_value tCOLON2 tCONSTANT tOP_ASGN arg",
    "arg : tCOLON3 tCONSTANT tOP_ASGN arg",
    "arg : backref tOP_ASGN arg",
    "arg : arg tDOT2 arg",
    "arg : arg tDOT3 arg",
    "arg : arg tPLUS arg",
    "arg : arg tMINUS arg",
    "arg : arg tSTAR2 arg",
    "arg : arg tDIVIDE arg",
    "arg : arg tPERCENT arg",
    "arg : arg tPOW arg",
    "arg : tUMINUS_NUM tINTEGER tPOW arg",
    "arg : tUMINUS_NUM tFLOAT tPOW arg",
    "arg : tUPLUS arg",
    "arg : tUMINUS arg",
    "arg : arg tPIPE arg",
    "arg : arg tCARET arg",
    "arg : arg tAMPER2 arg",
    "arg : arg tCMP arg",
    "arg : arg tGT arg",
    "arg : arg tGEQ arg",
    "arg : arg tLT arg",
    "arg : arg tLEQ arg",
    "arg : arg tEQ arg",
    "arg : arg tEQQ arg",
    "arg : arg tNEQ arg",
    "arg : arg tMATCH arg",
    "arg : arg tNMATCH arg",
    "arg : tBANG arg",
    "arg : tTILDE arg",
    "arg : arg tLSHFT arg",
    "arg : arg tRSHFT arg",
    "arg : arg tANDOP arg",
    "arg : arg tOROP arg",
    "arg : kDEFINED opt_nl arg",
    "arg : arg '?' arg ':' arg",
    "arg : primary",
    "arg_value : arg",
    "aref_args : none",
    "aref_args : command opt_nl",
    "aref_args : args trailer",
    "aref_args : args ',' tSTAR arg_value opt_nl",
    "aref_args : assocs trailer",
    "aref_args : tSTAR arg_value opt_nl",
    "paren_args : tLPAREN2 none tRPAREN",
    "paren_args : tLPAREN2 call_args opt_nl tRPAREN",
    "paren_args : tLPAREN2 block_call opt_nl tRPAREN",
    "paren_args : tLPAREN2 args ',' block_call opt_nl tRPAREN",
    "opt_paren_args : none",
    "opt_paren_args : paren_args",
    "call_args : command",
    "call_args : args opt_block_arg",
    "call_args : args ',' tSTAR arg_value opt_block_arg",
    "call_args : assocs opt_block_arg",
    "call_args : assocs ',' tSTAR arg_value opt_block_arg",
    "call_args : args ',' assocs opt_block_arg",
    "call_args : args ',' assocs ',' tSTAR arg opt_block_arg",
    "call_args : tSTAR arg_value opt_block_arg",
    "call_args : block_arg",
    "call_args2 : arg_value ',' args opt_block_arg",
    "call_args2 : arg_value ',' block_arg",
    "call_args2 : arg_value ',' tSTAR arg_value opt_block_arg",
    "call_args2 : arg_value ',' args ',' tSTAR arg_value opt_block_arg",
    "call_args2 : assocs opt_block_arg",
    "call_args2 : assocs ',' tSTAR arg_value opt_block_arg",
    "call_args2 : arg_value ',' assocs opt_block_arg",
    "call_args2 : arg_value ',' args ',' assocs opt_block_arg",
    "call_args2 : arg_value ',' assocs ',' tSTAR arg_value opt_block_arg",
    "call_args2 : arg_value ',' args ',' assocs ',' tSTAR arg_value opt_block_arg",
    "call_args2 : tSTAR arg_value opt_block_arg",
    "call_args2 : block_arg",
    "$$6 :",
    "command_args : $$6 open_args",
    "open_args : call_args",
    "$$7 :",
    "open_args : tLPAREN_ARG $$7 tRPAREN",
    "$$8 :",
    "open_args : tLPAREN_ARG call_args2 $$8 tRPAREN",
    "block_arg : tAMPER arg_value",
    "opt_block_arg : ',' block_arg",
    "opt_block_arg : none_block_pass",
    "args : arg_value",
    "args : args ',' arg_value",
    "mrhs : args ',' arg_value",
    "mrhs : args ',' tSTAR arg_value",
    "mrhs : tSTAR arg_value",
    "primary : literal",
    "primary : strings",
    "primary : xstring",
    "primary : regexp",
    "primary : words",
    "primary : qwords",
    "primary : var_ref",
    "primary : backref",
    "primary : tFID",
    "primary : kBEGIN bodystmt kEND",
    "$$9 :",
    "primary : tLPAREN_ARG expr $$9 opt_nl tRPAREN",
    "primary : tLPAREN compstmt tRPAREN",
    "primary : primary_value tCOLON2 tCONSTANT",
    "primary : tCOLON3 tCONSTANT",
    "primary : primary_value '[' aref_args tRBRACK",
    "primary : tLBRACK aref_args tRBRACK",
    "primary : tLBRACE assoc_list tRCURLY",
    "primary : kRETURN",
    "primary : kYIELD tLPAREN2 call_args tRPAREN",
    "primary : kYIELD tLPAREN2 tRPAREN",
    "primary : kYIELD",
    "primary : kDEFINED opt_nl tLPAREN2 expr tRPAREN",
    "primary : operation brace_block",
    "primary : method_call",
    "primary : method_call brace_block",
    "primary : kIF expr_value then compstmt if_tail kEND",
    "primary : kUNLESS expr_value then compstmt opt_else kEND",
    "$$10 :",
    "$$11 :",
    "primary : kWHILE $$10 expr_value do $$11 compstmt kEND",
    "$$12 :",
    "$$13 :",
    "primary : kUNTIL $$12 expr_value do $$13 compstmt kEND",
    "primary : kCASE expr_value opt_terms case_body kEND",
    "primary : kCASE opt_terms case_body kEND",
    "primary : kCASE opt_terms kELSE compstmt kEND",
    "$$14 :",
    "$$15 :",
    "primary : kFOR for_var kIN $$14 expr_value do $$15 compstmt kEND",
    "$$16 :",
    "primary : kCLASS cpath superclass $$16 bodystmt kEND",
    "$$17 :",
    "$$18 :",
    "primary : kCLASS tLSHFT expr $$17 term $$18 bodystmt kEND",
    "$$19 :",
    "primary : kMODULE cpath $$19 bodystmt kEND",
    "$$20 :",
    "primary : kDEF fname $$20 f_arglist bodystmt kEND",
    "$$21 :",
    "$$22 :",
    "primary : kDEF singleton dot_or_colon $$21 fname $$22 f_arglist bodystmt kEND",
    "primary : kBREAK",
    "primary : kNEXT",
    "primary : kREDO",
    "primary : kRETRY",
    "primary_value : primary",
    "then : term",
    "then : ':'",
    "then : kTHEN",
    "then : term kTHEN",
    "do : term",
    "do : ':'",
    "do : kDO_COND",
    "if_tail : opt_else",
    "if_tail : kELSIF expr_value then compstmt if_tail",
    "opt_else : none",
    "opt_else : kELSE compstmt",
    "for_var : lhs",
    "for_var : mlhs",
    "block_par : mlhs_item",
    "block_par : block_par ',' mlhs_item",
    "block_var : block_par",
    "block_var : block_par ','",
    "block_var : block_par ',' tAMPER lhs",
    "block_var : block_par ',' tSTAR lhs ',' tAMPER lhs",
    "block_var : block_par ',' tSTAR ',' tAMPER lhs",
    "block_var : block_par ',' tSTAR lhs",
    "block_var : block_par ',' tSTAR",
    "block_var : tSTAR lhs ',' tAMPER lhs",
    "block_var : tSTAR ',' tAMPER lhs",
    "block_var : tSTAR lhs",
    "block_var : tSTAR",
    "block_var : tAMPER lhs",
    "opt_block_var : none",
    "opt_block_var : tPIPE tPIPE",
    "opt_block_var : tOROP",
    "opt_block_var : tPIPE block_var tPIPE",
    "$$23 :",
    "do_block : kDO_BLOCK $$23 opt_block_var compstmt kEND",
    "block_call : command do_block",
    "block_call : block_call tDOT operation2 opt_paren_args",
    "block_call : block_call tCOLON2 operation2 opt_paren_args",
    "method_call : operation paren_args",
    "method_call : primary_value tDOT operation2 opt_paren_args",
    "method_call : primary_value tCOLON2 operation2 paren_args",
    "method_call : primary_value tCOLON2 operation3",
    "method_call : kSUPER paren_args",
    "method_call : kSUPER",
    "$$24 :",
    "brace_block : tLCURLY $$24 opt_block_var compstmt tRCURLY",
    "$$25 :",
    "brace_block : kDO $$25 opt_block_var compstmt kEND",
    "case_body : kWHEN when_args then compstmt cases",
    "when_args : args",
    "when_args : args ',' tSTAR arg_value",
    "when_args : tSTAR arg_value",
    "cases : opt_else",
    "cases : case_body",
    "opt_rescue : kRESCUE exc_list exc_var then compstmt opt_rescue",
    "opt_rescue :",
    "exc_list : arg_value",
    "exc_list : mrhs",
    "exc_list : none",
    "exc_var : tASSOC lhs",
    "exc_var : none",
    "opt_ensure : kENSURE compstmt",
    "opt_ensure : none",
    "literal : numeric",
    "literal : symbol",
    "literal : dsym",
    "strings : string",
    "string : string1",
    "string : string string1",
    "string1 : tSTRING_BEG string_contents tSTRING_END",
    "xstring : tXSTRING_BEG xstring_contents tSTRING_END",
    "regexp : tREGEXP_BEG xstring_contents tREGEXP_END",
    "words : tWORDS_BEG ' ' tSTRING_END",
    "words : tWORDS_BEG word_list tSTRING_END",
    "word_list :",
    "word_list : word_list word ' '",
    "word : string_content",
    "word : word string_content",
    "qwords : tQWORDS_BEG ' ' tSTRING_END",
    "qwords : tQWORDS_BEG qword_list tSTRING_END",
    "qword_list :",
    "qword_list : qword_list tSTRING_CONTENT ' '",
    "string_contents :",
    "string_contents : string_contents string_content",
    "xstring_contents :",
    "xstring_contents : xstring_contents string_content",
    "string_content : tSTRING_CONTENT",
    "$$26 :",
    "string_content : tSTRING_DVAR $$26 string_dvar",
    "$$27 :",
    "string_content : tSTRING_DBEG $$27 compstmt tRCURLY",
    "string_dvar : tGVAR",
    "string_dvar : tIVAR",
    "string_dvar : tCVAR",
    "string_dvar : backref",
    "symbol : tSYMBEG sym",
    "sym : fname",
    "sym : tIVAR",
    "sym : tGVAR",
    "sym : tCVAR",
    "dsym : tSYMBEG xstring_contents tSTRING_END",
    "numeric : tINTEGER",
    "numeric : tFLOAT",
    "numeric : tUMINUS_NUM tINTEGER",
    "numeric : tUMINUS_NUM tFLOAT",
    "variable : tIDENTIFIER",
    "variable : tIVAR",
    "variable : tGVAR",
    "variable : tCONSTANT",
    "variable : tCVAR",
    "variable : kNIL",
    "variable : kSELF",
    "variable : kTRUE",
    "variable : kFALSE",
    "variable : k__FILE__",
    "variable : k__LINE__",
    "var_ref : variable",
    "var_lhs : variable",
    "backref : tNTH_REF",
    "backref : tBACK_REF",
    "superclass : term",
    "$$28 :",
    "superclass : tLT $$28 expr_value term",
    "superclass : error term",
    "f_arglist : tLPAREN2 f_args opt_nl tRPAREN",
    "f_arglist : f_args term",
    "f_args : f_arg ',' f_optarg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_arg ',' f_optarg opt_f_block_arg",
    "f_args : f_arg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_arg opt_f_block_arg",
    "f_args : f_optarg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_optarg opt_f_block_arg",
    "f_args : f_rest_arg opt_f_block_arg",
    "f_args : f_block_arg",
    "f_args :",
    "f_norm_arg : tCONSTANT",
    "f_norm_arg : tIVAR",
    "f_norm_arg : tGVAR",
    "f_norm_arg : tCVAR",
    "f_norm_arg : tIDENTIFIER",
    "f_arg : f_norm_arg tASSOC arg_value",
    "f_arg : f_norm_arg",
    "f_arg : f_arg ',' f_norm_arg tASSOC arg_value",
    "f_arg : f_arg ',' f_norm_arg",
    "f_opt : tIDENTIFIER '=' arg_value",
    "f_optarg : f_opt",
    "f_optarg : f_optarg ',' f_opt",
    "restarg_mark : tSTAR2",
    "restarg_mark : tSTAR",
    "f_rest_arg : restarg_mark tIDENTIFIER",
    "f_rest_arg : restarg_mark",
    "blkarg_mark : tAMPER2",
    "blkarg_mark : tAMPER",
    "f_block_arg : blkarg_mark tIDENTIFIER",
    "opt_f_block_arg : ',' f_block_arg",
    "opt_f_block_arg :",
    "singleton : var_ref",
    "$$29 :",
    "singleton : tLPAREN2 $$29 expr opt_nl tRPAREN",
    "assoc_list : none",
    "assoc_list : assocs trailer",
    "assoc_list : args trailer",
    "assocs : assoc",
    "assocs : assocs ',' assoc",
    "assoc : arg_value tASSOC arg_value",
    "operation : tIDENTIFIER",
    "operation : tCONSTANT",
    "operation : tFID",
    "operation2 : tIDENTIFIER",
    "operation2 : tCONSTANT",
    "operation2 : tFID",
    "operation2 : op",
    "operation3 : tIDENTIFIER",
    "operation3 : tFID",
    "operation3 : op",
    "dot_or_colon : tDOT",
    "dot_or_colon : tCOLON2",
    "opt_terms :",
    "opt_terms : terms",
    "opt_nl :",
    "opt_nl : '\\n'",
    "trailer :",
    "trailer : '\\n'",
    "trailer : ','",
    "term : ';'",
    "term : '\\n'",
    "terms : term",
    "terms : terms ';'",
    "none :",
    "none_block_pass :",
    };

  /** debugging support, requires the package <tt>jay.yydebug</tt>.
      Set to <tt>null</tt> to suppress debugging messages.
    */
  protected jay.yydebug.yyDebug yydebug;

  /** index-checked interface to {@link #yyNames}.
      @param token single character or <tt>%token</tt> value.
      @return token name or <tt>[illegal]</tt> or <tt>[unknown]</tt>.
    */
  public static final String yyName (int token) {
    if (token < 0 || token > yyNames.length) return "[illegal]";
    String name;
    if ((name = yyNames[token]) != null) return name;
    return "[unknown]";
  }


  /** computes list of expected tokens on error by tracing the tables.
      @param state for which to compute the list.
      @return list of token names.
    */
  protected String[] yyExpecting (int state) {
    int token, n, len = 0;
    boolean[] ok = new boolean[yyNames.length];

    if ((n = yySindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyNames.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyNames[token] != null) {
          ++ len;
          ok[token] = true;
        }
    if ((n = yyRindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyNames.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyNames[token] != null) {
          ++ len;
          ok[token] = true;
        }

    String result[] = new String[len];
    for (n = token = 0; n < len;  ++ token)
      if (ok[token]) result[n++] = yyNames[token];
    return result;
  }

  /** the generated parser, with debugging messages.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @param yydebug debug message writer implementing <tt>yyDebug</tt>, or <tt>null</tt>.
      @return result of the last reduction, if any.
    */
  public Object yyparse (RubyYaccLexer yyLex, Object ayydebug)
				throws java.io.IOException {
    this.yydebug = (jay.yydebug.yyDebug)ayydebug;
    return yyparse(yyLex);
  }

  /** initial size and increment of the state/value stack [default 256].
      This is not final so that it can be overwritten outside of invocations
      of {@link #yyparse}.
    */
  protected int yyMax;

  /** executed at the beginning of a reduce action.
      Used as <tt>$$ = yyDefault($1)</tt>, prior to the user-specified action, if any.
      Can be overwritten to provide deep copy, etc.
      @param first value for <tt>$1</tt>, or <tt>null</tt>.
      @return first.
    */
  protected Object yyDefault (Object first) {
    return first;
  }

  /** the generated parser.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @return result of the last reduction, if any.
    */
  public Object yyparse (RubyYaccLexer yyLex) throws java.io.IOException {
    if (yyMax <= 0) yyMax = 256;			// initial size
    int yyState = 0, yyStates[] = new int[yyMax];	// state stack
    Object yyVal = null, yyVals[] = new Object[yyMax];	// value stack
    int yyToken = -1;					// current input
    int yyErrorFlag = 0;				// #tokens to shift

    yyLoop: for (int yyTop = 0;; ++ yyTop) {
      if (yyTop >= yyStates.length) {			// dynamically increase
        int[] i = new int[yyStates.length+yyMax];
        System.arraycopy(yyStates, 0, i, 0, yyStates.length);
        yyStates = i;
        Object[] o = new Object[yyVals.length+yyMax];
        System.arraycopy(yyVals, 0, o, 0, yyVals.length);
        yyVals = o;
      }
      yyStates[yyTop] = yyState;
      yyVals[yyTop] = yyVal;
      if (yydebug != null) yydebug.push(yyState, yyVal);

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyN;
        if ((yyN = yyDefRed[yyState]) == 0) {	// else [default] reduce (yyN)
          if (yyToken < 0) {
//            yyToken = yyLex.advance() ? yyLex.token() : 0;
            yyToken = yyLex.nextToken();
            if (yydebug != null)
              yydebug.lex(yyState, yyToken, yyName(yyToken), yyLex.value());
          }
          if ((yyN = yySindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken) {
            if (yydebug != null)
              yydebug.shift(yyState, yyTable[yyN], yyErrorFlag-1);
            yyState = yyTable[yyN];		// shift to yyN
            yyVal = yyLex.value();
            yyToken = -1;
            if (yyErrorFlag > 0) -- yyErrorFlag;
            continue yyLoop;
          }
          if ((yyN = yyRindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken)
            yyN = yyTable[yyN];			// reduce (yyN)
          else
            switch (yyErrorFlag) {
  
            case 0:
              support.yyerror("syntax error", yyExpecting(yyState), yyNames[yyToken]);
              if (yydebug != null) yydebug.error("syntax error");
  
            case 1: case 2:
              yyErrorFlag = 3;
              do {
                if ((yyN = yySindex[yyStates[yyTop]]) != 0
                    && (yyN += yyErrorCode) >= 0 && yyN < yyTable.length
                    && yyCheck[yyN] == yyErrorCode) {
           
