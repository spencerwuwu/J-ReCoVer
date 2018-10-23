// https://searchcode.com/api/result/5217714/

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server;

import com.android.internal.app.IBatteryStats;
import com.android.internal.app.ShutdownThread;
import com.android.server.am.BatteryStatsService;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.LocalPowerManager;
import android.os.Power;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.WorkSource;
import android.os.SystemProperties;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.view.WindowManagerPolicy;
import static android.provider.Settings.System.DIM_SCREEN;
import static android.provider.Settings.System.ELECTRON_BEAM_ANIMATION_ON;
import static android.provider.Settings.System.ELECTRON_BEAM_ANIMATION_OFF;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import static android.provider.Settings.System.STAY_ON_WHILE_PLUGGED_IN;
import static android.provider.Settings.System.WINDOW_ANIMATION_SCALE;
import static android.provider.Settings.System.TRANSITION_ANIMATION_SCALE;

import com.android.internal.app.ThemeUtils;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

public class PowerManagerService extends IPowerManager.Stub
        implements LocalPowerManager, Watchdog.Monitor {

    private static final String TAG = "PowerManagerService";
    private static final String TAGF = "LightFilter";
    static final String PARTIAL_NAME = "PowerManagerService";

    static final boolean DEBUG_SCREEN_ON = false;

    private static final boolean LOG_PARTIAL_WL = false;

    // Indicates whether touch-down cycles should be logged as part of the
    // LOG_POWER_SCREEN_STATE log events
    private static final boolean LOG_TOUCH_DOWNS = true;

    private static final int LOCK_MASK = PowerManager.PARTIAL_WAKE_LOCK
                                        | PowerManager.SCREEN_DIM_WAKE_LOCK
                                        | PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                        | PowerManager.FULL_WAKE_LOCK
                                        | PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK;

    //                       time since last state:               time since last event:
    // The short keylight delay comes from secure settings; this is the default.
    private static final int SHORT_KEYLIGHT_DELAY_DEFAULT = 6000; // t+6 sec
    private static final int MEDIUM_KEYLIGHT_DELAY = 15000;       // t+15 sec
    private static final int LONG_KEYLIGHT_DELAY = 6000;        // t+6 sec
    private static final int LONG_DIM_TIME = 7000;              // t+N-5 sec

    // How long to wait to debounce light sensor changes in milliseconds
    private static final int LIGHT_SENSOR_DELAY = 2000;

    // light sensor events rate in microseconds
    private static final int LIGHT_SENSOR_RATE = 1000000;

    // For debouncing the proximity sensor in milliseconds
    private static final int PROXIMITY_SENSOR_DELAY = 1000;

    // trigger proximity if distance is less than 5 cm
    private static final float PROXIMITY_THRESHOLD = 5.0f;

    // Cached secure settings; see updateSettingsValues()
    private int mShortKeylightDelay = SHORT_KEYLIGHT_DELAY_DEFAULT;

    // Default timeout for screen off, if not found in settings database = 15 seconds.
    private static final int DEFAULT_SCREEN_OFF_TIMEOUT = 15000;

    // flags for setPowerState
    private static final int SCREEN_ON_BIT          = 0x00000001;
    private static final int SCREEN_BRIGHT_BIT      = 0x00000002;
    private static final int BUTTON_BRIGHT_BIT      = 0x00000004;
    private static final int KEYBOARD_BRIGHT_BIT    = 0x00000008;
    private static final int BATTERY_LOW_BIT        = 0x00000010;

    // values for setPowerState

    // SCREEN_OFF == everything off
    private static final int SCREEN_OFF         = 0x00000000;

    // SCREEN_DIM == screen on, screen backlight dim
    private static final int SCREEN_DIM         = SCREEN_ON_BIT;

    // SCREEN_BRIGHT == screen on, screen backlight bright
    private static final int SCREEN_BRIGHT      = SCREEN_ON_BIT | SCREEN_BRIGHT_BIT;

    // SCREEN_BUTTON_BRIGHT == screen on, screen and button backlights bright
    private static final int SCREEN_BUTTON_BRIGHT  = SCREEN_BRIGHT | BUTTON_BRIGHT_BIT;

    // SCREEN_BUTTON_BRIGHT == screen on, screen, button and keyboard backlights bright
    private static final int ALL_BRIGHT         = SCREEN_BUTTON_BRIGHT | KEYBOARD_BRIGHT_BIT;

    // used for noChangeLights in setPowerState()
    private static final int LIGHTS_MASK        = SCREEN_BRIGHT_BIT | BUTTON_BRIGHT_BIT | KEYBOARD_BRIGHT_BIT;

    boolean mAnimateScreenLights = true;

    boolean mElectronBeamAnimationOn = false;
    boolean mElectronBeamAnimationOff = false;

    static final int ANIM_STEPS = 60/4;
    // Slower animation for autobrightness changes
    static final int AUTOBRIGHTNESS_ANIM_STEPS = 60;

    // These magic numbers are the initial state of the LEDs at boot.  Ideally
    // we should read them from the driver, but our current hardware returns 0
    // for the initial value.  Oops!
    static final int INITIAL_SCREEN_BRIGHTNESS = 255;
    static final int INITIAL_BUTTON_BRIGHTNESS = Power.BRIGHTNESS_OFF;
    static final int INITIAL_KEYBOARD_BRIGHTNESS = Power.BRIGHTNESS_OFF;

    private final int MY_UID;
    private final int MY_PID;

    private boolean mDoneBooting = false;
    private boolean mBootCompleted = false;
    private int mStayOnConditions = 0;
    private final int[] mBroadcastQueue = new int[] { -1, -1, -1 };
    private final int[] mBroadcastWhy = new int[3];
    private boolean mPreparingForScreenOn = false;
    private boolean mSkippedScreenOn = false;
    private boolean mInitialized = false;
    private int mPartialCount = 0;
    private int mPowerState;
    // mScreenOffReason can be WindowManagerPolicy.OFF_BECAUSE_OF_USER,
    // WindowManagerPolicy.OFF_BECAUSE_OF_TIMEOUT or WindowManagerPolicy.OFF_BECAUSE_OF_PROX_SENSOR
    private int mScreenOffReason;
    private int mUserState;
    private boolean mKeyboardVisible = false;
    private boolean mUserActivityAllowed = true;
    private int mProximityWakeLockCount = 0;
    private boolean mProximitySensorEnabled = false;
    private boolean mProximitySensorActive = false;
    private int mProximityPendingValue = -1; // -1 == nothing, 0 == inactive, 1 == active
    private long mLastProximityEventTime;
    private int mScreenOffTimeoutSetting;
    private int mMaximumScreenOffTimeout = Integer.MAX_VALUE;
    private int mKeylightDelay;
    private int mDimDelay;
    private int mScreenOffDelay;
    private int mWakeLockState;
    private long mLastEventTime = 0;
    private long mScreenOffTime;
    private volatile WindowManagerPolicy mPolicy;
    private final LockList mLocks = new LockList();
    private Intent mScreenOffIntent;
    private Intent mScreenOnIntent;
    private LightsService mLightsService;
    private Context mContext;
    private Context mUiContext;
    private LightsService.Light mLcdLight;
    private LightsService.Light mButtonLight;
    private LightsService.Light mKeyboardLight;
    private LightsService.Light mAttentionLight;
    private UnsynchronizedWakeLock mBroadcastWakeLock;
    private UnsynchronizedWakeLock mStayOnWhilePluggedInScreenDimLock;
    private UnsynchronizedWakeLock mStayOnWhilePluggedInPartialLock;
    private UnsynchronizedWakeLock mPreventScreenOnPartialLock;
    private UnsynchronizedWakeLock mProximityPartialLock;
    private HandlerThread mHandlerThread;
    private HandlerThread mScreenOffThread;
    private Handler mScreenOffHandler;
    private Handler mHandler;
    private final TimeoutTask mTimeoutTask = new TimeoutTask();
    private final BrightnessState mScreenBrightness
            = new BrightnessState(SCREEN_BRIGHT_BIT);
    private boolean mStillNeedSleepNotification;
    private boolean mIsPowered = false;
    private IActivityManager mActivityService;
    private IBatteryStats mBatteryStats;
    private BatteryService mBatteryService;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private Sensor mLightSensor;
    private boolean mLightSensorEnabled;
    private float mLightSensorValue = -1;
    private boolean mProxIgnoredBecauseScreenTurnedOff = false;
    private int mHighestLightSensorValue = -1;
    private boolean mLightSensorPendingDecrease = false;
    private boolean mLightSensorPendingIncrease = false;
    private float mLightSensorPendingValue = -1;
    private int mLightSensorScreenBrightness = -1;
    private int mLightSensorButtonBrightness = -1;
    private int mLightSensorKeyboardBrightness = -1;
    private boolean mDimScreen = true;
    private boolean mIsDocked = false;
    private long mNextTimeout;
    private volatile int mPokey = 0;
    private volatile boolean mPokeAwakeOnSet = false;
    private volatile boolean mInitComplete = false;
    private final HashMap<IBinder,PokeLock> mPokeLocks = new HashMap<IBinder,PokeLock>();
    // mLastScreenOnTime is the time the screen was last turned on
    private long mLastScreenOnTime;
    private boolean mPreventScreenOn;
    private int mScreenBrightnessOverride = -1;
    private int mButtonBrightnessOverride = -1;
    private int mScreenBrightnessDim;
    private boolean mUseSoftwareAutoBrightness;
    private boolean mAutoBrightessEnabled = true;
    private int[] mAutoBrightnessLevels;
    private int[] mLcdBacklightValues;
    private int[] mButtonBacklightValues;
    private int[] mKeyboardBacklightValues;
    private int mLightSensorWarmupTime;
    boolean mUnplugTurnsOnScreen;
    private int mWarningSpewThrottleCount;
    private long mWarningSpewThrottleTime;
    private int mAnimationSetting = ANIM_SETTING_OFF;

    // When using software auto-brightness, determines whether (true) button
    // and keyboard backlights should also be under automatic brightness
    // control (i.e., for dimmable backlights), or (false) if they should use
    // hard-coded brightness settings that timeout-to-off in subsequent screen
    // power states.
    private boolean mAutoBrightnessButtonKeyboard;

    // Must match with the ISurfaceComposer constants in C++.
    private static final int ANIM_SETTING_ON = 0x01;
    private static final int ANIM_SETTING_OFF = 0x10;

    // Custom light housekeeping
    private long mLightSettingsTag = -1;

    // Light sensor levels / values
    private boolean mLightDecrease;
    private float mLightHysteresis;
    private boolean mCustomLightEnabled;
    private int[] mCustomLightLevels;
    private int[] mCustomLcdValues;
    private int[] mCustomButtonValues;
    private int[] mCustomKeyboardValues;
    private int mLastLcdValue;
    private int mLastButtonValue;
    private int mLastKeyboardValue;
    private int mScreenDim = Power.BRIGHTNESS_DIM;
    private boolean mAlwaysOnAndDimmed;

    // Light sensor filter, times in milliseconds
    private boolean mLightFilterEnabled;
    private boolean mLightFilterRunning;
    private int mLightFilterSample = -1;
    private int[] mLightFilterSamples;
    private int mLightFilterIndex;
    private int mLightFilterSampleCounter;
    private int mLightFilterSum;
    private int mLightFilterEqualCounter;
    private int mLightFilterWindow;
    private int mLightFilterInterval;
    private int mLightFilterReset;

    // Used when logging number and duration of touch-down cycles
    private long mTotalTouchDownTime;
    private long mLastTouchDown;
    private int mTouchCycles;

    // could be either static or controllable at runtime
    private static final boolean mSpew = false;
    private static final boolean mDebugProximitySensor = (false || mSpew);
    private static final boolean mDebugLightSensor = (false || mSpew);
    
    private native void nativeInit();
    private native void nativeSetPowerState(boolean screenOn, boolean screenBright);
    private native void nativeStartSurfaceFlingerOffAnimation(int mode);
    private native void nativeStartSurfaceFlingerOnAnimation(int mode);

    /*
    static PrintStream mLog;
    static {
        try {
            mLog = new PrintStream("/data/power.log");
        }
        catch (FileNotFoundException e) {
            android.util.Slog.e(TAG, "Life is hard", e);
        }
    }
    static class Log {
        static void d(String tag, String s) {
            mLog.println(s);
            android.util.Slog.d(tag, s);
        }
        static void i(String tag, String s) {
            mLog.println(s);
            android.util.Slog.i(tag, s);
        }
        static void w(String tag, String s) {
            mLog.println(s);
            android.util.Slog.w(tag, s);
        }
        static void e(String tag, String s) {
            mLog.println(s);
            android.util.Slog.e(tag, s);
        }
    }
    */

    /**
     * This class works around a deadlock between the lock in PowerManager.WakeLock
     * and our synchronizing on mLocks.  PowerManager.WakeLock synchronizes on its
     * mToken object so it can be accessed from any thread, but it calls into here
     * with its lock held.  This class is essentially a reimplementation of
     * PowerManager.WakeLock, but without that extra synchronized block, because we'll
     * only call it with our own locks held.
     */
    private class UnsynchronizedWakeLock {
        int mFlags;
        String mTag;
        IBinder mToken;
        int mCount = 0;
        boolean mRefCounted;
        boolean mHeld;

        UnsynchronizedWakeLock(int flags, String tag, boolean refCounted) {
            mFlags = flags;
            mTag = tag;
            mToken = new Binder();
            mRefCounted = refCounted;
        }

        public void acquire() {
            if (!mRefCounted || mCount++ == 0) {
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.acquireWakeLockLocked(mFlags, mToken,
                            MY_UID, MY_PID, mTag, null);
                    mHeld = true;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        public void release() {
            if (!mRefCounted || --mCount == 0) {
                PowerManagerService.this.releaseWakeLockLocked(mToken, 0, false);
                mHeld = false;
            }
            if (mCount < 0) {
                throw new RuntimeException("WakeLock under-locked " + mTag);
            }
        }

        public boolean isHeld()
        {
            return mHeld;
        }

        public String toString() {
            return "UnsynchronizedWakeLock(mFlags=0x" + Integer.toHexString(mFlags)
                    + " mCount=" + mCount + " mHeld=" + mHeld + ")";
        }
    }

    private final class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mLocks) {
                boolean wasPowered = mIsPowered;
                mIsPowered = mBatteryService.isPowered();

                if (mIsPowered != wasPowered) {
                    // update mStayOnWhilePluggedIn wake lock
                    updateWakeLockLocked();

                    // treat plugging and unplugging the devices as a user activity.
                    // users find it disconcerting when they unplug the device
                    // and it shuts off right away.
                    // to avoid turning on the screen when unplugging, we only trigger
                    // user activity when screen was already on.
                    // temporarily set mUserActivityAllowed to true so this will work
                    // even when the keyguard is on.
                    // However, you can also set config_unplugTurnsOnScreen to have it
                    // turn on.  Some devices want this because they don't have a
                    // charging LED.
                    synchronized (mLocks) {
                        if (!wasPowered || (mPowerState & SCREEN_ON_BIT) != 0 ||
                                mUnplugTurnsOnScreen) {
                            forceUserActivityLocked();
                        }
                    }
                }
            }
        }
    }

    private final class BootCompletedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            bootCompleted();
        }
    }

    private final class DockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                    Intent.EXTRA_DOCK_STATE_UNDOCKED);
            dockStateChanged(state);
        }
    }

    /**
     * Set the setting that determines whether the device stays on when plugged in.
     * The argument is a bit string, with each bit specifying a power source that,
     * when the device is connected to that source, causes the device to stay on.
     * See {@link android.os.BatteryManager} for the list of power sources that
     * can be specified. Current values include {@link android.os.BatteryManager#BATTERY_PLUGGED_AC}
     * and {@link android.os.BatteryManager#BATTERY_PLUGGED_USB}
     * @param val an {@code int} containing the bits that specify which power sources
     * should cause the device to stay on.
     */
    public void setStayOnSetting(int val) {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.WRITE_SETTINGS, null);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.STAY_ON_WHILE_PLUGGED_IN, val);
    }

    public void setMaximumScreenOffTimeount(int timeMs) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.WRITE_SECURE_SETTINGS, null);
        synchronized (mLocks) {
            mMaximumScreenOffTimeout = timeMs;
            // recalculate everything
            setScreenOffTimeoutsLocked();
        }
    }

    private class SettingsObserver implements Observer {
        private int getInt(String name, int defValue) {
            ContentValues values = mSettings.getValues(name);
            Integer iVal = values != null ? values.getAsInteger(Settings.System.VALUE) : null;
            return iVal != null ? iVal : defValue;
        }

        private float getFloat(String name, float defValue) {
            ContentValues values = mSettings.getValues(name);
            Float fVal = values != null ? values.getAsFloat(Settings.System.VALUE) : null;
            return fVal != null ? fVal : defValue;
        }

        public void update(Observable o, Object arg) {

            synchronized (mLocks) {
                // STAY_ON_WHILE_PLUGGED_IN, default to when plugged into AC
                mStayOnConditions = getInt(STAY_ON_WHILE_PLUGGED_IN,
                        BatteryManager.BATTERY_PLUGGED_AC);
                updateWakeLockLocked();

                // SCREEN_OFF_TIMEOUT, default to 15 seconds
                mScreenOffTimeoutSetting = getInt(SCREEN_OFF_TIMEOUT, DEFAULT_SCREEN_OFF_TIMEOUT);

                // DIM_SCREEN
                //mDimScreen = getInt(DIM_SCREEN) != 0;

                updateLightSettings();

                // SCREEN_BRIGHTNESS_MODE, default to manual
                setScreenBrightnessMode(getInt(SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL));

                // recalculate everything
                setScreenOffTimeoutsLocked();

                //read user settings and device config to control animations availability
                mElectronBeamAnimationOn = (Settings.System.getInt(mContext.getContentResolver(),
                        ELECTRON_BEAM_ANIMATION_ON, 0) != 0) &&
                        mContext.getResources().getInteger(com.android.internal.R.integer.config_screenOnAnimation) >= 0;
                mElectronBeamAnimationOff = (Settings.System.getInt(mContext.getContentResolver(),
                        ELECTRON_BEAM_ANIMATION_OFF, 1) != 0) &&
                        mContext.getResources().getBoolean(com.android.internal.R.bool.config_screenOffAnimation);

                mAnimationSetting = 0;
                if (mElectronBeamAnimationOff) {
                    mAnimationSetting |= ANIM_SETTING_OFF;
                }
                if (mElectronBeamAnimationOn) {
                    mAnimationSetting |= ANIM_SETTING_ON;
                }
            }
        }
    }

    PowerManagerService() {
        // Hack to get our uid...  should have a func for this.
        long token = Binder.clearCallingIdentity();
        MY_UID = Process.myUid();
        MY_PID = Process.myPid();
        Binder.restoreCallingIdentity(token);

        // XXX remove this when the kernel doesn't timeout wake locks
        Power.setLastUserActivityTimeout(7*24*3600*1000); // one week

        // assume nothing is on yet
        mUserState = mPowerState = 0;

        // Add ourself to the Watchdog monitors.
        Watchdog.getInstance().addMonitor(this);
    }

    private ContentQueryMap mSettings;

    void init(Context context, LightsService lights, IActivityManager activity,
            BatteryService battery) {
        mLightsService = lights;
        mContext = context;
        mActivityService = activity;
        mBatteryStats = BatteryStatsService.getService();
        mBatteryService = battery;

        mLcdLight = lights.getLight(LightsService.LIGHT_ID_BACKLIGHT);
        mButtonLight = lights.getLight(LightsService.LIGHT_ID_BUTTONS);
        mKeyboardLight = lights.getLight(LightsService.LIGHT_ID_KEYBOARD);
        mAttentionLight = lights.getLight(LightsService.LIGHT_ID_ATTENTION);

        nativeInit();
        synchronized (mLocks) {
            updateNativePowerStateLocked();
        }

        mInitComplete = false;
        mScreenOffThread = new HandlerThread("PowerManagerService.mScreenOffThread") {
            @Override
            protected void onLooperPrepared() {
                mScreenOffHandler = new Handler();
                synchronized (mScreenOffThread) {
                    mInitComplete = true;
                    mScreenOffThread.notifyAll();
                }
            }
        };
        mScreenOffThread.start();

        synchronized (mScreenOffThread) {
            while (!mInitComplete) {
                try {
                    mScreenOffThread.wait();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
        
        mInitComplete = false;
        mHandlerThread = new HandlerThread("PowerManagerService") {
            @Override
            protected void onLooperPrepared() {
                super.onLooperPrepared();
                initInThread();
            }
        };
        mHandlerThread.start();

        synchronized (mHandlerThread) {
            while (!mInitComplete) {
                try {
                    mHandlerThread.wait();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
        
        nativeInit();
        synchronized (mLocks) {
            updateNativePowerStateLocked();
            // We make sure to start out with the screen on due to user activity.
            // (They did just boot their device, after all.)
            forceUserActivityLocked();
            mInitialized = true;
        }
    }

    void initInThread() {
        mHandler = new Handler();

        mBroadcastWakeLock = new UnsynchronizedWakeLock(
                                PowerManager.PARTIAL_WAKE_LOCK, "sleep_broadcast", true);
        mStayOnWhilePluggedInScreenDimLock = new UnsynchronizedWakeLock(
                                PowerManager.SCREEN_DIM_WAKE_LOCK, "StayOnWhilePluggedIn Screen Dim", false);
        mStayOnWhilePluggedInPartialLock = new UnsynchronizedWakeLock(
                                PowerManager.PARTIAL_WAKE_LOCK, "StayOnWhilePluggedIn Partial", false);
        mPreventScreenOnPartialLock = new UnsynchronizedWakeLock(
                                PowerManager.PARTIAL_WAKE_LOCK, "PreventScreenOn Partial", false);
        mProximityPartialLock = new UnsynchronizedWakeLock(
                                PowerManager.PARTIAL_WAKE_LOCK, "Proximity Partial", false);

        mScreenOnIntent = new Intent(Intent.ACTION_SCREEN_ON);
        mScreenOnIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        mScreenOffIntent = new Intent(Intent.ACTION_SCREEN_OFF);
        mScreenOffIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);

        Resources resources = mContext.getResources();

        mAnimateScreenLights = resources.getBoolean(
                com.android.internal.R.bool.config_animateScreenLights);

        mUnplugTurnsOnScreen = resources.getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen);

        mScreenBrightnessDim = resources.getInteger(
                com.android.internal.R.integer.config_screenBrightnessDim);

        // read settings for auto-brightness
        mUseSoftwareAutoBrightness = resources.getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
        mAutoBrightnessButtonKeyboard = mUseSoftwareAutoBrightness && resources.getBoolean(
                com.android.internal.R.bool.config_autoBrightnessButtonKeyboard);
        if (mUseSoftwareAutoBrightness) {
            mAutoBrightnessLevels = resources.getIntArray(
                    com.android.internal.R.array.config_autoBrightnessLevels);
            mLcdBacklightValues = resources.getIntArray(
                    com.android.internal.R.array.config_autoBrightnessLcdBacklightValues);
            mButtonBacklightValues = resources.getIntArray(
                    com.android.internal.R.array.config_autoBrightnessButtonBacklightValues);
            mKeyboardBacklightValues = resources.getIntArray(
                    com.android.internal.R.array.config_autoBrightnessKeyboardBacklightValues);
            mLightSensorWarmupTime = resources.getInteger(
                    com.android.internal.R.integer.config_lightSensorWarmupTime);
        }

       ContentResolver resolver = mContext.getContentResolver();
        Cursor settingsCursor = resolver.query(Settings.System.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?) or ("
                        + Settings.System.NAME + "=?) or ("
                        + Settings.System.NAME + "=?) or ("
                        + Settings.System.NAME + "=?) or ("
                        + Settings.System.NAME + "=?) or ("
                        + Settings.System.NAME + "=?) or ("
                        + Settings.System.NAME + "=?) or ("
                        + Settings.System.NAME + "=?) or ("
                        + Settings.System.NAME + "=?)",
                new String[]{STAY_ON_WHILE_PLUGGED_IN, SCREEN_OFF_TIMEOUT, DIM_SCREEN,
                        SCREEN_BRIGHTNESS_MODE, WINDOW_ANIMATION_SCALE, TRANSITION_ANIMATION_SCALE,
                        Settings.System.LIGHTS_CHANGED, ELECTRON_BEAM_ANIMATION_ON,
                        ELECTRON_BEAM_ANIMATION_OFF},
                null);
        mSettings = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, mHandler);
        SettingsObserver settingsObserver = new SettingsObserver();
        mSettings.addObserver(settingsObserver);

        // pretend that the settings changed so we will get their initial state
        settingsObserver.update(mSettings, null);

        // register for the battery changed notifications
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(new BatteryReceiver(), filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mContext.registerReceiver(new BootCompletedReceiver(), filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DOCK_EVENT);
        mContext.registerReceiver(new DockReceiver(), filter);

        // Listen for secure settings changes
        mContext.getContentResolver().registerContentObserver(
            Settings.Secure.CONTENT_URI, true,
            new ContentObserver(new Handler()) {
                public void onChange(boolean selfChange) {
                    updateSettingsValues();
                }
            });
        updateSettingsValues();

        synchronized (mHandlerThread) {
            mInitComplete = true;
            mHandlerThread.notifyAll();
        }
    }

    private class WakeLock implements IBinder.DeathRecipient
    {
        WakeLock(int f, IBinder b, String t, int u, int p) {
            super();
            flags = f;
            binder = b;
            tag = t;
            uid = u == MY_UID ? Process.SYSTEM_UID : u;
            pid = p;
            if (u != MY_UID || (
                    !"KEEP_SCREEN_ON_FLAG".equals(tag)
                    && !"KeyInputQueue".equals(tag))) {
                monitorType = (f & LOCK_MASK) == PowerManager.PARTIAL_WAKE_LOCK
                        ? BatteryStats.WAKE_TYPE_PARTIAL
                        : BatteryStats.WAKE_TYPE_FULL;
            } else {
                monitorType = -1;
            }
            try {
                b.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }
        public void binderDied() {
            synchronized (mLocks) {
                releaseWakeLockLocked(this.binder, 0, true);
            }
        }
        final int flags;
        final IBinder binder;
        final String tag;
        final int uid;
        final int pid;
        final int monitorType;
        WorkSource ws;
        boolean activated = true;
        int minState;
    }

    private void updateWakeLockLocked() {
        if (mStayOnConditions != 0 && mBatteryService.isPowered(mStayOnConditions)) {
            // keep the device on if we're plugged in and mStayOnWhilePluggedIn is set.
            mStayOnWhilePluggedInScreenDimLock.acquire();
            mStayOnWhilePluggedInPartialLock.acquire();
        } else {
            mStayOnWhilePluggedInScreenDimLock.release();
            mStayOnWhilePluggedInPartialLock.release();
        }
    }

    private boolean isScreenLock(int flags)
    {
        int n = flags & LOCK_MASK;
        return n == PowerManager.FULL_WAKE_LOCK
                || n == PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                || n == PowerManager.SCREEN_DIM_WAKE_LOCK
                || n == PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK;
    }

    void enforceWakeSourcePermission(int uid, int pid) {
        if (uid == Process.myUid()) {
            return;
        }
        mContext.enforcePermission(android.Manifest.permission.UPDATE_DEVICE_STATS,
                pid, uid, null);
    }

    public void acquireWakeLock(int flags, IBinder lock, String tag, WorkSource ws) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        if (uid != Process.myUid()) {
            mContext.enforceCallingOrSelfPermission(android.Manifest.permission.WAKE_LOCK, null);
        }
        if (ws != null) {
            enforceWakeSourcePermission(uid, pid);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (mLocks) {
                acquireWakeLockLocked(flags, lock, uid, pid, tag, ws);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    void noteStartWakeLocked(WakeLock wl, WorkSource ws) {
        if (wl.monitorType >= 0) {
            long origId = Binder.clearCallingIdentity();
            try {
                if (ws != null) {
                    mBatteryStats.noteStartWakelockFromSource(ws, wl.pid, wl.tag,
                            wl.monitorType);
                } else {
                    mBatteryStats.noteStartWakelock(wl.uid, wl.pid, wl.tag, wl.monitorType);
                }
            } catch (RemoteException e) {
                // Ignore
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    void noteStopWakeLocked(WakeLock wl, WorkSource ws) {
        if (wl.monitorType >= 0) {
            long origId = Binder.clearCallingIdentity();
            try {
                if (ws != null) {
                    mBatteryStats.noteStopWakelockFromSource(ws, wl.pid, wl.tag,
                            wl.monitorType);
                } else {
                    mBatteryStats.noteStopWakelock(wl.uid, wl.pid, wl.tag, wl.monitorType);
                }
            } catch (RemoteException e) {
                // Ignore
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    public void acquireWakeLockLocked(int flags, IBinder lock, int uid, int pid, String tag,
            WorkSource ws) {
        if (mSpew) {
            Slog.d(TAG, "acquireWakeLock flags=0x" + Integer.toHexString(flags) + " tag=" + tag);
        }

        if (ws != null && ws.size() == 0) {
            ws = null;
        }

        int index = mLocks.getIndex(lock);
        WakeLock wl;
        boolean newlock;
        boolean diffsource;
        WorkSource oldsource;
        if (index < 0) {
            wl = new WakeLock(flags, lock, tag, uid, pid);
            switch (wl.flags & LOCK_MASK)
            {
                case PowerManager.FULL_WAKE_LOCK:
                    if (mAutoBrightnessButtonKeyboard) {
                        wl.minState = SCREEN_BRIGHT;
                    } else {
                        wl.minState = (mKeyboardVisible ? ALL_BRIGHT : SCREEN_BUTTON_BRIGHT);
                    }
                    break;
                case PowerManager.SCREEN_BRIGHT_WAKE_LOCK:
                    wl.minState = SCREEN_BRIGHT;
                    break;
                case PowerManager.SCREEN_DIM_WAKE_LOCK:
                    wl.minState = SCREEN_DIM;
                    break;
                case PowerManager.PARTIAL_WAKE_LOCK:
                case PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK:
                    break;
                default:
                    // just log and bail.  we're in the server, so don't
                    // throw an exception.
                    Slog.e(TAG, "bad wakelock type for lock '" + tag + "' "
                            + " flags=" + flags);
                    return;
            }
            mLocks.addLock(wl);
            if (ws != null) {
                wl.ws = new WorkSource(ws);
            }
            newlock = true;
            diffsource = false;
            oldsource = null;
        } else {
            wl = mLocks.get(index);
            newlock = false;
            oldsource = wl.ws;
            if (oldsource != null) {
                if (ws == null) {
                    wl.ws = null;
                    diffsource = true;
                } else {
                    diffsource = oldsource.diff(ws);
                }
            } else if (ws != null) {
                diffsource = true;
            } else {
                diffsource = false;
            }
            if (diffsource) {
                wl.ws = new WorkSource(ws);
            }
        }
        if (isScreenLock(flags)) {
            // if this causes a wakeup, we reactivate all of the locks and
            // set it to whatever they want.  otherwise, we modulate that
            // by the current state so we never turn it more on than
            // it already is.
            if ((flags & LOCK_MASK) == PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) {
                mProximityWakeLockCount++;
                if (mProximityWakeLockCount == 1) {
                    enableProximityLockLocked();
                }
            } else {
                if ((wl.flags & PowerManager.ACQUIRE_CAUSES_WAKEUP) != 0) {
                    int oldWakeLockState = mWakeLockState;
                    mWakeLockState = mLocks.reactivateScreenLocksLocked();

                    // Disable proximity sensor if if user presses power key while we are in the
                    // "waiting for proximity sensor to go negative" state.
                    if ((mWakeLockState & SCREEN_ON_BIT) != 0
                            && mProximitySensorActive && mProximityWakeLockCount == 0) {
                        mProximitySensorActive = false;
                    }

                    if (mSpew) {
                        Slog.d(TAG, "wakeup here mUserState=0x" + Integer.toHexString(mUserState)
                                + " mWakeLockState=0x"
                                + Integer.toHexString(mWakeLockState)
                                + " previous wakeLockState=0x"
                                + Integer.toHexString(oldWakeLockState));
                    }
                } else {
                    if (mSpew) {
                        Slog.d(TAG, "here mUserState=0x" + Integer.toHexString(mUserState)
                                + " mLocks.gatherState()=0x"
                                + Integer.toHexString(mLocks.gatherState())
                                + " mWakeLockState=0x" + Integer.toHexString(mWakeLockState));
                    }
                    mWakeLockState = (mUserState | mWakeLockState) & mLocks.gatherState();
                }
                setPowerState(mWakeLockState | mUserState);
            }
        }
        else if ((flags & LOCK_MASK) == PowerManager.PARTIAL_WAKE_LOCK) {
            if (newlock) {
                mPartialCount++;
                if (mPartialCount == 1) {
                    if (LOG_PARTIAL_WL) EventLog.writeEvent(EventLogTags.POWER_PARTIAL_WAKE_STATE, 1, tag);
                }
            }
            Power.acquireWakeLock(Power.PARTIAL_WAKE_LOCK,PARTIAL_NAME);
        }

        if (diffsource) {
            // If the lock sources have changed, need to first release the
            // old ones.
            noteStopWakeLocked(wl, oldsource);
        }
        if (newlock || diffsource) {
            noteStartWakeLocked(wl, ws);
        }
    }

    public void updateWakeLockWorkSource(IBinder lock, WorkSource ws) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        if (ws != null && ws.size() == 0) {
            ws = null;
        }
        if (ws != null) {
            enforceWakeSourcePermission(uid, pid);
        }
        synchronized (mLocks) {
            int index = mLocks.getIndex(lock);
            if (index < 0) {
                throw new IllegalArgumentException("Wake lock not active");
            }
            WakeLock wl = mLocks.get(index);
            WorkSource oldsource = wl.ws;
            wl.ws = ws != null ? new WorkSource(ws) : null;
            noteStopWakeLocked(wl, oldsource);
            noteStartWakeLocked(wl, ws);
        }
    }

    public void releaseWakeLock(IBinder lock, int flags) {
        int uid = Binder.getCallingUid();
        if (uid != Process.myUid()) {
            mContext.enforceCallingOrSelfPermission(android.Manifest.permission.WAKE_LOCK, null);
        }

        synchronized (mLocks) {
            releaseWakeLockLocked(lock, flags, false);
        }
    }

    private void releaseWakeLockLocked(IBinder lock, int flags, boolean death) {
        WakeLock wl = mLocks.removeLock(lock);
        if (wl == null) {
            return;
        }

        if (mSpew) {
            Slog.d(TAG, "releaseWakeLock flags=0x"
                    + Integer.toHexString(wl.flags) + " tag=" + wl.tag);
        }

        if (isScreenLock(wl.flags)) {
            if ((wl.flags & LOCK_MASK) == PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) {
                mProximityWakeLockCount--;
                if (mProximityWakeLockCount == 0) {
                    if (mProximitySensorActive &&
                            ((flags & PowerManager.WAIT_FOR_PROXIMITY_NEGATIVE) != 0)) {
                        // wait for proximity sensor to go negative before disabling sensor
                        if (mDebugProximitySensor) {
                            Slog.d(TAG, "waiting for proximity sensor to go negative");
                        }
                    } else {
                        disableProximityLockLocked();
                    }
                }
            } else {
                mWakeLockState = mLocks.gatherState();
                // goes in the middle to reduce flicker
                if ((wl.flags & PowerManager.ON_AFTER_RELEASE) != 0) {
                    userActivity(SystemClock.uptimeMillis(), -1, false, OTHER_EVENT, false);
                }
                setPowerState(mWakeLockState | mUserState);
            }
        }
        else if ((wl.flags & LOCK_MASK) == PowerManager.PARTIAL_WAKE_LOCK) {
            mPartialCount--;
            if (mPartialCount == 0) {
                if (LOG_PARTIAL_WL) EventLog.writeEvent(EventLogTags.POWER_PARTIAL_WAKE_STATE, 0, wl.tag);
                Power.releaseWakeLock(PARTIAL_NAME);
            }
        }
        // Unlink the lock from the binder.
        wl.binder.unlinkToDeath(wl, 0);

        noteStopWakeLocked(wl, wl.ws);
    }

    private class PokeLock implements IBinder.DeathRecipient
    {
        PokeLock(int p, IBinder b, String t) {
            super();
            this.pokey = p;
            this.binder = b;
            this.tag = t;
            try {
                b.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }
        public void binderDied() {
            setPokeLock(0, this.binder, this.tag);
        }
        int pokey;
        IBinder binder;
        String tag;
        boolean awakeOnSet;
    }

    public void setPokeLock(int pokey, IBinder token, String tag) {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DEVICE_POWER, null);
        if (token == null) {
            Slog.e(TAG, "setPokeLock got null token for tag='" + tag + "'");
            return;
        }

        if ((pokey & POKE_LOCK_TIMEOUT_MASK) == POKE_LOCK_TIMEOUT_MASK) {
            throw new IllegalArgumentException("setPokeLock can't have both POKE_LOCK_SHORT_TIMEOUT"
                    + " and POKE_LOCK_MEDIUM_TIMEOUT");
        }

        synchronized (mLocks) {
            if (pokey != 0) {
                PokeLock p = mPokeLocks.get(token);
                int oldPokey = 0;
                if (p != null) {
                    oldPokey = p.pokey;
                    p.pokey = pokey;
                } else {
                    p = new PokeLock(pokey, token, tag);
                    mPokeLocks.put(token, p);
                }
                int oldTimeout = oldPokey & POKE_LOCK_TIMEOUT_MASK;
                int newTimeout = pokey & POKE_LOCK_TIMEOUT_MASK;
                if (((mPowerState & SCREEN_ON_BIT) == 0) && (oldTimeout != newTimeout)) {
                    p.awakeOnSet = true;
                }
            } else {
                PokeLock rLock = mPokeLocks.remove(token);
                if (rLock != null) {
                    token.unlinkToDeath(rLock, 0);
                }
            }

            int oldPokey = mPokey;
            int cumulative = 0;
            boolean oldAwakeOnSet = mPokeAwakeOnSet;
            boolean awakeOnSet = false;
            for (PokeLock p: mPokeLocks.values()) {
                cumulative |= p.pokey;
                if (p.awakeOnSet) {
                    awakeOnSet = true;
                }
            }
            mPokey = cumulative;
            mPokeAwakeOnSet = awakeOnSet;

            int oldCumulativeTimeout = oldPokey & POKE_LOCK_TIMEOUT_MASK;
            int newCumulativeTimeout = pokey & POKE_LOCK_TIMEOUT_MASK;

            if (oldCumulativeTimeout != newCumulativeTimeout) {
                setScreenOffTimeoutsLocked();
                // reset the countdown timer, but use the existing nextState so it doesn't
                // change anything
                setTimeoutLocked(SystemClock.uptimeMillis(), mTimeoutTask.nextState);
            }
        }
    }

    private static String lockType(int type)
    {
        switch (type)
        {
            case PowerManager.FULL_WAKE_LOCK:
                return "FULL_WAKE_LOCK                ";
            case PowerManager.SCREEN_BRIGHT_WAKE_LOCK:
                return "SCREEN_BRIGHT_WAKE_LOCK       ";
            case PowerManager.SCREEN_DIM_WAKE_LOCK:
                return "SCREEN_DIM_WAKE_LOCK          ";
            case PowerManager.PARTIAL_WAKE_LOCK:
                return "PARTIAL_WAKE_LOCK             ";
            case PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK:
                return "PROXIMITY_SCREEN_OFF_WAKE_LOCK";
            default:
                return "???                           ";
        }
    }

    private static String dumpPowerState(int state) {
        return (((state & KEYBOARD_BRIGHT_BIT) != 0)
                        ? "KEYBOARD_BRIGHT_BIT " : "")
                + (((state & SCREEN_BRIGHT_BIT) != 0)
                        ? "SCREEN_BRIGHT_BIT " : "")
                + (((state & SCREEN_ON_BIT) != 0)
                        ? "SCREEN_ON_BIT " : "")
                + (((state & BATTERY_LOW_BIT) != 0)
                        ? "BATTERY_LOW_BIT " : "");
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission Denial: can't dump PowerManager from from pid="
                    + Binder.getCallingPid()
                    + ", uid=" + Binder.getCallingUid());
            return;
        }

        long now = SystemClock.uptimeMillis();

        synchronized (mLocks) {
            pw.println("Power Manager State:");
            pw.println("  mIsPowered=" + mIsPowered
                    + " mPowerState=" + mPowerState
                    + " mScreenOffTime=" + (SystemClock.elapsedRealtime()-mScreenOffTime)
                    + " ms");
            pw.println("  mPartialCount=" + mPartialCount);
            pw.println("  mWakeLockState=" + dumpPowerState(mWakeLockState));
            pw.println("  mUserState=" + dumpPowerState(mUserState));
            pw.println("  mPowerState=" + dumpPowerState(mPowerState));
            pw.println("  mLocks.gather=" + dumpPowerState(mLocks.gatherState()));
            pw.println("  mNextTimeout=" + mNextTimeout + " now=" + now
                    + " " + ((mNextTimeout-now)/1000) + "s from now");
            pw.println("  mDimScreen=" + mDimScreen
                    + " mStayOnConditions=" + mStayOnConditions
                    + " mPreparingForScreenOn=" + mPreparingForScreenOn
                    + " mSkippedScreenOn=" + mSkippedScreenOn);
            pw.println("  mScreenOffReason=" + mScreenOffReason
                    + " mUserState=" + mUserState);
            pw.println("  mBroadcastQueue={" + mBroadcastQueue[0] + ',' + mBroadcastQueue[1]
                    + ',' + mBroadcastQueue[2] + "}");
            pw.println("  mBroadcastWhy={" + mBroadcastWhy[0] + ',' + mBroadcastWhy[1]
                    + ',' + mBroadcastWhy[2] + "}");
            pw.println("  mPokey=" + mPokey + " mPokeAwakeonSet=" + mPokeAwakeOnSet);
            pw.println("  mKeyboardVisible=" + mKeyboardVisible
                    + " mUserActivityAllowed=" + mUserActivityAllowed);
            pw.println("  mKeylightDelay=" + mKeylightDelay + " mDimDelay=" + mDimDelay
                    + " mScreenOffDelay=" + mScreenOffDelay);
            pw.println("  mPreventScreenOn=" + mPreventScreenOn
                    + "  mScreenBrightnessOverride=" + mScreenBrightnessOverride
                    + "  mButtonBrightnessOverride=" + mButtonBrightnessOverride);
            pw.println("  mScreenOffTimeoutSetting=" + mScreenOffTimeoutSetting
                    + " mMaximumScreenOffTimeout=" + mMaximumScreenOffTimeout);
            pw.println("  mLastScreenOnTime=" + mLastScreenOnTime);
            pw.println("  mBroadcastWakeLock=" + mBroadcastWakeLock);
            pw.println("  mStayOnWhilePluggedInScreenDimLock=" + mStayOnWhilePluggedInScreenDimLock);
            pw.println("  mStayOnWhilePluggedInPartialLock=" + mStayOnWhilePluggedInPartialLock);
            pw.println("  mPreventScreenOnPartialLock=" + mPreventScreenOnPartialLock);
            pw.println("  mProximityPartialLock=" + mProximityPartialLock);
            pw.println("  mProximityWakeLockCount=" + mProximityWakeLockCount);
            pw.println("  mProximitySensorEnabled=" + mProximitySensorEnabled);
            pw.println("  mProximitySensorActive=" + mProximitySensorActive);
            pw.println("  mProximityPendingValue=" + mProximityPendingValue);
            pw.println("  mLastProximityEventTime=" + mLastProximityEventTime);
            pw.println("  mLightSensorEnabled=" + mLightSensorEnabled);
            pw.println("  mLightSensorValue=" + mLightSensorValue
                    + " mLightSensorPendingValue=" + mLightSensorPendingValue);
            pw.println("  mLightSensorPendingDecrease=" + mLightSensorPendingDecrease
                    + " mLightSensorPendingIncrease=" + mLightSensorPendingIncrease);
            pw.println("  mLightSensorScreenBrightness=" + mLightSensorScreenBrightness
                    + " mLightSensorButtonBrightness=" + mLightSensorButtonBrightness
                    + " mLightSensorKeyboardBrightness=" + mLightSensorKeyboardBrightness);
            pw.println("  mUseSoftwareAutoBrightness=" + mUseSoftwareAutoBrightness);
            pw.println("  mAutoBrightnessButtonKeyboard=" + mAutoBrightnessButtonKeyboard);
            pw.println("  mAutoBrightessEnabled=" + mAutoBrightessEnabled);
            mScreenBrightness.dump(pw, "  mScreenBrightness: ");

            int N = mLocks.size();
            pw.println();
            pw.println("mLocks.size=" + N + ":");
            for (int i=0; i<N; i++) {
                WakeLock wl = mLocks.get(i);
                String type = lockType(wl.flags & LOCK_MASK);
                String acquireCausesWakeup = "";
                if ((wl.flags & PowerManager.ACQUIRE_CAUSES_WAKEUP) != 0) {
                    acquireCausesWakeup = "ACQUIRE_CAUSES_WAKEUP ";
                }
                String activated = "";
                if (wl.activated) {
                   activated = " activated";
                }
                pw.println("  " + type + " '" + wl.tag + "'" + acquireCausesWakeup
                        + activated + " (minState=" + wl.minState + ", uid=" + wl.uid
                        + ", pid=" + wl.pid + ")");
            }

            pw.println();
            pw.println("mPokeLocks.size=" + mPokeLocks.size() + ":");
            for (PokeLock p: mPokeLocks.values()) {
                pw.println("    poke lock '" + p.tag + "':"
                        + ((p.pokey & POKE_LOCK_IGNORE_TOUCH_EVENTS) != 0
                                ? " POKE_LOCK_IGNORE_TOUCH_EVENTS" : "")
                        + ((p.pokey & POKE_LOCK_SHORT_TIMEOUT) != 0
                                ? " POKE_LOCK_SHORT_TIMEOUT" : "")
                        + ((p.pokey & POKE_LOCK_MEDIUM_TIMEOUT) != 0
                                ? " POKE_LOCK_MEDIUM_TIMEOUT" : ""));
            }

            pw.println();
        }
    }

    private void setTimeoutLocked(long now, int nextState) {
        setTimeoutLocked(now, -1, nextState);
    }

    // If they gave a timeoutOverride it is the number of seconds
    // to screen-off.  Figure out where in the countdown cycle we
    // should jump to.
    private void setTimeoutLocked(long now, final long originalTimeoutOverride, int nextState) {
        long timeoutOverride = originalTimeoutOverride;
        if (mBootCompleted) {
            synchronized (mLocks) {
                long when = 0;
                if (timeoutOverride <= 0) {
                    switch (nextState)
                    {
                        case SCREEN_BRIGHT:
                            when = now + mKeylightDelay;
                            break;
                        case SCREEN_DIM:
                            if (mDimDelay >= 0) {
                                when = now + mDimDelay;
                                break;
                            } else {
                                Slog.w(TAG, "mDimDelay=" + mDimDelay + " while trying to dim");
                            }
                       case SCREEN_OFF:
                            synchronized (mLocks) {
                                when = now + mScreenOffDelay;
                            }
                            break;
                        default:
                            when = now;
                            break;
                    }
                } else {
                    override: {
                        if (timeoutOverride <= mScreenOffDelay) {
                            when = now + timeoutOverride;
                            nextState = SCREEN_OFF;
                            break override;
                        }
                        timeoutOverride -= mScreenOffDelay;

                        if (mDimDelay >= 0) {
                             if (timeoutOverride <= mDimDelay) {
                                when = now + timeoutOverride;
                                nextState = SCREEN_DIM;
                                break override;
                            }
                            timeoutOverride -= mDimDelay;
                        }

                        when = now + timeoutOverride;
                        nextState = SCREEN_BRIGHT;
                    }
                }
                if (mSpew) {
                    Slog.d(TAG, "setTimeoutLocked now=" + now
                            + " timeoutOverride=" + timeoutOverride
                            + " nextState=" + nextState + " when=" + when);
                }

                mHandler.removeCallbacks(mTimeoutTask);
                mTimeoutTask.nextState = nextState;
                mTimeoutTask.remainingTimeoutOverride = timeoutOverride > 0
                        ? (originalTimeoutOverride - timeoutOverride)
                        : -1;
                mHandler.postAtTime(mTimeoutTask, when);
                mNextTimeout = when; // for debugging
            }
        }
    }

    private void cancelTimerLocked()
    {
        mHandler.removeCallbacks(mTimeoutTask);
        mTimeoutTask.nextState = -1;
    }

    private class TimeoutTask implements Runnable
    {
        int nextState; // access should be synchronized on mLocks
        long remainingTimeoutOverride;
        public void run()
        {
            synchronized (mLocks) {
                if (mSpew) {
                    Slog.d(TAG, "user activity timeout timed out nextState=" + this.nextState);
                }

                if (nextState == -1) {
                    return;
                }

                mUserState = this.nextState;
                setPowerState(this.nextState | mWakeLockState);

                long now = SystemClock.uptimeMillis();

                switch (this.nextState)
                {
                    case SCREEN_BRIGHT:
                        if (mDimDelay >= 0) {
                            setTimeoutLocked(now, remainingTimeoutOverride, SCREEN_DIM);
                            break;
                        }
                    case SCREEN_DIM:
                        setTimeoutLocked(now, remainingTimeoutOverride, SCREEN_OFF);
                        break;
                }
            }
        }
    }

    private void sendNotificationLocked(boolean on, int why) {
        if (!mInitialized) {
            // No notifications sent until first initialization is done.
            // This is so that when we are moving from our initial state
            // which looks like the screen was off to it being on, we do not
            // go through the process of waiting for the higher-level user
            // space to be ready before turning up the display brightness.
            // (And also do not send needless broadcasts about the screen.)
            return;
        }

        if (DEBUG_SCREEN_ON) {
            RuntimeException here = new RuntimeException("here");
            here.fillInStackTrace();
            Slog.i(TAG, "sendNotificationLocked: " + on, here);
        }

        if (!on) {
            mStillNeedSleepNotification = false;
        }

        // Add to the queue.
        int index = 0;
        while (mBroadcastQueue[index] != -1) {
            index++;
        }
        mBroadcastQueue[index] = on ? 1 : 0;
        mBroadcastWhy[index] = why;

        // If we added it position 2, then there is a pair that can be stripped.
        // If we added it position 1 and we're turning the screen off, we can strip
        // the pair and do nothing, because the screen is already off, and therefore
        // keyguard has already been enabled.
        // However, if we added it at position 1 and we're turning it on, then position
        // 0 was to turn it off, and we can't strip that, because keyguard needs to come
        // on, so have to run the queue then.
        if (index == 2) {
            // While we're collapsing them, if it's going off, and the new reason
            // is more significant than the first, then use the new one.
            if (!on && mBroadcastWhy[0] > why) {
                mBroadcastWhy[0] = why;
            }
            mBroadcastQueue[0] = on ? 1 : 0;
            mBroadcastQueue[1] = -1;
            mBroadcastQueue[2] = -1;
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, 1, mBroadcastWakeLock.mCount);
            mBroadcastWakeLock.release();
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, 1, mBroadcastWakeLock.mCount);
            mBroadcastWakeLock.release();
            index = 0;
        }
        if (index == 1 && !on) {
            mBroadcastQueue[0] = -1;
            mBroadcastQueue[1] = -1;
            index = -1;
            // The wake lock was being held, but we're not actually going to do any
            // broadcasts, so release the wake lock.
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, 1, mBroadcastWakeLock.mCount);
            mBroadcastWakeLock.release();
        }

        // The broadcast queue has changed; make sure the screen is on if it
        // is now possible for it to be.
        if (mSkippedScreenOn) {
            updateLightsLocked(mPowerState, SCREEN_ON_BIT);
        }

        // Now send the message.
        if (index >= 0) {
            // Acquire the broadcast wake lock before changing the power
            // state. It will be release after the broadcast is sent.
            // We always increment the ref count for each notification in the queue
            // and always decrement when that notification is handled.
            mBroadcastWakeLock.acquire();
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_SEND, mBroadcastWakeLock.mCount);
            mHandler.post(mNotificationTask);
        }
    }

    private WindowManagerPolicy.ScreenOnListener mScreenOnListener =
            new WindowManagerPolicy.ScreenOnListener() {
                @Override public void onScreenOn() {
                    synchronized (mLocks) {
                        if (mPreparingForScreenOn) {
                            mPreparingForScreenOn = false;
                            updateLightsLocked(mPowerState, SCREEN_ON_BIT);
                            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP,
                                    4, mBroadcastWakeLock.mCount);
                            mBroadcastWakeLock.release();
                        }
                    }
                }
    };

    private Runnable mNotificationTask = new Runnable()
    {
        public void run()
        {
            while (true) {
                int value;
                int why;
                WindowManagerPolicy policy;
                synchronized (mLocks) {
                    value = mBroadcastQueue[0];
                    why = mBroadcastWhy[0];
                    for (int i=0; i<2; i++) {
                        mBroadcastQueue[i] = mBroadcastQueue[i+1];
                        mBroadcastWhy[i] = mBroadcastWhy[i+1];
                    }
                    policy = getPolicyLocked();
                    if (value == 1 && !mPreparingForScreenOn) {
                        mPreparingForScreenOn = true;
                        mBroadcastWakeLock.acquire();
                        EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_SEND,
                                mBroadcastWakeLock.mCount);
                    }
                }
                if (value == 1) {
                    mScreenOnStart = SystemClock.uptimeMillis();

                    policy.screenTurningOn(mScreenOnListener);
                    try {
                        ActivityManagerNative.getDefault().wakingUp();
                    } catch (RemoteException e) {
                        // ignore it
                    }

                    if (mSpew) {
                        Slog.d(TAG, "mBroadcastWakeLock=" + mBroadcastWakeLock);
                    }
                    if (mContext != null && ActivityManagerNative.isSystemReady()) {
                        mContext.sendOrderedBroadcast(mScreenOnIntent, null,
                                mScreenOnBroadcastDone, mHandler, 0, null, null);
                    } else {
                        synchronized (mLocks) {
                            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, 2,
                                    mBroadcastWakeLock.mCount);
                            mBroadcastWakeLock.release();
                        }
                    }
                }
                else if (value == 0) {
                    mScreenOffStart = SystemClock.uptimeMillis();

                    policy.screenTurnedOff(why);
                    try {
                        ActivityManagerNative.getDefault().goingToSleep();
                    } catch (RemoteException e) {
                        // ignore it.
                    }

                    if (mContext != null && ActivityManagerNative.isSystemReady()) {
                        mContext.sendOrderedBroadcast(mScreenOffIntent, null,
                                mScreenOffBroadcastDone, mHandler, 0, null, null);
                    } else {
                        synchronized (mLocks) {
                            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, 3,
                                    mBroadcastWakeLock.mCount);
                            updateLightsLocked(mPowerState, SCREEN_ON_BIT);
                            mBroadcastWakeLock.release();
                        }
                    }
                }
                else {
                    // If we're in this case, then this handler is running for a previous
                    // paired transaction.  mBroadcastWakeLock will already have been released.
                    break;
                }
            }
        }
    };

    long mScreenOnStart;
    private Broadcas
