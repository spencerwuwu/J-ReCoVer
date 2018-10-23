// https://searchcode.com/api/result/132828649/

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
 * LambdaExpressionTypeInferenceTest
 */
public class LambdaExpressionTypeInferenceTest extends ComboTestBase<LambdaExpressionTypeInferenceTest> {
    @Factory
    public static Object[] testLambdaExpressionInference() throws Exception {
        return factory(LambdaExpressionTypeInferenceTest.class);
    }

    @DimensionVar("FORMAL_SHAPE") FormalTypeShape formalShape;
    @DimensionVar("TREE_SHAPE") TreeShape treeShape;
    @DimensionVar("ARG_SHAPE_1") ArgTypeShape argShape1;
    @DimensionVar("ARG_SHAPE_2") ArgTypeShape argShape2;
    @DimensionVar("ARG_SHAPE_3") ArgTypeShape argShape3;

    @SourceFile("SAM.java")
    String samFile = "public interface SAM<X,Y> {\n"
            +"  Y apply(X x);\n"
            +"}";


    @SourceFile("Main.java")
    String clientFile = "import java.util.*;\n"
            +"public class Main {\n"
            +"  <A,R> void m(#{FORMAL_SHAPE} x) { }\n"
            +"  void test() {\n"
            +"     m(#{TREE_SHAPE});\n"
            +"  }\n"
            +"}";

    @Override
    protected void postCompile(String grp) {
        if (treeShape.compute().typeCheck(formalShape.compute(), new Type[] { argShape1.compute(), argShape2.compute(), argShape3.compute() }).isErroneous()) {
            assertCompileFailed();
        } else {
            assertCompileSucceeded();
        }
    }

    @Override
    protected boolean shouldSkip() {
        return argShape2.ordinal() != 0 && treeShape.depth < 2 || argShape3.ordinal() != 0 && treeShape.depth < 3;
    }

    static class Tree {
        TreeTag treeTag;
        Tree[] subtrees;

        Tree(TreeTag treeTag, Tree... subtrees) {
            this.treeTag = treeTag;
            this.subtrees = subtrees;
        }

        public String toString() {
            Object[] substrings = new Object[subtrees.length];
            for (int i = 0; i < subtrees.length; i++) {
                substrings[i] = subtrees[i].toString();
            }
            return String.format(treeTag.treeStr, substrings);
        }

        Type typeCheck(Type target, Type[] argtypes) {
            switch (treeTag) {
                case INT:   
                   Type type = new Type(TypeToken.INTEGER);
                   return type.compatibleWith(target) ? type : errType;
                case STRING:
                   type = new Type(TypeToken.STRING);
                   return type.compatibleWith(target) ? type : errType;
                case EXPR_LAMBDA:
                    Type restype = target.getReturnType();
                    if (restype == null) {
                        return errType;
                    }
                    Type argtype = target.getArgType();
                    int idx = subtrees[0].treeTag.argIndex();
                    if (argtype.isFree() && idx < 0) {
                        //implicit arg - stuck
                        return errType;
                    }
                    if (idx >= 0 && !argtypes[idx].compatibleWith(argtype)) {
                        //arg type mismatch
                        return errType;
                    }
                    
                    for (int i = 1; i < subtrees.length; i++) {
                        if (subtrees[i].typeCheck(restype, argtypes).isErroneous()) {
                            return errType;
                        }
                    }
                    return target;
                case COND:
                    Type typeToCheck = target;
                    if (subtrees[0].treeTag == TreeTag.INT && subtrees[1].treeTag == TreeTag.INT) {
                        Type owntype = new Type(TypeToken.INTEGER);
                        return owntype.compatibleWith(target) ? owntype : errType;
                    } else {
                        for (int i = 0; i < subtrees.length; i++) {
                            if (subtrees[i].typeCheck(target, argtypes).isErroneous()) {
                                return errType;
                            }
                        }
                        return target;
                    }
                default:
                    throw new AssertionError();
            }
        }
    }

    enum TreeTag implements StackItem<TreeTag> {
        INT("1", 0),
        STRING("\"\"", 0),
        COND("true ? %s : %s", 2),
        EXPR_LAMBDA("(%s)->%s", 2),
        IMPLICIT_1("x1", 0),
        IMPLICIT_2("x2", 0),
        IMPLICIT_3("x3", 0),
        EXPLICIT_1("#{ARG_SHAPE_1} x1", 0),
        EXPLICIT_2("#{ARG_SHAPE_2} x2", 0),
        EXPLICIT_3("#{ARG_SHAPE_3} x3", 0);

        String treeStr;
        int arity;

        TreeTag(String treeStr, int arity) {
            this.treeStr = treeStr;
            this.arity = arity;
        }

        public int arity() {
            return arity;
        }

        Tree apply(Tree... args) {
            return new Tree(this, args);
        }

        int argIndex() {
            switch (this) {
                case EXPLICIT_1:
                    return 0;
                case EXPLICIT_2:
                    return 1;
                case EXPLICIT_3:
                    return 2;
                default:
                    return -1;
            }
        }
    }

    enum TreeShape implements Template, StackReducer<TreeTag, Tree> {
        LAMBDA_INT(1, TreeTag.EXPLICIT_1, TreeTag.INT, TreeTag.EXPR_LAMBDA),
        LAMBDA_STRING(1, TreeTag.EXPLICIT_1, TreeTag.STRING, TreeTag.EXPR_LAMBDA),
        LAMBDA_COND_INT_INT(1, TreeTag.EXPLICIT_1, TreeTag.INT, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA),
        IMPL_LAMBDA_INT(1, TreeTag.IMPLICIT_1, TreeTag.INT, TreeTag.EXPR_LAMBDA),
        IMPL_LAMBDA_STRING(1, TreeTag.IMPLICIT_1, TreeTag.STRING, TreeTag.EXPR_LAMBDA),
        IMPL_LAMBDA_COND_INT_INT(1, TreeTag.IMPLICIT_1, TreeTag.INT, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA),
        LAMBDA_LAMBDA_INT(2, TreeTag.EXPLICIT_2, TreeTag.EXPLICIT_1, TreeTag.INT, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA),
        LAMBDA_LAMBDA_STRING(2, TreeTag.EXPLICIT_2, TreeTag.EXPLICIT_1, TreeTag.STRING, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA),
        LAMBDA_LAMBDA_COND_INT_INT(2, TreeTag.EXPLICIT_2, TreeTag.EXPLICIT_1, TreeTag.INT, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA),
        LAMBDA_IMPL_LAMBDA_INT(2, TreeTag.EXPLICIT_2, TreeTag.IMPLICIT_1, TreeTag.INT, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA),
        LAMBDA_IMPL_LAMBDA_STRING(2, TreeTag.EXPLICIT_2, TreeTag.IMPLICIT_1, TreeTag.STRING, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA),
        LAMBDA_IMPL_LAMBDA_COND_INT_INT(2, TreeTag.EXPLICIT_2, TreeTag.IMPLICIT_1, TreeTag.INT, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA),
        LAMBDA_COND_LAMBDA_INT_INT(2, TreeTag.EXPLICIT_2, TreeTag.EXPLICIT_1, TreeTag.INT, TreeTag.EXPR_LAMBDA, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA),
        LAMBDA_COND_LAMBDA_STRING_INT(2, TreeTag.EXPLICIT_2, TreeTag.EXPLICIT_1, TreeTag.STRING, TreeTag.EXPR_LAMBDA, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA),
        LAMBDA_COND_LAMBDA_COND_INT_INT_INT(2, TreeTag.EXPLICIT_2, TreeTag.EXPLICIT_1, TreeTag.INT, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA),
        LAMBDA_COND_IMPL_LAMBDA_INT_INT(2, TreeTag.EXPLICIT_2, TreeTag.IMPLICIT_1, TreeTag.INT, TreeTag.EXPR_LAMBDA, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA),
        LAMBDA_COND_IMPL_LAMBDA_STRING_INT(2, TreeTag.EXPLICIT_2, TreeTag.IMPLICIT_1, TreeTag.STRING, TreeTag.EXPR_LAMBDA, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA),
        LAMBDA_COND_IMPL_LAMBDA_COND_INT_INT_INT(2, TreeTag.EXPLICIT_2, TreeTag.IMPLICIT_1, TreeTag.INT, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA),
        LAMBDA_LAMBDA_LAMBDA_INT(3, TreeTag.EXPLICIT_3, TreeTag.EXPLICIT_2, TreeTag.EXPLICIT_1, TreeTag.INT, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA),
        LAMBDA_LAMBDA_LAMBDA_STRING(3, TreeTag.EXPLICIT_3, TreeTag.EXPLICIT_2, TreeTag.EXPLICIT_1, TreeTag.STRING, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA),
        LAMBDA_LAMBDA_LAMBDA_COND_INT_INT(3, TreeTag.EXPLICIT_3, TreeTag.EXPLICIT_2, TreeTag.EXPLICIT_1, TreeTag.INT, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA),
        LAMBDA_LAMBDA_IMPL_LAMBDA_INT(3, TreeTag.EXPLICIT_3, TreeTag.EXPLICIT_2, TreeTag.IMPLICIT_1, TreeTag.INT, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA),
        LAMBDA_LAMBDA_IMPL_LAMBDA_STRING(3, TreeTag.EXPLICIT_3, TreeTag.EXPLICIT_2, TreeTag.IMPLICIT_1, TreeTag.STRING, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA),
        LAMBDA_LAMBDA_IMPL_LAMBDA_COND_INT_INT(3, TreeTag.EXPLICIT_3, TreeTag.EXPLICIT_2, TreeTag.IMPLICIT_1, TreeTag.INT, TreeTag.INT, TreeTag.COND, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA, TreeTag.EXPR_LAMBDA);

        TreeTag[] treeTags;
        int depth;

        TreeShape(int depth, TreeTag... treeTags) {
            this.depth = depth;
            this.treeTags = treeTags;
        }

        Tree compute() {
            return process(treeTags, this);
        }

        public String expand(String selector) {
            return compute().toString();
        }

        public Tree reduce(TreeTag tag, Tree... args) {
            return new Tree(tag, args);
        }

        public Class<Tree> typeToken() {
            return Tree.class;
        }
    }

    static class Type {
        TypeToken typeToken;
        Type[] typeargs;

        Type(TypeToken typeToken, Type... typeargs) {
            this.typeToken = typeToken;
            this.typeargs = typeargs;
        }

        public String toString() {
            Object[] typeargStrings = new Object[typeargs.length];
            for (int i = 0; i < typeargs.length; i++) {
                typeargStrings[i] = typeargs[i].toString();
            }
            return String.format(typeToken.tokenStr, typeargStrings);
        }

        boolean isErroneous() { return false; }

        Type getReturnType() {
            return typeToken == TypeToken.SAM ?
                typeargs[1] : null;
        }

        Type getArgType() {
            return typeToken == TypeToken.SAM ?
                typeargs[0] : null;
        }

        boolean isFree() {
            switch (typeToken) {
                case A:
                case R:
                    return true;
                default:
                    for (Type t : typeargs) {
                        if (t.isFree()) return true;
                    }
                    return false;    
            }
        }

        boolean isFreeVar() {
            switch (typeToken) {
                case A:
                case R:
                    return true;
                default:                    
                    return false;    
            }
        }

        boolean compatibleWith(Type that) {
            if (that.isFreeVar()) {
                return true;
            } else {
                if (typeToken != that.typeToken)
                    return false;
                if (typeargs.length != that.typeargs.length) {
                    return false;
                }
                for (int i = 0; i < typeargs.length ; i++) {
                    if (!typeargs[i].compatibleWith(that.typeargs[i]))
                        return false;
                }
                return true;
            }
        }
    }

    static Type errType = new Type(null) {
        boolean isErroneous() {
            return true;
        }
    };

    enum TypeToken implements StackItem<TypeToken> {
        STRING("String", 0),
        INTEGER("Integer", 0),
        A("A", 0),
        R("R", 0),
        LIST("List<%s>", 1),
        SAM("SAM<%s,%s>", 2);

        int arity;
        String tokenStr;

        TypeToken(String tokenStr, int arity) {
            this.tokenStr = tokenStr;
            this.arity = arity;
        }

        public int arity() {
            return arity;
        }
    }

    enum ArgTypeShape implements Template, StackReducer<TypeToken, Type> {
        STRING(TypeToken.STRING),
        INTEGER(TypeToken.INTEGER),
        LIST_STRING(TypeToken.STRING, TypeToken.LIST),
        LIST_INTEGER(TypeToken.INTEGER, TypeToken.LIST);

        TypeToken[] tokens;

        ArgTypeShape(TypeToken... tokens) {
            this.tokens = tokens;
        }

        Type compute() {
            return process(tokens, this);
        }

        public String expand(String selector) {
            return compute().toString();
        }

        public Type reduce(TypeToken token, Type... args) {
            return new Type(token, args);
        }

        public Class<Type> typeToken() {
            return Type.class;
        }
    }

    enum FormalTypeShape implements Template, StackReducer<TypeToken, Type> {
        SAM_String_String(TypeToken.STRING, TypeToken.STRING, TypeToken.SAM),
        SAM_Integer_String(TypeToken.INTEGER, TypeToken.STRING, TypeToken.SAM),
        SAM_List_String_String(TypeToken.STRING, TypeToken.LIST, TypeToken.STRING, TypeToken.SAM),
        SAM_List_Integer_String(TypeToken.INTEGER, TypeToken.LIST, TypeToken.STRING, TypeToken.SAM),
        SAM_A_String(TypeToken.A, TypeToken.STRING, TypeToken.SAM),
        SAM_List_A_String(TypeToken.A, TypeToken.LIST, TypeToken.STRING, TypeToken.SAM),
        SAM_String_R(TypeToken.STRING, TypeToken.R, TypeToken.SAM),
        SAM_A_R(TypeToken.A, TypeToken.R, TypeToken.SAM),
        SAM_List_A_R(TypeToken.A, TypeToken.LIST, TypeToken.R, TypeToken.SAM),        
        SAM_String_SAM_String_String(TypeToken.STRING, TypeToken.STRING, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_Integer_String(TypeToken.STRING, TypeToken.INTEGER, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_List_String_String(TypeToken.STRING, TypeToken.STRING, TypeToken.LIST, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_List_Integer_String(TypeToken.STRING, TypeToken.INTEGER, TypeToken.LIST, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_A_String(TypeToken.STRING, TypeToken.A, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_List_A_String(TypeToken.STRING, TypeToken.A, TypeToken.LIST, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_String_R(TypeToken.STRING, TypeToken.STRING, TypeToken.R, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_A_R(TypeToken.STRING, TypeToken.A, TypeToken.R, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_List_A_R(TypeToken.STRING, TypeToken.A, TypeToken.LIST, TypeToken.R, TypeToken.SAM, TypeToken.SAM),
        SAM_A_SAM_String_String(TypeToken.A, TypeToken.STRING, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_String_SAM_String_String(TypeToken.STRING, TypeToken.STRING, TypeToken.STRING, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_String_SAM_Integer_String(TypeToken.STRING, TypeToken.STRING, TypeToken.INTEGER, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_String_SAM_List_String_String(TypeToken.STRING, TypeToken.STRING, TypeToken.STRING, TypeToken.LIST, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_String_SAM_List_Integer_String(TypeToken.STRING, TypeToken.STRING, TypeToken.INTEGER, TypeToken.STRING, TypeToken.LIST, TypeToken.SAM, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_String_SAM_A_String(TypeToken.STRING, TypeToken.STRING, TypeToken.A, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_String_SAM_List_A_String(TypeToken.STRING, TypeToken.STRING, TypeToken.A, TypeToken.LIST, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_String_SAM_String_R(TypeToken.STRING, TypeToken.STRING, TypeToken.STRING, TypeToken.R, TypeToken.SAM, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_String_SAM_A_R(TypeToken.STRING, TypeToken.STRING, TypeToken.A, TypeToken.R, TypeToken.SAM, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_String_SAM_List_A_R(TypeToken.STRING, TypeToken.STRING, TypeToken.A, TypeToken.LIST, TypeToken.R, TypeToken.SAM, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_A_SAM_String_String(TypeToken.STRING, TypeToken.A, TypeToken.STRING, TypeToken.STRING, TypeToken.SAM, TypeToken.SAM, TypeToken.SAM),
        SAM_String_SAM_A_SAM_String_R(TypeToken.STRING, TypeToken.A, TypeToken.STRING, TypeToken.R, TypeToken.SAM, TypeToken.SAM, TypeToken.SAM);

        TypeToken[] tokens;

        FormalTypeShape(TypeToken... tokens) {
            this.tokens = tokens;
        }

        Type compute() {
            return process(tokens, this);
        }

        public String expand(String selector) {
            return compute().toString();
        }

        public Type reduce(TypeToken token, Type... args) {
            return new Type(token, args);
        }

        public Class<Type> typeToken() {
            return Type.class;
        }
    }
}


