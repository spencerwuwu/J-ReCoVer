// https://searchcode.com/api/result/132828665/

package tools.javac.lambda;

import org.testng.annotations.Factory;
import tools.javac.combo.ComboTestBase;
import tools.javac.combo.DimensionVar;
import tools.javac.combo.SourceFile;
import tools.javac.combo.Template;

import static tools.javac.combo.StackProcessingUtils.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * NestedGenericMethodCallTest
 */
public class NestedGenericMethodCallTest extends ComboTestBase<NestedGenericMethodCallTest> {
    @Factory
    public static Object[] testNestedGenericMethodCallType() throws Exception {
        return factory(NestedGenericMethodCallTest.class);
    }

    @DimensionVar("ID_METHOD") IdMethod idMethod;
    @DimensionVar("NIL_METHOD") NilMethod nilMethod;
    @DimensionVar("CONS_METHOD") ConsMethod consMethod;
    @DimensionVar("SHAPE") Shape shape;
    @DimensionVar("TARGET") Type target;

    @SourceFile("Main.java")
    String clientFile = "import java.util.*;\n"
            +"public class Main {\n"
            +"  #{ID_METHOD}\n"
            +"  #{NIL_METHOD}\n"
            +"  #{CONS_METHOD}\n"
            +"  void test() { \n"
            + "   #{TARGET} t = #{SHAPE};\n"
            + " }\n"
            +"}";

    @Override
    protected void postCompile(String grp) {
        Type res = shape.compile(idMethod, nilMethod, consMethod);
        if (res.asSubtypeOf(target).isError()) {
            assertCompileFailed("Bad result = " + res);
        } else {
            assertCompileSucceeded();
        }
    }

    @Override
    protected boolean shouldSkip() {
        return target.free || target == Type.ERROR;
    }

    enum Type implements Template {
        Z("Z", true),
        OBJECT("Object", false),
        STRING("String", false),
        LIST_Z("List<Z>", true),
        LIST_OBJECT("List<Object>", false),
        LIST_STRING("List<String>", false),
        ERROR("<any>", false);

        String typeStr;
        boolean free;

        Type(String typeStr, boolean free) {
            this.typeStr = typeStr;
            this.free = free;
        }

        boolean isError() {
            return this == ERROR;
        }

        Type asSubtypeOf(Type that) {
            return asSubtypeMap[this.ordinal()][that.ordinal()];
        }
 
        boolean isSubtypeOf(Type that) {
            return !asSubtypeOf(that).isError();
        }

        public String expand(String selector) {
            return typeStr;
        }

        static Type[][] asSubtypeMap = new Type[][] {
                //                   Z           OBJECT      STRING       LIST_Z      LIST_OBJECT      LIST_STRING        ERROR
                { /* Z           */  OBJECT     ,OBJECT     ,STRING      ,ERROR      ,LIST_OBJECT     ,LIST_STRING       ,ERROR        },
                { /* OBJECT      */  OBJECT     ,OBJECT     ,ERROR       ,ERROR      ,ERROR           ,ERROR             ,ERROR        },
                { /* STRING      */  STRING     ,STRING     ,STRING      ,ERROR      ,ERROR           ,ERROR             ,ERROR        },
                { /* LIST_Z      */  LIST_OBJECT,LIST_OBJECT,ERROR       ,LIST_OBJECT,LIST_OBJECT     ,LIST_STRING       ,ERROR        },
                { /* LIST_OBJECT */  LIST_OBJECT,LIST_OBJECT,ERROR       ,LIST_OBJECT,LIST_OBJECT     ,ERROR             ,ERROR        },
                { /* LIST_STRING */  LIST_STRING,LIST_STRING,ERROR       ,LIST_STRING,ERROR           ,LIST_STRING       ,ERROR        },
                { /* ERROR       */  ERROR      ,ERROR      ,ERROR       ,ERROR      ,ERROR           ,ERROR             ,ERROR        }};

        static boolean isSubtypes(Type[] ts1, Type[] ts2) {
            assertEquals(ts1.length, ts2.length);
            for (int i = 0; i < ts1.length ; i++) {
                if (!ts1[i].isSubtypeOf(ts2[i])) return false;
            }
            return true;
        }

        static Type listOf(Type t) {
            switch (t) {
                case OBJECT: return LIST_OBJECT;
                case STRING: return LIST_STRING;
                case Z: return LIST_Z;              
                default: return ERROR;
            }
        }

        static Type elemtype(Type t) {
            switch (t) {
                case LIST_OBJECT: return OBJECT;
                case LIST_STRING: return STRING;
                case LIST_Z: return Z;
                default: return ERROR;
            }
        }   

        static Type lub(Type t1, Type t2) {
            if (!t1.asSubtypeOf(t2).isError()) {
                return t2;
            } else if (!t2.asSubtypeOf(t1).isError()) {
                return t1;
            } else {
                return Type.ERROR;
            }
        }
    }

    interface MethodSig {
        Type apply(Type... argTypes);
    }

    enum IdMethod implements Template, MethodSig {
        NON_GENERIC_1("String id(String s) { return null; };", Type.STRING),
        NON_GENERIC_2("List<String> id(List<String> s) { return null; };", Type.LIST_STRING),
        GENERIC_1("<Z> Z id(Z z) { return null; };", Type.Z),
        GENERIC_2("<Z> List<Z> id(List<Z> z) { return null; };", Type.LIST_Z);

        String sig;
        Type formal;

        IdMethod(String sig, Type formal) {
            this.sig = sig;
            this.formal = formal;
        }

        public String expand(String selector) {
            return sig;
        }

        public Type apply(Type... argTypes) {
            return argTypes[0].asSubtypeOf(formal);
        }
    }

    enum NilMethod implements Template, MethodSig {
        NON_GENERIC("List<String> nil() { return null; }", Type.LIST_STRING),
        GENERIC("<Z> List<Z> nil() { return null; }", Type.LIST_Z);

        String sig;
        Type returnType;

        NilMethod(String sig, Type returnType) {
            this.sig = sig;
            this.returnType = returnType;
        }

        public String expand(String selector) {
            return sig;
        }

        public Type apply(Type... argTypes) {            
            return returnType;
        }
    }

    enum ConsMethod implements Template, MethodSig {
        NON_GENERIC("List<String> cons(String s, List<String> ls) { return null; }", Type.STRING, Type.LIST_STRING) {
            Type getReturnType(Type... argTypes) { return Type.LIST_STRING; }
        },
        GENERIC_1("<Z> List<Z> cons(Z z, List<String> ls) { return null; }", Type.Z, Type.LIST_STRING) {
            Type getReturnType(Type... argTypes) { return Type.listOf(argTypes[0].asSubtypeOf(Type.Z)); }
        },
        GENERIC_2("<Z> List<Z> cons(String s, List<Z> lz) { return null; }", Type.STRING, Type.LIST_Z) {
            Type getReturnType(Type... argTypes) { return Type.listOf(Type.elemtype(argTypes[1].asSubtypeOf(Type.LIST_Z))); }
        },
        GENERIC_3("<Z> List<Z> cons(Z z, List<Z> lz) { return null; }", Type.Z, Type.LIST_Z) {
            Type getReturnType(Type... argTypes) {
                Type lub = Type.lub(argTypes[0], Type.elemtype(argTypes[1].asSubtypeOf(Type.LIST_Z)));
                return lub.isError() ? lub : Type.listOf(lub);
            }
        },
        GENERIC_4("<U, V> List<U> cons(U z, List<V> lz) { return null; }", Type.Z, Type.LIST_Z) {
            Type getReturnType(Type... argTypes) { return Type.listOf(argTypes[0].asSubtypeOf(Type.Z)); }
        };

        String sig;
        Type[] formals;

        ConsMethod(String sig, Type... formals) {
            this.sig = sig;
            this.formals = formals;
        }

        public String expand(String selector) {
            return sig;
        }

        public Type apply(Type... argTypes) {
            return Type.isSubtypes(argTypes, formals) ?
                    getReturnType(argTypes) : Type.ERROR;
        }

        abstract Type getReturnType(Type... argTypes);
    }

    enum Token implements StackItem<Token> {
        NIL(0, "nil"),
        DIAMOND(0, "new ArrayList<>"),
        STRING(0, "\"\""),
        ID(1, "id"),
        CONS(2, "cons");

        int arity;
        String tokenStr;

        Token(int arity, String tokenStr) {
            this.arity = arity;
            this.tokenStr = tokenStr;
        }

        public int arity() { return arity; }
    }

    enum Shape implements Template {
        NIL(Token.NIL),
        ID_NIL(Token.NIL, Token.ID),
        ID_ID_NIL(Token.NIL, Token.ID, Token.ID),
        CONS_STRING_NIL(Token.STRING, Token.NIL, Token.CONS),
        CONS_STRING_ID_NIL(Token.STRING, Token.NIL, Token.ID, Token.CONS),
        CONS_ID_STRING_NIL(Token.STRING, Token.ID, Token.NIL, Token.CONS),
        CONS_ID_STRING_ID_NIL(Token.STRING, Token.ID, Token.NIL, Token.ID, Token.CONS),
        CONS_ID_STRING_CONS_STRING_NIL(Token.STRING, Token.ID, Token.STRING, Token.NIL, Token.CONS, Token.CONS),
        CONS_ID_STRING_CONS_ID_STRING_NIL(Token.STRING, Token.ID, Token.STRING, Token.ID, Token.NIL, Token.CONS, Token.CONS),
        CONS_ID_STRING_CONS_STRING_ID_NIL(Token.STRING, Token.ID, Token.STRING, Token.NIL, Token.ID, Token.CONS, Token.CONS),
        CONS_ID_STRING_CONS_ID_STRING_ID_NIL(Token.STRING, Token.ID, Token.STRING, Token.ID, Token.NIL, Token.ID, Token.CONS, Token.CONS),
        CONS_ID_STRING_ID_CONS_ID_STRING_ID_NIL(Token.STRING, Token.ID, Token.STRING, Token.ID, Token.NIL, Token.ID, Token.CONS, Token.ID, Token.CONS),
        ID_CONS_ID_STRING_ID_CONS_ID_STRING_ID_NIL(Token.STRING, Token.ID, Token.STRING, Token.ID, Token.NIL, Token.ID, Token.CONS, Token.ID, Token.CONS, Token.ID),
        DIAMOND(Token.DIAMOND),
        ID_DIAMOND(Token.DIAMOND, Token.ID),
        CONS_STRING_DIAMOND(Token.STRING, Token.DIAMOND, Token.CONS),
        CONS_STRING_ID_DIAMOND(Token.STRING, Token.DIAMOND, Token.ID, Token.CONS),
        CONS_ID_STRING_DIAMOND(Token.STRING, Token.ID, Token.DIAMOND, Token.CONS),
        CONS_ID_STRING_ID_DIAMOND(Token.STRING, Token.ID, Token.DIAMOND, Token.ID, Token.CONS),
        CONS_ID_STRING_CONS_STRING_DIAMOND(Token.STRING, Token.ID, Token.STRING, Token.DIAMOND, Token.CONS, Token.CONS),
        CONS_ID_STRING_CONS_ID_STRING_DIAMOND(Token.STRING, Token.ID, Token.STRING, Token.ID, Token.DIAMOND, Token.CONS, Token.CONS),
        CONS_ID_STRING_CONS_STRING_ID_DIAMOND(Token.STRING, Token.ID, Token.STRING, Token.DIAMOND, Token.ID, Token.CONS, Token.CONS),
        CONS_ID_STRING_CONS_ID_STRING_ID_DIAMOND(Token.STRING, Token.ID, Token.STRING, Token.ID, Token.DIAMOND, Token.ID, Token.CONS, Token.CONS),
        CONS_ID_STRING_ID_CONS_ID_STRING_ID_DIAMOND(Token.STRING, Token.ID, Token.STRING, Token.ID, Token.DIAMOND, Token.ID, Token.CONS, Token.ID, Token.CONS),
        ID_CONS_ID_STRING_ID_CONS_ID_STRING_ID_DIAMOND(Token.STRING, Token.ID, Token.STRING, Token.ID, Token.DIAMOND, Token.ID, Token.CONS, Token.ID, Token.CONS, Token.ID);

        Token[] tokens;

        Shape(Token... tokens) {
            this.tokens = tokens;
        }

        Type compile(final IdMethod idMethod, final NilMethod nilMethod, final ConsMethod consMethod) {
            return process(tokens, new StackReducer<Token, Type>() {
                public Type reduce(Token t, Type[] operands) {
                    switch (t) {
                        case DIAMOND: return Type.LIST_Z;
                        case NIL: return nilMethod.apply(operands);
                        case STRING: return Type.STRING;
                        case ID: return idMethod.apply(operands);
                        case CONS: return consMethod.apply(operands);
                        default: throw new AssertionError();
                    }
                }
                public Class<Type> typeToken() { return Type.class; }
            });
        }

        public String expand(String selector) {
            return process(tokens, new StackReducer<Token, String>() {
                public String reduce(Token t, String[] operands) {
                    switch (t) {
                        case DIAMOND:
                        case NIL:
                        case ID:
                        case CONS:
                            StringBuilder buf = new StringBuilder();
                            String sep = "";
                            for (int i = 0; i < t.arity; i ++) {
                                buf.append(sep);
                                buf.append(operands[i]);
                                sep = ",";
                            }
                            buf.insert(0, "(");
                            buf.insert(0, t.tokenStr);
                            buf.append(")");
                            return buf.toString();
                        case STRING:
                            return t.tokenStr;
                        default: throw new AssertionError();
                    }
                }
                public Class<String> typeToken() { return String.class; }
            });
        }        
/*
        <T> T process(StackReducer<T> reducer) {
            int tos = 0;
            @SuppressWarnings("unchecked")
            T[] stack = (T[])java.lang.reflect.Array.newInstance(reducer.typeToken, tokens.length);
            for (Token t : tokens) {
                @SuppressWarnings("unchecked")
                T[] operands = (T[])java.lang.reflect.Array.newInstance(reducer.typeToken, t.arity);
                System.arraycopy(stack, tos - t.arity, operands, 0, t.arity);
                stack[tos - t.arity] = reducer.reduce(t, operands);
                tos = (tos - t.arity) + 1;
            }
            return stack[tos - 1];
        }*/
    }
}


