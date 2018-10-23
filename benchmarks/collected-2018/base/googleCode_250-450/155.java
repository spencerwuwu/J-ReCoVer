// https://searchcode.com/api/result/13631832/

import processing.core.*;
import processing.xml.*;

import processing.opengl.*;
import remixlab.proscene.Scene;
import codeanticode.glgraphics.*;

import java.applet.*;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.FocusEvent;
import java.awt.Image;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;

import javax.media.opengl.GL;

public class MillionCubes extends PApplet {

	// MillionCubes example.
	// By Andres Colubri
	//
	// Press any key to switch between normal and indexed modes.
	//
	// This example compares the performance between "normal"
	// GLModel usage and GLModel + indexed vertices.
	// Vertex indices allow to reduce the vertex count of a model
	// by reusing vertices shared between quads or triangles.
	//
	// One limitation of using vertex indices occurs when shared
	// vertices need to have different texture coordinates, normals
	// or colors assigned to them, depending on which face they are
	// in. In this case they cannot be shared, and they need to be
	// duplicated for each different texcoord/normal/color value.
	// This discussion is relevant on this issue:
	// http://www.mail-archive.com/android-developers@googlegroups.com/msg86794.html
	//
	// Also note that 1 million cubes might be too taxing for
	// some GPUs. If this sketch runs too slow, decrease the cube
	// count below.

	int cubeCount = 22;
	float cubeSize = 10;
	float volSize = 300;

	GLModel cubes;
	GLModel xcubes;

	boolean usingIndexed = false;

	// The indices that connect the 8 vertices
	// in a single cube, in the form of 12 triangles.
	int cubeIndices[] = { 0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6, 2, 2, 6, 7, 2, 7, 3,
			3, 7, 4, 3, 4, 0, 4, 7, 6, 4, 6, 5, 3, 0, 1, 3, 1, 2 };

	Chronometer chrono;
	Scene scene;

	int[][] invader1 = { { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 },
			{ 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0 },
			{ 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0 },
			{ 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0 },
			{ 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0 },
			{ 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0 },
			{ 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 0 },
			{ 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1 }, };

	public void setup() {
		size(640, 480, GLConstants.GLGRAPHICS);
		scene = new Scene(this);
		scene.setRadius(400);
		chrono = new Chronometer();

		println("Creating cubes...");
		createCubes(invader1);
		createIndexedCubes(invader1);
		println("Done.");

		printInfo();
	}

	public void draw() {
		chrono.update();

		GLGraphics renderer = (GLGraphics) g;
		renderer.beginGL();

		// We get the gl object contained in the GLGraphics renderer.
		GL gl = renderer.gl;

		// Now we can do direct calls to OpenGL:
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);

		// Disabling depth masking to properly render a semitransparent
		// object without using depth sorting.
		gl.glDepthMask(false);
		if (usingIndexed) {
			renderer.model(xcubes);
		} else {
			renderer.model(cubes);
		}
		gl.glDepthMask(true);

		renderer.endGL();
		
		chrono.printFps();
	}

	public void createCubes(int[][] bitmap) {
		int count = 0;
		for (int i = 0; i < bitmap.length; i++)
			for (int j = 0; j < bitmap[i].length; j++)
				if (bitmap[i][j] != 0)
					count++;

		cubes = new GLModel(this, 24 * count, QUADS, GLModel.STATIC);
		cubes.beginUpdateVertices();

		int tempCount = 0;
		for (int i = 0; i < bitmap.length; i++)
			for (int j = 0; j < bitmap[i].length; j++)
				if (bitmap[i][j] != 0) {
					int n0 = 24 * tempCount;
					tempCount++;
					float x0 = (cubeSize * 2 + 1) * i;
					float y0 = (cubeSize * 2 + 1) * j;
					float z0 = 0;
					// Front face
					cubes.updateVertex(n0 + 0, x0 - cubeSize, y0 - cubeSize, z0
							+ cubeSize);
					cubes.updateVertex(n0 + 1, x0 + cubeSize, y0 - cubeSize, z0
							+ cubeSize);
					cubes.updateVertex(n0 + 2, x0 + cubeSize, y0 + cubeSize, z0
							+ cubeSize);
					cubes.updateVertex(n0 + 3, x0 - cubeSize, y0 + cubeSize, z0
							+ cubeSize);
					// Back face
					cubes.updateVertex(n0 + 4, x0 - cubeSize, y0 - cubeSize, z0
							- cubeSize);
					cubes.updateVertex(n0 + 5, x0 + cubeSize, y0 - cubeSize, z0
							- cubeSize);
					cubes.updateVertex(n0 + 6, x0 + cubeSize, y0 + cubeSize, z0
							- cubeSize);
					cubes.updateVertex(n0 + 7, x0 - cubeSize, y0 + cubeSize, z0
							- cubeSize);
					// Rigth face
					cubes.updateVertex(n0 + 8, x0 + cubeSize, y0 - cubeSize, z0
							+ cubeSize);
					cubes.updateVertex(n0 + 9, x0 + cubeSize, y0 - cubeSize, z0
							- cubeSize);
					cubes.updateVertex(n0 + 10, x0 + cubeSize, y0 + cubeSize,
							z0 - cubeSize);
					cubes.updateVertex(n0 + 11, x0 + cubeSize, y0 + cubeSize,
							z0 + cubeSize);
					// Left face
					cubes.updateVertex(n0 + 12, x0 - cubeSize, y0 - cubeSize,
							z0 + cubeSize);
					cubes.updateVertex(n0 + 13, x0 - cubeSize, y0 - cubeSize,
							z0 - cubeSize);
					cubes.updateVertex(n0 + 14, x0 - cubeSize, y0 + cubeSize,
							z0 - cubeSize);
					cubes.updateVertex(n0 + 15, x0 - cubeSize, y0 + cubeSize,
							z0 + cubeSize);
					// Top face
					cubes.updateVertex(n0 + 16, x0 + cubeSize, y0 + cubeSize,
							z0 + cubeSize);
					cubes.updateVertex(n0 + 17, x0 + cubeSize, y0 + cubeSize,
							z0 - cubeSize);
					cubes.updateVertex(n0 + 18, x0 - cubeSize, y0 + cubeSize,
							z0 - cubeSize);
					cubes.updateVertex(n0 + 19, x0 - cubeSize, y0 + cubeSize,
							z0 + cubeSize);
					// Bottom face
					cubes.updateVertex(n0 + 20, x0 + cubeSize, y0 - cubeSize,
							z0 + cubeSize);
					cubes.updateVertex(n0 + 21, x0 + cubeSize, y0 - cubeSize,
							z0 - cubeSize);
					cubes.updateVertex(n0 + 22, x0 - cubeSize, y0 - cubeSize,
							z0 - cubeSize);
					cubes.updateVertex(n0 + 23, x0 - cubeSize, y0 - cubeSize,
							z0 + cubeSize);
				}
		cubes.endUpdateVertices();

		cubes.initColors();
		cubes.setColors(255, 0, 0, 10);
		
	}

	public void keyPressed() {
		usingIndexed = !usingIndexed;
		printInfo();
	}

	public void printInfo() {
		if (usingIndexed)
			println("Drawing indexed vertices");
		else
			println("Drawing non-indexed vertices");
	}

	public void createIndexedCubes(int[][] bitmap) {

		int count = 0;
		for (int i = 0; i < bitmap.length; i++)
			for (int j = 0; j < bitmap[i].length; j++)
				if (bitmap[i][j] != 0)
					count++;

		xcubes = new GLModel(this, 8 * count, TRIANGLES, GLModel.STATIC);

		xcubes.beginUpdateVertices();
		int tempCount = 0;
		for (int i = 0; i < bitmap.length; i++)
			for (int j = 0; j < bitmap[i].length; j++)
				if (bitmap[i][j] != 0) {
					int n0 = 8 * tempCount;
					tempCount++;
					float x0 = (cubeSize * 2 + 1) * i;
					float y0 = (cubeSize * 2 + 1) * j;
					float z0 = 0;
					xcubes.updateVertex(n0 + 0, x0 - cubeSize, y0 - cubeSize,
							z0 - cubeSize);
					xcubes.updateVertex(n0 + 1, x0 + cubeSize, y0 - cubeSize,
							z0 - cubeSize);
					xcubes.updateVertex(n0 + 2, x0 + cubeSize, y0 + cubeSize,
							z0 - cubeSize);
					xcubes.updateVertex(n0 + 3, x0 - cubeSize, y0 + cubeSize,
							z0 - cubeSize);
					xcubes.updateVertex(n0 + 4, x0 - cubeSize, y0 - cubeSize,
							z0 + cubeSize);
					xcubes.updateVertex(n0 + 5, x0 + cubeSize, y0 - cubeSize,
							z0 + cubeSize);
					xcubes.updateVertex(n0 + 6, x0 + cubeSize, y0 + cubeSize,
							z0 + cubeSize);
					xcubes.updateVertex(n0 + 7, x0 - cubeSize, y0 + cubeSize,
							z0 + cubeSize);
				}
		xcubes.endUpdateVertices();

		xcubes.initColors();
		xcubes.setColors(255, 0, 0, 10);

		// Creating vertex indices for all the cubes in the model.
		// Since each cube is identical, the indices are the same,
		// with the exception of the shifting to take into account
		// the position of the cube inside the model.
		int indices[] = new int[36 * count];
		for (int i = 0; i < count; i++) {
			int n0 = 36 * i;
			int m0 = 8 * i;
			for (int j = 0; j < 36; j++) {
				indices[n0 + j] = m0 + cubeIndices[j];
			}
		}

		xcubes.initIndices(36 * count);
		xcubes.updateIndices(indices);
	}

	// An utility class to calculate framerate more accurately.
	class Chronometer {
		int fcount;
		int lastmillis;
		int interval;
		float fps;
		float time;
		boolean updated;

		Chronometer() {
			lastmillis = 0;
			fcount = 0;
			interval = 5;
			updated = false;
		}

		Chronometer(int t) {
			lastmillis = 0;
			fcount = 0;
			interval = t;
			updated = false;
		}

		public void update() {
			fcount++;
			int t = millis();
			if (t - lastmillis > interval * 1000) {
				fps = (float) (fcount) / interval;
				time = (float) (t) / 1000;
				fcount = 0;
				lastmillis = t;
				updated = true;
			} else
				updated = false;
		}

		public void printFps() {
			if (updated)
				PApplet.println("FPS: " + fps);
		}
	}

	static public void main(String args[]) {
		PApplet.main(new String[] { "--bgcolor=#F0F0F0", "MillionCubes" });
	}
}

