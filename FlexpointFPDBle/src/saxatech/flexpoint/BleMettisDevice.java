package saxatech.flexpoint;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

public class BleMettisDevice {
	private static final String LOG_TAG = "BleMettisDevice";
	private static final boolean DEBUG = true;
	
	public static final int CONNECT_ERROR_NONE    = 0;
	public static final int CONNECT_ERROR_SERVICE = 1;
	public static final int CONNECT_ERROR_STACK   = 2;
	public static final int CONNECT_ERROR_IO      = 3;
	
	public static String connectErrorString(int connect_error) {
		switch (connect_error) {
		case CONNECT_ERROR_NONE:    return "No error";
		case CONNECT_ERROR_SERVICE: return "Failed to find service";
		case CONNECT_ERROR_STACK:   return "Android BT stack error";
		case CONNECT_ERROR_IO:      return "IO error";
		default:
			break;
		}
		return "Unknown error";
	}
	
	public interface ConnectCallback {
		public void onConnected();
		public void onDisconnected();
		public void onFailed(int connectError);
	}
	
	public interface DataCallback {
		public void onInfo(
			String version
			);
		
		public void onData (
			long timeStampNsec,
			int medial, int lateral, int heal,
			int cadence, int contactTime, int impactForce
		);
		
		public void onBattStatus(
			int battLevel, int maxBattLevel
		);
	}
		
	public BleMettisDevice( 
		Context context,
		BluetoothDevice bluetoothDevice,
		ConnectCallback connectCallback,
		DataCallback dataCallback
	)
	{
		connectParams = new ConnectParams(
			context,
			bluetoothDevice,
			connectCallback,
			dataCallback
			);
	}
	
	public boolean connect() {
		if (bluetoothGatt != null || connectParams == null)
			return false;
		
		bleDeviceGattCallback = new BleDeviceGattCallback(
			connectParams.connectCallback,
			connectParams.dataCallback
			);
		bluetoothGatt = connectParams.bluetoothDevice.connectGatt(
			connectParams.context, false, bleDeviceGattCallback
			);
		if (bluetoothGatt == null)
			return false;
		connectParams = null;
		return true;
	}
	
	public void close() {
		if (bluetoothGatt != null) {
			bluetoothGatt.close();
			bluetoothGatt = null;
		}
	}
	
	public boolean startData() {
		if (bluetoothGatt == null)
			return false;
		
		return bleDeviceGattCallback.startData();
	}
	
	public boolean setDataCharacteristic(
		byte[] data
		)
	{
		if (bluetoothGatt == null)
			return false;
		return bleDeviceGattCallback.setDataCharacteristic(
			bluetoothGatt, data
			);
	}
	
	private ConnectParams connectParams;
	private BluetoothGatt bluetoothGatt;
	private BleDeviceGattCallback bleDeviceGattCallback;
	
	private static final UUID DATA_SERVICE_UUID = 
		UUID.fromString("00006b7a-0000-1000-8000-00805f9b34fb");
	private static final UUID DATA_CHAR_UUID = 
		UUID.fromString("00006b7b-0000-1000-8000-00805f9b34fb");
	private static final UUID CCCD_UUID = 
		UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
		
	private class ConnectParams {
		private final Context context;
		private final BluetoothDevice bluetoothDevice;
		private final ConnectCallback connectCallback;
		private final DataCallback dataCallback;
				
		public ConnectParams(
			Context context,
			BluetoothDevice bluetoothDevice,
			ConnectCallback connectCallback,
			DataCallback dataCallback
			)
		{
			this.context = context;
			this.bluetoothDevice = bluetoothDevice;
			this.connectCallback = connectCallback;
			this.dataCallback = dataCallback;
		}		
	}
	
	private class BleDeviceGattCallback extends BluetoothGattCallback {
		private final ConnectCallback connectCallback;
		private final DataCallback dataCallback;
		private final BleCommandQueue commandQueue;
		private boolean setupDone;
		private boolean connected;
		
		private BluetoothGattService dataService;
		
		BleDeviceGattCallback(
			ConnectCallback connectCallback,
			DataCallback dataCallback
			)
		{
			this.connectCallback = connectCallback;
			this.dataCallback = dataCallback;
			this.commandQueue = new BleCommandQueue();
		}
		
		public boolean startData() {
			LogD("Enabling notifications");
			
			final BluetoothGattCharacteristic c =
				dataService.getCharacteristic(DATA_CHAR_UUID);
			final BluetoothGattDescriptor cccd =
				c.getDescriptor(CCCD_UUID);
			// enable notifications
						
			bluetoothGatt.setCharacteristicNotification(c, true);
			cccd.setValue(
				BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
				);
			bluetoothGatt.writeDescriptor(cccd);
			return true;
		}
		
		public void onConnectionStateChange(
			BluetoothGatt gatt, int status, int newState
			)
		{
			if (status != BluetoothGatt.GATT_SUCCESS) {
				LogE("onConnectionStateChange: " +
					newState + " failed: " + status);
				onConnectFailed(CONNECT_ERROR_IO);
				return;
			}
			if (newState == BluetoothGatt.STATE_CONNECTED) {
				LogD(gatt.getDevice().getAddress() +
					"Connected, discovering services");
				gatt.discoverServices();
			}
			else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
				LogD("Disconnected");
				onDisconnected();
			}
		}
		
		public void onServicesDiscovered(
			BluetoothGatt gatt, int status
			)
		{
			if (status != BluetoothGatt.GATT_SUCCESS) {
				LogE("onServicesDiscovered failed: " + status);
				onConnectFailed(CONNECT_ERROR_IO);
				return;
			}
			dataService = gatt.getService(
				DATA_SERVICE_UUID
				);
			if (dataService == null) {
				LogE("onServicesDiscovered failed to get dataService");
				onConnectFailed(CONNECT_ERROR_SERVICE);
				return;
			}
			
			onConnected();
		}
		
		@Override
		public void onCharacteristicRead(
			BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic,
			int status
			)
		{
			
			BleCommand command = commandQueue.get();
			if (command != null)
				command.run(gatt);			
		}
		
		public void onCharacteristicChanged(
			BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic
			)
		{	
			if (!DATA_CHAR_UUID.equals(characteristic.getUuid()))
				return;
		
			final byte[] data = characteristic.getValue();
			if (data.length != 6)
				return;
			
			final long timeStamp = System.nanoTime();
			
			dataCallback.onData(
				timeStamp,
				(int)data[0]& 0xff,
				(int)data[1]& 0xff,
				(int)data[2]& 0xff,
				(int)data[3]& 0xff,
				(int)data[4]& 0xff,
				(int)data[5]& 0xff
				);
		}
		
		@Override
		public void onCharacteristicWrite(
			BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic,
			int status
			)
		{
			if (status != BluetoothGatt.GATT_SUCCESS) {
				LogE("onCharacteristicWrite failed: " + status);				
			}
			else if (DATA_CHAR_UUID.equals(characteristic.getUuid() )) {
				LogD("DataService characteristic changed");
			}
			
			BleCommand command = commandQueue.get();
			if (command != null)
				command.run(gatt);			
		}
		@Override
		public void onDescriptorWrite (
			BluetoothGatt gatt, 
			BluetoothGattDescriptor descriptor,
			int status
			)
		{
			if (status != BluetoothGatt.GATT_SUCCESS) {
				LogE("onCharacteristicWrite failed: " + status);				
			}
			else {
				LogD("DataService descriptor changed");
			}
			
			BleCommand command = commandQueue.get();
			if (command != null)
				command.run(gatt);
		}
		
		private void onConnected() {
			setupDone = true;
			connected = true;
			connectCallback.onConnected();
		}
		private void onDisconnected() {
			connected = false;
			if (setupDone)
				connectCallback.onDisconnected();
			else
				onConnectFailed(CONNECT_ERROR_IO);
		}
		private void onConnectFailed(int connectError) {
			connected = false;
			if (!setupDone) {
				setupDone = true;
				connectCallback.onFailed(connectError);
			}
		}
		
		public boolean setDataCharacteristic(
			BluetoothGatt gatt,
			final byte[] data
			)
		{
			if (!connected)
				return false;
			
			commandQueue.put(gatt, new BleCommand() {
			public void run(
				BluetoothGatt gatt
				)
			{
				final BluetoothGattCharacteristic c =
					dataService.getCharacteristic(
						DATA_CHAR_UUID
						);
				c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
				c.setValue(data);
				gatt.writeCharacteristic(c);
			}
			});
			
			return true;
		}
	}
	
	private static interface BleCommand {
		public void run(
			BluetoothGatt gatt
			);
	}
	
	private static class BleCommandQueue {
		private class Q extends LinkedList<BleCommand> {
			public int size;			
		}
		private Q q = new Q();
		
		public void put(
			BluetoothGatt gatt,
			BleCommand command
			)
		{
			boolean runNow = false;
			synchronized(q) {
				++q.size;
				if (q.size == 1)
					runNow = true;
				else {
					q.add(command);
				}
			}
			if (runNow)
				command.run(gatt);
		}
		
		public BleCommand get()
		{
			BleCommand command = null;
			synchronized(q) {
				if (q.size == 1)
					q.size = 0;
				else {
					--q.size;
					command = q.poll();
				}
			}
			return command;
		}
	}
	
	private void LogD(String string) {
		if (DEBUG)
			Log.i(LOG_TAG, string);
	}
	private void LogE(String string) {
		Log.e(LOG_TAG, string);
	}
}
