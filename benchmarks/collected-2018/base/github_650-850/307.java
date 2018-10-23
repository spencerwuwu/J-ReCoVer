// https://searchcode.com/api/result/112629449/

package reuo.client.rendering;

import static java.lang.Math.PI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

import reuo.client.Client;
import reuo.client.rendering.TextureSpace.Partition;
import reuo.resources.ArtLoader;
import reuo.resources.Sprite;
import reuo.resources.Static;
import reuo.resources.StaticLoader;
import reuo.resources.TerrainLoader;
import reuo.resources.TerrainTileLoader;
import reuo.resources.TextureLoader;
import reuo.resources.TerrainLoader.Block.Cell;
import reuo.resources.TerrainTileLoader.Tile;

import com.sun.opengl.util.Animator;
import com.sun.opengl.util.FPSAnimator;
import com.sun.opengl.util.GLUT;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.TextureIO;

/**
 * Provides an OpenGL based renderer as a client instance.
 * @author Kristopher Ives
 */
public class Renderer extends Client implements GLEventListener, KeyListener{
	public static void main(String[] args){
		try{
			Renderer instance = new Renderer(args);
			MapCoordsFrame mapFrame = instance.new MapCoordsFrame();
		}catch(IOException e){
			e.printStackTrace();
		}
    }
	
	JFrame frame;
	GLCanvas glCanvas;
	GLU glu = new GLU();
	GLUT glut = new GLUT();
	Animator animator;
	
	float[] ambient = new float[]{1f, 1f, 1f, 0.5f};
	float[] light = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
	
	Map<Integer, Boolean> keyMap = new HashMap<Integer, Boolean>();
	float map_x = 1475.0f, map_y = 1645.0f;
	float p;
	
	private class MapCoordsFrame extends JFrame{
		JTextField xField = new JTextField();
		JTextField yField = new JTextField();
		JButton go = new JButton("Go");
		
		MapCoordsFrame(){
			this.setSize(200,100);
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.getContentPane().setLayout(new GridLayout());
			this.add(xField);
			this.add(yField);
			this.add(go);
			
			go.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					try{
						map_x = Float.parseFloat(xField.getText());
						map_y = Float.parseFloat(yField.getText());
					}catch(Exception e){
						
					}
				}
			});
			this.setVisible(true);
		}
	}
	
	TerrainLoader terrainLoader;
	TerrainTileLoader tileLoader;
	TextureLoader textureLoader;
	StaticLoader staticLoader;
	ArtLoader artLoader;
	Map<Integer, Texture> textures = new HashMap<Integer, Texture>();
	TextureSpace staticsSpace = new TextureSpace(512, 512);
	Map<Integer, Partition> staticPartitions = new HashMap<Integer, Partition>();
	Texture staticsTexture;
	
	public Renderer(String args[]) throws IOException{
		super(2593);
		
		String dir = args[0];
		
		FileInputStream texIdxStream = new FileInputStream(dir+"texidx.mul");
		FileInputStream texStream = new FileInputStream(dir+"texmaps.mul");
		textureLoader = new TextureLoader(texIdxStream.getChannel(), texStream.getChannel());
		
		FileInputStream terrainSource = new FileInputStream(dir+"map0.mul");
		terrainLoader = new TerrainLoader(terrainSource.getChannel());
		
		FileInputStream tileDataSource = new FileInputStream(dir+"tiledata.mul");
		tileLoader = new TerrainTileLoader(tileDataSource.getChannel());
		
		FileInputStream artIdxSource = new FileInputStream(dir+"artidx.mul");
		FileInputStream artDataSource = new FileInputStream(dir+"art.mul");
		artLoader = new ArtLoader(artIdxSource.getChannel(), artDataSource.getChannel());
		
		FileInputStream staticIdxSource = new FileInputStream(dir+"staidx0.mul");
		FileInputStream staticSource = new FileInputStream(dir+"statics0.mul");		
		staticLoader = new StaticLoader(staticIdxSource.getChannel(), staticSource.getChannel());
		
		frame = new JFrame("reUO");
		frame.setSize(new Dimension(640, 480));
		
		GLCapabilities caps = new GLCapabilities();
		caps.setRedBits(5);
		caps.setGreenBits(5);
		caps.setBlueBits(5);
		caps.setAlphaBits(0);
		caps.setDoubleBuffered(true);
		
		glCanvas = new GLCanvas(caps);
		glCanvas.setSize(640, 480);
		glCanvas.setIgnoreRepaint(true);
		glCanvas.addGLEventListener(this);
		glCanvas.addKeyListener(this);
		
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(glCanvas, BorderLayout.CENTER);
		
		animator = new FPSAnimator(glCanvas, 60);
		animator.setRunAsFastAsPossible(false);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		glCanvas.requestFocus();		
		frame.setVisible(true);
		animator.start();
		

	}
	
	private float getHeight(int x, int y){
		try{
			return(terrainLoader.get(x, y).getLevel());
		}catch(IOException e){
			e.printStackTrace();
			return(0);
		}
	}
	
	public void keyPressed(KeyEvent event){		
		keyMap.put(event.getKeyCode(), true);
	}
	
	public void keyReleased(KeyEvent event){
		keyMap.put(event.getKeyCode(), false);
	}
	
	public void keyTyped(KeyEvent event){}
	
	public boolean isKey(int code){
		Boolean key = keyMap.get(code);
		
		if(key == null){
			return(false);
		}
		
		return(key);
	}
	
	int[] glTextureIds = new int[1];
	Texture crystal;
	
	public void init(GLAutoDrawable glDrawable){
		GL gl = glDrawable.getGL();
		
		gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
		gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
		gl.glPixelStorei(GL.GL_UNPACK_LSB_FIRST, GL.GL_FALSE);
		gl.glPixelStorei(GL.GL_PACK_LSB_FIRST, GL.GL_TRUE);
		gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES, GL.GL_TRUE);
		gl.glPixelStorei(GL.GL_UNPACK_SWAP_BYTES, GL.GL_FALSE);
		
		gl.glGenTextures(1, glTextureIds, 0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, glTextureIds[0]);
		
		Sprite sprite;
		
		try{
			sprite = artLoader.get(17114);
		}catch(IOException e){
			e.printStackTrace();
			return;
		}
		
		//fixTexture(sprite.getPixels());
		
		System.out.printf("width=%d, height=%d\n", sprite.getWidth(), sprite.getHeight());
		
		TextureData data = new TextureData(
			GL.GL_RGBA,
			//sprite.getWidth(), sprite.getHeight(),
			64, 64,
			0,
			GL.GL_RGBA,
			GL.GL_UNSIGNED_SHORT_5_5_5_1,
			false,
			false,
			false,
			sprite.getPixels(),
			null
		);
		
		crystal = TextureIO.newTexture(data);
		
		
		gl.glGenTextures(1, glTextureIds, 0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, glTextureIds[0]);
		
		data = new TextureData(
			GL.GL_RGBA,
			512, 512,
			0,
			GL.GL_RGBA,
			GL.GL_UNSIGNED_SHORT_5_5_5_1,
			false,
			false,
			false,
			ByteBuffer.allocate(512 * 512 * 2),
			null
		);
		
		staticsTexture = TextureIO.newTexture(data);
		
		System.out.printf("GL format = {r=%d, g=%d, b=%d, a=%d}\n",
			glCanvas.getChosenGLCapabilities().getRedBits(),
			glCanvas.getChosenGLCapabilities().getGreenBits(),
			glCanvas.getChosenGLCapabilities().getBlueBits(),
			glCanvas.getChosenGLCapabilities().getAlphaBits()
		);
		
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDisable(GL.GL_DITHER);
		gl.glDisable(GL.GL_CULL_FACE);
		
		gl.glEnable(GL.GL_LIGHTING);
		gl.glEnable(GL.GL_LIGHT1);
		gl.glShadeModel(GL.GL_SMOOTH);
		
		// TODO: Light cycling!
		gl.glLightfv(GL.GL_LIGHT1, GL.GL_AMBIENT, ambient, 0);
		gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, light, 0);
		
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		
		gl.glOrtho(
			-8, 8,
			-6, 6,
			-128, 127
		);
		
		/*
		glu.gluPerspective(
			45.0f,
			640.0 / 480.0,
			1, 1000
		);
		*/
		
		/* Setup texture filters (no filtering) */
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		//gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		//gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		
		gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
		gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
	}
	
	/*
	private void fixTexture(ByteBuffer data){
		int pixel, a, r, g, b;
		
		//System.out.println(data.order());
		
		for(int i=0; i < data.limit(); i += 2){
			pixel = data.getShort(i);
			
			//a = 1;
			r = (pixel & 0x7C00) >> 10;
			g = (pixel & 0x3E0) >> 5;
			b = pixel & 0x1F;
			
			pixel = (r << 11) | ((g * 2) << 5) | b;
			data.putShort(i, (short)pixel);
		}
	}
	*/
	
	private float getWaterLevel(int x, int y){
		return((float)Math.sin((p + (x - y)) * 4) * 2.5f - 10);
	}
	
	boolean saved=false;
	
	public void display(GLAutoDrawable glDrawable){
		//System.out.printf("display = %s\n", Thread.currentThread());
		GL gl = glDrawable.getGL();
		
		p = ((System.currentTimeMillis() % 5000) / 5000.0f) * (float)PI * 2;
		
		float scrollX=0, scrollY=0;
		scrollX += isKey(KeyEvent.VK_RIGHT)	?  8.0f / 60.0f : 0.0f;
		scrollX += isKey(KeyEvent.VK_LEFT)	? -8.0f / 60.0f : 0.0f;
		scrollY += isKey(KeyEvent.VK_UP)		?  8.0f / 60.0f : 0.0f;
		scrollY += isKey(KeyEvent.VK_DOWN)	?  -8.0f / 60.0f : 0.0f;
		float angle = (float)Math.atan2(scrollY, scrollX);
		float distance = (float)Math.sqrt(scrollX * scrollX+scrollY*scrollY);
		scrollX = (float) Math.cos(angle-PI/4+PI/2) * distance;
		scrollY = (float) Math.sin(angle-PI/4+PI/2) * distance;
		
		map_x += scrollX;
		map_y -= scrollY;
		
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		float world_width = 768 * 8;
		float world_height = 512 * 8;
		
		
		gl.glRotatef(-45.0f, 0.0f, 0.0f, 1.0f);
		gl.glScalef(1.0f, -1.0f, 1.0f);
		glu.gluLookAt(
			map_x+3, map_y+4, 64,
			map_x, map_y, 0,
			0, 1, 0
		);
		
		float[] position = new float[]{map_x, map_y, 4f, 1.0f};
		
		//gl.glDisable(GL.GL_LIGHTING);
		gl.glEnable(GL.GL_LIGHT1);
		gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, position, 0);
		
		int startX = (int)Math.round(map_x - 10);
		int startY = (int)Math.round(map_y - 10);
		int endX = (int)Math.round(map_x + 12);
		int endY = (int)Math.round(map_y + 12);
		
		//gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		
		TerrainLoader.Block.Cell cell;
		Texture tileTexture = null;
		Tile tile;
		int tileId;
		int textureId;
		int artId;
		Partition part;
		TextureData data;
		
		if(!saved && isKey(KeyEvent.VK_SPACE)){
			System.out.printf("Forcing save texture...\n");
			
			try{
				TextureIO.write(staticsTexture, new File("test.png"));
			}catch(Exception e){}
			
			saved = true;
		}
		
		for(int x=startX; x < endX; x++){
			for(int y=startY; y < endY; y++){
				try{
					List<Static> statics = staticLoader.get(x, y);
					//System.out.println(statics.size());
					/*
					gl.glPushMatrix();
					gl.glLoadIdentity();
					gl.glRotatef(-45.0f, 0.0f, 0.0f, 1.0f);
					gl.glScalef(1.0f, -1.0f, 1.0f);
					
					glu.gluLookAt(
						0, 0, 0,
						3, 4, 64,
						0, 1, 0
					);
					
					gl.glTranslatef(x, y, 0);
					//gl.glTranslatef(x, y, 0);
					*/
					gl.glBindTexture(GL.GL_TEXTURE_2D, staticsTexture.getTextureObject());
					for(Static sta : statics){
						artId = sta.getArtId();
						part = staticPartitions.get(artId);
						
						if(part == null){
							Sprite sprite = artLoader.get(0x4000 + artId);
							
							if(sprite == null){
								continue;
							}
							
							part = staticsSpace.allocate(sprite.getWidth(), sprite.getHeight());
							
							if(part == null){
								if(!saved){
									TextureIO.write(staticsTexture, new File("test.png"));
									System.out.println("No space left?!");
									saved = true;
								}
								
								break;
							}else{
								staticPartitions.put(artId, part);
							}
							
							//fixTexture(sprite.getPixels());
							System.out.printf("static %dx%d\n", part.getWidth(), part.getHeight());
							
							//sprite.fix16bit();
							
							data = new TextureData(
								GL.GL_RGBA,
								part.getWidth(), part.getHeight(),
								0,
								GL.GL_RGBA,
								GL.GL_UNSIGNED_SHORT_5_5_5_1,
								false,
								false,
								false,
								sprite.getPixels(),
								null
							);
							
							staticsTexture.updateSubImage(
								data, 0,
								part.getX(), part.getY()
							);
						}
						
						if(part != null){
							float minU = part.getX() / 512.0f;
							float minV = part.getY() / 512.0f;
							float maxU = (part.getX() + part.getWidth()) / 512.0f;
							float maxV = (part.getY() + part.getHeight()) / 512.0f;
							float z = sta.getZ();
							
							
							gl.glBegin(GL.GL_QUADS);
							gl.glVertex3f(0, 0, getHeight(x, y)+z);
							gl.glTexCoord2f(maxU, minV);
							
							gl.glVertex3f(0+1, 0, getHeight(x, y)+z);
							gl.glTexCoord2f(maxU, maxV);
							
							gl.glVertex3f(0+1, 0+1, getHeight(x, y+1) + z);
							gl.glTexCoord2f(minU, maxV);
							
							gl.glVertex3f(0, 0+1, getHeight(x, y+1) + z);
							gl.glTexCoord2f(minU, minV);
							gl.glEnd();
							
						}
					}
					
					//gl.glPopMatrix();
					
					cell = terrainLoader.get(x, y);
					tileId = cell.getTileId();
					
					if(tileId < (512 * 32)){
						tile = tileLoader.get(tileId);
						textureId = tile.getTextureId();
						//System.out.printf("textureId = %d\n", textureId);
						
						if(tile.isTextured()){
							tileTexture = textures.get(textureId);
							
							if(tileTexture == null){
								System.out.printf("Loading texture %d\n", tile.getTextureId());
								gl.glGenTextures(1, glTextureIds, 0);
								
								//TextureLoader.Entry entry = textureLoader.getEntry(tile.getTextureId());
								Sprite sprite = textureLoader.get(tile.getTextureId());
								
								data = new TextureData(
									GL.GL_RGBA,
									sprite.getWidth(), sprite.getHeight(),
									0,
									GL.GL_RGBA,
									GL.GL_UNSIGNED_SHORT_5_5_5_1,
									false,
									false,
									true,
									sprite.getPixels(),
									null
								);
								
								tileTexture = TextureIO.newTexture(data);
								textures.put(textureId, tileTexture);
							}
						}else{
							tileTexture = null;
						}
						
					}else{
						tileTexture = null;
					}
				}catch(Exception e){
					e.printStackTrace();
					tileTexture = null;
				}
				
				if(tileTexture == null){
					//float c = (((x + y) % 2) == 0) ? 1.0f : 0.0f;
					//gl.glColor3f(c, c, c);
					continue;
				}else{
					//gl.glColor3f(1.0f, 1.0f, 1.0f);
					//System.out.printf("binding texture %d\n", tileTexture.getTextureObject());
					gl.glBindTexture(GL.GL_TEXTURE_2D, tileTexture.getTextureObject());
				}
				
				//gl.glDisable(GL.GL_TEXTURE_2D);
				//float c = (((x + y) % 2) == 0) ? 1.0f : 0.0f;
				//gl.glColor3f(c, c, c);
				
				
				gl.glBegin(GL.GL_QUADS);
				gl.glVertex3f(x, y, getHeight(x, y));
				gl.glTexCoord2f(1.0f, 0.0f);
				
				gl.glVertex3f(x+1, y, getHeight(x+1, y));
				gl.glTexCoord2f(1.0f, 1.0f);
				
				gl.glVertex3f(x+1, (y+1), getHeight(x+1, y+1));
				gl.glTexCoord2f(0.0f, 1.0f);
				
				gl.glVertex3f(x, (y+1), getHeight(x, y+1));
				gl.glTexCoord2f(0.0f, 0.0f);
				gl.glEnd();
			}
		}
		
		gl.glDisable(GL.GL_LIGHTING);
		int x = (int)map_x, y = (int)map_y;
		float h = 32;//getHeight(x, y);
		gl.glBindTexture(GL.GL_TEXTURE_2D, crystal.getTextureObject());
		
		gl.glBegin(GL.GL_QUADS);
		gl.glVertex3f(x, y, getHeight(x, y)+0.1f);
		gl.glTexCoord2f(1.0f, 0.0f);
		
		gl.glVertex3f(x+1, y, getHeight(x+1, y)+0.1f);
		gl.glTexCoord2f(1.0f, 1.0f);
		
		gl.glVertex3f(x+1, (y+1), getHeight(x+1, y+1)+0.1f);
		gl.glTexCoord2f(0.0f, 1.0f);
		
		gl.glVertex3f(x, (y+1), getHeight(x, y+1)+0.1f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glEnd();
		
		/* Render ghetto water */
		gl.glDisable(GL.GL_LIGHTING);
		gl.glDisable(GL.GL_TEXTURE_2D);
	
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		
		gl.glColor4f(0.0f, 0.5f, 1.0f, 0.66f);
		gl.glBegin(GL.GL_QUADS);
		
		for(x=startX; x < endX; x++){
			for(y=startY; y < endY; y++){
				gl.glVertex3f(x, y, getWaterLevel(x, y));
				gl.glVertex3f(x+1, y, getWaterLevel(x+1, y));
				gl.glVertex3f(x+1, (y+1), getWaterLevel(x+1, y+1));
				gl.glVertex3f(x, (y+1), getWaterLevel(x, y+1));
			}
		}
		
		gl.glEnd();
		
		
		gl.glFlush();
	}
	
	public void displayChanged(GLAutoDrawable glDrawable, boolean modeChanged, boolean deviceChanged){
		
	}
	
	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height){
		final GL gl = glDrawable.getGL();
		gl.glViewport(0, 0, width, height);
	}
	
	/**
	 * Provides fast access to a window of the terrain. An index of cells by texture
	 * is kept updated to reduce texture binds in the rendering process.
	 * @author Kristopher Ives
	 */
	private class TerrainWindow{
		Map<Texture, Cell> cellsByTexture;
		int width, height;
		int cx, cy;
		Cell[][] cells;
		
		/**
		 * Initializes a TerrainWindow with a specified width and height.
		 * @param width the width of the window (horizontal range)
		 * @param height the height of the window (vertical range)
		 */
		private TerrainWindow(int width, int height){
			cells = new Cell[width][height];
			cellsByTexture = new HashMap<Texture, Cell>(width * height);
		}
		
		/**
		 * Updates the window to the new coordiantes. The window will contain
		 * terrain information.
		 * @param cx the horizontal center coordinate of the window
		 * @param cy the vertical center coordinate of the window
		 */
		private void update(int cx, int cy){
			int dx = this.cx - cx;
			int dy = this.cy - cy;
			
			cellsByTexture.clear();
			this.cx = cx;
			this.cy = cy;
		}
	}
}
