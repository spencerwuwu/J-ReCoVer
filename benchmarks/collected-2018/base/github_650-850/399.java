// https://searchcode.com/api/result/139542842/

/*
 * Reference ETL Parser for Java
 * Copyright (c) 2000-2009 Constantine A Plotnikov
 *
 * Permission is hereby granted, free of charge, to any person 
 * obtaining a copy of this software and associated documentation 
 * files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, 
 * and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 * SOFTWARE. 
 */
package net.sf.etl.parsers.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import net.sf.etl.parsers.AbstractParser;
import net.sf.etl.parsers.ObjectName;
import net.sf.etl.parsers.ParserException;
import net.sf.etl.parsers.SourceLocation;
import net.sf.etl.parsers.StandardGrammars;
import net.sf.etl.parsers.TermParser;
import net.sf.etl.parsers.TermToken;
import net.sf.etl.parsers.Terms;
import net.sf.etl.parsers.TextPos;
import net.sf.etl.parsers.Token;

/**
 * <p>
 * This is an abstract parser that builds trees of objects basing on parser.
 * This class was created by refactoring common parts of BeansTermParser and
 * EMFTermParser. So might be still not generic enough for other purposes.
 * </p>
 * 
 * <p>
 * Note that abstract method of this parser are expected to throw an exception
 * if structural error occurs (like trying assigning to non existing feature of
 * the object)
 * </p>
 * 
 * <p>
 * Typical usage of the parsers derived from this one is the following:
 * </p>
 * 
 * <pre>
 * TermParser p = ... ; // configure term parser and start parsing
 * try {
 *     BeansTermBarser beansParser = new BeansTermBarser(p, null);
 *     while(beansParser.hasNext()) {
 *        MyBaseBeanType c = (MyBaseBeanType)beansParser.next(); 
 *     }
 * } finally {
 *     p.close();
 * }
 * </pre>
 * 
 * @see net.sf.etl.parsers.beans.BeansTermParser
 * @author const
 * @param <BaseObjectType>
 *            this is a base type for returned objects
 * @param <FeatureType>
 *            this is a type for feature metatype used by objects
 * @param <MetaObjectType>
 *            this is a type for meta object type
 * @param <HolderType>
 *            this is a holder type for collection properties
 */
public abstract class AbstractTreeParser<BaseObjectType, FeatureType, MetaObjectType, HolderType> {

	/** a logger */
	private static final java.util.logging.Logger log = java.util.logging.Logger
			.getLogger(AbstractTreeParser.class.getName());
	/**
	 * term parser
	 */
	protected final TermParser parser;
	/**
	 * This set contains namespaces ignored by parser
	 */
	protected final HashSet<String> ignoredNamespaces = new HashSet<String>();
	/**
	 * This is map from ignored object names to set of namespaces
	 */
	final HashMap<String, Set<String>> ignoredObjects = new HashMap<String, Set<String>>();
	/** flag indicating that parser had errors */
	protected boolean hadErrors = false;
	/**
	 * If this flag is true, when default statement is encountered during
	 * hasNext(), hasNext returns false (meaning that no more objects are
	 * expected here).
	 */
	private boolean abortOnDefault = false;
	/**
	 * The current position policy
	 */
	private PositionPolicy positionPolicy = PositionPolicy.EXPANDED;
	/**
	 * The current system identifier
	 */
	protected final String systemId;

	/**
	 * A constructor
	 * 
	 * @param parser
	 *            a term parser
	 */
	public AbstractTreeParser(TermParser parser) {
		super();
		this.parser = parser;
		this.systemId = parser.getSystemId();
	}

	/**
	 * @return the system identifier for the file being parsed
	 */
	public String getSystemId() {
		return systemId;
	}

	/**
	 * @return true if there are more terms in the stream
	 */
	public boolean hasNext() {
		while (true) {
			switch (parser.current().kind()) {
			case OBJECT_START:
				// if object should be ignored, skip object
				if (isIgnorable(parser.current().objectName())) {
					skipObject();
					break;
				}
				if (abortOnDefault
						&& parser.current().objectName().namespace().equals(
								StandardGrammars.DEFAULT_NS)) {
					return false;
				}
				return true;
			case EOF:
				return false;
			case GRAMMAR_ERROR:
			case SYNTAX_ERROR:
			case SEGMENT_ERROR:
			case LEXICAL_ERROR:
				hadErrors = true;
				handleErrorFromParser(parser.current());
			default:
				advanceParser();
			}
		}
	}

	/**
	 * Advance the parser
	 * 
	 * @return the result of {@link AbstractParser#advance()}
	 */
	protected boolean advanceParser() {
		return parser.advance();
	}

	/**
	 * finish parsing the segment after root object is parsed.
	 */
	private void finishSegment() {
		int segments = 0;
		while (true) {
			switch (parser.current().kind()) {
			case SEGMENT_START:
				segments++;
				break;
			case SEGMENT_END:
				if (segments == 0) {
					return;
				}
				segments--;
				break;
			case EOF:
				throw new IllegalStateException(
						"Seqments should be properly nested.");
			case GRAMMAR_ERROR:
			case SYNTAX_ERROR:
			case SEGMENT_ERROR:
			case LEXICAL_ERROR:
				hadErrors = true;
				handleErrorFromParser(parser.current());
				break;
			}
			advanceParser();
		}
	}

	/**
	 * Set abort on object from the namespace of default grammar
	 * {@link StandardGrammars#DEFAULT_NS}. Encountering objects from this
	 * namespace usually means that loading grammar has failed, so further
	 * processing of the source rarely makes sense.
	 * 
	 * @param value
	 *            if true {@link #hasNext()} is aborted.
	 */
	public void setAbortOnDefaultGrammar(boolean value) {
		abortOnDefault = value;
	}

	/**
	 * Get the next object from the stream. Note the method skips until the end
	 * of the segments, so the errors could be attributed to the correct
	 * statement object.
	 * 
	 * @return the next object in the stream
	 */
	public BaseObjectType next() {
		if (!hasNext()) {
			throw new IllegalStateException("there are not next object");
		}
		BaseObjectType rc = parseObject();
		finishSegment();
		return rc;
	}

	/**
	 * Check if object with specified object name should be ignored
	 * 
	 * @param name
	 *            a name to check
	 * @return true if object should be ignored
	 */
	protected boolean isIgnorable(ObjectName name) {
		// check if namespace is ignored
		if (ignoredNamespaces.contains(name.namespace())) {
			return true;
		}
		// check if specific object is ignored
		final Set<String> ns = ignoredObjects.get(parser.current().objectName()
				.name());
		if (ns != null && ns.contains(name.namespace())) {
			return true;
		}
		return false;
	}

	/**
	 * Skip object in the grammar
	 */
	protected void skipObject() {
		int objectCount = 0;
		while (true) {
			switch (parser.current().kind()) {
			case OBJECT_START:
				objectCount++;
				break;
			case OBJECT_END:
				objectCount--;
				if (objectCount == 0) {
					// exit skipping
					return;
				}
				break;
			case EOF:
				log
						.severe("EOF while skipping object. Possibly bug in grammar compiler");
				return;
			case GRAMMAR_ERROR:
			case SYNTAX_ERROR:
			case SEGMENT_ERROR:
			case LEXICAL_ERROR:
				hadErrors = true;
				handleErrorFromParser(parser.current());
			}
			advanceParser();
		}
	}

	/**
	 * Ignore objects from specified namespace.
	 * 
	 * @param ns
	 *            namespace to be ignored
	 */
	public void ignoreNamespace(String ns) {
		ignoredNamespaces.add(ns);
	}

	/**
	 * @return true if there were errors during parsing process
	 */
	public boolean hadErrors() {
		return hadErrors;
	}

	/**
	 * Ignore specific object kind. Primary candidates for such ignoring are
	 * doctype and blank statements.
	 * 
	 * @param namespace
	 *            a namespace
	 * @param name
	 *            a name in namespace
	 */
	public void ignoreObjects(String namespace, String name) {
		Set<String> namespaces = ignoredObjects.get(name);
		if (namespaces == null) {
			namespaces = new HashSet<String>();
			ignoredObjects.put(name, namespaces);
		}
		namespaces.add(namespace);
	}

	/**
	 * Parse object
	 * 
	 * @return parsed object or null if object cannot be parsed for some reason
	 */
	private BaseObjectType parseObject() {
		assert parser.current().kind() == Terms.OBJECT_START : "parser is not over object"
				+ parser.current();
		// create instance
		final ObjectName name = parser.current().objectName();
		final MetaObjectType metaObject = getMetaObject(name);
		final BaseObjectType rc = createInstance(metaObject, name);
		final Object startValue = setObjectStartPos(rc, metaObject, parser
				.current());
		objectStarted(rc);
		advanceParser();
		int extraObjects = 0;
		loop: while (true) {
			switch (parser.current().kind()) {
			case OBJECT_END:
				if (extraObjects > 0) {
					extraObjects--;
				} else {
					break loop;
				}
			case VALUE_START:
			case VALUE:
				handleUnexpectedValue(parser, parser.current());
				advanceParser();
				break;
			case OBJECT_START:
				handleUnexpectedObjectStart(parser, parser.current());
				extraObjects++;
				advanceParser();
				break;
			case PROPERTY_START:
			case LIST_PROPERTY_START:
				parseProperty(rc, metaObject);
				break;
			case GRAMMAR_ERROR:
			case SYNTAX_ERROR:
			case SEGMENT_ERROR:
			case LEXICAL_ERROR:
				handleErrorFromParser(parser.current());
				hadErrors = true;
			default:
				advanceParser();
				break;
			}
		}
		assert parser.current().kind() == Terms.OBJECT_END : "parser is not over end: "
				+ parser.current();
		assert parser.current().objectName().equals(name) : "type name does not match ";
		setObjectEndPos(rc, metaObject, startValue, parser.current());
		advanceParser();
		objectEnded(rc);
		return rc;
	}

	/**
	 * This method is called when object is about start being processed
	 * 
	 * @param object
	 *            the object to be processed
	 */
	protected void objectStarted(BaseObjectType object) {
	}

	/**
	 * This method is called when object was stopped to be processed
	 * 
	 * @param object
	 *            the object that was processed
	 */
	protected void objectEnded(BaseObjectType object) {
	}

	/**
	 * Parse property
	 * 
	 * @param rc
	 *            an object to parse
	 * @param metaObject
	 *            a metaobject associated with object
	 */
	protected void parseProperty(BaseObjectType rc, MetaObjectType metaObject) {
		assert parser.current().kind() == Terms.PROPERTY_START
				|| parser.current().kind() == Terms.LIST_PROPERTY_START : "parser is not over property: "
				+ parser.current();

		final FeatureType f = getPropertyMetaObject(rc, metaObject, parser
				.current());
		final boolean isList = parser.current().kind() == Terms.LIST_PROPERTY_START;
		final HolderType holder = isList ? startListCollection(rc, metaObject,
				f) : null;

		advanceParser();
		int extraObjects = 0;
		loop: while (true) {
			switch (parser.current().kind()) {
			case PROPERTY_END:
			case LIST_PROPERTY_END:
				if (extraObjects > 0) {
					extraObjects--;
				} else {
					break loop;
				}
			case PROPERTY_START:
			case LIST_PROPERTY_START:
				handleUnexpectedPropertyStart(parser, parser.current());
				extraObjects++;
				advanceParser();
				break;
			case OBJECT_START: {
				if (isIgnorable(parser.current().objectName())) {
					skipObject();
					break;
				}
				final Object v = parseObject();
				if (isList) {
					addToFeature(rc, f, holder, v);
				} else {
					setToFeature(rc, f, v);
				}
				break;
			}
				// FIXME multipart values
			case VALUE: {
				final Token value = parser.current().token().token();
				if (isList) {
					addValueToFeature(rc, f, holder, value);
				} else {
					setValueToFeature(rc, f, value);
				}
				advanceParser();
				break;
			}
			case GRAMMAR_ERROR:
			case SYNTAX_ERROR:
			case SEGMENT_ERROR:
			case LEXICAL_ERROR:
				hadErrors = true;
				handleErrorFromParser(parser.current());
			default:
				advanceParser();
				break;
			}
		}
		if (isList) {
			endListCollection(rc, metaObject, f, holder);
		}

	}

	/**
	 * Handle error from parser
	 * 
	 * @param errorToken
	 *            a token to be reported
	 */
	protected void handleErrorFromParser(TermToken errorToken) {
		if (log.isLoggable(Level.SEVERE)) {
			log.severe("Error is detected during parsing file "
					+ parser.getSystemId() + ": " + errorToken);
		}
	}

	/**
	 * Handle unexpected property start. Default implementation throws an
	 * exception. This means a serious bug in grammar. However, subclasses might
	 * reimplement this method to support some other policy.
	 * 
	 * @param parser
	 *            a term parser
	 * @param token
	 *            a token
	 */
	protected void handleUnexpectedPropertyStart(TermParser parser,
			TermToken token) {
		throw new ParserException("Unexpected property start inside property:"
				+ token);
	}

	/**
	 * Handle unexpected property end. Default implementation throws an
	 * exception. This means a serious bug in grammar. However, subclasses might
	 * reimplement this method to support some other policy.
	 * 
	 * @param parser
	 *            a term parser
	 * @param token
	 *            a token
	 */
	protected void handleUnexpectedObjectStart(TermParser parser,
			TermToken token) {
		throw new ParserException("Unexpected object start inside object:"
				+ token);
	}

	/**
	 * Handle unexpected value. Default implementation throws an exception. This
	 * means a serious bug in grammar. However, subclasses might reimplement
	 * this method to support some other policy.
	 * 
	 * @param parser
	 *            a term parser
	 * @param token
	 *            a token
	 */
	protected void handleUnexpectedValue(TermParser parser, TermToken token) {
		throw new ParserException("Unexpected value inside object:" + token);
	}

	/**
	 * Parse value to fit to feature
	 * 
	 * @param rc
	 *            a context object
	 * @param f
	 *            a feature that will be used to set or add this value
	 * @param value
	 *            a value to parse
	 * @return parsed value
	 */
	protected Object parseValue(BaseObjectType rc, FeatureType f, Token value) {
		return value.text();
	}

	/**
	 * Set value to feature
	 * 
	 * @param rc
	 *            an object
	 * @param f
	 *            a feature to update
	 * @param value
	 *            a value to set
	 */
	private void setValueToFeature(BaseObjectType rc, FeatureType f, Token value) {
		setToFeature(rc, f, parseValue(rc, f, value));

	}

	/**
	 * Add value to feature
	 * 
	 * @param rc
	 *            an object
	 * @param f
	 *            a feature to update
	 * @param holder
	 *            a collection
	 * @param value
	 *            a value to add
	 */
	private void addValueToFeature(BaseObjectType rc, FeatureType f,
			HolderType holder, Token value) {
		addToFeature(rc, f, holder, parseValue(rc, f, value));
	}

	/**
	 * Set object to feature
	 * 
	 * @param rc
	 *            an object
	 * @param f
	 *            a feature to update
	 * @param v
	 *            a value to set
	 */
	protected abstract void setToFeature(BaseObjectType rc, FeatureType f,
			Object v);

	/**
	 * Add object to feature
	 * 
	 * @param rc
	 *            an object
	 * @param f
	 *            a feature to update
	 * @param holder
	 *            a collection objects
	 * @param v
	 *            a value to add
	 */
	protected abstract void addToFeature(BaseObjectType rc, FeatureType f,
			HolderType holder, Object v);

	/**
	 * Start list collection. Note that this method has been created primarily
	 * because of beans parser. That parses need to update array. So to reduce
	 * array creation it is possible to create an array list from current array
	 * and than convert it back to array.
	 * 
	 * @param rc
	 *            an object
	 * @param metaObject
	 *            an metaobject
	 * @param f
	 *            a feature to be updated
	 * @return a collection
	 */
	protected abstract HolderType startListCollection(BaseObjectType rc,
			MetaObjectType metaObject, FeatureType f);

	/**
	 * Finish list collection
	 * 
	 * @param rc
	 *            an object
	 * @param metaObject
	 *            an type of object
	 * @param f
	 *            an feature to update
	 * @param holder
	 *            an holder of values
	 */
	protected abstract void endListCollection(BaseObjectType rc,
			MetaObjectType metaObject, FeatureType f, HolderType holder);

	/**
	 * get feature meta object
	 * 
	 * @param rc
	 *            an object
	 * @param metaObject
	 *            a metaobject to examine
	 * @param token
	 *            a token that contains LIST_PROPERTY_START or PROPERTY_START
	 *            events.
	 * @return a feature object
	 */
	protected FeatureType getPropertyMetaObject(BaseObjectType rc,
			MetaObjectType metaObject, TermToken token) {
		return getPropertyMetaObject(rc, metaObject, token.propertyName()
				.name());
	}

	/**
	 * get feature meta object
	 * 
	 * @param rc
	 *            an object
	 * @param metaObject
	 *            a metaobject to examine
	 * @param name
	 *            name of property.
	 * @return a feature object
	 */
	protected abstract FeatureType getPropertyMetaObject(BaseObjectType rc,
			MetaObjectType metaObject, String name);

	/**
	 * Set start position in object. Default implementation tries to set
	 * properties startLine, startColumn, and startOffset with corresponding
	 * values.
	 * 
	 * @param rc
	 *            an object
	 * @param metaObject
	 *            an meta object
	 * @param token
	 *            an start object token
	 * @return a value to be passed to
	 *         {@link #setObjectEndPos(Object, Object, Object, TermToken)}, the
	 *         default implementation returns the start position.
	 */
	protected Object setObjectStartPos(BaseObjectType rc,
			MetaObjectType metaObject, TermToken token) {
		final TextPos pos = token.start();
		switch (positionPolicy) {
		case EXPANDED:
			final FeatureType startLineFeature = getPropertyMetaObject(rc,
					metaObject, "startLine");
			setToFeature(rc, startLineFeature, new Integer(pos.line()));
			final FeatureType startColumnFeature = getPropertyMetaObject(rc,
					metaObject, "startColumn");
			setToFeature(rc, startColumnFeature, new Integer(pos.column()));
			final FeatureType startOffsetFeature = getPropertyMetaObject(rc,
					metaObject, "startOffset");
			setToFeature(rc, startOffsetFeature, new Long(pos.offset()));
			break;
		case POSITIONS:
			final FeatureType startFeature = getPropertyMetaObject(rc,
					metaObject, "start");
			setToFeature(rc, startFeature, pos);
			break;
		}
		return pos;
	}

	/**
	 * Set end position in object. Default implementation tries to set
	 * properties endLine, endColumn, and endOffset with corresponding values.
	 * 
	 * @param rc
	 *            an object
	 * @param metaObject
	 *            an meta object
	 * @param startValue
	 *            a value returned from
	 *            {@link #setObjectStartPos(Object, Object, TermToken)}
	 * @param token
	 *            an end object token
	 */
	protected void setObjectEndPos(BaseObjectType rc,
			MetaObjectType metaObject, Object startValue, TermToken token) {
		final TextPos pos = token.start();
		switch (positionPolicy) {
		case EXPANDED:
			final FeatureType endLineFeature = getPropertyMetaObject(rc,
					metaObject, "endLine");
			setToFeature(rc, endLineFeature, new Integer(pos.line()));
			final FeatureType endColumnFeature = getPropertyMetaObject(rc,
					metaObject, "endColumn");
			setToFeature(rc, endColumnFeature, new Integer(pos.column()));
			final FeatureType endOffsetFeature = getPropertyMetaObject(rc,
					metaObject, "endOffset");
			setToFeature(rc, endOffsetFeature, new Long(pos.offset()));
			break;
		case POSITIONS:
			final FeatureType endFeature = getPropertyMetaObject(rc,
					metaObject, "end");
			setToFeature(rc, endFeature, pos);
			break;
		case SOURCE_LOCATION:
			final FeatureType locationFeature = getPropertyMetaObject(rc,
					metaObject, "location");
			setToFeature(rc, locationFeature, new SourceLocation(
					(TextPos) startValue, pos, systemId));
			break;
		default:
			throw new IllegalStateException(
					"Uknown or unsupported position policy: " + positionPolicy);
		}
	}

	/**
	 * Set policy on how text position is reported to AST. If the neither policy
	 * defined in the enumeration {@link PositionPolicy} suits the AST classes,
	 * a custom policy could be implemented by overriding the methods
	 * {@link #setObjectStartPos(Object, Object, TermToken)} and
	 * {@link #setObjectEndPos(Object, Object, Object, TermToken)}.
	 * 
	 * @param policy
	 *            new value of policy
	 */
	public void setPosPolicy(PositionPolicy policy) {
		if (policy == null) {
			throw new NullPointerException("The null policy is not alllowed");
		}
		this.positionPolicy = policy;
	}

	/**
	 * Get meta object by name. Metaobject can be anything that can be used to
	 * create class. For example BeansTermParser uses BeanInfo as meta object.
	 * 
	 * @param name
	 *            an object to be mapped to metaobject
	 * @return an meta object
	 */
	protected abstract MetaObjectType getMetaObject(ObjectName name);

	/**
	 * Create instance of object from meta object
	 * 
	 * @param metaObject
	 *            a metaobject
	 * @param name
	 *            a name of object
	 * @return new instance
	 */
	protected abstract BaseObjectType createInstance(MetaObjectType metaObject,
			ObjectName name);

	/**
	 * Predefined position setting policies. They determine how start/end
	 * positions are saved in AST. It is possible to create a custom the policy
	 * by overriding the methods
	 * {@link AbstractTreeParser#setObjectStartPos(Object, Object, TermToken)}
	 * and
	 * {@link AbstractTreeParser#setObjectEndPos(Object, Object, Object, TermToken)}
	 * .
	 */
	public enum PositionPolicy {
		/**
		 * Use field {@code startLine} (int), {@code startColumn}(int), {@code
		 * startOffset}(long), {@code endLine}, {@code endColumn}, {@code
		 * endOffset}
		 */
		EXPANDED,
		/** Use fields {@code start} and {@code end} (both are {@link TextPos}) */
		POSITIONS,
		/** Use the field {@code location} of type {@link SourceLocation}. */
		SOURCE_LOCATION,
	}
}

