package saxatech.flexpoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class BleDeviceDiscovery {
	
	public static class DeviceInfo {
		public String name;
		public String address;
		public BluetoothDevice device;		
	}
	public interface DiscoveryCallback {
		public void onDeviceFound(
			String name, String address, BluetoothDevice device
			);
		public void onDevicesMatched(
			ArrayList<DeviceInfo> deviceInfoList
			);
	}
	
	private final BluetoothAdapter bluetoothAdapter;
	private final DiscoveryCallback discoveryCallback;
	private boolean isScanning;
	private BleScan bleScan;
	
	public BleDeviceDiscovery(
		BluetoothAdapter bluetoothAdapter,
		DiscoveryCallback discoveryCallback
		)
	{
		this.bluetoothAdapter = bluetoothAdapter;
		this.discoveryCallback = discoveryCallback;
	}
		
	public boolean start() {
		if (isScanning)
			return false;
		
		if (isScanning)
			return false;
		
		bleScan = new BleScan();
		isScanning = bluetoothAdapter.startLeScan(bleScan);
		return isScanning;
	}
	public boolean start(ArrayList<String> bluetoothAddressMatchList) {
		if (isScanning)
			return false;
		
		bleScan = new BleScan(bluetoothAddressMatchList);
		isScanning = bluetoothAdapter.startLeScan(bleScan);
		return isScanning;
	}
	public boolean startWithExcludedAddresses(
		ArrayList<String> excludedAddresses
		)
	{
		if (isScanning)
			return false;
		
		bleScan = new BleScan();
		bleScan.setExcludedAddresses(excludedAddresses);
		isScanning = bluetoothAdapter.startLeScan(bleScan);
		return isScanning;
	}
	
	public void stop() {
		if (!isScanning)
			return;
		
		bluetoothAdapter.stopLeScan(bleScan);
		isScanning = false;
	}
	public boolean isScanning() {
		return isScanning;
	}
	
	private class BleScan implements BluetoothAdapter.LeScanCallback {		
		private final Set<String> foundAddressMap = new HashSet<String>();
		private final Set<String> matchedAddressMap = new HashSet<String>();
		private final Set<String> ignoredAddressMap = new HashSet<String>();
		private final ArrayList<DeviceInfo> deviceInfoList = new ArrayList<DeviceInfo>();
		private boolean allDevicesMatched; 
		
		public BleScan() {			
		}
		public BleScan(
			ArrayList<String> bluetoothAddressMatchList
			)
		{
			for (String address : bluetoothAddressMatchList)
				matchedAddressMap.add(address);
		}
		
		public void setExcludedAddresses(
			ArrayList<String> ignoredAddresses
			)
		{
			for (String address : ignoredAddresses)
				ignoredAddressMap.add(address);
		}
				
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			if (allDevicesMatched)
				return;
			
			final String name = device.getName();
			final String address = device.getAddress();
			
			if (foundAddressMap.add(address)) {				
				if (!ignoredAddressMap.contains(address)) {				
					discoveryCallback.onDeviceFound(
						name, address, device
					);
				}
			}
			if (matchedAddressMap.contains(address)) {
				DeviceInfo deviceInfo = new DeviceInfo();
				deviceInfo.name    = name;
				deviceInfo.address = address;
				deviceInfo.device  = device;
				deviceInfoList.add(deviceInfo);
				
				matchedAddressMap.remove(address);
				if (matchedAddressMap.isEmpty()) {
					allDevicesMatched = true;
					discoveryCallback.onDevicesMatched(deviceInfoList);
				}
			}
		}
	}
}
