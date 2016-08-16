package edu.tamu.lenss.mdfs.activities;

import java.util.HashSet;

import adhoc.aodv.Node;
import adhoc.aodv.RouteTableManager;
import adhoc.etc.Logger;
import adhoc.etc.MyTextUtils;
//import adhoc.etc.MyTextUtils;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import edu.tamu.lenss.mdfs.R;

public class AODVSetting extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private SharedPreferences pref;
	public static final String PREF_BLOCKING_IPS = "pref_blocking_ips";
	public static final String PREF_KEY_STORAGE_RATIO = "pref_keystorage_ratio";
	public static final String PREF_FILE_STORAGE_RATIO = "pref_filestorage_ratio";
	public static final String PREF_KEY_CODING_RATIO = "pref_keycoding_ratio";
	public static final String PREF_FILE_CODING_RATIO = "pref_filecoding_ratio";
	
	
	private EditTextPreference blockingIps, keystorageET, fileStorageET, keyCodingET, fileCodingET;
	private static final String TAG = AODVSetting.class.getSimpleName();
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.aodv_setting);
		setTitle("AODV Settings");
		blockingIps = (EditTextPreference) getPreferenceScreen().findPreference(PREF_BLOCKING_IPS);
		keystorageET = (EditTextPreference) getPreferenceScreen().findPreference(PREF_KEY_STORAGE_RATIO);
		fileStorageET = (EditTextPreference) getPreferenceScreen().findPreference(PREF_FILE_STORAGE_RATIO);
		keyCodingET = (EditTextPreference) getPreferenceScreen().findPreference(PREF_KEY_CODING_RATIO);
		fileCodingET = (EditTextPreference) getPreferenceScreen().findPreference(PREF_FILE_CODING_RATIO);
		
		blockingIps.setSummary(blockingIps.getText());
		keystorageET.setSummary(keystorageET.getText());
		fileStorageET.setSummary(fileStorageET.getText());
		keyCodingET.setSummary(keyCodingET.getText());
		fileCodingET.setSummary(fileCodingET.getText());
		
		pref = PreferenceManager.getDefaultSharedPreferences(this);
	    pref.registerOnSharedPreferenceChangeListener(this);
	    
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		pref.unregisterOnSharedPreferenceChangeListener(this);
		
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.equals(PREF_BLOCKING_IPS)){
			String str = blockingIps.getText();
			Node node = Node.getInstance();
			RouteTableManager manager = node.getRouteManager();
			HashSet<Integer> blockingSet = manager.getBlockingIpSet();
			
			if(TextUtils.isEmpty(str)){
				blockingSet.clear();
				blockingIps.setSummary("");
				return;
			}
			blockingIps.setSummary(str);
			
			String[] ips = str.split(",");
			if(ips.length<1){
				ips = new String[1];
				ips[0]=str;
			}
			
			blockingSet.clear();
			for(int i=0; i < ips.length; i++){
				if(MyTextUtils.isNumeric(ips[i])){
					manager.removeForwardRouteEntry(Integer.parseInt(ips[i]));
					blockingSet.add(Integer.parseInt(ips[i]));
					Logger.v(TAG, ""+Integer.parseInt(ips[i]));
				}
			}
		}
		else if(key.equals(PREF_KEY_STORAGE_RATIO)){
			keystorageET.setSummary(keystorageET.getText());
		}
		else if(key.equals(PREF_FILE_STORAGE_RATIO)){
			fileStorageET.setSummary(fileStorageET.getText());
		}
		else if(key.equals(PREF_KEY_CODING_RATIO)){
			keyCodingET.setSummary(keyCodingET.getText());
		}
		else if(key.equals(PREF_FILE_CODING_RATIO)){
			fileCodingET.setSummary(fileCodingET.getText());
		}
		
	}
}
