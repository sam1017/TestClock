/**
 * bv name zhangjiachu Alarm数据同步：闹钟属性工具类
 */
package com.android.deskclock.alarms;

import android.net.Uri;
import android.text.TextUtils;
import com.android.deskclock.data.Weekdays;
import com.android.deskclock.provider.Alarm;
import java.io.Serializable;
import java.util.Calendar;

public class AlarmInfoTemp implements Serializable {

    private int _id;
    private int hour;
    private int minutes;
    private int daysofweek;
    private int enabled;
    private int vibrate;
    private String label ;
    private String ringtone;
    private int delete_after_use;
    private int flag;
    public AlarmInfoTemp() {
    }

    public int isFlag() {
        return flag;
    }


    public AlarmInfoTemp(int _id, int hour, int minutes, int daysofweek, int enabled, int vibrate, String label, String ringtone, int delete_after_use, int flag) {
        this._id = _id;
        this.hour = hour;
        this.minutes = minutes;
        this.daysofweek = daysofweek;
        this.enabled = enabled;
        this.vibrate = vibrate;
        this.label = label;
        this.ringtone = ringtone;
        this.delete_after_use = delete_after_use;
        this.flag = flag;
    }

    @Override
    public String toString() {
        return _id+","+hour+","+minutes+","+daysofweek+","+enabled+","+vibrate+","+label+","+ringtone+","+delete_after_use+","+flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public int getFlag() {
        return flag;
    }
    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public int getDaysofweek() {
        return daysofweek;
    }

    public void setDaysofweek(int daysofweek) {
        this.daysofweek = daysofweek;
    }

    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    public int getVibrate() {
        return vibrate;
    }

    public void setVibrate(int vibrate) {
        this.vibrate = vibrate;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRingtone() {
        return ringtone;
    }

    public void setRingtone(String ringtone) {
        this.ringtone = ringtone;
    }

    public int getDelete_after_use() {
        return delete_after_use;
    }

    public void setDelete_after_use(int delete_after_use) {
        this.delete_after_use = delete_after_use;
    }



    public Alarm ConvertToAlarm(){

        final Alarm alarm = new Alarm();

        alarm.hour = this.getHour();

        alarm.minutes = this.getMinutes();

        if (this.getEnabled() == 1) {
            alarm.enabled = true;
        }else {
            alarm.enabled = false;
        }

        final int daysDb = this.getDaysofweek();
        alarm.daysOfWeek = Weekdays.fromBits(daysDb);


        if (this.getVibrate() == 1){
            alarm.vibrate = true;
        } else {
            alarm.vibrate = false;
        }

        alarm.label = this.getLabel();

        final String alertString = this.getRingtone();
        if ("silent".equals(alertString)) {
            alarm.alert = Alarm.NO_RINGTONE_URI;
        } else {
            alarm.alert =
                    TextUtils.isEmpty(alertString) ? null : Uri.parse(alertString);
        }

        if (this.getDelete_after_use() == 1){
            alarm.deleteAfterUse = true;
        } else {
            alarm.deleteAfterUse = false;
        }

        return alarm;
    }

}
