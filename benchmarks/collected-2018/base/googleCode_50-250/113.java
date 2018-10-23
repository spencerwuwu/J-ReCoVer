// https://searchcode.com/api/result/291821/

package mw.client.dialogs.jframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import mw.client.gui.MWCardImpl;
import mw.client.managers.ConnectionManager;
import mw.client.managers.SettingsManager;
import mw.server.list.CardBeanList;
import mw.server.model.Damage;
import mw.server.model.bean.CardBean;

public class MultipleBlockers extends JFrame {

    private int assignDamage;
    private int assignedToPlayer = 0;

    private BorderLayout borderLayout1 = new BorderLayout();
    private JPanel mainPanel = new JPanel();
    private JScrollPane jScrollPane1 = new JScrollPane();
    private JLabel numberLabel = new JLabel();
    private JPanel jPanel3 = new JPanel();
    private BorderLayout borderLayout3 = new BorderLayout();
    private JPanel creaturePanel = new JPanel();
    private String buttonLabel = "Trample: deal X damage to player";
    private JButton assignToPlayer = new JButton(buttonLabel);
    
    private CardBean attacker;

    public MultipleBlockers(CardBeanList creatureList, CardBean attacker) {
        this();
        assignDamage = attacker.getAttack();
        this.attacker = attacker;
        updateDamageLabel();// update user message about assigning damage

        for (int i = 0; i < creatureList.size(); i++) {
            CardBean l = creatureList.get(i);
            MWCardImpl mtgCard = new MWCardImpl(l);
            mtgCard.setPreferredSize(new Dimension(SettingsManager.getManager().getCardSize().width,
                    SettingsManager.getManager().getCardSize().height));
            creaturePanel.add(mtgCard, BorderLayout.CENTER);
        }
        
        JDialog dialog = new JDialog(this, true);
        dialog.setTitle("Multiple Blockers");
        dialog.setContentPane(mainPanel);
        dialog.setSize(500, 300);
        Rectangle scrnRect = getGraphicsConfiguration().getBounds();
        dialog.setLocation((scrnRect.width - 500) / 2,
                (scrnRect.height - 255) / 2);
        dialog.setVisible(true);
    }

    public MultipleBlockers() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        this.getContentPane().setLayout(borderLayout1);
        this.setTitle("Multiple Blockers");
        mainPanel.setLayout(null);
        numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        numberLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        numberLabel.setText("Assign");
        numberLabel.setBounds(new Rectangle(52, 10, 343, 24));
        jPanel3.setLayout(borderLayout3);
        jPanel3.setBounds(new Rectangle(26, 45, 450, 160));
        jPanel3.setBorder(BorderFactory.createEmptyBorder());
        creaturePanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    creaturePanel_mousePressed(e);
                }
            }
        });
        creaturePanel
                .addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    public void mouseMoved(MouseEvent e) {
                        creaturePanel_mouseMoved(e);
                    }
                });
        mainPanel.add(jPanel3, null);
        jPanel3.add(jScrollPane1, BorderLayout.CENTER);
        mainPanel.add(numberLabel, null);
        jScrollPane1.getViewport().add(creaturePanel, null);
        jScrollPane1.setBorder(BorderFactory.createEmptyBorder());
        mainPanel.add(assignToPlayer, null);
        assignToPlayer.setVisible(false);
        assignToPlayer.setBounds(140, 210, 230, 25);
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        
        assignToPlayer.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent e) {
            	assignToPlayer_mousePressed(e);
            }
        });
    }

    void creaturePanel_mousePressed(MouseEvent e) {
        Object o = creaturePanel.getComponentAt(e.getPoint());
        if (o instanceof MWCardImpl) {
            CardBean card = ((MWCardImpl) o).getCard();
            Damage d = card.getAssignedDamage();
            d.addDamage(1);
            d.addSourceBean(attacker, 1);
        }
        
        /**
         * Reduce damage, show new user message, exit if necessary 
         */
        assignDamage--;
        updateDamageLabel();
        if (assignDamage == 0) {
            closeDialog();
        }

        if (attacker.getKeyword().contains("Trample")) {
        	boolean canUseTrample = true;
	        for (int i = 0; i < creaturePanel.getComponentCount(); i++) {
	            o = creaturePanel.getComponent(i);
	            if (o instanceof MWCardImpl) {
	                CardBean card = ((MWCardImpl) o).getCard();
	                if (card.getAssignedDamage().getDamage() < card.getDefense()) {
	                	canUseTrample = false;
	                }
	            }
	        }
	        
	        if (canUseTrample) {
	        	assignToPlayer.setVisible(true);
	        	String label = buttonLabel.replace("X", String.valueOf(assignDamage));
	        	assignToPlayer.setText(label);
	        }
        }
    }

    void assignToPlayer_mousePressed(MouseEvent e) {
    	assignedToPlayer = assignDamage;
    	assignDamage = 0;
    	closeDialog();
    }
    
    void updateDamageLabel() {
        numberLabel.setText("Assign " + assignDamage
                + " damage - double click on card to assign 1 damage");
    }

    void creaturePanel_mouseMoved(MouseEvent e) {
        Object o = creaturePanel.getComponentAt(e.getPoint());
        if (o instanceof MWCardImpl) {
            ((MWCardImpl) o).updateDescription();
        }
    }
    
    protected void closeDialog() {
        CardBeanList list = new CardBeanList();
        for (int i = 0; i < creaturePanel.getComponentCount(); i++) {
            Object o = creaturePanel.getComponent(i);
            if (o instanceof MWCardImpl) {
                CardBean card = ((MWCardImpl) o).getCard();
                if (card.getAssignedDamage().getDamage() > 0) {
                    list.add(card);
                }
            }
        }
        
        if (assignedToPlayer > 0) {
        	CardBean playerCard = new CardBean();
            playerCard.setPlayer(true);
            playerCard.setDamage(assignedToPlayer);
            playerCard.setTableID(attacker.getTableID());
            list.add(playerCard);
        }
        
        dispose();
        
        /**
         * Send the results to server
         */
        try {
            ConnectionManager.sendSetAssignedDamage(list);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
 
    }
    
    private static final long serialVersionUID = 1L;
}
