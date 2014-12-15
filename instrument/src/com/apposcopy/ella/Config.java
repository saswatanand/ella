package com.apposcopy.ella;

import java.io.*;
import java.util.*;

public class Config
{
	public String dxJar;
	public String apktoolJar;
	public String ellaDir;
	public String ellaRuntime;
	public String excludeFile;
	public String inputFile;
	public String outputFile;
	public String ellaOutDir;

	private static Config g;

	static Config g()
	{
		if(g == null)
			g = new Config();
		return g;
	}

	private Config(){ }

	void load(String ellaSettingsFile) throws IOException
	{
		Properties props = new Properties();
		props.load(new FileInputStream(ellaSettingsFile));

		dxJar = props.getProperty("dx.jar");
		ellaOutDir = props.getProperty("ella.outdir");
	}

	String outDir() throws IOException
	{
		String p = new File(inputFile).getCanonicalPath().replace(File.separatorChar, '_');
		String outDir = ellaOutDir + File.separator + p;
		new File(outDir).mkdirs();
		return outDir;
	}
}