package com.fmsh.einkesl.tools.text;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * 数据库管理类
 * @author niyijin
 */
public class FMDBManager {
    private static final Object sStorageLock = new Object();
    public static boolean WriteRecord(Context c,String strRecordName,String strData){
        synchronized(sStorageLock) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
            prefs.edit().putString(strRecordName, strData ).commit();
            return true;
        }
    }

    public static String ReadRecord(Context c,String strRecordName ){
        synchronized(sStorageLock) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
            String strRecordData = prefs.getString(strRecordName,"00000000" );
            if(strRecordData.equals("00000000")){
                return null;
            }
            return strRecordData;
        }
    }


}
