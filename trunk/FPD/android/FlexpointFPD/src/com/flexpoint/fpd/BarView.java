package com.flexpoint.fpd;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

public class BarView extends ImageView {
	private Paint barPaint;
	private Paint meterPaint;
	private Paint textPaint;
	private int width;
	private int height;
	private float scale;
	private float leftValue;
	private float rightValue;
	private float leftMaxValue;
	private float rightMaxValue;
	private float barWidth;
	private float xOffset;
	private int trackState = 0;
	
	private boolean paintReady;
	
	private int barColor = Color.RED;
	private int meterColor = Color.LTGRAY;
	private int textColor = Color.WHITE;
	
	private final int maxValue = 768;
	private final float textSize = 40.f;
	private final int HundredPercentThresh = 600; //Same as defined in calibrator
	
	public BarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		barPaint = new Paint();
		barPaint.setColor(barColor);
		barPaint.setStyle(Paint.Style.FILL);
		
		meterPaint = new Paint();
		meterPaint.setColor(meterColor);
		meterPaint.setStyle(Paint.Style.STROKE);
		meterPaint.setStrokeWidth(6);
		
		textPaint = new Paint();
		textPaint.setColor(textColor);
		textPaint.setTextSize(textSize);
	}
	
	public void setLeftValue(int v) {
		leftValue = v;  //in percent where 384 = 100% (128*3)
		if ( (leftValue > leftMaxValue) && (trackState != 0) )
			leftMaxValue = leftValue;
	}
	public void setRightValue(int v) {
		rightValue = v;
		if ( (rightValue > rightMaxValue) && (trackState != 0) )
			rightMaxValue = rightValue;
	}
	public void reset(int t)	{
		if (t != 0) {
			leftMaxValue = 0.0f;
			rightMaxValue = 0.0f;
		}
		
		trackState = t;
	}
	
	private void setupPaint(int w, int h) {
		width = w;
		height = h;
		scale = (float)h/maxValue;
		barWidth = (float)w/3.0f;
		xOffset  = barWidth/4;
		
		reset(1);
		
		paintReady = true;
	}
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (w != oldw || h != oldh) {
			setupPaint(w,h);
		}
	}
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (!paintReady)
			return;
				
		final float leftX = xOffset;		 
		canvas.drawRect(
			leftX, (float)height-(leftValue*scale),
			leftX+barWidth, height,
			barPaint
			);
		
		final float rightX = width-(xOffset+barWidth);
		canvas.drawRect(
			rightX, (float)height-(rightValue*scale),
			rightX+barWidth, height,
			barPaint
			);
		canvas.drawLine(
			leftX, (float)height-(leftMaxValue*scale),
			leftX+barWidth, (float)height-(leftMaxValue*scale),
			meterPaint
			);
		canvas.drawLine(
			rightX, (float)height-(rightMaxValue*scale),
			rightX+barWidth, (float)height-(rightMaxValue*scale),
			meterPaint
			);
		
		
		
		final int leftMax_percent  = (int)((leftMaxValue * 100.f)/HundredPercentThresh);
		final int rightMax_percent = (int)((rightMaxValue * 100.f)/HundredPercentThresh);
		
		canvas.drawText(
			"max " + Integer.toString(leftMax_percent) + "%",
			leftX+((barWidth-textSize)/4), textSize/2,
			textPaint
			);
		canvas.drawText(
			"max " + Integer.toString(rightMax_percent) + "%",
			rightX+((barWidth-textSize)/4), textSize/2,
			textPaint
			);
		
		final int left_percent  = (int)((leftValue * 100.f)/HundredPercentThresh);
		final int right_percent = (int)((rightValue * 100.f)/HundredPercentThresh);
		
		canvas.drawText(
			Integer.toString(left_percent) + "%",
			leftX+((barWidth-textSize)/2), height-textSize,
			textPaint
			);
		canvas.drawText(
			Integer.toString(right_percent) + "%",
			rightX+((barWidth-textSize)/2), height-textSize,
			textPaint
		);
		
	}
}
