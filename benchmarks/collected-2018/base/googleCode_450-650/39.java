// https://searchcode.com/api/result/1114093/

/**
 * 
 */
package cc.creativecomputing.graphics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import cc.creativecomputing.io.CCBufferUtil;
import cc.creativecomputing.math.CCVecMath;
import cc.creativecomputing.math.CCVector2f;
import cc.creativecomputing.math.CCVector3f;
import cc.creativecomputing.math.CCVector4f;

/**
 * <p>
 * The CCMesh class maps the OPENGL vertex arrays that can make
 * drawing much faster than using beginShape and endShape. Note
 * that there are different ways to work with the meshes. You can
 * setup the mesh once with all the data and than just repeatedly
 * draw it. Also look at the VBOMesh that is even much faster for
 * drawing static content.
 * </p>
 * <p>
 * To update the mesh content simply provide the data as a list of
 * vertices or a floatbuffer. Be aware that using a floatbuffer should
 * be preferred for better performance. You can also initialize a mesh
 * with a certain size and than add all vertices one by one.
 * </p>
 * <p>
 * You can also pass indices to draw an indexed array. This can be used
 * to reduce the mesh size. 
 * </p>
 * @author info
 * @see #CCVBOMesh
 */
public class CCMesh {
    // Mesh Data
    protected int _myNumberOfVertices = 0;
    protected int _myVertexSize;
    protected int[] _myTextureCoordSize = new int[8];
    
    protected int _myNumberOfIndices = 0;
    
    protected FloatBuffer _myVertices;
    protected FloatBuffer _myNormals;
    protected FloatBuffer[] _myTextureCoords = new FloatBuffer[8];
    protected FloatBuffer _myColors;
    
    protected IntBuffer _myIndices;
    
    protected CCDrawMode _myDrawMode = CCDrawMode.TRIANGLES;
    
    public CCMesh(){
    	_myVertexSize = 3;
    }
    
    public CCMesh(final int theNumberOfVertices){
    	_myNumberOfVertices = theNumberOfVertices;
    	_myVertexSize = 3;
    }
    
    public CCMesh(final CCDrawMode theDrawMode){
    	_myDrawMode = theDrawMode;
    	_myVertexSize = 3;
    }
    
    public CCMesh(final CCDrawMode theDrawMode, final int theNumberOfVertices){
    	_myDrawMode = theDrawMode;
    	_myNumberOfVertices = theNumberOfVertices;
    	_myVertexSize = 3;
    }
    
    public CCMesh(
    	final List<CCVector3f> theVertices,
    	final List<CCVector2f> theTextureCoords,
    	final List<CCColor> theColors
    ){
    	this(CCDrawMode.TRIANGLES,theVertices,theTextureCoords,theColors);
    }
    
    public CCMesh(
    	final CCDrawMode theDrawMode,
        final List<CCVector3f> theVertices,
        final List<CCVector2f> theTextureCoords,
        final List<CCColor> theColors
    ){
    	_myDrawMode = theDrawMode;
    	_myVertexSize = 3;
        if(theVertices != null)vertices(theVertices,false);
        if(theTextureCoords != null)textureCoords(theTextureCoords);
        if(theColors != null)colors(theColors);
    }
    
    //////////////////////////////////////////////////////
    //
    //  METHODS TO ADD VERTEX DATA
    //
    //////////////////////////////////////////////////////
    
    public void prepareVertexData(int theNumberOfVertices, int theVertexSize){
    	_myNumberOfVertices = theNumberOfVertices;
    	_myVertexSize = theVertexSize;
    	
    	if(_myVertices == null || _myVertices.limit() / _myVertexSize != _myNumberOfVertices) {
    		_myNumberOfVertices = theNumberOfVertices;
    		_myVertexSize = theVertexSize;
    		_myVertices = CCBufferUtil.newDirectFloatBuffer(_myNumberOfVertices * _myVertexSize);
    	}
    }
    
    public void prepareVertexData(int theVertexSize){
    	prepareVertexData(_myNumberOfVertices, theVertexSize);
    }
    
    /**
     * Adds the given vertex to the Mesh. 
     */
    public void addVertex(final float theX, final float theY, final float theZ){
    	prepareVertexData(_myNumberOfVertices, 3);
    	_myVertices.put(theX);
    	_myVertices.put(theY);
    	_myVertices.put(theZ);
    }
    
    public void addVertex(final float theX, final float theY){
    	addVertex(theX, theY, 0);
    }
    
    public void addVertex(final float theX, final float theY, final float theZ, final float theW){
    	prepareVertexData(_myNumberOfVertices, 4);
    	
    	_myVertices.put(theX);
    	_myVertices.put(theY);
    	_myVertices.put(theZ);
    	_myVertices.put(theW);
    }
    
    public void addVertex(final CCVector2f theVertex){
    	addVertex(theVertex.x, theVertex.y, 0);
    }
    
    public void addVertex(final CCVector3f theVertex){
    	addVertex(theVertex.x, theVertex.y, theVertex.z);
    }
    
    public void addVertex(final CCVector4f theVertex){
    	addVertex(theVertex.x, theVertex.y, theVertex.z, theVertex.w);
    }
    
    /**
     * Fills the mesh with the data from the given float buffer. This means the float 
     * buffer needs to contain all vertex data for the mesh. This method assumes you
     * pass three coords for every vertex.
     * @param theVertices vertex data for the mesh
     */
    public void vertices(final FloatBuffer theVertices){
    	vertices(theVertices, 3);
    }
    
    /**
     * Fills the mesh with the data from the given float buffer. This means the float 
     * buffer needs to contain all vertex data for the mesh.
     * @param theVertices vertex data for the mesh
     * @param theVertexSize
     */
    public void vertices(final FloatBuffer theVertices, int theVertexSize){
    	prepareVertexData(theVertices.limit() / theVertexSize, theVertexSize);
    	
    	_myVertices.rewind();
    	_myVertices.put(theVertices);
    	_myVertices.rewind();
    }
    
    public void vertices(final List<CCVector3f> theVertices){
    	vertices(theVertices,false);
    }
    
    protected FloatBuffer[] createVertexBufferFromList(final List<CCVector3f> theVertices, final boolean theGenerateNormals){
    	FloatBuffer[] myResult = new FloatBuffer[2];
    	
    	if(theVertices.size() == 0){
    		return null;
    	}
    	
    	_myVertexSize = 3;
    	_myNumberOfVertices = theVertices.size();
    	myResult[0] = CCBufferUtil.newDirectFloatBuffer(_myNumberOfVertices * _myVertexSize);
    	
    	myResult[0].rewind();
    	for(CCVector3f myVertex:theVertices){
    		myResult[0].put(myVertex.x);
    		myResult[0].put(myVertex.y);
    		myResult[0].put(myVertex.z);
    	}
    	myResult[0].rewind();
    	
    	if(!theGenerateNormals)return myResult;
    	
    	myResult[1] = CCBufferUtil.newDirectFloatBuffer(_myNumberOfVertices * _myVertexSize);

		CCVector3f v1,v2,v3,v21,v31,normal = new CCVector3f(1,0,0);
		
		int myPointsPerPolygon = -1;
		
    	switch(_myDrawMode){
    	case QUADS:
    		myPointsPerPolygon = 4;
    		break;
    	case TRIANGLES:
    		myPointsPerPolygon = 3;
    		break;
    	default :
    		return myResult;
    	}
    	
    	for(int i = 0; i < theVertices.size();i += myPointsPerPolygon){
			v1 = theVertices.get(i);
			v2 = theVertices.get(i+1);
			v3 = theVertices.get(i+2);

			v21 = CCVecMath.subtract(v2, v1);
			v31 = CCVecMath.subtract(v3, v1);
			
			normal = v21.cross(v31);
			normal.normalize();
			
			for(int j = 0; j < myPointsPerPolygon; j++){
				myResult[1].put(normal.x);
				myResult[1].put(normal.y);
				myResult[1].put(normal.z);
			}
		}
    	
    	return myResult;
    }
    
    public void vertices(final List<CCVector3f> theVertices, final boolean theGenerateNormals){
    	if(theVertices.size() == 0){
    		return;
    	}
    	
    	FloatBuffer[] myVertexBuffer = createVertexBufferFromList(theVertices, theGenerateNormals);
    	if(myVertexBuffer[0] != null)vertices(myVertexBuffer[0]);
    	if(myVertexBuffer[1] != null)normals(myVertexBuffer[1]);
    	
    }
    
    //////////////////////////////////////////////////////
    //
    //  METHODS TO ADD NORMAL DATA
    //
    //////////////////////////////////////////////////////
    
    public void prepareNormalData(int theNumberOfVertices){
    	_myNumberOfVertices = theNumberOfVertices;
    	
    	if(_myNormals == null || _myNormals.limit() / 3 != _myNumberOfVertices){
    		_myNumberOfVertices = theNumberOfVertices;
    		_myNormals = CCBufferUtil.newDirectFloatBuffer(_myNumberOfVertices * 3);
    	}
    }
    
    public void prepareNormalData() {
    	prepareNormalData(_myNumberOfVertices);
    }
    
    public void addNormal(final float theX, final float theY, final float theZ){
    	prepareNormalData(_myNumberOfVertices);
    	
    	_myNormals.put(theX);
    	_myNormals.put(theY);
    	_myNormals.put(theZ);
    }
    
    public void addNormal(final CCVector3f theNormal){
    	addNormal(theNormal.x, theNormal.y, theNormal.z);
    }
    
    public void normals(final float[] theNormalData){
    	prepareNormalData(theNormalData.length / 3);

		_myNormals.rewind();
		_myNormals.put(theNormalData);
		_myNormals.rewind();
    }

    public void normals(final FloatBuffer theNormalData) {
		prepareNormalData(theNormalData.limit() / 3);

		_myNormals.rewind();
		_myNormals.put(theNormalData);
		_myNormals.rewind();
		
	}
    
	public void normals(final List<CCVector3f> theNormals){
    	prepareNormalData(theNormals.size());
    	
    	_myNormals.rewind();
    	for(CCVector3f myNormal:theNormals){
        	_myNormals.put(myNormal.x);
        	_myNormals.put(myNormal.y);
        	_myNormals.put(myNormal.z);
    	}
    	_myNormals.rewind();
	}
    
    
    
    //////////////////////////////////////////////////////
    //
    //  METHODS TO ADD TEXTURE COORD DATA
    //
    //////////////////////////////////////////////////////
	
	public void prepareTextureCoordData(int theNumberOfVertices, int theLevel, int theTextureCoordSize){
		_myNumberOfVertices = theNumberOfVertices;
		_myTextureCoordSize[theLevel] = theTextureCoordSize;
		
		if(_myTextureCoords[theLevel] == null || _myNumberOfVertices != _myTextureCoords[theLevel].limit() / theTextureCoordSize) {
    		_myTextureCoords[theLevel] = CCBufferUtil.newDirectFloatBuffer(_myNumberOfVertices * theTextureCoordSize);
    	}
	}
	public void prepareTextureCoordData(int theLevel, int theTextureCoordSize){
		prepareTextureCoordData(_myNumberOfVertices, theLevel, theTextureCoordSize);
	}
    
    public void addTextureCoords(final int theLevel, final float theX, final float theY){
    	prepareTextureCoordData(_myNumberOfVertices, theLevel, 2);
    	
    	_myTextureCoords[theLevel].put(theX);
    	_myTextureCoords[theLevel].put(theY);
    }
    
    public void addTextureCoords(final float theX, final float theY){
    	addTextureCoords(0, theX, theY);
    }
    
    public void addTextureCoords(final int theLevel, final CCVector2f theTextureCoords){
    	addTextureCoords(theTextureCoords.x, theTextureCoords.y);
    }
    
    public void addTextureCoords(final int theLevel, final float theX, final float theY, final float theZ){
    	prepareTextureCoordData(_myNumberOfVertices, theLevel, 3);
    	
    	_myTextureCoords[theLevel].put(theX);
    	_myTextureCoords[theLevel].put(theY);
    	_myTextureCoords[theLevel].put(theZ);
    }
    
    public void addTextureCoords(final int theLevel, final CCVector3f theTextureCoords){
    	addTextureCoords(theLevel, theTextureCoords.x, theTextureCoords.y, theTextureCoords.z);
    }
    
    public void addTextureCoords(final int theLevel, final float theX, final float theY, final float theZ, final float theW){
    	prepareTextureCoordData(_myNumberOfVertices, theLevel, 4);
    	
    	_myTextureCoords[theLevel].put(theX);
    	_myTextureCoords[theLevel].put(theY);
    	_myTextureCoords[theLevel].put(theZ);
    	_myTextureCoords[theLevel].put(theW);
    }
    
    public void addTextureCoords(final int theLevel, final CCVector4f theTextureCoords){
    	addTextureCoords(theLevel, theTextureCoords.x, theTextureCoords.y, theTextureCoords.z, theTextureCoords.w);
    }
    
    public void textureCoords(final int theLevel, final FloatBuffer theTextureCoords, final int theTextureCoordSize){
    	prepareTextureCoordData(theTextureCoords.limit() / theTextureCoordSize, theLevel, theTextureCoordSize);
    	
    	_myTextureCoords[theLevel].rewind();
    	_myTextureCoords[theLevel].put(theTextureCoords);
    	_myTextureCoords[theLevel].rewind();
    }
    
    public void textureCoords(final int theLevel, final FloatBuffer theTextureCoords){
    	textureCoords(theLevel, theTextureCoords, 2);
    }
    
    public void textureCoords(final FloatBuffer theTextureCoords){
    	textureCoords(0, theTextureCoords);
    }
    
    public void textureCoords(final int theLevel, final List<CCVector2f> theTextureCoords){
    	prepareTextureCoordData(theTextureCoords.size(), theLevel, 2);
    	
    	_myTextureCoords[theLevel].rewind();
    	for(CCVector2f myTextureCoords:theTextureCoords){
    		_myTextureCoords[theLevel].put(myTextureCoords.x);
    		_myTextureCoords[theLevel].put(myTextureCoords.y);
    	}
    	_myTextureCoords[theLevel].rewind();
    }
    
    public void textureCoords(final List<CCVector2f> theTextureCoords){
    	textureCoords(0, theTextureCoords);
    }
    
    //////////////////////////////////////////////////////
    //
    //  METHODS TO ADD COLOR DATA
    //
    //////////////////////////////////////////////////////
    
    public void prepareColorData(int theNumberOfVertices){
    	_myNumberOfVertices = theNumberOfVertices;
    	if(_myColors == null || _myColors.limit() / 4 != _myNumberOfVertices){
    		_myNumberOfVertices = theNumberOfVertices;
    		_myColors = CCBufferUtil.newDirectFloatBuffer(_myNumberOfVertices * 4);
    	}
    }
    
    public void addColor(final float theRed, final float theGreen, final float theBlue, final float theAlpha){
    	prepareColorData(_myNumberOfVertices);
    	_myColors.put(theRed);
    	_myColors.put(theGreen);
    	_myColors.put(theBlue);
    	_myColors.put(theAlpha);
    }
    
    public void addColor(final CCColor theColor){
    	addColor(theColor.r, theColor.g, theColor.b, theColor.a);
    }
    
    public void addColor(final float theRed, final float theGreen, final float theBlue){
    	addColor(theRed, theGreen, theBlue, 1f);
    }
    
    public void addColor(final float theGray, final float theAlpha){
    	addColor(theGray, theGray, theGray, theAlpha);
    }
    
    public void addColor(final float theGray){
    	addColor(theGray, theGray, theGray, 1f);
    }
    
    public void colors(final List<CCColor> theColors){
    	prepareColorData(theColors.size());
    	_myColors.rewind();
    	
    	for(CCColor myColor:theColors){
    		_myColors.put(myColor.r);
    		_myColors.put(myColor.g);
    		_myColors.put(myColor.b);
    		_myColors.put(myColor.a);
    	}
    	_myColors.rewind();
    }
    
    public void colors(final FloatBuffer theColors){
    	prepareColorData(theColors.limit() / 4);
    	_myColors.rewind();
    	_myColors.put(theColors);
    	_myColors.rewind();
    }
    
    public void indices(final List<Integer> theIndices){
    	if(theIndices.size() == 0)return;
    	_myNumberOfIndices = theIndices.size();
    	_myIndices = CCBufferUtil.newIntBuffer(theIndices.size());
    	for(int myIndex:theIndices){
    		_myIndices.put(myIndex);
    	}
    	_myIndices.rewind();
    }
    
    public void indices(final IntBuffer theIndices) {
    	_myNumberOfIndices = theIndices.capacity();
    	_myIndices = theIndices;
    	_myIndices.rewind();
    }

    public int numberOfVertices() {
        return _myNumberOfVertices;
    }
    


    //////////////////////////////////////////////////////
    //
    //  METHODS TO RESET THE MESH
    //
    //////////////////////////////////////////////////////
    
    public void clearVertices(){
    	_myVertices = null;
    }
    
    public void clearTextureCoords(){
    	for(int i = 0; i < _myTextureCoords.length;i++) {
    		_myTextureCoords[i] = null;
    	}
    }
    
    public void clearNormals(){
    	_myNormals = null;
    }
    
    public void clearColors(){
    	_myColors = null;
    }
    
    public void clearIndices(){
    	_myIndices = null;
    }
    
    public void clearAll(){
    	clearVertices();
    	clearTextureCoords();
    	clearNormals();
    	clearColors();
    	clearIndices();
    }
    
    public void drawMode(CCDrawMode theDrawMode) {
    	_myDrawMode = theDrawMode;
    }
    
    public void enable(CCGraphics g){
    	// Enable Pointers
    	if(_myVertices != null){
    		_myVertices.rewind();
    		g.gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
 	   		g.gl.glVertexPointer(_myVertexSize, GL.GL_FLOAT, 0, _myVertices);
    	}
    	if(_myNormals != null){
    		_myNormals.rewind();
    		g.gl.getGL2GL3().glEnableClientState(GL2.GL_NORMAL_ARRAY);
 	   		g.gl.glNormalPointer(GL.GL_FLOAT, 0, _myNormals);
    	}
    	for(int i = 0; i < _myTextureCoords.length;i++) {
	    	if(_myTextureCoords[i] != null){
	    		_myTextureCoords[i].rewind();
	    		g.gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
	    		g.gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
	    		g.gl.glTexCoordPointer(_myTextureCoordSize[i], GL.GL_FLOAT, 0, _myTextureCoords[i]);
	    	}
    	}
    	if(_myColors != null){
    		_myColors.rewind();
    		g.gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
    		g.gl.glColorPointer(4, GL.GL_FLOAT, 0, _myColors);
    	}
    	
//    	if(_myDrawMode == CCGraphics.POINTS && g._myDrawTexture && _myTextureCoords != null){
//			g.gl.glEnable(GL.GL_POINT_SPRITE);
//			g.gl.glTexEnvi(GL.GL_POINT_SPRITE, GL.GL_COORD_REPLACE, GL.GL_TRUE); 
//		}
    }
    
    public void disable(CCGraphics g){
//    	if(_myDrawMode == CCGraphics.POINTS && g._myDrawTexture){
//			g.gl.glDisable(GL.GL_POINT_SPRITE);
//		}

        // Disable Pointers
        if(_myVertices != null){
        	g.gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        }
        if(_myNormals != null){
        	g.gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
        }
        for(int i = 0; i < _myTextureCoords.length;i++) {
	    	if(_myTextureCoords[i] != null){
	    		_myTextureCoords[i].rewind();
	    		g.gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
	        	g.gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
	    	}
    	}
		g.gl.glClientActiveTexture(GL.GL_TEXTURE0);
        if(_myColors != null){
        	g.gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
        }
    }
    
    public void drawArray(CCGraphics g){
    	// Draw All Of The Triangles At Once
    	if(_myIndices == null){
    		g.gl.getGL2GL3().glDrawArrays(_myDrawMode.glID, 0, _myNumberOfVertices);
    	}else{
    		g.gl.glDrawElements(_myDrawMode.glID, _myNumberOfIndices,GL.GL_UNSIGNED_INT, _myIndices);
    	}
    }

    public void draw(CCGraphics g) {
    	enable(g);
        drawArray(g);
    	disable(g);
    }
}
