package com.apposcopy.app;

import java.util.*;

public class Component
{
	public enum Type { activity, service, receiver, other }

	public final String name;
	public final Type type;
	public boolean exported = false;
	public final List<IntentFilter> intentFilters = new ArrayList();

	public Component(String name, Type type)
	{
		this.name = name;
		this.type = type;
	}

	public Component(String name)
	{
		this(name, Type.other);
	}

	public void addIntentFilter(IntentFilter ifilter)
	{
		intentFilters.add(ifilter);
	}

	public String toString()
	{
		StringBuilder builder = new StringBuilder("component : {");
		builder.append("name: "+name+", ");
		builder.append("type: "+type+", ");
		int len = intentFilters.size();
		for(int i = 0; i < len; i++){
			builder.append(intentFilters.get(i).toString());
			if(i < (len-1))
				builder.append(", ");
		}
		builder.append("} ");
		return builder.toString();
	}
}