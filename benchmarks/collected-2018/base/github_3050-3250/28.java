// https://searchcode.com/api/result/17331076/

/*******************************************************************************
 * Copyright (c) 2005-2011 eBay Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.ebayopensource.dsf.jst.validation.vjo.BugFixes;
import static com.ebay.junitnexgen.category.Category.Groups.FAST;
import static com.ebay.junitnexgen.category.Category.Groups.P1;
import static com.ebay.junitnexgen.category.Category.Groups.P3;
import static com.ebay.junitnexgen.category.Category.Groups.UNIT;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ebayopensource.dsf.jsgen.shared.ids.FieldProbIds;
import org.ebayopensource.dsf.jsgen.shared.ids.MethodProbIds;
import org.ebayopensource.dsf.jsgen.shared.ids.TypeProbIds;
import org.ebayopensource.dsf.jsgen.shared.ids.VarProbIds;
import org.ebayopensource.dsf.jsgen.shared.ids.VjoSyntaxProbIds;
import org.ebayopensource.dsf.jsgen.shared.validation.vjo.VjoSemanticProblem;
import org.ebayopensource.dsf.jsgen.shared.validation.vjo.semantic.VjoConstants;
import org.ebayopensource.dsf.jst.ProblemSeverity;
import org.ebayopensource.dsf.jst.validation.vjo.VjoValidationBaseTester;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.junitnexgen.category.Category;
import com.ebay.junitnexgen.category.Description;
import com.ebay.junitnexgen.category.ModuleInfo;

@Category( { P1, FAST, UNIT })
@ModuleInfo(value="DsfPrebuild",subModuleId="JsToJava")
public class VjoValidationBugFixTests extends VjoValidationBaseTester {
    @Test
    // Bug 4753
    // @Ignore //TODO - Bug 7725 created to fix this test
    @Category( { P1, FAST, UNIT })
    @Description("Test alias must be found at current space via this.vj$")
    public void testAliasError() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>();
        final List<VjoSemanticProblem> problems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4753.js", this
                        .getClass());
        // assertProblemEquals(expectProblems, problems);

        assertProblemEquals(expectedProblems, problems);
    }
    
    
    @Test
    // Bug 8786
    @Category( { P1, FAST, UNIT })
    @Description("Test alias must be found at current space via this.vj$")
    public void testBug8786() throws Exception {
        expectProblems.clear();
        final List<VjoSemanticProblem> problems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug8786.js",
                this.getClass());
        assertProblemEquals(expectProblems, problems);
    }
    

    @Test
    // Bug 4753
    // @Ignore //TODO - Bug 7725 created to fix this test
    @Category( { P1, FAST, UNIT })
    @Description("Test alias must be found at current space via this.vj$")
    public void testAliasErrorExtn() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>();
        expectedProblems.add(createNewProblem(TypeProbIds.UnusedActiveNeeds,
                1, 0));
        expectedProblems.add(createNewProblem(FieldProbIds.UndefinedField,
                6, 0));
        final List<VjoSemanticProblem> problems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4753Extn.js",
                this.getClass());
        assertProblemEquals(expectedProblems, problems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test no NPE before process context")
    // Bug 3942
    public void testBug3942Error() throws Exception {
        try {
            final List<VjoSemanticProblem> problems = getVjoSemanticProblem(
                    "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug3942.js",
                    this.getClass());
            Assert.assertEquals(0, problems.size());
        } catch (AssertionError err) {
            return;
        }

        Assert
                .fail("there should be syntax error blocking the following validation");
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test no NPE before process context")
    // Bug 3943
    public void testBug3943Error() throws Exception {
        final List<VjoSemanticProblem> problems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "IA.js", this
                        .getClass());
        Assert.assertEquals(0, problems.size());
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Tets correctly parser qualifier name")
    // Bug 3944
    // @Ignore //Bug 7832
    public void testBug3944Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>();
        final List<VjoSemanticProblem> problems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug3944.js", this
                        .getClass());
        for (VjoSemanticProblem p : problems) {
            System.out.println(p);
        }
        assertProblemEquals(expectedProblems, problems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Tets undefined function")
    // Bug 3945
    public void testBug3945Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>();
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedFunction,
                13, 0));
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedFunction,
                34, 0));
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedFunction,
                24, 0));
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedFunction,
                29, 0));
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedFunction,
                12, 0));
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedFunction,
                23, 0));
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedFunction,
                18, 0));
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedFunction,
                35, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug3945.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    // Bug 3947
    @Category( { P3, FAST, UNIT })
    @Description("test vjo.getType().clazz")
    @Ignore("linking seems to be problem for vjo.getType().clazz works when you run test standalone but not in multiple case")
    public void testBug3947Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>();
        expectedProblems.add(createNewProblem(
                VjoSyntaxProbIds.TypeUnknownNotInTypeSpace, 32, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug3947.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("test correctlly get type name from model")
    // Bug 3981
    public void testBug3981Error() throws Exception {
        try {
            final List<VjoSemanticProblem> problems = getVjoSemanticProblem(
                    "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug3981.js",
                    this.getClass());
            Assert.assertEquals(0, problems.size());
        } catch (AssertionError err) {
            return;
        }

        Assert
                .fail("there should be syntax error blocking the following validation");
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test arguments numerb is wrong with expected")
    // Bug 4591
    // @Ignore
    public void testBug4591Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>();
        expectedProblems.add(createNewProblem(
                MethodProbIds.WrongNumberOfArguments, 2, 0));
        expectedProblems.add(createNewProblem(
                VjoSyntaxProbIds.InvalidIdentifier, 2, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4591.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    //  
    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test create Function. Navigator. Array ")
    // Bug 4790, 4402, 3865, 3941, 4627, 4894, 4752, 3972, 5004, 5006
    public void testBug4626Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>();

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "CTypeUtil.js",
                this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    // Bug 4630
    // @Ignore
    @Category( { P1, FAST, UNIT })
    @Description("test two vjo.ctype exist  ")
    public void testBug4630Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>();
        expectedProblems.add(createNewProblem(
                VjoSyntaxProbIds.IncorrectVjoSyntax, 2, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4630.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    // Bug 4699
    @Category( { P3, FAST, UNIT })
    @Description("test return value 1.0 match definition type ")
    public void testBug4699Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4699.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test can't assign a type to a vaiable ")
    // Bug 4743
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug4743Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4743.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    // Bug 4754
    @Category( { P3, FAST, UNIT })
    @Description("test redefined local variable ")
    public void testBug4754Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(VarProbIds.RedefinedLocal, 8, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4754.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test create Function. Navigator. Array ")
    // Bug 4790
    public void testBug4790Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "CTypeUtil.js",
                this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test undefined method ")
    // Bug 4791
    public void testBug4791Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedMethod, 7,
                0));
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedMethod,
                12, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4791.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test ctype only exist inits block ")
    // Bug 4827
    public void testBug4827Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4827.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    // Bug 4926
    @Category( { P3, FAST, UNIT })
    @Description("test defined varibale in inits block ")
    public void testBug4926Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4926.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test anotation args number is differ with actual args number ")
    // Bug 4985
    public void testBug4985Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4985.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test invlok static method from init block")
    // Bug 4987
    public void testBug4987Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4987.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test method return type incompatible with declared type")
    // Bug 4991
    public void testBug4991Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                TypeProbIds.IncompatibleTypesInEqualityOperator, 9, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4991.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Ignore
    @Test
    @Category( { P1, FAST, UNIT })
    @Description("test overload the return type different invoving void situation")
    // Bug 4993
    public void testBug4993Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(MethodProbIds.AmbiguousMethod, 6,
                0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4993.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test invoking type is not import but exist in type space")
    // Bug 4997
    public void testBug4997Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                VjoSyntaxProbIds.TypeUnknownMissingImport, 4, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4997.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    // Bug 4998
    @Category( { P1, FAST, UNIT })
    @Description("test @SUPRESSTYPECHECK tag")
    public void testBug4998Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug4998.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    // Bug 5000
    @Category( { P1, FAST, UNIT })
    @Description("test prototype value of ")
    public void testBug5000Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5000.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    // Bug 5002
    @Category( { P1, FAST, UNIT })
    @Description("test create Date object with number ")
    public void testBug5002Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5002.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test alert method can be invloked ")
    // Bug 5013
    public void testBug5013Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5013.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test alert method argment is an invoking expression")
    // Bug 5061
    public void testBug5061Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5061.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test alert method can be exist in props block")
    // Bug 5065
    public void testBug5065Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5065.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("test alert method argment is an expression of array object invoking method or field ")
    // Bug 5066
    public void testBug5066Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5066.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test alert method can give accept arguemtn such as RegExp.$1  ")
    // Bug 5079
    public void testBug5079Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5079.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test alert method can give accept arguemtent as variable invoking expression  ")
    // Bug 5080
    public void testBug5080Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5080.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test alert method can give accept arguemtent as variable invoking expression  ")
    // Bug 5103
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug5103Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5103.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test document can be used directly ")
    // Bug 5108
    public void testBug5108Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5108.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test ctype can be declared as saitisfied IType ")
    // Bug 5152
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug5152Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5152.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test parseFloat can be used directly ")
    // Bug 5234
    public void testBug5234Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5234.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    // Bug 5293
    @Category( { P3, FAST, UNIT })
    @Description("Test type reference ")
    public void testBug5293Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5293.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test mutiply assignment with one var declaration ")
    // Bug 5296
    public void testBug5296Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5296.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test alert argument can be accept by array.name and array.location ")
    // Bug 5297
    public void testBug5297Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5297.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    // @Test //Bug 5296
    // @Ignore //due to itype method doesn't have default abtract modifier
    // it's confirmed that vjo.NEEDS_IMPL doesn't mean abstract equivalent
    // public void testBug5298Error() throws Exception {
    // VjoValidationProblemHelper helper = new VjoValidationProblemHelper();
    // helper.addProblems(new VjoSemanticProblem(
    // MethodProbIds.UndefinedMethod, 1, 0, 0, ProblemSeverity.error));
    // helper.addProblems(new VjoSemanticProblem(
    // MethodProbIds.UndefinedMethod, 1, 0, 0, ProblemSeverity.error));
    //      
    // lookUpTarget("Bug5296IType.vjo");
    // lookUpTarget("Bug5296CType.vjo");
    // VjoValidationResult result = getProblems("Bug5296.vjo");
    // printResult(result);
    // Assert.assertTrue(result.getAllProblems().size() ==
    // helper.getProblems().size());
    //      
    // for(VjoSemanticProblem problem : result.getAllProblems()){
    // Assert.assertTrue(helper.getProblems().contains(problem));
    // }
    // }
    //  
    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test encodeURIComponent method can be used directly ")
    // Bug 5305
    public void testBug5305Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5305.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test mutiply satisfy situation ")
    // Bug 5318
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug5318Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedMethod, 1,
                0));
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedMethod, 1,
                0));
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedMethod, 1,
                0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5318.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("static field can be initilize at inits block")
    public void testBug5346Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5346.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("arguments is bound with Array type,")
    public void testBug5349Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5349.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Assignment with var to var")
    public void testBug5395Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5395.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Outer ctype delcared as private")
    public void testBug5397Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                TypeProbIds.IllegalModifierForClass, 1, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5397.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test unkonw type in type space")
    // @Ignore
    public void testBug5398Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                VjoSyntaxProbIds.TypeUnknownNotInTypeSpace, 4, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5398.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test unkonw type in type space")
    public void testBug5399Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                VjoSyntaxProbIds.TypeUnknownMissingImport, 9, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5399.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test method return type is differ with declared")
    public void testBug5464Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(TypeProbIds.TypeMismatch, 10, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5464.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test invoking static field or fucntion from protos block")
    public void testBug5466Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                FieldProbIds.NonStaticFieldFromStaticInvocation, 9, 0));
        expectedProblems.add(createNewProblem(
                FieldProbIds.NonStaticFieldFromStaticInvocation, 10, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5466.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Support ?: expression")
    public void testBug5482Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5482.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Get field style")
    public void testBug5483Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5483.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test child and faterh construcours all missed anoation ")
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug5485Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.",
                "Bug5485CType2.js", this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test method atuo binding return type ")
    public void testBug5513Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5513.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test invloking array with index ")
    public void testBug5514Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5514.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Ignore
    public void testBug5515Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5515.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test anotation function lost () and return with this ")
    public void testBug5516Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5516.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test empty protos block have no valdiation error")
    public void testBug5612Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5612.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test final field assignment")
    public void testBug5678Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                FieldProbIds.FinalFieldAssignment, 11, 0));
        expectedProblems.add(createNewProblem(
                FieldProbIds.FinalFieldAssignment, 17, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5678.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test if statement exist in block")
    // test case is ignored as this requires a backend support of block
    // statement in method body
    public void testBug5685Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5685.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test switch expression return statements")
    public void testBug5710Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
              MethodProbIds.UnreachableStmt, 14, 0));
        
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5710.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test support inner function return statement")
    public void testBug5720Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5720.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test overload method modifiers")
    public void testBug5783Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
//        expectedProblems.add(createNewProblem(
//                MethodProbIds.OverloadMethodWithVariableModifiers, 26, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5783.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test java key words")
    public void testBug5880Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        for (int i = 0; i < VjoConstants.JAVA_ONLY_KEYWORDS.size(); i++) {
            expectedProblems.add(createNewProblem(
                    VjoSyntaxProbIds.InvalidIdentifier, i + 3, 0));
        }

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5880.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test overrriden static methods")
    public void testBug5891Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                MethodProbIds.OverrideSuperStaticMethod, 11, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.",
                "Bug5891CType2.js", this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test return statements in inits block")
    public void testBug5908Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug5908.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test private constructure")
    // Bug 6056
    public void testBug6056Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.",
                "Bug6056CType2.js", this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test undefined name in alert method")
    public void testBug6100Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(VarProbIds.UndefinedName, 4, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6100.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test method is overridden by child of child js file")
    // Bug 6184f method is overridden by child of child js file
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6184Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.",
                "Bug6184CType3.js", this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test multiprops block and protos block in itype ")
    // Bug 6191
    public void testBug6191Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        try {
            final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                    "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6191.js",
                    this.getClass());
            assertProblemEquals(expectedProblems, actualProblems);
        } catch (AssertionError error) {
            return;
        }

        Assert
                .fail("syntax error should blocked the test cases from reaching here");
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test otype related issues:multi endtype(). function block")
    // Bug 6217
    public void testBug6217Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
//        expectedProblems.add(createNewProblem(
//                VjoSyntaxProbIds.OTypeWithNoneObjLiteralProperty, 3, 0));
        expectedProblems.add(createNewProblem(
                MethodProbIds.BodyForAbstractMethod, 12, 0));
//        expectedProblems.add(createNewProblem(
//                VjoSyntaxProbIds.OTypeWithInnerTypes, 1, 0));
        // expectedProblems.add(createNewProblem(VjoSyntaxProbIds.IncorrectVjoSyntax,
        // 22, 0));
        // bugfix by roy, incorrect vjo syntax issue are now changed to
        // undefined method error
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedMethod,
                22, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6217.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test reduce overriden visibility")
    // Bug 6222
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6222Error1() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                MethodProbIds.OverrideSuperMethodWithReducedVisibility, 4, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.",
                "Bug6222CTypeErr1.js", this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test unimplement method from iType")
    // Bug 6222
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6222Error2() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedMethod, 1,
                0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.",
                "Bug6222CTypeErr2.js", this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test unimplement method from iType")
    // Bug 6222
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6222Error3() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedMethod, 1,
                0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.",
                "Bug6222CTypeErr3.js", this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test unimplement method from iType, but ctype no protos and props block")
    // Bug 6239
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6239Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedMethod, 1,
                0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6239CType.js",
                this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test Otype can't as an inner type")
    // Bug 6246
    public void testBug6246Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                VjoSyntaxProbIds.OTypeAsInnerType, 3, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6246.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test itype can't be defined with final")
    // Bug 6247
    public void testBug6247Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                TypeProbIds.IllegalModifierForInterface, 1, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6247.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test itype mtype can't be initilized")
    // Bug 6310
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6310Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(TypeProbIds.ObjectMustBeClass, 7,
                0));
        expectedProblems.add(createNewProblem(TypeProbIds.ObjectMustBeClass, 6,
                0));
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6310.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }
    
    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test itype mtype can't be initilized")
    // Bug 6310
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug8846Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(TypeProbIds.TypeMismatch, 7, 0));
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug8846.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
        VjoSemanticProblem problem = actualProblems.get(0);
        Assert.assertTrue(problem.getMessage().contains("Date"));
        Assert.assertTrue(problem.getMessage().contains("Bug8846"));
        Assert.assertTrue(problem.getMessage().contains("String"));
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test ctype inherits atype which have abstract method")
    // Bug 6312
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6312Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedMethod, 1,
                0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6312CType.js",
                this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test multi overloaded methods exist in itype. But ctype only implement one method")
    // Bug 6351
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6351Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(MethodProbIds.UndefinedMethod, 1,
                0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6351CType.js",
                this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test arguments field in current function's property and function")
    public void testBug6358Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6358.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test mtype expect itype which have some function")
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6445Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6445MType.js",
                this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test ctype and inner type have same name")
    public void testBug6451Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(TypeProbIds.HidingEnclosingType,
                1, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6451.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test create static innertype, and instance innertype")
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6452Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6452.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test ctype mix mtype, and mtype's function and property can be used")
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6465Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6465.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test etype only eixt values and inits block")
    public void testBug6476Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6476.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test itype's modifier and method's mdofier")
    public void testBug6512Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                VjoSyntaxProbIds.ITypeAllowsOnlyPublicModifier, 4, 0));
        expectedProblems.add(createNewProblem(
                MethodProbIds.MethodBothFinalAndAbstract, 4, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6512.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test assign etype's value to a variable")
    // @Ignore //parser issue that enum values couldn't be parsed if etype has
    // satisfies
    // unignored in 7725 bugfix, parser seems to be working in this case now
    public void testBug6514Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6514EType.js",
                this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P1, FAST, UNIT })
    @Description("Test itype's inner type is marked final by default")
    // when inner typs is itype, all members become both abstract and final
    // which caused the validation error
    // from validation perspective, it's better that parser could
    // unmark itype's inner type's final flag
    public void testBug6544Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6544.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test itype can't exist instance property")
    public void testBug6545Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                VjoSyntaxProbIds.ITypeAllowsOnlyPublicModifier, 5, 0));
        expectedProblems.add(createNewProblem(
                VjoSyntaxProbIds.ITypeWithInstanceProperty, 9, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6545.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test function without anonation")
    public void testBug6550Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6550.js", this
                        .getClass());
        assertProblemEquals(expectedProblems, actualProblems);
    }

    @Test
    @Category( { P3, FAST, UNIT })
    @Description("Test itype public function is overriden with default access specifier")
    // @Ignore //TODO - Bug 7725 created to fix this test
    public void testBug6555Error() throws Exception {
        final List<VjoSemanticProblem> expectedProblems = new ArrayList<VjoSemanticProblem>(
                0);
        expectedProblems.add(createNewProblem(
                MethodProbIds.OverrideSuperMethodWithReducedVisibility, 5, 0));

        final List<VjoSemanticProblem> actualProblems = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6555CType.js",
                this.getClass());
        assertProblemEquals(expectedProblems, actualProblems);

        final List<VjoSemanticProblem> expectedProblems2 = new ArrayList<VjoSemanticProblem>(
                0);
        final List<VjoSemanticProblem> actualProblems2 = getVjoSemanticProblem(
                "org.ebayopensource.dsf.jst.validation.vjo.BugFixes.", "Bug6555Main.js",
                this.getClass());
        assertProblemEquals(expectedProblems2, actualProblems2);

        final List
