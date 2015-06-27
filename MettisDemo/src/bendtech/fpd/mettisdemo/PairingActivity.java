package bendtech.fpd.mettisdemo;

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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class PairingActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pairing);

		if (savedInstanceState == null) {
			final Context appContext = getApplicationContext();
			final Settings settings = new Settings(appContext);
			
			final PairingFragment pairingFragment = new PairingFragment();
			pairingFragment.attachSettings(settings);
			
			getFragmentManager().beginTransaction()
				.add(R.id.container, pairingFragment).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.pairing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void nextScreen() {
		Intent intent = new Intent(this, CalibrationActivity.class);
		startActivity(intent);
		overridePendingTransition(R.anim.fade, R.anim.hold);
		finish();
	}
	
	public static class PairingFragment extends Fragment {
		private static final int BLE_SCAN_PERIOD_MS = 16000;
		
		private Handler handler;
		private Settings settings;
		private TextView textViewState;
		private ListView listViewBt;
				
		private ArrayList<String> listItems = new ArrayList<String>();
		private ArrayAdapter<String> adapter;
		
		private BluetoothAdapter bluetoothAdapter;
		private BleDeviceDiscovery bleDeviceDiscovery;
				
		private String stateText = "Scanning...";
		
		public PairingFragment() {
			handler = new Handler(Looper.getMainLooper());
		}

		public void attachSettings(Settings settings) {
			this.settings = settings;
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			
			final Context appContext = getActivity()
				.getApplicationContext();
			
			BluetoothManager bluetoothManager 
				= (BluetoothManager)appContext.getSystemService(Context.BLUETOOTH_SERVICE);
		
			bluetoothAdapter = bluetoothManager.getAdapter();
			
			if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
				Toast.makeText(
					appContext,
					"Bluetooth is not enabled",
					Toast.LENGTH_LONG
					).show();
				stateText = "Bluetooth is not enabled";
				return;
			}
			
			adapter = new ArrayAdapter<String>(
				appContext,
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
			
			bleDeviceDiscovery.start();
		}
		@Override
		public View onCreateView(
			LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState
			)
		{
			View rootView = inflater.inflate(R.layout.fragment_pairing,
				container, false);
						
			textViewState = (TextView)rootView.findViewById(R.id.textViewState);
			listViewBt = (ListView)rootView.findViewById(R.id.listViewBt);		
						
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
		public void onDestroy() {
			super.onDestroy();
			if (bleDeviceDiscovery != null)
				bleDeviceDiscovery.stop();
		}
		
		private boolean leftPaired;
		private boolean rightPaired;
		
		private final OnItemClickListener onBtAddressItemClickListener =
				new OnItemClickListener()
			{		public void onItemClick(
					AdapterView<?> parent, View view,
					int position, long id)
				{
					if (bleDeviceDiscovery.isScanning()) {
						bleDeviceDiscovery.stop();
						setStateText("Available devices:");
					}
				
					final Context context = getActivity();
					
					final String btAddress = ((TextView)view).getText().toString();
					
					if (!leftPaired && !rightPaired) {
						new AlertDialog.Builder(context)
						.setTitle("Insole selection")
						.setMessage("Select the side of the insole")
						.setPositiveButton("Left", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								leftPaired = true;
								settings.setLeftInsoleMacAddress(btAddress);
								
								listItems.remove(btAddress);
								adapter.notifyDataSetChanged();
								
								Toast.makeText(
									context,
									"Left insole paired",
									Toast.LENGTH_SHORT
									).show();
							}
						})
						.setNegativeButton("Right", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								rightPaired = true;
								settings.setRightInsoleMacAddress(btAddress);
								
								listItems.remove(btAddress);
								adapter.notifyDataSetChanged();
								
								Toast.makeText(
									context,
									"Right insole paired",
									Toast.LENGTH_SHORT
									).show();
							}
						}).create().show();
					}
					else {
						if (!leftPaired) {
							settings.setLeftInsoleMacAddress(btAddress);
							
							Toast.makeText(
								context,
								"Left insole paired",
								Toast.LENGTH_LONG
								).show();							
						}
						else {
							settings.setRightInsoleMacAddress(btAddress);
							
							Toast.makeText(
								context,
								"Right insole paired",
								Toast.LENGTH_SHORT
								).show();
						}
						
						final PairingActivity pairingActivity =
							((PairingActivity)getActivity());
						pairingActivity.nextScreen();						
					}
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

}
