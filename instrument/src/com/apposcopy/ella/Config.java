package com.apposcopy.ella;

import java.io.*;
import java.util.*;

public class Config
{
	public String dxJar;
	public String zipAlign;
	public String apktoolJar;
	public String ellaRuntime;
	public String excludeFile;
	public String inputFile;
	public String outputFile;
	public String outDir;
	public String appId;
	public String serverAddress;
	public String keyStore;
	public String storePass;
	public String keyPass;
	public String alias;
	public String instrumentorClassNames;
	public boolean useAndroidDebug;
	public final Map<String,String> extras = new HashMap();

	private static Config g;

	static Config g()
	{
		if(g == null)
			g = new Config();
		return g;
	}

	private Config()
	{ 
		String ellaSettingsFile = System.getProperty("ella.settings");
		Properties props = new Properties();
		try{
			props.load(new FileInputStream(ellaSettingsFile));
		} catch(Exception e){
			throw new Error("Error reading internal ella settings file "+ellaSettingsFile+".", e);
		}

		load(props);
		loadExtras(props);
	}

	void load(Properties props)
	{
		outDir = props.getProperty("ella.outdir");
		excludeFile = props.getProperty("ella.exclude");
		ellaRuntime = props.getProperty("ella.runtime");
		apktoolJar = props.getProperty("ella.apktool");
		inputFile = props.getProperty("ella.inputfile");
		outputFile = props.getProperty("ella.outputfile");
		appId = props.getProperty("ella.appid");

		String btPath = props.getProperty("ella.android.buildtools.dir");

		dxJar = btPath + File.separator + "lib" + File.separator + "dx.jar";

		File zipAlignFile = new File(btPath, "zipalign");
		if(zipAlignFile.exists())
			zipAlign = zipAlignFile.getPath();
		else {
			zipAlignFile = new File(btPath, File.separator+".."+File.separator+".."+File.separator+"tools"+File.separator+"zipalign");
			if(zipAlignFile.exists())
				zipAlign = zipAlignFile.getPath();
			else
				assert false : zipAlignFile.getPath();
		}
			
		keyStore = props.getProperty("ella.jarsigner.keystore");
		storePass = props.getProperty("ella.jarsigner.storepass");
		keyPass = props.getProperty("ella.jarsigner.keypass");
		alias = props.getProperty("ella.jarsigner.alias");
				
		serverAddress = props.getProperty("ella.server.ip")+":"+props.getProperty("ella.server.port");
		
		instrumentorClassNames = props.getProperty("ella.instrumentor").trim();
		
		useAndroidDebug = props.getProperty("ella.android.debug").equals("true");
	}

	private void loadExtras(Properties props)
	{
		for(String key : props.stringPropertyNames()){
			if(key.startsWith("ella.x."))
				extras.put(key, props.getProperty(key));
		}
	}
}