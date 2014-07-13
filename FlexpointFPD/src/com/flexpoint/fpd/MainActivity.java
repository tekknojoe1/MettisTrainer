package com.flexpoint.fpd;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import saxatech.flexpoint.BleFPDIdentity;
import saxatech.flexpoint.DiscoveryFragment;

public class MainActivity extends Activity 
	implements DiscoveryFragment.ActivityCallbacks
{
	private DiscoveryFragment discoveryFragment;
	private MainActivityFragment mainActivityFragment;
	private static final String DISCOVERY_FRAGMENT = "discovery_fragment";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SlideDirector.setFirstActivity();
		
		FragmentManager fm = getFragmentManager();
		
		mainActivityFragment = (MainActivityFragment)
			fm.findFragmentById(R.id.container);
			
		if (mainActivityFragment == null) {
			mainActivityFragment = new MainActivityFragment();
			fm.beginTransaction()
				.add(R.id.container, mainActivityFragment)
				.commit();
		}
		
		discoveryFragment = (DiscoveryFragment)
			fm.findFragmentByTag(DISCOVERY_FRAGMENT);
		
		if (discoveryFragment == null) {
			discoveryFragment = new DiscoveryFragment();
			fm.beginTransaction()
				.add(discoveryFragment, DISCOVERY_FRAGMENT)
				.commit();
		}
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
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
			
	public static class MainActivityFragment extends Fragment {

		public MainActivityFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
		public void setDiscoveryStatusText(String text) {
			TextView tv = (TextView)getView().findViewById(R.id.discoveryStatus);
			tv.setText(text);
		}
	}

	@Override
	public void onDiscovered(BleFPDIdentity identity) {		
		Intent intent = new Intent(MainActivity.this, DataActivity.class);		
		intent.putExtras(identity.makeIntoBundle());
		startActivity(intent);
		finish();
	}

	@Override
	public void onFailedDiscovery() {
		mainActivityFragment.setDiscoveryStatusText(
			"Failed to find devices"
		);
	}

}
