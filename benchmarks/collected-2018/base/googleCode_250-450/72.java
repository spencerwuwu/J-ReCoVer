// https://searchcode.com/api/result/1514120/

/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.inputmethod.norwegian;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.view.inputmethod.EditorInfo;

public class NorwegianKeyboard extends Keyboard {

    private Drawable mShiftLockIcon;
    private Drawable mShiftLockPreviewIcon;
    private Drawable mOldShiftIcon;
    private Key mShiftKey;
    private Key mEnterKey;
    private Key mDelKey;
    private Key mSpaceKey;
    private Key mSymbolsKey;
    private Key[] mNumbersKeys;
    private Key mStarKey;
    private Key mPoundKey;
    
    private static final int SHIFT_OFF = 0;
    private static final int SHIFT_ON = 1;
    private static final int SHIFT_LOCKED = 2;
    
    private int mShiftState = SHIFT_OFF;
    
    static int sSpacebarVerticalCorrection;
    
    private boolean mDarkIcons;
    
    public NorwegianKeyboard(Context context, int xmlLayoutResId, boolean darkIcons) {
        this(context, xmlLayoutResId, 0, darkIcons);
    }

    public NorwegianKeyboard(Context context, int xmlLayoutResId, int mode, boolean darkIcons) {
        super(context, xmlLayoutResId, mode);
        mDarkIcons = darkIcons;
        Resources res = context.getResources();
        mShiftLockIcon = res.getDrawable(darkIcons ? R.drawable.sym_keyboard_shift_locked_dark : R.drawable.sym_keyboard_shift_locked);
        mShiftLockPreviewIcon = res.getDrawable(R.drawable.sym_keyboard_feedback_shift_locked);
        mShiftLockPreviewIcon.setBounds(0, 0, 
                mShiftLockPreviewIcon.getIntrinsicWidth(),
                mShiftLockPreviewIcon.getIntrinsicHeight());
        sSpacebarVerticalCorrection = res.getDimensionPixelOffset(
                R.dimen.spacebar_vertical_correction);
    }

    public NorwegianKeyboard(Context context, int layoutTemplateResId, 
            CharSequence characters, int columns, int horizontalPadding) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding);
    }

    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, 
            XmlResourceParser parser) {
        Key key = new NorwegianKey(res, parent, x, y, parser);
        if (key.codes[0] == 10) {
            mEnterKey = key;
        } else if (key.codes[0] == KEYCODE_DELETE) {
            mDelKey = key;
        } else if (key.codes[0] == NorwegianIME.KEYCODE_SPACE) {
            mSpaceKey = key;
        } else if (key.codes[0] == KEYCODE_MODE_CHANGE) {
            mSymbolsKey = key;
        } else if (key.codes[0] > 47 && key.codes[0] < 58) {
            if(mNumbersKeys == null)
                mNumbersKeys = new Key[10];
            mNumbersKeys[key.codes[0] - 48] = key;
        } else if (key.codes[0] == 42) {
            mStarKey = key;
        } else if (key.codes[0] == 35) {
            mPoundKey = key;
        }
        return key;
    }
    
    void setImeOptions(Resources res, int mode, int options) {
        setImeOptions(res, mode, options, mDarkIcons);
    }
    
    void setImeOptions(Resources res, int mode, int options, boolean darkIcons) {
        mDarkIcons = darkIcons;
        if (mEnterKey != null) {
            mEnterKey.text = null;
            switch (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
                case EditorInfo.IME_ACTION_GO:
                    mEnterKey.iconPreview = null;
                    mEnterKey.icon = null;
                    mEnterKey.label = res.getText(R.string.label_go_key);
                    break;
                case EditorInfo.IME_ACTION_NEXT:
                    mEnterKey.iconPreview = null;
                    mEnterKey.icon = null;
                    mEnterKey.label = res.getText(R.string.label_next_key);
                    break;
                case EditorInfo.IME_ACTION_DONE:
                    mEnterKey.iconPreview = null;
                    mEnterKey.icon = null;
                    mEnterKey.label = res.getText(R.string.label_done_key);
                    break;
                case EditorInfo.IME_ACTION_SEARCH:
                    mEnterKey.iconPreview = res.getDrawable(
                            R.drawable.sym_keyboard_feedback_search);
                    mEnterKey.icon = res.getDrawable(
                            darkIcons ? R.drawable.sym_keyboard_search_dark : R.drawable.sym_keyboard_search);
                    mEnterKey.label = null;
                    break;
                case EditorInfo.IME_ACTION_SEND:
                    mEnterKey.iconPreview = null;
                    mEnterKey.icon = null;
                    mEnterKey.label = res.getText(R.string.label_send_key);
                    break;
                default:
                    mEnterKey.iconPreview = res.getDrawable(
                            R.drawable.sym_keyboard_feedback_return);
                    mEnterKey.icon = res.getDrawable(
                            darkIcons ? R.drawable.sym_keyboard_return_dark : R.drawable.sym_keyboard_return);
                    mEnterKey.label = null;
                    if (mode == KeyboardSwitcher.MODE_IM)
                        mEnterKey.text = "\n";
                    break;
            }
            // Set the initial size of the preview icon
            if (mEnterKey.iconPreview != null) {
                mEnterKey.iconPreview.setBounds(0, 0, 
                        mEnterKey.iconPreview.getIntrinsicWidth(),
                        mEnterKey.iconPreview.getIntrinsicHeight());
            }
        }
        
        if(darkIcons) {
            if(mShiftKey != null)
                mShiftKey.icon = res.getDrawable(isShifted() ? R.drawable.sym_keyboard_shift_locked_dark : R.drawable.sym_keyboard_shift_dark);
            if(mDelKey !=null)
                mDelKey.icon = res.getDrawable(R.drawable.sym_keyboard_delete_dark);
            if(mSpaceKey != null)
                mSpaceKey.icon = res.getDrawable(R.drawable.sym_keyboard_space_dark);
            mOldShiftIcon = res.getDrawable(R.drawable.sym_keyboard_shift_dark);
            mShiftLockIcon = res.getDrawable(R.drawable.sym_keyboard_shift_locked_dark);
            
//            if(mode == KeyboardSwitcher.MODE_PHONE) {
                if(mSymbolsKey != null)
                    mSymbolsKey.icon = res.getDrawable(R.drawable.sym_keyboard_numalt_dark);
                if(mNumbersKeys != null) {
                    if(mNumbersKeys[0] != null)
                        mNumbersKeys[0].icon = res.getDrawable(R.drawable.sym_keyboard_num0_dark);
                    if(mNumbersKeys[1] != null)
                        mNumbersKeys[1].icon = res.getDrawable(R.drawable.sym_keyboard_num1_dark);
                    if(mNumbersKeys[2] != null)
                        mNumbersKeys[2].icon = res.getDrawable(R.drawable.sym_keyboard_num2_dark);
                    if(mNumbersKeys[3] != null)
                        mNumbersKeys[3].icon = res.getDrawable(R.drawable.sym_keyboard_num3_dark);
                    if(mNumbersKeys[4] != null)
                        mNumbersKeys[4].icon = res.getDrawable(R.drawable.sym_keyboard_num4_dark);
                    if(mNumbersKeys[5] != null)
                        mNumbersKeys[5].icon = res.getDrawable(R.drawable.sym_keyboard_num5_dark);
                    if(mNumbersKeys[6] != null)
                        mNumbersKeys[6].icon = res.getDrawable(R.drawable.sym_keyboard_num6_dark);
                    if(mNumbersKeys[7] != null)
                        mNumbersKeys[7].icon = res.getDrawable(R.drawable.sym_keyboard_num7_dark);
                    if(mNumbersKeys[8] != null)
                        mNumbersKeys[8].icon = res.getDrawable(R.drawable.sym_keyboard_num8_dark);
                    if(mNumbersKeys[9] != null)
                        mNumbersKeys[9].icon = res.getDrawable(R.drawable.sym_keyboard_num9_dark);
                }
                if(mStarKey != null)
                    mStarKey.icon = res.getDrawable(R.drawable.sym_keyboard_numstar_dark);
                if(mPoundKey != null)
                    mPoundKey.icon = res.getDrawable(R.drawable.sym_keyboard_numpound_dark);
//            }
        }
    }
    
    void enableShiftLock() {
        int index = getShiftKeyIndex();
        if (index >= 0) {
            mShiftKey = getKeys().get(index);
            if (mShiftKey instanceof NorwegianKey) {
                ((NorwegianKey)mShiftKey).enableShiftLock();
            }
            mOldShiftIcon = mShiftKey.icon;
        }
    }

    void setShiftLocked(boolean shiftLocked) {
        if (mShiftKey != null) {
            if (shiftLocked) {
                mShiftKey.on = true;
                mShiftKey.icon = mShiftLockIcon;
                mShiftState = SHIFT_LOCKED;
            } else {
                mShiftKey.on = false;
                mShiftKey.icon = mShiftLockIcon;
                mShiftState = SHIFT_ON;
            }
        }
    }

    boolean isShiftLocked() {
        return mShiftState == SHIFT_LOCKED;
    }
    
    @Override
    public boolean setShifted(boolean shiftState) {
        boolean shiftChanged = false;
        if (mShiftKey != null) {
            if (shiftState == false) {
                shiftChanged = mShiftState != SHIFT_OFF;
                mShiftState = SHIFT_OFF;
                mShiftKey.on = false;
                mShiftKey.icon = mOldShiftIcon;
            } else {
                if (mShiftState == SHIFT_OFF) {
                    shiftChanged = mShiftState == SHIFT_OFF;
                    mShiftState = SHIFT_ON;
                    mShiftKey.icon = mShiftLockIcon;
                }
            }
        } else {
            return super.setShifted(shiftState);
        }
        return shiftChanged;
    }
    
    @Override
    public boolean isShifted() {
        if (mShiftKey != null) {
            return mShiftState != SHIFT_OFF;
        } else {
            return super.isShifted();
        }
    }

    static class NorwegianKey extends Keyboard.Key {
        
        private boolean mShiftLockEnabled;
        
        public NorwegianKey(Resources res, Keyboard.Row parent, int x, int y, 
                XmlResourceParser parser) {
            super(res, parent, x, y, parser);
            if (popupCharacters != null && popupCharacters.length() == 0) {
                // If there is a keyboard with no keys specified in popupCharacters
                popupResId = 0;
            }
        }
        
        void enableShiftLock() {
            mShiftLockEnabled = true;
        }

        @Override
        public void onReleased(boolean inside) {
            if (!mShiftLockEnabled) {
                super.onReleased(inside);
            } else {
                pressed = !pressed;
            }
        }
        
        /**
         * Overriding this method so that we can reduce the target area for certain keys.
         */
        @Override
        public boolean isInside(int x, int y) {
            final int code = codes[0];
            if (code == KEYCODE_SHIFT ||
                    code == KEYCODE_DELETE) {
                y -= height / 10;
                if (code == KEYCODE_SHIFT) x += width / 6;
                if (code == KEYCODE_DELETE) x -= width / 6;
            } else if (code == NorwegianIME.KEYCODE_SPACE) {
                y += NorwegianKeyboard.sSpacebarVerticalCorrection;
            }
            return super.isInside(x, y);
        }
    }
}

