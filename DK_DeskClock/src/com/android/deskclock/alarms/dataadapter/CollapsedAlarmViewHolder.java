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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.SystemProperties;

import com.android.deskclock.ThemeUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;

import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import java.util.Calendar;
import java.util.List;
/*bv zhangjiachu add for alarm style start*/
import android.content.Intent;
import com.android.deskclock.alarms.AlarmSetupActivity;
import android.util.Log;
import com.android.deskclock.AlarmClockFragment;
/*bv zhangjiachu add for alarm style end*/
// bv zhangjiachu modify for debug bug id :302 2020-03-20 start
import android.widget.CheckBox;
// bv zhangjiachu modify for debug bug id :302 2020-03-20 end
//bv zhangjiachu modify for alarm dark style 20200602 start
import android.graphics.Color;
//bv zhangjiachu modify for alarm dark style 20200602 end
/**
 * A ViewHolder containing views for an alarm item in collapsed stated.
 */
public final class CollapsedAlarmViewHolder extends AlarmItemViewHolder {
    /*bv zhangjiachu add for alarm style start*/
    private static final String TAG = "CollapsedAlarmViewHolder";
    private boolean mIsBvOS = Utils.isBvOS();
	/*bv zhangjiachu modify for alarm style end*/

    /*bv zhangjiachu add for alarm style 20200229 start*/
    private static final float CLOCK_ENABLED_ALPHA = 1f;
    private static final float CLOCK_DISABLED_ALPHA = 0.4f;
    /*bv zhangjiachu modify for alarm style 20200229 end*/
    //bv zhangjiachu modify for fixbug 20200528 start
    //public static final int VIEW_TYPE = R.layout.alarm_time_collapsed;
    public static final int VIEW_TYPE;
    //bv zhangjiachu modify for fixbug 20200528 end
    /*hct-fankou add DeskClock LightTheme 20191113 start*/
    //bv xiaoye add for bv os flag 20200511 start
    public static final int HCT_VIEW_TYPE;
    static {
        if(Utils.isBvOS()){
            HCT_VIEW_TYPE = R.layout.bv_alarm_time_collapsed;
            //bv zhangjiachu add for fixbug 20200528 start
            VIEW_TYPE = R.layout.bv_alarm_time_collapsed;
            //bv zhangjiachu add for fixbug 20200528 end
        }else {
            HCT_VIEW_TYPE = R.layout.hct_alarm_time_collapsed;
            //bv zhangjiachu add for fixbug 20200528 start
            VIEW_TYPE = R.layout.alarm_time_collapsed;
            //bv zhangjiachu add for fixbug 20200528 end
        }
    }
    //bv xiaoye add for bv os flag 20200511 end

    private final TextView alarmLabel;
    public final TextView daysOfWeek;
    private final TextView upcomingInstanceLabel;
    private final View hairLine;
    // bv zhangjiachu modify for debug bug id :302 2020-03-20 start
    private CheckBox cbxSelect;
    // bv zhangjiachu modify for debug bug id :302 2020-03-20 end

    private CollapsedAlarmViewHolder(View itemView) {
        super(itemView);

        alarmLabel = (TextView) itemView.findViewById(R.id.label);
        daysOfWeek = (TextView) itemView.findViewById(R.id.days_of_week);
        upcomingInstanceLabel = (TextView) itemView.findViewById(R.id.upcoming_instance_label);
        hairLine = itemView.findViewById(R.id.hairline);

        if(mIsBvOS){
            // bv zhangjiachu modify for debug bug id :302 2020-03-20 start
            cbxSelect = (CheckBox) itemView.findViewById(R.id.cbx_choice);
            // bv zhangjiachu modify for debug bug id :302 2020-03-20 end
            //bv zhangjiachu modify for alarm dark style 20200602 start
            if(SystemProperties.getBoolean("sys.hctclock_lighttheme",false) || !ThemeUtils.isDarkTheme(itemView.getContext())){
                    alarmLabel.setTextColor(Color.parseColor(itemView.getContext().getString(R.string.no_control_text_color)));
                    daysOfWeek.setTextColor(Color.parseColor(itemView.getContext().getString(R.string.no_control_text_color)));
                    upcomingInstanceLabel.setTextColor(Color.parseColor(itemView.getContext().getString(R.string.no_control_text_color)));
                }
                //bv zhangjiachu modify for alarm dark style 20200602 end
        }

        // Expand handler
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsBvOS){
                    bvAlarmEditClickHandler(v);
                }else {
                      Events.sendAlarmEvent(R.string.action_expand_implied, R.string.label_deskclock);
                      getItemHolder().expand();
                }

            }
        });
        alarmLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*bv zhangjiachu modify for alarm style start*/
                //bv zhangjiachu add bvAlarmEditClickHandler 点击进入编辑闹钟 20200821
                if(mIsBvOS){
                    bvAlarmEditClickHandler(v);
                } else {
                      Events.sendAlarmEvent(R.string.action_expand_implied, R.string.label_deskclock);
                      getItemHolder().expand();
                }
                /*bv zhangjiachu modify for alarm style end*/
            }
        });
        arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*bv zhangjiachu modify for alarm style start*/
                if(!mIsBvOS){
                       Events.sendAlarmEvent(R.string.action_expand, R.string.label_deskclock);
                       getItemHolder().expand();
                }
                /*bv zhangjiachu modify for alarm style end*/
            }
        });
        // Edit time handler
        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsBvOS){
                    bvAlarmEditClickHandler(v);
                }else {
                    getItemHolder().getAlarmTimeClickHandler().onClockClicked(getItemHolder().item);
                    Events.sendAlarmEvent(R.string.action_expand_implied, R.string.label_deskclock);
                    getItemHolder().expand();
                }
                /*bv zhangjiachu modify for alarm style end*/
            }
        });

        if(mIsBvOS){
            // bv zhangjiachu modify for debug bug id :302 2020-03-20 start
            cbxSelect.setOnClickListener(new View.OnClickListener() {
                @Override

                public void onClick(View v) {
                    final Context context = v.getContext();
                    CheckBox checkBox=(CheckBox)v;
                    getItemHolder().setmIsSeleted(checkBox.isChecked());
                    if (checkBox.isChecked()){
                        AlarmClockFragment.mSelectNum++;
                    } else {
                        AlarmClockFragment.mSelectNum--;
                    }

                    CollapsedAlarmViewHolder.this.getItemHolder().notifyItemSelectedChanged();

                    Log.d("TAG", "onClick: getItemHolder().itemId =" +getItemHolder().itemId+" , "+checkBox.isChecked());
                }
            });
            // bv zhangjiachu modify for debug bug id :302 2020-03-20 end
        }

        itemView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /*bv zhangjiachu add 20200819 start*/
    private void bvAlarmEditClickHandler(View v){
        if (!AlarmClockFragment.mIsAlarmDelete) {
            final Alarm alarm = CollapsedAlarmViewHolder.this.getItemHolder().item;
            AlarmSetupActivity.bvSetSelectedAlarm(alarm);
            Intent intent = new Intent(v.getContext(), com.android.deskclock.alarms.AlarmSetupActivity.class);
            intent.putExtra("alarm", alarm);
            v.getContext().startActivity(intent);
        } else {// bv zhangjiachu modify for debug bug id :302 2020-03-20 start
            boolean cSelected = getItemHolder().ismSeleted();
            getItemHolder().setmIsSeleted(!cSelected);
            if (getItemHolder().ismSeleted()){
                AlarmClockFragment.mSelectNum++;
            } else {
                AlarmClockFragment.mSelectNum--;
            }
            cbxSelect.setChecked(getItemHolder().ismSeleted());
            CollapsedAlarmViewHolder.this.getItemHolder().notifyItemSelectedChanged();
        }
    }
    /*bv zhangjiachu add 20200819 end*/

    @Override
    protected void onBindItemView(AlarmItemHolder itemHolder) {
        super.onBindItemView(itemHolder);
        final Alarm alarm = itemHolder.item;
        final AlarmInstance alarmInstance = itemHolder.getAlarmInstance();
        final Context context = itemView.getContext();
        bindRepeatText(context, alarm);
        bindReadOnlyLabel(context, alarm);
        bindUpcomingInstance(context, alarm);
        bindPreemptiveDismissButton(context, alarm, alarmInstance);
        if(mIsBvOS){
            // bv zhangjiachu modify for debug bug id :302 2020-03-20 start
            bindAlarmEdit(itemHolder);
            // bv zhangjiachu modify for debug bug id :302 2020-03-20 end
        }
    }

    // bv zhangjiachu modify for debug bug id :302 2020-03-20 start
    private void bindAlarmEdit(AlarmItemHolder itemHolder) {
        Log.d(TAG, "bindClockEdit: mIsAlarmDelete = " + AlarmClockFragment.mIsAlarmDelete);
        if(AlarmClockFragment.mIsAlarmDelete) {
            onOff.setVisibility(View.GONE);
            cbxSelect.setVisibility(View.VISIBLE);
            cbxSelect.setChecked(itemHolder.ismSeleted());
        }else{
            onOff.setVisibility(View.VISIBLE);
            cbxSelect.setVisibility(View.GONE);
        }
    }
    // bv zhangjiachu modify for debug bug id :302 2020-03-20 end

    //bv zhangjimeng 2020/07/20,fix bug:when check switch button dayweek label not updated
    @Override
    protected void updateOtherLabels(boolean checked) {
        super.updateOtherLabels(checked);
        daysOfWeek.setAlpha(checked ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        upcomingInstanceLabel.setAlpha(checked ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        alarmLabel.setAlpha(checked ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        return;
    }
    //bv zhangjimeng fix end


    private void bindReadOnlyLabel(Context context, Alarm alarm) {
        if (alarm.label != null && alarm.label.length() != 0) {
            //bv zhangjiachu add ", " for label 20200731 start
            final String temp;
            temp = alarm.label + ", ";
            //alarmLabel.setText(alarm.label);
            alarmLabel.setText(temp);
            //bv zhangjiachu add ", " for label 20200731 end
            alarmLabel.setVisibility(View.VISIBLE);
            alarmLabel.setContentDescription(context.getString(R.string.label_description)
                    + " " + alarm.label);
        } else {
            alarmLabel.setVisibility(View.GONE);
        }
        if(mIsBvOS){
            /*bv zhangjiachu add for alarm style 20200229 start*/
            alarmLabel.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
            /*bv zhangjiachu add for alarm style 20200229 end*/
        }
    }

    private void bindRepeatText(Context context, Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            final Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
            final String daysOfWeekText = alarm.daysOfWeek.toString(context, weekdayOrder);
            daysOfWeek.setText(daysOfWeekText);

            final String string = alarm.daysOfWeek.toAccessibilityString(context, weekdayOrder);
            daysOfWeek.setContentDescription(string);

            daysOfWeek.setVisibility(View.VISIBLE);
        } else {
            daysOfWeek.setVisibility(View.GONE);
        }
        if(mIsBvOS){
            /*bv zhangjiachu add for alarm style 20200229 start*/
            daysOfWeek.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
            /*bv zhangjiachu add for alarm style 20200229 end*/
        }
    }

    private void bindUpcomingInstance(Context context, Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            upcomingInstanceLabel.setVisibility(View.GONE);
        } else {
            upcomingInstanceLabel.setVisibility(View.VISIBLE);
            final String labelText = Alarm.isTomorrow(alarm, Calendar.getInstance()) ?
                    context.getString(R.string.alarm_tomorrow) :
                    context.getString(R.string.alarm_today);
            upcomingInstanceLabel.setText(labelText);
            if(mIsBvOS){
                /*bv zhangjiachu add for alarm style 20200229 start*/
                upcomingInstanceLabel.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
                /*bv zhangjiachu add for alarm style 20200229 end*/
            }
        }
    }

    @Override
    public Animator onAnimateChange(List<Object> payloads, int fromLeft, int fromTop, int fromRight,
            int fromBottom, long duration) {
        /* There are no possible partial animations for collapsed view holders. */
        return null;
    }

    @Override
    public Animator onAnimateChange(final ViewHolder oldHolder, ViewHolder newHolder,
            long duration) {
        if (!(oldHolder instanceof AlarmItemViewHolder)
                || !(newHolder instanceof AlarmItemViewHolder)) {
            return null;
        }

        final boolean isCollapsing = this == newHolder;
        setChangingViewsAlpha(isCollapsing ? 0f : 1f);

        final Animator changeAnimatorSet = isCollapsing
                ? createCollapsingAnimator((AlarmItemViewHolder) oldHolder, duration)
                : createExpandingAnimator((AlarmItemViewHolder) newHolder, duration);
        changeAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                clock.setVisibility(View.VISIBLE);
                onOff.setVisibility(View.VISIBLE);
                if(Utils.isBvOS()){
                    arrow.setVisibility(View.INVISIBLE);  //bv zhangjiachu modify to View.INVISIBLE
                }
                arrow.setTranslationY(0f);
                setChangingViewsAlpha(1f);
                arrow.jumpDrawablesToCurrentState();
            }
        });
        return changeAnimatorSet;
    }

    private Animator createExpandingAnimator(AlarmItemViewHolder newHolder, long duration) {
        clock.setVisibility(View.INVISIBLE);
        onOff.setVisibility(View.INVISIBLE);
        arrow.setVisibility(View.INVISIBLE);

        final AnimatorSet alphaAnimatorSet = new AnimatorSet();
        alphaAnimatorSet.playTogether(
                ObjectAnimator.ofFloat(alarmLabel, View.ALPHA, 0f),
                ObjectAnimator.ofFloat(daysOfWeek, View.ALPHA, 0f),
                ObjectAnimator.ofFloat(upcomingInstanceLabel, View.ALPHA, 0f),
                ObjectAnimator.ofFloat(preemptiveDismissButton, View.ALPHA, 0f),
                ObjectAnimator.ofFloat(hairLine, View.ALPHA, 0f));
        alphaAnimatorSet.setDuration((long) (duration * ANIM_SHORT_DURATION_MULTIPLIER));

        final View oldView = itemView;
        final View newView = newHolder.itemView;
        final Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(oldView, oldView, newView)
                .setDuration(duration);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimatorSet, boundsAnimator);
        return animatorSet;
    }

    private Animator createCollapsingAnimator(AlarmItemViewHolder oldHolder, long duration) {
        final AnimatorSet alphaAnimatorSet = new AnimatorSet();
        alphaAnimatorSet.playTogether(
                ObjectAnimator.ofFloat(alarmLabel, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(daysOfWeek, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(upcomingInstanceLabel, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(preemptiveDismissButton, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(hairLine, View.ALPHA, 1f));
        final long standardDelay = (long) (duration * ANIM_STANDARD_DELAY_MULTIPLIER);
        alphaAnimatorSet.setDuration(standardDelay);
        alphaAnimatorSet.setStartDelay(duration - standardDelay);

        final View oldView = oldHolder.itemView;
        final View newView = itemView;
        final Animator boundsAnimator = AnimatorUtils.getBoundsAnimator(newView, oldView, newView)
                .setDuration(duration);
        boundsAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final View oldArrow = oldHolder.arrow;
        final Rect oldArrowRect = new Rect(0, 0, oldArrow.getWidth(), oldArrow.getHeight());
        final Rect newArrowRect = new Rect(0, 0, arrow.getWidth(), arrow.getHeight());
        ((ViewGroup) newView).offsetDescendantRectToMyCoords(arrow, newArrowRect);
        ((ViewGroup) oldView).offsetDescendantRectToMyCoords(oldArrow, oldArrowRect);
        final float arrowTranslationY = oldArrowRect.bottom - newArrowRect.bottom;
        arrow.setTranslationY(arrowTranslationY);
        if(mIsBvOS){
            arrow.setVisibility(View.INVISIBLE); //  bv zhangjiachu  modify View.INVISIBLE  default :View.VISIBLE
        }
        clock.setVisibility(View.VISIBLE);
        onOff.setVisibility(View.VISIBLE);

        final Animator arrowAnimation = ObjectAnimator.ofFloat(arrow, View.TRANSLATION_Y, 0f)
                .setDuration(duration);
        arrowAnimation.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimatorSet, boundsAnimator, arrowAnimation);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                AnimatorUtils.startDrawableAnimation(arrow);
            }
        });
        return animatorSet;
    }

    private void setChangingViewsAlpha(float alpha) {
        alarmLabel.setAlpha(alpha);
        daysOfWeek.setAlpha(alpha);
        upcomingInstanceLabel.setAlpha(alpha);
        hairLine.setAlpha(alpha);
        preemptiveDismissButton.setAlpha(alpha);
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {
        private final LayoutInflater mLayoutInflater;

        public Factory(LayoutInflater layoutInflater) {
            mLayoutInflater = layoutInflater;
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup parent, int viewType) {
            return new CollapsedAlarmViewHolder(mLayoutInflater.inflate(
                    viewType, parent, false /* attachToRoot */));
        }
    }
}
