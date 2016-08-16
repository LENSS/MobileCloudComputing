package edu.nps.secureshare.android;

import java.io.IOException;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import edu.nps.secureshare.android.services.NetworkServerService;
import edu.nps.secureshare.network.SecureShareNetworkClient;
import edu.nps.secureshare.network.DsRegisterNeighborMessage;

public class SecureShareSettingsActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		setContentView(R.layout.settings);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(prefsChangedListener);
		
		Button defaultsButton = (Button)findViewById(R.id.set_defaults_button);
		defaultsButton.setOnClickListener(defaultsListener);
		
		TextView ip_address = (TextView)findViewById(R.id.Menu_IPAddress);
		ip_address.setText(NetworkServerService.getLocalIpAddress());
		
		TextView server_ip_address = (TextView)findViewById(R.id.Menu_Server_IPAddress);
		server_ip_address.setText(prefs.getString("ds_server", "not set"));
	}
	
	private OnSharedPreferenceChangeListener prefsChangedListener = 
		new SharedPreferences.OnSharedPreferenceChangeListener() {
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			if (key.equals("ds_server")) {
				// Get the ds server address
				String destination_ip = sharedPreferences.getString("ds_server", null);
				// create message with local IP address
				DsRegisterNeighborMessage neighborMessage = 
					new DsRegisterNeighborMessage(NetworkServerService.getLocalIpAddress());
				// send updated address to server
				SecureShareNetworkClient client = new SecureShareNetworkClient(destination_ip, 
						NetworkServerService.SERVER_PORT);
				try {
					client.sendMessage(neighborMessage);
					//client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Toast.makeText(getApplicationContext(), "Set directory service server", 
						Toast.LENGTH_SHORT).show();
			} else {
				int n = Integer.parseInt(sharedPreferences.getString("n_value", "-1"));
				int k = Integer.parseInt(sharedPreferences.getString("k_value", "-1"));
				
				if (n < k) {
					Toast.makeText(getApplicationContext(), "Warning: n cannot be less than k.  Setting n = " + (2*k-1) + ", k = " + k, Toast.LENGTH_LONG).show();
					SharedPreferences.Editor editor = sharedPreferences.edit();
					editor.putString("n_value", (2*k-1)+"");
					editor.commit();
				} else {
					Toast.makeText(getApplicationContext(), 
							"Set preferences. n = " + n + ", k = " + k, 
							Toast.LENGTH_LONG).show();
				}
				
			}
		}
	};
	
	private OnClickListener defaultsListener = new View.OnClickListener() {
		public void onClick(View v) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("n_value", "7");
			editor.putString("k_value", "4");
			editor.commit();
			Toast.makeText(getApplicationContext(), "Reset preferences. n = 7, k = 4", Toast.LENGTH_LONG).show();
		}
	};
	
}
