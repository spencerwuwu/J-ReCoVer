// https://searchcode.com/api/result/58025501/

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class Cannonball01{
  public static void main(String[] args){
    new Display();
  }//end main
}//end controlling class Cannonball01
//======================================================//

class Display extends JFrame 
                 implements ChangeListener,ActionListener{
  //Save a reference to this object for access by the GUI.
  Display displayObj;

  //Specify the horizontal and vertical size of a JFrame
  // object.
  int hSize = 400;
  int vSize = 400;
  Image osi;//an off-screen image
  int osiWidth;//off-screen image width
  int osiHeight;//off-screen image height
  MyCanvas myCanvas;//a subclass of Canvas

  double pi = Math.PI;//a convenience variable

  //The following are 3D perspective drawing parameters.
  GM03.Point3D camera = new GM03.Point3D(0,0,0);
  GM03.ColMatrix3D angle = new GM03.ColMatrix3D(0,0,0);
  double scale = 300;

  //Specifies the type of projection. This value is
  // required by the draw methods in the GM03 game-math
  // library.
  final int type = 3;

  //Variables used to save the radius and location of the
  // safety net. The radius cannot be modified by the
  // player but the location can be modified by the
  // player.
  double radius = 10;
  GM03.Point3D centerPoint = new GM03.Point3D(0,0,100);

  Thread animator;//A reference to an animation thread

  Graphics2D g2D;//A reference to the off-screen image.

  //Determines whether to draw angular guidelines when
  // positioning the net.
  boolean guideLines;
  //Determines whether to draw a verticalSightLine used
  // for adjusting the azimuth of the cannon.
  boolean verticalSightLine;
  //Determines whether to draw a horizontalSightLine used
  // for adjusting the elevation of the cannon.
  boolean horizontalSightLine;

  //References to the current position of the camera.
  double cameraX;
  double cameraY;
  double cameraZ;

  //User input and data output components.
  JSlider positionXslider;
  JSlider positionZslider;
  JTextField positionXoutput;
  JTextField positionZoutput;

  JSlider azimuthSlider;
  JSlider elevationSlider;
  JTextField azimuthOutput;
  JTextField elevationOutput;

  JSlider velocitySlider;
  JTextField velocityOutput;

  JButton fireButton;

  //Values obtained from user input.
  double velocity;
  double azimuthAngle;
  double elevationAngle;
  //----------------------------------------------------//

  Display(){//constructor
    //Set JFrame size, title, and close operation.
    setSize(hSize,vSize);
    setTitle("Copyright 2008,R.G.Baldwin");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    //Create a new drawing canvas and add it to the
    // center of the JFrame.
    myCanvas = new MyCanvas();
    this.getContentPane().add(myCanvas);

    //This object must be visible before you can get an
    // off-screen image.  It must also be visible before
    // you can compute the size of the canvas.
    setVisible(true);
    osiWidth = myCanvas.getWidth();
    osiHeight = myCanvas.getHeight();

    //Create an off-screen image and get a graphics
    // context on it.
    osi = createImage(osiWidth,osiHeight);
    g2D = (Graphics2D)(osi.getGraphics());

    //Translate the origin to the center of the
    // off-screen image.
    GM03.translate(g2D,0.5*osiWidth,-0.5*osiHeight);

    //Make a reference to this object available to the
    // constructor for the GUI class.
    displayObj = this;

    //Instantiate the JFrame that contains the user input
    // components. Those user input components were
    // placed in a separate JFrame object simply because
    // there isn't sufficient space available to contain
    // them in the main display.
    new GUI();

    //Register this object as a change listener on the
    // sliders. This can't be done until the GUI object
    // has been instantiated.
    positionXslider.addChangeListener(this);
    positionZslider.addChangeListener(this);
    azimuthSlider.addChangeListener(this);
    elevationSlider.addChangeListener(this);
    velocitySlider.addChangeListener(this);

    //Register this object as an action listener on the
    // Fire button.
    fireButton.addActionListener(this);

    //Elevate the camera above the origin and tilt it
    // downward for initial adjustments.
    //Note that the camera is rotated by 180 degrees
    // around the y-axis to cause it to be shooting
    // along the positive z-axis.
    camera.setData(1,100);
    angle = new GM03.ColMatrix3D(45,180,0);

    //Project the scene onto the 2D off-screen image and 
    // display it on the screen.
    drawTheImage(g2D);
    myCanvas.repaint();

  }//end constructor
  //----------------------------------------------------//

  //The purpose of this method is to create the 3D scene
  // and project it onto the 2D off-screen image.
  void drawTheImage(Graphics2D g2D){
    //Erase the screen
    g2D.setColor(Color.WHITE);
    GM03.fillRect(g2D,
                 -osiWidth/2,
                  osiHeight/2,
                  osiWidth,
                  osiHeight);

    //Draw three vectors that simulate guy wires and
    // anchors holding up the net.
    g2D.setColor(Color.GRAY);    
    new GM03.Vector3D(1.5*radius,0.0,1.5*radius).
                               draw(g2D,new GM03.Point3D(
                                 centerPoint.getData(0),
                                 0.0,
                                 centerPoint.getData(2)),
                                 type,scale,camera,angle);

    new GM03.Vector3D(-1.5*radius,0.0,1.5*radius).
                               draw(g2D,new GM03.Point3D(
                                 centerPoint.getData(0),
                                 0.0,
                                 centerPoint.getData(2)),
                                 type,scale,camera,angle);

    new GM03.Vector3D(0.0,0.0,-1.5*1.414*radius).
                               draw(g2D,new GM03.Point3D(
                                 centerPoint.getData(0),
                                 0.0,
                                 centerPoint.getData(2)),
                                 type,scale,camera,angle);


    //Define points that represent a circle on the x-z
    // plane. Store the points in an array.
    int numberPoints = 12;
    GM03.Point3D[] points = 
                           new GM03.Point3D[numberPoints];

    for(int cnt = 0;cnt < points.length;cnt++){
      points[cnt] = new GM03.Point3D(
                 centerPoint.getData(0) + radius*Math.cos(
                           (cnt*360/numberPoints)*pi/180),
                 centerPoint.getData(1),
                 centerPoint.getData(2) + radius*Math.sin(
                          (cnt*360/numberPoints)*pi/180));
    }//end for loop


    //Draw lines that connect every point to every other
    // point to create the mesh in the net.
    g2D.setColor(Color.GRAY);
    for(int row = 0;row < points.length;row++){
      for(int col = row;col < points.length;col++){
        new GM03.Line3D(points[row],points[col]).
                        draw(g2D,type,scale,camera,angle);
      }//end inner loop
    }//end outer loop

    //Test flags and draw certain lines on the display
    // according to the values of the flags.
    if(guideLines){
      //Draw three lines that radiate out from the origin
      // with an angle of 45 degrees between them.
      g2D.setColor(Color.BLUE);
      new GM03.Line3D(new GM03.Point3D(0.0,0.0,0.0),
                      new GM03.Point3D(0.0,0.0,400.0)).
                      draw(g2D,type,scale,camera,angle);
      new GM03.Line3D(new GM03.Point3D(0.0,0.0,0.0),
                      new GM03.Point3D(400.0,0.0,400.0)).
                      draw(g2D,type,scale,camera,angle);
      new GM03.Line3D(new GM03.Point3D(0.0,0.0,0.0),
                      new GM03.Point3D(-400.0,0.0,400.0)).
                      draw(g2D,type,scale,camera,angle);
    }//end if

    if(verticalSightLine){
      //Draw a vertical sighting line at the center of the
      // display that is used to adjust the azimuth of the
      // cannon.
      g2D.setColor(Color.RED);
      GM03.drawLine(g2D,0.0,-osiHeight/2,0.0,osiHeight/2);
    }//end if

    if(horizontalSightLine){
      //Draw a horizontal sighting line at the center of
      // the display that is used to adjust the elevation
      // of the cannon.
      g2D.setColor(Color.RED);
      GM03.drawLine(g2D,-osiWidth/2,0.0,osiWidth/2,0.0);
    }//end if

  }//end drawTheImage
  //====================================================//


  //This method is called to respond to change events on
  // any of the sliders.
  public void stateChanged(ChangeEvent e){

    //Elevate the camera above the origin and tilt it
    // downward
    //Note that the camera is rotated by 180 degrees
    // around the y-axis to cause it to be shooting along
    // the positive z-axis.
    camera.setData(0,0);
    camera.setData(1,100);
    camera.setData(2,0);

    angle.setData(0,45);
    angle.setData(1,180);
    angle.setData(2,0);

    //Get the ID of the slider that was the source of the
    // event.
    JSlider source = (JSlider)e.getSource();

    //Take the appropriate action on the basis of the ID
    // of the source of the event.
    if(source.getName().equals("positionXslider")){
      //Set the x-coordinate of the net and display the
      // coordinate value.
      centerPoint.setData(0,source.getValue());
      positionXoutput.setText("" + source.getValue());
      guideLines = true;//Draw angular guidelines
      verticalSightLine = false;//No verticalSightLine.
      horizontalSightLine = false;//No horizontalSightLine

    }else if(source.getName().equals("positionZslider")){
      //Set the z-coordinate of the net and display the
      // coordinate value.
      centerPoint.setData(2,source.getValue());
      positionZoutput.setText("" + source.getValue());
      guideLines = true;//Draw angular guidelines
      verticalSightLine = false;//No verticalSightLine.
      horizontalSightLine = false;//No horizontalSightLine

    }else if(source.getName().equals("azimuthSlider")){
      //Rotate the cannon around the y-axis.
      azimuthAngle = source.getValue();
      //Display direction that the cannon is pointing.
      azimuthOutput.setText("" + source.getValue());
      //Set the camera to ground level with no vertical
      // tilt.
      camera.setData(1,0.0);
      angle.setData(0,0.0);
      //Rotate the camera to point in the same direction
      // as the cannon.
      angle.setData(1,180 + source.getValue());
      guideLines = false;//Do not draw angular guidelines
      verticalSightLine = true;//Draw verticalSightLine.
      horizontalSightLine = false;//No horizontalSightLine

    }else if(source.getName().equals("elevationSlider")){
      //Set and display the elevation angle for the
      // cannon.
      elevationAngle = source.getValue();
      elevationOutput.setText("" + source.getValue());
      //Set the camera to ground level with a vertical
      // tilt equal to the elevation angle. Scale the
      // angle by 1/4 to keep the net within the bounds of
      // the display.
      camera.setData(1,0.0);
      angle.setData(0,-source.getValue()/4);
      guideLines = false;//Draw angular guidelines
      verticalSightLine = false;//No verticalSightLine.
      //Draw horizontalSightLine
      horizontalSightLine = true;

    }else if(source.getName().equals("velocitySlider")){
      //Set and display the velocity value.
      velocity = source.getValue();
      velocityOutput.setText("" + source.getValue());
      guideLines = false;//Do not draw angular guidelines
      verticalSightLine = false;//No verticalSightLine.
      horizontalSightLine = false;//No horizontalSightLine
    }//end if

    //Project the scene onto the 2D off-screen image and 
    // display it on the screen.
    drawTheImage(g2D);
    myCanvas.repaint();
  }//end stateChanged
  //----------------------------------------------------//

  //This method is called whenever the user clicks the
  // fireButton
  public void actionPerformed(ActionEvent e){
    //Disable the sliders while the cannonball is in
    // flight.
    positionXslider.setEnabled(false);
    positionZslider.setEnabled(false);
    azimuthSlider.setEnabled(false);
    elevationSlider.setEnabled(false);
    velocitySlider.setEnabled(false);

    //Set all of the parameters based on the current
    // values of the associated sliders.
    centerPoint.setData(0,positionXslider.getValue());
    centerPoint.setData(2,positionZslider.getValue());
    azimuthAngle = azimuthSlider.getValue();
    elevationAngle = elevationSlider.getValue();
    velocity = velocitySlider.getValue();
    guideLines = false;//Do not draw angular guidelines
    verticalSightLine = false;//No verticalSightLine
    horizontalSightLine = false;//No horizontalSightLine.

    //Instantiate the animation thread and start it
    // running.
    animator = new Animator();
    animator.start();
  }//end actionPerformed
  //====================================================//

  //This is an inner class of the Display class.
  class MyCanvas extends Canvas{
    //Override the update method to eliminate the default
    // clearing of the Canvas in order to reduce or
    // eliminate the flashing that that is often caused by
    // such default clearing.
    //In this case, it isn't necessary to clear the canvas
    // because the off-screen image is cleared each time
    // it is updated. This method will be called when the
    // JFrame and the Canvas appear on the screen or when
    // the repaint method is called on the Canvas object.
    public void update(Graphics g){
      paint(g);//Call the overridden paint method.
    }//end overridden update()

    //Override the paint() method. The purpose of the
    // paint method is to display the off-screen image on
    // the screen. This method is called by the update
    // method above.
    public void paint(Graphics g){
      g.drawImage(osi,0,0,this);
    }//end overridden paint()

  }//end inner class MyCanvas
  //====================================================//

  //This is the animation thread class for the program. It
  // is an inner class.
  class Animator extends Thread{
    //Gradational acceleration constant.
    final double gravity = 32.174;//ft/sec/sec

    //The following velocity components are computed from
    // the user-specified velocity, azimuth, and 
    // elevation values.
    double initialVelocityX;
    double initialVelocityY;
    double initialVelocityZ;

    //Store the camera angle and position here.
    GM03.ColMatrix3D cameraAngle;
    GM03.Point3D cameraPosition;

    //Unit vectors along the x, y, and z axes.
    GM03.Vector3D uVectorX = new GM03.Vector3D(1,0,0);
    GM03.Vector3D uVectorY = new GM03.Vector3D(0,1,0);
    GM03.Vector3D uVectorZ = new GM03.Vector3D(0,0,1);

    Animator(){//constructor
      //Compute the x,y, and z-components of the velocity.
      initialVelocityZ = 
        velocity * Math.cos(Math.toRadians(azimuthAngle));
      initialVelocityX = 
        velocity * Math.sin(Math.toRadians(azimuthAngle));
      initialVelocityY = velocity * Math.cos(
                          Math.toRadians(elevationAngle));

      //Initialize the camera position and angle.
      cameraPosition = new GM03.Point3D(0,0,0);
      //Note that the camera is rotated by 180 degrees
      // around the y-axis to cause it to be shooting
      // along the positive z-axis.
      cameraAngle = new GM03.ColMatrix3D(0,180,0);

      //Copy the camera position and angle to the objects
      // used by the draw methods.
      camera = cameraPosition.clone();
      angle = cameraAngle.clone();

      //Project the scene onto the 2D off-screen image and
      // display it on the screen.
      drawTheImage(g2D);
      myCanvas.repaint();

    }//end constructor
    //--------------------------------------------------//

    //This method is called when the start method is 
    // called on the animation thread.
    public void run(){
      double time = 0.0;
      //This is one of the factors that controls the
      // animation speed.
      double deltaTime = 0.01;
      //Position the camera at the origin.
      cameraX = 0.0;
      cameraY = 0.0;
      cameraZ = 0.0;

      //Compute, draw, and sleep
      while(cameraY >= 0.0){
        //Loop until the camera completes the trajectory
        // and goes slightly negative in terms of the 
        // y-coordinate.
        //Compute and save the new position of the camera
        // based on the equations of motion.
        cameraX = initialVelocityX * time;
        cameraZ = initialVelocityZ * time;
        cameraY = 
          initialVelocityY*time - (gravity*time*time)/2.0;

        cameraPosition.setData(0,cameraX);
        cameraPosition.setData(1,cameraY);
        cameraPosition.setData(2,cameraZ);

        //Copy the camera information saved locally to the
        // instance variable that is used by the various 
        // draw methods.
        camera = cameraPosition.clone();

        //Increment time.
        time += deltaTime;

        //Compute the angle from the camera to the center
        // of the net.
        GM03.Vector3D displacement = cameraPosition.
                       getDisplacementVector(centerPoint);
        double alpha = Math.acos(displacement.normalize().
                                           dot(uVectorX));
        double theta = Math.acos(displacement.normalize().
                                           dot(uVectorZ));

        //Convert the angles to degrees and save them.
        cameraAngle.setData(0,Math.toDegrees(theta));
        cameraAngle.setData(
                    1,180 + (90 - Math.toDegrees(alpha)));
        angle = cameraAngle.clone();

        //Project the scene onto the 2D off-screen image
        // and display it on the screen.
        drawTheImage(g2D);
        myCanvas.repaint();

        //Put the thread to sleep temporarily. This is one
        // of the factors that controls the animation
        // speed.
        try{
          Thread.currentThread().sleep(37);
        }catch(InterruptedException e){
          e.printStackTrace();
        }//end catch
      }//end animation loop

      //The animation loop has terminated meaning that the
      // y-coordinate for the camera has gone slightly
      // negative, signaling that it has reached the end
      // of its trajectory.

      //Pause for a short period to allow the player to
      // view the impact in perspective before switching
      // to the birds-eye view.
      try{
        Thread.currentThread().sleep(2000);
      }catch(InterruptedException e){
        e.printStackTrace();
      }//end catch

      //Enable the sliders when the cannonball lands to
      // allow the player to make adjustments and fire
      // the cannon again.
      positionXslider.setEnabled(true);
      positionZslider.setEnabled(true);
      azimuthSlider.setEnabled(true);
      elevationSlider.setEnabled(true);
      velocitySlider.setEnabled(true);

      //Pull up for a birds-eye view directly above the
      // safety net.
      camera.setData(0,centerPoint.getData(0));
      camera.setData(1,175);
      camera.setData(2,centerPoint.getData(2));

      angle.setData(0,90);
      angle.setData(1,180);
      angle.setData(2,0);

      guideLines = true;//Draw angular guideLines.
      verticalSightLine = false;//No verticalSightLine.
      horizontalSightLine = false;//No horizontalSightLine

      //Project the scene onto the 2D off-screen image.
      drawTheImage(g2D);


      //Now determine if the human cannonball landed in
      // the safety net.
      if(cameraPosition.
            getDisplacementVector(centerPoint).getLength()
                                                < radius){
        //The human cannonball hit the net.
        //Draw a green circle at the point of impact.
        g2D.setColor(Color.GREEN);
        cameraPosition.draw(g2D,type,scale,camera,angle);

        //Beep the computer to indicate success.
        Toolkit.getDefaultToolkit().beep();
      }else{
        //The human cannonball missed the net.
        //Draw a red circle at the point of impact and
        // do not beep the computer.
        g2D.setColor(Color.RED);
        cameraPosition.draw(g2D,type,scale,camera,angle);
      }//end else

      //Draw a blue circle at the origin.
      g2D.setColor(Color.BLUE);
      new GM03.Point3D(0,0,0).draw(
                             g2D,type,scale,camera,angle);

      //Cause the overridden paint method belonging to
      // myCanvas to be executed.
      myCanvas.repaint();
    }//end run method
  }//end inner class Animator
  //====================================================//

  //This is an inner class of the Display class. An object
  // of this class provides a home for the user input
  // components.
  //An object of this class is needed only because there
  // isn't sufficient space on the object of the Display
  // class to contain all of the user input components.
  // This object is displayed to the right of the object
  // of the Display class on the screen.
  class GUI extends JFrame{
    GUI(){
      //Instantiate a JPanel that will house the user
      // input components and set its layout manager.
      JPanel controlPanel = new JPanel();
      controlPanel.setLayout(new GridLayout(0,2));

      //Add the user input component and appropriate
      // labels to the control panel.
      controlPanel.add(
              new JLabel("Net Position X",JLabel.CENTER));
      controlPanel.add(
              new JLabel("Net Position Z",JLabel.CENTER));

      positionXslider = new JSlider(-150,150,0);
      positionXslider.setName("positionXslider");
      positionXslider.setMinorTickSpacing(10);
      positionXslider.setMajorTickSpacing(75);
      positionXslider.setPaintTicks(true);
      positionXslider.setPaintLabels(true);

      positionZslider = new JSlider(0,300,100);
      positionZslider.setName("positionZslider");
      positionZslider.setMinorTickSpacing(10);
      positionZslider.setMajorTickSpacing(75);
      positionZslider.setPaintTicks(true);
      positionZslider.setPaintLabels(true);

      controlPanel.add(positionXslider);
      controlPanel.add(positionZslider);

      positionXoutput = 
          new JTextField("" + positionXslider.getValue());
      positionXoutput.setEditable(false);
      positionZoutput = 
          new JTextField("" + positionZslider.getValue());
      positionZoutput.setEditable(false);
      controlPanel.add(positionXoutput);
      controlPanel.add(positionZoutput);

      controlPanel.add(
              new JLabel("Cannon Azimuth",JLabel.CENTER));
      controlPanel.add(
            new JLabel("Cannon Elevation",JLabel.CENTER));

      azimuthSlider = new JSlider(-150,150,0);
      azimuthSlider.setName("azimuthSlider");
      azimuthSlider.setMinorTickSpacing(10);
      azimuthSlider.setMajorTickSpacing(75);
      azimuthSlider.setPaintTicks(true);
      azimuthSlider.setPaintLabels(true);
      controlPanel.add(azimuthSlider);

      elevationSlider = new JSlider(0,150,50);
      elevationSlider.setName("elevationSlider");
      elevationSlider.setMinorTickSpacing(10);
      elevationSlider.setMajorTickSpacing(75);
      elevationSlider.setPaintTicks(true);
      elevationSlider.setPaintLabels(true);
      controlPanel.add(elevationSlider);

      azimuthOutput = 
            new JTextField("" + azimuthSlider.getValue());
      azimuthOutput.setEditable(false);
      elevationOutput = 
          new JTextField("" + elevationSlider.getValue());
      elevationOutput.setEditable(false);
      controlPanel.add(azimuthOutput);
      controlPanel.add(elevationOutput);

      controlPanel.add(
             new JLabel("Muzzle Velocity",JLabel.CENTER));
      //Empty field
      controlPanel.add(new JLabel("",JLabel.CENTER));

      velocitySlider = new JSlider(0,150,50);
      velocitySlider.setName("velocitySlider");
      velocitySlider.setMinorTickSpacing(10);
      velocitySlider.setMajorTickSpacing(75);
      velocitySlider.setPaintTicks(true);
      velocitySlider.setPaintLabels(true);
      controlPanel.add(velocitySlider);

      controlPanel.add(
                 new JLabel("Fire Button",JLabel.CENTER));

      velocityOutput = 
           new JTextField("" + velocitySlider.getValue());
      velocityOutput.setEditable(false);
      controlPanel.add(velocityOutput);

      fireButton = new JButton("Fire");
      controlPanel.add(fireButton);

      //Add the control panel to the JFrame.
      this.getContentPane().add(
                        BorderLayout.CENTER,controlPanel);

      //Set the position and size of the object on the
      // screen.
     setBounds(displayObj.getWidth(),0,300,400);

      setTitle("Copyright 2008,R.G.Baldwin");
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      setVisible(true);
    }//end constructor
  }//end class GUI
  //====================================================//
}//end class Display
//======================================================//
