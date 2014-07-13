package com.flexpoint.fpd;

import java.util.concurrent.TimeUnit;

import android.util.Log;

public class SensorGraphData implements SensorDataSetHandler {
	private static final int MAX_TIME_SHIFT_NSEC = 100 * 1000 * 1000;
	
	private final int sampleRate = 10;
	private int samples;
	SensorDataSet outLeft;
	SensorDataSet outRight;
	SensorDataSet outClub;
		
	public void HandleData(
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
		if (right.samplePos > 0) {
			final double scale = (double)right.samplePos / plotSamples;
			 
			for (int i=0; i < plotSamples; ++i) {
				outRight.timeStampNsec[i] = i * sampleRateNsec;
				outRight.fs0[i] = right.fs0[(int)(i * scale)];
				outRight.fs1[i] = right.fs1[(int)(i * scale)];
				outRight.fs2[i] = right.fs2[(int)(i * scale)];
			}	
		}
		
		
		
	}
	
	/* 
	 	//final double guessedSamplesPerSec =
		//	(1.0/((double)idealData.samplePos / elapsedTimeMsec));
	  
	   Log.i("ANALYZED", "time: " + elapsedTimeMsec +
			", " + guessedSamplesPerSec +
			", samples: " + idealData.samplePos + ", " + samples);
	public void HandleData(
		SensorDataSet left, SensorDataSet right, SensorDataSet club
		)
	{	
		long maxEndTimeNsec = 0;
		
		if (left.samplePos > 0) {			
			final int lastPos = left.samplePos - 1;			
			if (left.timeStampNsec[lastPos] > maxEndTimeNsec)
				maxEndTimeNsec = left.timeStampNsec[lastPos];
		}
		if (right.samplePos > 0) {
			final int lastPos = right.samplePos - 1;
			if (right.timeStampNsec[lastPos] > maxEndTimeNsec)
				maxEndTimeNsec = right.timeStampNsec[lastPos];
		}
		if (club.samplePos > 0) {
			final int lastPos = club.samplePos - 1;
			if (club.timeStampNsec[lastPos] > maxEndTimeNsec)
				maxEndTimeNsec = club.timeStampNsec[lastPos];
		}
		
		long minStartTimeNsec = maxEndTimeNsec;
		
		if (left.samplePos > 0) {			
			if (left.timeStampNsec[0] < minStartTimeNsec)
				minStartTimeNsec = left.timeStampNsec[0];
		}
		if (right.samplePos > 0) {			
			if (right.timeStampNsec[0] < minStartTimeNsec)
				minStartTimeNsec = right.timeStampNsec[0];
		}
		if (club.samplePos > 0) {
			if (club.timeStampNsec[0] < minStartTimeNsec)
				minStartTimeNsec = club.timeStampNsec[0];
		}
		
		final long elapsedTimeNsec = maxEndTimeNsec - minStartTimeNsec;
		if (elapsedTimeNsec < 1)
			return;
		
		long leftTimeShiftNsec  = 0;
		long rightTimeShiftNsec = 0;
		long clubTimeShiftNsec  = 0;
		
		if (left.samplePos > 0) {			
			if (left.timeStampNsec[0] > minStartTimeNsec) {
				leftTimeShiftNsec = left.timeStampNsec[0] - minStartTimeNsec;
				if (leftTimeShiftNsec > MAX_TIME_SHIFT_NSEC)
					leftTimeShiftNsec = MAX_TIME_SHIFT_NSEC;
			}
		}
		if (right.samplePos > 0) {			
			if (right.timeStampNsec[0] > minStartTimeNsec) {
				rightTimeShiftNsec = right.timeStampNsec[0] - minStartTimeNsec;
				if (rightTimeShiftNsec > MAX_TIME_SHIFT_NSEC)
					rightTimeShiftNsec = MAX_TIME_SHIFT_NSEC;
			}
		}
		if (club.samplePos > 0) {
			if (club.timeStampNsec[0] > minStartTimeNsec) {
				clubTimeShiftNsec = club.timeStampNsec[0] - minStartTimeNsec;
				if (clubTimeShiftNsec > MAX_TIME_SHIFT_NSEC)
					clubTimeShiftNsec = MAX_TIME_SHIFT_NSEC;
			}
		}
		
		final int msecs = (int)(elapsedTimeNsec / (1000*1000));
				
		Log.i("ELAPSED TIME", "" + msecs + ", samples: " + right.samplePos);
		
		samples = msecs / sampleRate;
		sensorData = new SensorDataSet(samples);
			
		
		
	}
	*/
}
