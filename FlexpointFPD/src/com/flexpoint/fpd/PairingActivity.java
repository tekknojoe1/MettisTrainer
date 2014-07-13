package com.flexpoint.fpd;

import java.util.ArrayList;

import saxatech.flexpoint.BleFPDIdentity;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class PairingActivity extends Activity
	implements PairFragment.ActivityCallbacks
{
	private PairFragment pairFragment;
	private boolean singlePair;
	private Preferences prefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pairing);
		
		prefs = new Preferences(this);
		
		ArrayList<String> excludedAddresses = new ArrayList<String>();
		
		Bundle b = getIntent().getExtras();
		singlePair = b.getBoolean("singlePair");
		final boolean pairLeft = b.getBoolean("pairLeft");
		
		final String excluded = b.getString("exclude");
		if (excluded != null)
			excludedAddresses.add(excluded);
				
		FragmentManager fm = getFragmentManager();
		
		pairFragment = (PairFragment)
			fm.findFragmentById(R.id.container);
		if (pairFragment == null) {
			pairFragment = new PairFragment();
			pairFragment.setParameters(pairLeft, excludedAddresses);
			
			fm.beginTransaction()
				.add(R.id.container, pairFragment)
				.commit();
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

	@Override
	public void onLeftSelected(String btAddress) {
		BleFPDIdentity.setPairedLeft(this, btAddress);
		
		if (singlePair) {
			prefs.setPaired(true);
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
		}
		else {
			Intent intent = new Intent(this, PairingActivity.class);
			Bundle b = new Bundle();
			b.putBoolean("singlePair", true);
			b.putBoolean("pairLeft", false);
			b.putString("exclude", btAddress);
			intent.putExtras(b);
			startActivity(intent);	
		}
		finish();
	}

	@Override
	public void onRightSelected(String btAddress) {		
		BleFPDIdentity.setPairedRight(this, btAddress);
		prefs.setPaired(true);
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
		finish();
	}
}
