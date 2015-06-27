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
import saxatech.flexpoint.BleFPDDeviceGroup;

public class InsolesFragment extends Fragment {
	public static final int DEVICE_TYPE_LEFT_SHOE  = BleFPDDeviceGroup.DEVICE_TYPE_LEFT_SHOE;
	public static final int DEVICE_TYPE_RIGHT_SHOE = BleFPDDeviceGroup.DEVICE_TYPE_RIGHT_SHOE;
	
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
	
	private Handler handler;
	
	public InsolesFragment() {
		handler = new Handler(Looper.getMainLooper());
	}
	
	private String leftInsoleMacAddress;
	private String rightInsoleMacAddress;
	private ActivityCallbacks activityCallbacks;
	private BleFPDDeviceGroup bleDevices;
	
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
		
		bleDevices = new BleFPDDeviceGroup();
		final boolean rv = bleDevices.connect(
			context,
			handler,
			bluetoothLeftShoe,
			bluetoothRightShoe,
			null,
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
	
	private class BleDevConnectCallback implements BleFPDDeviceGroup.ConnectCallback {
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
				BleFPDDeviceGroup.connectErrorString(connect_error)
				);
		}
	}
	private class BleDevDataCallback implements BleFPDDeviceGroup.DataCallback {
		@Override
		public void onData(
			int deviceType,
			long timeStamp,
			int fs0, int fs1, int fs2,
			int fs3, int fs4,
			int acX, int acY, int acZ,
			int mgX, int mgY, int mgZ
			)
		{
			if (activityCallbacks == null)
				return;
			activityCallbacks.onSensor(deviceType, timeStamp, fs0, fs1, fs2);
		}
		@Override
		public void onBatteryStatus(int deviceType, int batteryLevel,
			int maxBatteryLevel, boolean isCharging
			)
		{
			// TODO Auto-generated method stub			
		}
	}
}
