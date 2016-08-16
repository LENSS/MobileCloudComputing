package edu.nps.secureshare.android.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// http://www.androidcompetencycenter.com/2009/06/start-service-at-boot/
public class SecureShareStartupIntentReceiver extends BroadcastReceiver {
	String DEBUG_TAG = "Broadcast Receiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent serviceIntent = new Intent("edu.nps.secureshare.android.services.NetworkServerService.SERVICE");
    	context.startService(serviceIntent);
	}

}
