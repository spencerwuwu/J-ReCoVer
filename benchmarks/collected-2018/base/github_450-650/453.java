// https://searchcode.com/api/result/96628460/

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
/**
 * The actual game of mine sweeper.  
 * All logic and graphics are handled on a single thread.
 * 
 * @author Jake
 *
 */
public class MinesweeperFrame extends JFrame 
{
	private static final long serialVersionUID = 1L;	//constant used for serializable classes (not required here but is convention)
	private JPanel contentPane;							//JPanel containing all the visual elements of the game
	private Tile[][] tiles;								//Array storing all the game tiles as objects, each containing information on the tile
	private static BufferedImage FLAG_IMAGE;			//flag image
	private static BufferedImage BOMB_IMAGE;			//bomb image
	private static BufferedImage RAISED_IMAGE;			//raised tile image
	private static BufferedImage LOWERED_IMAGE;			//lowered tile image
	private static BufferedImage NUM1_IMAGE;			//number 1 image
	private static BufferedImage NUM2_IMAGE;			//number 2 image
	private static BufferedImage NUM3_IMAGE;			//number 3 image
	private static BufferedImage NUM4_IMAGE;			//number 4 image
	private static BufferedImage NUM5_IMAGE;			//number 5 image
	private static BufferedImage NUM6_IMAGE;			//number 6 image
	private static BufferedImage NUM7_IMAGE;			//number 7 image
	private static BufferedImage NUM8_IMAGE;			//number 8 image
	private final int WIDTH  = 800;						//game window width
	private final int HEIGHT = 600;						//game window height
	private static int wOffset;							//width offset from the top left corner of the screen to top left of grid
	private static int hOffset;							//height offset from the top left corner of the screen to top left of grid
	private static int remaining;						//tiles left unchecked
	private static int remainingBombs;					//potential bombs remaining on field
	private final int gridSize;							//the size of the grid

	/**
	 * The method to open a new game board.
	 * @param gridSize The size of the grid.
	 * @param bombs The number of bombs in the field.
	 */
	public MinesweeperFrame(final int gridSize, final int bombs)
	{
		this.gridSize = gridSize;					//set grid size
		tiles = new Tile[gridSize][gridSize];		//init array of Game tiles
		remaining = gridSize*gridSize;				//set remaining tiles
		remainingBombs = bombs;						//set remaining bombs
		wOffset = WIDTH/2  - (gridSize * 20)/2;		//set wOffset
		hOffset = HEIGHT/2 - (gridSize * 20)/2;		//set hOffset
		
		setTitle("Minesweeper: " + gridSize + " by " + gridSize);	//set title
		setResizable(false);										//don't allow resizing
		setSize(800, 650);											//set frame size
		setDefaultCloseOperation(EXIT_ON_CLOSE);					//terminate on close
		
		try {
			FLAG_IMAGE    = ImageIO.read(new File("flag.png"));		//load image
			BOMB_IMAGE    = ImageIO.read(new File("bomb.png"));		//load image
			RAISED_IMAGE  = ImageIO.read(new File("raised.png"));	//load image
			LOWERED_IMAGE = ImageIO.read(new File("lowered.png"));	//load image
			NUM1_IMAGE    = ImageIO.read(new File("1.png"));		//load image
			NUM2_IMAGE    = ImageIO.read(new File("2.png"));		//load image
			NUM3_IMAGE    = ImageIO.read(new File("3.png"));		//load image
			NUM4_IMAGE    = ImageIO.read(new File("4.png"));		//load image
			NUM5_IMAGE    = ImageIO.read(new File("5.png"));		//load image
			NUM6_IMAGE    = ImageIO.read(new File("6.png"));		//load image
			NUM7_IMAGE    = ImageIO.read(new File("7.png"));		//load image
			NUM8_IMAGE    = ImageIO.read(new File("8.png"));		//load image
		} catch (IOException e) {									//if an image failed to load
			JOptionPane.showMessageDialog(null, "Missing resouce!");//notify user
			System.exit(0);											//terminate application
		}
		
		for(int x = 0; x < gridSize; x++)
		{
			for(int y = 0; y < gridSize; y++)
			{
				tiles[x][y] = new Tile((byte)0, (byte)0);	//fill array with blank tiles
			}
		}
		
		Random rand = new Random();				//create random object					
		for(int i = 0; i < bombs; i++)
		{
			int x = rand.nextInt(gridSize);		//choose random tile X
			int y = rand.nextInt(gridSize);		//choose random tile Y
			
			if(tiles[x][y].getType() == 0x01)	//if tile is already a bomb
			{
				i--;							//place a new bomb else were
			} else {
				tiles[x][y] = new Tile			//make tile a bomb
						((byte)0x01, (byte)0x00);
			}
		}

		contentPane = new JPanel()								//define content pane
		{
			private static final long serialVersionUID = 1L;	//constant used for serializable classes (not required here but is convention)

			@Override
			public void paintComponent(Graphics g)					//overrides paint component to draw game
			{
				Graphics2D g2 = (Graphics2D) g.create();			//create Graphics2D object from content panes graphics
				g2.setColor(Color.LIGHT_GRAY);						//set colour to light grey
				g2.fillRect(0, 0, 125, 100);						//draw rectangle for score keeping
				g2.setColor(Color.BLACK);							//set colour to black
				g2.drawChars("remaining blocks:".toCharArray(),		//write remaining blocks 
						0, 16, 10, 25);
				g2.drawChars((remaining + "").toCharArray(), 		//update remaining blocks
						0, (remaining + "").length(), 10, 40);
				g2.drawChars("remaining bombs:".toCharArray(), 		//write remaining bombs
						0, 15, 10, 65);
				g2.drawChars((remainingBombs + "").toCharArray(), 	//update remaining bombs
						0, (remainingBombs + "").length(), 10, 80);
				
				for(int x = 0; x < gridSize; x++)
				{
					for(int y = 0; y < gridSize; y++)				//cycle through every tile and draw the corresponding image
					{
						g2.drawImage(RAISED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
						switch (tiles[x][y].getGraphicsType())
						{
							case(Tile.GRAPHICS_BLANK):
							{
								g2.drawImage(RAISED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
							case(Tile.GRAPHICS_FLAG):
							{
								g2.drawImage(RAISED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								g2.drawImage(FLAG_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
							case(Tile.GRAPHICS_BOMB):
							{
								g2.drawImage(LOWERED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								g2.drawImage(BOMB_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
							case(Tile.GRAPHICS_1):
							{
								g2.drawImage(LOWERED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								g2.drawImage(NUM1_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
							case(Tile.GRAPHICS_2):
							{
								g2.drawImage(LOWERED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								g2.drawImage(NUM2_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
							case(Tile.GRAPHICS_3):
							{
								g2.drawImage(LOWERED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								g2.drawImage(NUM3_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
							case(Tile.GRAPHICS_4):
							{
								g2.drawImage(LOWERED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								g2.drawImage(NUM4_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
							case(Tile.GRAPHICS_5):
							{
								g2.drawImage(LOWERED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								g2.drawImage(NUM5_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
							case(Tile.GRAPHICS_6):
							{
								g2.drawImage(LOWERED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								g2.drawImage(NUM6_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
							case(Tile.GRAPHICS_7):
							{
								g2.drawImage(LOWERED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								g2.drawImage(NUM7_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
							case(Tile.GRAPHICS_8):
							{
								g2.drawImage(LOWERED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								g2.drawImage(NUM8_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
							case(Tile.GRAPHICS_EXPOSED):
							{
								g2.drawImage(LOWERED_IMAGE, wOffset + (x * 20), hOffset + (y * 20), null);
								break;
							}
						}
					}
				}
			}
		};
		setContentPane(contentPane);	//set the content pane
		
		addMouseListener(new MouseListener()						//create mouse listener for tile interactions
		{
			@Override
			public void mouseClicked(MouseEvent e) 					//on mouse click
			{
				Tile t;												//new tile object
				int mouseX = e.getX();								//mouse X position on game screen
				int mouseY = e.getY();								//mouse Y position on game screen
				int boardX = ((mouseX - (wOffset + 5)) / 20);		//determine x coordinate on grid
				int boardY = ((mouseY - (hOffset + 5)) / 20) - 1;	//determine y coordinate on grid
				
				try{
					t = tiles[boardX][boardY];						//try to retrieve the tile clicked
				} catch (Exception e1)
				{
					return;											//if no tile was clicked then return
				}
				
				if(t != null)										//null check (not nessicary but avoids potential errors)
				{
					if(e.getButton() == MouseEvent.BUTTON1)				//if tile was left clicked
					{		
						if(t.getGraphicsType() == Tile.GRAPHICS_FLAG)	//if tile is flagged then do nothing
						{
							return;
						}
						
						if(t.getType() == Tile.TILE_EMPTY)				//if tile was unselected then select it and check surrounding tiles
						{
							showTile(t, boardX, boardY);
							contentPane.repaint();						//re draw game board
						}
						
						if(t.getType() == Tile.TILE_BOMB)				//if tile was a bomb
						{
							t.setGraphicsType(Tile.GRAPHICS_BOMB);		//show bomb
							contentPane.repaint();						//redraw
							JOptionPane.showMessageDialog(null, "You lose.");	//you lost
							System.exit(0);								//terminate program
						}
					} else if(e.getButton() == MouseEvent.BUTTON3)		//if tile was right clicked
					{
						if(t.getGraphicsType() == Tile.GRAPHICS_FLAG)	//toggle flagged
						{
							t.setGraphicsType(Tile.GRAPHICS_BLANK);
							remainingBombs++;							//add to remaining potential bombs count
						} else if(t.getGraphicsType() == Tile.GRAPHICS_BLANK)	//toggle flagged
						{
							t.setGraphicsType(Tile.GRAPHICS_FLAG);
							remainingBombs--;							//remove from remaining potential bombs count
						}
						contentPane.repaint();							//redraw board
					}
					
					if(remainingBombs == 0)													//if there are no remaining potential bombs
					{
						for(int x = 0; x < gridSize; x++)
						{
							for(int y = 0; y < gridSize; y++)
							{
								if(tiles[x][y].getGraphicsType() == Tile.GRAPHICS_FLAG)
								{
									if(!(tiles[x][y].getType() == Tile.TILE_BOMB))			//check if all tiles flagged are bombs
									{
										return;												//if not then do nothing
									}
								}
							}
						}
						JOptionPane.showMessageDialog(null, "You Win!");					//if so then you win
						System.exit(0); 													//terminate application
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
		});
	}

	/**
	 * The recursive method used to show adjacent tiles
	 * that are not bombs
	 * 
	 * @param t The tile to check around.
	 * @param boardX The X coordinate of the Tile
	 * @param boardY the y coordinate of the Tile
	 */
	private void showTile(Tile t, int boardX, int boardY) 
	{
		int adj = checkAdjacent(boardX, boardY);				//determine number of adjacent bombs
		
		if(!(t.getGraphicsType() == Tile.GRAPHICS_EXPOSED))		//if tile has not already been exposed then reduce the number of remaining tiles
		{
			remaining--;	//TODO: fix the count, it does not de-increment correctly when multiple tiles are revealed
		}
		
		switch(adj)			//set selected Tiles graphics to a number corresponding to the number of adjacent bombs
		{
			case(0):		//if there are no adjacent bombs then
			{
				if(!(t.getGraphicsType() == Tile.GRAPHICS_BLANK))	//make sure that the tile has not already been exposed
				{
					return;
				}
				
				t.setGraphicsType(Tile.GRAPHICS_EXPOSED);			//show the tile
				
				//if the adjacent tile is on the board then recursively show that tile as well
				if(boardX > 0)
				{
					showTile(tiles[boardX-1][boardY]  , boardX-1, boardY);
					if(boardY > 0)
					{
						showTile(tiles[boardX-1][boardY-1], boardX-1, boardY-1);
					}
					if(boardY < (gridSize-1))
					{
						showTile(tiles[boardX-1][boardY+1], boardX-1, boardY+1);
					}
				}
				if(boardX < (gridSize-1))
				{
					showTile(tiles[boardX+1][boardY]  , boardX+1, boardY);
					if(boardY > 0)
					{
						showTile(tiles[boardX+1][boardY-1], boardX+1, boardY-1);
					}
					if(boardY < (gridSize-1))
					{
						showTile(tiles[boardX+1][boardY+1], boardX+1, boardY+1);
					}
				}
				if(boardY > 0)
				{
					showTile(tiles[boardX][boardY-1], boardX, boardY-1);
				}
				if(boardY < (gridSize-1))
				{
					showTile(tiles[boardX][boardY+1], boardX, boardY+1);
				}
				break;
			}
			case(1):
			{
				t.setGraphicsType(Tile.GRAPHICS_1);
				break;
			}
			case(2):
			{
				t.setGraphicsType(Tile.GRAPHICS_2);
				break;
			}
			case(3):
			{
				t.setGraphicsType(Tile.GRAPHICS_3);
				break;
			}
			case(4):
			{
				t.setGraphicsType(Tile.GRAPHICS_4);
				break;
			}
			case(5):
			{
				t.setGraphicsType(Tile.GRAPHICS_5);
				break;
			}
			case(6):
			{
				t.setGraphicsType(Tile.GRAPHICS_6);
				break;
			}
			case(7):
			{
				t.setGraphicsType(Tile.GRAPHICS_7);
				break;
			}
			case(8):
			{
				t.setGraphicsType(Tile.GRAPHICS_8);
				break;
			}
		}
	}

	/**
	 * The method to count the number of adjacent bombs.
	 * 
	 * @param boardX The x coordinate to look around.
	 * @param boardY The y coordinate to look around.
	 * @return The number of adjacent bombs.
	 */
	private int checkAdjacent(int boardX, int boardY) 
	{
		int adjBombs = 0;												//init 0 adjacent bombs
		//for each adjacent tile, if it is in the grid and is a bomb then increase counter, otherwise continue
		try{
			if(tiles[boardX-1][boardY+1].getType() == Tile.TILE_BOMB)
			{
				adjBombs++;
			}
		} catch (ArrayIndexOutOfBoundsException e){}
		try{
			if(tiles[boardX][boardY+1].getType() == Tile.TILE_BOMB)
			{
				adjBombs++;
			}
		} catch (ArrayIndexOutOfBoundsException e){}
		try{
			if(tiles[boardX+1][boardY+1].getType() == Tile.TILE_BOMB)
			{
				adjBombs++;
			}
		} catch (ArrayIndexOutOfBoundsException e){}
		try{
			if(tiles[boardX-1][boardY].getType() == Tile.TILE_BOMB)
			{
				adjBombs++;
			}
		} catch (ArrayIndexOutOfBoundsException e){}
		try{
			if(tiles[boardX+1][boardY].getType() == Tile.TILE_BOMB)
			{
				adjBombs++;
			}
		} catch (ArrayIndexOutOfBoundsException e){}
		try{
			if(tiles[boardX-1][boardY-1].getType() == Tile.TILE_BOMB)
			{
				adjBombs++;
			}
		} catch (ArrayIndexOutOfBoundsException e){}
		try{
			if(tiles[boardX][boardY-1].getType() == Tile.TILE_BOMB)
			{
				adjBombs++;
			}
		} catch (ArrayIndexOutOfBoundsException e){}
		try{
			if(tiles[boardX+1][boardY-1].getType() == Tile.TILE_BOMB)
			{
				adjBombs++;
			}
		} catch (ArrayIndexOutOfBoundsException e){}
		return adjBombs;	//return the number
	}
}

