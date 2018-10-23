// https://searchcode.com/api/result/123053236/

package gfxObjects.polygons;

import java.nio.BufferOverflowException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL12;

import util.MemoryObject;
import util.ResourceManager;
import util.VBO;
import util.VecMath;
import gfx.ShaderManager;
import gfxObjects.Entity;
import gfxObjects.Triangle;
import static gfx.glListObjects.GL_GRND;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

public class DiamondSquareGround extends Entity implements MemoryObject {
    private int vertexBufferID;
    private int indexBufferID;
    private int colourBufferID;
    private int normalBufferID;
    int arraySize;
    float normal[][][][];
    float[][] zArray;
//    List<Triangle> tList;
    FloatBuffer matSpecular;
    double w = 1;
    double h = 1;
    double d = 2;
    Random rng;

    public DiamondSquareGround(float x, float y, float z, int arraySize) {
	super(x, y, z);
	ResourceManager.getInstance().addMemoryObject(this);
	this.arraySize = arraySize;
//	tList = new ArrayList<Triangle>(arraySize * arraySize * 2);
	// Hur "kantig" miljon ska vara
	float H = 32;
//	rng = new Random(Sys.getTime());
	rng = new Random(1);
	// Skapa materialet
	matSpecular = BufferUtils.createFloatBuffer(4);
	matSpecular.put(0.0f).put(0.0f).put(0.0f).put(1.0f).flip();
	// Skapar en array som ska lagra vara z-varden
	zArray = new float[arraySize][arraySize];
	// Fyller matrisen med varden for att visa icke-initierade platser.
	for (int i = 0; i < zArray.length; i++) {
	    for (int j = 0; j < zArray[i].length; j++) {
		zArray[i][j] = 0xDEADBEEF;
	    }
	}
	// Satter varden pa hornen.
	zArray[0][0] = 0;
	zArray[0][arraySize - 1] = 0;
	zArray[arraySize - 1][0] = 0;
	zArray[arraySize - 1][arraySize - 1] = 0;

	// printArray(zArray);
	// While the length of the side of the squares is greater than zero {
	// Pass through the array and perform the diamond step for each square
	// present.
	// Pass through the array and perform the square step for each diamond
	// present.
	// Reduce the random number range. }
	// I det forsta stegen finns det bara en kvadrat. Alltsa ar langden pa
	// sidan lika lang som matrisen.
	int sideLength = zArray.length;
	while (noBeef(zArray) == false) {
	    zArray = diamond(zArray, sideLength - 1, H);
	    zArray = square(zArray, sideLength - 1, H);
	    sideLength = (sideLength + 1) / 2;
	    H = H / 2;
	}
	
	float[] n;
	colourBufferID = VBO.createVBOID();
	FloatBuffer colourData = BufferUtils.createFloatBuffer(arraySize * arraySize * 6 * 4);
	vertexBufferID = VBO.createVBOID();
	FloatBuffer vertexData = BufferUtils.createFloatBuffer(arraySize * arraySize * 6 * 3);
	normalBufferID = VBO.createVBOID();
	FloatBuffer normalData = BufferUtils.createFloatBuffer(arraySize * arraySize * 6 * 3);
	indexBufferID = VBO.createVBOID();
	IntBuffer indexBuffer = BufferUtils.createIntBuffer(arraySize * arraySize * 6);
	for (int i = 0; i < arraySize * arraySize * 6; i++) {
	    indexBuffer.put(i);
	}
	for (int i = 0; i < arraySize - 1; i++) {
	    for (int j = 0; j < arraySize - 1; j++) {
		for (int j2 = 0; j2 < 6; j2++) {
		    colourData.put(0.623529412f);
		    colourData.put(0.545098039f);
		    colourData.put(0.439215686f);
		    colourData.put(1.0f);
		}
		
		n = VecMath.cross(
			new float[]{i + x, j + y, zArray[i][j] + z},
			new float[]{i + x, j + y + 1, zArray[i][j + 1] + z},
			new float[]{i + x + 1, j + y + 1, zArray[i + 1][j + 1] + z}	
		);
		//Triangel 1
		normalData.put(n);
		vertexData.put(i + x);
		vertexData.put(j + y);
		vertexData.put(zArray[i][j] + z);
		
		normalData.put(n);
		vertexData.put(i + x);
		vertexData.put(j + y + 1);
		vertexData.put(zArray[i][j + 1] + z);
		
		normalData.put(n);
		vertexData.put(i + x + 1);
		vertexData.put(j + y + 1);
		vertexData.put(zArray[i + 1][j + 1] + z);
		
		n = VecMath.cross(
			new float[]{i + x, j + y, zArray[i][j] + z},
			new float[]{i + x + 1, j + y, zArray[i + 1][j] + z},
			new float[]{i + x + 1, j + y + 1, zArray[i + 1][j + 1] + z}	
		);
		//Triangel 2
		normalData.put(n);
		vertexData.put(i + x);
		vertexData.put(j + y);
		vertexData.put(zArray[i][j] + z);

		normalData.put(n);
		vertexData.put(i + x + 1);
		vertexData.put(j + y);
		vertexData.put(zArray[i + 1][j] + z );
		
		normalData.put(n);
		vertexData.put(i + x + 1);
		vertexData.put(j + y + 1);
		vertexData.put(zArray[i + 1][j + 1] + z);
	    }
	}
	vertexData.flip();
	VBO.bufferData(vertexBufferID, vertexData);
	
	normalData.flip();
	VBO.bufferData(normalBufferID, normalData);
	
	colourData.flip();
	VBO.bufferData(colourBufferID, colourData);

	indexBuffer.flip();
	VBO.bufferElementData(indexBufferID, indexBuffer);
	
//	this.makeList();
	
    }
    
    @Override
    public void draw() {
	glMaterial(GL_FRONT, GL_SPECULAR, matSpecular);				// sets specular material color
	glMaterialf(GL_FRONT, GL_SHININESS, 75.0f);
	glEnableClientState(GL_VERTEX_ARRAY);
	ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, vertexBufferID);
	glVertexPointer(3, GL_FLOAT, 0, 0);
	 
	glEnableClientState(GL_NORMAL_ARRAY);
	ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, normalBufferID);
	glNormalPointer(GL_FLOAT, 0, 0);
	  
	glEnableClientState(GL_COLOR_ARRAY);
	ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, colourBufferID);
	glColorPointer(4, GL_FLOAT, 0, 0);
	 
	ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, indexBufferID);
	GL12.glDrawRangeElements(GL_TRIANGLES, 0, arraySize * arraySize * 6, arraySize * arraySize * 6, GL_UNSIGNED_INT, 0);
    }

//    public void checkPlayer(int x, int y) {
//	tList.get(x + y * arraySize).setR(1.0f);
//	tList.get(x + y * arraySize).setG(0.0f);
//	tList.get(x + y * arraySize).setB(1.0f);
//    }

    private boolean noBeef(float[][] zArray) {
	for (int i = 0; i < zArray.length; i++) {
	    for (int j = 0; j < zArray[i].length; j++) {
		if (zArray[i][j] == 0xDEADBEEF) {
		    return false;
		}
	    }
	}
	return true;
    }

    private float[][] square(float[][] zArray, int sideLength, float H) {
	float[][] tArray = zArray;
	int sideHalf = (sideLength / 2);
	for (int i = 0; i < zArray.length; i++) {
	    for (int j = 0; j < zArray[i].length; j++) {
		// Finns det nagot vettigt varde i faltet?
		if (zArray[i][j] != 0xDEADBEEF) {
		    // System.out.println("i: " + i);
		    // System.out.println("j: " + j);
		    // System.out.println("zArray.length: " + zArray.length);
		    // System.out.println("sideLength: " + sideLength);
		    // System.out.println("sideHalf: " + sideHalf);
		    // System.out.println("");
		    if (i + sideHalf < zArray.length &&
			    j + sideHalf < zArray.length && 
			    i - sideHalf >= 0 && j - sideHalf >= 0) {
			// Ar det mitt-punkten av en kvadrat?
			if (zArray[i + sideHalf][j + sideHalf] != 0xDEADBEEF && 
				zArray[i - sideHalf][j + sideHalf] != 0xDEADBEEF && 
				zArray[i + sideHalf][j - sideHalf] != 0xDEADBEEF && 
				zArray[i + sideHalf][j - sideHalf] != 0xDEADBEEF) {
			    // Gor en diamant. Vardet bestams av medelvarden.
			    tArray[i + sideHalf][j] = diamondAdd(zArray, i + sideHalf, j, sideHalf, H);
			    tArray[i - sideHalf][j] = diamondAdd(zArray, i - sideHalf, j, sideHalf, H);
			    tArray[i][j + sideHalf] = diamondAdd(zArray, i, j + sideHalf, sideHalf, H);
			    tArray[i][j - sideHalf] = diamondAdd(zArray, i, j - sideHalf, sideHalf, H);
			}
		    }
		}
	    }
	}
	return tArray;
    }

    private float diamondAdd(float[][] zArray, int x, int y, int sideHalf, float H) {
	// TODO Auto-generated method stub
	int div = 0;
	float addAmount = 0;
	if (x + sideHalf < zArray.length) {
	    addAmount += zArray[x + sideHalf][y];
	    div++;
	}
	if (y + sideHalf < zArray.length) {
	    addAmount += zArray[x][y + sideHalf];
	    div++;
	}
	if (x - sideHalf > 0) {
	    addAmount += zArray[x - sideHalf][y];
	    div++;
	}
	if (y - sideHalf > 0) {
	    addAmount += zArray[x][y - sideHalf];
	    div++;
	}
	return (float) ((addAmount / div) + (2 * rng.nextDouble() - 0.5) * H);
    }

    private float[][] diamond(float[][] zArray, int sideLength, float H) {
	// Gar igenom matrisen och skapar mittpunkter for alla fyrkanter.
	float[][] tArray = zArray;
	int sideHalf = (sideLength / 2);
	for (int i = 0; i < zArray.length; i++) {
	    for (int j = 0; j < zArray[i].length; j++) {
		// Finns det nagot vettigt varde i faltet?
		if (zArray[i][j] != 0xDEADBEEF) {
		    // System.out.println("i: " + i);
		    // System.out.println("j: " + j);
		    // System.out.println("sideLength: " + sideLength);
		    // System.out.println("");
		    // Forsakra att fyrkanten inte ar storre an matrisen.
		    if (i + sideLength < zArray.length && j + sideLength < zArray.length) {
			// Kolla om faltet ar det ovre vanstra hornet i en
			// kvadrat med storleken sideLength
			if (zArray[i + sideLength][j] != 0xDEADBEEF && zArray[i][j + sideLength] != 0xDEADBEEF && zArray[i + sideLength][j + sideLength] != 0xDEADBEEF) {
			    // okej, alla fragor gick igenom. Skapa en
			    // mitt-punkt.
			    // Vardet pa mittpunkten ar medelvardet pa
			    // fyrkantens horn + lite slumpmassighet.
			    tArray[i + sideHalf][j + sideHalf] = (float) ((2 * rng.nextDouble() - 0.5) * H) + 
				    (zArray[i][j] + 
					    zArray[i + sideLength][j] + 
					    zArray[i][j + sideLength] + 
					    zArray[i + sideLength][j + sideLength]) / 4;
			}
		    }
		}
	    }
	}
	return tArray;
    }
    public float getY(float x, float z){
		return zArray[(int) x][(int) z];
    }
    @Override public void free() {
	// TODO Auto-generated method stub
	glDeleteBuffers(colourBufferID);
	glDeleteBuffers(vertexBufferID);
	glDeleteBuffers(indexBufferID);
	glDeleteBuffers(normalBufferID);
    }
}

