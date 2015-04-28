package com.apposcopy.ella.runtime;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastReceiver extends android.content.BroadcastReceiver
{
	public void onReceive(Context context, Intent intent) 
	{
		Log.d("ella", "Broadcast received.");
		Log.d("ella", "To stop uploading coverage data.");
		Ella.stopRecording();
	}
}
