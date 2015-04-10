package com.apposcopy.ella.runtime;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Date;
import java.text.DateFormat;

public class BroadcastReceiver extends android.content.BroadcastReceiver
{
	public void onReceive(Context context, Intent intent) 
	{
		Log.d("ella", "Broadcast received.");
		String action = intent.getStringExtra("action");
		if(action.equals("b")){
			String url = intent.getStringExtra("url");
			Log.d("ella", "To begin uploading coverage data. url = "+ url);

			//start upload service
			Intent i = new Intent(context, UploadService.class);
			i.putExtra("url", url);
			DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
			String date = dateFormat.format(new Date());
			date = date.replace('/','.').replace(' ','_').replace(':', '.');
			i.putExtra("beginTime", date);
			context.startService(i);

		} else if(action.equals("e")){
			Log.d("ella", "To stop uploading coverage data.");

			//end upload service
			Intent i = new Intent(context, UploadService.class);
			i.putExtra("end", "");
			context.startService(i);
		}
	}
}
