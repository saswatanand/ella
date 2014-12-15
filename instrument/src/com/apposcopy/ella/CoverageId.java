package com.apposcopy.ella;

import java.util.*;
import java.io.*;

public class CoverageId
{
	private final List<String> meths = new ArrayList();
	private int id = 0;

	private static CoverageId g;

	static CoverageId g()
	{
		if(g == null)
			g = new CoverageId();
		return g;
	}

	public int idFor(String methodSig)
	{
		int id = meths.size();
		System.out.println(id + " " + methodSig);
		meths.add(methodSig);
		return id;
	}

	public void dump() throws IOException
	{
		File dumpDir = new File(Config.g().outDir());
		dumpDir.mkdirs();
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dumpDir, "covids")));
		for(String m : meths)
			writer.write(m+"\n");
		writer.close();
	}
	
}