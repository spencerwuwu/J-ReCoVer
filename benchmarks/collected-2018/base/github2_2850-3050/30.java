// https://searchcode.com/api/result/17109946/

/*
 * Copyright 1999-2004 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package javax.imageio;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.stream.ImageInputStream;

/**
 * An abstract superclass for parsing and decoding of images.  This
 * class must be subclassed by classes that read in images in the
 * context of the Java Image I/O framework.
 *
 * <p> <code>ImageReader</code> objects are normally instantiated by
 * the service provider interface (SPI) class for the specific format.
 * Service provider classes (e.g., instances of
 * <code>ImageReaderSpi</code>) are registered with the
 * <code>IIORegistry</code>, which uses them for format recognition
 * and presentation of available format readers and writers.
 *
 * <p> When an input source is set (using the <code>setInput</code>
 * method), it may be marked as "seek forward only".  This setting
 * means that images contained within the input source will only be
 * read in order, possibly allowing the reader to avoid caching
 * portions of the input containing data associated with images that
 * have been read previously.
 *
 * @see ImageWriter
 * @see javax.imageio.spi.IIORegistry
 * @see javax.imageio.spi.ImageReaderSpi
 *
 */
public abstract class ImageReader {

    /**
     * The <code>ImageReaderSpi</code> that instantiated this object,
     * or <code>null</code> if its identity is not known or none
     * exists.  By default it is initialized to <code>null</code>.
     */
    protected ImageReaderSpi originatingProvider;

    /**
     * The <code>ImageInputStream</code> or other
     * <code>Object</code> by <code>setInput</code> and retrieved
     * by <code>getInput</code>.  By default it is initialized to
     * <code>null</code>.
     */
    protected Object input = null;

    /**
     * <code>true</code> if the current input source has been marked
     * as allowing only forward seeking by <code>setInput</code>.  By
     * default, the value is <code>false</code>.
     *
     * @see #minIndex
     * @see #setInput
     */
    protected boolean seekForwardOnly = false;

    /**
     * <code>true</code> if the current input source has been marked
     * as allowing metadata to be ignored by <code>setInput</code>.
     * By default, the value is <code>false</code>.
     *
     * @see #setInput
     */
    protected boolean ignoreMetadata = false;

    /**
     * The smallest valid index for reading, initially 0.  When
     * <code>seekForwardOnly</code> is <code>true</code>, various methods
     * may throw an <code>IndexOutOfBoundsException</code> on an
     * attempt to access data associate with an image having a lower
     * index.
     *
     * @see #seekForwardOnly
     * @see #setInput
     */
    protected int minIndex = 0;

    /**
     * An array of <code>Locale</code>s which may be used to localize
     * warning messages, or <code>null</code> if localization is not
     * supported.
     */
    protected Locale[] availableLocales = null;

    /**
     * The current <code>Locale</code> to be used for localization, or
     * <code>null</code> if none has been set.
     */
    protected Locale locale = null;

    /**
     * A <code>List</code> of currently registered
     * <code>IIOReadWarningListener</code>s, initialized by default to
     * <code>null</code>, which is synonymous with an empty
     * <code>List</code>.
     */
    protected List<IIOReadWarningListener> warningListeners = null;

    /**
     * A <code>List</code> of the <code>Locale</code>s associated with
     * each currently registered <code>IIOReadWarningListener</code>,
     * initialized by default to <code>null</code>, which is
     * synonymous with an empty <code>List</code>.
     */
    protected List<Locale> warningLocales = null;

    /**
     * A <code>List</code> of currently registered
     * <code>IIOReadProgressListener</code>s, initialized by default
     * to <code>null</code>, which is synonymous with an empty
     * <code>List</code>.
     */
    protected List<IIOReadProgressListener> progressListeners = null;

    /**
     * A <code>List</code> of currently registered
     * <code>IIOReadUpdateListener</code>s, initialized by default to
     * <code>null</code>, which is synonymous with an empty
     * <code>List</code>.
     */
    protected List<IIOReadUpdateListener> updateListeners = null;

    /**
     * If <code>true</code>, the current read operation should be
     * aborted.
     */
    private boolean abortFlag = false;

    /**
     * Constructs an <code>ImageReader</code> and sets its
     * <code>originatingProvider</code> field to the supplied value.
     *
     * <p> Subclasses that make use of extensions should provide a
     * constructor with signature <code>(ImageReaderSpi,
     * Object)</code> in order to retrieve the extension object.  If
     * the extension object is unsuitable, an
     * <code>IllegalArgumentException</code> should be thrown.
     *
     * @param originatingProvider the <code>ImageReaderSpi</code> that is
     * invoking this constructor, or <code>null</code>.
     */
    protected ImageReader(ImageReaderSpi originatingProvider) {
        this.originatingProvider = originatingProvider;
    }

    /**
     * Returns a <code>String</code> identifying the format of the
     * input source.
     *
     * <p> The default implementation returns
     * <code>originatingProvider.getFormatNames()[0]</code>.
     * Implementations that may not have an originating service
     * provider, or which desire a different naming policy should
     * override this method.
     *
     * @exception IOException if an error occurs reading the
     * information from the input source.
     *
     * @return the format name, as a <code>String</code>.
     */
    public String getFormatName() throws IOException {
        return originatingProvider.getFormatNames()[0];
    }

    /**
     * Returns the <code>ImageReaderSpi</code> that was passed in on
     * the constructor.  Note that this value may be <code>null</code>.
     *
     * @return an <code>ImageReaderSpi</code>, or <code>null</code>.
     *
     * @see ImageReaderSpi
     */
    public ImageReaderSpi getOriginatingProvider() {
        return originatingProvider;
    }

    /**
     * Sets the input source to use to the given
     * <code>ImageInputStream</code> or other <code>Object</code>.
     * The input source must be set before any of the query or read
     * methods are used.  If <code>input</code> is <code>null</code>,
     * any currently set input source will be removed.  In any case,
     * the value of <code>minIndex</code> will be initialized to 0.
     *
     * <p> The <code>seekForwardOnly</code> parameter controls whether
     * the value returned by <code>getMinIndex</code> will be
     * increased as each image (or thumbnail, or image metadata) is
     * read.  If <code>seekForwardOnly</code> is true, then a call to
     * <code>read(index)</code> will throw an
     * <code>IndexOutOfBoundsException</code> if <code>index &lt
     * this.minIndex</code>; otherwise, the value of
     * <code>minIndex</code> will be set to <code>index</code>.  If
     * <code>seekForwardOnly</code> is <code>false</code>, the value of
     * <code>minIndex</code> will remain 0 regardless of any read
     * operations.
     *
     * <p> The <code>ignoreMetadata</code> parameter, if set to
     * <code>true</code>, allows the reader to disregard any metadata
     * encountered during the read.  Subsequent calls to the
     * <code>getStreamMetadata</code> and
     * <code>getImageMetadata</code> methods may return
     * <code>null</code>, and an <code>IIOImage</code> returned from
     * <code>readAll</code> may return <code>null</code> from their
     * <code>getMetadata</code> method.  Setting this parameter may
     * allow the reader to work more efficiently.  The reader may
     * choose to disregard this setting and return metadata normally.
     *
     * <p> Subclasses should take care to remove any cached
     * information based on the previous stream, such as header
     * information or partially decoded image data.
     *
     * <p> Use of a general <code>Object</code> other than an
     * <code>ImageInputStream</code> is intended for readers that
     * interact directly with a capture device or imaging protocol.
     * The set of legal classes is advertised by the reader's service
     * provider's <code>getInputTypes</code> method; most readers
     * will return a single-element array containing only
     * <code>ImageInputStream.class</code> to indicate that they
     * accept only an <code>ImageInputStream</code>.
     *
     * <p> The default implementation checks the <code>input</code>
     * argument against the list returned by
     * <code>originatingProvider.getInputTypes()</code> and fails
     * if the argument is not an instance of one of the classes
     * in the list.  If the originating provider is set to
     * <code>null</code>, the input is accepted only if it is an
     * <code>ImageInputStream</code>.
     *
     * @param input the <code>ImageInputStream</code> or other
     * <code>Object</code> to use for future decoding.
     * @param seekForwardOnly if <code>true</code>, images and metadata
     * may only be read in ascending order from this input source.
     * @param ignoreMetadata if <code>true</code>, metadata
     * may be ignored during reads.
     *
     * @exception IllegalArgumentException if <code>input</code> is
     * not an instance of one of the classes returned by the
     * originating service provider's <code>getInputTypes</code>
     * method, or is not an <code>ImageInputStream</code>.
     *
     * @see ImageInputStream
     * @see #getInput
     * @see javax.imageio.spi.ImageReaderSpi#getInputTypes
     */
    public void setInput(Object input,
                         boolean seekForwardOnly,
                         boolean ignoreMetadata) {
        if (input != null) {
            boolean found = false;
            if (originatingProvider != null) {
                Class[] classes = originatingProvider.getInputTypes();
                for (int i = 0; i < classes.length; i++) {
                    if (classes[i].isInstance(input)) {
                        found = true;
                        break;
                    }
                }
            } else {
                if (input instanceof ImageInputStream) {
                    found = true;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Incorrect input type!");
            }

            this.seekForwardOnly = seekForwardOnly;
            this.ignoreMetadata = ignoreMetadata;
            this.minIndex = 0;
        }

        this.input = input;
    }

    /**
     * Sets the input source to use to the given
     * <code>ImageInputStream</code> or other <code>Object</code>.
     * The input source must be set before any of the query or read
     * methods are used.  If <code>input</code> is <code>null</code>,
     * any currently set input source will be removed.  In any case,
     * the value of <code>minIndex</code> will be initialized to 0.
     *
     * <p> The <code>seekForwardOnly</code> parameter controls whether
     * the value returned by <code>getMinIndex</code> will be
     * increased as each image (or thumbnail, or image metadata) is
     * read.  If <code>seekForwardOnly</code> is true, then a call to
     * <code>read(index)</code> will throw an
     * <code>IndexOutOfBoundsException</code> if <code>index &lt
     * this.minIndex</code>; otherwise, the value of
     * <code>minIndex</code> will be set to <code>index</code>.  If
     * <code>seekForwardOnly</code> is <code>false</code>, the value of
     * <code>minIndex</code> will remain 0 regardless of any read
     * operations.
     *
     * <p> This method is equivalent to <code>setInput(input,
     * seekForwardOnly, false)</code>.
     *
     * @param input the <code>ImageInputStream</code> or other
     * <code>Object</code> to use for future decoding.
     * @param seekForwardOnly if <code>true</code>, images and metadata
     * may only be read in ascending order from this input source.
     *
     * @exception IllegalArgumentException if <code>input</code> is
     * not an instance of one of the classes returned by the
     * originating service provider's <code>getInputTypes</code>
     * method, or is not an <code>ImageInputStream</code>.
     *
     * @see #getInput
     */
    public void setInput(Object input,
                         boolean seekForwardOnly) {
        setInput(input, seekForwardOnly, false);
    }

    /**
     * Sets the input source to use to the given
     * <code>ImageInputStream</code> or other <code>Object</code>.
     * The input source must be set before any of the query or read
     * methods are used.  If <code>input</code> is <code>null</code>,
     * any currently set input source will be removed.  In any case,
     * the value of <code>minIndex</code> will be initialized to 0.
     *
     * <p> This method is equivalent to <code>setInput(input, false,
     * false)</code>.
     *
     * @param input the <code>ImageInputStream</code> or other
     * <code>Object</code> to use for future decoding.
     *
     * @exception IllegalArgumentException if <code>input</code> is
     * not an instance of one of the classes returned by the
     * originating service provider's <code>getInputTypes</code>
     * method, or is not an <code>ImageInputStream</code>.
     *
     * @see #getInput
     */
    public void setInput(Object input) {
        setInput(input, false, false);
    }

    /**
     * Returns the <code>ImageInputStream</code> or other
     * <code>Object</code> previously set as the input source.  If the
     * input source has not been set, <code>null</code> is returned.
     *
     * @return the <code>Object</code> that will be used for future
     * decoding, or <code>null</code>.
     *
     * @see ImageInputStream
     * @see #setInput
     */
    public Object getInput() {
        return input;
    }

    /**
     * Returns <code>true</code> if the current input source has been
     * marked as seek forward only by passing <code>true</code> as the
     * <code>seekForwardOnly</code> argument to the
     * <code>setInput</code> method.
     *
     * @return <code>true</code> if the input source is seek forward
     * only.
     *
     * @see #setInput
     */
    public boolean isSeekForwardOnly() {
        return seekForwardOnly;
    }

    /**
     * Returns <code>true</code> if the current input source has been
     * marked as allowing metadata to be ignored by passing
     * <code>true</code> as the <code>ignoreMetadata</code> argument
     * to the <code>setInput</code> method.
     *
     * @return <code>true</code> if the metadata may be ignored.
     *
     * @see #setInput
     */
    public boolean isIgnoringMetadata() {
        return ignoreMetadata;
    }

    /**
     * Returns the lowest valid index for reading an image, thumbnail,
     * or image metadata.  If <code>seekForwardOnly()</code> is
     * <code>false</code>, this value will typically remain 0,
     * indicating that random access is possible.  Otherwise, it will
     * contain the value of the most recently accessed index, and
     * increase in a monotonic fashion.
     *
     * @return the minimum legal index for reading.
     */
    public int getMinIndex() {
        return minIndex;
    }

    // Localization

    /**
     * Returns an array of <code>Locale</code>s that may be used to
     * localize warning listeners and compression settings.  A return
     * value of <code>null</code> indicates that localization is not
     * supported.
     *
     * <p> The default implementation returns a clone of the
     * <code>availableLocales</code> instance variable if it is
     * non-<code>null</code>, or else returns <code>null</code>.
     *
     * @return an array of <code>Locale</code>s that may be used as
     * arguments to <code>setLocale</code>, or <code>null</code>.
     */
    public Locale[] getAvailableLocales() {
        if (availableLocales == null) {
            return null;
        } else {
            return (Locale[])availableLocales.clone();
        }
    }

    /**
     * Sets the current <code>Locale</code> of this
     * <code>ImageReader</code> to the given value.  A value of
     * <code>null</code> removes any previous setting, and indicates
     * that the reader should localize as it sees fit.
     *
     * @param locale the desired <code>Locale</code>, or
     * <code>null</code>.
     *
     * @exception IllegalArgumentException if <code>locale</code> is
     * non-<code>null</code> but is not one of the values returned by
     * <code>getAvailableLocales</code>.
     *
     * @see #getLocale
     */
    public void setLocale(Locale locale) {
        if (locale != null) {
            Locale[] locales = getAvailableLocales();
            boolean found = false;
            if (locales != null) {
                for (int i = 0; i < locales.length; i++) {
                    if (locale.equals(locales[i])) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Invalid locale!");
            }
        }
        this.locale = locale;
    }

    /**
     * Returns the currently set <code>Locale</code>, or
     * <code>null</code> if none has been set.
     *
     * @return the current <code>Locale</code>, or <code>null</code>.
     *
     * @see #setLocale
     */
    public Locale getLocale() {
        return locale;
    }

    // Image queries

    /**
     * Returns the number of images, not including thumbnails, available
     * from the current input source.
     *
     * <p> Note that some image formats (such as animated GIF) do not
     * specify how many images are present in the stream.  Thus
     * determining the number of images will require the entire stream
     * to be scanned and may require memory for buffering.  If images
     * are to be processed in order, it may be more efficient to
     * simply call <code>read</code> with increasing indices until an
     * <code>IndexOutOfBoundsException</code> is thrown to indicate
     * that no more images are available.  The
     * <code>allowSearch</code> parameter may be set to
     * <code>false</code> to indicate that an exhaustive search is not
     * desired; the return value will be <code>-1</code> to indicate
     * that a search is necessary.  If the input has been specified
     * with <code>seekForwardOnly</code> set to <code>true</code>,
     * this method throws an <code>IllegalStateException</code> if
     * <code>allowSearch</code> is set to <code>true</code>.
     *
     * @param allowSearch if <code>true</code>, the true number of
     * images will be returned even if a search is required.  If
     * <code>false</code>, the reader may return <code>-1</code>
     * without performing the search.
     *
     * @return the number of images, as an <code>int</code>, or
     * <code>-1</code> if <code>allowSearch</code> is
     * <code>false</code> and a search would be required.
     *
     * @exception IllegalStateException if the input source has not been set,
     * or if the input has been specified with <code>seekForwardOnly</code>
     * set to <code>true</code>.
     * @exception IOException if an error occurs reading the
     * information from the input source.
     *
     * @see #setInput
     */
    public abstract int getNumImages(boolean allowSearch) throws IOException;

    /**
     * Returns the width in pixels of the given image within the input
     * source.
     *
     * <p> If the image can be rendered to a user-specified size, then
     * this method returns the default width.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return the width of the image, as an <code>int</code>.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs reading the width
     * information from the input source.
     */
    public abstract int getWidth(int imageIndex) throws IOException;

    /**
     * Returns the height in pixels of the given image within the
     * input source.
     *
     * <p> If the image can be rendered to a user-specified size, then
     * this method returns the default height.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return the height of the image, as an <code>int</code>.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs reading the height
     * information from the input source.
     */
    public abstract int getHeight(int imageIndex) throws IOException;

    /**
     * Returns <code>true</code> if the storage format of the given
     * image places no inherent impediment on random access to pixels.
     * For most compressed formats, such as JPEG, this method should
     * return <code>false</code>, as a large section of the image in
     * addition to the region of interest may need to be decoded.
     *
     * <p> This is merely a hint for programs that wish to be
     * efficient; all readers must be able to read arbitrary regions
     * as specified in an <code>ImageReadParam</code>.
     *
     * <p> Note that formats that return <code>false</code> from
     * this method may nonetheless allow tiling (<i>e.g.</i> Restart
     * Markers in JPEG), and random access will likely be reasonably
     * efficient on tiles.  See {@link #isImageTiled
     * <code>isImageTiled</code>}.
     *
     * <p> A reader for which all images are guaranteed to support
     * easy random access, or are guaranteed not to support easy
     * random access, may return <code>true</code> or
     * <code>false</code> respectively without accessing any image
     * data.  In such cases, it is not necessary to throw an exception
     * even if no input source has been set or the image index is out
     * of bounds.
     *
     * <p> The default implementation returns <code>false</code>.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return <code>true</code> if reading a region of interest of
     * the given image is likely to be efficient.
     *
     * @exception IllegalStateException if an input source is required
     * to determine the return value, but none has been set.
     * @exception IndexOutOfBoundsException if an image must be
     * accessed to determine the return value, but the supplied index
     * is out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public boolean isRandomAccessEasy(int imageIndex) throws IOException {
        return false;
    }

    /**
     * Returns the aspect ratio of the given image (that is, its width
     * divided by its height) as a <code>float</code>.  For images
     * that are inherently resizable, this method provides a way to
     * determine the appropriate width given a deired height, or vice
     * versa.  For non-resizable images, the true width and height
     * are used.
     *
     * <p> The default implementation simply returns
     * <code>(float)getWidth(imageIndex)/getHeight(imageIndex)</code>.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return a <code>float</code> indicating the aspect ratio of the
     * given image.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public float getAspectRatio(int imageIndex) throws IOException {
        return (float)getWidth(imageIndex)/getHeight(imageIndex);
    }

    /**
     * Returns an <code>ImageTypeSpecifier</code> indicating the
     * <code>SampleModel</code> and <code>ColorModel</code> which most
     * closely represents the "raw" internal format of the image.  For
     * example, for a JPEG image the raw type might have a YCbCr color
     * space even though the image would conventionally be transformed
     * into an RGB color space prior to display.  The returned value
     * should also be included in the list of values returned by
     * <code>getImageTypes</code>.
     *
     * <p> The default implementation simply returns the first entry
     * from the list provided by <code>getImageType</code>.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return an <code>ImageTypeSpecifier</code>.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs reading the format
     * information from the input source.
     */
    public ImageTypeSpecifier getRawImageType(int imageIndex)
        throws IOException {
        return (ImageTypeSpecifier)getImageTypes(imageIndex).next();
    }

    /**
     * Returns an <code>Iterator</code> containing possible image
     * types to which the given image may be decoded, in the form of
     * <code>ImageTypeSpecifiers</code>s.  At least one legal image
     * type will be returned.
     *
     * <p> The first element of the iterator should be the most
     * "natural" type for decoding the image with as little loss as
     * possible.  For example, for a JPEG image the first entry should
     * be an RGB image, even though the image data is stored
     * internally in a YCbCr color space.
     *
     * @param imageIndex the index of the image to be
     * <code>retrieved</code>.
     *
     * @return an <code>Iterator</code> containing at least one
     * <code>ImageTypeSpecifier</code> representing suggested image
     * types for decoding the current given image.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs reading the format
     * information from the input source.
     *
     * @see ImageReadParam#setDestination(BufferedImage)
     * @see ImageReadParam#setDestinationType(ImageTypeSpecifier)
     */
    public abstract Iterator<ImageTypeSpecifier>
        getImageTypes(int imageIndex) throws IOException;

    /**
     * Returns a default <code>ImageReadParam</code> object
     * appropriate for this format.  All subclasses should define a
     * set of default values for all parameters and return them with
     * this call.  This method may be called before the input source
     * is set.
     *
     * <p> The default implementation constructs and returns a new
     * <code>ImageReadParam</code> object that does not allow source
     * scaling (<i>i.e.</i>, it returns <code>new
     * ImageReadParam()</code>.
     *
     * @return an <code>ImageReadParam</code> object which may be used
     * to control the decoding process using a set of default settings.
     */
    public ImageReadParam getDefaultReadParam() {
        return new ImageReadParam();
    }

    /**
     * Returns an <code>IIOMetadata</code> object representing the
     * metadata associated with the input source as a whole (i.e., not
     * associated with any particular image), or <code>null</code> if
     * the reader does not support reading metadata, is set to ignore
     * metadata, or if no metadata is available.
     *
     * @return an <code>IIOMetadata</code> object, or <code>null</code>.
     *
     * @exception IOException if an error occurs during reading.
     */
    public abstract IIOMetadata getStreamMetadata() throws IOException;

    /**
     * Returns an <code>IIOMetadata</code> object representing the
     * metadata associated with the input source as a whole (i.e.,
     * not associated with any particular image).  If no such data
     * exists, <code>null</code> is returned.
     *
     * <p> The resuting metadata object is only responsible for
     * returning documents in the format named by
     * <code>formatName</code>.  Within any documents that are
     * returned, only nodes whose names are members of
     * <code>nodeNames</code> are required to be returned.  In this
     * way, the amount of metadata processing done by the reader may
     * be kept to a minimum, based on what information is actually
     * needed.
     *
     * <p> If <code>formatName</code> is not the name of a supported
     * metadata format, <code>null</code> is returned.
     *
     * <p> In all cases, it is legal to return a more capable metadata
     * object than strictly necessary.  The format name and node names
     * are merely hints that may be used to reduce the reader's
     * workload.
     *
     * <p> The default implementation simply returns the result of
     * calling <code>getStreamMetadata()</code>, after checking that
     * the format name is supported.  If it is not,
     * <code>null</code> is returned.
     *
     * @param formatName a metadata format name that may be used to retrieve
     * a document from the returned <code>IIOMetadata</code> object.
     * @param nodeNames a <code>Set</code> containing the names of
     * nodes that may be contained in a retrieved document.
     *
     * @return an <code>IIOMetadata</code> object, or <code>null</code>.
     *
     * @exception IllegalArgumentException if <code>formatName</code>
     * is <code>null</code>.
     * @exception IllegalArgumentException if <code>nodeNames</code>
     * is <code>null</code>.
     * @exception IOException if an error occurs during reading.
     */
    public IIOMetadata getStreamMetadata(String formatName,
                                         Set<String> nodeNames)
        throws IOException
    {
        return getMetadata(formatName, nodeNames, true, 0);
    }

    private IIOMetadata getMetadata(String formatName,
                                    Set nodeNames,
                                    boolean wantStream,
                                    int imageIndex) throws IOException {
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (nodeNames == null) {
            throw new IllegalArgumentException("nodeNames == null!");
        }
        IIOMetadata metadata =
            wantStream
            ? getStreamMetadata()
            : getImageMetadata(imageIndex);
        if (metadata != null) {
            if (metadata.isStandardMetadataFormatSupported() &&
                formatName.equals
                (IIOMetadataFormatImpl.standardMetadataFormatName)) {
                return metadata;
            }
            String nativeName = metadata.getNativeMetadataFormatName();
            if (nativeName != null && formatName.equals(nativeName)) {
                return metadata;
            }
            String[] extraNames = metadata.getExtraMetadataFormatNames();
            if (extraNames != null) {
                for (int i = 0; i < extraNames.length; i++) {
                    if (formatName.equals(extraNames[i])) {
                        return metadata;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns an <code>IIOMetadata</code> object containing metadata
     * associated with the given image, or <code>null</code> if the
     * reader does not support reading metadata, is set to ignore
     * metadata, or if no metadata is available.
     *
     * @param imageIndex the index of the image whose metadata is to
     * be retrieved.
     *
     * @return an <code>IIOMetadata</code> object, or
     * <code>null</code>.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public abstract IIOMetadata getImageMetadata(int imageIndex)
        throws IOException;

    /**
     * Returns an <code>IIOMetadata</code> object representing the
     * metadata associated with the given image, or <code>null</code>
     * if the reader does not support reading metadata or none
     * is available.
     *
     * <p> The resuting metadata object is only responsible for
     * returning documents in the format named by
     * <code>formatName</code>.  Within any documents that are
     * returned, only nodes whose names are members of
     * <code>nodeNames</code> are required to be returned.  In this
     * way, the amount of metadata processing done by the reader may
     * be kept to a minimum, based on what information is actually
     * needed.
     *
     * <p> If <code>formatName</code> is not the name of a supported
     * metadata format, <code>null</code> may be returned.
     *
     * <p> In all cases, it is legal to return a more capable metadata
     * object than strictly necessary.  The format name and node names
     * are merely hints that may be used to reduce the reader's
     * workload.
     *
     * <p> The default implementation simply returns the result of
     * calling <code>getImageMetadata(imageIndex)</code>, after
     * checking that the format name is supported.  If it is not,
     * <code>null</code> is returned.
     *
     * @param imageIndex the index of the image whose metadata is to
     * be retrieved.
     * @param formatName a metadata format name that may be used to retrieve
     * a document from the returned <code>IIOMetadata</code> object.
     * @param nodeNames a <code>Set</code> containing the names of
     * nodes that may be contained in a retrieved document.
     *
     * @return an <code>IIOMetadata</code> object, or <code>null</code>.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IllegalArgumentException if <code>formatName</code>
     * is <code>null</code>.
     * @exception IllegalArgumentException if <code>nodeNames</code>
     * is <code>null</code>.
     * @exception IOException if an error occurs during reading.
     */
    public IIOMetadata getImageMetadata(int imageIndex,
                                        String formatName,
                                        Set<String> nodeNames)
        throws IOException {
        return getMetadata(formatName, nodeNames, false, imageIndex);
    }

    /**
     * Reads the image indexed by <code>imageIndex</code> and returns
     * it as a complete <code>BufferedImage</code>, using a default
     * <code>ImageReadParam</code>.  This is a convenience method
     * that calls <code>read(imageIndex, null)</code>.
     *
     * <p> The image returned will be formatted according to the first
     * <code>ImageTypeSpecifier</code> returned from
     * <code>getImageTypes</code>.
     *
     * <p> Any registered <code>IIOReadProgressListener</code> objects
     * will be notified by calling their <code>imageStarted</code>
     * method, followed by calls to their <code>imageProgress</code>
     * method as the read progresses.  Finally their
     * <code>imageComplete</code> method will be called.
     * <code>IIOReadUpdateListener</code> objects may be updated at
     * other times during the read as pixels are decoded.  Finally,
     * <code>IIOReadWarningListener</code> objects will receive
     * notification of any non-fatal warnings that occur during
     * decoding.
     *
     * @param imageIndex the index of the image to be retrieved.
     *
     * @return the desired portion of the image as a
     * <code>BufferedImage</code>.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public BufferedImage read(int imageIndex) throws IOException {
        return read(imageIndex, null);
    }

    /**
     * Reads the image indexed by <code>imageIndex</code> and returns
     * it as a complete <code>BufferedImage</code>, using a supplied
     * <code>ImageReadParam</code>.
     *
     * <p> The actual <code>BufferedImage</code> returned will be
     * chosen using the algorithm defined by the
     * <code>getDestination</code> method.
     *
     * <p> Any registered <code>IIOReadProgressListener</code> objects
     * will be notified by calling their <code>imageStarted</code>
     * method, followed by calls to their <code>imageProgress</code>
     * method as the read progresses.  Finally their
     * <code>imageComplete</code> method will be called.
     * <code>IIOReadUpdateListener</code> objects may be updated at
     * other times during the read as pixels are decoded.  Finally,
     * <code>IIOReadWarningListener</code> objects will receive
     * notification of any non-fatal warnings that occur during
     * decoding.
     *
     * <p> The set of source bands to be read and destination bands to
     * be written is determined by calling <code>getSourceBands</code>
     * and <code>getDestinationBands</code> on the supplied
     * <code>ImageReadParam</code>.  If the lengths of the arrays
     * returned by these methods differ, the set of source bands
     * contains an index larger that the largest available source
     * index, or the set of destination bands contains an index larger
     * than the largest legal destination index, an
     * <code>IllegalArgumentException</code> is thrown.
     *
     * <p> If the supplied <code>ImageReadParam</code> contains
     * optional setting values not supported by this reader (<i>e.g.</i>
     * source render size or any format-specific settings), they will
     * be ignored.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param param an <code>ImageReadParam</code> used to control
     * the reading process, or <code>null</code>.
     *
     * @return the desired portion of the image as a
     * <code>BufferedImage</code>.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IllegalArgumentException if the set of source and
     * destination bands specified by
     * <code>param.getSourceBands</code> and
     * <code>param.getDestinationBands</code> differ in length or
     * include indices that are out of bounds.
     * @exception IllegalArgumentException if the resulting image would
     * have a width or height less than 1.
     * @exception IOException if an error occurs during reading.
     */
    public abstract BufferedImage read(int imageIndex, ImageReadParam param)
        throws IOException;

    /**
     * Reads the image indexed by <code>imageIndex</code> and returns
     * an <code>IIOImage</code> containing the image, thumbnails, and
     * associated image metadata, using a supplied
     * <code>ImageReadParam</code>.
     *
     * <p> The actual <code>BufferedImage</code> referenced by the
     * returned <code>IIOImage</code> will be chosen using the
     * algorithm defined by the <code>getDestination</code> method.
     *
     * <p> Any registered <code>IIOReadProgressListener</code> objects
     * will be notified by calling their <code>imageStarted</code>
     * method, followed by calls to their <code>imageProgress</code>
     * method as the read progresses.  Finally their
     * <code>imageComplete</code> method will be called.
     * <code>IIOReadUpdateListener</code> objects may be updated at
     * other times during the read as pixels are decoded.  Finally,
     * <code>IIOReadWarningListener</code> objects will receive
     * notification of any non-fatal warnings that occur during
     * decoding.
     *
     * <p> The set of source bands to be read and destination bands to
     * be written is determined by calling <code>getSourceBands</code>
     * and <code>getDestinationBands</code> on the supplied
     * <code>ImageReadParam</code>.  If the lengths of the arrays
     * returned by these methods differ, the set of source bands
     * contains an index larger that the largest available source
     * index, or the set of destination bands contains an index larger
     * than the largest legal destination index, an
     * <code>IllegalArgumentException</code> is thrown.
     *
     * <p> Thumbnails will be returned in their entirety regardless of
     * the region settings.
     *
     * <p> If the supplied <code>ImageReadParam</code> contains
     * optional setting values not supported by this reader (<i>e.g.</i>
     * source render size or any format-specific settings), those
     * values will be ignored.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param param an <code>ImageReadParam</code> used to control
     * the reading process, or <code>null</code>.
     *
     * @return an <code>IIOImage</code> containing the desired portion
     * of the image, a set of thumbnails, and associated image
     * metadata.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IllegalArgumentException if the set of source and
     * destination bands specified by
     * <code>param.getSourceBands</code> and
     * <code>param.getDestinationBands</code> differ in length or
     * include indices that are out of bounds.
     * @exception IllegalArgumentException if the resulting image
     * would have a width or height less than 1.
     * @exception IOException if an error occurs during reading.
     */
    public IIOImage readAll(int imageIndex, ImageReadParam param)
        throws IOException {
        if (imageIndex < getMinIndex()) {
            throw new IndexOutOfBoundsException("imageIndex < getMinIndex()!");
        }

        BufferedImage im = read(imageIndex, param);

        ArrayList thumbnails = null;
        int numThumbnails = getNumThumbnails(imageIndex);
        if (numThumbnails > 0) {
            thumbnails = new ArrayList();
            for (int j = 0; j < numThumbnails; j++) {
                thumbnails.add(readThumbnail(imageIndex, j));
            }
        }

        IIOMetadata metadata = getImageMetadata(imageIndex);
        return new IIOImage(im, thumbnails, metadata);
    }

    /**
     * Returns an <code>Iterator</code> containing all the images,
     * thumbnails, and metadata, starting at the index given by
     * <code>getMinIndex</code>, from the input source in the form of
     * <code>IIOImage</code> objects.  An <code>Iterator</code>
     * containing <code>ImageReadParam</code> objects is supplied; one
     * element is consumed for each image read from the input source
     * until no more images are available.  If the read param
     * <code>Iterator</code> runs out of elements, but there are still
     * more images available from the input source, default read
     * params are used for the remaining images.
     *
     * <p> If <code>params</code> is <code>null</code>, a default read
     * param will be used for all images.
     *
     * <p> The actual <code>BufferedImage</code> referenced by the
     * returned <code>IIOImage</code> will be chosen using the
     * algorithm defined by the <code>getDestination</code> method.
     *
     * <p> Any registered <code>IIOReadProgressListener</code> objects
     * will be notified by calling their <code>sequenceStarted</code>
     * method once.  Then, for each image decoded, there will be a
     * call to <code>imageStarted</code>, followed by calls to
     * <code>imageProgress</code> as the read progresses, and finally
     * to <code>imageComplete</code>.  The
     * <code>sequenceComplete</code> method will be called after the
     * last image has been decoded.
     * <code>IIOReadUpdateListener</code> objects may be updated at
     * other times during the read as pixels are decoded.  Finally,
     * <code>IIOReadWarningListener</code> objects will receive
     * notification of any non-fatal warnings that occur during
     * decoding.
     *
     * <p> The set of source bands to be read and destination bands to
     * be written is determined by calling <code>getSourceBands</code>
     * and <code>getDestinationBands</code> on the supplied
     * <code>ImageReadParam</code>.  If the lengths of the arrays
     * returned by these methods differ, the set of source bands
     * contains an index larger that the largest available source
     * index, or the set of destination bands contains an index larger
     * than the largest legal destination index, an
     * <code>IllegalArgumentException</code> is thrown.
     *
     * <p> Thumbnails will be returned in their entirety regardless of the
     * region settings.
     *
     * <p> If any of the supplied <code>ImageReadParam</code>s contain
     * optional setting values not supported by this reader (<i>e.g.</i>
     * source render size or any format-specific settings), they will
     * be ignored.
     *
     * @param params an <code>Iterator</code> containing
     * <code>ImageReadParam</code> objects.
     *
     * @return an <code>Iterator</code> representing the
     * contents of the input source as <code>IIOImage</code>s.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IllegalArgumentException if any
     * non-<code>null</code> element of <code>params</code> is not an
     * <code>ImageReadParam</code>.
     * @exception IllegalArgumentException if the set of source and
     * destination bands specified by
     * <code>param.getSourceBands</code> and
     * <code>param.getDestinationBands</code> differ in length or
     * include indices that are out of bounds.
     * @exception IllegalArgumentException if a resulting image would
     * have a width or height less than 1.
     * @exception IOException if an error occurs during reading.
     *
     * @see ImageReadParam
     * @see IIOImage
     */
    public Iterator<IIOImage>
        readAll(Iterator<? extends ImageReadParam> params)
        throws IOException
    {
        List output = new ArrayList();

        int imageIndex = getMinIndex();

        // Inform IIOReadProgressListeners we're starting a sequence
        processSequenceStarted(imageIndex);

        while (true) {
            // Inform IIOReadProgressListeners and IIOReadUpdateListeners
            // that we're starting a new image

            ImageReadParam param = null;
            if (params != null && params.hasNext()) {
                Object o = params.next();
                if (o != null) {
                    if (o instanceof ImageReadParam) {
                        param = (ImageReadParam)o;
                    } else {
                        throw new IllegalArgumentException
                            ("Non-ImageReadParam supplied as part of params!");
                    }
                }
            }

            BufferedImage bi = null;
            try {
                bi = read(imageIndex, param);
            } catch (IndexOutOfBoundsException e) {
                break;
            }

            ArrayList thumbnails = null;
            int numThumbnails = getNumThumbnails(imageIndex);
            if (numThumbnails > 0) {
                thumbnails = new ArrayList();
                for (int j = 0; j < numThumbnails; j++) {
                    thumbnails.add(readThumbnail(imageIndex, j));
                }
            }

            IIOMetadata metadata = getImageMetadata(imageIndex);
            IIOImage im = new IIOImage(bi, thumbnails, metadata);
            output.add(im);

            ++imageIndex;
        }

        // Inform IIOReadProgressListeners we're ending a sequence
        processSequenceComplete();

        return output.iterator();
    }

    /**
     * Returns <code>true</code> if this plug-in supports reading
     * just a {@link java.awt.image.Raster <code>Raster</code>} of pixel data.
     * If this method returns <code>false</code>, calls to
     * {@link #readRaster <code>readRaster</code>} or {@link #readTileRaster
     * <code>readTileRaster</code>} will throw an
     * <code>UnsupportedOperationException</code>.
     *
     * <p> The default implementation returns <code>false</code>.
     *
     * @return <code>true</code> if this plug-in supports reading raw
     * <code>Raster</code>s.
     *
     * @see #readRaster
     * @see #readTileRaster
     */
    public boolean canReadRaster() {
        return false;
    }

    /**
     * Returns a new <code>Raster</code> object containing the raw pixel data
     * from the image stream, without any color conversion applied.  The
     * application must determine how to interpret the pixel data by other
     * means.  Any destination or image-type parameters in the supplied
     * <code>ImageReadParam</code> object are ignored, but all other
     * parameters are used exactly as in the {@link #read <code>read</code>}
     * method, except that any destination offset is used as a logical rather
     * than a physical offset.  The size of the returned <code>Raster</code>
     * will always be that of the source region clipped to the actual image.
     * Logical offsets in the stream itself are ignored.
     *
     * <p> This method allows formats that normally apply a color
     * conversion, such as JPEG, and formats that do not normally have an
     * associated colorspace, such as remote sensing or medical imaging data,
     * to provide access to raw pixel data.
     *
     * <p> Any registered <code>readUpdateListener</code>s are ignored, as
     * there is no <code>BufferedImage</code>, but all other listeners are
     * called exactly as they are for the {@link #read <code>read</code>}
     * method.
     *
     * <p> If {@link #canReadRaster <code>canReadRaster()</code>} returns
     * <code>false</code>, this method throws an
     * <code>UnsupportedOperationException</code>.
     *
     * <p> If the supplied <code>ImageReadParam</code> contains
     * optional setting values not supported by this reader (<i>e.g.</i>
     * source render size or any format-specific settings), they will
     * be ignored.
     *
     * <p> The default implementation throws an
     * <code>UnsupportedOperationException</code>.
     *
     * @param imageIndex the index of the image to be read.
     * @param param an <code>ImageReadParam</code> used to control
     * the reading process, or <code>null</code>.
     *
     * @return the desired portion of the image as a
     * <code>Raster</code>.
     *
     * @exception UnsupportedOperationException if this plug-in does not
     * support reading raw <code>Raster</code>s.
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     *
     * @see #canReadRaster
     * @see #read
     * @see java.awt.image.Raster
     */
    public Raster readRaster(int imageIndex, ImageReadParam param)
        throws IOException {
        throw new UnsupportedOperationException("readRaster not supported!");
    }

    /**
     * Returns <code>true</code> if the image is organized into
     * <i>tiles</i>, that is, equal-sized non-overlapping rectangles.
     *
     * <p> A reader plug-in may choose whether or not to expose tiling
     * that is present in the image as it is stored.  It may even
     * choose to advertise tiling when none is explicitly present.  In
     * general, tiling should only be advertised if there is some
     * advantage (in speed or space) to accessing individual tiles.
     * Regardless of whether the reader advertises tiling, it must be
     * capable of reading an arbitrary rectangular region specified in
     * an <code>ImageReadParam</code>.
     *
     * <p> A reader for which all images are guaranteed to be tiled,
     * or are guaranteed not to be tiled, may return <code>true</code>
     * or <code>false</code> respectively without accessing any image
     * data.  In such cases, it is not necessary to throw an exception
     * even if no input source has been set or the image index is out
     * of bounds.
     *
     * <p> The default implementation just returns <code>false</code>.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @return <code>true</code> if the image is tiled.
     *
     * @exception IllegalStateException if an input source is required
     * to determine the return value, but none has been set.
     * @exception IndexOutOfBoundsException if an image must be
     * accessed to determine the return value, but the supplied index
     * is out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public boolean isImageTiled(int imageIndex) throws IOException {
        return false;
    }

    /**
     * Returns the width of a tile in the given image.
     *
     * <p> The default implementation simply returns
     * <code>getWidth(imageIndex)</code>, which is correct for
     * non-tiled images.  Readers that support tiling should override
     * this method.
     *
     * @return the width of a tile.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public int getTileWidth(int imageIndex) throws IOException {
        return getWidth(imageIndex);
    }

    /**
     * Returns the height of a tile in the given image.
     *
     * <p> The default implementation simply returns
     * <code>getHeight(imageIndex)</code>, which is correct for
     * non-tiled images.  Readers that support tiling should override
     * this method.
     *
     * @return the height of a tile.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @exception IllegalStateException if the input source has not been set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public int getTileHeight(int imageIndex) throws IOException {
        return getHeight(imageIndex);
    }

    /**
     * Returns the X coordinate of the upper-left corner of tile (0,
     * 0) in the given image.
     *
     * <p> A reader for which the tile grid X offset always has the
     * same value (usually 0), may return the value without accessing
     * any image data.  In such cases, it is not necessary to throw an
     * exception even if no input source has been set or the image
     * index is out of bounds.
     *
     * <p> The default implementation simply returns 0, which is
     * correct for non-tiled images and tiled images in most formats.
     * Readers that support tiling with non-(0, 0) offsets should
     * override this method.
     *
     * @return the X offset of the tile grid.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @exception IllegalStateException if an input source is required
     * to determine the return value, but none has been set.
     * @exception IndexOutOfBoundsException if an image must be
     * accessed to determine the return value, but the supplied index
     * is out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public int getTileGridXOffset(int imageIndex) throws IOException {
        return 0;
    }

    /**
     * Returns the Y coordinate of the upper-left corner of tile (0,
     * 0) in the given image.
     *
     * <p> A reader for which the tile grid Y offset always has the
     * same value (usually 0), may return the value without accessing
     * any image data.  In such cases, it is not necessary to throw an
     * exception even if no input source has been set or the image
     * index is out of bounds.
     *
     * <p> The default implementation simply returns 0, which is
     * correct for non-tiled images and tiled images in most formats.
     * Readers that support tiling with non-(0, 0) offsets should
     * override this method.
     *
     * @return the Y offset of the tile grid.
     *
     * @param imageIndex the index of the image to be queried.
     *
     * @exception IllegalStateException if an input source is required
     * to determine the return value, but none has been set.
     * @exception IndexOutOfBoundsException if an image must be
     * accessed to determine the return value, but the supplied index
     * is out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public int getTileGridYOffset(int imageIndex) throws IOException {
        return 0;
    }

    /**
     * Reads the tile indicated by the <code>tileX</code> and
     * <code>tileY</code> arguments, returning it as a
     * <code>BufferedImage</code>.  If the arguments are out of range,
     * an <code>IllegalArgumentException</code> is thrown.  If the
     * image is not tiled, the values 0, 0 will return the entire
     * image; any other values will cause an
     * <code>IllegalArgumentException</code> to be thrown.
     *
     * <p> This method is merely a convenience equivalent to calling
     * <code>read(int, ImageReadParam)</code> with a read param
     * specifiying a source region having offsets of
     * <code>tileX*getTileWidth(imageIndex)</code>,
     * <code>tileY*getTileHeight(imageIndex)</code> and width and
     * height of <code>getTileWidth(imageIndex)</code>,
     * <code>getTileHeight(imageIndex)</code>; and subsampling
     * factors of 1 and offsets of 0.  To subsample a tile, call
     * <code>read</code> with a read param specifying this region
     * and different subsampling parameters.
     *
     * <p> The default implementation returns the entire image if
     * <code>tileX</code> and <code>tileY</code> are 0, or throws
     * an <code>IllegalArgumentException</code> otherwise.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param tileX the column index (starting with 0) of the tile
     * to be retrieved.
     * @param tileY the row index (starting with 0) of the tile
     * to be retrieved.
     *
     * @return the tile as a <code>BufferedImage</code>.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if <code>imageIndex</code>
     * is out of bounds.
     * @exception IllegalArgumentException if the tile indices are
     * out of bounds.
     * @exception IOException if an error occurs during reading.
     */
    public BufferedImage readTile(int imageIndex,
                                  int tileX, int tileY) throws IOException {
        if ((tileX != 0) || (tileY != 0)) {
            throw new IllegalArgumentException("Invalid tile indices");
        }
        return read(imageIndex);
    }

    /**
     * Returns a new <code>Raster</code> object containing the raw
     * pixel data from the tile, without any color conversion applied.
     * The application must determine how to interpret the pixel data by other
     * means.
     *
     * <p> If {@link #canReadRaster <code>canReadRaster()</code>} returns
     * <code>false</code>, this method throws an
     * <code>UnsupportedOperationException</code>.
     *
     * <p> The default implementation checks if reading
     * <code>Raster</code>s is supported, and if so calls {@link
     * #readRaster <code>readRaster(imageIndex, null)</code>} if
     * <code>tileX</code> and <code>tileY</code> are 0, or throws an
     * <code>IllegalArgumentException</code> otherwise.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param tileX the column index (starting with 0) of the tile
     * to be retrieved.
     * @param tileY the row index (starting with 0) of the tile
     * to be retrieved.
     *
     * @return the tile as a <code>Raster</code>.
     *
     * @exception UnsupportedOperationException if this plug-in does not
     * support reading raw <code>Raster</code>s.
     * @exception IllegalArgumentException if the tile indices are
     * out of bounds.
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if <code>imageIndex</code>
     * is out of bounds.
     * @exception IOException if an error occurs during reading.
     *
     * @see #readTile
     * @see #readRaster
     * @see java.awt.image.Raster
     */
    public Raster readTileRaster(int imageIndex,
                                 int tileX, int tileY) throws IOException {
        if (!canReadRaster()) {
            throw new UnsupportedOperationException
                ("readTileRaster not supported!");
        }
        if ((tileX != 0) || (tileY != 0)) {
            throw new IllegalArgumentException("Invalid tile indices");
        }
        return readRaster(imageIndex, null);
    }

    // RenderedImages

    /**
     * Returns a <code>RenderedImage</code> object that contains the
     * contents of the image indexed by <code>imageIndex</code>.  By
     * default, the returned image is simply the
     * <code>BufferedImage</code> returned by <code>read(imageIndex,
     * param)</code>.
     *
     * <p> The semantics of this method may differ from those of the
     * other <code>read</code> methods in several ways.  First, any
     * destination image and/or image type set in the
     * <code>ImageReadParam</code> may be ignored.  Second, the usual
     * listener calls are not guaranteed to be made, or to be
     * meaningful if they are.  This is because the returned image may
     * not be fully populated with pixel data at the time it is
     * returned, or indeed at any time.
     *
     * <p> If the supplied <code>ImageReadParam</code> contains
     * optional setting values not supported by this reader (<i>e.g.</i>
     * source render size or any format-specific settings), they will
     * be ignored.
     *
     * <p> The default implementation just calls {@link #read
     * <code>read(imageIndex, param)</code>}.
     *
     * @param imageIndex the index of the image to be retrieved.
     * @param param an <code>ImageReadParam</code> used to control
     * the reading process, or <code>null</code>.
     *
     * @return a <code>RenderedImage</code> object providing a view of
     * the image.
     *
     * @exception IllegalStateException if the input source has not been
     * set.
     * @exception IndexOutOfBoundsException if the supplied index is
     * out of bounds.
     * @exception IllegalArgumentException if the set of source and
     * destination bands specified by
     * <code>param.getSourceBands</code> and
     * <code>param.getDe
