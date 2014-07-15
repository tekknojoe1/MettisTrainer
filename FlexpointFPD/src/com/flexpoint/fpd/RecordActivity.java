package com.flexpoint.fpd;


import java.util.Timer;
import java.util.TimerTask;

import saxatech.flexpoint.BleFPDDeviceGroup;
import saxatech.flexpoint.BleFPDIdentity;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

public class RecordActivity extends Activity implements
	DeviceFragment.ActivityCallbacks,
	RecordActivityFragment.ActivityCallbacks
{
	private static final String DEVICE_FRAGMENT = "device_fragment";
	private RecordActivityFragment recordActivityFragment;
	private DeviceFragment deviceFragment;	
	private BleFPDIdentity identity;
	private SlideDirector slideDirector;
	private boolean canPlot;
	private boolean recording;
	private Handler handler;
	private Timer recordTimer;
	private StaticDynamicCalibration calibrator = new StaticDynamicCalibration();
	private StaticRecordBuffer recordBuffer = new StaticRecordBuffer();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record);

		handler = new Handler(Looper.getMainLooper());
		
		FragmentManager fm = getFragmentManager();
		
		recordActivityFragment = (RecordActivityFragment)
			fm.findFragmentById(R.id.container);
			
		if (recordActivityFragment == null) {
			recordActivityFragment = new RecordActivityFragment();
			fm.beginTransaction()
				.add(R.id.container, recordActivityFragment)
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
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (recordTimer != null)
			recordTimer.cancel();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return slideDirector.onTouchEvent(ev);
	}
	
	private class OnSlideListener implements SlideDirector.OnSlideListener {
		public void onSwipeLeft(Class<?> nextActivity) {
			deviceFragment.close();
			Intent intent = new Intent(RecordActivity.this, nextActivity);
			intent.putExtras(identity.makeIntoBundle());
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.fade, R.anim.hold);
		}
		public void onSwipeRight(Class<?> nextActivity) {
			deviceFragment.close();
			Intent intent = new Intent(RecordActivity.this, nextActivity);
			intent.putExtras(identity.makeIntoBundle());
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.fade, R.anim.hold);
		}
	}

	@Override
	public void onConnected() {
		slideDirector.setEnabled(true);
		recordActivityFragment.enableControls(true);
		recordActivityFragment.setDeviceStatusText("Connected");
	}

	@Override
	public void onDisconnected() {
		slideDirector.setEnabled(true);
		stopRecording();
		recordActivityFragment.enableControls(false);
		recordActivityFragment.setDeviceStatusText("Disconnected");		
	}

	@Override
	public void onFailed(String error) {
		slideDirector.setEnabled(true);
		recordActivityFragment.setDeviceStatusText(error);
	}

	@Override
	public void onSensor(int deviceType, long timeStampNsec, int fs0, int fs1, int fs2) {
		if (deviceType == BleFPDDeviceGroup.DEVICE_TYPE_LEFT_SHOE) {
			if (canPlot)			
				recordActivityFragment.setSensorDataLeft(fs0, fs1, fs2);
			if (!recording)
				return;
			calibrator.setLeftSensors(
				calibrator.adjusted_left_fs0(),
				calibrator.adjusted_left_fs1(),
				calibrator.adjusted_left_fs2()
				);
			recordBuffer.storeLeft(timeStampNsec, fs0, fs1, fs2);
		}
		else if (deviceType == BleFPDDeviceGroup.DEVICE_TYPE_RIGHT_SHOE) {
			if (canPlot)			
				recordActivityFragment.setSensorDataRight(fs0, fs1, fs2);
			if (!recording)
				return;
			calibrator.setRightSensors(
				calibrator.adjusted_right_fs0(),
				calibrator.adjusted_right_fs1(),
				calibrator.adjusted_right_fs2()
				);
			recordBuffer.storeRight(timeStampNsec, fs0, fs1, fs2);
		}
		else if (deviceType == BleFPDDeviceGroup.DEVICE_TYPE_CLUB) {
			if (!recording)
				return;
			recordBuffer.storeClub(timeStampNsec, fs0, fs1, fs2);
		}
	}

	@Override
	public void onButtonRec() {
		recordBuffer.reset();
		recordActivityFragment.startRecording();		
	}

	@Override
	public void onButtonStop() {
		recordActivityFragment.setProgressTime(0);
		stopRecording();	
	}
	

	@Override
	public void onButtonAnalyze() {
		slideDirector.slideLeft();
	}

	@Override
	public void onPreRecordStart() {
		recording = true;
	}
	
	@Override
	public void onRecordingStart() {		
		canPlot = true;
		
		if (recordTimer != null)
			recordTimer.cancel();
		
		recordTimer = new Timer();
		recordTimer.scheduleAtFixedRate(new TimerTask() {
			private static final int MAX_TIME_SECS = 15;
			private int countDown = MAX_TIME_SECS;
			private ToneGenerator countTone = 
				new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
			
			public void run() {
				handler.post(new Runnable() {
					public void run() {						
						if (countDown == 0) {
							recordActivityFragment.setProgressTime(MAX_TIME_SECS);
							countTone.startTone(ToneGenerator.TONE_DTMF_1,100);
							stopRecording();
						}
						else if (countDown == MAX_TIME_SECS) {
							recordActivityFragment.setProgressTimeMax(MAX_TIME_SECS);							
						}
						else {
							recordActivityFragment.incrementProgressTime();
						}
						--countDown;
					}
				});
			}
		}, 100, 1000);
	}
	
	private void stopRecording() {
		canPlot   = false;
		recording = false;
		if (recordTimer != null)
			recordTimer.cancel();
		recordActivityFragment.stopRecording();		
	}
}
