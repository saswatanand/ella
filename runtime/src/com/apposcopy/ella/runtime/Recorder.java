package com.apposcopy.ella.runtime;

public abstract class Recorder
{
	public void m(int methodId)
	{
		throw new RuntimeException("Abstract recorder");
	}

	public void v(Object obj, int metadata)
	{
		throw new RuntimeException("Abstract recorder");
	}

	public String data()
	{
		throw new RuntimeException("Abstract recorder");
	}
}
