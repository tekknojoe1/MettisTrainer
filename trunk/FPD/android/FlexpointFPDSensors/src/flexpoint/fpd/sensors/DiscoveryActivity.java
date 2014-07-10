package flexpoint.fpd.sensors;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

public class DiscoveryActivity extends Activity implements DiscoveryFragment.ActivityCallbacks {
	private DiscoveryFragment discoveryFragment;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_discovery);

		FragmentManager fm = getFragmentManager();
		
		discoveryFragment = (DiscoveryFragment)
			fm.findFragmentById(R.id.container);
		if (discoveryFragment == null) {
			discoveryFragment = new DiscoveryFragment();
			fm.beginTransaction()
				.add(R.id.container, discoveryFragment)
				.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.discovery, menu);
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
	public void onDeviceSelected(String btAddress, boolean commonReversed) {
		Intent intent = new Intent(this, DataActivity.class);
		Bundle b = new Bundle();
		b.putString("BtAddress", btAddress);
		b.putBoolean("CommonReversed", commonReversed);
		intent.putExtras(b);
		startActivity(intent);
		finish();
	}
}
