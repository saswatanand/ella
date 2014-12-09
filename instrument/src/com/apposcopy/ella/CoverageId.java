package com.apposcopy.ella;

import java.util.*;

public class CoverageId
{
	private static final Map<Integer,String> idToMethSig = new HashMap();
	private static int id = 0;
	
	public static int idFor(String methodSig)
	{
		id++;
		idToMethSig.put(id, methodSig);
		System.out.println(id + " " + methodSig);
		return id;
	}
	
}