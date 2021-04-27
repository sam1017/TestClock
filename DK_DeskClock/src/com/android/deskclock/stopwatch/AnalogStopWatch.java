/*
* Author:zhangjiachu
* Creation time：20210303
* Function：AnalogStopWatch
*/
package com.android.deskclock.stopwatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Stopwatch;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;


public class AnalogStopWatch extends FrameLayout {

    private static final String TAG = "AnalogStopWatch";
    private static final Boolean DEBUG = false;

    private final Runnable mClockTick = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) {
                Log.d(TAG, "run: ++++");
            }
            onTimeChanged();
            final long delay = 100;
            postDelayed(this, delay);
        }
    };

    private ImageView mMinuteHand;
    private ImageView mSecondHand;

    private float secondAngle = 0f;
    private float minuteAngle = 0f;
    private Stopwatch mStopwatch;

    public AnalogStopWatch(Context context) {
        this(context, null /* attrs */);
    }

    public AnalogStopWatch(Context context, AttributeSet attrs) {
        this(context, attrs, 0 /* defStyleAttr */);
    }

    public AnalogStopWatch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final ImageView dial = new ImageView(context);
        dial.setImageResource(ThemeUtils.isDarkTheme(context)?
                R.mipmap.bv_stopwatch_analog_dial_dark : R.mipmap.bv_stopwatch_analog_dial);
        dial.getDrawable().mutate();
        addView(dial);

        mMinuteHand = new ImageView(context);
        mMinuteHand.setImageResource(R.mipmap.bv_stopwatch_analog_minute);
        mMinuteHand.getDrawable().mutate();
        addView(mMinuteHand);

        mSecondHand = new ImageView(context);
        mSecondHand.setImageResource(R.mipmap.bv_clock_analog_second);
        mSecondHand.getDrawable().mutate();
        addView(mSecondHand);
    }

    @Override
    protected void onAttachedToWindow() {
        if (DEBUG) {
            Log.d(TAG, "onAttachedToWindow: ");
        }
        super.onAttachedToWindow();
        getStopwatch();
        onTimeChanged();
        if (mStopwatch.isRunning()){
            mClockTick.run();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mClockTick);
    }

    private void getStopwatch(){
        mStopwatch = DataModel.getDataModel().getStopwatch();
    }

    private void onTimeChanged() {
        getStopwatch();
        final long totalTime = mStopwatch.getTotalTime();

        final int hours = (int) (totalTime / HOUR_IN_MILLIS);
        int remainder = (int) (totalTime % HOUR_IN_MILLIS);

        final int halfMinutes = (int) (remainder / (MINUTE_IN_MILLIS/2)); //每半分钟跳动一次
        remainder = (int) (remainder % MINUTE_IN_MILLIS);
        if (DEBUG) {
            Log.d(TAG, "onTimeChanged:  minutes : remainder = " + remainder);
        }

        //final int seconds = (int) (remainder / SECOND_IN_MILLIS);
        //remainder = (int) (remainder % SECOND_IN_MILLIS);
        final int seconds = (int) (remainder / 100); //每隔0.1秒跳动一次

        minuteAngle = halfMinutes * 6f;
        secondAngle = seconds * 0.6f;
        mMinuteHand.setRotation(minuteAngle);
        mSecondHand.setRotation(secondAngle);
        invalidate();
    }

    public void startTimer(){
        mClockTick.run();
    }

    public void resetTimer(){
        mClockTick.run();
    }

    public void pauseTimer(){
        removeCallbacks(mClockTick);
    }
}
