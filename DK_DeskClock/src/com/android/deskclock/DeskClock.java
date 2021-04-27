/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.UiModeManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
/*bv zhangjiachu modify for alarm style 20200103 start*/
import android.widget.Button;
import android.widget.ImageButton;

import android.widget.LinearLayout;
import android.support.v4.content.ContextCompat;
import android.content.res.ColorStateList;
/*bv zhangjiachu modify for alarm style 20200103 end*/
import android.widget.ImageView;
import android.widget.TextView;

import com.android.deskclock.actionbarmenu.MenuItemControllerFactory;
import com.android.deskclock.actionbarmenu.NightModeMenuItemController;
import com.android.deskclock.actionbarmenu.OptionsMenuManager;
import com.android.deskclock.actionbarmenu.SettingsMenuItemController;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.DataModel.SilentSetting;
import com.android.deskclock.data.OnSilentSettingsListener;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.uidata.TabListener;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.widget.toast.SnackbarManager;

import static android.support.v4.view.ViewPager.SCROLL_STATE_DRAGGING;
import static android.support.v4.view.ViewPager.SCROLL_STATE_IDLE;
import static android.support.v4.view.ViewPager.SCROLL_STATE_SETTLING;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.android.deskclock.AnimatorUtils.getScaleAnimator;
/*hct-fankou add DeskClock LightTheme 20191113 start*/
import android.os.SystemProperties;
import android.graphics.Color;
import android.view.Window;
import android.view.WindowManager;
/*hct-fankou add DeskClock LightTheme 20191113 end*/
//bv zhangjiachu add for modify Image color in dark theme 20200528 start
import android.graphics.PorterDuff;
//bv zhangjiachu add for modify Image color in dark theme 20200528 end
/**
 * The main activity of the application which displays 4 different tabs contains alarms, world
 * clocks, timers and a stopwatch.
 */
public class DeskClock extends BaseActivity
        implements FabContainer, LabelDialogFragment.AlarmLabelDialogHandler {
    /*bv zhangjiachu modify for alarm style 20200103 start*/
    private static final String TAG = "DeskClock";
	/*bv zhangjiachu modify for alarm style 20200103 end*/

    /** Models the interesting state of display the {@link #mFab} button may inhabit. */
    private enum FabState { SHOWING, HIDE_ARMED, HIDING }

    /** Coordinates handling of context menu items. */
    private final OptionsMenuManager mOptionsMenuManager = new OptionsMenuManager();

    /** Shrinks the {@link #mFab}, {@link #mLeftButton} and {@link #mRightButton} to nothing. */
    private final AnimatorSet mHideAnimation = new AnimatorSet();

    /** Grows the {@link #mFab}, {@link #mLeftButton} and {@link #mRightButton} to natural sizes. */
    private final AnimatorSet mShowAnimation = new AnimatorSet();

    /** Hides, updates, and shows only the {@link #mFab}; the buttons are untouched. */
    private final AnimatorSet mUpdateFabOnlyAnimation = new AnimatorSet();

    /** Hides, updates, and shows only the {@link #mLeftButton} and {@link #mRightButton}. */
    private final AnimatorSet mUpdateButtonsOnlyAnimation = new AnimatorSet();

    /** Automatically starts the {@link #mShowAnimation} after {@link #mHideAnimation} ends. */
    private final AnimatorListenerAdapter mAutoStartShowListener = new AutoStartShowListener();

    /** Updates the user interface to reflect the selected tab from the backing model. */
    private final TabListener mTabChangeWatcher = new TabChangeWatcher();

    /** Shows/hides a snackbar explaining which setting is suppressing alarms from firing. */
    private final OnSilentSettingsListener mSilentSettingChangeWatcher =
            new SilentSettingChangeWatcher();

    /** Displays a snackbar explaining why alarms may not fire or may fire silently. */
    private Runnable mShowSilentSettingSnackbarRunnable;

    /** The view to which snackbar items are anchored. */
    private View mSnackbarAnchor;

    /** The current display state of the {@link #mFab}. */
    private FabState mFabState = FabState.SHOWING;

    /** The single floating-action button shared across all tabs in the user interface. */
    private ImageView mFab;

    /** The button left of the {@link #mFab} shared across all tabs in the user interface. */
    /*bv zhangjiachu modify for alarm style 20200103 start*/
    private Button mLeftButton;
    /** The button right of the {@link #mFab} shared across all tabs in the user interface. */
    private Button mRightButton;
    private ImageButton mBVLeftButton;
    private ImageButton mBVRightButton;

    private LinearLayout mDeleteAlarmTab; //delete_alarm_tab
    private TextView mDeleteButtom;
    private LinearLayout mDeleteButtomLayout;

    private TextView mCancel;
    private TextView mSeleteAll;
    private TextView mSeleteNum;

    //bv zhangjiachu add for title 20210301
    private TextView mDeskTitle;

    /*bv zhangjiachu modify for alarm style 20200103 end*/

    //bv zhangjiachu add for fixbug:913 20200528 start
    private boolean mSettingsChanged=false;
    //bv zhangjiachu add for fixbug:913 20200528 end

    /** The controller that shows the drop shadow when content is not scrolled to the top. */
    private DropShadowController mDropShadowController;

    /** The ViewPager that pages through the fragments representing the content of the tabs. */
    private ViewPager mFragmentTabPager;

    /** Generates the fragments that are displayed by the {@link #mFragmentTabPager}. */
    private FragmentTabPagerAdapter mFragmentTabPagerAdapter;

    /** The container that stores the tab headers. */
    private TabLayout mTabLayout;

    /** {@code true} when a settings change necessitates recreating this activity. */
    private boolean mRecreateActivity;
    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);

        // Fragments may query the latest intent for information, so update the intent.
        setIntent(newIntent);
    }

    //bv zhangjimeng 2020/07/04, modify deskclock perf,begin
    private boolean mIsBvOS = Utils.isBvOS();
    private boolean mIsLowLevelPhone = Utils.isLowLevelPhone();
    private boolean mIsLightTheme = false;

    private void initTheme() {
        if (ThemeUtils.isDarkTheme(this)){
            SystemProperties.set("sys.hctclock_lighttheme", "false");
            setTheme(R.style.ThemeDarkDeskClock);
        } else {
            SystemProperties.set("sys.hctclock_lighttheme", "true");
            setTheme(R.style.HctThemeDeskClock);
            mIsLightTheme = true;
        }
    }
    private void initTab() {
        // Create the tabs that make up the user interface.
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        final int tabCount = UiDataModel.getUiDataModel().getTabCount();
        final boolean showTabLabel = getResources().getBoolean(R.bool.showTabLabel);
        final boolean showTabHorizontally = getResources().getBoolean(R.bool.showTabHorizontally);
        for (int i = 0; i < tabCount; i++) {
            final UiDataModel.Tab tabModel = UiDataModel.getUiDataModel().getTab(i);
            final @StringRes int labelResId = tabModel.getLabelResId();

            final TabLayout.Tab tab = mTabLayout.newTab()
                    .setTag(tabModel)
                    .setIcon(tabModel.getIconResId())
                    .setContentDescription(labelResId);

            if (showTabLabel) {
                tab.setText(labelResId);
                tab.setCustomView(R.layout.tab_item);

                @SuppressWarnings("ConstantConditions")
                final TextView text = (TextView) tab.getCustomView()
                        .findViewById(android.R.id.text1);
                //bv zhangjiachu modify for TabAlarms text color 20200604 start
                mTabLayout.setTabTextColors(getResources().getColor(R.color.colorTabAlarmsTitle), getResources().getColor(R.color.hct_Accent_color));
                //bv zhangjiachu modify for TabAlarms text color 20200604 end
                text.setTextColor(mTabLayout.getTabTextColors());

                // Bind the icon to the TextView.
                final Drawable icon = tab.getIcon();
                icon.setTintList(mTabLayout.getTabTextColors());
                if (showTabHorizontally) {
                    // Remove the icon so it doesn't affect the minimum TabLayout height.
                    tab.setIcon(null);
                    text.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                } else {
                    text.setCompoundDrawablesRelativeWithIntrinsicBounds(null, icon, null, null);
                }
            }
            mTabLayout.addTab(tab);
            UiDataModel.getUiDataModel().addTabListener(mTabChangeWatcher);
        }
    }

    private void initAnimations() {

        long duration = UiDataModel.getUiDataModel().getShortAnimationDuration();
        final ValueAnimator hideFabAnimation = getScaleAnimator(mFab, 1f, 0f);
        final ValueAnimator showFabAnimation = getScaleAnimator(mFab, 0f, 1f);

        final ValueAnimator leftHideAnimation;
        final ValueAnimator rightHideAnimation;
        final ValueAnimator leftShowAnimation;
        final ValueAnimator rightShowAnimation;

        leftHideAnimation = getScaleAnimator(mBVLeftButton, 1f, 0f);
        rightHideAnimation = getScaleAnimator(mBVRightButton, 1f, 0f);
        leftShowAnimation = getScaleAnimator(mBVLeftButton, 0f, 1f);
        rightShowAnimation = getScaleAnimator(mBVRightButton, 0f, 1f);

        leftHideAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                getSelectedDeskClockFragment().onUpdateFabButtons(mBVLeftButton, mBVRightButton);
            }
        });
        hideFabAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                getSelectedDeskClockFragment().onUpdateFab(mFab);
            }
        });

        // Build the reusable animations that hide and show the fab and left/right buttons.
        // These may be used independently or be chained together.
        mHideAnimation
                .setDuration(duration)
                .play(hideFabAnimation)
                .with(leftHideAnimation)
                .with(rightHideAnimation);

        mShowAnimation
                .setDuration(duration)
                .play(showFabAnimation)
                .with(leftShowAnimation)
                .with(rightShowAnimation);

        // Build the reusable animation that hides and shows only the fab.
        mUpdateFabOnlyAnimation
                .setDuration(duration)
                .play(showFabAnimation)
                .after(hideFabAnimation);

        // Build the reusable animation that hides and shows only the buttons.
        mUpdateButtonsOnlyAnimation
                .setDuration(duration)
                .play(leftShowAnimation)
                .with(rightShowAnimation)
                .after(leftHideAnimation)
                .after(rightHideAnimation);
    }

    private void initListeners() {
        mFab.setOnClickListener(view -> getSelectedDeskClockFragment().onFabClick(mFab));
        mBVLeftButton.setOnClickListener(view -> getSelectedDeskClockFragment().onLeftButtonClick(mBVLeftButton));
        mBVRightButton.setOnClickListener(view -> getSelectedDeskClockFragment().onRightButtonClick(mBVRightButton));

        mDeleteButtom.setOnClickListener(v -> getSelectedDeskClockFragment().onUpdateDelClick(mDeleteButtom));

        mCancel.setOnClickListener(v -> getSelectedDeskClockFragment().onUpdateCancelClick(mCancel));

        mSeleteAll.setOnClickListener(v -> getSelectedDeskClockFragment().onUpdateSeleteAllClick(mSeleteAll));

        // Mirror changes made to the selected tab into UiDataModel.
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                UiDataModel.getUiDataModel().setSelectedTab((UiDataModel.Tab) tab.getTag());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void bvCreateDeskClock(Bundle savedInstanceState) {
        initTheme();
        setContentView(R.layout.bv_desk_clock);
        mSnackbarAnchor = findViewById(R.id.content);

        // Configure the toolbar.
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (mIsLightTheme) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(
                    getString(R.string.hct_default_actionbar_background)));
            //bv zhangjiachu add for alarm style 20200514 start
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            window.setNavigationBarColor(Color.parseColor(
                    getString(R.string.hct_default_actionbar_background)));
            View hairline = mSnackbarAnchor.findViewById(R.id.tab_hairline);
            hairline.setBackgroundColor(Color.parseColor(
                    getString(R.string.hct_line_color)));
            toolbar.setBackgroundColor(Color.parseColor(
                    getString(R.string.hct_default_actionbar_background)));
        }
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        // Configure the menu item controllers add behavior to the toolbar.
        //bv zhangjiachu delete new NightModeMenuItemController(this) 20200723
        mOptionsMenuManager.addMenuItemController(
                /*new NightModeMenuItemController(this),*/ new SettingsMenuItemController(this));
        mOptionsMenuManager.addMenuItemController(
                MenuItemControllerFactory.getInstance().buildMenuItemControllers(this));

        // Inflate the menu during creation to avoid a double layout pass. Otherwise, the menu
        // inflation occurs *after* the initial draw and a second layout pass adds in the menu.
        onCreateOptionsMenu(toolbar.getMenu());

        mDeskTitle = (TextView) findViewById(R.id.bv_deskclock_title);

        mDeleteAlarmTab = (LinearLayout) findViewById(R.id.delete_alarm_tab);
        mCancel = (TextView) findViewById(R.id.cancel_delete);
        mSeleteAll = (TextView) findViewById(R.id.select_all);
        /*alarm num checked*/
        mSeleteNum = (TextView) findViewById(R.id.select_num);

        mDeleteButtomLayout = (LinearLayout) findViewById(R.id.delete_buttom_layout);
        mDeleteButtom = (TextView) findViewById(R.id.delete_buttom);
        initTab();

        mFab = (ImageView) findViewById(R.id.fab);
        mBVLeftButton = (ImageButton) findViewById(R.id.left_button);
        mBVRightButton = (ImageButton) findViewById(R.id.right_button);

        // Customize the view pager.
        mFragmentTabPagerAdapter = new FragmentTabPagerAdapter(this);
        mFragmentTabPager = (ViewPager) findViewById(R.id.desk_clock_pager);
        // Keep all four tabs to minimize jank.
        mFragmentTabPager.setOffscreenPageLimit(3);
        // Set Accessibility Delegate to null so view pager doesn't intercept movements and
        // prevent the fab from being selected.
        mFragmentTabPager.setAccessibilityDelegate(null);
        // Mirror changes made to the selected page of the view pager into UiDataModel.
        mFragmentTabPager.addOnPageChangeListener(new PageChangeWatcher());
        mFragmentTabPager.setAdapter(mFragmentTabPagerAdapter);

        mTabLayout.setTabRippleColor(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.transparent)));
        initAnimations();
        initListeners();
        return;
    }
    //bv zhangjimeng 2020/07/04, modify deskclock perf,end

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //bv zhangjimeng 2020/07/04, modify deskclock perf,begin
        if(mIsBvOS){
            bvCreateDeskClock(savedInstanceState);
            return;
        }
        //bv zhangjimeng 2020/07/04, modify deskclock perf,end


        /*hct-fankou add DeskClock LightTheme 20191113 start*/
        //hct-fangkou modify for support NightMode 20191211 start
        if(1==SystemProperties.getInt("ro.hct_clock_change_theme",0)){
            if(isNightMode()){
                SystemProperties.set("sys.hctclock_lighttheme","false");
                setTheme(R.style.ThemeDarkDeskClock);
            }else{
                SystemProperties.set("sys.hctclock_lighttheme","true");
                setTheme(R.style.HctThemeDeskClock);
            }
        }else{//hct-fangkou modify for support NightMode 20191211 end
            if(isNightMode()){
                setTheme(R.style.ThemeDarkDeskClock);
            }
        }
        /*hct-fankou add DeskClock LightTheme 20191113 end*/
        if(Utils.isBvOS()){
            setContentView(R.layout.bv_desk_clock);
        }else {
            setContentView(R.layout.desk_clock);
        }
        mSnackbarAnchor = findViewById(R.id.content);

        // Configure the toolbar.
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        /*hct-fankou add DeskClock LightTheme 20191113 start*/
        if(SystemProperties.getBoolean("sys.hctclock_lighttheme",false) || !ThemeUtils.isDarkTheme(getApplicationContext())){
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            /*zhangjiachu del for fixbug: 1381 20200706 start*/
            /*window.setStatusBarColor(Color.parseColor(
                    getString(R.string.hct_default_actionbar_background)));*/
            /*zhangjiachu del for fixbug: 1381 20200706 end*/
            //bv zhangjiachu add for alarm style 20200514 start
            if(Utils.isBvOS()) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                window.setNavigationBarColor(Color.parseColor(
                        getString(R.string.hct_default_actionbar_background)));
            } else {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
            //bv zhangjiachu add for alarm style 20200514 end
            View hairline= mSnackbarAnchor.findViewById(R.id.tab_hairline);
            hairline.setBackgroundColor(Color.parseColor(
                    getString(R.string.hct_line_color)));
            toolbar.setBackgroundColor(Color.parseColor(
                    getString(R.string.hct_default_actionbar_background)));
        }
        /*hct-fankou add DeskClock LightTheme 20191113 end*/
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        // Configure the menu item controllers add behavior to the toolbar.
        mOptionsMenuManager.addMenuItemController(
                new NightModeMenuItemController(this), new SettingsMenuItemController(this));
        mOptionsMenuManager.addMenuItemController(
                MenuItemControllerFactory.getInstance().buildMenuItemControllers(this));

        // Inflate the menu during creation to avoid a double layout pass. Otherwise, the menu
        // inflation occurs *after* the initial draw and a second layout pass adds in the menu.
        onCreateOptionsMenu(toolbar.getMenu());
        if(Utils.isBvOS()){
            /*bv zhangjiachu add for alarm style 20200104 start*/
            mDeleteAlarmTab = (LinearLayout) findViewById(R.id.delete_alarm_tab);
            mCancel = (TextView) findViewById(R.id.cancel_delete);
            mSeleteAll = (TextView) findViewById(R.id.select_all);
            /*alarm num checked*/
            mSeleteNum = (TextView) findViewById(R.id.select_num);

            mDeleteButtomLayout = (LinearLayout)findViewById(R.id.delete_buttom_layout);
            mDeleteButtom =(TextView) findViewById(R.id.delete_buttom);
            /*bv zhangjiachu add for alarm style 20200104 end*/
        }
        // Create the tabs that make up the user interface.
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        final int tabCount = UiDataModel.getUiDataModel().getTabCount();
        final boolean showTabLabel = getResources().getBoolean(R.bool.showTabLabel);
        final boolean showTabHorizontally = getResources().getBoolean(R.bool.showTabHorizontally);
        for (int i = 0; i < tabCount; i++) {
            final UiDataModel.Tab tabModel = UiDataModel.getUiDataModel().getTab(i);
            final @StringRes int labelResId = tabModel.getLabelResId();

            final TabLayout.Tab tab = mTabLayout.newTab()
                    .setTag(tabModel)
                    .setIcon(tabModel.getIconResId())
                    .setContentDescription(labelResId);

            if (showTabLabel) {
                tab.setText(labelResId);
                tab.setCustomView(R.layout.tab_item);

                @SuppressWarnings("ConstantConditions")
                final TextView text = (TextView) tab.getCustomView()
                        .findViewById(android.R.id.text1);
                text.setTextColor(mTabLayout.getTabTextColors());

                // Bind the icon to the TextView.
                final Drawable icon = tab.getIcon();
                if (showTabHorizontally) {
                    // Remove the icon so it doesn't affect the minimum TabLayout height.
                    tab.setIcon(null);
                    text.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                } else {
                    text.setCompoundDrawablesRelativeWithIntrinsicBounds(null, icon, null, null);
                }
            }

            mTabLayout.addTab(tab);
        }
        /*hct-fankou add DeskClock LightTheme 20191113 start*/
        if(ThemeUtils.isDarkTheme(getApplicationContext()) && !Utils.isBvOS()){
            mTabLayout.getTabAt(0).setIcon(R.drawable.hct_ic_tab_alarm_activated);
            mTabLayout.getTabAt(1).setIcon(R.drawable.hct_ic_tab_clock_normal);
            mTabLayout.getTabAt(2).setIcon(R.drawable.hct_ic_tab_timer_normal);
            mTabLayout.getTabAt(3).setIcon(R.drawable.hct_ic_tab_stopwatch_normal);
        }
        /*hct-fankou add DeskClock LightTheme 20191113 end*/

        // Configure the buttons shared by the tabs.
        mFab = (ImageView) findViewById(R.id.fab);
        /*bv zhangjiachu modify for alarm style 20200103 start*/
        if(Utils.isBvOS()){
            mBVLeftButton = (ImageButton) findViewById(R.id.left_button);
            mBVRightButton = (ImageButton) findViewById(R.id.right_button);
        }else {
            mLeftButton = (Button) findViewById(R.id.left_button);
            mRightButton = (Button) findViewById(R.id.right_button);
        }
        /*bv zhangjiachu modify for alarm style 20200103 end*/

        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getSelectedDeskClockFragment().onFabClick(mFab);
            }
        });

        if(Utils.isBvOS()){
            /*bv zhangjiachu add for alarm style 20200104 start*/
            mBVLeftButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    getSelectedDeskClockFragment().onLeftButtonClick(mBVLeftButton);
                }
            });
            mBVRightButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    getSelectedDeskClockFragment().onRightButtonClick(mBVRightButton);
                }
            });

            mDeleteButtom.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    getSelectedDeskClockFragment().onUpdateDelClick(mDeleteButtom);
                }
            });

            mCancel.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                    getSelectedDeskClockFragment().onUpdateCancelClick(mCancel);
                }
            });

            mSeleteAll.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {

                    getSelectedDeskClockFragment().onUpdateSeleteAllClick(mSeleteAll);
                }
            });
            /*bv zhangjiachu add for alarm style 20200104 end*/
        }else {
            mLeftButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    getSelectedDeskClockFragment().onLeftButtonClick(mLeftButton);
                }
            });
            mRightButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    getSelectedDeskClockFragment().onRightButtonClick(mRightButton);
                }
            });
        }

        final long duration = UiDataModel.getUiDataModel().getShortAnimationDuration();

        final ValueAnimator hideFabAnimation = getScaleAnimator(mFab, 1f, 0f);
        final ValueAnimator showFabAnimation = getScaleAnimator(mFab, 0f, 1f);

        final ValueAnimator leftHideAnimation;
        final ValueAnimator rightHideAnimation;
        final ValueAnimator leftShowAnimation;
        final ValueAnimator rightShowAnimation;
        if(Utils.isBvOS()){
            leftHideAnimation = getScaleAnimator(mBVLeftButton, 1f, 0f);
            rightHideAnimation = getScaleAnimator(mBVRightButton, 1f, 0f);
            leftShowAnimation = getScaleAnimator(mBVLeftButton, 0f, 1f);
            rightShowAnimation = getScaleAnimator(mBVRightButton, 0f, 1f);
            leftHideAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    getSelectedDeskClockFragment().onUpdateFabButtons(mBVLeftButton, mBVRightButton);
                }
            });
        }else {
            leftHideAnimation = getScaleAnimator(mLeftButton, 1f, 0f);
            rightHideAnimation = getScaleAnimator(mRightButton, 1f, 0f);
            leftShowAnimation = getScaleAnimator(mLeftButton, 0f, 1f);
            rightShowAnimation = getScaleAnimator(mRightButton, 0f, 1f);
            leftHideAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    getSelectedDeskClockFragment().onUpdateFabButtons(mLeftButton, mRightButton);
                }
            });
        }

        hideFabAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                getSelectedDeskClockFragment().onUpdateFab(mFab);
            }
        });

        // Build the reusable animations that hide and show the fab and left/right buttons.
        // These may be used independently or be chained together.
        mHideAnimation
                .setDuration(duration)
                .play(hideFabAnimation)
                .with(leftHideAnimation)
                .with(rightHideAnimation);

        mShowAnimation
                .setDuration(duration)
                .play(showFabAnimation)
                .with(leftShowAnimation)
                .with(rightShowAnimation);

        // Build the reusable animation that hides and shows only the fab.
        mUpdateFabOnlyAnimation
                .setDuration(duration)
                .play(showFabAnimation)
                .after(hideFabAnimation);

        // Build the reusable animation that hides and shows only the buttons.
        mUpdateButtonsOnlyAnimation
                .setDuration(duration)
                .play(leftShowAnimation)
                .with(rightShowAnimation)
                .after(leftHideAnimation)
                .after(rightHideAnimation);

        // Customize the view pager.
        mFragmentTabPagerAdapter = new FragmentTabPagerAdapter(this);
        mFragmentTabPager = (ViewPager) findViewById(R.id.desk_clock_pager);
        // Keep all four tabs to minimize jank.
        mFragmentTabPager.setOffscreenPageLimit(3);
        // Set Accessibility Delegate to null so view pager doesn't intercept movements and
        // prevent the fab from being selected.
        mFragmentTabPager.setAccessibilityDelegate(null);
        // Mirror changes made to the selected page of the view pager into UiDataModel.
        mFragmentTabPager.addOnPageChangeListener(new PageChangeWatcher());
        mFragmentTabPager.setAdapter(mFragmentTabPagerAdapter);

        // Mirror changes made to the selected tab into UiDataModel.
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                /*hct-fankou add DeskClock LightTheme 20191113 start*/
				//bv zhangjiachu add for alarm style 20200117 start
                if(!Utils.isBvOS()){
                    int position = tab.getPosition();
                    if(SystemProperties.getBoolean("sys.hctclock_lighttheme",false)) {
                        if (position == 0) {
                            setTabIconChange(R.drawable.hct_ic_tab_alarm_activated,
                                    R.drawable.hct_ic_tab_clock_normal,
                                    R.drawable.hct_ic_tab_timer_normal,
                                    R.drawable.hct_ic_tab_stopwatch_normal);
                        } else if (position == 1) {
                            setTabIconChange(R.drawable.hct_ic_tab_alarm_normal,
                                    R.drawable.hct_ic_tab_clock_activated,
                                    R.drawable.hct_ic_tab_timer_normal,
                                    R.drawable.hct_ic_tab_stopwatch_normal);
                        } else if (position == 2) {
                            setTabIconChange(R.drawable.hct_ic_tab_alarm_normal,
                                    R.drawable.hct_ic_tab_clock_normal,
                                    R.drawable.hct_ic_tab_timer_activated,
                                    R.drawable.hct_ic_tab_stopwatch_normal);
                        } else if (position == 3) {
                            setTabIconChange(R.drawable.hct_ic_tab_alarm_normal,
                                    R.drawable.hct_ic_tab_clock_normal,
                                    R.drawable.hct_ic_tab_timer_normal,
                                    R.drawable.hct_ic_tab_stopwatch_activated);
                        }
                    }
                }
				//bv zhangjiachu add for alarm style 20200117 end
                /*hct-fankou add DeskClock LightTheme 20191113 end*/
                UiDataModel.getUiDataModel().setSelectedTab((UiDataModel.Tab) tab.getTag());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Honor changes to the selected tab from outside entities.
        UiDataModel.getUiDataModel().addTabListener(mTabChangeWatcher);
        if(mIsBvOS){
            /*bv zhangjiachu add for alarm style 20200117 start
             * delete TabRippleColor*/
            mTabLayout.setTabRippleColor(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.transparent)));
            /*bv zhangjiachu add for alarm style 20200117 end*/
        }
    }

    /*hct-fankou add DeskClock LightTheme 20191113 start*/
	/*bv zhangjiachu add for alarm style 20200117 start*/
    public void setTabIconChange(int icon0,int icon1,int icon2,int icon3){
        mTabLayout.getTabAt(0).setIcon(icon0);
        mTabLayout.getTabAt(1).setIcon(icon1);
        mTabLayout.getTabAt(2).setIcon(icon2);
        mTabLayout.getTabAt(3).setIcon(icon3);
    }
	/*bv zhangjiachu add for alarm style 20200117 end*/
    /*hct-fankou add DeskClock LightTheme 20191113 end*/

    //hct-fangkou modify for support NightMode 20191211 start
    public boolean isNightMode() {
        return getSystemService(UiModeManager.class).getNightMode() == UiModeManager.MODE_NIGHT_YES;
    }
    //hct-fangkou modify for support NightMode 20191211 end

    @Override
    protected void onStart() {
        super.onStart();
        DataModel.getDataModel().addSilentSettingsListener(mSilentSettingChangeWatcher);
        DataModel.getDataModel().setApplicationInForeground(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final View dropShadow = findViewById(R.id.drop_shadow);
        //bv zhangjiachu add for hidden drop_shadow 20200708 start
        dropShadow.setVisibility(View.GONE);
        //bv zhangjiachu add for hidden drop_shadow 20200708 end
        mDropShadowController = new DropShadowController(dropShadow, UiDataModel.getUiDataModel(),
                mSnackbarAnchor.findViewById(R.id.tab_hairline));

        // ViewPager does not save state; this honors the selected tab in the user interface.
        updateCurrentTab();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (mRecreateActivity) {
            mRecreateActivity = false;

            // A runnable must be posted here or the new DeskClock activity will be recreated in a
            // paused state, even though it is the foreground activity.
            mFragmentTabPager.post(new Runnable() {
                @Override
                public void run() {
                    recreate();
                }
            });
        }
    }

    @Override
    public void onPause() {
        if (mDropShadowController != null) {
            mDropShadowController.stop();
            mDropShadowController = null;
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        DataModel.getDataModel().removeSilentSettingsListener(mSilentSettingChangeWatcher);
        //bv zhangjiachu add && !mSettingsChanged for fixbug:913 20200528 start
        if (!isChangingConfigurations() && !mSettingsChanged) {
            //bv zhangjiachu add && !mSettingsChanged for fixbug:913 20200528 end
            DataModel.getDataModel().setApplicationInForeground(false);
        }

        //bv zhangjiachu add for fixbug:913 20200528 start
        mSettingsChanged=false;
        //bv zhangjiachu add for fixbug:913 20200528 end
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        UiDataModel.getUiDataModel().removeTabListener(mTabChangeWatcher);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenuManager.onCreateOptionsMenu(menu);
        return true;
    }

    /*bv zhangjiachu add for Controll option menu display 20200804 start*/
    private Menu mMenu;

    public void bvPrepareMenu(){
        if (mMenu != null){
            onPrepareOptionsMenu(mMenu);
        }
    }
    /*bv zhangjiachu add for Controll option menu display 20200804 end*/

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mOptionsMenuManager.onPrepareOptionsMenu(menu);
        /*bv zhangjiachu add for Controll option menu display 20200804 start*/
        MenuItem item = menu.findItem(R.id.menu_item_settings);
        if (item != null){
            item.setVisible(!AlarmClockFragment.mIsAlarmDelete && !ClockFragment.mCityDeleteIng);
        }
        mMenu = menu;
        /*bv zhangjiachu add for Controll option menu display 20200804 end*/
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mOptionsMenuManager.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    /**
     * Called by the LabelDialogFormat class after the dialog is finished.
     */
    @Override
    public void onDialogLabelSet(Alarm alarm, String label, String tag) {
        final Fragment frag = getFragmentManager().findFragmentByTag(tag);
        if (frag instanceof AlarmClockFragment) {
            ((AlarmClockFragment) frag).setLabel(alarm, label);
        }
    }

    /**
     * Listens for keyboard activity for the tab fragments to handle if necessary. A tab may want to
     * respond to key presses even if they are not currently focused.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return getSelectedDeskClockFragment().onKeyDown(keyCode,event)
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public void updateFab(@UpdateFabFlag int updateType) {
        final DeskClockFragment f = getSelectedDeskClockFragment();

        switch (updateType & FAB_ANIMATION_MASK) {
            case FAB_SHRINK_AND_EXPAND:
                mUpdateFabOnlyAnimation.start();
                break;
            case FAB_IMMEDIATE:
                f.onUpdateFab(mFab);
                if(mIsBvOS){
                    /*bv zhangjiachu add for alarm style 20200104 start*/
                    f.onUpdateTabs(mTabLayout, mDeleteAlarmTab, mDeskTitle);
                    f.onUpdateCancel(mCancel);
                    f.onUpdateSeleteAll(mSeleteAll);
                    f.onUpdateSeleteNum(mSeleteNum);
                    f.onUpdateDelLayout(mDeleteButtomLayout);
                    /*bv zhangjiachu add for alarm style 20200104 end*/
                }
                break;
            case FAB_MORPH:
                f.onMorphFab(mFab);
                break;
        }
        switch (updateType & FAB_REQUEST_FOCUS_MASK) {
            case FAB_REQUEST_FOCUS:
                mFab.requestFocus();
                break;
        }
        switch (updateType & BUTTONS_ANIMATION_MASK) {
            case BUTTONS_IMMEDIATE:
                if(!f.isAdded()){
                    Log.d(TAG,"BUTTONS_IMMEDIATE FRAG NOT ATTACHED");
                    return;
                }
                if(mIsBvOS){
                    f.onUpdateFabButtons(mBVLeftButton, mBVRightButton);
                }else {
                    f.onUpdateFabButtons(mLeftButton, mRightButton);
                }
                break;
            case BUTTONS_SHRINK_AND_EXPAND:
                mUpdateButtonsOnlyAnimation.start();
                break;
        }
        switch (updateType & BUTTONS_DISABLE_MASK) {
            case BUTTONS_DISABLE:
                if(mIsBvOS){
                    mBVLeftButton.setClickable(false);
                    mBVRightButton.setClickable(false);
                }else {
                    mLeftButton.setClickable(false);
                    mRightButton.setClickable(false);
                }
                break;
        }
        switch (updateType & FAB_AND_BUTTONS_SHRINK_EXPAND_MASK) {
            case FAB_AND_BUTTONS_SHRINK:
                mHideAnimation.start();
                break;
            case FAB_AND_BUTTONS_EXPAND:
                mShowAnimation.start();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Recreate the activity if any settings have been changed
        if (requestCode == SettingsMenuItemController.REQUEST_CHANGE_SETTINGS
                && resultCode == RESULT_OK) {
            //hct-fangkou  modify for change setting recreate activity start 20200407
            //mRecreateActivity = true;
            if (getResources().getBoolean(R.bool.hct_setting_recreate_activity)) {
                mRecreateActivity = true;
            } else {
                //bv zhangjiachu add for fixbug:913 20200528 start
                mSettingsChanged=true;
                //bv zhangjiachu add for fixbug:913 20200528 end
                finish();
                startActivity(getIntent());
                /*bv zhangjiachu add android.R.anim.fade_in, android.R.anim.fade_out 添加动画过渡，防止闪白 20200706 start*/
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                /*bv zhangjiachu add android.R.anim.fade_in, android.R.anim.fade_out 添加动画过渡，防止闪白 20200706 start*/
            }
            //hct-fangkou  modify for change setting recreate activity end 20200407
        }
    }

    /**
     * Configure the {@link #mFragmentTabPager} and {@link #mTabLayout} to display UiDataModel's
     * selected tab.
     */
    private void updateCurrentTab() {
        // Fetch the selected tab from the source of truth: UiDataModel.
        final UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();

        // Update the selected tab in the tablayout if it does not agree with UiDataModel.
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            final TabLayout.Tab tab = mTabLayout.getTabAt(i);
            if (tab != null && tab.getTag() == selectedTab && !tab.isSelected()) {
                tab.select();
                break;
            }
        }

        // Update the selected fragment in the viewpager if it does not agree with UiDataModel.
        for (int i = 0; i < mFragmentTabPagerAdapter.getCount(); i++) {
            final DeskClockFragment fragment = mFragmentTabPagerAdapter.getDeskClockFragment(i);
            if (fragment.isTabSelected() && mFragmentTabPager.getCurrentItem() != i) {
                mFragmentTabPager.setCurrentItem(i);
                break;
            }
        }

    }

    /**
     * @return the DeskClockFragment that is currently selected according to UiDataModel
     */
    private DeskClockFragment getSelectedDeskClockFragment() {
        for (int i = 0; i < mFragmentTabPagerAdapter.getCount(); i++) {
            final DeskClockFragment fragment = mFragmentTabPagerAdapter.getDeskClockFragment(i);
            if (fragment.isTabSelected()) {
                return fragment;
            }
        }
        final UiDataModel.Tab selectedTab = UiDataModel.getUiDataModel().getSelectedTab();
        throw new IllegalStateException("Unable to locate selected fragment (" + selectedTab + ")");
    }

    /**
     * @return a Snackbar that displays the message with the given id for 5 seconds
     */
    private Snackbar createSnackbar(@StringRes int messageId) {
        return Snackbar.make(mSnackbarAnchor, messageId, 5000 /* duration */);
    }

    /**
     * As the view pager changes the selected page, update the model to record the new selected tab.
     */
    private final class PageChangeWatcher implements OnPageChangeListener {

        /** The last reported page scroll state; used to detect exotic state changes. */
        private int mPriorState = SCROLL_STATE_IDLE;

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Only hide the fab when a non-zero drag distance is detected. This prevents
            // over-scrolling from needlessly hiding the fab.

            //bv zhangjimeng
            if (mIsBvOS && mIsLowLevelPhone) {
                mFabState = FabState.HIDING;
                return;
            }

            if (mFabState == FabState.HIDE_ARMED && positionOffsetPixels != 0) {
                mFabState = FabState.HIDING;
                mHideAnimation.start();
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (mPriorState == SCROLL_STATE_IDLE && state == SCROLL_STATE_SETTLING) {
                // The user has tapped a tab button; play the hide and show animations linearly.
                //bv zhangjimeng 2020/07/04, modify deskclock perf,begin
                if (mIsBvOS && mIsLowLevelPhone) {
                    mFabState = FabState.HIDING;
                    return;
                }
                //bv zhangjimeng 2020/07/04, modify deskclock perf,end
                mHideAnimation.addListener(mAutoStartShowListener);
                mHideAnimation.start();
                mFabState = FabState.HIDING;
            } else if (mPriorState == SCROLL_STATE_SETTLING && state == SCROLL_STATE_DRAGGING) {
                //bv zhangjimeng 2020/07/04, modify deskclock perf,begin
                if (mIsBvOS && mIsLowLevelPhone) {
                    mFabState = FabState.HIDING;
                    return;
                }
                //bv zhangjimeng 2020/07/04, modify deskclock perf,end
                // The user has interrupted settling on a tab and the fab button must be re-hidden.
                if (mShowAnimation.isStarted()) {
                    mShowAnimation.cancel();
                }
                if (mHideAnimation.isStarted()) {
                    // Let the hide animation finish naturally; don't auto show when it ends.
                    mHideAnimation.removeListener(mAutoStartShowListener);
                } else {
                    // Start and immediately end the hide animation to jump to the hidden state.
                    mHideAnimation.start();
                    mHideAnimation.end();
                }
                mFabState = FabState.HIDING;

            } else if (state != SCROLL_STATE_DRAGGING && mFabState == FabState.HIDING) {
                //bv zhangjimeng 2020/07/04, modify deskclock perf,begin
                if (mIsBvOS && mIsLowLevelPhone) {
                    mFabState = FabState.SHOWING;
                    return;
                }
                //bv zhangjimeng 2020/07/04, modify deskclock perf,end
                // The user has lifted their finger; show the buttons now or after hide ends.
                if (mHideAnimation.isStarted()) {
                    // Finish the hide animation and then start the show animation.
                    mHideAnimation.addListener(mAutoStartShowListener);
                } else {
                    updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                    mShowAnimation.start();

                    // The animation to show the fab has begun; update the state to showing.
                    mFabState = FabState.SHOWING;
                }
            } else if (state == SCROLL_STATE_DRAGGING) {
                // The user has started a drag so arm the hide animation.
                mFabState = FabState.HIDE_ARMED;
            }

            // Update the last known state.
            mPriorState = state;
        }

        @Override
        public void onPageSelected(int position) {
            mFragmentTabPagerAdapter.getDeskClockFragment(position).selectTab();
            DeskClockFragment fragment = getSelectedDeskClockFragment();
            //bv zhangjimeng 2020/07/04, modify deskclock perf,begin
            if (mIsBvOS) {
                if (fragment instanceof AlarmClockFragment) {
                    ((AlarmClockFragment) fragment).exitDeleteEdit();
                }
                if (fragment instanceof ClockFragment) {
                    ((ClockFragment) fragment).exitWorldClockDeleteEdit();
                }
                if (fragment.isAdded() && mIsLowLevelPhone) {
                    fragment.onUpdateFabButtons(mBVLeftButton, mBVRightButton);
                    updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                }
                return;
            }
            //bv zhangjimeng 2020/07/04, modify deskclock perf,end
        }
    }

    /**
     * If this listener is attached to {@link #mHideAnimation} when it ends, the corresponding
     * {@link #mShowAnimation} is automatically started.
     */
    private final class AutoStartShowListener extends AnimatorListenerAdapter {
        @Override
        public void onAnimationEnd(Animator animation) {
            // Prepare the hide animation for its next use; by default do not auto-show after hide.
            mHideAnimation.removeListener(mAutoStartShowListener);

            // Update the buttons now that they are no longer visible.
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);

            // Automatically start the grow animation now that shrinking is complete.
            mShowAnimation.start();

            // The animation to show the fab has begun; update the state to showing.
            mFabState = FabState.SHOWING;
        }
    }

    /**
     * Shows/hides a snackbar as silencing settings are enabled/disabled.
     */
    private final class SilentSettingChangeWatcher implements OnSilentSettingsListener {
        @Override
        public void onSilentSettingsChange(SilentSetting before, SilentSetting after) {
            if (mShowSilentSettingSnackbarRunnable != null) {
                mSnackbarAnchor.removeCallbacks(mShowSilentSettingSnackbarRunnable);
                mShowSilentSettingSnackbarRunnable = null;
            }

            if (after == null) {
                SnackbarManager.dismiss();
            } else {
                mShowSilentSettingSnackbarRunnable = new ShowSilentSettingSnackbarRunnable(after);
                mSnackbarAnchor.postDelayed(mShowSilentSettingSnackbarRunnable, SECOND_IN_MILLIS);
            }
        }
    }

    /**
     * Displays a snackbar that indicates a system setting is currently silencing alarms.
     */
    private final class ShowSilentSettingSnackbarRunnable implements Runnable {

        private final SilentSetting mSilentSetting;

        private ShowSilentSettingSnackbarRunnable(SilentSetting silentSetting) {
            mSilentSetting = silentSetting;
        }

        public void run() {
            // Create a snackbar with a message explaining the setting that is silencing alarms.
            final Snackbar snackbar = createSnackbar(mSilentSetting.getLabelResId());

            // Set the associated corrective action if one exists.
            if (mSilentSetting.isActionEnabled(DeskClock.this)) {
                final int actionResId = mSilentSetting.getActionResId();
                snackbar.setAction(actionResId, mSilentSetting.getActionListener());
            }

            SnackbarManager.show(snackbar);
        }
    }

    /**
     * As the model reports changes to the selected tab, update the user interface.
     */
    private final class TabChangeWatcher implements TabListener {
        @Override
        public void selectedTabChanged(UiDataModel.Tab oldSelectedTab,
                UiDataModel.Tab newSelectedTab) {
            // Update the view pager and tab layout to agree with the model.
            updateCurrentTab();

            // Avoid sending events for the initial tab selection on launch and re-selecting a tab
            // after a configuration change.
            if (DataModel.getDataModel().isApplicationInForeground()) {
                switch (newSelectedTab) {
                    case ALARMS:
                        Events.sendAlarmEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case CLOCKS:
                        Events.sendClockEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case TIMERS:
                        Events.sendTimerEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case STOPWATCH:
                        Events.sendStopwatchEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                }
            }

            // If the hide animation has already completed, the buttons must be updated now when the
            // new tab is known. Otherwise they are updated at the end of the hide animation.
            if (!mHideAnimation.isStarted()) {
                updateFab(FAB_AND_BUTTONS_IMMEDIATE);
            }
        }
    }
}
