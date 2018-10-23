// https://searchcode.com/api/result/108253658/

/* gvSIG. Sistem a de Informacion Geografica de la Generalitat Valenciana
 *
 * Copyright (C) 2007 Generalitat Valenciana.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,USA.
 *
 * For more information, contact:
 *
 *  Generalitat Valenciana
 *   Conselleria d'Infraestructures i Transport
 *   Av. Blasco Ibanez, 50
 *   46010 VALENCIA
 *   SPAIN
 *
 *      +34 9638 62 495
 *      gvsig@gva.es
 *      www.gvsig.gva.es
 */
package org.gvsig.bxml.stream;

import java.io.IOException;

import javax.xml.namespace.QName;

/**
 * BxmlStreamWriter defines an "XML push" like streaming API for writing XML documents encoded as
 * per the OGC Binary XML Best Practices document.
 * <p>
 * This interface does not defines setter methods to control encoding behavior such as byte order to
 * use or character encoding. A BxmlStreamWriter instance is meant to be already configured with the
 * desired encoding options when returned from the {@link BxmlOutputFactory}.
 * </p>
 * <p>
 * Sample usage:
 * 
 * <code>
 * <pre>
 * BxmlOutputFactory factory = BxmlFactoryFinder.newOutputFactory();
 * BxmlStreamWriter writer = factory.createSerializer(inputStream);
 * 
 * writer.setSchemaLocation(&quot;http://www.example.com&quot;, &quot;http://www.example.com/schema.xsd&quot;);
 * 
 * writer.writeStartDocument();
 * 
 * writer.writeNamespace(&quot;ex&quot;, &quot;http://www.example.com&quot;)
 * writer.setDefaultNamespace(&quot;http://www.anotherexample.com&quot;);
 * 
 * writer.writeStartElement(&quot;&quot;, &quot;root&quot;);
 * 
 * writer.writeStartElement(&quot;http://www.example.com&quot;, &quot;child&quot;);
 * 
 * //child attributes list...
 * writer.writeStartAttribute(&quot;http://www.example.com&quot;, &quot;att&quot;);
 * writer.writeValue(&quot;attValue&quot;);
 * writer.writeStartAttribute(&quot;http://www.example.com&quot;, &quot;emptyAtt&quot;);
 * writer.writeEndAttributes();
 * 
 * //child value as a purely streamed int array
 * writer.startArray(EventType.VALUE_INT, 10);
 * for(int i = 0; i &lt; 10; i++){
 *   writer.writeValue(i);
 * }
 * writer.endArray();
 * 
 * writer.writeComment(&quot;interleaved value type follow&quot;);
 * 
 * writer.writeValue(&quot;10 11 12 13&quot;);
 * 
 * writer.writeEndElement(); // child
 * writer.writeEndElement(); // root
 * writer.writeEndDocument();
 * </pre>
 * </code>
 * 
 * </p>
 * 
 * @author Gabriel Roldan (OpenGeo)
 * @version $Id$
 * @since 1.0
 */
public interface BxmlStreamWriter {

    /**
     * Returns a safe copy of the encoding options this writer is settled up with.
     * <p>
     * Modifications to the returned object state does not affect this writer behaviour.
     * </p>
     * 
     * @return an object representing the set of encoding options this writer operates with
     */
    public EncodingOptions getEncodingOptions();

    /**
     * Returns the event corresponding to the last write call.
     * <p>
     * For example, if the last write call was {@link #writeStartElement(String, String)
     * writeStartElement} it shall return {@link EventType#START_ELEMENT START_ELEMENT}, if it was
     * {@link #writeStartAttribute(String, String) writeStartAttribute}, {@link EventType#ATTRIBUTE
     * ATTRIBUTE}, etc.
     * </p>
     * 
     * @post {$return != null}
     * @return {@link EventType#NONE} if writing has not yet started (ie, writeStartDocument has not
     *         been called), the event for the last write operation otherwise.
     */
    public EventType getLastEvent();

    /**
     * @return {@code null} if no tag event has been written yet, START_ELEMENT or END_ELEMENT
     *         otherwise, depending on which of them was lately called
     */
    public EventType getLastTagEvent();

    /**
     * Returns the element's local name of the deepest currently open element.
     * 
     * @pre {getTagDeep() > 0}
     * @post {$return != null}
     * @return the name of the currently open element
     */
    public String getCurrentElementName();

    /**
     * Returns the element's local namespace of the deepest currently open element.
     * 
     * @pre {getTagDeep() > 0}
     * @post {$return != null}
     * @return the namespace URI of the currently open element
     */
    public String getCurrentElementNamespace();

    /**
     * @post {$return >= 0}
     * @return how deep is the current element tree
     */
    public int getTagDepth();

    /**
     * Closes this stream writer and releases any system resources associated with it, but does not
     * close the underlying output stream or any other output source it may be operating upon.
     * <p>
     * A call to this method implies a flush of any possible buffered writes before closing the
     * stream.
     * </p>
     * <p>
     * The first call to this method closes and releases resources. Subsequent calls shall take no
     * effect and return quitely. After the first call to this method any non query method (defined
     * as metadata retrieval methods in the class' javadoc) shall fail with an io exception.
     * </p>
     * 
     * @throws IOException
     *             if an I/O error occurs.
     */
    public void close() throws IOException;

    /**
     * Returns whether this stream writer and its underlying output source are open and able to
     * receive more write calls.
     * <p>
     * A {@code BxmlStreamWriter} should be open since its creation until {@link #close()} is
     * explicitly called by client code.
     * </p>
     * 
     * @return {@code true} if this stream is still open, {@code false} otherwise
     */
    public boolean isOpen();

    /**
     * Write any cached data to the underlying output mechanism.
     * 
     * @throws IOException
     */
    public void flush() throws IOException;

    /**
     * Test if the current event is of the given {@code type} and if the {@code namespaceUri} and
     * {@code localName} match the current namespace and name of the current event.
     * <p>
     * If either of the three arguments is {@code null} it is not tested. In other words, only non
     * null arguments are tested.
     * </p>
     * <p>
     * If either {@code namespaceUri} or {@code localName} is <strong>not</strong> {@code null},
     * they are compared to {@link #getCurrentElementNamespace()} or
     * {@link #getCurrentElementName()}, meaning {@link #getLastEvent()} can't be NONE nor
     * END_DOCUMENT.
     * </p>
     * <p>
     * If a test does not pass, an unchecked Exception is thrown to indicate there's a programming
     * error on the client code.
     * </p>
     * <p>
     * This method is a convenience utility for client code to make sure the parser is in a given
     * state before proceeding.
     * </p>
     * 
     * @pre {if( namespaceUri != null || localName != null ) getTagDeep() > 0}
     * @param type
     *            {@link EventType} to test it's the {@link #getLastEvent() last writing event}, or
     *            {@code null} if not to be tested
     * @param namespaceUri
     *            the namespace URI to test against the currently open element, {@code null} if not
     *            to be tested
     * @param localName
     *            the expected currently open element's local name, or {@code null} if not to be
     *            tested
     * @throws IllegalStateException
     */
    public void require(EventType type, String namespaceUri, String localName)
            throws IllegalStateException;

    /**
     * @pre {getLastEvent() == START_DOCUMENT}
     * @pre {namespaceUri != null AND schemaLocationUri != null}
     * @pre {namespaceUri != ""}
     * @param namespaceUri
     * @param schemaLocationUri
     */
    public void setSchemaLocation(String namespaceUri, String schemaLocationUri);

    /**
     * Initiates the encoding of an XML formatted document in the BXML encoding.
     * <p>
     * This should be the first non query method to call after a stream writer is created. The
     * implementation will use the call to this method to write down the document header and xml
     * declaration.
     * </p>
     * 
     * @pre {getLastEvent() IN (NONE, NAMESPACE_DECL)}
     * @post {getLastEvent() == START_DOCUMENT}
     * @throws IOException
     */
    public void writeStartDocument() throws IOException;

    /**
     * Finishes up encoding a BXML document.
     * <p>
     * Implementations shall use this method to write down the document's trailer token.
     * </p>
     * 
     * @pre {getTagDeep() == 0}
     * @post {getLastEvent() == END_DOCUMENT}
     * @throws IOException
     */
    public void writeEndDocument() throws IOException;

    /**
     * @pre {namespaceUri != null}
     * @pre {localName != null}
     * @post {getLastEvent() == START_ELEMENT}
     * @param namespaceUri
     *            the namespace uri for the element
     * @param localName
     *            the elements non qualified (local) name
     * @throws IOException
     */
    public void writeStartElement(final String namespaceUri, final String localName)
            throws IOException;

    /**
     * @pre {getLastEvent() != NONE}
     * @pre {qname != null}
     * @post {getLastEvent() == START_ELEMENT}
     * @param qname
     *            qualified element name. Only namespace and localName are relevant. Prefix, if
     *            present, is ignored. The currently mapped prefix for the namespace will be used in
     *            any case.
     * @throws IOException
     * @see #writeNamespace(String, String)
     */
    public void writeStartElement(final QName qname) throws IOException;

    /**
     * @pre {getTagDeep() > 0}
     * @pre { ( getLastEvent().isValue() AND getWrittenValueCount() == getValueLength() ) ||
     *      getLastEvent() IN ( START_ELEMENT, END_ELEMENT, ATTRIBUTES_END, COMMENT}
     * @post {getLastEvent() == END_ELEMENT}
     * @throws IOException
     */
    public void writeEndElement() throws IOException;

    /**
     * Starts writting an xml attribute.
     * <p>
     * Writting an attribute and its value is split in two parts. This method only stand for the
     * attribute declaration. Writting an attribute value is made after calling this method and is
     * optional. The attribute value can be done with any of the {@code writeValue} methods, even
     * using various {@code writeValue} calls for the same attribute.
     * </p>
     * <p>
     * After the last element attribute has been written, {@link #writeEndAttributes()} shall be
     * called to indicate the end of the attribute list.
     * </p>
     * Sample usage:
     * 
     * <pre>
     * &lt;code&gt;
     * &lt;b&gt;writer.startAttribute(&quot;&quot;, &quot;att1&quot;);&lt;/b&gt;
     * writer.writeValue(&quot;value1&quot;);
     * writer.writeValue(1);
     * writer.writeValue(2);
     * writer.writeValue(3);
     * &lt;b&gt;writer.startAttribute(&quot;&quot;, &quot;att2&quot;);&lt;/b&gt;
     * writer.writeValue(1.0L);
     * ...
     * writer.writeEndAttributes();
     * &lt;/code&gt;
     * </pre>
     * 
     * @pre {getLastEvent() IN (START_ELEMENT, ATTRIBUTE) }
     * @pre {namespaceUri != null}
     * @pre {localName != null}
     * @pre {getPrefix(namespaceUri != null}
     * @post {getLastEvent() == ATTRIBUTE}
     * @param namespaceUri
     *            not null
     * @param localName
     *            not null
     * @throws IOException
     * @see #writeEndAttributes()
     */
    public void writeStartAttribute(final String namespaceUri, final String localName)
            throws IOException;

    /**
     * Starts writing an xml attribute for the given qualified name.
     * 
     * @pre {qname != null}
     * @param qname
     *            a QName where to get the namespace uri and local name for the attribute
     * @throws IOException
     * @see #writeStartAttribute(String, String)
     */
    public void writeStartAttribute(final QName qname) throws IOException;

    /**
     * Indicates there are no more element attributes to encode for the current element.
     * <p>
     * This method is useful for the encoder to recognize (and write down the appropriate tokens)
     * the following potential call to some writeValue method corresponds to the element's contents
     * and not to the current attribute value.
     * </p>
     * Sample usage:
     * 
     * <pre>
     * &lt;code&gt;
     * writer.startElement(&quot;&quot;, &quot;element&quot;);
     * writer.startAttribute(&quot;&quot;, &quot;att1&quot;);
     * writer.writeValue(&quot;value1&quot;);
     * writer.startAttribute(&quot;&quot;, &quot;att2&quot;);
     * writer.writeValue(1.0L);
     * ...
     * &lt;b&gt;writer.writeEndAttributes();&lt;/b&gt;
     * writer.value(&quot;element value&quot;);
     * ...
     * &lt;/code&gt;
     * </pre>
     * 
     * @pre {getLastTagEvent() == START_ELEMENT}
     * @post {getLastEvent() == ATTRIBUTES_END}
     * @throws IOException
     */
    public void writeEndAttributes() throws IOException;

    /**
     * Writes a single string value down to the underlying xml output stream after a
     * {@link EventType#START_ELEMENT START_ELEMENT} or {@link EventType#ATTRIBUTE ATTRIBUTE} event,
     * or while already writing the content for any of those events (so the full content of an
     * element or attribute can be written in chunks just like for the {@code characters} method in
     * traditional SAX events).
     * 
     * @pre {value != null}
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      COMMENT)}
     * @post {getLastEvent() == VALUE_STRING}
     * @post {getValueLength() == 1}
     * @post {getWrittenValueCount() == 1}
     * @param value
     * @throws IOException
     */
    public void writeValue(final String value) throws IOException;

    /**
     * Same than {@link #writeValue(String)}, but the string is provided through the {@code length}
     * characters of the char array {@code chars}, from index {@code offset} inclusive.
     * 
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE )}
     * @pre {value != null}
     * @post {getLastEvent() == VALUE_STRING}
     * @post {getValueLength() == 1}
     * @post {getWrittenValueCount() == 1}
     * @param chars
     * @param offset
     * @param length
     * @throws IOException
     */
    public void writeValue(final char[] chars, final int offset, final int length)
            throws IOException;

    /**
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @post {getWrittenValueCount() = 1 + $pre:getWrittenValueCount()}
     * @post {getLastEvent() == VALUE_INT}
     * @param value
     * @throws IOException
     */
    public void writeValue(final int value) throws IOException;

    /**
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @post {getWrittenValueCount() = 1 + $pre:getWrittenValueCount()}
     * @post {getLastEvent() == VALUE_LONG}
     * @param value
     * @throws IOException
     */
    public void writeValue(final long value) throws IOException;

    /**
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @post {getWrittenValueCount() = 1 + $pre:getWrittenValueCount()}
     * @post {getLastEvent() == VALUE_FLOAT}
     * @param value
     * @throws IOException
     */
    public void writeValue(final float value) throws IOException;

    /**
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @post {getWrittenValueCount() = 1 + $pre:getWrittenValueCount()}
     * @post {getLastEvent() == VALUE_DOUBLE}
     * @param value
     * @throws IOException
     */
    public void writeValue(final double value) throws IOException;

    /**
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @post {getWrittenValueCount() = 1 + $pre:getWrittenValueCount()}
     * @post {getLastEvent() == VALUE_BOOL}
     * @param value
     * @throws IOException
     */
    public void writeValue(final boolean value) throws IOException;

    /**
     * Writes {@code length} values in the {@code value} array starting at index {@code offset}
     * inclusive as a primitive array of length {@code length}, or appends to the currently being
     * written array.
     * <p>
     * If an array is already being written, it shall be of the same primitive type. The
     * {@code length} values from the {@code value} array will be appended to the current array
     * value (most probably by directly writting them down to the underlying bxml output stream),
     * and the {@link #getWrittenValueCount() written value count} will be increased by
     * {@code length}.
     * </p>
     * <p>
     * If an array is not already being writting, the values will be written as an array of length
     * {@code length}
     * </p>
     * 
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @pre {if (true == isArrayInProgress()) getValueLength() >= getWrittenValueCount() + length}
     * @post {getValueLength() = isArrayInProgress()? $pre:getValueLength() : length}
     * @post {getWrittenValueCount == isArrayInProgress()? $pre:getWrittenValueLength() + length :
     *       length}}
     * @post {getLastEvent() == VALUE_BOOL}
     * @param value
     * @param offset
     * @param length
     * @throws IOException
     */
    public void writeValue(final boolean[] value, final int offset, final int length)
            throws IOException;

    /**
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @post {getWrittenValueCount() = 1 + $pre:getWrittenValueCount()}
     * @post {getLastEvent() == VALUE_INT}
     * @param value
     * @throws IOException
     */
    public void writeValue(final byte value) throws IOException;

    /**
     * Writes {@code length} values in the {@code value} array starting at index {@code offset}
     * inclusive as a primitive array of length {@code length}, or appends to the currently being
     * written array.
     * <p>
     * If an array is already being written, it shall be of the same primitive type. The
     * {@code length} values from the {@code value} array will be appended to the current array
     * value (most probably by directly writting them down to the underlying bxml output stream),
     * and the {@link #getWrittenValueCount() written value count} will be increased by
     * {@code length}.
     * </p>
     * <p>
     * If an array is not already being writting, the values will be written as an array of length
     * {@code length}
     * </p>
     * 
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @pre {if (true == isArrayInProgress()) getValueLength() >= getWrittenValueCount() + length}
     * @post {getValueLength() = isArrayInProgress()? $pre:getValueLength() : length}
     * @post {getWrittenValueCount == isArrayInProgress()? $pre:getWrittenValueLength() + length :
     *       length}}
     * @post {getLastEvent() == VALUE_BYTE}
     * @param value
     * @param offset
     * @param length
     * @throws IOException
     */
    public void writeValue(final byte[] value, final int offset, final int length)
            throws IOException;

    /**
     * Writes {@code length} values in the {@code value} array starting at index {@code offset}
     * inclusive as a primitive array of length {@code length}, or appends to the currently being
     * written array.
     * <p>
     * If an array is already being written, it shall be of the same primitive type. The
     * {@code length} values from the {@code value} array will be appended to the current array
     * value (most probably by directly writting them down to the underlying bxml output stream),
     * and the {@link #getWrittenValueCount() written value count} will be increased by
     * {@code length}.
     * </p>
     * <p>
     * If an array is not already being writting, the values will be written as an array of length
     * {@code length}
     * </p>
     * 
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @pre {if (true == isArrayInProgress()) getValueLength() >= getWrittenValueCount() + length}
     * @post {getValueLength() = isArrayInProgress()? $pre:getValueLength() : length}
     * @post {getWrittenValueCount == isArrayInProgress()? $pre:getWrittenValueLength() + length :
     *       length}}
     * @post {getLastEvent() == VALUE_INT}
     * @param value
     * @param offset
     * @param length
     * @throws IOException
     */
    public void writeValue(final int[] value, final int offset, final int length)
            throws IOException;

    /**
     * Writes {@code length} values in the {@code value} array starting at index {@code offset}
     * inclusive as a primitive array of length {@code length}, or appends to the currently being
     * written array.
     * <p>
     * If an array is already being written, it shall be of the same primitive type. The
     * {@code length} values from the {@code value} array will be appended to the current array
     * value (most probably by directly writting them down to the underlying bxml output stream),
     * and the {@link #getWrittenValueCount() written value count} will be increased by
     * {@code length}.
     * </p>
     * <p>
     * If an array is not already being writting, the values will be written as an array of length
     * {@code length}
     * </p>
     * 
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @pre {if (true == isArrayInProgress()) getValueLength() >= getWrittenValueCount() + length}
     * @post {getValueLength() = isArrayInProgress()? $pre:getValueLength() : length}
     * @post {getWrittenValueCount == isArrayInProgress()? $pre:getWrittenValueLength() + length :
     *       length}}
     * @post {getLastEvent() == VALUE_LONG}
     * @param value
     * @param offset
     * @param length
     * @throws IOException
     */
    public void writeValue(final long[] value, final int offset, final int length)
            throws IOException;

    /**
     * Writes {@code length} values in the {@code value} array starting at index {@code offset}
     * inclusive as a primitive array of length {@code length}, or appends to the currently being
     * written array.
     * <p>
     * If an array is already being written, it shall be of the same primitive type. The
     * {@code length} values from the {@code value} array will be appended to the current array
     * value (most probably by directly writting them down to the underlying bxml output stream),
     * and the {@link #getWrittenValueCount() written value count} will be increased by
     * {@code length}.
     * </p>
     * <p>
     * If an array is not already being writting, the values will be written as an array of length
     * {@code length}
     * </p>
     * 
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @pre {if (true == isArrayInProgress()) getValueLength() >= getWrittenValueCount() + length}
     * @post {getValueLength() = isArrayInProgress()? $pre:getValueLength() : length}
     * @post {getWrittenValueCount == isArrayInProgress()? $pre:getWrittenValueLength() + length :
     *       length}}
     * @post {getLastEvent() == VALUE_FLOAT}
     * @param value
     * @param offset
     * @param length
     * @throws IOException
     */
    public void writeValue(final float[] value, final int offset, final int length)
            throws IOException;

    /**
     * Writes {@code length} values in the {@code value} array starting at index {@code offset}
     * inclusive as a primitive array of length {@code length}, or appends to the currently being
     * written array.
     * <p>
     * If an array is already being written, it shall be of the same primitive type. The
     * {@code length} values from the {@code value} array will be appended to the current array
     * value (most probably by directly writting them down to the underlying bxml output stream),
     * and the {@link #getWrittenValueCount() written value count} will be increased by
     * {@code length}.
     * </p>
     * <p>
     * If an array is not already being writting, the values will be written as an array of length
     * {@code length}
     * </p>
     * 
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END, COMMENT)}
     * @pre {if (true == isArrayInProgress()) getValueLength() >= getWrittenValueCount() + length}
     * @post {getValueLength() = isArrayInProgress()? $pre:getValueLength() : length}
     * @post {getWrittenValueCount == isArrayInProgress()? $pre:getWrittenValueLength() + length :
     *       length}}
     * @post {getLastEvent() == VALUE_DOUBLE}
     * @param value
     * @param offset
     * @param length
     * @throws IOException
     */
    public void writeValue(final double[] value, int offset, int length) throws IOException;

    /**
     * Writes a comments block
     * <p>
     * The provided {@code commentContent} is the coalesced value of the equivalent XML comment
     * block (enclosed between {@code <!--} and {@code -->} tags.
     * </p>
     * 
     * @post {getLastEvent() == COMMENT}
     * @param commentContent
     *            the coallesced value of the comment section
     * @throws IOException
     */
    public void writeComment(String commentContent) throws IOException;

    /**
     * @pre {isArrayInProgress() == false}
     * @pre {valueType != null}
     * @pre {valueType.isValue() == true}
     * @pre {valueType != VALUE_STRING}
     * @pre {arrayLength >= 0}
     * @post {getLastEvent() == valueType}
     * @post {getWrittenValueCount() == 0}
     * @post {getValueLength() == arrayLength}
     * @pre {isArrayInProgress() == true}
     * @param valueType
     * @param arrayLength
     * @throws IOException
     */
    public void startArray(EventType valueType, int arrayLength) throws IOException;

    /**
     * @return whether the writer is inside a start/endArray loop
     */
    public boolean isArrayInProgress();

    /**
     * Needs to be called when done with startArray
     * 
     * @pre {isArrayInProgress() == true}
     * @pre {getLastEvent().isValue() == true}
     * @pre {getWrittenValueCount() == getValueLength()}
     * @post {getValueLength() == 0}
     * @post {getWrittenValueCount() == 0}
     * @pre {isArrayInProgress() == false}
     * @throws IOException
     */
    public void endArray() throws IOException;

    /**
     * Binds a URI to the default namespace for the current context.
     * <p>
     * This URI is bound in the scope of the current START_ELEMENT / END_ELEMENT pair. If this
     * method is called before a START_ELEMENT has been written the uri is bound in the root scope.
     * </p>
     * 
     * @pre {defaultNamespaceUri != null}
     * @pre {getLastEvent() IN (NONE, START_DOCUMENT , START_ELEMENT, NAMESPACE_DECL)}
     * @post {getLastEvent() == NAMESPACE_DECL}
     * @param defaultNamespaceUri
     *            the uri to bind to the default namespace, may be {@code null}
     */
    public void writeDefaultNamespace(String defaultNamespaceUri) throws IOException;

    /**
     * Returns the prefix the {@code uri} is bound to in the current context
     * 
     * @pre {uri != null}
     * @param uri
     *            the namespace URI
     * @return the prefix the {@code uri} is bound to or {@code null} if not bound
     */
    public String getPrefix(String uri);

    /**
     * Sets the prefix the uri is bound to.
     * <p>
     * This prefix is bound in the scope of the current START_ELEMENT / END_ELEMENT pair. If this
     * method is called before a START_ELEMENT has been written the prefix is bound in the root
     * scope.
     * </p>
     * 
     * @pre {uri != null}
     * @pre {getLastEvent() IN (NONE, START_DOCUMENT , START_ELEMENT, NAMESPACE_DECL)}
     * 
     * @param prefix
     *            the prefix to bind to the uri, may not be null
     * @param uri
     *            the uri to bind to the prefix, may be null
     * 
     * @return the prefix the {@code uri} is bound to or {@code null} if not bound
     */
    public void setPrefix(String prefix, String uri);

    /**
     * Writes a namespace declaration while at a START_ELEMENT event.
     * <p>
     * This prefix is bound in the scope of the current START_ELEMENT / END_ELEMENT pair (current
     * context). If this method is called before a START_ELEMENT has been written the prefix is
     * bound in the root scope.
     * </p>
     * 
     * @pre {getLastEvent() IN (START_DOCUMENT, START_ELEMENT, NAMESPACE_DECL)}
     * @pre {prefix != null}
     * @pre {namespaceUri != null}
     * @post {if(prefix == "xmlns"){getPrefix(namespaceUri) == null}else{getPrefix(namespaceUri) ==
     *       prefix}}
     * @post {getLastEvent() == NAMESPACE_DECL}
     * @param prefix
     * @param namespaceUri
     *            bounds prefix to this URI in the active context
     */
    public void writeNamespace(String defPrefix, String defUri) throws IOException;

    /**
     * @return {@code true} if writing a value as a string table reference is supported
     * @see #getStringTableReference(CharSequence)
     * @see #writeStringTableValue(long)
     */
    public boolean supportsStringTableValues();

    /**
     * Writes a String value as a StringTable entry reference, provided the given StringTable entry
     * reference identifier already exists.
     * <p>
     * The reference passed in as argument shall be previsously acquired through
     * {@link #getStringTableReference(CharSequence)}
     * </p>
     * 
     * @pre {supportsStringTableValues() == true}
     * @pre {getLastEvent().isValue() == true || getLastEvent() IN (START_ELEMENT, ATTRIBUTE,
     *      ATTRIBUTES_END )}
     * @post {getLastEvent() == VALUE_STRING}
     * @param stringTableEntryId
     *            the string table entry reference to write down as a value token.
     * @throws IOException
     * @see #getStringTableReference(CharSequence)
     */
    public void writeStringTableValue(final long stringTableEntryId) throws IOException;

    /**
     * Returns the StringTable entry identifier for a given string value, creating the StringTable
     * entry if needed.
     * <p>
     * Using a string table reference as an attribute or element content may significantly reduce
     * the final file size, and improve performance, for highly referenced and repetitive conent
     * values, as may be some attribute values that are repeated in every element of a given type.
     * </p>
     * <p>
     * To use a StringTable reference as an attribute or element value, the reference shall be
     * acquired previously to start the attribute or element token.
     * </p>
     * <p>
     * This separation between adding an entry to the string table and writting it down as a value
     * token is needed due to the streaming nature of this encoder. For example, if the entry index
     * is going to be used as the value of an attribute, the entry must exist before the
     * {@code writeStartAttribute(xxx)} method is called. The same occurs if a string table
     * reference needs to be used as an element's content token, the string table entry shall exist
     * before the corresponding {@code writeStartElement} method is called.
     * </p>
     * 
     * @pre {supportsStringTableValues() == true}
     * @pre {getLastEvent() IN (START_DOCUMENT, END_ELEMENT, START_ELEMENT, ATTRIBUTE,
     *      NAMESPACE_DECL)}
     * @pos {$return >= 0}
     * @param stringValue
     * @return the identifier for the given string value in the StringTable
     * @throws IOException
     */
    public long getStringTableReference(final CharSequence stringValue) throws IOException;

    /**
     * @pre {getLastEvent().isValue() == true}
     * @post {$return >= 0}
     * @return the length of the current value being written.
     */
    public long getValueLength();

    /**
     * @pre {getLastEvent().isValue() == true}
     * @post {$return >= 0}
     * @post {$return <= getValueLength()}
     * @return how many elements has already being written for the current value
     */
    public long getWrittenValueCount();

    /**
     * @param qName
     */
    public void setWriteAttributeValueAsStringTable(final String qName);

}

