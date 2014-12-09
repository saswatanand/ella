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
	private static final String COVERAGE_FILE = "/ella_coverage";
	private static final String LOG_DIR_PREFIX = "/data/data/";
	private static final String PKG_FILE = "/sdcard/ella_pkg.txt";
	private static final String RECORD_COVERAGE_FILE = "/sdcard/ella_record_coverage";
	private static final String DUMP_FINISHED_FILE = "/ella_dump_finished";

	final static String url = "http://kuhu1.stanford.edu:8080/ella/uploadcoverage";

	public static final BitSet coverage = new BitSet();

	static {
		new ShutDownHook().start();
	}

	public static void m(int mId)
	{
		//System.out.println("Covered "+mId);
		coverage.set(mId);
	}

	private static void dumpCoverage(String pkg) throws IOException
	{
		System.out.println("ELLA: About to upload coverage data");

		/*
		File file = new File(fileName);
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(coverage.toString());
		writer.close();
		*/

		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(url);

		List<NameValuePair> nameValuePairs = new ArrayList();
		nameValuePairs.add(new BasicNameValuePair("pkg", pkg));
		nameValuePairs.add(new BasicNameValuePair("cov", coverage.toString()));

		post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		HttpResponse response = client.execute(post);
		System.out.println("ELLA: Coverage uploaded.");
	}


	private static class ShutDownHook extends Thread
	{
		ShutDownHook()
		{
		}
		
		public void run()
		{

			//keep recording coverage as long as this file exists on the sd card
			File file = new File(RECORD_COVERAGE_FILE);
			while(file.exists()){
				try{
					sleep(500);
				}catch(InterruptedException e){
					break;
				}
			}

			String pkg = null;
			try{
				//dump coverage
				pkg = new BufferedReader(new FileReader(PKG_FILE)).readLine();
				dumpCoverage(pkg);

				//create an empty file to indicate to the world that
				//coverage has been dumped
				FileWriter writer = new FileWriter(new File(LOG_DIR_PREFIX+pkg+DUMP_FINISHED_FILE));
				writer.write(0);
				writer.close();
			}catch(IOException e){
				throw new Error(e);
			}
			System.out.println("ELLA: Created file "+LOG_DIR_PREFIX+pkg+DUMP_FINISHED_FILE);
		}
	}
	
}