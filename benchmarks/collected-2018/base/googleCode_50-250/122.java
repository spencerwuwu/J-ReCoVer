// https://searchcode.com/api/result/1115554/

package cc.creativecomputing.math;



public class CCQuaternion {
	private final static float EPSILON = 0.0001f;
    public float w;

    public float x;

    public float y;

    public float z;

    public CCQuaternion() {
        reset();
    }
    
    public CCQuaternion(final CCVector3f theVector, final float theAngle){
    	set(theVector, theAngle);
    }


    public CCQuaternion(float theW, float theX, float theY, float theZ) {
        w = theW;
        x = theX;
        y = theY;
        z = theZ;
    }


    public void reset() {
        w = 1.0f;
        x = 0.0f;
        y = 0.0f;
        z = 0.0f;
    }

    public void set(final float theX, final float theY, final float theZ, final float theW){
    	x = theX;
    	y = theY;
    	z = theZ;
    	w = theW;
    }
    
    public void set(final float[] theValues){
    	x = theValues[0];
    	y = theValues[1];
    	z = theValues[2];
    	w = theValues[3];
    }

    public void set(CCVector3f theVector3f, float theW) {
        w = theW;
        x = theVector3f.x();
        y = theVector3f.y();
        z = theVector3f.z();
    }


    public void set(CCQuaternion theQuaternion) {
        w = theQuaternion.w;
        x = theQuaternion.x;
        y = theQuaternion.y;
        z = theQuaternion.z;
    }


    public void multiply(CCQuaternion theA, CCQuaternion theB) {
        w = theA.w * theB.w - theA.x * theB.x - theA.y * theB.y - theA.z * theB.z;
        x = theA.w * theB.x + theA.x * theB.w + theA.y * theB.z - theA.z * theB.y;
        y = theA.w * theB.y + theA.y * theB.w + theA.z * theB.x - theA.x * theB.z;
        z = theA.w * theB.z + theA.z * theB.w + theA.x * theB.y - theA.y * theB.x;
    }

    public void multiply(CCQuaternion theA) {
        float w2 = theA.w * w - theA.x * x - theA.y * y - theA.z * z;
        float x2 = theA.w * x + theA.x * w + theA.y * z - theA.z * y;
        float y2 = theA.w * y + theA.y * w + theA.z * x - theA.x * z;
        float z2 = theA.w * z + theA.z * w + theA.x * y - theA.y * x;
        w = w2;
        x = x2;
        y = y2;
        z = z2;
    }

    public CCVector4f getVectorAndAngle() {
        CCVector4f theResult = new CCVector4f();

        float s = (float) Math.sqrt(1.0f - w * w);
        if (s < EPSILON) {
            s = 1.0f;
        }

        theResult.w(CCMath.acos(w) * 2.0f);
        theResult.x(x / s);
        theResult.y(y / s);
        theResult.z(z / s);

        return theResult;
    }
    
    public void fromVectorAndAngle(final CCVector4f theVector){
    	fromVectorAndAngle(theVector.w, theVector.x, theVector.y, theVector.z);
    }
    
    public void fromVectorAndAngle(final float theAngle, final float theX, final float theY, final float theZ) {
    	w = CCMath.cos(theAngle / 2);
    	
    	float s = (float) Math.sqrt(1.0f - w * w);
        if (s < EPSILON) {
            s = 1.0f;
        }
        
        x = theX * s;
        y = theY * s;
        z = theZ * s;
    }
    
    /**
     * <code>fromAngles</code> builds a quaternion from the Euler rotation
     * angles (y,r,p).
     *
     * @param angles
     *            the Euler angles of rotation (in radians).
     */
    public void fromAngles(float[] angles) {
        if (angles.length != 3)
            throw new IllegalArgumentException(
                    "Angles array must have three elements");


        fromAngles(angles[0], angles[1], angles[2]);
    }

    /**
	 * <code>fromAngles</code> builds a Quaternion from the Euler rotation angles (y,r,p). 
	 * Note that they are applied in order: roll, pitch, but are ordered in x, y, and z 
	 * for convenience. 
	 * 
	 * See: http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToQuaternion/index.htm
	 * 
	 * @param theYaw the Euler yaw of rotation in radians. (aka Bank, often rot around x)
	 * @param roll the Euler roll of rotation in radians. (aka Heading, often rot around y)
	 * @param pitch the Euler pitch of rotation in radians. (aka Attitude, often rot around z)
	 */
	public CCQuaternion fromAngles(final float theYaw, final float roll, final float pitch) {
		float angle;
		float sinRoll, sinPitch, sinYaw, cosRoll, cosPitch, cosYaw;
		angle = pitch * 0.5f;
		sinPitch = CCMath.sin(angle);
        cosPitch = CCMath.cos(angle);
        angle = roll * 0.5f;
        sinRoll = CCMath.sin(angle);
        cosRoll = CCMath.cos(angle);
        angle = theYaw * 0.5f;
        sinYaw = CCMath.sin(angle);
        cosYaw = CCMath.cos(angle);

        // variables used to reduce multiplication calls.
        float cosRollXcosPitch = cosRoll * cosPitch;
        float sinRollXsinPitch = sinRoll * sinPitch;
        float cosRollXsinPitch = cosRoll * sinPitch;
        float sinRollXcosPitch = sinRoll * cosPitch;
       
        w = (cosRollXcosPitch * cosYaw - sinRollXsinPitch * sinYaw);
        x = (cosRollXcosPitch * sinYaw + sinRollXsinPitch * cosYaw);
        y = (sinRollXcosPitch * cosYaw + cosRollXsinPitch * sinYaw);
        z = (cosRollXsinPitch * cosYaw - sinRollXcosPitch * sinYaw);
       
        normalize();
        return this;
    }

    /**
	 * <code>norm</code> returns the norm of this quaternion. This is the dot product of this quaternion with itself.
	 * 
	 * @return the norm of the quaternion.
	 */
	public float norm() {
		return w * w + x * x + y * y + z * z;
	}

	/**
	 * <code>normalize</code> normalizes the current <code>Quaternion</code>
	 */
	public void normalize() {
		float n = CCMath.invSqrt(norm());
		x *= n;
		y *= n;
		z *= n;
		w *= n;
	}
	
	public CCQuaternion clone() {
		return new CCQuaternion(w, x, y, z);
	}

	public String toString() {
		return x + ":" + y + ":" + z + ":"+ w;
	}
}

