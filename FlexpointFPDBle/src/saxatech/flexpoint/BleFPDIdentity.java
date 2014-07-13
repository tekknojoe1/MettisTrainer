package saxatech.flexpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class BleFPDIdentity implements Parcelable {
	public static final String CONFIG_FNAME = "fpd_identity.cfg";
	
	private static final String FPD_PAIR_PREFS   = "FPDPairing";
	private static final String FPD_PAIRED_LEFT  = "pairedLeft";
	private static final String FPD_PAIRED_RIGHT = "pairedRight";
		
	private String leftShoeAddr;
	private String rightShoeAddr;
	private String clubAddr;
	// mainly for debugging. only set from cfg file.
	private boolean leftShoeDisabled;
	private boolean rightShoeDisabled;
	private boolean clubDisabled;
	private boolean logData;
			
	public BleFPDIdentity() {		
	}
	
	public BleFPDIdentity(Parcel in) {
		readFromParcel(in);
	}
	
	public String getLeftShoeAddr() {
		return leftShoeAddr;
	}
	public String getRightShoeAddr() {
		return rightShoeAddr;
	}
	public String getClubAddr() {
		return clubAddr;
	}	
	public boolean isLeftShoeEnabled() {
		return !leftShoeDisabled;
	}
	public boolean isRightShoeEnabled() {
		return !rightShoeDisabled;
	}
	public boolean isClubEnabled() {
		return !clubDisabled;
	}
	public boolean isDataLoggingEnabled() {
		return logData;
	}
	
	public void setLeftShoeAddr(String addr) {
		leftShoeAddr = addr;
	}
	public void setRightShoeAddr(String addr) {
		rightShoeAddr = addr;
	}
	public void setClubAddr(String addr) {
		clubAddr = addr;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(leftShoeAddr);
		dest.writeString(rightShoeAddr);
		dest.writeString(clubAddr);
		dest.writeBooleanArray(new boolean []  
			{
				leftShoeDisabled,
				rightShoeDisabled,
				clubDisabled,
				logData
			}
		);
	}
	
	private void readFromParcel(Parcel in) {
		leftShoeAddr = in.readString();
		rightShoeAddr = in.readString();
		clubAddr = in.readString();
		boolean[] array = new boolean[4];
		in.readBooleanArray(array);
		leftShoeDisabled  = array[0];
		rightShoeDisabled = array[1];
		clubDisabled      = array[2];
		logData           = array[3];
	}
	
	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR =
		new Parcelable.Creator() {
			public BleFPDIdentity createFromParcel(Parcel in) {
				return new BleFPDIdentity(in);
			}
			public BleFPDIdentity[] newArray(int size) {
				return new BleFPDIdentity[size];
			}
	};
	
	public Bundle makeIntoBundle() {
		Bundle b = new Bundle();
		b.putParcelable("Identity", this);
		return b;
	}
	
	public static void setPairedLeft(Context context, String address) {
		SharedPreferences sharedPrefs = context.getSharedPreferences(
				FPD_PAIR_PREFS, Context.MODE_PRIVATE
				);
		sharedPrefs.edit()
		.putString(FPD_PAIRED_LEFT, address)
		.commit();
	}
	public static void setPairedRight(Context context, String address) {
		SharedPreferences sharedPrefs = context.getSharedPreferences(
				FPD_PAIR_PREFS, Context.MODE_PRIVATE
				);
		sharedPrefs.edit()
		.putString(FPD_PAIRED_RIGHT, address)
		.commit();
	}
	
	public static BleFPDIdentity getFromBundle(Bundle b) {
		return b.getParcelable("Identity");
	}
		
	public static boolean needsPairingFromJSON(Context context) {
		return needsPairingFromJSON(context, CONFIG_FNAME);
	}
	
	public static boolean needsPairingFromJSON(Context context, String fname) {
		BufferedReader reader = null;
		try {
			AssetManager assets = context.getAssets();
			reader = new BufferedReader(
				new InputStreamReader(assets.open(fname))
				);
			String jsonString = "";
			String line = reader.readLine();
			while (line != null) {
				jsonString += line;
				line = reader.readLine();
			}
			JSONObject json = new JSONObject(jsonString);
			return json.getBoolean("needs_pairing");
			
		} catch (Exception e) {
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {					
				}
			}
		}
		return true;
	}
	
	public static BleFPDIdentity loadFromJSON(Context context) {
		return loadFromJSON(context, CONFIG_FNAME);
	}
	
	public static BleFPDIdentity loadFromJSON(Context context, String fname) {
		BleFPDIdentity identity = null;
		BufferedReader reader = null;
		
		SharedPreferences sharedPrefs = context.getSharedPreferences(
			FPD_PAIR_PREFS, Context.MODE_PRIVATE
			);
		
		try {
			AssetManager assets = context.getAssets();
			reader = new BufferedReader(
				new InputStreamReader(assets.open(fname))
				);
			String jsonString = "";
			String line = reader.readLine();
			while (line != null) {
				jsonString += line;
				line = reader.readLine();
			}
			JSONObject json = new JSONObject(jsonString);
			final String invalid_addr =
				"invalid BT address format (note: Alpha must be uppercase)";
			
			BleFPDIdentity fpdi = new BleFPDIdentity();
									
			if (json.has("left_shoe_disabled"))
				fpdi.leftShoeDisabled = json.getBoolean("left_shoe_disabled");
			if (json.has("right_shoe_disabled"))
				fpdi.rightShoeDisabled = json.getBoolean("right_shoe_disabled");
			if (json.has("club_disabled"))
				fpdi.clubDisabled = json.getBoolean("club_disabled");
			if (json.has("log_data"))
				fpdi.logData = json.getBoolean("log_data");
						
			if (!fpdi.leftShoeDisabled) {
				try {
					fpdi.leftShoeAddr =
						json.getString("left_shoe_addr").toUpperCase(Locale.ENGLISH);
				} catch (JSONException e) {
					final String address = sharedPrefs.getString(FPD_PAIRED_LEFT, "");
					if (address.isEmpty())
						throw new JSONException("No address found for left");
					fpdi.leftShoeAddr = address;
				}
				if (!BluetoothAdapter.checkBluetoothAddress(fpdi.leftShoeAddr))
					throw new JSONException(invalid_addr + ": left_shoe_addr");
			}
			if (!fpdi.rightShoeDisabled) {
				try {
					fpdi.rightShoeAddr =
						json.getString("right_shoe_addr").toUpperCase(Locale.ENGLISH);
				} catch (JSONException e) {
					final String address = sharedPrefs.getString(FPD_PAIRED_RIGHT, "");
					if (address.isEmpty())
						throw new JSONException("No address found for left");
					fpdi.rightShoeAddr = address;
				}
				if (!BluetoothAdapter.checkBluetoothAddress(fpdi.rightShoeAddr))
					throw new JSONException(invalid_addr+ ": right_shoe_addr");
			}
			if (!fpdi.clubDisabled) {
				fpdi.clubAddr =
					json.getString("club_addr").toUpperCase(Locale.ENGLISH);
				if (!BluetoothAdapter.checkBluetoothAddress(fpdi.clubAddr))
					throw new JSONException(invalid_addr + ": club_addr");
			}
			
			identity = fpdi;
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {					
				}
			}
		}
		return identity;
	}
		
}
