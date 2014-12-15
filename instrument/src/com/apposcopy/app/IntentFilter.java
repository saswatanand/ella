package com.apposcopy.app;

import java.util.*;

public class IntentFilter
{
	public final Set<String> actions = new HashSet();
	public final Set<Data> data = new HashSet();
	public final Set<String> categories = new HashSet();
	
	private int priority;

	public void setPriority(String p)
	{
		int pr = Integer.parseInt(p);
		this.priority = pr;
	}

	public int getPriority()
	{
		return this.priority;
	}

	public void addAction(String action)
	{
		actions.add(action);
	}

	public void addData(Data dt)
	{
		data.add(dt);
	}

	public void addCategory(String cat)
	{
		categories.add(cat);
	}
	
	public boolean isMAIN()
	{
		for(String act : actions){
			if(act.equals("android.intent.action.MAIN"))
				return true;
		}
		return false;
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder("intent-filter: { ");
		builder.append("actions: {");
		int len = actions.size();
		int i = 0;
		for(String act : actions){
			builder.append(act);
			if(i < (len-1))
				builder.append(", ");
			i++;
		}
		builder.append("} ");

		builder.append("data: {");
		len = data.size();
		i = 0;
		for(Data dt : data){
			builder.append(dt.toString());
			if(i < (len-1))
				builder.append(", ");
			i++;
		}
		builder.append("} ");

		builder.append("} ");
		return builder.toString();
	}
}
