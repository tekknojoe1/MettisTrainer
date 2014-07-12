package flexpoint.fpd.sensors;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class DataFragment extends Fragment {
	private TextView textViewState;
	private TextView textViewVersion;
	//private plotDynamic plot1;
	private FastPlot plot1;
	private FastPlot plot2;
	private FastPlot plot3;
	private FrameLayout frame1;
	private FrameLayout frame2;
	private FrameLayout frame3;
	private String stateText = "Waiting for device to connect...";
	private String versionText = "";
	
	public DataFragment() {		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		Context context = getActivity().getApplicationContext();
		plot1 = new FastPlot(context);
		plot2 = new FastPlot(context);
		plot3 = new FastPlot(context);
	}
	
	@Override
	public View onCreateView(
		LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState
		)
	{
		View rootView = inflater.inflate(
			R.layout.fragment_data, container,
			false
			);
		
		textViewState = (TextView)rootView.findViewById(R.id.textViewState);
		textViewVersion = (TextView)rootView.findViewById(R.id.textViewVersion);
		
		frame1 = (FrameLayout)rootView.findViewById(R.id.frameLayoutSens1);
		frame2 = (FrameLayout)rootView.findViewById(R.id.frameLayoutSens2);
		frame3 = (FrameLayout)rootView.findViewById(R.id.frameLayoutSens3);
		
		int plotHeight =
			getActivity().getResources().getConfiguration().orientation ==
			Configuration.ORIENTATION_LANDSCAPE ?
					200 : 250;
		
		FrameLayout.LayoutParams lp =
			new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, plotHeight
				);
		
		frame1.addView(plot1, lp);
		frame2.addView(plot2, lp);
		frame3.addView(plot3, lp);
		
		textViewState.setText(stateText);
		textViewVersion.setText(versionText);
		
		return rootView;
	}
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		frame1.removeAllViews();
		frame2.removeAllViews();
		frame3.removeAllViews();
	}
	
	public void setStateText(String text) {
		stateText = text;
		textViewState.setText(stateText);
	}
	
	public void setVersionText(String text) {
		versionText = text;
		textViewVersion.setText(versionText);
	}
	
	public void setSensorData(int fs0, int fs1, int fs2) {
		plot1.addData(fs0);
		plot2.addData(fs1);
		plot3.addData(fs2);
	}
	
	public void updateGraphs() {
		plot1.postInvalidate();
		plot2.postInvalidate();
		plot3.postInvalidate();
	}
}
