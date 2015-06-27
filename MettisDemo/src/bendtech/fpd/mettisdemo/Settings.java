package bendtech.fpd.mettisdemo;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
	private static final String METTIS_PREFS = "MettisDemoPrefs";
	private static final String LEFT_INSOLE_MAC = "leftInsoleMacAddress";
	private static final String RIGHT_INSOLE_MAC = "rightInsoleMacAddress";
	private static final String LEFT_STANCE_S0 = "leftStanceS0";
	private static final String LEFT_STANCE_S1 = "leftStanceS1";
	private static final String LEFT_STANCE_S2 = "leftStanceS2";
	private static final String RIGHT_STANCE_S0 = "leftStanceS0";
	private static final String RIGHT_STANCE_S1 = "leftStanceS1";
	private static final String RIGHT_STANCE_S2 = "leftStanceS2";
		
	private final SharedPreferences sharedPrefs;
	
	public Settings(Context context) {
		sharedPrefs = context.getSharedPreferences(
			METTIS_PREFS, Context.MODE_PRIVATE
			);
	}
	public String leftInsoleMacAddress() {
		return sharedPrefs.getString(LEFT_INSOLE_MAC, "");
	}
	public String rightInsoleMacAddress() {
		return sharedPrefs.getString(RIGHT_INSOLE_MAC, "");
	}
	public boolean isPaired() {
		final String left = leftInsoleMacAddress();
		final String right = rightInsoleMacAddress();
		
		return !left.isEmpty() && !right.isEmpty();
	}
	
	public void setLeftInsoleMacAddress(String value) {
		sharedPrefs.edit()
			.putString(LEFT_INSOLE_MAC, value)
			.commit();
	}
	public void setRightInsoleMacAddress(String value) {
		sharedPrefs.edit()
			.putString(RIGHT_INSOLE_MAC, value)
			.commit();
	}
	public void setLeftStances(int off0, int off1, int off2) {
		sharedPrefs.edit()
			.putInt(LEFT_STANCE_S0, off0)
			.putInt(LEFT_STANCE_S1, off1)
			.putInt(LEFT_STANCE_S2, off2)
			.commit();
	}
	public void setRightStances(int off0, int off1, int off2) {
		sharedPrefs.edit()
			.putInt(RIGHT_STANCE_S0, off0)
			.putInt(RIGHT_STANCE_S1, off1)
			.putInt(RIGHT_STANCE_S2, off2)
			.commit();
	}
	public int leftStanceS0() {
		return sharedPrefs.getInt(LEFT_STANCE_S0, 0);
	}
	public int leftStanceS1() {
		return sharedPrefs.getInt(LEFT_STANCE_S1, 0);
	}
	public int leftStanceS2() {
		return sharedPrefs.getInt(LEFT_STANCE_S2, 0);
	}
	
	public int rightStanceS0() {
		return sharedPrefs.getInt(RIGHT_STANCE_S0, 0);
	}
	public int rightStanceS1() {
		return sharedPrefs.getInt(RIGHT_STANCE_S1, 0);
	}
	public int rightStanceS2() {
		return sharedPrefs.getInt(RIGHT_STANCE_S2, 0);
	}
}
