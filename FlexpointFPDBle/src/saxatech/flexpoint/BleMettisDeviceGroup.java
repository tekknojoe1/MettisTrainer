package saxatech.flexpoint;

import java.io.FileNotFoundException;
import java.util.ArrayDeque;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BleMettisDeviceGroup {
	private static final String LOG_TAG = "BleMettisDeviceGroup";
	private static final boolean DEBUG = true;
	
	public static final int CONNECT_ERROR_NONE    = BleMettisDevice.CONNECT_ERROR_NONE;
	public static final int CONNECT_ERROR_SERVICE = BleMettisDevice.CONNECT_ERROR_SERVICE;
	public static final int CONNECT_ERROR_STACK   = BleMettisDevice.CONNECT_ERROR_STACK;
	public static final int CONNECT_ERROR_IO      = BleMettisDevice.CONNECT_ERROR_IO;
	
	public static String connectErrorString(int connect_error) {
		return BleMettisDevice.connectErrorString(connect_error);
	}
	
	public  static final int DEVICE_TYPE_LEFT_SHOE  = 0x1;
	public  static final int DEVICE_TYPE_RIGHT_SHOE = 0x2;
	
	public interface ConnectCallback {
		public void onConnected();
		public void onDisconnected(int deviceType);
		public void onFailed(int deviceType, int connectError);
	}
	public interface DataCallback {
		public void onInfo(
			int deviceType,
			String version
			);
		
		public void onData (
			int deviceType,
			long timeStampNsec,
			int medial, int lateral, int heal,
			int cadence, int contactTime, int impactForce
		);
		
		public void onBattStatus(
			int deviceType,
			int battLevel, int maxBattLevel
		);
	}

	private Handler handler;
	java.util.Date startDate;
	private boolean connectStarted;
	private boolean disconnected;
	private DeviceInst deviceLeft;
	private DeviceInst deviceRight;
	private BleFPDDataLogger loggerLeft;
	private BleFPDDataLogger loggerRight;
	private ArrayDeque<DeviceInst> deviceList = new ArrayDeque<DeviceInst>(3);	
	private int deviceMask;
	private int devicesOrd;
		
	public BleMettisDeviceGroup() {
		handler = new Handler(Looper.getMainLooper());
	}
	
	public boolean connect(
		Context context,
		BluetoothDevice bluetoothDeviceLeftShoe,
		BluetoothDevice bluetoothDeviceRightShoe,
		boolean logData,
		ConnectCallback connectCallback,
		DataCallback dataCallback
		)
	{
		if (connectStarted)
			return false;
		if (bluetoothDeviceLeftShoe == null &&
			bluetoothDeviceRightShoe == null)
		{
			return false;
		}
		connectStarted = true;
		
		startDate = new java.util.Date(); 
		
		if (bluetoothDeviceLeftShoe != null) {
			if (logData) {
				try {
					loggerLeft = BleFPDDataLogger.createDataLogger(
						DEVICE_TYPE_LEFT_SHOE, startDate
						);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			deviceLeft = new DeviceInst(
				DEVICE_TYPE_LEFT_SHOE,
				new BleMettisDevice(
					context,
					bluetoothDeviceLeftShoe,
					new BleDevConnectCallback(
						DEVICE_TYPE_LEFT_SHOE, connectCallback, handler
						),
					new BleDevDataCallback(
						DEVICE_TYPE_LEFT_SHOE, dataCallback, handler, loggerLeft
						)
					)
			);
			deviceList.add(deviceLeft);
			deviceMask |= DEVICE_TYPE_LEFT_SHOE;
		}
		if (bluetoothDeviceRightShoe != null) {
			if (logData) {
				try {
					loggerRight = BleFPDDataLogger.createDataLogger(
						DEVICE_TYPE_RIGHT_SHOE, startDate
						);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			deviceRight = new DeviceInst(
				DEVICE_TYPE_RIGHT_SHOE,
				new BleMettisDevice(
					context,
					bluetoothDeviceRightShoe,
					new BleDevConnectCallback(
						DEVICE_TYPE_RIGHT_SHOE, connectCallback, handler
						),
					new BleDevDataCallback(
						DEVICE_TYPE_RIGHT_SHOE, dataCallback, handler, loggerRight
						)
					)
			);
			deviceList.add(deviceRight);
			deviceMask |= DEVICE_TYPE_RIGHT_SHOE;
		}
		return true;
	}
	
	public void startData() {
		if (deviceLeft != null)
			deviceLeft.mettisDevice.startData();
		if (deviceRight != null)
			deviceRight.mettisDevice.startData();
	}
	public void close() {
		if (loggerLeft != null)
			loggerLeft.close();
		if (loggerRight != null)
			loggerRight.close();
		
		if (deviceLeft != null) {
			deviceLeft.mettisDevice.close();
			deviceLeft = null;
		}
		if (deviceRight != null) {
			deviceRight.mettisDevice.close();
			deviceRight = null;
		}
	}
		
	private static class DeviceInst {
		public int deviceType;       
		public BleMettisDevice mettisDevice;
		
		public DeviceInst(
			int deviceType, BleMettisDevice mettisDevice
			)
		{
			this.deviceType   = deviceType;
			this.mettisDevice = mettisDevice;
		}
	}
	private class BleDevConnectCallback implements BleMettisDevice.ConnectCallback {
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
					if (!devInst.mettisDevice.connect()) {
						connectCallback.onFailed(
							devInst.deviceType, CONNECT_ERROR_STACK
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
	
	private class BleDevDataCallback implements BleMettisDevice.DataCallback {
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
		public synchronized void onData (
				final long timeStampNsec,
				final int medial, final int lateral,
				final int heal,	final int cadence, 
				final int contactTime, final int impactForce
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
						timeStampNsec,
						medial, lateral, heal,
						cadence, contactTime,
						impactForce
						);
				}
			});
			if (logger != null) {
				logger.writeData(
					0,
					medial, lateral, heal,
					cadence, contactTime,
					impactForce, 0, 0,
					0, 0, 0
					);
			}
		}

		@Override
		public void onBattStatus(
				int battLevel, int maxBattLevel
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
	
}
