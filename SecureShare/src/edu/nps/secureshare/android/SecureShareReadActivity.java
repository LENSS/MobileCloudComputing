package edu.nps.secureshare.android;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import edu.nps.secureshare.android.services.NetworkServerService;
import edu.nps.secureshare.directoryservice.DataRecord;
import edu.nps.secureshare.directoryservice.DirectoryService;
import edu.nps.secureshare.mdfs.FragmentContainer;
import edu.nps.secureshare.mdfs.MDFS;
import edu.nps.secureshare.network.SecureShareFragmentRequestMessage;
import edu.nps.secureshare.network.SecureShareMessage;
import edu.nps.secureshare.network.SecureShareNetworkClient;
import edu.nps.secureshare.network.SecureShareTransmitFragmentMessage;

public class SecureShareReadActivity extends Activity {
	private static final String DEBUG_TAG = "ReadActivity";
	
	List<DataRecord> records = null;
	DataRecordAdapter adapter = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.read);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String dsServerIpAddress = prefs.getString("ds_server", null);
		DirectoryService ds = new DirectoryService(dsServerIpAddress);
		
		try {
			records = ds.list();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		ListView list = (ListView)findViewById(R.id.ListView_Read);
		adapter = new DataRecordAdapter();
		list.setAdapter(adapter);
		list.setOnItemClickListener(optionSelectedListener);
	}


	private OnItemClickListener optionSelectedListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			DataRecord d = records.get(position);
			String filename = d.filename();
			int n = d.n();
			int k = d.k();
			long timestamp = d.timestamp();
			
			Log.i(DEBUG_TAG, "Filename: " + filename + ", n: " + n + ", k: " + k + ", timestamp: " + timestamp);

			SecureShareFileReader reader = new SecureShareFileReader(filename, timestamp, n, k);
			Thread thread = new Thread(reader);
			thread.start();
			finish();
		}
	};
	
	class DataRecordAdapter extends ArrayAdapter<DataRecord> {
		DataRecordAdapter() {
			super(SecureShareReadActivity.this, android.R.layout.simple_list_item_1, records);
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			DataRecordHolder holder = null;
			
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.read_item, null);
				holder = new DataRecordHolder(row);
				row.setTag(holder);
			} else {
				holder = (DataRecordHolder)row.getTag();
			}
			
			holder.populateFrom(records.get(position));
			
			return row;
		}
	}
	
	static class DataRecordHolder {
		private TextView filename = null;
		private TextView timestamp = null;
		private TextView source = null;
		private View row = null;
		
		DataRecordHolder(View row) {
			this.row = row;
			
			filename = (TextView)this.row.findViewById(R.id.filename);
			timestamp = (TextView)this.row.findViewById(R.id.timestamp);
			source = (TextView)this.row.findViewById(R.id.source);
		}
		
		void populateFrom(DataRecord d) {
			filename.setText(d.filename());
			timestamp.setText("Timestamp: " + String.format("%1$TD %1$TT", d.timestamp()));
			source.setText("Source: " + d.source());	
		}
	}
	
	private class SecureShareFileReader implements Runnable{
		
		// Member Variables
		private String mFilename;
		private long mTimestamp;
		private int mN;
		private int mK;
		
		// Constructor
		public SecureShareFileReader(String filename, long timestamp, int n, int k) {
			this.mFilename = filename;
			this.mTimestamp = timestamp;
			this.mN = n;
			this.mK = k;
		}
		
		@Override
		public void run() {
			Vector<FragmentContainer> fragments = getFragments();
			
			MDFS mdfs = new MDFS();
			Log.d(DEBUG_TAG, "MDFS instantiated");
			
			Log.d(DEBUG_TAG, "K is: " + mK + " and we are passing " + fragments.size() + " fragments to MDFS");			
			
			
			Log.d(DEBUG_TAG, "About to call getFile with " + fragments.size() + " fragments");
			byte[] plainByteFile = mdfs.getFile(fragments);
			
			Log.d(DEBUG_TAG, "returned data is " + plainByteFile.length + " bits long");
			
			
			Log.i(DEBUG_TAG, "Successfully made it far enough to read " + mFilename);
			
			
			// TODO destroy notification that file is downloaded
			// TODO save file to temp directory
			Log.i(DEBUG_TAG, "The downloads are stored in " + getFilesDir().getPath());
			
			FileOutputStream fos = null;
			
			try {
				fos = openFileOutput(mFilename, MODE_WORLD_READABLE);
				fos.write(plainByteFile);
				fos.flush();
				fos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}	
			
			// TODO update downloaded files database
			
			//ds.registerDownloadedFile(mFilename, mTimestamp);
			DatabaseHelper Dbhelper = new DatabaseHelper(getApplicationContext());
			Dbhelper.registerDownloadedFile(mFilename, mTimestamp);
			Dbhelper.close();
			// TODO set notification that filename has been downloaded
			
		}
		
		private Vector<FragmentContainer> getFragments() {
			
			FragmentContainer fragment = null; // temp holding container
			Vector<FragmentContainer> result = new Vector<FragmentContainer>();
			SharedPreferences prefs = 
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String dsServerIpAddress = prefs.getString("ds_server", null);
			DirectoryService ds = new DirectoryService(dsServerIpAddress);
			
			String fragHash = null;
			String address = null;
			
			int index = 0;
			int totalFrags = 0;
			
			while (totalFrags < mK) {
				try {
					fragHash = FragmentContainer.generateFragmentHash(
							mFilename, mTimestamp, index);
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
				Log.d(DEBUG_TAG, "FragHash for fragment " + index + " is: " + fragHash);
				try {
					address = ds.whoHasFragment(fragHash);
				} catch (IOException e) {
					Log.e(DEBUG_TAG, "Can't lookup fragHash");
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					Log.e(DEBUG_TAG, "Can't lookup fragHash");
					e.printStackTrace();
				}
			
				if (address != null) {
					fragment = getFragment(fragHash, address);
				} else {
					fragment = null; // the hash was not stored
				}
				
				
				if (fragment != null) {
					result.add(fragment);
					Log.i(DEBUG_TAG, "Added a fragment to results");
					totalFrags++;
				}
				index++;
				if (index == mN) {
					//throw Exception();
				}
			}
			return result;
		}
		
		private FragmentContainer getFragment(String fragHash, String address) {
			// Container for return value
			SecureShareTransmitFragmentMessage msg = null;
			
			// Create Fragment Request Message containing fragHash
			SecureShareFragmentRequestMessage request = new 
				SecureShareFragmentRequestMessage(fragHash);
			
			// Client contacts foreign node at destination on 
			// NetworkServerService.SERVER_PORT
			int port = NetworkServerService.SERVER_PORT;
			
			try {
				SecureShareNetworkClient client = new SecureShareNetworkClient(address, port);
				client.sendMessage(request);
				
				// Expect reply containing a fragmentContainer
				Object o = client.receiveMessage();
				
				if (!(o instanceof SecureShareMessage))
					throw new IllegalArgumentException("Wanted SecureShareMessage, got " + o);
				
				// Made it this far, so cast
				msg = (SecureShareTransmitFragmentMessage)o;
			} catch (Exception e) {
				Log.e(DEBUG_TAG, "Could not create client connection to host " + 
						address + " on port " + port);
			}
			return msg.getFragment();
		}
	}
}
