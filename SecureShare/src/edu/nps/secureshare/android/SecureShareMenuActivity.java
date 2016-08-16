package edu.nps.secureshare.android;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SecureShareMenuActivity extends Activity {
	// result codes
	public static final int READ_FILE_OPTION = 1000;
	public static final int WRITE_FILE_OPTION = 1001;
	public static final int SETTINGS_OPTION = 1002;
	public static final int HELP_OPTION = 1003;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu);
		
		
		ListView menuList = (ListView)findViewById(R.id.ListView_Menu);
		
		
		String[] menuItems = {
				getResources().getString(R.string.read_menu_item),
				getResources().getString(R.string.write_menu_item),
				getResources().getString(R.string.download_menu_item),
				getResources().getString(R.string.settings_menu_item),
				getResources().getString(R.string.help_menu_item)
		};
		
		ArrayAdapter<String> menuAdapter = new ArrayAdapter<String>(this, 
				R.layout.menu_item, menuItems);
		
		Intent serviceIntent = new Intent("edu.nps.secureshare.android.services.NetworkServerService.SERVICE");
    	getApplicationContext().startService(serviceIntent);
		
		menuList.setAdapter(menuAdapter);
		menuList.setOnItemClickListener(menuItemListener);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		startActivity(item.getIntent());
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.app_options, menu);
		menu.findItem(R.id.help_menu_item).setIntent(
				new Intent(this, SecureShareHelpActivity.class));
		menu.findItem(R.id.settings_menu_item).setIntent(
				new Intent(this, SecureShareSettingsActivity.class));
		return true;
	}
	

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String server_ip = prefs.getString("ds_server", "not set");
		if (server_ip.equals("not set")) {
			Toast.makeText(getApplicationContext(), "The directory service server must be set", Toast.LENGTH_LONG).show();
			Intent i = new Intent(SecureShareMenuActivity.this,
					SecureShareSettingsActivity.class);
			startActivity(i);
		}
	}

	private OnItemClickListener menuItemListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View itemClicked,
				int position, long id) {
			TextView textView = (TextView) itemClicked;
			String text = textView.getText().toString();
			if (text.equalsIgnoreCase(getResources().getString(R.string.read_menu_item))) {
				// Launch the read activity
				Intent i = new Intent(SecureShareMenuActivity.this, 
						SecureShareReadActivity.class);
				startActivity(i);
			} else if (text.equalsIgnoreCase(getResources().getString(R.string.write_menu_item))) {
				// Launch the write activity
				Intent i = new Intent(SecureShareMenuActivity.this,
						SecureShareWriteActivity.class);
				startActivity(i);
			} else if (text.equalsIgnoreCase(getResources().getString(R.string.download_menu_item))) {
				Intent i = new Intent(SecureShareMenuActivity.this, 
						SecureShareDownloadsActivity.class);
				startActivity(i);
			} else if (text.equalsIgnoreCase(getResources().getString(R.string.settings_menu_item))) {
				// Launch the settings activity
				Intent i = new Intent(SecureShareMenuActivity.this,
						SecureShareSettingsActivity.class);
				startActivity(i);
			} else if (text.equalsIgnoreCase(getResources().getString(R.string.help_menu_item))) {
				// Launch the help activity
				Intent i = new Intent(SecureShareMenuActivity.this,
						SecureShareHelpActivity.class);
				startActivity(i);
			}
		}
	};

}
