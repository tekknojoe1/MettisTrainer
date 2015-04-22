package flexpoint.fpd.sensors;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import saxatech.flexpoint.BleFPDDevice;
import saxatech.flexpoint.BleMettisDevice;

public class DeviceFragment extends Fragment {

	public static interface ActivityCallbacks {
		public void onConnected();
		public void onDisconnected();
		public void onFailed(String error);
		
		public void onSensor(
			int fs0, int fs1, int fs2, int fs3, int fs4
			);
		public void onVersion(String versionString);
	}
	
	private String  btAddress;
	private ActivityCallbacks activityCallbacks;
	private Handler handler;
	private BleMettisDevice bleDevice;
	
	public void setDevice(String btAddress, boolean commonReversed) {
		this.btAddress = btAddress;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		activityCallbacks = (ActivityCallbacks)activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Retain this fragment across configuration changes.
		setRetainInstance(true);
		
		handler = new Handler();
		
		Context context = getActivity().getApplicationContext();
		
		BluetoothManager bluetoothManager =
				(BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
			
		final BluetoothAdapter bluetoothAdapter =
				bluetoothManager.getAdapter();
		
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			Toast.makeText(
				context,
				"Bluetooth is not enabled",
				Toast.LENGTH_LONG
				).show();
			failedInit("Bluetooth is not enabled.");
			return;
		}
		
		BluetoothDevice btDevice =
			bluetoothAdapter.getRemoteDevice(btAddress);
		
		if (btDevice == null) {
			Toast.makeText(
				context,
				"Bluetooth device no longer available.",
				Toast.LENGTH_LONG
				).show();
			failedInit("Bluetooth device no longer available.");
		}
		
		bleDevice = new BleMettisDevice(
			context, btDevice,
			new BleDeviceConnectCallback(),
			new BleDeviceDataCallback()
			);
		bleDevice.connect();
	}
	
	private void failedInit(final String error) {
		handler.post(new Runnable() {
			public void run() {
				if (activityCallbacks != null)
					activityCallbacks.onFailed(error);
			}
		});
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		activityCallbacks = null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (bleDevice != null)
			bleDevice.close();
	}
	
	private class BleDeviceConnectCallback implements BleMettisDevice.ConnectCallback {

		@Override
		public void onConnected() {
			handler.post(new Runnable() {
				public void run() {					
					if (activityCallbacks != null) {
						bleDevice.startData();
						activityCallbacks.onConnected();
					}
				}
			});
		}

		@Override
		public void onDisconnected() {
			handler.post(new Runnable() {
				public void run() {
					if (activityCallbacks != null)
						activityCallbacks.onDisconnected();
				}
			});
		}

		@Override
		public void onFailed(final int connect_error) {
			handler.post(new Runnable() {
				public void run() {
					if (activityCallbacks != null) {
						final String errorString =
							BleFPDDevice.connectErrorString(connect_error);
						activityCallbacks.onFailed(errorString);
					}
				}
			});
		}
	}
	
	private class BleDeviceDataCallback implements BleMettisDevice.DataCallback {

		@Override
		public void onInfo(String version) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onData(long timeStampNsec, int medial, int lateral,
				int heal, int cadence, int contactTime, int impactForce) {
			// TODO Auto-generated method stub
			activityCallbacks.onSensor(medial, lateral, heal, 0, 0);
		}

		@Override
		public void onBattStatus(int battLevel, int maxBattLevel) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	/*
	private class BleDeviceDataCallback implements BleFPDDevice.DataCallback {

		@Override
		public void onAIOData(
			final long timeStamp,
			final int fs0,
			final int fs1,
			final int fs2,
			final int fs3,
			final int fs4,
			int acX,
			int acY,
			int acZ,
			int mgX,
			int mgY,
			int mgZ
			)
		{			
			handler.post(new Runnable() {
				public void run() {
					if (activityCallbacks != null) {						
						activityCallbacks.onSensor(
							fs0, fs1, fs2, fs3, fs4
							);
					}
				}
			});
		}

		@Override
		public void onBatteryStatus(
			int batteryLevel, int maxBatteryLevel,
			boolean isCharging
			)
		{	
		}

		@Override
		public void onInfo(final String versionString) {
			handler.post(new Runnable() {
				public void run() {
					if (activityCallbacks != null) {						
						activityCallbacks.onVersion(
							versionString
							);
					}
				}
			});
		}
		
	}
	*/
}
