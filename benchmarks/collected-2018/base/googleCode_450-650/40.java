// https://searchcode.com/api/result/1115323/

/**
 * 
 */
package cc.creativecomputing.graphics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;


import cc.creativecomputing.graphics.CCColor;
import cc.creativecomputing.graphics.CCGraphics;
import cc.creativecomputing.graphics.CCGraphics.CCDrawMode;
import cc.creativecomputing.math.CCVecMath;
import cc.creativecomputing.math.CCVector2f;
import cc.creativecomputing.math.CCVector3f;
import cc.creativecomputing.math.CCVector4f;

import com.sun.opengl.util.BufferUtil;

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
    
    /**
     * Adds the given vertex to the Mesh. 
     */
    public void addVertex(final float theX, final float theY, final float theZ){
    	if(_myVertices == null) {
    		_myVertexSize = 3;
    		_myVertices = BufferUtil.newFloatBuffer(_myNumberOfVertices * _myVertexSize);
    	}
    	
    	_myVertices.put(theX);
    	_myVertices.put(theY);
    	_myVertices.put(theZ);
    }
    
    public void addVertex(final CCVector3f theVertex){
    	addVertex(theVertex.x, theVertex.y, theVertex.z);
    }
    
    public void addVertex(final float theX, final float theY, final float theZ, final float theW){
    	if(_myVertices == null) {
    		_myVertexSize = 4;
    		_myVertices = BufferUtil.newFloatBuffer(_myNumberOfVertices * _myVertexSize);
    	}
    	
    	_myVertices.put(theX);
    	_myVertices.put(theY);
    	_myVertices.put(theZ);
    	_myVertices.put(theW);
    }
    
    public void addVertex(final CCVector4f theVertex){
    	if(_myVertices == null) {
    		_myVertexSize = 4;
    		_myVertices = BufferUtil.newFloatBuffer(_myNumberOfVertices * _myVertexSize);
    	}
    	_myVertices.put(theVertex.x);
    	_myVertices.put(theVertex.y);
    	_myVertices.put(theVertex.z);
    	_myVertices.put(theVertex.w);
    }
    
    public void vertices(final FloatBuffer theVertices){
    	_myNumberOfVertices = theVertices.limit() / _myVertexSize;
    	_myVertices = theVertices;
    }
    
    public void vertices(final List<CCVector3f> theVertices){
    	vertices(theVertices,false);
    }
    
    public void vertices(final List<CCVector3f> theVertices, final boolean theGenerateNormals){
    	if(theVertices.size() == 0){
    		return;
    	}
    	
    	if(theVertices.size() != _myNumberOfVertices){
    		_myNumberOfVertices = theVertices.size();
    		_myVertices = BufferUtil.newFloatBuffer(_myNumberOfVertices * _myVertexSize);
    	}
    	for(CCVector3f myVertex:theVertices){
        	_myVertices.put(myVertex.x);
        	_myVertices.put(myVertex.y);
        	_myVertices.put(myVertex.z);
    	}
    	_myVertices.flip();
    	
    	if(!theGenerateNormals)return;
    	
    	List<CCVector3f> myNormals = new ArrayList<CCVector3f>();

		CCVector3f v1,v2,v3,v21,v31,normal = new CCVector3f(1,0,0);
		
    	switch(_myDrawMode){
    	case QUADS:
    		for(int i = 0; i < theVertices.size();i+=4){
    			v1 = theVertices.get(i);
    			v2 = theVertices.get(i+1);
    			v3 = theVertices.get(i+2);
  
    			v21 = CCVecMath.subtract(v2, v1);
    			v31 = CCVecMath.subtract(v3, v1);
    			
    			normal = v21.cross(v31);
    			normal.normalize();
    			
    			myNormals.add(normal);
    			myNormals.add(normal);
    			myNormals.add(normal);
    			myNormals.add(normal);
    		}
        	normals(myNormals);
    		break;
    	case TRIANGLES:
    		for(int i = 0; i < theVertices.size();i+=3){
    			v1 = theVertices.get(i);
    			v2 = theVertices.get(i+1);
    			v3 = theVertices.get(i+2);
  
    			v21 = CCVecMath.subtract(v2, v1);
    			v31 = CCVecMath.subtract(v3, v1);
    			
    			normal = v21.cross(v31);
    			normal.normalize();
//    			
    			myNormals.add(normal);
    			myNormals.add(normal);
    			myNormals.add(normal);
    		}
        	normals(myNormals);
    		break;
    	}
    }
    
    //////////////////////////////////////////////////////
    //
    //  METHODS TO ADD NORMAL DATA
    //
    //////////////////////////////////////////////////////
    public void addNormal(final float theX, final float theY, final float theZ){
    	if(_myNormals == null)_myNormals = BufferUtil.newFloatBuffer(_myNumberOfVertices * 3);
    	
    	_myNormals.put(theX);
    	_myNormals.put(theY);
    	_myNormals.put(theZ);
    }
    
    public void addNormal(final CCVector3f theNormal){
    	if(_myNormals == null)_myNormals = BufferUtil.newFloatBuffer(_myNumberOfVertices * 3);
    	_myNormals.put(theNormal.x);
    	_myNormals.put(theNormal.y);
    	_myNormals.put(theNormal.z);
    }
    
    public void addNormals(final float[] theNormalData){
    	if(_myNormals == null)_myNormals = BufferUtil.newFloatBuffer(_myNumberOfVertices * 3);
    	_myNormals.put(theNormalData);
    }

    public void normals(final FloatBuffer theNormalBuffer){
    	_myNormals = theNormalBuffer;
    	_myNormals.flip();
    }
    
    public void normals(final List<CCVector3f> theNormals){
    	if(theNormals == null || theNormals.size() == 0)return;
    	_myNormals = BufferUtil.newFloatBuffer(theNormals.size() * 3);
    	for(CCVector3f myNormal:theNormals){
        	_myNormals.put(myNormal.x);
        	_myNormals.put(myNormal.y);
        	_myNormals.put(myNormal.z);
    	}
    	_myNormals.flip();
    }
    
    
    
    //////////////////////////////////////////////////////
    //
    //  METHODS TO ADD TEXTURE COORD DATA
    //
    //////////////////////////////////////////////////////
    
    public void addTextureCoords(final float theX, final float theY){
    	addTextureCoords(0, theX, theY);
    }
    
    public void addTextureCoords(final int theLevel, final CCVector2f theTextureCoords){
    	if(_myTextureCoords[theLevel] == null) {
    		_myTextureCoords[theLevel] = BufferUtil.newFloatBuffer(_myNumberOfVertices * 2);
    		_myTextureCoordSize[theLevel] = 2;
    	}
    	
    	_myTextureCoords[theLevel].put(theTextureCoords.x);
    	_myTextureCoords[theLevel].put(theTextureCoords.y);
    }
    
    public void addTextureCoords(final int theLevel, final float theX, final float theY){
    	if(_myTextureCoords[theLevel] == null) {
    		_myTextureCoords[theLevel] = BufferUtil.newFloatBuffer(_myNumberOfVertices * 2);
    		_myTextureCoordSize[theLevel] = 2;
    	}
    	
    	_myTextureCoords[theLevel].put(theX);
    	_myTextureCoords[theLevel].put(theY);
    }
    
    public void addTextureCoords(final int theLevel, final CCVector3f theTextureCoords){
    	if(_myTextureCoords[theLevel] == null) {
    		_myTextureCoords[theLevel] = BufferUtil.newFloatBuffer(_myNumberOfVertices * 3);
    		_myTextureCoordSize[theLevel] = 3;
    	}
    	
    	_myTextureCoords[theLevel].put(theTextureCoords.x);
    	_myTextureCoords[theLevel].put(theTextureCoords.y);
    	_myTextureCoords[theLevel].put(theTextureCoords.z);
    }
    
    public void addTextureCoords(final int theLevel, final float theX, final float theY, final float theZ){
    	if(_myTextureCoords[theLevel] == null) {
    		_myTextureCoords[theLevel] = BufferUtil.newFloatBuffer(_myNumberOfVertices * 3);
    		_myTextureCoordSize[theLevel] = 3;
    	}
    	
    	_myTextureCoords[theLevel].put(theX);
    	_myTextureCoords[theLevel].put(theY);
    	_myTextureCoords[theLevel].put(theZ);
    }
    
    public void addTextureCoords(final int theLevel, final CCVector4f theTextureCoords){
    	if(_myTextureCoords[theLevel] == null) {
    		_myTextureCoords[theLevel] = BufferUtil.newFloatBuffer(_myNumberOfVertices * 4);
    		_myTextureCoordSize[theLevel] = 4;
    	}
    	
    	_myTextureCoords[theLevel].put(theTextureCoords.x);
    	_myTextureCoords[theLevel].put(theTextureCoords.y);
    	_myTextureCoords[theLevel].put(theTextureCoords.z);
    	_myTextureCoords[theLevel].put(theTextureCoords.w);
    }
    
    public void addTextureCoords(final int theLevel, final float theX, final float theY, final float theZ, final float theW){
    	if(_myTextureCoords[theLevel] == null) {
    		_myTextureCoords[theLevel] = BufferUtil.newFloatBuffer(_myNumberOfVertices * 4);
    		_myTextureCoordSize[theLevel] = 4;
    	}
    	_myTextureCoords[theLevel].put(theX);
    	_myTextureCoords[theLevel].put(theY);
    	_myTextureCoords[theLevel].put(theZ);
    	_myTextureCoords[theLevel].put(theW);
    }
    
    public void textureCoords(final FloatBuffer theTextureCoords){
    	textureCoords(0, theTextureCoords);
    }
    
    public void textureCoords(final List<CCVector2f> theTextureCoords){
    	textureCoords(0, theTextureCoords);
    }
    
    public void textureCoords(final int theLevel, final FloatBuffer theTextureCoords){
    	textureCoords(theLevel, 2, theTextureCoords);
    }
    
    public void textureCoords(final int theLevel, final int theTextureCoordSize, final FloatBuffer theTextureCoords){
    	_myTextureCoords[theLevel] = theTextureCoords;
    	_myTextureCoordSize[theLevel] = theTextureCoordSize;
    }
    
    public void textureCoords(final int theLevel, final List<CCVector2f> theTextureCoords){
    	textureCoords(theLevel, 2, theTextureCoords);
    }
    
    public void textureCoords(final int theLevel, final int theTextureCoordSize, final List<CCVector2f> theTextureCoords){
    	if(theTextureCoords == null || theTextureCoords.size() == 0)return;
    	_myTextureCoords[theLevel] = BufferUtil.newFloatBuffer(theTextureCoords.size() * theTextureCoordSize);
    	_myTextureCoordSize[theLevel] = theTextureCoordSize;
    	for(CCVector2f myTextureCoords:theTextureCoords){
    		_myTextureCoords[theLevel].put(myTextureCoords.x);
    		_myTextureCoords[theLevel].put(myTextureCoords.y);
    	}
    	_myTextureCoords[theLevel].flip();
    }
    
    //////////////////////////////////////////////////////
    //
    //  METHODS TO ADD COLOR DATA
    //
    //////////////////////////////////////////////////////
    
    public void addColor(final float theRed, final float theGreen, final float theBlue, final float theAlpha){
    	if(_myColors == null)_myColors = BufferUtil.newFloatBuffer(_myNumberOfVertices * 4);
    	_myColors.put(theRed);
    	_myColors.put(theGreen);
    	_myColors.put(theBlue);
    	_myColors.put(theAlpha);
    }
    
    public void addColor(final CCColor theColor){
    	addColor(theColor.red(), theColor.green(), theColor.blue(), theColor.alpha());
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
    	if(theColors.size() == 0)return;
    	_myColors = BufferUtil.newFloatBuffer(theColors.size() * 4);
    	for(CCColor myColor:theColors){
    		_myColors.put(myColor.red());
    		_myColors.put(myColor.green());
    		_myColors.put(myColor.blue());
    		_myColors.put(myColor.alpha());
    	}
    	_myColors.flip();
    }
    
    public void colors(final FloatBuffer theColors){
    	_myColors = theColors;
    	_myColors.flip();
    }
    
    public void indices(final List<Integer> theIndices){
    	if(theIndices.size() == 0)return;
    	_myNumberOfIndices = theIndices.size();
    	_myIndices = BufferUtil.newIntBuffer(theIndices.size());
    	for(int myIndex:theIndices){
    		_myIndices.put(myIndex);
    	}
    	_myIndices.flip();
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
    		g.gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
 	   		g.gl.glVertexPointer(_myVertexSize, GL.GL_FLOAT, 0, _myVertices);
    	}
    	if(_myNormals != null){
    		_myNormals.rewind();
    		g.gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
 	   		g.gl.glNormalPointer(GL.GL_FLOAT, 0, _myNormals);
    	}
    	for(int i = 0; i < _myTextureCoords.length;i++) {
	    	if(_myTextureCoords[i] != null){
	    		_myTextureCoords[i].rewind();
	    		g.gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
	    		g.gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
	    		g.gl.glTexCoordPointer(_myTextureCoordSize[i], GL.GL_FLOAT, 0, _myTextureCoords[i]);
	    	}
    	}
    	if(_myColors != null){
    		_myColors.rewind();
    		g.gl.glEnableClientState(GL.GL_COLOR_ARRAY);
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
        	g.gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        }
        if(_myNormals != null){
        	g.gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
        }
        for(int i = 0; i < _myTextureCoords.length;i++) {
	    	if(_myTextureCoords[i] != null){
	    		_myTextureCoords[i].rewind();
	    		g.gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
	        	g.gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
	    	}
    	}
		g.gl.glClientActiveTexture(GL.GL_TEXTURE0);
        if(_myColors != null){
        	g.gl.glDisableClientState(GL.GL_COLOR_ARRAY);
        }
    }
    
    public void drawArray(CCGraphics g){
    	// Draw All Of The Triangles At Once
    	if(_myIndices == null){
    		g.gl.glDrawArrays(_myDrawMode.glID, 0, _myNumberOfVertices);
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
