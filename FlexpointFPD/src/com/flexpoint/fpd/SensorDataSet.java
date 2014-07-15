package com.flexpoint.fpd;

public class SensorDataSet {
	public final int maxSamples;
	
	public final long[] timeStampNsec;
	public final int[]  fs0;
	public final int[]  fs1;
	public final int[]  fs2;
	
	public int samplePos;
	public int sampleRate;
	
	public SensorDataSet(int maxSamples ) {
		this.maxSamples = maxSamples;
		timeStampNsec = new long[maxSamples];
		fs0 = new int[maxSamples];
		fs1 = new int[maxSamples];
		fs2 = new int[maxSamples];
	}
	
	public SensorDataSet makeCopy() {
		SensorDataSet sensorData = new SensorDataSet(samplePos);
		if (samplePos > 0) {
			System.arraycopy(timeStampNsec, 0, sensorData.timeStampNsec, 0, samplePos);
			System.arraycopy(fs0, 0, sensorData.fs0, 0, samplePos);
			System.arraycopy(fs1, 0, sensorData.fs1, 0, samplePos);
			System.arraycopy(fs2, 0, sensorData.fs2, 0, samplePos);
			sensorData.samplePos = samplePos;
			sensorData.sampleRate = sampleRate;
		}
		return sensorData;
	}
}
