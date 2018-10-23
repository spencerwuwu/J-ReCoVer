// https://searchcode.com/api/result/119287240/

package org.ilzd.animation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.*;
import javax.swing.event.MouseInputAdapter;

/**
 * Simple drawing example.
 * @author jlepak
 */
public class SimpleDraw implements GameDraw, GameControl {

    // Implement mouse and keyboard handling
    Mouse mouse;
    Keys keys;
    
    // Reference to panel that does the animation
    AnimationPanel animator;
    
    // Track mouse location
    int mouseX = -1;
    int mouseY = -1;
    
    // MouseInputAdapter provides empty default handlers for all mouse events.
    // It implements the MouseMotionListener and MouseListener interfaces.
    private class Mouse extends MouseInputAdapter {
        @Override public void mouseMoved(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
        }
    }
    
    // KeyAdapter provides empty default handlers for all key events.
    // It implements the KeyListener interface.
    private class Keys extends KeyAdapter {
        
        // Use '1' and '2' to reduce or increase frame rate by about 10%
        @Override public void keyPressed(KeyEvent e) {
            double rate = animator.getFrameRate();
            if (e.getKeyCode() == KeyEvent.VK_1)
                animator.setFrameRate(rate / 1.1);
            else if (e.getKeyCode() == KeyEvent.VK_2)
                animator.setFrameRate(rate * 1.1);
        }
    }
    
    SimpleDraw() {
        mouse = new Mouse();
        keys = new Keys();
    }
    
    public void setAnimator(AnimationPanel a) {
        animator = a;
    }
    
    public void draw(Graphics2D g, AnimationPanel panel) {
        // Partially clear frame
        g.setColor(new Color(255, 255, 255, 10));
        g.fillRect(0, 0, panel.getWidth(), panel.getHeight());
        
        // Draw circle at mouse location
        g.setColor(Color.red);
        int diam = 50;
        int centerX = mouseX - diam/2;
        int centerY = mouseY - diam/2;
        g.fillOval(centerX, centerY, diam, diam);
    }

    public MouseListener getMouseListener() {
        return mouse;
    }
    
    public MouseMotionListener getMouseMotionListener() {
        return mouse;
    }

    public KeyListener getKeyListener() {
        return keys;
    }
    
}

