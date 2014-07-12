package com.flexpoint.fpd;

import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class SlideDirector {	
	interface OnSlideListener {
		public void onSwipeLeft(Class<?> nextActivity);
		public void onSwipeRight(Class<?> nextActivity);
	}
	
	@SuppressWarnings("deprecation")
	SlideDirector()	{
		gestureDetector = new GestureDetector(new MultiDetector());
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public boolean onTouchEvent(MotionEvent ev) {
		return gestureDetector.onTouchEvent(ev);
	}
	public void setOnSlideListener(OnSlideListener onSlideListener) {
		this.onSlideListener = onSlideListener;
	}
	
	private static Class<?> activities[] = {
		DataActivity.class, RecordActivity.class//Slide2Activity.class,
		//Slide3Activity.class, //Slide4Activity.class,
		//Slide5Activity.class, //Slide6Activity.class,
		//Slide7Activity.class, //Slide8Activity.class,
		//Slide9Activity.class		
	};
	private static int currentActivity = 0;	
	private final GestureDetector gestureDetector;
	private OnSlideListener onSlideListener;
	private boolean enabled;
	
	private class MultiDetector extends SimpleOnGestureListener {
		private static final int SWIPE_MIN_DISTANCE = 120;
		
		@Override
		public boolean onDoubleTap(MotionEvent ev) {
			if (!enabled)
				return true;
			
			if (onSlideListener != null) {
            	if (++currentActivity >= activities.length)
            		currentActivity = 0;
            	onSlideListener.onSwipeLeft(activities[currentActivity]);
            }
			return true;
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			try {
				if (!enabled)
					return false;
				
	            // right to left swipe
	            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) {
	                if (onSlideListener != null) {
	                	if (++currentActivity >= activities.length)
	                		currentActivity = 0;
	                	onSlideListener.onSwipeLeft(activities[currentActivity]);
	                }
	            	
	            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {
	                if (onSlideListener != null) {
	                	if (--currentActivity < 0)
	                		currentActivity = activities.length-1;
	                	onSlideListener.onSwipeRight(activities[currentActivity]);
	                }
	            }
	        } catch (Exception e) {
	            // nothing
	        }
	        return false;
		}		
	}
}