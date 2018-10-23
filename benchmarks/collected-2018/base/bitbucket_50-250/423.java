// https://searchcode.com/api/result/76404848/

package gui;

import gui.SpriteMap.SpriteType;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import core.*;

public class GameCanvas extends Canvas implements MouseListener {
	private static final long serialVersionUID = 1L;

	private JFrame frame;
	private SpriteMap spriteMap;
	private GinRummyGame theGame;
	private boolean shouldHideCards = true, shouldRevealDeck = false, canClickDiscard=true;
	private int clickedCard=-1, clickedPile=-1;
	private Font playerFont = new Font("Book Antiqua", Font.BOLD, 18);
	private Color cardShader = new Color(255,238,0,127);
	private Image offscreenBuffer;
	
	public GameCanvas(JFrame frame, SpriteMap spriteMap, GinRummyGame theGame) {
		this.frame = frame;
		this.spriteMap = spriteMap;
		this.theGame = theGame;
		setBackground(new Color(0, 130, 59));
		this.addMouseListener(this);
	}
	
	@Override
	public void paint(Graphics gfx) {
		/* Double buffer to reduce flickering */
		if(offscreenBuffer==null)
			offscreenBuffer = createImage(getWidth(), getHeight());
		Graphics2D gfxBuffer = (Graphics2D) offscreenBuffer.getGraphics();
		gfxBuffer.clearRect(0,0,getWidth(), getHeight());
		drawScreen(gfxBuffer);
		((Graphics2D) gfx).drawImage(offscreenBuffer,0,0,null);
//		gfxBuffer.dispose();
//		gfxBuffer=null;
	 }

	private void drawScreen(Graphics2D gfx2D) {
		if(theGame.currentPlayer instanceof ComputerPlayer) {
			/* Get card selection from AI if computer player. */
			clickedPile = ((ComputerPlayer)theGame.currentPlayer).getPileSelection();
			clickedCard = ((ComputerPlayer)theGame.currentPlayer).getWeakestCardIndex((shouldRevealDeck=clickedPile==1));
		}
		
		/* Draw hand and opponent cards */
		for (int i=0;i<7;i++) {
			if(shouldHideCards&&!theGame.areAllPlayersComputer()) {
				gfx2D.setColor(Color.DARK_GRAY);
				gfx2D.fillRect((i*50)+50, 290, 40, 60);
				gfx2D.setColor(Color.WHITE);
				gfx2D.drawString ("?", (i*50)+66, 316);
			} else {
				gfx2D.setColor(i==clickedCard?Color.YELLOW:Color.WHITE);
				gfx2D.fillRect((i*50)+50, 290, 40, 60);
				gfx2D.setColor(Color.DARK_GRAY);
				gfx2D.drawRect((i*50)+51, 291, 37, 57);
				gfx2D.setColor((theGame.currentPlayer.hand.get(i).isRed())?Color.RED:Color.BLACK);
				gfx2D.drawString (theGame.currentPlayer.hand.get(i).toString(), (i*50)+59, 316);
			}
			gfx2D.drawImage(spriteMap.getSprite(SpriteType.CARDBACK), null, (i*30)+122, 20);
			if(theGame.getNumOfPlayers()>2) {
				gfx2D.drawImage(spriteMap.getSprite(SpriteType.CARDBACKROT), null, 20-20, (i*30)+60);
				gfx2D.drawImage(spriteMap.getSprite(SpriteType.CARDBACKROT), null, 392-20, (i*30)+60);
			}
		}
		
		/* Draw piles */
		gfx2D.setColor(clickedPile==0?Color.YELLOW:Color.WHITE);
		gfx2D.fillRect(150, 130, 50, 75);
		gfx2D.setColor(Color.DARK_GRAY);
		gfx2D.drawRect(151, 131, 47, 72);
		gfx2D.setColor(theGame.discardPile.peek().isRed()?Color.RED:Color.BLACK);
		gfx2D.drawString (theGame.discardPile.peek().toString(), 165, 161);
		if(shouldRevealDeck) {
			gfx2D.setColor(clickedPile==1?Color.YELLOW:Color.WHITE);
			gfx2D.fillRect(240, 130, 50, 75);
			gfx2D.setColor(Color.DARK_GRAY);
			gfx2D.drawRect(241, 131, 47, 72);
			gfx2D.setColor((theGame.cardDeck.deck.peek().isRed())?Color.RED:Color.BLACK);
			gfx2D.drawString (theGame.cardDeck.deck.peek().toString(), 255, 161);
		} else
			gfx2D.drawImage(spriteMap.getSprite(SpriteType.CARDBACKLARGE), null, 240, 130);
		if(clickedPile==1) {
			gfx2D.setColor(cardShader);
			gfx2D.fillRect(240, 130, 50, 75);
		}
		
		/* Draw info messages */
		gfx2D.setColor(Color.BLUE);
		gfx2D.setFont(playerFont);
		gfx2D.drawString ("Player "+(theGame.currentPlayer.playerID+1), 190, 280);
		if(theGame.currentPlayer instanceof ComputerPlayer)
			gfx2D.drawString("Click to finish Computer's turn", 90, 260);
		else if(shouldHideCards)
			gfx2D.drawString("Click to begin turn", 140, 260);
		else if(clickedCard>=0&&clickedPile>=0)
			gfx2D.drawString("Click to finish turn", 139, 260);
	}

	//mousePressed instead of mouseClicked because mouseClicked ignores accidental drags.
	@Override
	public void mousePressed(MouseEvent mouseEvent) {
		if(shouldHideCards&&!(theGame.currentPlayer instanceof ComputerPlayer)) {
			shouldHideCards=false;
			clickedPile=clickedCard=-1;
			repaint();
			return;
		}
		if(clickedCard>=0&&clickedPile>=0) {
			theGame.currentPlayer.playMove(clickedCard, clickedPile);
			if(theGame.currentPlayer.hasWon()) {
				shouldHideCards=false;
				repaint();
				JOptionPane.showMessageDialog(frame, "Player "+(theGame.currentPlayer.playerID+1)+" has won the game!", "Congratulations!", JOptionPane.INFORMATION_MESSAGE);
				System.exit(0);
			}
			shouldRevealDeck=false;
			shouldHideCards=true;
			canClickDiscard=true;
			theGame.setNextPlayerAsCurrent();
			repaint();
			return;
		}
		if(theGame.currentPlayer instanceof ComputerPlayer)
			return;
		int posX=mouseEvent.getX(), posY=mouseEvent.getY();
		if(canClickDiscard&&posX>150&&posX<200&&posY>130&&posY<205){
			clickedPile=0;
			canClickDiscard=false;
			repaint();
		}else if(canClickDiscard&&posX>240&&posX<290&&posY>130&&posY<205) {
			clickedPile=1;
			canClickDiscard=false;
			shouldRevealDeck=true;
			repaint();
		}else if(!canClickDiscard) {
			if(clickedPile==1&&posX>240&&posX<290&&posY>130&&posY<205) {
				clickedCard=7;
				canClickDiscard=true;
				repaint();
				return;
			}
			for (int i=0;i<7;i++) {
				if(posX>(i*50)+50&&posX<(i*50)+90&&posY>290&&posY<350) {
					clickedCard=i;
					canClickDiscard=true;
					repaint();
					break;
				}
			}
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent parammouseEvent) {}

	@Override
	public void mouseReleased(MouseEvent paramMouseEvent) {}

	@Override
	public void mouseEntered(MouseEvent paramMouseEvent) {}

	@Override
	public void mouseExited(MouseEvent paramMouseEvent) {}
}
