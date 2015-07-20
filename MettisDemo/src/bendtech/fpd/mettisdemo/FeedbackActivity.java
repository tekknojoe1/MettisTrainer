package bendtech.fpd.mettisdemo;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.Build;

public class FeedbackActivity extends Activity implements InsolesFragment.ActivityCallbacks {
	private static final String INSOLES_FRAGMENT = "insoles_fragment";
	private Handler handler;
	private Settings settings;
	private FeedbackFragment feedbackFragment;
	private InsolesFragment insolesFragment;
	
	private SensorProcessor sensorProcessorLeft;
	private SensorProcessor sensorProcessorRight;
	
	private FootStrikeMetering footStrikeMeterLeft = new FootStrikeMetering();
	private FootStrikeMetering footStrikeMeterRight = new FootStrikeMetering();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feedback);

		handler = new Handler(Looper.getMainLooper());
		
		final FragmentManager fm = getFragmentManager();
		settings = new Settings(getApplicationContext());
		
		feedbackFragment = (FeedbackFragment)
			fm.findFragmentById(R.id.container);
		if (feedbackFragment == null) {
			feedbackFragment = new FeedbackFragment();
			fm.beginTransaction()
				.add(R.id.container, feedbackFragment)
				.commit();
		}
		
		insolesFragment = (InsolesFragment)
			fm.findFragmentByTag(INSOLES_FRAGMENT);
		if (insolesFragment == null) {
			insolesFragment = new InsolesFragment();
			insolesFragment.setInsoles(
				settings.leftInsoleMacAddress(),
				settings.rightInsoleMacAddress()
				);
			fm.beginTransaction()
				.add(insolesFragment, INSOLES_FRAGMENT)
				.commit();
		}
		
		final CadenceAverager cadenceAverager = new CadenceAverager();
		
		final int leftStanceS0 = settings.leftStanceS0();
		final int leftStanceS1 = settings.leftStanceS1();
		final int leftStanceS2 = settings.leftStanceS2(); 
		
		footStrikeMeterLeft.setListener(new StrikeListener() {
			@Override
			public void onSrike(int p0, int p1, int p2) {
				strikeLeft(p0, p1, p2);
			}
		});
		
		sensorProcessorLeft = new SensorProcessor(
			leftStanceS0, leftStanceS1, leftStanceS2
			);
		sensorProcessorLeft.setListener(new OnSensorProcessListener() {
			@Override
			public void onPercent(int p0, int p1, int p2) {
				feedbackFragment.setLeftPercentages(p0, p1, p2);
				footStrikeMeterLeft.setPercentages(p0, p1, p2);
			}
			@Override
			public void onNoContact() {
				feedbackFragment.setLeftPercentages(0, 0, 0);
				footStrikeMeterLeft.postResults();
			}
			@Override
			public void onCadence(int cadence) {
				cadenceAverager.pushLeftCadence(cadence);
			}
		});
		
		final int rightStanceS0 = settings.rightStanceS0();
		final int rightStanceS1 = settings.rightStanceS1();
		final int rightStanceS2 = settings.rightStanceS2();
		
		footStrikeMeterRight.setListener(new StrikeListener() {
			@Override
			public void onSrike(int p0, int p1, int p2) {
				strikeRight(p0, p1, p2);
			}
		});
		
		sensorProcessorRight = new SensorProcessor(
			rightStanceS0, rightStanceS1, rightStanceS2
			);
		sensorProcessorRight.setListener(new OnSensorProcessListener() {
			@Override
			public void onPercent(int p0, int p1, int p2) {
				feedbackFragment.setRightPercentages(p0, p1, p2);
				footStrikeMeterRight.setPercentages(p0, p1, p2);
			}
			@Override
			public void onNoContact() {
				feedbackFragment.setRightPercentages(0, 0, 0);
				footStrikeMeterRight.postResults();
			}
			@Override
			public void onCadence(int cadence) {
				cadenceAverager.pushRightCadence(cadence);
			}
		});
		
		feedbackFragment.setStatusText("Connecting to insoles...");
	}
	
	private int avgLeftStrike;
	private int avgRightStrike;
	private int strikeValue = 50;
	
	private void strikeLeft(int p0, int p1, int p2) {
		int a = avgLeftStrike = p2;
		if (avgRightStrike != 0)
			a = (avgRightStrike + avgLeftStrike) / 2;
		
		if (a < 20 || a > 58)
			strikeValue -= 10;
		else
			strikeValue += 10;
		
		if (strikeValue > 100)
			strikeValue = 100;
		if (strikeValue < 0)
			strikeValue = 0;
		
		Log.i("STRIKE", "strike left");
		feedbackFragment.setStrikeProgress(strikeValue);
	}
	private void strikeRight(int p0, int p1, int p2) {
		int a = avgRightStrike = p2;
		if (avgLeftStrike != 0)
			a = (avgRightStrike + avgLeftStrike) / 2;
		
		if (a < 20 || a > 58)
			--strikeValue;
		else
			++strikeValue;
		
		if (strikeValue > 100)
			strikeValue = 100;
		if (strikeValue < 0)
			strikeValue = 0;
		
		Log.i("STRIKE", "strike right");
		feedbackFragment.setStrikeProgress(strikeValue);
	}
	
	private interface StrikeListener {
		public void onSrike(int p0, int p1, int p2);
	}
	
	private class FootStrikeMetering {
		private boolean init;
		private int avp0;
		private int avp1;
		private int avp2;
		
		StrikeListener strikeListener;
		
		public void setListener(StrikeListener listener) {
			strikeListener = listener;
		}
		
		public void setPercentages(int p0, int p1, int p2) {
			if (!init) {
				avp0 = p0;
				avp1 = p1;
				avp2 = p2;
				init = true;
			}
			else {
				avp0 += p0; avp0 /= 2;
				avp1 += p1; avp1 /= 2;
				avp2 += p2; avp2 /= 2;
			}
		}
		public void postResults() {
			if (!init)
				return;
			
			strikeListener.onSrike(avp0, avp1, avp2);
			init = false;
		}
	}
	
	private class CadenceAverager {
		private static final int CADENCE_ARRAY_LENGTH = 10;
		private int leftArrayPos;
		private int rightArrayPos;
		private int[] leftCadenceArray = new int[CADENCE_ARRAY_LENGTH];
		private int[] rightCadenceArray = new int[CADENCE_ARRAY_LENGTH];
		
		private int leftCadence;
		private int rightCadence;
		
		public void pushLeftCadence(int cadence) {
			if (leftArrayPos < CADENCE_ARRAY_LENGTH) {
				leftCadenceArray[leftArrayPos++] = cadence;
			}
			else {
				int i=1;
				for (; i < CADENCE_ARRAY_LENGTH; ++i)
					leftCadenceArray[i-1] = leftCadenceArray[i];
				leftCadenceArray[i-1] = cadence;
			}
			int avg = 0;
			for (int i=0; i < leftArrayPos; ++i)
				avg += leftCadenceArray[i];
			leftCadence = avg / leftArrayPos;
			
			notifyCadence(leftCadence, rightCadence == 0 ? leftCadence : rightCadence);			
		}
		public void pushRightCadence(int cadence) {
			if (rightArrayPos < CADENCE_ARRAY_LENGTH) {
				rightCadenceArray[rightArrayPos++] = cadence;
			}
			else {
				int i=1;
				for (; i < CADENCE_ARRAY_LENGTH; ++i)
					rightCadenceArray[i-1] = rightCadenceArray[i];
				rightCadenceArray[i-1] = cadence;
			}
			int avg = 0;
			for (int i=0; i < rightArrayPos; ++i)
				avg += rightCadenceArray[i];
			rightCadence = avg / rightArrayPos;
			
			notifyCadence(leftCadence == 0 ? rightCadence : leftCadence, rightCadence);
		}
		
		private void notifyCadence(int left, int right) {
			final int cadence = (left+right)/2;
			
			Log.i("CADENCE", "" + cadence);
			feedbackFragment.setCadence(Integer.toString(cadence));
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.feedback, menu);
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
	
	public static class FeedbackFragment extends Fragment {
		private String statusText = "";
		private TextView textStatus;
		private SensorPadView sensorLeftMedial;
		private SensorPadView sensorLeftLat;
		private SensorPadView sensorLeftHeel;
		private TextView textLeftMedial;
		private TextView textLeftLat;
		private TextView textLeftHeel;
		
		private SensorPadView sensorRightMedial;
		private SensorPadView sensorRightLat;
		private SensorPadView sensorRightHeel;
		private TextView textRightMedial;
		private TextView textRightLat;
		private TextView textRightHeel;
		
		private TextView textCadence;
		
		private StrikeProgressView strikeProgress;
		
		public FeedbackFragment() {
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			
			
		}
		@Override
		public View onCreateView(
			LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState
			)
		{
			View rootView = inflater.inflate(R.layout.fragment_feedback,
				container, false);
			
			textStatus = (TextView)rootView.findViewById(R.id.textStatus);
			textStatus.setText(statusText);
			
			sensorLeftMedial = (SensorPadView)rootView.findViewById(R.id.sensorLeftMedial);
			sensorLeftLat = (SensorPadView)rootView.findViewById(R.id.sensorLeftLat);
			sensorLeftHeel = (SensorPadView)rootView.findViewById(R.id.sensorLeftHeel);
			textLeftMedial = (TextView)rootView.findViewById(R.id.textLeftMedial);
			textLeftLat = (TextView)rootView.findViewById(R.id.textLeftLat);
			textLeftHeel = (TextView)rootView.findViewById(R.id.textLeftHeel);
			
			sensorRightMedial = (SensorPadView)rootView.findViewById(R.id.sensorRightMedial);
			sensorRightLat = (SensorPadView)rootView.findViewById(R.id.sensorRightLat);
			sensorRightHeel = (SensorPadView)rootView.findViewById(R.id.sensorRightHeel);
			textRightMedial = (TextView)rootView.findViewById(R.id.textRightMedial);
			textRightLat = (TextView)rootView.findViewById(R.id.textRightLat);
			textRightHeel = (TextView)rootView.findViewById(R.id.textRightHeel);
			
			textCadence = (TextView)rootView.findViewById(R.id.textCadence);
			
			strikeProgress = (StrikeProgressView)rootView.findViewById(R.id.strikeProgress);
			
			return rootView;
		}
		
		public void setStatusText(String text) {
			statusText = text;
			if (textStatus != null)
				textStatus.setText(statusText);
		}
		public void enableStatusText(boolean enable) {
			textStatus.setVisibility(enable ? View.VISIBLE : View.GONE);
		}
			
		public void setCadence(String text) {
			textCadence.setText(text);
		}
		public void setStrikeProgress(int value) {
			strikeProgress.setMeter(value);
			strikeProgress.invalidate();
		}
		
		public void setLeftPercentages(int p0, int p1, int p2) {
			textLeftMedial.setText(Integer.toString(p0));
			textLeftLat.setText(Integer.toString(p1));
			textLeftHeel.setText(Integer.toString(p2));
			
		}
		public void setRightPercentages(int p0, int p1, int p2) {
			textRightMedial.setText(Integer.toString(p0));
			textRightLat.setText(Integer.toString(p1));
			textRightHeel.setText(Integer.toString(p2));			
		}
	}

	@Override
	public void onConnected() {
		feedbackFragment.setStatusText("Insoles connected");
		handler.postDelayed(new Runnable() {
			public void run() {
				feedbackFragment.enableStatusText(false);
			}
		}, 1500);
	}

	@Override
	public void onDisconnected() {
		feedbackFragment.setStatusText("Insoles disconnected");
		feedbackFragment.enableStatusText(true);
	}

	@Override
	public void onFailed(String error) {
		feedbackFragment.setStatusText("Failed to connect:\n" + error);
		feedbackFragment.enableStatusText(true);
	}

	@Override
	public void onSensor(
		int deviceType,
		long timeStampNsec,
		int medial, int lateral, int heal,
		int cadence, int contactTime, int impactForce
		)
	{
		switch (deviceType) {
		case InsolesFragment.DEVICE_TYPE_LEFT_SHOE:
			sensorProcessorLeft.setValues(timeStampNsec, medial, lateral, heal);
			break;
		case InsolesFragment.DEVICE_TYPE_RIGHT_SHOE:
			sensorProcessorRight.setValues(timeStampNsec, medial, lateral, heal);
			break;
		}
	}
	
	private interface OnSensorProcessListener {
		public void onPercent(int p0, int p1, int p2);
		public void onNoContact();
		public void onCadence(int cadence);
	}
	
	private class SensorProcessor {
		private final int stanceS0;
		private final int stanceS1;
		private final int stanceS2;
		private final int threshold;		 
		private OnSensorProcessListener onSensorProcessListener;
				
		private boolean high;		 
		private long lastStartTimeNsec;
		private long startTimeNsec;
		
		private int count;
		private long ts;
		private int as0;
		private int as1;
		private int as2;
		
		public SensorProcessor(
			int stanceS0, int stanceS1, int stanceS2
			)
		{
			this.stanceS0 = stanceS0;
			this.stanceS1 = stanceS1;
			this.stanceS2 = stanceS2;
			this.threshold = stanceS2;
		}
		
		public void setListener(OnSensorProcessListener listener) {
			onSensorProcessListener = listener;
		}
		
		private int adjustSensor(int s, int stance) {
			final int off = 128 - stance;
			s += off;
			if (s < 0)
				return 0;
			return s;
		}
		
		public void setValues(
			long timeStampNsec,
			int s0, int s1, int s2
			)
		{
			if (count == 0) {
				ts = timeStampNsec;
				as0 = s0;
				as1 = s1;
				as2 = s2;
				++count;
				return;
			}
			else if (count < 7) {
				as0 += s0;
				as1 += s1;
				as2 += s2;
				++count;
				return;
			}
			else {
				as0 += s0;
				as1 += s1;
				as2 += s2;
				timeStampNsec = ts;
				s0 = as0 / 8;
				s1 = as1 / 8;
				s2 = as2 / 8;
				count = 0;
			}
			
			final int totalStrike = s0 + s1 + s2;
			final int totalStrikeThreshold = (int)(0.5f * (stanceS0 + stanceS1 + stanceS2));
			final int heelStrike = s2;
			
			if (totalStrike >= totalStrikeThreshold) {
				s0 = adjustSensor(s0, stanceS0);
				s1 = adjustSensor(s1, stanceS1);
				s2 = adjustSensor(s2, stanceS2);
				final int sum = s0 + s1 + s2;
				s0 = (int)(((float)s0 / sum) * 100.0f);
				s1 = (int)(((float)s1 / sum) * 100.0f);
				s2 = (int)(((float)s2 / sum) * 100.0f);
				
				onSensorProcessListener.onPercent(s0, s1, s2);				
			}
			else {
				onSensorProcessListener.onNoContact();
			}
			
			if (heelStrike >= stanceS2) {
				//s0 = adjustSensor(s0, stanceS0);
				//s1 = adjustSensor(s1, stanceS1);
				//s2 = adjustSensor(s2, stanceS2);
				//final int sum = s0 + s1 + s2;
				//s0 = (int)(((float)s0 / sum) * 100.0f);
				//s1 = (int)(((float)s1 / sum) * 100.0f);
				//s2 = (int)(((float)s2 / sum) * 100.0f);
				//
				//onSensorProcessListener.onPercent(s0, s1, s2);
				
				if (high)
					return;
				Log.i("STEP", "DOWN");
				high = true;
				lastStartTimeNsec = startTimeNsec;
				startTimeNsec = timeStampNsec;
				if (lastStartTimeNsec != 0) {
					notifyCadence(startTimeNsec - lastStartTimeNsec);
				}
			}
			else if (heelStrike < (int)(0.88f * stanceS2)) {
				if (!high)
					return;
				
				Log.i("STEP", "UP");
				high = false;
				//onSensorProcessListener.onNoContact();
			}
		}
		
		private void notifyCadence(long deltaTimeNsec) {
			final int stepTimeMsec = (int)(deltaTimeNsec / (1000 * 1000));
			if (stepTimeMsec < 240)
				return;
			final int stepsPerMin = (60000 / stepTimeMsec);
			
			onSensorProcessListener.onCadence(stepsPerMin);
		}
	}
}
