// https://searchcode.com/api/result/131270866/

/*
 * JanesImageResizerView.java
 */

package janesimageresizer;

import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * The application's main frame.
 */
public class JanesImageResizerView extends FrameView {

    public JanesImageResizerView(SingleFrameApplication app) {
        super(app);

        initComponents();
        

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);
        
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }
    /**
     * @param title
     * @param message 
     */
    
    public void errorMessage(String title,String message)
    {
        JOptionPane.showMessageDialog(this.getFrame(),message,title,JOptionPane.ERROR_MESSAGE); 
    }
    @Action
    public void runProgram()throws IOException
    {
        
        if(this.percentageReduce.getText().isEmpty())
        {
            errorMessage("Width error","Please enter a sutiable width");            
            return;
        }
        if(this.imagesSaveLocation.getText().isEmpty())
        {
            errorMessage("Location error","Please enter a location to save the images");
            return;
        }
        if(this.targetImgLocation.getText().isEmpty())
        {
            errorMessage("Location error","Please enter a target location for images to be resized");
            return;
        }
        try{
            Double.parseDouble(this.percentageReduce.getText());
        }
        catch(NumberFormatException nfo)
        {
            errorMessage("Invalid number entered","Please enter a number for the % reduce");
            return;
        }
        final File imageSaveDir = new File(this.imagesSaveLocation.getText());
        final double reductionPercentage = Double.parseDouble(this.percentageReduce.getText());
        
        final String imageFormat = this.imageFormats.getSelectedItem().toString();
        final ArrayList<File> targetImgs = this.targetImages;
        final JTextArea createdImages = this.newlyCreatedImages;
        final JTextArea convErrorsSpace = this.conversionErrors;
        
        if(!imageSaveDir.exists())
        {
            imageSaveDir.mkdir();
        }
        (new Thread()
        {
            @Override
            public void run(){
                for(final File f:targetImgs)
                {
                    final String newImageName = imageSaveDir.getAbsolutePath()+File.separator+f.getName();
                    
                    boolean isImageConverted = false;
                    try{
                    isImageConverted = JanesImageResizerApp.ResizeImages(f.getAbsolutePath(),
                                                      reductionPercentage,                                                      
                                                      newImageName,
                                                      imageFormat);
                    }catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                    
                    if(isImageConverted)                        
                    {
                        createdImages.append(f.getName());
                        createdImages.append("\n");
                        createdImages.setCaretPosition(createdImages.getText().length()-1);
                        /*SwingUtilities.invokeLater(new Runnable(){
                           public void run()
                           {
                                createdImages.append(f.getName());
                                createdImages.append("\n");
                           }
                        });*/
                    }
                    else
                    {
                        convErrorsSpace.append(f.getName()+":unable to read img");
                        convErrorsSpace.append("\n");  
                        convErrorsSpace.setCaretPosition(convErrorsSpace.getText().length()-1);
                    }

                } 
            }
        }
        ).start();
    }
    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = JanesImageResizerApp.getApplication().getMainFrame();
            aboutBox = new JanesImageResizerAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        JanesImageResizerApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        targetImageLBL = new javax.swing.JLabel();
        saveLocationLBL = new javax.swing.JLabel();
        targetImgLocation = new javax.swing.JTextField();
        imagesSaveLocation = new javax.swing.JTextField();
        imageLocationBrowse = new javax.swing.JButton();
        targetImageBrowse = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        newlyCreatedImages = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        percentageReduce = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        imageFormats = new javax.swing.JComboBox();
        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        imageList = new javax.swing.JList();
        jScrollPane3 = new javax.swing.JScrollPane();
        conversionErrors = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        targetImageLBL.setLabelFor(targetImgLocation);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(janesimageresizer.JanesImageResizerApp.class).getContext().getResourceMap(JanesImageResizerView.class);
        targetImageLBL.setText(resourceMap.getString("targetImageLBL.text")); // NOI18N
        targetImageLBL.setName("targetImageLBL"); // NOI18N

        saveLocationLBL.setLabelFor(imagesSaveLocation);
        saveLocationLBL.setText(resourceMap.getString("saveLocationLBL.text")); // NOI18N
        saveLocationLBL.setName("saveLocationLBL"); // NOI18N

        targetImgLocation.setEditable(false);
        targetImgLocation.setText(resourceMap.getString("targetImgLocation.text")); // NOI18N
        targetImgLocation.setName("targetImgLocation"); // NOI18N

        imagesSaveLocation.setText(resourceMap.getString("imagesSaveLocation.text")); // NOI18N
        imagesSaveLocation.setName("imagesSaveLocation"); // NOI18N

        imageLocationBrowse.setText(resourceMap.getString("imageLocationBrowse.text")); // NOI18N
        imageLocationBrowse.setName("imageLocationBrowse"); // NOI18N
        imageLocationBrowse.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                imageLocationBrowseMouseClicked(evt);
            }
        });

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(janesimageresizer.JanesImageResizerApp.class).getContext().getActionMap(JanesImageResizerView.class, this);
        targetImageBrowse.setAction(actionMap.get("selectDirForImagesToResize")); // NOI18N
        targetImageBrowse.setText(resourceMap.getString("targetImageBrowse.text")); // NOI18N
        targetImageBrowse.setName("targetImageBrowse"); // NOI18N
        targetImageBrowse.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                targetImageBrowseMouseClicked(evt);
            }
        });

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        newlyCreatedImages.setColumns(20);
        newlyCreatedImages.setRows(5);
        newlyCreatedImages.setName("newlyCreatedImages"); // NOI18N
        jScrollPane2.setViewportView(newlyCreatedImages);

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel3.setLabelFor(percentageReduce);
        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        percentageReduce.setText(resourceMap.getString("percentageReduce.text")); // NOI18N
        percentageReduce.setName("percentageReduce"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        imageFormats.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "JPG", "PNG", "GIF" }));
        imageFormats.setName("imageFormats"); // NOI18N

        jButton1.setAction(actionMap.get("runProgram")); // NOI18N
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        imageList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        imageList.setName("imageList"); // NOI18N
        jScrollPane1.setViewportView(imageList);

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        conversionErrors.setColumns(20);
        conversionErrors.setForeground(resourceMap.getColor("conversionErrors.foreground")); // NOI18N
        conversionErrors.setRows(5);
        conversionErrors.setName("conversionErrors"); // NOI18N
        jScrollPane3.setViewportView(conversionErrors);

        jLabel6.setLabelFor(conversionErrors);
        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(targetImageLBL, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(targetImgLocation, javax.swing.GroupLayout.PREFERRED_SIZE, 311, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(targetImageBrowse, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(saveLocationLBL, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButton1)
                                    .addComponent(jLabel3))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(percentageReduce)))
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGap(4, 4, 4)
                                .addComponent(imagesSaveLocation, javax.swing.GroupLayout.PREFERRED_SIZE, 291, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(6, 6, 6)
                                .addComponent(imageLocationBrowse, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(imageFormats, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 206, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addGap(6, 6, 6)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 218, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(targetImageLBL))
                    .addComponent(targetImgLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(targetImageBrowse))
                .addGap(6, 6, 6)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(saveLocationLBL))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(imagesSaveLocation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(imageLocationBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(percentageReduce, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addGap(11, 11, 11)
                        .addComponent(jButton1))
                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(imageFormats, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel5)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel6)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE))
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 706, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 536, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    @Action
    public void selectDirForImagesToResize()
    {
        int returnValue = chooser.showOpenDialog(this.getFrame());
        
        if(returnValue == JFileChooser.APPROVE_OPTION)
        {
            File file = chooser.getSelectedFile();            
            this.targetImages.clear();
            
            targetImgLocation.setText(file.getAbsolutePath());
            this.imagesSaveLocation.setText("");      
            
            if (!file.isDirectory())
            {
                this.targetImages.add(file);
                this.imagesSaveLocation.setText(file.getParentFile().getAbsolutePath()+File.separator+"small");
            }
            else{
                try{            
                    this.targetImages = JanesImageResizerApp.getFilesInDir(file.getAbsolutePath(), this.targetImages);
                }
                catch(IOException e)
                {
                    StringBuilder build = new StringBuilder();
                    build.append("Error retrieving files from the directory");
                    build.append(e.getMessage());
                    JOptionPane.showMessageDialog(this.getFrame(),build.toString(),"FileNotFound",JOptionPane.ERROR_MESSAGE); 
                }                    
                this.imagesSaveLocation.setText(file.getAbsolutePath()+File.separator+"small");
            }
            
            
            this.imageList.removeAll();
            DefaultListModel model = new DefaultListModel();

            for(File f:this.targetImages)
            {
                model.addElement(f.getName());
            }        
            this.imageList.setModel(model);
        }
    }
    @Action
    public void selectTargetToSaveResizedDirs()
    {
        int returnValue = chooser.showOpenDialog(this.getFrame());    
        if(returnValue == JFileChooser.APPROVE_OPTION)
        {
            this.imagesSaveLocation.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
private void targetImageBrowseMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_targetImageBrowseMouseClicked
    
    
}//GEN-LAST:event_targetImageBrowseMouseClicked

private void imageLocationBrowseMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_imageLocationBrowseMouseClicked
    int returnValue = chooser.showOpenDialog(this.getFrame());
    
    if(returnValue == JFileChooser.APPROVE_OPTION)
    {
        File file = chooser.getSelectedFile();        
        imagesSaveLocation.setText(file.getAbsolutePath());        
    }
}//GEN-LAST:event_imageLocationBrowseMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea conversionErrors;
    private javax.swing.JComboBox imageFormats;
    private javax.swing.JList imageList;
    private javax.swing.JButton imageLocationBrowse;
    private javax.swing.JTextField imagesSaveLocation;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JTextArea newlyCreatedImages;
    private javax.swing.JTextField percentageReduce;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel saveLocationLBL;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JButton targetImageBrowse;
    private javax.swing.JLabel targetImageLBL;
    private javax.swing.JTextField targetImgLocation;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    final JFileChooser chooser = new JFileChooser();  
    private ArrayList<File> targetImages = new ArrayList<File>();
    private int busyIconIndex = 0;

    private JDialog aboutBox;
}

