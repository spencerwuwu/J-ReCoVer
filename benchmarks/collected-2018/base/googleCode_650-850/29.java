// https://searchcode.com/api/result/88323/

package com.grt192.actuator;

import com.grt192.actuator.exception.GRTCANJaguarException;
import com.grt192.core.Actuator;
import com.grt192.core.Command;
import com.grt192.event.component.CANTimeoutListener;
import com.grt192.sensor.canjaguar.GRTJagEncoder;
import com.grt192.sensor.canjaguar.GRTJagFaultSensor;
import com.grt192.sensor.canjaguar.GRTJagPotentiometer;
import com.grt192.sensor.canjaguar.GRTJagPowerSensor;
import com.grt192.sensor.canjaguar.GRTJagSwitchPair;

import edu.wpi.first.wpilibj.can.CANTimeoutException;
import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.CANJaguar.ControlMode;
import edu.wpi.first.wpilibj.PIDOutput;
import java.util.Vector;

/**
 * A GRTCANJaguar is a driver for a Jaguar speed-controller using the
 * CANbus(Controller Area Network) interface.
 * The CANBus (v. 2.0B) is optimized for  electromagnetically noisy environments,
 * and features at 1M bits/s bit rate, and allows access to Jaguar current,
 * voltage, speed, fault, switch, and other parameters.
 * @author ajc
 */
public class GRTCANJaguar extends Actuator implements PIDOutput {
    // Control Modes

    public static final int PERCENT_CONTROL = 1;
    public static final int SPEED_CONTROL = 2;
    public static final int POSITION_CONTROL = 3;
    public static final int CURRENT_CONTROL = 4;
    public static final int VOLTAGE_CONTROL = 5;
    // Position Sensors
    public static final int POTENTIOMETER = 0;
    public static final int ENCODER = 1;
    //Speed Sensors
    public static final int INV_ENCODER = 2;
    public static final int QUAD_ENCODER = 3;
    // Neutral Modes
    public static final int COAST = 0;
    public static final int BRAKE = 1;
    public static final int JUMPER = 2;
    public static final int ERROR = -999;
    private CANJaguar jaguar;
    //sensors
    private GRTJagEncoder encoder;
    private GRTJagPotentiometer potentiometer;
    private GRTJagPowerSensor powerSensor;
    private GRTJagSwitchPair switches;
    private GRTJagFaultSensor faultSensor;
    //CANTimeoutListener list
    private Vector listeners;

    /**
     * Constructs  GRTCANJaguar on a channel and in a default control mode of 0
     * @param channel
     */
    public GRTCANJaguar(int channel) {
        this(channel, 0);
    }

    /**
     * Constructs GRTCANJaguar at a certain channel and using a specified control
     * @param channel
     * @param controlMode
     */
    public GRTCANJaguar(int channel, int controlMode) {
        listeners = new Vector();
        try {
            switch (controlMode) {
                case PERCENT_CONTROL:
                    jaguar = new CANJaguar(channel,
                            CANJaguar.ControlMode.kPercentVbus);
                    break;
                case SPEED_CONTROL:
                    jaguar = new CANJaguar(channel, CANJaguar.ControlMode.kSpeed);
                    break;
                case POSITION_CONTROL:
                    jaguar = new CANJaguar(channel, CANJaguar.ControlMode.kPosition);
                    break;
                case CURRENT_CONTROL:
                    jaguar = new CANJaguar(channel, CANJaguar.ControlMode.kCurrent);
                    break;
                case VOLTAGE_CONTROL:
                    jaguar = new CANJaguar(channel, CANJaguar.ControlMode.kVoltage);
                default:
                    jaguar = new CANJaguar(channel);
            }
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Changes the control mode, or how the jaguar responds to commands
     * @param controlMode
     */
    public void changeControlMode(int controlMode) {
        try {
            switch (controlMode) {
                case PERCENT_CONTROL:
                    jaguar.changeControlMode(CANJaguar.ControlMode.kPercentVbus);
                    break;
                case SPEED_CONTROL:
                    jaguar.changeControlMode(CANJaguar.ControlMode.kSpeed);
                    break;
                case POSITION_CONTROL:
                    jaguar.changeControlMode(CANJaguar.ControlMode.kPosition);
                    break;
                case CURRENT_CONTROL:
                    jaguar.changeControlMode(CANJaguar.ControlMode.kCurrent);
                    break;
                case VOLTAGE_CONTROL:
                    jaguar.changeControlMode(CANJaguar.ControlMode.kVoltage);
                default:
                    jaguar.changeControlMode(CANJaguar.ControlMode.kPercentVbus);
            }
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     *Sets the encoder or potentiometer used to gauge position
     * @param sensor
     */
    public void setPositionSensor(int sensor) {
        try {
            if (sensor == ENCODER || sensor == QUAD_ENCODER) {
                jaguar.setPositionReference(CANJaguar.PositionReference.kQuadEncoder);
            } else if (sensor == POTENTIOMETER) {
                jaguar.setPositionReference(CANJaguar.PositionReference.kPotentiometer);
            } else {
                jaguar.setPositionReference(CANJaguar.PositionReference.kNone);
            }
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Sets the position sensor used to gauge speed.
     * This is distinct from <code>setPositionSensor()</code>.
     * @param sensor
     */
    public void setSpeedSensor(int sensor) {
        try {
            if (sensor == QUAD_ENCODER) {
                jaguar.setSpeedReference(CANJaguar.SpeedReference.kQuadEncoder);
            } else if (sensor == ENCODER) {
                jaguar.setSpeedReference(CANJaguar.SpeedReference.kEncoder);
            } else if (sensor == INV_ENCODER) {
                jaguar.setSpeedReference(CANJaguar.SpeedReference.kInvEncoder);
            } else {
                jaguar.setSpeedReference(CANJaguar.SpeedReference.kNone);
//                jaguar.setSpeedReference(CANJaguar.SpeedReference.kInvEncoder.kEncoder.);
            }
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Sets PID gains
     * @param p the proportional gain for closed loop control
     * @param i the integral gain for closed loop control
     * @param d the derivative gain for closed loop control
     */
    public void setPID(double p, double i, double d) {
        try {
            jaguar.setPID(p, i, d);
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Sets the proportional gain for closed loop control
     * @param p
     */
    public void setP(double p) {
        try {
            jaguar.setPID(p, jaguar.getI(), jaguar.getD());
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Sets the integral gain for closed loop control
     * @param i
     */
    public void setI(double i) {
        try {
            jaguar.setPID(jaguar.getP(), i, jaguar.getD());
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Sets the derivative gain for closed loop control
     * @param d
     */
    public void setD(double d) {
        try {
            jaguar.setPID(jaguar.getP(), jaguar.getI(), d);
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Gets the value of the proportional gain for closed loop control
     * @return
     */
    public double getP() {
        try {
            return jaguar.getP();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return ERROR;
    }

    /**
     * Gets the value of the integral gain for closed loop control
     * @return
     */
    public double getI() {
        try {
            return jaguar.getI();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return ERROR;
    }

    /**
     * Gets the value of the derivative gain for closed loop control
     * @return
     */
    public double getD() {
        try {
            return jaguar.getD();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return ERROR;
    }

    /**
     * Enable closed loop control, using sensors attached to the control IO on
     * The CANJaguar.
     * 
     * Closed loop control uses feedback from either position, speed, or current sensors,
     * and runs a PID loop on its onboard CANJag processsor in order to automatically
     * vary the voltage in order to reach setpoints in position, speed, or current.
     * The PID Gains must be set with <code>setPID()</code> or <code>setP()</code>, etc.
     *
     * The corresponding sensor must be activated with <code>setPositionSensor</code>
     * or <code>setSpeedSensor</code> for position and speed control.
     */
    public void enableClosedLoop() {
        enableClosedLoop(0.0);
    }

    /**
     * Enable closed loop control, using sensors attached to the control IO on
     * The CANJaguar.
     *
     * Closed loop control uses feedback from either position, speed, or current sensors,
     * and runs a PID loop on its onboard CANJag processsor in order to automatically
     * vary the voltage in order to reach setpoints in position, speed, or current.
     * The PID Gains must be set with <code>setPID()</code> or <code>setP()</code>, etc.
     *
     * The corresponding sensor must be activated with <code>setPositionSensor</code>
     * or <code>setSpeedSensor</code> for position and speed control.
     * @param initialPosition initial setpoint
     */
    public void enableClosedLoop(double initialPosition) {
        try {
            jaguar.enableControl(initialPosition);
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Disables closed loop control
     */
    public void disableClosedLoop() {
        try {
            jaguar.disableControl();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Returns the input voltage of the jaguar speed controller
     * @return
     */
    public double getInputVoltage() {
        try {
            return jaguar.getBusVoltage();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return ERROR;
    }

    /**
     * Return the output voltage of the jaguar
     * @return
     */
    public double getOutputVoltage() {
        try {
            return jaguar.getOutputVoltage();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return ERROR;
    }

    /**
     * Returns the output current of the jaguar
     * @return
     */
    public double getOutputCurrent() {
        try {
            return jaguar.getOutputCurrent();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return ERROR;
    }

    /**
     * Returns the temperature in the Jaguar
     * @return
     */
    public double getTemperature() {
        try {
            return jaguar.getTemperature();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return ERROR;
    }

    /**
     * Gets the position of the attached and configured encoder or potentiometer.
     * Configuration is done with <code>setPositionSensor()</code>
     * @return
     */
    public double getPosition() {
        try {
            return jaguar.getPosition();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return ERROR;
    }

    /**
     * Gets the speed of the attached and configured encoder or potentiometer.
     * Configuration is done with <code>setSpeedSensor()</code>
     * @return
     */
    public double getSpeed() {
        try {
            return jaguar.getSpeed();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return ERROR;
    }

    /**
     * The motor can turn forward if true
     * @return
     */
    public boolean getLeftLimitStatus() {
        try {
            return jaguar.getForwardLimitOK();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return false;
    }

    /**
     * The motor can turn backward if true
     * @return
     */
    public boolean getRightLimitStatus() {
        try {
            return jaguar.getReverseLimitOK();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return false;
    }

    /**
     * Gets a bit mask of faults. Bits are in <code>CANJaguar.Faults</code>
     * @return fault-bit-mask
     */
    public short getFaults() {

        try {
            return jaguar.getFaults();
        } catch (CANTimeoutException ex) {
            notifyCANTimeout();
        }
        return 0;
    }

    /**
     * Sets the maximum voltage change rate.
     *
     * When in percent voltage output mode, the rate at which the voltage changes can
     * be limited to reduce current spikes.  Set this to 0.0 to disable rate limiting.
     *
     * @param rate
     */
    public void setVoltageRampRate(double rate) {
        try {
            jaguar.setVoltageRampRate(rate);
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Sets the encoder resolution
     * @param countsPerRev
     */
    public void setEncoderResolution(int countsPerRev) {
        try {
            jaguar.configEncoderCodesPerRev(countsPerRev);
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Configure Soft Position Limits when in Position Controller mode.
     *
     * When controlling position, you can add additional limits on top of the limit switch inputs
     * that are based on the position feedback.  If the position limit is reached or the
     * switch is opened, that direction will be disabled.
     *
     *
     * @param leftLimit
     * @param rightLimit
     */
    public void setSoftLimits(double leftLimit, double rightLimit) {
        try {
            jaguar.configSoftPositionLimits(leftLimit, rightLimit);
        } catch (CANTimeoutException e) {
            notifyCANTimeout();

        }
    }

    /**
     * Disable soft position limits for position control mode
     */
    public void disableSoftLimits() {
        try {
            jaguar.disableSoftPositionLimits();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Configures what the controller does to the H-Bridge when neutral (not driving the output).
     *
     * This allows you to override the jumper configuration for brake or coast.
     *
     * @param mode
     */
    public void setNeutralMode(int mode) {
        try {
            switch (mode) {
                case COAST:
                    jaguar.configNeutralMode(CANJaguar.NeutralMode.kCoast);
                    break;
                case BRAKE:
                    jaguar.configNeutralMode(CANJaguar.NeutralMode.kBrake);
                    break;
                case JUMPER:
                    jaguar.configNeutralMode(CANJaguar.NeutralMode.kJumper);
                    break;
            }
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Configures the number of turns on the potentiometer.
     *
     * There is no special support for continuous turn potentiometers.
     * Only integer numbers of turns are supported.
     *
     * @param turns
     */
    public void setPotentiometerTurns(int turns) {
        try {
            jaguar.configPotentiometerTurns(turns);
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Gets the recently set outputValue setpoint.
     *
     * In PercentVoltage Mode, the outputValue is in the range -1.0 to 1.0
     *
     * @return
     */
    public double getLastCommand() {
        try {
            return jaguar.getX();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
        return ERROR;
    }

    /**
     * Behavior is a function of the control mode.
     *
     * In Percent control, the command must have a value between -1 and 1,
     * -1 being full reverse and 1 being full forward voltage.
     *
     * In Current control, the command is interpreted as a current setpoint(amps).
     *
     * In Speed control, the command is interpreted as a speed setpoint(rpm).
     *
     * In Position control, the command is interpreted as a position setpoint.
     *
     * In Voltage control, the command is interpreted as a raw voltage to output.
     * @param c
     * @throws GRTCANJaguarException
     */
    protected void executeCommand(Command c) {
        try {
            double value = c.getValue();
            jaguar.setX(value);
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * Halts the jaguar by stopping it completely.
     */
    protected void halt() {
        try {
            jaguar.setX(0);
            disableClosedLoop();
        } catch (CANTimeoutException e) {
            notifyCANTimeout();
        }
    }

    /**
     * @deprecated
     * @param output
     */
    public void pidWrite(double output) {
        jaguar.pidWrite(output);
    }

    /**
     * Gets the encoder wired to the control IO on this CANJaguar.
     * @return
     */
    synchronized public GRTJagEncoder getEncoder() {
        if (encoder == null) {
            encoder = new GRTJagEncoder(this, 25, "Encoder" + this);
            encoder.start();
        }
        return encoder;
    }

    /**
     * Gets the p wired to the control IO on this CANJaguar.
     * @return
     */
    synchronized public GRTJagPotentiometer getPotentiometer() {
        if (potentiometer == null) {
            potentiometer =
                    new GRTJagPotentiometer(this, 25, "Potentiometer" + this);
            potentiometer.start();
        }
        return potentiometer;
    }

    /**
     * Gets a power sensor associated with this CANJaguar
     * @return
     */
    synchronized public GRTJagPowerSensor getPowerSensor() {
        if (powerSensor == null) {
            powerSensor = new GRTJagPowerSensor(this, 50, "PowerSensor" + this);
            powerSensor.start();
        }
        return powerSensor;
    }

    /**
     * Returns a switch-pair sensor.
     * @return
     */
    synchronized public GRTJagSwitchPair getSwitches() {
        if (switches == null) {
            switches = new GRTJagSwitchPair(this, 5, "Switch" + this);
            switches.start();
        }
        return switches;
    }

    /**
     * Gets a fault sensor, which polls the Jaguar for faults
     * @return a fault sensor
     */
    synchronized public GRTJagFaultSensor getFaultSensor() {
        if (faultSensor == null) {
            faultSensor = new GRTJagFaultSensor(this, 50, "FaultSensor" + this);
            faultSensor.start();
        }
        return faultSensor;

    }

    /** Starts notifying <code>CANTimeoutListener</code> l for all CANTimeouts **/
    public void addCANTimeoutListener(CANTimeoutListener l) {
        listeners.addElement(l);
    }

    /** Stops notifying <code>CANTimeoutListener</code> l for all CANTimeouts **/
    public void removeCANTimeoutListener(CANTimeoutListener l) {
        listeners.removeElement(l);
    }

    /** Notifies all CANTimeoutListeners that a CANTimeoutException has occurred **/
    public void notifyCANTimeout() {
        GRTCANJaguarException ex = new GRTCANJaguarException(GRTCANJaguarException.CAN_TIMEOUT, this);
        for (int i = 0; i < listeners.size(); i++) {
            ((CANTimeoutListener) listeners.elementAt(i)).CANTimedOut(ex);
        }
    }
}

