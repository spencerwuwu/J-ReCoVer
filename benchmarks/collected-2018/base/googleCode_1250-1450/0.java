// https://searchcode.com/api/result/13898923/

//### This file created by BYACC 1.8(/Java extension  1.15)
//### Java capabilities added 7 Jan 97, Bob Jamison
//### Updated : 27 Nov 97  -- Bob Jamison, Joe Nieten
//###           01 Jan 98  -- Bob Jamison -- fixed generic semantic constructor
//###           01 Jun 99  -- Bob Jamison -- added Runnable support
//###           06 Aug 00  -- Bob Jamison -- made state variables class-global
//###           03 Jan 01  -- Bob Jamison -- improved flags, tracing
//###           16 May 01  -- Bob Jamison -- added custom stack sizing
//###           04 Mar 02  -- Yuval Oren  -- improved java performance, added options
//###           14 Mar 02  -- Tomas Hurka -- -d support, static initializer workaround
//### Please send bug reports to tom@hukatronic.cz
//### static char yysccsid[] = "@(#)yaccpar	1.8 (Berkeley) 01/20/90";






//#line 2 "kurs.y"
	/* USER CODE*/
//#line 19 "Parser.java"




public class Parser
{

boolean yydebug;        //do I want debug output?
int yynerrs;            //number of errors so far
int yyerrflag;          //was there an error?
int yychar;             //the current working character

//########## MESSAGES ##########
//###############################################################
// method: debug
//###############################################################
void debug(String msg)
{
  if (yydebug)
    System.out.println(msg);
}

//########## STATE STACK ##########
final static int YYSTACKSIZE = 500;  //maximum stack size
int statestk[] = new int[YYSTACKSIZE]; //state stack
int stateptr;
int stateptrmax;                     //highest index of stackptr
int statemax;                        //state when highest index reached
//###############################################################
// methods: state stack push,pop,drop,peek
//###############################################################
final void state_push(int state)
{
  try {
		stateptr++;
		statestk[stateptr]=state;
	 }
	 catch (ArrayIndexOutOfBoundsException e) {
     int oldsize = statestk.length;
     int newsize = oldsize * 2;
     int[] newstack = new int[newsize];
     System.arraycopy(statestk,0,newstack,0,oldsize);
     statestk = newstack;
     statestk[stateptr]=state;
  }
}
final int state_pop()
{
  return statestk[stateptr--];
}
final void state_drop(int cnt)
{
  stateptr -= cnt; 
}
final int state_peek(int relative)
{
  return statestk[stateptr-relative];
}
//###############################################################
// method: init_stacks : allocate and prepare stacks
//###############################################################
final boolean init_stacks()
{
  stateptr = -1;
  val_init();
  return true;
}
//###############################################################
// method: dump_stacks : show n levels of the stacks
//###############################################################
void dump_stacks(int count)
{
int i;
  System.out.println("=index==state====value=     s:"+stateptr+"  v:"+valptr);
  for (i=0;i<count;i++)
    System.out.println(" "+i+"    "+statestk[i]+"      "+valstk[i]);
  System.out.println("======================");
}


//########## SEMANTIC VALUES ##########
//public class ParserVal is defined in ParserVal.java


String   yytext;//user variable to return contextual strings
ParserVal yyval; //used to return semantic vals from action routines
ParserVal yylval;//the 'lval' (result) I got from yylex()
ParserVal valstk[];
int valptr;
//###############################################################
// methods: value stack push,pop,drop,peek.
//###############################################################
void val_init()
{
  valstk=new ParserVal[YYSTACKSIZE];
  yyval=new ParserVal();
  yylval=new ParserVal();
  valptr=-1;
}
void val_push(ParserVal val)
{
  if (valptr>=YYSTACKSIZE)
    return;
  valstk[++valptr]=val;
}
ParserVal val_pop()
{
  if (valptr<0)
    return new ParserVal();
  return valstk[valptr--];
}
void val_drop(int cnt)
{
int ptr;
  ptr=valptr-cnt;
  if (ptr<0)
    return;
  valptr = ptr;
}
ParserVal val_peek(int relative)
{
int ptr;
  ptr=valptr-relative;
  if (ptr<0)
    return new ParserVal();
  return valstk[ptr];
}
final ParserVal dup_yyval(ParserVal val)
{
  ParserVal dup = new ParserVal();
  dup.ival = val.ival;
  dup.dval = val.dval;
  dup.sval = val.sval;
  dup.obj = val.obj;
  return dup;
}
//#### end semantic value section ####
public final static short CLASS=257;
public final static short STATIC=258;
public final static short VOID=259;
public final static short RETURN=260;
public final static short NEW=261;
public final static short IF=262;
public final static short ELSE=263;
public final static short FOR=264;
public final static short BM=265;
public final static short INT=266;
public final static short DOUBLE=267;
public final static short BOOLEAN=268;
public final static short TRANSPOSITION=269;
public final static short INVERSION=270;
public final static short GET_H=271;
public final static short GET_L=272;
public final static short CONJUNCTION=273;
public final static short DISJUNCTION=274;
public final static short ID=275;
public final static short ADD_OP=276;
public final static short MUL_OP=277;
public final static short ASSIGN_OP=278;
public final static short UNAR_OP=279;
public final static short NOT_OP=280;
public final static short AND_OP=281;
public final static short OR_OP=282;
public final static short SEPARATOR1=283;
public final static short SEPARATOR2=284;
public final static short SEPARATOR3=285;
public final static short SEPARATOR4=286;
public final static short SEPARATOR5=287;
public final static short SEPARATOR6=288;
public final static short SEPARATOR7=289;
public final static short SEPARATOR8=290;
public final static short SEPARATOR9=291;
public final static short INT_VAL=292;
public final static short REAL_VAL=293;
public final static short BOOL_VAL=294;
public final static short MAIN=295;
public final static short PRINTLN=296;
public final static short READLN=297;
public final static short YYERRCODE=256;
final static short yylhs[] = {                           -1,
    0,    1,    2,    2,    2,    3,    4,    4,    8,    8,
    8,    8,    8,    8,    8,    8,   10,   10,   10,   10,
   10,   10,   11,   12,   12,   12,   12,   12,   13,   18,
   18,   19,   20,   20,   14,   15,   16,   17,    9,    9,
   21,   22,   23,   24,   24,   24,   25,   25,   25,   26,
   26,   27,   28,   29,   29,   29,   30,   30,   31,   31,
   32,   32,   33,   34,   35,   36,   37,   37,   38,   39,
   40,   41,   41,   42,   42,   43,   43,    6,   47,    6,
   44,   44,   44,   45,   45,   48,   48,   49,   49,   49,
   46,   46,    5,    5,    5,   50,   50,   50,   50,   50,
   50,    7,    7,    7,   51,   52,   53,   53,
};
final static short yylen[] = {                            2,
    1,    6,    3,    4,    0,    8,    1,    1,    1,    1,
    1,    1,    2,    3,    3,    3,    1,    1,    3,    3,
    3,    2,    4,    1,    1,    1,    1,    1,    5,    1,
    3,    3,    1,    3,    5,    5,    6,    6,    1,    1,
    5,    5,   11,    4,    3,    0,    1,    1,    0,    1,
    0,    1,    1,    1,    1,    1,    2,    4,    2,    4,
    2,    4,    8,    1,    1,    1,    4,    0,    1,    1,
    4,    1,    0,    1,    3,    1,    1,   11,    0,   10,
    1,    1,    1,    1,    0,    1,    3,    2,    2,    2,
    2,    2,    3,    4,    0,    1,    1,    1,    1,    1,
    1,    1,    1,    1,    4,    2,    3,    3,
};
final static short yydefred[] = {                         0,
    0,    0,    1,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    7,    8,  103,  102,   54,   55,
   56,  104,    0,   81,   82,   83,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,   10,   11,  108,    0,   12,   39,
   40,    0,    0,    2,    3,    0,    0,    0,    0,   23,
   24,   25,   26,   27,   28,    0,    0,   17,    0,    0,
   18,    0,   13,    0,    0,    0,    0,    4,    0,    0,
    0,    0,    0,   84,    0,    0,    0,    0,   22,    0,
    0,    0,    0,    0,   16,    0,   15,    0,   88,   89,
   90,    0,    0,    0,    0,    0,    0,    0,    0,   21,
   20,    0,    0,    0,    0,    0,   87,    0,    0,    0,
    0,    0,    0,    0,    0,   41,   42,    0,    0,    0,
    0,    0,    0,    0,   96,   97,   98,   99,    0,  100,
  101,    0,    0,    0,    0,   29,    0,   35,   36,    0,
    0,    0,    0,    0,    0,    0,  106,    0,    0,   79,
    0,    0,   32,   31,   37,   38,    0,    0,    0,    0,
    0,   77,    0,    0,   72,    0,    0,    0,    6,   93,
   80,    0,    0,    0,   34,    0,    0,    0,    0,   71,
    0,   94,  105,   92,   91,   78,   65,    0,    0,   45,
   48,   47,    0,   75,    0,   44,    0,   66,    0,   50,
    0,    0,   63,    0,   69,    0,   52,    0,    0,    0,
   70,   67,   53,   43,
};
final static short yydgoto[] = {                          2,
    3,   13,   35,   14,  134,   15,  135,  173,   49,   72,
   17,   60,   61,   62,   63,   64,   65,  120,  121,  145,
   50,   51,  136,  171,  203,  211,  218,  224,   18,   19,
   20,   21,  137,  168,  198,  209,  213,  216,  222,  138,
  174,  175,  176,   27,   83,  184,  181,   84,   85,  139,
  140,  141,   22,
};
final static short yysindex[] = {                      -204,
 -261,    0,    0, -220, -209, -148, -202, -196, -192, -177,
 -168, -209, -138, -159,    0,    0,    0,    0,    0,    0,
    0,    0, -150,    0,    0,    0, -143, -145, -144, -142,
 -141, -265, -163, -157, -151, -209, -140, -139, -239, -197,
 -197, -212, -217, -197,    0,    0,    0, -257,    0,    0,
    0, -209, -137,    0,    0, -191, -191, -126, -136,    0,
    0,    0,    0,    0,    0, -257, -257,    0, -212, -212,
    0, -169,    0, -156, -170, -197, -197,    0, -146, -133,
 -132, -131, -135,    0, -134, -130, -128, -147,    0, -173,
 -212, -212, -127, -125,    0, -124,    0, -123,    0,    0,
    0, -122, -191, -121, -118, -120, -115, -114, -112,    0,
    0, -119, -111, -110, -225, -225,    0, -225, -149, -109,
 -129, -108, -107, -102, -100,    0,    0, -105, -104, -226,
 -225, -103,  -91, -106,    0,    0,    0,    0,  -97,    0,
    0, -101,  -96,  -98,  -95,    0, -118,    0,    0,  -99,
  -94, -212, -250, -260,  -93,  -86,    0,  -92, -225,    0,
 -206, -149,    0,    0,    0,    0, -169,  -90,  -83,  -82,
  -88,    0, -257,  -87,    0,  -84, -225,  -85,    0,    0,
    0,  -80, -222,  -81,    0,  -79,  -74,  -78, -262,    0,
 -260,    0,    0,    0,    0,    0,    0, -225,  -77,    0,
    0,    0,  -76,    0,  -75,    0,  -73,    0,  -64,    0,
  -70,  -72,    0,  -69,    0, -225,    0, -225,  -71,  -67,
    0,    0,    0,    0,
};
final static short yyrindex[] = {                         0,
    0,    0,    0,    0,  -54,    0,    0,    0,    0,    0,
    0,  -66,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,  -61,  -60,
  -57,    0,    0,    0,    0, -253,    0,    0,    0,    0,
    0,    0, -186,    0,    0,    0,    0,  -56,    0,    0,
    0, -253,    0,    0,    0,  -59,  -59,    0,    0,    0,
    0,    0,    0,    0,    0,  -53,  -52,    0,    0,    0,
    0,  -51,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,  -58,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0, -183,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0, -152,    0,    0,  -55,  -55,    0,  -48,    0,    0,
  -50,    0,    0,    0,    0,    0,    0,    0,    0,    0,
  -55,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,  -49,    0,    0,    0,    0,    0,    0,
    0,    0,  -45,  -46,    0,    0,    0,    0, -243,    0,
    0,    0,    0,    0,    0,    0,  -44,    0,    0,    0,
    0,    0, -193,    0,    0,  -43, -243,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,  -41,    0,
    0,    0,    0,    0,    0,    0,    0,  -55,    0,    0,
    0,    0,    0,    0,    0,    0,  -42,    0,  -37,    0,
    0,    0,    0,    0,    0,  -55,    0,  -55,    0,    0,
    0,    0,    0,    0,
};
final static short yygindex[] = {                         0,
    0,   -5,    0,    0, -113,    0,   -1,  -32,    0,  -68,
    0,    0,    0,    0,    0,    0,    0,    3,    0,   -8,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,  -36,    0,    0,  103,    0,    0,   60,    0,    0,
    0,    0,    0,
};
final static int YYTABLESIZE=246;
static short yytable[];
static { yytable();}
static void yytable(){
yytable = new short[]{                         48,
   89,   90,  142,   16,  143,    5,   33,   66,   67,   43,
   16,   75,  201,    4,   43,  169,   95,  155,   76,   77,
   44,   58,  111,  112,  170,   44,   45,   46,   47,  202,
   55,   45,   46,  172,   16,   59,  128,    5,  129,    7,
    8,    9,   10,   96,   97,  180,   78,   95,    6,  130,
   16,   32,    1,   76,   77,    7,    8,    9,   10,  154,
  195,   73,   68,  192,  131,   11,   74,   69,   43,    5,
  132,  133,   28,   70,   80,   81,   82,   43,   29,   44,
   12,   71,   30,  167,  205,   45,   46,  182,   44,    9,
    9,   76,   14,   76,   45,   46,    9,   31,    9,   14,
    9,   14,  219,   14,  220,   76,   77,   91,   92,   32,
   23,   91,   92,  110,   93,   94,   95,   24,   25,   26,
   34,  106,  107,   36,   37,  108,  109,   52,  183,   19,
   19,   38,   39,   40,   19,   41,   42,   53,   87,   54,
   98,   99,  100,  101,  144,   56,   57,   88,   79,  164,
  103,  102,   77,  185,  204,  147,  104,  105,  113,   86,
  114,   91,  117,  161,    0,  122,  115,  116,  118,  119,
  123,  124,  150,  125,  151,  126,  127,  146,  148,  149,
  152,  153,  156,  157,  158,  159,  162,  165,  178,  160,
  179,  187,  166,  163,  189,  188,  186,  177,  212,  190,
  191,  193,  194,  199,    5,    0,  207,    0,    0,  196,
  197,   95,    0,  200,  206,  208,  214,  215,  210,  221,
  217,   57,   59,  223,    5,   61,  107,   85,   86,   58,
   60,   62,    0,    0,    0,   95,   30,   46,    0,   33,
   73,   49,   64,   74,   51,   68,
};
}
static short yycheck[];
static { yycheck(); }
static void yycheck() {
yycheck = new short[] {                         32,
   69,   70,  116,    5,  118,  259,   12,   40,   41,  275,
   12,   44,  275,  275,  275,  266,  260,  131,  276,  277,
  286,  261,   91,   92,  275,  286,  292,  293,  294,  292,
   36,  292,  293,  294,   36,  275,  262,  291,  264,  265,
  266,  267,  268,   76,   77,  159,   52,  291,  258,  275,
   52,  278,  257,  276,  277,  265,  266,  267,  268,  286,
  283,  279,  275,  177,  290,  275,  284,  280,  275,  290,
  296,  297,  275,  286,  266,  267,  268,  275,  275,  286,
  290,  294,  275,  152,  198,  292,  293,  294,  286,  276,
  277,  285,  276,  287,  292,  293,  283,  275,  285,  283,
  287,  285,  216,  287,  218,  276,  277,  281,  282,  278,
  259,  281,  282,  287,  271,  272,  287,  266,  267,  268,
  259,  269,  270,  283,  275,  273,  274,  291,  161,  282,
  283,  275,  278,  278,  287,  278,  278,  295,  265,  291,
  287,  275,  275,  275,  294,  286,  286,  284,  286,  147,
  285,  287,  277,  162,  191,  285,  287,  286,  286,   57,
  286,  281,  103,  260,   -1,  286,  290,  290,  290,  288,
  286,  286,  275,  286,  275,  287,  287,  287,  287,  287,
  286,  286,  286,  275,  291,  283,  285,  287,  275,  291,
  283,  275,  287,  289,  283,  278,  287,  291,  263,  287,
  285,  287,  283,  278,  259,   -1,  283,   -1,   -1,  291,
  290,  260,   -1,  292,  292,  291,  287,  290,  292,  291,
  290,  283,  283,  291,  291,  283,  283,  287,  287,  283,
  283,  283,   -1,   -1,   -1,  291,  287,  283,   -1,  289,
  287,  283,  287,  287,  287,  283,
};
}
final static short YYFINAL=2;
final static short YYMAXTOKEN=297;
final static String yyname[] = {
"end-of-file",null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,
null,null,null,"CLASS","STATIC","VOID","RETURN","NEW","IF","ELSE","FOR","BM",
"INT","DOUBLE","BOOLEAN","TRANSPOSITION","INVERSION","GET_H","GET_L",
"CONJUNCTION","DISJUNCTION","ID","ADD_OP","MUL_OP","ASSIGN_OP","UNAR_OP",
"NOT_OP","AND_OP","OR_OP","SEPARATOR1","SEPARATOR2","SEPARATOR3","SEPARATOR4",
"SEPARATOR5","SEPARATOR6","SEPARATOR7","SEPARATOR8","SEPARATOR9","INT_VAL",
"REAL_VAL","BOOL_VAL","MAIN","PRINTLN","READLN",
};
final static String yyrule[] = {
"$accept : line",
"line : programm",
"programm : CLASS ID SEPARATOR8 class_operators main SEPARATOR9",
"class_operators : class_operator SEPARATOR1 class_operators",
"class_operators : SEPARATOR8 class_operators SEPARATOR9 class_operators",
"class_operators :",
"main : VOID MAIN SEPARATOR4 SEPARATOR5 SEPARATOR8 operators SEPARATOR9 SEPARATOR1",
"class_operator : declare_func",
"class_operator : standart_operator",
"arithmExp : ID",
"arithmExp : INT_VAL",
"arithmExp : REAL_VAL",
"arithmExp : size_operations",
"arithmExp : ID UNAR_OP",
"arithmExp : arithmExp ADD_OP arithmExp",
"arithmExp : arithmExp MUL_OP arithmExp",
"arithmExp : SEPARATOR4 arithmExp SEPARATOR5",
"logicExp : ID",
"logicExp : BOOL_VAL",
"logicExp : logicExp OR_OP logicExp",
"logicExp : logicExp AND_OP logicExp",
"logicExp : SEPARATOR4 logicExp SEPARATOR5",
"logicExp : NOT_OP logicExp",
"matrix_declare : BM ID ASSIGN_OP matrix_operations",
"matrix_operations : declare_BM",
"matrix_operations : transposition",
"matrix_operations : inversion",
"matrix_operations : conjunction",
"matrix_operations : disjunction",
"declare_BM : NEW BM SEPARATOR4 mparams SEPARATOR5",
"mparams : mparam",
"mparams : mparam SEPARATOR3 mparams",
"mparam : SEPARATOR6 mline SEPARATOR7",
"mline : BOOL_VAL",
"mline : BOOL_VAL SEPARATOR3 mline",
"transposition : ID SEPARATOR2 TRANSPOSITION SEPARATOR4 SEPARATOR5",
"inversion : ID SEPARATOR2 INVERSION SEPARATOR4 SEPARATOR5",
"conjunction : ID SEPARATOR2 CONJUNCTION SEPARATOR4 ID SEPARATOR5",
"disjunction : ID SEPARATOR2 DISJUNCTION SEPARATOR4 ID SEPARATOR5",
"size_operations : getH",
"size_operations : getL",
"getH : ID SEPARATOR2 GET_H SEPARATOR4 SEPARATOR5",
"getL : ID SEPARATOR2 GET_L SEPARATOR4 SEPARATOR5",
"for : FOR SEPARATOR4 init_cycle SEPARATOR1 condition SEPARATOR1 increments SEPARATOR5 startcycle operators endcycle",
"init_cycle : INT ID ASSIGN_OP INT_VAL",
"init_cycle : ID ASSIGN_OP INT_VAL",
"init_cycle :",
"condition : INT_VAL",
"condition : ID",
"condition :",
"increments : INT_VAL",
"increments :",
"startcycle : SEPARATOR8",
"endcycle : SEPARATOR9",
"initialization : init_int",
"initialization : init_double",
"initialization : init_boolean",
"init_int : INT ID",
"init_int : INT ID ASSIGN_OP arithmExp",
"init_double : DOUBLE ID",
"init_double : DOUBLE ID ASSIGN_OP arithmExp",
"init_boolean : BOOLEAN ID",
"init_boolean : BOOLEAN ID ASSIGN_OP logicExp",
"if : IF SEPARATOR4 ifcond SEPARATOR5 startif operators endif elseORnull",
"ifcond : logicExp",
"startif : SEPARATOR8",
"endif : SEPARATOR9",
"elseORnull : ELSE startelse operators endelse",
"elseORnull :",
"startelse : SEPARATOR8",
"endelse : SEPARATOR9",
"call_func : ID SEPARATOR4 paramsORnull SEPARATOR5",
"paramsORnull : params",
"paramsORnull :",
"params : param",
"params : param SEPARATOR3 params",
"param : arithmExp",
"param : BOOL_VAL",
"declare_func : STATIC types ID SEPARATOR4 fparamsORnull SEPARATOR5 SEPARATOR8 operators RETURN return_res SEPARATOR9",
"$$1 :",
"declare_func : STATIC VOID ID SEPARATOR4 fparamsORnull SEPARATOR5 SEPARATOR8 operators SEPARATOR9 $$1",
"types : INT",
"types : DOUBLE",
"types : BOOLEAN",
"fparamsORnull : fparams",
"fparamsORnull :",
"fparams : fparam",
"fparams : fparam SEPARATOR3 fparams",
"fparam : INT ID",
"fparam : DOUBLE ID",
"fparam : BOOLEAN ID",
"return_res : arithmExp SEPARATOR1",
"return_res : BOOL_VAL SEPARATOR1",
"operators : body_operator SEPARATOR1 operators",
"operators : SEPARATOR8 operators SEPARATOR9 operators",
"operators :",
"body_operator : standart_operator",
"body_operator : for",
"body_operator : if",
"body_operator : call_func",
"body_operator : println",
"body_operator : readln",
"standart_operator : initialization",
"standart_operator : matrix_declare",
"standart_operator : eq_operator",
"println : PRINTLN SEPARATOR4 ID SEPARATOR5",
"readln : READLN ID",
"eq_operator : ID ASSIGN_OP arithmExp",
"eq_operator : ID ASSIGN_OP BOOL_VAL",
};

//#line 386 "kurs.y"

void yyerror(String s){
	System.out.println("par:"+s);
}


//TODO bac1ca
public static String CLASS_NAME = "Test";
public static int m_currIdx = 0;

static Analyzer scanner;

int yylex()
{
	int token = 0;
	try{
		Yytoken currToken = scanner.yylex();
		yylval = new ParserVal(currToken.tokValue); //SEE BELOW
		token = currToken.tokClass;
	} catch (Exception e) {
		//e.printStackTrace();
	}
	System.err.println("token = " + token);
	return token;
}

void dotest(){
	yyparse();
}

public static void main(String args[]){
	if (args.length > 0){
		String bavaFile = args[0];
		scanner = CommonUtil.getScanner(bavaFile);
		Parser par = new Parser(false);
		par.dotest();
		GTable.printResltsToFile("result.tokens");
		GTable.triandsToFile("result.tetrads");

		try {
			new ByteCodeGenerator(CLASS_NAME, GTable.getTetrads()).genByteCode();
		} catch (Exception e) {
			e.printStackTrace();
		}
	} else {
		System.out.println("Please enter path to file!");
	}	
}
//#line 492 "Parser.java"
//###############################################################
// method: yylexdebug : check lexer state
//###############################################################
void yylexdebug(int state,int ch)
{
String s=null;
  if (ch < 0) ch=0;
  if (ch <= YYMAXTOKEN) //check index bounds
     s = yyname[ch];    //now get it
  if (s==null)
    s = "illegal-symbol";
  debug("state "+state+", reading "+ch+" ("+s+")");
}





//The following are now global, to aid in error reporting
int yyn;       //next next thing to do
int yym;       //
int yystate;   //current parsing state from state table
String yys;    //current token string


//###############################################################
// method: yyparse : parse input and execute indicated items
//###############################################################
int yyparse()
{
boolean doaction;
  init_stacks();
  yynerrs = 0;
  yyerrflag = 0;
  yychar = -1;          //impossible char forces a read
  yystate=0;            //initial state
  state_push(yystate);  //save it
  val_push(yylval);     //save empty value
  while (true) //until parsing is done, either correctly, or w/error
    {
    doaction=true;
    if (yydebug) debug("loop"); 
    //#### NEXT ACTION (from reduction table)
    for (yyn=yydefred[yystate];yyn==0;yyn=yydefred[yystate])
      {
      if (yydebug) debug("yyn:"+yyn+"  state:"+yystate+"  yychar:"+yychar);
      if (yychar < 0)      //we want a char?
        {
        yychar = yylex();  //get next token
        if (yydebug) debug(" next yychar:"+yychar);
        //#### ERROR CHECK ####
        if (yychar < 0)    //it it didn't work/error
          {
          yychar = 0;      //change it to default string (no -1!)
          if (yydebug)
            yylexdebug(yystate,yychar);
          }
        }//yychar<0
      yyn = yysindex[yystate];  //get amount to shift by (shift index)
      if ((yyn != 0) && (yyn += yychar) >= 0 &&
          yyn <= YYTABLESIZE && yycheck[yyn] == yychar)
        {
        if (yydebug)
          debug("state "+yystate+", shifting to state "+yytable[yyn]);
        //#### NEXT STATE ####
        yystate = yytable[yyn];//we are in a new state
        state_push(yystate);   //save it
        val_push(yylval);      //push our lval as the input for next rule
        yychar = -1;           //since we have 'eaten' a token, say we need another
        if (yyerrflag > 0)     //have we recovered an error?
           --yyerrflag;        //give ourselves credit
        doaction=false;        //but don't process yet
        break;   //quit the yyn=0 loop
        }

    yyn = yyrindex[yystate];  //reduce
    if ((yyn !=0 ) && (yyn += yychar) >= 0 &&
            yyn <= YYTABLESIZE && yycheck[yyn] == yychar)
      {   //we reduced!
      if (yydebug) debug("reduce");
      yyn = yytable[yyn];
      doaction=true; //get ready to execute
      break;         //drop down to actions
      }
    else //ERROR RECOVERY
      {
      if (yyerrflag==0)
        {
        yyerror("syntax error");
        yynerrs++;
        }
      if (yyerrflag < 3) //low error count?
        {
        yyerrflag = 3;
        while (true)   //do until break
          {
          if (stateptr<0)   //check for under & overflow here
            {
            yyerror("stack underflow. aborting...");  //note lower case 's'
            return 1;
            }
          yyn = yysindex[state_peek(0)];
          if ((yyn != 0) && (yyn += YYERRCODE) >= 0 &&
                    yyn <= YYTABLESIZE && yycheck[yyn] == YYERRCODE)
            {
            if (yydebug)
              debug("state "+state_peek(0)+", error recovery shifting to state "+yytable[yyn]+" ");
            yystate = yytable[yyn];
            state_push(yystate);
            val_push(yylval);
            doaction=false;
            break;
            }
          else
            {
            if (yydebug)
              debug("error recovery discarding state "+state_peek(0)+" ");
            if (stateptr<0)   //check for under & overflow here
              {
              yyerror("Stack underflow. aborting...");  //capital 'S'
              return 1;
              }
            state_pop();
            val_pop();
            }
          }
        }
      else            //discard this token
        {
        if (yychar == 0)
          return 1; //yyabort
        if (yydebug)
          {
          yys = null;
          if (yychar <= YYMAXTOKEN) yys = yyname[yychar];
          if (yys == null) yys = "illegal-symbol";
          debug("state "+yystate+", error recovery discards token "+yychar+" ("+yys+")");
          }
        yychar = -1;  //read another
        }
      }//end error recovery
    }//yyn=0 loop
    if (!doaction)   //any reason not to proceed?
      continue;      //skip action
    yym = yylen[yyn];          //get count of terminals on rhs
    if (yydebug)
      debug("state "+yystate+", reducing "+yym+" by rule "+yyn+" ("+yyrule[yyn]+")");
    if (yym>0)                 //if count of rhs not 'nil'
      yyval = val_peek(yym-1); //get current semantic value
    yyval = dup_yyval(yyval); //duplicate yyval if ParserVal is used as semantic value
    switch(yyn)
      {
//########## USER-SUPPLIED ACTIONS ##########
case 1:
//#line 69 "kurs.y"
{ System.err.println("done!!!"); }
break;
case 2:
//#line 73 "kurs.y"
{
		CLASS_NAME = val_peek(4).sval;
	}
break;
case 3:
//#line 77 "kurs.y"
{}
break;
case 4:
//#line 78 "kurs.y"
{}
break;
case 5:
//#line 79 "kurs.y"
{}
break;
case 6:
//#line 82 "kurs.y"
{
		GTable.addTetrad(new Tetrad(R.DEFF, val_peek(7), val_peek(6), Tetrad.EMPTY));
	}
break;
case 7:
//#line 86 "kurs.y"
{}
break;
case 8:
//#line 87 "kurs.y"
{}
break;
case 9:
//#line 92 "kurs.y"
{ yyval = val_peek(0); }
break;
case 10:
//#line 93 "kurs.y"
{ yyval = val_peek(0); }
break;
case 11:
//#line 94 "kurs.y"
{ yyval = val_peek(0); }
break;
case 12:
//#line 95 "kurs.y"
{}
break;
case 13:
//#line 96 "kurs.y"
{
 		GTable.addTetrad(new Tetrad("+", val_peek(1), "1", m_currIdx));
 	 	yyval = new ParserVal("t" + m_currIdx);
 	 	m_currIdx++; }
break;
case 14:
//#line 100 "kurs.y"
{	
 		GTable.addTetrad(new Tetrad(val_peek(1), val_peek(2), val_peek(0), m_currIdx));
 	 	yyval = new ParserVal("t" + m_currIdx);
 	 	m_currIdx++; }
break;
case 15:
//#line 104 "kurs.y"
{
 		GTable.addTetrad(new Tetrad(val_peek(1), val_peek(2), val_peek(0), m_currIdx));
 		yyval = new ParserVal("t" + m_currIdx); 
 		m_currIdx++; }
break;
case 16:
//#line 108 "kurs.y"
{ yyval = val_peek(1); }
break;
case 17:
//#line 113 "kurs.y"
{ yyval = val_peek(0); }
break;
case 18:
//#line 114 "kurs.y"
{ yyval = val_peek(0); }
break;
case 19:
//#line 116 "kurs.y"
{	
 		GTable.addTetrad(new Tetrad(val_peek(1), val_peek(2), val_peek(0), m_currIdx));
 	 	yyval = new ParserVal("t" + m_currIdx);
 	 	m_currIdx++; 
	}
break;
case 20:
//#line 122 "kurs.y"
{	
 		GTable.addTetrad(new Tetrad(val_peek(1), val_peek(2), val_peek(0), m_currIdx));
 	 	yyval = new ParserVal("t" + m_currIdx);
 	 	m_currIdx++; 
	}
break;
case 21:
//#line 127 "kurs.y"
{ yyval = val_peek(1); }
break;
case 22:
//#line 128 "kurs.y"
{ yyval = new ParserVal("!" + val_peek(0).sval); }
break;
case 23:
//#line 133 "kurs.y"
{ System.err.println(" matrix "); }
break;
case 24:
//#line 135 "kurs.y"
{}
break;
case 25:
//#line 136 "kurs.y"
{}
break;
case 26:
//#line 137 "kurs.y"
{}
break;
case 27:
//#line 138 "kurs.y"
{}
break;
case 28:
//#line 139 "kurs.y"
{}
break;
case 29:
//#line 141 "kurs.y"
{}
break;
case 30:
//#line 143 "kurs.y"
{}
break;
case 31:
//#line 144 "kurs.y"
{}
break;
case 32:
//#line 146 "kurs.y"
{}
break;
case 33:
//#line 148 "kurs.y"
{}
break;
case 34:
//#line 149 "kurs.y"
{}
break;
case 35:
//#line 151 "kurs.y"
{}
break;
case 36:
//#line 153 "kurs.y"
{}
break;
case 37:
//#line 155 "kurs.y"
{}
break;
case 38:
//#line 157 "kurs.y"
{}
break;
case 39:
//#line 159 "kurs.y"
{}
break;
case 40:
//#line 160 "kurs.y"
{}
break;
case 41:
//#line 163 "kurs.y"
{
		GTable.addTetrad(new Tetrad("CALL", val_peek(2), val_peek(4), m_currIdx));
		yyval = new ParserVal("t" + m_currIdx);
		m_currIdx++;
	}
break;
case 42:
//#line 170 "kurs.y"
{
		GTable.addTetrad(new Tetrad("CALL", val_peek(2), val_peek(4), m_currIdx)); /* TODO*/
		yyval = new ParserVal("t" + m_currIdx);
		m_currIdx++;
	}
break;
case 43:
//#line 178 "kurs.y"
{}
break;
case 44:
//#line 181 "kurs.y"
{
		GTable.addTetrad(new Tetrad(R.INIT_CYC, val_peek(3), val_peek(2), Tetrad.EMPTY));
		GTable.addTetrad(new Tetrad(R.INIT_CYC, val_peek(1), val_peek(0), Tetrad.EMPTY));		
	}
break;
case 45:
//#line 186 "kurs.y"
{
 		ParserVal val = null;
		GTable.addTetrad(new Tetrad(R.INIT_CYC, val, val_peek(2), Tetrad.EMPTY));
		GTable.addTetrad(new Tetrad(R.INIT_CYC, val_peek(1), val_peek(0), Tetrad.EMPTY));		
 	}
break;
case 46:
//#line 191 "kurs.y"
{}
break;
case 47:
//#line 194 "kurs.y"
{
		GTable.addTetrad(new Tetrad(R.COND_CYC, val_peek(0)));
	}
break;
case 48:
//#line 198 "kurs.y"
{
		GTable.addTetrad(new Tetrad(R.COND_CYC, val_peek(0)));
 	}
break;
case 49:
//#line 201 "kurs.y"
{}
break;
case 50:
//#line 204 "kurs.y"
{
		GTable.addTetrad(new Tetrad(R.ITER_CYC, val_peek(0)));
	}
break;
case 51:
//#line 207 "kurs.y"
{}
break;
case 52:
//#line 209 "kurs.y"
{ GTable.addTetrad(new Tetrad(R.SCYC)); }
break;
case 53:
//#line 211 "kurs.y"
{ GTable.addTetrad(new Tetrad(R.ECYC)); }
break;
case 54:
//#line 217 "kurs.y"
{}
break;
case 55:
//#line 218 "kurs.y"
{}
break;
case 56:
//#line 219 "kurs.y"
{}
break;
case 57:
//#line 222 "kurs.y"
{
		GTable.addTetrad(new Tetrad(val_peek(1), val_peek(0), null, Tetrad.EMPTY));
	}
break;
case 58:
//#line 226 "kurs.y"
{
		GTable.addTetrad(new Tetrad(val_peek(3), val_peek(2), null, Tetrad.EMPTY));
		GTable.addTetrad(new Tetrad(val_peek(1), val_peek(2), val_peek(0), Tetrad.EMPTY));
	}
break;
case 59:
//#line 232 "kurs.y"
{
		GTable.addTetrad(new Tetrad(val_peek(1), val_peek(0), null, Tetrad.EMPTY));
	}
break;
case 60:
//#line 236 "kurs.y"
{
		GTable.addTetrad(new Tetrad(val_peek(3), val_peek(2), null, Tetrad.EMPTY));
		GTable.addTetrad(new Tetrad(val_peek(1), val_peek(2), val_peek(0), Tetrad.EMPTY));
	}
break;
case 61:
//#line 242 "kurs.y"
{
		GTable.addTetrad(new Tetrad(val_peek(1), val_peek(0), null, Tetrad.EMPTY));
	}
break;
case 62:
//#line 246 "kurs.y"
{
		GTable.addTetrad(new Tetrad(val_peek(3), val_peek(2), null, Tetrad.EMPTY));
		GTable.addTetrad(new Tetrad(val_peek(1), val_peek(2), val_peek(0), Tetrad.EMPTY));
	}
break;
case 63:
//#line 254 "kurs.y"
{}
break;
case 64:
//#line 256 "kurs.y"
{ GTable.addTetrad(new Tetrad(R.CIF, val_peek(0)));}
break;
case 65:
//#line 258 "kurs.y"
{ GTable.addTetrad(new Tetrad(R.SIF)); }
break;
case 66:
//#line 260 "kurs.y"
{ GTable.addTetrad(new Tetrad(R.EIF)); }
break;
case 67:
//#line 262 "kurs.y"
{}
break;
case 68:
//#line 263 "kurs.y"
{}
break;
case 69:
//#line 265 "kurs.y"
{ GTable.addTetrad(new Tetrad(R.SEL)); }
break;
case 70:
//#line 267 "kurs.y"
{ GTable.addTetrad(new Tetrad(R.EEL)); }
break;
case 71:
//#line 273 "kurs.y"
{
		String op2 = null;
		GTable.addTetrad(new Tetrad(R.CALL, val_peek(3), op2, Tetrad.EMPTY));	
	}
break;
case 72:
//#line 278 "kurs.y"
{}
break;
case 73:
//#line 279 "kurs.y"
{}
break;
case 74:
//#line 281 "kurs.y"
{}
break;
case 75:
//#line 282 "kurs.y"
{}
break;
case 76:
//#line 285 "kurs.y"
{
		String op2 = null;
		GTable.addTetrad(new Tetrad(R.CALLPARAM, val_peek(0), op2, Tetrad.EMPTY));	
	}
break;
case 77:
//#line 290 "kurs.y"
{
 		String op2 = null;
 		GTable.addTetrad(new Tetrad(R.CALLPARAM, val_peek(0), op2, Tetrad.EMPTY));	
	}
break;
case 78:
//#line 300 "kurs.y"
{
		GTable.addTetrad(new Tetrad(R.DEFF, val_peek(9), val_peek(8), Tetrad.EMPTY));
	}
break;
case 79:
//#line 303 "kurs.y"
{}
break;
case 80:
//#line 304 "kurs.y"
{
		GTable.addTetrad(new Tetrad(R.DEFF, val_peek(8), val_peek(7), Tetrad.EMPTY));
	}
break;
case 81:
//#line 308 "kurs.y"
{}
break;
case 82:
//#line 309 "kurs.y"
{}
break;
case 83:
//#line 310 "kurs.y"
{}
break;
case 84:
//#line 312 "kurs.y"
{}
break;
case 85:
//#line 313 "kurs.y"
{}
break;
case 86:
//#line 315 "kurs.y"
{}
break;
case 87:
//#line 316 "kurs.y"
{}
break;
case 88:
//#line 319 "kurs.y"
{
		GTable.addTetrad(new Tetrad(R.PARAM, val_peek(1), val_peek(0), Tetrad.EMPTY));
	}
break;
case 89:
//#line 323 "kurs.y"
{
		GTable.addTetrad(new Tetrad(R.PARAM, val_peek(1), val_peek(0), Tetrad.EMPTY));
	}
break;
case 90:
//#line 327 "kurs.y"
{
		GTable.addTetrad(new Tetrad(R.PARAM, val_peek(1), val_peek(0), Tetrad.EMPTY));
	}
break;
case 91:
//#line 332 "kurs.y"
{
		ParserVal val = null;
		GTable.addTetrad(new Tetrad(R.RET, val_peek(1), val, Tetrad.EMPTY));
	}
break;
case 92:
//#line 337 "kurs.y"
{
 		ParserVal val = null;
		GTable.addTetrad(new Tetrad(R.RET, val_peek(1), val, Tetrad.EMPTY));
 	}
break;
case 93:
//#line 346 "kurs.y"
{}
break;
case 94:
//#line 347 "kurs.y"
{}
break;
case 95:
//#line 348 "kurs.y"
{}
break;
case 96:
//#line 350 "kurs.y"
{}
break;
case 97:
//#line 351 "kurs.y"
{}
break;
case 98:
//#line 352 "kurs.y"
{}
break;
case 99:
//#line 353 "kurs.y"
{ System.err.println("call_func"); }
break;
case 100:
//#line 354 "kurs.y"
{}
break;
case 101:
//#line 355 "kurs.y"
{}
break;
case 102:
//#line 357 "kurs.y"
{}
break;
case 103:
//#line 358 "kurs.y"
{}
break;
case 104:
//#line 359 "kurs.y"
{}
break;
case 105:
//#line 365 "kurs.y"
{
		GTable.addTetrad(new Tetrad(val_peek(3), val_peek(1), null, Tetrad.EMPTY));
	}
break;
case 106:
//#line 369 "kurs.y"
{}
break;
case 107:
//#line 377 "kurs.y"
{
		GTable.addTetrad(new Tetrad(val_peek(1), val_peek(2), val_peek(0), Tetrad.EMPTY));
		/*$$ = $3; //TODO*/
	}
break;
case 108:
//#line 382 "kurs.y"
{ GTable.addTetrad(new Tetrad(val_peek(1), val_peek(2), val_peek(0), Tetrad.EMPTY)); }
break;
//#line 1160 "Parser.java"
//########## END OF USER-SUPPLIED ACTIONS ##########
    }//switch
    //#### Now let's reduce... ####
    if (yydebug) debug("reduce");
    state_drop(yym);             //we just reduced yylen states
    yystate = state_peek(0);     //get new state
    val_drop(yym);               //corresponding value drop
    yym = yylhs[yyn];            //select next TERMINAL(on lhs)
    if (yystate == 0 && yym == 0)//done? 'rest' state and at first TERMINAL
      {
      if (yydebug) debug("After reduction, shifting from state 0 to state "+YYFINAL+"");
      yystate = YYFINAL;         //explicitly say we're done
      state_push(YYFINAL);       //and save it
      val_push(yyval);           //also save the semantic value of parsing
      if (yychar < 0)            //we want another character?
        {
        yychar = yylex();        //get next character
        if (yychar<0) yychar=0;  //clean, if necessary
        if (yydebug)
          yylexdebug(yystate,yychar);
        }
      if (yychar == 0)          //Good exit (if lex returns 0 ;-)
         break;                 //quit the loop--all DONE
      }//if yystate
    else                        //else not done yet
      {                         //get next state and push, for next yydefred[]
      yyn = yygindex[yym];      //find out where to go
      if ((yyn != 0) && (yyn += yystate) >= 0 &&
            yyn <= YYTABLESIZE && yycheck[yyn] == yystate)
        yystate = yytable[yyn]; //get new state
      else
        yystate = yydgoto[yym]; //else go to new defred
      if (yydebug) debug("after reduction, shifting from state "+state_peek(0)+" to state "+yystate+"");
      state_push(yystate);     //going again, so push state & val...
      val_push(yyval);         //for next action
      }
    }//main loop
  return 0;//yyaccept!!
}
//## end of method parse() ######################################



//## run() --- for Thread #######################################
/**
 * A default run method, used for operating this parser
 * object in the background.  It is intended for extending Thread
 * or implementing Runnable.  Turn off with -Jnorun .
 */
public void run()
{
  yyparse();
}
//## end of method run() ########################################



//## Constructors ###############################################
/**
 * Default constructor.  Turn off with -Jnoconstruct .

 */
public Parser()
{
  //nothing to do
}


/**
 * Create a parser, setting the debug to true or false.
 * @param debugMe true for debugging, false for no debug.
 */
public Parser(boolean debugMe)
{
  yydebug=debugMe;
}
//###############################################################



}
//################### END OF CLASS ##############################

