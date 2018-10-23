// https://searchcode.com/api/result/5685296/

// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "Ruby19Parser.y"
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
 * Copyright (C) 2008-2009 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
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
import org.jruby.ast.Hash19Node;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LiteralNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilImplicitNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExe19Node;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RestArgNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.ZYieldNode;
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

public class Ruby19Parser implements RubyParser {
    protected ParserSupport19 support;
    protected RubyYaccLexer lexer;

    public Ruby19Parser() {
        this(new ParserSupport19());
    }

    public Ruby19Parser(ParserSupport19 support) {
        this.support = support;
        lexer = new RubyYaccLexer(false);
        lexer.setParserSupport(support);
        support.setLexer(lexer);
    }

    public void setWarnings(IRubyWarnings warnings) {
        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }
					// line 141 "-"
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
//yyLhs 550
    -1,   121,     0,   118,   119,   119,   119,   119,   120,   124,
   120,    35,    34,    36,    36,    36,    36,   125,    37,    37,
    37,    37,    37,    37,    37,    37,    37,    37,    37,    37,
    37,    37,    37,    37,    37,    37,    37,    37,    37,    37,
    37,    32,    32,    38,    38,    38,    38,    38,    38,    42,
    33,    33,    33,    33,    33,    56,    56,    56,   127,    95,
    41,    41,    41,    41,    41,    41,    41,    41,    96,    96,
   107,   107,    97,    97,    97,    97,    97,    97,    97,    97,
    97,    97,    68,    68,    82,    82,    86,    86,    69,    69,
    69,    69,    69,    69,    69,    69,    74,    74,    74,    74,
    74,    74,    74,    74,     7,     7,    31,    31,    31,     8,
     8,     8,     8,     8,   100,   100,   101,   101,    58,   128,
    58,     9,     9,     9,     9,     9,     9,     9,     9,     9,
     9,     9,     9,     9,     9,     9,     9,     9,     9,     9,
     9,     9,     9,     9,     9,     9,     9,     9,     9,     9,
   116,   116,   116,   116,   116,   116,   116,   116,   116,   116,
   116,   116,   116,   116,   116,   116,   116,   116,   116,   116,
   116,   116,   116,   116,   116,   116,   116,   116,   116,   116,
   116,   116,   116,   116,   116,   116,   116,   116,   116,   116,
   116,   116,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,    39,    39,    39,    39,    39,    39,    70,    73,    73,
    73,    73,    50,    54,    54,   110,   110,    48,    48,    48,
    48,    48,   130,    52,    89,    88,    88,    88,    76,    76,
    76,    76,    67,    67,    67,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,   131,    40,    40,    40,    40,
    40,    40,    40,    40,    40,    40,    40,    40,    40,    40,
    40,    40,    40,    40,    40,   133,   135,    40,   136,   137,
    40,    40,    40,   138,   139,    40,   140,    40,   142,   143,
    40,   144,    40,   145,    40,   146,   147,    40,    40,    40,
    40,    40,    43,   132,   132,   132,   134,   134,    46,    46,
    44,    44,   109,   109,   111,   111,    81,    81,   112,   112,
   112,   112,   112,   112,   112,   112,   112,    64,    64,    64,
    64,    64,    64,    64,    64,    64,    64,    64,    64,    64,
    64,    64,    66,    66,    65,    65,    65,   104,   104,   103,
   103,   113,   113,   148,   106,    63,    63,   105,   105,   149,
    94,    55,    55,    55,    24,    24,    24,    24,    24,    24,
    24,    24,    24,   150,    93,   151,    93,    71,    45,    45,
    98,    98,    72,    72,    72,    47,    47,    49,    49,    28,
    28,    28,    16,    17,    17,    17,    18,    19,    20,    25,
    25,    78,    78,    27,    27,    26,    26,    77,    77,    21,
    21,    22,    22,    23,   152,    23,   153,    23,    59,    59,
    59,    59,     3,     2,     2,     2,     2,    30,    29,    29,
    29,    29,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,    53,    99,    60,    60,    51,   154,
    51,    51,    62,    62,    61,    61,    61,    61,    61,    61,
    61,    61,    61,    61,    61,    61,    61,    61,    61,   117,
   117,   117,   117,    10,    10,   102,   102,    79,    79,    57,
   108,    87,    87,    80,    80,    12,    12,    14,    14,    13,
    13,    92,    91,    91,    15,   155,    15,    85,    85,    83,
    83,    84,    84,     4,     4,     4,     5,     5,     5,     5,
     6,     6,     6,    11,    11,   122,   122,   126,   126,   114,
   115,   129,   129,   129,   141,   141,   123,   123,    75,    90,
    }, yyLen = {
//yyLen 550
     2,     0,     2,     2,     1,     1,     3,     2,     1,     0,
     5,     4,     2,     1,     1,     3,     2,     0,     4,     3,
     3,     3,     2,     3,     3,     3,     3,     3,     4,     1,
     3,     3,     6,     5,     5,     5,     3,     3,     3,     3,
     1,     3,     3,     1,     3,     3,     3,     2,     1,     1,
     1,     1,     2,     2,     2,     1,     4,     4,     0,     5,
     2,     3,     4,     5,     4,     5,     2,     2,     1,     3,
     1,     3,     1,     2,     3,     5,     2,     4,     2,     4,
     1,     3,     1,     3,     2,     3,     1,     3,     1,     4,
     3,     3,     3,     3,     2,     1,     1,     4,     3,     3,
     3,     3,     2,     1,     1,     1,     2,     1,     3,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     0,
     4,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     3,     5,     3,     5,     6,     5,     5,     5,
     5,     4,     3,     3,     3,     3,     3,     3,     3,     3,
     3,     4,     4,     2,     2,     3,     3,     3,     3,     3,
     3,     3,     3,     3,     3,     3,     3,     3,     2,     2,
     3,     3,     3,     3,     3,     6,     1,     1,     1,     2,
     4,     2,     3,     1,     1,     1,     1,     1,     2,     2,
     4,     1,     0,     2,     2,     2,     1,     1,     1,     2,
     3,     4,     3,     4,     2,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     3,     0,     4,     3,     3,     2,
     3,     3,     1,     4,     3,     1,     5,     4,     3,     2,
     1,     2,     2,     6,     6,     0,     0,     7,     0,     0,
     7,     5,     4,     0,     0,     9,     0,     6,     0,     0,
     8,     0,     5,     0,     6,     0,     0,     9,     1,     1,
     1,     1,     1,     1,     1,     2,     1,     1,     1,     5,
     1,     2,     1,     1,     1,     3,     1,     3,     1,     4,
     6,     3,     5,     2,     4,     1,     3,     6,     8,     4,
     6,     4,     2,     6,     2,     4,     6,     2,     4,     2,
     4,     1,     1,     1,     3,     1,     4,     1,     2,     1,
     3,     1,     1,     0,     3,     4,     2,     3,     3,     0,
     5,     2,     4,     4,     2,     4,     4,     3,     3,     3,
     2,     1,     4,     0,     5,     0,     5,     5,     1,     1,
     6,     0,     1,     1,     1,     2,     1,     2,     1,     1,
     1,     1,     1,     1,     1,     2,     3,     3,     3,     3,
     3,     0,     3,     1,     2,     3,     3,     0,     3,     0,
     2,     0,     2,     1,     0,     3,     0,     4,     1,     1,
     1,     1,     2,     1,     1,     1,     1,     3,     1,     1,
     2,     2,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     0,
     4,     2,     3,     2,     6,     8,     4,     6,     4,     6,
     2,     4,     6,     2,     4,     2,     4,     1,     0,     1,
     1,     1,     1,     1,     1,     1,     3,     1,     3,     3,
     3,     1,     3,     1,     3,     1,     1,     2,     1,     1,
     1,     2,     2,     0,     1,     0,     4,     1,     2,     1,
     3,     3,     2,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     0,     1,     0,     1,     2,
     2,     0,     1,     1,     1,     1,     1,     2,     0,     0,
    }, yyDefRed = {
//yyDefRed 959
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   295,   298,     0,     0,     0,   320,   321,     0,
     0,     0,   458,   457,   459,   460,     0,     0,     0,     9,
     0,   462,   461,   463,     0,     0,   454,   453,     0,   456,
   413,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   429,   431,   431,     0,     0,   373,   466,
   467,   448,   449,     0,   410,     0,   266,     0,   414,   267,
   268,     0,   269,   270,   265,   409,   411,    29,    43,     0,
     0,     0,     0,     0,     0,   271,     0,    51,     0,     0,
    82,     0,     4,     0,     0,    68,     0,     2,     0,     5,
     7,   318,   319,   282,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   464,     0,   107,     0,   322,     0,
   272,   311,   160,   171,   161,   184,   157,   177,   167,   166,
   182,   165,   164,   159,   185,   169,   158,   172,   176,   178,
   170,   163,   179,   186,   181,     0,     0,     0,     0,   156,
   175,   174,   187,   188,   189,   190,   191,   155,   162,   153,
   154,     0,     0,     0,     0,   111,     0,   145,   146,   142,
   124,   125,   126,   133,   130,   132,   127,   128,   147,   148,
   134,   135,   515,   139,   138,   123,   144,   141,   140,   136,
   137,   131,   129,   121,   143,   122,   149,   313,   112,     0,
   514,   113,   180,   173,   183,   168,   150,   151,   152,   109,
   110,   115,   114,   117,     0,   116,   118,     0,     0,     0,
     0,     0,    13,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   544,   545,     0,     0,     0,   546,     0,     0,
     0,     0,     0,     0,   332,   333,     0,     0,     0,     0,
     0,     0,   247,    53,     0,     0,     0,   519,   251,    54,
    52,     0,    67,     0,     0,   390,    66,     0,   538,     0,
     0,    17,     0,     0,     0,   213,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   238,     0,     0,
     0,   517,     0,     0,     0,     0,     0,     0,     0,     0,
   229,    47,   228,   445,   444,   446,   442,   443,     0,     0,
     0,     0,     0,     0,     0,     0,   292,     0,   395,   393,
   384,     0,   289,   415,   291,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,   379,   381,
     0,     0,     0,     0,     0,     0,    84,     0,     0,     0,
     0,     0,     0,     3,     0,     0,   450,   451,     0,   104,
     0,   106,     0,   469,   306,   468,     0,     0,     0,     0,
     0,     0,   533,   534,   315,   119,     0,     0,     0,   274,
    12,     0,     0,   324,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   547,     0,     0,     0,
     0,     0,     0,   303,   522,   259,   254,     0,     0,   248,
   257,     0,   249,     0,   284,     0,   253,   246,   245,     0,
     0,   288,    46,    19,    21,    20,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   277,     0,     0,
   280,     0,   542,   239,     0,   241,   518,   281,     0,    86,
     0,     0,     0,     0,     0,   436,   434,   447,   433,   432,
   416,   430,   417,   418,   419,   420,   423,     0,   425,   426,
     0,     0,   491,   490,   489,   492,     0,     0,   506,   505,
   510,   509,   495,     0,     0,     0,   503,     0,     0,     0,
     0,   487,   497,   493,     0,     0,    58,    61,    23,    24,
    25,    26,    27,    44,    45,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   528,     0,     0,   529,   388,     0,     0,
     0,     0,   387,     0,   389,     0,   526,   527,     0,     0,
    36,     0,     0,    42,    41,     0,    37,   258,     0,     0,
     0,     0,     0,    85,    30,    39,     0,    31,     0,     6,
     0,   471,     0,     0,     0,     0,     0,     0,   108,     0,
     0,     0,     0,     0,     0,     0,     0,   403,     0,     0,
   404,     0,     0,   330,     0,     0,   325,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   302,   327,   296,   326,
   299,     0,     0,     0,     0,     0,     0,   521,     0,     0,
     0,   255,   520,   283,   539,   242,   287,    18,     0,     0,
    28,     0,     0,     0,     0,   276,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   422,   424,   428,     0,
   494,     0,     0,   334,     0,   336,     0,     0,   507,   511,
     0,   485,     0,   367,   376,     0,     0,   374,     0,   480,
     0,   483,   365,     0,   363,     0,   362,     0,     0,     0,
     0,     0,     0,   244,     0,   385,   243,     0,     0,   386,
     0,     0,     0,    56,   382,    57,   383,     0,     0,     0,
     0,    83,     0,     0,     0,   309,     0,     0,   392,   312,
   516,     0,   473,     0,   316,   120,     0,     0,   406,   331,
     0,    11,   408,     0,   328,     0,     0,     0,     0,     0,
     0,   301,     0,     0,     0,     0,     0,     0,   261,   250,
   286,    10,   240,    87,     0,     0,   438,   439,   440,   435,
   441,   499,     0,     0,     0,     0,   496,     0,     0,   512,
   371,     0,   369,   372,     0,     0,     0,     0,   498,     0,
   504,     0,     0,     0,     0,     0,     0,   361,     0,   501,
     0,     0,     0,     0,     0,    33,     0,    34,     0,    63,
    35,     0,     0,    65,     0,   540,     0,     0,     0,     0,
     0,     0,   470,   307,   472,   314,     0,     0,     0,     0,
     0,   405,     0,   407,     0,   293,     0,   294,   260,     0,
     0,     0,   304,   437,   335,     0,     0,     0,   337,   375,
     0,   486,     0,   378,   377,     0,   478,     0,   476,     0,
   481,   484,     0,     0,   359,     0,     0,   354,     0,   357,
   364,   396,   394,     0,     0,   380,    32,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   398,   397,   399,
   297,   300,     0,     0,     0,     0,     0,   370,     0,     0,
     0,     0,     0,     0,     0,   366,     0,     0,     0,     0,
   502,    59,   310,     0,     0,     0,     0,     0,     0,   400,
     0,     0,     0,     0,   479,     0,   474,   477,   482,   279,
     0,   360,     0,   351,     0,   349,     0,   355,   358,   317,
     0,   329,   305,     0,     0,     0,     0,     0,     0,     0,
     0,   475,   353,     0,   347,   350,   356,     0,   348,
    }, yyDgoto = {
//yyDgoto 156
     1,   224,   306,    64,    65,   597,   562,   116,   212,   556,
   502,   394,   503,   504,   505,   199,    66,    67,    68,    69,
    70,   309,   308,   479,    71,    72,    73,   487,    74,    75,
    76,   117,    77,    78,   218,   219,   220,   221,    80,    81,
    82,    83,   226,   276,   744,   888,   745,   737,   437,   741,
   564,   384,   262,    85,   705,    86,    87,   506,   214,   769,
   228,   603,   604,   508,   794,   694,   695,   576,    89,    90,
   254,   415,   609,   286,   229,   222,   255,   315,   313,   509,
   510,   674,    93,   256,   257,   293,   470,   796,   429,   258,
   430,   681,   779,   322,   359,   517,    94,    95,   398,   230,
   215,   216,   512,   781,   684,   687,   316,   284,   799,   246,
   439,   675,   676,   782,   434,   711,   201,   513,    97,    98,
    99,     2,   235,   236,   273,   446,   435,   698,   606,   463,
   263,   459,   404,   238,   628,   754,   239,   755,   636,   892,
   593,   405,   590,   821,   389,   391,   605,   826,   317,   551,
   515,   514,   665,   664,   592,   390,
    }, yySindex = {
//yySindex 959
     0,     0, 14478, 14849,  5477, 17432, 18140, 18032, 14602, 16817,
 16817, 12817,     0,     0, 12990, 15095, 15095,     0,     0, 15095,
  -207,  -137,     0,     0,     0,     0,   122, 17924,   164,     0,
  -142,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 16940, 16940,  -191,   -53, 14726, 16817, 15464, 15833,  3918,
 16940, 17063, 18247,     0,     0,     0,   247,   256,     0,     0,
     0,     0,     0,     0,     0,  -183,     0,   -58,     0,     0,
     0,  -236,     0,     0,     0,     0,     0,     0,     0,  1088,
    19,  4886,     0,    32,   529,     0,   -37,     0,   -19,   284,
     0,   273,     0, 17309,   276,     0,    16,     0,   138,     0,
     0,     0,     0,     0,  -207,  -137,    18,   164,     0,     0,
   142, 16817,   -32, 14602,     0,  -183,     0,    76,     0,   622,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   -23,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   305,     0,     0, 14972,   208,   106,
   138,  1088,     0,   109,     0,    19,    57,   631,    23,   470,
   216,    57,     0,     0,   138,   292,   496,     0, 16817, 16817,
   258,     0,   691,     0,     0,     0,   293, 16940, 16940, 16940,
 16940,  4886,     0,     0,   279,   550,   552,     0,     0,     0,
     0,  3457,     0, 15095, 15095,     0,     0,  4443,     0, 16817,
  -206,     0, 15956,   275, 14602,     0,   771,   322,   327,   329,
   295, 14726,   307,     0,   164,    19,   318,     0,   156,   163,
   279,     0,   163,   289,   349, 17555,     0,   831,     0,   620,
     0,     0,     0,     0,     0,     0,     0,     0,   407,   568,
   606,   377,   296,   785,   298,  -118,     0,  2404,     0,     0,
     0,   341,     0,     0,     0, 16817, 16817, 16817, 16817, 14972,
 16817, 16817, 16940, 16940, 16940, 16940, 16940, 16940, 16940, 16940,
 16940, 16940, 16940, 16940, 16940, 16940, 16940, 16940, 16940, 16940,
 16940, 16940, 16940, 16940, 16940, 16940, 16940, 16940,     0,     0,
  2470,  2956, 15095, 18739, 18739, 17063,     0, 16079, 14726,  6009,
   637, 16079, 17063,     0, 14230,   353,     0,     0,    19,     0,
     0,     0,   138,     0,     0,     0,  3532,  3992, 15095, 14602,
 16817,  2416,     0,     0,     0,     0,  1088, 16202,   420,     0,
     0, 14354,   295,     0, 14602,   428,  5016,  6629, 15095, 16940,
 16940, 16940, 14602,   292, 16325,   432,     0,    69,    69,     0,
 14111, 18409, 15095,     0,     0,     0,     0, 16940, 15218,     0,
     0, 15587,     0,   164,     0,   357,     0,     0,     0,   164,
    56,     0,     0,     0,     0,     0, 18032, 16817,  4886, 14478,
   340,  5016,  6629, 16940, 16940, 16940,   164,     0,     0,   164,
     0, 15710,     0,     0, 15833,     0,     0,     0,     0,     0,
   659, 18464, 18519, 15095, 17555,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    -5,     0,     0,
   674,   653,     0,     0,     0,     0,  1414,  2004,     0,     0,
     0,     0,     0,   403,   409,   675,     0,   657,  -168,   679,
   681,     0,     0,     0,  -157,  -157,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   322,  2564,  2564,  2564,  2564,
  1863,  1863,  5372,  4039,  2564,  2564,  3004,  3004,   889,   889,
   322,  1932,   322,   322,   -51,   -51,  1863,  1863,  2555,  2555,
  1696,  -157,   391,     0,   392,  -137,     0,     0,   396,     0,
   397,  -137,     0,     0,     0,   164,     0,     0,  -137,  -137,
     0,  4886, 16940,     0,     0,  4520,     0,     0,   677,   692,
   164, 17555,   697,     0,     0,     0,     0,     0,  4955,     0,
   138,     0, 16817, 14602,  -137,     0,     0,  -137,     0,   164,
   478,    56,  2004,   138, 14602, 18354, 18032,     0,     0,   406,
     0, 14602,   486,     0,  1088,   335,     0,   413,   416,   425,
   397,   164,  4520,   420,   499,    60,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   164, 16817,     0, 16940,   279,
   552,     0,     0,     0,     0,     0,     0,     0,    56,   405,
     0,   322,   322,  4886,     0,     0,   163, 17555,     0,     0,
     0,     0,   164,   659, 14602,  -126,     0,     0,     0, 16940,
     0,  1414,   515,     0,   725,     0,   164,   657,     0,     0,
  1342,     0,   507,     0,     0, 14602, 14602,     0,  2004,     0,
  2004,     0,     0,  1828,     0, 14602,     0, 14602,  -157,   715,
 14602, 17063, 17063,     0,   341,     0,     0, 17063, 16940,     0,
   341,   437,   443,     0,     0,     0,     0,     0, 16940, 17063,
 16448,     0,   659, 17555, 16940,     0,   138,   517,     0,     0,
     0,   164,     0,   532,     0,     0, 17678,    57,     0,     0,
 14602,     0,     0, 16817,     0,   533, 16940, 16940, 16940,   461,
   564,     0, 16571, 14602, 14602, 14602,     0,    69,     0,     0,
     0,     0,     0,     0,     0,   440,     0,     0,     0,     0,
     0,     0,   164,  1387,   770,  1485,     0,   164,   788,     0,
     0,   790,     0,     0,   566,   477,   797,   798,     0,   799,
     0,   788,   792,   807,   657,   815,   816,     0,   508,     0,
   601,   506, 14602, 16940,   605,     0,  4886,     0,  4886,     0,
     0,  4886,  4886,     0, 17063,     0,  4886, 16940,     0,   659,
  4886, 14602,     0,     0,     0,     0,  2416,   560,     0,   836,
     0,     0, 14602,     0,    57,     0, 16940,     0,     0,   -31,
   610,   614,     0,     0,     0,   835,  1387,   538,     0,     0,
  1342,     0,   507,     0,     0,  1342,     0,  2004,     0,  1342,
     0,     0, 17801,  1342,     0,   523,  2528,     0,  2528,     0,
     0,     0,     0,   521,  4886,     0,     0,  4886,     0,   632,
 14602,     0, 18574, 18629, 15095,   208, 14602,     0,     0,     0,
     0,     0, 14602,  1387,   835,  1387,   847,     0,   788,   864,
   788,   788,   585,   851,   788,     0,   865,   887,   890,   788,
     0,     0,     0,   666,     0,     0,     0,     0,   164,     0,
   335,   687,   835,  1387,     0,  1342,     0,     0,     0,     0,
 18684,     0,  1342,     0,  2528,     0,  1342,     0,     0,     0,
     0,     0,     0,   835,   788,     0,     0,   788,   912,   788,
   788,     0,     0,  1342,     0,     0,     0,   788,     0,
    }, yyRindex = {
//yyRindex 959
     0,     0,   146,     0,     0,     0,     0,     0,   648,     0,
     0,   689,     0,     0,     0, 13155, 13261,     0,     0, 13403,
  4771,  4278,     0,     0,     0,     0, 17186,     0, 16694,     0,
     0,     0,     0,     0,  2183,  3292,     0,     0,  2306,     0,
     0,     0,     0,     0,     0,    30,     0,   636,   638,    90,
     0,     0,   849,     0,     0,     0,   868,  -102,     0,     0,
     0,     0,     0, 13506,     0, 15341,     0,  6993,     0,     0,
     0,  7094,     0,     0,     0,     0,     0,     0,     0,    50,
  1006, 14005,  7238, 14058,     0,     0, 14115,     0, 13620,     0,
     0,     0,     0,   150,     0,     0,     0,     0,    45,     0,
     0,     0,     0,     0,  7342,  6299,     0,   644, 11782, 11908,
     0,     0,     0,    30,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  1013,  1159,  1759,  2065,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,  2081,  2422,  2545,  2896,     0,  3038,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 12878,     0,     0,     0,   315,     0,
  1192,    21,     0,     0,  6413,  5378,     0,     0,  6745,     0,
     0,     0,     0,     0,   689,     0,   722,     0,     0,     0,
     0,   664,     0,   765,     0,     0,     0,     0,     0,     0,
     0, 11566,     0,     0,  1138,  2000,  2000,     0,     0,     0,
     0,   661,     0,     0,    96,     0,     0,   661,     0,     0,
     0,     0,     0,     0,    26,     0,     0,  7703,  7455,  7587,
 13754,    30,     0,    84,   661,    97,     0,     0,   650,   650,
     0,     0,   643,     0,     0,     0,  1052,     0,  1237,   157,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   257,     0,     0,
     0, 13865,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,    24,     0,     0,     0,     0,     0,    30,   169,
   225,     0,     0,     0,    51,     0,     0,     0,   145,     0,
 12298,     0,     0,     0,     0,     0,     0,     0,    24,   648,
     0,   149,     0,     0,     0,     0,   261,   468,   378,     0,
     0,  1346,  6879,     0,   721, 12424,     0,     0,    24,     0,
     0,     0,    95,     0,     0,     0,     0,     0,     0,  1061,
     0,     0,    24,     0,     0,     0,     0,     0,  4889,     0,
     0,  4889,     0,   661,     0,     0,     0,     0,     0,   661,
   661,     0,     0,     0,     0,     0,     0,     0,  1603,    26,
     0,     0,     0,     0,     0,     0,   661,     0,    73,   661,
     0,   671,     0,     0,  -169,     0,     0,     0,  1492,     0,
   226,     0,     0,    24,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,   103,     0,     0,     0,     0,     0,    93,     0,     0,
     0,     0,     0,    37,     0,    10,     0,  -164,     0,    10,
    10,     0,     0,     0, 12556, 12693,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  7804,  9768,  9890, 10008, 10105,
  9313,  9433, 10191, 10464, 10281, 10378, 10554, 10594,  8733,  8855,
  7919,  8978,  8052,  8167,  8516,  8629,  9553,  9650,  9096,  9204,
   950, 12556,  5132,     0,  5255,  4648,     0,     0,  5625,  3662,
  5748, 15341,     0,  3785,     0,   683,     0,     0,  5871,  5871,
     0, 10679,     0,     0,     0,  1895,     0,     0,     0,     0,
   661,     0,   232,     0,     0,     0, 10502,     0, 11652,     0,
     0,     0,     0,   648,  6547, 12040, 12166,     0,     0,   683,
     0,   661,    98,     0,   648,     0,     0,     0,   155,   591,
     0,   748,   769,     0,   271,   769,     0,  2676,  2799,  3169,
  4155,   683, 11687,   769,     0,     0,     0,     0,     0,     0,
     0,   431,   952,  1264,   746,   683,     0,     0,     0,  1517,
  2000,     0,     0,     0,     0,     0,     0,     0,   661,     0,
     0,  8268,  8384, 10775,    83,     0,   650,     0,   148,   853,
   888,   926,   683,   235,    26,     0,     0,     0,     0,     0,
     0,     0,   104,     0,   108,     0,   661,    96,     0,     0,
     0,     0,     0,     0,     0,   166,    26,     0,     0,     0,
     0,     0,     0,   652,     0,   166,     0,    26, 12693,     0,
   166,     0,     0,     0, 13908,     0,     0,     0,     0,     0,
 13969, 13054,     0,     0,     0,     0,     0, 11514,     0,     0,
     0,     0,   445,     0,     0,     0,     0,     0,     0,     0,
     0,   661,     0,     0,     0,     0,     0,     0,     0,     0,
   166,     0,     0,     0,     0,     0,     0,     0,     0,  6198,
     0,     0,     0,   760,   166,   166,   856,     0,     0,     0,
     0,     0,     0,     0,   627,     0,     0,     0,     0,     0,
     0,     0,   661,     0,   110,     0,     0,   661,    10,     0,
     0,   154,     0,     0,     0,     0,    10,    10,     0,    10,
     0,    10,   202,    -2,   652,    -2,    -2,     0,     0,     0,
     0,     0,    26,     0,     0,     0, 10860,     0, 10921,     0,
     0, 11018, 11104,     0,     0,     0, 11213,     0, 11668,   497,
 11274,   648,     0,     0,     0,     0,   149,     0,   702,     0,
   802,     0,   648,     0,     0,     0,     0,     0,     0,   769,
     0,     0,     0,     0,     0,   126,     0,   128,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     9,     0,     0,     0,
     0,     0,     0,     0, 11360,     0,     0, 11457, 14186,     0,
   648,   874,     0,     0,    24,   315,   721,     0,     0,     0,
     0,     0,   166,     0,   130,     0,   132,     0,    10,    10,
    10,    10,     0,   203,    -2,     0,    -2,    -2,    -2,    -2,
     0,     0,     0,     0,   742,  1038,  1124,   588,   683,     0,
   769,     0,   139,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  1004,     0,     0,   162,    10,   646,   954,    -2,    -2,    -2,
    -2,     0,     0,     0,     0,     0,     0,    -2,     0,
    }, yyGindex = {
//yyGindex 156
     0,   371,     0,     8,  1581,  -308,     0,   -54,    20,    -6,
   -43,     0,     0,     0,   822,     0,     0,     0,   969,     0,
     0,     0,   599,  -187,     0,     0,     0,     0,     0,     0,
    12,  1037,  -302,   -46,   423,  -380,     0,    89,    13,  1395,
    28,    -8,    65,   218,  -392,     0,   127,     0,   886,     0,
    66,     0,    -4,  1043,   115,     0,     0,  -580,     0,     0,
   672,  -254,   227,     0,     0,     0,  -362,  -180,   -80,    52,
   624,  -397,     0,     0,   786,     1,   -10,     0,     0,  5714,
   366,  -578,     0,   -18,  -270,     0,  -401,   192,  -238,  -155,
     0,  1173,  -307,  1007,     0,  -581,  1065,   213,   190,   884,
     0,   -13,  -606,     0,  -582,     0,     0,  -166,  -718,     0,
  -334,  -645,   412,   237,   287,  -327,     0,  -611,   641,     0,
    22,     0,   -36,   -34,     0,     0,   -24,     0,     0,  -251,
     0,     0,  -219,     0,  -375,     0,     0,     0,     0,     0,
     0,    79,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,
    };
    protected static final short[] yyTable = Ruby19YyTables.yyTable();
    protected static final short[] yyCheck = Ruby19YyTables.yyCheck();

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
    "program : $$1 top_compstmt",
    "top_compstmt : top_stmts opt_terms",
    "top_stmts : none",
    "top_stmts : top_stmt",
    "top_stmts : top_stmts terms top_stmt",
    "top_stmts : error top_stmt",
    "top_stmt : stmt",
    "$$2 :",
    "top_stmt : klBEGIN $$2 tLCURLY top_compstmt tRCURLY",
    "bodystmt : compstmt opt_rescue opt_else opt_ensure",
    "compstmt : stmts opt_terms",
    "stmts : none",
    "stmts : stmt",
    "stmts : stmts terms stmt",
    "stmts : error stmt",
    "$$3 :",
    "stmt : kALIAS fitem $$3 fitem",
    "stmt : kALIAS tGVAR tGVAR",
    "stmt : kALIAS tGVAR tBACK_REF",
    "stmt : kALIAS tGVAR tNTH_REF",
    "stmt : kUNDEF undef_list",
    "stmt : stmt kIF_MOD expr_value",
    "stmt : stmt kUNLESS_MOD expr_value",
    "stmt : stmt kWHILE_MOD expr_value",
    "stmt : stmt kUNTIL_MOD expr_value",
    "stmt : stmt kRESCUE_MOD stmt",
    "stmt : klEND tLCURLY compstmt tRCURLY",
    "stmt : command_asgn",
    "stmt : mlhs '=' command_call",
    "stmt : var_lhs tOP_ASGN command_call",
    "stmt : primary_value '[' opt_call_args rbracket tOP_ASGN command_call",
    "stmt : primary_value tDOT tIDENTIFIER tOP_ASGN command_call",
    "stmt : primary_value tDOT tCONSTANT tOP_ASGN command_call",
    "stmt : primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_call",
    "stmt : backref tOP_ASGN command_call",
    "stmt : lhs '=' mrhs",
    "stmt : mlhs '=' arg_value",
    "stmt : mlhs '=' mrhs",
    "stmt : expr",
    "command_asgn : lhs '=' command_call",
    "command_asgn : lhs '=' command_asgn",
    "expr : command_call",
    "expr : expr kAND expr",
    "expr : expr kOR expr",
    "expr : kNOT opt_nl expr",
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
    "cmd_brace_block : tLBRACE_ARG $$4 opt_block_param compstmt tRCURLY",
    "command : operation command_args",
    "command : operation command_args cmd_brace_block",
    "command : primary_value tDOT operation2 command_args",
    "command : primary_value tDOT operation2 command_args cmd_brace_block",
    "command : primary_value tCOLON2 operation2 command_args",
    "command : primary_value tCOLON2 operation2 command_args cmd_brace_block",
    "command : kSUPER command_args",
    "command : kYIELD command_args",
    "mlhs : mlhs_basic",
    "mlhs : tLPAREN mlhs_inner rparen",
    "mlhs_inner : mlhs_basic",
    "mlhs_inner : tLPAREN mlhs_inner rparen",
    "mlhs_basic : mlhs_head",
    "mlhs_basic : mlhs_head mlhs_item",
    "mlhs_basic : mlhs_head tSTAR mlhs_node",
    "mlhs_basic : mlhs_head tSTAR mlhs_node ',' mlhs_post",
    "mlhs_basic : mlhs_head tSTAR",
    "mlhs_basic : mlhs_head tSTAR ',' mlhs_post",
    "mlhs_basic : tSTAR mlhs_node",
    "mlhs_basic : tSTAR mlhs_node ',' mlhs_post",
    "mlhs_basic : tSTAR",
    "mlhs_basic : tSTAR ',' mlhs_post",
    "mlhs_item : mlhs_node",
    "mlhs_item : tLPAREN mlhs_inner rparen",
    "mlhs_head : mlhs_item ','",
    "mlhs_head : mlhs_head mlhs_item ','",
    "mlhs_post : mlhs_item",
    "mlhs_post : mlhs_post ',' mlhs_item",
    "mlhs_node : variable",
    "mlhs_node : primary_value '[' opt_call_args rbracket",
    "mlhs_node : primary_value tDOT tIDENTIFIER",
    "mlhs_node : primary_value tCOLON2 tIDENTIFIER",
    "mlhs_node : primary_value tDOT tCONSTANT",
    "mlhs_node : primary_value tCOLON2 tCONSTANT",
    "mlhs_node : tCOLON3 tCONSTANT",
    "mlhs_node : backref",
    "lhs : variable",
    "lhs : primary_value '[' opt_call_args rbracket",
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
    "op : tNMATCH",
    "op : tGT",
    "op : tGEQ",
    "op : tLT",
    "op : tLEQ",
    "op : tNEQ",
    "op : tLSHFT",
    "op : tRSHFT",
    "op : tPLUS",
    "op : tMINUS",
    "op : tSTAR2",
    "op : tSTAR",
    "op : tDIVIDE",
    "op : tPERCENT",
    "op : tPOW",
    "op : tBANG",
    "op : tTILDE",
    "op : tUPLUS",
    "op : tUMINUS",
    "op : tAREF",
    "op : tASET",
    "op : tBACK_REF2",
    "reswords : k__LINE__",
    "reswords : k__FILE__",
    "reswords : k__ENCODING__",
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
    "arg : var_lhs tOP_ASGN arg kRESCUE_MOD arg",
    "arg : primary_value '[' opt_call_args rbracket tOP_ASGN arg",
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
    "arg : arg '?' arg opt_nl ':' arg",
    "arg : primary",
    "arg_value : arg",
    "aref_args : none",
    "aref_args : args trailer",
    "aref_args : args ',' assocs trailer",
    "aref_args : assocs trailer",
    "paren_args : tLPAREN2 opt_call_args rparen",
    "opt_paren_args : none",
    "opt_paren_args : paren_args",
    "opt_call_args : none",
    "opt_call_args : call_args",
    "call_args : command",
    "call_args : args opt_block_arg",
    "call_args : assocs opt_block_arg",
    "call_args : args ',' assocs opt_block_arg",
    "call_args : block_arg",
    "$$6 :",
    "command_args : $$6 call_args",
    "block_arg : tAMPER arg_value",
    "opt_block_arg : ',' block_arg",
    "opt_block_arg : ','",
    "opt_block_arg : none_block_pass",
    "args : arg_value",
    "args : tSTAR arg_value",
    "args : args ',' arg_value",
    "args : args ',' tSTAR arg_value",
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
    "$$7 :",
    "primary : tLPAREN_ARG expr $$7 rparen",
    "primary : tLPAREN compstmt tRPAREN",
    "primary : primary_value tCOLON2 tCONSTANT",
    "primary : tCOLON3 tCONSTANT",
    "primary : tLBRACK aref_args tRBRACK",
    "primary : tLBRACE assoc_list tRCURLY",
    "primary : kRETURN",
    "primary : kYIELD tLPAREN2 call_args rparen",
    "primary : kYIELD tLPAREN2 rparen",
    "primary : kYIELD",
    "primary : kDEFINED opt_nl tLPAREN2 expr rparen",
    "primary : kNOT tLPAREN2 expr rparen",
    "primary : kNOT tLPAREN2 rparen",
    "primary : operation brace_block",
    "primary : method_call",
    "primary : method_call brace_block",
    "primary : tLAMBDA lambda",
    "primary : kIF expr_value then compstmt if_tail kEND",
    "primary : kUNLESS expr_value then compstmt opt_else kEND",
    "$$8 :",
    "$$9 :",
    "primary : kWHILE $$8 expr_value do $$9 compstmt kEND",
    "$$10 :",
    "$$11 :",
    "primary : kUNTIL $$10 expr_value do $$11 compstmt kEND",
    "primary : kCASE expr_value opt_terms case_body kEND",
    "primary : kCASE opt_terms case_body kEND",
    "$$12 :",
    "$$13 :",
    "primary : kFOR for_var kIN $$12 expr_value do $$13 compstmt kEND",
    "$$14 :",
    "primary : kCLASS cpath superclass $$14 bodystmt kEND",
    "$$15 :",
    "$$16 :",
    "primary : kCLASS tLSHFT expr $$15 term $$16 bodystmt kEND",
    "$$17 :",
    "primary : kMODULE cpath $$17 bodystmt kEND",
    "$$18 :",
    "primary : kDEF fname $$18 f_arglist bodystmt kEND",
    "$$19 :",
    "$$20 :",
    "primary : kDEF singleton dot_or_colon $$19 fname $$20 f_arglist bodystmt kEND",
    "primary : kBREAK",
    "primary : kNEXT",
    "primary : kREDO",
    "primary : kRETRY",
    "primary_value : primary",
    "then : term",
    "then : kTHEN",
    "then : term kTHEN",
    "do : term",
    "do : kDO_COND",
    "if_tail : opt_else",
    "if_tail : kELSIF expr_value then compstmt if_tail",
    "opt_else : none",
    "opt_else : kELSE compstmt",
    "for_var : lhs",
    "for_var : mlhs",
    "f_marg : f_norm_arg",
    "f_marg : tLPAREN f_margs rparen",
    "f_marg_list : f_marg",
    "f_marg_list : f_marg_list ',' f_marg",
    "f_margs : f_marg_list",
    "f_margs : f_marg_list ',' tSTAR f_norm_arg",
    "f_margs : f_marg_list ',' tSTAR f_norm_arg ',' f_marg_list",
    "f_margs : f_marg_list ',' tSTAR",
    "f_margs : f_marg_list ',' tSTAR ',' f_marg_list",
    "f_margs : tSTAR f_norm_arg",
    "f_margs : tSTAR f_norm_arg ',' f_marg_list",
    "f_margs : tSTAR",
    "f_margs : tSTAR ',' f_marg_list",
    "block_param : f_arg ',' f_block_optarg ',' f_rest_arg opt_f_block_arg",
    "block_param : f_arg ',' f_block_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "block_param : f_arg ',' f_block_optarg opt_f_block_arg",
    "block_param : f_arg ',' f_block_optarg ',' f_arg opt_f_block_arg",
    "block_param : f_arg ',' f_rest_arg opt_f_block_arg",
    "block_param : f_arg ','",
    "block_param : f_arg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "block_param : f_arg opt_f_block_arg",
    "block_param : f_block_optarg ',' f_rest_arg opt_f_block_arg",
    "block_param : f_block_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "block_param : f_block_optarg opt_f_block_arg",
    "block_param : f_block_optarg ',' f_arg opt_f_block_arg",
    "block_param : f_rest_arg opt_f_block_arg",
    "block_param : f_rest_arg ',' f_arg opt_f_block_arg",
    "block_param : f_block_arg",
    "opt_block_param : none",
    "opt_block_param : block_param_def",
    "block_param_def : tPIPE opt_bv_decl tPIPE",
    "block_param_def : tOROP",
    "block_param_def : tPIPE block_param opt_bv_decl tPIPE",
    "opt_bv_decl : none",
    "opt_bv_decl : ';' bv_decls",
    "bv_decls : bvar",
    "bv_decls : bv_decls ',' bvar",
    "bvar : tIDENTIFIER",
    "bvar : f_bad_arg",
    "$$21 :",
    "lambda : $$21 f_larglist lambda_body",
    "f_larglist : tLPAREN2 f_args opt_bv_decl rparen",
    "f_larglist : f_args opt_bv_decl",
    "lambda_body : tLAMBEG compstmt tRCURLY",
    "lambda_body : kDO_LAMBDA compstmt kEND",
    "$$22 :",
    "do_block : kDO_BLOCK $$22 opt_block_param compstmt kEND",
    "block_call : command do_block",
    "block_call : block_call tDOT operation2 opt_paren_args",
    "block_call : block_call tCOLON2 operation2 opt_paren_args",
    "method_call : operation paren_args",
    "method_call : primary_value tDOT operation2 opt_paren_args",
    "method_call : primary_value tCOLON2 operation2 paren_args",
    "method_call : primary_value tCOLON2 operation3",
    "method_call : primary_value tDOT paren_args",
    "method_call : primary_value tCOLON2 paren_args",
    "method_call : kSUPER paren_args",
    "method_call : kSUPER",
    "method_call : primary_value '[' opt_call_args rbracket",
    "$$23 :",
    "brace_block : tLCURLY $$23 opt_block_param compstmt tRCURLY",
    "$$24 :",
    "brace_block : kDO $$24 opt_block_param compstmt kEND",
    "case_body : kWHEN args then compstmt cases",
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
    "string : tCHAR",
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
    "$$25 :",
    "string_content : tSTRING_DVAR $$25 string_dvar",
    "$$26 :",
    "string_content : tSTRING_DBEG $$26 compstmt tRCURLY",
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
    "variable : k__ENCODING__",
    "var_ref : variable",
    "var_lhs : variable",
    "backref : tNTH_REF",
    "backref : tBACK_REF",
    "superclass : term",
    "$$27 :",
    "superclass : tLT $$27 expr_value term",
    "superclass : error term",
    "f_arglist : tLPAREN2 f_args rparen",
    "f_arglist : f_args term",
    "f_args : f_arg ',' f_optarg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_arg ',' f_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "f_args : f_arg ',' f_optarg opt_f_block_arg",
    "f_args : f_arg ',' f_optarg ',' f_arg opt_f_block_arg",
    "f_args : f_arg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_arg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "f_args : f_arg opt_f_block_arg",
    "f_args : f_optarg ',' f_rest_arg opt_f_block_arg",
    "f_args : f_optarg ',' f_rest_arg ',' f_arg opt_f_block_arg",
    "f_args : f_optarg opt_f_block_arg",
    "f_args : f_optarg ',' f_arg opt_f_block_arg",
    "f_args : f_rest_arg opt_f_block_arg",
    "f_args : f_rest_arg ',' f_arg opt_f_block_arg",
    "f_args : f_block_arg",
    "f_args :",
    "f_bad_arg : tCONSTANT",
    "f_bad_arg : tIVAR",
    "f_bad_arg : tGVAR",
    "f_bad_arg : tCVAR",
    "f_norm_arg : f_bad_arg",
    "f_norm_arg : tIDENTIFIER",
    "f_arg_item : f_norm_arg",
    "f_arg_item : tLPAREN f_margs rparen",
    "f_arg : f_arg_item",
    "f_arg : f_arg ',' f_arg_item",
    "f_opt : tIDENTIFIER '=' arg_value",
    "f_block_opt : tIDENTIFIER '=' primary_value",
    "f_block_optarg : f_block_opt",
    "f_block_optarg : f_block_optarg ',' f_block_opt",
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
    "$$28 :",
    "singleton : tLPAREN2 $$28 expr rparen",
    "assoc_list : none",
    "assoc_list : assocs trailer",
    "assocs : assoc",
    "assocs : assocs ',' assoc",
    "assoc : arg_value tASSOC arg_value",
    "assoc : tLABEL arg_value",
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
    "rparen : opt_nl tRPAREN",
    "rbracket : opt_nl tRBRACK",
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


