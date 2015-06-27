package bendtech.fpd.mettisdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SensorPadView extends ImageView {
	private Paint outterPaint;
	private Paint innerPaint;
	private int center_x;
	private int center_y;
	private double diameter;
	
	private boolean paintReady;

	public SensorPadView(Context context, AttributeSet attr) {
		super(context, attr);
		
		outterPaint = new Paint();
		outterPaint.setColor(Color.BLACK);
		outterPaint.setAntiAlias(true);
		outterPaint.setStrokeWidth(4);
		outterPaint.setStyle(Paint.Style.STROKE);
		
		innerPaint = new Paint();
		innerPaint.setAntiAlias(true);
		innerPaint.setColor(0xff707072);
		innerPaint.setStyle(Paint.Style.FILL);
	}
	
	private void setupPaint(int w, int h) {
		center_x = w/2;
		center_y = h/2;
		diameter = Math.sqrt(w * w + h * h);
		
		paintReady = true;
	}
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (w != oldw || h != oldh)
			setupPaint(w,h);
	}
	protected void onDraw(Canvas canvas) {
		if (!paintReady)
			return;
		
		canvas.drawCircle(
			center_x, center_y, 0.333f * (float)diameter, outterPaint
			);
		canvas.drawCircle(
			1.0f + center_x, 1.0f + center_y, 0.25f * (float)diameter, innerPaint
			);
	}
}
