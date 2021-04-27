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

package com.android.deskclock.settings;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
//hct-fangkou add for alarm paddingstart same to other start 20200407
import android.util.DisplayMetrics;
import android.widget.LinearLayout;
import android.widget.TextView;
//hct-fangkou add for alarm paddingstart same to other end 20200407

import com.android.deskclock.R;
import com.android.deskclock.RingtonePreviewKlaxon;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.media.AudioManager.STREAM_ALARM;

public class AlarmVolumePreference extends Preference {

    private static final long ALARM_PREVIEW_DURATION_MS = 2000;

    private SeekBar mSeekbar;
    private ImageView mAlarmIcon;
    private boolean mPreviewPlaying;
    //hct-fangkou add for alarm paddingstart same to other start 20200407
    private LinearLayout mAlarmVolume;
    private TextView mTitle;
    //hct-fangkou add for alarm paddingstart same to other end 20200407

    public AlarmVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final Context context = getContext();
        final AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);

        // Disable click feedback for this preference.
        holder.itemView.setClickable(false);

        mSeekbar = (SeekBar) holder.findViewById(R.id.alarm_volume_slider);
        mSeekbar.setMax(audioManager.getStreamMaxVolume(STREAM_ALARM));
        //*/hct.fangkou, 2019-11-16, add for min alarm volume.
        mSeekbar.setMin(audioManager.getStreamMinVolume(STREAM_ALARM));
        //*/
        mSeekbar.setProgress(audioManager.getStreamVolume(STREAM_ALARM));
        mAlarmIcon = (ImageView) holder.findViewById(R.id.alarm_icon);
        onSeekbarChanged();
        //hct-fangkou add for alarm paddingstart same to other start 20200407
        //setalarmvolumepaddingstart(context,holder);
        //hct-fangkou add for alarm paddingstart same to other end 20200407

        final ContentObserver volumeObserver = new ContentObserver(mSeekbar.getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                // Volume was changed elsewhere, update our slider.
                mSeekbar.setProgress(audioManager.getStreamVolume(STREAM_ALARM));
            }
        };

        mSeekbar.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                context.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI,
                        true, volumeObserver);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                context.getContentResolver().unregisterContentObserver(volumeObserver);
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioManager.setStreamVolume(STREAM_ALARM, progress, 0);
                }
                onSeekbarChanged();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mPreviewPlaying && seekBar.getProgress() != 0) {
                    // If we are not currently playing and progress is set to non-zero, start.
                    RingtonePreviewKlaxon.start(
                            context, DataModel.getDataModel().getDefaultAlarmRingtoneUri());
                    mPreviewPlaying = true;
                    seekBar.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            RingtonePreviewKlaxon.stop(context);
                            mPreviewPlaying = false;
                        }
                    }, ALARM_PREVIEW_DURATION_MS);
                }
            }
        });
    }

    //hct-fangkou add for alarm paddingstart same to other start 20200407
    private void setalarmvolumepaddingstart(Context context,PreferenceViewHolder holder) {
        mAlarmVolume = (LinearLayout) holder.findViewById(R.id.idalarmvolume);
        mTitle = (TextView) holder.findViewById(android.R.id.title);
        DisplayMetrics mDm = context.getResources().getDisplayMetrics();
        int densityDPI = mDm.densityDpi;
        int hdpi= mDm.heightPixels;
        int wdpi= mDm.widthPixels;
        int hwdpi=hdpi-wdpi;
        boolean isMuti= hwdpi>-300 && hwdpi<300;
        String sysDPI = SystemProperties.get("ro.sf.lcd_density");
        if(densityDPI==Integer.valueOf(sysDPI) && !isMuti &&
                context.getResources().getBoolean(R.bool.hct_alarmvolume_paddingstart_same)){
            mAlarmVolume.setPadding(
                    context.getResources().getDimensionPixelSize(R.dimen.hct_alarmvolume_paddingleft_size),
                    16,mAlarmVolume.getPaddingRight(),16);
            mTitle.setPadding(
                    context.getResources().getDimensionPixelSize(R.dimen.hct_alarmvolume_paddingleft_size),
                    16,mAlarmVolume.getPaddingRight(),16);
        }
    }
    //hct-fangkou add for alarm paddingstart same to other end 20200407

    private void onSeekbarChanged() {
        mSeekbar.setEnabled(doesDoNotDisturbAllowAlarmPlayback());
        /*hct-fankou add DeskClock LightTheme 20191128*/
        mAlarmIcon.setImageResource(mSeekbar.getProgress() == 0 ?
                R.drawable.ic_alarm_off_24dp :
                        ThemeUtils.isDarkTheme(getContext()) ?
                                R.drawable.hct_ic_alarm_small : R.drawable.ic_alarm_small);
    }

    private boolean doesDoNotDisturbAllowAlarmPlayback() {
        return !Utils.isNOrLater() || doesDoNotDisturbAllowAlarmPlaybackNPlus();
    }

    @TargetApi(Build.VERSION_CODES.N)
    private boolean doesDoNotDisturbAllowAlarmPlaybackNPlus() {
        final NotificationManager notificationManager = (NotificationManager)
                getContext().getSystemService(NOTIFICATION_SERVICE);
        return notificationManager.getCurrentInterruptionFilter() !=
                NotificationManager.INTERRUPTION_FILTER_NONE;
    }
}
