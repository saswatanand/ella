package com.apposcopy.ella.runtime;

import java.util.*;

public class MethodCoverageRecorder implements CoverageRecorder
{
	protected BitSet coverage = new BitSet();

	public void m(int mId)
	{
		coverage.set(mId);
	}
	
	public String data()
	{
		return coverage.toString();
	}
	
	public boolean supportsContinuousReporting() {
		return false;
	}

}
