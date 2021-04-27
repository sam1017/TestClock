/*
* Author:zhangjiachu
* Creation time：20210306
* Function：TimerCircleFrame
* 此文件暂时弃用，使用BvTimerCircleView
*/
package com.android.deskclock.timer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.Timer;


public class TimerCircleFrame extends FrameLayout {

    private static final String TAG = "TimerCircleFrame";
    private static final Boolean DEBUG = true;


    private ImageView mSecondHand;

    private Timer mTimer;
    private float mDotDiameter;

    public TimerCircleFrame(Context context) {
        this(context, null /* attrs */);
    }

    public TimerCircleFrame(Context context, AttributeSet attrs) {
        this(context, attrs, 0 /* defStyleAttr */);
    }

    public TimerCircleFrame(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final ImageView dial = new ImageView(context);
        mDotDiameter = context.getResources().getDimension(R.dimen.circletimer_dot_size);
        dial.setImageResource(R.mipmap.bv_timer_analog_dial);
        dial.getDrawable().mutate();
        addView(dial);

        /*mMinuteHand = new ImageView(context);
        mMinuteHand.setImageResource(R.mipmap.bv_stopwatch_analog_minute);
        mMinuteHand.getDrawable().mutate();
        addView(mMinuteHand);*/

        mSecondHand = new ImageView(context);
        mSecondHand.setImageResource(R.mipmap.bv_ic_timer_point);
        mSecondHand.getDrawable().mutate();
        addView(mSecondHand);

        //CompletedCircle completedCircle = new CompletedCircle(context);
        //addView(completedCircle);
    }

    @Override
    protected void onAttachedToWindow() {
        if (DEBUG) {
            Log.d(TAG, "onAttachedToWindow: ");
        }
        super.onAttachedToWindow();
        onTimeChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void update(Timer timer) {
        if (DEBUG) {
            Log.d(TAG, "update: ");
        }
            onTimeChanged();
        if (mTimer != timer) {
            if (DEBUG){
                Log.d(TAG, "update: mTimer");
            }
            mTimer = timer;
            postInvalidateOnAnimation();
        }
    }

    private void onTimeChanged() {
        if (mTimer == null){
            return;
        }
        // If the timer is reset，复位至默认
        final float redPercent;
        if (mTimer.isReset()){
            redPercent = 0;
        } else if (mTimer.isExpired()){
            redPercent = 1;
        } else {
            redPercent =
                    Math.min(1, (float) mTimer.getElapsedTime() / (float) mTimer.getTotalLength());
        }

        final float secondAngle = -redPercent * 360f;
        mSecondHand.setRotation(secondAngle);

        if (mTimer.isRunning()){
            postInvalidateOnAnimation();
        }
    }
}
