// https://searchcode.com/api/result/66999346/

/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.plugin.image;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.Utils;

/**
 * @version $Id: 4090c7113accd6e25d72ddcc95e165d7247d3887 $
 * @deprecated the plugin technology is deprecated, consider rewriting as components
 */
@Deprecated
public class ImagePlugin extends XWikiDefaultPlugin
{
    /**
     * Logging helper object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ImagePlugin.class);

    /**
     * The name used for retrieving this plugin from the context.
     * 
     * @see XWikiPluginInterface#getName()
     */
    private static final String PLUGIN_NAME = "image";

    /**
     * The comma sign, used as a separator for x and y in boundaries request parameters (top left and bottom right).
     */
    private static final String COMMA = ",";

    /**
     * Cache for already served images.
     */
    private Cache<byte[]> imageCache;

    /**
     * The size of the cache. This parameter can be configured using the key {@code xwiki.plugin.image.cache.capacity}.
     */
    private int capacity = 50;

    /**
     * Default JPEG image quality.
     */
    private float defaultQuality = 0.5f;

    /**
     * The object used to process images.
     */
    private final ImageProcessor imageProcessor = Utils.getComponent(ImageProcessor.class);

    /**
     * Creates a new instance of this plugin.
     * 
     * @param name the name of the plugin
     * @param className the class name
     * @param context the XWiki context
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public ImagePlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiPluginInterface#getPluginApi(XWikiPluginInterface, XWikiContext)
     */
    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ImagePluginAPI((ImagePlugin) plugin, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiPluginInterface#getName()
     */
    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
        initCache(context);

        String defaultQualityParam = context.getWiki().Param("xwiki.plugin.image.defaultQuality");
        if (!StringUtils.isBlank(defaultQualityParam)) {
            try {
                this.defaultQuality = Math.max(0, Math.min(1, Float.parseFloat(defaultQualityParam.trim())));
            } catch (NumberFormatException e) {
                LOG.warn("Failed to parse xwiki.plugin.image.defaultQuality configuration parameter. "
                    + "Using {} as the default image quality.", this.defaultQuality);
            }
        }
    }

    /**
     * Tries to initializes the image cache. If the initialization fails the image cache remains {@code null}.
     * 
     * @param context the XWiki context
     */
    private void initCache(XWikiContext context)
    {
        CacheConfiguration configuration = new CacheConfiguration();

        configuration.setConfigurationId("xwiki.plugin.image");

        // Set folder to store cache.
        File tempDir = context.getWiki().getTempDirectory(context);
        File imgTempDir = new File(tempDir, configuration.getConfigurationId());
        try {
            imgTempDir.mkdirs();
        } catch (Exception ex) {
            LOG.warn("Cannot create temporary files.", ex);
        }
        configuration.put("cache.path", imgTempDir.getAbsolutePath());
        // Set cache constraints.
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        configuration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

        String capacityParam = context.getWiki().Param("xwiki.plugin.image.cache.capacity");
        if (!StringUtils.isBlank(capacityParam) && StringUtils.isNumeric(capacityParam.trim())) {
            try {
                this.capacity = Integer.parseInt(capacityParam.trim());
            } catch (NumberFormatException e) {
                LOG.warn(String.format("Failed to parse xwiki.plugin.image.cache.capacity configuration parameter. "
                    + "Using %s as the cache capacity.", this.capacity), e);
            }
        }
        lru.setMaxEntries(this.capacity);

        try {
            this.imageCache = context.getWiki().getLocalCacheFactory().newCache(configuration);
        } catch (CacheException e) {
            LOG.error("Error initializing the image cache.", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiPluginInterface#flushCache()
     */
    @Override
    public void flushCache()
    {
        if (this.imageCache != null) {
            this.imageCache.dispose();
        }
        this.imageCache = null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Allows to scale and crop images server-side, in order to have real thumbnails for reduced traffic. The new image
     * dimensions and boundaries are passed in the request as the {@code width}, {@code height} and {@boundaries}
     * parameters. If only one of the dimensions is specified, then the other one is computed to preserve
     * the original aspect ratio of designated area of the image (or the whole image if no boundaries are specified).
     * 
     * @see XWikiDefaultPlugin#downloadAttachment(XWikiAttachment, XWikiContext)
     */
    @Override
    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context)
    {
        if (!this.imageProcessor.isMimeTypeSupported(attachment.getMimeType(context))) {
            return attachment;
        }

        int height = -1;
        try {
            height = Integer.parseInt(context.getRequest().getParameter("height"));
        } catch (NumberFormatException e) {
            // Ignore.
        }

        int width = -1;
        try {
            width = Integer.parseInt(context.getRequest().getParameter("width"));
        } catch (NumberFormatException e) {
            // Ignore.
        }

        float quality = -1;
        try {
            quality = Float.parseFloat(context.getRequest().getParameter("quality"));
        } catch (NumberFormatException e) {
            // Ignore.
        } catch (NullPointerException e) {
            // Ignore.
        }

        Rectangle cropCanvas = getBoundariesFromRequestParameter("boundaries", context);
        
        // If no scaling or cropping needed, return the original image.
        if (!this.doesAffectImage(cropCanvas, width, height, quality)) {
            return attachment;
        }

        try {
            // Transform the image attachment before is it downloaded.
            return downloadImage(attachment, width, height, cropCanvas, quality, context);
        } catch (Exception e) {
            LOG.warn("Failed to transform image attachment.", e);
            return attachment;
        }
    }

    /**
     * Helper method to test if passed parameters would affect or not an image.
     * 
     * @param boundaries the boundaries to crop the image to.
     * @param width the width to render the image with
     * @param height the height to render the image with
     * @param quality the quality to render the image with
     * @return true if the image would change, false otherwise
     */
    private boolean doesAffectImage(Rectangle boundaries, int width, int height, float quality)
    {
        return !(boundaries == null && height <= 0 && width <= 0 && quality < 0);
    }
    
    /**
     * Gets boundaries from a request parameter. Expected form is 4 integers, comma separated. Each individual integer
     * means the following (in expected order) :
     * <ul>
     * <li><tt>x</tt> : the x position of the top left corner of the boundary.
     * <li><tt>y</tt> : the y position of the top left corner of the boundary.
     * <li><tt>width</tt> : the width of the boundary.
     * <li><tt>height</tt> : the height of the boundary.
     * </ul>
     * 
     * @param parameterName the name of the parameter to try and get boundaries from
     * @param context the XWiki context (used to access current request)
     * @return the boundaries, or {@code null} if none was passed for this parameter or if the format does not match
     *         the expected format
     */
    private Rectangle getBoundariesFromRequestParameter(String parameterName, XWikiContext context)
    {
        String parameterValue = context.getRequest().getParameter(parameterName);
        if (!StringUtils.isBlank(parameterValue)
            && StringUtils.countMatches(parameterValue, COMMA) == 3) {
            try {
                String[] values = parameterValue.split(COMMA);
                int x = Integer.parseInt(values[0]);
                int y = Integer.parseInt(values[1]);
                int width = Integer.parseInt(values[2]);
                int height = Integer.parseInt(values[3]);
                return new Rectangle(x, y, width, height);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return null;
    }

    /**
     * Transforms the given image (i.e. crop, shrinks the image and changes its quality) before it is downloaded.
     * 
     * @param image the image to be downloaded
     * @param width the desired image width; this value is taken into account only if it is greater than zero and less
     *            than the current image width
     * @param height the desired image height; this value is taken into account only if it is greater than zero and less
     *            than the current image height
     * @param boundaries the boundaries to crop the original image from. May be {@code null} when the image does not 
     *            have to be cropped            
     * @param quality the desired compression quality
     * @param context the XWiki context
     * @return the transformed image
     * @throws Exception if transforming the image fails
     */
    private XWikiAttachment downloadImage(XWikiAttachment image, int width, int height, Rectangle boundaries,
        float quality, XWikiContext context) throws Exception
    {
        if (this.imageCache == null) {
            initCache(context);
        }
        boolean keepAspectRatio = Boolean.valueOf(context.getRequest().getParameter("keepAspectRatio"));
        XWikiAttachment thumbnail =
            this.imageCache == null ? createAttachmentThumbnail(image, width, height, boundaries, keepAspectRatio,
                quality, context)
                : downloadImageFromCache(image, width, height, boundaries, keepAspectRatio, quality, context);
        // If the image has been transformed, update the file name extension to match the image format.
        String fileName = thumbnail.getFilename();
        String extension = StringUtils.lowerCase(StringUtils.substringAfterLast(fileName, String.valueOf('.')));
        if (thumbnail != image && !Arrays.asList("jpeg", "jpg", "png").contains(extension)) {
            // The scaled image is PNG, so correct the extension in order to output the correct MIME type.
            thumbnail.setFilename(StringUtils.substringBeforeLast(fileName, ".") + ".png");
        }
        return thumbnail;
    }

    /**
     * Downloads the given image from cache.
     * 
     * @param image the image to be downloaded
     * @param width the desired image width; this value is taken into account only if it is greater than zero and less
     *            than the current (or cropped if top left and bottom right parameters are used) image width
     * @param height the desired image height; this value is taken into account only if it is greater than zero and less
     *            than the current (or cropped if top left and bottom right parameters are used) image height
     * @param boundaries the boundaries to crop the original image from
     * @param keepAspectRatio {@code true} to preserve aspect ratio when resizing the image, {@code false} otherwise
     * @param quality the desired compression quality
     * @param context the XWiki context
     * @return the transformed image
     * @throws Exception if transforming the image fails
     */
    private XWikiAttachment downloadImageFromCache(XWikiAttachment image, int width, int height, Rectangle boundaries,
        boolean keepAspectRatio, float quality, XWikiContext context) throws Exception
    {
        String boundariesKey = "-1,-1,-1,-1";
        if (boundaries != null) {
            boundariesKey = String.format("%s,%s,%s,%s",
                boundaries.x, boundaries.width, boundaries.y, boundaries.height);
        }

        String key =
            String.format("%s;%s;%s;%s;%s;%s;%s", image.getId(), image.getVersion(), width, height, keepAspectRatio,
                quality, boundariesKey);
        byte[] data = this.imageCache.get(key);
        XWikiAttachment thumbnail;
        if (data != null) {
            thumbnail = (XWikiAttachment) image.clone();
            thumbnail.setContent(new ByteArrayInputStream(data), data.length);
        } else {
            thumbnail = createAttachmentThumbnail(image, width, height, boundaries, keepAspectRatio, quality, context);
            this.imageCache.set(key, thumbnail.getContent(context));
        }
        return thumbnail;
    }

    /**
     * Reduces the size (i.e. the number of bytes) of an image by scaling its width and height and by reducing its
     * compression quality. This helps decreasing the time needed to download the image attachment.
     * 
     * @param attachment the image to be shrunk
     * @param requestedWidth the desired image width; this value is taken into account only if it is greater than zero
     *            and less than the current image width
     * @param requestedHeight the desired image height; this value is taken into account only if it is greater than zero
     *            and less than the current image height
     * @param boundaries the boundaries to crop the original image from
     * @param keepAspectRatio {@code true} to preserve the image aspect ratio even when both requested dimensions are
     *            properly specified (in this case the image will be resized to best fit the rectangle with the
     *            requested width and height), {@code false} otherwise
     * @param requestedQuality the desired compression quality
     * @param context the XWiki context
     * @return the modified image attachment
     * @throws Exception if shrinking the image fails
     */
    private XWikiAttachment createAttachmentThumbnail(XWikiAttachment attachment, int requestedWidth,
        int requestedHeight, Rectangle boundaries, boolean keepAspectRatio, float requestedQuality,
        XWikiContext context)
        throws Exception
    {
        Image image = this.imageProcessor.readImage(attachment.getContentInputStream(context));

        // Compute the new image dimension.
        int currentWidth = image.getWidth(null);
        int currentHeight = image.getHeight(null);
        if (boundaries != null) {
            currentWidth = boundaries.getSize().width;
            currentHeight = boundaries.getSize().height;
        }
        int[] dimensions =
            reduceImageDimensions(currentWidth, currentHeight, requestedWidth, requestedHeight, keepAspectRatio);

        float quality = requestedQuality;
        if (quality < 0) {
            // If no scaling is needed and the quality parameter is not specified, return the original image.
            if (dimensions[0] == image.getWidth(null) && dimensions[1] == image.getHeight(null)) {
                return attachment;
            }
            quality = this.defaultQuality;
        }

        // Scale the image to the new dimensions.
        RenderedImage shrunkImage =
            this.imageProcessor.createThumbnail(image, dimensions[0], dimensions[1], boundaries);

        // Write the shrunk image to a byte array output stream.
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        this.imageProcessor.writeImage(shrunkImage, attachment.getMimeType(context), quality, bout);

        // Create an image attachment for the shrunk image.
        XWikiAttachment thumbnail = (XWikiAttachment) attachment.clone();
        thumbnail.setContent(new ByteArrayInputStream(bout.toByteArray()), bout.size());

        return thumbnail;
    }

    /**
     * Computes the new image dimension which:
     * <ul>
     * <li>uses the requested width and height only if both are smaller than the current values</li>
     * <li>preserves the aspect ratio when width or height is not specified.</li>
     * </ul>
     * 
     * @param currentWidth the current image width
     * @param currentHeight the current image height
     * @param requestedWidth the desired image width; this value is taken into account only if it is greater than zero
     *            and less than the current image width
     * @param requestedHeight the desired image height; this value is taken into account only if it is greater than zero
     *            and less than the current image height
     * @param keepAspectRatio {@code true} to preserve the image aspect ratio even when both requested dimensions are
     *            properly specified (in this case the image will be resized to best fit the rectangle with the
     *            requested width and height), {@code false} otherwise
     * @return new width and height values
     */
    private int[] reduceImageDimensions(int currentWidth, int currentHeight, int requestedWidth, int requestedHeight,
        boolean keepAspectRatio)
    {
        double aspectRatio = (double) currentWidth / (double) currentHeight;

        int width = currentWidth;
        int height = currentHeight;

        if (requestedWidth <= 0 || requestedWidth >= currentWidth) {
            // Ignore the requested width. Check the requested height.
            if (requestedHeight > 0 && requestedHeight < currentHeight) {
                // Reduce the height, keeping aspect ratio.
                width = (int) (requestedHeight * aspectRatio);
                height = requestedHeight;
            }
        } else if (requestedHeight <= 0 || requestedHeight >= currentHeight) {
            // Ignore the requested height. Reduce the width, keeping aspect ratio.
            width = requestedWidth;
            height = (int) (requestedWidth / aspectRatio);
        } else if (keepAspectRatio) {
            // Reduce the width and check if the corresponding height is less than the requested height.
            width = requestedWidth;
            height = (int) (requestedWidth / aspectRatio);
            if (height > requestedHeight) {
                // We have to reduce the height instead and compute the width based on it.
                width = (int) (requestedHeight * aspectRatio);
                height = requestedHeight;
            }
        } else {
            // Reduce both width and height, possibly loosing aspect ratio.
            width = requestedWidth;
            height = requestedHeight;
        }

        return new int[] {width, height};
    }

    /**
     * @param attachment an image attachment
     * @param context the XWiki context
     * @return the width of the specified image
     * @throws IOException if reading the image from the attachment content fails
     * @throws XWikiException if reading the attachment content fails
     */
    public int getWidth(XWikiAttachment attachment, XWikiContext context) throws IOException, XWikiException
    {
        return this.imageProcessor.readImage(attachment.getContentInputStream(context)).getWidth(null);
    }

    /**
     * @param attachment an image attachment
     * @param context the XWiki context
     * @return the height of the specified image
     * @throws IOException if reading the image from the attachment content fails
     * @throws XWikiException if reading the attachment content fails
     */
    public int getHeight(XWikiAttachment attachment, XWikiContext context) throws IOException, XWikiException
    {
        return this.imageProcessor.readImage(attachment.getContentInputStream(context)).getHeight(null);
    }
}

