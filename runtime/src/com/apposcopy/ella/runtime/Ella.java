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
	private static final String RECORD_COVERAGE_FILE = "/sdcard/ella_record_coverage";
	private static final String URL_FILE = "/sdcard/ella_url.txt";
	private static String id; //instrumentation will set the value
	private static String covRecorderClassName;
	private static CoverageRecorder covRecorder;

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
		new EllaMonitor().start();
	}

	public static void m(int mId)
	{
		//System.out.println("Covered "+mId);
		covRecorder.m(mId);
	}
	
	private static void dumpCoverage(String url) throws IOException
	{
		dumpCoverage(url, false);
	}

	private static void dumpCoverage(String url, boolean append) throws IOException
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


	private static class EllaMonitor extends Thread
	{
		EllaMonitor()
		{
		}
		
		public void run()
		{
			String url = "";
			boolean append = false;
			try{
				url = new BufferedReader(new FileReader(URL_FILE)).readLine();
			}catch(IOException e){
				throw new Error(e);
			}
		
			//keep recording coverage as long as this file exists on the sd card
			File file = new File(RECORD_COVERAGE_FILE);
			while(file.exists()){
				try{
					sleep(500);
					// If continuous coverage reporting is supported, 
					// send an update to the server
					if(covRecorder.supportsContinuousReporting()) {
						try{
							//dump coverage
							dumpCoverage(url, append);
						}catch(IOException e){
							throw new Error(e);
						}
						if(!append) append = true; // Send requests other than the first with append=true.
					}
				}catch(InterruptedException e){
					break;
				}
			}

			try{
				//dump coverage
				dumpCoverage(url, append);
			}catch(IOException e){
				throw new Error(e);
			}
			System.out.println("ELLA: Shutdown hook done.");
		}
	}
	
}
