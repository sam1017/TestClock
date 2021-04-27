/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.widget.TextView;
import android.os.SystemProperties;

import com.android.deskclock.uidata.UiDataModel;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * A controller which will format a provided time in millis to display as a stopwatch.
 */
public final class StopwatchTextController {

    private final TextView mMainTextView;
    private final TextView mHundredthsTextView;

    private long mLastTime = Long.MIN_VALUE;

    public StopwatchTextController(TextView mainTextView, TextView hundredthsTextView) {
        mMainTextView = mainTextView;
        mHundredthsTextView = hundredthsTextView;
    }

    public void setTimeString(long accumulatedTime) {
        // Since time is only displayed to centiseconds, if there is a change at the milliseconds
        // level but not the centiseconds level, we can avoid unnecessary work.
        if ((mLastTime / 10) == (accumulatedTime / 10)) {
            return;
        }

        final int hours = (int) (accumulatedTime / HOUR_IN_MILLIS);
        int remainder = (int) (accumulatedTime % HOUR_IN_MILLIS);

        final int minutes = (int) (remainder / MINUTE_IN_MILLIS);
        remainder = (int) (remainder % MINUTE_IN_MILLIS);

        final int seconds = (int) (remainder / SECOND_IN_MILLIS);
        remainder = (int) (remainder % SECOND_IN_MILLIS);

        mHundredthsTextView.setText(UiDataModel.getUiDataModel().getFormattedNumber(
                remainder / 10, 2));

        // Avoid unnecessary computations and garbage creation if seconds have not changed since
        // last layout pass.
        if ((mLastTime / SECOND_IN_MILLIS) != (accumulatedTime / SECOND_IN_MILLIS)) {
            final Context context = mMainTextView.getContext();
            final String time;
            //bv zhangjiachu modify bvGetTimeString 20200805 start
            if (Utils.isBvOS()){
                time = Utils.bvGetTimeString(context, hours, minutes, seconds);
                /*bv zhangjiachu add for fixbug:1959 调整秒表布局 20200821 start*/
                if (hours == 0) {
                    mMainTextView.setPadding(DensityUtil.dip2px(context, 30), 0, 0, 0);
                    mHundredthsTextView.setPadding(0, 0, DensityUtil.dip2px(context, 30), 0);
                } else { //bv zhangjiachu add else 解决hours>0时显示没有padding
                    mMainTextView.setPadding(0, 0, 0, 0);
                    mHundredthsTextView.setPadding(0, 0, 0, 0);
                }
                /*bv zhangjiachu add for fixbug:1959 调整秒表布局 20200821 end*/
            } else {
                time = Utils.getTimeString(context, hours, minutes, seconds);
            }
            //bv zhangjiachu modify bvGetTimeString 20200805 end
            mMainTextView.setText(time);
        }
        mLastTime = accumulatedTime;
    }
}
