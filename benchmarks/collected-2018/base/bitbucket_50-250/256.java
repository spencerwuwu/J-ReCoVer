// https://searchcode.com/api/result/121194315/

package ca.uottawa.okorol.bioinf.ModuleInducer.services;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.TestCase;
import ca.uottawa.okorol.bioinf.ModuleInducer.exceptions.DataFormatException;
import ca.uottawa.okorol.bioinf.ModuleInducer.tools.ArrayTools;
import ca.uottawa.okorol.bioinf.ModuleInducer.tools.DataFormatter;


public class ExperimenterTest extends TestCase {
	private Experimenter fixture;
	
	public void testExtractTheory(){
		String alephOutput = "[select example] [13]\n[sat] [13]\n[has_crm(T18D3.4)]\n\n[bottom clause]\nhas_crm(A) :-\n   has_tfbs(A, m2), has_tfbs(A, m7), before(m2, m7, A), same_strand(m2, m2, A),\n\n   same_strand(m7, m7, A), close_tfbs(m2, m7, A), close_tfbs(m7, m2, A), close_t\nfbs(m7, m7, A).\n[literals] [9]\n[saturation time] [0.0]\n[reduce]\n[best label so far] [[1, 0, 2, 1]/0]\nhas_crm(A).\n[clauses constructed] [1]\n[search time] [0.0]\n[best clause]\nhas_crm('T18D3.4').\n[pos cover = 1 neg cover = 0] [pos-neg] [1]\n[atoms left] [0]\n[positive examples left] [0]\n[estimated time to finish (secs)] [0.0]\n\n[theory]\n\n[Rule 1] [Pos cover = 6 Neg cover = 0]\nhas_crm(A) :-\n   same_strand(m2, m7, A).\n\n[Rule 2] [Pos cover = 4 Neg cover = 0]\nhas_crm(A) :-\n   close_tfbs(m4, m4, A).\n\n[Rule 3] [Pos cover = 8 Neg cover = 0]\nhas_crm(A) :-\n   close_tfbs(m2, m2, A).\n\n[Rule 4] [Pos cover = 1 Neg cover = 0]\nhas_crm('F29F11.5').\n\n[Rule 5] [Pos cover = 1 Neg cover = 0]\nhas_crm('K12F2.1').\n\n[Rule 6] [Pos cover = 1 Neg cover = 0]\nhas_crm('T18D3.4').\n\n[Training set performance]\n           Actual\n        +          -\n     +  16          0          16\nPred\n     -  0          96          96\n\n        16          96         112";
        
		ArrayList<String[]> actual = new ArrayList<String[]>();
		actual.add(new String[]{"has_crm(Z)", "same_strand(m2, m7, Z)"});
		actual.add(new String[]{"has_crm(Z)", "close_tfbs(m4, m4, Z)"});
		actual.add(new String[]{"has_crm(Z)", "close_tfbs(m2, m2, Z)"});
		actual.add(new String[]{"has_crm('F29F11.5')"});
		actual.add(new String[]{"has_crm('K12F2.1')"});
		actual.add(new String[]{"has_crm('T18D3.4')"});
		
		ArrayList<String[]> expected = DataFormatter.extractTheoryByRules(alephOutput);
		
		assertEquals(expected.size(), actual.size());
		
		for (int i = 0; i < expected.size(); i++){
			String[] expectedRule = expected.get(i);
			String[] actualRule = actual.get(i);
			assertTrue(ArrayTools.arrayEquals(expectedRule, actualRule));
		}
        
	}
	
	public void testRuleEquals(){
		String[] clause1, clause2;
		
		clause1 = new String[] {"has_crm('ABC')"};
		clause2 = new String[] {"has_crm('ABC')"};
		
		assertTrue(Experimenter.ruleEquals(clause1, clause2));
		
		clause1 = new String[] {"has_crm(ABC)"};
		clause2 = new String[] {"has_crm('ABC')"};
		
		assertFalse(Experimenter.ruleEquals(clause1, clause2));
		
		clause1 = new String[] {"has_crm(Z)", "same_strand(m2, m7, Z)", "close_tfbs(m4, m4, Z)", "close_tfbs(m2, m2, Z)", "before(m2, m5, Z)"};
		clause2 = new String[] {"has_crm(Z)", "before(m2, m5, Z)", "same_strand(m2, m7, Z)", "close_tfbs(m2, m2, Z)", "close_tfbs(m4, m4, Z)"};
		
		assertTrue(Experimenter.ruleEquals(clause1, clause2));
		
		clause1 = new String[] {"has_crm(Z)", "hababa(Z)", "same_strand(m2, m7, Z)", "close_tfbs(m4, m4, Z)", "before(m2, m5, Z)"};
		clause2 = new String[] {"has_crm(Z)", "before(m2, m5, Z)", "same_strand(m2, m7, Z)", "hababa(Z)", "close_tfbs(m4, m4, Z)"};
		
		assertTrue(Experimenter.ruleEquals(clause1, clause2));
		
		clause1 = new String[] {"has_crm(ABC)", "before(m2, m5, Z)"};
		clause2 = new String[] {"has_crm(ABC)", "before(m2, m5, Z)", "before(m2, m5, Z)"};
		
		assertFalse(Experimenter.ruleEquals(clause1, clause2));
		
	}
	
	public void testContainsTerm_containsRule(){
		ArrayList<String[]> theory = new ArrayList<String[]>();
		String term;
		String rule;
		
		theory.add(new String[]{"has_crm(Z)", "close_tfbs(m2, m2, Z)", "same_strand(mA2, mB3, Z)"});
		theory.add(new String[]{"has_crm(Z)", "before(m2, m5, Z)"});
		theory.add(new String[]{"has_crm(Z)", "same_strand(m2, m7, Z)", "before(m3, m2, Z)"});
		theory.add(new String[]{"has_crm(Z)", "close_tfbs(m2, m4, Z)"});
		theory.add(new String[]{"has_crm(Z)", "before(m6, m5, Z)", "close_tfbs(m9, m2, Z)", "same_strand(m2, m7, Z)"});
		
		// *** test containsTerm
		
		term = "";
		assertFalse(Experimenter.containsTerm(theory, term));
		
		term = "close_tfbs(m2, m4, A)";
		assertTrue(Experimenter.containsTerm(theory, term));

		term = "close_tfbs(m2, m1, A)";
		assertFalse(Experimenter.containsTerm(theory, term));
		
		term = "before(m3, m2, A)";
		assertTrue(Experimenter.containsTerm(theory, term));

		term = "before(m3, m2, B)";
		assertTrue(Experimenter.containsTerm(theory, term));
		
		// *** test containsRule
		rule = "";
		assertFalse(Experimenter.containsRule(theory, rule));
		
		rule = "has_crm(A) :- close_tfbs(m2, m1, A).";
		assertFalse(Experimenter.containsRule(theory, rule));

		rule = "has_crm(A) :- close_tfbs(m2, m4, A).";
		assertTrue(Experimenter.containsRule(theory, rule));

		rule = "has_crm(A) :- close_tfbs(m9, m2, A), before(m6, m5, A), same_strand(m2, m7, A)";
		assertTrue(Experimenter.containsRule(theory, rule));
	
	}
	
	public void testUnifyVarNames(){
		String clause;
		String expected;
		
		clause = "has_crm('ABC') :- same_strand(mA2, mB3, A), other(B).";
		expected = "has_crm('ABC') :- same_strand(mA2, mB3, Z), other(Z).";
		
		assertEquals(expected, Experimenter.unifyVarNames(clause));
		
		clause = "has_crm('F29F11.5').";
		expected = "has_crm('F29F11.5').";
		
		assertEquals(expected, Experimenter.unifyVarNames(clause));
		
		clause = "has_crm(A) :-  close_tfbs(m2, m2, A).";
		expected = "has_crm(A) :-  close_tfbs(m2, m2, Z).";
	}
	
	public void testTokeniseAndTransform(){
		String clause;
		String[] expected;
		
		clause = "has_crm('ABC') :- same_strand(mA2, mB3, A), other(B).";
		expected = new String[]{"has_crm('ABC')", "same_strand(mA2, mB3, Z)", "other(Z)"};
		
		String[] actual = Experimenter.tokeniseAndTransform(clause);
		
		//ArrayTools.printArray(actual);
		
		assertEquals(expected.length, actual.length);
		
		Arrays.equals(expected, actual); //doesn't work
		
		for (int i = 0; i < expected.length; i++){
			assertEquals(expected[i], actual[i]);
		}
		
	}
	
	public void testCompareTheories(){
		String expectedAlephOutput;
		String actualAlephOutput;
		double actualAccuracy;
		
		
		expectedAlephOutput = "";
		actualAlephOutput = "";
		try {
			actualAccuracy = Experimenter.compareTheories(expectedAlephOutput, actualAlephOutput);
			fail("Should throw an exception.");
		} catch (DataFormatException e) {
			//expected to be here
		}
		
		
		expectedAlephOutput = "[select example] [13]\n[sat] [13]\n[has_crm(T18D3.4)]\n\n[bottom clause]\nhas_crm(A) :-\n   has_tfbs(A, m2), has_tfbs(A, m7), before(m2, m7, A), same_strand(m2, m2, A),\n\n   same_strand(m7, m7, A), close_tfbs(m2, m7, A), close_tfbs(m7, m2, A), close_t\nfbs(m7, m7, A).\n[literals] [9]\n[saturation time] [0.0]\n[reduce]\n[best label so far] [[1, 0, 2, 1]/0]\nhas_crm(A).\n[clauses constructed] [1]\n[search time] [0.0]\n[best clause]\nhas_crm('T18D3.4').\n[pos cover = 1 neg cover = 0] [pos-neg] [1]\n[atoms left] [0]\n[positive examples left] [0]\n[estimated time to finish (secs)] [0.0]\n\n[theory]\n\n[Rule 1] [Pos cover = 6 Neg cover = 0]\nhas_crm(A) :-\n   same_strand(m2, m7, A).\n\n[Rule 2] [Pos cover = 4 Neg cover = 0]\nhas_crm(A) :-\n   close_tfbs(m4, m4, A).\n\n[Rule 3] [Pos cover = 8 Neg cover = 0]\nhas_crm(A) :-\n   close_tfbs(m2, m2, A).\n\n[Rule 4] [Pos cover = 1 Neg cover = 0]\nhas_crm('F29F11.5').\n\n[Rule 5] [Pos cover = 1 Neg cover = 0]\nhas_crm('K12F2.1').\n\n[Rule 6] [Pos cover = 1 Neg cover = 0]\nhas_crm('T18D3.4').\n\n[Training set performance]\n           Actual\n        +          -\n     +  16          0          16\nPred\n     -  0          96          96\n\n        16          96         112";
		actualAlephOutput = "[select example] [13]\n[sat] [13]\n[has_crm(T18D3.4)]\n\n[bottom clause]\nhas_crm(A) :-\n   has_tfbs(A, m2), has_tfbs(A, m7), before(m2, m7, A), same_strand(m2, m2, A),\n\n   same_strand(m7, m7, A), close_tfbs(m2, m7, A), close_tfbs(m7, m2, A), close_t\nfbs(m7, m7, A).\n[literals] [9]\n[saturation time] [0.0]\n[reduce]\n[best label so far] [[1, 0, 2, 1]/0]\nhas_crm(A).\n[clauses constructed] [1]\n[search time] [0.0]\n[best clause]\nhas_crm('T18D3.4').\n[pos cover = 1 neg cover = 0] [pos-neg] [1]\n[atoms left] [0]\n[positive examples left] [0]\n[estimated time to finish (secs)] [0.0]\n\n[theory]\n\n[Rule 1] [Pos cover = 6 Neg cover = 0]\nhas_crm(A) :-\n   same_strand(m2, m7, A).\n\n[Rule 2] [Pos cover = 4 Neg cover = 0]\nhas_crm(A) :-\n   close_tfbs(m4, m4, A).\n\n[Rule 3] [Pos cover = 8 Neg cover = 0]\nhas_crm(A) :-\n   close_tfbs(m2, m2, A).\n\n[Rule 4] [Pos cover = 1 Neg cover = 0]\nhas_crm('F29F11.5').\n\n[Rule 5] [Pos cover = 1 Neg cover = 0]\nhas_crm('K12F2.1').\n\n[Rule 6] [Pos cover = 1 Neg cover = 0]\nhas_crm('T18D3.4').\n\n[Training set performance]\n           Actual\n        +          -\n     +  16          0          16\nPred\n     -  0          96          96\n\n        16          96         112";
		try {
			actualAccuracy = Experimenter.compareTheories(expectedAlephOutput, actualAlephOutput);
			assertTrue(Double.compare(1.0, actualAccuracy) == 0);
			
		} catch (DataFormatException e) {
			fail();
		}
		
		
		expectedAlephOutput = "[select example] [13]\n[sat] [13]\n[has_crm(T18D3.4)]\n\n[bottom clause]\nhas_crm(A) :-\n   has_tfbs(A, m2), has_tfbs(A, m7), before(m2, m7, A), same_strand(m2, m2, A),\n\n   same_strand(m7, m7, A), close_tfbs(m2, m7, A), close_tfbs(m7, m2, A), close_t\nfbs(m7, m7, A).\n[literals] [9]\n[saturation time] [0.0]\n[reduce]\n[best label so far] [[1, 0, 2, 1]/0]\nhas_crm(A).\n[clauses constructed] [1]\n[search time] [0.0]\n[best clause]\nhas_crm('T18D3.4').\n[pos cover = 1 neg cover = 0] [pos-neg] [1]\n[atoms left] [0]\n[positive examples left] [0]\n[estimated time to finish (secs)] [0.0]\n\n[theory]\n\n[Rule 1] [Pos cover = 6 Neg cover = 0]\nhas_crm(A) :-\n   same_strand(m2, m7, A).\n\n[Rule 2] [Pos cover = 4 Neg cover = 0]\nhas_crm(A) :-\n   close_tfbs(m4, m4, A).\n\n[Rule 3] [Pos cover = 8 Neg cover = 0]\nhas_crm(A) :-\n   close_tfbs(m2, m2, A).\n\n[Rule 4] [Pos cover = 1 Neg cover = 0]\nhas_crm('F29F11.5').\n\n[Rule 5] [Pos cover = 1 Neg cover = 0]\nhas_crm('K12F2.1').\n\n[Rule 6] [Pos cover = 1 Neg cover = 0]\nhas_crm('T18D3.4').\n\n[Training set performance]\n           Actual\n        +          -\n";
		actualAlephOutput = ".4)]\n\n[bottom clause]\nhas_crm(A) :-\n   has_tfbs(A, m2), has_tfbs(A, m7), before(m2, m7, A), same_strand(m2, m2, A),\n\n   same_strand(m7, m7, A), close_tfbs(m2, m7, A), close_tfbs(m7, m2, A), close_t\nfbs(m7, m7, A).\n[literals] [9]\n[saturation time] [0.0]\n[reduce]\n[best label so far] [[1, 0, 2, 1]/0]\nhas_crm(A).\n[clauses constructed] [1]\n[search time] [0.0]\n[best clause]\nhas_crm('T18D3.4').\n[pos cover = 1 neg cover = 0] [pos-neg] [1]\n[atoms left] [0]\n[positive examples left] [0]\n[estimated time to finish (secs)] [0.0]\n\n[theory]\n\n[Rule 1] [Pos cover = 6 Neg cover = 0]\nhas_crm(A) :-\n   same_strand(m2, m7, A).\n\n[Rule 2] [Pos cover = 4 Neg cover = 0]\nhas_crm(A) :-\n   close_tfbs(m4, m4, A).\n\n[Rule 6] [Pos cover = 1 Neg cover = 0]\nhas_crm('T18D3.4').\n\n[Training set performance]\n           Actual\n        +          -\n     +  16          0          16\nPred\n     -  0          96          96\n\n        16          96         112";
		try {
			actualAccuracy = Experimenter.compareTheories(expectedAlephOutput, actualAlephOutput);
			assertTrue(Double.compare(0.5, actualAccuracy) == 0);
			
		} catch (DataFormatException e) {
			fail();
		}
		
	}

}

