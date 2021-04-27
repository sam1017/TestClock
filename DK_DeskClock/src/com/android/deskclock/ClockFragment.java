/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.data.City;
import com.android.deskclock.data.CityListener;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.worldclock.CitySelectionActivity;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
/*bv zhangjiachu add for alarm style start*/
import android.view.KeyEvent;
import android.util.ArraySet;
import android.util.Log;
import android.support.design.widget.TabLayout;
import android.os.Message;
import java.util.Set;
import android.widget.LinearLayout;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
/*bv zhangjiachu add for alarm style end*/
import static android.app.AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.android.deskclock.uidata.UiDataModel.Tab.CLOCKS;
import static java.util.Calendar.DAY_OF_WEEK;
/*hct-fankou add DeskClock LightTheme 20191113 start*/
import android.os.SystemProperties;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.v4.graphics.ColorUtils;
import static android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM;
import android.graphics.Canvas;
/**
 * Fragment that shows the clock (analog or digital), the next alarm info and the world clock.
 */
public final class ClockFragment extends DeskClockFragment {
    protected static String TAG = "ClockFragment";
    protected static boolean mIsBvOS = Utils.isBvOS();
    // Updates dates in the UI on every quarter-hour.
    private final Runnable mQuarterHourUpdater = new QuarterHourRunnable();

    // Updates the UI in response to changes to the scheduled alarm.
    private BroadcastReceiver mAlarmChangeReceiver;

    // Detects changes to the next scheduled alarm pre-L.
    private ContentObserver mAlarmObserver;

    private TextClock mDigitalClock;
    private AnalogClock mAnalogClock;
    private View mClockFrame;
    private SelectedCitiesAdapter mCityAdapter;
    private RecyclerView mCityList;
    private String mDateFormat;
    private String mDateFormatForAccessibility;

    /*bv zhangjiachu add for alarm style start*/
    public static boolean mCityDeleteIng = false;

    public static boolean mCityDeleteSelectAll = false;

    public static int mCityDeleteSelectNum = 0;

    public static final Set<City> mUnSelectedDeleteCities = new ArraySet<>();

    /** Draws a gradient over the bottom of the {@link #mCityList} to reduce clash with the fab. */
    private GradientItemDecoration mGradientItemDecoration;
    /*bv zhangjiachu add for alarm style end*/

    /**
     * The public no-arg constructor required by all fragments.
     */
    public ClockFragment() {
        super(CLOCKS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlarmObserver = Utils.isPreL() ? new AlarmObserverPreL() : null;
        mAlarmChangeReceiver = Utils.isLOrLater() ? new AlarmChangedBroadcastReceiver() : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle icicle) {
        super.onCreateView(inflater, container, icicle);

        final View fragmentView = inflater.inflate(R.layout.clock_fragment, container, false);

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);

        if (mIsBvOS) {
            mCityAdapter = new BVSelectedCitiesAdapter(getActivity(), mDateFormat,
                    /*bv zhangjiachu modify for alarm style: add handler start*/
                    //mDateFormatForAccessibility);
                    mDateFormatForAccessibility, handler);
            /*bv zhangjiachu modify for alarm style: add handler end*/
        }else {
            mCityAdapter = new SelectedCitiesAdapter(getActivity(), mDateFormat,
                    mDateFormatForAccessibility, null);
        }

        mCityList = (RecyclerView) fragmentView.findViewById(R.id.cities);
        mCityList.setLayoutManager(new LinearLayoutManager(getActivity()));
        //bv zhangjiachu add GradientItemDecoration 20200805 start
        mGradientItemDecoration = new GradientItemDecoration(getActivity());
        mCityList.addItemDecoration(mGradientItemDecoration);
        //bv zhangjiachu add GradientItemDecoration 20200805 end
        mCityList.setAdapter(mCityAdapter);
        mCityList.setItemAnimator(null);
        DataModel.getDataModel().addCityListener(mCityAdapter);

        final ScrollPositionWatcher scrollPositionWatcher = new ScrollPositionWatcher();
        mCityList.addOnScrollListener(scrollPositionWatcher);

        final Context context = container.getContext();
        mCityList.setOnTouchListener(new CityListOnLongClickListener(context));
        fragmentView.setOnLongClickListener(new StartScreenSaverListener());

        // On tablet landscape, the clock frame will be a distinct view. Otherwise, it'll be added
        // on as a header to the main listview.
        mClockFrame = fragmentView.findViewById(R.id.main_clock_left_pane);
        if (mClockFrame != null) {
            mDigitalClock = (TextClock) mClockFrame.findViewById(R.id.digital_clock);
            mAnalogClock = (AnalogClock) mClockFrame.findViewById(R.id.analog_clock);
            Utils.setClockIconTypeface(mClockFrame);
            Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);

            Utils.setClockStyle(mDigitalClock, mAnalogClock);
            //bv zhangjiachu add  mIsBvOS 20200803
            if (mIsBvOS){
                Utils.setBvClockSecondsEnabled(mDigitalClock, mAnalogClock);
            } else {
                Utils.setClockSecondsEnabled(mDigitalClock, mAnalogClock);
            }
        }

        // Schedule a runnable to update the date every quarter hour.
        UiDataModel.getUiDataModel().addQuarterHourCallback(mQuarterHourUpdater, 100);

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();

        final Activity activity = getActivity();

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);

        // Watch for system events that effect clock time or format.
        if (mAlarmChangeReceiver != null) {
            final IntentFilter filter = new IntentFilter(ACTION_NEXT_ALARM_CLOCK_CHANGED);
            activity.registerReceiver(mAlarmChangeReceiver, filter);
        }

        // Resume can be invoked after changing the clock style or seconds display.
        if (mDigitalClock != null && mAnalogClock != null) {
            Utils.setClockStyle(mDigitalClock, mAnalogClock);
            //bv zhangjiachu add  mIsBvOS 20200803
            if (mIsBvOS){
                Utils.setBvClockSecondsEnabled(mDigitalClock, mAnalogClock);
            } else {
                Utils.setClockSecondsEnabled(mDigitalClock, mAnalogClock);
            }
        }

        final View view = getView();
        if (view != null && view.findViewById(R.id.main_clock_left_pane) != null) {
            // Center the main clock frame by hiding the world clocks when none are selected.
            mCityList.setVisibility(mCityAdapter.getItemCount() == 0 ? GONE : VISIBLE);
        }

        refreshAlarm();

        // Alarm observer is null on L or later.
        if (mAlarmObserver != null) {
            @SuppressWarnings("deprecation")
            final Uri uri = Settings.System.getUriFor(Settings.System.NEXT_ALARM_FORMATTED);
            activity.getContentResolver().registerContentObserver(uri, false, mAlarmObserver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        final Activity activity = getActivity();
        if (mAlarmChangeReceiver != null) {
            activity.unregisterReceiver(mAlarmChangeReceiver);
        }
        if (mAlarmObserver != null) {
            activity.getContentResolver().unregisterContentObserver(mAlarmObserver);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        UiDataModel.getUiDataModel().removePeriodicCallback(mQuarterHourUpdater);
        DataModel.getDataModel().removeCityListener(mCityAdapter);
    }

    @Override
    public void onFabClick(@NonNull ImageView fab) {
        startActivity(new Intent(getActivity(), CitySelectionActivity.class));
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        /*bv zhangjiachu modify for alarm style start*/
        if(Utils.isBvOS()){
            if (mCityDeleteIng){
                fab.setVisibility(GONE);
            } else {
                fab.setVisibility(VISIBLE);
            }
            fab.setImageResource(R.drawable.ic_bv_add);
        }else {
            fab.setVisibility(VISIBLE);
            fab.setImageResource(R.drawable.ic_public);
        }
        fab.setClickable(true);
        fab.setAlpha(1.0f);
        /*bv zhangjiachu modify for alarm style end*/
        fab.setContentDescription(fab.getResources().getString(R.string.button_cities));
    }

    /*bv zhangjiachu add for alarm style 20200104 start*/
    @Override
    public void onUpdateTabs(@NonNull TabLayout tabs, @NonNull LinearLayout delete_alarm_tab, @NonNull TextView deskTitle) {
        /*bv zhangjiachu modify for alarm style 20200110 start*/
        deskTitle.setText(R.string.menu_clock);
        tabs.setVisibility(VISIBLE);
        if (mCityDeleteIng) {
            //bv zhangjiachu modify for bv os 20210301
            //tabs.setVisibility(GONE);
            delete_alarm_tab.setVisibility(VISIBLE);
            deskTitle.setVisibility(GONE);
        } else {
            //bv zhangjiachu modify for bv os 20210301
            //tabs.setVisibility(VISIBLE);
            delete_alarm_tab.setVisibility(GONE);
            deskTitle.setVisibility(VISIBLE);
        }
        /*bv zhangjiachu modify for alarm style 20200110 start*/
    }

    @Override
    public void onUpdateDelLayout(@NonNull LinearLayout delete_buttom_layout) {
        if (mCityDeleteIng){
            delete_buttom_layout.setVisibility(VISIBLE);
        } else {
            delete_buttom_layout.setVisibility(GONE);
        }
    }

    @Override
    public void onUpdateDelClick(@NonNull TextView delete_buttom_layout) {
        mCityDeleteIng = false;
        if (getActivity() instanceof DeskClock){
            ((DeskClock)getActivity()).bvPrepareMenu();
        }
        mCityDeleteSelectNum = 0;
        if (!mCityDeleteIng) {
            LogUtils.d("onUpdateDelClick: mUnSelectedDeleteCities" + mUnSelectedDeleteCities);
            DataModel.getDataModel().setSelectedCities(mUnSelectedDeleteCities);
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);
            mCityAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onUpdateCancel(@NonNull TextView cancel_delete) {

    }

    @Override
    public void onUpdateCancelClick(@NonNull TextView cancel_delete) {
        mCityDeleteIng = false;
        if (getActivity() instanceof DeskClock){
            ((DeskClock)getActivity()).bvPrepareMenu();
        }
        mCityDeleteSelectNum = 0;
        mCityDeleteSelectAll = false;
        if (!mCityDeleteIng) {
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);
            mCityAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onUpdateSeleteAll(@NonNull TextView select_all) {
        if (!mCityDeleteSelectAll){
            select_all.setText(android.R.string.selectAll);
        } else {
            /// bv zhangjiachu modify [Fragment not attached to activity] Check whether fragment
            /// is attached to activity or not by isAdded() method before accessing app's resources @{
            if (isAdded()) {
                final Resources resources = getResources();
                select_all.setText(resources.getString(R.string.unselectAll));
            }
        }
    }

    @Override
    public void onUpdateSeleteAllClick(@NonNull TextView select_all) {
        final List<City> selected = DataModel.getDataModel().getSelectedCities();
        if (mCityDeleteSelectAll){
            mUnSelectedDeleteCities.clear();
            mUnSelectedDeleteCities.addAll(selected);
            mCityDeleteSelectNum = 0;
            mCityDeleteSelectAll = false;
        }else{
            mCityDeleteSelectNum = selected.size();
            mUnSelectedDeleteCities.clear();
            mCityDeleteSelectAll = true;
        }
        updateFab(FAB_AND_BUTTONS_IMMEDIATE);
        mCityAdapter.notifyDataSetChanged();
        LogUtils.d("onUpdateSeleteAllClick: mCityDeleteSelectNum = " + mCityDeleteSelectNum);
        LogUtils.d("onUpdateSeleteAllClick: mCityDeleteSelectAll = " + mCityDeleteSelectAll +"    "+ "mUnSelectedDeleteCities = " + mUnSelectedDeleteCities.size());
    }

    /*bv zhangjiachu modify for alarm style: add handler start*/
    private Handler handler = new Handler() {
        //Context context = getApplicationContext();
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AlarmUtils.MSG_WORLD_CLOCK_ALTER: {
                    mCityDeleteSelectNum = (int) msg.obj;
                    updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                    break;
                }
            }
        }
    };
    /*bv zhangjiachu modify for alarm style: add handler end*/

    @Override
    public void onUpdateSeleteNum(@NonNull TextView select_num) {
        /// bv zhangjiachu modify [Fragment not attached to activity] Check whether fragment
        /// is attached to activity or not by isAdded() method before accessing app's resources @{
        if (isAdded()){
            final Resources resources = getResources();
            select_num.setText(resources.getString(R.string.delete_alarm_num, mCityDeleteSelectNum));
        }
    }


    /*bv zhangjiachu add for alarm style 20200110 start*/
    public void exitWorldClockDeleteEdit() {
        //bv zhangjimeng fixed for performance
        mCityDeleteSelectAll = false;
        mCityDeleteSelectNum = 0;
        if (mCityDeleteIng) {
            mCityDeleteIng = false;
            if (getActivity() instanceof DeskClock){
                ((DeskClock)getActivity()).bvPrepareMenu();
            }
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);
            mCityAdapter.notifyDataSetChanged();
        }
    }
    /*bv zhangjiachu add for alarm style 20200110 end*/


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // By default return false so event continues to propagate
        if (keyCode == KeyEvent.KEYCODE_BACK){
            if (mCityDeleteIng){
                mCityDeleteIng = false;
                mCityDeleteSelectAll = false;
                mCityDeleteSelectNum = 0;
                if (getActivity() instanceof DeskClock){
                    ((DeskClock)getActivity()).bvPrepareMenu();
                }
                //DeskClock.mSeleteNum.setText(context.getString(R.string.delete_alarm_num, mSelectNum));
                if (!mCityDeleteIng) {
                    updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                    mCityAdapter.notifyDataSetChanged();
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    /*bv zhangjiachu add for alarm style 20200104 end*/
    @Override
    /*bv zhangjiachu modify for alarm style 20200103 start*/
    //public void onUpdateFabButtons(@NonNull Button left, @NonNull Button right) {
    public void onUpdateFabButtons(@NonNull ImageButton left, @NonNull ImageButton right) {
        /*bv zhangjiachu modify for alarm style 20200103 end*/
        left.setVisibility(INVISIBLE);
        right.setVisibility(INVISIBLE);
    }

    public void onUpdateFabButtons(@NonNull Button left, @NonNull Button right) {
        left.setVisibility(INVISIBLE);
        right.setVisibility(INVISIBLE);
    }

    /**
     * Refresh the next alarm time.
     */
    private void refreshAlarm() {
        if (mClockFrame != null) {
            if (mIsBvOS) {
                /*bv zhangjiachu modify for alarm style 20200110 start*/
                Utils.bvRefreshAlarm(getActivity(), mClockFrame);
                /*bv zhangjiachu modify for alarm style 20200110 end*/
            }else {
                Utils.refreshAlarm(getActivity(), mClockFrame);
            }
        } else {
            mCityAdapter.refreshAlarm();
        }
    }

    /**
     * @param color the newly installed app window color
     */
    protected void onAppColorChanged(@ColorInt int color) {
        if (mGradientItemDecoration != null) {
            mGradientItemDecoration.updateGradientColors(color);
        }
    }

    /**
     * Draws a tinting gradient over the bottom of the RecyclerView. This reduces the
     * contrast between floating buttons and the laps list content.
     */
    private static final class GradientItemDecoration extends RecyclerView.ItemDecoration {

        //  0% -  25% of gradient length -> opacity changes from 0% to 50%
        // 25% -  90% of gradient length -> opacity changes from 50% to 100%
        // 90% - 100% of gradient length -> opacity remains at 100%
        private static final int[] ALPHAS = {
                0x00, // 0%
                0x1A, // 10%
                0x33, // 20%
                0x4D, // 30%
                0x66, // 40%
                0x80, // 50%
                0x89, // 53.8%
                0x93, // 57.6%
                0x9D, // 61.5%
                0xA7, // 65.3%
                0xB1, // 69.2%
                0xBA, // 73.0%
                0xC4, // 76.9%
                0xCE, // 80.7%
                0xD8, // 84.6%
                0xE2, // 88.4%
                0xEB, // 92.3%
                0xF5, // 96.1%
                0xFF, // 100%
                0xFF, // 100%
                0xFF, // 100%
        };

        /**
         * A reusable array of control point colors that define the gradient. It is based on the
         * background color of the window and thus recomputed each time that color is changed.
         */
        private final int[] mGradientColors = new int[ALPHAS.length];

        /** The drawable that produces the tinting gradient effect of this decoration. */
        private final GradientDrawable mGradient = new GradientDrawable();

        /** The height of the gradient; sized relative to the fab height. */
        private final int mGradientHeight;

        GradientItemDecoration(Context context) {
            mGradient.setOrientation(TOP_BOTTOM);
            updateGradientColors(ThemeUtils.resolveColor(context, android.R.attr.windowBackground));

            final Resources resources = context.getResources();
            final float fabHeight = resources.getDimensionPixelSize(R.dimen.fab_height);
            mGradientHeight = Math.round(fabHeight * 1.2f);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.onDrawOver(c, parent, state);

            final int w = parent.getWidth();
            final int h = parent.getHeight();

            mGradient.setBounds(0, h - mGradientHeight, w, h);
            mGradient.draw(c);
        }

        /**
         * Given a {@code baseColor}, compute a gradient of tinted colors that define the fade
         * effect to apply to the bottom of the lap list.
         *
         * @param baseColor a base color to which the gradient tint should be applied
         */
        void updateGradientColors(@ColorInt int baseColor) {
            // Compute the tinted colors that form the gradient.
            for (int i = 0; i < mGradientColors.length; i++) {
                mGradientColors[i] = ColorUtils.setAlphaComponent(baseColor, ALPHAS[i]);
            }

            // Set the gradient colors into the drawable.
            mGradient.setColors(mGradientColors);
        }
    }

    /**
     * Long pressing over the main clock starts the screen saver.
     */
    private final class StartScreenSaverListener implements View.OnLongClickListener {

        @Override
        public boolean onLongClick(View view) {
            /*bv zhangjiachu modify for alarm style 20200110 start
            this feature go to delete world clock*/
            final List<City> selected = DataModel.getDataModel().getSelectedCities();
            //bv zhangjiachu modify for fixbug:3685 start
            if (Utils.isBvOS()) {
                if ((selected.size() != 0)) {
                    if (getActivity().getResources().getBoolean(R.bool.bv_config_start_screensaver_flags)) {
                        LogUtils.d("zzzz onLongClick...mCityDeleteIng = " + mCityDeleteIng);
                        mCityDeleteIng = true;
                        mCityDeleteSelectAll = false;
                        mCityDeleteSelectNum = 0;

                        if (getActivity() instanceof DeskClock) {
                            ((DeskClock) getActivity()).bvPrepareMenu();
                        }

                        mUnSelectedDeleteCities.clear();
                        mUnSelectedDeleteCities.addAll(selected);
                        LogUtils.d("getItemCount: ClockFragment.mUnSelectedDeleteCities = " + ClockFragment.mUnSelectedDeleteCities);

                        /*bv zhangjiachu add for alarm style start*/
                        if (mCityDeleteIng) {
                            updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                        }
                        mCityAdapter.notifyDataSetChanged();
                    }
                }
            } else {
                /*bv zhangjiachu modify for alarm style 20200110 end*/
                //*/ hct.sjt, 2019-02-19, modify for start screensaver flags start.
                if (getActivity().getResources().getBoolean(R.bool.hct_config_start_screensaver_flags)) {
                    startActivity(new Intent(getActivity(), ScreensaverActivity.class)
                            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));
                } else {
                    startActivity(new Intent(getActivity(), ScreensaverActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));
                }
            }
            return true;
        }
    }

    /**
     * Long pressing over the city list starts the screen saver.
     */
    private final class CityListOnLongClickListener extends GestureDetector.SimpleOnGestureListener
            implements View.OnTouchListener {

        private final GestureDetector mGestureDetector;

        private CityListOnLongClickListener(Context context) {
            mGestureDetector = new GestureDetector(context, this);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            final View view = getView();
            if (view != null) {
                view.performLongClick();
            }
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
        }
    }

    /**
     * This runnable executes at every quarter-hour (e.g. 1:00, 1:15, 1:30, 1:45, etc...) and
     * updates the dates displayed within the UI. Quarter-hour increments were chosen to accommodate
     * the "weirdest" timezones (e.g. Nepal is UTC/GMT +05:45).
     */
    private final class QuarterHourRunnable implements Runnable {
        @Override
        public void run() {
            mCityAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Prior to L, a ContentObserver was used to monitor changes to the next scheduled alarm.
     * In L and beyond this is accomplished via a system broadcast of
     * {@link AlarmManager#ACTION_NEXT_ALARM_CLOCK_CHANGED}.
     */
    private final class AlarmObserverPreL extends ContentObserver {
        private AlarmObserverPreL() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            refreshAlarm();
        }
    }

    /**
     * Update the display of the scheduled alarm as it changes.
     */
    private final class AlarmChangedBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshAlarm();
        }
    }

    /**
     * Updates the vertical scroll state of this tab in the {@link UiDataModel} as the user scrolls
     * the recyclerview or when the size/position of elements within the recyclerview changes.
     */
    private final class ScrollPositionWatcher extends RecyclerView.OnScrollListener
            implements View.OnLayoutChangeListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            setTabScrolledToTop(Utils.isScrolledToTop(mCityList));
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                int oldLeft, int oldTop, int oldRight, int oldBottom) {
            setTabScrolledToTop(Utils.isScrolledToTop(mCityList));
        }
    }

    private static class BVSelectedCitiesAdapter extends SelectedCitiesAdapter{

        private final static int BV_MAIN_CLOCK = R.layout.bv_main_clock_frame;
        private final static int BV_WORLD_CLOCK = R.layout.bv_world_clock_item;
        final City homeCity = getHomeCity();//bv zhangjimeng added

        private BVSelectedCitiesAdapter(Context context, String dateFormat,
                /*bv zhangjiachu modify for alarm style: add handler start*/
                                      //String dateFormatForAccessibility) {
                                      String dateFormatForAccessibility, Handler handler) {
            /*bv zhangjiachu modify for alarm style: add handler end*/
            mContext = context;
            mDateFormat = dateFormat;
            mDateFormatForAccessibility = dateFormatForAccessibility;
            mInflater = LayoutInflater.from(context);
            mIsPortrait = Utils.isPortrait(context);
            mShowHomeClock = DataModel.getDataModel().getShowHomeClock();
            /*bv zhangjiachu modify for alarm style: add handler start*/
            myHandler=handler;
            /*bv zhangjiachu modify for alarm style: add handler end*/
        }

        public int getItemViewType(int position) {
            if (position == 0 && mIsPortrait) {
                return BV_MAIN_CLOCK;
            }
            return BV_WORLD_CLOCK;
        }

        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(viewType, parent, false);
            switch (viewType) {
                case BV_WORLD_CLOCK:
                    /*bv zhangjiachu modify for alarm style: add handler start*/
                    BvCityViewHolder cityViewHolder = new BvCityViewHolder(view);
                    cityViewHolder.setHandler(myHandler);
                    return  cityViewHolder;
                    /*bv zhangjiachu modify for alarm style: add handler end*/
                case BV_MAIN_CLOCK:
                    return new BvMainClockViewHolder(view);
                default:
                    throw new IllegalArgumentException("View type not recognized");
            }
        }

        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final int viewType = getItemViewType(position);
            switch (viewType) {
                case BV_WORLD_CLOCK:
                    // Retrieve the city to bind.
                    final City city;
                    // If showing home clock, put it at the top
                    if (mShowHomeClock && position == (mIsPortrait ? 1 : 0)) {
                        city = homeCity;
                    } else {
                        final int positionAdjuster = (mIsPortrait ? 1 : 0)
                                + (mShowHomeClock ? 1 : 0);
                        city = getCities().get(position - positionAdjuster);
                    }
                    ((BvCityViewHolder) holder).bind(mContext, city, position, mIsPortrait);
                    break;
                case BV_MAIN_CLOCK:
                    ((BvMainClockViewHolder) holder).bind(mContext, mDateFormat,
                            mDateFormatForAccessibility, getItemCount() > 1);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected view type: " + viewType);
            }
        }

        protected static final class BvCityViewHolder extends RecyclerView.ViewHolder {

            private final TextView mName;
            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;
            private final TextView mHoursAhead;
            /*bv zhangjiachu add for alarm style 20200110 start*/
            public CheckBox mCityDeleteChoice;
            /*bv zhangjiachu add for alarm style 20200110 end*/

            /*bv zhangjiachu modify for alarm style: add handler start*/
            private Handler mHandler = null;
            City homecity = null;
            boolean isAnalogClock = DataModel.getDataModel().getClockStyle() == DataModel.ClockStyle.ANALOG;

            public void setHandler(Handler handler) {
                mHandler = handler;
            }

            /*bv zhangjiachu modify for alarm style: add handler end*/
            private BvCityViewHolder(View itemView) {
                super(itemView);
                homecity = DataModel.getDataModel().getHomeCity();
                mName = (TextView) itemView.findViewById(R.id.city_name);
                mDigitalClock = (TextClock) itemView.findViewById(R.id.digital_clock);
                mAnalogClock = (AnalogClock) itemView.findViewById(R.id.analog_clock);
                mHoursAhead = (TextView) itemView.findViewById(R.id.hours_ahead);

                if (isAnalogClock) {
                    mDigitalClock.setVisibility(GONE);
                    mAnalogClock.setVisibility(VISIBLE);
                    mAnalogClock.enableSeconds(false);
                    Log.d(TAG, "bind : isAnalogClock mAnalogClock.setVisibility(VISIBLE);");
                } else {
                    mAnalogClock.setVisibility(GONE);
                    mDigitalClock.setVisibility(VISIBLE);
                    //bv zhangjiachu modify for bv alarm style 20200721 start
                    //mDigitalClock.setFormat12Hour(Utils.get12ModeFormat(0.3f /* amPmRatio */,
                    mDigitalClock.setFormat12Hour(Utils.get12ModeFormat(0.5f /* amPmRatio */,
                            //bv zhangjiachu modify for bv alarm style 20200721 end
                            false));
                    mDigitalClock.setFormat24Hour(Utils.get24ModeFormat(false));
                }

                /*bv zhangjiachu add for alarm style 20200110 start*/
                if (mIsBvOS) {
                    mCityDeleteChoice = (CheckBox) itemView.findViewById(R.id.city_choice);

                    //mCityDeleteChoice.setOnCheckedChangeListener(this);
                    mCityDeleteChoice.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final Context context = v.getContext();
                            final City city = (City) v.getTag();
                            CheckBox checkBox = (CheckBox) v;
                            if (checkBox.isChecked()) {
                                mUnSelectedDeleteCities.remove(city);
                                v.announceForAccessibility(context.getString(R.string.city_unchecked,
                                        city.getName()));
                                mCityDeleteSelectNum++;
                            } else {
                                mUnSelectedDeleteCities.add(city);
                                v.announceForAccessibility(context.getString(R.string.city_checked,
                                        city.getName()));
                                mCityDeleteSelectNum--;
                            }
                            /*bv zhangjiachu modify for alarm style: add handler start*/
                            if (mHandler != null) {
                                Message msg = mHandler.obtainMessage();
                                msg.what = AlarmUtils.MSG_WORLD_CLOCK_ALTER;
                                msg.obj = mCityDeleteSelectNum;
                                mHandler.sendMessage(msg);
                            }
                            /*bv zhangjiachu modify for alarm style: add handler end*/
                        }
                    });
                }
                /*bv zhangjiachu add for alarm style 20200110 end*/
            }

            private void bind(Context context, City city, int position, boolean isPortrait) {
                final String cityTimeZoneId = city.getTimeZone().getID();
                if(isAnalogClock){
                    mAnalogClock.setTimeZone(cityTimeZoneId);
                }else{
                    mDigitalClock.setTimeZone(cityTimeZoneId);
                }
                Log.d(TAG, "bind");
                // Configure the digital clock or analog clock depending on the user preference.

                /*bv zhangjiachu add for alarm style 20200111 start*/
                if (mIsBvOS) {
                    // bv zhangjiachu modify for debug bug id :287 2020-03-12 start
                    if (mCityDeleteIng && (city != homecity)) {
                        // bv zhangjiachu modify for debug bug id :287 2020-03-12 end
                        mCityDeleteChoice.setVisibility(VISIBLE);
                    } else {
                        mCityDeleteChoice.setVisibility(GONE);
                    }
                    mCityDeleteChoice.setTag(city);
                    // bv zhangjiachu modify for debug bug id :287 2020-03-12 start
                    if (city != homecity) {
                        mCityDeleteChoice.setChecked(!(mUnSelectedDeleteCities.contains(city)));
                    }
                    // bv zhangjiachu modify for debug bug id :287 2020-03-12 end
                    mCityDeleteChoice.setContentDescription(city.getName());
                    LogUtils.d("CityViewHolder1: mUnSelectedDeleteCities= " + mUnSelectedDeleteCities);
                }
                /*bv zhangjiachu add for alarm style 20200111 end*/

                LogUtils.d("zzzz bind...mCityDeleteIng = " + mCityDeleteIng);
                // Supply top and bottom padding dynamically.
                final Resources res = context.getResources();
                final int padding = res.getDimensionPixelSize(R.dimen.medium_space_top);
                final int top = position == 0 && !isPortrait ? 0 : padding;
                final int left = itemView.getPaddingLeft();
                final int right = itemView.getPaddingRight();
                /*bv zhangjiachu modify for alarm style 20200302 start*/
                final int bottom;
                if (mIsBvOS) {
                    /*bv zhangjiachu modify for alarm style 20200302 start*/
                    final int bottomPadding = res.getDimensionPixelSize(R.dimen.medium_space_bottom);
                    /*bv zhangjiachu modify for alarm style 20200302 end*/
                    bottom = bottomPadding; //itemView.getPaddingBottom();
                } else {
                    bottom = itemView.getPaddingBottom();
                }
                /*bv zhangjiachu modify for alarm style 20200302 end*/
                itemView.setPadding(left, top, right, bottom);

                // Bind the city name.
                mName.setText(city.getName());

                // Compute if the city week day matches the weekday of the current timezone.
                final Calendar localCal = Calendar.getInstance(TimeZone.getDefault());
                final Calendar cityCal = Calendar.getInstance(city.getTimeZone());
                final boolean displayDayOfWeek =
                        localCal.get(DAY_OF_WEEK) != cityCal.get(DAY_OF_WEEK);

                // Compare offset from UTC time on today's date (daylight savings time, etc.)
                final TimeZone currentTimeZone = TimeZone.getDefault();
                final TimeZone cityTimeZone = TimeZone.getTimeZone(cityTimeZoneId);
                final long currentTimeMillis = System.currentTimeMillis();
                final long currentUtcOffset = currentTimeZone.getOffset(currentTimeMillis);
                final long cityUtcOffset = cityTimeZone.getOffset(currentTimeMillis);
                final long offsetDelta = cityUtcOffset - currentUtcOffset;

                final int hoursDifferent = (int) (offsetDelta / DateUtils.HOUR_IN_MILLIS);
                final int minutesDifferent = (int) (offsetDelta / DateUtils.MINUTE_IN_MILLIS) % 60;
                final boolean displayMinutes = offsetDelta % DateUtils.HOUR_IN_MILLIS != 0;
                final boolean isAhead = hoursDifferent > 0 || (hoursDifferent == 0
                        && minutesDifferent > 0);
                if (!Utils.isLandscape(context)) {
                    // Bind the number of hours ahead or behind, or hide if the time is the same.
                    final boolean displayDifference = hoursDifferent != 0 || displayMinutes;
                    mHoursAhead.setVisibility(displayDifference ? VISIBLE : GONE);
                    final String timeString = Utils.createHoursDifferentString(
                            context, displayMinutes, isAhead, hoursDifferent, minutesDifferent);
                    mHoursAhead.setText(displayDayOfWeek ?
                            (context.getString(isAhead ? R.string.world_hours_tomorrow
                                    : R.string.world_hours_yesterday, timeString))
                            : timeString);
                } else {
                    // Only tomorrow/yesterday should be shown in landscape view.
                    mHoursAhead.setVisibility(displayDayOfWeek ? View.VISIBLE : View.GONE);
                    if (displayDayOfWeek) {
                        mHoursAhead.setText(context.getString(isAhead ? R.string.world_tomorrow
                                : R.string.world_yesterday));
                    }

                }

            }
        }

        protected static final class BvMainClockViewHolder extends RecyclerView.ViewHolder {

            private final View mHairline;
            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;
            private final TextClock mBvDigitalClock;
            private final AnalogClock mBvAnalogClock;
            private final LinearLayout mBvDigitalClockLayout;
            /*bv zhangjiachu add for alarm style 20200309 start*/
            private final LinearLayout mNoWorldClock;
            /*bv zhangjiachu add for alarm style 20200309 end*/

            private BvMainClockViewHolder(View itemView) {
                super(itemView);

                /*bv zhangjiachu add for alarm style 20200309 start*/
                mNoWorldClock = itemView.findViewById(R.id.no_worldclock);
                /*bv zhangjiachu add for alarm style 20200309 end*/
                mHairline = itemView.findViewById(R.id.hairline);
                mDigitalClock = (TextClock) itemView.findViewById(R.id.digital_clock);
                mAnalogClock = (AnalogClock) itemView.findViewById(R.id.analog_clock);
                //bv zhangjiachu add for bv os 20210302 start
                mBvDigitalClock = (TextClock) itemView.findViewById(R.id.bv_digital_clock);
                mBvAnalogClock = (AnalogClock) itemView.findViewById(R.id.bv_analog_clock);
                mBvDigitalClockLayout = (LinearLayout) itemView.findViewById(R.id.bv_digital_clock_layout);
                Utils.setBvTimeFormat(mBvDigitalClock, true);
                mBvDigitalClockLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBvDigitalClockLayout.setVisibility(GONE);
                        mBvAnalogClock.setVisibility(VISIBLE);
                    }
                });
                mBvAnalogClock.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBvDigitalClockLayout.setVisibility(VISIBLE);
                        mBvAnalogClock.setVisibility(GONE);
                    }
                });
                //bv zhangjiachu add for bv os 20210302 end
                Utils.setClockIconTypeface(itemView);
            }

            private void bind(Context context, String dateFormat,
                              String dateFormatForAccessibility, boolean showHairline) {
                /*bv zhangjiachu modify for alarm style 20200108 start*/
                if (mIsBvOS) {
                    Utils.bvRefreshAlarm(context, itemView);
                } else {
                    Utils.refreshAlarm(context, itemView);
                }
                /*bv zhangjiachu modify for alarm style 20200108 start*/

                Utils.updateDate(dateFormat, dateFormatForAccessibility, itemView);

                /*hct-fankou add DeskClock LightTheme 20191113 end*/
                Utils.setClockStyle(mDigitalClock, mAnalogClock);
                if (mIsBvOS) {
                    //bv zhangjiachu modify for hidden Hairline 20200312 start
                    mHairline.setVisibility(GONE);
                    //bv zhangjiachu modify for hidden Hairline 20200312 end
                    /*bv zhangjiachu add for alarm style 20200309 start*/
                    mNoWorldClock.setVisibility((DataModel.getDataModel().getSelectedCities().size() == 0 && !DataModel.getDataModel().getShowHomeClock())
                            && DataModel.getDataModel().getClockStyle() != DataModel.ClockStyle.ANALOG ? VISIBLE : GONE);
                    /*bv zhangjiachu add for alarm style 20200309 end*/
                } else {
                    mHairline.setVisibility(showHairline ? VISIBLE : GONE);
                }
                //bv zhangjiachu add  mIsBvOS 20200803
                if (mIsBvOS){
                    Utils.setBvClockSecondsEnabled(mDigitalClock, mAnalogClock);
                } else {
                    Utils.setClockSecondsEnabled(mDigitalClock, mAnalogClock);
                }
            }
        }

    }

    /**
     * This adapter lists all of the selected world clocks. Optionally, it also includes a clock at
     * the top for the home timezone if "Automatic home clock" is turned on in settings and the
     * current time at home does not match the current time in the timezone of the current location.
     * If the phone is in portrait mode it will also include the main clock at the top.
     */
    private static class SelectedCitiesAdapter extends RecyclerView.Adapter
            implements CityListener {

        private final static int MAIN_CLOCK = R.layout.main_clock_frame;
        private final static int WORLD_CLOCK = R.layout.world_clock_item;

        protected LayoutInflater mInflater;
        protected Context mContext;
        protected boolean mIsPortrait;
        protected boolean mShowHomeClock;
        protected String mDateFormat;
        protected String mDateFormatForAccessibility;

        /*bv zhangjiachu modify for alarm style: add handler start*/
        protected Handler myHandler;
		/*bv zhangjiachu modify for alarm style: add handler end*/

        private SelectedCitiesAdapter(Context context, String dateFormat,
                /*bv zhangjiachu modify for alarm style: add handler start*/
                //String dateFormatForAccessibility) {
                String dateFormatForAccessibility, Handler handler) {
            /*bv zhangjiachu modify for alarm style: add handler end*/
            mContext = context;
            mDateFormat = dateFormat;
            mDateFormatForAccessibility = dateFormatForAccessibility;
            mInflater = LayoutInflater.from(context);
            mIsPortrait = Utils.isPortrait(context);
            mShowHomeClock = DataModel.getDataModel().getShowHomeClock();
			/*bv zhangjiachu modify for alarm style: add handler start*/
            myHandler=handler;
			/*bv zhangjiachu modify for alarm style: add handler end*/
        }

        private SelectedCitiesAdapter() {}

        @Override
        public int getItemViewType(int position) {
            if (position == 0 && mIsPortrait) {
                return MAIN_CLOCK;
            }
            return WORLD_CLOCK;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(viewType, parent, false);
            switch (viewType) {
                case WORLD_CLOCK:
                    if(Utils.isBvOS()){
                        /*bv zhangjiachu modify for alarm style: add handler start*/
                        CityViewHolder cityViewHolder = new CityViewHolder(view);
                        cityViewHolder.setHandler(myHandler);
                        return  cityViewHolder;
                        /*bv zhangjiachu modify for alarm style: add handler end*/
                    }else {
                        return new CityViewHolder(view);
                    }
                case MAIN_CLOCK:
                    return new MainClockViewHolder(view);
                default:
                    throw new IllegalArgumentException("View type not recognized");
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final int viewType = getItemViewType(position);
            switch (viewType) {
                case WORLD_CLOCK:
                    // Retrieve the city to bind.
                    final City city;
                    // If showing home clock, put it at the top
                    if (mShowHomeClock && position == (mIsPortrait ? 1 : 0)) {
                        city = getHomeCity();
                    } else {
                        final int positionAdjuster = (mIsPortrait ? 1 : 0)
                                + (mShowHomeClock ? 1 : 0);
                        city = getCities().get(position - positionAdjuster);
                    }
                    ((CityViewHolder) holder).bind(mContext, city, position, mIsPortrait);
                    break;
                case MAIN_CLOCK:
                    ((MainClockViewHolder) holder).bind(mContext, mDateFormat,
                            mDateFormatForAccessibility, getItemCount() > 1);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected view type: " + viewType);
            }
        }

        @Override
        public int getItemCount() {
            final int mainClockCount = mIsPortrait ? 1 : 0;
            final int homeClockCount = mShowHomeClock ? 1 : 0;
            final int worldClockCount = getCities().size();
            return mainClockCount + homeClockCount + worldClockCount;
        }

        protected City getHomeCity() {
            return DataModel.getDataModel().getHomeCity();
        }

        protected List<City> getCities() {
            return DataModel.getDataModel().getSelectedCities();
        }

        private void refreshAlarm() {
            if (mIsPortrait && getItemCount() > 0) {
                notifyItemChanged(0);
            }
        }

        @Override
        public void citiesChanged(List<City> oldCities, List<City> newCities) {
            notifyDataSetChanged();
        }

        protected static final class CityViewHolder extends RecyclerView.ViewHolder {

            private final TextView mName;
            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;
            private final TextView mHoursAhead;
            /*bv zhangjiachu add for alarm style 20200110 start*/
            public  CheckBox mCityDeleteChoice;
            /*bv zhangjiachu add for alarm style 20200110 end*/

            /*bv zhangjiachu modify for alarm style: add handler start*/
            private Handler mHandler=null;

            public void setHandler(Handler handler){
                mHandler=handler;
            }
            /*bv zhangjiachu modify for alarm style: add handler end*/
            private CityViewHolder(View itemView) {
                super(itemView);

                mName = (TextView) itemView.findViewById(R.id.city_name);
                mDigitalClock = (TextClock) itemView.findViewById(R.id.digital_clock);
                mAnalogClock = (AnalogClock) itemView.findViewById(R.id.analog_clock);
                mHoursAhead = (TextView) itemView.findViewById(R.id.hours_ahead);
                /*bv zhangjiachu add for alarm style 20200110 start*/
                if(Utils.isBvOS()){
                    mCityDeleteChoice = (CheckBox) itemView.findViewById(R.id.city_choice);

                    //mCityDeleteChoice.setOnCheckedChangeListener(this);
                    mCityDeleteChoice.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final Context context = v.getContext();
                            final City city = (City) v.getTag();
                            CheckBox checkBox=(CheckBox)v;
                            if (checkBox.isChecked()){
                                mUnSelectedDeleteCities.remove(city);
                                v.announceForAccessibility(context.getString(R.string.city_unchecked,
                                        city.getName()));
                                mCityDeleteSelectNum++;
                            } else {
                                mUnSelectedDeleteCities.add(city);
                                v.announceForAccessibility(context.getString(R.string.city_checked,
                                        city.getName()));
                                mCityDeleteSelectNum--;
                            }
                            /*bv zhangjiachu modify for alarm style: add handler start*/
                            if (mHandler!=null) {
                                Message msg = mHandler.obtainMessage();
                                msg.what = AlarmUtils.MSG_WORLD_CLOCK_ALTER;
                                msg.obj = mCityDeleteSelectNum;
                                mHandler.sendMessage(msg);
                            }
                            /*bv zhangjiachu modify for alarm style: add handler end*/
                        }
                    });
                }
                /*bv zhangjiachu add for alarm style 20200110 end*/
            }

            private void bind(Context context, City city, int position, boolean isPortrait) {
                final String cityTimeZoneId = city.getTimeZone().getID();

                // Configure the digital clock or analog clock depending on the user preference.
                if (DataModel.getDataModel().getClockStyle() == DataModel.ClockStyle.ANALOG) {
                    mDigitalClock.setVisibility(GONE);
                    mAnalogClock.setVisibility(VISIBLE);
                    mAnalogClock.setTimeZone(cityTimeZoneId);
                    mAnalogClock.enableSeconds(false);
                } else {
                    mAnalogClock.setVisibility(GONE);
                    mDigitalClock.setVisibility(VISIBLE);
                    mDigitalClock.setTimeZone(cityTimeZoneId);
                    //bv zhangjiachu modify for bv alarm style 20200721 start
                    //mDigitalClock.setFormat12Hour(Utils.get12ModeFormat(0.3f /* amPmRatio */,
                    mDigitalClock.setFormat12Hour(Utils.get12ModeFormat(0.5f /* amPmRatio */,
                            //bv zhangjiachu modify for bv alarm style 20200721 end
                            false));
                    mDigitalClock.setFormat24Hour(Utils.get24ModeFormat(false));
                }

                /*bv zhangjiachu add for alarm style 20200111 start*/
                if(Utils.isBvOS()){
                    // bv zhangjiachu modify for debug bug id :287 2020-03-12 start
                    if (mCityDeleteIng && (city != DataModel.getDataModel().getHomeCity())){
                        // bv zhangjiachu modify for debug bug id :287 2020-03-12 end
                        mCityDeleteChoice.setVisibility(VISIBLE);
                    } else {
                        mCityDeleteChoice.setVisibility(GONE);
                    }
                    mCityDeleteChoice.setTag(city);
                    // bv zhangjiachu modify for debug bug id :287 2020-03-12 start
                    if (city != DataModel.getDataModel().getHomeCity()) {
                        mCityDeleteChoice.setChecked(!(mUnSelectedDeleteCities.contains(city)));
                    }
                    // bv zhangjiachu modify for debug bug id :287 2020-03-12 end
                    mCityDeleteChoice.setContentDescription(city.getName());
                    LogUtils.d("CityViewHolder1: mUnSelectedDeleteCities= " + mUnSelectedDeleteCities);
                }
                /*bv zhangjiachu add for alarm style 20200111 end*/

                LogUtils.d("zzzz bind...mCityDeleteIng = " + mCityDeleteIng);
                // Supply top and bottom padding dynamically.
                final Resources res = context.getResources();
                final int padding = res.getDimensionPixelSize(R.dimen.medium_space_top);
                final int top = position == 0 && !isPortrait ? 0 : padding;
                final int left = itemView.getPaddingLeft();
                final int right = itemView.getPaddingRight();
                /*bv zhangjiachu modify for alarm style 20200302 start*/
                final int bottom;
                if(Utils.isBvOS()){
                    /*bv zhangjiachu modify for alarm style 20200302 start*/
                    final int bottomPadding = res.getDimensionPixelSize(R.dimen.medium_space_bottom);
                    /*bv zhangjiachu modify for alarm style 20200302 end*/
                    bottom = bottomPadding; //itemView.getPaddingBottom();
                }else {
                    bottom = itemView.getPaddingBottom();
                }
                /*bv zhangjiachu modify for alarm style 20200302 end*/
                itemView.setPadding(left, top, right, bottom);

                // Bind the city name.
                mName.setText(city.getName());

                // Compute if the city week day matches the weekday of the current timezone.
                final Calendar localCal = Calendar.getInstance(TimeZone.getDefault());
                final Calendar cityCal = Calendar.getInstance(city.getTimeZone());
                final boolean displayDayOfWeek =
                        localCal.get(DAY_OF_WEEK) != cityCal.get(DAY_OF_WEEK);

                // Compare offset from UTC time on today's date (daylight savings time, etc.)
                final TimeZone currentTimeZone = TimeZone.getDefault();
                final TimeZone cityTimeZone = TimeZone.getTimeZone(cityTimeZoneId);
                final long currentTimeMillis = System.currentTimeMillis();
                final long currentUtcOffset = currentTimeZone.getOffset(currentTimeMillis);
                final long cityUtcOffset = cityTimeZone.getOffset(currentTimeMillis);
                final long offsetDelta = cityUtcOffset - currentUtcOffset;

                final int hoursDifferent = (int) (offsetDelta / DateUtils.HOUR_IN_MILLIS);
                final int minutesDifferent = (int) (offsetDelta / DateUtils.MINUTE_IN_MILLIS) % 60;
                final boolean displayMinutes = offsetDelta % DateUtils.HOUR_IN_MILLIS != 0;
                final boolean isAhead = hoursDifferent > 0 || (hoursDifferent == 0
                        && minutesDifferent > 0);
                if (!Utils.isLandscape(context)) {
                    // Bind the number of hours ahead or behind, or hide if the time is the same.
                    final boolean displayDifference = hoursDifferent != 0 || displayMinutes;
                    mHoursAhead.setVisibility(displayDifference ? VISIBLE : GONE);
                    final String timeString = Utils.createHoursDifferentString(
                            context, displayMinutes, isAhead, hoursDifferent, minutesDifferent);
                    mHoursAhead.setText(displayDayOfWeek ?
                            (context.getString(isAhead ? R.string.world_hours_tomorrow
                                    : R.string.world_hours_yesterday, timeString))
                            : timeString);
                } else {
                    // Only tomorrow/yesterday should be shown in landscape view.
                    mHoursAhead.setVisibility(displayDayOfWeek ? View.VISIBLE : View.GONE);
                    if (displayDayOfWeek) {
                        mHoursAhead.setText(context.getString(isAhead ? R.string.world_tomorrow
                                : R.string.world_yesterday));
                    }

                }
            }
        }

        protected static final class MainClockViewHolder extends RecyclerView.ViewHolder {

            private final View mHairline;
            private final TextClock mDigitalClock;
            private final AnalogClock mAnalogClock;
            /*bv zhangjiachu add for alarm style 20200309 start*/
            private final LinearLayout mNoWorldClock;
            /*bv zhangjiachu add for alarm style 20200309 end*/

            private MainClockViewHolder(View itemView) {
                super(itemView);

                /*bv zhangjiachu add for alarm style 20200309 start*/
                mNoWorldClock = itemView.findViewById(R.id.no_worldclock);
                /*bv zhangjiachu add for alarm style 20200309 end*/
                mHairline = itemView.findViewById(R.id.hairline);
                mDigitalClock = (TextClock) itemView.findViewById(R.id.digital_clock);
                mAnalogClock = (AnalogClock) itemView.findViewById(R.id.analog_clock);
                Utils.setClockIconTypeface(itemView);
            }

            private void bind(Context context, String dateFormat,
                    String dateFormatForAccessibility, boolean showHairline) {
                /*bv zhangjiachu modify for alarm style 20200108 start*/
                if(Utils.isBvOS()){
                    Utils.bvRefreshAlarm(context, itemView);
                }else {
                    Utils.refreshAlarm(context, itemView);
                }
                /*bv zhangjiachu modify for alarm style 20200108 start*/

                Utils.updateDate(dateFormat, dateFormatForAccessibility, itemView);
                Utils.setClockStyle(mDigitalClock, mAnalogClock);
                if(Utils.isBvOS()){
                    //bv zhangjiachu modify for hidden Hairline 20200312 start
                    mHairline.setVisibility(GONE);
                    //bv zhangjiachu modify for hidden Hairline 20200312 end
                    /*bv zhangjiachu add for alarm style 20200309 start*/
                    mNoWorldClock.setVisibility((DataModel.getDataModel().getSelectedCities().size() == 0 && !DataModel.getDataModel().getShowHomeClock())
                            && DataModel.getDataModel().getClockStyle() != DataModel.ClockStyle.ANALOG ? VISIBLE : GONE);
                    /*bv zhangjiachu add for alarm style 20200309 end*/
                }else {
                    mHairline.setVisibility(showHairline ? VISIBLE : GONE);
                }
                //bv zhangjiachu add  mIsBvOS 20200803
                if (mIsBvOS){
                    Utils.setBvClockSecondsEnabled(mDigitalClock, mAnalogClock);
                } else {
                    Utils.setClockSecondsEnabled(mDigitalClock, mAnalogClock);
                }
            }
        }
    }
}
