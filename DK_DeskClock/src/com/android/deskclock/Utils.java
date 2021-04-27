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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.AnyRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.os.BuildCompat;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.uidata.UiDataModel;

import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY;
import static android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.graphics.Bitmap.Config.ARGB_8888;
/*hct-fankou add DeskClock LightTheme 20191113 start*/
import android.os.SystemProperties;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class Utils {

    /**
     * {@link Uri} signifying the "silent" ringtone.
     */
    public static final Uri RINGTONE_SILENT = Uri.EMPTY;

    public static void enforceMainLooper() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalAccessError("May only call from main thread.");
        }
    }

    public static void enforceNotMainLooper() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalAccessError("May not call from main thread.");
        }
    }

    public static int indexOf(Object[] array, Object item) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(item)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return {@code true} if the device is prior to {@link Build.VERSION_CODES#LOLLIPOP}
     */
    public static boolean isPreL() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#LOLLIPOP} or
     * {@link Build.VERSION_CODES#LOLLIPOP_MR1}
     */
    public static boolean isLOrLMR1() {
        final int sdkInt = Build.VERSION.SDK_INT;
        return sdkInt == Build.VERSION_CODES.LOLLIPOP || sdkInt == Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#LOLLIPOP} or later
     */
    public static boolean isLOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#LOLLIPOP_MR1} or later
     */
    public static boolean isLMR1OrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#M} or later
     */
    public static boolean isMOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#N} or later
     */
    public static boolean isNOrLater() {
        return BuildCompat.isAtLeastN();
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#N_MR1} or later
     */
    public static boolean isNMR1OrLater() {
        return BuildCompat.isAtLeastNMR1();
    }

    /**
     * @param resourceId identifies an application resource
     * @return the Uri by which the application resource is accessed
     */
    public static Uri getResourceUri(Context context, @AnyRes int resourceId) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getPackageName())
                .path(String.valueOf(resourceId))
                .build();
    }

    /**
     * @param view the scrollable view to test
     * @return {@code true} iff the {@code view} content is currently scrolled to the top
     */
    public static boolean isScrolledToTop(View view) {
        return !view.canScrollVertically(-1);
    }

    /**
     * Calculate the amount by which the radius of a CircleTimerView should be offset by any
     * of the extra painted objects.
     */
    public static float calculateRadiusOffset(
            float strokeSize, float dotStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(dotStrokeSize, markerStrokeSize));
    }

    /**
     * Configure the clock that is visible to display seconds. The clock that is not visible never
     * displays seconds to avoid it scheduling unnecessary ticking runnables.
     */
    public static void setClockSecondsEnabled(TextClock digitalClock, AnalogClock analogClock) {
        final boolean displaySeconds = DataModel.getDataModel().getDisplayClockSeconds();
        final DataModel.ClockStyle clockStyle = DataModel.getDataModel().getClockStyle();
        switch (clockStyle) {
            case ANALOG:
                setTimeFormat(digitalClock, false);
                analogClock.enableSeconds(displaySeconds);
                return;
            case DIGITAL:
                analogClock.enableSeconds(false);
                setTimeFormat(digitalClock, displaySeconds);
                return;
        }

        throw new IllegalStateException("unexpected clock style: " + clockStyle);
    }

    /**
     * Author bv zhangjiachu
     * remove am/pm
     * Configure the clock that is visible to display seconds. The clock that is not visible never
     * displays seconds to avoid it scheduling unnecessary ticking runnables.
     */
    public static void setBvClockSecondsEnabled(TextClock digitalClock, AnalogClock analogClock) {
        final boolean displaySeconds = DataModel.getDataModel().getDisplayClockSeconds();
        final DataModel.ClockStyle clockStyle = DataModel.getDataModel().getClockStyle();
        switch (clockStyle) {
            case ANALOG:
                setBvTimeFormat(digitalClock, false);
                analogClock.enableSeconds(displaySeconds);
                return;
            case DIGITAL:
                analogClock.enableSeconds(false);
                setBvTimeFormat(digitalClock, displaySeconds);
                return;
        }

        throw new IllegalStateException("unexpected clock style: " + clockStyle);
    }

    /**
     * Set whether the digital or analog clock should be displayed in the application.
     * Returns the view to be displayed.
     */
    public static View setClockStyle(View digitalClock, View analogClock) {
        final DataModel.ClockStyle clockStyle = DataModel.getDataModel().getClockStyle();
        switch (clockStyle) {
            case ANALOG:
                digitalClock.setVisibility(View.GONE);
                analogClock.setVisibility(View.VISIBLE);
                return analogClock;
            case DIGITAL:
                digitalClock.setVisibility(View.VISIBLE);
                analogClock.setVisibility(View.GONE);
                return digitalClock;
        }

        throw new IllegalStateException("unexpected clock style: " + clockStyle);
    }

    /**
     * For screensavers to set whether the digital or analog clock should be displayed.
     * Returns the view to be displayed.
     */
    public static View setScreensaverClockStyle(View digitalClock, View analogClock) {
        final DataModel.ClockStyle clockStyle = DataModel.getDataModel().getScreensaverClockStyle();
        switch (clockStyle) {
            case ANALOG:
                digitalClock.setVisibility(View.GONE);
                analogClock.setVisibility(View.VISIBLE);
                return analogClock;
            case DIGITAL:
                digitalClock.setVisibility(View.VISIBLE);
                analogClock.setVisibility(View.GONE);
                return digitalClock;
        }

        throw new IllegalStateException("unexpected clock style: " + clockStyle);
    }

    /**
     * For screensavers to dim the lights if necessary.
     */
    public static void dimClockView(boolean dim, View clockView) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setColorFilter(new PorterDuffColorFilter(
                (dim ? 0x40FFFFFF : 0xC0FFFFFF),
                PorterDuff.Mode.MULTIPLY));
        clockView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * Update and return the PendingIntent corresponding to the given {@code intent}.
     *
     * @param context the Context in which the PendingIntent should start the service
     * @param intent  an Intent describing the service to be started
     * @return a PendingIntent that will start a service
     */
    public static PendingIntent pendingServiceIntent(Context context, Intent intent) {
        return PendingIntent.getService(context, 0, intent, FLAG_UPDATE_CURRENT);
    }

    /**
     * Update and return the PendingIntent corresponding to the given {@code intent}.
     *
     * @param context the Context in which the PendingIntent should start the activity
     * @param intent  an Intent describing the activity to be started
     * @return a PendingIntent that will start an activity
     */
    public static PendingIntent pendingActivityIntent(Context context, Intent intent) {
        return PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT);
    }

    /**
     * @return The next alarm from {@link AlarmManager}
     */
    public static String getNextAlarm(Context context) {
        return isPreL() ? getNextAlarmPreL(context) : getNextAlarmLOrLater(context);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getNextAlarmPreL(Context context) {
        final ContentResolver cr = context.getContentResolver();
        return Settings.System.getString(cr, Settings.System.NEXT_ALARM_FORMATTED);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getNextAlarmLOrLater(Context context) {
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final AlarmClockInfo info = getNextAlarmClock(am);
        if (info != null) {
            final long triggerTime = info.getTriggerTime();
            final Calendar alarmTime = Calendar.getInstance();
            alarmTime.setTimeInMillis(triggerTime);
            return AlarmUtils.getFormattedTime(context, alarmTime);
        }

        return null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static AlarmClockInfo getNextAlarmClock(AlarmManager am) {
        return am.getNextAlarmClock();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void updateNextAlarm(AlarmManager am, AlarmClockInfo info, PendingIntent op) {
        am.setAlarmClock(info, op);
    }

    public static boolean isAlarmWithin24Hours(AlarmInstance alarmInstance) {
        final Calendar nextAlarmTime = alarmInstance.getAlarmTime();
        final long nextAlarmTimeMillis = nextAlarmTime.getTimeInMillis();
        return nextAlarmTimeMillis - System.currentTimeMillis() <= DateUtils.DAY_IN_MILLIS;
    }

    /**
     * Clock views can call this to refresh their alarm to the next upcoming value.
     */
    public static void refreshAlarm(Context context, View clock) {
        final TextView nextAlarmIconView = (TextView) clock.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarmView = (TextView) clock.findViewById(R.id.nextAlarm);
        if (nextAlarmView == null) {
            return;
        }

        final String alarm = getNextAlarm(context);
        if (!TextUtils.isEmpty(alarm)) {
            final String description = context.getString(R.string.next_alarm_description, alarm);
            nextAlarmView.setText(alarm);
            nextAlarmView.setContentDescription(description);
            nextAlarmView.setVisibility(View.VISIBLE);
            nextAlarmIconView.setVisibility(View.VISIBLE);
            nextAlarmIconView.setContentDescription(description);
        } else {
            nextAlarmView.setVisibility(View.GONE);
            nextAlarmIconView.setVisibility(View.GONE);
        }
        /*hct-fankou add DeskClock LightTheme 20191113 start*/
        if(ThemeUtils.isDarkTheme(context)
                //bv zhangjiachu add for exclude ScreensaverActivity.class Screensaver.class
                && !(context.getClass().equals(ScreensaverActivity.class))
                && !(context.getClass().equals(Screensaver.class))
                //bv zhangjiachu add for exclude ScreensaverActivity.class Screensaver.class
                ){
            nextAlarmIconView.setTextColor(Color.parseColor(context.getString(R.string.hct_text_color)));
            nextAlarmView.setTextColor(Color.parseColor(context.getString(R.string.hct_text_color)));
        }
        /*hct-fankou add DeskClock LightTheme 20191113 end*/
    }

    /**
	 * bv zhangjiachu add for alarm style 
     * Clock views can call this to refresh their alarm to the next upcoming value.
     */
    public static void bvRefreshAlarm(Context context, View clock) {
        final TextView localTime = (TextView) clock.findViewById(R.id.local_time);
        final TextView nextAlarmIconView = (TextView) clock.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarmView = (TextView) clock.findViewById(R.id.nextAlarm);
        if (nextAlarmView == null) {
            return;
        }

        final String alarm = getNextAlarm(context);
        if (!TextUtils.isEmpty(alarm)) {
            final String description = context.getString(R.string.next_alarm_description, alarm);
            nextAlarmView.setText(alarm);
            nextAlarmView.setContentDescription(description);
            nextAlarmView.setVisibility(View.GONE);
            nextAlarmIconView.setVisibility(View.GONE);
            nextAlarmIconView.setContentDescription(description);
            localTime.setVisibility(View.VISIBLE);
            localTime.setContentDescription(description);
        } else {
            nextAlarmView.setVisibility(View.GONE);
            nextAlarmIconView.setVisibility(View.GONE);
        }
    }

    public static void setClockIconTypeface(View clock) {
        final TextView nextAlarmIconView = (TextView) clock.findViewById(R.id.nextAlarmIcon);
        nextAlarmIconView.setTypeface(UiDataModel.getUiDataModel().getAlarmIconTypeface());
    }

    /**
     * Clock views can call this to refresh their date.
     **/
    public static void updateDate(String dateSkeleton, String descriptionSkeleton, View clock) {
        final TextView dateDisplay = (TextView) clock.findViewById(R.id.date);
        if (dateDisplay == null) {
            return;
        }

        final Locale l = Locale.getDefault();
        final String datePattern = DateFormat.getBestDateTimePattern(l, dateSkeleton);
        final String descriptionPattern = DateFormat.getBestDateTimePattern(l, descriptionSkeleton);

        final Date now = new Date();
        dateDisplay.setText(new SimpleDateFormat(datePattern, l).format(now));
        dateDisplay.setVisibility(View.VISIBLE);
        dateDisplay.setContentDescription(new SimpleDateFormat(descriptionPattern, l).format(now));
    }

    /***
     * Formats the time in the TextClock according to the Locale with a special
     * formatting treatment for the am/pm label.
     *
     * @param clock          TextClock to format
     * @param includeSeconds whether or not to include seconds in the clock's time
     */
    public static void setTimeFormat(TextClock clock, boolean includeSeconds) {
        if (clock != null) {
            // Get the best format for 12 hours mode according to the locale
            clock.setFormat12Hour(get12ModeFormat(0.5f /* amPmRatio */, includeSeconds));
            // Get the best format for 24 hours mode according to the locale
            clock.setFormat24Hour(get24ModeFormat(includeSeconds));
        }
    }

    /**
     * @param amPmRatio      a value between 0 and 1 that is the ratio of the relative size of the
     *                       am/pm string to the time string
     * @param includeSeconds whether or not to include seconds in the time string
     * @return format string for 12 hours mode time, not including seconds98
     */
    public static CharSequence get12ModeFormat(float amPmRatio, boolean includeSeconds) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(),
                includeSeconds ? "hmsa" : "hma");
        if (amPmRatio <= 0) {
            pattern = pattern.replaceAll("a", "").trim();
        }

        //bv zhangjiachu add " hh" for bv alarm style 20200803 start
        if (isBvOS()){
            pattern = pattern.replaceAll("h", " hh");
        }
        //bv zhangjiachu add " hh" for bv alarm style 20200803 start

        // Replace spaces with "Hair Space"
        pattern = pattern.replaceAll(" ", "\u200A");

        // Build a spannable so that the am/pm will be formatted
        int amPmPos = pattern.indexOf('a');
        if (amPmPos == -1) {
            return pattern;
        }

        final Spannable sp = new SpannableString(pattern);
        sp.setSpan(new RelativeSizeSpan(amPmRatio), amPmPos, amPmPos + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new StyleSpan(Typeface.NORMAL), amPmPos, amPmPos + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new TypefaceSpan("sans-serif"), amPmPos, amPmPos + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return sp;
    }

    /***
     * Author bv zhangjiachu
     * Formats the time in the TextClock according to the Locale with a special
     * formatting remove the am/pm label.
     *
     * @param clock          TextClock to format
     * @param includeSeconds whether or not to include seconds in the clock's time
     */
    public static void setBvTimeFormat(TextClock clock, boolean includeSeconds) {
        if (clock != null) {
            // Get the best format for 12 hours mode according to the locale
            //clock.setFormat12Hour(getBvFormat12Hour(0.0f /* amPmRatio */, includeSeconds));
            clock.setFormat12Hour(null);
            // Get the best format for 24 hours mode according to the locale
            clock.setFormat24Hour(get24ModeFormat(includeSeconds));
        }
    }


    public static CharSequence get24ModeFormat(boolean includeSeconds) {
        return DateFormat.getBestDateTimePattern(Locale.getDefault(),
                includeSeconds ? "Hms" : "Hm");
    }

    /**
     * Returns string denoting the timezone hour offset (e.g. GMT -8:00)
     *
     * @param useShortForm Whether to return a short form of the header that rounds to the
     *                     nearest hour and excludes the "GMT" prefix
     */
    public static String getGMTHourOffset(TimeZone timezone, boolean useShortForm) {
        final int gmtOffset = timezone.getRawOffset();
        final long hour = gmtOffset / DateUtils.HOUR_IN_MILLIS;
        final long min = (Math.abs(gmtOffset) % DateUtils.HOUR_IN_MILLIS) /
                DateUtils.MINUTE_IN_MILLIS;

        if (useShortForm) {
            return String.format(Locale.ENGLISH, "%+d", hour);
        } else {
            return String.format(Locale.ENGLISH, "GMT %+d:%02d", hour, min);
        }
    }

    /**
     * Given a point in time, return the subsequent moment any of the time zones changes days.
     * e.g. Given 8:00pm on 1/1/2016 and time zones in LA and NY this method would return a Date for
     * midnight on 1/2/2016 in the NY timezone since it changes days first.
     *
     * @param time  a point in time from which to compute midnight on the subsequent day
     * @param zones a collection of time zones
     * @return the nearest point in the future at which any of the time zones changes days
     */
    public static Date getNextDay(Date time, Collection<TimeZone> zones) {
        Calendar next = null;
        for (TimeZone tz : zones) {
            final Calendar c = Calendar.getInstance(tz);
            c.setTime(time);

            // Advance to the next day.
            c.add(Calendar.DAY_OF_YEAR, 1);

            // Reset the time to midnight.
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            if (next == null || c.compareTo(next) < 0) {
                next = c;
            }
        }

        return next == null ? null : next.getTime();
    }

    public static String getNumberFormattedQuantityString(Context context, int id, int quantity) {
        final String localizedQuantity = NumberFormat.getInstance().format(quantity);
        return context.getResources().getQuantityString(id, quantity, localizedQuantity);
    }

    /**
     * @return {@code true} iff the widget is being hosted in a container where tapping is allowed
     */
    public static boolean isWidgetClickable(AppWidgetManager widgetManager, int widgetId) {
        final Bundle wo = widgetManager.getAppWidgetOptions(widgetId);
        return wo != null
                && wo.getInt(OPTION_APPWIDGET_HOST_CATEGORY, -1) != WIDGET_CATEGORY_KEYGUARD;
    }

    /**
     * @return a vector-drawable inflated from the given {@code resId}
     */
    public static VectorDrawableCompat getVectorDrawable(Context context, @DrawableRes int resId) {
        return VectorDrawableCompat.create(context.getResources(), resId, context.getTheme());
    }

    /**
     * This method assumes the given {@code view} has already been layed out.
     *
     * @return a Bitmap containing an image of the {@code view} at its current size
     */
    public static Bitmap createBitmap(View view) {
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    /**
     * {@link ArraySet} is @hide prior to {@link Build.VERSION_CODES#M}.
     */
    @SuppressLint("NewApi")
    public static <E> ArraySet<E> newArraySet(Collection<E> collection) {
        final ArraySet<E> arraySet = new ArraySet<>(collection.size());
        arraySet.addAll(collection);
        return arraySet;
    }

    /**
     * @param context from which to query the current device configuration
     * @return {@code true} if the device is currently in portrait or reverse portrait orientation
     */
    public static boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT;
    }

    /**
     * @param context from which to query the current device configuration
     * @return {@code true} if the device is currently in landscape or reverse landscape orientation
     */
    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE;
    }

    public static long now() {
        return DataModel.getDataModel().elapsedRealtime();
    }

    public static long wallClock() {
        return DataModel.getDataModel().currentTimeMillis();
    }

    /**
     * @param context to obtain strings.
     * @param displayMinutes whether or not minutes should be included
     * @param isAhead {@code true} if the time should be marked 'ahead', else 'behind'
     * @param hoursDifferent the number of hours the time is ahead/behind
     * @param minutesDifferent the number of minutes the time is ahead/behind
     * @return String describing the hours/minutes ahead or behind
     */
    public static String createHoursDifferentString(Context context, boolean displayMinutes,
            boolean isAhead, int hoursDifferent, int minutesDifferent) {
        String timeString;
        if (displayMinutes && hoursDifferent != 0) {
            // Both minutes and hours
            final String hoursShortQuantityString =
                    Utils.getNumberFormattedQuantityString(context,
                            R.plurals.hours_short, Math.abs(hoursDifferent));
            final String minsShortQuantityString =
                    Utils.getNumberFormattedQuantityString(context,
                            R.plurals.minutes_short, Math.abs(minutesDifferent));
            final @StringRes int stringType = isAhead
                    ? R.string.world_hours_minutes_ahead
                    : R.string.world_hours_minutes_behind;
            timeString = context.getString(stringType, hoursShortQuantityString,
                    minsShortQuantityString);
        } else {
            // Minutes alone or hours alone
            final String hoursQuantityString = Utils.getNumberFormattedQuantityString(
                    context, R.plurals.hours, Math.abs(hoursDifferent));
            final String minutesQuantityString = Utils.getNumberFormattedQuantityString(
                    context, R.plurals.minutes, Math.abs(minutesDifferent));
            final @StringRes int stringType = isAhead ? R.string.world_time_ahead
                    : R.string.world_time_behind;
            timeString = context.getString(stringType, displayMinutes
                    ? minutesQuantityString : hoursQuantityString);
        }
        return timeString;
    }

    /**
     * @param context The context from which to obtain strings
     * @param hours Hours to display (if any)
     * @param minutes Minutes to display (if any)
     * @param seconds Seconds to display
     * @return Provided time formatted as a String
     */
    static String getTimeString(Context context, int hours, int minutes, int seconds) {
        //bv zhangjiachu add TimeString : 00:00:00 20200805 start
        if (isBvOS()){
            return context.getString(R.string.hours_minutes_seconds, hours, minutes, seconds);
        }
        //bv zhangjiachu add TimeString : 00:00:00 20200805 end
        if (hours != 0) {
            return context.getString(R.string.hours_minutes_seconds, hours, minutes, seconds);
        }
        if (minutes != 0) {
            return context.getString(R.string.minutes_seconds, minutes, seconds);
        }
        return context.getString(R.string.seconds, seconds);
    }

    /**
     * auther bv zhangjiachu add for alarm style 20200102
     * @param context The context from which to obtain strings
     * @param hours Hours to display (if any)
     * @param minutes Minutes to display (if any)
     * @param seconds Seconds to display
     * @return Provided time formatted as a String
     */
    static String bvGetTimeString(Context context, int hours, int minutes, int seconds) {
        if (hours != 0) {
            return context.getString(R.string.stopwatch_hours_minutes_seconds, hours, minutes, seconds);
        }
        return context.getString(R.string.stopwatch_minutes_seconds, minutes, seconds);
    }

    public static final class ClickAccessibilityDelegate extends AccessibilityDelegateCompat {

        /** The label for talkback to apply to the view */
        private final String mLabel;

        /** Whether or not to always make the view visible to talkback */
        private final boolean mIsAlwaysAccessibilityVisible;

        public ClickAccessibilityDelegate(String label) {
            this(label, false);
        }

        public ClickAccessibilityDelegate(String label, boolean isAlwaysAccessibilityVisible) {
            mLabel = label;
            mIsAlwaysAccessibilityVisible = isAlwaysAccessibilityVisible;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            if (mIsAlwaysAccessibilityVisible) {
                info.setVisibleToUser(true);
            }
            info.addAction(new AccessibilityActionCompat(
                    AccessibilityActionCompat.ACTION_CLICK.getId(), mLabel));
        }
    }

    /**
     * M [ALPS03538536]: to check if the ringtone media file is removed from SD-card or not.
     *
     * @param ringtone
     * @return boolean
     */
    public static boolean isRingtoneExisted(Context ctx, Uri ringtoneUri) {
        String ringtone = ringtoneUri.toString();
        boolean result = false;
        if (ringtone != null) {
            //M: Skip internal, system and ringtone in deskclock folder
            if (ringtone.contains("internal")) {
                return true;
            }
            if (ringtone.contains("system")) {
                return true;
            }
            if (ringtone.contains("deskclock")) {
                return true;
            }
            //*/ hct.fangkou, 2019-11-22, modify for download ringtone play fail start.
            if (ringtone.contains("com.android.providers.downloads.documents")) {
                return true;
            }
            // hct.fangkou, 2019-22-22, modify for download ringtone play fail end. */

            String path = getRealPathFromURI(ctx, ringtoneUri);
            if (!TextUtils.isEmpty(path)) {
                result = new File(path).exists();
            }
            //*/ hct.fangkou, 2019-11-22, modify for storage/emulated/0/ ringtone play fail start.
            String uripath=ringtoneUri.getPath();
            if(result==false){
                if (!TextUtils.isEmpty(uripath)) {
                    result = true;
                }
            }
            //*/ hct.fangkou, 2019-11-22, modify for storage/emulated/0/ ringtone play fail end.
            LogUtils.i("isRingtoneExisted: " + result + " ,ringtone: " + ringtone
                    + " ,Path: " + path + " , uripath = "+uripath);
        }
        return result;
    }

    /**
     * M [ALPS03538536]: @{ get RingtonePath
     * @param ringtone
     * @return string path
     */
    public static String getRealPathFromURI(final Context context,
            final Uri ringtoneUri) {
        Cursor cursor = null;
        String filepath = null;
        LogUtils.i("getRealPathFromURI alarmRingtone: "
                + ringtoneUri.toString());
        if (null != ringtoneUri) {
            final String scheme = ringtoneUri.getScheme();
            if (null == scheme || scheme.equals("")
                    || scheme.equals(ContentResolver.SCHEME_FILE)) {
                filepath = ringtoneUri.getPath();

            } else if (scheme.equals("http")) {
                filepath = ringtoneUri.toString();

            } else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                try {
                    String[] proj = { MediaStore.MediaColumns.DATA };
                    cursor = context.getContentResolver().query(ringtoneUri,
                            proj, null, null, null);
                    if (null != cursor && 0 != cursor.getCount()
                            && cursor.moveToFirst()) {
                        int column_index = cursor
                                .getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        filepath = cursor.getString(column_index);
                    } else {
                        LogUtils.w("Given Uri could not be found in media store");
                    }
                } catch (SQLiteException e) {
                    LogUtils.e("database operation error: " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    LogUtils.e("IllegalArgument error: " + e.getMessage());
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } else {
                LogUtils.w("Given Uri scheme is not supported");
            }
        }
        return filepath;
    }
    /** @} */

    //bv zhangjimeng 2020/07/04, modify deskclock perf,begin
    private static int mBVOs = -1;
    public static boolean isBvOS(){
        if (mBVOs != -1) {
            return mBVOs == 1;
        }
        mBVOs = SystemProperties.getInt("ro.blackview.os", 0);
        return mBVOs == 1;
    }
    public static boolean isLowLevelPhone() {
/* String model = SystemProperties.get("ro.product.vendor.model", "");
        switch (model) {
            case "A80Pro":
                return true;

            default:
                return true;
        }
*/
      return true;
    }
    //bv zhangjimeng 2020/07/04, modify deskclock perf,end


    public static void initToolBar(final AppCompatActivity activity){
        Toolbar bvToolbar = activity.findViewById(R.id.bv_toolbar);
        if (bvToolbar != null){
            bvToolbar.setTitle("");
            TextView textView = bvToolbar.findViewById(R.id.bv_toolbar_title);
            if (textView != null){
                textView.setText(activity.getTitle());
            }

            bvToolbar.setNavigationIcon(R.drawable.ic_bv_back);
            bvToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.onBackPressed();
                }
            });

            activity.setSupportActionBar(bvToolbar);
        }
    }

    //bv zhangjiachu add for add font type 20200823
    public static final Typeface OPPO_SANS_R = Typeface.create("oppo-sans-r", Typeface.NORMAL);
    public static final Typeface OPPO_SANS_M = Typeface.create("oppo-sans-m", Typeface.NORMAL);
    public static final Typeface OPPO_SANS_L = Typeface.create("oppo-sans-l", Typeface.NORMAL);
}