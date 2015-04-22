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
	
	private boolean paintReady;
	
	private int barColor = Color.RED;
	private int meterColor = Color.LTGRAY;
	private int textColor = Color.WHITE;
	
	private final int maxValue = 768;
	private final float textSize = 40.f;
	
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
		leftValue = scale * v;
		if (leftValue > leftMaxValue)
			leftMaxValue = leftValue;
	}
	public void setRightValue(int v) {
		rightValue = scale * v;
		if (rightValue > rightMaxValue)
			rightMaxValue = rightValue;
	}
	public void reset()	{
		leftMaxValue = 0.0f;
		rightMaxValue = 0.0f;
	}
	
	private void setupPaint(int w, int h) {
		width = w;
		height = h;
		scale = (float)h/maxValue;
		barWidth = (float)w/3.0f;
		xOffset  = barWidth/4;
		
		reset();
		
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
			leftX, (float)height-leftValue,
			leftX+barWidth, height,
			barPaint
			);
		
		final float rightX = width-(xOffset+barWidth);
		canvas.drawRect(
			rightX, (float)height-rightValue,
			rightX+barWidth, height,
			barPaint
			);
		canvas.drawLine(
			leftX, (float)height-leftMaxValue,
			leftX+barWidth, (float)height-leftMaxValue,
			meterPaint
			);
		canvas.drawLine(
			rightX, (float)height-rightMaxValue,
			rightX+barWidth, (float)height-rightMaxValue,
			meterPaint
			);
		
		final float max_v = leftValue + rightValue;
		final int left_percent  = (int)((leftValue * 100.f)/max_v);
		final int right_percent = (int)((rightValue * 100.f)/max_v);
		
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
