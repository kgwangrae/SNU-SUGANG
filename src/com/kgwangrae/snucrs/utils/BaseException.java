package com.kgwangrae.snucrs.utils;

import android.util.Log;

/**
 * Basic form of Exception used in this application. 
 * @author Gwangrae Kim
 */
@SuppressWarnings("serial")
public class BaseException extends Exception {
	private String mReason = null;
	
	public BaseException (String TAG, String reason) {
		this.mReason = reason;
		Log.e(TAG, reason);
	}
	public String getReason () {
		return mReason;
	}
}
