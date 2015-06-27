package bendtech.fpd.mettisdemo;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class CalibrationActivity extends Activity implements InsolesFragment.ActivityCallbacks {
	private static final String INSOLES_FRAGMENT = "insoles_fragment";
	private Handler handler;
	private Settings settings;
	private CalibrationFragment calibrationFragment;
	private InsolesFragment insolesFragment;
	
	private Calibrator calibratorLeft = new Calibrator();
	private Calibrator calibratorRight = new Calibrator();
		
	private int leftStanceS0;
	private int leftStanceS1;
	private int leftStanceS2;
	private int rightStanceS0;
	private int rightStanceS1;
	private int rightStanceS2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calibration);

		handler = new Handler(Looper.getMainLooper());
		
		final FragmentManager fm = getFragmentManager();
		settings = new Settings(getApplicationContext());
		
		calibrationFragment = (CalibrationFragment)
			fm.findFragmentById(R.id.container);
		if (calibrationFragment == null) {
			calibrationFragment = new CalibrationFragment();
			fm.beginTransaction()
				.add(R.id.container, calibrationFragment)
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
		
		calibrationFragment.setStatusText("Connecting to insoles...");
		
		calibratorLeft.setListener(new OnCalibrationListener() {
			@Override
			public void onCalibration(
				int stanceS0, int stanceS1, int stanceS2,
				int s0, int s1,	int s2
				)
			{
				calibrationFragment.setLeftCalSensors(s0, s1, s2);
				leftStanceS0 = stanceS0;
				leftStanceS1 = stanceS1;
				leftStanceS2 = stanceS2;				
			}
		});
		calibratorRight.setListener(new OnCalibrationListener() {
			@Override
			public void onCalibration(
				int stanceS0, int stanceS1, int stanceS2,
				int s0, int s1,	int s2
				)
			{
				calibrationFragment.setRightCalSensors(s0, s1, s2);
				rightStanceS0 = stanceS0;
				rightStanceS1 = stanceS1;
				rightStanceS2 = stanceS2;
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.calibration, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent(this, PairingActivity.class);
			startActivity(intent);
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public static class CalibrationFragment extends Fragment {
		private TextView textStatus;
		private String   statusText = "";
		
		private TextView textError;
		
		private TextView textLeftS0;
		private TextView textLeftS1;
		private TextView textLeftS2;		
		private TextView textRightS0;
		private TextView textRightS1;
		private TextView textRightS2;
		
		private TextView textLeftCalS0;
		private TextView textLeftCalS1;
		private TextView textLeftCalS2;		
		private TextView textRightCalS0;
		private TextView textRightCalS1;
		private TextView textRightCalS2;
		
		private Button buttonDone;
		
		public CalibrationFragment() {
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
			View rootView = inflater.inflate(R.layout.fragment_calibration,
					container, false);
			
			textStatus = (TextView)rootView.findViewById(R.id.textStatus);
			textError = (TextView)rootView.findViewById(R.id.textError);
			textError.setVisibility(View.GONE);
		
			textLeftS0 = (TextView)rootView.findViewById(R.id.textLeftS0);
			textLeftS1 = (TextView)rootView.findViewById(R.id.textLeftS1);
			textLeftS2 = (TextView)rootView.findViewById(R.id.textLeftS2);			
			textRightS0 = (TextView)rootView.findViewById(R.id.textRightS0);
			textRightS1 = (TextView)rootView.findViewById(R.id.textRightS1);
			textRightS2 = (TextView)rootView.findViewById(R.id.textRightS2);
			
			textLeftCalS0 = (TextView)rootView.findViewById(R.id.textLeftCalS0);
			textLeftCalS1 = (TextView)rootView.findViewById(R.id.textLeftCalS1);
			textLeftCalS2 = (TextView)rootView.findViewById(R.id.textLeftCalS2);			
			textRightCalS0 = (TextView)rootView.findViewById(R.id.textRightCalS0);
			textRightCalS1 = (TextView)rootView.findViewById(R.id.textRightCalS1);
			textRightCalS2 = (TextView)rootView.findViewById(R.id.textRightCalS2);
			
			buttonDone = (Button)rootView.findViewById(R.id.buttonDone);
			buttonDone.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					CalibrationActivity calibrationActivity =
						(CalibrationActivity)getActivity();
					calibrationActivity.doneCalibrating();
				}
				
			});
			buttonDone.setEnabled(false);
			
			textStatus.setText(statusText);
			
			return rootView;
		}
		
		public void setStatusText(String text) {
			statusText = text;
			if (textStatus != null)
				textStatus.setText(statusText);
		}
		
		public void setErrorText(String text) {
			if (textError != null) {
				textError.setVisibility(View.VISIBLE);
				textError.setText(text);
			}
		}
		
		public void enableDoneButton(boolean enable) {
			buttonDone.setEnabled(enable);
		}
		
		public void setLeftSensors(int s0, int s1, int s2) {
			textLeftS0.setText(Integer.toString(s0));
			textLeftS1.setText(Integer.toString(s1));
			textLeftS2.setText(Integer.toString(s2));
		}
		
		public void setRightSensors(int s0, int s1, int s2) {
			textRightS0.setText(Integer.toString(s0));
			textRightS1.setText(Integer.toString(s1));
			textRightS2.setText(Integer.toString(s2));
		}
		
		public void setLeftCalSensors(int s0, int s1, int s2) {
			textLeftCalS0.setText(Integer.toString(s0));
			textLeftCalS1.setText(Integer.toString(s1));
			textLeftCalS2.setText(Integer.toString(s2));
		}
		
		public void setRightCalSensors(int s0, int s1, int s2) {
			textRightCalS0.setText(Integer.toString(s0));
			textRightCalS1.setText(Integer.toString(s1));
			textRightCalS2.setText(Integer.toString(s2));
		}
		
	}
		
	private boolean calibrationRunning;
	
	private void enableCalibration() {
		calibrationRunning = true;
		calibrationFragment.enableDoneButton(true);
	}
	
	private void doneCalibrating() {
		settings.setLeftStances(
			leftStanceS0, leftStanceS1, leftStanceS2
			);
		settings.setRightStances(
			rightStanceS0, rightStanceS1, rightStanceS2
			);
		
		Intent intent = new Intent(this, FeedbackActivity.class);
		startActivity(intent);
		
		finish();
	}
	
	@Override
	public void onConnected() {
		calibrationFragment.setStatusText(
			"Insoles connected"
			);
		
		handler.postDelayed(new Runnable() {
			public void run() {
				calibrationFragment.setStatusText(
					"Please stand in shoes to calibrate"
					);
				handler.postDelayed(new Runnable() {
					public void run() {
						calibrationFragment.setStatusText(
							"Calibrating..."
							);
						enableCalibration();
					}
				}, 4000);
			}
		}, 2000);
	}

	@Override
	public void onDisconnected() {
		calibrationFragment.setStatusText(
			"Insoles disconnected"
			);
	}

	@Override
	public void onFailed(String error) {
		calibrationFragment.setStatusText(
			"Failed to connect to insoles"
			);
		calibrationFragment.setErrorText(error);
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
			calibrationFragment.setLeftSensors(medial, lateral, heal);
			if (calibrationRunning)
				calibratorLeft.setSensors(medial, lateral, heal);
			break;
		case InsolesFragment.DEVICE_TYPE_RIGHT_SHOE:
			calibrationFragment.setRightSensors(medial, lateral, heal);
			if (calibrationRunning)
				calibratorRight.setSensors(medial, lateral, heal);
			break;
		}
	}
	
	private interface OnCalibrationListener {
		public void onCalibration(			
			int stanceS0, int stanceS1, int stanceS2,
			int s0, int s1, int s2
			);
	}
		
	private class Calibrator {
		private boolean init;
		private int count;
		private int avgS0;
		private int avgS1;
		private int avgS2;
						
		private OnCalibrationListener onCalibrationListener;
				
		public void setListener(OnCalibrationListener listener) {
			onCalibrationListener = listener;
		}
		
		public void setSensors(int s0, int s1, int s2) {
			++count;
			if (!init) {
				avgS0 = s0;
				avgS1 = s1;
				avgS2 = s2;
				init = true;
				return;
			}
			avgS0 *= 19; avgS0 += s0; avgS0 /= 20;
			avgS1 *= 19; avgS1 += s1; avgS1 /= 20;
			avgS2 *= 19; avgS2 += s2; avgS2 /= 20;
						
			if ((count % 50) == 0) {
				final int offS0 = 128 - avgS0;
				final int offS1 = 128 - avgS1;
				final int offS2 = 128 - avgS2;
				
				s0 += offS0;
				s1 += offS1;
				s2 += offS2;
				
				if (onCalibrationListener != null) {
					onCalibrationListener.onCalibration(
						avgS0, avgS1, avgS2,
						s0, s1, s2
						);
				}
			}
		}
	}
}
