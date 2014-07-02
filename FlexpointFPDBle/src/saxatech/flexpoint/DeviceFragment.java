package saxatech.flexpoint;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

public class DeviceFragment extends Fragment {	
	public static final int DEVICE_TYPE_LEFT_SHOE  = 1;
	public static final int DEVICE_TYPE_RIGHT_SHOE = 2;
	public static final int DEVICE_TYPE_CLUB       = 3;
	
	public static interface ActivityCallbacks {
		public BleFPDIdentity onFetchIdentity();
		public void onConnected();
		public void onDisconnected();
		public void onFailed(String error, int deviceType);
		
		public void onData(
			int deviceType,
			int fs0, int fs2, int fs3, int fs4, int fs5,
			int acX, int acY, int acZ,
			int mgX, int mgY, int mgZ
			);
	}
	
	private Context appContext;
	private ActivityCallbacks callbacks;
	private Handler handler;
	private BluetoothAdapter bluetoothAdapter;
	private BleFPDIdentity fpdIdentity;
	private BleFPDDevice fpdDeviceLeftShoe;
	private BleFPDDevice fpdDeviceRightShoe;
	private BleFPDDevice fpdDeviceClub;
	private boolean leftShoeConnected;
	private boolean rightShoeConnected;
	private boolean clubConnected;
	private boolean disconnected;
	private boolean logData;
	private boolean feeding;
		
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
		
		fpdIdentity = callbacks.onFetchIdentity();
		if (fpdIdentity == null) {
			failedInit("Identity is null");
			return;
		}
		
		BluetoothManager bluetoothManager =
			(BluetoothManager)appContext.getSystemService(Context.BLUETOOTH_SERVICE);
		
		bluetoothAdapter = bluetoothManager.getAdapter();
		
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			failedInit("Bluetooth is disabled");
			return;
		}
		
		long delay = 0;
		
		if (fpdIdentity.isLeftShoeEnabled()) {
			final String address = fpdIdentity.getLeftShoeAddr();
			final BluetoothDevice bluetoothDevice =
				bluetoothAdapter.getRemoteDevice(address);
			if (bluetoothDevice == null) {
				failedInit("Bluetooth device: " + address + " no longer present");
				return;
			}
			
			final BleLeftShoeConnectCallback connectCallback =
				new BleLeftShoeConnectCallback();
			final BleLeftShoeDataCallback dataCallback =
				new BleLeftShoeDataCallback();
		
			final boolean bReverseCommon = false;
			
			fpdDeviceLeftShoe = new BleFPDDevice();
			handler.postDelayed(new Runnable() {
				public void run() {
					if (disconnected)
						return;
					fpdDeviceLeftShoe.connect(
						appContext,
						bluetoothDevice, 
						connectCallback,
						dataCallback,
						bReverseCommon, // flags
						0x1f  // ex_bits
						);
				}
			}, delay);
			delay += 3000;
		}
		if (fpdIdentity.isRightShoeEnabled()) {
			final String address = fpdIdentity.getRightShoeAddr();
			final BluetoothDevice bluetoothDevice =
				bluetoothAdapter.getRemoteDevice(address);
			if (bluetoothDevice == null) {
				failedInit("Bluetooth device: " + address + " no longer present");
				return;
			}
			
			final BleRightShoeConnectCallback connectCallback =
				new BleRightShoeConnectCallback();
			final BleRightShoeDataCallback dataCallback =
				new BleRightShoeDataCallback();
			
			final boolean bReverseCommon = true;
			
			fpdDeviceRightShoe = new BleFPDDevice();
			handler.postDelayed(new Runnable() {
				public void run() {
					if (disconnected)
						return;
					fpdDeviceRightShoe.connect(
						appContext,
						bluetoothDevice, 
						connectCallback,
						dataCallback,
						bReverseCommon, // flags
						0x1f  // ex_bits
						);
				}
			}, delay);
			delay += 3000;
		}
		if (fpdIdentity.isClubEnabled()) {
			final String address = fpdIdentity.getClubAddr();
			final BluetoothDevice bluetoothDevice =
				bluetoothAdapter.getRemoteDevice(address);
			if (bluetoothDevice == null) {
				failedInit("Bluetooth device: " + address + " no longer present");
				return;
			}
			
			final BleClubConnectCallback connectCallback =
				new BleClubConnectCallback();
			final BleClubDataCallback dataCallback =
				new BleClubDataCallback();
			
			final boolean bReverseCommon = false;
			
			fpdDeviceClub = new BleFPDDevice();
			handler.postDelayed(new Runnable() {
				public void run() {
					if (disconnected)
						return;
					fpdDeviceClub.connect(
						appContext,
						bluetoothDevice, 
						connectCallback,
						dataCallback,
						bReverseCommon,    // flags
						0x1f  // ex_bits
						);
				}
			}, delay);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		stopDevices();
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		callbacks = null; //don't leak activity.
	}
	
	private void failedInit(final String error) {
		handler.post(new Runnable() {
			public void run() {
				callbacks.onFailed(error, 0);
			}
		});
	}
	
	private void stopDevices() {
		
		if (fpdDeviceLeftShoe != null)
			fpdDeviceLeftShoe.close();
		if (fpdDeviceRightShoe != null)
			fpdDeviceRightShoe.close();
		if (fpdDeviceClub != null)
			fpdDeviceClub.close();		
	}
	
	private synchronized void enableFeed(boolean enable) {		
		feeding = enable;
	}
	
	private void handleOnConnect() {
		if (callbacks == null)
			return;
		
		if (fpdDeviceLeftShoe != null)
			fpdDeviceLeftShoe.startData();
		if (fpdDeviceRightShoe != null)
			fpdDeviceRightShoe.startData();
		if (fpdDeviceClub != null)
			fpdDeviceClub.startData();
				
		callbacks.onConnected();
		enableFeed(true);
	}
				
	private class BleLeftShoeConnectCallback implements BleFPDDevice.ConnectCallback {
		public void onConnected() {
			handler.post(new Runnable() {
				public void run() {
					leftShoeConnected = true;					
					if ((rightShoeConnected || fpdDeviceRightShoe == null) &&
						(clubConnected || fpdDeviceClub == null))
					{
						handleOnConnect();
					}
				}
			});
		}
		public void onDisconnected() {
			enableFeed(false);
			
			handler.post(new Runnable() {
				public void run() {
					if (!disconnected) {
						disconnected = true;
						leftShoeConnected = false;
						stopDevices();
						if (callbacks != null)
							callbacks.onDisconnected();
					}
				}
			});
		}
		public void onFailed(final int connect_error) {
			enableFeed(false);
			
			handler.post(new Runnable() {
				public void run() {
					if (!disconnected) {
						disconnected = true;
						leftShoeConnected = false;
						stopDevices();
						if (callbacks != null) {
							callbacks.onFailed(
								BleFPDDevice.connectErrorString(connect_error),
								DEVICE_TYPE_LEFT_SHOE
								);
						}
					}
				}
			});
		}
	}
	
	private class BleRightShoeConnectCallback implements BleFPDDevice.ConnectCallback {
		public void onConnected() {
			handler.post(new Runnable() {
				public void run() {
					rightShoeConnected = true;
					if ((leftShoeConnected || fpdDeviceLeftShoe == null) &&
						(clubConnected || fpdDeviceClub == null))
					{
						handleOnConnect();
					}
				}
			});
		}
		public void onDisconnected() {
			enableFeed(false);
			
			handler.post(new Runnable() {
				public void run() {
					if (!disconnected) {
						disconnected = true;
						rightShoeConnected = false;
						stopDevices();
						if (callbacks != null)
							callbacks.onDisconnected();
					}
				}
			});
		}
		public void onFailed(final int connect_error) {
			enableFeed(false);
			
			handler.post(new Runnable() {
				public void run() {
					if (!disconnected) {
						disconnected = true;
						rightShoeConnected = false;
						stopDevices();
						if (callbacks != null) {
							callbacks.onFailed(
								BleFPDDevice.connectErrorString(connect_error),
								DEVICE_TYPE_RIGHT_SHOE
								);
						}
					}
				}
			});
		}
	}
	
	private class BleClubConnectCallback implements BleFPDDevice.ConnectCallback {
		public void onConnected() {
			handler.post(new Runnable() {
				public void run() {
					clubConnected = true;
					if ((leftShoeConnected || fpdDeviceLeftShoe == null) &&
						(rightShoeConnected || fpdDeviceRightShoe == null))
					{
						handleOnConnect();
					}
				}
			});
		}
		public void onDisconnected() {
			enableFeed(false);
			
			handler.post(new Runnable() {
				public void run() {
					if (!disconnected) {
						disconnected = true;
						clubConnected = false;
						stopDevices();
						if (callbacks != null)
							callbacks.onDisconnected();
					}
				}
			});
		}
		public void onFailed(final int connect_error) {
			enableFeed(false);
			
			handler.post(new Runnable() {
				public void run() {
					if (!disconnected) {
						disconnected = true;
						clubConnected = false;
						stopDevices();
						if (callbacks != null) {
							callbacks.onFailed(
								BleFPDDevice.connectErrorString(connect_error),
								DEVICE_TYPE_CLUB
								);
						}
					}
				}
			});
		}
	}
	
	private void onData(
		final int deviceType,
		final int fs0, final int fs2,
		final int fs3, final int fs4,
		final int fs5,
		final int acX, final int acY,
		final int acZ,
		final int mgX, final int mgY,
		final int mgZ
		)
	{
		handler.post(new Runnable() {
			public void run() {
				if (callbacks == null)
					return;
				callbacks.onData(
					deviceType,
					fs0, fs2, fs3, fs4, fs5,
					acX, acY, acZ,
					mgX, mgY, mgZ
					);
			}
		});
	}
	
	private class BleLeftShoeDataCallback implements BleFPDDevice.DataCallback {
		public void onAIOData(
			int fs0, int fs2, int fs3, int fs4, int fs5,
			int acX, int acY, int acZ,
			int mgX, int mgY, int mgZ
			)
		{
			onData(
				DEVICE_TYPE_LEFT_SHOE,
				fs0, fs2, fs3, fs4, fs5,
				acX, acY, acZ,
				mgX, mgY, mgZ
				);
				
		}
		public void onBatteryStatus(int batteryLevel, int maxBatteryLevel,
				boolean isCharging) {
			
		}
	}
	
	private class BleRightShoeDataCallback implements BleFPDDevice.DataCallback {
		public void onAIOData(
			int fs0, int fs2, int fs3, int fs4, int fs5,
			int acX, int acY, int acZ,
			int mgX, int mgY, int mgZ
			)
		{
			onData(
				DEVICE_TYPE_RIGHT_SHOE,
				fs0, fs2, fs3, fs4, fs5,
				acX, acY, acZ,
				mgX, mgY, mgZ
				);
		}
		public void onBatteryStatus(int batteryLevel, int maxBatteryLevel,
				boolean isCharging) {
			
		}
	}
	
	private class BleClubDataCallback implements BleFPDDevice.DataCallback {
		public void onAIOData(
			int fs0, int fs2, int fs3, int fs4, int fs5,
			int acX, int acY, int acZ,
			int mgX, int mgY, int mgZ
			)
		{
			onData(
				DEVICE_TYPE_CLUB,
				fs0, fs2, fs3, fs4, fs5,
				acX, acY, acZ,
				mgX, mgY, mgZ
				);
		}
		public void onBatteryStatus(int batteryLevel, int maxBatteryLevel,
				boolean isCharging) {
			
		}
	}
	
	
		
}
