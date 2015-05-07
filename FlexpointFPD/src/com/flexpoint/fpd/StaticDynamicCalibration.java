package com.flexpoint.fpd;

import android.util.Log;

public class StaticDynamicCalibration {
	private static final String LOG_TAG = "Calibration";
	
	private static Calibrator calibrator = new Calibrator();
	
	public interface CalibrationCallback {
		public void onCalibrationComplete();
	}
	
	
	public void setLeftSensors(
		int fs0, int fs1, int fs2,
		CalibrationCallback calibrationCallback
		)
	{
		calibrator.setLeftSensors(fs0, fs1, fs2, calibrationCallback);
	}
	public void setRightSensors(
		int fs0, int fs1, int fs2,
		CalibrationCallback calibrationCallback
		)
	{
		calibrator.setRightSensors(fs0, fs1, fs2, calibrationCallback);
	}
	
	public int adjusted_left_fs0() {
		return calibrator.adjusted_left_fs0;
	}
	public int adjusted_left_fs1() {
		return calibrator.adjusted_left_fs1;
	}
	public int adjusted_left_fs2() {
		return calibrator.adjusted_left_fs2;
	}
	public int summed_left() {
		return calibrator.summed_left;
	}
	
	public int adjusted_right_fs0() {
		return calibrator.adjusted_right_fs0;
	}
	public int adjusted_right_fs1() {
		return calibrator.adjusted_right_fs1;
	}
	public int adjusted_right_fs2() {
		return calibrator.adjusted_right_fs2;
	}
	public int summed_right() {
		return calibrator.summed_right;
	}
	
	private static void LogD(String msg)
	{
		Log.i(LOG_TAG, msg);
	}
	
	private static class Calibrator {
				
		private static final int MaxSensors = 3;
		private static final int SampleSize = 16;
		private static final int StillnessThresh = 5;
		private static final int StillnessTime = 60;
		private static final int HundredPercentThresh = 600; //We are going to say that 600 is 100 percent. This give us some room to go above 100 percent (up to 768 which would be 128%)
		
		public int adjusted_left_fs0;
		public int adjusted_left_fs1;
		public int adjusted_left_fs2;
		
		public int adjusted_right_fs0;
		public int adjusted_right_fs1;
		public int adjusted_right_fs2;
		
		
		public int left_max_value;
		public int right_max_value;
		
		public int summed_left;
		public int summed_right;
				
		int left_hist[] = new int[SampleSize];
		int left_sample_ptr;
		int left_hist_sum;
		int left_ave;
		int left_stillness;
		int left_stillness_timer = StillnessTime;
		int left_baseline;
		
		int right_hist[] = new int[SampleSize];
		int right_sample_ptr;
		int right_hist_sum;
		int right_ave;
		int right_stillness;
		int right_stillness_timer = StillnessTime;
		int right_baseline;
					
		public Calibrator() {
			left_max_value = HundredPercentThresh;
			left_hist_sum = 0;
			left_sample_ptr = 0;
			left_stillness = 0;
			
			right_max_value = HundredPercentThresh;
			right_hist_sum = 0;
			right_sample_ptr = 0;
			right_stillness = 0;
		}
		
		public void setLeftSensors(
			int fs0, int fs1, int fs2,
			CalibrationCallback calibrationCallback
			)
		{		
			int i;
			int left_sum = fs0+fs1+fs2; //All sensor should already be baselined by the hardware and should have values between 0-255
			
			left_hist_sum -= left_hist[left_sample_ptr]; //Remove latest sample
			left_hist[left_sample_ptr] = left_sum; //Add newest sample
			left_hist_sum += left_hist[left_sample_ptr]; //Update history sum with newest sample
			left_ave = left_hist_sum / SampleSize;
			
			left_sample_ptr++;
			if (left_sample_ptr >= SampleSize) {
				left_sample_ptr = 0;
			}
			
			//Compute left stillness
			left_stillness = 0;
			for (i=0;i<SampleSize;i++) {
				left_stillness += Math.abs(left_hist[i] - left_ave);
			}
			left_stillness /= SampleSize;
			
			if (left_stillness < StillnessThresh) {
				
				if (left_stillness_timer == 0) {
					
					if (left_ave > (2 * right_ave) ) {
						calibrationCallback.onCalibrationComplete();
						//Standing on left foot
						left_max_value = left_ave; //Store max value
						
					} else if (right_ave > (2 * left_ave) )
						
						//Standing on right foot, assume left foot is in the air (not loaded)
						left_baseline = left_ave;						
					
				} else {
					left_stillness_timer--;
				}
				
			} else {
				left_stillness_timer = StillnessTime;
			}
			
			summed_left = left_sum - left_baseline;
			if (summed_left < 0) {
				summed_left = 0;
			}
			
			summed_left = summed_left * HundredPercentThresh / left_max_value;
			
			//Lets just use raw values here
			adjusted_left_fs0 = fs0;
			adjusted_left_fs1 = fs1;
			adjusted_left_fs2 = fs2;
			
			LogD("left stillness = " + left_stillness + " summed left " + left_sum);
		}
		
		public void setRightSensors(
			int fs0, int fs1, int fs2,
			CalibrationCallback calibrationCallback
			)
		{
			int i;
			int right_sum = fs0+fs1+fs2; //All sensor should already be baselined by the hardware and should have values between 0-255
			
			right_hist_sum -= right_hist[right_sample_ptr]; //Remove latest sample
			right_hist[right_sample_ptr] = right_sum; //Add newest sample
			right_hist_sum += right_hist[right_sample_ptr]; //Update history sum with newest sample
			right_ave = right_hist_sum / SampleSize;
			
			right_sample_ptr++;
			if (right_sample_ptr >= SampleSize) {
				right_sample_ptr = 0;
			}
			
			//Compute left stillness
			right_stillness = 0;
			for (i=0;i<SampleSize;i++) {
				right_stillness += Math.abs(right_hist[i] - right_ave);
			}
			right_stillness /= SampleSize;
			
			if (right_stillness < StillnessThresh) {
				
				if (right_stillness_timer == 0) {
					
					if (right_ave > (2 * left_ave) ) {
						calibrationCallback.onCalibrationComplete();
						//Standing on right foot
						right_max_value = right_ave; //Store max value
						
					} else if (left_ave > (2 * right_ave) )
						
						//Standing on left foot, assume right foot is in the air (not loaded)
						right_baseline = right_ave;						
					
				} else {
					right_stillness_timer--;
				}
				
			} else {
				right_stillness_timer = StillnessTime;
			}
			
			summed_right = right_sum - right_baseline;
			if (summed_right < 0) {
				summed_right = 0;
			}
			
			summed_right = summed_right * HundredPercentThresh / right_max_value;
						
			adjusted_right_fs0 = fs0;
			adjusted_right_fs1 = fs1;
			adjusted_right_fs2 = fs2;
			
		}
		
		
		
		
	}
}
