// https://searchcode.com/api/result/12260824/

package com.googlecode.androbuntu.Turntable3D;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLU;
import android.os.SystemClock;
import android.util.Log;


class Point2d {
	float x, y;
	
	Point2d(float my_x, float my_y) {
		x = my_x;
		y = my_y;
	}
};


public class UbuntuLogoRenderer implements GLSurfaceView.Renderer{

	
	private TurntableWidget parent;
	
	float eye_z, frust_near, frust_far;
	public boolean spin_direction;
	public int spin_increment_multiplier;

	private long last_frame_time;
	private float last_angular_position;	// On a scale from 0 to 1
	
	
	
	
	
	
	
	
	
	
	
    private static final float[] _tempGluUnProjectData = new float[40]; 
    private static final int     _temp_m   = 0; 
    private static final int     _temp_A   = 16; 
    private static final int     _temp_in  = 32; 
    private static final int     _temp_out = 36; 
    public static int gluUnProject(float winx, float winy, float winz, 
    		float model[], int offsetM, 
    		float proj[], int offsetP, 
    		int viewport[], int offsetV, 
    		float[] xyz, int offset) 
    { 
    	/* Transformation matrices */ 
    	//   float[] m = new float[16], A = new float[16]; 
    	//   float[] in = new float[4], out = new float[4]; 
    	/* Normalize between -1 and 1 */ 
    	_tempGluUnProjectData[_temp_in]   = (winx - viewport[offsetV]) * 
    	2f / viewport[offsetV+2] - 1.0f; 
    	_tempGluUnProjectData[_temp_in+1] = (winy - viewport[offsetV+1]) * 
    	2f / viewport[offsetV+3] - 1.0f; 
    	_tempGluUnProjectData[_temp_in+2] = 2f * winz - 1.0f; 
    	_tempGluUnProjectData[_temp_in+3] = 1.0f; 
    	/* Get the inverse */ 
    	android.opengl.Matrix.multiplyMM(_tempGluUnProjectData, _temp_A, 
    			proj, offsetP, model, offsetM); 
    	android.opengl.Matrix.invertM(_tempGluUnProjectData, _temp_m, 
    			_tempGluUnProjectData, _temp_A); 
    	android.opengl.Matrix.multiplyMV(_tempGluUnProjectData, _temp_out, 
    			_tempGluUnProjectData, _temp_m, 
    			_tempGluUnProjectData, _temp_in); 
    	if (_tempGluUnProjectData[_temp_out+3] == 0.0) 
    		return GL10.GL_FALSE; 
    	xyz[offset]  =  _tempGluUnProjectData[_temp_out  ] / 
    	_tempGluUnProjectData[_temp_out+3]; 
    	xyz[offset+1] = _tempGluUnProjectData[_temp_out+1] / 
    	_tempGluUnProjectData[_temp_out+3]; 
    	xyz[offset+2] = _tempGluUnProjectData[_temp_out+2] / 
    	_tempGluUnProjectData[_temp_out+3]; 
    	return GL10.GL_TRUE; 
    } 
	
	
	
	
	
	
	
	
	
	
    public UbuntuLogoRenderer(Context context) {

    	parent = (TurntableWidget) context;
    	

    	last_frame_time = SystemClock.uptimeMillis();
 
    	spin_direction = false;
    	spin_increment_multiplier = 0;
        
        mUbuntu = new UbuntuLogo(parent);
        mProjector = new Projector();

        eye_z = -1.0f;
        frust_near = 1.0f;
        frust_far = 2.0f;
        
    }

    @Override
	public int[] getConfigSpec() {
        if (true) {
            // We want a depth buffer and an alpha buffer
            int[] configSpec = {
                    EGL10.EGL_RED_SIZE,      8,
                    EGL10.EGL_GREEN_SIZE,    8,
                    EGL10.EGL_BLUE_SIZE,     8,
                    EGL10.EGL_ALPHA_SIZE,    8,
                    EGL10.EGL_DEPTH_SIZE,   16,
                    EGL10.EGL_NONE
            };
            return configSpec;
        } else {
            // We want a depth buffer, don't care about the
            // details of the color buffer.
            int[] configSpec = {
                    EGL10.EGL_DEPTH_SIZE,   16,
                    EGL10.EGL_NONE
            };
            return configSpec;
        }
    }

    @Override
	public void surfaceCreated(GL10 gl) {
        /*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glDisable(GL10.GL_DITHER);

      
        /*
         * Some one-time OpenGL initialization can be made here
         * probably based on features of this particular context
         */
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

//        gl.glClearColor(.5f, .5f, .5f, 1);
        gl.glClearColor(0, 0, 0, 0);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glEnable(GL10.GL_ALPHA);
        
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        

		gl.glLineWidth(10.0f);
    }

    @Override
	public void drawFrame(GL10 gl) {
        /*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glDisable(GL10.GL_DITHER);
  
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);


        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        GLU.gluLookAt(gl,
        		0.0f, 0.0f, eye_z,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        );

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

        float[] obj = new float[3];
        float[] model_mat = new float[16];
        float[] proj_mat = new float[16];
        int[] viewport = new int[4];
        
        mProjector.retrieveCurrentProjection(gl, proj_mat);
        mProjector.retrieveCurrentModelView(gl, model_mat);	// Must call this second to remain in "MODELVIEW" mode!
		mProjector.getCurrentViewport(viewport);

		
        float[] desired_reprojection_coords = new float[3];
        desired_reprojection_coords[0] = parent.last_tap[0];
        desired_reprojection_coords[1] = viewport[3] - parent.last_tap[1];
        desired_reprojection_coords[2] = frust_near - eye_z;

//        Log.d("bork0", "Desired window coords: (" + String.valueOf(desired_reprojection_coords[0]) + ", " + String.valueOf(desired_reprojection_coords[1]) + ")");
		
		gluUnProject(desired_reprojection_coords[0], desired_reprojection_coords[1], desired_reprojection_coords[2], model_mat, 0, proj_mat, 0, viewport, 0, obj, 0);


//        Log.d("bork1", "Pos: (" + String.valueOf(obj[0]) + ", " + String.valueOf(obj[1]) + ", " + String.valueOf(obj[2]) + ")");

//        float[] win = new float[3];
//        GLU.gluProject(obj[0], obj[1], -obj[2], model_mat, 0, proj_mat, 0, viewport, 0, win, 0);
//        Log.d("bork2", "Reprojection: (" + String.valueOf(win[0]) + ", " + String.valueOf(win[1]) + ", " + String.valueOf(win[2]) + ")");

        gl.glTranslatef(obj[0], obj[1], 0);
        

        
        // Here we do all of the physics time math:
        long temp_time = SystemClock.uptimeMillis();
        long time_diff = temp_time - last_frame_time;
        last_frame_time = temp_time;
        float elapsed_wall_seconds = time_diff/1000f;
        float base_angular_speed = 10/60f;	// given in revolutions/sec's
  
        
        float direction = spin_direction ? 1 : -1;
        float traversed_angle = spin_increment_multiplier * direction * base_angular_speed * elapsed_wall_seconds;
 
        
        if (parent.finger_touching)
        	traversed_angle /= 3;
        
        float angle = (last_angular_position + traversed_angle) % 1f;
        last_angular_position = angle;
        
        

        gl.glRotatef(angle*360, 0, 0, 1.0f);


        mUbuntu.draw(gl);

        
        mProjector.getCurrentModelView(gl);
    }


    @Override
	public void sizeChanged(GL10 gl, int w, int h) {

        gl.glViewport(0, 0, w, h);
        mProjector.setCurrentView(0, 0, w, h);

        /*
        * Set our projection matrix. This doesn't have to be done
        * each time we draw, but usually a new projection needs to
        * be set when the viewport is resized.
        */

        float ratio = (float) w / h;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, frust_near, frust_far);	// EXPERIMENTAL
        mProjector.getCurrentProjection(gl);
    }

    private UbuntuLogo mUbuntu;
    private Projector mProjector;
}



class UbuntuLogo {
	
    private FloatBuffer DiscVertexBuffer;
    private ShortBuffer DiscIndexBuffer;
    
    private FloatBuffer ArcAnnulusVertexBuffer;
    private ShortBuffer AnnulusIndexBuffer;

    private FloatBuffer GapChunkVertexBuffer;
    private ShortBuffer GapChunkIndexBuffer;
  
    private FloatBuffer LightningVertexBuffer;
    private ShortBuffer LightningIndexBuffer;

    
    private float head_radius;	// This is the distance of the head centers from the origin
    private float head_distance;
    private float arm_thickness;
    private float arm_radius;
    private float arm_gap;
    private float head_border_radius;
    
    private int VERTS;
    private int VERTS2;
    private int VERTS3;
    
    private final static short[] ubuntu_colors = {
    	255, 181, 21,	// Yellow
    	255, 99, 9,	// Orange
    	201, 0, 22,	// Red
    };

	private int LIGHTNING_VERTS = 16;
	Random rand;
	TurntableWidget parent;
    
    public UbuntuLogo(TurntableWidget parent) {
    	
    	this.parent = parent;
    	
    	head_distance = 0.45f;
    	head_radius = 0.1f;
    	head_border_radius = 1.25f;	// This is a multiplier for the head radius, not an absolute distance.
    	arm_thickness = 0.2f;
    	arm_radius = 0.3f;
//    	arm_gap = 1/35f;	// This used to be a fraction of an arc.
    	arm_gap = (head_border_radius - 1)*head_radius;	// Now it is a linear distance that matches the border around the heads.
    	
		rand = new Random();
        
    	// This should be drawn with "TRIANGLE_FAN"
    	Point2d[] circle_vertices = generate_circle_vertices(16, head_radius);
    	VERTS = circle_vertices.length;
    		
        ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
        vbb.order(ByteOrder.nativeOrder());
        DiscVertexBuffer = vbb.asFloatBuffer();

        for (int i = 0; i < VERTS; i++) {
        	DiscVertexBuffer.put(circle_vertices[i].x);
        	DiscVertexBuffer.put(circle_vertices[i].y);
        	DiscVertexBuffer.put(0);
        }

        DiscVertexBuffer.position(0);
        DiscIndexBuffer = indexgen(VERTS);
        
        // -----------------------------------------
        
    	// This should be drawn with "TRIANGLE_STRIP"
//   	Point2d[] arc_vertices = generate_annulus_vertices(16, arm_radius, arm_thickness);
//      Point2d[] arc_vertices = generate_partial_annulus_vertices(16, arm_radius, arm_thickness, 1/3f);
//      Point2d[] arc_vertices = generate_ubuntu_arms(16, arm_radius, arm_thickness);
        Point2d[] arc_vertices = generate_headless_ubuntu_arms(16, arm_radius, arm_thickness, head_border_radius*head_radius, head_distance);
        VERTS2 = arc_vertices.length;
    		
        ByteBuffer vbb2 = ByteBuffer.allocateDirect(VERTS2 * 3 * 4);
        vbb2.order(ByteOrder.nativeOrder());
        ArcAnnulusVertexBuffer = vbb2.asFloatBuffer();

        for (int i = 0; i < VERTS2; i++) {
        	ArcAnnulusVertexBuffer.put(arc_vertices[i].x);
        	ArcAnnulusVertexBuffer.put(arc_vertices[i].y);
        	ArcAnnulusVertexBuffer.put(0);
        } 

        ArcAnnulusVertexBuffer.position(0);
        AnnulusIndexBuffer = indexgen(VERTS2);
        

        // -----------------------------------------
        
        Point2d[] gap_vertices = generate_gap_chunk(16, arm_radius, arm_thickness, head_border_radius*head_radius, head_distance);
        VERTS3 = gap_vertices.length;
    		
        ByteBuffer vbb3 = ByteBuffer.allocateDirect(VERTS3 * 3 * 4);
        vbb3.order(ByteOrder.nativeOrder());
        GapChunkVertexBuffer = vbb3.asFloatBuffer();

        for (int i = 0; i < VERTS3; i++) {
        	GapChunkVertexBuffer.put(gap_vertices[i].x);
        	GapChunkVertexBuffer.put(gap_vertices[i].y);
        	GapChunkVertexBuffer.put(0);
        }

        GapChunkVertexBuffer.position(0);
        GapChunkIndexBuffer = indexgen(VERTS3);
        
        
        
        ByteBuffer vbb4 = ByteBuffer.allocateDirect(LIGHTNING_VERTS * 3 * 4);
        vbb4.order(ByteOrder.nativeOrder());
        LightningVertexBuffer = vbb4.asFloatBuffer();

		LightningIndexBuffer = indexgen(VERTS3);
    }
    
    private ShortBuffer indexgen(int count) {
    	
        ByteBuffer ibb3 = ByteBuffer.allocateDirect(count * 2);
        ibb3.order(ByteOrder.nativeOrder());
        ShortBuffer buf = ibb3.asShortBuffer();
        for (int i = 0; i < count; i++)
        	buf.put((short) i);

        buf.position(0);
        return buf;
    }
    
	// This should be drawn with "TRIANGLE_FAN"
	public Point2d[] generate_circle_vertices(int steps, float radius) {
		
		int num_points = steps + 2;
		Point2d[] circle_vertices = new Point2d[num_points];
		circle_vertices[0] = new Point2d(0, 0);

		for (int i=0; i <= steps; i++) {
			float angle = i / (float) steps;
			
			float arg = angle*2*(float)Math.PI;
			float x = radius * (float) Math.cos(arg);
			float y = radius * (float) Math.sin(arg);
			
			circle_vertices[i+1] = new Point2d(x, y);
		}
		return circle_vertices;
	}
	

	
	// This should be drawn with "TRIANGLE_STRIP"
	public Point2d[] generate_annulus_vertices(int steps, float radius, float width) {
		
		float half_width = width/2;
		if (half_width > radius)
			return null;
		
		int num_points = (steps+1)*2;
		Point2d[] circle_vertices = new Point2d[num_points];
		
		float inner_radius = radius - half_width;
		float outer_ratio = (radius + half_width)/inner_radius;
		for (int i=0; i <= steps; i++) {
			float angle = i / (float) steps;
			float x = inner_radius * (float) Math.cos(angle*2*Math.PI);
			float y = inner_radius * (float) Math.sin(angle*2*Math.PI);
			
			circle_vertices[i*2] = new Point2d(x, y);
			
			x *= outer_ratio;
			y *= outer_ratio;
			circle_vertices[i*2 + 1] = new Point2d(x, y);
		}
		
		return circle_vertices;
	}
    
	
	
	// This should be drawn with "TRIANGLE_STRIP"
	// "arcspan" must be between 0 and 1 inclusive.
	public Point2d[] generate_partial_annulus_vertices(int steps, float radius, float width, float arcspan) {
		
		float half_width = width/2;
		if (half_width > radius)
			return null;
		
		int num_points = (steps+1)*2;
		Point2d[] circle_vertices = new Point2d[num_points];
		
		float inner_radius = radius - half_width;
		float outer_ratio = (radius + half_width)/inner_radius;
		for (int i=0; i <= steps; i++) {
			float angle = arcspan * i / steps;
			
			float arg = (2*angle - arcspan)*(float)Math.PI;
			float x = inner_radius * (float) Math.cos(arg);
			float y = inner_radius * (float) Math.sin(arg);
			
			circle_vertices[i*2] = new Point2d(x, y);
			
			x *= outer_ratio;
			y *= outer_ratio;
			circle_vertices[i*2 + 1] = new Point2d(x, y);
		}
		
		return circle_vertices;
	}
	
	
	
	// This should be drawn with "TRIANGLE_STRIP"
	public Point2d[] generate_ubuntu_arms(int steps, float radius, float width) {
		
		float half_width = width/2;
		if (half_width > radius)
			return null;
		
		int num_points = (steps+1)*2;
		Point2d[] circle_vertices = new Point2d[num_points];
		
		float inner_radius = radius - half_width;
		float outer_radius = radius + half_width;
		float outer_ratio = outer_radius/inner_radius;
			
		float arcspan = 1/3f;
		
		// Here we must translate the linear gap distance to a fraction of an arc.
		float inner_arc_gap = (float) (2*Math.asin( (arm_gap/2.0)/inner_radius ) / (2*Math.PI));
		Log.d("blargo", Float.toString(inner_arc_gap));
		float inner_arc_span = arcspan - inner_arc_gap;
		
		float outer_arc_gap = (float) (Math.asin( Math.sin(inner_arc_gap*2*Math.PI)/outer_ratio ) / (2*Math.PI) );
		float outer_arc_span = arcspan - outer_arc_gap;
		
		
		for (int i=0; i <= steps; i++) {
			
			float angle, arg, x, y;
			
			// Inner contour
			angle = inner_arc_span * i / steps;
			
			arg = (2*angle - inner_arc_span)*(float)Math.PI;
			x = inner_radius * (float) Math.cos(arg);
			y = inner_radius * (float) Math.sin(arg);
			
			circle_vertices[i*2] = new Point2d(x, y);
			
			// Outer contour - is piecewise-defined
			angle = outer_arc_span * i / steps;
			arg = (2*angle - outer_arc_span)*(float)Math.PI;
			x = outer_radius * (float) Math.cos(arg);
			y = outer_radius * (float) Math.sin(arg);

			circle_vertices[i*2 + 1] = new Point2d(x, y);
		}
		
		return circle_vertices;
	}
	
	
	// This should be drawn with "TRIANGLE_FAN"
	public Point2d[] generate_gap_chunk(int steps, float radius, float width, float satellite_circle_radius, float satellite_circle_distance) {

		float half_width = width/2;
		float outer_radius = radius + half_width;
		float inner_radius = radius - half_width;
		
		// Uses law of cosines
		float a, b, c;
		a = satellite_circle_radius;
		b = satellite_circle_distance;
		c = outer_radius;
		float half_head_arc_span = (float) ( Math.acos((a*a + b*b - c*c) / (2*a*b)) / (2*Math.PI) );
		
		Log.d("fark", "mini_circle arc span: "+ half_head_arc_span);
		
		int num_points = steps + 2;
		Point2d[] circle_vertices = new Point2d[num_points];
		circle_vertices[0] = new Point2d(inner_radius, 0);

		for (int i=0; i <= steps; i++) {
			float angle = half_head_arc_span * i / (float) steps;
			
			Log.d("fark", "iteration "+i+": inner_angle: "+ angle);
			
			float arg = angle * 2 * (float) Math.PI;
			float x = satellite_circle_distance - satellite_circle_radius * (float) Math.cos(arg);
			float y = satellite_circle_radius * (float) Math.sin(arg);
			
			circle_vertices[i+1] = new Point2d(x, y);
		}

		
		return circle_vertices;
	}
		
		
	// This should be drawn with "TRIANGLE_STRIP"
	public Point2d[] generate_headless_ubuntu_arms(int steps, float radius, float width, float satellite_circle_radius, float satellite_circle_distance) {
		
		float half_width = width/2;
		if (half_width > radius)
			return null;
		
		int num_points = (steps+1)*2;
		Point2d[] circle_vertices = new Point2d[num_points];
		
		float inner_radius = radius - half_width;
		float outer_radius = radius + half_width;
		float outer_ratio = outer_radius/inner_radius;
			
		float arcspan = 1/6f;
		
		// Here we must translate the linear gap distance to a fraction of an arc.
		float inner_arc_gap = (float) (Math.asin( (arm_gap/2.0)/inner_radius ) / (2*Math.PI));
//		float inner_arc_gap = arm_gap;
		float inner_arc_span = arcspan - inner_arc_gap;
		
		float outer_arc_gap = (float) (Math.asin( Math.sin(inner_arc_gap*2*Math.PI)/outer_ratio ) / (2*Math.PI) );
		float outer_arc_span = arcspan - outer_arc_gap;
		
		// Uses law of cosines
		float a, b, c;
		a = outer_radius;
		b = satellite_circle_distance;
		c = satellite_circle_radius;
		float half_arm_arc_gap = (float) ( Math.acos((a*a + b*b - c*c) / (2*a*b)) / (2*Math.PI) );
		
		float corrected_outer_span = outer_arc_span - half_arm_arc_gap;

		
		for (int i=0; i <= steps; i++) {
			
			float angle, arg, x, y;
			
			// Inner contour
			angle = inner_arc_span * i / steps;
			
			arg = 2*angle*(float)Math.PI;
			x = inner_radius * (float) Math.cos(arg);
			y = inner_radius * (float) Math.sin(arg);
			
			circle_vertices[i*2] = new Point2d(x, y);
			
			
			// Outer contour - is piecewise-defined
			angle = corrected_outer_span * i / steps;
			arg = 2*(angle + half_arm_arc_gap)*(float)Math.PI;
			x = outer_radius * (float) Math.cos(arg);
			y = outer_radius * (float) Math.sin(arg);

			circle_vertices[i*2 + 1] = new Point2d(x, y);
		}
		
		return circle_vertices;
	}

	
	private void regenerate_lightning() {

		float lightning_radius = head_distance;
		float bolt_width = head_radius;
		
        for (int i = 0; i < LIGHTNING_VERTS; i++) {
        	
			float angle = (i + rand.nextFloat()) / LIGHTNING_VERTS;
			float arg = 2*angle*(float)Math.PI;
			
			float perturbed_radius = lightning_radius + bolt_width * (rand.nextFloat() - 0.5f);
			float x = perturbed_radius * (float) Math.cos(arg);
			float y = perturbed_radius * (float) Math.sin(arg);
			
        	LightningVertexBuffer.put(x);
        	LightningVertexBuffer.put(y);
        	LightningVertexBuffer.put(0);
        }
        LightningVertexBuffer.position(0);
	}
	
	private void redraw_lightning(GL10 gl) {

		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, LightningVertexBuffer);
		gl.glDrawElements(GL10.GL_LINE_LOOP, LIGHTNING_VERTS, GL10.GL_UNSIGNED_SHORT, LightningIndexBuffer);
	}
	
    public void draw(GL10 gl) {
        
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glFrontFace(GL10.GL_CW);

    	for (int i=0; i<3; i++) {

    		gl.glColor4f(ubuntu_colors[i*3 + 0]/(float)255, ubuntu_colors[i*3 + 1]/(float)255, ubuntu_colors[i*3 + 2]/(float)255, 1f);
    		// We invert the colors because of a weird bug on the G1
//    		gl.glColor4f(ubuntu_colors[i*3 + 2]/(float)255, ubuntu_colors[i*3 + 1]/(float)255, ubuntu_colors[i*3 + 0]/(float)255, 1f);

       		gl.glPushMatrix();
       		gl.glTranslatef(head_distance, 0, 0);
 
    		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, DiscVertexBuffer);
    		gl.glDrawElements(GL10.GL_TRIANGLE_FAN, VERTS, GL10.GL_UNSIGNED_SHORT, DiscIndexBuffer);
	
    		gl.glPopMatrix();
    		
    		
    		gl.glRotatef(120, 0, 0, 1);

    		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, ArcAnnulusVertexBuffer);
    		gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, VERTS2, GL10.GL_UNSIGNED_SHORT, AnnulusIndexBuffer);

    		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, GapChunkVertexBuffer);
    		gl.glDrawElements(GL10.GL_TRIANGLE_FAN, VERTS3, GL10.GL_UNSIGNED_SHORT, GapChunkIndexBuffer);


       		gl.glPushMatrix();
       		gl.glScalef(1, -1, 1);
            gl.glFrontFace(GL10.GL_CCW);
    		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, ArcAnnulusVertexBuffer);
    		gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, VERTS2, GL10.GL_UNSIGNED_SHORT, AnnulusIndexBuffer);

    		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, GapChunkVertexBuffer);
    		gl.glDrawElements(GL10.GL_TRIANGLE_FAN, VERTS3, GL10.GL_UNSIGNED_SHORT, GapChunkIndexBuffer);

            gl.glFrontFace(GL10.GL_CW);
    		gl.glPopMatrix();
    	}
    	
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, GapChunkVertexBuffer);
		gl.glDrawElements(GL10.GL_TRIANGLE_FAN, VERTS3, GL10.GL_UNSIGNED_SHORT, GapChunkIndexBuffer);
 
    
		if (parent.finger_touching && false) {	// This looks kinda dumb at the moment
			regenerate_lightning();
			
			gl.glDisable(GL10.GL_DEPTH_TEST);
			
			gl.glColor4f(0, 0.9f, 0.7f, 1f);
			redraw_lightning(gl);
			
			gl.glDisable(GL10.GL_DEPTH_TEST);
		}
    
    }
}

