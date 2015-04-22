package com.flexpoint.fpd;

public class StaticDynamicCalibration {
	private static Calibrator calibrator = new Calibrator();
	//private static NullCalibrator calibrator = new NullCalibrator();
	
	public void setLeftSensors(int fs0, int fs1, int fs2) {
		calibrator.setLeftSensors(fs0, fs1, fs2);
	}
	public void setRightSensors(int fs0, int fs1, int fs2) {
		calibrator.setRightSensors(fs0, fs1, fs2);
	}
	
	public int adjusted_left_fs0() {
		return calibrator.left_fs0;
	}
	public int adjusted_left_fs1() {
		return calibrator.left_fs1;
	}
	public int adjusted_left_fs2() {
		return calibrator.left_fs2;
	}
	public int summed_left() {
		return calibrator.left_sum;
	}
	
	public int adjusted_right_fs0() {
		return calibrator.right_fs0;
	}
	public int adjusted_right_fs1() {
		return calibrator.right_fs1;
	}
	public int adjusted_right_fs2() {
		return calibrator.right_fs2;
	}
	public int summed_right() {
		return calibrator.right_sum;
	}
		
	private static class NullCalibrator {
		public int left_sum;
		public int right_sum;
				
		public int left_fs0;
		public int left_fs1;
		public int left_fs2;
		
		public int right_fs0;
		public int right_fs1;
		public int right_fs2;
				
		public void setLeftSensors(int fs0, int fs1, int fs2) {
			left_fs0 = fs0;
			left_fs1 = fs1;
			left_fs2 = fs2;
			
			left_sum = left_fs0+left_fs1+left_fs2;
		}
		public void setRightSensors(int fs0, int fs1, int fs2) {
			right_fs0 = fs0;
			right_fs1 = fs1;
			right_fs2 = fs2;
			
			right_sum = right_fs0+right_fs1+right_fs2;
		}
	}
	
	private static class Calibrator {
		public int left_sum;
		public int right_sum;
		
		private static final int MaxSensors = 3;
		
		int minLeftA[] = new int[MaxSensors];
		int maxLeftA[] = new int[MaxSensors];
		
		int minRightA[] = new int[MaxSensors];
		int maxRightA[] = new int[MaxSensors];
		
		public int left_fs0;
		public int left_fs1;
		public int left_fs2;
		
		public int right_fs0;
		public int right_fs1;
		public int right_fs2;
			
		public Calibrator() {
			for (int i=0; i < MaxSensors; ++i) {
				minLeftA[i]  = 64;
				maxLeftA[i]  = 192;
				minRightA[i] = 64;
				maxRightA[i] = 192;
			}
		}
		
		public void setLeftSensors(int fs0, int fs1, int fs2) {		
			if (fs0 < minLeftA[0])
				minLeftA[0] = fs0;
			if (fs1 < minLeftA[1])
				minLeftA[1] = fs1;
			if (fs2 < minLeftA[2])
				minLeftA[2] = fs2;
			
			if (fs0 > maxLeftA[0])
				maxLeftA[0] = fs0;
			if (fs1 > maxLeftA[1])
				maxLeftA[1] = fs1;
			if (fs2 > maxLeftA[2])
				maxLeftA[2] = fs2;
			
			left_fs0 = 256*(fs0-minLeftA[0])/(maxLeftA[0]-minLeftA[0]);
			left_fs1 = 256*(fs1-minLeftA[1])/(maxLeftA[1]-minLeftA[1]);
			left_fs2 = 256*(fs2-minLeftA[2])/(maxLeftA[2]-minLeftA[2]);
			
			left_sum = left_fs0+left_fs1+left_fs2;
		}
		public void setRightSensors(int fs0, int fs1, int fs2) {
			if (fs0 < minRightA[0])
				minRightA[0] = fs0;
			if (fs1 < minRightA[1])
				minRightA[1] = fs1;
			if (fs2 < minRightA[2])
				minRightA[2] = fs2;
			
			if (fs0 > maxRightA[0])
				maxRightA[0] = fs0;
			if (fs1 > maxRightA[1])
				maxRightA[1] = fs1;
			if (fs2 > maxRightA[2])
				maxRightA[2] = fs2;
			
			right_fs0 = 256*(fs0-minRightA[0])/(maxRightA[0]-minRightA[0]);
			right_fs1 = 256*(fs1-minRightA[1])/(maxRightA[1]-minRightA[1]);
			right_fs2 = 256*(fs2-minRightA[2])/(maxRightA[2]-minRightA[2]);
			
			right_sum = right_fs0+right_fs1+right_fs2;
		}
	}
}
