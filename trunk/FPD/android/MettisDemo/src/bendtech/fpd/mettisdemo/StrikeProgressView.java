package bendtech.fpd.mettisdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

public class StrikeProgressView extends ImageView {
	private static final int STEPS = 64;
	private Paint barPaint;
	private Paint markPaint;
	private int width;
	private int height;
	private int markWidth;
	private int pos;
	private boolean paintReady;
	
	public StrikeProgressView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		barPaint = new Paint();
		barPaint.setAntiAlias(true);
		barPaint.setColor(0xff000000);
		barPaint.setStyle(Paint.Style.FILL);
		
		markPaint = new Paint();
		markPaint.setColor(Color.BLACK);
		markPaint.setStyle(Paint.Style.FILL);		
	}
	
	public void setMeter(int p) {
		if (p > 100)
			p = 100;
		if (p < 0)
			p = 0;
		
		final int value = (int)(2.55 * p);
		final int color = 0xFF000000 | (value <<8) | (value/2);
		
		barPaint.setColor(color);
		pos = (int)((width / 100.0f) * p);
	}
	
	private void setupPaint(int w, int h) {
		width  = w;
		height = h;
		markWidth = w / STEPS;
		
		paintReady = true;
	}
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (w != oldw || h != oldh)
			setupPaint(w,h);
	}
	protected void onDraw(Canvas canvas) {
		if (!paintReady)
			return;
		
		canvas.drawRect(0,0,width,height, barPaint);
		canvas.drawRect(pos-markWidth/2,0,pos+markWidth/2,height,markPaint);
	}
}
