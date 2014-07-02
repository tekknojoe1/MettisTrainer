package saxatech.flexpoint;

import java.util.UUID;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

public class BleFPDDevice {
	private static final String LOG_TAG = "BleFPDDevice";
	private static final boolean DEBUG = true;
	
	public static final int CONNECT_ERROR_NONE      = 0;
	public static final int CONNECT_ERROR_STACK     = 1;
	public static final int CONNECT_ERROR_IO        = 6;
	public static final int CONNECT_ERROR_BAD_STATE = 7;
	
	public static String connectErrorString(int connect_error) {
		switch (connect_error) {
		case CONNECT_ERROR_NONE:      return new String("No error");
		case CONNECT_ERROR_STACK:     return new String("Android BT stack error");
		case CONNECT_ERROR_IO:        return new String("IO error");
		case CONNECT_ERROR_BAD_STATE: return new String("Invalid hardware state");
		default:
			break;
		}
		return new String("Unknown error");
	}
	
	public interface ConnectCallback {
		public void onConnected();
		public void onDisconnected();
		public void onFailed(int connect_error);
	}
	
	public interface DataCallback {
		public void onAIOData(
			int fs0, int fs1, int fs2, int fs3, int fs4,
			int acX, int acY, int acZ,
			int mgX, int mgY, int mgZ
			);
		
		public void onBatteryStatus(
			int batteryLevel, int maxBatteryLevel,
			boolean isCharging
			);
	}
	
	public BleFPDDevice() {		
	}
	public BleFPDDevice(
		Context context,
		BluetoothDevice bluetoothDevice,
		ConnectCallback connectCallback,
		DataCallback dataCallback,
		boolean reverseCommon,
		int psocBits
		)
	{
		cachedConnectParams = new CachedConnectParams(
			context,
			bluetoothDevice,
			connectCallback,
			dataCallback,
			reverseCommon,
			psocBits
			);
	}
			
	public boolean connect(
		Context context,
		BluetoothDevice bluetoothDevice,
		ConnectCallback connectCallback,
		DataCallback dataCallback,
		boolean reverseCommon,
		int psocBits
		)
	{
		if (bluetoothGatt != null)
			return false;
				
		this.psocBits  = psocBits;
		this.psocFlags = 0;
		
		if (reverseCommon)
			this.psocFlags |= PSOC_FLAG_REVERSE_COMMON;
		
		bleDeviceGattCallback = new BleDeviceGattCallback(
			connectCallback,
			dataCallback
			);
		
		bluetoothGatt = bluetoothDevice.connectGatt(
			context, false, bleDeviceGattCallback
			);
		return bluetoothGatt != null;
	}
	
	public boolean connect() {
		if (bluetoothGatt != null || cachedConnectParams == null)
			return false;
		
		this.psocBits = cachedConnectParams.psocBits;
		this.psocFlags = 0;
		
		if (cachedConnectParams.reverseCommon)
			this.psocFlags |= PSOC_FLAG_REVERSE_COMMON;
		
		bleDeviceGattCallback = new BleDeviceGattCallback(
			cachedConnectParams.connectCallback,
			cachedConnectParams.dataCallback
			);
		
		bluetoothGatt = cachedConnectParams.bluetoothDevice.connectGatt(
			cachedConnectParams.context, false, bleDeviceGattCallback
			);
		cachedConnectParams = null;
		return bluetoothGatt != null;
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
	
	private BluetoothGatt bluetoothGatt;
	private BleDeviceGattCallback bleDeviceGattCallback;
	private CachedConnectParams cachedConnectParams;
	private int psocFlags;
	private int psocBits;
	private int sampleRate = 10;
	private static final int PSOC_FLAG_REVERSE_COMMON = 0x1;
	
	private class CachedConnectParams {
		public final Context context;
		public final BluetoothDevice bluetoothDevice;
		public final ConnectCallback connectCallback;
		public final DataCallback dataCallback;
		public final boolean reverseCommon;
		public final int psocBits;
		
		CachedConnectParams(
			Context context,
			BluetoothDevice bluetoothDevice,
			ConnectCallback connectCallback,
			DataCallback dataCallback,
			boolean reverseCommon,
			int psocBits
			)
		{
			this.context = context;
			this.bluetoothDevice = bluetoothDevice;
			this.connectCallback = connectCallback;
			this.dataCallback = dataCallback;
			this.reverseCommon = reverseCommon;
			this.psocBits = psocBits;
		}
	}
	
	private static final UUID serviceUUID =
		BleFPDUuids.serviceUUID;	
	
	private static final UUID characteristicStatusUUID =
		BleFPDUuids.charStatusUUID;
	
	private static final UUID characteristicConfigUUID =
		BleFPDUuids.charConfigUUID;
	
	private static final UUID characteristicLogDataUUID =
		BleFPDUuids.charLogDataUUID;
	
	private class BleDeviceGattCallback extends BluetoothGattCallback {
		private final ConnectCallback connectCallback;
		private final DataCallback dataCallback;
		private boolean setupDone;
		private boolean dataReady;
		
		private static final int maxBatteryLevel = 100;
		
		BleDeviceGattCallback(
			ConnectCallback connectCallback,
			DataCallback dataCallback
			)
		{
			this.connectCallback = connectCallback;
			this.dataCallback = dataCallback;
		}
				
		public boolean startData() {
			if (!dataReady)
				return false;
		
			final BluetoothGattCharacteristic c =
				bluetoothGattService.getCharacteristic(
					characteristicLogDataUUID
					);
			
			return bluetoothGatt.setCharacteristicNotification(c, true);
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
		
		private BluetoothGattService bluetoothGattService;
		
		public void onServicesDiscovered(
			BluetoothGatt gatt, int status
			)
		{
			if (status != BluetoothGatt.GATT_SUCCESS) {
				LogE("onServicesDiscovered failed: " + status);
				onConnectFailed(CONNECT_ERROR_IO);
				return;
			}
			bluetoothGattService = gatt.getService(serviceUUID);
			if (bluetoothGattService == null) {
				LogE("onServicesDiscovered failed to get service");
				onConnectFailed(CONNECT_ERROR_IO);
				return;
			}
			LogD("services discovered, start setup");
			startSetup(gatt);
		}
		
		private final static int STATE_SET_S0_GAIN   = 1;
		private final static int STATE_SET_S1_GAIN   = 2;
		private final static int STATE_SET_S2_GAIN   = 3;
		private final static int STATE_SETUP_SENSORS = 4;
		private final static int STATE_DONE          = 5;
				
		private final static int APP_CFG_AIO = 6;
		private final static int APP_CFG_REG = 7;
		
		private final static int APP_LOG_ENABLE_DIRECT = 9; // uint8 (1, 0, 0, 1)
		private final static int APP_SEN_ACL  = 0;
		private final static int APP_SEN_MAG  = 1;
		private final static int APP_SEN_PSOC = 2;
		
		private final static int APP_PSOC_S0_REG_OFFSET = 0x80;
		private final static int APP_PSOC_S0_REG_GAIN   = 0x81;
		private final static int APP_PSOC_S1_REG_OFFSET = 0x82;
		private final static int APP_PSOC_S1_REG_GAIN   = 0x83;
		private final static int APP_PSOC_S2_REG_OFFSET = 0x84;
		private final static int APP_PSOC_S2_REG_GAIN   = 0x85;
		
		private final static int APP_DEF_GAIN = 10;
		
		private int state;
		
		private void startSetup(BluetoothGatt gatt) {
			try {
				Thread.sleep(600);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			
			LogD("start setup");
			state = STATE_SET_S0_GAIN;
			setupGain(gatt, APP_PSOC_S0_REG_GAIN, APP_DEF_GAIN);
			//setupSensors(gatt);		
		}
		
		private void setupSensors(BluetoothGatt gatt) {
			final byte[] aio_packet = new byte[20];
			aio_packet[0]  = (byte)APP_CFG_AIO;
			aio_packet[1]  = (byte)APP_LOG_ENABLE_DIRECT;
			// accel APPConfigSensor
			aio_packet[2]  = (byte)APP_SEN_ACL; //accel sensor id
			aio_packet[3]  = (byte)00;   // accel rate LB 
			aio_packet[4]  = (byte)00;   // accel rate HB
			aio_packet[5]  = (byte)10;   // accel res 10-bit
			aio_packet[6]  = (byte)0;    // accel flags
			aio_packet[7]  = (byte)0x3;  // accel ex_bits: 3 = x,y,z
			// mag APPConfigSensor
			aio_packet[8]  = (byte)APP_SEN_MAG; //accel sensor id
			aio_packet[9]  = (byte)00;   // accel rate LB 
			aio_packet[10] = (byte)00;   // accel rate HB
			aio_packet[11] = (byte)10;   // accel res 10-bit
			aio_packet[12] = (byte)0;    // accel flags
			aio_packet[13] = (byte)0x3;  // accel ex_bits: 3 = x,y,z
			// psoc APPConfigSensor
			aio_packet[14] = (byte)APP_SEN_PSOC; //accel sensor id
			aio_packet[15] = (byte)(sampleRate & 0xff); // sample time LB 
			aio_packet[16] = (byte)((sampleRate >> 8) & 0xff); // sample time HB
			aio_packet[17] = (byte)8;    // accel res 8-bit
			aio_packet[18] = (byte)psocFlags; // accel flags: 0x1 = reverse common
			aio_packet[19] = (byte)psocBits;  // accel ex_bits: 5 = all sensor lines
						
			final BluetoothGattCharacteristic c =
				bluetoothGattService.getCharacteristic(
					characteristicConfigUUID
					);
			c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
			c.setValue(aio_packet);
			gatt.writeCharacteristic(c);
		}
		
		private void setupGain(BluetoothGatt gatt, int reg, int value) {
			final byte[] reg_packet = new byte[5];
			reg_packet[0] = (byte)APP_CFG_REG;
			reg_packet[1] = (byte)APP_SEN_PSOC;
			reg_packet[2] = (byte)reg;
			reg_packet[3] = (byte)(value & 0xff);
			reg_packet[4] = (byte)((value >> 8) & 0xff);
			
			final BluetoothGattCharacteristic c =
				bluetoothGattService.getCharacteristic(
					characteristicConfigUUID
					);
			c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
			c.setValue(reg_packet);
			gatt.writeCharacteristic(c);	
		}
		
		private static final int FW_GATT_INVALID_STATE = 0x81;		
		
		public void onCharacteristicWrite(
			BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic, int status
			)
		{
			if (status != BluetoothGatt.GATT_SUCCESS) {
				LogE("onCharacteristicWrite failed: " + status);
				final int updateError = status == FW_GATT_INVALID_STATE ?
					CONNECT_ERROR_BAD_STATE : CONNECT_ERROR_IO;
				onConnectFailed(updateError);
				return;
			}
			if (!characteristicConfigUUID.equals(characteristic.getUuid()))
				return;
			
			switch (state) {
			case STATE_SET_S0_GAIN:
				LogD("STATE_SET_S0_GAIN - ok");
				state = STATE_SET_S1_GAIN;
				setupGain(gatt, APP_PSOC_S1_REG_GAIN, APP_DEF_GAIN);
				break;
			case STATE_SET_S1_GAIN:
				LogD("STATE_SET_S1_GAIN - ok");
				state = STATE_SET_S2_GAIN;
				setupGain(gatt, APP_PSOC_S2_REG_GAIN, APP_DEF_GAIN);
				break;
			case STATE_SET_S2_GAIN:
				LogD("STATE_SET_S2_GAIN - ok");
				state = STATE_SETUP_SENSORS;
				setupSensors(gatt);
				break;
			case STATE_SETUP_SENSORS:
				LogD("STATE_SETUP_SENSORS - ok");
				state = STATE_DONE;				
				onConnected();				
				break;
			case STATE_DONE:
				break;
			}
		}
				
		private long startTime;
				
		public void onCharacteristicChanged(
			BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic
			)
		{
			// FIXME: try this in a thread instead...			
			boolean readStatus = false;
			if (startTime == 0) {
				startTime = System.nanoTime();
				readStatus = true;
			} else {
				final long endTime = System.nanoTime();
				final double elapsedSecs =
					((double)endTime-startTime) / 1000000000.0; 
				if (elapsedSecs > 20.0) {
					startTime = endTime;
					readStatus = true;
				}
			}
			if (readStatus) {
				BluetoothGattCharacteristic c =
					bluetoothGattService.getCharacteristic(
						characteristicStatusUUID
						);
				
				gatt.readCharacteristic(c);
			}
			
			if (!characteristicLogDataUUID.equals(characteristic.getUuid()))
				return;
			
			
			final byte[] data = characteristic.getValue();
				
			int pos = 0;
			int length = data.length;
			
			while (length > 0) {
				// get the packet set size
				final int setSize = ((data[pos] >> 4) & 0x0f) + 2; //includes header
				// check the packet type (3 == all, but just sensors now).
				if ((data[pos] & 0x0f) == 3) {
					if (setSize > length)
						break;
					
					process8BitSensor(data, pos, setSize);
				}
				pos    += setSize;
				length -= setSize;
			}
		}
		
		private int[] sensor_buf = new int[5];
		
		private void process8BitSensor(byte[] data, int offset, int length) {
			// remove the header
			++offset;
			--length;
			
			final int min = (length < sensor_buf.length) ?
				length : sensor_buf.length;
			int i=0;
			for (; i < min; ++i) {
				sensor_buf[i] = (int)data[offset+i] & 0xff; 
			}
			for (; i < sensor_buf.length; ++i) {
				sensor_buf[i] = 0;
			}
			dataCallback.onAIOData(
				sensor_buf[0],
				sensor_buf[1],
				sensor_buf[2],
				sensor_buf[3],
				sensor_buf[4],
				0,0,0,0,0,0
				);
		}
		
		public void onCharacteristicRead(
			BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic, int status
			)
		{
			if (status != BluetoothGatt.GATT_SUCCESS) {
				LogE("onCharacteristicRead failed: " + status);
				return;
			}
			if (!characteristicStatusUUID.equals(characteristic.getUuid()))
				return;
			
			final byte[] response = characteristic.getValue();
			final int batteryLevel   = (int)response[4] & 0xff;
			final boolean isCharging = ((int)response[5] & 0x02) == 0x02;
			
			dataCallback.onBatteryStatus(
				batteryLevel, maxBatteryLevel, isCharging
				);
		}
				
		private void onConnected() {
			setupDone = true;
			dataReady = true;
			connectCallback.onConnected();
		}
		private void onDisconnected() {
			dataReady = false;
			if (!setupDone)
				connectCallback.onFailed(CONNECT_ERROR_IO);
			else
				connectCallback.onDisconnected();
		}
		private void onConnectFailed(int connectError) {			
			if (!setupDone) {
				setupDone = true;
				connectCallback.onFailed(connectError);
			}
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
