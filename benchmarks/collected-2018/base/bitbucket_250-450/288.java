// https://searchcode.com/api/result/60990681/

package tutorials.nehe;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.glu.GLU;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;

/**
 * This lesson features another rotating textured cube, but it still manages to throw a lot at you
 * regardless.  Aside from using a new texture, lighting and texture filtering are introduced.  In
 * order to get a better view of how textures are affected, keyboard controls for zooming, manually
 * rotating, changing texture filters, and toggling lighting are provided.
 * <p/>
 * There are a bunch of keyboard controls in this lesson: use the arrow keys to spin the cube on the
 * x and y axes.  Zoom in and out with pgup/pgdown.  Hit the 'L' key to toggle lighting, and finally
 * use the 'F' key to toggle between different texture filters (read the source to see what they
 * are).
 */

public class Lesson07 {
    private String windowTitle = "NeHe Lesson 7: Texture Filters, Lighting & Keyboard Control";
    private int windowWidth  = 800;
    private int windowHeight = 600;

    private boolean quitRequested = false;

    private boolean lightingEnabled = false;

    private float xrot = 0.0f;
    private float xdelta = 0.0f;

    private float yrot = 0.0f;
    private float ydelta = 0.0f;

    private float zoom = 0.0f;

    // There's several kinds of lights, but two that we're concerned with here.  The first is
    // "ambient" light, which is essentially the "background light level" of the scene, lighting
    // everything uniformly in all directions.  The four components of the light are the standard
    // RGBA color values (alpha is almost always going to be 1).  With all components at .5,
    // this yields a dim light, but enough to see all the faces of the cube.
    private float[] lightAmbient = {0.5f, 0.5f, 0.5f, 1.0f};

    // Diffuse light comes from a positioned source, and lights up surfaces that face the source
    // directly more than those that are faced away from it.  There's also specular light which
    // is like diffuse except it depends on the viewing angle to also be in the path of the
    // reflection (think glare on shiny objects).  We're not using specular light in this lesson.
    private float[] lightDiffuse = {1.0f, 1.0f, 1.0f, 1.0f};

    // The light is positioned back "behind" on the z axis.  The fourth component in the light
    // position is a scaling factor that will pretty much always be 1.0.
    private float[] lightPosition = {0.0f, 0.0f, 2.0f, 1.0f};

    private int selectedTexture = 0;

    private String texturePath = "tutorials/nehe/files/Crate.png";
    private List<Texture> cubeTextures = new ArrayList<Texture>();

    public static void main(String[] args) throws Exception {
        Lesson07 app = new Lesson07();
        app.run();
    }

    private void initGL() throws IOException {
        DisplayMode dm = Display.getDisplayMode();
        int w = dm.getWidth();
        int h = dm.getHeight();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(45.0f, (float) w / (float) h, 0.1f, 100.0f);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        // glHint is a handy command that tells OpenGL whether to aim for speed or quality on the
        // given setting.  This one says to interpolate colors and textures as smoothly as possible.
        // It doesn't really make much difference in this app, but I'm bringing it in anyway.
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);

        // Here's where we define our light.  OpenGL supports up to eight lights at a time.
        FloatBuffer temp = BufferUtils.createFloatBuffer(4);
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT, (FloatBuffer) temp.put(lightAmbient).rewind());
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, (FloatBuffer) temp.put(lightDiffuse).rewind());
        GL11.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION, (FloatBuffer) temp.put(lightPosition).rewind());
        GL11.glEnable(GL11.GL_LIGHT1);

        // Note that enabling a light doesn't automatically "turn on the lights".  To do that, you
        // need to call this to tell OpenGL to support lighting in general.  This is also toggled
        // whenever we hit the 'L' key.
        if (lightingEnabled)
            GL11.glEnable(GL11.GL_LIGHTING);
        else
            GL11.glDisable(GL11.GL_LIGHTING);

        // A note on the buffers used above: LWJGL doesn't pass arrays to functions that expect
        // arrays, it uses direct buffers for speed reasons.  Future versions of LWJGL may add
        // nicer shortcuts to BufferUtils like toFloatBuffer(myFloatArray), but it's not there yet.

        // Call our texture loading routine (it's big, so I don't want to stuff it all in here)
        loadTextures();
    }


    private void loadTextures() throws IOException {
        Texture tex;

        // Filters filters filters

        // Here, we apply no filtering at all. The ironic thing here is that on modern systems, this
        // will actually look pretty decent, since without any other guidance, OpenGL will choose
        // whatever the default is set to in the driver.   I've disabled it here by commenting it
        // out, since the idea is to progress from the worst to the best.  Feel free to uncomment
        // it to see what it looks like on your system.

        // tex = loadPNG(texturePath);
        // tex.bind();
        // cubeTextures.add(tex);


        // There are two kinds of basic filters: Minification and Magnification (bet you never
        // thought that first one was a word!) aka min and mag.  When a texture is shrunk to
        // smaller than its normal size, the min filter is applied.  Similarly when a texture
        // needs to be stretched, the mag filter gets used.  The simplest algorithm to use is
        // "nearest neighbor", which says to simply duplicate the nearest texel (a pixel in a
        // texture as pointed to by a texture coordinate) when magnifying, and to simply chop off
        // texels when minifying.  It's fast, but it's rough.
        tex = loadPNG(texturePath);
        tex.bind();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        cubeTextures.add(tex);


        // That was about as basic as filtering gets, and the instant you start spinning the cube,
        // you can see it's pretty awful, especially when you spin it around the Y axis (sideways).
        // The wood grain texture in this lesson was chosen for a reason: without aggressive
        // filtering, you get all kinds of moire and shimmer effects.  The effect is even worse when
        // you zoom out (shimmer) and when you zoom in (blocky).

        // The second filter we try is a linear filter.  Instead of simply copying texels when
        // magnifying, linear filtering chooses new color values based on linearly interpolating
        // the neighboring pixels.  This trades blockiness for blurriness when magnifying, and when
        // minifying, it can reduce artifacts slightly, but it's not by much.
        tex = loadPNG(texturePath);
        tex.bind();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        cubeTextures.add(tex);


        // Nearest-neighbor and Linear are basically about it when it comes to magnification
        // filters, since there's not a lot you can do to create image data that just isn't there.
        // When it comes to minification however, there's more weapons left in our arsenal.
        // The first thing we look at is "MIP Mapping".  Break out your Latin textbooks, fellas:
        // MIP stands for "Multum In Parvo", which means "Many In Little".  A mipmap turns a single
        // texture image into a series of images, each half the size of the other, for only 33% more
        // space needed.  For mipmaps to have any effect, they have to be used with one of the two
        // minification filters that understands them.  The two filters are commonly called
        // "bilinear" and "trilinear" filtering, but appear below under their actual names.  We're
        // going to jump straight to trilinear because it adds virtually no overhead on modern GPUs.
        tex = loadPNG(texturePath);
        tex.bind();
        // GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        cubeTextures.add(tex);
        // More on the above:
        // It used to be tricky to generate mipmaps, but not since OpenGL 3.0, where it's literally
        // just a single call to glGenerateMipmap.  I actually have to use this method, because I
        // can't seem to make the old method actually work in LWJGL.  I should also note that
        // hand-decimated mipmaps like you can make in photoshop can still look much nicer than the
        // ones generated by OpenGL, but that's something for another lesson.


        // The best it gets (at least until custom shaders come in) is anisotropic filtering.
        // Explaining exactly how it works is way beyond the scope of this lesson, but in short,
        // the word "anisotropic" means "not directionally uniform"; in other words, it means to
        // apply different parameters for filtering in the horizontal direction than in vertical.
        // When foreshortening occurs, anisotropic filtering can reduce a lot of the blurring that
        // occurs when the normal isotropic algorithms are applied.  You can notice anisotropic
        // filtering at its most dramatic effect by zooming in closely, then rotating the top of the
        // cube toward you slowly.  The regular mipmapped texture is blurry, but the aniso filtered
        // texture is relatively sharp all the way to the far edge.
        tex = loadPNG(texturePath);
        tex.bind();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        // I static import these extension constants because they're huge enough as it is
        float max = GL11.glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, max);
        cubeTextures.add(tex);

    }

    private Texture loadPNG(String path) throws IOException {
        return TextureLoader.getTexture(
                "PNG", ResourceLoader.getResourceAsStream(path), true
        );
    }

    private void renderScene() {
        xrot = (xrot + xdelta) % 360;
        yrot = (yrot + ydelta) % 360;

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glLoadIdentity();

        GL11.glTranslatef(0.0f, 0.0f, -5.0f + zoom);
        GL11.glRotatef(xrot, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(yrot, 0.0f, 1.0f, 0.0f);

        cubeTextures.get(selectedTexture).bind();
        GL11.glBegin(GL11.GL_QUADS);
        {
            // A new call that appears once per face has snuck in here: glNormal.  What glNormal
            // does is describe an "orthonormal unit vector", otherwise called a normal for short.
            // A normal is a vector that describes what direction the next polygons to be drawn
            // are facing.  Normally (har har), a normal is perpendicular to the polygon being
            // drawn, but it doesn't have to be.  By tweaking normals in fine-grained geometry,
            // you can create the illusion of texture -- but we're not going to cover normal mapping
            // for many lessons yet, and these do indeed stick straight out.
            // Note that normals are only meaningful when it comes to lighting: you're essentially
            // telling OpenGL which direction bisects the light reflection angle.  With lighting
            // off, normals have no effect.
            // Finally, your normals should be normalized, i.e. have a length of 1.  You can make
            // this happen automatically by calling glEnable(GL_NORMALIZE).
            GL11.glNormal3f(0.0f, 0.0f, 1.0f);
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex3f(-1.0f, -1.0f, 1.0f);
            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex3f(1.0f, -1.0f, 1.0f);
            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex3f(1.0f, 1.0f, 1.0f);
            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex3f(-1.0f, 1.0f, 1.0f);

            GL11.glNormal3f(0.0f, 0.0f, -1.0f);
            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex3f(-1.0f, -1.0f, -1.0f);
            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex3f(-1.0f, 1.0f, -1.0f);
            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex3f(1.0f, 1.0f, -1.0f);
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex3f(1.0f, -1.0f, -1.0f);

            GL11.glNormal3f(0.0f, 1.0f, 0.0f);
            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex3f(-1.0f, 1.0f, -1.0f);
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex3f(-1.0f, 1.0f, 1.0f);
            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex3f(1.0f, 1.0f, 1.0f);
            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex3f(1.0f, 1.0f, -1.0f);

            GL11.glNormal3f(0.0f, -1.0f, 0.0f);
            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex3f(-1.0f, -1.0f, -1.0f);
            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex3f(1.0f, -1.0f, -1.0f);
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex3f(1.0f, -1.0f, 1.0f);
            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex3f(-1.0f, -1.0f, 1.0f);

            GL11.glNormal3f(1.0f, 0.0f, 0.0f);
            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex3f(1.0f, -1.0f, -1.0f);
            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex3f(1.0f, 1.0f, -1.0f);
            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex3f(1.0f, 1.0f, 1.0f);
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex3f(1.0f, -1.0f, 1.0f);

            GL11.glNormal3f(-1.0f, 0.0f, 0.0f);
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex3f(-1.0f, -1.0f, -1.0f);
            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex3f(-1.0f, -1.0f, 1.0f);
            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex3f(-1.0f, 1.0f, 1.0f);
            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex3f(-1.0f, 1.0f, -1.0f);
        }
        GL11.glEnd();
    }

    /** Reads queued keyboard events and takes appropriate action on them. */
    private void handleInput() throws LWJGLException {
        if (Display.isCloseRequested()) {
            // The display window is being closed
            quitRequested = true;
            return;
        }
        // Level-triggered (held) events
        if (Keyboard.isKeyDown(Keyboard.KEY_UP))
            xdelta -= 0.01;
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN))
            xdelta += 0.01;
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT))
            ydelta -= 0.01;
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT))
            ydelta += 0.01;
        if (Keyboard.isKeyDown(Keyboard.KEY_PRIOR)) // pgup
            zoom += 0.01;
        if (Keyboard.isKeyDown(Keyboard.KEY_NEXT)) // pgdown
            zoom -= 0.01;


        // Edge-triggered (press) events
        while (Keyboard.next()) {
            int key = Keyboard.getEventKey();
            boolean isDown = Keyboard.getEventKeyState();
            if (isDown) {
                switch (key) {
                    case Keyboard.KEY_ESCAPE:
                        quitRequested = true;
                        break;

                    case Keyboard.KEY_RETURN:
                        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU))
                            Display.setFullscreen(!Display.isFullscreen());
                        break;

                    case Keyboard.KEY_L:
                        lightingEnabled = !lightingEnabled;
                        if (lightingEnabled)
                            GL11.glEnable(GL11.GL_LIGHTING);
                        else
                            GL11.glDisable(GL11.GL_LIGHTING);
                        break;

                    case Keyboard.KEY_F:
                        selectedTexture = (selectedTexture + 1) % cubeTextures.size();
                }
            }
        }

    }


    // Everything below here is the same as it was in the previous lesson.

    /** Sets up OpenGL, runs the main loop of our app, and handles exiting */
    public void run() throws Exception {
        initialize();
        try {
            while (!quitRequested) {
                // This is the main loop of our application
                handleInput();      // Process input (e.g. keyboard, mouse, window events)
                renderScene();      // Render the frame to be drawn to the back buffer
                Display.update();   // Display the back buffer, then poll for input
                Display.sync(60);   // Sleep long enough for the app to run at 60FPS
            }
        } catch (Exception e) {
            Sys.alert(windowTitle, "An error occured -- now exiting.");
            e.printStackTrace();
            System.exit(0);
        } finally {
            cleanup();
        }
    }

    /** Sets up the window and sets up openGL options. */
    private void initialize() throws Exception {
        initDisplay();  // Get a display window
        initGL();       // Set options and initial projection
    }

    /** Creates a new window and sets options on it */
    private void initDisplay() throws LWJGLException {
        DisplayMode mode = new DisplayMode(windowWidth, windowHeight);
        Display.setDisplayMode(mode);
        Display.setTitle(windowTitle);
        Display.setVSyncEnabled(true);  
        Display.create();
    }

    /** Perform final actions to release resources. */
    private void cleanup() {
        Display.destroy();
    }


}

