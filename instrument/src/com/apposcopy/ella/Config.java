package com.apposcopy.ella;

import java.io.*;
import java.util.*;

public class Config
{
	public String dxJar;
	public String zipAlign;
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

	private Config(){ }

	void load(String ellaSettingsFile) throws IOException
	{
		Properties props = new Properties();
		props.load(new FileInputStream(ellaSettingsFile));

		String btPath = props.getProperty("android.buildtools.dir");
		if(btPath == null)
			btPath = findAndroidBuildToolsPath();

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
			

		ellaOutDir = props.getProperty("ella.outdir", ellaDir+File.separator+"ella-out").trim();
		
		keyStore = props.getProperty("jarsigner.keystore");
		storePass = props.getProperty("jarsigner.storepass");
		keyPass = props.getProperty("jarsigner.keypass");
		alias = props.getProperty("jarsigner.alias");
		
		if(keyStore != null && storePass != null && keyPass != null && alias != null){
			keyStore = keyStore.trim();
			storePass = storePass.trim();
			keyPass = keyPass.trim();
			alias = alias.trim();
		} else {
			keyStore = ellaDir+File.separator+".android"+File.separator+"debug.keystore";
			storePass = "android";
			keyPass = "android";
			alias = "androiddebugkey";
		}
		
		tomcatUrl = props.getProperty("tomcat.url");
		if(tomcatUrl != null){
			tomcatUrl = tomcatUrl.trim();
			if(!tomcatUrl.startsWith("http://") && !tomcatUrl.startsWith("http://"))
				throw new RuntimeException("The value of tomcat.url must start with either http:// or https://. Current value: "+tomcatUrl);
		} else {
			try{
				tomcatUrl = "http://"+java.net.InetAddress.getLocalHost().getHostAddress()+":8080";
			}catch(java.net.UnknownHostException e){
				throw new Error(e);
			}
		}
		
		instrumentorClassNames = props.getProperty("ella.instrumentor", "com.apposcopy.ella.MethodCoverageInstrumentor").trim();
		
		useAndroidDebug = props.getProperty("ella.android.debug","false").equals("true");
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