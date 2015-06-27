package bendtech.fpd.mettisdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;

public class SplashActivity extends Activity {
	private Settings settings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		settings = new Settings(getApplicationContext());
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new SplashFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.splash, menu);
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

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class SplashFragment extends Fragment {
		private Handler handler;
		private ImageView imageLogo;
		private Animation animateLogo;
		
		public SplashFragment() {
			handler = new Handler(Looper.getMainLooper());
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_splash,
					container, false);
			
			imageLogo = (ImageView)rootView.findViewById(R.id.imageLogo);			
			return rootView;
		}
		
		@Override
		public void onStart() {
			super.onStart();
			
			if (animateLogo != null)
				return;
			
			animateLogo = new AlphaAnimation(0.0f, 1.0f);
			animateLogo.setDuration(2000);
			animateLogo.setRepeatCount(0);
			animateLogo.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
				}
				@Override
				public void onAnimationEnd(Animation animation) {
					handler.postDelayed(new Runnable() {
						public void run() {
							nextScreen();
						}
					},2000);
				}
				@Override
				public void onAnimationRepeat(Animation animation) {
				}
			});
			imageLogo.startAnimation(animateLogo);
		}
		
		private void nextScreen() {
			BluetoothManager bluetoothManager
				= (BluetoothManager)getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
			BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
			
			if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
				new AlertDialog.Builder(getActivity())
				.setTitle("Bluetooth not enabled")
				.setMessage("Bluetooth must be enabled to use this application.")
				.setNeutralButton("Abort", new DialogInterface.OnClickListener() {				
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getActivity().finish();
					}
				})
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						getActivity().finish();
					}
				})
				.create().show();
				return;
			}
			
			final SplashActivity splashActivity = ((SplashActivity)getActivity()); 
			final Settings settings = splashActivity.settings;
			
			if (!settings.isPaired()) {
				Intent intent = new Intent(splashActivity, PairingActivity.class);
				splashActivity.startActivity(intent);
			}
			else {
				Intent intent = new Intent(splashActivity, CalibrationActivity.class);
				splashActivity.startActivity(intent);
				splashActivity.overridePendingTransition(R.anim.fade, R.anim.hold);
			}
			splashActivity.finish();
		}
	}

}
