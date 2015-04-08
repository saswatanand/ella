package com.apposcopy.ella.runtime;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.NameValuePair;

import java.util.*;
import java.io.*;

public class Ella
{
	//instrumentation will set values of the following two fields
	private static String id; 
	private static String covRecorderClassName;
	
	private static CoverageRecorder covRecorder;
	private static EllaMonitor ellaMonitor;

	static {
		try{
			covRecorder = (CoverageRecorder) Class.forName(covRecorderClassName).newInstance();
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

	//called from the broadcast recvr
	public static void startRecording(String ellaServerUrl)
	{
		ellaMonitor = new EllaMonitor(ellaServerUrl);
		ellaMonitor.start();
		System.out.println("ELLA: Coverage recording started.");
	}

	//called from the broadcast recvr
	public static void stopRecording()
	{
		ellaMonitor.stop();
		
		try{
			//dump coverage
			ellaMonitor.dumpCoverage();
		}catch(IOException e){
			throw new Error(e);
		}
		System.out.println("ELLA: Coverage recording stopped.");
	}

	private static class EllaMonitor extends Thread
	{
		private boolean append = false;
		private String url;

		EllaMonitor(String url)
		{
			this.url = url;
		}
		
		public void run()
		{
			while(true){
				try{
					sleep(500);
					// If continuous coverage reporting is supported, 
					// send an update to the server
					if(covRecorder.supportsContinuousReporting()) {
						try{
							//dump coverage
							dumpCoverage();
						}catch(IOException e){
							throw new Error(e);
						}
						if(!append) append = true; // Send requests other than the first with append=true.
					}
				}catch(InterruptedException e){
					break;
				}
			}
		}
		
		public void dumpCoverage() throws IOException
		{
			System.out.println("ELLA: About to upload coverage data to "+url);
			
			/*
			  File file = new File(fileName);
			  BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			  writer.write(coverage.toString());
			  writer.close();
			*/
			
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(url);
			
			List<NameValuePair> nameValuePairs = new ArrayList();
			nameValuePairs.add(new BasicNameValuePair("id", id));
			nameValuePairs.add(new BasicNameValuePair("cov", covRecorder.data()));
			if(append) {
				nameValuePairs.add(new BasicNameValuePair("append", "true"));
			}
			
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = client.execute(post);
			System.out.println("ELLA: Coverage uploaded. id: "+id);
		}
	}
	
}
