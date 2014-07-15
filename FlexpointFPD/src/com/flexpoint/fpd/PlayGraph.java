package com.flexpoint.fpd;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

public class PlayGraph extends View implements SensorDataSetHandler {
	private SensorDataSet sensorDataLeft;
	private SensorDataSet sensorDataRight;
	private Bitmap renderBitmap;
	private Paint  linePaint;
	private Paint  plotPaintLeft;
	private Paint  plotPaintRight;
	private Paint  timeLinePaint;
	private int    totalSamples;
	private int    lengthMsec;
	private boolean ready;
	private boolean hasSamples;
	
	private int bkgColor       = 0xff282527;
	private int lineColor      = Color.BLACK;
	private int plotLeftColor  = Color.BLUE;
	private int plotRightColor = Color.RED;
	private int timeLineColor  = Color.WHITE;
	
	private int maxSampleValue = 256;
	private int width;
	private int height;
	private int pointWidth = 4;
	private int plotPoints;
	private int playPosMs = -1;
	private float playPosScale;
	
	public PlayGraph(Context context) {
		super(context);
		
		linePaint = new Paint();
		linePaint.setColor(lineColor);
		linePaint.setStyle(Paint.Style.STROKE);
		
		plotPaintLeft = new Paint();
		plotPaintLeft.setColor(plotLeftColor);
		plotPaintLeft.setAntiAlias(true);
		plotPaintLeft.setStyle(Paint.Style.STROKE);
		plotPaintLeft.setStrokeWidth(pointWidth);
		
		plotPaintRight = new Paint();
		plotPaintRight.setColor(plotRightColor);
		plotPaintRight.setAntiAlias(true);
		plotPaintRight.setStyle(Paint.Style.STROKE);
		plotPaintRight.setStrokeWidth(pointWidth);
		
		timeLinePaint = new Paint();
		timeLinePaint.setColor(timeLineColor);
		timeLinePaint.setStyle(Paint.Style.STROKE);
		plotPaintRight.setStrokeWidth(2);
	}

	public void resetPlayPos() {
		playPosMs = -1;
	}
	
	public void setPlayPosMsec(int ms) {
		if (ms < 0)
			ms = 0;
		if (ms > lengthMsec)
			ms = lengthMsec;
		
		playPosMs = ms;
	}
	
	public int getPlayTimeMsec() {
		return lengthMsec;
	}
	
	@Override
	public void onData(
		SensorDataSet left, SensorDataSet right,
		SensorDataSet club
		)
	{
		sensorDataLeft  = left.makeCopy();
		sensorDataRight = right.makeCopy();
		
		// FIXME: should throw if samplePos, samplesPerSec
		// are not equal between left and right data sets.
		if (sensorDataLeft.samplePos < 1)
			return;
		if (sensorDataLeft.sampleRate < 1)
			return;
		
		totalSamples = sensorDataLeft.samplePos;
		lengthMsec = totalSamples * sensorDataLeft.sampleRate;
		
		hasSamples = true;
	}
	
	@Override
	protected void onSizeChanged(
		int w, int h, int oldw, int oldh
		)
	{
		if (w != oldw || h != oldh) {
			setup(w,h);
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (!ready)
			return;
		
		canvas.drawBitmap(renderBitmap, 0,0, null);
		
		if (playPosMs < 0)
			return;
		
		float x = playPosScale * playPosMs;
		canvas.drawLine(x, 0, x, height, timeLinePaint);
	}
	
	private void setup(int w, int h) {
		ready = true;
		
		width  = w;
		height = h;
		
		if (renderBitmap != null)
			renderBitmap.recycle();
		
		renderBitmap = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas();
		canvas.setBitmap(renderBitmap);
		canvas.drawColor(bkgColor);
		
		if (!hasSamples)
			return;
		
		plotPoints = w/pointWidth;
		playPosScale = (float)w / lengthMsec;
		
		float scaleSampleToY    = (float)h/maxSampleValue;
		float scaleXToSamplePos = (float)totalSamples/plotPoints;
		
		final float[] pnts = new float[4 * plotPoints];
		
		// draw left
		for (int x=1; x < plotPoints; ++x) {
			final int samplePos1 = (int)((x-1) * scaleXToSamplePos);			
			final int sample1 =	(
				((sensorDataLeft.fs0[samplePos1] + sensorDataLeft.fs1[samplePos1])/2) +
				  sensorDataLeft.fs2[samplePos1]
				)/2;
			final int samplePos2 = (int)(x * scaleXToSamplePos);			
			final int sample2 =	(
				((sensorDataLeft.fs0[samplePos2] + sensorDataLeft.fs1[samplePos2])/2) +
				  sensorDataLeft.fs2[samplePos2]
				)/2;
						
			pnts[(x*4)+0] = (x*pointWidth);   // sx;
			pnts[(x*4)+1] = h - (scaleSampleToY * sample1); // sy;
			pnts[(x*4)+2] = (x*pointWidth)+pointWidth; // ex;
			pnts[(x*4)+3] = h - (scaleSampleToY * sample2); // ey;
		}
		
		canvas.drawLines(pnts, plotPaintLeft);
		
		// draw right
		for (int x=1; x < plotPoints; ++x) {
			final int samplePos1 = (int)((x-1) * scaleXToSamplePos);			
			final int sample1 =	(
				((sensorDataRight.fs0[samplePos1] + sensorDataRight.fs1[samplePos1])/2) +
				  sensorDataRight.fs2[samplePos1]
				)/2;
			final int samplePos2 = (int)(x * scaleXToSamplePos);			
			final int sample2 =	(
				((sensorDataRight.fs0[samplePos2] + sensorDataRight.fs1[samplePos2])/2) +
				  sensorDataRight.fs2[samplePos2]
				)/2;
						
			pnts[(x*4)+0] = (x*pointWidth);   // sx;
			pnts[(x*4)+1] = h - (scaleSampleToY * sample1); // sy;
			pnts[(x*4)+2] = (x*pointWidth)+pointWidth; // ex;
			pnts[(x*4)+3] = h - (scaleSampleToY * sample2); // ey;
		}
		
		canvas.drawLines(pnts, plotPaintRight);
	}
	
}
