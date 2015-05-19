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
		private static final int HundredPercentThresh = 600; //We are going to say that 600 is 100 percent. This give us some room to go above 100 percent (up to 768 which would be 128%)
		
		public int adjusted_left_fs0;
		public int adjusted_left_fs1;
		public int adjusted_left_fs2;
		
		public int adjusted_right_fs0;
		public int adjusted_right_fs1;
		public int adjusted_right_fs2;
		
		public int summed_left;
		public int summed_right;
				
		
		
					
		public Calibrator() {
			
		}
		
				
		public void setLeftSensors(
			int fs0, int fs1, int fs2,
			CalibrationCallback calibrationCallback
			)
		{		
			int i;
			int left_sum = fs0+fs1+fs2; //All sensor should already be baselined by the hardware and should have values between 0-255
			
						
			summed_left = left_sum; //Calibration done in the firmware now
			
			//Lets just use raw values here
			adjusted_left_fs0 = fs0;
			adjusted_left_fs1 = fs1;
			adjusted_left_fs2 = fs2;
			
		}
		
		public void setRightSensors(
			int fs0, int fs1, int fs2,
			CalibrationCallback calibrationCallback
			)
		{
			int i;
			int right_sum = fs0+fs1+fs2; //All sensor should already be baselined by the hardware and should have values between 0-255
			
						
			summed_right = right_sum;
						
			adjusted_right_fs0 = fs0;
			adjusted_right_fs1 = fs1;
			adjusted_right_fs2 = fs2;
			
		}
		
		
		
		
	}
}
