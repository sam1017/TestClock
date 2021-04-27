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

package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.support.v7.widget.AppCompatImageView;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * This widget display an analog clock with two hands for hours and minutes.
 */
public class AnalogClock extends FrameLayout {

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                final String tz = intent.getStringExtra("time-zone");
                mTime = Calendar.getInstance(TimeZone.getTimeZone(tz));
            }
            onTimeChanged();
        }
    };

    private final Runnable mClockTick = new Runnable() {
        @Override
        public void run() {
            onTimeChanged();

            if (mEnableSeconds) {
                final long now = System.currentTimeMillis();
                final long delay = SECOND_IN_MILLIS - now % SECOND_IN_MILLIS;
                postDelayed(this, delay);
            }
        }
    };

    private final ImageView mHourHand;
    private final ImageView mMinuteHand;
    private final ImageView mSecondHand;

    private Calendar mTime;
    private String mDescFormat;
    private TimeZone mTimeZone;
    private boolean mEnableSeconds = true;

    public AnalogClock(Context context) {
        this(context, null /* attrs */);
    }

    public AnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0 /* defStyleAttr */);
    }

    public AnalogClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTime = Calendar.getInstance();
        mDescFormat = ((SimpleDateFormat) DateFormat.getTimeFormat(context)).toLocalizedPattern();

        // Must call mutate on these instances, otherwise the drawables will blur, because they're
        // sharing their size characteristics with the (smaller) world cities analog clocks.
        final ImageView dial = new AppCompatImageView(context);
        dial.setImageResource(Utils.isBvOS()?
                ThemeUtils.isDarkTheme(context)? R.mipmap.bv_clock_analog_dial_dark : R.mipmap.bv_clock_analog_dial
                : R.drawable.clock_analog_dial);
        dial.getDrawable().mutate();
        addView(dial);

        mHourHand = new AppCompatImageView(context);
        mHourHand.setImageResource(Utils.isBvOS()? R.mipmap.bv_clock_analog_hour : R.drawable.clock_analog_hour);
        mHourHand.getDrawable().mutate();
        addView(mHourHand);

        mMinuteHand = new AppCompatImageView(context);
        mMinuteHand.setImageResource(Utils.isBvOS()? R.mipmap.bv_clock_analog_minute : R.drawable.clock_analog_minute);
        mMinuteHand.getDrawable().mutate();
        addView(mMinuteHand);

        mSecondHand = new AppCompatImageView(context);
        mSecondHand.setImageResource(Utils.isBvOS()? R.mipmap.bv_clock_analog_second : R.drawable.clock_analog_second);
        mSecondHand.getDrawable().mutate();
        addView(mSecondHand);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(mIntentReceiver, filter);

        // Refresh the calendar instance since the time zone may have changed while the receiver
        // wasn't registered.
        mTime = Calendar.getInstance(mTimeZone != null ? mTimeZone : TimeZone.getDefault());
        onTimeChanged();

        // Tick every second.
        if (mEnableSeconds) {
            mClockTick.run();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        getContext().unregisterReceiver(mIntentReceiver);
        removeCallbacks(mClockTick);
    }

    private void onTimeChanged() {
        mTime.setTimeInMillis(System.currentTimeMillis());
        // bv wangquanlong modify for bug#5665 start
        //final float hourAngle = mTime.get(Calendar.HOUR) * 30f;
        final float hourAngle = mTime.get(Calendar.HOUR) * 30f + mTime.get(Calendar.MINUTE) * 0.5f;
        mHourHand.setRotation(hourAngle);
        //final float minuteAngle = mTime.get(Calendar.MINUTE) * 6f;
        final float minuteAngle = mTime.get(Calendar.MINUTE) * 6f + mTime.get(Calendar.SECOND) * 0.1f;
        // bv wangquanlong modify for bug#5665 end
        mMinuteHand.setRotation(minuteAngle);
        if (mEnableSeconds) {
            final float secondAngle = mTime.get(Calendar.SECOND) * 6f;
            mSecondHand.setRotation(secondAngle);
        }
    }

    public void setTimeZone(String id) {
        mTimeZone = TimeZone.getTimeZone(id);
        mTime.setTimeZone(mTimeZone);
        onTimeChanged();
    }

    public void enableSeconds(boolean enable) {
        mEnableSeconds = enable;
        if (mEnableSeconds) {
            mSecondHand.setVisibility(VISIBLE);
            mClockTick.run();
        } else {
            mSecondHand.setVisibility(GONE);
        }
    }
}
