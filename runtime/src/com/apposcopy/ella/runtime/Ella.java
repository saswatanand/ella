package com.apposcopy.ella.runtime;

import android.util.Log;
import android.os.Debug;
import android.os.SystemClock;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
	private static long overhead = 0L;
	private static int vCount = 0;

	static {
		startRecording();
	}

	public static void m(int mId)
	{
		recorder.m(mId);
	}

	public static void v(int metadata, Object obj)
	{
		long start = SystemClock.uptimeMillis();
		recorder.v(obj, metadata);
		long elapsedTime = SystemClock.uptimeMillis() - start;
		overhead += elapsedTime;
		vCount++;
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
			File f = new File(traceDirName, "ella.txt");
			if(!traceDir.exists()){
				traceDir.mkdir();
				try{
					FileWriter fw = new FileWriter(f);
					fw.close();
				} catch(IOException e){
					Log.e("ella", "Error in creating ella.txt");
				}
			}
			while(!stop){
				try{
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
					String traceFileName = traceDirName+"/"+dateFormat.format(new Date());
					Debug.startMethodTracing(traceFileName);
					
					vCount = 0;
					overhead = 0L;
					
					sleep(TRACERECORD_TIME_PERIOD);					

					Debug.stopMethodTracing();
					
					try{
						FileWriter fw = new FileWriter(f, true);
						String info = overhead+" "+vCount+"\n";
						fw.write(info, 0, info.length());
						fw.close();
					} catch(IOException e){
						Log.e("ella", "Error in dumping ella overhead");
					}
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

		UploadThread()
		{
			super();
		}
		
		public void run()
		{
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
				assert false; // Should never happen
				return;
			}
			String jsonString = json.toString();
			Log.d("ella", "Uploading coverage. id: "+id+ " data size: "+payload.length());
			URL url = new URL(uploadUrl);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			try {
				urlConnection.setDoOutput(true);
				urlConnection.setChunkedStreamingMode(0);
				urlConnection.setRequestProperty("Content-Type", "application/json");
				urlConnection.setRequestProperty("Accept", "application/json");
				urlConnection.setRequestMethod("POST");

				OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
				out.write(jsonString.getBytes(java.nio.charset.Charset.forName("UTF-8")));
				out.flush();

				int responseCode = urlConnection.getResponseCode();
				if(responseCode == 200)
					Log.d("ella", "Coverage uploaded. id: "+id+ " data size: "+payload.length());
				else
					Log.d("ella", "Failed to upload coverage, server response code: " + responseCode);
			} finally {
				urlConnection.disconnect();
			}
		}
	}
}
