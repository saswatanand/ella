package com.apposcopy.ella.runtime;

import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.json.JSONObject;
import org.json.JSONException;

public class Ella
{
	//instrumentation will set values of the following three fields
	private static String id; 
	private static String recorderClassName;
	private static String uploadUrl;

	private static final int UPLOAD_TIME_PERIOD = 500;
	private static Recorder recorder;
	private static UploadThread uploadThread;

	static {
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
	}

	public static void m(int mId)
	{
		recorder.m(mId);
	}

	public static void v(int metadata, Object obj)
	{
		recorder.v(obj, metadata);
	}

	static void stopRecording()
	{
		uploadThread.stop = true;
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
					// If continuous coverage reporting is supported, 
					// send an update to the server
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
			if(payload.length() == 0){
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
