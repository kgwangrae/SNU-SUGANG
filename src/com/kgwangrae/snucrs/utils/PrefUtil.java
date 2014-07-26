package com.kgwangrae.snucrs.utils;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefUtil {
	public static SharedPreferences.Editor getPrefEditor(Context c, String category) {
		return c.getSharedPreferences(category, Context.MODE_PRIVATE).edit();
	}
	
}
