// https://searchcode.com/api/result/125144451/

package plantsearch.core.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Enclosed;
import org.semanticweb.owl.model.OWLException;

import plantsearch.core.PlantSearchException;
import plantsearch.core.nlp.SemanticToken.Type;
import plantsearch.core.nlp.IndexedOntology.CategoryIndexEntry;
import plantsearch.core.nlp.IndexedOntology.IndexedOntologyException;
import plantsearch.loaders.OntologyLoader;

/**
 * Takes tokens produced by the {@link BasicTokeniser} and turns them into
 * higher semantically relevant tokens (uses the ontology)
 * 
 */
@RunWith(Enclosed.class)
public class SemanticTokeniser {
	private static Logger logger = Logger.getLogger(SemanticTokeniser.class);
	
	private final IndexedOntology ontology;

	private final String[] tokens;
	private int start;

	public SemanticTokeniser(String[] tokens, IndexedOntology ontology) {
		this.tokens = tokens;
		this.ontology = ontology;
		this.start = 0;
		eatWhitespace();
	}

	private void eatWhitespace() {
		while (start < tokens.length
				&& BasicTokeniser.isSpaceToken(tokens[start])) {
			start++;
		}
	}

	boolean hasNext() {
		return start < tokens.length;
	}

	List<SemanticToken> next() throws IndexedOntologyException {
		String token = tokens[start];
		List<SemanticToken> semanticTokens = null;

		if (BasicTokeniser.isNumberToken(token) || BasicTokeniser.isInequalityToken(token))
			semanticTokens = tryCreateNumeric();
		else
			semanticTokens = tryCreateFromOntology();

		if (semanticTokens == null || semanticTokens.size() == 0)
			semanticTokens = Collections.singletonList((SemanticToken) new UnrecognisedToken(start, start));

		/* move starting position for next time past the smallest returned chunk */
		int minEnd = Integer.MAX_VALUE;
		for (SemanticToken semanticToken : semanticTokens) {
			if (minEnd > semanticToken.end)
				minEnd = semanticToken.end;
		}
		start = minEnd + 1;
		
		eatWhitespace();
		
		if (logger.isDebugEnabled()) {
			if (semanticTokens.size() == 1)
				logger.debug("created: " + dumpSemanticToken(semanticTokens.get(0)));
			else {
				logger.debug("---");
				for (SemanticToken semanticToken : semanticTokens)
					logger.debug("created: " + dumpSemanticToken(semanticToken));
				logger.debug("---");
			}
		}
		
		return semanticTokens;
	}
	
	private String dumpSemanticToken(SemanticToken semanticToken) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(semanticToken.getSemanticTokenType().toString());
		sb.append("] ");
		for (int i = semanticToken.getStart(); i <= semanticToken.getEnd(); i++) {
			sb.append(tokens[i]);
			//sb.append(" ");
		}
		return sb.toString();
	}
	

	private static final int ONTOLOGY_MAX_WORD_SPAN = 3;
	
	private List<SemanticToken> tryCreateFromOntology() throws IndexedOntologyException {
		/* attempt to match several consecutive tokens, longer first */
		int currentSpan = ONTOLOGY_MAX_WORD_SPAN;
		if (start + currentSpan > tokens.length)
			currentSpan = tokens.length - start;
		
		StringBuilder word = new StringBuilder();
		
		while (currentSpan > 0) {
			
			word.setLength(0);
			/* TODO: add all tokens and then use setLength to reduce it */
			for (int i = start; i < start + currentSpan; i++)
				word.append(tokens[i]);
			
			List<String> uris = ontology.getClassesByLexicalIndex(word.toString());
			if (uris.size() > 0) {
				/* return this set */
				ArrayList<SemanticToken> semanticTokens = new ArrayList<SemanticToken>(uris.size());
				for (String uri : uris) {
					semanticTokens.add(makeSemanticTokenFromURI(start, start + currentSpan - 1, uri));
				}
				return semanticTokens;
			}
			
			currentSpan--;
		}
		
		return null;
	}

	private SemanticToken makeSemanticTokenFromURI(int start, int end, String uri) throws IndexedOntologyException {
		
		/* work out what type of ontology entity this is */
		SemanticToken.Type semanticTokenType = null;
		
		CategoryIndexEntry cat = ontology.getCategory(uri);
		if (cat != null)
			semanticTokenType = cat.getSemanticTokenType();
		else
			semanticTokenType = SemanticToken.Type.OTHER;
		
		switch (semanticTokenType) {
			case BOOLEAN_PROPERTY:
				return new BooleanPropertyToken(start, end, uri);
			case BOTANIC_FEATURE:
				return new BotanicFeatureToken(start, end, uri);
			case DIMENSION_PROPERTY:
				return new MetricSpacePropertyToken(start, end, uri);
			case OTHER:
				return new UnrecognisedToken(start, end);
			case PLANT_PART:
				return new PlantPartToken(start, end, uri);
			case BOTANIC_TERM:
				return new UnrecognisedToken(start, end);
			case QUALIFICATION:
				return new QualificationToken(start, end, uri);
			case RELATION:
				return new RelationToken(start, end, uri);
			case QUANTITY:
				return new QuantityToken(start, end, uri);
			case STOP:
				return new StopToken(start, end, uri);
			case IGNORE:
				return new IgnoreToken(start, end, uri);
			case BARE_NUMBER:
			case DIMENSION:
				assert(false);
		}
		return new UnrecognisedToken(start, end);

	}
	
	private List<SemanticToken> tryCreateNumeric() {

		String inequality = null;
		String n1 = null, n2 = null;
		int dashPos = -1;

		int pos = start;
		int end = -1;
		
		while (true) {
			
			if (pos == tokens.length) {
				/* past end of input; last token was a match */
				end = tokens.length - 1; /* end right at the end */
				break;
			}
			
			String token = tokens[pos];
			pos++;

			/* ignore spaces */
			if (BasicTokeniser.isSpaceToken(token))
				continue;

			else if (BasicTokeniser.isNumberToken(token)) {
				if (n1 == null)
					n1 = token; /* add number */
				else if (n1.endsWith("."))
					n1 = n1 + token; /* add decimal part */
				else if (n2 == null && dashPos != -1)
					n2 = token; /* after dash, add second number */
				else if (n2 != null && n2.endsWith("."))
					n2 = n2 + token;
				else
					break;
			}

			else if (token.equals(".")) {
				if (n1 != null && !n1.contains("."))
					n1 = n1 + ".";
				else if (n2 != null && !n2.contains("."))
					n2 = n2 + ".";
				else
					break;
			}

			else if (BasicTokeniser.isDashToken(token)) {
				if (dashPos != -1 && dashPos != pos - 1)
					/* quit if have multiple dashes, however absorb consecutive dashes
					 * since "5--10" is commonly used
					 */
					break;
				else
					dashPos = pos;
			}

			else if (token.equals("<") || token.equals(">")) {
				if (inequality == null && n1 == null)
					inequality = token;
				else
					break;
			}
			else
				break;
		}
		if (end == -1)
			/* we failed to match on the current token */
			end = pos - 2;
		
		/* do not capture the "-" in fragments like "3-veined" */
		if (BasicTokeniser.isDashToken(tokens[end]))
			end--;
		/* do not capture a final "." */
		else if (tokens[end].equals("."))
			end--;		
		
		/* now, let's see what we've got */
		if (n1 == null)
			return null; /* incomplete.. */

		/* convert to floats */
		float f1 = Float.valueOf(n1);
		float f2;
		if (n2 == null)
			f2 = Float.NaN;
		else
			f2 = Float.valueOf(n2);
		
		/* see if we've got a unit after this */
		DimensionToken.Unit unit = null;
		if (end < tokens.length - 1) {
			unit = DimensionToken.tryGetUnit(tokens[end + 1]);
			if (unit != null)
				end++;
		}
		
		SemanticToken semanticToken;
		Character inequalityChar = inequality == null ? null : inequality.charAt(0);
		if (unit == null) {
			semanticToken = new NumericToken(start, end, f1, f2, inequalityChar);
		} else {
			semanticToken = new DimensionToken(start, end, f1, f2, inequalityChar, unit);
		}
		
		
		return Collections.singletonList(semanticToken);
	}

	public static class ChunkCreatorTest {

		IndexedOntology ontology = null;
		SemanticTokeniser semanticTokeniser;
		
		@Before public void load() throws OWLException, PlantSearchException {
			if (ontology == null) {
				ontology = OntologyLoader.loadMagnoliopsidaFile();
			}
		}
		
		@Test public void test1() throws PlantSearchException {
			start("6 stamens");

			checkNextSingleToken(Type.BARE_NUMBER, 0, 1);
			checkNextSingleToken(Type.PLANT_PART, 2, 2);
			checkEnd();
		}
		
		@Test public void test2() throws PlantSearchException {
			start("6");

			checkNextSingleToken(Type.BARE_NUMBER, 0, 0);
			checkEnd();
		}
		
		@Test public void test3() throws PlantSearchException {
			start("6-5.2 cm");

			checkNextSingleToken(Type.DIMENSION, 0, 6);
			checkEnd();
		}
		
		@Test public void test4() throws PlantSearchException {
			start("6-5.2 cm long");

			checkNextSingleToken(Type.DIMENSION, 0, 6);
			checkNextSingleToken(Type.DIMENSION_PROPERTY, 8, 8);
			checkEnd();
		}
		
		@Test public void test5() throws PlantSearchException {
			start("6-5.2 cm long.");

			checkNextSingleToken(Type.DIMENSION, 0, 6);
			checkNextSingleToken(Type.DIMENSION_PROPERTY, 8, 8);
			checkNextSingleToken(Type.STOP, 9, 9);
			checkEnd();
		}
		
		
		@Test public void test6() throws PlantSearchException {
			start("maybe 6-5.2 cm long.");

			checkNextSingleToken(Type.QUALIFICATION, 0, 0);
			checkNextSingleToken(Type.DIMENSION, 2, 8);
			checkNextSingleToken(Type.DIMENSION_PROPERTY, 10, 10);
			checkNextSingleToken(Type.STOP, 11, 11);
			checkEnd();
		}
		
		@Test public void test7() throws PlantSearchException {
			start("maybe 6-5.2 cm.");

			checkNextSingleToken(Type.QUALIFICATION, 0, 0);
			checkNextSingleToken(Type.DIMENSION, 2, 8);
			checkNextSingleToken(Type.STOP, 9, 9);
			checkEnd();
		}
		
		@Test public void test8() throws PlantSearchException {
			start("6-5.2.");

			checkNextSingleToken(Type.BARE_NUMBER, 0, 4);
			checkNextSingleToken(Type.STOP, 5, 5);
			checkEnd();
		}
		
		
		private void start(String line) {
			BasicTokeniser basicTokeniser = new BasicTokeniser(line);
			String[] tokens = basicTokeniser.justGetTokens();
			semanticTokeniser = new SemanticTokeniser(tokens, ontology);
		}
		
		private void checkNextSingleToken(Type type, int start, int end) throws PlantSearchException {
			assertTrue(semanticTokeniser.hasNext());
			List<SemanticToken> semanticTokens = semanticTokeniser.next();
			assertEquals(1, semanticTokens.size());
			SemanticToken semanticToken = semanticTokens.get(0);
			
			assertEquals(type, semanticToken.getSemanticTokenType());
			assertEquals(start, semanticToken.getStart());
			assertEquals(end, semanticToken.getEnd());
		}
		
		private void checkEnd() {
			assertFalse(semanticTokeniser.hasNext());
			semanticTokeniser = null;
		}
		
	}

}

