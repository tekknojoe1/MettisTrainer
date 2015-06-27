package bendtech.fpd.mettisdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CadenceView extends ImageView {	
	private Paint outterPaint;
	private int center_x;
	private int center_y;
	private double diameter;
	
	private boolean paintReady;
	
	public CadenceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		outterPaint = new Paint();
		outterPaint.setColor(0xff404042);
		outterPaint.setAntiAlias(true);
		outterPaint.setStrokeWidth(4);
		outterPaint.setStyle(Paint.Style.STROKE);
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
	}
}
