package saxatech.flexpoint;

import java.util.ArrayList;

import saxatech.flexpoint.BleDeviceDiscovery.DeviceInfo;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class DiscoveryFragment extends Fragment {
	private static final boolean DEBUG = true;
	private static final String LOG_TAG = "DiscoveryFragment";
	
	public static interface ActivityCallbacks {
		void onDiscovered(BleFPDIdentity identitiy);
		void onFailedDiscovery();
	}
		
	private Context appContext;
	private ActivityCallbacks callbacks;
	private Handler handler;
	private BluetoothAdapter bluetoothAdapter;
	private BleFPDIdentity fpdIdentity;
	private BleDeviceDiscovery bleDeviceDiscovery;
	private BleDiscoveryCallback bleDiscoveryCallback;
	
	private static final int BLE_SCAN_PERIOD_MS = 10000;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		appContext = activity.getApplicationContext();
		callbacks = (ActivityCallbacks)activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Retain this fragment across configuration changes.
		setRetainInstance(true);
		
		handler = new Handler();
		
		BluetoothManager bluetoothManager =
			(BluetoothManager)appContext.getSystemService(Context.BLUETOOTH_SERVICE);
		
		bluetoothAdapter = bluetoothManager.getAdapter();
		
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			Toast.makeText(
				appContext,
				"Bluetooth is not enabled", 
				Toast.LENGTH_LONG
				).show();
			failedInit();
			return;
		}
		
		fpdIdentity = BleFPDIdentity.loadFromJSON(appContext);
		if (fpdIdentity == null) {
			Toast.makeText(
				appContext,
				"Failed to load Mettis config",
				Toast.LENGTH_LONG
				).show();
			failedInit();
			return;
		}
		
		final ArrayList<String> deviceMatchList =
				new ArrayList<String>();
			
		if (fpdIdentity.isLeftShoeEnabled())
			deviceMatchList.add(fpdIdentity.getLeftShoeAddr());
		if (fpdIdentity.isRightShoeEnabled())
			deviceMatchList.add(fpdIdentity.getRightShoeAddr());
		if (fpdIdentity.isClubEnabled())
			deviceMatchList.add(fpdIdentity.getClubAddr());
		
		if (deviceMatchList.isEmpty()) {
			Toast.makeText(
				appContext,
				"All devices in identity config are disabled",
				Toast.LENGTH_LONG
				).show();
			failedInit();
			return;
		}
		
		bleDiscoveryCallback = new BleDiscoveryCallback();
		
		bleDeviceDiscovery = new BleDeviceDiscovery(
			bluetoothAdapter,
			bleDiscoveryCallback
			);
		
		handler.postDelayed(new Runnable() {
			public void run() {
				if (bleDeviceDiscovery.isScanning()) {
					bleDeviceDiscovery.stop();
					callbacks.onFailedDiscovery();
				}
			}
		}, BLE_SCAN_PERIOD_MS);
				
		bleDeviceDiscovery.start(deviceMatchList);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (bleDeviceDiscovery != null)
			bleDeviceDiscovery.stop();
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		callbacks = null; //don't leak activity.
	}
		
	private void failedInit() {
		handler.post(new Runnable() {
			public void run() {
				callbacks.onFailedDiscovery();
			}
		});
	}
	
	private class BleDiscoveryCallback implements BleDeviceDiscovery.DiscoveryCallback {

		@Override
		public void onDeviceFound(
			String name, String address, BluetoothDevice device)
		{
			if (!name.contains("Mettis Trainer"))
				return;
				
			if (DEBUG)
				Log.i(LOG_TAG, "found: " + name + " " + address);
		}

		@Override
		public void onDevicesMatched(ArrayList<DeviceInfo> deviceInfoList) {
			handler.post(new Runnable() {
				public void run() {
					bleDeviceDiscovery.stop();					
					callbacks.onDiscovered(fpdIdentity);
				}
			});
		}
	}
}
