// https://searchcode.com/api/result/102638082/

package nrgrobot;

import edu.wpi.first.wpilibj.*;

/**
 * DrivePID.java provides code to make the robot drive straight by using the encoders on the motors.
 * @author PaulD
 */
public class DrivePID
{
    private double m_P;                     // factor for "proportional" control
    private double m_I;                     // factor for "integral" control
    //private double m_D;                   // factor for "derivative" control
    private double m_maximumOutput = 1.0;   // motor maximum output
    private double m_minimumOutput = -1.0;  // motor minimum output
    private boolean m_enabled = false;      // is the pid controller currently enabled?
    //private double m_prevError = 0.0;     // the prior sensor input (used to compute velocity)
    private double m_error = 0.0;           // the current error term
    private double m_prevError = 0.0;	    // the prior error (used to compute velocity)
    private double m_totalError = 0.0;      // the sum of the errors for use in the integral calc
    private double m_tolerance = 0.05;      // the percetage error that is considered on target
    private double m_result = 0.0;          // the sum of the P+I+D terms
    private int prevEncL = 0;               // value of the left track encoder from the prev iteration
    private int prevEncR = 0;               // value of the right track encoder from the prev iteration
    private Encoder leftEncoder;            // encoder to read for the left track
    private Encoder rightEncoder;           // encoder to read for the right track
    private NRGDrive drive;
    //public final boolean ENCODER_SIMULATING = false; // TODO: turn this off later
    //private int encoderBiasL;       // TODO: testing only, remove this later
    //private int encoderBiasR;       // TODO: testing only, remove this later
    private long prevTime;           // TODO: testing only, remove this later

    private InputAverager leftAverager; // Smooth out the PID input values
    private InputAverager rightAverager;

    private static final double MINIMUM_MOTOR_SPEED = .15;  // Min PWM value that actually moves our robot

    /**
     * Allocate a PID object with the given constants for P, I, D
     * @param driveNRG a reference to the main NRGDrive object
     * @param Kp the proportional coefficient
     * @param Ki the integral coefficient
     * @param Kd the derivative coefficient -- not used
     * @param encL the reference to the encoder on the left drive track
     * @param encL the reference to the encoder on the right drive track
     */
    public DrivePID (NRGDrive driveNRG, double Kp, double Ki, Encoder encL, Encoder encR)
    {
        drive = driveNRG;
        setPID(Kp, Ki);
        encL.reset();
        encR.reset();
        leftEncoder = encL;
        rightEncoder = encR;
        leftAverager = new InputAverager(DriveSettings.DRIVE_INPUT_AVERAGER_PERIOD);
        rightAverager = new InputAverager(DriveSettings.DRIVE_INPUT_AVERAGER_PERIOD);
        prevTime = System.currentTimeMillis();
    }

    public void initForTeleop()
    {
        // Cut down # of averaging periods during teleop to reduce driver response lag
        leftAverager = new InputAverager((DriveSettings.DRIVE_INPUT_AVERAGER_PERIOD+1)/2);
        rightAverager = new InputAverager((DriveSettings.DRIVE_INPUT_AVERAGER_PERIOD+1)/2);
    }
    /**
     * Take the raw tank drive left & right joystick inputs, then calculate the outputs with PID corrections,
     * and write to the left & right motor outputs.
     * Note: we don't use the 'D' term of general PID algorithm
     * @param joyL - left joystick/motorSpeed setting (-1.0 to 1.0)
     * @param joyR - right joystick/motorSpeed seeting (-1.0 to 1.0)
     */
    public void driveStraight(double speed, double desiredHeading)
    {
        String sts;

        double joyL = MathHelper.clamp(speed, -1.0, 1.0);
        double joyR = joyL;
        double joyLout, joyRout;
        leftAverager.addValue(joyL);
        rightAverager.addValue(joyR);
        double joyLAvg = leftAverager.getAverageValue();
        double joyRAvg = rightAverager.getAverageValue();
        double currentHeading = Sensors.gyro.getAngle();

        if (Debug.DRIVEPID)
        {
            long curTime = System.currentTimeMillis();
            sts = (curTime-prevTime) + "ms " + "Lav:" + MathHelper.round2(joyLAvg) + " Rav:" + MathHelper.round2(joyRAvg) + "  ";
            Debug.print(Debug.DRIVEPID, sts);
            prevTime = curTime;
        }
        if (speed == 0)
        {
            m_prevError = 0.0;
            m_result = 0.0;
        }
        else
        {   // Generic PID calculation is: error = setpoint - input;
            m_error = MathHelper.convertToSmallestRelativeAngle(desiredHeading - currentHeading);
            if (((m_totalError + m_error) * m_I < m_maximumOutput) &&
                ((m_totalError + m_error) * m_I > m_minimumOutput))
            {
                m_totalError += m_error;
            }
            final double Kp = 0.02;   //TODO: move these constants to DriveSettings
            final double Ki = 0.005;
            final double Kd = 0.001;
            m_result = Kp * m_error + Ki * m_totalError + Kd * (m_error - m_prevError);
            m_prevError = m_error;
        }

        if (Debug.DRIVEPID)
        {
            sts = " e" + MathHelper.round2(m_error)
                + " t" + MathHelper.round2(m_totalError)
                + " r" + MathHelper.round2(m_result) + "  ";
            LCD.println3(sts);
            Debug.print(Debug.DRIVEPID, sts);
        }

        joyRout = joyRAvg;
        joyLout = joyLAvg + m_result;
        if (joyLout > m_maximumOutput || joyLout < m_minimumOutput)
        {
            // If we're trying to drive the left motor beyond its limits, then scale back the right motor instead
            joyLout = joyLAvg;
            joyRout = joyRAvg - m_result;
        }
        // Adjust final output to account for motors being mounted in opposite directions
        joyLout *= JoystickSettings.LEFT_DRIVE_MULTIPLIER;
        joyRout *= JoystickSettings.RIGHT_DRIVE_MULTIPLIER;

        // Note: the framework drive methods clamp their arguments between -1.0 and 1.0 for us
        if (NRGDrive.getReversed())
        {
            drive.setLeftRightMotorOutputs(-joyRout, -joyLout);
            //drive.tankDrive(-joyRout, -joyLout);
            sts = "LM: " + MathHelper.round2(-joyRout) + "  RM: " + MathHelper.round2(-joyLout);
        } else {
            drive.setLeftRightMotorOutputs(joyLout, joyRout);
            //drive.tankDrive(joyLout, joyRout);
            sts = "LM: " + MathHelper.round2(joyLout) + "  RM: " + MathHelper.round2(joyRout);
        }
        //LCD.println5(sts);
        Debug.println(Debug.DRIVEPID, sts);
    }

    // Remaps joystick drive values so we get linear robot speed response from the motors by
    // removing most of the deadzone (ie, when the motor PWM value is too small to move the bot)
    private double linearizeMotorSpeed(double joySpeed)
    {
        double speed = Math.abs(joySpeed);
        // Maintain a small deadzone so we can always come to a complete stop
        if(speed < JoystickSettings.JOY_DEADZONE_THRESHOLD)
            return 0.0;
        double scaleFactor = (1 - MINIMUM_MOTOR_SPEED) / (1 - JoystickSettings.JOY_DEADZONE_THRESHOLD);
        speed = (speed - JoystickSettings.JOY_DEADZONE_THRESHOLD) * scaleFactor + MINIMUM_MOTOR_SPEED;
        return (joySpeed > 0) ? speed : -speed;
    }

    public void calculate(double joyL, double joyR)
    {
        calculate(linearizeMotorSpeed(joyL), linearizeMotorSpeed(joyR), DriveSettings.SQUARE_JOYSTICK_INPUTS);
    }

    /**
     * Take the tank drive left & right joystick inputs, then calculate the outputs with PID corrections,
     * and write to the left & right motor outputs.
     * Note: we don't use the 'D' term of general PID algorithm
     * @param joyL - left joystick/motorSpeed setting (-1.0 to 1.0)
     * @param joyR - right joystick/motorSpeed seeting (-1.0 to 1.0)
     * @param squareInputs - L,R joystick settings are squared if this is true to increase human control.
     *        Note that autonomous routines should always set this to false.
     */
    public void calculate(double joyL, double joyR, boolean squareInputs)
    {
        double joyLout, joyRout;
        leftAverager.addValue(joyL);
        rightAverager.addValue(joyR);
        double joyLAvg = leftAverager.getAverageValue();
        double joyRAvg = rightAverager.getAverageValue();

        // Square the inputs (while preserving the sign) to increase fine control while permitting full power
        if (squareInputs)
        {
            joyLAvg *= Math.abs(joyLAvg);
            joyRAvg *= Math.abs(joyRAvg);
        }

        String sts = "L:" + MathHelper.round2(joyL) + " R:" + MathHelper.round2(joyR) + "  ";
        sts += "Lav:" + MathHelper.round2(joyLAvg) + " Rav:" + MathHelper.round2(joyRAvg) + "  ";
        long curTime = System.currentTimeMillis();
        Debug.print(Debug.DRIVEPID, (curTime-prevTime) + "ms " + sts);
        prevTime = curTime;

        if (!m_enabled)
        {
            // Just pass the joystick values through "as is"
            joyLout = joyLAvg;
            joyRout = joyRAvg;
            // TODO: delete encoder print lines below after testing
            int encL = leftEncoder.getRaw();        // TODO: should we be using raw values?
            int encR = -rightEncoder.getRaw();
            Debug.print(Debug.DRIVEPID, "RawEL:" + encL + " RawER:" + encR + "   ");
        }
        else
        {
            int encL = leftEncoder.getRaw();        // TODO: should we be using raw values?
            int encR = -rightEncoder.getRaw();
            Debug.print(Debug.DRIVEPID, "RawEL:" + encL + " RawER:" + encR + "   ");
            int deltaEncL = encL - prevEncL;
            int deltaEncR = encR - prevEncR;
            if (drive.getReversed())
            {
                // Reverse and negate encoder deltas if robot drive is in reverse mode
                int tmp = deltaEncL;
                deltaEncL = -deltaEncR;
                deltaEncR = -tmp;
            }
            prevEncL = encL;
            prevEncR = encR;

            /* // TODO: remove this if statement after testing...
            if (ENCODER_SIMULATING)
            {
                deltaEncL = (int)(joyLAvg * encoderBiasL);
                deltaEncR = (int)(joyRAvg * encoderBiasR);
            } */

            // Don't try to do PID adjustments if we're only driving one side of the robot.
            // Also avoid divide by zero cases (deltaEncR==0) or cases where we're reversing
            // direction and the 'slop' in the drive train lets the joy input be positive at
            // the same time the deltaEnc is negative (or vice versa).
            if (joyL == 0 || joyR == 0 || deltaEncL*joyLAvg <= 0.0 || deltaEncR*joyRAvg <= 0.0)
            {
                m_error = 0.0;
                m_totalError = 0.0;
            } 
            else
            {   // Generic PID calculation is: error = setpoint - input;
                m_error = joyLAvg - (joyRAvg * deltaEncL) / deltaEncR;  // Treat joyL as the setpoint
                if (((m_totalError + m_error) * m_I < m_maximumOutput) &&
                    ((m_totalError + m_error) * m_I > m_minimumOutput))
                {
                    m_totalError += m_error;
                }
            }

            m_result = m_P * m_error + m_I * m_totalError;

            sts = deltaEncL + "," + deltaEncR
                    + " e" + MathHelper.round2(m_error)
                    + " r" + MathHelper.round2(m_result) + "   ";
            LCD.println3(sts);
            Debug.print(Debug.DRIVEPID, sts);

            joyRout = joyRAvg;
            joyLout = joyLAvg + m_result;
            // Make sure (because of 'slop' in the drive system at low speeds) that the PID
            // correction isn't so large that it reverses the pos/neg sign of the joyL value.
            if ((joyLout * joyLAvg) < 0.0)  // true iff joyLout & joyLavg have opposite signs
                joyLout = 0.0;

            if (joyLout > m_maximumOutput)
            {
                // If we're trying to drive the left motor above its max, then scale back both motors
                joyRout *= m_maximumOutput / joyLout;
                joyLout = m_maximumOutput;
            }
            else if (joyLout < m_minimumOutput)
            {
                // If we're trying to drive the left motor below its min, then scale back both motors
                joyRout *= m_minimumOutput / joyLout;
                joyLout = m_minimumOutput;
            }
            /*
            if (ENCODER_SIMULATING)      // TODO: remove this if after testing...
            {
                encoderBiasL *= joyLout / joyLAvg;
                encoderBiasR *= joyRout / joyRAvg;
            } */
        }
        // Adjust final output to account for motors being mounted in opposite directions
        joyLout *= JoystickSettings.LEFT_DRIVE_MULTIPLIER;
        joyRout *= JoystickSettings.RIGHT_DRIVE_MULTIPLIER;

        // Note: the framework drive methods clamp their arguments between -1.0 and 1.0 for us
        if (NRGDrive.getReversed())
        {
            drive.setLeftRightMotorOutputs(-joyRout, -joyLout);
            //drive.tankDrive(-joyRout, -joyLout);
            sts = "LM: " + MathHelper.round2(-joyRout) + "  RM: " + MathHelper.round2(-joyLout);
        } else {
            drive.setLeftRightMotorOutputs(joyLout, joyRout);
            //drive.tankDrive(joyLout, joyRout);
            sts = "LM: " + MathHelper.round2(joyLout) + "  RM: " + MathHelper.round2(joyRout);
        }
        //LCD.println5(sts);
        Debug.println(Debug.DRIVEPID, sts);
    }

    /**
     * Set the DrivePID Controller gain parameters (proportional, integral, and differential coefficients).
     * @param p Proportional coefficient
     * @param i Integral coefficient
     * @param d Differential coefficient -- not used
     */
    public void setPID(double p, double i)
    {
        m_P = p;
        m_I = i;
    }

    /**
     * Set the percentage error which is considered tolerable for use with
     * OnTarget. (Input of 15.0 = 15 percent)
     * @param percent error which is tolerable
     */
    public void setTolerance(double percent)
    {
        m_tolerance = percent;
    }

    /**
     * Return true if the error is within the percentage of the total input range,
     * determined by setTolerance.
     * @return true if the error is less than the tolerance
     */
    public boolean onTarget()
    {
        return (Math.abs(m_error) < m_tolerance / 100);
    }

    /**
     * Begin computing the DrivePID corrections
     */
    public void enable()
    {
        m_enabled = true;
    }

    /**
     * Stop computing the DrivePID corrections
     */
    public void disable()
    {
        m_enabled = false;
    }

    /**
     * Gets whether the DrivePID is running.
     * @return true only if the DrivePID is currently enabled.
     */
    public boolean isEnabled()
    {
        return m_enabled;
    }

    public void stop()
    {
        final double EPSILON = 1E-6;
        while (Math.abs(leftAverager.getAverageValue()) > EPSILON)
            leftAverager.addValue(0.0);
        while (Math.abs(rightAverager.getAverageValue()) > EPSILON)
            rightAverager.addValue(0.0);
        this.calculate(0.0, 0.0, false);
        Debug.println(Debug.DRIVEPID, "DrivePID Stop Reset Averagers...");
    }

    /**
     * Reset the previous error,, the integral term, and disable the controller.
     */
    public void reset()
    {
        disable();
        m_prevError = 0;
        m_totalError = 0.0;
        prevEncL = leftEncoder.getRaw();        // TODO: should we be using raw values?
        prevEncR = -rightEncoder.getRaw();

        /* //TODO: remove encode simulation code after testing PID?
        if (ENCODER_SIMULATING)
        {
            encoderBiasL = 1000;
            encoderBiasR = 1000 + (int)(100.0 * JoystickHelper.getAnalogValue(JoystickSettings.LEFT_DRIVE_JOY, Joystick.AxisType.kZ));
        } */
    }
}

