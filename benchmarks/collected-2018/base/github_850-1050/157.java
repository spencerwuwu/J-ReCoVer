// https://searchcode.com/api/result/99334556/

package net.blit.core;





public class Mat4 {

    public float [] m = new float[16];
        
    public Mat4() {
    }
    
    public Mat4(Mat4 source) {
    	this.copyArray(this.m, 0, source.m, 0);
    }
    
    public Mat4(float [] source) {
    	this(source, 0);
    }
    
    public Mat4(float [] source, int offset) {
    	System.arraycopy(source, offset, this.m, 0, 16);
    }    
    
    private void copyArray(float [] dst, int dst_offset, float [] src, int src_offset) {
    	System.arraycopy(src, src_offset, dst, dst_offset, 16);
    }

    public void copyTo(Mat4 dst) {
        this.copyArray(dst.m, 0, this.m, 0);
    }

    public void copyFrom(Mat4 src) {
    	this.copyArray(this.m, 0, src.m, 0);
    }

    
    public void setIdentity () {
        
        float [] m = this.m;
        
        m[0] = 1;
        m[1] = 0;
        m[2] = 0;
        m[3] = 0;
    
        m[4] = 0;
        m[5] = 1;
        m[6] = 0;
        m[7] = 0;
    
        m[8] = 0;
        m[9] = 0;
        m[10] = 1;
        m[11] = 0;
    
        m[12] = 0;
        m[13] = 0;
        m[14] = 0;
        m[15] = 1;
        
    }

    public void setScaling(float sx, float sy, float sz) {
        float [] m = this.m;
        m[0] = sx;
        m[1] = 0;
        m[2] = 0;
        m[3] = 0;
        m[4] = 0;
        m[5] = sy;
        m[6] = 0;
        m[7] = 0;
        m[8] = 0;
        m[9] = 0;
        m[10] = sz;
        m[11] = 0;
        m[12] = 0;
        m[13] = 0;
        m[14] = 0;
        m[15] = 1;
    }    
    
    
    public void inverseAffine (Mat4 target) {
    	Mat4 source = this;
        float[] sourcem = source.m;
        float i00 = sourcem[0];
        float i01 = sourcem[1];
        float i02 = sourcem[2];
        float i03 = sourcem[3];
        float i10 = sourcem[4];
        float i11 = sourcem[5];
        float i12 = sourcem[6];
        float i13 = sourcem[7];
        float i20 = sourcem[8];
        float i21 = sourcem[9];
        float i22 = sourcem[10];
        float i23 = sourcem[11];
        float i30 = sourcem[12];
        float i31 = sourcem[13];
        float i32 = sourcem[14];
        float i33 = sourcem[15];
    
        float s0  = i00 * i11 - i10 * i01;
        float s1  = i00 * i12 - i10 * i02;
        float s2  = i00 * i13 - i10 * i03;
        float s3  = i01 * i12 - i11 * i02;
        float s4  = i01 * i13 - i11 * i03;
        float s5  = i02 * i13 - i12 * i03;
    
        float c5  = i22 * i33 - i32 * i23;
        float c4  = i21 * i33 - i31 * i23;
        float c3  = i21 * i32 - i31 * i22;
        float c2  = i20 * i33 - i30 * i23;
        float c1  = i20 * i32 - i30 * i22;
        float c0  = i20 * i31 - i30 * i21;
    
        // Should check for 0 determinant
    
        float invdet  = 1 / (s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0);
    
        float mi00 = (i11 * c5 - i12 * c4 + i13 * c3) * invdet;
        float mi01 = (-i01 * c5 + i02 * c4 - i03 * c3) * invdet;
        float mi02 = (i31 * s5 - i32 * s4 + i33 * s3) * invdet;
        float mi03 = (-i21 * s5 + i22 * s4 - i23 * s3) * invdet;
    
        float mi10 = (-i10 * c5 + i12 * c2 - i13 * c1) * invdet;
        float mi11 = (i00 * c5 - i02 * c2 + i03 * c1) * invdet;
        float mi12 = (-i30 * s5 + i32 * s2 - i33 * s1) * invdet;
        float mi13 = (i20 * s5 - i22 * s2 + i23 * s1) * invdet;
    
        float mi20 = (i10 * c4 - i11 * c2 + i13 * c0) * invdet;
        float mi21 = (-i00 * c4 + i01 * c2 - i03 * c0) * invdet;
        float mi22 = (i30 * s4 - i31 * s2 + i33 * s0) * invdet;
        float mi23 = (-i20 * s4 + i21 * s2 - i23 * s0) * invdet;
    
        float mi30 = (-i10 * c3 + i11 * c1 - i12 * c0) * invdet;
        float mi31 = (i00 * c3 - i01 * c1 + i02 * c0) * invdet;
        float mi32 = (-i30 * s3 + i31 * s1 - i32 * s0) * invdet;
        float mi33 = (i20 * s3 - i21 * s1 + i22 * s0) * invdet;
       
        float [] targetm = target.m;
        targetm[0] = mi00;
        targetm[1] = mi01;
        targetm[2] = mi02;
        targetm[3] = mi03;
        targetm[4] = mi10;
        targetm[5] = mi11;
        targetm[6] = mi12;
        targetm[7] = mi13;
        targetm[8] = mi20;
        targetm[9] = mi21;
        targetm[10] = mi22;
        targetm[11] = mi23;
        targetm[12] = mi30;
        targetm[13] = mi31;
        targetm[14] = mi32;
        targetm[15] = mi33;        
        
    
//        var sourcem = source.m;
//        var i00 = sourcem[0];
//        var i01 = sourcem[1];
//        var i02 = sourcem[2];
//        var i03 = sourcem[3];
//        var i10 = sourcem[4];
//        var i11 = sourcem[5];
//        var i12 = sourcem[6];
//        var i13 = sourcem[7];
//        var i20 = sourcem[8];
//        var i21 = sourcem[9];
//        var i22 = sourcem[10];
//        var i23 = sourcem[11];
//        var i30 = sourcem[12];
//        var i31 = sourcem[13];
//        var i32 = sourcem[14];
//        var i33 = sourcem[15];
//        var s0 = i00 * i11 - i10 * i01;
//        var s1 = i00 * i12 - i10 * i02;
//        var s2 = i00 * i13 - i10 * i03;
//        var s3 = i01 * i12 - i11 * i02;
//        var s4 = i01 * i13 - i11 * i03;
//        var s5 = i02 * i13 - i12 * i03;
//        var c5 = i22 * i33 - i32 * i23;
//        var c4 = i21 * i33 - i31 * i23;
//        var c3 = i21 * i32 - i31 * i22;
//        var c2 = i20 * i33 - i30 * i23;
//        var c1 = i20 * i32 - i30 * i22;
//        var c0 = i20 * i31 - i30 * i21;
//        var invdet = 1.0 / (s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0);
//        var mi00 = (i11 * c5 - i12 * c4 + i13 * c3) * invdet;
//        var mi01 = (-i01 * c5 + i02 * c4 - i03 * c3) * invdet;
//        var mi02 = (i31 * s5 - i32 * s4 + i33 * s3) * invdet;
//        var mi03 = (-i21 * s5 + i22 * s4 - i23 * s3) * invdet;
//        var mi10 = (-i10 * c5 + i12 * c2 - i13 * c1) * invdet;
//        var mi11 = (i00 * c5 - i02 * c2 + i03 * c1) * invdet;
//        var mi12 = (-i30 * s5 + i32 * s2 - i33 * s1) * invdet;
//        var mi13 = (i20 * s5 - i22 * s2 + i23 * s1) * invdet;
//        var mi20 = (i10 * c4 - i11 * c2 + i13 * c0) * invdet;
//        var mi21 = (-i00 * c4 + i01 * c2 - i03 * c0) * invdet;
//        var mi22 = (i30 * s4 - i31 * s2 + i33 * s0) * invdet;
//        var mi23 = (-i20 * s4 + i21 * s2 - i23 * s0) * invdet;
//        var mi30 = (-i10 * c3 + i11 * c1 - i12 * c0) * invdet;
//        var mi31 = (i00 * c3 - i01 * c1 + i02 * c0) * invdet;
//        var mi32 = (-i30 * s3 + i31 * s1 - i32 * s0) * invdet;
//        var mi33 = (i20 * s3 - i21 * s1 + i22 * s0) * invdet;
//        var targetm = target.m;
//        targetm[0] = mi00;
//        targetm[1] = mi01;
//        targetm[2] = mi02;
//        targetm[3] = mi03;
//        targetm[4] = mi10;
//        targetm[5] = mi11;
//        targetm[6] = mi12;
//        targetm[7] = mi13;
//        targetm[8] = mi20;
//        targetm[9] = mi21;
//        targetm[10] = mi22;
//        targetm[11] = mi23;
//        targetm[12] = mi30;
//        targetm[13] = mi31;
//        targetm[14] = mi32;
//        targetm[15] = mi33;
        
    }        
    
    public void transpose () {
        float [] m = this.m;
        float temp;
        temp = m[1];
        m[1] = m[4];
        m[4] = temp;
        temp = m[2];
        m[2] = m[8];
        m[8] = temp;
        temp = m[3];
        m[3] = m[12];
        m[12] = temp;
        temp = m[6];
        m[6] = m[9];
        m[9] = temp;
        temp = m[7];
        m[7] = m[13];
        m[13] = temp;
        temp = m[11];
        m[11] = m[14];
        m[14] = temp;
    } 

    
    public void copyTransposed (Mat4 tgt) {
    	float [] t = tgt.m;
    	float [] s = this.m;
    	t[0] = s[0];
    	t[1] = s[4];
    	t[2] = s[8];
    	t[3] = s[12];
    	t[4] = s[1];
    	t[5] = s[5];
    	t[6] = s[9];
    	t[7] = s[13];
    	t[8] = s[2];
    	t[9] = s[6];
    	t[10] = s[10];
    	t[11] = s[14];
    	t[12] = s[3];
    	t[13] = s[7];
    	t[14] = s[11];
    	t[15] = s[15];
       
    } 
    
    public void transform(Vec3 result, Vec3 source) {
        float [] m = this.m;
//        var x = source.x * m[0] + source.y * m[1] + source.z * m[2] + m[3];
//        var y = source.y * m[4] + source.y * m[5] + source.z * m[6] + m[7];
//        var z = source.z * m[8] + source.y * m[9] + source.z * m[10] + m[11];
        float x = source.x * m[0] + source.y * m[4] + source.z * m[8] + m[12];
        float y = source.x * m[1] + source.y * m[5] + source.z * m[9] + m[13];
        float z = source.x * m[2] + source.y * m[6] + source.z * m[10] + m[14];
        result.x = x;
        result.y = y;
        result.z = z;
    } 
    
    public void setOrtho (float left, float right, float bottom, float top, float znear, float zfar) {
        
        float r_l = right - left;
        float t_b = top - bottom;
        float f_n = zfar - znear;
        float tx = - (right + left) / (right - left);
        float ty = - (top + bottom) / (top - bottom);
        float tz = - (zfar + znear) / (zfar - znear);
        
        float [] m = this.m;
    
        m[0] = 2 / r_l;
        m[4] = 0;
        m[8] = 0;
        m[12] = tx;
    
        m[1] = 0;
        m[5] = 2 / t_b;
        m[9] = 0;
        m[13] = ty;
    
        m[2] = 0;
        m[6] = 0;
        m[10] = 2 / f_n;
        m[14] = tz;
    
        m[3] = 0;
        m[7] = 0;
        m[11] = 0;
        m[15] = 1;
        
    }
    

    
    public void setPerspective (float fovYDegrees, float aspect, float zNear, float zFar){
        float ymax = zNear * (float)Math.tan(fovYDegrees * Math.PI / 360.0);
        float xmax = ymax * aspect;
        this.setFrustum(-xmax, xmax, -ymax, ymax, zNear, zFar);
    }    
    
    
    
    
	
	    
	
	public void setFrustum (float left, float right, float bottom, float top, float zNear, float zFar) {
	
	    float []m = this.m;
	     
	    float temp = 2 * zNear;
	    float temp2 = right - left;
	    float temp3 = top - bottom;
	    float temp4 = zFar - zNear;
	  
	    m[0] = temp / temp2;
	    m[1] = 0;
	    m[2] = 0;
	    m[3] = 0;
	    m[4] = 0;
	    m[5] = temp / temp3;
	    m[6] = 0;
	    m[7] = 0;
	    m[8] = (right + left) / temp2;
	    m[9] = (top + bottom) / temp3;
	    m[10] = (-zFar - zNear) / temp4;
	    m[11] = -1;
	    m[12] = 0;
	    m[13] = 0;
	    m[14] = (-temp * zFar) / temp4;
	    m[15] = 0;
	    
	}


    
    
    public void setLookAt(float eyex, float eyey, float eyez, float centerx, float centery, float centerz, float upx, float upy, float upz) {
        
        float forwardx = centerx - eyex;
        float forwardy = centery - eyey;
        float forwardz = centerz - eyez;
      
        //  forward.normalize()
        float len = (float)Math.sqrt(forwardx*forwardx+forwardy*forwardy+forwardz*forwardz);
        float epsilon = 0.0000001f;
        if (len > epsilon) {
        	float rlen = 1.0f / len;
            forwardx *= rlen;
            forwardy *= rlen;
            forwardz *= rlen;
        }
      
        //side = forward x up
        float sidex = forwardy * upz - forwardz * upy;
        float sidey = forwardz * upx - forwardx * upz;
        float sidez = forwardx * upy - forwardy * upx;
    
        //  side.normalize()
        len = (float)Math.sqrt(sidex*sidex + sidey*sidey + sidez*sidez);
        if (len > epsilon) {
        	float rlen = 1.0f / len;
            forwardx *= rlen;
            forwardy *= rlen;
            forwardz *= rlen;
        }
      
        //  Recompute up as: up = side x forward
        upx = sidey * forwardz - sidez * forwardy;
        upy = sidez * forwardx - sidex * forwardz;
        upz = sidex * forwardy - sidey * forwardx;
        
        
        Mat4 lookat__temp1 = new Mat4();
        Mat4 lookat__temp2 = new Mat4(); // createtemp
    
        float [] lookat__temp1m = lookat__temp1.m;
    
        lookat__temp1m[0] = sidex;
        lookat__temp1m[4] = sidey;
        lookat__temp1m[8] = sidez;
        lookat__temp1m[12] = 0;
        lookat__temp1m[1] = upx;
        lookat__temp1m[5] = upy;
        lookat__temp1m[9] = upz;
        lookat__temp1m[13] = 0;
        lookat__temp1m[2] = -forwardx;
        lookat__temp1m[6] = -forwardy;
        lookat__temp1m[10] = -forwardz;
        lookat__temp1m[14] = 0;
        lookat__temp1m[3] = 0;
        lookat__temp1m[7] = 0;
        lookat__temp1m[11] = 0;
        lookat__temp1m[15] = 1;
    
        lookat__temp2.setTranslation(-eyex, -eyey, -eyez);
        
        lookat__temp1.mul(this, lookat__temp2);
        
        
        //context.blit().mat4Factory().releaseTempN(2);
        
    }
    
    public void setTranslation (float tx, float ty, float tz) {
        float [] m = this.m;
        m[0] = 1;
        m[1] = 0;
        m[2] = 0;
        m[3] = 0;
        m[4] = 0;
        m[5] = 1;
        m[6] = 0;
        m[7] = 0;
        m[8] = 0;
        m[9] = 0;
        m[10] = 1;
        m[11] = 0;
        m[12] = tx;
        m[13] = ty;
        m[14] = tz;
        m[15] = 1;
    }
    

    public void mul(Mat4 dst, Mat4 rightTerm_) {
        
        float [] result = dst.m;
        float [] left = this.m;
        float [] right = rightTerm_.m;
        
        if (result == left || result == right) {
        	float result0 = left[0] * right[0] + left[4] * right[1] + left[8] * right[2] + left[12] * right[3];
            float result1 = left[1] * right[0] + left[5] * right[1] + left[9] * right[2] + left[13] * right[3];
            float result2 = left[2] * right[0] + left[6] * right[1] + left[10] * right[2] + left[14] * right[3];
            float result3 = left[3] * right[0] + left[7] * right[1] + left[11] * right[2] + left[15] * right[3];
            float result4 = left[0] * right[4] + left[4] * right[5] + left[8] * right[6] + left[12] * right[7];
            float result5 = left[1] * right[4] + left[5] * right[5] + left[9] * right[6] + left[13] * right[7];
            float result6 = left[2] * right[4] + left[6] * right[5] + left[10] * right[6] + left[14] * right[7];
            float result7 = left[3] * right[4] + left[7] * right[5] + left[11] * right[6] + left[15] * right[7];
            float result8 = left[0] * right[8] + left[4] * right[9] + left[8] * right[10] + left[12] * right[11];
            float result9 = left[1] * right[8] + left[5] * right[9] + left[9] * right[10] + left[13] * right[11];
            float result10 = left[2] * right[8] + left[6] * right[9] + left[10] * right[10] + left[14] * right[11];
            float result11 = left[3] * right[8] + left[7] * right[9] + left[11] * right[10] + left[15] * right[11];
            float result12 = left[0] * right[12] + left[4] * right[13] + left[8] * right[14] + left[12] * right[15];
            float result13 = left[1] * right[12] + left[5] * right[13] + left[9] * right[14] + left[13] * right[15];
            float result14 = left[2] * right[12] + left[6] * right[13] + left[10] * right[14] + left[14] * right[15];
            float result15 = left[3] * right[12] + left[7] * right[13] + left[11] * right[14] + left[15] * right[15];
            result[0] = result0;
            result[1] = result1;
            result[2] = result2;
            result[3] = result3;
            result[4] = result4;
            result[5] = result5;
            result[6] = result6;
            result[7] = result7;
            result[8] = result8;
            result[9] = result9;
            result[10] = result10;
            result[11] = result11;
            result[12] = result12;
            result[13] = result13;
            result[14] = result14;
            result[15] = result15;
        } else {
            result[0] = left[0] * right[0] + left[4] * right[1] + left[8] * right[2] + left[12] * right[3];
            result[1] = left[1] * right[0] + left[5] * right[1] + left[9] * right[2] + left[13] * right[3];
            result[2] = left[2] * right[0] + left[6] * right[1] + left[10] * right[2] + left[14] * right[3];
            result[3] = left[3] * right[0] + left[7] * right[1] + left[11] * right[2] + left[15] * right[3];
            result[4] = left[0] * right[4] + left[4] * right[5] + left[8] * right[6] + left[12] * right[7];
            result[5] = left[1] * right[4] + left[5] * right[5] + left[9] * right[6] + left[13] * right[7];
            result[6] = left[2] * right[4] + left[6] * right[5] + left[10] * right[6] + left[14] * right[7];
            result[7] = left[3] * right[4] + left[7] * right[5] + left[11] * right[6] + left[15] * right[7];
            result[8] = left[0] * right[8] + left[4] * right[9] + left[8] * right[10] + left[12] * right[11];
            result[9] = left[1] * right[8] + left[5] * right[9] + left[9] * right[10] + left[13] * right[11];
            result[10] = left[2] * right[8] + left[6] * right[9] + left[10] * right[10] + left[14] * right[11];
            result[11] = left[3] * right[8] + left[7] * right[9] + left[11] * right[10] + left[15] * right[11];
            result[12] = left[0] * right[12] + left[4] * right[13] + left[8] * right[14] + left[12] * right[15];
            result[13] = left[1] * right[12] + left[5] * right[13] + left[9] * right[14] + left[13] * right[15];
            result[14] = left[2] * right[12] + left[6] * right[13] + left[10] * right[14] + left[14] * right[15];
            result[15] = left[3] * right[12] + left[7] * right[13] + left[11] * right[14] + left[15] * right[15];
        }

        
    }

    public String toString () {
        String s = "";
        float [] m = this.m;
        s += "[";
        s += m[0];
        int mlen = m.length;
        for(int i = 1; i < mlen; i++) {
            s += ",";
            s += m[i];
        }
        s += "]";
        return s;
    }
    
    
    
    
    //todo: remove this and use integer literals instead
	private int M(int row, int col) {
	    return col * 4 + row;
	} 
    
    
        
        
    
    public void setRotation(float angleInRads, float x, float y, float z) {
        
        float [] m = this.m;
        
        float angle = angleInRads;
    
        
    //    Generate a 4x4 transformation matrix from glRotate parameters, and
    //    post-multiply the input matrix by it.
    //    This function was contributed by Erich Boleyn (erich@uruk.org).
    //    Optimizations contributed by Rudolf Opalla (rudi@khm.de).
         
        //void _math_matrix_rotate( GLmatrix *mat, GLfloat angle, GLfloat x, GLfloat y, GLfloat z )
        {
           float xx, yy, zz, xy, yz, zx, xs, ys, zs, one_c, s, c;
           
           this.setIdentity();
           
           boolean optimized;
        
           float DEG2RAD = (float)(Math.PI / 180.0);  
           s = (float)Math.sin( angle * DEG2RAD );
           c = (float)Math.cos( angle * DEG2RAD );
        
           //memcpy(m, Identity, sizeof(float)*16);
           optimized = false;
        
           if (x == 0.0) {
              if (y == 0.0) {
                 if (z != 0.0) {
                    optimized = true;
                    // rotate only around z-axis 
                    m[this.M(0,0)] = c;
                    m[this.M(1,1)] = c;
                    if (z < 0.0) {
                        m[this.M(0,1)] = s;
                        m[this.M(1,0)] = -s;
                    } else {
                        m[this.M(0,1)] = -s;
                        m[this.M(1,0)] = s;
                    }
                 }
              } else if (z == 0.0) {
                 optimized = true;
                 // rotate only around y-axis 
                 m[M(0,0)] = c;
                 m[M(2,2)] = c;
                 if (y < 0.0) {
                     m[M(0,2)] = -s;
                     m[M(2,0)] = s;
                 } else {
                     m[M(0,2)] = s;
                    m[M(2,0)] = -s;
                 }
              }
           } else if (y == 0.0) {
              if (z == 0.0) {
                 optimized = false;
                 // rotate only around x-axis 
                 m[M(1,1)] = c;
                 m[M(2,2)] = c;
                 if (x < 0.0) {
                     m[M(1,2)] = s;
                     m[M(2,1)] = -s;
                 } else {
                     m[M(1,2)] = -s;
                     m[M(2,1)] = s;
                 }
              }
           }
        
           if (!optimized) {
              float mag = (float)Math.sqrt(x * x + y * y + z * z);
              if (mag <= 1.0e-4f) 
                 return; // no rotation, leave mat as-is 
    
              x /= mag;
              y /= mag;
              z /= mag;
        
        
              
    //         *     Arbitrary axis rotation matrix.
    //         *
    //         *  This is composed of 5 matrices, Rz, Ry, T, Ry', Rz', multiplied
    //         *  like so:  Rz * Ry * T * Ry' * Rz'.  T is the final rotation
    //         *  (which is about the X-axis), and the two composite transforms
    //         *  Ry' * Rz' and Rz * Ry are (respectively) the rotations necessary
    //         *  from the arbitrary axis to the X-axis then back.  They are
    //         *  all elementary rotations.
    //         *
    //         *  Rz' is a rotation about the Z-axis, to bring the axis vector
    //         *  into the x-z plane.  Then Ry' is applied, rotating about the
    //         *  Y-axis to bring the axis vector parallel with the X-axis.  The
    //         *  rotation about the X-axis is then performed.  Ry and Rz are
    //         *  simply the respective inverse transforms to bring the arbitrary
    //         *  axis back to its original orientation.  The first transforms
    //         *  Rz' and Ry' are considered inverses, since the data from the
    //         *  arbitrary axis gives you info on how to get to it, not how
    //         *  to get away from it, and an inverse must be applied.
    //         *
    //         *  The basic calculation used is to recognize that the arbitrary
    //         *  axis vector (x, y, z), since it is of unit length, actually
    //         *  represents the sines and cosines of the angles to rotate the
    //         *  X-axis to the same orientation, with theta being the angle about
    //         *  Z and phi the angle about Y (in the order described above)
    //         *  as follows:
    //         *
    //         *  cos ( theta ) = x / sqrt ( 1 - z^2 )
    //         *  sin ( theta ) = y / sqrt ( 1 - z^2 )
    //         *
    //         *  cos ( phi ) = sqrt ( 1 - z^2 )
    //         *  sin ( phi ) = z
    //         *
    //         *  Note that cos ( phi ) can further be inserted to the above
    //         *  formulas:
    //         *
    //         *  cos ( theta ) = x / cos ( phi )
    //         *  sin ( theta ) = y / sin ( phi )
    //         *
    //         *  ...etc.  Because of those relations and the standard trigonometric
    //         *  relations, it is pssible to reduce the transforms down to what
    //         *  is used below.  It may be that any primary axis chosen will give the
    //         *  same results (modulo a sign convention) using thie method.
    //         *
    //         *  Particularly nice is to notice that all divisions that might
    //         *  have caused trouble when parallel to certain planes or
    //         *  axis go away with care paid to reducing the expressions.
    //         *  After checking, it does perform correctly under all cases, since
    //         *  in all the cases of division where the denominator would have
    //         *  been zero, the numerator would have been zero as well, giving
    //         *  the expected result.
               
        
              xx = x * x;
              yy = y * y;
              zz = z * z;
              xy = x * y;
              yz = y * z;
              zx = z * x;
              xs = x * s;
              ys = y * s;
              zs = z * s;
              one_c = 1.0f - c;
        
              // We already hold the identity-matrix so we can skip some statements 
              
              m[M(0,0)] = (one_c * xx) + c;
              m[M(0,1)] = (one_c * xy) - zs;
              m[M(0,2)] = (one_c * zx) + ys;
              m[M(1,0)] = (one_c * xy) + zs;
              m[M(1,1)] = (one_c * yy) + c;
              m[M(1,2)] = (one_c * yz) - xs;
              m[M(2,0)] = (one_c * zx) - ys;
              m[M(2,1)] = (one_c * yz) + xs;
              m[M(2,2)] = (one_c * zz) + c;
           }
           //#undef M
        
           //matrix_multf( mat, m, MAT_FLAG_ROTATION );
        }
    
    } // end of method setRotation


    
    
    
    private static void test(double a, double b) {
    	if (Math.abs(a-b) > 0.001) throw new RuntimeException(a + " != " + b);
    }
    
    private static void test(Mat4 a, float [] values){
    	if (values.length != 16) throw new RuntimeException();
    	for(int i = 0 ; i < values.length; i++)
        	test(a.m[i], values[i]);
    }
    private static void test(Mat4 a, double [] values){
    	if (values.length != 16) throw new RuntimeException();
    	for(int i = 0 ; i < values.length; i++)
        	test(a.m[i], values[i]);
    }
    
    
    public static void runUnitTests() {

    	
    	//test tempStack mechanism
    	

    	/*
    	{
    		mat4.INTERNAL_assertTempStackEmpty();
    		
    		mat4 a = mat4.createTemp();
    		mat4 b = mat4.createTemp();
    		mat4 c = mat4.createTemp();
    		
    		mat4.releaseTemp(c);
    		mat4.releaseTemp(b);
    		mat4.releaseTemp(a);
    		
    		mat4.INTERNAL_assertTempStackEmpty();
    	}

    	{
    		mat4.INTERNAL_assertTempStackEmpty();
    		
    		mat4 a = mat4.createTemp();
    		
    		mat4 b = mat4.createTemp();
    		mat4 c = mat4.createTemp();
    		mat4.releaseTempN(2);
    		
    		mat4 d = mat4.createTemp();
    		mat4 e = mat4.createTemp();
    		
    		mat4.releaseTemp(e);
    		mat4.releaseTemp(d);
    		
    		mat4 f = mat4.createTemp();
    		
    		mat4.releaseTemp(f);
    		mat4.releaseTemp(a);
    		
    		mat4.INTERNAL_assertTempStackEmpty();
    	}
    	*/
    	
    	
    	
    	/*
    	{
    		Mat4 a = new Mat4(context);
    		a.setIdentity();
    		test(a, new float[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1});
    	}
    	{
    		Mat4 a = new Mat4(context, new float[]{5,4,6,3,4,4,4,1,2,3,1,2,3,1,2,3});
    		Mat4 b = new Mat4(context, new float[]{1,5,3,3,4,2,3,1,2,4,2,3,2,2,1,2});
    		a.copyFrom(b);
    		test(a, new float[]{1,5,3,3,4,2,3,1,2,4,2,3,2,2,1,2});
    		test(b, new float[]{1,5,3,3,4,2,3,1,2,4,2,3,2,2,1,2});
    	}
    	{
    		Mat4 a = new Mat4(context, new float[]{5,4,6,3,4,4,4,1,2,3,1,2,3,1,2,3});
    		Mat4 b = new Mat4(context, new float[]{1,5,3,3,4,2,3,1,2,4,2,3,2,2,1,2});
    		a.copyTo(b);
    		test(a, new float[]{5,4,6,3,4,4,4,1,2,3,1,2,3,1,2,3});
    		test(b, new float[]{5,4,6,3,4,4,4,1,2,3,1,2,3,1,2,3});
    	}
    	{
    		Mat4 a = new Mat4(context, new float[]{5,4,6,3,4,4,4,1,2,3,1,2,3,1,2,3});
    		a.setScaling(3,4,5);
    		test(a, new float[]{3,0,0,0,0,4,0,0,0,0,5,0,0,0,0,1});
    	}
    	
    	
    	{
    		
    		
//        	
//        	OGL gl = blit.opengl().gl();
//    		mat4 a = new mat4(new float[]{1,4,3,2,3,1,4,2,43,1,2,2,4,1,2,3});
//    		mat4 b = new mat4(new float[]{6,5,4,5,3,4,2,3,4,35,4,5,4,6,4,2});
//        	gl.gl11.matrixMode(gl.gl11.PROJECTION);
//        	gl.gl11.loadIdentity();
//    		gl.gl11.multMatrix(a.m, 0);
//    		gl.gl11.multMatrix(b.m, 0);
//    		float [] results = new float[16];
//    		gl.gl2.getGL2().glGetFloatv(gl.gl11.PROJECTION_MATRIX, results, 0);
//    		mat4 c = new mat4(results);
//    		System.out.println(c);
    		//[213.0,38.0,56.0,45.0,113.0,21.0,35.0,27.0,301.0,60.0,170.0,101.0,202.0,28.0,48.0,34.0]
    		
    		Mat4 a = new Mat4(context, new float[]{1,4,3,2,3,1,4,2,43,1,2,2,4,1,2,3});
    		Mat4 b = new Mat4(context, new float[]{6,5,4,5,3,4,2,3,4,35,4,5,4,6,4,2});
    		Mat4 c = new Mat4(context);
    		a.mul(c, b);
    		test(c, new double[]{213.0,38.0,56.0,45.0,113.0,21.0,35.0,27.0,301.0,60.0,170.0,101.0,202.0,28.0,48.0,34.0});    		
    	}
    	
    	{
    		//inverseAffine
    		Mat4 a = new Mat4(context);
    		Mat4 b = new Mat4(context);
    		Mat4 c = new Mat4(context);
    		Mat4 d = new Mat4(context);
    		Mat4 e = new Mat4(context);
    		Mat4 ident = new Mat4(context);
    		ident.setIdentity();
    		a.setRotation(2.345f, 3, 4, 5);
    		b.setTranslation(1.345f, 3, 2);
    		a.mul(c, b);
    		c.inverseAffine(d);
    		c.mul(e, d);
    		test(c, new double[]{0.9993133,0.029133355,-0.022894662,0.0,-0.028731382,0.99943054,0.017694399,0.0,0.023397129,-0.017024443,0.9995813,0.0,1.3046765,3.003427,2.0214524,1.0});
    		test(d, new double[]{0.9993133,-0.028731387,0.02339712,0.0,0.02913335,0.99943054,-0.017024454,0.0,-0.02289467,0.01769439,0.9995813,-0.0,-1.3449999,-2.9999998,-2.0,1.0});
    		test(e, ident.m);
    	}
    	
    	{
    		Mat4 a = new Mat4(context, new float[]{1,4,3,2,3,1,4,2,43,1,2,2,4,1,2,3});
    		a.transpose();
    		test(a, new float[]{1,3,43,4,4,1,1,1,3,4,2,2,2,2,2,3});
    	}
    	{
    		Mat4 a = new Mat4(context, new float[]{1,4,3,2,3,1,4,2,43,1,2,2,4,1,2,3});
    		Mat4 b = new Mat4(context);
    		a.copyTransposed(b);
    		test(a, new float[]{1,4,3,2,3,1,4,2,43,1,2,2,4,1,2,3});
    		test(b, new float[]{1,3,43,4,4,1,1,1,3,4,2,2,2,2,2,3});
    	}
    	{
    		Mat4 a = new Mat4(context, new float[]{1,4,3,2,3,1,4,2,43,1,2,2,4,1,2,3});
    		Vec3 b = new Vec3(context,1,2,3);
    		Vec3 c = new Vec3(context,6,6,6);
    		a.transform(c, b);
    		test(a, new float[]{1,4,3,2,3,1,4,2,43,1,2,2,4,1,2,3});
    		b.INTERNAL_test(b,1,2,3);
    		//vec3.test(b, );
    	}
    	*/
    	
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    //multiply this matrix with translation(x, y, z)
    private static Mat4 tempmat = new Mat4();
    public void translate(float x, float y, float z) {    	
		tempmat.setTranslation(x, y, z);
		this.mul(this, tempmat);
    }
    public void scale(float sx, float sy, float sz) {    	
		tempmat.setScaling(sx, sy, sz);
		this.mul(this, tempmat);
    }
    
    
    
}




