package com.flexpoint.fpd;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;

public class SensorDraw {
	private final Paint sensorPaint;
	
	private class Pnt {
		private final RectF rect;
		private final RectF rectCopy;
		
		public Pnt(
			int viewWidth, int viewHeight,
			float dpi, float x, float y,
			int widthDp, int heightDp,
			int xFudgeDp
			)
		{	
			final int width  = dpToPx(widthDp, dpi);
			final int height = dpToPx(heightDp, dpi);
			
			rect = new RectF();
			rect.left   = (x * viewWidth - width/2) + dpToPx(xFudgeDp, dpi);
			rect.top    = (y * viewHeight - height/2);
			rect.right  = rect.left + width;
			rect.bottom = rect.top  + height;
			
			rectCopy = new RectF();
		}
		
		public final RectF getRectF() {
			rectCopy.set(rect);
			return rectCopy;
		}
		
		private final int dpToPx(int dp, float dpi) {
			return Math.round(dp * (dpi / DisplayMetrics.DENSITY_DEFAULT));
		}
	};
		
	// 0 medial (under big toe)
	// 1 lateral (under pinky toe)
	// 2 heel
	private Pnt leftSensorPnts[] = new Pnt[3];
	private Pnt rightSensorPnts[] = new Pnt[3];
			
	public SensorDraw(int w, int h, float dpi) {				
		sensorPaint = new Paint();
		sensorPaint.setColor(0xff000000);
		sensorPaint.setStyle(Paint.Style.FILL);
		
		final int leftXFudgeDp = 0;
		
		leftSensorPnts[2] = new Pnt(
			w, h, dpi,
			0.1975f,
			0.82075f,
			68,
			68,
			leftXFudgeDp
			);
		
		leftSensorPnts[0] = new Pnt(
			w, h, dpi,
			0.3375f,
			0.3075f,
			52,
			52,
			leftXFudgeDp
			);
		
		leftSensorPnts[1] = new Pnt(
			w, h, dpi,
			0.16416f, //0.0875f,
			0.375f,
			45,
			45,
			leftXFudgeDp
			);
		
		final int rightXFudgeDp = 0;
		
		rightSensorPnts[2] = new Pnt(
			w, h, dpi,
			0.8025f,
			0.82075f,
			68,
			68,
			rightXFudgeDp
			);
		
		rightSensorPnts[0] = new Pnt(
			w, h, dpi,
			0.6625f,
			0.3075f,
			52,
			52,
			rightXFudgeDp
			);
		
		rightSensorPnts[1] = new Pnt(
			w, h, dpi,
			0.83584f, //0.9125f,
			0.375f,
			45,
			45,
			rightXFudgeDp
			);
	}
	
	public void drawLeft(int sensorNum, int value, Canvas canvas) {
		
		if (value > 255)
			value = 255;
		
		//final int c = 0xFF000000 | (value <<8) | (value /2);
		
		int c = 0xFF000000;
				
		if (value < 64) {			
			c |= (value<<2); //Blue 0 - 255
		} else if (value < 128) {
			c |= 255 | ( (value-64)<<10); //Blue 255 Green 0-255
		} else if (value < 192) {
			c |= (64-(value-128)<<2) | (255<<8) | ( (value-128)<<18); //Blue 255-0 Green 255 Red 0-255
		} else {
			c |= (64-(value-192)<<10) | (255<<16); //Green 255-0 Red 255
		}
		
		final float s = (float)value/256;
		
		sensorPaint.setColor(c);
		RectF r = leftSensorPnts[sensorNum].getRectF();
		
		final float x = (r.left + r.right)/2;
		final float y = (r.top + r.bottom)/2;
		
		final float w = (r.right-r.left)/2;
		final float h = (r.bottom-r.top)/2;
				
		r.left   = x - w*s;
		r.top    = y - h*s;
		r.right  = x + w*s;
		r.bottom = y + h*s;
		
		canvas.drawOval(r, sensorPaint);
	}
	public void drawRight(int sensorNum, int value, Canvas canvas) {

		if (value > 255)
			value = 255;
		//final int c = 0xFF000000 | (value <<8) | (value /2);
		
		int c = 0xFF000000;
		
		if (value < 64) {
			c |= (value<<2); //Blue 0 - 255
		} else if (value < 128) {
			c |= 255 | ( (value-64)<<10); //Blue 255 Green 0-255
		} else if (value < 192) {
			c |= (64-(value-128)<<2) | (255<<8) | ( (value-128)<<18); //Blue 255-0 Green 255 Red 0-255
		} else {
			c |= (64-(value-192)<<10) | (255<<16); //Green 255-0 Red 255
		}
		
		final float s = (float)value/256;
		
		sensorPaint.setColor(c);
		RectF r = rightSensorPnts[sensorNum].getRectF();
		
		final float x = (r.left + r.right)/2;
		final float y = (r.top + r.bottom)/2;
		
		final float w = (r.right-r.left)/2;
		final float h = (r.bottom-r.top)/2;
				
		r.left   = x - w*s;
		r.top    = y - h*s;
		r.right  = x + w*s;
		r.bottom = y + h*s;
		
		canvas.drawOval(r, sensorPaint);
	}
	
	
}
