package edu.tamu.lenss.mdfs.activities;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import adhoc.etc.Logger;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.MDFSDirectory;
import edu.tamu.lenss.mdfs.MDFSFileRetriever;
import edu.tamu.lenss.mdfs.MDFSFileRetriever.FileRetrieverListener;
import edu.tamu.lenss.mdfs.R;
import edu.tamu.lenss.mdfs.SingleMediaScanner;
import edu.tamu.lenss.mdfs.SingleMediaScanner.SingleFileSannerListener;
import edu.tamu.lenss.mdfs.comm.ServiceHelper;
import edu.tamu.lenss.mdfs.models.DeleteFile;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;

public class DirectoryList extends Activity {
	private static final String TAG = DirectoryList.class.getSimpleName();
	private ScheduledThreadPoolExecutor refreshExecutor = new ScheduledThreadPoolExecutor(1);
	private ListView directoryLV;
	private List<MDFSFileInfo> fileList;
	private DirListAdapter aa;
	private TextView statusTV;
	private static final int MSG_UPDATE_LIST = 0;
	private static final int MSG_UPDATE_STATUS = 1;
	
	private Handler uiHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			int type = msg.what;
			switch(type){
			case MSG_UPDATE_LIST:
				populateData();
				break;
			case MSG_UPDATE_STATUS:
				String str = (String)msg.obj;
				statusTV.setText(str + " ...");
				break;
			}
		}
	};
	private FileRetrieverListener fileRetrieverListener = new FileRetrieverListener(){
		@Override
		public void onError(String error) {
			updateStatusBar(error);
		}

		@Override
		public void statusUpdate(String status) {
			updateStatusBar(status);
		}

		@Override
		public void onComplete(File decryptedFile, MDFSFileInfo fileInfo) {
			updateStatusBar(decryptedFile.getName() + " is downloaded to " + decryptedFile.getPath() );
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.directory_list);
		directoryLV = (ListView) findViewById(R.id.directoryLV);
		statusTV = (TextView) findViewById(R.id.statusTV);;
		final ImageView horizontalLineFooter = new ImageView(this);
		horizontalLineFooter.setImageResource(R.drawable.line_horizontal);
		directoryLV.addFooterView(horizontalLineFooter);
		directoryLV.setSelector(R.drawable.listview_item_bg_selected);
		directoryLV.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				chooseAction(position);
			}
		});
		startRefrshingThread();
		//populateData();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		refreshExecutor.shutdown();
	}
	
	private void updateStatusBar(String msg){
		Message m = new Message();
		m.obj = msg;
		m.what=MSG_UPDATE_STATUS;
		uiHandler.sendMessage(m);
	}

	private void startRefrshingThread(){
		refreshExecutor.scheduleAtFixedRate(new Runnable(){
    		@Override
    		public void run() {
    			Message m = new Message();
    			m.what=MSG_UPDATE_LIST;
    			uiHandler.sendMessage(m);
    		}
    	}, 0, Constants.DIRECTORY_LIST_REFRESH_PERIOD, TimeUnit.MILLISECONDS);
	}
	
	private void populateData(){
		MDFSDirectory directory = ServiceHelper.getInstance().getDirectory();
		fileList = directory.getFileList();
		if(fileList.size() < 1){
			((TextView) (findViewById(R.id.emptyTV))).setVisibility(View.VISIBLE);
		}
		else{
			((TextView) (findViewById(R.id.emptyTV))).setVisibility(View.GONE);
		}
		if(aa == null){
			aa = new DirListAdapter(this, R.layout.list_row_fileinfo);
			directoryLV.setAdapter(aa);
			//Logger.v(TAG, "first time set adapter");
		}
		else{
			aa.notifyDataSetChanged();
			//Logger.v(TAG, "update adapter");
		}
	}
	
	private void chooseAction(int position){
		final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		if(fileList.size() < position)	// In case the directory was updated after the last time the list is updated
			return;
		final MDFSFileInfo fInfo = fileList.get(position);
		alertDialog.setCancelable(true);
		alertDialog.setTitle("Action");
		alertDialog.setMessage("You can download any file, but you may only delete the file you created");

		// If the decrypted exists, we can open it directory
		final String path = Constants.DIR_DECRYPTED + "/" + fInfo.getFileName();
		final File f = AndroidIOUtils.getExternalFile(path);
		if(f.exists()){
			alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "View", new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					/*Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.parse("file://" + "/sdcard/" + path), "image/*");
					startActivity(intent);
					alertDialog.dismiss();*/
					openGallery(f);
				}
			});
		}
		else{
			alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Download", new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					MDFSFileRetriever retriever = new MDFSFileRetriever(fInfo.getFileName(), fInfo.getCreatedTime());
					retriever.setListener(fileRetrieverListener);
					retriever.start();
				}
			});
		}
		
		alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Delete", new DialogInterface.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				DeleteFile deleteFile = new DeleteFile();
				deleteFile.setFile(fInfo.getFileName(), fInfo.getCreatedTime());
				ServiceHelper.getInstance().deleteFiles(deleteFile);
			}
		});
		
		if(!alertDialog.isShowing())
		   	alertDialog.show();
	}
	
	private void openGallery(final File file){
		new SingleMediaScanner(this, file, new SingleFileSannerListener(){
			@Override
			public void onScanCompleted(String path, Uri uri) {
				if (uri != null) {
			        Intent intent = new Intent(Intent.ACTION_VIEW);
			        intent.setData(uri);
			        startActivity(intent);
		        }
				else{
					Logger.v(TAG, "No Files in the Decrypted Directory");
				}
			}
		});
	}
	
	private class DirListAdapter extends ArrayAdapter<MDFSFileInfo>{
		private int resource;
		private Context adapterContext;
		private LayoutInflater vi;
		private ViewHolder holder;
		private SimpleDateFormat format =
	            new SimpleDateFormat("MM/dd/yy_HH:mm");
		
		public DirListAdapter(Context context, int resourceId) {
			super(context, resourceId, fileList);
			this.resource = resourceId;
			this.adapterContext = context;
			this.vi = (LayoutInflater) adapterContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// Inflate a new view if this is not an update.
			if (convertView == null) {
				convertView = vi.inflate(resource, null);
				holder = new ViewHolder();
				holder.nameTV = (TextView) convertView.findViewById(R.id.nameTV);
				holder.ownerTV = (TextView) convertView.findViewById(R.id.ownerTV);
				holder.timeTV = (TextView) convertView.findViewById(R.id.dateTV);
				holder.infoTV = (TextView) convertView.findViewById(R.id.infoTV);
				convertView.setTag(holder);
			}
			else{
				holder = (ViewHolder) convertView.getTag();
				holder.resetViews();
			}
			// Setup holder...
			//holder.nameTV.setText("Lane " + laneList.get(position).getLaneName());
			MDFSFileInfo fileInfo = fileList.get(position);
			String str = "by: Node ";
			str += fileInfo.getCreator() + "   ";
			str += fileInfo.getFileLength()/1024 + "k";
			holder.nameTV.setText(fileInfo.getFileName());
			holder.ownerTV.setText(str);
			holder.timeTV.setText(format.format(new Date(fileInfo.getCreatedTime())));
			if(ServiceHelper.getInstance().getDirectory().isDecryptedFileCached(fileInfo.getCreatedTime()))
				str = "Cached";
			else
				str = "No Cache";
			str += "   " + fileInfo.getK2() + " frags required";
			holder.infoTV.setText(str);
			
			return convertView;
		}
		
		@Override
		public int getCount() {
            return fileList.size();
        }
		
		private class ViewHolder {
	        TextView nameTV, ownerTV, timeTV, infoTV;
	        public void resetViews(){
	        	nameTV.setText("");
	        	ownerTV.setText("");
	        	timeTV.setText("");
	        	infoTV.setText("");
	        }
	    }
	}
}
