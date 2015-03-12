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
	public String tomcatUrl;
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

		dxJar = props.getProperty("dx.jar");
		if(dxJar == null){
			String dxPath = props.getProperty("dx");
			String btPath;
			if(dxPath == null){
				btPath = findAndroidBuildToolsPath();
			}else{
				dxPath = dxPath.trim();
				assert dxPath.substring(dxPath.length()-2).equals("dx");
				btPath = dxPath.substring(0, dxPath.length()-2);
			}
			dxJar = btPath + File.separator + "lib" + File.separator + "dx.jar";
			System.out.println("dxJar = "+dxJar);
		}
		dxJar = dxJar.trim();

		ellaOutDir = props.getProperty("ella.outdir", ellaDir+File.separator+"ella-out").trim();
		
		keyStore = props.getProperty("jarsigner.keystore").trim();
		storePass = props.getProperty("jarsigner.storepass").trim();
		keyPass = props.getProperty("jarsigner.keypass").trim();
		alias = props.getProperty("jarsigner.alias").trim();
		
		tomcatUrl = props.getProperty("tomcat.url").trim();
	}

	String outDir()
	{
		String outDir = ellaOutDir + File.separator + appId;
		new File(outDir).mkdirs();
		return outDir;
	}

	public static String findAndroidBuildToolsPath() {
		String rtn = null;
		String[] sCmdExts = {""}; //TODO: support windows?
		StringTokenizer st = new StringTokenizer(System.getenv("PATH"), File.pathSeparator);
		String path, sFile;
		while (st.hasMoreTokens()) {
			path = st.nextToken();
			for (String sExt : sCmdExts) {
				sFile = path + File.separator + "dx" + sExt;
				if (new File(sFile).isFile()) {
					return path;
				}
			}
		}
		return null;
	}
}