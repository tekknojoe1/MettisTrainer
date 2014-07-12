package flexpoint.fpd.sensors;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

public class DataActivity extends Activity implements DeviceFragment.ActivityCallbacks {
	private static final String DEVICE_FRAGMENT = "device_fragment";
	
	private DataFragment dataFragment;
	private DeviceFragment deviceFragment;
	private Timer updateTimer;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_data);

		FragmentManager fm = getFragmentManager();
		
		dataFragment = (DataFragment)
			fm.findFragmentById(R.id.container);
		if (dataFragment == null) {
			dataFragment = new DataFragment();
			fm.beginTransaction()
				.add(R.id.container, dataFragment)
				.commit();
		}
		
		deviceFragment = (DeviceFragment)
			fm.findFragmentByTag(DEVICE_FRAGMENT);
		if (deviceFragment == null) {
			Bundle b = getIntent().getExtras();
			final String btAddress = b.getString("BtAddress");
			final boolean commonReversed = b.getBoolean("CommonReversed");
			
			deviceFragment = new DeviceFragment();
			deviceFragment.setDevice(btAddress, commonReversed);
			fm.beginTransaction()
				.add(deviceFragment, DEVICE_FRAGMENT)
				.commit();
		}
		
		final Runnable updateUi = new Runnable() {
			public void run() {
				dataFragment.updateGraphs();
			}
		};
		updateTimer = new Timer();
		updateTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				runOnUiThread(updateUi);
			}
		}, 100, 100);
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

	@Override
	public void onConnected() {
		// TODO Auto-generated method stub
		Log.i("CALLBACK", "+++onConnected");
		dataFragment.setStateText("Connected");
	}

	@Override
	public void onDisconnected() {
		Log.i("CALLBACK", "+++onDisconnected");
		dataFragment.setStateText("Disconnected");
	}

	@Override
	public void onFailed(String error) {
		Log.i("CALLBACK", "+++onFailed: " + error);
		dataFragment.setStateText(error);
	}

	@Override
	public void onSensor(int fs0, int fs1, int fs2, int fs3, int fs4) {
		//String data = String.format(
		//	"%02X %02X %02X %02X %02X",
		//	fs0, fs1, fs2, fs3, fs4
		//	);
		//Log.i("DATA", data);
		dataFragment.setSensorData(fs0, fs1, fs2);
	}

	@Override
	public void onVersion(String versionString) {
		dataFragment.setVersionText(versionString);
		
	}
}
