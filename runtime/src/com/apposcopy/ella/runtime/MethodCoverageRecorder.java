package com.apposcopy.ella.runtime;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MethodCoverageRecorder extends Recorder
{
	protected Set<Integer> coverage = Collections.newSetFromMap(new ConcurrentHashMap<Integer,Boolean>());

	public void m(int mId)
	{
		coverage.add(mId);
	}
	
	public String data()
	{
		StringBuilder builder = new StringBuilder();
		for(int mId : coverage){
			builder.append(mId).append('\n');
		}
		return builder.toString();
	}

}
