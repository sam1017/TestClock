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

package com.android.deskclock.alarms.dataadapter;

import android.os.Bundle;
/*hct-fankou add DeskClock LightTheme 20191113 start*/
import android.os.SystemProperties;

import com.android.deskclock.ItemAdapter;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.alarms.AlarmTimeClickHandler;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

public class AlarmItemHolder extends ItemAdapter.ItemHolder<Alarm> {

    private static final java.lang.String EXPANDED_KEY = "expanded";
    private final AlarmInstance mAlarmInstance;
    private final AlarmTimeClickHandler mAlarmTimeClickHandler;
    private boolean mExpanded;
    /*bv zhangjiachu add for alarm style start*/
    private boolean mIsCheck = false;

    public boolean ismIsCheck() {
        return mIsCheck;
    }

    public void setmIsCheck(boolean mIsCheck) {
        this.mIsCheck = mIsCheck;
    }

    private boolean mIsSeleted = false;

    public boolean ismSeleted() {
        return mIsSeleted;
    }

    public void setmIsSeleted(boolean mIsSeleted) {
        this.mIsSeleted = mIsSeleted;
    }


    /*bv zhangjiachu add for alarm style end*/
    public AlarmItemHolder(Alarm alarm, AlarmInstance alarmInstance,
            AlarmTimeClickHandler alarmTimeClickHandler) {
        super(alarm, alarm.id);
        mAlarmInstance = alarmInstance;
        mAlarmTimeClickHandler = alarmTimeClickHandler;
    }

    @Override
    public int getItemViewType() {
        /*hct-fankou add DeskClock LightTheme 20191113 start*/
        /*bv zhangjiachu modify for Utils.isBvOS()*/
        return isExpanded() ? (Utils.isBvOS() ?
                ExpandedAlarmViewHolder.HCT_VIEW_TYPE : ExpandedAlarmViewHolder.VIEW_TYPE)
                : (Utils.isBvOS() ?
                CollapsedAlarmViewHolder.HCT_VIEW_TYPE : CollapsedAlarmViewHolder.VIEW_TYPE);
    }

    public AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return mAlarmTimeClickHandler;
    }

    public AlarmInstance getAlarmInstance() {
        return mAlarmInstance;
    }

    public void expand() {
        if (!isExpanded()) {
            mExpanded = true;
            notifyItemChanged();
        }
    }

    public void collapse() {
        if (isExpanded()) {
            mExpanded = false;
            notifyItemChanged();
        }
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(EXPANDED_KEY, mExpanded);
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        mExpanded = bundle.getBoolean(EXPANDED_KEY);
    }
}
