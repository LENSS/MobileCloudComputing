package edu.tamu.lenss.mdfs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class FailureEstimator extends BroadcastReceiver {
	private Context context;
	private int scale = -1;
    private int level = -1;
    private double batteryPercentage;
    
	public FailureEstimator(Context cont) {
		this.context = cont;
	}
	
	public void start(){
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		context.registerReceiver(this, filter);
	}
	
	public void stop(){
		context.unregisterReceiver(this);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		 level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
         scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
         //voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
         batteryPercentage = (double)level/scale;
         //Log.v("BatteryManager", "level is "+level+"/"+scale+", temp is "+temp+", voltage is "+voltage);
	}
	
	public float getFailureProbability(){
		float probability;
		probability = (float)(1-batteryPercentage);
		if(probability <= 0 || probability >= 1){
			if(Math.abs(probability-0) < Math.abs(probability-1))
				probability = 0.01f;
			else
				probability = 0.99f;
		}
		return probability;
	}

}
