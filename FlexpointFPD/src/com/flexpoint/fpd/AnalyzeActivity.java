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
		private AveragedData averagedData;
		private FootView     footView;
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
			averagedData = new AveragedData();
			
			// fix up the data (fixed data in sensorGraphData)
			SensorGraphData sensorGraphData = new SensorGraphData();
			StaticRecordBuffer.pushData(sensorGraphData);
			// push the data into the display graph
			sensorGraphData.pushData(playGraph);
			sensorGraphData.pushData(averagedData);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_analyze,
					container, false);
			
			playGraph.resetPlayPos();
			footView = (FootView)rootView.findViewById(R.id.footView1);
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
				footView.setVisibility(View.GONE);
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
		public int getPlayTimeMsec() {
			return playGraph.getPlayTimeMsec();
		}
		
		private static int REFRESH_RATE_MS = 30;
		
		private void startPlaying() {
			playGraph.resetPlayPos();
			playGraph.postInvalidate();
			
			playTimer = new Timer();
			playTimer.scheduleAtFixedRate(new TimerTask() {
				private int lengthMsec = getPlayTimeMsec();
				private int playPos;
				
				public void run() {
					handler.post(new Runnable() {
						public void run() {
							playGraph.setPlayPosMsec(playPos);
							averagedData.avgSamples(playPos, REFRESH_RATE_MS);
							
							footView.setLeftSensors(
								averagedData.avgLeftFs0,
								averagedData.avgLeftFs1,
								averagedData.avgLeftFs2
								);
							footView.setRightSensors(
								averagedData.avgRightFs0,
								averagedData.avgRightFs1,
								averagedData.avgRightFs2
								);
							
							footView.postInvalidate();
							playGraph.postInvalidate();
							textPlayTime.setText(String.format("Time: %.3f", (float)playPos/1000));
							
							if (playPos >= lengthMsec)
								stopPlaying();
							else
								playPos += REFRESH_RATE_MS;
						}
					});
				}
			}, 250, REFRESH_RATE_MS);
		}
		private void stopPlaying() {
			if (playTimer != null)
				playTimer.cancel();
			togglePlay.setChecked(false);
		}
		
		private class AveragedData implements SensorDataSetHandler {
			private SensorDataSet sensorDataLeft;
			private SensorDataSet sensorDataRight;
			private int     totalSamples;
			private int     sampleRate;
			private boolean hasSamples;
			
			public int avgLeftFs0;
			public int avgLeftFs1;
			public int avgLeftFs2;
			
			public int avgRightFs0;
			public int avgRightFs1;
			public int avgRightFs2;
			
			public void avgSamples(int playPosMsecs, int msecsToAvg) {
				if (!hasSamples)
					return;
				
				final int sampleStart = playPosMsecs/sampleRate;
				final int samples     = msecsToAvg/sampleRate;
				final int maxSamples  = Math.min(totalSamples - (samples + sampleStart), samples);
				
				avgLeftFs0 =0;
				avgLeftFs1 =0;
				avgLeftFs2 =0;
				
				avgRightFs0 =0;
				avgRightFs1 =0;
				avgRightFs2 =0;
				
				if (maxSamples < 1)
					return;
				 
				for (int i=0; i < maxSamples; ++i) {
					avgLeftFs0 += sensorDataLeft.fs0[sampleStart + i];
					avgLeftFs1 += sensorDataLeft.fs1[sampleStart + i];
					avgLeftFs2 += sensorDataLeft.fs2[sampleStart + i];
					
					avgRightFs0 += sensorDataRight.fs0[sampleStart + i];
					avgRightFs1 += sensorDataRight.fs1[sampleStart + i];
					avgRightFs2 += sensorDataRight.fs2[sampleStart + i];
				}
				
				avgLeftFs0 /= maxSamples;
				avgLeftFs1 /= maxSamples;
				avgLeftFs2 /= maxSamples;
				
				avgRightFs0 /= maxSamples;
				avgRightFs1 /= maxSamples;
				avgRightFs2 /= maxSamples;
			}
			
			
			@Override
			public void onData(
				SensorDataSet left, SensorDataSet right,
				SensorDataSet club
			)
			{
				sensorDataLeft  = left;
				sensorDataRight = right;
				
				// FIXME: should throw if samplePos, samplesPerSec
				// are not equal between left and right data sets.
				if (sensorDataLeft.samplePos < 1)
					return;
				if (sensorDataLeft.sampleRate < 1)
					return;
				
				totalSamples = sensorDataLeft.samplePos;
				sampleRate   = sensorDataLeft.sampleRate;
				
				hasSamples = true;
			}
		}
	}

}
