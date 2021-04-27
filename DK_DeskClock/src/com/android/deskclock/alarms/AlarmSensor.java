
package com.android.deskclock.alarms;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemProperties;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.util.Log;
import java.util.Stack;
import android.content.Intent;
import android.content.IntentFilter;
/**
 * Created by wpf on 15-9-11.
 */
public class AlarmSensor implements SensorEventListener {

    private static final String TAG = "AlarmSensor";
    private static final boolean DEBUG = true;
    
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Context mContext;

    private static AlarmSensor mAlarmSensor;
    private boolean mIsSensorRegisted = false;
    private int mSensorStatusFlag = 1;
    private boolean mPhoneAttitudeInitFlag = false;
    private PHONE_ATTITUDE mInitPhoneAttitude;

    private float lastX;
    private float lastY;
    private float lastZ;
    private long currTime;
    private long lastTime;
    private long duration;
    private float currShake;
    private float totalShake;
    
    private enum PHONE_ATTITUDE {
        ATT_UNKNOWN,
        ATT_UP,
        ATT_DOWN,
        ATT_VERTICAL,
        ATT_OBLIUP,
        ATT_OBLIDOWN,
    }



    private AlarmSensor(Context context) {
        super();
        this.mContext = context;
    }

    public static AlarmSensor getInstance(Context context) {
        if (mAlarmSensor == null) {
            mAlarmSensor = new AlarmSensor(context);
        }
        return mAlarmSensor;
    }

    public void init() {
        if(DEBUG) Log.d(TAG, "AlarmSensor init");
        
        mPhoneAttitudeInitFlag = false;



        if (null == mSensorManager) {
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        }

        if (null != mSensorManager && null == mSensor) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (null == mSensor) {
                mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            }
        }
        if (null != mSensor && !mIsSensorRegisted) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            mIsSensorRegisted = true;
        }

        if(DEBUG) Log.d(TAG, "init() returned ");
    }

    private PHONE_ATTITUDE getPhoneAttitude(int z) {
        PHONE_ATTITUDE ret = PHONE_ATTITUDE.ATT_UNKNOWN;
        if (z > 8) ret = PHONE_ATTITUDE.ATT_UP;
        else if (z < -8) ret = PHONE_ATTITUDE.ATT_DOWN;
        else {
            if(z > -2 && z < 2) {
                ret = PHONE_ATTITUDE.ATT_VERTICAL;
            } else if(z > 0){
                ret = PHONE_ATTITUDE.ATT_OBLIUP;
            } else {
                ret = PHONE_ATTITUDE.ATT_OBLIDOWN;
            }
        }

        return ret;
    }

    private boolean isTurned(PHONE_ATTITUDE currAttitude) {
        boolean ret = false;
        switch(currAttitude) {
            case ATT_UP:
                if(mInitPhoneAttitude == PHONE_ATTITUDE.ATT_VERTICAL ||
                        mInitPhoneAttitude == PHONE_ATTITUDE.ATT_DOWN ||
                        mInitPhoneAttitude == PHONE_ATTITUDE.ATT_OBLIDOWN){
                    ret = true;
                }
                break;
            case ATT_DOWN:
                if(mInitPhoneAttitude == PHONE_ATTITUDE.ATT_VERTICAL ||
                        mInitPhoneAttitude == PHONE_ATTITUDE.ATT_UP ||
                        mInitPhoneAttitude == PHONE_ATTITUDE.ATT_OBLIUP) {
                    ret = true;
                }
                break;
            default:
                break;
        }

        return ret;
    }    

    public void onSensorChanged(SensorEvent event) {
        int x = (int) event.values[0];
        int y = (int) event.values[1];
        int z = (int) event.values[2];
        
        if(DEBUG) Log.d(TAG, "onSensorChanged,x="+x+",y="+y+",z="+z);
        
            PHONE_ATTITUDE currentPhoneStatue = PHONE_ATTITUDE.ATT_UNKNOWN;
    
            if (!mPhoneAttitudeInitFlag) {
                mInitPhoneAttitude = getPhoneAttitude(z);
                if(DEBUG) Log.d(TAG, "***WPF***: onSensorChanged init z:" + z + "init att:" + mInitPhoneAttitude);
                mPhoneAttitudeInitFlag = true;
            } else {
                currentPhoneStatue = getPhoneAttitude(z);
            }
    
            if (isTurned(currentPhoneStatue)) {
                doMute();
            }


    }

    public void onAccuracyChanged(Sensor mSensor, int accuracy) {
    }

    private void stopListen() {
        if(DEBUG) Log.d(TAG, "entry stopListen");
        if (null != mSensorManager && null != mSensor && mIsSensorRegisted) {
            mSensorManager.unregisterListener(this);
            mIsSensorRegisted = false;
        }
    }

    public void destroy() {
        stopListen();
    }

    private void doMute() {
        if(DEBUG) Log.d(TAG, "entry doMute");

 	if(android.os.SystemProperties.getBoolean("persist.sys.alarmdismiss", false)){
             mContext.sendBroadcast(new Intent(AlarmService.ALARM_DISMISS_ACTION));
	}else{
             mContext.sendBroadcast(new Intent(AlarmService.ALARM_SNOOZE_ACTION));
	}




    }

}



