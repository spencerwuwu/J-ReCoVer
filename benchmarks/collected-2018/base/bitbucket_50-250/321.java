// https://searchcode.com/api/result/122324385/

/**
 * 
 */
package com.cwalter.universe.viewer.loader;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;

import com.cwalter.universe.viewer.texture.BaseQuadTextureNode;
import com.cwalter.universe.viewer.texture.TextureInfo;


/**
 * We can figure out the max number of textures that would need to be loaded at one time by:
 * 1. Find the max screen resolution being supported. For example 1080p is 1900 x 1080.
 * 2. Find half the resolution of the current chunk size. This is necessary because images can
 * be scaled down to 50.00001% before the next size is used. For this example that would be 128x 128.
 * 3. Find the total number of textures that could fill the screen. 15 x 9 = 135.
 * 4. Divide each chunk dimension by 2 and add it to the total sum to calculate intermediate images.
 * 5. Repeat this process until both dimension are 1.
 * 6. Add 1 for each layer necessary to support the full texture size.
 * 
 * 1900 x 1080
 * 15 x 9 = 135
 * 8 x 5 = 40
 * 4 x 3 = 12
 * 2 x 2 = 4
 * 1 x 1 = 1
 * 
 * 192 Textures
 * 
 * 10 layer, 5 used
 * 
 * 197 Textures is bare minimum for 1900 x 1080
 * 
 * 197 x 256 x 256 x 3 = 38,731,776
 * 36.93 MB
 * 
 * To reduce the instances of texture not being loaded when scrolling we can expand the size
 * of the loaded area to 3 x 3 the original size. This bring the total area to 5700 x 3240.
 * 
 * 5700 x 3240
 * 45 x 26 = 1170
 * 23 x 13 = 299
 * 12 x 7 = 84
 * 6 x 4 = 24
 * 3 x 2 = 6
 * 2 x 1 = 2
 * 1 x 1 = 1
 * 
 * 1586 Textures
 * 
 * 10 layers, 7 used
 * 
 * 1589 Textures is bare minimum for 5700 x 3240
 * 
 * 1589 x 256 x 256 x 4 = 312,410,112
 * 297.93 MB
 * 
 * This balloons up our space requirements considerably but is still doable. We still have one more
 * issue which is texture detail popping in on zoom. We can potentially avoid this by loading images
 * at a higher detail level than what is actually needed. To simulate this measurement we can double
 * the size of the loaded area.
 * 
 * 11400 x 6480
 * 90 x 51 = 4590
 * 45 x 26 = 1170
 * 23 x 13 = 299
 * 12 x 7 = 84
 * 6 x 4 = 24
 * 3 x 2 = 6
 * 2 x 1 = 2
 * 1 x 1 = 1
 * 
 * 6176 Textures
 * 
 * 10 layers, 8 used
 * 
 * 6178 Textures is bare minimum.
 * 
 * 6178 x 256 x 256 x 4 = 1,619,001,344
 * 
 * 1544 MB
 * 
 * These are all worst case scenario results and all still easily handled assuming we are running on modern
 * hardware and are the primary process. Also consider that these numbers are for easily navigating an
 * image of theoretically unlimited size and are allowing for immediate movements. We can reduce memory
 * requirements back down again if we can assume:
 * 1. Maximum speed to traverse the texture is limited.
 * 2. Special effects can be employed at high speeds to reduce detail level needs as blurring would be applied.
 * 3. The region we can immediately zoom on is restricted to the current viewable area.
 * 
 * Additionally if the textures are compressed and stored in a faster caching system than a hard disk we 
 * can potentially load them on the fly with less to no need for intermediary images.
 * 
 * @author Chris Walter
 *
 */
public abstract class BaseTextureLoader implements ITextureLoader {

	protected String format;
	
	protected File basePath;
	
	protected int chunk;

	protected Map<String, TextureInfo> resourceMap;

	protected final Queue<BaseQuadTextureNode> lastUsedTextures;

	protected final TextureInfo.NodeType nodeType;

	public BaseTextureLoader(final File basePath, final String format, final int chunk, final TextureInfo.NodeType nodeType) {
		this.basePath = basePath;
		this.format = format;
		this.chunk = chunk;
		this.resourceMap = new HashMap<String, TextureInfo>();
		this.nodeType = nodeType;

		lastUsedTextures = new LinkedBlockingQueue<BaseQuadTextureNode>();
	}

	protected void commonLoad(final InputStream inputStream, final BaseQuadTextureNode texture) {
		try {
			BufferedImage image = ImageIO.read(inputStream);
			if(image.getWidth() < chunk || image.getHeight() < chunk) {
				BufferedImage opaqueImage = new BufferedImage(chunk, chunk, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = (Graphics2D) opaqueImage.getGraphics();
				g2d.setColor(Color.black);
				g2d.fillRect(0, 0, chunk, chunk);
				g2d.drawImage(image, null, 0, 0);
				g2d.dispose();
				image = opaqueImage;
			}
			texture.setImage(image);
		} catch (IOException e) {
			texture.setBroken(true);
		}
	}
	
	public void map(final String textureSetName, final int width, final int height) {
		resourceMap.put(textureSetName, new TextureInfo(width, height, chunk, nodeType));
	}
	
	/**
	 * Return a collection of quad texture nodes that intsersect the given viewing information
	 * in the order of least detailed to most detailed.
	 * 
	 * @param key
	 * @param rectangle
	 * @param distance
	 * @return
	 */
	public abstract Collection<BaseQuadTextureNode> get(final String key, final Rectangle rectangle, final double distance);
	
	protected abstract int getCacheSize();
}

