package com.apposcopy.ella.runtime;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.NameValuePair;

import android.util.Log;

import java.util.*;
import java.io.*;

public class Ella
{
	//instrumentation will set values of the following three fields
	private static String id; 
	private static String covRecorderClassName;
	private static String uploadUrl;

	private static final int UPLOAD_TIME_PERIOD = 500;
	private static CoverageRecorder covRecorder;
	private static UploadThread uploadThread;

	static {
		try{
			covRecorder = (CoverageRecorder) Class.forName(covRecorderClassName).newInstance();			
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
		//System.out.println("Covered "+mId);
		covRecorder.m(mId);
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
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(uploadUrl);
			
			List<NameValuePair> nameValuePairs = new ArrayList();
			nameValuePairs.add(new BasicNameValuePair("id", id));
			String payload = covRecorder.data();
			nameValuePairs.add(new BasicNameValuePair("cov", payload));
			nameValuePairs.add(new BasicNameValuePair("stop", String.valueOf(stop)));
			if(first){
				nameValuePairs.add(new BasicNameValuePair("recorder", covRecorderClassName));
				first = false;
			}

			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = client.execute(post);
			Log.d("ella", "Coverage uploaded. id: "+id+ " data size: "+payload.length());
		}
	}
}
