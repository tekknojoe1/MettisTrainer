package com.flexpoint.fpd;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
	private static final String FPD_PREFS = "FPDPrefs";
	private static final String FPD_IS_PAIRED = "isPaired";	
	private final SharedPreferences sharedPrefs;
	
	public Preferences(Context context) {
		sharedPrefs = context.getSharedPreferences(
			FPD_PREFS, Context.MODE_PRIVATE
			);
	}
	
	public boolean isPaired() {
		return sharedPrefs.getBoolean(FPD_IS_PAIRED, false);
	}
	public void setPaired(boolean set) {
		sharedPrefs.edit()
		.putBoolean(FPD_IS_PAIRED, set)
		.commit();
	}
}
