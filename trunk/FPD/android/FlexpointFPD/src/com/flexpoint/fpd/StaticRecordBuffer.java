package com.flexpoint.fpd;

public class StaticRecordBuffer {
	public static final int MAX_SAMPLES = 2000;	
	private static Buffer buffer = new Buffer();
	
	private static class DataSet {
		public long[] timeStamp = new long[MAX_SAMPLES];
		public int[] fs0 = new int[MAX_SAMPLES];
		public int[] fs1 = new int[MAX_SAMPLES];
		public int[] fs2 = new int[MAX_SAMPLES];
		
		public int pos;
	}
	
	public static interface OnAnalyzeData {
		public void OnData(DataSet left, DataSet right, DataSet club);
	}
	
	public void reset() {
		buffer.reset();
	}
	
	public boolean storeLeft(long timeStamp, int fs0, int fs1, int fs2) {
		return Buffer.pushSensorData(buffer.left, timeStamp, fs0, fs1, fs2);
	}
	public boolean storeRight(long timeStamp, int fs0, int fs1, int fs2) {
		return Buffer.pushSensorData(buffer.right,timeStamp, fs0, fs1, fs2);
	}
	public boolean storeClub(long timeStamp, int fs0, int fs1, int fs2) {
		return Buffer.pushSensorData(buffer.club,timeStamp, fs0, fs1, fs2);
	}
	
	public static void analyzeData(OnAnalyzeData analyzer) {
		analyzer.OnData(
			buffer.left, buffer.right, buffer.club
			);
	}
		
	private static class Buffer {		
		public DataSet left  = new DataSet();
		public DataSet right = new DataSet();
		public DataSet club  = new DataSet();
				
		public void reset() {
			left.pos = 0;
			right.pos = 0;
			club.pos = 0;
		}
				
		public static boolean pushSensorData(
			DataSet set, long timeStamp, int fs0, int fs1, int fs2
			)
		{
			if (set.pos < MAX_SAMPLES) {
				set.fs0[set.pos] = fs0;
				set.fs1[set.pos] = fs1;
				set.fs2[set.pos] = fs2;
				++set.pos;
				return true;
			}
			return false;
		}
	}
}
