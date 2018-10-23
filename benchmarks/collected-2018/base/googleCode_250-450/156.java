// https://searchcode.com/api/result/13631835/

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

public class CopyOfMillionCubes extends PApplet {

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

	int cubeCount = 1;
	float cubeSize = 50;
	float volSize = 300;

	GLModel cubes;
	GLModel xcubes;

	boolean usingIndexed = false;

	// The indices that connect the 8 vertices
	// in a single cube, in the form of 12 triangles.
	int cubeIndices[] = { 0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6, 2, 2, 6, 7, 2, 7, 3,
			3, 7, 4, 3, 4, 0, 4, 7, 6, 4, 6, 5, 3, 0, 1, 3, 1, 2 };

	int[][] invader1 = { { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 },
			{ 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0 },
			{ 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0 },
			{ 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0 },
			{ 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0 },
			{ 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0 },
			{ 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 0 },
			{ 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1 }, };

	int[][] invader10 = { { 1, 1, 1, 1, 1 }, { 1, 0, 0, 0, 0} };

	int[][] invader2 = { { 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } };

	Chronometer chrono;
	GLModel[] invadersModel;
	Scene scene;

	public void setup() {
		size(640, 480, GLConstants.GLGRAPHICS);
		scene = new Scene(this);
		scene.setRadius(4000);
		chrono = new Chronometer();

		println("Creating cubes...");
		invadersModel = new GLModel[cubeCount];
		for (int i = 0; i < cubeCount; i++) {
			GLModel cube = createIndexedCube(invader10);
			// GLModel cube = createSimple();
			invadersModel[i] = cube;
		}
		println("Done.");

		printInfo();
	}

	public void draw() {
		chrono.update();

		GLGraphics renderer = (GLGraphics) g;
		renderer.beginGL();

		for (int i = 0; i < cubeCount; i++) {
			renderer.model(invadersModel[i]);
		}

		renderer.endGL();

		chrono.printFps();
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

	public GLModel createSimple() {

		int count = 1;
		GLModel xcube = new GLModel(this, 8 * count, TRIANGLES, GLModel.STREAM);

		xcube.beginUpdateVertices();

		float x0 = 0;
		float y0 = 0;
		float z0 = 0;
		int n0 = 0;

		createCube(xcube, n0, x0, y0, z0);

		xcube.endUpdateVertices();

		xcube.initColors();
		xcube.setColors(255, 0, 0, 100);

		// Creating vertex indices for all the cubes in the model.
		// Since each cube is identical, the indices are the same,
		// with the exception of the shifting to take into account
		// the position of the cube inside the model.

		int indices[] = new int[36];
		for (int j = 0; j < 36; j++) {
			indices[j] = cubeIndices[j];
		}

		xcube.initIndices(36);
		xcube.updateIndices(indices);
		return xcube;
	}

	public GLModel createIndexedCube(int[][] bitmap) {

		int count = 0;
		for (int i = 0; i < bitmap.length; i++)
			for (int j = 0; j < bitmap[i].length; j++)
				if (bitmap[i][j] != 0)
					count++;

		GLModel xcube = new GLModel(this, 8 * count, TRIANGLES, GLModel.STATIC);

		xcube.beginUpdateVertices();

		float x0 = -cubeSize;
		float y0 = -cubeSize;
		float z0 = 0;

		int cubeCount = 0;

		for (int i = 0; i < bitmap.length; i++) {
			x0 += cubeSize + 1;
			for (int j = 0; j < bitmap[i].length; j++) {
				y0 += cubeSize + 1;
				if (bitmap[i][j] != 0) {
					int n0 = 8 * cubeCount;
					createCube(xcube, n0, x0, y0, z0);
					cubeCount++;
					xcube.setEmission(0, 0, 255, 60);
				}
			}
			y0 = -cubeSize;
		}

		xcube.endUpdateVertices();

		xcube.initColors();
		xcube.setColors(255, 155, 200, 80);

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

		xcube.initIndices(36 * count);
		xcube.updateIndices(indices);
		return xcube;
	}

	private void createCube(GLModel xcube, int n0, float x0, float y0, float z0) {
		xcube.updateVertex(n0 + 0, x0 - cubeSize, y0 - cubeSize, z0 - cubeSize);
		xcube.updateVertex(n0 + 1, x0 + cubeSize, y0 - cubeSize, z0 - cubeSize);
		xcube.updateVertex(n0 + 2, x0 + cubeSize, y0 + cubeSize, z0 - cubeSize);
		xcube.updateVertex(n0 + 3, x0 - cubeSize, y0 + cubeSize, z0 - cubeSize);
		xcube.updateVertex(n0 + 4, x0 - cubeSize, y0 - cubeSize, z0 + cubeSize);
		xcube.updateVertex(n0 + 5, x0 + cubeSize, y0 - cubeSize, z0 + cubeSize);
		xcube.updateVertex(n0 + 6, x0 + cubeSize, y0 + cubeSize, z0 + cubeSize);
		xcube.updateVertex(n0 + 7, x0 - cubeSize, y0 + cubeSize, z0 + cubeSize);
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
		PApplet.main(new String[] { "--bgcolor=#F0F0F0", "CopyOfMillionCubes" });
	}
}

