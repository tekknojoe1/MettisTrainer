package com.flexpoint.fpd;



public class SensorGraphData implements SensorDataSetHandler {
	private static final int MAX_TIME_SHIFT_NSEC = 100 * 1000 * 1000;
	
	private final int sampleRate = 10;
	SensorDataSet outLeft;
	SensorDataSet outRight;
	SensorDataSet outClub;
		
	public void pushData(SensorDataSetHandler dataHandler) {
		dataHandler.onData(outLeft, outRight, outClub);
	}
	
	public void onData(
		SensorDataSet left, SensorDataSet right, SensorDataSet club
		)
	{	
		SensorDataSet idealData = left;
				
		if (right.samplePos > idealData.samplePos)
			idealData = right;
		
		double elapsedTimeMsec = 0;
		if (idealData.samplePos > 0) {
			elapsedTimeMsec =
				((double)(idealData.timeStampNsec[idealData.samplePos-1] - 
				 idealData.timeStampNsec[0]) / (1000*1000));
		}
		
		final int plotSamples = elapsedTimeMsec == 0 ?
			0 : (int)(elapsedTimeMsec / sampleRate);
				
		outLeft  = new SensorDataSet(plotSamples);
		outRight = new SensorDataSet(plotSamples);
		outClub  = new SensorDataSet(plotSamples);
		
		if (plotSamples == 0)
			return;
		
		outLeft.sampleRate  = sampleRate;
		outRight.sampleRate = sampleRate;
		outClub.sampleRate  = sampleRate;
		
		final long sampleRateNsec = (long)sampleRate * 1000 * 1000;
		
		if (left.samplePos > 0) {
			final double scale = (double)left.samplePos / plotSamples;
			 
			for (int i=0; i < plotSamples; ++i) {
				outLeft.timeStampNsec[i] = i * sampleRateNsec;
				outLeft.fs0[i] = left.fs0[(int)(i * scale)];
				outLeft.fs1[i] = left.fs1[(int)(i * scale)];
				outLeft.fs2[i] = left.fs2[(int)(i * scale)];
			}
		}
		outLeft.samplePos = plotSamples;
		
		
		if (right.samplePos > 0) {
			final double scale = (double)right.samplePos / plotSamples;
			 
			for (int i=0; i < plotSamples; ++i) {
				outRight.timeStampNsec[i] = i * sampleRateNsec;
				outRight.fs0[i] = right.fs0[(int)(i * scale)];
				outRight.fs1[i] = right.fs1[(int)(i * scale)];
				outRight.fs2[i] = right.fs2[(int)(i * scale)];
			}
		}
		outRight.samplePos = plotSamples;
	}
}
