package com.apposcopy.ella.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.google.gson.Gson;

import java.util.*;
import java.util.logging.*;
import java.io.*;

/*
 * @author Saswat Anand
 */
public class Server
{
	private static final Logger logger = Logger.getLogger(Server.class.getName());
	private static final int BUFSIZE = 10240;

	private Map<String,String> appIdToTraceId = new HashMap();
	private String ellaOutDir;

    private class CoverageUpdate {
    	private String id;
    	public String getAppId() { return id; }
    	private String cov;
    	public String getData() { return cov; }
    	private String stop;
    	public boolean requestsStop() { return stop.equals("true"); }
    	private String recorder;
    	public String getRecorderName() { return recorder; }
    }

	public void collect(String data) throws IOException
	{
		BufferedWriter out = null;
		try {
			//System.out.println("request: "+sb.toString());
			CoverageUpdate covUpdate = (CoverageUpdate) new Gson().fromJson(data, CoverageUpdate.class);
			
			String appId = covUpdate.getAppId();
			String path = ellaOutDir + File.separator + appId;
			File dir = new File(path);
			String traceId = appIdToTraceId.get(appId);
			if(traceId == null){
				dir.mkdir();

				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
				traceId = dateFormat.format(new Date());
				appIdToTraceId.put(appId, traceId);
			}
			
			File datFile = new File(dir, "coverage.dat."+traceId);
			boolean append = datFile.exists();
			out = new BufferedWriter(new FileWriter(datFile, append));
			if(!append){
				StringBuilder builder = new StringBuilder();
				builder.append("recorder:").append(covUpdate.getRecorderName()).append("\n");
				builder.append("version:").append("1").append("\n");
				builder.append("###").append("\n");
				String metaData = builder.toString();
				out.write(metaData, 0, metaData.length());
			}
			out.write(covUpdate.getData());
			
			if(covUpdate.requestsStop()){
				appIdToTraceId.remove(appId);
			}
			//logger.log(Level.INFO, "Upload succeeded");
		} catch (FileNotFoundException fne) {
			throw new Error(fne);
		} catch(Exception e){
			throw new Error(e);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	public static void main(String[] args) throws IOException 
	{
		Server server = new Server();
		server.setup(args);
		server.start();
	}

	public void start() throws IOException
	{
		ServerSocket serverSocket = new ServerSocket(Integer.parseInt(getProperty("ella.server.port")));
		Socket socket = serverSocket.accept();
		PushbackInputStream inputStream = new PushbackInputStream(new BufferedInputStream(socket.getInputStream()));
		int read;
		byte[] buf = new byte[BUFSIZE];
		int offset = 0;
		do{
			read = inputStream.read(buf, offset, buf.length - offset);
			if(read > 0){
				int splitByte = findMessageBreak(buf, offset, read);
				if(splitByte >= 0){
					String msg = new String(buf, 0, splitByte, java.nio.charset.Charset.forName("UTF-8"));
					collect(msg);
					int startOfNextMessage = splitByte+4;
					int totalRead = offset + read;
					if(startOfNextMessage < totalRead) 
						inputStream.unread(buf, startOfNextMessage, totalRead - startOfNextMessage);
					offset = 0;
				} else {
					offset += read;
					if(offset > buf.length * 0.8){
						byte[] newbuff = new byte[buf.length*2];
						System.arraycopy(buf, 0, newbuff, 0, offset);
						buf = newbuff;
					}
				}
			}
		}while(read >= 0);
		inputStream.close();
		socket.close();
	}
	
	int findMessageBreak(byte[] buf, int offset, int num)
	{
		int splitbyte = Math.max(0, offset - 3);
		int end = offset + num;
		while (splitbyte + 3 < end) {
			if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
				return splitbyte;
			}
			splitbyte++;
		}
		return -1;
	}

	private String ellaDir;
	private Properties settingsProp;
	private Properties commandLineProp;
	private String ellaSettingsDir;
	private String userDir;
	private String command;

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
		
		ellaOutDir = getPathProperty("ella.outdir");
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