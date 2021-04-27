/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.timer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.data.Timer;

/**
 * Custom view that draws timer progress as a circle.
 */
public final class BvTimerCircleView extends View {

    /** The size of the dot indicating the progress through the timer. */
    private final float mDotRadius;

    /** An amount to subtract from the true radius to account for drawing thicknesses. */
    private final float mRadiusOffset;

    /** The color indicating the remaining portion of the timer. */
    private final int mRemainderColor;

    /** The color indicating the completed portion of the timer. */
    private final int mCompletedColor;

    /** The size of the stroke that paints the timer circle. */
    private final float mStrokeSize;

    private final Paint mPaint = new Paint();
    private final Paint mFill = new Paint();
    private final RectF mArcRect = new RectF();

    private Timer mTimer;

    private Bitmap mBackgound;
    private Bitmap mSecond;
    private Bitmap mCircle;

    private int mXCenter;
    private int mYCenter;

    @SuppressWarnings("unused")
    public BvTimerCircleView(Context context) {
        this(context, null);
    }

    @SuppressLint("ResourceType")
    public BvTimerCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources resources = context.getResources();
        final float dotDiameter = resources.getDimension(R.dimen.circletimer_dot_size);

        mDotRadius = dotDiameter / 2f;
        mStrokeSize = resources.getDimension(R.dimen.circletimer_circle_size);
        mRadiusOffset = Utils.calculateRadiusOffset(mStrokeSize, dotDiameter, 0);

        mRemainderColor = Color.WHITE;
        //mCompletedColor = ThemeUtils.resolveColor(context, R.attr.colorAccent);
        mCompletedColor = resources.getColor(R.color.bv_windowBackground);

        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);

        mFill.setAntiAlias(true);
        mFill.setColor(mCompletedColor);
        mFill.setStyle(Paint.Style.FILL);
        //背景
        mBackgound = BitmapFactory.decodeResource(resources, R.mipmap.bv_timer_analog_dial);
        mSecond = BitmapFactory.decodeResource(resources, R.mipmap.bv_ic_timer_point);
        mCircle = BitmapFactory.decodeResource(resources, R.mipmap.bv_ic_timer_circle);
        mXCenter = mBackgound.getWidth()/2;
        mYCenter = mBackgound.getHeight()/2;

    }

    void update(Timer timer) {
        if (mTimer != timer) {
            mTimer = timer;
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mTimer == null) {
            return;
        }

        // Compute the size and location of the circle to be drawn.
        /*final int xCenter = getWidth() / 2;
        final int yCenter = getHeight() / 2;*/
        final float radius = Math.min(mXCenter, mYCenter) - mRadiusOffset;

        // Reset old painting state.
        mPaint.setColor(mRemainderColor);
        mPaint.setStrokeWidth(mStrokeSize);
        canvas.drawBitmap(mBackgound, 0, 0, mPaint);
        // If the timer is reset, draw a simple white circle.
        final float redPercent;
        if (mTimer.isReset()) {
            // Draw a complete white circle; no red arc required.
            //canvas.drawCircle(mXCenter, mYCenter, radius, mPaint);
            canvas.drawBitmap(mCircle, 0, 0, mPaint);
            canvas.drawBitmap(mSecond, 0, 0, mPaint);
            // Red percent is 0 since no timer progress has been made.
            redPercent = 0;
        } else if (mTimer.isExpired()) {
            mPaint.setColor(mCompletedColor);

            // Draw a complete white circle; no red arc required.
            //canvas.drawCircle(mXCenter, mYCenter, radius, mPaint);

            // Red percent is 1 since the timer has expired.
            canvas.drawBitmap(mCircle, 0, 0, mPaint);
            redPercent = 1;
            canvas.drawArc(mArcRect, 270 , -redPercent * 360, false, mPaint);
            canvas.drawBitmap(mSecond, 0, 0, mPaint);
        } else {
            // Draw a combination of red and white arcs to create a circle.
            mArcRect.top = mYCenter - radius;
            mArcRect.bottom = mYCenter + radius;
            mArcRect.left = mXCenter - radius;
            mArcRect.right = mXCenter + radius;
            redPercent =
                    Math.min(1, (float) mTimer.getElapsedTime() / (float) mTimer.getTotalLength());
            final float whitePercent = 1 - redPercent;
            //
            Matrix matrix = new Matrix();
            matrix.setRotate(-redPercent*360, mXCenter,mYCenter);
            canvas.drawBitmap(mCircle, matrix, mPaint);
            // Draw a white arc to indicate the amount of timer that remains.
            //canvas.drawArc(mArcRect, 270, whitePercent * 360, false, mPaint);

            // Draw a red arc to indicate the amount of timer completed.
            mPaint.setColor(mCompletedColor);
            canvas.drawArc(mArcRect, 270, -redPercent * 360, false, mPaint);
            matrix.setRotate(-redPercent*360, mXCenter,mYCenter);
            canvas.drawBitmap(mSecond, matrix, mPaint);
        }

        // Draw a red dot to indicate current progress through the timer.
        //final float dotAngleDegrees = 270 - redPercent * 360;
        //final double dotAngleRadians = Math.toRadians(dotAngleDegrees);
        //final float dotX = mXCenter + (float) (radius * Math.cos(dotAngleRadians));
        //final float dotY = mYCenter + (float) (radius * Math.sin(dotAngleRadians));
        //canvas.drawCircle(dotX, dotY, mDotRadius, mFill);

        if (mTimer.isRunning()) {
            postInvalidateOnAnimation();
        }
    }
}
