package edu.nps.secureshare.android;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.nps.secureshare.android.services.NetworkServerService;
import edu.nps.secureshare.directoryservice.DataRecord;
import edu.nps.secureshare.directoryservice.DirectoryService;
import edu.nps.secureshare.mdfs.FragmentContainer;
import edu.nps.secureshare.mdfs.MDFS;
import edu.nps.secureshare.network.SecureShareNetworkClient;
import edu.nps.secureshare.network.SecureShareStoreFragmentMessage;

public class SecureShareWriteActivity extends Activity {
	
	private static final String DEBUG_TAG = "WriteActivity";
	Thread fileWriteThread = null;
	
	// Return codes
	private static final int SELECT_FILE = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.write);
		
		ListView optionsList = (ListView)findViewById(R.id.ListView_Write);
		String[] options = {
				getResources().getString(R.string.select_image_menu_item)
		};
			
		ArrayAdapter<String> optionsAdapter = new ArrayAdapter<String>(this, 
			R.layout.menu_item, options);
		
		optionsList.setAdapter(optionsAdapter);
		optionsList.setOnItemClickListener(optionSelectedListener);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		TextView fragSettings = (TextView)findViewById(R.id.write_settings);
		String settings = "Current value of preferences: n = " + prefs.getString("n_value", "not set") + ", k = " + prefs.getString("k_value", "not set");
		fragSettings.setText(settings);
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

	private OnItemClickListener optionSelectedListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View itemClicked,
				int position, long id) {
			TextView textView = (TextView) itemClicked;
			String text = textView.getText().toString();
			if (text.equalsIgnoreCase(getResources().getString(R.string.select_image_menu_item))) {
				// Launch the read activity
				startActivityForResult(new Intent(SecureShareWriteActivity.this, 
						SecureShareSelectImageActivity.class), SELECT_FILE);
			}
		}
	};
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Start a service.  The service will start a thread to send the file 
		// so that it doesn't block the UI.
		switch (requestCode) {
		case SELECT_FILE:
			if (resultCode == RESULT_OK) {
				Toast.makeText(this, "File selected for send", Toast.LENGTH_SHORT).show();
				String filePath = data.getStringExtra("filePath");
				Log.i(DEBUG_TAG, "The file path is " + filePath);
				
				SecureShareFileWriter writer = new SecureShareFileWriter(filePath);
				Thread thread = new Thread(writer);
				thread.start();
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "File selection canceled", Toast.LENGTH_SHORT).show();
			}
		}
		
		// Return to the main menu activity
		//startActivity(new Intent(this, SecureShareMenuActivity.class));
		finish();
	}
	
	private class SecureShareFileWriter implements Runnable {
		String filePath;
		
		public SecureShareFileWriter(String filePath) {
			Log.i(DEBUG_TAG, "File writer instantiated");
			this.filePath = filePath;
		}

		@Override
		public void run() {
			Log.i(DEBUG_TAG, "Attempting to open file " + filePath);
			
			File file = new File(filePath);
			InputStream is = null;
			try {
				is = new FileInputStream(file);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			
			byte[] byteFile = null;
			try {
				byteFile = readBytes(is);
			} catch (IOException e) {
				e.printStackTrace();
			}
			String filename = file.getName();
			Long timestamp = file.lastModified();
			Log.i(DEBUG_TAG, filename + " with timestamp: " + timestamp + " is " + byteFile.length + " bytes long");
			
			// This is where my stuff begins its magic
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			MDFS mdfs = new MDFS();
			int k = Integer.parseInt(prefs.getString("k_value", "4"));
			int n = Integer.parseInt(prefs.getString("n_value", "7"));
			
			
			DataRecord record = new DataRecord(filename, timestamp, n, k, NetworkServerService.getLocalIpAddress());
			
			// call the encoder to get a Vector containing file fragments
			Vector<FragmentContainer> holder = mdfs.getFragments(filename, timestamp,
					byteFile, k, n);
			Log.i(DEBUG_TAG, "The encoder returned " + holder.size() + " fragments.");
			
			// Send the files (keywords is null for now)
			write(holder, record);
		}	
		
		
		private void write(Vector<FragmentContainer> fragments, DataRecord record) {
			int index = 0;
			int fragmentsRemaining = fragments.size();
			int n = record.n();
			
			SharedPreferences prefs = 
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String dsServerIpAddress = prefs.getString("ds_server", null);
			DirectoryService ds = new DirectoryService(dsServerIpAddress);
			String neighbors[] = null;
			try {
				neighbors = ds.getNeighbors(n);
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}
			int[] success = new int[n];
			
			// TODO set notification that file is writing
			Log.i(DEBUG_TAG, "Begin writing");
			
			// initialize success array with 1s (1 indicates not successfully sent yet)
			for (int i=0; i<n; i++) {
				success[i] = 1;
			}
			
			while (fragmentsRemaining > 0) {
				if (success[index] == 1) { // fragment has not been successfully sent yet
					if (send(fragments.get(index), neighbors[index])) {
						success[index] = 0;
						String fragHash = null;
						try {
							fragHash = fragments.get(index).getFragmentHash();
						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
						}
						try {
							ds.registerFragment(fragHash, neighbors[index]);
						} catch (IOException e) {
							Log.e(DEBUG_TAG, "Error registering fragment");
							e.printStackTrace();
						}
						fragmentsRemaining--;
					}
					index = (index +1) % n; // loop from 0 to n-1
				}
			}
			try {
				ds.registerFile(record);
			} catch (IOException e) {
				Log.e(DEBUG_TAG, "Error registering file");
			}
			// TODO destroy notification
			// TODO set notification that file is finished writing
		}
		
		private boolean send(FragmentContainer fragment, String destination) {
			Log.i(DEBUG_TAG, "DEBUG: Send called. Sending " + fragment.getFilename() + " fragment to " + destination);
			
			// Client contacts foreign node at destination on NetworkServerService.SERVER_PORT
			// Create a STORE_FRAGMENT_REQUEST message with fragment
			// Once message is transmitted, wait for ACK message
			// Once ACK received, return true
			// otherwise, return false
			
			// Create a STORE_FRAGMENT_REQUEST message with fragment
			SecureShareStoreFragmentMessage storeMsg = new SecureShareStoreFragmentMessage(fragment);
			int port = NetworkServerService.SERVER_PORT;
			

				// Client contacts foreign node at destination on NetworkServerService.SERVER_PORT
			Log.i(DEBUG_TAG, "Trying to contact " + destination + ":" + port);
			try {
				SecureShareNetworkClient client = new SecureShareNetworkClient(destination, port);
				Log.i(DEBUG_TAG, "Connected to " + destination + ":" + port);
				client.sendMessage(storeMsg);
				// TODO Handle ACK message here...  Right now it just goes to the ether
				// client.close();
			} catch (Exception e) {
				Log.e(DEBUG_TAG, "Could not create client connection to host " + 
						destination + " on port " + port);
				return false;
			}
			return true;
		}
	}

	
	public byte[] readBytes(InputStream inputStream) throws IOException {
	  // this dynamically extends to take the bytes you read
	  ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

	  // this is storage overwritten on each iteration with bytes
	  int bufferSize = 1024;
	  byte[] buffer = new byte[bufferSize];

	  // we need to know how may bytes were read to write them to the byteBuffer
	  int len = 0;
	  while ((len = inputStream.read(buffer)) != -1) {
	    byteBuffer.write(buffer, 0, len);
	  }

	  // and then we can return your byte array.
	  return byteBuffer.toByteArray();
	}
}
