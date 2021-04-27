/**
 * bv name add for  Alarm数据同步：ObjectToJson/JsonToObject:实现Json和Object之间转换的工具类
 */
package com.android.deskclock.alarms;
import android.text.TextUtils;
import android.util.Log;
import com.android.deskclock.LogUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtil {

    private final static String TAG = "JsonUtil";

    public static String ObjectToJson(AlarmInfoTemp infoTemp) throws JSONException {
        //创建JSONObject
        JSONObject  jsonObject = new  JSONObject();
        try{
            //键值对赋值
            jsonObject.put("_id", infoTemp.get_id());
            jsonObject.put("hour",infoTemp.getHour());
            jsonObject.put("minutes",infoTemp.getMinutes());
            jsonObject.put("daysofweek",infoTemp.getDaysofweek());
            jsonObject.put("enabled",infoTemp.getEnabled());
            jsonObject.put("vibrate",infoTemp.getVibrate());
            jsonObject.put("label",infoTemp.getLabel());
            jsonObject.put("ringtone",infoTemp.getRingtone());
            jsonObject.put("delete_after_use",infoTemp.getDelete_after_use());
            jsonObject.put("flag",infoTemp.getFlag());
        }catch (Exception e){
            //info = null;
            Log.d(TAG,"ZZZ parseTransPackageInfo jsonToObject err : "+e.getMessage());
        }
        return jsonObject.toString();
    }

    public static AlarmInfoTemp JsonToObject(String jsonData) throws JSONException {
        AlarmInfoTemp info;
        Log.d(TAG,"ZZZ parseTransPackageInfo jsonObject  ....");
        if (TextUtils.isEmpty(jsonData)){
            Log.d(TAG,"ZZZ parseTransPackageInfo jsonObject  info: " + jsonData);
            return null;
        }
        //LogUtils.d(TAG,"ZZZ parseTransPackageInfo jsonObject  jsonData: "+ jsonData);
        info = new AlarmInfoTemp();
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            info.set_id(jsonObject.getInt("id")); 
            info.setHour(jsonObject.getInt("hour"));
            info.setMinutes(jsonObject.getInt("minutes"));
            info.setDaysofweek(jsonObject.getInt("daysofweek"));
            info.setEnabled(jsonObject.getInt("enabled"));
            info.setVibrate(jsonObject.getInt("vibrate"));
            info.setLabel(jsonObject.getString("label"));
            info.setRingtone(jsonObject.getString("ringtone"));
            info.setDelete_after_use(jsonObject.getInt("delete_after_use"));
            info.setFlag(jsonObject.getInt("flag"));
            Log.d(TAG,"ZZZ parseTransPackageInfo jsonObject  info: "+ info);
            //Log.e("JsonToObject",info.get_id()));
        }catch (Exception e){
            Log.e(TAG,"jsonObject err : " + e.getMessage());
	}
        //LogUtils.d(TAG,"ZZZ parseTransPackageInfo jsonObject  info: "+ info);
        return info;
    }

}
