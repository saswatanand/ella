package com.apposcopy.ella.runtime;

public class MethodSequenceRecorder extends SequenceRecorder
{
	public void m(int methodId)
	{
		long time = System.currentTimeMillis();
		record(methodId, time);
	}
}