package com.apposcopy.ella.runtime;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastReceiver extends android.content.BroadcastReceiver
{
	public void onReceive(Context context, Intent intent) 
	{
		String action = intent.getStringExtra("action");
		if(action.equals("b")){
			String url = intent.getStringExtra("url");
			Ella.startRecording(url);
		} else if(action.equals("e")){
			Ella.stopRecording();
		}
	}
}
