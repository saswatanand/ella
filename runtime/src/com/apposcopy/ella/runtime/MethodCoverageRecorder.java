package com.apposcopy.ella.runtime;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MethodCoverageRecorder implements CoverageRecorder
{
	protected Set<Integer> coverage = Collections.newSetFromMap(new ConcurrentHashMap<Integer,Boolean>());

	public void m(int mId)
	{
		coverage.add(mId);
	}
	
	public String data()
	{
		return coverage.toString();
	}

}
