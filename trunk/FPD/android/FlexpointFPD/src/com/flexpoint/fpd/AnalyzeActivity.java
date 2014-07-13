package com.flexpoint.fpd;

import saxatech.flexpoint.BleFPDIdentity;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class AnalyzeActivity extends Activity {	
	private BleFPDIdentity identity;
	private SlideDirector slideDirector;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_analyze);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
				.add(R.id.container, new PlaceholderFragment()).commit();
		}
		Bundle b = getIntent().getExtras();
		identity = BleFPDIdentity.getFromBundle(b);
		
		slideDirector = new SlideDirector();
		slideDirector.setOnSlideListener(new OnSlideListener());
		slideDirector.setEnabled(true);
		
		SensorGraphData graphData = new SensorGraphData();
		StaticRecordBuffer.analyzeData(graphData);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return slideDirector.onTouchEvent(ev);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.analyze, menu);
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
	
	private class OnSlideListener implements SlideDirector.OnSlideListener {
		public void onSwipeLeft(Class<?> nextActivity) {
			Intent intent = new Intent(AnalyzeActivity.this, nextActivity);
			intent.putExtras(identity.makeIntoBundle());
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.fade, R.anim.hold);
		}
		public void onSwipeRight(Class<?> nextActivity) {
			Intent intent = new Intent(AnalyzeActivity.this, nextActivity);
			intent.putExtras(identity.makeIntoBundle());
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.fade, R.anim.hold);
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_analyze,
					container, false);
			return rootView;
		}
	}

}
