package com.flexpoint.fpd;

import java.util.ArrayList;

import saxatech.flexpoint.BleDeviceDiscovery;
import saxatech.flexpoint.BleDeviceDiscovery.DeviceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class PairFragment extends Fragment {
	private static final boolean DEBUG = true;
	private static final String LOG_TAG = "PairFragment";
	
	public static interface ActivityCallbacks {
		public void onLeftSelected(String btAddress);
		public void onRightSelected(String btAddress);
	}
	
	private ActivityCallbacks activityCallbacks;
	
	private TextView textViewDesc;
	private TextView textViewState;
	private ListView listViewBt;
	
	private ArrayList<String> listItems = new ArrayList<String>();
	private ArrayAdapter<String> adapter;
	
	private BluetoothAdapter bluetoothAdapter;
	private BleDeviceDiscovery bleDeviceDiscovery;
	private Handler handler;
	
	private String descText = "";
	private String stateText = "Scanning...";
	
	private boolean pairLeft;
	private ArrayList<String> excludedAddresses;
	
	private static final int BLE_SCAN_PERIOD_MS = 16000;
	
	public PairFragment() {		
	}
	
	public void setParameters(
		boolean pairLeft,
		ArrayList<String> excludedAddresses
		)
	{
		this.pairLeft = pairLeft;
		this.excludedAddresses = excludedAddresses;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		activityCallbacks = (ActivityCallbacks)activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		Context context = getActivity().getApplicationContext();
		
		if (pairLeft)
			descText = "Select a device to pair: LEFT SHOE";
		else
			descText = "Select a device to pair: RIGHT SHOE";
		
		BluetoothManager bluetoothManager 
		= (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
	
		bluetoothAdapter = bluetoothManager.getAdapter();
		
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			Toast.makeText(
				context,
				"Bluetooth is not enabled",
				Toast.LENGTH_LONG
				).show();
			stateText = "Bluetooth is not enabled";
			return;
		}
		
		handler = new Handler();
		
		adapter = new ArrayAdapter<String>(
			context,
			R.layout.simple_list_item_1,
			listItems
			);
		
		final BleDiscoveryCallback bleDiscoveryCallback =
			new BleDiscoveryCallback();
		bleDeviceDiscovery = new BleDeviceDiscovery(
			bluetoothAdapter,
			bleDiscoveryCallback
			);
		
		handler.postDelayed(new Runnable() {
			public void run() {
				if (bleDeviceDiscovery.isScanning()) {
					bleDeviceDiscovery.stop();
					setStateText("Available devices:");
				};
			}
		}, BLE_SCAN_PERIOD_MS);
		
		bleDeviceDiscovery.startWithExcludedAddresses(
			excludedAddresses
			);
	}
	
	@Override
	public View onCreateView(
		LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState
		)
	{
		View rootView = inflater.inflate(
			R.layout.fragment_pair, container, false
			);
				
		textViewDesc = (TextView)rootView.findViewById(R.id.textViewDesc);
		textViewState = (TextView)rootView.findViewById(R.id.textViewState);
		listViewBt = (ListView)rootView.findViewById(R.id.listViewBt);		
				
		textViewDesc.setText(descText);
		textViewState.setText(stateText);
		listViewBt.setAdapter(adapter);
		
		listViewBt.setOnItemClickListener(onBtAddressItemClickListener);
		
		return rootView;
	}
	
	private void setStateText(String text) {
		stateText = text;
		textViewState.setText(stateText);
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		activityCallbacks = null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (bleDeviceDiscovery != null)
			bleDeviceDiscovery.stop();
	}
	
	private final OnItemClickListener onBtAddressItemClickListener =
		new OnItemClickListener()
	{		public void onItemClick(
			AdapterView<?> parent, View view,
			int position, long id)
		{
			if (bleDeviceDiscovery.isScanning())
				bleDeviceDiscovery.stop();
		
			final String btAddress = ((TextView)view).getText().toString();
			
			final String message = pairLeft ?
				"Is this a left shoe?" :
				"Is this a right shoe?";
			
			new AlertDialog.Builder(getActivity())
			.setTitle("Shoe selection")
			.setMessage(message)
			.setNeutralButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					if (pairLeft)
						activityCallbacks.onLeftSelected(btAddress);
					else
						activityCallbacks.onRightSelected(btAddress);
				}
			})
			.setPositiveButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					
				}
			}).create().show();			
		}
	};
	
	private class BleDiscoveryCallback implements BleDeviceDiscovery.DiscoveryCallback {
		@Override
		public void onDeviceFound(
			String name, final String address, BluetoothDevice device)
		{
			if (!name.contains("Flexpoint"))
				return;
				
			handler.post(new Runnable() {
				public void run() {
					listItems.add(address);
					adapter.notifyDataSetChanged();
				}
			});
		}

		@Override
		public void onDevicesMatched(ArrayList<DeviceInfo> deviceInfoList) {
		}
	}
}
