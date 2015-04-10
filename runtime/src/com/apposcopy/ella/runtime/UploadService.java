package com.apposcopy.ella.runtime;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

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


public class UploadService extends Service
{
	private static final int UPLOAD_TIME_PERIOD = 500;
	private UploadThread uploadThread;

	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent.hasExtra("end")){
			uploadThread.stop = true;
			return START_NOT_STICKY;
		}

		String url = intent.getStringExtra("url");
		String beginTime = intent.getStringExtra("beginTime");
		if(uploadThread == null){
			uploadThread = new UploadThread(url, beginTime);
			uploadThread.start();
			Log.d("ella", "starting the upload thread");
		} else {
			Log.d("ella", "upload thread is already running");
		}
		return START_REDELIVER_INTENT;
	}

	public IBinder onBind(Intent intent) {
        return null;
    }

	private class UploadThread extends Thread
	{
		private String url;
		private String beginTime;
		private boolean stop = false;
		private CoverageRecorder covRecorder = Ella.covRecorder;

		UploadThread(String url, String beginTime)
		{
			this.url = url;
			this.beginTime = beginTime;
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
			UploadService.this.stopSelf();
		}
		
		public void uploadCoverage() throws IOException
		{
			//Log.d("ella", "About to upload coverage data to "+url);
			
			/*
			  File file = new File(fileName);
			  BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			  writer.write(coverage.toString());
			  writer.close();
			*/
			

			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(url);
			
			List<NameValuePair> nameValuePairs = new ArrayList();
			nameValuePairs.add(new BasicNameValuePair("id", Ella.id));
			String payload = covRecorder.data();
			nameValuePairs.add(new BasicNameValuePair("cov", payload));
			nameValuePairs.add(new BasicNameValuePair("beginTime", beginTime));
			
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = client.execute(post);
			System.out.println("ELLA: Coverage uploaded. id: "+Ella.id+ "data: "+payload);
		}
	}

	
}