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
	public String appId;
	public String keyStore;
	public String storePass;
	public String keyPass;
	public String alias;

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

		dxJar = props.getProperty("dx.jar").trim();
		ellaOutDir = props.getProperty("ella.outdir").trim();
		
		keyStore = props.getProperty("jarsigner.keystore").trim();
		storePass = props.getProperty("jarsigner.storepass").trim();
		keyPass = props.getProperty("jarsigner.keypass").trim();
		alias = props.getProperty("jarsigner.alias").trim();
	}

	String outDir()
	{
		String outDir = ellaOutDir + File.separator + appId;
		new File(outDir).mkdirs();
		return outDir;
	}
}