package com.flexpoint.fpd;

import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import saxatech.flexpoint.BleFPDDeviceGroup;
import saxatech.flexpoint.BleFPDIdentity;

public class DataActivity extends Activity
	implements DeviceFragment.ActivityCallbacks
{
	private static final String DEVICE_FRAGMENT = "device_fragment";
	private DataActivityFragment dataActivityFragment;
	private DeviceFragment deviceFragment;
	private BleFPDIdentity identity;	
	private SlideDirector slideDirector;
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
			identity = BleFPDIdentity.getFromBundle(b);
			deviceFragment.setDevices(identity);
			
			fm.beginTransaction()
				.add(deviceFragment, DEVICE_FRAGMENT)
				.commit();
		}
		
		slideDirector = new SlideDirector();
		slideDirector.setOnSlideListener(new OnSlideListener());
		
		updateTimer = new Timer();
		updateTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				dataActivityFragment.updateUi();
			}
		}, 100, 50);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return slideDirector.onTouchEvent(ev);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_pairing) {
			PairingActivity.startThisActivity(this);
			finish();
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
				
		StaticDynamicCalibration calibrator = new StaticDynamicCalibration();
		
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
			calibrator.setLeftSensors(fs0, fs1, fs2);
			
			footView.setLeftSensors(
				calibrator.adjusted_left_fs0(),
				calibrator.adjusted_left_fs1(),
				calibrator.adjusted_left_fs2()
				);
			barView.setLeftValue(calibrator.summed_left());
		}
		public void setRightSensors(int fs0, int fs1, int fs2) {
			calibrator.setRightSensors(fs0, fs1, fs2);
			
			footView.setRightSensors(
				calibrator.adjusted_right_fs0(),
				calibrator.adjusted_right_fs1(),
				calibrator.adjusted_right_fs2()
				);			
			barView.setRightValue(calibrator.summed_right());
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
		slideDirector.setEnabled(true);
		dataActivityFragment.setDeviceStatusText("Connected");		
	}

	@Override
	public void onDisconnected() {
		slideDirector.setEnabled(true);
		dataActivityFragment.setDeviceStatusText("Disconnected");
	}

	@Override
	public void onFailed(String error) {
		slideDirector.setEnabled(true);
		dataActivityFragment.setDeviceStatusText(error);
	}

	@Override
	public void onSensor(int deviceType, long timeStamp, int fs0, int fs1, int fs2) {		
		if (deviceType == BleFPDDeviceGroup.DEVICE_TYPE_LEFT_SHOE) {
			dataActivityFragment.setLeftSensors(fs0, fs1, fs2);
		}
		else if (deviceType == BleFPDDeviceGroup.DEVICE_TYPE_RIGHT_SHOE) {
			dataActivityFragment.setRightSensors(fs0, fs1, fs2);
		}
	}
	
	private class OnSlideListener implements SlideDirector.OnSlideListener {
		public void onSwipeLeft(Class<?> nextActivity) {
			deviceFragment.close();
			Intent intent = new Intent(DataActivity.this, nextActivity);
			intent.putExtras(identity.makeIntoBundle());			
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.fade, R.anim.hold);
		}
		public void onSwipeRight(Class<?> nextActivity) {
			deviceFragment.close();
			Intent intent = new Intent(DataActivity.this, nextActivity);
			intent.putExtras(identity.makeIntoBundle());
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.fade, R.anim.hold);
		}
	}
}
