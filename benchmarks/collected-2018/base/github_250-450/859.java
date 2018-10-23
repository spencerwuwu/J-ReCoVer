// https://searchcode.com/api/result/95747214/

/**
 * Physics.java
 * 
 * 	This file is part of FootyBees.
 *
 *  FootyBees is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FootyBees is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FootyBees.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Copyright 2014 Devin Rusnak
 *  @author Devin Rusank
 *
 */
package footybees;

public class Physics {

	// class instance 
	public static Physics instance;
	
	// class constants
	private static final double AIR_DENSITY = 1.25;				// kg/m^3
	private static final double DRAG_COEFFICIENT = 0.0026;		// text-book
	private static final double ROLLING_COEFFICIENT = 0.31;		// online soccer problem
	private static final int PLAYER_MASS = 68;					// kilograms
	private static final double GRAVITY = 9.8;					// m/s^2
	private static final double TIME = 0.1;						// frequency of update getting called
	
	// constructor
	private Physics() {	}
	
	/**
	 * Class instance access method.
	 * @return instance of class Physics
	 */
	public static Physics getPhysics() {
		if(instance == null)
			instance = new Physics();
		return instance;
	}
	
	/**
	 * Kicks the ball at the specified angle to the coordinate plane.
	 * The force is given as the force along the path of flight so the 
	 * component vectors need to be derived with some trig.
	 * 
	 * @param force - imparted newtons along the vector sum
	 * @param angleXY - angle counter-clockwise from the positive x-axis towards the positive y-axis, pitch?
	 * @param angleXZ - angle counter-clockwise from the positive x-axis towards the positive Z-axis, yaw?
	 */
	public void kickBall(double force, double angleXY, double angleXZ) {
		double x, y, z;
		// compute z_vector
		z = Ball.getBall().getZ() + (force * Math.sin(angleXZ));		// v[0z] = v[0]sin(a[0])
		Ball.getBall().setZ(z);
		
		// compute y_vector
		y = Ball.getBall().getY() + (force * Math.sin(angleXY));		// v[0y] = v[0]sin(a[0])
		Ball.getBall().setY(y);
		
		// compute x_vector
		x = Ball.getBall().getX() + (force * Math.cos(angleXY));		// v[0x] = v[0]cos(a[0]) 
		Ball.getBall().setX(x);
	}
	
	
	/**
	 * Force(drag) = -1/2 * rho * v^2 * C * A
	 * @param area - surface area of object
	 * @param velocity - speed of object
	 * @return the positive value of the force due to drag, in Newtons.
	 */
	public double airDrag(double area, double velocity) {
		return ( 0.5*AIR_DENSITY*(velocity*velocity)*DRAG_COEFFICIENT*area );
	}
	
	/**
	 * Rolling Friction = Mu[r] * n
	 * @param newtons - Force of the rolling object.
	 * @return newtons - New force of object after applying friction.
	 */
	public double rollingFriction(double newtons) {
		return (ROLLING_COEFFICIENT * newtons);
	}
	
	/**
	 * Terminal Speed = sqrt( m g / D )
	 * @param mass - Mass of object falling
	 * @return Top speed for the object trough the fluid(air) in m/s.
	 */
	public double terminalSpeed(double mass) {
		return Math.sqrt( (mass * GRAVITY) / DRAG_COEFFICIENT);
	}
	
	/**
	 *  t = 0.01s
	 */
	public void update() {
		double accel = 0.0;
		double velocity = 0.0;
		double outside_forces = 0.0;
		
		// Ball...
		//	...apply forces, then calculate distance moved.
		
		// *** X vector computation *** //
		accel = Ball.getBall().getX() / Ball.getBall().getMass();	
		//System.out.println("A1 " + accel);
		outside_forces = airDrag( 2*Math.PI*(Ball.getBall().getSize()/2), Math.abs(accel*TIME) );
		
		if(Ball.getBall().getZPos() <= 1.0) 							// on ground apply rolling friction too
			outside_forces += rollingFriction( Math.abs(Ball.getBall().getX()) );
		
		if(Ball.getBall().getX() < 0.0)						// update force
			Ball.getBall().setX(Ball.getBall().getX() + outside_forces);
		else
			Ball.getBall().setX(Ball.getBall().getX() - outside_forces);
		
		if( Math.abs(Ball.getBall().getX()) < 0.025 )		// damped check, simplifies rolling w/ torque and what not
			Ball.getBall().setX(0.0);
		
		accel = Ball.getBall().getX() / Ball.getBall().getMass();		// get acceleration 
		//System.out.println("A2 " + accel);
		velocity = accel * TIME;										// get velocity
		
		if( Math.abs(velocity) > terminalSpeed(Ball.getBall().getMass()) ) {	// terminal check
			System.out.println("terminal dude");
			if(velocity < 0.0 )
				velocity =  -1.0 * terminalSpeed(Ball.getBall().getMass());
			else
				velocity = terminalSpeed(Ball.getBall().getMass());
		}
		if( Math.abs(Ball.getBall().getX()) < 0.001 ) 		// stop the ball from rolling slowly forever
			Ball.getBall().setX(0.0);
		else
			Ball.getBall().setXPos( Ball.getBall().getXPos() + velocity );		// update x position			
		
				
		// *** Y vector computation *** //
		accel = Ball.getBall().getY() / Ball.getBall().getMass();
		outside_forces = airDrag( 2*Math.PI*(Ball.getBall().getSize()/2), Math.abs(accel*TIME) );

		if(Ball.getBall().getZPos() <= 1.0) 							// on ground apply rolling friction too
			outside_forces += rollingFriction( Math.abs(Ball.getBall().getY()) );

		if(Ball.getBall().getY() < 0.0)						// update force
			Ball.getBall().setY(Ball.getBall().getY() + outside_forces);
		else
			Ball.getBall().setY(Ball.getBall().getY() - outside_forces);
		
		if( Math.abs(Ball.getBall().getY()) < 0.025 )		// damped check, simplifies rolling w/ torque and what not
			Ball.getBall().setY(0.0);
		
		accel = Ball.getBall().getY() / Ball.getBall().getMass();	// get acceleration
		velocity = accel * TIME;									// get velocity
		
		if( Math.abs(velocity) > terminalSpeed(Ball.getBall().getMass()) ) {	// terminal check
			System.out.println("terminal dude");
			if(velocity < 0.0 )
				velocity =  -1.0 * terminalSpeed(Ball.getBall().getMass());
			else
				velocity = terminalSpeed(Ball.getBall().getMass());
		}
		if( Math.abs(Ball.getBall().getY()) < 0.03 ) 		// stop the ball from rolling slowly forever
			Ball.getBall().setY(0.0);
		else
			Ball.getBall().setYPos( Ball.getBall().getYPos() + velocity );		// update y position

		
		// *** Z vector computation *** //
		accel = Ball.getBall().getZ() / Ball.getBall().getMass();
		
		if( Ball.getBall().getZ() >= 0.0 ) {		// ball is accelerating upward
			outside_forces = airDrag( 2*Math.PI*(Ball.getBall().getSize()/2), Math.abs(accel*TIME) );
			outside_forces += GRAVITY * TIME;	
			Ball.getBall().setZ( Ball.getBall().getZ() - outside_forces); 	// update force
		}
		else {										// ball is accelerating downwards
			outside_forces = airDrag( 2*Math.PI*(Ball.getBall().getSize()/2), Math.abs(accel*TIME) );
			outside_forces -= GRAVITY * TIME;	
			Ball.getBall().setZ( Ball.getBall().getZ() + outside_forces); 	// update force
		}
			
		accel = Ball.getBall().getZ() / Ball.getBall().getMass();			// get acceleration
		velocity = accel * TIME;											// get velocity
		
		if( Math.abs(velocity) > terminalSpeed(Ball.getBall().getMass()) ) {	// terminal check
			System.out.println("terminal dude");
			if(velocity < 0.0 )
				velocity =  -1.0 * terminalSpeed(Ball.getBall().getMass());
			else
				velocity = terminalSpeed(Ball.getBall().getMass());
		}
		
		if(Ball.getBall().getZPos() + velocity <= 0.0) {	// collision w/ ground check
			//System.out.println("boom");
			if(Math.abs(Ball.getBall().getZ()) < 1.03) {	// if collision is weak enough, stop bouncing
				Ball.getBall().setZPos(0.0);
				Ball.getBall().setZ(0.0);
				
				// Damp X,Y velocity from impact ????? TODO
			}
			else											// strong collision, bouncing ball
				Ball.getBall().setZ( Ball.getBall().getZ() * -0.7 ); // simplified collision, reverse direction of force and reduce it by 30%.
		}
		else
			Ball.getBall().setZPos( Ball.getBall().getZPos() + velocity );		// update z position
		
		if(Driver.debug) {		// debugging output
			System.out.println("Pos: (" + Ball.getBall().getXPos() + ", " + Ball.getBall().getYPos() + ", " + Ball.getBall().getZPos() + ")");
			System.out.println("N: (" + Ball.getBall().getX() + ", " + Ball.getBall().getY() + ", " + Ball.getBall().getZ() + ")");
		}
		
		// update players TODO
		for(int i=0; i<11; i++) {
			
		}
		
	} // end update()
	
	public void updateSimple() {
		double accel = 0.0;
		double velocity = 0.0;
		double outside_forces = 0.25;				// set value of external roll damping forces
		double mass = Ball.getBall().getMass();
		
		// Update Ball
			// x_vector computation
		if(Ball.getBall().getX() < 0.025)			// damp check
			Ball.getBall().setX(0.0);
		else {										// apply damping force
			if(Ball.getBall().getX() > 0.0)			// determine direction of force
				Ball.getBall().setX( Ball.getBall().getX() - outside_forces );	
			else
				Ball.getBall().setX( Ball.getBall().getX() + outside_forces );
			
			accel = Ball.getBall().getX() * mass;
			velocity = accel * TIME;
			
			Ball.getBall().setXPos( Ball.getBall().getXPos() + velocity );
		}
		
			// y_vector computation
		if(Ball.getBall().getY() < 0.025)			// damp check
			Ball.getBall().setY(0.0);
		else {										// apply damping force
			if(Ball.getBall().getY() > 0.0)			// determine direction of force
				Ball.getBall().setY( Ball.getBall().getY() - outside_forces );	
			else
				Ball.getBall().setY( Ball.getBall().getY() + outside_forces );
			
			accel = Ball.getBall().getY() * mass;
			velocity = accel * TIME;
			
			Ball.getBall().setYPos( Ball.getBall().getYPos() + velocity );
		}
		
		// update players
		for(int i=0; i<11; i++) {
			
			// x_vector computation
			accel = Team.getHome().getSquad().get(i).getX() * PLAYER_MASS;
			// accel = 0
			velocity = accel * TIME;
			Team.getHome().getSquad().get(i).setXPos(Team.getHome().getSquad().get(i).getXPos() + velocity);
			
			
			// y_vector computation
			
		}
		
	} // end updateSimple()
}

