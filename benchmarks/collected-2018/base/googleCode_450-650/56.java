// https://searchcode.com/api/result/1515894/


package com.menny.android.anysoftkeyboard.keyboards;

import java.util.HashMap;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import com.menny.android.anysoftkeyboard.AnyKeyboardContextProvider;
import com.menny.android.anysoftkeyboard.AnySoftKeyboardConfiguration;
import com.menny.android.anysoftkeyboard.R;
import com.menny.android.anysoftkeyboard.Workarounds;

public abstract class AnyKeyboard extends Keyboard 
{
	private final static String TAG = "ASK - AK";
	protected class ShiftedKeyData
	{
		public final char ShiftCharacter;
		public final AnyKey KeyboardKey;
		
		public ShiftedKeyData(AnyKey key)
		{
			KeyboardKey = key;
			ShiftCharacter = (char) key.codes[1]; 
		}
	}
	public final static int KEYCODE_LANG_CHANGE = -99;
	public final static int KEYCODE_ALTER_LAYOUT = -98;
	public final static int KEYCODE_KEYBOARD_CYCLE = -97;
	public final static int KEYCODE_KEYBOARD_REVERSE_CYCLE = -96;
	
	public final static int KEYCODE_SMILEY = -10;
	
	public static final int KEYCODE_LEFT = -20;
	public static final int KEYCODE_RIGHT = -21;
	public static final int KEYCODE_UP = -22;
	public static final int KEYCODE_DOWN = -23;
	
	public static final int	KEYCODE_CTRL = -11;
	
	public interface HardKeyboardAction
	{
		int getKeyCode();
		boolean isAltActive();
		boolean isShiftActive();
		void setNewKeyCode(int keyCode);
	}
	
	public interface HardKeyboardTranslator
	{
		/*
		 * Gets the current state of the hard keyboard, and may change the output key-code.
		 */
		void translatePhysicalCharacter(HardKeyboardAction action);
	}
	
	private static final int SHIFT_OFF = 0;
    private static final int SHIFT_ON = 1;
    private static final int SHIFT_LOCKED = 2;
    
    private int mShiftState = SHIFT_OFF;
    
    private final boolean mDebug;
	private HashMap<Character, ShiftedKeyData> mSpecialShiftKeys;
    
    //private Drawable mShiftLockIcon;
    //private Drawable mShiftLockPreviewIcon;
    private final Drawable mOffShiftIcon;
    private final Drawable mOnShiftIcon;
    //private Drawable mOldShiftPreviewIcon;
    private final Key mShiftKey;
    private final EnterKey mEnterKey;
	private final Key mSmileyKey;
	private final Key mQuestionMarkKey;
	
	private final boolean mRightToLeftLayout;//the "super" ctor will create keys, and we'll set the correct value there.
	
    private final Context mKeyboardContext;
    private final AnyKeyboardContextProvider mASKContext;
    
    protected AnyKeyboard(AnyKeyboardContextProvider askContext, Context context,//note: the context can be from a different package!
    		int xmlLayoutResId) 
    {
        //should use the package context for creating the layout
        super(context, xmlLayoutResId);
        
        mDebug = AnySoftKeyboardConfiguration.getInstance().getDEBUG();
        mKeyboardContext = context;
        mASKContext = askContext;
        
        mOnShiftIcon = askContext.getApplicationContext().getResources().getDrawable(R.drawable.sym_keyboard_shift_on);
        mOffShiftIcon = askContext.getApplicationContext().getResources().getDrawable(R.drawable.sym_keyboard_shift);
        //going to revisit the keys to fix some stuff
        boolean rightToLeftLayout = false;
        EnterKey enterKey = null;
        Key shiftKey = null;
        Key smileyKey = null;
        Key questionKey = null;
        
        for(final Key key : getKeys())
        {
            if ((key.codes != null) && (key.codes.length > 0))
            {
                final int primaryCode = key.codes[0];
                //detecting LTR languages
                if (Workarounds.isRightToLeftCharacter((char)primaryCode))
                    rightToLeftLayout = true;//one is enough
                
                //creating less sensitive keys if required
                switch(primaryCode)
                {
                case 10:
                    enterKey = (EnterKey)key;
                    break;
                case KEYCODE_SHIFT: 
                    shiftKey = key;
                    break;
                case AnyKeyboard.KEYCODE_SMILEY: 
                    smileyKey = key;
                    break;
                case 63:
                    if (key.edgeFlags == Keyboard.EDGE_BOTTOM) 
                    {
                        questionKey = key;
                    }
                    break;
                case Keyboard.KEYCODE_MODE_CHANGE:
                case AnyKeyboard.KEYCODE_LANG_CHANGE:
                    final String keysMode = AnySoftKeyboardConfiguration.getInstance().getChangeLayoutKeysSize();
                    if (keysMode.equals("None"))
                    {
                        key.label = null;
                        key.height = 0;
                        key.width = 0;
                    }
                    else if (keysMode.equals("Big"))
                    {
                        String keyText = (primaryCode == Keyboard.KEYCODE_MODE_CHANGE)?
                                askContext.getApplicationContext().getString(R.string.change_symbols_regular) :
                                    askContext.getApplicationContext().getString(R.string.change_lang_regular);
                        key.label = keyText;
                        //key.height *= 1.5;
                    }
                    else
                    {
                        String keyText = (primaryCode == Keyboard.KEYCODE_MODE_CHANGE)?
                                askContext.getApplicationContext().getString(R.string.change_symbols_wide) :
                                    askContext.getApplicationContext().getString(R.string.change_lang_wide);
                        key.label = keyText;
                    }
                    break;
                    default:
                        //setting the character label
                        if (isAlphabetKey(key))
                        {
                            key.label = ""+((char)primaryCode); 
                        }
                }
            }
        }
        mEnterKey = enterKey;
        mShiftKey = shiftKey;
        mSmileyKey = smileyKey;
        mQuestionMarkKey = questionKey;
        mRightToLeftLayout = rightToLeftLayout;
    }
    
    protected AnyKeyboardContextProvider getASKContext()
    {
        return mASKContext;
    }

	protected Context getKeyboardContext()
    {
    	return mKeyboardContext;
    }
    
    public abstract String getDefaultDictionaryLanguage();
    
    //this function is called from within the super constructor.
    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, 
            XmlResourceParser parser) {
    	if (mSpecialShiftKeys == null) mSpecialShiftKeys = new HashMap<Character, ShiftedKeyData>();
    	
    	AnyKey key = new AnyKey(res, parent, x, y, parser);
    	
        if ((key.codes != null) && (key.codes.length > 0))
        {
        	final int primaryCode = key.codes[0];
    		
        	//creating less sensitive keys if required
        	switch(primaryCode)
        	{
        	case 10://enter
        		key = new EnterKey(res, parent, x, y, parser);
        		break;
        	case KEYCODE_DELETE://delete
        	case KEYCODE_SHIFT://shift
        		key = new LessSensitiveAnyKey(res, parent, x, y, parser);
        		break;
	        }
        }
        
        if (mDebug)
        {
        	final int primaryKey = ((key.codes != null) && key.codes.length > 0)?
        			key.codes[0] : -1;
        	Log.v(TAG, "Key '"+primaryKey+"' will have - width: "+key.width+", height:"+key.height+", text: '"+key.label+"'.");
        }
        
        setPopupKeyChars(key);
        
        if ((key.codes != null) && (key.codes.length > 1))
        {
        	final int primaryCode = key.codes[0];
        	if ((primaryCode>0) && (primaryCode<Character.MAX_VALUE))
        	{
        		Character primary = new Character((char)primaryCode);
        		ShiftedKeyData keyData = new ShiftedKeyData(key);
	        	if (!mSpecialShiftKeys.containsKey(primary))
	        		mSpecialShiftKeys.put(primary, keyData);
	        	if (mDebug)
	            	Log.v(TAG, "Adding mapping ("+primary+"->"+keyData.ShiftCharacter+") to mSpecialShiftKeys.");
	        }
        }
        		
        return key;
    }

    @Override
    protected Row createRowFromXml(Resources res, XmlResourceParser parser) 
    {
    	Row aRow = super.createRowFromXml(res, parser);
    	if ((aRow.rowEdgeFlags&EDGE_TOP) != 0)
    	{
    		String layoutChangeType = AnySoftKeyboardConfiguration.getInstance().getChangeLayoutKeysSize();
    		//top row
    		if (layoutChangeType.equals("None"))
    			aRow.defaultHeight = 0;
    		else if (layoutChangeType.equals("Big"))
    			aRow.defaultHeight *= 1.5;
    	}
    	
    	if (res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
    		aRow.defaultHeight = (int)(aRow.defaultHeight * AnySoftKeyboardConfiguration.getInstance().getKeysHeightFactorInPortrait());
    	else if (res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
    		aRow.defaultHeight = (int)(aRow.defaultHeight * AnySoftKeyboardConfiguration.getInstance().getKeysHeightFactorInLandscape());
    		
    	return aRow;
    }
    
    private boolean isAlphabetKey(Key key) {
		return  (!key.modifier) && 
				(!key.sticky) &&
				(!key.repeatable) &&
				(key.icon == null) &&
				(key.codes[0] > 0);
	}

    public boolean isLetter(char keyValue)
    {
    	return (Character.isLetter(keyValue) || (keyValue == '\''));
    }
	/**
     * This looks at the ime options given by the current editor, to set the
     * appropriate label on the keyboard's enter key (if it has one).
     */
    public void setImeOptions(Resources res, int options) {
    	if (mDebug)
    		Log.d(TAG, "AnyKeyboard.setImeOptions");
        if (mEnterKey == null) {
            return;
        }
        mEnterKey.enable();
        //sometimes, the OS will request the IME to make ENTER disappear...
        //we will respect that.
        //I hope that the GUI will provide a different option for ACTION
        //NOTE: TextView will set this flag in multi-line inputs.
        //but in these cases we DO want it to be available.
        final boolean NO_ENTER_ACTION = ((options&EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0);
        
    	final int action = (options&EditorInfo.IME_MASK_ACTION);
    	
    	if (AnySoftKeyboardConfiguration.getInstance().getDEBUG()) 
    		Log.d(TAG, "Input Connection ENTER key with action: "+action + " and NO_ACTION flag is: "+NO_ENTER_ACTION);
    	
        switch (action) {
            case EditorInfo.IME_ACTION_GO:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                //there is a problem with LTR languages
                mEnterKey.label = Workarounds.workaroundCorrectStringDirection(res.getText(R.string.label_go_key));
                break;
            case EditorInfo.IME_ACTION_NEXT:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
              //there is a problem with LTR languages
                mEnterKey.label = Workarounds.workaroundCorrectStringDirection(res.getText(R.string.label_next_key));
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_search);
                mEnterKey.label = null;
                break;
            case EditorInfo.IME_ACTION_SEND:
            	if (NO_ENTER_ACTION)
            	{
            		Log.d(TAG, "Disabling the ENTER key, since this is a SEND action, and OS requested no mistakes.");
            		mEnterKey.disable();
            	}
            	else
            	{
	                mEnterKey.iconPreview = null;
	                mEnterKey.icon = null;
	                //there is a problem with LTR languages
	                mEnterKey.label = Workarounds.workaroundCorrectStringDirection(res.getText(R.string.label_send_key));
            	}
                break;
            default:
                mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_return);
                mEnterKey.label = null;
                break;
        }
    }
    
    public abstract int getKeyboardNameResId();
    
    public String getKeyboardName()
    {
        return mKeyboardContext.getResources().getString(getKeyboardNameResId());
    }
    
    public boolean isLeftToRightLanguage()
    {
    	return !mRightToLeftLayout;
    }
    
    public abstract int getKeyboardIconResId();
    
	public void setShiftLocked(boolean shiftLocked) {
        if (mShiftKey != null) {
        	if (mDebug) Log.d(TAG, "setShiftLocked: Switching to locked: "+shiftLocked);
        	mShiftKey.on = shiftLocked;
        	if (shiftLocked)
        		mShiftState = SHIFT_LOCKED;
        }
    }
    
    @Override
    public boolean isShifted() {
        if (mShiftKey != null) {
            return mShiftState != SHIFT_OFF;
        } else {
            return super.isShifted();
        }
    }
    
	@Override
	public boolean setShifted(boolean shiftState) 
	{
		final boolean superResult = super.setShifted(shiftState);
		final boolean changed = (shiftState == (mShiftState == SHIFT_OFF));
		
		if (mDebug) Log.d(TAG, "setShifted: shiftState:"+shiftState+". super result:"+superResult + " changed: "+changed);
		
		if (changed || superResult)
		{//layout changed. Need to change labels.
			mShiftState = shiftState? SHIFT_ON : SHIFT_OFF;
			
			//going over the special keys only.
			for(ShiftedKeyData data : mSpecialShiftKeys.values())
			{
				onKeyShifted(data, shiftState);
			}
			
			if (mShiftKey != null) {
	            if (shiftState) {
	            	if (mDebug) Log.d(TAG, "Switching to regular ON shift icon - shifted");
	            	mShiftKey.icon = mOnShiftIcon;
	            } else {
	            	if (mDebug) Log.d(TAG, "Switching to regular OFF shift icon - un-shifted");
	            	mShiftKey.icon = mOffShiftIcon;
	            }
	            //making sure it is off. Only caps turn it on
	            mShiftKey.on = false;
	        }
			return true;
		}
		else
			return false;
	}
	
	public boolean isShiftLocked() {
		return mShiftState == SHIFT_LOCKED;
	}

	protected void onKeyShifted(ShiftedKeyData data, boolean shiftState) 
	{
		AnyKey aKey = data.KeyboardKey;
		aKey.label = shiftState? ""+data.ShiftCharacter : ""+((char)aKey.codes[0]);
	}
	
	protected void setPopupKeyChars(Key aKey) 
	{
		if (aKey.popupResId > 0)
			return;//if the keyboard XML already specified the popup, then no need to override
		
		if ((aKey.codes != null) && (aKey.codes.length > 0))
        {
			switch(((char)aKey.codes[0]))
			{
			case '\''://in the generic bottom row
				aKey.popupResId = R.xml.popup;
				aKey.popupCharacters = "\"\u201e\u201d";
				break;
			case '-':
				aKey.popupResId = R.xml.popup;
				aKey.popupCharacters = "\u2013";
				break;
			case '.'://in the generic bottom row
				aKey.popupResId = R.xml.popup;
				aKey.popupCharacters = ";:-_\u00b7\u2026";
				break;
			case ','://in the generic bottom row
				aKey.popupResId = R.xml.popup;
				aKey.popupCharacters = "()";
				break;
			case '_':
				aKey.popupResId = R.xml.popup;
				aKey.popupCharacters = ",-";
				break;
			//the two below are switched in regular and Internet mode
			case '?'://in the generic bottom row
				aKey.popupResId = R.xml.popup;
				aKey.popupCharacters = "!/@\u00bf\u00a1";
				break;
			case '@'://in the generic Internet mode
				aKey.popupResId = R.xml.popup;
				aKey.popupCharacters = "!/?\u00bf\u00a1";
				break;
			}
        }
	}

	public void setTextVariation(Resources res, int inputType) 
	{
		if (mDebug)
    		Log.d(TAG, "setTextVariation");
		int variation = inputType &  EditorInfo.TYPE_MASK_VARIATION;
		
		switch (variation) {
	        case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
	        case EditorInfo.TYPE_TEXT_VARIATION_URI:
	        	if (mSmileyKey != null)
	        	{
	        		//Log.d("AnySoftKeyboard", "Changing smiley key to domains.");
	        		mSmileyKey.iconPreview = null;// res.getDrawable(sym_keyboard_key_domain_preview);
	        		mSmileyKey.icon = res.getDrawable(R.drawable.sym_keyboard_key_domain);
		        	mSmileyKey.label = null;
		        	mSmileyKey.text = AnySoftKeyboardConfiguration.getInstance().getDomainText();
		        	mSmileyKey.popupResId = R.xml.popup_domains;
	        	}
	        	if (mQuestionMarkKey != null)
	        	{
	        		//Log.d("AnySoftKeyboard", "Changing question mark key to AT.");
		        	mQuestionMarkKey.codes[0] = (int)'@';
		        	mQuestionMarkKey.label = "@";
		        	mQuestionMarkKey.popupCharacters = "!/?\u00bf\u00a1";
	        	}
	        	break;
	        default:
	        	if (mSmileyKey != null)
	        	{
	        		//Log.d("AnySoftKeyboard", "Changing smiley key to smiley.");
	        		mSmileyKey.icon = res.getDrawable(R.drawable.sym_keyboard_smiley);
		        	mSmileyKey.label = null;
		        	mSmileyKey.text = null;// ":-) ";
		        	mSmileyKey.popupResId = R.xml.popup_smileys;
	        	}
	        	if (mQuestionMarkKey != null)
	        	{
	        		//Log.d("AnySoftKeyboard", "Changing question mark key to question.");
		        	mQuestionMarkKey.codes[0] = (int)'?';
		        	mQuestionMarkKey.label = "?";
		        	mQuestionMarkKey.popupCharacters = "!/@\u00bf\u00a1";
	        	}
	        	break;
        }
	}
	
	public int getShiftedKeyValue(int primaryCode) 
	{
		if ((primaryCode>0) && (primaryCode<Character.MAX_VALUE))
		{
			Character c = new Character((char)primaryCode);
			if (mSpecialShiftKeys.containsKey(c))
			{
				char shifted = mSpecialShiftKeys.get(c).ShiftCharacter;
				if (mDebug)
		        	Log.v(TAG, "Returned the shifted mapping ("+c+"->"+shifted+") from mSpecialShiftKeys.");
				return shifted;
			}
		}
		//else...best try.
		return Character.toUpperCase(primaryCode);
	}
	
	class AnyKey extends Keyboard.Key {
        //private boolean mShiftLockEnabled;
        
        public AnyKey(Resources res, Keyboard.Row parent, int x, int y, 
                XmlResourceParser parser) {
            super(res, parent, x, y, parser);
            if (popupCharacters != null && popupCharacters.length() == 0) {
                // If there is a keyboard with no keys specified in popupCharacters
                popupResId = 0;
            }
        }
        
//        void enableShiftLock() {
//            mShiftLockEnabled = true;
//        }
//
//        @Override
//        public void onReleased(boolean inside) {
//            if (!mShiftLockEnabled) {
//                super.onReleased(inside);
//            } else {
//                pressed = !pressed;
//            }
//        }
    }
	
	private class LessSensitiveAnyKey extends AnyKey {
        
		private int mStartX;
		private int mStartY;
		private int mEndX;
		private int mEndY;
		
        public LessSensitiveAnyKey(Resources res, Keyboard.Row parent, int x, int y, 
                XmlResourceParser parser) {
            super(res, parent, x, y, parser);
            mStartX = this.x;
            mStartY = this.y;
            mEndX = this.width + this.x;
            mEndY = this.height + this.y;
        	
            if ((this.edgeFlags & Keyboard.EDGE_BOTTOM) != 0)
            {//the enter key!
            	//we want to "click" it only if it in the lower
        		mStartY += (this.height * 0.15);
            }
            else
            {
	            if ((this.edgeFlags & Keyboard.EDGE_LEFT) != 0)
	            {//usually, shift
	            	mEndX -= (this.width * 0.1);
	            }
	            
	            if ((this.edgeFlags & Keyboard.EDGE_RIGHT) != 0)
	            {//usually, delete
	            	//this is below the ENTER.. We want to be careful with this.
	            	mStartY += (this.height * 0.05);
	        		mEndY -= (this.height * 0.05);
	        		mStartX += (this.width * 0.15);
	            }
            }
        }
        
        
         /**
         * Overriding this method so that we can reduce the target area for certain keys.
         */
        @Override
        public boolean isInside(int clickedX, int clickedY) 
        {
        	return 	clickedX >= mStartX &&
				clickedX <= mEndX &&
				clickedY >= mStartY &&
				clickedY <= mEndY;
        }
    }

	private class EnterKey extends LessSensitiveAnyKey
	{
		private final int mOriginalHeight;
		private boolean mEnabled;
		
		public EnterKey(Resources res, Row parent, int x, int y,
				XmlResourceParser parser) {
			super(res, parent, x, y, parser);
			mOriginalHeight = this.height;
			mEnabled = true;
		}
		
		public void disable()
		{
			if (AnySoftKeyboardConfiguration.getInstance().getActionKeyInvisibleWhenRequested())
				this.height = 0;
			
			iconPreview = null;
            icon = null;
            label = "  ";//can not use NULL.
            mEnabled = false;
		}
		
		public void enable()
		{
			this.height = mOriginalHeight;
			mEnabled = true;
		}
		
		@Override
		public boolean isInside(int clickedX, int clickedY) {
			if (mEnabled)
				return super.isInside(clickedX, clickedY);
			else
				return false;//disabled.
		}
	}
	
	public abstract String getKeyboardPrefId();
}

