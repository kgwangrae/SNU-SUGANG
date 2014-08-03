package com.kgwangrae.snucrs.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefUtil {
	private static String credentialCategory = "credential";
	private static String jSessionIdKey = "jSessionId";
	private static String timeStampKey = "timeStamp";
	
	public static String getJSessionId(Context c) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE)
					.getString(jSessionIdKey, null);
	}
	public static long getTimeStamp(Context c) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE)
					.getLong(timeStampKey,0);
	}
	public static boolean setCredential(Context c, String jSessionId, long timeStamp) {
		SharedPreferences.Editor pref 
			= c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE).edit();
		return pref.putString(jSessionIdKey, jSessionId)
						.putLong(timeStampKey,timeStamp)
						.commit();
	}
	public static boolean putCurrentTime(Context c) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE).edit()
				.putLong(timeStampKey,System.currentTimeMillis()).commit();
	}
}
