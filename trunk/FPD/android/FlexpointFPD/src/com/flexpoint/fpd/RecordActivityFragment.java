package com.flexpoint.fpd;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Fragment;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ProgressBar;

public class RecordActivityFragment extends Fragment {
	
	public static interface ActivityCallbacks {
		public void onButtonRec();
		public void onButtonStop();
		public void onButtonAnalyze();
		public void onPreRecordStart();
		public void onRecordingStart();
	}
			
	public RecordActivityFragment() {
	}

	private Handler handler;
	private ActivityCallbacks activityCallbacks;
	private TextView     textDeviceStatus;
	private ToggleButton toggleRecord;
	private TextView     textCountDown;
	private ImageView    imageRecLight;
	private ProgressBar  progressTime;
	private FrameLayout  frameLayoutSensLeft;
	private FrameLayout  frameLayoutSensRight;
	private FastPlot     plotSensLeft;
	private FastPlot     plotSensRight;
	private Button       buttonAnalyze;
	private boolean      canShowAnalyze;
	private boolean      recording;
	private Timer        countDownTimer;
			
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		activityCallbacks = (ActivityCallbacks)activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler(Looper.getMainLooper());
		plotSensLeft  = new FastPlot(getActivity());
		plotSensRight = new FastPlot(getActivity());
		plotSensLeft.setLabel("Left");
		plotSensRight.setLabel("Right"); 
	}
	
	@Override
	public View onCreateView(
		LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState
		)
	{
		View rootView = inflater.inflate(
			R.layout.fragment_record, container, false
			);
		textDeviceStatus = (TextView)rootView.findViewById(R.id.deviceStatus);
		toggleRecord = (ToggleButton)rootView.findViewById(R.id.toggleRecord);
		textCountDown = (TextView)rootView.findViewById(R.id.textCountDown);
		imageRecLight = (ImageView)rootView.findViewById(R.id.imageRecLight);
		progressTime = (ProgressBar)rootView.findViewById(R.id.progressTime);
		frameLayoutSensLeft = (FrameLayout)rootView.findViewById(R.id.frameLayoutSensLeft);
		frameLayoutSensRight = (FrameLayout)rootView.findViewById(R.id.frameLayoutSensRight);
		buttonAnalyze = (Button)rootView.findViewById(R.id.buttonAnalyze);
		
		final int plotHeight = 200;
		FrameLayout.LayoutParams lp =
			new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, plotHeight
				);
		frameLayoutSensLeft.addView(plotSensLeft, lp);
		frameLayoutSensRight.addView(plotSensRight, lp);
		
		textCountDown.setText("");
		buttonAnalyze.setVisibility(View.INVISIBLE);
		enableControls(false);
		
		toggleRecord.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				ToggleButton tb = (ToggleButton)view;
				if (activityCallbacks == null)
					return;
				if (tb.isChecked())
					activityCallbacks.onButtonRec();
				else
					activityCallbacks.onButtonStop();
			}
		});
		
		buttonAnalyze.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (activityCallbacks != null)
					activityCallbacks.onButtonAnalyze();
			}
		});
		
		return rootView;
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		frameLayoutSensLeft.removeAllViews();
		frameLayoutSensRight.removeAllViews();
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		activityCallbacks = null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (countDownTimer != null)
			countDownTimer.cancel();
	}
	
	public void setDeviceStatusText(String text) {
		textDeviceStatus.setText(text);
	}
	
	public void enableControls(boolean enable) {
		textDeviceStatus.setEnabled(enable);
		toggleRecord.setEnabled(enable);
		imageRecLight.setEnabled(enable);
		progressTime.setEnabled(enable);
		frameLayoutSensLeft.setEnabled(enable);
	}
	
	public void setProgressTimeMax(int value) {
		progressTime.setMax(value);
	}
	public void setProgressTime(int value) {
		progressTime.setProgress(value);
	}
	public void incrementProgressTime() {
		progressTime.incrementProgressBy(1);
	}
		
	public void startRecording() {
		if (recording)
			return;
		recording = true;
		
		plotSensLeft.reset();
		plotSensRight.reset();
		plotSensLeft.postInvalidate();
		plotSensRight.postInvalidate();
		
		progressTime.setProgress(0);
		buttonAnalyze.setVisibility(View.INVISIBLE);
		canShowAnalyze = false;
		
		countDownTimer = new Timer();
		countDownTimer.scheduleAtFixedRate(new TimerTask() {
			private int countDown = 5;
			private ToneGenerator countTone = 
				new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
			
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						if (!recording)
							return;
						
						if (countDown == 0) {
							textCountDown.setText("recording");
							setRecLight(REC_LIGHT_MODE_ON);					
							cancel();
							countTone.startTone(ToneGenerator.TONE_DTMF_D,500);
							if (activityCallbacks != null)
								activityCallbacks.onRecordingStart();
							canShowAnalyze = true;
						}
						else {
							if (countDown == 2) {
								if (activityCallbacks != null)
									activityCallbacks.onPreRecordStart();								
							}
							textCountDown.setText(String.valueOf(countDown));
							countTone.startTone(ToneGenerator.TONE_DTMF_1,100);
							setRecLight(REC_LIGHT_MODE_FLASH);
						}
						--countDown;
					}
				});
			}
		}, 0, 1000);
	}
	
	public void stopRecording() {
		if (!recording)
			return;
		recording = false;
		countDownTimer.cancel();
		toggleRecord.setChecked(false);
		textCountDown.setText("");		
		setRecLight(REC_LIGHT_MODE_OFF);
		if (canShowAnalyze)
			buttonAnalyze.setVisibility(View.VISIBLE);
	}
	
	public void setSensorDataLeft(int fs0, int fs1, int fs2) {
		final int p = Math.max((fs0+fs1)/2, fs2);
		plotSensLeft.addData(p);
		plotSensLeft.postInvalidate();
	}
	public void setSensorDataRight(int fs0, int fs1, int fs2) {
		final int p = Math.max((fs0+fs1)/2, fs2);
		plotSensRight.addData(p);
		plotSensRight.postInvalidate();
	}
	
	private static final int REC_LIGHT_MODE_OFF    = 0;
	private static final int REC_LIGHT_MODE_ON     = 1;
	private static final int REC_LIGHT_MODE_FLASH  = 2;
	
	private int currentRecLightMode = REC_LIGHT_MODE_OFF;
	
	private void setRecLight(int recLightMode) {
		if (recLightMode == REC_LIGHT_MODE_OFF) {
			imageRecLight.setImageResource(
				R.drawable.recording_light_off
				);
		}	
		else if (recLightMode == REC_LIGHT_MODE_ON) {
			imageRecLight.setImageResource(
				R.drawable.recording_light_on
				);
		}
		else {
			imageRecLight.setImageResource(
				R.drawable.recording_light_on
				);
			handler.postDelayed(new Runnable() {
				public void run() {
					if (currentRecLightMode == REC_LIGHT_MODE_FLASH) {
						imageRecLight.setImageResource(
							R.drawable.recording_light_off
							);
					}
				}
			}, 250);
		}
		currentRecLightMode = recLightMode;
	}
}
