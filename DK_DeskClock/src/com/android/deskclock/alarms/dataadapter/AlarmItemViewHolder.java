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

import android.content.Context;
/*hct-fankou add DeskClock LightTheme 20191113 start*/
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.HandlerThread;
import android.os.SystemProperties;
/*hct-fankou add DeskClock LightTheme 20191113 end*/
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.deskclock.AlarmUtils;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.ItemAnimator;
import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.widget.TextTime;
import com.android.deskclock.Utils;
/*bv zhangjiachu add for alarm style 20200106 start*/
import java.util.ArrayList;
import com.android.deskclock.DeskClock;
import com.android.deskclock.AlarmClockFragment;
import android.widget.CheckBox;
import android.util.Log;
import com.android.internal.os.BackgroundThread;
/*bv zhangjiachu add for alarm style 20200106 end*/
/**
 * Abstract ViewHolder for alarm time items.
 */
public abstract class AlarmItemViewHolder extends ItemAdapter.ItemViewHolder<AlarmItemHolder>
        implements ItemAnimator.OnAnimateChangeListener {
    /*bv zhangjiachu add for alarm style 20200106 start*/
    private static final String TAG = "AlarmItemViewHolder";
	/*bv zhangjiachu add for alarm style 20200106 end*/
    private static final float CLOCK_ENABLED_ALPHA = 1f;
    /*bv zhangjiachu modify for alarm style 20200229 start*/
    private static final float CLOCK_DISABLED_ALPHA = 0.69f;
    private static final float BV_CLOCK_DISABLED_ALPHA = 0.4f;
    /*bv zhangjiachu modify for alarm style 20200229 end*/
    /*bv zhangjiachu del for alarm style 20200106 start*/
    //private ItemAdapter<AlarmItemHolder> mItemAdapter;
    /*bv zhangjiachu del for alarm style 20200106 end*/
    public static final float ANIM_STANDARD_DELAY_MULTIPLIER = 1f / 6f;
    public static final float ANIM_LONG_DURATION_MULTIPLIER = 2f / 3f;
    public static final float ANIM_SHORT_DURATION_MULTIPLIER = 1f / 4f;
    public static final float ANIM_SHORT_DELAY_INCREMENT_MULTIPLIER =
            1f - ANIM_LONG_DURATION_MULTIPLIER - ANIM_SHORT_DURATION_MULTIPLIER;
    public static final float ANIM_LONG_DELAY_INCREMENT_MULTIPLIER =
            1f - ANIM_STANDARD_DELAY_MULTIPLIER - ANIM_SHORT_DURATION_MULTIPLIER;

    public static final String ANIMATE_REPEAT_DAYS = "ANIMATE_REPEAT_DAYS";

    public final TextTime clock;
    public final CompoundButton onOff;
    public final ImageView arrow;
    public final TextView preemptiveDismissButton;
    //bv zhangjimeng added,
    private boolean mIsBvOS = Utils.isBvOS();

    public AlarmItemViewHolder(View itemView) {
        super(itemView);
        clock = (TextTime) itemView.findViewById(R.id.digital_clock);
        onOff = (CompoundButton) itemView.findViewById(R.id.onoff);
        arrow = (ImageView) itemView.findViewById(R.id.arrow);
        preemptiveDismissButton =
                (TextView) itemView.findViewById(R.id.preemptive_dismiss_button);
        preemptiveDismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlarmInstance alarmInstance = getItemHolder().getAlarmInstance();
                if (alarmInstance != null) {
                    getItemHolder().getAlarmTimeClickHandler().dismissAlarmInstance(alarmInstance);
                }
            }
        });
        onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                BackgroundThread.getHandler().post(() -> {
                    getItemHolder().getAlarmTimeClickHandler().setAlarmEnabled(
                            getItemHolder().item, checked);
                });
                float disAlpha = CLOCK_DISABLED_ALPHA;
                if (mIsBvOS) {
                    disAlpha = BV_CLOCK_DISABLED_ALPHA;
                }
                clock.setAlpha(checked ? CLOCK_ENABLED_ALPHA : disAlpha);
                //bv zhangjimeng 2020/07/20,fix bug:when check switch button dayweek label not
                updateOtherLabels(checked);
                //bv zhangjimeng fix end
            }
        });
    }

    //bv zhangjimeng 2020/07/20,fix bug:when check switch button dayweek label not updated
    protected void updateOtherLabels(boolean checked) {
        return;
    }
    //bv zhangjimeng fix end

    @Override
    protected void onBindItemView(final AlarmItemHolder itemHolder) {
        final Alarm alarm = itemHolder.item;
        bindOnOffSwitch(alarm);
        bindClock(alarm);
        final Context context = itemView.getContext();
        itemView.setContentDescription(clock.getText() + " " + alarm.getLabelOrDefault(context));
        if(mIsBvOS){
            arrow.setVisibility(View.INVISIBLE);  //bv zhangjiachu add for alarm style
        }
    }

    protected void bindOnOffSwitch(Alarm alarm) {
        if (onOff.isChecked() != alarm.enabled) {
            onOff.setChecked(alarm.enabled);
        }
    }

    protected void bindClock(Alarm alarm) {
        clock.setTime(alarm.hour, alarm.minutes);
        //bv xiaoye add for bv os flag 20200511 start
        float disAlpha = CLOCK_DISABLED_ALPHA;
        if(mIsBvOS){
            disAlpha = BV_CLOCK_DISABLED_ALPHA;
        }
        clock.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : disAlpha);
        //bv xiaoye add for bv os flag 20200511 end
    }

    protected boolean bindPreemptiveDismissButton(Context context, Alarm alarm,
            AlarmInstance alarmInstance) {
        boolean canBind = alarm.canPreemptivelyDismiss() && alarmInstance != null;
        if(Utils.isBvOS()){
            /***********bv zhangjiachu modify for alarm style start************/
            canBind = false;
            /***********bv zhangjiachu modify for alarm style end************/
        }
        if (canBind) {
            preemptiveDismissButton.setVisibility(View.VISIBLE);
            final String dismissText = alarm.instanceState == AlarmInstance.SNOOZE_STATE
                    ? context.getString(R.string.alarm_alert_snooze_until,
                            AlarmUtils.getAlarmText(context, alarmInstance, false))
                    : context.getString(R.string.alarm_alert_dismiss_text);
            preemptiveDismissButton.setText(dismissText);
            preemptiveDismissButton.setClickable(true);
            /*hct-fankou add DeskClock LightTheme 20191119 start*/
            final Drawable offIcon = Utils.getVectorDrawable(context,R.drawable.hct_ic_alarm_off_gray);
            if(ThemeUtils.isDarkTheme(context)) {
                preemptiveDismissButton.setTextColor(Color.parseColor(context.getString(R.string.hct_text_color)));
                preemptiveDismissButton.setCompoundDrawablesRelativeWithIntrinsicBounds(offIcon, null, null, null);
            }
            /*hct-fankou add DeskClock LightTheme 20191119 end*/
        } else {
            preemptiveDismissButton.setVisibility(View.GONE);
            preemptiveDismissButton.setClickable(false);
        }
        return canBind;
    }
}
