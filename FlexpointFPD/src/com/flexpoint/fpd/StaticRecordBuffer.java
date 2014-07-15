package com.flexpoint.fpd;

public class StaticRecordBuffer {
	public static final int MAX_SAMPLES = 2500;	
	private static Buffer buffer = new Buffer();
		
	public void reset() {
		buffer.reset();
	}
	
	public boolean storeLeft(long timeStampNsec, int fs0, int fs1, int fs2) {
		return Buffer.pushSensorData(buffer.left, timeStampNsec, fs0, fs1, fs2);
	}
	public boolean storeRight(long timeStampNsec, int fs0, int fs1, int fs2) {
		return Buffer.pushSensorData(buffer.right,timeStampNsec, fs0, fs1, fs2);
	}
	public boolean storeClub(long timeStampNsec, int fs0, int fs1, int fs2) {
		return Buffer.pushSensorData(buffer.club,timeStampNsec, fs0, fs1, fs2);
	}
	
	public static void pushData(SensorDataSetHandler dataHandler) {
		dataHandler.onData(
			buffer.left, buffer.right, buffer.club
			);
	}
		
	private static class Buffer {		
		public SensorDataSet left  = new SensorDataSet(MAX_SAMPLES);
		public SensorDataSet right = new SensorDataSet(MAX_SAMPLES);
		public SensorDataSet club  = new SensorDataSet(MAX_SAMPLES);
				
		public void reset() {
			left.samplePos = 0;
			right.samplePos = 0;
			club.samplePos = 0;
		}
				
		public static boolean pushSensorData(
			SensorDataSet set, long timeStampNsec, int fs0, int fs1, int fs2
			)
		{
			if (set.samplePos < MAX_SAMPLES) {
				set.timeStampNsec[set.samplePos] = timeStampNsec;
				set.fs0[set.samplePos] = fs0;
				set.fs1[set.samplePos] = fs1;
				set.fs2[set.samplePos] = fs2;
				++set.samplePos;
				return true;
			}
			return false;
		}
	}
}
