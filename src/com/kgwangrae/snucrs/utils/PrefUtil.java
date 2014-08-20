package com.kgwangrae.snucrs.utils;

import android.content.Context;

public class PrefUtil {
	private static String credentialCategory = "credential";
	private static String jSessionIdKey = "jSessionId";
	private static String timeStampKey = "timeStamp";
	private static String studentIdKey = "sid";
	private static String passwordKey = "pw";
	
	public static String getJSessionId(Context c) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE)
					.getString(jSessionIdKey, null);
	}
	public static long getTimeStamp(Context c) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE)
					.getLong(timeStampKey,0);
	}
	public static boolean setJSessionId(Context c, String jSessionId, long timeStamp) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE).edit()
				.putString(jSessionIdKey, jSessionId)
				.putLong(timeStampKey,timeStamp)
				.commit();
	}
	public static boolean renewTimestamp(Context c) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE).edit()
				.putLong(timeStampKey,System.currentTimeMillis()).commit();
	}
	
	public static String getStudentId (Context c) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE)
				.getString(studentIdKey,null);
	}
	public static String getPassword (Context c) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE)
				.getString(passwordKey,null);
	}
	public static boolean saveLoginInfo (Context c, String studentId, String password) {
		return c.getSharedPreferences(credentialCategory, Context.MODE_PRIVATE).edit()
				.putString(studentIdKey, studentId)
				.putString(passwordKey, password)
				.commit();
	}
	public static boolean deleteLoginInfo (Context c) {
		return saveLoginInfo(c, null, null);
	}
}
