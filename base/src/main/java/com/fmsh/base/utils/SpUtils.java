package com.fmsh.base.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by wuyajiang on 2018/3/7.
 */
public class SpUtils {
	private static SharedPreferences sp;


	public static void putBooleanValue( Context context,String key,
			boolean value) {

		if (sp == null) {
			sp = context.getSharedPreferences("data", Context.MODE_PRIVATE);
		}
		sp.edit().putBoolean(key, value).apply();
	}

	public static boolean getBooleanValue( Context context,String key,
			boolean defValue) {

		if (sp == null) {
			sp = context.getSharedPreferences("data", 0);
		}
		return sp.getBoolean(key, defValue);
	}

	public static void putStringValue( Context context,String key,
			String value) {

		if (sp == null) {
			sp = context.getSharedPreferences("data", 0);
		}
		sp.edit().putString(key, value).apply();
	}

	public static String getStringValue( Context context,String key,
			String defValue) {

		if (sp == null) {
			sp = context.getSharedPreferences("data", 0);
		}
		return sp.getString(key, defValue);
	}

	public static void remove( Context context,String key) {
		// TODO Auto-generated method stub
		if (sp == null) {
			sp = context.getSharedPreferences("data", 0);
		}
		sp.edit().remove(key).apply();
	}

	public static int getIntValue( Context context,String key,
										int defValue) {

		if (sp == null) {
			sp = context.getSharedPreferences("data", 0);
		}
		return sp.getInt(key, defValue);
	}

	public static void putIntValue(Context context, String key,
									  int value) {

		if (sp == null) {
			sp = context.getSharedPreferences("data", 0);
		}
		sp.edit().putInt(key, value).apply();
	}

	public static void clear(Context context){
		if (sp == null) {
			sp = context.getSharedPreferences("data", 0);
		}
		sp.edit().clear().apply();
	}

}
