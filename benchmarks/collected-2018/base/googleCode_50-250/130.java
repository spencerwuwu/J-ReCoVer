// https://searchcode.com/api/result/2314268/

package skylight1.opengl;

import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * A Geometry that can render itself to an OpenGL context.
 */
public class OpenGLGeometry {
	private final int mode;

	private final int first;

	private final int numberOfVerticies;

	private OpenGLGeometryBuilderImpl<?, ?> openGLGeometryBuilderImpl;
	
	private final Texture texture;

	private final int modelPositionInBuffer;

	private final int texturePositionInBuffer;

	private final int normalsPositionInBuffer;

	private final int coloursPositionInBuffer;
	
	private final float[] boundingSphere;

	public OpenGLGeometry(final int aMode, final int aFirst, final int aNumberOfVerticies,
			final OpenGLGeometryBuilderImpl<?, ?> anOpenGLGeometryBuilderImpl, final float[] aBoundingSphere,
			final Texture aTexture) {
		mode = aMode;
		first = aFirst;
		numberOfVerticies = aNumberOfVerticies;
		openGLGeometryBuilderImpl = anOpenGLGeometryBuilderImpl;
		boundingSphere = aBoundingSphere;
		texture = aTexture;
		
		// To reduce calculations in the draw method, pre-calculate some offets
		modelPositionInBuffer = first * FastGeometryBuilderImpl.MODEL_COORDINATES_PER_VERTEX;
		texturePositionInBuffer = first * FastGeometryBuilderImpl.TEXTURE_COORDINATES_PER_VERTEX;
		normalsPositionInBuffer = first * FastGeometryBuilderImpl.NORMAL_COMPONENTS_PER_VERTEX;
		coloursPositionInBuffer = first * FastGeometryBuilderImpl.COLOUR_PARTS_PER_VERTEX;
	}

	/**
	 * Draws the geometry defined by this object. If the geometry builder is not active, and any associated texture,
	 * then the results are unpredictable.
	 */
	public void draw(GL10 aGL10) {
		texture.activate();
		aGL10.glDrawArrays(mode, first, numberOfVerticies);
	}

	public float[] getBoundingSphere() {
		return boundingSphere;
	}

	/**
	 * Updates the model associated with this geometry.
	 */
	public void updateModel(FastGeometryBuilder<?, ?> aFastGeometryBuilder) {
		if (!openGLGeometryBuilderImpl.complete) {
			throw new IllegalStateException(
					"Updates are not permitted until after the first time the geometry builder has been enabled.");
		}
		final IntBuffer modelCoordinatesAsBuffer = openGLGeometryBuilderImpl.modelCoordinatesAsBuffer;
		modelCoordinatesAsBuffer.position(modelPositionInBuffer);
		modelCoordinatesAsBuffer.put(((FastGeometryBuilderImpl<?, ?>) aFastGeometryBuilder).modelCoordinates);
		modelCoordinatesAsBuffer.position(0);
	}

	/**
	 * Updates the textures associated with this geometry.
	 */
	public void updateTexture(FastGeometryBuilder<?, ?> aFastGeometryBuilder) {
		if (!openGLGeometryBuilderImpl.complete) {
			throw new IllegalStateException(
					"Updates are not permitted until after the first time the geometry builder has been enabled.");
		}
		final IntBuffer textureCoordinatesAsBuffer = openGLGeometryBuilderImpl.textureCoordinatesAsBuffer;
		textureCoordinatesAsBuffer.position(texturePositionInBuffer);
		textureCoordinatesAsBuffer.put(((FastGeometryBuilderImpl<?, ?>) aFastGeometryBuilder).textureCoordinates);
		textureCoordinatesAsBuffer.position(0);
	}

	/**
	 * Updates the normals associated with this geometry.
	 */
	public void updateNormals(FastGeometryBuilder<?, ?> aFastGeometryBuilder) {
		if (!openGLGeometryBuilderImpl.complete) {
			throw new IllegalStateException(
					"Updates are not permitted until after the first time the geometry builder has been enabled.");
		}
		final IntBuffer normalAsBuffer = openGLGeometryBuilderImpl.normalsAsBuffer;
		normalAsBuffer.position(normalsPositionInBuffer);
		normalAsBuffer.put(((FastGeometryBuilderImpl<?, ?>) aFastGeometryBuilder).normalComponents);
		normalAsBuffer.position(0);
	}

	/**
	 * Updates the colours associated with this geometry.
	 */
	public void updateColours(FastGeometryBuilder<?, ?> aFastGeometryBuilder) {
		if (!openGLGeometryBuilderImpl.complete) {
			throw new IllegalStateException("Updates are not permitted until after the first time the geometry builder has been enabled.");
		}
		final IntBuffer coloursAsBuffer = openGLGeometryBuilderImpl.coloursAsBuffer;
		coloursAsBuffer.position(coloursPositionInBuffer);
		coloursAsBuffer.put(((FastGeometryBuilderImpl<?, ?>) aFastGeometryBuilder).colours);
		coloursAsBuffer.position(0);
	}

	public int getNumberOfVerticies() {
		return numberOfVerticies;
	}
}

