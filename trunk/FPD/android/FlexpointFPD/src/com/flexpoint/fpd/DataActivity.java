package com.flexpoint.fpd;

import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import saxatech.flexpoint.BleFPDDeviceGroup;
import saxatech.flexpoint.BleFPDIdentity;

public class DataActivity extends Activity
	implements DeviceFragment.ActivityCallbacks
{

	private DeviceFragment deviceFragment;
	private DataActivityFragment dataActivityFragment;
	private static final String DEVICE_FRAGMENT = "device_fragment";
	private Timer updateTimer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data);

		FragmentManager fm = getFragmentManager();
		
		dataActivityFragment = (DataActivityFragment)
			fm.findFragmentById(R.id.container);
		
		if (dataActivityFragment == null) {
			dataActivityFragment = new DataActivityFragment();
			fm.beginTransaction()
				.add(R.id.container, dataActivityFragment)
				.commit();
		}
		
		deviceFragment = (DeviceFragment)
			fm.findFragmentByTag(DEVICE_FRAGMENT);
		
		if (deviceFragment == null) {
			deviceFragment = new DeviceFragment();
			Bundle b = getIntent().getExtras();
			BleFPDIdentity identity = b.getParcelable("Identity");
			deviceFragment.setDevices(identity);
			
			fm.beginTransaction()
				.add(deviceFragment, DEVICE_FRAGMENT)
				.commit();
		}
		
		updateTimer = new Timer();
		updateTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				dataActivityFragment.updateUi();
			}
		}, 100, 50);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.data, menu);
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
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class DataActivityFragment extends Fragment {
		private TextView textViewStatus;
		private FootView footView;
		private BarView  barView;
		private String statusText = "Waiting for devices....";
				
		SensorProcessor sensorProcessor = new SensorProcessor();
		
		public DataActivityFragment() {
		}

		@Override
		public View onCreateView(
			LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState
			)
		{
			View rootView = inflater.inflate(R.layout.fragment_data, container,
					false);
			textViewStatus = (TextView)rootView.findViewById(R.id.deviceStatus);
			textViewStatus.setText(statusText);
			footView = (FootView)rootView.findViewById(R.id.footView1);
			barView = (BarView)rootView.findViewById(R.id.barView1);
			return rootView;
		}
		public void setDeviceStatusText(String text) {
			statusText = text;
			textViewStatus.setText(statusText);
		}
		public void setLeftSensors(int fs0, int fs1, int fs2) {
			sensorProcessor.setLeftSensors(fs0, fs1, fs2);
			
			footView.setLeftSensors(
				sensorProcessor.left_fs0,
				sensorProcessor.left_fs1,
				sensorProcessor.left_fs2
				);
			barView.setLeftValue(sensorProcessor.left_sum);
		}
		public void setRightSensors(int fs0, int fs1, int fs2) {
			sensorProcessor.setRightSensors(fs0, fs1, fs2);
			
			footView.setRightSensors(
				sensorProcessor.right_fs0,
				sensorProcessor.right_fs1,
				sensorProcessor.right_fs2
				);			
			barView.setRightValue(sensorProcessor.right_sum);
		}
		public void updateUi() {
			if (footView != null)
				footView.postInvalidate();
			if (barView != null)
				barView.postInvalidate();
		}
	}
	
	

	@Override
	public void onConnected() {
		// TODO Auto-generated method stub
		Log.i("DATA_ACTIVITY", "+++++onConnected");
		dataActivityFragment.setDeviceStatusText("connected");		
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		Log.i("DATA_ACTIVITY", "+++++onDisconnected");
		dataActivityFragment.setDeviceStatusText("Disconnected");
	}

	@Override
	public void onFailed(String error) {
		// TODO Auto-generated method stub
		Log.i("DATA_ACTIVITY", "+++++onFailed: " + error);
		dataActivityFragment.setDeviceStatusText(error);
	}

	@Override
	public void onSensor(int deviceType, int fs0, int fs1, int fs2) {
		//Log.i("DATA_ACTIVITY", "+++ data: " + deviceType);
		if (deviceType == BleFPDDeviceGroup.DEVICE_TYPE_LEFT_SHOE) {
			dataActivityFragment.setLeftSensors(fs0, fs1, fs2);
		}
		else if (deviceType == BleFPDDeviceGroup.DEVICE_TYPE_RIGHT_SHOE) {
			dataActivityFragment.setRightSensors(fs0, fs1, fs2);
		}
	}
	
	/*
	private static class Packet {
		int deviceType;
		int fs0; int fs1; int fs2; int fs3;	int fs4;
		int acX; int acY; int acZ; int mgX; int mgY; int mgZ;
	}
	private static class PacketQ {
		public ArrayDeque<Packet> q = new ArrayDeque<Packet>(20);
	}
	
	private PacketQ leftQ = new PacketQ();
	private PacketQ rightQ = new PacketQ();
	private PacketQ clubQ = new PacketQ();
		
	private PacketQ dequeList[];
	
	@Override
	public BleFPDIdentity onFetchIdentity() {
		Bundle b = getIntent().getExtras();
		BleFPDIdentity identity = b.getParcelable("Identity");
		
		// count the enabled devices...
		int count = 0;
		if (identity.isLeftShoeEnabled())
			++count;
		if (identity.isRightShoeEnabled())
			++count;
		if (identity.isClubEnabled())
			++count;
				
		dequeList = new PacketQ[count];
		
		int i = 0;
		if (identity.isLeftShoeEnabled())
			dequeList[i++] = leftQ;
		if (identity.isRightShoeEnabled())
			dequeList[i++] = rightQ;
		if (identity.isClubEnabled())
			dequeList[i++] = clubQ;		
		
		return identity;
	}
	*/
	
	/*
	public void onData(
		int deviceType,
		int fs0, int fs1, int fs2,
		int fs3, int fs5,
		int acX, int acY, int acZ,
		int mgX, int mgY, int mgZ
		)
	{
		// TODO Auto-generated method stub
		//Log.i("DATA", "dev: " + deviceType);
		Packet packet = new Packet();
		packet.deviceType = deviceType;
		packet.fs0 = fs0;
		packet.fs1 = fs1;
		packet.fs2 = fs2;
				
		if (deviceType == DeviceFragment.DEVICE_TYPE_LEFT_SHOE) {
			leftQ.q.add(packet);
		}
		else if (deviceType == DeviceFragment.DEVICE_TYPE_RIGHT_SHOE) {
			rightQ.q.add(packet);
		}
		else if (deviceType == DeviceFragment.DEVICE_TYPE_CLUB) {
			clubQ.q.add(packet);
		}
		
		int c=0;
		for (; c < dequeList.length; ++c) {
			if (dequeList[c].q.isEmpty())
				break;
		}
		if (c == dequeList.length) {
			for (int i=0; i < c; ++i) {
				Packet p = dequeList[i].q.remove();
				Log.i("DATA", "dev: " + p.deviceType);
			}
		}
	}
	*/

}
