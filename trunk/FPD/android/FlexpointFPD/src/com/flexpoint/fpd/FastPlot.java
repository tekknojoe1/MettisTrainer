package com.flexpoint.fpd;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public class FastPlot extends View {
	private Canvas drawCanvas;
	private Paint bkgPaint;
	private Paint linePaint;
	private Paint plotPaint;
	private Path  plotPath;
	private int width;
	private int height;
	private float scale;
	private int paintPos;
	private int plotPos;
	private float firstPoint;
	private float lastPoint;
	private Bitmap frontBmp;
	private Bitmap backBmp;
	private String label;
		
	private boolean ready;
	
	private int bkgColor = 0xff282527;
	private int lineColor = Color.WHITE;
	private int plotColor = Color.RED;
	
	private int maxPoint = 256;
	private int xInc = 4;
			
	public FastPlot(Context context) {
		super(context);
		
		drawCanvas = new Canvas();
		
		bkgPaint = new Paint();
		bkgPaint.setColor(bkgColor);
		bkgPaint.setStyle(Paint.Style.FILL);
		
		linePaint = new Paint();
		linePaint.setColor(lineColor);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setTextSize(20.f);
		
		plotPaint = new Paint();
		plotPaint.setColor(plotColor);
		plotPaint.setAntiAlias(true);
		plotPaint.setStyle(Paint.Style.FILL);
		
		plotPath = new Path();
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

	public void setMaxPoint(int maxPoint) {
		this.maxPoint = Math.min(Math.max(1, maxPoint), 0xffff);
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public void setPlotColor(int color) {
		this.plotColor = color;
		plotPaint.setColor(plotColor);
	}
	
	public void addData(int p) {
		if (p < 0) p = 0;
		else if (p > maxPoint) p = maxPoint;
		
		final float currentPoint = scale*p;
		final float fheight = height;
				
		if (plotPos + xInc > width * 2)
			plotPos = 0;
		
		if (plotPos == 0) {
			plotPath.rewind();
			firstPoint = lastPoint;
			
			plotPath.moveTo(0, fheight - lastPoint);
			plotPath.lineTo(xInc, fheight - currentPoint);
			plotPos = xInc;
		}
		else {
			plotPos += xInc;
			plotPath.lineTo(plotPos, fheight - currentPoint);
		}
		lastPoint = currentPoint;
	}
	
	public void reset() {
		if (!ready)
			return;
		
		paintPos = 0;
		
		drawCanvas.setBitmap(frontBmp);
		drawCanvas.drawColor(bkgColor);
		drawCanvas.setBitmap(backBmp);
		drawCanvas.drawColor(bkgColor);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (!ready)
			return;
				
		if (plotPos > 0) {			
			// close the path			
			plotPath.lineTo(plotPos, height);
			plotPath.lineTo(0, height);
			plotPath.lineTo(0, (float)height - firstPoint);
					
			if (paintPos + plotPos < width) {
				drawCanvas.setBitmap(backBmp);
				plotPath.offset(paintPos, 0);
				drawCanvas.drawPath(plotPath, plotPaint);
				paintPos += plotPos;
			}
			else {
				final int offset = width - paintPos;
				paintPos = width;
				drawCanvas.setBitmap(frontBmp);
				drawCanvas.drawRect(0,0,paintPos, height, bkgPaint);
				drawCanvas.drawPath(plotPath, plotPaint);
				
				drawCanvas.setBitmap(backBmp);
				drawCanvas.drawBitmap(backBmp, -(plotPos-offset), 0, null);
				drawCanvas.drawBitmap(frontBmp, width-plotPos, 0, null);
			}
			plotPos = 0;
		}
		
		canvas.drawBitmap(backBmp, 0, 0, null);
		canvas.drawLine(0, height-2, width, height-2, linePaint);
		if (label != null)
			canvas.drawText(label, (float)0.15*width, (float)0.15*height, linePaint);
	}
		
	private void setup(int w, int h) {
		width  = w;
		height = h;
		scale  = (float)h/maxPoint;
		paintPos = 0;
		plotPos = 0;
		firstPoint = 0;
		lastPoint = 0;
		
		if (frontBmp != null)
			frontBmp.recycle();
		if (backBmp != null)
			backBmp.recycle();
		
		frontBmp = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
		backBmp = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
		
		drawCanvas.setBitmap(frontBmp);
		drawCanvas.drawColor(bkgColor);
		drawCanvas.setBitmap(backBmp);
		drawCanvas.drawColor(bkgColor);
						
		ready = true;		
	}
}
