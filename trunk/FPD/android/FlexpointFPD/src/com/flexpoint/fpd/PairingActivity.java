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
	
	public static void startThisActivity(Activity from) {
		Intent intent = new Intent(from, PairingActivity.class);
		Bundle b = new Bundle();
		b.putBoolean("singlePair", false);
		b.putBoolean("pairLeft", true);
		intent.putExtras(b);
		from.startActivity(intent);
	}
	
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
