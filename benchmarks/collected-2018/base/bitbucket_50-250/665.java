// https://searchcode.com/api/result/56085411/

package roboflight.core.peer;

import java.util.ArrayList;
import java.util.List;

import roboflight.Missile;
import roboflight.MissileBlank;
import roboflight.Rules;
import roboflight.Utils;
import roboflight.events.ExecuteEvent;
import roboflight.phys.Sphere;

public final class MissilePeer extends EnginePeer {
	/**
	 * I made this final to avoid having to check if it is initialized. Since
	 * either it will be or the class fails to initialize!
	 */
	public final Missile missile;
	public final Sphere sphere;
	public RobotPeer owner;
	public boolean firstExecute = true;
	public double energy = 0;

	public boolean dieOnUpdate = false;
	public boolean isBlank = false;
	public long timeFired = 0;

	// Trails
	public List<double[]> trail = new ArrayList<double[]>();

	{
		rot_limit[0] = Rules.MISSILE_LIMIT_ROLL;
		rot_limit[1] = Rules.MISSILE_LIMIT_YAW;
		rot_limit[2] = Rules.MISSILE_LIMIT_PITCH;
		thrust_max[0] = Rules.MISSILE_MAX_THRUST_X;
		thrust_max[1] = Rules.MISSILE_MAX_THRUST_Y;
		thrust_max[2] = Rules.MISSILE_MAX_THRUST_Y;
		thrust_min[0] = Rules.MISSILE_MIN_THRUST_X;
		thrust_min[1] = Rules.MISSILE_MIN_THRUST_Y;
		thrust_min[2] = Rules.MISSILE_MIN_THRUST_Y;
		speed_limit = Rules.MISSILE_MAX_SPEED;
	}

	public MissilePeer(Class<? extends Missile> cls) throws InstantiationException, IllegalAccessException {
		missile = cls.newInstance();
		missile.setPeer(this);
		if(missile instanceof MissileBlank) isBlank = true;;
		sphere = new Sphere(location, 2);
	}

	@Override
	public final void update() {
		if(engine.tick % 4 == 0)
			trail.add(new double[]{ location.x, location.y, location.z });
		updatePeer();

		if(dieOnUpdate) {
			engine.killMissile(this);
			return;
		}

		if(location.distance(engine.center) >= 1000) {
			engine.killMissile(this);
			return;
		}

		// Reduce the energy
		double drain = velocity.length() / Rules.MISSILE_FUEL_ENERGY_RATIO;
		if(isBlank) drain *= 0.5;
		energy -= drain;

		if(energy <= 0) {
			engine.killMissile(this);
		}
	}

	public double getDamage() {
		double damage = Rules.getExplosionYield(energy, missile.getYieldRatio());
		if(damage < 1.0 || isBlank)
			damage = 1;
		return damage;
	}

	public void setThrust(double x) {
		x = Utils.limit(Rules.MISSILE_MIN_THRUST_X, x, Rules.MISSILE_MAX_THRUST_X);
		thrust.setX(x);
	}

	public long lastUpdate = 0;

	public void updateBoundingBox() {
		if(lastUpdate != engine.tick) {
			sphere.center = location;
			lastUpdate = engine.tick;
		}
	}

	@Override
	public void execute() {
		if(firstExecute) {
			missile.onStart();
			firstExecute = false;
		}
		missile.onExecute(new ExecuteEvent() {
			@Override
			public long getTime() {
				return engine.tick;
			}
		});
	}

}

