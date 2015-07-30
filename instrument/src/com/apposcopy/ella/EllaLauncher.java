package com.apposcopy.ella;

import java.io.*;
import java.util.*;
import java.security.MessageDigest;

/*
 * @author Saswat Anand
 */
public class EllaLauncher
{
	private String ellaDir;
	private Properties settingsProp;
	private Properties commandLineProp;
	private String ellaSettingsDir;
	private String userDir;
	private String command;

	public static void main(String[] args)
	{
		EllaLauncher launcher = new EllaLauncher();
		launcher.setup(args);
		launcher.instrument();
	}

	void setup(String[] args)
	{
		commandLineProp = new Properties();

		int i = 0;
		while(i < args.length){
			String key = args[i];
			if(key.startsWith("-")){
				String value = args[i+1];
				commandLineProp.put(key.substring(1), value);
				i++;
			} else if(command == null){
				command = key;
			} else if(commandLineProp.getProperty("ella.inputfile") == null){
				commandLineProp.put("ella.inputfile", key);
			} else if(commandLineProp.getProperty("ella.outputfile") == null){
				commandLineProp.put("ella.outputfile", key);
			} else {
				System.out.println("WARN: ignoring commandline argument "+key);
			}
			i++;
		}


		ellaDir = findEllaDir();
		//System.out.println("ella dir: "+ellaDir);

		String ellaSettingsFilePath = commandLineProp.getProperty("ella.settings");
		File ellaSettingsFile = ellaSettingsFilePath == null ? new File(ellaDir, "ella.settings") : new File(ellaSettingsFilePath);
		ellaSettingsDir = ellaSettingsFile.getAbsoluteFile().getParentFile().getPath();
		userDir = System.getProperty("user.dir");

		settingsProp = new Properties();
		try{
			settingsProp.load(new FileInputStream(ellaSettingsFile));
		} catch(Exception e){
			throw new Error("Error reading settings file "+ellaSettingsFilePath+".", e);
		}
	}

	String getProperty(String key)
	{
		String v = commandLineProp.getProperty(key);
		return v == null ? settingsProp.getProperty(key) : v;
	}

	String getPathProperty(String key)
	{
		String v = commandLineProp.getProperty(key);
		if(v != null){
			File f = new File(v);
			return f.isAbsolute() ? v : new File(userDir, v).getPath();
		}
		v = settingsProp.getProperty(key);
		if(v != null){
			File f = new File(v);
			return f.isAbsolute() ? v : new File(ellaSettingsDir, v).getPath();
		}
		return null;
	}	

	String findEllaDir()
	{
		String classPath = System.getProperty("java.class.path");
		//System.out.println(classPath);
		for(String cp : classPath.split(File.pathSeparator)){
			File file = new File(cp);
			if(file.isFile() && file.getName().equals("ella.instrument.jar")){
				try{
					return file.getAbsoluteFile().getParentFile().getParentFile().getCanonicalPath(); 
				}catch(IOException e){
					throw new Error("Error occurred while finding canonical path.", e);
				}
			}
		}
		throw new RuntimeException("Could not determine Ella directory");
	}
	
	void instrument()
	{
		Properties outProps = new Properties();

		String apk = getProperty("ella.inputfile");
		File apkFile = new File(apk);
		if(!apkFile.isFile())
			throw new Error("Input file "+apk+" does not exist or is not a file.");

		String ellaOutDir = getPathProperty("ella.outdir");

		//ella.inputfile
		outProps.setProperty("ella.inputfile", apkFile.getPath());

		//ella.appid
		String appId = getProperty("ella.appid");
		if(appId == null){
			try{
				appId = appPathToAppId(apkFile.getCanonicalPath());
			}catch(IOException e){
				throw new Error("Error occurred while finding canonical path of "+apkFile, e);
			}
		}
		outProps.setProperty("ella.appid", appId);

		//ella.outdir
		String outDir = ellaOutDir + File.separator + appId;
		new File(outDir).mkdirs();
		outProps.setProperty("ella.outdir", outDir);
		
		//ella.outputfile
		String outputFilePath = getProperty("ella.outputfile");
		File outputFile = outputFilePath != null ? new File(outputFilePath) : new File(outDir, "instrumented.apk");
		outputFile.getAbsoluteFile().getParentFile().mkdirs();
		outProps.setProperty("ella.outputfile", outputFile.getPath());

		//ella.exclude
		outProps.setProperty("ella.exclude", getPathProperty("ella.exclude"));

		////ella.dir
		//outProps.setProperty("ella.dir", ellaDir);
		
		//ella.runtime
		outProps.setProperty("ella.runtime", ellaDir+File.separator+"bin"+File.separator+"ella.runtime.dex");
		
		//ella.apktool
		String[] apktoolJar = new File(ellaDir+File.separator+"bin").list(new FilenameFilter(){ 
				public boolean accept(File dir, String name){
					return name.startsWith("apktool");
				}
			});
		assert apktoolJar.length == 1;
		outProps.setProperty("ella.apktool", ellaDir+File.separator+"bin"+File.separator+apktoolJar[0]);

		//android.buildtools.dir
		String btPath = getPathProperty("android.buildtools.dir");
		if(btPath == null){
			String sdkPath = findAndroidSDKPath();
			btPath = findAndroidBuildToolsPath(sdkPath);
			//System.out.println("path to build-tools directory: "+btPath);			
		}
		File dxPath = new File(btPath, "dx");
		if(!dxPath.isFile())
			throw new Error("The configuration variable android.buildtools.dir is probably not set correctly. Current value is "+btPath);
		outProps.setProperty("android.buildtools.dir", btPath);
		
		//ella.android.debug
		outProps.setProperty("ella.android.debug", getProperty("ella.android.debug"));

		//ella.instrumentor
		String instrumentorClassNames = getProperty("ella.instrumentor");
		for(String className : instrumentorClassNames.split(",")){
			//check if it exists in the ella.instrument.jar
		}
		outProps.setProperty("ella.instrumentor", instrumentorClassNames);

		//jarsigner
		String keyStore = getPathProperty("ella.jarsigner.keystore");
		String storePass = getProperty("ella.jarsigner.storepass");
		String keyPass = getProperty("ella.jarsigner.keypass");
		String alias = getProperty("ella.jarsigner.alias");

		if(keyStore == null || storePass == null || keyPass == null || alias == null)
			throw new Error("unexpected settings. keyStore: "+keyStore+" storePass: "+storePass+" keyPass: "+keyPass+" alias: "+alias);
		if(!new File(keyStore).isFile())
			throw new Error("keyStore file "+keyStore+" does not exist.");

		outProps.setProperty("ella.jarsigner.keystore", keyStore);
		outProps.setProperty("ella.jarsigner.storepass", storePass);
		outProps.setProperty("ella.jarsigner.keypass", keyPass);
		outProps.setProperty("ella.jarsigner.alias", alias);
		
		//ella.server.ip
		String serverIp = getProperty("ella.server.ip");
		if(serverIp != null){
			serverIp = serverIp.trim();
		} else {
			if(getProperty("ella.use.emulator.host.loopback").equals("true")){
				serverIp = "10.0.2.2";
			} else {
				try{
					serverIp = java.net.InetAddress.getLocalHost().getHostAddress();
				}catch(java.net.UnknownHostException e){
					throw new Error(e);
				}
			}
		}
		outProps.setProperty("ella.server.ip", serverIp);

		//ella.server.port
		int port = Integer.parseInt(getProperty("ella.server.port"));
		if(port < 0 || port > 65535)
			throw new Error("port number "+port+" is not between 0 and 65535");
		outProps.setProperty("ella.server.port", String.valueOf(port));

		//ella.x.*
		for(String key : settingsProp.stringPropertyNames()){
			if(key.startsWith("ella.x."))
				outProps.setProperty(key, settingsProp.getProperty(key));
		}
		for(String key : commandLineProp.stringPropertyNames()){
			if(key.startsWith("ella.x."))
				outProps.setProperty(key, commandLineProp.getProperty(key));
		}

		File ellaInternalSettingsFile = new File(outDir, "ella.settings");
		try{
			outProps.store(new FileOutputStream(ellaInternalSettingsFile), "ella settings");
			//System.out.println("settings file "+ellaInternalSettingsFile.getPath());
		}catch(IOException e){
			throw new Error("Error writing the ella settings to "+ellaInternalSettingsFile.getPath(), e);
		}

		String maxHeapSize = getProperty("ella.instrumentor.max.heapsize");
		
		String commandLine = "java -Xmx"+maxHeapSize+" -ea -classpath "+ellaDir+File.separator+"bin"+File.separator+"ella.instrument.jar "+"-Della.settings="+ellaInternalSettingsFile.getPath()+" com.apposcopy.ella.Main";
		
		commandLine += " "+command;

		//System.out.println(commandLine);
		try{
			//for(String cm : commandLine.split(" ")) System.out.println("\""+cm+"\"");
			ProcessBuilder pb = new ProcessBuilder(commandLine.split(" "));
			File log = new File(outDir, "log.txt");
			System.out.println("Standard output and error is redirected to "+log.getPath());
			log.delete();
			pb.redirectErrorStream(true);
			pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
			Process p = pb.start();
			p.waitFor();
			int exitCode = p.exitValue();
			if(exitCode != 0)
				throw new Error("Error occurred while instrumenting. exit code is "+exitCode);
		}catch(Exception e){
			throw new Error("Error occurred while instrumenting.", e);
		}
	}

	String findAndroidSDKPath() 
	{
		String[] sCmdExts = {""}; //TODO: support windows?
		StringTokenizer st = new StringTokenizer(System.getenv("PATH"), File.pathSeparator);
		String path, sFile;
		while (st.hasMoreTokens()) {
			path = st.nextToken();
			for (String sExt : sCmdExts) {
				sFile = path + File.separator + "android" + sExt;
				File file = new File(sFile);
				if (file.isFile()) {
					return file.getAbsoluteFile().getParentFile().getParent();
				}
			}
		}
		throw new RuntimeException("The executable 'android' is not in the path");
	}

	String findAndroidBuildToolsPath(String sdkPath)
	{
		File buildToolsDir = new File(sdkPath, "build-tools");
		//find the latest version
		String latestVersion = null;
		for(String v : buildToolsDir.list()){
			if(latestVersion == null || v.compareTo(latestVersion) > 0){
				latestVersion = v;
			}
		}
		assert latestVersion != null : "Build tools not found";
		return buildToolsDir.getPath()+File.separator+latestVersion;
	}

    String appPathToAppId(String appPath) 
	{
    	String appId = appPath.replace(File.separatorChar, '_');
    	if(appId.length() > 100) {
    		return sha256(appId);
    	} else {
    		return appId;
    	}
    }

	String sha256(String base) {
		try{
		    MessageDigest digest = MessageDigest.getInstance("SHA-256");
		    byte[] hash = digest.digest(base.getBytes("UTF-8"));
		    StringBuffer hexString = new StringBuffer();

		    for (int i = 0; i < hash.length; i++) {
		        String hex = Integer.toHexString(0xff & hash[i]);
		        if(hex.length() == 1) hexString.append('0');
		        hexString.append(hex);
		    }

		    return hexString.toString();
		} catch(Exception ex){
		   throw new RuntimeException(ex);
		}
    }

}