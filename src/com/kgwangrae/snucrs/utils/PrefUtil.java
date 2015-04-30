package com.kgwangrae.snucrs.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefUtil {
  public static String KEY_STUDENT_ID = "sid";
  public static String KEY_PASSWORD = "pw";
  
	private static String PREF_KEY_JSESSIONID = "jSessionId";
	private static String PREF_KEY_TIMESTAMP = "timeStamp";
  private static String PREF_KEY_PRESUB = "isPresubmissionMode";
  
  private static SharedPreferences sugangPref = SugangApp.getAppContext()
    .getSharedPreferences("sugangpref", Context.MODE_PRIVATE);
	
	public static String getJSessionId() {
		return sugangPref.getString(PREF_KEY_JSESSIONID, null);
	}
	public static long getTimeStamp() {
		return sugangPref.getLong(PREF_KEY_TIMESTAMP, 0);
	}
	public static boolean setJSessionId(String jSessionId, long timeStamp) {
		return sugangPref.edit()
        .putString(PREF_KEY_JSESSIONID, jSessionId)
				.putLong(PREF_KEY_TIMESTAMP,timeStamp)
				.commit();
	}
	public static boolean renewTimestamp() {
		return sugangPref.edit()
      .putLong(PREF_KEY_TIMESTAMP, System.currentTimeMillis())
      .commit();
	}
	
	public static String getStudentId() {
		return sugangPref.getString(KEY_STUDENT_ID, null);
	}
	public static String getPassword() {
		return sugangPref.getString(KEY_PASSWORD, null);
	}
	public static boolean saveLoginInfo(String studentId, String password) {
		return sugangPref.edit()
				.putString(KEY_STUDENT_ID, studentId)
				.putString(KEY_PASSWORD, password)
				.commit();
	}
	public static boolean deleteLoginInfo() {
		return saveLoginInfo(null, null);
	}

  /**
   * TODO avoid using context as a parameter in above methods.
   */
  public static boolean setPresubmission(boolean isPresubmission) {
    return sugangPref.edit()
      .putBoolean(PREF_KEY_PRESUB, isPresubmission)
      .commit();
  }

  public static boolean isPresubmissionMode(){
    return sugangPref.getBoolean(PREF_KEY_PRESUB, false);
  }
}
