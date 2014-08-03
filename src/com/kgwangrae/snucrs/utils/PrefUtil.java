package com.kgwangrae.snucrs.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefUtil {
	private static String credentialCategory = "credential";
	private static String jSessionIdKey = "jSessionId";
	private static String timeStampKey = "timeStamp";
	
	public static SharedPreferences.Editor getPrefEditor(Context c, String category) {
		return c.getSharedPreferences(category, Context.MODE_PRIVATE).edit();
	}
	public static String getJSessionId(Context c) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE)
					.getString(jSessionIdKey, null);
	}
	public static boolean setJSessionId(Context c, String jSessionId) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE).edit()
				.putString(jSessionIdKey, jSessionId).commit();
	}
	public static long getTimeStamp(Context c) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE)
					.getLong(timeStampKey,0);
	}
	public static boolean setTimeStamp(Context c, long timeStamp) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE).edit()
				.putLong(timeStampKey,timeStamp).commit();
	}
}
