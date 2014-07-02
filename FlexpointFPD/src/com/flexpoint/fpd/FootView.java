package com.flexpoint.fpd;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

public class FootView extends ImageView {
	private final Context context;
	private boolean paintReady;
	private SensorDraw sensorDraw;
	
	public FootView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}
	
	private void setupPaint(int w, int h, float dpi) {
		sensorDraw = new SensorDraw(w, h, dpi);		
		paintReady = true;
	}
	
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		if (!paintReady || (w != oldw || w != oldh)) {
			final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
			setupPaint(w, h, displayMetrics.xdpi);
		}
	}
	
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (!paintReady)
			return;
		
		drawSensors(canvas);
	}
	
	private int left0;
	private int left1;
	private int left2;
	
	private int right0;
	private int right1;
	private int right2;
	
	public void setLeftSensors(
		int s0, int s1, int s2
		)
	{
		left0 = s0;
		left1 = s1;
		left2 = s2;
	}
	
	public void setRightSensors(
		int s0, int s1, int s2
		)
	{
		right0 = s0;
		right1 = s1;
		right2 = s2;
	}
	
	private void drawSensors(Canvas canvas) {
		sensorDraw.drawLeft(0, left0, canvas);
		sensorDraw.drawLeft(1, left1, canvas);
		sensorDraw.drawLeft(2, left2, canvas);
		
		sensorDraw.drawRight(0, right0, canvas);
		sensorDraw.drawRight(1, right1, canvas);
		sensorDraw.drawRight(2, right2, canvas);
	}
}
