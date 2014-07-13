package com.flexpoint.fpd;

public class SensorDataSet {
	public final int maxSamples;
	
	public final long[] timeStampNsec;
	public final int[]  fs0;
	public final int[]  fs1;
	public final int[]  fs2;
	
	public int samplePos;
	
	public SensorDataSet(int maxSamples ) {
		this.maxSamples = maxSamples;
		timeStampNsec = new long[maxSamples];
		fs0 = new int[maxSamples];
		fs1 = new int[maxSamples];
		fs2 = new int[maxSamples];
	}
}
