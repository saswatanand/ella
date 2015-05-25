package com.apposcopy.ella.runtime;

public class ValueRecorder extends SequenceRecorder
{
	public void v(Object obj, int metadata)
	{
		int hashCode = System.identityHashCode(obj);
		record(hashCode, metadata);
	}
}