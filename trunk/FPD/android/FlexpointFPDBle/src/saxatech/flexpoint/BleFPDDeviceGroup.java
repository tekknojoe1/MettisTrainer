package saxatech.flexpoint;

import java.io.FileNotFoundException;
import java.util.ArrayDeque;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class BleFPDDeviceGroup {
	private static final String LOG_TAG = "BleFPDDeviceGroup";
	private static final boolean DEBUG = true;
	
	public  static final int DEVICE_TYPE_LEFT_SHOE  = 0x1;
	public  static final int DEVICE_TYPE_RIGHT_SHOE = 0x2;
	public  static final int DEVICE_TYPE_CLUB       = 0x4;
	
	public static final int CONNECT_ERROR_NONE      = BleFPDDevice.CONNECT_ERROR_NONE;
	public static final int CONNECT_STACK_ERROR     = BleFPDDevice.CONNECT_ERROR_STACK;
	public static final int CONNECT_ERROR_IO        = BleFPDDevice.CONNECT_ERROR_IO;
	public static final int CONNECT_ERROR_BAD_STATE = BleFPDDevice.CONNECT_ERROR_BAD_STATE; 
	
	public static String connectErrorString(int connect_error) {
		return BleFPDDevice.connectErrorString(connect_error);
	}
	
	public interface ConnectCallback {
		public void onConnected();
		public void onDisconnected(int deviceType);
		public void onFailed(int deviceType, int connect_error);
	}
	public interface DataCallback {
		public void onData(
			int deviceType,
			long timeStampNsec,
			int fs0, int fs1, int fs2, int fs3, int fs4,
			int acX, int acY, int acZ,
			int mgX, int mgY, int mgZ
			);
		public void onBatteryStatus(
			int deviceType,
			int batteryLevel, int maxBatteryLevel,
			boolean isCharging
			);
	}
		
	public boolean connect(
		Context context,
		Handler handler,
		BluetoothDevice bluetoothDeviceLeftShoe,
		BluetoothDevice bluetoothDeviceRightShoe,
		BluetoothDevice bluetoothDeviceClub,
		boolean         logData,
		ConnectCallback connectCallback,		
		DataCallback    dataCallback
		)
	{
		if (connectStarted)
			return false;
		if (bluetoothDeviceLeftShoe  == null &&
			bluetoothDeviceRightShoe == null &&
			bluetoothDeviceClub      == null)
		{
			return false;
		}
		connectStarted = true;
		
		startDate = new java.util.Date(); 
		
		if (bluetoothDeviceLeftShoe != null) {
			if (logData) {
				try {
					loggerLeftShoe = BleFPDDataLogger.createDataLogger(
						DEVICE_TYPE_LEFT_SHOE, startDate
						);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			deviceLeftShoe = new DeviceInst(
				DEVICE_TYPE_LEFT_SHOE,
				new BleFPDDevice(
					context,
					bluetoothDeviceLeftShoe,
					new BleDevConnectCallback(
						DEVICE_TYPE_LEFT_SHOE, connectCallback, handler
						),
					new BleDevDataCallback(
						DEVICE_TYPE_LEFT_SHOE, dataCallback, handler, loggerLeftShoe
						),
					false, //reverseCommon = false
					0x1f  // ex_bits
					)
			);
			deviceList.add(deviceLeftShoe);
			deviceMask |= DEVICE_TYPE_LEFT_SHOE;
		}
		if (bluetoothDeviceRightShoe != null) {
			if (logData) {
				try {
					loggerRightShoe = BleFPDDataLogger.createDataLogger(
						DEVICE_TYPE_RIGHT_SHOE, startDate
						);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			deviceRightShoe = new DeviceInst(
				DEVICE_TYPE_RIGHT_SHOE,
				new BleFPDDevice(
					context,
					bluetoothDeviceRightShoe,
					new BleDevConnectCallback(
						DEVICE_TYPE_RIGHT_SHOE, connectCallback, handler
						),
					new BleDevDataCallback(
						DEVICE_TYPE_RIGHT_SHOE, dataCallback, handler, loggerRightShoe
						),
					true, //reverseCommon = true
					0x1f  // ex_bits
					)
			);
			deviceList.add(deviceRightShoe);
			deviceMask |= DEVICE_TYPE_RIGHT_SHOE;
		}
		if (bluetoothDeviceClub != null) {
			if (logData) {
				try {
					loggerClub = BleFPDDataLogger.createDataLogger(
						DEVICE_TYPE_CLUB, startDate
						);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			deviceClub = new DeviceInst(
				DEVICE_TYPE_CLUB,
				new BleFPDDevice(
					context,
					bluetoothDeviceClub,
					new BleDevConnectCallback(
						DEVICE_TYPE_CLUB, connectCallback, handler
						),
					new BleDevDataCallback(
						DEVICE_TYPE_CLUB, dataCallback, handler, loggerClub
						),
					true, //reverseCommon = true
					0x1f  // ex_bits
					)
			);
			deviceList.add(deviceClub);
			deviceMask |= DEVICE_TYPE_CLUB;
		}
			
		return deviceList.remove().fpdDevice.connect();
	}
	
	public void startData() {
		if (deviceLeftShoe != null)
			deviceLeftShoe.fpdDevice.startData();
		if (deviceRightShoe != null)
			deviceRightShoe.fpdDevice.startData();
		if (deviceClub != null)
			deviceClub.fpdDevice.startData();
	}
	public void close() {
		if (loggerLeftShoe != null)
			loggerLeftShoe.close();
		if (loggerRightShoe != null)
			loggerRightShoe.close();
		if (loggerClub != null)
			loggerClub.close();
		
		if (deviceLeftShoe != null) {
			deviceLeftShoe.fpdDevice.close();
			deviceLeftShoe = null;
		}
		if (deviceRightShoe != null) {
			deviceRightShoe.fpdDevice.close();
			deviceRightShoe = null;
		}
		if (deviceClub != null) {
			deviceClub.fpdDevice.close();
			deviceClub = null;
		}
	}
	
	java.util.Date startDate;
	private boolean connectStarted;
	private boolean disconnected;
	private DeviceInst deviceLeftShoe;
	private DeviceInst deviceRightShoe;
	private DeviceInst deviceClub;
	private BleFPDDataLogger loggerLeftShoe;
	private BleFPDDataLogger loggerRightShoe;
	private BleFPDDataLogger loggerClub;
	private ArrayDeque<DeviceInst> deviceList = new ArrayDeque<DeviceInst>(3);	
	private int deviceMask;
	private int devicesOrd;
	
	private static class DeviceInst {
		public int deviceType;       
		public BleFPDDevice fpdDevice;
		
		public DeviceInst(
			int deviceType, BleFPDDevice fpdDevice
			)
		{
			this.deviceType = deviceType;
			this.fpdDevice  = fpdDevice;
		}
	}
	
	private class BleDevConnectCallback implements BleFPDDevice.ConnectCallback {
		private final int deviceType;
		private final ConnectCallback connectCallback;
		private final Handler handler;
		
		public BleDevConnectCallback(
			int deviceType,
			ConnectCallback connectCallback,
			Handler handler
			)
		{
			this.deviceType = deviceType;
			this.connectCallback = connectCallback;
			this.handler = handler;
		}
		
		@Override
		public void onConnected() {
			handler.post(new Runnable() {
				public void run() {
					if (disconnected)
						return;
					
					LogD("Connected " + deviceType);					
					
					DeviceInst devInst = deviceList.poll();
					if (devInst == null) {
						// all devices connected
						connectCallback.onConnected();
						return;
					}
					if (!devInst.fpdDevice.connect()) {
						connectCallback.onFailed(
							devInst.deviceType, CONNECT_STACK_ERROR
							);
					}
				}
			});
		}

		@Override
		public void onDisconnected() {
			handler.post(new Runnable() {
				public void run() {
					disconnected = true;
					deviceList.clear();
					connectCallback.onDisconnected(deviceType);
				}
			});
		}

		@Override
		public void onFailed(final int connect_error) {
			handler.post(new Runnable() {
				public void run() {
					disconnected = true;
					deviceList.clear();
					connectCallback.onFailed(deviceType, connect_error);
				}
			});
		}
		
	}
	private class BleDevDataCallback implements BleFPDDevice.DataCallback {
		private final int deviceType;
		private final DataCallback dataCallback;
		private final Handler handler;
		private final BleFPDDataLogger logger;
		
		public BleDevDataCallback(
			int deviceType,
			DataCallback dataCallback,
			Handler handler,
			BleFPDDataLogger logger
			)
		{
			this.deviceType = deviceType;
			this.dataCallback = dataCallback;
			this.handler = handler;
			this.logger = logger;
		}
		@Override
		public synchronized void onAIOData(
			final long timeStamp,
			final int fs0, final int fs1, final int fs2,
			final int fs3, final int fs4,
			final int acX, final int acY, final int acZ,
			final int mgX, final int mgY, final int mgZ
			)
		{
			if (devicesOrd != deviceMask) {
				switch (deviceType) {
				case DEVICE_TYPE_LEFT_SHOE:
					devicesOrd |= DEVICE_TYPE_LEFT_SHOE;
					break;
				case DEVICE_TYPE_RIGHT_SHOE:
					devicesOrd |= DEVICE_TYPE_RIGHT_SHOE;
					break;
				case DEVICE_TYPE_CLUB:
					devicesOrd |= DEVICE_TYPE_CLUB;
					break;
				}
				if (devicesOrd != deviceMask)
					return;
			}
			
			handler.post(new Runnable() {
				public void run() {
					if (disconnected)
						return;
										
					dataCallback.onData(
						deviceType,
						timeStamp,
						fs0, fs1, fs2, fs3, fs4,
						acX, acY, acZ,
						mgX, mgY, mgZ
						);
				}
			});
			if (logger != null) {
				logger.writeData(
					0,
					fs0, fs1, fs2, fs3, fs4,
					acX, acY, acZ,
					mgX, mgY, mgZ
					);
			}
		}

		@Override
		public void onBatteryStatus(
			int batteryLevel, int maxBatteryLevel,
			boolean isCharging
			)
		{
			
		}
		@Override
		public void onInfo(String versionString) {			
			
		}
	}
	
	private void LogD(String string) {
		if (DEBUG)
			Log.i(LOG_TAG, string);
	}
	//private void LogE(String string) {
	//	Log.e(LOG_TAG, string);
	//}
} 
