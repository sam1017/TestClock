/**
 * bv name zhangjiachu add Alarm数据同步：服务端（闹钟）的Service
 */
package com.android.deskclock.alarms;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.android.deskclock.LogUtils;
import com.android.deskclock.provider.Alarm;
import com.czy.alarm.AlarmController;
import com.czy.alarm.IListener;
import java.util.ArrayList;
import java.util.List;


public class AlarmTempService extends Service {

    private final String TAG = "AlarmTempService";
    private static final int MSG_ADD_ALARM=10001;

    private AlarmTempAddHandler mAlarmTempAddHandler;

    private List<AlarmInfoTemp> AlarmInfoTempList;

    private IListener listener;

    private Context context;

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_ADD_ALARM:{
                    AlarmInfoTemp alarmInfoTemp =(AlarmInfoTemp)msg.obj;
                    //保存闹钟
                    Alarm alarm=alarmInfoTemp.ConvertToAlarm();
                    Log.d(TAG, "zzz sendAlarmInfoTempInOut : alarmInfoTemp JsonToObject    " + alarmInfoTemp + "alarm:     " + alarm);
		    mAlarmTempAddHandler.asyncAddAlarm(alarmInfoTemp, alarm);
                    break;
                }

            }
        }
    };

    public AlarmTempService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AlarmInfoTempList = new ArrayList<>();
        mAlarmTempAddHandler = new AlarmTempAddHandler(getApplicationContext(), this);
    }

    private final AlarmController.Stub stub = new AlarmController.Stub() {


        @Override
        public void sendAlarmInfoTempInOut(String jsonAlarmInfoTemp) throws RemoteException {
	    Log.d(TAG, "zzz sendAlarmInfoTempInOut : jsonAlarmInfoTemp    " + jsonAlarmInfoTemp);
            if (TextUtils.isEmpty(jsonAlarmInfoTemp)) {
                Log.d(TAG, "zzz sendAlarmInfoTempInOut: " + jsonAlarmInfoTemp);
            }else {
                AlarmInfoTemp alarmInfoTemp = null;
                try {
                //解析jsonToObject，获得闹钟信息
                //AlarmInfoTemp temp1 =FastJsonUtils.jsonToObject(alarmInfoTemp, AlarmInfoTemp.class);
                    //Log.d(TAG, "zzz sendAlarmInfoTempInOut : alarmInfoTemp != null" + alarmInfoTemp);
                    alarmInfoTemp = JsonUtil.JsonToObject(jsonAlarmInfoTemp);
		    Log.d(TAG, "zzz sendAlarmInfoTempInOut : alarmInfoTemp JsonToObject    " + alarmInfoTemp);
                }catch (Exception e){
                    LogUtils.d(TAG,"ZZZ parseTransPackageInfo jsonObject err : "+e.getMessage());
                }
                //保存闹钟
                Message msg=handler.obtainMessage();
                msg.what=MSG_ADD_ALARM;
                msg.obj=alarmInfoTemp;
                handler.sendMessage(msg);
            }
        }


        @Override
        public void setListener(IListener lst) throws RemoteException {
            listener = lst;
        }
    };



    /**
     *  report Result to client
     *
     */
    public void reportResult(String temp) throws RemoteException {
        listener.sendProccessResult(temp);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }

}

