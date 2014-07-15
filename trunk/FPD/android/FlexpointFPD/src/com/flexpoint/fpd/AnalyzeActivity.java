package com.flexpoint.fpd;

import java.util.Timer;
import java.util.TimerTask;

import saxatech.flexpoint.BleFPDIdentity;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class AnalyzeActivity extends Activity {
	private AnalyzeActivityFragment analyzeActivityFragment;
	private BleFPDIdentity identity;
	private SlideDirector slideDirector;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_analyze);

		FragmentManager fm = getFragmentManager();
		
		analyzeActivityFragment = (AnalyzeActivityFragment)
			fm.findFragmentById(R.id.container);
		
		if (analyzeActivityFragment == null) {
			analyzeActivityFragment = new AnalyzeActivityFragment();
			fm.beginTransaction()
				.add(R.id.container, analyzeActivityFragment)
				.commit();
		}
		Bundle b = getIntent().getExtras();
		identity = BleFPDIdentity.getFromBundle(b);
		
		slideDirector = new SlideDirector();
		slideDirector.setOnSlideListener(new OnSlideListener());
		slideDirector.setEnabled(true);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return slideDirector.onTouchEvent(ev);
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
	
	public static class AnalyzeActivityFragment extends Fragment {
		private Handler      handler;
		private PlayGraph    playGraph;
		private FrameLayout  frameLayoutPlayGraph;
		private TextView     textPlayTime;
		private ToggleButton togglePlay;
		private Timer        playTimer;
		
		public AnalyzeActivityFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			
			handler = new Handler(Looper.getMainLooper());
			
			Context context = getActivity().getApplicationContext();
			playGraph = new PlayGraph(context);
			
			// fix up the data (fixed data in sensorGraphData)
			SensorGraphData sensorGraphData = new SensorGraphData();
			StaticRecordBuffer.pushData(sensorGraphData);
			// push the data into the display graph
			sensorGraphData.pushData(playGraph);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_analyze,
					container, false);
			
			playGraph.resetPlayPos();
			frameLayoutPlayGraph = (FrameLayout)rootView.findViewById(R.id.frameLayoutPlayGraph);
			textPlayTime = (TextView)rootView.findViewById(R.id.textPlayTime);
			togglePlay = (ToggleButton)rootView.findViewById(R.id.togglePlay);
			
			if (playGraph.getPlayTimeMsec() == 0)
				togglePlay.setEnabled(false);
									
			int plotHeight = 200;
			
			if (getActivity().getResources().getConfiguration().orientation ==
				Configuration.ORIENTATION_LANDSCAPE)
			{
				Display display = getActivity().getWindowManager().getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				
				plotHeight = (int)(0.50f * size.y);
			}
			
			FrameLayout.LayoutParams lp =
				new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT, plotHeight
					);
			
			frameLayoutPlayGraph.addView(playGraph, lp);
			
			togglePlay.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					ToggleButton tb = (ToggleButton)view;
					if (tb.isChecked())
						startPlaying();
					else
						stopPlaying();
				}
			});
			
			return rootView;
		}
		
		@Override 
		public void onPause() {
			super.onPause();
			stopPlaying();
			textPlayTime.setText("");
		}
		
		@Override
		public void onDestroyView() {
			super.onDestroyView();
			frameLayoutPlayGraph.removeAllViews();
		}
		
		public void resetPlayPos() {
			playGraph.resetPlayPos();
			playGraph.postInvalidate();
		}
		public void setPlayPosMsec(int ms) {
			playGraph.setPlayPosMsec(ms);
			playGraph.postInvalidate();
		}
		public int getPlayTimeMsec() {
			return playGraph.getPlayTimeMsec();
		}
		
		private void startPlaying() {
			resetPlayPos();
			
			playTimer = new Timer();
			playTimer.scheduleAtFixedRate(new TimerTask() {
				private int lengthMsec = getPlayTimeMsec();
				private int playPos;
				
				public void run() {
					handler.post(new Runnable() {
						public void run() {
							setPlayPosMsec(playPos);
							textPlayTime.setText(String.format("Time: %.3f", (float)playPos/1000));
							
							if (playPos >= lengthMsec)
								stopPlaying();
							else
								playPos += 50;
						}
					});
				}
			}, 250, 50);
		}
		private void stopPlaying() {
			if (playTimer != null)
				playTimer.cancel();
			togglePlay.setChecked(false);
		}
	}

}
