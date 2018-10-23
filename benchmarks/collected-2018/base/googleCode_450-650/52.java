// https://searchcode.com/api/result/1114384/

package cc.creativecomputing.math;


public class CCQuaternion {
	/**
     * <code>slerp</code> sets this quaternion's value as an interpolation
     * between two other quaternions.
     *
     * @param theQ1
     *            the first quaternion.
     * @param theQ2
     *            the second quaternion.
     * @param t
     *            the amount to interpolate between the two quaternions.
     */
    public static CCQuaternion blend(CCQuaternion theQ1, CCQuaternion theQ2, float t) {
        // Create a local quaternion to store the interpolated quaternion
        if (theQ1.x == theQ2.x && theQ1.y == theQ2.y && theQ1.z == theQ2.z && theQ1.w == theQ2.w) {
            return new CCQuaternion(theQ1);
        }

        float result = 
        	(theQ1.x * theQ2.x) + 
        	(theQ1.y * theQ2.y) + 
        	(theQ1.z * theQ2.z) + 
        	(theQ1.w * theQ2.w);

        if (result < 0.0f) {
            // Negate the second quaternion and the result of the dot product
            theQ2.x = -theQ2.x;
            theQ2.y = -theQ2.y;
            theQ2.z = -theQ2.z;
            theQ2.w = -theQ2.w;
            result = -result;
        }

        // Set the first and second scale for the interpolation
        float scale0 = 1 - t;
        float scale1 = t;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - result) > 0.1f) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            float theta = CCMath.acos(result);
            float invSinTheta = 1f / CCMath.sin(theta);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = CCMath.sin((1 - t) * theta) * invSinTheta;
            scale1 = CCMath.sin((t * theta)) * invSinTheta;
        }

        // Calculate the x, y, z and w values for the quaternion by using a
        // special
        // form of linear interpolation for quaternions.
        return new CCQuaternion(
        	(scale0 * theQ1.w) + (scale1 * theQ2.w), 
        	(scale0 * theQ1.x) + (scale1 * theQ2.x), 
        	(scale0 * theQ1.y) + (scale1 * theQ2.y), 
        	(scale0 * theQ1.z) + (scale1 * theQ2.z)
        );
    }
    
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
    
    public CCQuaternion(CCQuaternion theQuaternion) {
        set(theQuaternion);
    }


    public CCQuaternion(float theW, float theX, float theY, float theZ) {
        w = theW;
        x = theX;
        y = theY;
        z = theZ;
    }

    /**
     * Sets this Quaternion to {0, 0, 0, 1}.  Same as calling set(0,0,0,1).
     */
    public void loadIdentity() {
        x = y = z = 0;
        w = 1;
    }

    /**
     * @return true if this Quaternion is {0,0,0,1}
     */
    public boolean isIdentity() {
        if (x == 0 && y == 0 && z == 0 && w == 1) {
            return true;
        } else {
            return false;
        }
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
    
    /**
     * <code>mult</code> multiplies this quaternion by a parameter vector. The
     * result is stored in the supplied vector
     *
     * @param v
     *            the vector to multiply this quaternion by.
     * @return v
     */
    public CCVector3f multLocal(CCVector3f v) {
        float tempX, tempY;
        tempX = 
        	w * w * v.x + 
        	2 * y * w * v.z - 2 * z * w * v.y + x * x * v.x + 
        	2 * y * x * v.y + 2 * z * x * v.z - z * z * v.x - y * y * v.x;
        tempY = 
        	2 * x * y * v.x + y * y * v.y + 2 * z * y * v.z + 
        	2 * w * z * v.x - z * z * v.y + w * w * v.y - 
        	2 * x * w * v.z - x * x * v.y;
        v.z = 
        	2 * x * z * v.x + 2 * y * z * v.y + z * z * v.z - 
        	2 * w * y * v.x - y * y * v.z + 2 * w * x * v.y - x * x * v.z + w * w * v.z;
        v.x = tempX;
        v.y = tempY;
        return v;
    }

    /**
     * Multiplies this Quaternion by the supplied quaternion. The result is
     * stored in this Quaternion, which is also returned for chaining. Similar
     * to this *= q.
     *
     * @param q
     *            The Quaternion to multiply this one by.
     * @return This Quaternion, after multiplication.
     */
    public CCQuaternion multLocal(CCQuaternion q) {
        float x1 = x * q.w + y * q.z - z * q.y + w * q.x;
        float y1 = -x * q.z + y * q.w + z * q.x + w * q.y;
        float z1 = x * q.y - y * q.x + z * q.w + w * q.z;
        w = -x * q.x - y * q.y - z * q.z + w * q.w;
        x = x1;
        y = y1;
        z = z1;
        return this;
    }
    
    /**
     * <code>mult</code> multiplies this quaternion by a parameter vector. The
     * result is returned as a new vector.
     * 
     * @param theVector
     *            the vector to multiply this quaternion by.
     * @param theOutput
     *            the vector to store the result in. It IS safe for v and store
     *            to be the same object.
     * @return the result vector.
     */
    public CCVector3f multiply(CCVector3f theVector, CCVector3f theOutput) {
        if (theOutput == null) {
            theOutput = new CCVector3f();
        }
        if (theVector.x == 0 && theVector.y == 0 && theVector.z == 0) {
            theOutput.set(0, 0, 0);
        } else {
            float vx = theVector.x, vy = theVector.y, vz = theVector.z;
            theOutput.x = w * w * vx + 2 * y * w * vz - 2 * z * w * vy + x * x
                    * vx + 2 * y * x * vy + 2 * z * x * vz - z * z * vx - y
                    * y * vx;
            theOutput.y = 2 * x * y * vx + y * y * vy + 2 * z * y * vz + 2 * w
                    * z * vx - z * z * vy + w * w * vy - 2 * x * w * vz - x
                    * x * vy;
            theOutput.z = 2 * x * z * vx + 2 * y * z * vy + z * z * vz - 2 * w
                    * y * vx - y * y * vz + 2 * w * x * vy - x * x * vz + w
                    * w * vz;
        }
        return theOutput;
    }
    
    public CCVector3f multiply(CCVector3f theVector) {
    	return multiply(theVector, null);
    }
    
    /**
     * <code>dot</code> calculates and returns the dot product of this
     * quaternion with that of the parameter quaternion.
     *
     * @param theQuaternion
     *            the quaternion to calculate the dot product of.
     * @return the dot product of this and the parameter quaternion.
     */
    public float dot(CCQuaternion theQuaternion) {
        return w * theQuaternion.w + x * theQuaternion.x + y * theQuaternion.y + z * theQuaternion.z;
    }

    /**
     * Sets the values of this quaternion to the slerp from itself to q2 by
     * changeAmnt
     *
     * @param q2
     *            Final interpolation value
     * @param changeAmnt
     *            The amount diffrence
     */
    public void slerp(CCQuaternion q2, float changeAmnt) {
        if (this.x == q2.x && this.y == q2.y && this.z == q2.z
                && this.w == q2.w) {
            return;
        }

        float result = (this.x * q2.x) + (this.y * q2.y) + (this.z * q2.z)
                + (this.w * q2.w);

        if (result < 0.0f) {
            // Negate the second quaternion and the result of the dot product
            q2.x = -q2.x;
            q2.y = -q2.y;
            q2.z = -q2.z;
            q2.w = -q2.w;
            result = -result;
        }

        // Set the first and second scale for the interpolation
        float scale0 = 1 - changeAmnt;
        float scale1 = changeAmnt;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - result) > 0.1f) {
            // Get the angle between the 2 quaternions, and then store the sin()
            // of that angle
            float theta = CCMath.acos(result);
            float invSinTheta = 1f / CCMath.sin(theta);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = CCMath.sin((1 - changeAmnt) * theta) * invSinTheta;
            scale1 = CCMath.sin((changeAmnt * theta)) * invSinTheta;
        }

        // Calculate the x, y, z and w values for the quaternion by using a
        // special
        // form of linear interpolation for quaternions.
        this.x = (scale0 * this.x) + (scale1 * q2.x);
        this.y = (scale0 * this.y) + (scale1 * q2.y);
        this.z = (scale0 * this.z) + (scale1 * q2.z);
        this.w = (scale0 * this.w) + (scale1 * q2.w);
    }

    /**
     * Sets the values of this quaternion to the nlerp from itself to q2 by blend.
     * @param q2
     * @param blend
     */
    public void nlerp(CCQuaternion q2, float blend) {
        float dot = dot(q2);
        float blendI = 1.0f - blend;
        if (dot < 0.0f) {
            x = blendI * x - blend * q2.x;
            y = blendI * y - blend * q2.y;
            z = blendI * z - blend * q2.z;
            w = blendI * w - blend * q2.w;
        } else {
            x = blendI * x + blend * q2.x;
            y = blendI * y + blend * q2.y;
            z = blendI * z + blend * q2.z;
            w = blendI * w + blend * q2.w;
        }
        normalize();
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
     * <code>toAngles</code> returns this quaternion converted to Euler
     * rotation angles (yaw,roll,pitch).<br/>
     * See http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToEuler/index.htm
     * 
     * @param angles
     *            the float[] in which the angles should be stored, or null if
     *            you want a new float[] to be created
     * @return the float[] in which the angles are stored.
     */
    public float[] toAngles(float[] angles) {
        if (angles == null) {
            angles = new float[3];
        } else if (angles.length != 3) {
            throw new IllegalArgumentException("Angles array must have three elements");
        }

        float sqw = w * w;
        float sqx = x * x;
        float sqy = y * y;
        float sqz = z * z;
        float unit = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
        // is correction factor
        float test = x * y + z * w;
        if (test > 0.499 * unit) { // singularity at north pole
            angles[1] = 2 * CCMath.atan2(x, w);
            angles[2] = CCMath.HALF_PI;
            angles[0] = 0;
        } else if (test < -0.499 * unit) { // singularity at south pole
            angles[1] = -2 * CCMath.atan2(x, w);
            angles[2] = -CCMath.HALF_PI;
            angles[0] = 0;
        } else {
            angles[1] = CCMath.atan2(2 * y * w - 2 * x * z, sqx - sqy - sqz + sqw); // roll or heading 
            angles[2] = CCMath.asin(2 * test / unit); // pitch or attitude
            angles[0] = CCMath.atan2(2 * x * w - 2 * y * z, -sqx + sqy - sqz + sqw); // yaw or bank
        }
        return angles;
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
	


    /**
     * <code>inverse</code> returns the inverse of this quaternion as a new
     * quaternion. If this quaternion does not have an inverse (if its normal is
     * 0 or less), then null is returned.
     *
     * @return the inverse of this quaternion or null if the inverse does not
     *         exist.
     */
    public CCQuaternion inverse() {
        float norm = norm();
        if (norm > 0.0) {
            float invNorm = 1.0f / norm;
            return new CCQuaternion(
            	-x * invNorm, 
            	-y * invNorm, 
            	-z * invNorm, 
            	w * invNorm
            );
        }
        // return an invalid result to flag the error
        return null;
    }

    /**
     * <code>inverse</code> calculates the inverse of this quaternion and
     * returns this quaternion after it is calculated. If this quaternion does
     * not have an inverse (if it's norma is 0 or less), nothing happens
     *
     * @return the inverse of this quaternion
     */
    public CCQuaternion inverseLocal() {
        float norm = norm();
        if (norm > 0.0) {
            float invNorm = 1.0f / norm;
            x *= -invNorm;
            y *= -invNorm;
            z *= -invNorm;
            w *= invNorm;
        }
        return this;
    }
	
	public CCQuaternion clone() {
		return new CCQuaternion(w, x, y, z);
	}

	/**
    *
    * <code>toString</code> creates the string representation of this
    * <code>Quaternion</code>. The values of the quaternion are displace (x,
    * y, z, w), in the following manner: <br>
    * (x, y, z, w)
    *
    * @return the string representation of this object.
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
       return "(" + x + ", " + y + ", " + z + ", " + w + ")";
   }

   /**
    * <code>equals</code> determines if two quaternions are logically equal,
    * that is, if the values of (x, y, z, w) are the same for both quaternions.
    *
    * @param theObject
    *            the object to compare for equality
    * @return true if they are equal, false otherwise.
    */
   @Override
   public boolean equals(Object theObject) {
       if (!(theObject instanceof CCQuaternion)) {
           return false;
       }

       if (this == theObject) {
           return true;
       }

       CCQuaternion myQuaternion = (CCQuaternion) theObject;
       return 
       		x == myQuaternion.x && 
       		y == myQuaternion.y && 
       		z == myQuaternion.z && 
       		w == myQuaternion.w;
   }
}

