// https://searchcode.com/api/result/291823/

package mw.client.dialogs.unused;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import mw.client.constants.Constants;
import mw.client.gui.MWCardImpl;
import mw.client.managers.DialogManager;
import mw.client.managers.SettingsManager;
import mw.client.utils.dialogs.DialogContainer;
import mw.client.utils.dialogs.DlgParams;
import mw.client.utils.dialogs.IDialogPanel;
import mw.server.list.CardList;
import mw.server.model.bean.CardBean;

public class AssignDamageDialog extends IDialogPanel implements MouseMotionListener {

    private static final long serialVersionUID = 1L;
    private JLabel jTitle = null;
    private JPanel mainContainer = null;
    
    private JScrollPane jScrollPane1 = new JScrollPane();
    private BorderLayout borderLayout3 = new BorderLayout();
    private JPanel creaturePanel = new JPanel();
    
    // TODO:
    CardList creatureList;
    int assignDamage;
    
    /**
     * This is the default constructor
     */
    public AssignDamageDialog(DlgParams params) {
        super(params);
        
        // TODO:
        /*
        CardFactory cardFactory = new CardFactory(Constant.IO.cardFile);
        creatureList = new CardList();
        creatureList.add(cardFactory.getCard("Elvish Piper"));
        creatureList.add(cardFactory.getCard("Lantern Kami"));
        creatureList.add(cardFactory.getCard("Frostling"));
        creatureList.add(cardFactory.getCard("Frostling"));
        creatureList.add(cardFactory.getCard("Frostling"));
        assignDamage = 3;
        */
        
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        
        jTitle = new JLabel();
        jTitle.setBounds(new Rectangle(5, 3, 450, 16));
        jTitle.setFont(new Font("Dialog", Font.BOLD, 14));
        updateDamageLabel();
        
        this.setLayout(null);
        
        this.add(jTitle, null);
        this.add(getMainContainer(), null);
        
        mainContainer.setLayout(borderLayout3);
        mainContainer.setBounds(new Rectangle(26, 45, 450, 160));
        mainContainer.add(jScrollPane1, BorderLayout.CENTER);
        jScrollPane1.getViewport().add(creaturePanel, null);
        
        displayBlockers();
        makeTransparent();
        
        /**
         * Mouse handlers
         */
        creaturePanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                creaturePanel_mousePressed(e);
            }
        });
        
        /**
         * Mouse handlers
         */
        creaturePanel
                .addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    public void mouseMoved(MouseEvent e) {
                        creaturePanel_mouseMoved(e);
                    }
                });
    }
    
    private void displayBlockers() {
        
        for (int i = 0; i < creatureList.size(); i++) {
            CardBean l = new CardBean(creatureList.get(i));
            MWCardImpl mtgCard = new MWCardImpl(l);
            mtgCard.setVisible(true);
            //mtgCard.setBorder(BorderFactory.createLineBorder(Color.green));
            mtgCard.setPreferredSize(new Dimension(SettingsManager.getManager().getCardSize().width,
            		SettingsManager.getManager().getCardSize().height));
            creaturePanel.add(mtgCard, BorderLayout.CENTER);
        }
        
    }

    /**
     * This method initializes jRadioButtonMe	
     * 	
     * @return javax.swing.JRadioButton	
     */
    private JPanel getMainContainer() {
        if (mainContainer == null) {
            mainContainer = new JPanel(true);
            mainContainer.setOpaque(false);
            mainContainer.setBounds(new Rectangle(0, 0, getDlgParams().rect.width - 90, getDlgParams().rect.height - 90));
        }
        return mainContainer;
    }
    
    public void mouseMoved(MouseEvent e) {
        JComponent j = (JComponent) getComponentAt(e.getX(), e.getY());
        /*
        if (j != null && j instanceof MTGCard) {
            MTGCard card = (MTGCard) j;
            card.updateDescription();
        }
        */
    }

    public void mouseDragged(MouseEvent e) {
    }
    
    protected void updateDamageLabel() {
        jTitle.setText("Assign " + assignDamage
                + " damage - click on card to assign damage");
    }
    
    void creaturePanel_mousePressed(MouseEvent e) {
        Object o = creaturePanel.getComponentAt(e.getPoint());
        if (o instanceof MWCardImpl) {
            //MWCard card = (MWCard) o;
            //Card c = card.getCard();
            
            //TODO: commented because of damage is not int now but is Damage class
            //c.setAssignedDamage(c.getAssignedDamage() + 1);

            // if(guiDisplay != null)
            // guiDisplay.updateCardDetail(c);
        }
        
        /**
         * Reduce damage, show new user message, exit if necessary
         */
        assignDamage--;
        updateDamageLabel();
        if (assignDamage == 0) {
            DialogManager.getManager().fadeOut((DialogContainer)getParent());
        }
    }

    void creaturePanel_mouseMoved(MouseEvent e) {
        /*
        JComponent j = (JComponent) getComponentAt(e.getX(), e.getY());
        if (j != null && j instanceof MTGCard) {
            MTGCard card = (MTGCard) j;
            card.updateDescription();
        }
        */
    }
   
}  

