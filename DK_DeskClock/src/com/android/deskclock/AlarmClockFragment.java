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

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.v4.graphics.ColorUtils;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Button;

import com.android.deskclock.alarms.AlarmTimeClickHandler;
import com.android.deskclock.alarms.AlarmUpdateHandler;
import com.android.deskclock.alarms.ScrollHandler;
import com.android.deskclock.alarms.TimePickerDialogFragment;
import com.android.deskclock.alarms.dataadapter.AlarmItemHolder;
import com.android.deskclock.alarms.dataadapter.CollapsedAlarmViewHolder;
import com.android.deskclock.alarms.dataadapter.ExpandedAlarmViewHolder;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.widget.EmptyViewController;
import com.android.deskclock.widget.toast.SnackbarManager;
import com.android.deskclock.widget.toast.ToastManager;
/*bv zhangjiachu add for alarm style 20191226 start*/
import com.android.deskclock.alarms.AlarmSetupActivity;

import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.util.Log;
import android.view.KeyEvent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
/*bv zhangjiachu add for alarm style 20191226 end*/

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM;
import static com.android.deskclock.uidata.UiDataModel.Tab.ALARMS;
/*hct-fankou add DeskClock LightTheme 20191113 start*/
import android.os.SystemProperties;

/**
 * A fragment that displays a list of alarm time and allows interaction with them.
 */
public final class AlarmClockFragment extends DeskClockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ScrollHandler,
        TimePickerDialogFragment.OnTimeSetListener {
    /*bv zhangjiachu add for alarm style 20191226 start*/
    private static final String TAG = "AlarmClockFragment";
	/*bv zhangjiachu add for alarm style 20191226 end*/
    // This extra is used when receiving an intent to create an alarm, but no alarm details
    // have been passed in, so the alarm page should start the process of creating a new alarm.
    public static final String ALARM_CREATE_NEW_INTENT_EXTRA = "deskclock.create.new";

    // This extra is used when receiving an intent to scroll to specific alarm. If alarm
    // can not be found, and toast message will pop up that the alarm has be deleted.
    public static final String SCROLL_TO_ALARM_INTENT_EXTRA = "deskclock.scroll.to.alarm";

    private static final String KEY_EXPANDED_ID = "expandedId";

    /*bv zhangjiachu add for alarm style 20191226 start
    *  mIsAlarmDelete = true : Delete alarm in progress
    *  mIsAlarmDelete = false : Deleted alarm has exited
    */
    public static boolean mIsAlarmDelete = false;
    /*delete alarm select/unselect all*/
    private boolean mIsSelectAll = false;
    /*delete alarm select number*/
    public static int mSelectNum = 0;
    /*bv zhangjiachu add for alarm style 20191226 end*/
    // Updates "Today/Tomorrow" in the UI when midnight passes.
    private final Runnable mMidnightUpdater = new MidnightRunnable();

    /** Draws a gradient over the bottom of the {@link #mRecyclerView} to reduce clash with the fab. */
    private GradientItemDecoration mGradientItemDecoration;

    // Views
    private ViewGroup mMainLayout;
    private RecyclerView mRecyclerView;

    // Data
    private Loader mCursorLoader;
    private long mScrollToAlarmId = Alarm.INVALID_ID;
    private long mExpandedAlarmId = Alarm.INVALID_ID;
    private long mCurrentUpdateToken;

    // Controllers
    private ItemAdapter<AlarmItemHolder> mItemAdapter;
    private AlarmUpdateHandler mAlarmUpdateHandler;
    private EmptyViewController mEmptyViewController;
    private AlarmTimeClickHandler mAlarmTimeClickHandler;
    private LinearLayoutManager mLayoutManager;
    //bv zhangjiachu add for bv os 20210302 start
    private TextClock mBvDigitalClock;
    private AnalogClock mBvAnalogClock;
    private LinearLayout mBvDigitalClockLayout;
    //bv zhangjiachu add for bv os 20210302 end
    /**
     * The public no-arg constructor required by all fragments.
     */
    public AlarmClockFragment() {
        super(ALARMS);
    }
    //bv zhangjimeng 2020/07/04, modify deskclock perf,begin
    private boolean mIsBvOS = Utils.isBvOS();
    private boolean mIsLightTheme;
    //bv zhangjimeng 2020/07/04, modify deskclock perf,end
    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Log.d(TAG,"onCreate");
        mCursorLoader = getLoaderManager().initLoader(0, null, this);
        if (savedState != null) {
            mExpandedAlarmId = savedState.getLong(KEY_EXPANDED_ID, Alarm.INVALID_ID);
        }
    }
    /*bv zhangjiachu add for alarm style 20191226 start*/
    public void initAlarmsChecked(boolean checked, boolean selected){
        ArrayList<AlarmItemHolder> alarmItemHolders = (ArrayList<AlarmItemHolder>) mItemAdapter.getItems();
        //bv zhangjiachu add if (alarmItemHolders != null) fixbug NullPointerException: Attempt to invoke virtual method 'int java.util.ArrayList.size()' on a null object reference
        if (alarmItemHolders != null) {
            for (int i = 0; i < alarmItemHolders.size(); i++) {
                alarmItemHolders.get(i).setmIsCheck(checked);
                alarmItemHolders.get(i).setmIsSeleted(selected);
            }
        }
        mItemAdapter.notifyDataSetChanged();
    }
    /*bv zhangjiachu add for alarm style 20191226 end*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(Utils.isBvOS()? R.layout.bv_alarm_clock : R.layout.alarm_clock, container, false);
        final Context context = getActivity();
        mIsLightTheme = ThemeUtils.isDarkTheme(context);
        //bv zhangjiachu add for bv os 20210302 start
        mBvDigitalClock = (TextClock) v.findViewById(R.id.bv_digital_clock);
        mBvAnalogClock = (AnalogClock) v.findViewById(R.id.bv_analog_clock);
        mBvDigitalClockLayout = (LinearLayout) v.findViewById(R.id.bv_digital_clock_layout);
        //bv zhangjiachu add for bv os 20210302 start
        mGradientItemDecoration = new GradientItemDecoration(getActivity());
        mRecyclerView = (RecyclerView) v.findViewById(R.id.alarms_recycler_view);
        mLayoutManager = new LinearLayoutManager(context) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                final int extraSpace = super.getExtraLayoutSpace(state);
                if (state.willRunPredictiveAnimations()) {
                    return Math.max(getHeight(), extraSpace);
                }
                return extraSpace;
            }
        };
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(mGradientItemDecoration);
        mMainLayout = (ViewGroup) v.findViewById(R.id.main);
        mAlarmUpdateHandler = new AlarmUpdateHandler(context, this, mMainLayout);
        final TextView emptyView = (TextView) v.findViewById(R.id.alarms_empty_view);
        final Drawable noAlarms = Utils.getVectorDrawable(context, Utils.isBvOS()? R.drawable.ic_no_alarm : R.drawable.ic_noalarms);
        emptyView.setCompoundDrawablesWithIntrinsicBounds(null, noAlarms, null, null);
        emptyView.setCompoundDrawablePadding(getResources().getDimensionPixelOffset(R.dimen.no_alarm_icon_padding));
        mEmptyViewController = new EmptyViewController(mMainLayout, mRecyclerView, emptyView);
        mAlarmTimeClickHandler = new AlarmTimeClickHandler(this, savedState, mAlarmUpdateHandler,
                this);

        //bv zhangjiachu add for bv os 20210302 start
        Utils.setBvTimeFormat(mBvDigitalClock, true);
        /*时钟点击切换*/
        bvClockClick();
        //bv zhangjiachu add for bv os 20210302 end
        mItemAdapter = new ItemAdapter<>();
        /*bv zhangjiachu add for alarm style start*/
        if(mIsBvOS){
            mItemAdapter.setOnItemLongClickedListener(new ItemAdapter.OnItemLongClickedListener(){

                @Override
                public void onItemLongClicked(ItemAdapter.ItemViewHolder<?> viewHolder, int id) {
                    initAlarmsChecked(true, false);

                    mIsAlarmDelete = true;
                    mSelectNum = 0;
                    if (mIsAlarmDelete){
                        updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                    }
                    if (getActivity() instanceof DeskClock){
                        ((DeskClock)getActivity()).bvPrepareMenu();
                    }
                    //mRecyclerView
                }

            });
        }
        /*bv zhangjiachu add for alarm style end*/
        mItemAdapter.setHasStableIds();
        if(mIsBvOS){
            mItemAdapter.withViewTypes(new CollapsedAlarmViewHolder.Factory(inflater),
                    /*hct-fankou add DeskClock LightTheme 20191113 start*/
                    /*bv zhangjiachu add for alarm style start*/
                    //null,
                    new ItemAdapter.OnItemClickedListener(){

                        @Override
                        public void onItemClicked(ItemAdapter.ItemViewHolder<?> viewHolder, int id) {
                            //isAlarmEdit(true);
                            Log.i("yangld", "id=" + id);
                        }
                    },
                    /*bv zhangjiachu add for alarm style end*/
                    (mIsLightTheme ?
                            CollapsedAlarmViewHolder.HCT_VIEW_TYPE : CollapsedAlarmViewHolder.VIEW_TYPE));
        }else {
            mItemAdapter.withViewTypes(new CollapsedAlarmViewHolder.Factory(inflater),
                    /*hct-fankou add DeskClock LightTheme 20191113 start*/
                    null,
                    (mIsLightTheme ?
                            CollapsedAlarmViewHolder.HCT_VIEW_TYPE : CollapsedAlarmViewHolder.VIEW_TYPE));
        }
        mItemAdapter.withViewTypes(new ExpandedAlarmViewHolder.Factory(context),
                /*hct-fankou add DeskClock LightTheme 20191113 start*/
                null, (mIsLightTheme?
                        ExpandedAlarmViewHolder.HCT_VIEW_TYPE : ExpandedAlarmViewHolder.VIEW_TYPE));
        mItemAdapter.setOnItemChangedListener(new ItemAdapter.OnItemChangedListener() {

            /*bv zhangjiachu add for alarm style 20200106 start*/
            @Override
            public void onItemSelectedChanged(ItemAdapter.ItemHolder<?> holder) {

                if(mIsBvOS){
                    if (mSelectNum == 0){
                        mIsSelectAll = false;
                    }

                    if (mSelectNum == alarmItemHoldersAll()){
                        mIsSelectAll = true;
                    }
                    updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                    Log.d(TAG, "onItemSelectedChanged: mSelectNum = " + mSelectNum + "  " + "alarmItemHoldersAll() = " + alarmItemHoldersAll());
                }
            }
            /*bv zhangjiachu add for alarm style 20200106 start*/

            @Override
            public void onItemChanged(ItemAdapter.ItemHolder<?> holder) {
                //Log.d(TAG, "onItemChanged: aaaa++++ ");
                if (((AlarmItemHolder) holder).isExpanded()) {
                    if (mExpandedAlarmId != holder.itemId) {
                        // Collapse the prior expanded alarm.
                        final AlarmItemHolder aih = mItemAdapter.findItemById(mExpandedAlarmId);
                        if (aih != null) {
                            aih.collapse();
                        }
                        // Record the freshly expanded alarm.
                        mExpandedAlarmId = holder.itemId;
                        final RecyclerView.ViewHolder viewHolder =
                                mRecyclerView.findViewHolderForItemId(mExpandedAlarmId);
                        if (viewHolder != null) {
                            smoothScrollTo(viewHolder.getAdapterPosition());
                        }
                    }
                } else if (mExpandedAlarmId == holder.itemId) {
                    // The expanded alarm is now collapsed so update the tracking id.
                    mExpandedAlarmId = Alarm.INVALID_ID;
                }
            }

            @Override
            public void onItemChanged(ItemAdapter.ItemHolder<?> holder, Object payload) {
                /* No additional work to do */
                //Log.d(TAG, "onItemChanged: ++++ ");
            }
        });
        final ScrollPositionWatcher scrollPositionWatcher = new ScrollPositionWatcher();
        mRecyclerView.addOnLayoutChangeListener(scrollPositionWatcher);
        mRecyclerView.addOnScrollListener(scrollPositionWatcher);
        mRecyclerView.setAdapter(mItemAdapter);
        final ItemAnimator itemAnimator = new ItemAnimator();
        itemAnimator.setChangeDuration(300L);
        itemAnimator.setMoveDuration(300L);
        mRecyclerView.setItemAnimator(itemAnimator);
        return v;
    }

    /*Author: bv zhangjiachu
    * 功能：时钟空间点击事件
    */
    private void bvClockClick() {
        mBvAnalogClock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBvAnalogClock.setVisibility(View.GONE);
                mBvDigitalClockLayout.setVisibility(View.VISIBLE);
            }
        });
        mBvDigitalClock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBvAnalogClock.setVisibility(View.VISIBLE);
                mBvDigitalClockLayout.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!isTabSelected()) {
            TimePickerDialogFragment.removeTimeEditDialog(getFragmentManager());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Schedule a runnable to update the "Today/Tomorrow" values displayed for non-repeating
        // alarms when midnight passes.
        UiDataModel.getUiDataModel().addMidnightCallback(mMidnightUpdater, 100);

        // Check if another app asked us to create a blank new alarm.
        final Intent intent = getActivity().getIntent();
        if (intent == null) {
            return;
        }

        if (intent.hasExtra(ALARM_CREATE_NEW_INTENT_EXTRA)) {
            UiDataModel.getUiDataModel().setSelectedTab(ALARMS);
            if (intent.getBooleanExtra(ALARM_CREATE_NEW_INTENT_EXTRA, false)) {
                // An external app asked us to create a blank alarm.
                startCreatingAlarm();
            }

            // Remove the CREATE_NEW extra now that we've processed it.
            intent.removeExtra(ALARM_CREATE_NEW_INTENT_EXTRA);
        } else if (intent.hasExtra(SCROLL_TO_ALARM_INTENT_EXTRA)) {
            UiDataModel.getUiDataModel().setSelectedTab(ALARMS);

            long alarmId = intent.getLongExtra(SCROLL_TO_ALARM_INTENT_EXTRA, Alarm.INVALID_ID);
            if (alarmId != Alarm.INVALID_ID) {
                setSmoothScrollStableId(alarmId);
                if (mCursorLoader != null && mCursorLoader.isStarted()) {
                    // We need to force a reload here to make sure we have the latest view
                    // of the data to scroll to.
                    mCursorLoader.forceLoad();
                }
            }

            // Remove the SCROLL_TO_ALARM extra now that we've processed it.
            intent.removeExtra(SCROLL_TO_ALARM_INTENT_EXTRA);
        }
    }

    /*bv zhangjiachu add for alarm style 20191228 start*/
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final Context context= getContext();
        // By default return false so event continues to propagate
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mIsBvOS){
            if (mIsAlarmDelete){
                initAlarmsChecked(false, false);
                mIsAlarmDelete = false;
                mSelectNum = 0;
                mIsSelectAll = false;
                if (!mIsAlarmDelete) {
                    Log.d(TAG, "onKeyDown: mIsAlarmDelete = " + mIsAlarmDelete);
                    updateFab(FAB_AND_BUTTONS_IMMEDIATE);
                }
                if (getActivity() instanceof DeskClock){
                    ((DeskClock)getActivity()).bvPrepareMenu();
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    /*bv zhangjiachu add for alarm style 20191228 end*/

    /*bv zhangjiachu add for alarm style 20200110 start*/
    public void exitDeleteEdit() {
        if (mIsAlarmDelete || mIsSelectAll || mSelectNum > 0) {
            initAlarmsChecked(false, false);
        }
        mIsAlarmDelete = false;
        if (getActivity() instanceof DeskClock){
            ((DeskClock)getActivity()).bvPrepareMenu();
        }
        mSelectNum = 0;
        mIsSelectAll = false;
    }
    /*bv zhangjiachu add for alarm style 20200110 end*/

    @Override
    public void onPause() {
        super.onPause();
        UiDataModel.getUiDataModel().removePeriodicCallback(mMidnightUpdater);

        // When the user places the app in the background by pressing "home",
        // dismiss the toast bar. However, since there is no way to determine if
        // home was pressed, just dismiss any existing toast bar when restarting
        // the app.
        mAlarmUpdateHandler.hideUndoBar();
    }

    @Override
    public void smoothScrollTo(int position) {
        mLayoutManager.scrollToPositionWithOffset(position, 0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAlarmTimeClickHandler.saveInstance(outState);
        outState.putLong(KEY_EXPANDED_ID, mExpandedAlarmId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToastManager.cancelToast();
    }

    public void setLabel(Alarm alarm, String label) {
        alarm.label = label;
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return Alarm.getAlarmsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
        final List<AlarmItemHolder> itemHolders = new ArrayList<>(data.getCount());
        for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
            final Alarm alarm = new Alarm(data);
            final AlarmInstance alarmInstance = alarm.canPreemptivelyDismiss()
                    ? new AlarmInstance(data, true /* joinedTable */) : null;
            final AlarmItemHolder itemHolder =
                    new AlarmItemHolder(alarm, alarmInstance, mAlarmTimeClickHandler);
            itemHolders.add(itemHolder);
        }
        setAdapterItems(itemHolders, SystemClock.elapsedRealtime());
    }

    /**
     * Updates the adapters items, deferring the update until the current animation is finished or
     * if no animation is running then the listener will be automatically be invoked immediately.
     *
     * @param items       the new list of {@link AlarmItemHolder} to use
     * @param updateToken a monotonically increasing value used to preserve ordering of deferred
     *                    updates
     */
    private void setAdapterItems(final List<AlarmItemHolder> items, final long updateToken) {
        if (updateToken < mCurrentUpdateToken) {
            LogUtils.v("Ignoring adapter update: %d < %d", updateToken, mCurrentUpdateToken);
            return;
        }

        if (mRecyclerView.getItemAnimator().isRunning()) {
            // RecyclerView is currently animating -> defer update.
            mRecyclerView.getItemAnimator().isRunning(
                    new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                @Override
                public void onAnimationsFinished() {
                    setAdapterItems(items, updateToken);
                }
            });
        } else if (mRecyclerView.isComputingLayout()) {
            // RecyclerView is currently computing a layout -> defer update.
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    setAdapterItems(items, updateToken);
                }
            });
        } else {
            mCurrentUpdateToken = updateToken;
            mItemAdapter.setItems(items);

            // Show or hide the empty view as appropriate.
            final boolean noAlarms = items.isEmpty();
            mEmptyViewController.setEmpty(noAlarms);
            if (noAlarms) {
                // Ensure the drop shadow is hidden when no alarms exist.
                setTabScrolledToTop(true);
            }

            // Expand the correct alarm.
            if (mExpandedAlarmId != Alarm.INVALID_ID) {
                final AlarmItemHolder aih = mItemAdapter.findItemById(mExpandedAlarmId);
                if (aih != null) {
                    mAlarmTimeClickHandler.setSelectedAlarm(aih.item);
                    if(Utils.isBvOS()){
                        /*bv zhangjiachu modify for alarm style 20200227 start*/
                        aih.collapse();
                        /*bv zhangjiachu modify for alarm style 20200227 end*/
                    }else {
                        aih.expand();
                    }
                } else {
                    mAlarmTimeClickHandler.setSelectedAlarm(null);
                    mExpandedAlarmId = Alarm.INVALID_ID;
                }
            }

            // Scroll to the selected alarm.
            if (mScrollToAlarmId != Alarm.INVALID_ID) {
                scrollToAlarm(mScrollToAlarmId);
                setSmoothScrollStableId(Alarm.INVALID_ID);
            }
        }
    }

    /**
     * @param alarmId identifies the alarm to be displayed
     */
    private void scrollToAlarm(long alarmId) {
        final int alarmCount = mItemAdapter.getItemCount();
        int alarmPosition = -1;
        for (int i = 0; i < alarmCount; i++) {
            long id = mItemAdapter.getItemId(i);
            if (id == alarmId) {
                alarmPosition = i;
                break;
            }
        }

        if (alarmPosition >= 0) {
            if(Utils.isBvOS()){
                /*bv zhangajichu modify for alarm style 20200227 start*/
                mItemAdapter.findItemById(alarmId).collapse();
                /*bv zhangajichu modify for alarm style 20200227 end*/
            }else {
                mItemAdapter.findItemById(alarmId).expand();
            }
            smoothScrollTo(alarmPosition);
        } else {
            // Trying to display a deleted alarm should only happen from a missed notification for
            // an alarm that has been marked deleted after use.
            SnackbarManager.show(Snackbar.make(mMainLayout, R.string
                    .missed_alarm_has_been_deleted, Snackbar.LENGTH_LONG));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    @Override
    public void setSmoothScrollStableId(long stableId) {
        mScrollToAlarmId = stableId;
    }
    /*bv zhangjiachu modify for alarm style start */

    public void selectAllAlarms(boolean selected){
        ArrayList<AlarmItemHolder> alarmItemHolders = (ArrayList<AlarmItemHolder>) mItemAdapter.getItems();
        for(int i = 0; i < alarmItemHolders.size(); i++) {
            alarmItemHolders.get(i).setmIsSeleted(selected);
        }
        mItemAdapter.notifyDataSetChanged();
    }
   /*bv zhangjiachu modify for alarm style end */
   
    @Override
    public void onFabClick(@NonNull ImageView fab) {
        mAlarmUpdateHandler.hideUndoBar();
        startCreatingAlarm();
    }
	/*bv zhangjiachu modify for alarm style start */
    @Override
    public void onUpdateDelClick(@NonNull TextView delete_buttom_layout){
        if (mSelectNum != 0){
            deleteAlarms();
            //delete alarms and the select number return 0
            mSelectNum = 0;
            mIsAlarmDelete = false;
            if (getActivity() instanceof DeskClock){
                ((DeskClock)getActivity()).bvPrepareMenu();
            }
        }
        //delete alarms and exit alarm delete edit
        if (!mIsAlarmDelete){
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);
        }
    }

    @Override
    public void onUpdateCancel(@NonNull TextView cancel_delete) {

    }

    @Override
    public void onUpdateCancelClick(@NonNull TextView cancel_delete) {
        mIsAlarmDelete = false;
        if (getActivity() instanceof DeskClock){
            ((DeskClock)getActivity()).bvPrepareMenu();
        }
        if (!mIsAlarmDelete) {
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);
        }
        mSelectNum = 0;
        initAlarmsChecked(false, false);
    }

    @Override
    public void onUpdateSeleteAll(@NonNull TextView select_all) {
        if (!mIsSelectAll) {
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
        if (!mIsSelectAll) {
            selectAllAlarms(true);
            selectAlarmNum();
            mIsSelectAll = true;
        } else {
            selectAllAlarms(false);
            mSelectNum = 0;
            mIsSelectAll = false;
        }
        updateFab(FAB_AND_BUTTONS_IMMEDIATE);
    }

    @Override
    public void onUpdateSeleteNum(@NonNull TextView select_num) {
        /// bv zhangjiachu modify [Fragment not attached to activity] Check whether fragment
        /// is attached to activity or not by isAdded() method before accessing app's resources @{
        if (isAdded()) {
            final Resources resources = getResources();
            select_num.setText(resources.getString(R.string.delete_alarm_num, mSelectNum));
        }
    }

    public int alarmItemHoldersAll(){
        int num = 0;
        ArrayList<AlarmItemHolder> alarmItemHolders = (ArrayList<AlarmItemHolder>) mItemAdapter.getItems();
        num =alarmItemHolders.size();
        return num;
    }

    public void selectAlarmNum(){
        //init mSelectNum = 0
        mSelectNum = 0;
        ArrayList<AlarmItemHolder> alarmItemHolders = (ArrayList<AlarmItemHolder>) mItemAdapter.getItems();
        for(int i = 0; i < alarmItemHolders.size(); i++){
            if(alarmItemHolders.get(i).ismSeleted()){
                mSelectNum++;
            }
        }
    }

    public void deleteAlarms(){
        Alarm alarm=null;
        ArrayList<AlarmItemHolder> alarmItemHolders = (ArrayList<AlarmItemHolder>) mItemAdapter.getItems();

        Iterator<AlarmItemHolder> iter = alarmItemHolders.iterator();
        while(iter.hasNext()) {
            AlarmItemHolder itemHolder = iter.next();
            if(itemHolder.ismSeleted()){
                //delete alarm
                alarm= itemHolder.item;
                if(alarm!=null){
                    mAlarmUpdateHandler.asyncDeleteAlarm(alarm,true);
                }
                iter.remove();
            }
        }
        //bv zhangjiachu add for noAlarms display 20200722 start
        // Show or hide the empty view as appropriate.
        final boolean noAlarms = alarmItemHolders.isEmpty();
        mEmptyViewController.setEmpty(noAlarms);
        //bv zhangjiachu add for noAlarms display 20200722 end
        mItemAdapter.notifyDataSetChanged();
    }
    /*bv zhangjiachu modify for alarm style end */

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        /*bv zhangjiachu modify for alarm style start */
        if(mIsBvOS){
            if (mIsAlarmDelete){
                fab.setVisibility(View.GONE);
            /*fab.setImageResource(R.drawable.ic_delete);
            fab.setContentDescription(fab.getResources().getString(R.string.delete));*/
            }else {
                fab.setVisibility(View.VISIBLE);
            }
            /*bv zhangjiachu modify for alarm style start*/
            fab.setImageResource(R.drawable.ic_bv_add);
        }else {
            fab.setVisibility(View.VISIBLE);
            fab.setImageResource(R.drawable.ic_add_white_24dp);
        }
        /*bv zhangjiachu modify for alarm style end*/
        fab.setContentDescription(fab.getResources().getString(R.string.button_alarms));
        fab.setClickable(true);
        fab.setAlpha(1.0f);
        /*bv zhangjiachu modify for alarm style end */
    }

    /*bv zhangjiachu modify for alarm style 20200103 start*/
    @Override
    public void onUpdateTabs(@NonNull TabLayout tabs, @NonNull LinearLayout delete_alarm_tab, @NonNull TextView deskTitle){
        deskTitle.setText(R.string.menu_alarm);
        if (mIsAlarmDelete) {
            //bv zhangjiachu modify for bv os 20210301
            //tabs.setVisibility(View.INVISIBLE);
            delete_alarm_tab.setVisibility(View.VISIBLE);
            deskTitle.setVisibility(View.GONE);
        } else {
            //bv zhangjiachu modify for bv os 20210301
            //tabs.setVisibility(View.VISIBLE);
            delete_alarm_tab.setVisibility(View.GONE);
            deskTitle.setVisibility(View.VISIBLE);
        }
    }

    public void onUpdateDelLayout(@NonNull LinearLayout delete_buttom_layout){
        if (mIsAlarmDelete) {
            delete_buttom_layout.setVisibility(View.VISIBLE);

        } else {
            delete_buttom_layout.setVisibility(View.GONE);
        }
    }
    /*bv zhangjiachu modify for alarm style 20200103 end*/

    @Override
    /*bv zhangjiachu modify for alarm style 20200103 start*/
    //public void onUpdateFabButtons(@NonNull Button left, @NonNull Button right) {
    public void onUpdateFabButtons(@NonNull ImageButton left, @NonNull ImageButton right) {
        /*bv zhangjiachu modify for alarm style 20200103 end*/
        if (mIsAlarmDelete){
            left.setVisibility(View.GONE);
            right.setVisibility(View.GONE);
        } else {
            left.setVisibility(View.INVISIBLE);
            right.setVisibility(View.INVISIBLE);
        }
    }

    public void onUpdateFabButtons(@NonNull Button left, @NonNull Button right) {
        left.setVisibility(View.INVISIBLE);
        right.setVisibility(View.INVISIBLE);
    }

    private void startCreatingAlarm() {
        // Clear the currently selected alarm.
        mAlarmTimeClickHandler.setSelectedAlarm(null);
        /*bv zhangjiachu modify for alarm style start*/
        if(mIsBvOS){
            AlarmSetupActivity.bvSetSelectedAlarm(null);
            Intent intent = new Intent(getContext(), com.android.deskclock.alarms.AlarmSetupActivity.class);
            getContext().startActivity(intent);
        }else {
            TimePickerDialogFragment.show(this);
        }
        /*bv zhangjiachu modify for alarm style end*/
    }

    @Override
    public void onTimeSet(TimePickerDialogFragment fragment, int hourOfDay, int minute) {
        mAlarmTimeClickHandler.onTimeSet(hourOfDay, minute);
    }

    public void removeItem(AlarmItemHolder itemHolder) {
        mItemAdapter.removeItem(itemHolder);
    }

    /**
     * Updates the vertical scroll state of this tab in the {@link UiDataModel} as the user scrolls
     * the recyclerview or when the size/position of elements within the recyclerview changes.
     */
    private final class ScrollPositionWatcher extends RecyclerView.OnScrollListener
            implements View.OnLayoutChangeListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            setTabScrolledToTop(Utils.isScrolledToTop(mRecyclerView));
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                int oldLeft, int oldTop, int oldRight, int oldBottom) {
            setTabScrolledToTop(Utils.isScrolledToTop(mRecyclerView));
        }
    }

    /**
     * This runnable executes at midnight and refreshes the display of all alarms. Collapsed alarms
     * that do no repeat will have their "Tomorrow" strings updated to say "Today".
     */
    private final class MidnightRunnable implements Runnable {
        @Override
        public void run() {
            mItemAdapter.notifyDataSetChanged();
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
                0x4D, // 30%getHeight
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
}
