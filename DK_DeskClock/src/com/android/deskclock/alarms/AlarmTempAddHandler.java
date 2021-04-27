/**
 * bv name zhangjiachu add Alarm数据同步：添加闹钟功能
 */
package com.android.deskclock.alarms;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Calendar;

public class AlarmTempAddHandler {
    private final static String TAG = "AlarmTempAddHandler";

    private AlarmTempService mService;

    private Context mAppContext;

    public AlarmTempAddHandler(Context context, AlarmTempService service) {
	Log.d(TAG, "AlarmTempAddHandler context: " + context);
        this.mAppContext = context;
	//mAppContext = context.getApplicationContext();
        this.mService=service;
    }

    /**
     * Adds a new alarm on the background.
     *
     * @param alarmInfoTemp The alarm to be added.
     */
    public void asyncAddAlarm(final AlarmInfoTemp alarmInfoTemp, final Alarm alarm) {
        final AsyncTask<Void, Void, AlarmInfoTemp> updateTask =
                new AsyncTask<Void, Void, AlarmInfoTemp>() {
                    @Override
                    protected AlarmInfoTemp doInBackground(Void... parameters) {

                        if (alarmInfoTemp != null) {

                            Events.sendAlarmEvent(R.string.action_create, R.string.label_deskclock);
                            ContentResolver cr = mAppContext.getContentResolver();

                            // Add alarm to db
                            Alarm newAlarm = Alarm.addAlarm(cr, alarm);

                            if(newAlarm.id != -1L){
                                alarmInfoTemp.setFlag(1);
                            }
                            // Be ready to scroll to this alarm on UI later.
                          //  mScrollHandler.setSmoothScrollStableId(newAlarm.id);

                            // Create and add instance to db
                            if (newAlarm.enabled) {
                                setupAlarmInstance(newAlarm);
                            }
                            Log.d(TAG,"asyncAddAlarm.alarmInfoTemp : " + alarmInfoTemp);
                        }
                        return alarmInfoTemp;
                    }

                    @Override
                    protected void onPostExecute(AlarmInfoTemp alarmInfoTemp) {
                        Log.d(TAG,"onPostExecute.alarmInfoTemp : " + alarmInfoTemp);
                        //report Result to client
                        try {
                            mService.reportResult(JsonUtil.ObjectToJson(alarmInfoTemp));
                            Log.d(TAG,"JsonUtil.ObjectToJson(alarmInfoTemp) : " + alarmInfoTemp);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
        updateTask.execute();
    }

    private AlarmInstance setupAlarmInstance(Alarm alarm) {

        final ContentResolver cr = mAppContext.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance());
        newInstance = AlarmInstance.addInstance(cr, newInstance);
        // Register instance to state manager
        AlarmStateManager.registerInstance(mAppContext, newInstance, true);
        return newInstance;
    }
}
