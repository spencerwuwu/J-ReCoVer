// https://searchcode.com/api/result/96595223/

package github.com.kikeEsteban.audioBrowser.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;


/**
 * TODO: document your custom view class.
 */
public class TimeScrollView extends View {
    private String mExampleString = "test"; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    Paint backgroundPaint;
    Paint screenPaint;
    Paint noLoopPaint;
    Paint noLoopOncePaint;
    Paint playbackPaint;
    Paint selectorLoopPaint;

    private float mScreenOffset;
    private float mScreenWidth;
    private float mStart;
    private float mEnd;
    private float mLoopMode;
    private float mPlayback;


    public static final int NO_LOOP_MODE = 0;
    public static final int CONTINUOUS_LOOP_MODE = 1;
    public static final int ONCE_LOOP_MODE = 2;

    public TimeScrollView(Context context) {
        super(context);
        init(null, 0);
    }

    public TimeScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public TimeScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.TimeScrollView, defStyle, 0);

        mExampleString = a.getString(
                R.styleable.TimeScrollView_exampleString);
        mExampleColor = a.getColor(
                R.styleable.TimeScrollView_exampleColor,
                mExampleColor);
        mExampleString = "test";
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(
                R.styleable.TimeScrollView_exampleDimension,
                mExampleDimension);

        if (a.hasValue(R.styleable.TimeScrollView_exampleDrawable)) {
            mExampleDrawable = a.getDrawable(
                    R.styleable.TimeScrollView_exampleDrawable);
            mExampleDrawable.setCallback(this);
        }

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        backgroundPaint = new Paint();
        backgroundPaint.setARGB(255,0,255,255);

        noLoopPaint = new Paint();
        noLoopPaint.setARGB(100,255,255,0);

        noLoopOncePaint = new Paint();
        noLoopOncePaint.setARGB(100,205,110,0);

        selectorLoopPaint = new Paint();
        selectorLoopPaint.setARGB(255,255,255,0);

        playbackPaint = new Paint();
        playbackPaint.setARGB(255,255,0,0);

        screenPaint = new Paint();
        screenPaint.setARGB(255,200,200,200);
        screenPaint.setStyle(Paint.Style.STROKE);

    }

    // Offset and width within [0 .. 1] range
    public synchronized void setData(float offset, float width, float start, float end, float loopMode, float playback){
        if(offset < 0)
            offset = 0;
        if(width < 0)
            width = 0;
        if (offset > 1)
            offset = 1;
        if (width > 1)
            width = 1;
        mScreenOffset = offset;
        mScreenWidth = width;
        mStart = start;
        mEnd = end;
        mLoopMode = loopMode;
        mPlayback = playback;
    }

    public synchronized float[] getData(){
        float[] data = new float[6];
        data[0] = mScreenOffset;
        data[1] = mScreenWidth;
        data[2] = mStart;
        data[3] = mEnd;
        data[4] = mLoopMode;
        data[5] = mPlayback;
        return data;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        // Draw the text.

        float[] data = getData();

        float offset = data[0]*canvas.getWidth();
        float width = data[1]*canvas.getWidth();
        float start = data[2]*canvas.getWidth();
        float end = data[3]*canvas.getWidth();
        if(end == canvas.getWidth())
            end -= 1;
        float loopMode = data[4];
        float playback = data[5]*canvas.getWidth();

        if(loopMode == NO_LOOP_MODE){
            canvas.drawRect(0,5*canvas.getHeight()/8,canvas.getWidth(),3*canvas.getHeight()/8,backgroundPaint);
        } else if(loopMode == CONTINUOUS_LOOP_MODE){
            canvas.drawRect(0,5*canvas.getHeight()/8,start,3*canvas.getHeight()/8,noLoopPaint);
            canvas.drawRect(start,5*canvas.getHeight()/8,end,3*canvas.getHeight()/8,backgroundPaint);
            canvas.drawRect(end,5*canvas.getHeight()/8,canvas.getWidth(),3*canvas.getHeight()/8,noLoopPaint);
        } else if(loopMode == ONCE_LOOP_MODE){
            canvas.drawRect(0,5*canvas.getHeight()/8,start,3*canvas.getHeight()/8,noLoopOncePaint);
            canvas.drawRect(start,5*canvas.getHeight()/8,end,3*canvas.getHeight()/8,backgroundPaint);
            canvas.drawRect(end,5*canvas.getHeight()/8,canvas.getWidth(),3*canvas.getHeight()/8,noLoopOncePaint);
        }

        canvas.drawRect(offset,5,offset+width,canvas.getHeight()-5,screenPaint);
        canvas.drawLine(start,5,start,canvas.getHeight()-5,selectorLoopPaint);
        canvas.drawLine(end,5,end,canvas.getHeight()-5,selectorLoopPaint);
        canvas.drawLine(playback,5,playback,canvas.getHeight()-5,playbackPaint);

        /*canvas.drawText(mExampleString,
                paddingLeft + (contentWidth - mTextWidth) / 2,
                paddingTop + (contentHeight + mTextHeight) / 2,
                mTextPaint);
*/
        // Draw the example drawable on top of the text.
        if (mExampleDrawable != null) {
            mExampleDrawable.setBounds(paddingLeft, paddingTop,
                    paddingLeft + contentWidth, paddingTop + contentHeight);
            mExampleDrawable.draw(canvas);
        }
    }

    /**
     * Gets the example string attribute value.
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return mExampleString;
    }



    /**
     * Gets the example drawable attribute value.
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }
}

