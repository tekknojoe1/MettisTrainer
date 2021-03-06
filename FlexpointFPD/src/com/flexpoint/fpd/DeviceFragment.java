package com.flexpoint.fpd;

import saxatech.flexpoint.BleMettisDeviceGroup;
import saxatech.flexpoint.BleFPDIdentity;
import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

public class DeviceFragment extends Fragment {
	public static final int DEVICE_TYPE_LEFT_SHOE  = BleMettisDeviceGroup.DEVICE_TYPE_LEFT_SHOE;
	public static final int DEVICE_TYPE_RIGHT_SHOE = BleMettisDeviceGroup.DEVICE_TYPE_RIGHT_SHOE;
	
	public static interface ActivityCallbacks {
		public void onConnected();
		public void onDisconnected();
		public void onFailed(String error);
		
		public void onSensor(
			int deviceType,
			long timeStampNsec,
			int fs0, int fs1, int fs2
			);
	}
	
	private String leftShoeAddress;
	private String rightShoeAddress;
	private ActivityCallbacks activityCallbacks;
	private Handler handler;
	private BleMettisDeviceGroup bleDevices;
	private boolean logData;
	
	public void setDevices(
		BleFPDIdentity identity
		)
	{
		leftShoeAddress = null;
		rightShoeAddress = null;		
		
		if (identity.isLeftShoeEnabled())
			leftShoeAddress = identity.getLeftShoeAddr();
		if (identity.isRightShoeEnabled())
			rightShoeAddress = identity.getRightShoeAddr();		
		logData = identity.isDataLoggingEnabled();
	}
	
	public void setDevices(
		String leftShoeAddress,
		String rightShoeAddress,
		String clubAddress
		)
	{
		this.leftShoeAddress = leftShoeAddress;
		this.rightShoeAddress = rightShoeAddress;
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
		
		BluetoothDevice bluetoothLeftShoe  = null;
		BluetoothDevice bluetoothRightShoe = null;
		
		if (leftShoeAddress != null)
			bluetoothLeftShoe = bluetoothAdapter.getRemoteDevice(leftShoeAddress);
		if (rightShoeAddress != null)
			bluetoothRightShoe = bluetoothAdapter.getRemoteDevice(rightShoeAddress);		
		
		bleDevices = new BleMettisDeviceGroup();
		final boolean rv = bleDevices.connect(
			context,
			bluetoothLeftShoe,
			bluetoothRightShoe,
			logData,
			new BleDevConnectCallback(),
			new BleDevDataCallback()
			);
		
		if (!rv) {
			Toast.makeText(
				context,
				"Failed initial device connect.",
				Toast.LENGTH_LONG
				).show();
			failedInit("Failed initial device connect.");
			return;
		}
	}
	@Override
	public void onDetach() {
		super.onDetach();
		activityCallbacks = null;
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		close();
	}
	
	public void close() {
		if (bleDevices != null)
			bleDevices.close();
	}
	
	public boolean setLeftDataCharacteristic(
		byte[] data
		)
	{
		return bleDevices.setDataCharacteristic(
			BleMettisDeviceGroup.DEVICE_TYPE_LEFT_SHOE,
			data
			);
	}
	public boolean setRightDataCharacteristic(
		byte[] data
		)
	{
		return bleDevices.setDataCharacteristic(
			BleMettisDeviceGroup.DEVICE_TYPE_RIGHT_SHOE,
			data
			);
	}
			
	private void failedInit(final String error) {
		handler.post(new Runnable() {
			public void run() {
				if (activityCallbacks != null)
					activityCallbacks.onFailed(error);
			}
		});
	}
	
	private class BleDevConnectCallback implements BleMettisDeviceGroup.ConnectCallback {
		@Override
		public void onConnected() {
			if (activityCallbacks == null)
				return;
			bleDevices.startData();
			activityCallbacks.onConnected();
		}
		@Override
		public void onDisconnected(int deviceType) {
			bleDevices.close();
			if (activityCallbacks == null)
				return;			
			activityCallbacks.onDisconnected();
		}
		@Override
		public void onFailed(int deviceType, int connect_error) {
			bleDevices.close();
			if (activityCallbacks == null)
				return;
			activityCallbacks.onFailed(
				BleMettisDeviceGroup.connectErrorString(connect_error)
				);
		}
	}
	private class BleDevDataCallback implements BleMettisDeviceGroup.DataCallback {
		@Override
		public void onData(
				int deviceType,
				long timeStamp,
				int medial, int lateral, int heal,
				int cadence, int contactTime, int impactForce
			)
		{
			if (activityCallbacks == null)
				return;
			activityCallbacks.onSensor(deviceType, timeStamp, medial, lateral, heal);
		}
		@Override
		public void onBattStatus(
			int deviceType, int batteryLevel,
			int maxBatteryLevel
			)
		{
			// TODO Auto-generated method stub			
		}
		@Override
		public void onInfo(
			int deviceType,
			String version
			)
		{
			// TODO Auto-generated method stub
			
		}
	}
}
