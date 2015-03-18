package com.apposcopy.ella.runtime;

public class MethodSequenceRecorder implements CoverageRecorder
{
	protected int[] sequence = new int[10000];
	protected long[] timeStamps = new long[10000];
		
	protected int index = 0;

	public void m(int mId)
	{
		if(index >= sequence.length){
			int[] oldSeq = sequence;
			int newLen = oldSeq.length*2;
			int[] newSeq = new int[newLen];
			System.arraycopy(oldSeq, 0, newSeq, 0, oldSeq.length);
			sequence = newSeq;
			
			long[] oldTimeStamps = timeStamps;
			long[] newTimeStamps = new long[newLen];
			System.arraycopy(oldTimeStamps, 0, newTimeStamps, 0, oldTimeStamps.length);
			timeStamps = newTimeStamps;
		}
		sequence[index] = mId;			
		timeStamps[index] = System.currentTimeMillis();
		index++;
	}
	
	public String data()
	{
		StringBuilder builder = new StringBuilder();
		int[] seq = sequence;
		long[] ts = timeStamps;
		for(int i = 0; i < index; i++)
			builder.append(" "+ts[i]+" "+seq[i]);
		return builder.toString();
	}
}