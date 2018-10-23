// https://searchcode.com/api/result/127636419/

package net.ruisystem.audio.listeners;

import net.ruisystem.RUIS;
import net.ruisystem.audio.*;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import java.io.*;
import java.nio.*;
import java.util.Arrays;
import java.util.Vector;

import javax.sound.sampled.*;

import processing.core.PVector;

import edu.emory.mathcs.jtransforms.fft.*;

/**
 * A listener using headphones with HRTF rendering.
 * 
 * The MIT KEMAR HRTF dataset is used.
 * 
 * @author Robert Albrecht 2012
 */
public class HeadphoneListener implements Listener
{
  Vector3f position;
  Vector3f direction;
  Vector3f up;
  
  //RuisSkeleton skeleton;
  RUIS ruis;
  
  SourceDataLine line;
  
  boolean HRTFsLoaded = false;
  boolean headTrackingEnabled = true;

  final float[] HRTFElevations = {-40, -30, -20, -10, 0, 10, 20, 30, 40, 50, 60, 70, 80, 90};
  final int[] HRTFAzimuthsPer180 = {5, 7, 10, 10, 10, 10, 10, 7, 5, 4, 4, 4, 3, 1};

  float[][] HRTFDirections;

  final int HRIRSamples = 128; // samples per file
  final int frameSize = 128; // frame size in samples
  final int HRTFSize = 256; // includes the zero padding necessary for the convolution
  
  final int bufferSize = 2048; // buffer size in samples

  float[][][] HRIRLeft; // head-related impulse responses
  float[][][] HRIRRight;
  
  float[][][] HRTFLeft; // head-related transfer functions (FFT of the impulse responses)
  float[][][] HRTFRight;
  
  int[][][] HRTFMeshTriangles;
  
  float[] convolutionTail; // convolution tail that overlaps with the next frame
  
  final int sampleSizeInBits = 16;
  final int sampleSizeInBytes = 2;
  
  Vector<Rendering> renderings;

  class Rendering
  {
    int elevationIndex;
    int azimuthIndex;
    float gain;
    float[] fft;
  }
  
  /**
   * Creates a new {@code HeadphoneListener} instance.
   * 
   * @param ruis instance of RUIS
   */
  public HeadphoneListener(RUIS ruis)
  {
    this.ruis = ruis;
    
    renderings = new Vector<Rendering>();
    HRTFDirections = new float[HRTFElevations.length][];
    HRIRLeft = new float[HRTFElevations.length][][];
    HRIRRight = new float[HRTFElevations.length][][];
    HRTFLeft = new float[HRTFElevations.length][][];
    HRTFRight = new float[HRTFElevations.length][][];
    
    // calculate the azimuths
    for (int i = 0; i < HRTFElevations.length-1; i++)
    {
      if (HRTFElevations[i] == 50) // 50 degrees elevation lacks 180 degrees azimuth for some reason
      {
        HRTFDirections[i] = new float[2 * HRTFAzimuthsPer180[i]];
        HRIRLeft[i] = new float[2 * HRTFAzimuthsPer180[i]][HRIRSamples];
        HRIRRight[i] = new float[2 * HRTFAzimuthsPer180[i]][HRIRSamples];
        HRTFLeft[i] = new float[2 * HRTFAzimuthsPer180[i]][HRTFSize];
        HRTFRight[i] = new float[2 * HRTFAzimuthsPer180[i]][HRTFSize];
      }
      else
      {
        HRTFDirections[i] = new float[2 * HRTFAzimuthsPer180[i] - 1];
        HRIRLeft[i] = new float[2 * HRTFAzimuthsPer180[i] - 1][HRIRSamples];
        HRIRRight[i] = new float[2 * HRTFAzimuthsPer180[i] - 1][HRIRSamples];
        HRTFLeft[i] = new float[2 * HRTFAzimuthsPer180[i] - 1][HRTFSize];
        HRTFRight[i] = new float[2 * HRTFAzimuthsPer180[i] - 1][HRTFSize];
      }
      
      for (int j = 0; j < HRTFAzimuthsPer180[i]; j++)
      {
        if (HRTFElevations[i] == 50)
        {
          HRTFDirections[i][j] = j * 168.0f/(HRTFAzimuthsPer180[i]-1); // 168 degrees azimuth is the max for 50 degrees elevation
          HRTFDirections[i][HRTFDirections[i].length - 1 - j] = 360.0f - j * 168.0f/(HRTFAzimuthsPer180[i]-1);
        }
        else
        {
          HRTFDirections[i][j] = j * 180.0f/(HRTFAzimuthsPer180[i]-1);
          HRTFDirections[i][HRTFDirections[i].length - 1 - j] = 360.0f - j * 180.0f/(HRTFAzimuthsPer180[i]-1);
        }
      }
    }
    
    // separately add the 90 degree direction with only one measurement
    HRTFDirections[HRTFElevations.length-1] = new float[1];
    HRTFDirections[HRTFElevations.length-1][0] = 0;
    HRIRLeft[HRTFElevations.length-1] = new float[1][HRIRSamples];
    HRIRRight[HRTFElevations.length-1] = new float[1][HRIRSamples];
    HRTFLeft[HRTFElevations.length-1] = new float[1][HRTFSize];
    HRTFRight[HRTFElevations.length-1] = new float[1][HRTFSize];

    // load the head-related impulse responses and calculate the transfer functions  
    try
    {
      for (int i = 0; i < HRTFElevations.length; i++)
      {
        int elevation = Math.round(HRTFElevations[i]);

        for (int j = 0; j < HRTFDirections[i].length; j++)
        {
          int azimuth = Math.round(HRTFDirections[i][j]);

          if (azimuth <= 180)
          {
            String filename = ruis.p.dataPath("")  + String.format("audio/hrir/elev%d/H%de%03da.dat", elevation, elevation, azimuth);

            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));

            // samples are 16-bit big-endian integers
            // apparently java always treats streams as big-endian, so no conversion needed
            short leftSample;
            short rightSample;

            for (int k = 0; k < HRIRSamples; k++)
            {
              // read the samples stored in an interleaved fashion
              leftSample = in.readShort();
              rightSample = in.readShort();

              // convert to float
              HRIRLeft[i][j][k] = ((float)leftSample) / 32768.0f;
              HRIRRight[i][j][k] = ((float)rightSample) / 32768.0f;
            }
          }
          else // azimuths above 180 are mirror images of those below
          {
            for (int k = 0; k < HRIRSamples; k++)
            {
              HRIRLeft[i][j][k] = HRIRRight[i][HRIRRight[i].length - 1 - j][k];
              HRIRRight[i][j][k] = HRIRLeft[i][HRIRLeft[i].length - 1 - j][k];
            }
          }

          // do the FFT

          System.arraycopy(HRIRLeft[i][j], 0, HRTFLeft[i][j], 0, HRIRSamples); // copy over the samples, leaving zero-padding afterwards
          System.arraycopy(HRIRRight[i][j], 0, HRTFRight[i][j], 0, HRIRSamples);

          FloatFFT_1D fft = new FloatFFT_1D(HRTFSize);

          fft.realForward(HRTFLeft[i][j]);
          fft.realForward(HRTFRight[i][j]);
        }
      }
    }
    catch (IOException e)
    {
      System.err.println("Error loading head-related transfer functions:");
      System.err.println(e);
      return;
    }
    
    HRTFsLoaded = true;

    // triangulate the HRTF mesh
    
    HRTFMeshTriangles = new int[HRTFElevations.length - 1][][];
    
    for (int e_down = 0; e_down < HRTFMeshTriangles.length; e_down++)
    {
      int e_up = e_down + 1;
      
      int[][] triangles = new int[HRTFDirections[e_down].length + HRTFDirections[e_up].length - 2][6];
      int t = 0;
      
      int a_up = 0;
      int a_down = 0;
      
      int e1, e2, e3, a1, a2, a3;
      
      while (a_down < HRTFDirections[e_down].length - 1 || a_up < HRTFDirections[e_up].length - 1)
      {
        e1 = e_down;
        a1 = a_down;
        e2 = e_up;
        a2 = a_up;
        
        if (a_down == HRTFDirections[e_down].length - 1)
        {
          e3 = e_up;
          a3 = ++a_up;
        }
        else if (a_up == HRTFDirections[e_up].length - 1)
        {
          e3 = e_down;
          a3 = ++a_down;
        }
        else
        {
          if (HRTFDirections[e_down][a_down + 1] <= HRTFDirections[e_up][a_up + 1])
          {
            e3 = e_down;
            a3 = ++a_down;
          }
          else
          {
            e3 = e_up;
            a3 = ++a_up;
          }
        }
        
        triangles[t][0] = e1;
        triangles[t][1] = a1;
        triangles[t][2] = e2;
        triangles[t][3] = a2;
        triangles[t][4] = e3;
        triangles[t][5] = a3;
        t++;
      }
      
      HRTFMeshTriangles[e_down] = triangles;
    }
    
    convolutionTail = new float[HRIRSamples * 2];
    
    AudioFormat format = new AudioFormat(Audio.sampleRate, sampleSizeInBits, 2, true, true); // 44.1 kHz, 16 bit, 2 channel, signed, big-endian byte order PCM
    
    // open a line for output
    DataLine.Info info = new DataLine.Info(
        SourceDataLine.class,
        format,
        bufferSize * sampleSizeInBytes * 2);
    
    try
    {
      line = (SourceDataLine) AudioSystem.getLine(info);
      
      line.open(format, bufferSize * sampleSizeInBytes * 2);
      line.start();
    } 
    catch (Exception e)
    {
      System.err.println("Could not open SourceDataLine for audio playback:");
      System.err.println(e);
    }
  }

  /**
   * Set the position of the listener's head.
   * 
   * @param position position of the listener's head
   */
  public void setHeadPosition(Vector3f position)
  {
    this.position = position;
  }
  
  /**
   * Get the position of the listener's head.
   * 
   * @return position of the listener's head
   */
  public Vector3f getHeadPosition()
  {
    return position;
  }

  /**
   * Set the direction where the head is facing.
   * 
   * @param direction direction where the head is facing
   */
  public void setHeadDirection(Vector3f direction)
  {
    direction.normalize();
    this.direction = direction;
  }
  
  /**
   * Get the direction where the head is facing.
   * 
   * @return direction where the head is facing
   */
  public Vector3f getHeadDirection()
  {
    return direction;
  }

  /**
   * Set the up direction of the head.
   * 
   * @param up up direction of the head
   */
  public void setHeadUp(Vector3f up)
  {
    up.normalize();
    this.up = up;
  }
  
  /**
   * Get the up direction of the head.
   * 
   * @return up direction of the head
   */
  public Vector3f getHeadUp()
  {
    return up;
  }
  
  /**
   * Set the location and orientation of the headphone listener based on the skeleton.
   * 
   * @param skeleton skeleton to track, set to {@code null} to disable tracking 
   */
  /*public void trackSkeleton(RuisSkeleton skeleton)
  {
    this.skeleton = skeleton;
  }*/
  
  
  public void followHeadTracking(boolean enable)
  {
    headTrackingEnabled = enable;
  }
  
  /**
   * Selects the appropriate HRTFs and gains using vector-based amplitude panning with the three nearest points in the HRTF mesh.
   * 
   * @param elevation elevation angle
   * @param azimuth azimuth
   * @param samples samples of the frame
   * @param amplification amplification to apply to the samples
   */
  private void calculateAmplitudePanning(float elevation, float azimuth, float[] samples, float amplification)
  {
    while (azimuth < 0)
      azimuth += 360.0f;
    
    while (azimuth > 360.0f)
      azimuth -= 360.0f;
    
    // get the triangle from the HRTF mesh to use for VBAP
    
    if (elevation < HRTFElevations[0]) // restrict the elevation to the area where we have HRTF measurements
      elevation = HRTFElevations[0];
    
    // get the nearest point in the mesh
    int e = getElevationIndex(elevation);
    int a = getAzimuthIndex(e, azimuth);
    
    int te = e;
    if (elevation < HRTFElevations[te] || e == HRTFElevations.length - 1) // the point is in the triangles that are one level lower
      te--;
    
    // find the triangle that contains the requested elevation and azimuth
    float[] gains = null;
    int e1 = 0, a1 = 0, e2 = 0, a2 = 0, e3 = 0, a3 = 0;
    
    float threshold = 0;
    
    // first try to get a triangle with positive gains, then gradually accept more negative gains
    // as we might not get a triangle that has purely positive gains at all
    outerloop:
    for (threshold = 0; threshold >= -1; threshold -= 0.01)
    {
      for (int i = 0; i < HRTFMeshTriangles[te].length; i++)
      { 
        if (HRTFMeshTriangles[te][i][0] == e && HRTFMeshTriangles[te][i][1] == a || HRTFMeshTriangles[te][i][2] == e && HRTFMeshTriangles[te][i][3] == a || HRTFMeshTriangles[te][i][4] == e && HRTFMeshTriangles[te][i][5] == a)
        {
          e1 = HRTFMeshTriangles[te][i][0];
          a1 = HRTFMeshTriangles[te][i][1];
          e2 = HRTFMeshTriangles[te][i][2];
          a2 = HRTFMeshTriangles[te][i][3];
          e3 = HRTFMeshTriangles[te][i][4];
          a3 = HRTFMeshTriangles[te][i][5];
          
          gains = VBAP(elevation, azimuth, HRTFElevations[e1], HRTFDirections[e1][a1], HRTFElevations[e2], HRTFDirections[e2][a2], HRTFElevations[e3], HRTFDirections[e3][a3]);
          
          if (gains[0] >= threshold && gains[1] >= threshold && gains[2] >= threshold) // if the gains are positive, we have found the right triangle (also allow slightly negative gains that we might get at the border between two triangles)
            break outerloop;
        }
      }
    }
    
    if (gains != null)
    {
      if (gains[0] < threshold || gains[1] < threshold || gains[2] < threshold)
        System.err.println("Incorrect HRTF mesh triangle found for the headphone listener.");
      
      // perform the convolution with the HRTFs in the frequency domain
      float[] fft = new float[HRTFSize];
      System.arraycopy(samples, 0, fft, 0, samples.length);

      FloatFFT_1D floatFFT = new FloatFFT_1D(HRTFSize);
      floatFFT.realForward(fft);

      Rendering rendering = new Rendering();
      rendering.elevationIndex = e1;
      rendering.azimuthIndex = a1;
      rendering.gain = gains[0] * amplification;
      rendering.fft = fft;
      renderings.add(rendering);

      rendering = new Rendering();
      rendering.elevationIndex = e2;
      rendering.azimuthIndex = a2;
      rendering.gain = gains[1] * amplification;
      rendering.fft = fft;
      renderings.add(rendering);

      rendering = new Rendering();
      rendering.elevationIndex = e3;
      rendering.azimuthIndex = a3;
      rendering.gain = gains[2] * amplification;
      rendering.fft = fft;
      renderings.add(rendering);
    }
    else
    {
      System.err.println("Error in headphone listener HRTF mesh triangulation. No triangle found corresponding to the given elevation and azimuth.");
    }
  }
  
  /**
   * Get the VBAP gains for the specified directional point and triangle.
   * 
   * @param elevation elevation angle of the point
   * @param azimuth azimuth of the point
   * @param elevation_m elevation angle of the first point in the triangle
   * @param azimuth_m azimuth of the first point in the triangle
   * @param elevation_n elevation angle of the second point in the triangle
   * @param azimuth_n azimuth of the second point in the triangle
   * @param elevation_k elevation angle of the third point in the triangle
   * @param azimuth_k azimuth of the third point in the triangle
   * @return an array containing the gains for the three points in the triangle
   */
  private float[] VBAP(float elevation, float azimuth, float elevation_m, float azimuth_m, float elevation_n, float azimuth_n, float elevation_k, float azimuth_k)
  {
    Vector3f p = new Vector3f();
    Vector3f l_m = new Vector3f();
    Vector3f l_n = new Vector3f();
    Vector3f l_k = new Vector3f();
    
    //convert elevation angles to inclination
    float inclination = 180 - (elevation + 90);
    float inclination_m = 180 - (elevation_m + 90);
    float inclination_n = 180 - (elevation_n + 90);
    float inclination_k = 180 - (elevation_k + 90); 
   
    p.x = (float) (Math.cos(Math.toRadians(azimuth)) * Math.sin(Math.toRadians(inclination)));
    p.y = (float) (Math.sin(Math.toRadians(azimuth)) * Math.sin(Math.toRadians(inclination)));
    p.z = (float) Math.cos(Math.toRadians(inclination));
    
    l_m.x = (float) (Math.cos(Math.toRadians(azimuth_m)) * Math.sin(Math.toRadians(inclination_m)));
    l_m.y = (float) (Math.sin(Math.toRadians(azimuth_m)) * Math.sin(Math.toRadians(inclination_m)));
    l_m.z = (float) Math.cos(Math.toRadians(inclination_m));
   
    l_n.x = (float) (Math.cos(Math.toRadians(azimuth_n)) * Math.sin(Math.toRadians(inclination_n)));
    l_n.y = (float) (Math.sin(Math.toRadians(azimuth_n)) * Math.sin(Math.toRadians(inclination_n)));
    l_n.z = (float) Math.cos(Math.toRadians(inclination_n));
    
    l_k.x = (float) (Math.cos(Math.toRadians(azimuth_k)) * Math.sin(Math.toRadians(inclination_k)));
    l_k.y = (float) (Math.sin(Math.toRadians(azimuth_k)) * Math.sin(Math.toRadians(inclination_k)));
    l_k.z = (float) Math.cos(Math.toRadians(inclination_k));
    
    return VBAP(p, l_m, l_n, l_k);
  }
  
  /**
   * Get the VBAP gains for the specified direction and triangle of directions.
   * 
   * @param p the direction
   * @param l_m the first direction in the triangle
   * @param l_n the second direction in the triangle
   * @param l_k the thir direction in the triangle
   * @return an array containing the gains for the three points in the triangle
   */
  float[] VBAP(Vector3f p, Vector3f l_m, Vector3f l_n, Vector3f l_k)
  {    
    Matrix3f L = new Matrix3f();
    
    L.setColumn(0, l_m);
    L.setColumn(1, l_n);
    L.setColumn(2, l_k);
    
    try
    {
      L.invert(); // invert the matrix
    }
    catch (javax.vecmath.SingularMatrixException e)
    {
      System.err.println("Cannot invert matrix for headphone listener VBAP calculations.");
      
      // just return some gains
      float[] gains = {1, 0, 0};
      return gains; 
    }
    
    Vector3f g = new Vector3f();
    
    // perform the multiplication (no method for this?!)
    g.x = L.m00 * p.x + L.m01 * p.y + L.m02 * p.z;
    g.y = L.m10 * p.x + L.m11 * p.y + L.m12 * p.z;
    g.z = L.m20 * p.x + L.m21 * p.y + L.m22 * p.z;
    
    // finish by normalizing the coefficients
    g.normalize();
    
    float[] gains = {g.x, g.y, g.z};
    return gains; 
  }
  
  /**
   * Convolve samples with the head-related impulse response in the specified direction.
   * 
   * @param elevationIndex elevation index for the HRIR
   * @param azimuthIndex azimuth index for the HRIR
   * @param samples samples to convolve with the HRIR
   * @return the convolved samples
   */
  float[] convolveWithImpulseResponse(int elevationIndex, int azimuthIndex, float[] samples)
  {
    float[] leftSamples = convolve(HRIRLeft[elevationIndex][azimuthIndex], samples);
    float[] rightSamples = convolve(HRIRRight[elevationIndex][azimuthIndex], samples);
    
    // interleave the samples
    
    float[] iSamples = new float[leftSamples.length * 2];
    
    for (int i = 0; i < leftSamples.length; i++)
    {
      iSamples[i * 2] = leftSamples[i];
      iSamples[i * 2 + 1] = rightSamples[i];
    }
    
    return iSamples;
  }

  /**
   * Returns the index of the value in {@code haystack} that is closest to the value of {@code needle}.
   *  
   * @param needle number to search for
   * @param haystack array of numbers to search in
   * @return index of the closest number found in the {@code haystack}
   */
  private int nearestNumber(float needle, float[] haystack)
  {
    float currentDistance = Float.MAX_VALUE;
    int currentIndex = -1;
    
    for (int i = 0; i < haystack.length; i++)
    {
      if (Math.abs(needle - haystack[i]) < currentDistance)
      {
        currentDistance = Math.abs(needle - haystack[i]);
        currentIndex = i;
      }
    }
    
    return currentIndex;
  }
  
  /**
   * Get the elevation index closest to the specified elevation angle.
   * 
   * The elevation angle is 0 degrees in the horizontal plane and
   * increases upwards (elevation must be between 90 and -90 degrees).
   * 
   * @param elevation elevation angle
   * @return elevation index
   */
  private int getElevationIndex(float elevation)
  {
    int e = nearestNumber(elevation, HRTFElevations);
    
    if (e == -1)
      e = 0;

    return e;
  }
  
  /**
   * Get the index of the azimuth based on the elevation index and azimuth in degrees.
   * 
   * The azimuth starts at 0 degrees in front of the listener and increases clockwise.
   * 
   * @param elevationIndex elevation index
   * @param azimuth azimuth
   * @return azimuth index
   */
  private int getAzimuthIndex(int elevationIndex, float azimuth)
  {
    while (azimuth < 0)
      azimuth += 360.0f;
    
    while (azimuth > 360.0f)
      azimuth -= 360.0f;

    int a = 0;
    
    if (!Float.isNaN(azimuth))
      a = nearestNumber(azimuth, HRTFDirections[elevationIndex]);
    
    if (a == -1)
      a = 0;
    
    return a;
  }
  
  /**
   * Get the head-related impulse response of the left ear.
   * 
   * @param elevation elevation angle
   * @param azimuth azimuth
   * @return the head-related impulse response
   */
  float[] getLeftImpulseResponse(float elevation, float azimuth)
  {
    int e = getElevationIndex(elevation);
    int a = getAzimuthIndex(e, azimuth);
    
    return HRIRLeft[e][a];
  }

  /**
   * Get the head-related impulse response of the right ear.
   * 
   * @param elevation elevation angle
   * @param azimuth azimuth
   * @return the head-related impulse response
   */
  float[] getRightImpulseResponse(float elevation, float azimuth)
  {
    int e = getElevationIndex(elevation);
    int a = getAzimuthIndex(e, azimuth);
    
    return HRIRRight[e][a];
  }

  /**
   * Convolve the samples with the head-related impulse response of the left ear.
   * 
   * @param elevation elevation angle
   * @param azimuth azimuth
   * @param samples samples to convolve with the HRIR
   * @return the convolved samples
   */
  float[] convolveWithLeftImpulseResponse(float elevation, float azimuth, float[] samples)
  {
    return convolve(getLeftImpulseResponse(elevation, azimuth), samples);
  }

  /**
   * Convolve the samples with the head-related impulse response of the right ear.
   * 
   * @param elevation elevation angle
   * @param azimuth azimuth
   * @param samples samples to convolve with the HRIR
   * @return the convolved samples
   */
  float[] convolveWithRightImpulseResponse(float elevation, float azimuth, float[] samples)
  {
    return convolve(getRightImpulseResponse(elevation, azimuth), samples);
  }

  /**
   * Convolve two arrays of floats.
   * 
   * @param f the first array of the convolution
   * @param g the second array of the convolution
   * @return the convolution of {@code f} and {@code g}
   */
  float[] convolve(float[] f, float[] g)
  {
    int resultLength = f.length + g.length;
    float[] result = new float[resultLength];

    for (int n = 0; n < resultLength; n++)
    {
      result[n] = 0.0f;

      for (int m = 0; m < f.length; m++)
      {
        if ((n - m) >= g.length)
          continue;

        if ((n - m) < 0)
          break;

        result[n] += f[m] * g[n - m];
      }
    }
    
    return result;
  }

  public boolean acceptsMoreFrames()
  { 
    if (line == null)
      return false;
    
    if (line.available() > frameSize * sampleSizeInBytes * 2)
      return true;

    return false;
  }
  
  public int getFrameSize()
  {
    return frameSize;
  }
  
  private void updateHeadTracking()
  {
    PVector pos = ruis.viewManager.getViewPointLocation();
    setHeadPosition(new Vector3f(pos.x, pos.y, pos.z));
    
    //PVector up = ruis.getViewPointUp();
    PVector up = ruis.getViewPointRotation().getUp();
    setHeadUp(new Vector3f(up.x, up.y, up.z));
    
    //PVector dir = ruis.getViewPointForward();
    PVector dir = ruis.getViewPointRotation().getForward();
    setHeadDirection(new Vector3f(dir.x, dir.y, dir.z));
  }
  
  /**
   * Update the position and direction of the listener based on the tracked skeleton.
   */
  /*private void updateFromSkeleton()
  {
    if (skeleton == null)
      return;

    PVector position = skeleton.getJoint(RuisSkeleton.JOINT_HEAD).getWorldLocation();
    setHeadPosition(new Vector3f(position.x, position.y, position.z));
    
    //PVector up = skeleton.getJoint(RuisSkeleton.JOINT_HEAD).getWorldDirection();
    //setHeadUp(new Vector3f(up.x, up.y, up.z));
    
    PMatrix3D rotation = skeleton.getJoint(RuisSkeleton.JOINT_HEAD).getWorldRotation();
    PVector dir = new PVector();
    rotation.mult(new PVector(1, 0, 0), dir); // is the x unit vector the right choice?????????????????????????????? 
    setHeadDirection(new Vector3f(dir.x, dir.y, dir.z));
    
    rotation.mult(new PVector(0, 1, 0), dir); // is the y unit vector the right choice?????????????????????????????? 
    setHeadUp(new Vector3f(dir.x, dir.y, dir.z));
  }*/

  public void pushFrames(Vector<Frame> frames)
  {
    // make sure the HRTFs loaded successfully before doing anything
    if (!HRTFsLoaded)
      return;
    
    // first update the position and orientation if we are tracking a skeleton
    // updateFromSkeleton();
    
    // update the listener position and orientation based on head tracking data
    if (headTrackingEnabled)
      updateHeadTracking();
    
    float[] data = new float[frameSize * 2 * 2]; // includes the convolution tail that overlaps with the next frame
    
    // add the convolution tail of the previous frame
    for (int i = 0; i < convolutionTail.length; i++)
    {
      data[i] += convolutionTail[i];
    }
    
    for (int i = 0; i < frames.size(); i++)
    {
      Frame frame = frames.elementAt(i);
      
      if (frame.getSamples().length != frameSize) // make sure that the frame size is correct
      {
        System.err.println("Frame of incorrect size pushed to the headphone listener. Skipping this frame.");
        continue;
      }
      
      // get the direction of the source
      // first project the listener->source vector on the plane perpendicular to the up vector
      Vector3f sourceDir = new Vector3f(frame.getPosition());
      sourceDir.sub(position);
      Vector3f temp = new Vector3f();
      temp.cross(sourceDir, up);
      Vector3f projSourceDir = new Vector3f();
      projSourceDir.cross(up, temp);

      float azimuth = direction.angle(projSourceDir); // between 0 and PI
      float elevation = up.angle(sourceDir); // between 0 and PI

      // now we need to find out if the azimuth is between 0 and PI or PI and 2*PI
      Vector3f right = new Vector3f();
      right.cross(up, direction);
      float angleFromRight = right.angle(projSourceDir);

      if (angleFromRight > Math.PI/2) // the source is on the left of the listener
        azimuth = (float) (2 * Math.PI - azimuth);

      // convert to degrees
      azimuth = 180.0f * azimuth/(float)Math.PI;
      elevation = 180.0f * elevation/(float)Math.PI;
      elevation = -(elevation - 90.0f); // change the elevation to the values used in the HRTFs

      // how much to attenuate based on distance
      float a = 1 / (sourceDir.length() * 0.1f); // RUIS coordinates are in cm, set 10 cm as the reference distance
      if (a > 1) // don't amplify (if the distance is less than 10 cm)
        a = 1;
        
      calculateAmplitudePanning(elevation, azimuth, frame.getSamples(), a); // adds entries to the renderings vector
    }
    
    while (renderings.size() > 0)
    {
      float[] sum = new float[HRTFSize];
      
      Rendering rendering = renderings.remove(0);
      
      for (int i = 0; i < HRTFSize; i++)
        sum[i] = rendering.fft[i] * rendering.gain;
      
      // sum all renderings in the same direction, to reduce the number of multiplications needed
      for (int i = renderings.size() - 1; i >= 0; i--)
      {
        if (renderings.get(i).elevationIndex == rendering.elevationIndex && renderings.get(i).azimuthIndex == rendering.azimuthIndex)
        {
          Rendering otherRendering = renderings.remove(i);
          
          for (int j = 0; j < HRTFSize; j++)
            sum[j] += otherRendering.fft[j] * otherRendering.gain;
        }
      }
      
      // multiply with the transfer function
      float[] left = multiplyFFT(sum, HRTFLeft[rendering.elevationIndex][rendering.azimuthIndex]);
      float[] right = multiplyFFT(sum, HRTFRight[rendering.elevationIndex][rendering.azimuthIndex]);

      // do the inverse FFT
      FloatFFT_1D fft = new FloatFFT_1D(HRTFSize);
      fft.realInverse(left, true);
      fft.realInverse(right, true);
      
      // interleave and add the samples to the mix
      for (int i = 0; i < left.length; i++)
      {
        data[i * 2] += left[i];
        data[i * 2 + 1] += right[i];
      }
    }
    
    // copy the convolution tail
    convolutionTail = Arrays.copyOfRange(data, frameSize * 2, data.length);
    
    // convert to 16 bit signed big-endian byte-order PCM
    byte[] stereoBytes = new byte[2 * 2 * frameSize]; // stereo, 2 bytes per sample
    ByteBuffer byteBuffer = ByteBuffer.wrap(stereoBytes);
    byteBuffer.order(ByteOrder.BIG_ENDIAN);
    ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
    
    for (int i = 0; i < frameSize * 2; i++)
    {
      shortBuffer.put(i, (short) (data[i] * Short.MAX_VALUE));
    }

    // write the data to the SourceDataLine
    line.write(stereoBytes, 0, stereoBytes.length);
  }
  
  /**
   * Multiply two Fourier transforms.
   * 
   * @param f1 the first factor
   * @param f2 the second factor
   * @return the product
   */
  float[] multiplyFFT(float[] f1, float[] f2)
  {
    // The jTransforms FFT layout looks like this:
    // a[2*k] = Re[k], 0<=k<n/2
    // a[2*k+1] = Im[k], 0<k<n/2
    // a[1] = Re[n/2]

    float[] result = new float[f1.length]; // two times the length isn't needed for the multiplication, but later for the inverse transform

    // the imaginary part of the FFT (and thus the multiplication result) is 0 for indices 0 and 1
    result[0] = f1[0] * f2[0];
    result[1] = f1[1] * f2[1];

    for (int i = 1; i < f1.length/2; i++)
    {
      // multiplication of complex numbers:
      // (a + bi)(c + di) = (ac - bd) + (bc + ad)i

      result[i*2] = f1[i*2] * f2[i*2] - f1[i*2 + 1] * f2[i*2 + 1]; // the real part
      result[i*2 + 1] = f1[i*2 + 1] * f2[i*2] + f1[i*2] * f2[i*2 + 1]; // the imaginary part
    }
    
    return result;
  }
}
