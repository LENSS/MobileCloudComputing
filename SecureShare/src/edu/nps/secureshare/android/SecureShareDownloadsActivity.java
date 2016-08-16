package edu.nps.secureshare.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SecureShareDownloadsActivity extends Activity {
	private static final String DEBUG_TAG = "ViewDownloadsActivity";
	private DatabaseHelper helper = null;
	
	List<FileDetails> filesList = new ArrayList<FileDetails>();
	FileListAdapter adapter = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.downloads);
		helper = new DatabaseHelper(this);
		
		Cursor c = helper.listDownloadedFiles();
		if (c.moveToFirst()) {
			do {
				filesList.add(new FileDetails(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.DOWNLOAD_FILE_NAME)), 
						c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.DOWNLOAD_FILE_TIME_STAMP))));	
			} while (c.moveToNext());
		}
		
		c.close();
		
			
		ListView list = (ListView)findViewById(R.id.ListView_Downloads);
		
		adapter = new FileListAdapter();
		list.setAdapter(adapter);
		list.setOnItemClickListener(optionSelectedListener);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		helper.close();
	}

	private OnItemClickListener optionSelectedListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			File file = new File(getFilesDir() + "/" +filesList.get(position).getFilename());

			String extension = getFileExtension(file.getName());
			String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			
			Log.d(DEBUG_TAG, "Files dir is: " + getFilesDir().getPath());
			Intent i = new Intent();
			i.setAction(Intent.ACTION_VIEW);
			if (mimeType != null) {
				i.setDataAndType(Uri.fromFile(file), mimeType);
				startActivity(i);
			} else {
				Toast.makeText(SecureShareDownloadsActivity.this, 
						extension + " is an unknown extension", Toast.LENGTH_LONG).show();
			}	
			finish();
		}
	};
	
	private String getFileExtension(String filename) {
	    String ext="";
	    int mid= filename.lastIndexOf(".");
	    ext=filename.substring(mid+1,filename.length());  
	    return ext.toLowerCase();
	}
	
	class FileListAdapter extends ArrayAdapter<FileDetails> {
		FileListAdapter() {
			super(SecureShareDownloadsActivity.this, android.R.layout.simple_list_item_1, filesList);
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			FileViewHolder holder = null;
			
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.downloaded_item, null);
				holder = new FileViewHolder(row);
				row.setTag(holder);
			} else {
				holder = (FileViewHolder)row.getTag();
			}
			
			holder.populateFrom(filesList.get(position));
			
			return row;
		}
	}
	
	class FileDetails {
		private String filename;
		private long timestamp;
		
		public FileDetails(String filename, long timestamp) {
			this.filename = filename;
			this.timestamp = timestamp;
		}
		
		public String getFilename() {
			return filename;
		}
		
		public long getTimestamp() {
			return timestamp;
		}
	}
	
	static class FileViewHolder {
		private TextView filename = null;
		private TextView timestamp = null;
		private View row = null;
		
		FileViewHolder(View row) {
			this.row = row;
			filename = (TextView)this.row.findViewById(R.id.download_filename);
			timestamp = (TextView)this.row.findViewById(R.id.download_timestamp);
		}
		
		void populateFrom(FileDetails d) {
			filename.setText(d.getFilename());
			timestamp.setText("Timestamp: " + String.format("%1$TD %1$TT", d.getTimestamp()));	
		}
	}
}
