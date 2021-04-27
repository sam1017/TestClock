package com.android.deskclock.timer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.TimerTextController;
import com.android.deskclock.Utils;
import com.android.deskclock.data.Timer;

import static android.R.attr.state_activated;
import static android.R.attr.state_pressed;
import android.support.v4.view.ViewCompat;

public class BvTimerItem extends LinearLayout {

    /** Displays the remaining time or time since expiration. */
    private TextView mTimerText;

    /** Formats and displays the text in the timer. */
    private TimerTextController mTimerTextController;

    /** Displays timer progress as a color circle that changes from white to red. */
    //bv zhangjiachu modify for bv os Timer 20210309 start
    //private TimerCircleView mCircleView;
    private BvTimerCircleView mCircleView;
    //bv zhangjiachu modify for bv os Timer 20210309 end

    /** A button that either resets the timer or adds time to it, depending on its state. */
    private TextView mResetAddButton;

    /** Displays the label associated with the timer. Tapping it presents an edit dialog. */
    private TextView mLabelView;

    /** The last state of the timer that was rendered; used to avoid expensive operations. */
    private Timer.State mLastState;

    public BvTimerItem(Context context) {
        super(context);
    }

    public BvTimerItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mLabelView = (TextView) findViewById(R.id.timer_label);
        mResetAddButton = (TextView) findViewById(R.id.reset_add);
        //mCircleFrame = (TimerCircleFrame) findViewById(R.id.timer_time);
        mCircleView = (BvTimerCircleView) findViewById(R.id.timer_time);
        mTimerText = (TextView) findViewById(R.id.timer_time_text);
        mTimerTextController = new TimerTextController(mTimerText);
        final Context c = mTimerText.getContext();
        final int colorAccent = ThemeUtils.resolveColor(c, R.attr.colorAccent);
        final int textColorPrimary = ThemeUtils.resolveColor(c, android.R.attr.textColorPrimary);
        mTimerText.setTextColor(new ColorStateList(
                new int[][] { { -state_activated, -state_pressed }, {} },
                new int[] { textColorPrimary, colorAccent }));
    }

    /**
     * Updates this view to display the latest state of the {@code timer}.
     */
    void update(Timer timer) {
        // Update the time.
        mTimerTextController.setTimeString(timer.getRemainingTime());
        /*if (mCircleFrame != null){
            mCircleFrame.update(timer);
        }*/
        if (mCircleView != null){
            mCircleView.update(timer);
        }
        /*bv zhangjiachu add for fixbug:4538 start*/
        // Update the label if it changed.
        final String label = timer.getLabel();
        if (!TextUtils.equals(label, mLabelView.getText())) {
            mLabelView.setText(label);
        }
        /*bv zhangjiachu add for fixbug:4538 end*/
        // Update some potentially expensive areas of the user interface only on state changes.
        if (timer.getState() != mLastState){
            mLastState = timer.getState();
            final Context context = getContext();
            switch (mLastState){
                case RESET:
                case PAUSED:{
                    mResetAddButton.setText(R.string.timer_reset);
                    mResetAddButton.setContentDescription(null);
                    mTimerText.setClickable(true);
                    mTimerText.setActivated(false);
                    mTimerText.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
                    ViewCompat.setAccessibilityDelegate(mTimerText, new Utils.ClickAccessibilityDelegate(
                            context.getString(R.string.timer_start), true));
                    break;
                }
                case RUNNING: {
                    final String addTimeDesc = context.getString(R.string.timer_plus_one);
                    mResetAddButton.setText(R.string.timer_add_minute);
                    mResetAddButton.setContentDescription(addTimeDesc);
                    mTimerText.setClickable(true);
                    mTimerText.setActivated(false);
                    mTimerText.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
                    ViewCompat.setAccessibilityDelegate(mTimerText, new Utils.ClickAccessibilityDelegate(
                            context.getString(R.string.timer_pause)));
                    break;
                }
                case EXPIRED:
                case MISSED: {
                    final String addTimeDesc = context.getString(R.string.timer_plus_one);
                    mResetAddButton.setText(R.string.timer_add_minute);
                    mResetAddButton.setContentDescription(addTimeDesc);
                    mTimerText.setClickable(false);
                    mTimerText.setActivated(true);
                    mTimerText.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
                    break;
                }
            }
        }
    }

}
