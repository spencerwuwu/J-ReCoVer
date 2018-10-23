// https://searchcode.com/api/result/1806951/

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

package com.android.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.hardware.CameraSound;
import android.location.Location;
import android.media.CameraProfile;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.camera.ui.CameraPicker;
import com.android.camera.ui.FaceView;
import com.android.camera.ui.IndicatorControlContainer;
import com.android.camera.ui.PopupManager;
import com.android.camera.ui.Rotatable;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.RotateLayout;
import com.android.camera.ui.RotateTextToast;
import com.android.camera.ui.SharePopup;
import com.android.camera.ui.ZoomControl;

/** The Camera activity which can preview and take pictures. */
public class Camera extends ActivityBase implements FocusManager.Listener,
        View.OnTouchListener, ShutterButton.OnShutterButtonListener,
        SurfaceHolder.Callback, ModePicker.OnModeChangeListener,
        FaceDetectionListener, CameraPreference.OnPreferenceChangedListener,
        LocationManager.Listener {

    private static final String TAG = "camera";

    private static final int CROP_MSG = 1;
    private static final int FIRST_TIME_INIT = 2;
    private static final int CLEAR_SCREEN_DELAY = 3;
    private static final int SET_CAMERA_PARAMETERS_WHEN_IDLE = 4;
    private static final int CHECK_DISPLAY_ROTATION = 5;
    private static final int SHOW_TAP_TO_FOCUS_TOAST = 6;
    private static final int UPDATE_THUMBNAIL = 7;
    private static final int FINISH_PINCH_TO_ZOOM = 8;
    private static final int START_TOUCH_TO_FOCUS = 9;
    private static final int CAMERA_TIMER = 10;

    // The subset of parameters we need to update in setCameraParameters().
    private static final int UPDATE_PARAM_INITIALIZE = 1;
    private static final int UPDATE_PARAM_ZOOM = 2;
    private static final int UPDATE_PARAM_PREFERENCE = 4;
    private static final int UPDATE_PARAM_ALL = -1;

    // When setCameraParametersWhenIdle() is called, we accumulate the subsets
    // needed to be updated in mUpdateSet.
    private int mUpdateSet;

    private static final int SCREEN_DELAY = 2 * 60 * 1000;

    private static final int ZOOM_STOPPED = 0;
    private static final int ZOOM_START = 1;
    private static final int ZOOM_STOPPING = 2;

    private static final int TOUCH_TO_FOCUS_START_DELAY = 150; // milliseconds

    private int mZoomState = ZOOM_STOPPED;
    private boolean mSmoothZoomSupported = false;
    private int mZoomValue;  // The current zoom value.
    private int mZoomMax;
    private int mTargetZoomValue;
    private ZoomControl mZoomControl;

    private boolean mStartZoom = false;
    private float oldDistance = 1f;

    private Parameters mParameters;
    private Parameters mInitialParams;
    private boolean mFocusAreaSupported;
    private boolean mMeteringAreaSupported;
    private boolean mAeLockSupported;
    private boolean mAwbLockSupported;

    private MyOrientationEventListener mOrientationListener;
    // The degrees of the device rotated clockwise from its natural orientation.
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    // The orientation compensation for icons and thumbnails. Ex: if the value
    // is 90, the UI components should be rotated 90 degrees counter-clockwise.
    private int mOrientationCompensation = 0;
    private ComboPreferences mPreferences;

    private static final String sTempCropFilename = "crop-temp";

    private ContentProviderClient mMediaProviderClient;
    private SurfaceHolder mSurfaceHolder = null;
    private ShutterButton mShutterButton;
    private GestureDetector mPopupGestureDetector;
    private boolean mOpenCameraFail = false;
    private boolean mCameraDisabled = false;
    private boolean mFaceDetectionStarted = false;

    private View mPreviewPanel;  // The container of PreviewFrameLayout.
    private PreviewFrameLayout mPreviewFrameLayout;
    private View mPreviewFrame;  // Preview frame area.
    private RotateDialogController mRotateDialog;

    // A popup window that contains a bigger thumbnail and a list of apps to share.
    private SharePopup mSharePopup;
    // The bitmap of the last captured picture thumbnail and the URI of the
    // original picture.
    private Thumbnail mThumbnail;
    // An imageview showing showing the last captured picture thumbnail.
    private RotateImageView mThumbnailView;
    private ModePicker mModePicker;
    private FaceView mFaceView;
    private RotateLayout mFocusAreaIndicator;
    private Rotatable mReviewCancelButton;
    private Rotatable mReviewDoneButton;

    // mCropValue and mSaveUri are used only if isImageCaptureIntent() is true.
    private String mCropValue;
    private Uri mSaveUri;

    // Small indicators which show the camera settings in the viewfinder.
    private TextView mExposureIndicator;
    private ImageView mGpsIndicator;
    private ImageView mFlashIndicator;
    private ImageView mTimerIndicator;
    private ImageView mSceneIndicator;
    private ImageView mWhiteBalanceIndicator;
    private ImageView mFocusIndicator;
    // A view group that contains all the small indicators.
    private Rotatable mOnScreenIndicators;

    // We use a thread in ImageSaver to do the work of saving images and
    // generating thumbnails. This reduces the shot-to-shot time.
    private ImageSaver mImageSaver;

    private CameraSound mCameraSound;

    private String mStorage;

    private Runnable mDoSnapRunnable = new Runnable() {
        public void run() {
            onShutterButtonClick();
        }
    };

    private final StringBuilder mBuilder = new StringBuilder();
    private final Formatter mFormatter = new Formatter(mBuilder);
    private final Object[] mFormatterArgs = new Object[1];

    /**
     * An unpublished intent flag requesting to return as soon as capturing
     * is completed.
     *
     * TODO: consider publishing by moving into MediaStore.
     */
    private static final String EXTRA_QUICK_CAPTURE =
            "android.intent.extra.quickCapture";

    // The display rotation in degrees. This is only valid when mCameraState is
    // not PREVIEW_STOPPED.
    private int mDisplayRotation;
    // The value for android.hardware.Camera.setDisplayOrientation.
    private int mDisplayOrientation;
    private boolean mPausing;
    private boolean mFirstTimeInitialized;
    private boolean mIsImageCaptureIntent;

    private static final int PREVIEW_STOPPED = 0;
    private static final int IDLE = 1;  // preview is active
    // Focus is in progress. The exact focus state is in Focus.java.
    private static final int FOCUSING = 2;
    private static final int SNAPSHOT_IN_PROGRESS = 3;
    private int mCameraState = PREVIEW_STOPPED;
    private boolean mSnapshotOnIdle = false;
    private boolean mAspectRatioChanged = false;

    private ContentResolver mContentResolver;
    private boolean mDidRegister = false;

    private LocationManager mLocationManager;

    private final ShutterCallback mShutterCallback = new ShutterCallback();
    private final PostViewPictureCallback mPostViewPictureCallback =
            new PostViewPictureCallback();
    private final RawPictureCallback mRawPictureCallback =
            new RawPictureCallback();
    private final AutoFocusCallback mAutoFocusCallback =
            new AutoFocusCallback();
    private final ZoomListener mZoomListener = new ZoomListener();
    private final CameraErrorCb mErrorCallback = new CameraErrorCb();

    private long mFocusStartTime;
    private long mCaptureStartTime;
    private long mShutterCallbackTime;
    private long mPostViewPictureCallbackTime;
    private long mRawPictureCallbackTime;
    private long mJpegPictureCallbackTime;
    private long mOnResumeTime;
    private long mPicturesRemaining;
    private byte[] mJpegImageData;

    // These latency time are for the CameraLatency test.
    public long mAutoFocusTime;
    public long mShutterLag;
    public long mShutterToPictureDisplayedTime;
    public long mPictureDisplayedToJpegCallbackTime;
    public long mJpegCallbackFinishTime;

    // This handles everything about focus.
    private FocusManager mFocusManager;
    private String mSceneMode;
    private Toast mNotSelectableToast;
    private Toast mNoShareToast;

    private final Handler mHandler = new MainHandler();
    private IndicatorControlContainer mIndicatorControlContainer;
    private PreferenceGroup mPreferenceGroup;

    // Camera timer.
    private boolean mTimerMode = false;
    private TextView mRecordingTimeView;
    private RotateLayout mRecordingTimeRect;

    // Burst mode
    private int mBurstShotsDone = 0;
    private boolean mBurstShotInProgress = false;

    // multiple cameras support
    private int mNumberOfCameras;
    private int mCameraId;
    private int mFrontCameraId;
    private int mBackCameraId;

    private boolean mQuickCapture;

    private int mSnapshotMode;
    private boolean zslrestartPreview;

    /**
     * This Handler is used to post message back onto the main thread of the
     * application
     */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CLEAR_SCREEN_DELAY: {
                    getWindow().clearFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                }

                case FIRST_TIME_INIT: {
                    initializeFirstTime();
                    break;
                }

                case SET_CAMERA_PARAMETERS_WHEN_IDLE: {
                    setCameraParametersWhenIdle(0);
                    break;
                }

                case CHECK_DISPLAY_ROTATION: {
                    // Set the display orientation if display rotation has changed.
                    // Sometimes this happens when the device is held upside
                    // down and camera app is opened. Rotation animation will
                    // take some time and the rotation value we have got may be
                    // wrong. Framework does not have a callback for this now.
                    if (Util.getDisplayRotation(Camera.this) != mDisplayRotation) {
                        setDisplayOrientation();
                    }
                    if (SystemClock.uptimeMillis() - mOnResumeTime < 5000) {
                        mHandler.sendEmptyMessageDelayed(CHECK_DISPLAY_ROTATION, 100);
                    }
                    break;
                }

                case SHOW_TAP_TO_FOCUS_TOAST: {
                    showTapToFocusToast();
                    break;
                }

                case UPDATE_THUMBNAIL: {
                    mImageSaver.updateThumbnail();
                    break;
                }

                case FINISH_PINCH_TO_ZOOM: {
                    mStartZoom = false;
                    break;
                }

                case START_TOUCH_TO_FOCUS: {
                    if (msg.obj != null) {
                        mFocusManager.onTouch((MotionEvent)(msg.obj));
                    }
                    break;
                }
                case CAMERA_TIMER: {
                    updateTimer(msg.arg1);
                    break;
                }
            }
        }
    }

    private void stopCamera() {

        stopPreview();
        // Close the camera now because other activities may need to use it.
        closeCamera();
        if (mCameraSound != null)
            mCameraSound.release();
        resetScreenOn();

        // Clear UI.
        collapseCameraControls();
        if (mSharePopup != null)
            mSharePopup.dismiss();
        if (mFaceView != null)
            mFaceView.clear();

        if (mFirstTimeInitialized) {
            mOrientationListener.disable();
            if (mImageSaver != null) {
                mImageSaver.finish();
                mImageSaver = null;
            }
            if (!mIsImageCaptureIntent && mThumbnail != null && !mThumbnail.fromFile()) {
                mThumbnail.saveTo(new File(getFilesDir(), Thumbnail.LAST_THUMB_FILENAME));
            }
        }

        if (mDidRegister) {
            unregisterReceiver(mReceiver);
            mDidRegister = false;
        }
        if (mLocationManager != null)
            mLocationManager.recordLocation(false);
        updateExposureOnScreenIndicator(0);

        if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }

        // If we are in an image capture intent and has taken
        // a picture, we just clear it in onPause.
        mJpegImageData = null;

        // Remove the messages in the event queue.
        mHandler.removeMessages(FIRST_TIME_INIT);
        mHandler.removeMessages(CHECK_DISPLAY_ROTATION);
        mFocusManager.removeMessages();
    }

    private class CameraErrorCb implements android.hardware.Camera.ErrorCallback {
        private static final String TAG = "CameraErrorCallback";

        public void onError(int error, android.hardware.Camera camera) {
            Log.e(TAG, "Got camera error callback. error=" + error);
            if (error == android.hardware.Camera.CAMERA_ERROR_SERVER_DIED) {
                // We are not sure about the current state of the app (in
                // preview or
                // snapshot or recording). Closing the app is better than
                // creating a
                // new Camera object.
                throw new RuntimeException("Media server died.");
            } else if (error == android.hardware.Camera.CAMERA_ERROR_UNKNOWN) {
                stopCamera();
                Util.showErrorAndFinish(Camera.this, R.string.cannot_connect_camera);
            }
        }
    }

    private void resetExposureCompensation() {
        String value = mPreferences.getString(CameraSettings.KEY_EXPOSURE,
                CameraSettings.EXPOSURE_DEFAULT_VALUE);
        if (!CameraSettings.EXPOSURE_DEFAULT_VALUE.equals(value)) {
            Editor editor = mPreferences.edit();
            editor.putString(CameraSettings.KEY_EXPOSURE, "0");
            editor.apply();
            if (mIndicatorControlContainer != null) {
                mIndicatorControlContainer.reloadPreferences();
            }
        }
    }

    private void keepMediaProviderInstance() {
        // We want to keep a reference to MediaProvider in camera's lifecycle.
        // TODO: Utilize mMediaProviderClient instance to replace
        // ContentResolver calls.
        if (mMediaProviderClient == null) {
            mMediaProviderClient = getContentResolver()
                    .acquireContentProviderClient(MediaStore.AUTHORITY);
        }
    }

    // Snapshots can only be taken after this is called. It should be called
    // once only. We could have done these things in onCreate() but we want to
    // make preview screen appear as soon as possible.
    private void initializeFirstTime() {
        if (mFirstTimeInitialized) return;

        // Create orientation listenter. This should be done first because it
        // takes some time to get first orientation.
        mOrientationListener = new MyOrientationEventListener(Camera.this);
        mOrientationListener.enable();

        // Initialize location sevice.
        boolean recordLocation = RecordLocationPreference.get(
                mPreferences, getContentResolver());
        initOnScreenIndicator();
        mLocationManager.recordLocation(recordLocation);

        keepMediaProviderInstance();
        checkStorage();

        // Initialize last picture button.
        mContentResolver = getContentResolver();
        if (!mIsImageCaptureIntent) {  // no thumbnail in image capture intent
            initThumbnailButton();
        }

        // Initialize shutter button.
        mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        mShutterButton.setOnShutterButtonListener(this);
        mShutterButton.setVisibility(View.VISIBLE);

        // Initialize focus UI.
        mPreviewFrame = findViewById(R.id.camera_preview);
        mPreviewFrame.setOnTouchListener(this);
        mFocusAreaIndicator = (RotateLayout) findViewById(R.id.focus_indicator_rotate_layout);
        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
        boolean mirror = (info.facing == CameraInfo.CAMERA_FACING_FRONT);
        mFocusManager.initialize(mFocusAreaIndicator, mPreviewFrame, mFaceView, this,
                mirror, mDisplayOrientation);
        mImageSaver = new ImageSaver();
        Util.initializeScreenBrightness(getWindow(), getContentResolver());
        installIntentFilter();
        initializeZoom();
        updateOnScreenIndicators();
        startFaceDetection();
        // Show the tap to focus toast if this is the first start.
        if (mFocusAreaSupported &&
                mPreferences.getBoolean(CameraSettings.KEY_CAMERA_FIRST_USE_HINT_SHOWN, true)) {
            // Delay the toast for one second to wait for orientation.
            mHandler.sendEmptyMessageDelayed(SHOW_TAP_TO_FOCUS_TOAST, 1000);
        }

        mFirstTimeInitialized = true;
        addIdleHandler();
    }

    private void addIdleHandler() {
        MessageQueue queue = Looper.myQueue();
        queue.addIdleHandler(new MessageQueue.IdleHandler() {
            public boolean queueIdle() {
                Storage.ensureOSXCompatible(mStorage);
                return false;
            }
        });
    }

    private void initThumbnailButton() {
        // Load the thumbnail from the disk.
        mThumbnail = Thumbnail.loadFrom(new File(getFilesDir(), Thumbnail.LAST_THUMB_FILENAME));
        updateThumbnailButton();
    }

    private void updateThumbnailButton() {
        // Update last image if URI is invalid and the storage is ready.
        if ((mThumbnail == null || !Util.isUriValid(mThumbnail.getUri(), mContentResolver))
                && mPicturesRemaining >= 0) {
            mThumbnail = Thumbnail.getLastThumbnail(mContentResolver,
                Storage.generateBucketId(mStorage));
        }
        if (mThumbnail != null) {
            mThumbnailView.setBitmap(mThumbnail.getBitmap());
        } else {
            mThumbnailView.setBitmap(null);
        }
    }

    // If the activity is paused and resumed, this method will be called in
    // onResume.
    private void initializeSecondTime() {
        // Start orientation listener as soon as possible because it takes
        // some time to get first orientation.
        mOrientationListener.enable();

        // Start location update if needed.
        boolean recordLocation = RecordLocationPreference.get(
                mPreferences, getContentResolver());
        mLocationManager.recordLocation(recordLocation);

        installIntentFilter();
        mImageSaver = new ImageSaver();
        initializeZoom();
        keepMediaProviderInstance();
        checkStorage();
        hidePostCaptureAlert();

        if (!mIsImageCaptureIntent) {
            updateThumbnailButton();
            mModePicker.setCurrentMode(ModePicker.MODE_CAMERA);
        }
    }

    private class ZoomChangeListener implements ZoomControl.OnZoomChangedListener {
        // only for immediate zoom
        @Override
        public void onZoomValueChanged(int index) {
            Camera.this.onZoomValueChanged(index);
        }

        // only for smooth zoom
        @Override
        public void onZoomStateChanged(int state) {
            if (mPausing) return;

            Log.v(TAG, "zoom picker state=" + state);
            if (state == ZoomControl.ZOOM_IN) {
                Camera.this.onZoomValueChanged(mZoomMax);
            } else if (state == ZoomControl.ZOOM_OUT) {
                Camera.this.onZoomValueChanged(0);
            } else {
                mTargetZoomValue = -1;
                if (mZoomState == ZOOM_START) {
                    mZoomState = ZOOM_STOPPING;
                    mCameraDevice.stopSmoothZoom();
                }
            }
        }
    }

    private void initializeZoom() {
        // Get the parameter to make sure we have the up-to-date zoom value.
        mParameters = mCameraDevice.getParameters();
        if (!mParameters.isZoomSupported()) return;
        mZoomMax = mParameters.getMaxZoom();
        // Currently we use immediate zoom for fast zooming to get better UX and
        // there is no plan to take advantage of the smooth zoom.
        mZoomControl.setZoomMax(mZoomMax);
        mZoomControl.setZoomIndex(mParameters.getZoom());
        mZoomControl.setSmoothZoomSupported(mSmoothZoomSupported);
        mZoomControl.setOnZoomChangeListener(new ZoomChangeListener());
        mCameraDevice.setZoomChangeListener(mZoomListener);
    }

    private void onZoomValueChanged(int index) {
        // Not useful to change zoom value when the activity is paused.
        if (mPausing) return;

        if (mSmoothZoomSupported) {
            if (mTargetZoomValue != index && mZoomState != ZOOM_STOPPED) {
                mTargetZoomValue = index;
                if (mZoomState == ZOOM_START) {
                    mZoomState = ZOOM_STOPPING;
                    mCameraDevice.stopSmoothZoom();
                }
            } else if (mZoomState == ZOOM_STOPPED && mZoomValue != index) {
                mTargetZoomValue = index;
                mCameraDevice.startSmoothZoom(index);
                mZoomState = ZOOM_START;
            }
        } else {
            mZoomValue = index;
            setCameraParametersWhenIdle(UPDATE_PARAM_ZOOM);
        }
    }

    @Override
    public void startFaceDetection() {
        // Workaround for a buggy camera library
        if (Util.noFaceDetectOnFrontCamera() &&
                (CameraHolder.instance().getCameraInfo()[mCameraId].facing == CameraInfo.CAMERA_FACING_FRONT)) {
            return;
        }
        if (Util.noFaceDetectOnRearCamera() &&
                (CameraHolder.instance().getCameraInfo()[mCameraId].facing == CameraInfo.CAMERA_FACING_BACK)) {
            return;
        }
        if (mFaceDetectionStarted || mCameraState != IDLE) return;
        if (mParameters.getMaxNumDetectedFaces() > 0) {
            mFaceDetectionStarted = true;
            mFaceView = (FaceView) findViewById(R.id.face_view);
            mFaceView.clear();
            mFaceView.setVisibility(View.VISIBLE);
            mFaceView.setDisplayOrientation(mDisplayOrientation);
            CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
            mFaceView.setMirror(info.facing == CameraInfo.CAMERA_FACING_FRONT);
            mFaceView.resume();
            mCameraDevice.setFaceDetectionListener(this);
            mCameraDevice.startFaceDetection();
        }
    }

    @Override
    public void stopFaceDetection() {
        if (!mFaceDetectionStarted) return;
        if (mParameters.getMaxNumDetectedFaces() > 0) {
            mFaceDetectionStarted = false;
            mCameraDevice.setFaceDetectionListener(null);
            mCameraDevice.stopFaceDetection();
            if (mFaceView != null) mFaceView.clear();
        }
    }

    private class PopupGestureListener
            extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            // Check if the popup window is visible.
            View popup = mIndicatorControlContainer.getActiveSettingPopup();
            if (popup == null) return false;


            // Let popup window, indicator control or preview frame handle the
            // event by themselves. Dismiss the popup window if users touch on
            // other areas.
            if (!Util.pointInView(e.getX(), e.getY(), popup)
                    && !Util.pointInView(e.getX(), e.getY(), mIndicatorControlContainer)
                    && !Util.pointInView(e.getX(), e.getY(), mPreviewFrame)) {
                mIndicatorControlContainer.dismissSettingPopup();
                // Let event fall through.
            }
            return false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent m) {
        // Check if the popup window should be dismissed first.
        if (mPopupGestureDetector != null && mPopupGestureDetector.onTouchEvent(m)) {
            return true;
        }

        return super.dispatchTouchEvent(m);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received intent action=" + action);
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_CHECKING)) {
                checkStorage();
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                checkStorage();
                if (!mIsImageCaptureIntent) {
                    updateThumbnailButton();
                }
            }
        }
    };

    private void initOnScreenIndicator() {
        mGpsIndicator = (ImageView) findViewById(R.id.onscreen_gps_indicator);
        mExposureIndicator = (TextView) findViewById(R.id.onscreen_exposure_indicator);
        mFlashIndicator = (ImageView) findViewById(R.id.onscreen_flash_indicator);
        mTimerIndicator = (ImageView) findViewById(R.id.onscreen_timer_indicator);
        mSceneIndicator = (ImageView) findViewById(R.id.onscreen_scene_indicator);
        mWhiteBalanceIndicator =
                (ImageView) findViewById(R.id.onscreen_white_balance_indicator);
        mFocusIndicator = (ImageView) findViewById(R.id.onscreen_focus_indicator);
    }

    @Override
    public void showGpsOnScreenIndicator(boolean hasSignal) {
        if (mGpsIndicator == null) {
            return;
        }
        if (hasSignal) {
            mGpsIndicator.setImageResource(R.drawable.ic_viewfinder_gps_on);
        } else {
            mGpsIndicator.setImageResource(R.drawable.ic_viewfinder_gps_no_signal);
        }
        mGpsIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideGpsOnScreenIndicator() {
        if (mGpsIndicator == null) {
            return;
        }
        mGpsIndicator.setVisibility(View.GONE);
    }

    private void updateExposureOnScreenIndicator(int value) {
        if (mExposureIndicator == null) {
            return;
        }
        if (value == 0) {
            mExposureIndicator.setText("");
            mExposureIndicator.setVisibility(View.GONE);
        } else {
            float step = mParameters.getExposureCompensationStep();
            mFormatterArgs[0] = value * step;
            mBuilder.delete(0, mBuilder.length());
            mFormatter.format("%+1.1f", mFormatterArgs);
            String exposure = mFormatter.toString();
            mExposureIndicator.setText(exposure);
            mExposureIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void updateFlashOnScreenIndicator(String value) {
        if (mFlashIndicator == null) {
            return;
        }
        mFlashIndicator.setVisibility(View.VISIBLE);
        if (Parameters.FLASH_MODE_AUTO.equals(value)) {
            mFlashIndicator.setImageResource(R.drawable.ic_indicators_landscape_flash_auto);
        } else if (Parameters.FLASH_MODE_ON.equals(value)) {
            mFlashIndicator.setImageResource(R.drawable.ic_indicators_landscape_flash_on);
        } else if (Parameters.FLASH_MODE_OFF.equals(value)) {
            mFlashIndicator.setImageResource(R.drawable.ic_indicators_landscape_flash_off);
        }
    }

    private void updateTimerOnScreenIndicator() {
        if (mTimerIndicator == null) {
            return;
        }
        if (mCaptureMode == 0) {
            mTimerIndicator.setVisibility(View.GONE);
        } else {
            mTimerIndicator.setImageResource(R.drawable.ic_indicators_timer);
            mTimerIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void updateSceneOnScreenIndicator(boolean isVisible) {
        if (mSceneIndicator == null) {
            return;
        }
        mSceneIndicator.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    private void updateWhiteBalanceOnScreenIndicator(String value) {
        if (mWhiteBalanceIndicator == null) {
            return;
        }
        if (Parameters.WHITE_BALANCE_AUTO.equals(value)) {
            mWhiteBalanceIndicator.setVisibility(View.GONE);
        } else {
            if (Parameters.WHITE_BALANCE_FLUORESCENT.equals(value)) {
                mWhiteBalanceIndicator.setImageResource(R.drawable.ic_indicators_fluorescent);
            } else if (Parameters.WHITE_BALANCE_INCANDESCENT.equals(value)) {
                mWhiteBalanceIndicator.setImageResource(R.drawable.ic_indicators_incandescent);
            } else if (Parameters.WHITE_BALANCE_DAYLIGHT.equals(value)) {
                mWhiteBalanceIndicator.setImageResource(R.drawable.ic_indicators_sunlight);
            } else if (Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT.equals(value)) {
                mWhiteBalanceIndicator.setImageResource(R.drawable.ic_indicators_cloudy);
            }
            mWhiteBalanceIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void updateFocusOnScreenIndicator(String value) {
        if (mFocusIndicator == null) {
            return;
        }
        if (Parameters.FOCUS_MODE_INFINITY.equals(value)) {
            mFocusIndicator.setImageResource(R.drawable.ic_indicators_landscape);
            mFocusIndicator.setVisibility(View.VISIBLE);
        } else if (Parameters.FOCUS_MODE_MACRO.equals(value)) {
            mFocusIndicator.setImageResource(R.drawable.ic_indicators_macro);
            mFocusIndicator.setVisibility(View.VISIBLE);
        } else {
            mFocusIndicator.setVisibility(View.GONE);
        }
    }

    private void updateOnScreenIndicators() {
        boolean isAutoScene = (Parameters.SCENE_MODE_AUTO.equals(mParameters.getSceneMode()) || Parameters.SCENE_MODE_OFF.equals(mParameters.getSceneMode()));
        updateSceneOnScreenIndicator(!isAutoScene);
        updateExposureOnScreenIndicator(CameraSettings.readExposure(mPreferences));
        updateFlashOnScreenIndicator(mParameters.getFlashMode());
        updateTimerOnScreenIndicator();
        updateWhiteBalanceOnScreenIndicator(mParameters.getWhiteBalance());
        updateFocusOnScreenIndicator(mParameters.getFocusMode());
    }
    private final class ShutterCallback
            implements android.hardware.Camera.ShutterCallback {
        public void onShutter() {
            mShutterCallbackTime = System.currentTimeMillis();
            mShutterLag = mShutterCallbackTime - mCaptureStartTime;
            Log.v(TAG, "mShutterLag = " + mShutterLag + "ms");
            mFocusManager.onShutter();
        }
    }

    private final class PostViewPictureCallback implements PictureCallback {
        public void onPictureTaken(
                byte [] data, android.hardware.Camera camera) {
            mPostViewPictureCallbackTime = System.currentTimeMillis();
            Log.v(TAG, "mShutterToPostViewCallbackTime = "
                    + (mPostViewPictureCallbackTime - mShutterCallbackTime)
                    + "ms");
        }
    }

    private final class RawPictureCallback implements PictureCallback {
        public void onPictureTaken(
                byte [] rawData, android.hardware.Camera camera) {
            mRawPictureCallbackTime = System.currentTimeMillis();
            Log.v(TAG, "mShutterToRawCallbackTime = "
                    + (mRawPictureCallbackTime - mShutterCallbackTime) + "ms");
        }
    }

    private final class JpegPictureCallback implements PictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            mLocation = loc;
        }

        public void onPictureTaken(
                final byte [] jpegData, final android.hardware.Camera camera) {
            if (mPausing) {
                return;
            }

            if (getResources().getBoolean(R.bool.restartPreviewOnPictureTaken)) {
                // If preview is running, restart it
                if (mCameraState != PREVIEW_STOPPED) {
                    stopPreview();
                    startPreview();
                }
            }

            mJpegPictureCallbackTime = System.currentTimeMillis();
            // If postview callback has arrived, the captured image is displayed
            // in postview callback. If not, the captured image is displayed in
            // raw picture callback.
            if (mPostViewPictureCallbackTime != 0) {
                mShutterToPictureDisplayedTime =
                        mPostViewPictureCallbackTime - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime =
                        mJpegPictureCallbackTime - mPostViewPictureCallbackTime;
            } else {
                mShutterToPictureDisplayedTime =
                        mRawPictureCallbackTime - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime =
                        mJpegPictureCallbackTime - mRawPictureCallbackTime;
            }
            Log.v(TAG, "mPictureDisplayedToJpegCallbackTime = "
                    + mPictureDisplayedToJpegCallbackTime + "ms");

            if (!mIsImageCaptureIntent && mSnapshotMode != CameraInfo.CAMERA_SUPPORT_MODE_ZSL) {
                startPreview();
                startFaceDetection();
            } else {
                setCameraState(IDLE);
            }

            if (!mIsImageCaptureIntent) {
                Size s = mParameters.getPictureSize();
                mImageSaver.addImage(jpegData, mLocation, s.width, s.height);
            } else {
                mJpegImageData = jpegData;
                if (!mQuickCapture) {
                    showPostCaptureAlert();
                } else {
                    doAttach();
                }
            }

            // Check this in advance of each shot so we don't add to shutter
            // latency. It's true that someone else could write to the SD card in
            // the mean time and fill it, but that could have happened between the
            // shutter press and saving the JPEG too.
            checkStorage();

            long now = System.currentTimeMillis();
            mJpegCallbackFinishTime = now - mJpegPictureCallbackTime;
            Log.v(TAG, "mJpegCallbackFinishTime = "
                    + mJpegCallbackFinishTime + "ms");
            mJpegPictureCallbackTime = 0;

            if (mSnapshotOnIdle && mBurstShotsDone > 0) {
                mHandler.post(mDoSnapRunnable);
            }
        }
    }

    private final class AutoFocusCallback
            implements android.hardware.Camera.AutoFocusCallback {
        public void onAutoFocus(
                boolean focused, android.hardware.Camera camera) {
            if (mPausing) return;

            mAutoFocusTime = System.currentTimeMillis() - mFocusStartTime;
            Log.v(TAG, "mAutoFocusTime = " + mAutoFocusTime + "ms");
            setCameraState(IDLE);
            mFocusManager.onAutoFocus(focused);
        }
    }

    private final class ZoomListener
            implements android.hardware.Camera.OnZoomChangeListener {
        @Override
        public void onZoomChange(
                int value, boolean stopped, android.hardware.Camera camera) {
            Log.v(TAG, "Zoom changed: value=" + value + ". stopped=" + stopped);
            mZoomValue = value;

            // Update the UI when we get zoom value.
            mZoomControl.setZoomIndex(value);

            // Keep mParameters up to date. We do not getParameter again in
            // takePicture. If we do not do this, wrong zoom value will be set.
            mParameters.setZoom(value);

            if (stopped && mZoomState != ZOOM_STOPPED) {
                if (mTargetZoomValue != -1 && value != mTargetZoomValue) {
                    mCameraDevice.startSmoothZoom(mTargetZoomValue);
                    mZoomState = ZOOM_START;
                } else {
                    mZoomState = ZOOM_STOPPED;
                }
            }
        }
    }

    // Each SaveRequest remembers the data needed to save an image.
    private static class SaveRequest {
        byte[] data;
        Location loc;
        int width, height;
        long dateTaken;
        int previewWidth;
    }

    // We use a queue to store the SaveRequests that have not been completed
    // yet. The main thread puts the request into the queue. The saver thread
    // gets it from the queue, does the work, and removes it from the queue.
    //
    // There are several cases the main thread needs to wait for the saver
    // thread to finish all the work in the queue:
    // (1) When the activity's onPause() is called, we need to finish all the
    // work, so other programs (like Gallery) can see all the images.
    // (2) When we need to show the SharePop, we need to finish all the work
    // too, because we want to show the thumbnail of the last image taken.
    //
    // If the queue becomes too long, adding a new request will block the main
    // thread until the queue length drops below the threshold (QUEUE_LIMIT).
    // If we don't do this, we may face several problems: (1) We may OOM
    // because we are holding all the jpeg data in memory. (2) We may ANR
    // when we need to wait for saver thread finishing all the work (in
    // onPause() or showSharePopup()) because the time to finishing a long queue
    // of work may be too long.
    private class ImageSaver extends Thread {
        private static final int QUEUE_LIMIT = 3;

        private ArrayList<SaveRequest> mQueue;
        private Thumbnail mPendingThumbnail;
        private Object mUpdateThumbnailLock = new Object();
        private boolean mStop;

        // Runs in main thread
        public ImageSaver() {
            mQueue = new ArrayList<SaveRequest>();
            start();
        }

        // Runs in main thread
        public void addImage(final byte[] data, Location loc, int width,
                int height) {
            SaveRequest r = new SaveRequest();
            r.data = data;
            r.loc = (loc == null) ? null : new Location(loc);  // make a copy
            r.width = width;
            r.height = height;
            r.dateTaken = System.currentTimeMillis();
            if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                r.previewWidth = mPreviewFrameLayout.getHeight();
            } else {
                r.previewWidth = mPreviewFrameLayout.getWidth();
            }
            synchronized (this) {
                while (mQueue.size() >= QUEUE_LIMIT) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // ignore.
                    }
                }
                mQueue.add(r);
                notifyAll();  // Tell saver thread there is new work to do.
            }
        }

        // Runs in saver thread
        @Override
        public void run() {
            while (true) {
                SaveRequest r;
                synchronized (this) {
                    if (mQueue.isEmpty()) {
                        notifyAll();  // notify main thread in waitDone

                        // Note that we can only stop after we saved all images
                        // in the queue.
                        if (mStop) break;

                        try {
                            wait();
                        } catch (InterruptedException ex) {
                            // ignore.
                        }
                        continue;
                    }
                    r = mQueue.get(0);
                }
                storeImage(r.data, r.loc, r.width, r.height, r.dateTaken,
                        r.previewWidth);
                synchronized(this) {
                    mQueue.remove(0);
                    notifyAll();  // the main thread may wait in addImage
                }
            }
        }

        // Runs in main thread
        public void waitDone() {
            synchronized (this) {
                while (!mQueue.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // ignore.
                    }
                }
            }
            updateThumbnail();
        }

        // Runs in main thread
        public void finish() {
            waitDone();
            synchronized (this) {
                mStop = true;
                notifyAll();
            }
            try {
                join();
            } catch (InterruptedException ex) {
                // ignore.
            }
        }

        // Runs in main thread (because we need to update mThumbnailView in the
        // main thread)
        public void updateThumbnail() {
            Thumbnail t;
            synchronized (mUpdateThumbnailLock) {
                mHandler.removeMessages(UPDATE_THUMBNAIL);
                t = mPendingThumbnail;
                mPendingThumbnail = null;
            }

            if (t != null) {
                mThumbnail = t;
                mThumbnailView.setBitmap(mThumbnail.getBitmap());
            }
            // Share popup may still have the reference to the old thumbnail. Clear it.
            mSharePopup = null;
        }

        // Runs in saver thread
        private void storeImage(final byte[] data, Location loc, int width,
                int height, long dateTaken, int previewWidth) {
            String title = Util.createJpegName(dateTaken);
            int orientation = Exif.getOrientation(data);
            Uri uri = Storage.addImage(mContentResolver, mStorage, title, dateTaken,
                    loc, orientation, data, width, height);
            if (uri != null) {
                boolean needThumbnail;
                synchronized (this) {
                    // If the number of requests in the queue (include the
                    // current one) is greater than 1, we don't need to generate
                    // thumbnail for this image. Because we'll soon replace it
                    // with the thumbnail for some image later in the queue.
                    needThumbnail = (mQueue.size() <= 1);
                }
                if (needThumbnail) {
                    // Create a thumbnail whose width is equal or bigger than
                    // that of the preview.
                    int ratio = (int) Math.ceil((double) width / previewWidth);
                    int inSampleSize = Integer.highestOneBit(ratio);
                    Thumbnail t = Thumbnail.createThumbnail(
                                data, orientation, inSampleSize, uri);
                    synchronized (mUpdateThumbnailLock) {
                        // We need to update the thumbnail in the main thread,
                        // so send a message to run updateThumbnail().
                        mPendingThumbnail = t;
                        mHandler.sendEmptyMessage(UPDATE_THUMBNAIL);
                    }
                }
                Util.broadcastNewPicture(Camera.this, uri);
            }
        }
    }

    private void setCameraState(int state) {
        mCameraState = state;
        switch (state) {
            case SNAPSHOT_IN_PROGRESS:
            case FOCUSING:
                enableCameraControls(false);
                break;
            case IDLE:
            case PREVIEW_STOPPED:
                enableCameraControls(true);
                break;
        }
    }

    @Override
    public boolean capture() {
        // If we are already in the middle of taking a snapshot then ignore.
        if (mCameraState == SNAPSHOT_IN_PROGRESS || mCameraDevice == null) {
            return false;
        }
        mCaptureStartTime = System.currentTimeMillis();
        mPostViewPictureCallbackTime = 0;
        mJpegImageData = null;

        // Set rotation and gps data.
        Util.setRotationParameter(mParameters, mCameraId, mOrientation);
        Location loc = mLocationManager.getCurrentLocation();
        Util.setGpsParameters(mParameters, loc);
        mCameraDevice.setParameters(mParameters);

        mCameraDevice.takePicture(mShutterCallback, mRawPictureCallback,
                mPostViewPictureCallback, new JpegPictureCallback(loc));
        if (mSnapshotMode == CameraInfo.CAMERA_SUPPORT_MODE_ZSL) {
            zslrestartPreview = false;
        }
        mFaceDetectionStarted = false;
        setCameraState(SNAPSHOT_IN_PROGRESS);

        return true;
    }

    @Override
    public void setFocusParameters() {
        setCameraParameters(UPDATE_PARAM_PREFERENCE);
    }

    @Override
    public void playSound(int soundId) {
        if(focusSound(mPreferences)) {
            mCameraSound.playSound(soundId);
        }
    }

    private boolean saveDataToFile(String filePath, byte[] data) {
        FileOutputStream f = null;
        try {
            f = new FileOutputStream(filePath);
            f.write(data);
        } catch (IOException e) {
            return false;
        } finally {
            Util.closeSilently(f);
        }
        return true;
    }

    private void getPreferredCameraId() {
        mPreferences = new ComboPreferences(this);
        CameraSettings.upgradeGlobalPreferences(mPreferences.getGlobal());
        mCameraId = CameraSettings.readPreferredCameraId(mPreferences);

        // Testing purpose. Launch a specific camera through the intent extras.
        int intentCameraId = Util.getCameraFacingIntentExtras(this);
        if (intentCameraId != -1) {
            mCameraId = intentCameraId;
        }
    }

    Thread mCameraOpenThread = new Thread(new Runnable() {
        public void run() {
            try {
                mCameraDevice = Util.openCamera(Camera.this, mCameraId);
            } catch (CameraHardwareException e) {
                mOpenCameraFail = true;
            } catch (CameraDisabledException e) {
                mCameraDisabled = true;
            }
        }
    });

    Thread mCameraPreviewThread = new Thread(new Runnable() {
        public void run() {
            initializeCapabilities();
            startPreview();
        }
    });

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getPreferredCameraId();
        mStorage = CameraSettings.readStorage(mPreferences);
        powerShutter(mPreferences);
        String[] defaultFocusModes = getResources().getStringArray(
                R.array.pref_camera_focusmode_default_array);
        mFocusManager = new FocusManager(mPreferences, defaultFocusModes);

        /*
         * To reduce startup time, we start the camera open and preview threads.
         * We make sure the preview is started at the end of onCreate.
         */
        mCameraOpenThread.start();

        mIsImageCaptureIntent = isImageCaptureIntent();
        setContentView(R.layout.camera);
        if (mIsImageCaptureIntent) {
            mReviewDoneButton = (Rotatable) findViewById(R.id.btn_done);
            mReviewCancelButton = (Rotatable) findViewById(R.id.btn_cancel);
            findViewById(R.id.btn_cancel).setVisibility(View.VISIBLE);
        } else {
            mThumbnailView = (RotateImageView) findViewById(R.id.thumbnail);
            mThumbnailView.enableFilter(false);
            mThumbnailView.setVisibility(View.VISIBLE);
        }
        mRecordingTimeView = (TextView) findViewById(R.id.recording_time);
        mRecordingTimeRect = (RotateLayout) findViewById(R.id.recording_time_rect);
        mRotateDialog = new RotateDialogController(this, R.layout.rotate_dialog);

        mPreferences.setLocalId(this, mCameraId);
        CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());

        mNumberOfCameras = CameraHolder.instance().getNumberOfCameras();
        mQuickCapture = getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);

        // we need to reset exposure for the preview
        resetExposureCompensation();

        Util.enterLightsOutMode(getWindow());

        // don't set mSurfaceHolder here. We have it set ONLY within
        // surfaceChanged / surfaceDestroyed, other parts of the code
        // assume that when it is set, the surface is also set.
        SurfaceView preview = (SurfaceView) findViewById(R.id.camera_preview);
        SurfaceHolder holder = preview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Make sure camera device is opened.
        try {
            mCameraOpenThread.join();
            mCameraOpenThread = null;
            if (mOpenCameraFail) {
                Util.showErrorAndFinish(this, R.string.cannot_connect_camera);
                return;
            } else if (mCameraDisabled) {
                Util.showErrorAndFinish(this, R.string.camera_disabled);
                return;
            }
        } catch (InterruptedException ex) {
            // ignore
        }
        mCameraPreviewThread.start();

        if (mIsImageCaptureIntent) {
            setupCaptureParams();
        } else {
            mModePicker = (ModePicker) findViewById(R.id.mode_picker);
            mModePicker.setVisibility(View.VISIBLE);
            mModePicker.setOnModeChangeListener(this);
            mModePicker.setCurrentMode(ModePicker.MODE_CAMERA);
        }

        mZoomControl = (ZoomControl) findViewById(R.id.zoom_control);
        mOnScreenIndicators = (Rotatable) findViewById(R.id.on_screen_indicators);
        mLocationManager = new LocationManager(this, this);

        mBackCameraId = CameraHolder.instance().getBackCameraId();
        mFrontCameraId = CameraHolder.instance().getFrontCameraId();

        // Wait until the camera settings are retrieved.
        synchronized (mCameraPreviewThread) {
            try {
                mCameraPreviewThread.wait();
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        // Do this after starting preview because it depends on camera
        // parameters.
        initializeIndicatorControl();
        mCameraSound = new CameraSound();

        // Make sure preview is started.
        try {
            mCameraPreviewThread.join();
        } catch (InterruptedException ex) {
            // ignore
        }
        mCameraPreviewThread = null;
    }

    private void overrideCameraSettings(final String flashMode,
            final String whiteBalance, final String focusMode) {
        if (mIndicatorControlContainer != null) {
            mIndicatorControlContainer.overrideSettings(
                    CameraSettings.KEY_FLASH_MODE, flashMode,
                    CameraSettings.KEY_WHITE_BALANCE, whiteBalance,
                    CameraSettings.KEY_FOCUS_MODE, focusMode);
        }
    }

    private void updateSceneModeUI() {
        // If scene mode is set, we cannot set flash mode, white balance, and
        // focus mode, instead, we read it from driver
        if (Parameters.SCENE_MODE_AUTO.equals(mSceneMode) || Parameters.SCENE_MODE_OFF.equals(mSceneMode)) {
            overrideCameraSettings(null, null, null);
        } else {
            overrideCameraSettings(mParameters.getFlashMode(),
                    mParameters.getWhiteBalance(), mParameters.getFocusMode());
        }
    }

    private void loadCameraPreferences() {
        CameraSettings settings = new CameraSettings(this, mInitialParams,
                mCameraId, CameraHolder.instance().getCameraInfo());
        mPreferenceGroup = settings.getPreferenceGroup(R.xml.camera_preferences);
    }

    private void initializeIndicatorControl() {
        // setting the indicator buttons.
        mIndicatorControlContainer =
                (IndicatorControlContainer) findViewById(R.id.indicator_control);
        if (mIndicatorControlContainer == null) return;
        loadCameraPreferences();
        final String[] SETTING_KEYS = {
                CameraSettings.KEY_FLASH_MODE,
                CameraSettings.KEY_WHITE_BALANCE,
                CameraSettings.KEY_EXPOSURE,
                CameraSettings.KEY_SCENE_MODE};
        final String[] OTHER_SETTING_KEYS = {
                CameraSettings.KEY_RECORD_LOCATION,
                CameraSettings.KEY_POWER_SHUTTER,
                CameraSettings.KEY_FOCUS_SOUND,
                CameraSettings.KEY_STORAGE,
                CameraSettings.KEY_PICTURE_SIZE,
                CameraSettings.KEY_FOCUS_MODE,
                CameraSettings.KEY_ISO,
                CameraSettings.KEY_TIMER_MODE,
                CameraSettings.KEY_BURST_MODE};

        CameraPicker.setImageResourceId(R.drawable.ic_switch_photo_facing_holo_light);
        mIndicatorControlContainer.initialize(this, mPreferenceGroup,
                mParameters.isZoomSupported(),
                SETTING_KEYS, OTHER_SETTING_KEYS);
        updateSceneModeUI();
        mIndicatorControlContainer.setListener(this);
    }

    private boolean collapseCameraControls() {
        if ((mIndicatorControlContainer != null)
                && mIndicatorControlContainer.dismissSettingPopup()) {
            return true;
        }
        return false;
    }

    private void enableCameraControls(boolean enable) {
        if (mIndicatorControlContainer != null) {
            mIndicatorControlContainer.setEnabled(enable);
        }
        if (mModePicker != null) mModePicker.setEnabled(enable);
        if (mZoomControl != null) mZoomControl.setEnabled(enable);
        if (mThumbnailView != null) mThumbnailView.setEnabled(enable);
    }

    private class MyOrientationEventListener
            extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            mOrientation = Util.roundOrientation(orientation, mOrientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mOrientation
                    + Util.getDisplayRotation(Camera.this);
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                setOrientationIndicator(mOrientationCompensation);
            }

            // Show the toast after getting the first orientation changed.
            if (mHandler.hasMessages(SHOW_TAP_TO_FOCUS_TOAST)) {
                mHandler.removeMessages(SHOW_TAP_TO_FOCUS_TOAST);
                showTapToFocusToast();
            }
            mRecordingTimeRect.setOrientation(mOrientationCompensation);
        }
    }

    private void setOrientationIndicator(int orientation) {
        Rotatable[] indicators = {mThumbnailView, mModePicker, mSharePopup,
                mIndicatorControlContainer, mZoomControl, mFocusAreaIndicator, mFaceView,
                mReviewCancelButton, mReviewDoneButton, mRotateDialog, mOnScreenIndicators};
        for (Rotatable indicator : indicators) {
            if (indicator != null) indicator.setOrientation(orientation);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMediaProviderClient != null) {
            mMediaProviderClient.release();
            mMediaProviderClient = null;
        }
    }

    private void checkStorage() {
        mPicturesRemaining = Storage.getAvailableSpace(mStorage);
        if (mPicturesRemaining > Storage.LOW_STORAGE_THRESHOLD) {
            mPicturesRemaining = (mPicturesRemaining - Storage.LOW_STORAGE_THRESHOLD)
                    / Storage.PICTURE_SIZE;
        } else if (mPicturesRemaining > 0) {
            mPicturesRemaining = 0;
        }

        updateStorageHint();
    }

    @OnClickAttr
    public void onThumbnailClicked(View v) {
        if (isCameraIdle() && mThumbnail != null) {
            showSharePopup();
        }
    }

    @OnClickAttr
    public void onReviewRetakeClicked(View v) {
        hidePostCaptureAlert();
        startPreview();
        startFaceDetection();
    }

    @OnClickAttr
    public void onReviewDoneClicked(View v) {
        doAttach();
    }

    @OnClickAttr
    public void onReviewCancelClicked(View v) {
        doCancel();
    }

    private void doAttach() {
        if (mPausing) {
            return;
        }

        byte[] data = mJpegImageData;

        if (mCropValue == null) {
            // First handle the no crop case -- just return the value.  If the
            // caller specifies a "save uri" then write the data to it's
            // stream. Otherwise, pass back a scaled down version of the bitmap
            // directly in the extras.
            if (mSaveUri != null) {
                OutputStream outputStream = null;
                try {
                    outputStream = mContentResolver.openOutputStream(mSaveUri);
                    outputStream.write(data);
                    outputStream.close();

                    setResultEx(RESULT_OK);
                    finish();
                } catch (IOException ex) {
                    // ignore exception
                } finally {
                    Util.closeSilently(outputStream);
                }
            } else {
                int orientation = Exif.getOrientation(data);
                Bitmap bitmap = Util.makeBitmap(data, 50 * 1024);
                bitmap = Util.rotate(bitmap, orientation);
                setResultEx(RESULT_OK,
                        new Intent("inline-data").putExtra("data", bitmap));
                finish();
            }
        } else {
            // Save the image to a temp file and invoke the cropper
            Uri tempUri = null;
            FileOutputStream tempStream = null;
            try {
                File path = getFileStreamPath(sTempCropFilename);
                path.delete();
                tempStream = openFileOutput(sTempCropFilename, 0);
                tempStream.write(data);
                tempStream.close();
                tempUri = Uri.fromFile(path);
            } catch (FileNotFoundException ex) {
                setResultEx(Activity.RESULT_CANCELED);
                finish();
                return;
            } catch (IOException ex) {
                setResultEx(Activity.RESULT_CANCELED);
                finish();
                return;
            } finally {
                Util.closeSilently(tempStream);
            }

            Bundle newExtras = new Bundle();
            if (mCropValue.equals("circle")) {
                newExtras.putString("circleCrop", "true");
            }
            if (mSaveUri != null) {
                newExtras.putParcelable(MediaStore.EXTRA_OUTPUT, mSaveUri);
            } else {
                newExtras.putBoolean("return-data", true);
            }

            Intent cropIntent = new Intent("com.android.camera.action.CROP");

            cropIntent.setData(tempUri);
            cropIntent.putExtras(newExtras);

            startActivityForResult(cropIntent, CROP_MSG);
        }
    }

    private void doCancel() {
        setResultEx(RESULT_CANCELED, new Intent());
        finish();
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
        if (mTimerMode && pressed || mPausing || collapseCameraControls() || mCameraState == SNAPSHOT_IN_PROGRESS) return;

        // Do not do focus if there is not enough storage.
        if (pressed && !canTakePicture()) return;

        if (pressed) {
            mFocusManager.onShutterDown();
        } else {
            mFocusManager.onShutterUp();
        }
    }

    private void updateTimer(int timerSeconds) {
        mRecordingTimeView.setText(String.format("%d:%02d", timerSeconds / 60, timerSeconds % 60));
        timerSeconds--;
        if (timerSeconds < 0) {
            capture();
            onShutterButtonClick();
        } else {
            if (timerSeconds < 2) {
                mFocusManager.onShutterDown();
                mFocusManager.onShutterUp();
            }
            Message timerMsg = Message.obtain();
            timerMsg.arg1 = timerSeconds;
            timerMsg.what = CAMERA_TIMER;
            mHandler.sendMessageDelayed(timerMsg, 1000);
        }
    }

    @Override
    public void onShutterButtonClick() {
        int nbBurstShots = Integer.valueOf(mPreferences.getString(CameraSettings.KEY_BURST_MODE, "1"));
        if (!mTimerMode) {
            if (mCaptureMode != 0) {
                mTimerMode = true;
                mShutterButton.setImageDrawable(getResources().getDrawable(
                        R.drawable.btn_video_shutter_recording_holo));
                mRecordingTimeView.setVisibility(View.VISIBLE);
                updateTimer(mCaptureMode);
                return;
            } else {
                mFocusManager.zslPreventAutoFocus();
            }
        } else if (mTimerMode) {
            mShutterButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.btn_camera_shutter_holo));
            mTimerMode = false;
            mHandler.removeMessages(CAMERA_TIMER);
            mRecordingTimeView.setVisibility(View.GONE);
            return;
        }

        if (mPausing || collapseCameraControls()) {
            return;
        }

        // Do not take the picture if there is not enough storage.
        if (mPicturesRemaining <= 0) {
            Log.i(TAG, "Not enough space or storage not ready. remaining=" + mPicturesRemaining);
            return;
        }

        Log.v(TAG, "onShutterButtonClick: mCameraState=" + mCameraState);

        // If the user wants to do a snapshot while the previous one is still
        // in progress, remember the fact and do it after we finish the previous
        // one and re-start the preview. Snapshot in progress also includes the
        // state that autofocus is focusing and a picture will be taken when
        // focus callback arrives.
        if (mFocusManager.isFocusingSnapOnFinish() || mCameraState == SNAPSHOT_IN_PROGRESS) {
            mSnapshotOnIdle = true;
            return;
        }

        if (mSnapshotMode == CameraInfo.CAMERA_SUPPORT_MODE_ZSL) {
            mFocusManager.setZslEnable(true);
        } else {
            mFocusManager.setZslEnable(false);
        }

        mFocusManager.doSnap();
        mBurstShotsDone++;

        if (mBurstShotsDone == nbBurstShots) {
            mBurstShotsDone = 0;
            mSnapshotOnIdle = false;
        } else if (mSnapshotOnIdle == false) {
            // queue a new shot until we done all our shots

