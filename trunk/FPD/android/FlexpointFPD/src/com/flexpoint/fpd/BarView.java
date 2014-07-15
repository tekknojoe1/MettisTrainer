package com.flexpoint.fpd;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

public class BarView extends ImageView {
	private Paint barPaint;
	private int width;
	private int height;
	private float scale;
	private float leftValue;
	private float rightValue;
	private float barWidth;
	private float xOffset;
	
	private boolean paintReady;
	
	private int barColor = Color.RED;	
	
	private int maxValue = 768;
	
	public BarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		barPaint = new Paint();
		barPaint.setColor(barColor);
		barPaint.setStyle(Paint.Style.FILL);
	}
	
	public void setLeftValue(int v) {
		leftValue = scale * v;
	}
	public void setRightValue(int v) {
		rightValue = scale * v;
	}
	
	private void setupPaint(int w, int h) {
		width = w;
		height = h;
		scale = (float)h/maxValue;
		barWidth = (float)w/3.0f;
		xOffset  = barWidth/4;
		
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
	}
}
