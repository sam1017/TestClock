package com.android.deskclock.alarms;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.app.UiModeManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.view.LayoutInflater;
import java.util.GregorianCalendar;
import java.util.List;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.DeskClock;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.uidata.UiDataModel;
import android.graphics.drawable.Drawable;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.events.Events;
import com.android.deskclock.ringtone.RingtonePickerActivity;
import android.graphics.Paint;
import android.util.Log;
import java.util.Calendar;
import android.text.format.DateFormat;
import android.os.SystemProperties;

public class AlarmSetupActivity extends AppCompatActivity implements RingtonePickerActivity.AlarmUpdateCallBack ,NumberPickerView.OnValueChangeListener{

    private final static String TAG = "AlarmSetupActivity";

    //private CheckBox repeat;
    private TextView addAlarmTitle;
    private TextView mEditLabel;
    private LinearLayout mRepeatDays;
    // bv zhangjiachu add for debug bug id : 250 2020-03-03 start
    private LinearLayout mEditLabelLayout;
    // bv zhangjiachu add for debug bug id : 250 2020-03-03 end
    private CompoundButton[] mDayButtons = new CompoundButton[7];
    //private CheckBox vibrate;
    private TextView mRingtone;
    private TextView mSaveTextButton;
    private TextView mCancelTextButton;
    /*new timepicker start*/
    private NumberPickerView mPickerViewH;
    private NumberPickerView mPickerView24H;
    private NumberPickerView mPickerViewM;
    private Space mSpace24Hour;
    /*24 hour format minite*/
    private NumberPickerView mPickerViewM24H;
    private NumberPickerView mPickerViewD;
    /*new timepicker end*/
    private CompoundButton mVibrateButton;
    //public ViewGroup mMainLayout;

    /*mCurrentAlarm 当前闹钟*/
    private Alarm mCurrentAlarm;
    private AlarmUpdateHandler mAlarmUpdateHandler;

    // Controllers
    /*mSelectedAlarm: 选中编辑闹钟*/
    private static Alarm mSelectedAlarm;
    private Alarm mTempalarm;
    //bv zhangjiachu add：点击“返回”恢复初始铃声 20210203 start
    private Alarm mInitAlarm = new Alarm();
    //bv zhangjiachu add：点击“返回”恢复初始铃声 20210203 end

    private Context mContext;
    private boolean mIs24Hour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*init theme*/
        initTheme();
        setContentView(R.layout.activity_alarm_setup);

        mContext = getApplicationContext();
        mAlarmUpdateHandler = new AlarmUpdateHandler(mContext, null, null);
        mIs24Hour = DateFormat.is24HourFormat(mContext);

        addAlarmTitle = (TextView)findViewById(R.id.add_alarm_title);
        mSaveTextButton = (TextView)findViewById(R.id.ok_save);
        mCancelTextButton = (TextView)findViewById(R.id.cancel_save);
        mRingtone = (TextView)findViewById(R.id.choose_ringtone);
        mRepeatDays = (LinearLayout)findViewById(R.id.repeat_days);
        mEditLabel = (TextView)findViewById(R.id.edit_label);
        // bv zhangjiachu add for debug bug id : 250 2020-03-03 start
        mEditLabelLayout = (LinearLayout)findViewById(R.id.edit_label_layout);
        // bv zhangjiachu add for debug bug id : 250 2020-03-03 end
        mVibrateButton = (CompoundButton)findViewById(R.id.vibrate_onoff);

        mPickerViewH = findViewById(R.id.picker_hour);
        mPickerView24H = findViewById(R.id.picker_24hour);
        mPickerViewM = findViewById(R.id.picker_minute);
        mPickerViewM24H = findViewById(R.id.picker_minute_24hour);
        mPickerViewD = findViewById(R.id.picker_half_day);
        mSpace24Hour = findViewById(R.id.space_24hour);
        mPickerView24H.setOnValueChangedListener(this);
        mPickerViewH.setOnValueChangedListener(this);
        mPickerViewM.setOnValueChangedListener(this);
        mPickerViewM24H.setOnValueChangedListener(this);
        mPickerViewD.setOnValueChangedListener(this);
        //初始化当前时钟
        initCurrentAlarm();
        //设置字体
        //setTypeface();
        //时间选择器
        initTimePicker();
        //星期选择
        initWeekdays();
        // Edit label handler
        mEditLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEditLabelClicked(mCurrentAlarm);
            }
        });
        // bv zhangjiachu add for debug bug id : 250 2020-03-03 start
        mEditLabelLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onEditLabelClicked(mCurrentAlarm);
            }
        });
        // bv zhangjiachu add for debug bug id : 250 2020-03-03 end
        mVibrateButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                setAlarmVibrationEnabled(mCurrentAlarm, checked);
            }
        });
        //点击Cancle跳转到deskclock
        mCancelTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //bv zhangjiachu add：点击“返回”恢复初始铃声 20210203 start
                mCurrentAlarm.alert = mInitAlarm.alert;
                mAlarmUpdateHandler.BvAsyncUpdateAlarm(mCurrentAlarm, false, true);
                //bv zhangjiachu add：点击“返回”恢复初始铃声 20210203 end
                Intent intent = new Intent(AlarmSetupActivity.this, DeskClock.class);
                startActivity(intent);
            }
        });
        //set_ringtone
        mRingtone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Context context= view.getContext();
                onRingtoneClicked(context, mCurrentAlarm);
            }
        });
        //更新闹钟
        updateAlarm();
    }

    private void updateAlarm(){
        if (mSelectedAlarm != null) {
            mSaveTextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Context context = v.getContext();
                    if (mIs24Hour) {
                        mCurrentAlarm.hour = mPickerView24H.getValue();
                        mCurrentAlarm.minutes = mPickerViewM24H.getValue();
                    } else {
                        if (mPickerViewD.getValue() == 1) {
                            mCurrentAlarm.hour = mPickerViewH.getValue() + 12;
                        } else {
                            mCurrentAlarm.hour = mPickerViewH.getValue();
                        }
                        mCurrentAlarm.minutes = mPickerViewM.getValue();
                    }
                    // Start a second background task to persist the updated alarm.
                    mCurrentAlarm.enabled = true;
                    //bv zhangjimeng 2020/07/20,fixbug: here need contentprovider notify
                    mAlarmUpdateHandler.BvAsyncUpdateAlarm(mCurrentAlarm, mCurrentAlarm.enabled, true);
                    //bv zhangjimeng fix end
                    Intent intent = new Intent(AlarmSetupActivity.this, DeskClock.class);
                    startActivity(intent);
                }
            });
        } else {
            mSaveTextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Context context = v.getContext();
                    if (mIs24Hour) {
                        mCurrentAlarm.hour = mPickerView24H.getValue();
                        mCurrentAlarm.minutes = mPickerViewM24H.getValue();
                    } else {
                        if (mPickerViewD.getValue() == 1) {
                            mCurrentAlarm.hour = mPickerViewH.getValue() + 12;
                        } else {
                            mCurrentAlarm.hour = mPickerViewH.getValue();
                        }
                        mCurrentAlarm.minutes = mPickerViewM.getValue();
                    }
                    // Start a second background task to persist the updated alarm.
                    mCurrentAlarm.enabled = true;
                    mAlarmUpdateHandler.bvasyncAddAlarm(mCurrentAlarm);
                    Intent intent = new Intent(AlarmSetupActivity.this, DeskClock.class);
                    startActivity(intent);
                }
            });
        }
    }

    private void initWeekdays() {
        // Build button for each day.
        final LayoutInflater inflater = LayoutInflater.from(this);
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < 7; i++) {
            final View dayButtonFrame = inflater.inflate(R.layout.bv_day_button, mRepeatDays,
                    false /* attachToRoot */);
            final CompoundButton dayButton =
                    (CompoundButton) dayButtonFrame.findViewById(R.id.bv_day_button_box);
            final int weekday = weekdays.get(i);
            //hct-fangkou modify lighttheme weekdaytext color start 20191127
            //dayButton.setText(UiDataModel.getUiDataModel().getShortWeekday(weekday));
            Paint mPaint=new Paint();
            dayButton.setButtonDrawable(new Drawable() {
                @Override
                public void draw(Canvas canvas) {
                    if(Utils.isBvOS()){
                        if(dayButton.isChecked()) {
                            mPaint.setColor(Color.WHITE);
                        } else {
                            //mPaint.setColor(Color.BLACK);
                            mPaint.setColor(getResources().getColor(R.color.bv_second_text_color));
                        }
                    }else{
                        mPaint.setColor(Color.WHITE);
                    }
                    mPaint.setTextSize(dayButton.getHeight()*1/2);
                    mPaint.setStyle(Paint.Style.FILL);
                    mPaint.setAntiAlias(true);
                    mPaint.setTextAlign(Paint.Align.CENTER);
                    Paint.FontMetrics fontMetrics=mPaint.getFontMetrics();
                    float top=fontMetrics.top;
                    float bottom=fontMetrics.bottom;
                    int baseLineY=(int)((bottom-top)/2);
                    canvas.drawText(UiDataModel.getUiDataModel().getShortWeekday(weekday),
                            dayButtonFrame.getHeight()/2,dayButtonFrame.getHeight()/2+baseLineY/2,mPaint);
                }

                @Override
                public void setAlpha(int i) {

                }

                @Override
                public void setColorFilter(ColorFilter colorFilter) {

                }

                @Override
                public int getOpacity() {
                    return 0;
                }
            });

            dayButton.setBackground(mContext.getDrawable(R.drawable.bv_toggle_circle));
            dayButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(weekday));
            mRepeatDays.addView(dayButtonFrame);
            mDayButtons[i] = dayButton;
        }
        // Day buttons handler
        for (int i = 0; i < mDayButtons.length; i++) {
            final int buttonIndex = i;
            mDayButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean isChecked = ((CompoundButton) view).isChecked();
                    setDayOfWeekEnabled(mCurrentAlarm,
                            isChecked, buttonIndex);
                }
            });
        }
    }

    private void initTimePicker() {

        /*initTime start*/
        if (mSelectedAlarm != null){
            addAlarmTitle.setText(R.string.edit_alarms);
            int h = mCurrentAlarm.hour;
            int m = mCurrentAlarm.minutes;
            int d = h < 12 ? 0 : 1;
            if (mIs24Hour){
                setData(mPickerView24H, 0, 23, h);
                setData(mPickerViewM24H, 0, 59, m);
            }else{
                Log.d(TAG, "initTime: h = " + h);
                h = h % 12;
                setData(mPickerViewH, 0, 11, h);
                Log.d(TAG, "initTime: h2 = " + h);
                setData(mPickerViewM, 0, 59, m);
            }
            setData(mPickerViewD, 0, 1, d);
        } else {
            addAlarmTitle.setText(R.string.button_alarms);
            GregorianCalendar calendar = (GregorianCalendar) GregorianCalendar.getInstance();
            int h = calendar.get(Calendar.HOUR_OF_DAY);
            int m = calendar.get(Calendar.MINUTE);
            int d = h < 12 ? 0 : 1;
            if (mIs24Hour){
                setData(mPickerView24H, 0, 23, h);
                setData(mPickerViewM24H, 0, 59, m);
            }else{
                h = h % 12;
                setData(mPickerViewH, 0, 11, h);
                setData(mPickerViewM, 0, 59, m);
            }
            setData(mPickerViewD, 0, 1, d);
        }
        /*initTime end*/

        /*setVisibility */
        if (mIs24Hour){
            mPickerViewD.setVisibility(View.GONE);
            mPickerViewH.setVisibility(View.GONE);
            mPickerViewM.setVisibility(View.GONE);
            mPickerView24H.setVisibility(View.VISIBLE);
            mPickerViewM24H.setVisibility(View.VISIBLE);
            mSpace24Hour.setVisibility(View.VISIBLE);
        }else{
            mPickerViewD.setVisibility(View.VISIBLE);
            mPickerViewH.setVisibility(View.VISIBLE);
            mPickerViewM.setVisibility(View.VISIBLE);
            mPickerView24H.setVisibility(View.GONE);
            mPickerViewM24H.setVisibility(View.GONE);
            mSpace24Hour.setVisibility(View.GONE);
        }
    }

    private void initTheme(){
        if (ThemeUtils.isDarkTheme(this)){
            setTheme(R.style.ThemeDarkDeskClock);
        } else {
            setTheme(R.style.HctThemeDeskClock);
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(
                    getString(R.string.hct_default_actionbar_background)));
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            window.setNavigationBarColor(Color.parseColor(
                    getString(R.string.hct_default_actionbar_background)));
        }
        //bv zhangjiachu add for alarm style 20200310 end
    }

    private void initCurrentAlarm(){
        if (mSelectedAlarm != null) {
            //用Intent传当前选中闹钟的值
            Intent intent = getIntent();
            mCurrentAlarm = intent.getParcelableExtra("alarm");
            //bv zhangjiachu add for modify monkey mCurrentAlarm = null error start
            if (mCurrentAlarm == null){
                mCurrentAlarm = mSelectedAlarm;
            }
            //bv zhangjiachu add for modify monkey mCurrentAlarm = null error end
        } else {
            mCurrentAlarm = new Alarm();
            //初始化铃声
            mCurrentAlarm.alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }
        //bv zhangjiachu add：点击“返回”恢复初始铃声 20210203 start
        mInitAlarm.alert = mCurrentAlarm.alert;
        //bv zhangjiachu add：点击“返回”恢复初始铃声 20210203 end
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindRingtone(mContext, mCurrentAlarm);
        bindDaysOfWeekButtons(mCurrentAlarm, mContext);
        bindEditLabel(mContext, mCurrentAlarm);
        bindOnOffSwitch(mCurrentAlarm);
    }

    private void setData(NumberPickerView picker, int minValue, int maxValue, int value) {
        picker.setMinValue(minValue);
        picker.setMaxValue(maxValue);
        picker.setValue(value);
    }

    public static void bvSetSelectedAlarm(Alarm selectedAlarm) {
        mSelectedAlarm = selectedAlarm;
    }

    protected void bindOnOffSwitch(Alarm alarm) {
        if (mVibrateButton.isChecked() != alarm.vibrate) {
            mVibrateButton.setChecked(alarm.vibrate);
        }
    }

    public void setAlarmVibrationEnabled(Alarm alarm, boolean newState) {
        if (newState != alarm.vibrate) {
            alarm.vibrate = newState;
            Events.sendAlarmEvent(R.string.action_toggle_vibrate, R.string.label_deskclock);

            if (newState) {
                // Buzz the vibrator to preview the alarm firing behavior.
                final Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                if (v.hasVibrator()) {
                    v.vibrate(300);
                }
            }
        }
    }

    public void onEditLabelClicked(final Alarm alarm) {
        Events.sendAlarmEvent(R.string.action_set_label, R.string.label_deskclock);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater builderInflater = LayoutInflater.from(mContext);
        View editTextLayout = builderInflater.inflate(R.layout.edit_text_layout,
                null);
        final EditText editText = (EditText) editTextLayout
                .findViewById(R.id.edit_text);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.requestFocus();
        editText.selectAll();
        if (!alarm.label.isEmpty()){
            editText.setText(alarm.label);
        }

        // Add the buttons
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                //按下确定键后的事件
                alarm.label= editText.getText().toString();
                bindEditLabel(mContext, mCurrentAlarm);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Set other dialog properties
        // 自定义 title样式
        builder.setTitle(R.string.label);
        builder.setView(editTextLayout);
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onRingtoneClicked(Context context, Alarm alarm) {

        Events.sendAlarmEvent(R.string.action_set_ringtone, R.string.label_deskclock);
        Log.d(TAG, "onRingtoneClicked: alarm = " + alarm + "mSelectedAlarm:" + mSelectedAlarm);
        final Intent intent =
                RingtonePickerActivity.createAlarmRingtonePickerIntent(context, alarm);
        if (mSelectedAlarm == null) {
            RingtonePickerActivity.setHandler(handler);
        } else {
            //编辑闹钟时编辑铃声才使用此回调 解决monkey测试报错
            RingtonePickerActivity.setCallBack(intent, this, context);
        }
        context.startActivity(intent);
    }

    /*
    * author: bv zhangjiachu
    * Features：新建时钟选择闹钟关联
    */
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AlarmUtils.MSG_ADD_ALARM_ALTER: {
                    mTempalarm = (Alarm) msg.obj;
                    mCurrentAlarm.alert = mTempalarm.alert;
                    //bv zhangjiachu delete:解决跑monkey保错 20210203 start
                    //mCurrentAlarm.id = -1;
                    //bv zhangjiachu delete:解决跑monkey保错 20210203 start
                    break;
                }
            }
        }
    };

    private void bindRingtone(Context context, Alarm alarm) {
        final String title = DataModel.getDataModel().getRingtoneTitle(alarm.alert);
        mRingtone.setText(title);

        final String description = context.getString(R.string.ringtone_description);
        mRingtone.setContentDescription(description + " " + title);

        final Drawable icon = getResources().getDrawable(R.drawable.bv_ic_ringtone);
        mRingtone.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null);
    }

    private void bindEditLabel(Context context, Alarm alarm) {
        mEditLabel.setText(alarm.label);
        mEditLabel.setContentDescription(alarm.label != null && alarm.label.length() > 0
                ? context.getString(R.string.label_description) + " " + alarm.label
                : context.getString(R.string.no_label_specified));
        final Drawable icon = getResources().getDrawable(R.drawable.bv_ic_ringtone);
        mEditLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null);
    }

    private void setDayOfWeekEnabled(Alarm alarm, boolean isChecked, int buttonIndex) {
        final Calendar now = Calendar.getInstance();
        final Calendar oldNextAlarmTime = alarm.getNextAlarmTime(now);

        final int weekday = DataModel.getDataModel().getWeekdayOrder().getCalendarDays().get(buttonIndex);
        alarm.daysOfWeek = alarm.daysOfWeek.setBit(weekday, isChecked);

        final Calendar newNextAlarmTime = alarm.getNextAlarmTime(now);
        final boolean popupToast = !oldNextAlarmTime.equals(newNextAlarmTime);
    }

    private void bindDaysOfWeekButtons(Alarm currentAlarm, Context context) {
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < weekdays.size(); i++) {
            final CompoundButton dayButton = mDayButtons[i];
            if (currentAlarm.daysOfWeek.isBitOn(weekdays.get(i))) {
                dayButton.setChecked(true);
                dayButton.setTextColor(ThemeUtils.resolveColor(context,
                        android.R.attr.windowBackground));
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(Color.WHITE);
            }
        }
    }

    @Override
    public void upDateRing(boolean status) {
        final ContentResolver cr = mContext.getContentResolver();
        if (status) {
            Alarm alarm = Alarm.getAlarm(cr, mCurrentAlarm.id);
            mCurrentAlarm = alarm;
            bindRingtone(mContext, mCurrentAlarm);
        }
    }

    @Override
    public void onValueChange(NumberPickerView picker, int oldVal, int newVal) {

    }

}
