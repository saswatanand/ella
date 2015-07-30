package com.apposcopy.ella.runtime;

import android.util.Log;
import android.os.Debug;
import android.os.SystemClock;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.Socket;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.json.JSONObject;
import org.json.JSONException;

public class Ella
{
	//instrumentation will set values of the following four fields
	private static String id; 
	private static String recorderClassName;
	private static String uploadUrl;
	private static boolean useAndroidDebug;

	private static final int UPLOAD_TIME_PERIOD = 500;
	private static Recorder recorder;
	private static UploadThread uploadThread;

	private static final int TRACERECORD_TIME_PERIOD = 5000;
	private static TraceRecordingThread traceRecordingThread;

	static {
		startRecording();
	}

	public static void m(int mId)
	{
		recorder.m(mId);
	}

	public static void v(int metadata, Object obj)
	{
		recorder.v(obj, metadata);
	}

	static void startRecording()
	{
		try{
			recorder = (Recorder) Class.forName(recorderClassName).newInstance();	
			uploadThread = new UploadThread();
			uploadThread.start();
		} catch(ClassNotFoundException e){
			throw new Error(e);
		} catch(InstantiationException e){
			throw new Error(e);
		} catch(IllegalAccessException e){
			throw new Error(e);
		} 
		
		if(useAndroidDebug){
			traceRecordingThread = new TraceRecordingThread();
			traceRecordingThread.start();
		}
	}

	static void stopRecording()
	{
		uploadThread.stop = true;
		if(traceRecordingThread != null)
			traceRecordingThread.stop = true;
	}

	private static class TraceRecordingThread extends Thread
	{
		private boolean stop = false;

		TraceRecordingThread()
		{
			super();
		}
		
		public void run()
		{
			int count = 1;
			String traceDirName = "/sdcard/debug.traces";
			File traceDir = new File(traceDirName);
			if(!traceDir.exists()){
				traceDir.mkdir();
			}
			while(!stop){
				try{
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
					String traceFileName = traceDirName+"/"+dateFormat.format(new Date());
					Debug.startMethodTracing(traceFileName);
					sleep(TRACERECORD_TIME_PERIOD);					
					Debug.stopMethodTracing();
				}catch(InterruptedException e){
					break;
				}
			}
		}
	}

	private static class UploadThread extends Thread
	{
		private boolean stop = false;
		private boolean first = true;
		private OutputStream socketOut;

		UploadThread()
		{
			super();
		}
		
		public void run()
		{
			String[] tokens = uploadUrl.split(":");
			String serverAddress = tokens[0];
			int port = Integer.parseInt(tokens[1]);
			try{
				Socket socket = new Socket(serverAddress, port);
				socket.setKeepAlive(true);
				socketOut = socket.getOutputStream();
			}catch(IOException e){
				throw new Error(e);
			}

			while(!stop){
				try{
					sleep(UPLOAD_TIME_PERIOD);					
					try{
						uploadCoverage();
					}catch(IOException e){
						throw new Error(e);
					}
				}catch(InterruptedException e){
					break;
				}
			}
			
			try{
				socketOut.close();
			}catch(IOException e){
				throw new Error(e);
			}
		}
		
		public void uploadCoverage() throws IOException
		{
			String payload = recorder.data();
			if(payload.length() == 0 && stop == false){
				Log.d("ella", "no data to upload");
				return;
			}
			JSONObject json = new JSONObject();
			try {
				json.put("id",id);
				json.put("cov", payload);
				json.put("stop", String.valueOf(stop));
				if(first){
					json.put("recorder", recorderClassName);
					first = false;
				}
			} catch(JSONException e) {
				throw new Error(e);
			}
			String jsonString = json.toString();
			byte[] content = jsonString.getBytes(java.nio.charset.Charset.forName("UTF-8"));
			Log.d("ella", "Uploading coverage. id: "+id+ " data size: "+content.length);

			try{
				socketOut.write(content);
				socketOut.write('\r');socketOut.write('\n');socketOut.write('\r');socketOut.write('\n');
				socketOut.flush();
			} catch(IOException e){
				throw new Error(e);
			}			
		}
	}
}
