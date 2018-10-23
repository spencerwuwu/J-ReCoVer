// https://searchcode.com/api/result/3997609/

/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.Ti2DMatrix;
import org.appcelerator.titanium.view.Ti2DMatrix.Operation;
import org.appcelerator.titanium.view.TiAnimation;
import org.appcelerator.titanium.view.TiBackgroundColorWrapper;
import org.appcelerator.titanium.view.TiBorderWrapperView;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;
import org.appcelerator.titanium.view.TiUIView;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.animation.AnimatorProxy;

/**
 * Builds and starts animations. When possible, Honeycomb+ animations
 * (i.e., Property Animators) are used, even on pre-Honeycomb, which is
 * possible because we use the NineOldAndroids compatibility library.
 * The only time Property Animators are not used -- i.e., the only time
 * we revert to using old-style view animations -- is when the animation's
 * transform property is set to a Ti2DMatrix that is "complicated", where
 * complicated means one of the following is true:
 * <ol>
 * <li>The Ti2DMatrix is the product of matrix multiplication, e.g.,
 * <pre>
 * {@code
 *			var newMatrix = baseMatrix.multiply(otherMatrix);
 * }
 * </pre>
 * </li>
 * <li>The Ti2DMatrix is the inverse of another matrix, e.g.,
 * <pre>
 * {@code
 *			var newMatrix = oldMatrix.invert();
 * }
 * </pre>
 * </li>
 * <li>The Ti2DMatrix is the result of concatenating an operation more
 * than once, e.g.,
 * <pre>
 * {@code
 * 			var newMatrix = baseMatrix.rotate(45).rotate(10);
 * }
 * </pre>
 * 		or
 * <pre>
 * {@code
 * 			var newMatrix = baseMatrix.rotate(45).scale(2,2).rotate(10)
 * }
 * </pre>
 * 		which contains two rotates.
 * </li>
 * </ul>
 * Concatenation of matrix operations does not in itself cause old style
 * animations to run -- only if an operation is repeated. For example...
 * <pre>
 * {@code
 * 			var newMatrix = baseMatrix.rotate(45).translate(10,10).scale(2,2)
 * }
 * </pre>
 * ...would be fine since no operation is duplicated. In such a case, a set of
 * Honeycomb+ property Animators can be safely derived.
 */
public class TiAnimationBuilder
{
	private static final String TAG = "TiAnimationBuilder";
	private static final boolean PRE_HONEYCOMB = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;

	// Views on which animations are currently running.
	private static ArrayList<WeakReference<View>> sRunningViews = new ArrayList<WeakReference<View>>();

	protected float anchorX;
	protected float anchorY;
	protected Ti2DMatrix tdm = null;
	protected Double delay = null;
	protected Double duration = null;
	protected Double toOpacity = null;
	protected Double repeat = null;
	protected Boolean autoreverse = null;
	protected String top = null, bottom = null, left = null, right = null;
	protected String centerX = null, centerY = null;
	protected String width = null, height = null;
	protected Integer backgroundColor = null;

	protected TiAnimation animationProxy;
	protected KrollFunction callback;
	protected boolean relayoutChild = false, applyOpacity = false;
	@SuppressWarnings("rawtypes")
	protected HashMap options;
	protected View view;
	protected AnimatorHelper animatorHelper;
	protected TiViewProxy viewProxy;

	public TiAnimationBuilder()
	{
		anchorX = Ti2DMatrix.DEFAULT_ANCHOR_VALUE;
		anchorY = Ti2DMatrix.DEFAULT_ANCHOR_VALUE;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void applyOptions(HashMap options)
	{
		if (options == null) {
			return;
		}

		if (options.containsKey(TiC.PROPERTY_ANCHOR_POINT)) {
			Object anchorPoint = options.get(TiC.PROPERTY_ANCHOR_POINT);
			if (anchorPoint instanceof HashMap) {
				HashMap point = (HashMap) anchorPoint;
				anchorX = TiConvert.toFloat(point, TiC.PROPERTY_X);
				anchorY = TiConvert.toFloat(point, TiC.PROPERTY_Y);
			} else {
				Log.e(TAG,
						"Invalid argument type for anchorPoint property. Ignoring");
			}
		}

		if (options.containsKey(TiC.PROPERTY_TRANSFORM)) {
			tdm = (Ti2DMatrix) options.get(TiC.PROPERTY_TRANSFORM);
		}
		if (options.containsKey(TiC.PROPERTY_DELAY)) {
			delay = TiConvert.toDouble(options, TiC.PROPERTY_DELAY);
		}

		if (options.containsKey(TiC.PROPERTY_DURATION)) {
			duration = TiConvert.toDouble(options, TiC.PROPERTY_DURATION);
		}

		if (options.containsKey(TiC.PROPERTY_OPACITY)) {
			toOpacity = TiConvert.toDouble(options, TiC.PROPERTY_OPACITY);
		}

		if (options.containsKey(TiC.PROPERTY_REPEAT)) {
			repeat = TiConvert.toDouble(options, TiC.PROPERTY_REPEAT);

			if (repeat == 0d) {
				// A repeat of 0 is probably non-sensical. Titanium iOS
				// treats it as 1 and so should we.
				repeat = 1d;
			}
		} else {
			repeat = 1d; // Default as indicated in our documentation.
		}

		if (options.containsKey(TiC.PROPERTY_AUTOREVERSE)) {
			autoreverse = TiConvert
					.toBoolean(options, TiC.PROPERTY_AUTOREVERSE);
		}

		if (options.containsKey(TiC.PROPERTY_TOP)) {
			top = TiConvert.toString(options, TiC.PROPERTY_TOP);
		}

		if (options.containsKey(TiC.PROPERTY_BOTTOM)) {
			bottom = TiConvert.toString(options, TiC.PROPERTY_BOTTOM);
		}

		if (options.containsKey(TiC.PROPERTY_LEFT)) {
			left = TiConvert.toString(options, TiC.PROPERTY_LEFT);
		}

		if (options.containsKey(TiC.PROPERTY_RIGHT)) {
			right = TiConvert.toString(options, TiC.PROPERTY_RIGHT);
		}

		if (options.containsKey(TiC.PROPERTY_CENTER)) {
			Object centerPoint = options.get(TiC.PROPERTY_CENTER);
			if (centerPoint instanceof HashMap) {
				HashMap center = (HashMap) centerPoint;
				centerX = TiConvert.toString(center, TiC.PROPERTY_X);
				centerY = TiConvert.toString(center, TiC.PROPERTY_Y);

			} else {
				Log.e(TAG,
						"Invalid argument type for center property. Ignoring");
			}
		}

		if (options.containsKey(TiC.PROPERTY_WIDTH)) {
			width = TiConvert.toString(options, TiC.PROPERTY_WIDTH);
		}

		if (options.containsKey(TiC.PROPERTY_HEIGHT)) {
			height = TiConvert.toString(options, TiC.PROPERTY_HEIGHT);
		}

		if (options.containsKey(TiC.PROPERTY_BACKGROUND_COLOR)) {
			backgroundColor = TiConvert.toColor(options,
					TiC.PROPERTY_BACKGROUND_COLOR);
		}

		this.options = options;
	}

	public void applyAnimation(TiAnimation anim)
	{
		this.animationProxy = anim;
		applyOptions(anim.getProperties());
	}

	public void setCallback(KrollFunction callback)
	{
		this.callback = callback;
	}

	/**
	 * Builds the Animations used for pre-Honeycomb
	 * style of animations, i.e., view animations
	 * instead of property animations. We use this
	 * only when we can't use NineOldAndroids to accomplish
	 * the animation, which happens only when the
	 * animation contains a transform (Ti2DMatrix) that
	 * is too complicated. See this class documentation
	 * for more information.
	 * @return An AnimationSet with the view Animations that
	 * will be used to accomplish the requested animation.
	 */
	private AnimationSet buildViewAnimations()
	{
		if (Log.isDebugModeEnabled()) {
			Log.w(TAG, "Using legacy animations");
		}

		ViewParent parent = view.getParent();
		int parentWidth = 0;
		int parentHeight = 0;

		if (parent instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) parent;
			parentHeight = group.getMeasuredHeight();
			parentWidth = group.getMeasuredWidth();
		}

		return buildViewAnimations(view.getLeft(), view.getTop(),
				view.getMeasuredWidth(), view.getMeasuredHeight(), parentWidth,
				parentHeight);
	}

	/**
	 * Builds the Animators used for Honeycomb+ animations
	 * @return AnimatorSet containing the Animator instances
	 * that will be used to accomplish this animation.
	 */
	private AnimatorSet buildPropertyAnimators()
	{
		if (Log.isDebugModeEnabled()) {
			Log.d(TAG, "Property Animations will be used.");
		}
		ViewParent parent = view.getParent();
		int parentWidth = 0;
		int parentHeight = 0;

		if (parent instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) parent;
			parentHeight = group.getHeight();
			parentWidth = group.getWidth();
		}

		return buildPropertyAnimators(view.getLeft(), view.getTop(),
				view.getWidth(), view.getHeight(), parentWidth, parentHeight);
	}

	/**
	 * Builds the Animators used for Honeycomb+ animations (i.e.,
	 * property animation instead of view animation.)
	 * @param x The view's left property.
	 * @param y The view's top property.
	 * @param w The view's width.
	 * @param h The view's height.
	 * @param parentWidth The view parent's width.
	 * @param parentHeight The view parent's height.
	 * @return AnimatorSet containing the Animator instances
	 * that will be used to accomplish this animation.
	 */
	private AnimatorSet buildPropertyAnimators(int x, int y, int w, int h, int parentWidth, int parentHeight)
	{
		List<Animator> animators = new ArrayList<Animator>();
		boolean includesRotation = false;

		if (toOpacity != null) {
			addAnimator(animators, ObjectAnimator.ofFloat(view, "alpha", toOpacity.floatValue()));
		}

		if (backgroundColor != null) {
			TiBackgroundColorWrapper bgWrap = TiBackgroundColorWrapper.wrap(view);
			int currentBackgroundColor = bgWrap.getBackgroundColor();
			ObjectAnimator bgAnimator = ObjectAnimator.ofInt(view, "backgroundColor", currentBackgroundColor,
					backgroundColor);
			bgAnimator.setEvaluator(new ArgbEvaluator());
			addAnimator(animators, bgAnimator);
		}

		if (tdm != null) {
			AnimatorUpdateListener updateListener = null;
			// Need to invalidate the parent view for Honeycomb+. Otherwise it will not draw correctly.
			if (!PRE_HONEYCOMB) {
				updateListener = new AnimatorUpdateListener();
			}

			// Derive a set of property Animators from the
			// operations in the matrix so we can go ahead
			// and use Honeycomb+ animations rather than
			// our custom TiMatrixAnimation.
			List<Operation> operations = tdm.getAllOperations();
			if (operations.size() == 0) {
				// Identity matrix, which means any transforms that
				// were previously done should be reversed.
				addAnimator(animators, ObjectAnimator.ofFloat(view, "rotation", 0f));
				addAnimator(animators, ObjectAnimator.ofFloat(view, "scaleX", 1f));
				addAnimator(animators, ObjectAnimator.ofFloat(view, "scaleY", 1f));
				addAnimator(animators, ObjectAnimator.ofFloat(view, "translationX", 0f));
				addAnimator(animators, ObjectAnimator.ofFloat(view, "translationY", 0f));

				// Relayout child in pre-Honeycomb so that touch targets get
				// updated.
				relayoutChild = (autoreverse == null || !autoreverse.booleanValue());

			} else {

				for (Operation operation : operations) {
					if (operation.anchorX != Ti2DMatrix.DEFAULT_ANCHOR_VALUE
							|| operation.anchorY != Ti2DMatrix.DEFAULT_ANCHOR_VALUE) {
						setAnchor(w, h, operation.anchorX, operation.anchorY);
					}
					switch (operation.type) {
						case Operation.TYPE_ROTATE:
							includesRotation = true;
							if (operation.rotationFromValueSpecified) {
								ObjectAnimator anim = ObjectAnimator.ofFloat(view, "rotation", operation.rotateFrom,
									operation.rotateTo);
								if (updateListener != null) {
									anim.addUpdateListener(updateListener);
								}
								addAnimator(animators, anim);
							} else {
								ObjectAnimator anim = ObjectAnimator.ofFloat(view, "rotation", operation.rotateTo);
								if (updateListener != null) {
									anim.addUpdateListener(updateListener);
								}
								addAnimator(animators, anim);
							}
							break;
						case Operation.TYPE_SCALE:
							if (operation.scaleFromValuesSpecified) {
								ObjectAnimator animX = ObjectAnimator.ofFloat(view, "scaleX", operation.scaleFromX,
									operation.scaleToX);
								if (updateListener != null) {
									animX.addUpdateListener(updateListener);
								}
								addAnimator(animators, animX);
								addAnimator(animators, ObjectAnimator.ofFloat(view, "scaleY", operation.scaleFromY,
										operation.scaleToY));

							} else {
								ObjectAnimator animX = ObjectAnimator.ofFloat(view, "scaleX", operation.scaleToX);
								if (updateListener != null) {
									animX.addUpdateListener(updateListener);
								}
								addAnimator(animators, animX);
								addAnimator(animators, ObjectAnimator.ofFloat(view, "scaleY", operation.scaleToY));
							}
							break;
						case Operation.TYPE_TRANSLATE:
							ObjectAnimator animX = ObjectAnimator.ofFloat(view, "translationX", operation.translateX);
							if (updateListener != null) {
								animX.addUpdateListener(updateListener);
							}
							addAnimator(animators, animX);
							addAnimator(animators, ObjectAnimator.ofFloat(view, "translationY", operation.translateY));
					}
				}
			}
		}

		if (top != null || bottom != null || left != null || right != null || centerX != null || centerY != null) {
			TiDimension optionTop = null, optionBottom = null;
			TiDimension optionLeft = null, optionRight = null;
			TiDimension optionCenterX = null, optionCenterY = null;
			TiDimension optionHeight = null, optionWidth=null;
			int newHeight = h, newWidth=w;

			// Note that we're stringifying the values to make sure we
			// use the correct TiDimension constructor, except when
			// we know the values are expressed for certain in pixels.
			if (top != null) {
				optionTop = new TiDimension(top, TiDimension.TYPE_TOP);
			} else if (bottom == null && centerY == null) {
				// Fix a top value since no other y-axis value is being set.
				optionTop = new TiDimension(view.getTop(), TiDimension.TYPE_TOP);
				optionTop.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (bottom != null) {
				optionBottom = new TiDimension(bottom, TiDimension.TYPE_BOTTOM);
			}

			if (left != null) {
				optionLeft = new TiDimension(left, TiDimension.TYPE_LEFT);
			} else if (right == null && centerX == null) {
				// Fix a left value since no other x-axis value is being set.
				optionLeft = new TiDimension(view.getLeft(), TiDimension.TYPE_LEFT);
				optionLeft.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (right != null) {
				optionRight = new TiDimension(right, TiDimension.TYPE_RIGHT);
			}

			if (centerX != null) {
				optionCenterX = new TiDimension(centerX, TiDimension.TYPE_CENTER_X);
			}

			if (centerY != null) {
				optionCenterY = new TiDimension(centerY, TiDimension.TYPE_CENTER_Y);
			}

			int horizontal[] = new int[2];
			int vertical[] = new int[2];
			ViewParent parent = view.getParent();
			View parentView = null;

			if (parent instanceof View) {
				parentView = (View) parent;
			} else {
				Log.e(TAG, "Parent view doesn't exist");
			}

			if (height != null) {
				optionHeight = new TiDimension(height, TiDimension.TYPE_HEIGHT);
				newHeight = optionHeight.getAsPixels(parentView);
			}

			if (width != null) {
				optionWidth = new TiDimension(width, TiDimension.TYPE_WIDTH);
				newWidth = optionWidth.getAsPixels(parentView);
			}

			TiCompositeLayout.computePosition(parentView, optionLeft, optionCenterX, optionRight, newWidth, 0, parentWidth,
					horizontal);
			TiCompositeLayout.computePosition(parentView, optionTop, optionCenterY, optionBottom, newHeight, 0, parentHeight,
					vertical);

			// For pre-Honeycomb, there will be flicker during animation if using animatorHelper.
			if (PRE_HONEYCOMB) {
				int translationX = horizontal[0] - x;
				int translationY = vertical[0] - y;
				addAnimator(animators, ObjectAnimator.ofFloat(view, "translationX", translationX));
				addAnimator(animators, ObjectAnimator.ofFloat(view, "translationY", translationY));

			// For Honeycomb+, animatorHelper will reset layout parameters so the layout will keep correct (TIMOB-15951).
			} else {
				if (animatorHelper == null) {
					animatorHelper = new AnimatorHelper();
				}
				
				if (left != null) {
					addAnimator(animators, ObjectAnimator.ofInt(animatorHelper, TiC.PROPERTY_LEFT, x, horizontal[0]));
				} 
				
				if (right != null) {
					int afterRight = optionRight.getAsPixels(parentView);;
					
					TiDimension beforeRightD = ((TiCompositeLayout.LayoutParams)view.getLayoutParams()).optionRight;
					int beforeRight = 0;
					if (beforeRightD != null) {
						beforeRight = beforeRightD.getAsPixels(parentView);
					} else {
						beforeRight = parentWidth - view.getRight(); 
					}

					addAnimator(animators, ObjectAnimator.ofInt(animatorHelper, TiC.PROPERTY_RIGHT, beforeRight, afterRight));
				} 

				if (centerX != null) {
					int afterCenterX = optionCenterX.getAsPixels(parentView);;
					
					int beforeCenterX = 0;
					TiDimension beforeCenterXD = ((TiCompositeLayout.LayoutParams)view.getLayoutParams()).optionCenterX;
					
					if (beforeCenterXD != null) {
						beforeCenterX = beforeCenterXD.getAsPixels(parentView);
					} else {
						beforeCenterX = (view.getRight() + view.getLeft()) / 2;
					}
					
					addAnimator(animators, ObjectAnimator.ofInt(animatorHelper, "centerX", beforeCenterX, afterCenterX));
				}
				
				
				if (top != null) {
					addAnimator(animators, ObjectAnimator.ofInt(animatorHelper, TiC.PROPERTY_TOP, y, vertical[0]));
					
				}
				
				if (bottom != null) {
					int afterBottom = optionBottom.getAsPixels(parentView);

					int beforeBottom = 0;
					TiDimension beforeBottomD = ((TiCompositeLayout.LayoutParams)view.getLayoutParams()).optionBottom;
					if (beforeBottomD != null) {
						beforeBottom = beforeBottomD.getAsPixels(parentView);
					} else {
						beforeBottom = parentHeight - view.getBottom();
					}
					
					addAnimator(animators, ObjectAnimator.ofInt(animatorHelper, TiC.PROPERTY_BOTTOM, beforeBottom, afterBottom));
					
				}
				
				if (centerY != null) {
					int afterCenterY = optionCenterY.getAsPixels(parentView);;
					
					int beforeCenterY = 0;
					TiDimension beforeCenterYD = ((TiCompositeLayout.LayoutParams)view.getLayoutParams()).optionCenterY;

					if (beforeCenterYD != null) {
						beforeCenterY = beforeCenterYD.getAsPixels(parentView);
					} else {
						beforeCenterY = (view.getTop() + view.getBottom()) / 2;
					}

					addAnimator(animators, ObjectAnimator.ofInt(animatorHelper, "centerY", beforeCenterY, afterCenterY));
				}
			}

			// Pre-Honeycomb, we will need to update layout params at end of
			// animation so that touch events will be recognized at new location,
			// and so that view will stay at new location after changes in
			// orientation. But if autoreversing to original layout, no
			// need to re-layout. Also, don't do it if a rotation is included,
			// since the re-layout will lose the rotation.
			relayoutChild = !includesRotation && (autoreverse == null || !autoreverse.booleanValue());

		}

		if (tdm == null && (width != null || height != null)) {
			TiDimension optionWidth, optionHeight;

			if (width != null) {
				optionWidth = new TiDimension(width, TiDimension.TYPE_WIDTH);
			} else {
				optionWidth = new TiDimension(w, TiDimension.TYPE_WIDTH);
				optionWidth.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (height != null) {
				optionHeight = new TiDimension(height, TiDimension.TYPE_HEIGHT);
			} else {
				optionHeight = new TiDimension(h, TiDimension.TYPE_HEIGHT);
				optionHeight.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			ViewParent parent = view.getParent();
			View parentView = null;
			if (parent instanceof View) {
				parentView = (View) parent;
			}
			int toWidth = optionWidth.getAsPixels(parentView != null ? parentView : view);
			int toHeight = optionHeight.getAsPixels(parentView != null ? parentView : view);

			// For pre-Honeycomb, there will be flicker during animation if using animatorHelper.
			if (PRE_HONEYCOMB) {
				float scaleX = (float) toWidth / w;
				float scaleY = (float) toHeight / h;
				addAnimator(animators, ObjectAnimator.ofFloat(view, "scaleX", scaleX));
				addAnimator(animators, ObjectAnimator.ofFloat(view, "scaleY", scaleY));

			// For Honeycomb+, animatorHelper will reset layout parameters so the layout will keep correct (TIMOB-15951, TIMOB-16087).
			} else {
				if (animatorHelper == null) {
					animatorHelper = new AnimatorHelper();
				}
				if (width != null) {
					addAnimator(animators, ObjectAnimator.ofInt(animatorHelper, "width", w, toWidth));
				}
				if (height != null) {
					addAnimator(animators, ObjectAnimator.ofInt(animatorHelper, "height", h, toHeight));
				}
			}

			setAnchor(w, h);

			// Pre-Honeycomb, will need to update layout params at end of
			// animation so that touch events will be recognized within new
			// size rectangle, and so that new size will survive
			// any changes in orientation. But if autoreversing
			// to original layout, no need to re-layout.
			// Also, don't do it if a rotation is included,
			// since the re-layout will lose the rotation.
			relayoutChild = !includesRotation && (autoreverse == null || !autoreverse.booleanValue());
		}

		// Because of https://github.com/JakeWharton/NineOldAndroids/issues/54
		// we add a dummy animator if all of these conditions are met:
		// 1. There is only one animator so far.
		// 2. That animator is for the alpha property.
		// 3. The view has a non-null background.
		// 4. Pre-Honeycomb (e.g., Gingerbread) is running.
		if (PRE_HONEYCOMB && animators.size() == 1 && toOpacity != null && view.getBackground() != null) {
			float currentScaleX = ViewHelper.getScaleX(view);
			ValueAnimator dummyAnimator = ObjectAnimator.ofFloat(view, "scaleX", currentScaleX + 0.001f);
			dummyAnimator.setRepeatCount(1);
			dummyAnimator.setRepeatMode(ValueAnimator.REVERSE);
			addAnimator(animators, dummyAnimator);
		}

		AnimatorSet as = new AnimatorSet();
		as.playTogether(animators);

		as.addListener(new AnimatorListener());

		if (duration != null) {
			as.setDuration(duration.longValue());
		}

		if (delay != null) {
			as.setStartDelay(delay.longValue());
		}

		return as;
	}

	private void addAnimation(AnimationSet animationSet, Animation animation)
	{
		// repeatCount is ignored at the AnimationSet level, so it needs to
		// be set for each child animation manually.

		// We need to reduce the repeat count by 1, since for native Android
		// 1 would mean repeating it once.
		int repeatCount = (repeat == null ? 0 : repeat.intValue() - 1);

		// In Android (native), the repeat count includes reverses. So we
		// need to double-up and add one to the repeat count if we're reversing.
		if (autoreverse != null && autoreverse.booleanValue()) {
			repeatCount = repeatCount * 2 + 1;
		}

		animation.setRepeatCount(repeatCount);

		animationSet.addAnimation(animation);
	}

	private void addAnimator(List<Animator> list, ValueAnimator animator)
	{
		animator.setInterpolator(new LinearInterpolator());

		// repeatCount is ignored at the AnimatorSet level, so it needs to
		// be set for each member animator manually. Same with
		// repeat mode.

		// We need to reduce the repeat count by 1, since for native Android
		// 1 would mean repeating it once.
		int repeatCount = (repeat == null ? 0 : repeat.intValue() - 1);
		int repeatMode = ValueAnimator.RESTART;

		// In Android (native), the repeat count includes reverses. So we
		// need to double-up and add one to the repeat count if we're reversing.
		if (autoreverse != null && autoreverse.booleanValue()) {
			repeatCount = repeatCount * 2 + 1;
		}

		animator.setRepeatCount(repeatCount);
		if (autoreverse != null && autoreverse.booleanValue()) {
			repeatMode = ValueAnimator.REVERSE;
		}
		animator.setRepeatMode(repeatMode);

		list.add(animator);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setViewPivotHC(float pivotX, float pivotY)
	{
		view.setPivotX(pivotX);
		view.setPivotY(pivotY);
	}

	private void setViewPivot(float pivotX, float pivotY)
	{
		AnimatorProxy proxy = AnimatorProxy.wrap(view);
		proxy.setPivotX(pivotX);
		proxy.setPivotY(pivotY);
	}

	public TiMatrixAnimation createMatrixAnimation(Ti2DMatrix matrix)
	{
		return new TiMatrixAnimation(matrix, anchorX, anchorY);
	}


	/**
	 * Builds the Animations used for pre-Honeycomb
	 * style of animations, i.e., view animations
	 * instead of property animations. We use this
	 * only when we can't use NineOldAndroids to accomplish
	 * the animation, which happens only when the
	 * animation contains a transform (Ti2DMatrix) that
	 * is too complicated. See this class documentation
	 * for more information.
	 * @param x The view's left property.
	 * @param y The view's top property.
	 * @param w The view's width.
	 * @param h The view's height.
	 * @param parentWidth The view parent's width.
	 * @param parentHeight The view parent's height.
	 * @return An AnimationSet with the view Animations that
	 * will be used to accomplish the requested animation.
	 */
	private AnimationSet buildViewAnimations(int x, int y, int w, int h,
			int parentWidth, int parentHeight)
	{
		boolean includesRotation = false;
		AnimationSet as = new AnimationSet(false);
		AnimationListener animationListener = new AnimationListener();
		as.setAnimationListener(animationListener);
		TiUIView tiView = viewProxy.peekView();

		if (toOpacity != null) {
			// Determine which value to use for "from" value, in this order:
			// 1.) If we previously performed an alpha animation on the view,
			// use that as the from value.
			// 2.) Else, if we have set an opacity property on the view, use
			// that as the from value.
			// 3.) Else, use 1.0f as the from value.

			float fromOpacity;
			float currentAnimatedAlpha = tiView == null ? Float.MIN_VALUE
					: tiView.getAnimatedAlpha();

			if (currentAnimatedAlpha != Float.MIN_VALUE) {
				// MIN_VALUE is used as a signal that no value has been set.
				fromOpacity = currentAnimatedAlpha;

			} else if (viewProxy.hasProperty(TiC.PROPERTY_OPACITY)) {
				fromOpacity = TiConvert.toFloat(viewProxy
						.getProperty(TiC.PROPERTY_OPACITY));

			} else {
				fromOpacity = 1.0f;
			}

			Animation animation = new AlphaAnimation(fromOpacity,
					toOpacity.floatValue());

			// Remember the toOpacity value for next time, since we no way of
			// looking
			// up animated alpha values on the Android native view itself.
			if (tiView != null) {
				tiView.setAnimatedAlpha(toOpacity.floatValue());
			}

			applyOpacity = true; // Used in the animation listener
			addAnimation(as, animation);
			animation.setAnimationListener(animationListener);

			if (viewProxy.hasProperty(TiC.PROPERTY_OPACITY)
					&& toOpacity != null) {
				prepareOpacityForAnimation();
			}
		}

		if (backgroundColor != null) {
			int fromBackgroundColor = 0;

			if (viewProxy.hasProperty(TiC.PROPERTY_BACKGROUND_COLOR)) {
				fromBackgroundColor = TiConvert.toColor(TiConvert
						.toString(viewProxy
								.getProperty(TiC.PROPERTY_BACKGROUND_COLOR)));
			} else {
				Log.w(TAG,
						"Cannot animate view without a backgroundColor. View doesn't have that property. Using #00000000");
				fromBackgroundColor = Color.argb(0, 0, 0, 0);
			}

			Animation a = new TiColorAnimation(view, fromBackgroundColor,
					backgroundColor);
			addAnimation(as, a);
		}

		if (tdm != null) {

			Animation anim;
			if (tdm.hasScaleOperation() && tiView != null) {
				tiView.setAnimatedScaleValues(tdm.verifyScaleValues(tiView,
						(autoreverse != null && autoreverse.booleanValue())));
			}

			if (tdm.hasRotateOperation() && tiView != null) {
				includesRotation = true;
				tiView.setAnimatedRotationDegrees(tdm.verifyRotationValues(
						tiView,
						(autoreverse != null && autoreverse.booleanValue())));
			}

			anim = new TiMatrixAnimation(tdm, anchorX, anchorY);
			addAnimation(as, anim);

		}

		if (top != null || bottom != null || left != null || right != null
				|| centerX != null || centerY != null) {
			TiDimension optionTop = null, optionBottom = null;
			TiDimension optionLeft = null, optionRight = null;
			TiDimension optionCenterX = null, optionCenterY = null;

			// Note that we're stringifying the values to make sure we
			// use the correct TiDimension constructor, except when
			// we know the values are expressed for certain in pixels.
			if (top != null) {
				optionTop = new TiDimension(top, TiDimension.TYPE_TOP);
			} else if (bottom == null && centerY == null) {
				// Fix a top value since no other y-axis value is being set.
				optionTop = new TiDimension(view.getTop(), TiDimension.TYPE_TOP);
				optionTop.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (bottom != null) {
				optionBottom = new TiDimension(bottom, TiDimension.TYPE_BOTTOM);
			}

			if (left != null) {
				optionLeft = new TiDimension(left, TiDimension.TYPE_LEFT);
			} else if (right == null && centerX == null) {
				// Fix a left value since no other x-axis value is being set.
				optionLeft = new TiDimension(view.getLeft(),
						TiDimension.TYPE_LEFT);
				optionLeft.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (right != null) {
				optionRight = new TiDimension(right, TiDimension.TYPE_RIGHT);
			}

			if (centerX != null) {
				optionCenterX = new TiDimension(centerX,
						TiDimension.TYPE_CENTER_X);
			}

			if (centerY != null) {
				optionCenterY = new TiDimension(centerY,
						TiDimension.TYPE_CENTER_Y);
			}

			int horizontal[] = new int[2];
			int vertical[] = new int[2];
			ViewParent parent = view.getParent();
			View parentView = null;

			if (parent instanceof View) {
				parentView = (View) parent;
			}

			TiCompositeLayout.computePosition(parentView, optionLeft,
					optionCenterX, optionRight, w, 0, parentWidth, horizontal);
			TiCompositeLayout.computePosition(parentView, optionTop,
					optionCenterY, optionBottom, h, 0, parentHeight, vertical);

			Animation animation = new TranslateAnimation(Animation.ABSOLUTE, 0,
					Animation.ABSOLUTE, horizontal[0] - x, Animation.ABSOLUTE,
					0, Animation.ABSOLUTE, vertical[0] - y);

			animation.setAnimationListener(animationListener);
			addAnimation(as, animation);

			// Will need to update layout params at end of animation
			// so that touch events will be recognized at new location,
			// and so that view will stay at new location after changes in
			// orientation. But if autoreversing to original layout, no
			// need to re-layout. But don't do it if a rotation is included
			// because re-layout will lose the rotation.
			relayoutChild = !includesRotation
					&& (autoreverse == null || !autoreverse.booleanValue());

			if (Log.isDebugModeEnabled()) {
				Log.d(TAG, "animate " + viewProxy + " relative to self: "
						+ (horizontal[0] - x) + ", " + (vertical[0] - y),
						Log.DEBUG_MODE);
			}

		}

		if (tdm == null && (width != null || height != null)) {
			TiDimension optionWidth, optionHeight;

			if (width != null) {
				optionWidth = new TiDimension(width, TiDimension.TYPE_WIDTH);
			} else {
				optionWidth = new TiDimension(w, TiDimension.TYPE_WIDTH);
				optionWidth.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (height != null) {
				optionHeight = new TiDimension(height, TiDimension.TYPE_HEIGHT);
			} else {
				optionHeight = new TiDimension(h, TiDimension.TYPE_HEIGHT);
				optionHeight.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}
			
			ViewParent parent = view.getParent();
			View parentView = null;

			if (parent instanceof View) {
				parentView = (View) parent;
			}

			int toWidth = optionWidth.getAsPixels((parentView != null)?parentView:view);
			int toHeight = optionHeight.getAsPixels((parentView != null)?parentView:view);

			SizeAnimation sizeAnimation = new SizeAnimation(view, w, h,
					toWidth, toHeight);

			if (duration != null) {
				sizeAnimation.setDuration(duration.longValue());
			}

			sizeAnimation.setInterpolator(new LinearInterpolator());
			sizeAnimation.setAnimationListener(animationListener);
			addAnimation(as, sizeAnimation);

			// Will need to update layout params at end of animation
			// so that touch events will be recognized within new
			// size rectangle, and so that new size will survive
			// any changes in orientation. But if autoreversing
			// to original layout, no need to re-layout. But don't do it if a
			// rotation is included
			// because re-layout will lose the rotation.
			relayoutChild = !includesRotation
					&& (autoreverse == null || !autoreverse.booleanValue());
		}

		// Set duration, repeatMode and fillAfter only after adding children.
		// The values are pushed down to the child animations.
		as.setFillAfter(true);

		if (duration != null) {
			as.setDuration(duration.longValue());
		}

		if (autoreverse != null && autoreverse.booleanValue()) {
			as.setRepeatMode(Animation.REVERSE);
		} else {
			as.setRepeatMode(Animation.RESTART);
		}

		// startOffset is relevant to the animation set and thus
		// not also set on the child animations.
		if (delay != null) {
			as.setStartOffset(delay.longValue());
		}

		return as;
	}

	/**
	 * Pre-Honeycomb size animation.
	 */
	protected class SizeAnimation extends Animation
	{

		protected View view;
		protected float fromWidth, fromHeight, toWidth, toHeight;
		protected static final String TAG = "TiSizeAnimation";

		public SizeAnimation(View view, float fromWidth, float fromHeight,
				float toWidth, float toHeight)
		{
			this.view = view;
			this.fromWidth = fromWidth;
			this.fromHeight = fromHeight;
			this.toWidth = toWidth;
			this.toHeight = toHeight;

			if (Log.isDebugModeEnabled()) {
				Log.d(TAG, "animate view from (" + fromWidth + "x" + fromHeight
						+ ") to (" + toWidth + "x" + toHeight + ")",
						Log.DEBUG_MODE);
			}
		}

		@Override
		protected void applyTransformation(float interpolatedTime,
				Transformation transformation)
		{
			super.applyTransformation(interpolatedTime, transformation);

			int width = 0;
			if (fromWidth == toWidth) {
				width = (int) fromWidth;

			} else {
				width = (int) Math.floor(fromWidth
						+ ((toWidth - fromWidth) * interpolatedTime));
			}

			int height = 0;
			if (fromHeight == toHeight) {
				height = (int) fromHeight;

			} else {
				height = (int) Math.floor(fromHeight
						+ ((toHeight - fromHeight) * interpolatedTime));
			}

			ViewGroup.LayoutParams params = view.getLayoutParams();
			params.width = width;
			params.height = height;

			if (params instanceof TiCompositeLayout.LayoutParams) {
				TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
				tiParams.optionHeight = new TiDimension(height,
						TiDimension.TYPE_HEIGHT);
				tiParams.optionHeight.setUnits(TypedValue.COMPLEX_UNIT_PX);
				tiParams.optionWidth = new TiDimension(width,
						TiDimension.TYPE_WIDTH);
				tiParams.optionWidth.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			view.setLayoutParams(params);
		}
	}

	/**
	 * Pre-Honeycomb matrix animation.
	 */
	public static class TiMatrixAnimation extends Animation
	{
		protected Ti2DMatrix matrix;
		protected int childWidth, childHeight;
		protected float anchorX = -1, anchorY = -1;

		public boolean interpolate = true;

		public TiMatrixAnimation(Ti2DMatrix matrix, float anchorX, float anchorY)
		{
			this.matrix = matrix;
			this.anchorX = anchorX;
			this.anchorY = anchorY;
		}

		@Override
		public void initialize(int width, int height, int parentWidth,
				int parentHeight)
		{
			super.initialize(width, height, parentWidth, parentHeight);
			this.childWidth = width;
			this.childHeight = height;
		}

		@Override
		protected void applyTransformation(float interpolatedTime,
				Transformation transformation)
		{
			super.applyTransformation(interpolatedTime, transformation);
			if (interpolate) {
				Matrix m = matrix.interpolate(interpolatedTime, childWidth,
						childHeight, anchorX, anchorY);
				transformation.getMatrix().set(m);

			} else {
				transformation.getMatrix().set(
						getFinalMatrix(childWidth, childHeight));
			}
		}

		public Matrix getFinalMatrix(int childWidth, int childHeight)
		{
			return matrix.interpolate(1.0f, childWidth, childHeight, anchorX,
					anchorY);
		}

		public void invalidateWithMatrix(View view)
		{
			int width = view.getWidth();
			int height = view.getHeight();
			Matrix m = getFinalMatrix(width, height);
			RectF rectF = new RectF(0, 0, width, height);
			m.mapRect(rectF);
			rectF.inset(-1.0f, -1.0f);
			Rect rect = new Rect();
			rectF.round(rect);

			if (view.getParent() instanceof ViewGroup) {
				int left = view.getLeft();
				int top = view.getTop();

				((ViewGroup) view.getParent()).invalidate(left + rect.left, top
						+ rect.top, left + rect.width(), top + rect.height());
			}
		}
	}

	/**
	 * Pre-Honeycomb color animation.
	 */
	public static class TiColorAnimation extends Animation
	{
		View view;
		TransitionDrawable transitionDrawable;
		boolean reversing = false;
		int duration = 0;

		public TiColorAnimation(View view, int fromColor, int toColor)
		{
			this.view = view;

			ColorDrawable fromColorDrawable = new ColorDrawable(fromColor);
			ColorDrawable toColorDrawable = new ColorDrawable(toColor);
			transitionDrawable = new TransitionDrawable(new Drawable[] {
					fromColorDrawable, toColorDrawable });

			this.setAnimationListener(new android.view.animation.Animation.AnimationListener()
			{

				@SuppressWarnings("deprecation")
				public void onAnimationStart(Animation animation)
				{
					TiColorAnimation.this.view
							.setBackgroundDrawable(transitionDrawable);
					TiColorAnimation.this.duration = Long.valueOf(
							animation.getDuration()).intValue();
					transitionDrawable
							.startTransition(TiColorAnimation.this.duration);
				}

				public void onAnimationRepeat(Animation animation)
				{
					if (animation.getRepeatMode() == Animation.REVERSE) {
						reversing = !reversing;
					}
					if (reversing) {
						transitionDrawable
								.reverseTransition(TiColorAnimation.this.duration);
					} else {
						transitionDrawable
								.startTransition(TiColorAnimation.this.duration);
					}
				}

				public void onAnimationEnd(Animation animation)
				{
				}
			});
		}
	}

	/**
	 * A helper class for Honeycomb+ Property Animators to animate width/height/top/bottom/left/right/center.
	 * Based on the Android doc http://developer.android.com/guide/topics/graphics/prop-animation.html, to have
	 * the ObjectAnimator update properties correctly, the property must have a setter function.
	 */
	protected class AnimatorHelper
	{
		public void setWidth(final int w)
		{
			ViewGroup.LayoutParams params = view.getLayoutParams();
			params.width = w;

			if (params instanceof TiCompositeLayout.LayoutParams) {
				TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
				tiParams.optionWidth = new TiDimension(w, TiDimension.TYPE_WIDTH);
				tiParams.optionWidth.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			view.setLayoutParams(params);
			invalidateParentView();
		}

		public void setHeight(final int h)
		{
			ViewGroup.LayoutParams params = view.getLayoutParams();
			params.height = h;

			if (params instanceof TiCompositeLayout.LayoutParams) {
				TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
				tiParams.optionHeight = new TiDimension(h, TiDimension.TYPE_HEIGHT);
				tiParams.optionHeight.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			view.setLayoutParams(params);
			invalidateParentView();
		}

		public void setLeft(final int l)
		{
			ViewGroup.LayoutParams params = view.getLayoutParams();
			if (params instanceof TiCompositeLayout.LayoutParams) {
				TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
				tiParams.optionLeft = new TiDimension(l, TiDimension.TYPE_LEFT);
				tiParams.optionLeft.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}
			view.requestLayout();
			invalidateParentView();
		}
		
		public void setRight(final int r)
		{
			ViewGroup.LayoutParams params = view.getLayoutParams();
			if (params instanceof TiCompositeLayout.LayoutParams) {
				TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
				tiParams.optionRight = new TiDimension(r, TiDimension.TYPE_RIGHT);
				tiParams.optionRight.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}
			view.requestLayout();
			invalidateParentView();
		}

		public void setTop(final int t)
		{
			ViewGroup.LayoutParams params = view.getLayoutParams();
			if (params instanceof TiCompositeLayout.LayoutParams) {
				TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
				tiParams.optionTop = new TiDimension(t, TiDimension.TYPE_TOP);
				tiParams.optionTop.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}
			view.requestLayout();
			invalidateParentView();
		}
		
		public void setBottom(final int b)
		{
			ViewGroup.LayoutParams params = view.getLayoutParams();
			if (params instanceof TiCompositeLayout.LayoutParams) {
				TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
				tiParams.optionBottom = new TiDimension(b, TiDimension.TYPE_BOTTOM);
				tiParams.optionBottom.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}
			view.requestLayout();
			invalidateParentView();
		}
		
		public void setCenterX(final int b)
		{
			ViewGroup.LayoutParams params = view.getLayoutParams();
			if (params instanceof TiCompositeLayout.LayoutParams) {
				TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
				tiParams.optionCenterX = new TiDimension(b, TiDimension.TYPE_CENTER_X);
				tiParams.optionCenterX.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}
			view.requestLayout();
			invalidateParentView();
		}
		
		public void setCenterY(final int b)
		{
			ViewGroup.LayoutParams params = view.getLayoutParams();
			if (params instanceof TiCompositeLayout.LayoutParams) {
				TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams) params;
				tiParams.optionCenterY = new TiDimension(b, TiDimension.TYPE_CENTER_Y);
				tiParams.optionCenterY.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}
			view.requestLayout();
			invalidateParentView();
		}
		
		private void invalidateParentView() 
		{
			ViewParent vp = view.getParent();
			if (vp instanceof View) {
				((View) vp).invalidate();
			}
		}
	}

	/**
	 * The listener to receive callbacks on every animation frame.
	 */
	protected class AnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener
	{
		public void onAnimationUpdate(ValueAnimator animation)
		{
			ViewParent vp = view.getParent();
			if (vp instanceof View) {
				// Need to invalidate the parent view. Otherwise, it will not draw correctly.
				((View) vp).invalidate();
			}
		}
	}

	/**
	 * The listener for Honeycomb+ property Animators.
	 */
	protected class AnimatorListener implements Animator.AnimatorListener
	{

		public void onAnimationCancel(Animator animator)
		{
			if (animator instanceof AnimatorSet) {
				setAnimationRunningFor(view, false);
			}
		}

		@SuppressWarnings("unchecked")
		public void onAnimationEnd(Animator animator)
		{
			if (relayoutChild) {
				if (PRE_HONEYCOMB) {
					LayoutParams params = null;
					View viewToSetParams = view;
					if (view.getParent() instanceof TiBorderWrapperView) {
						viewToSetParams = (View) view.getParent();
					}
					params = (LayoutParams) viewToSetParams.getLayoutParams();
					TiConvert.fillLayout(options, params);
					viewToSetParams.setLayoutParams(params);
					view.clearAnimation();
					relayoutChild = false;
				}
				// TIMOB-11298 Propagate layout property changes to proxy
				for (Object key : options.keySet()) {
					if (TiC.PROPERTY_TOP.equals(key)
							|| TiC.PROPERTY_BOTTOM.equals(key)
							|| TiC.PROPERTY_LEFT.equals(key)
							|| TiC.PROPERTY_RIGHT.equals(key)
							|| TiC.PROPERTY_CENTER.equals(key)
							|| TiC.PROPERTY_WIDTH.equals(key)
							|| TiC.PROPERTY_HEIGHT.equals(key)
							|| TiC.PROPERTY_BACKGROUND_COLOR.equals(key)) {
						viewProxy.setProperty((String) key, options.get(key));
					}
				}
			}

			if (animator instanceof AnimatorSet) {
				setAnimationRunningFor(view, false);
				if (callback != null) {
					callback.callAsync(viewProxy.getKrollObject(),
							new Object[] { new KrollDict() });
				}

				if (animationProxy != null) {
					// In versions prior to Honeycomb, don't fire the event
					// until the message queue is empty. There appears to be
					// a bug in versions before Honeycomb where this
					// onAnimationEnd listener can be called even before the
					// animation is really complete.
					if (Build.VERSION.SDK_INT >= TiC.API_LEVEL_HONEYCOMB) {
						animationProxy.fireEvent(TiC.EVENT_COMPLETE, null);
					} else {
						Looper.myQueue().addIdleHandler(
								new MessageQueue.IdleHandler()
								{
									public boolean queueIdle()
									{
										animationProxy.fireEvent(
												TiC.EVENT_COMPLETE, null);
										return false;
									}
								});
					}
				}
			}

		}

		public void onAnimationRepeat(Animator animator)
		{
		}

		public void onAnimationStart(Animator animator)
		{
			if (animationProxy != null) {
				animationProxy.fireEvent(TiC.EVENT_START, null);
			}
		}

	}

	/**
	 * Listener for "classic" view animations (e.g., pre-Honeycomb.)
	 */
	protected class AnimationListener implements Animation.AnimationListener
	{
		@SuppressWarnings("unchecked")
		public void onAnimationEnd(Animation a)
		{
			if (relayoutChild) {
				// Do it only for TiCompositeLayout.LayoutParams, for border views
				// height and width are defined as 'MATCH_PARENT' and no change is
				// needed
				if (view.getLayoutParams() instanceof TiCompositeLayout.LayoutParams) {
					LayoutParams params = (LayoutParams) view.getLayoutParams();
					TiConvert.fillLayout(options, params);
					view.setLayoutParams(params);
				}
				view.clearAnimation();
				relayoutChild = false;
				// TIMOB-11298 Propagate layout property changes to proxy
				for (Object key : options.keySet()) {
					if (TiC.PROPERTY_TOP.equals(key)
							|| TiC.PROPERTY_BOTTOM.equals(key)
							|| TiC.PROPERTY_LEFT.equals(key)
							|| TiC.PROPERTY_RIGHT.equals(key)
							|| TiC.PROPERTY_CENTER.equals(key)
							|| TiC.PROPERTY_WIDTH.equals(key)
							|| TiC.PROPERTY_HEIGHT.equals(key)
							|| TiC.PROPERTY_BACKGROUND_COLOR.equals(key)) {
						viewProxy.setProperty((String) key, options.get(key));
					}
				}
			}

			if (applyOpacity
					&& (autoreverse == null || !autoreverse.booleanValue())) {
				// There is an android bug where animations still occur after
				// this method. We clear it from the view to
				// correct this.
				view.clearAnimation();
				if (toOpacity.floatValue() == 0) {
					view.setVisibility(View.INVISIBLE);

				} else {
					if (view.getVisibility() == View.INVISIBLE) {
						view.setVisibility(View.VISIBLE);
					}
					// this is apparently the only way to apply an opacity to
					// the entire view and have it stick
					AlphaAnimation aa = new AlphaAnimation(
							toOpacity.floatValue(), toOpacity.floatValue());
					aa.setDuration(1);
					aa.setFillAfter(true);
					view.setLayoutParams(view.getLayoutParams());
					view.startAnimation(aa);
				}

				applyOpacity = false;
			}

			if (a instanceof AnimationSet) {
				setAnimationRunningFor(view, false);
				if (callback != null) {
					callback.callAsync(viewProxy.getKrollObject(),
							new Object[] { new KrollDict() });
				}

				if (animationProxy != null) {
					// In versions prior to Honeycomb, don't fire the event
					// until the message queue is empty. There appears to be
					// a bug in versions before Honeycomb where this
					// onAnimationEnd listener can be called even before the
					// animation is really complete.
					if (Build.VERSION.SDK_INT >= TiC.API_LEVEL_HONEYCOMB) {
						animationProxy.fireEvent(TiC.EVENT_COMPLETE, null);
					} else {
						Looper.myQueue().addIdleHandler(
								new MessageQueue.IdleHandler()
								{
									public boolean queueIdle()
									{
										animationProxy.fireEvent(
												TiC.EVENT_COMPLETE, null);
										return false;
									}
								});
					}
				}
			}
		}

		public void onAnimationRepeat(Animation a)
		{
		}

		public void onAnimationStart(Animation a)
		{
			if (animationProxy != null) {
				animationProxy.fireEvent(TiC.EVENT_START, null);
			}
		}
	}

	/**
	 * Assemble and start (or schedule if delayed) the animation on the given
	 * view.
	 * @param viewProxy The Titanium view proxy for the view.
	 * @param view The native View
	 */
	public void start(TiViewProxy viewProxy, View view)
	{
		if (isAnimationRunningFor(view)) {
			if (!viewProxy.getOverrideCurrentAnimation()) {
				return;
			}
			//clear current animation
			view.clearAnimation();
		}
		// Indicate that an animation is running on this view.
		setAnimationRunningFor(view);

		this.view = view;
		this.viewProxy = viewProxy;

		if (tdm == null || tdm.canUsePropertyAnimators()) {
			// We can use Honeycomb+ property Animators via the
			// NineOldAndroids library.
			buildPropertyAnimators().start();
		} else {
			// We cannot use Honeycomb+ property Animators
			// because a matrix transform is too complicated
			// (see top of this file for explanation.)
			view.startAnimation(buildViewAnimations());
		}
	}

	private void setAnchor(int width, int height)
	{
		setAnchor(width, height, anchorX, anchorY);
	}

	private void setAnchor(int width, int height, float thisAnchorX,
			float thisAnchorY)
	{
		float pivotX = 0, pivotY = 0;

		if (thisAnchorX != Ti2DMatrix.DEFAULT_ANCHOR_VALUE) {
			pivotX = width * thisAnchorX;
		}

		if (thisAnchorY != Ti2DMatrix.DEFAULT_ANCHOR_VALUE) {
			pivotY = height * thisAnchorY;
		}

		if (PRE_HONEYCOMB) {
			setViewPivot(pivotX, pivotY);
		} else {
			setViewPivotHC(pivotX, pivotY);
		}
	}

	/**
	 * Determine whether an animation is currently
	 * running on the given view by checking the
	 * static map of views with running animations.
	 * @param v The native View.
	 * @return true if the map of running animations contains
	 * the View, false otherwise.
	 */
	public static boolean isAnimationRunningFor(View v)
	{
		if (sRunningViews.size() == 0) {
			return false;
		}

		// Not synchronized because we know it can only run on UI thread.
		for (WeakReference<View> viewRef : sRunningViews) {
			View refd = viewRef.get();
			if (v.equals(refd)) {
				return true;
			}
		}

		return false;

	}

	/**
	 * Indicate that an animation is currently running
	 * on the given view.
	 * @param v The native View.
	 */
	private static void setAnimationRunningFor(View v)
	{
		setAnimationRunningFor(v, true);
	}

	/**
	 * Indicate that an animation is or is not running for
	 * a given view by either adding it to or removing it
	 * from a map that tracks which views have animations
	 * currently running.
	 * @param v The native View.
	 * @param running Whether an animation is running or not.
	 */
	private static void setAnimationRunningFor(View v, boolean running)
	{
		if (running) {
			if (!isAnimationRunningFor(v)) {
				sRunningViews.add(new WeakReference<View>(v));
			}

		} else {
			WeakReference<View> toRemove = null;

			for (WeakReference<View> viewRef : sRunningViews) {
				View refd = viewRef.get();
				if (v.equals(refd)) {
					toRemove = viewRef;
					break;
				}
			}

			if (toRemove != null) {
				sRunningViews.remove(toRemove);
			}

		}
	}

	// Initialize the opacity to 1 when we are going to change it in
	// the animation. If the opacity of the view was initialized to
	// 0, the animation doesn't work at all. If it was initialized to
	// something less than 1.0, then it "works" but doesn't give the
	// expected results. The reason seems to be partially explained
	// here:
	// http://stackoverflow.com/a/11387049/67842
	// Basically, the AlphaAnimation is transforming the
	// *existing* alpha value of the view. So to do what we want it
	// to do, we need to start with a base of 1. Surprisingly, this
	// does not seem to show a blip if the opacity was less than
	// 1.0 to begin with.
	private void prepareOpacityForAnimation()
	{
		TiUIView tiView = viewProxy.peekView();
		if (tiView == null) {
			return;
		}
		tiView.setOpacity(1.0f);
	}

	public boolean isUsingPropertyAnimators()
	{
		return (tdm == null || tdm.canUsePropertyAnimators());
	}
}

