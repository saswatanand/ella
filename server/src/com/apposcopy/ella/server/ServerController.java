package com.apposcopy.ella.server;

import java.util.*;
import java.io.*;
import java.net.*;

/*
 * @author Saswat Anand
 */
public class ServerController
{
	public static void main(String[] args) throws IOException 
	{
		ServerController controller = new ServerController();
		String command = controller.setup(args);
		if(command.equals("start")){
			controller.start();
		} else if(command.equals("kill")){
			controller.kill();
		}
	}
	
	void kill()
	{
		try{
			//send a message to shutdown
			String port = getProperty("ella.server.port");
			Socket socket = null;
			try{
				socket = new Socket(InetAddress.getLoopbackAddress(), Integer.parseInt(port));
			}catch(ConnectException e){
				System.out.println("Server is not running.");
				return;
			}
			OutputStream socketOut = socket.getOutputStream();
			socketOut.write('\r');socketOut.write('\n');socketOut.write('\r');socketOut.write('\n');
			socketOut.flush();
			socketOut.close();
		}catch(IOException e){
			throw new Error(e);
		}
	}

	void start()
	{
		String maxHeapSize = getProperty("ella.server.max.heapsize");
		String ellaOutDir = getPathProperty("ella.outdir");
		String port = getProperty("ella.server.port");

		String commandLine = "java -Xmx"+maxHeapSize+" -ea -classpath "+ellaDir+File.separator+"bin"+File.separator+"ella.server.jar com.apposcopy.ella.server.Server "+ellaOutDir+" "+port;
		
		//System.out.println(commandLine);
		try{
			//for(String cm : commandLine.split(" ")) System.out.println("\""+cm+"\"");
			ProcessBuilder pb = new ProcessBuilder(commandLine.split(" "));
			File log = new File(ellaOutDir, "serverlog.txt");
			System.out.println("Standard output and error is redirected to "+log.getPath());
			//log.delete();
			pb.redirectErrorStream(true);
			pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
			pb.start();
		}catch(Exception e){
			throw new Error("Error occurred while instrumenting.", e);
		}
	}

	private Properties settingsProp;
	private Properties commandLineProp;
	private String ellaSettingsDir;
	private String userDir;
	private String ellaDir;

	String setup(String[] args)
	{
		String command = null;
		commandLineProp = new Properties();
		
		int i = 0;
		while(i < args.length){
			String key = args[i];
			if(key.startsWith("-")){
				String value = args[i+1];
				commandLineProp.put(key.substring(1), value);
				i++;
			} else {
				command = key;
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
		
		return command;
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
			if(file.isFile() && file.getName().equals("ella.server.jar")){
				try{
					return file.getAbsoluteFile().getParentFile().getParentFile().getCanonicalPath(); 
				}catch(IOException e){
					throw new Error("Error occurred while finding canonical path.", e);
				}
			}
		}
		throw new RuntimeException("Could not determine Ella directory");
	}
}