package com.flexpoint.fpd;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import saxatech.flexpoint.BleFPDIdentity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

public class PlotActivity extends Activity {
    private BleFPDIdentity identity;
    
    private SensorData sensorData;
    
    private class SensorData implements SensorDataSetHandler {
    	
    	private class PlotData {
    		public XYSeries series1;
    		public XYSeries series2;
    		public XYSeries series3;
    	}
    	    	
    	public boolean hasData;    	
    	public PlotData leftPlotData  = new PlotData();
    	public PlotData rightPlotData = new PlotData();
    	private final Context context;
    	private final LineAndPointFormatter series1Format;        
        private final LineAndPointFormatter series2Format;        
        private final LineAndPointFormatter series3Format;
        
    	public SensorData(Context context) {
    		this.context = context;
    		
    		series1Format = new LineAndPointFormatter();
            series1Format.configure(context, R.xml.line_point_formatter_1);
            adjustLineAndPointFormat(series1Format);
            
            series2Format = new LineAndPointFormatter();
            series2Format.configure(context, R.xml.line_point_formatter_2);
            adjustLineAndPointFormat(series2Format);
            
            series3Format = new LineAndPointFormatter();
            series3Format.configure(context, R.xml.line_point_formatter_3);
            adjustLineAndPointFormat(series3Format);
    	}
            
    	
    	private void adjustLineAndPointFormat(LineAndPointFormatter l) {
    		Paint p = l.getLinePaint();
    		p.setStrokeWidth(3);
    		l.setLinePaint(p);
    		
    		p = l.getVertexPaint();
    		p.setStrokeWidth(1);
    		l.setVertexPaint(p);
    	}
    	
		@Override
		public void onData(
			SensorDataSet left,
			SensorDataSet right,
			SensorDataSet club
			)
		{
			final int maxSamples = Math.max(left.samplePos, right.samplePos);
			if (maxSamples < 1)
				return;
			
			hasData = true;
			
			setPlotData(left, leftPlotData);
			setPlotData(right, rightPlotData);
		}
		
		public void initLeftXYPlot(XYPlot xyPlot) {
			initXYPlot(leftPlotData, xyPlot);
		}
		public void initRightXYPlot(XYPlot xyPlot) {
			initXYPlot(rightPlotData, xyPlot);
		}
		
		private void initXYPlot(PlotData plotData, XYPlot xyPlot) {
			Set<XYSeries> set = xyPlot.getSeriesSet();
			for (XYSeries s : set) {
				xyPlot.removeSeries(s);
			}
			
			if (!hasData)
				return;
			
			xyPlot.addSeries(plotData.series1, series1Format);
			xyPlot.addSeries(plotData.series2, series2Format);
			xyPlot.addSeries(plotData.series3, series3Format);			
		}
		
		private void setPlotData(SensorDataSet sensorData, PlotData plotData) {
			final int count = sensorData.samplePos;
			
			Number[] s1 = new Number[count];
			Number[] s2 = new Number[count];
			Number[] s3 = new Number[count];
			
			for (int i=0; i < count; ++i) {
				s1[i] = sensorData.fs0[i];
				s2[i] = sensorData.fs1[i];
				s3[i] = sensorData.fs2[i];
			}
			
			plotData.series1 = new SimpleXYSeries(
				Arrays.asList(s1),
				SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
				"Medial"
				);
			plotData.series2 = new SimpleXYSeries(
				Arrays.asList(s2),
				SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
				"Lateral"
				);
			plotData.series3 = new SimpleXYSeries(
				Arrays.asList(s3),
				SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
				"Heel"
				);
		}
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);
        
        Bundle b = getIntent().getExtras();
		identity = BleFPDIdentity.getFromBundle(b);
		
		final MultitouchPlot multitouchPlot = (MultitouchPlot)
			findViewById(R.id.multitouchPlot);
				
		Context context = getApplicationContext();
		
		final RadioButton radioLeftData = (RadioButton)findViewById(R.id.radioLeftData);
		final RadioButton radioRightData = (RadioButton)findViewById(R.id.radioRightData);
				
		sensorData = new SensorData(context);
		StaticRecordBuffer.pushData(sensorData);
		
		// Reduce the number of range labels
        multitouchPlot.setTicksPerRangeLabel(3);
        
        // By default, AndroidPlot displays developer guides to aid in laying out your plot.
        // To get rid of them call disableAllMarkup():        
        
		multitouchPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.BLACK);
		multitouchPlot.getGraphWidget().getDomainGridLinePaint().setColor(0xFF303030);
		multitouchPlot.getGraphWidget().getRangeGridLinePaint().setColor(0xFF303030);
		multitouchPlot.getGraphWidget().getDomainSubGridLinePaint().setColor(0xFF303030);
		multitouchPlot.getGraphWidget().getRangeSubGridLinePaint().setColor(0xFF303030);
		
		multitouchPlot.getGraphWidget().getDomainLabelPaint().setTextSize(18);
		multitouchPlot.getGraphWidget().getRangeLabelPaint().setTextSize(18);
		
		
		multitouchPlot.setRangeBoundaries(0, 300, BoundaryMode.FIXED);
		//multitouchPlot.setDomainBoundaries(0, 2.2, BoundaryMode.FIXED);
        
		sensorData.initLeftXYPlot(multitouchPlot);
		radioLeftData.setChecked(true);
		
		radioLeftData.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(
				CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked) {
					sensorData.initLeftXYPlot(multitouchPlot);
					multitouchPlot.redraw();
				}
			}
		});
		radioRightData.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(
				CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked) {
					sensorData.initRightXYPlot(multitouchPlot);
					multitouchPlot.redraw();
				}
			}
		});
    }
    
    @Override
    public void onBackPressed() {
    	Intent intent = new Intent(this, AnalyzeActivity.class);
		intent.putExtras(identity.makeIntoBundle());
		startActivity(intent);
		finish();
    }
    
}
