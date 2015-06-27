package bendtech.fpd.mettisdemo;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import saxatech.flexpoint.BleMettisDeviceGroup;

public class InsolesFragment extends Fragment {
	public static final int DEVICE_TYPE_LEFT_SHOE  = BleMettisDeviceGroup.DEVICE_TYPE_LEFT_SHOE;
	public static final int DEVICE_TYPE_RIGHT_SHOE = BleMettisDeviceGroup.DEVICE_TYPE_RIGHT_SHOE;
	
	public static interface ActivityCallbacks {
		public void onConnected();
		public void onDisconnected();
		public void onFailed(String error);
		
		public void onSensor(
			int deviceType,
			long timeStampNsec,
			int medial, int lateral, int heal,
			int cadence, int contactTime, int impactForce
			);
	}
	
	private Handler handler;
	
	public InsolesFragment() {
		handler = new Handler(Looper.getMainLooper());
	}
	
	private String leftInsoleMacAddress;
	private String rightInsoleMacAddress;
	private ActivityCallbacks activityCallbacks;
	private BleMettisDeviceGroup bleDevices;
	
	public void setInsoles(
		String leftInsoleMacAddress,
		String rightInsoleMacAddress
		)
	{
		this.leftInsoleMacAddress = leftInsoleMacAddress;
		this.rightInsoleMacAddress = rightInsoleMacAddress;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		activityCallbacks = (ActivityCallbacks)activity;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Context context = getActivity().getApplicationContext();
		
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
			return;
		}
		
		BluetoothDevice bluetoothLeftShoe;
		BluetoothDevice bluetoothRightShoe;
			
		bluetoothLeftShoe = bluetoothAdapter.getRemoteDevice(leftInsoleMacAddress);		
		bluetoothRightShoe = bluetoothAdapter.getRemoteDevice(rightInsoleMacAddress);
		
		bleDevices = new BleMettisDeviceGroup();
		final boolean rv = bleDevices.connect(
			context,
			bluetoothLeftShoe,
			bluetoothRightShoe,
			false,
			new BleDevConnectCallback(),
			new BleDevDataCallback()
			);
		
		if (!rv) {
			Toast.makeText(
				context,
				"Failed initial device connect.",
				Toast.LENGTH_LONG
				).show();
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
			activityCallbacks.onSensor(
				deviceType, timeStamp,
				medial, lateral, heal,
				cadence, contactTime, impactForce
				);
		}
		@Override
		public void onBattStatus(
			int deviceType, int batteryLevel,
			int maxBatteryLevel
			)
		{			
		}
		@Override
		public void onInfo(
			int deviceType, String version
			)
		{	
		}
	}
}
