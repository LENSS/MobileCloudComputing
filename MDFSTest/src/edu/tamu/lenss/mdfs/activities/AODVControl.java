package edu.tamu.lenss.mdfs.activities;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import adhoc.aodv.Constants;
import adhoc.aodv.Node;
import adhoc.aodv.RouteTableManager;
import adhoc.aodv.routes.ForwardRouteEntry;
import adhoc.etc.IOUtilities;
import adhoc.etc.Logger;
import adhoc.etc.MyTextUtils;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import edu.tamu.lenss.mdfs.R;
import edu.tamu.lenss.mdfs.comm.ServiceHelper;
import edu.tamu.lenss.mdfs.comm.TopologyHandler.TopologyListener;
import edu.tamu.lenss.mdfs.models.NodeInfo;
import edu.tamu.lenss.mdfs.placement.PlacementHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.utils.AndroidUIUtilities;

public class AODVControl extends Activity{
	private EditText rreqDests, testFilesDests;
	private Button rreqBtn, discoveryBtn, sendFilesBtn, pictureBtn, videoBtn, selectBtn, viewBtn, deleteAllBtn;
	private Node myNode;
	private ProgressBar progressBar;
	private BroadcastReceiver batteryReceiver;
	private double batteryPercentage;
	private IntentFilter filter;
	private static final String TAG = AODVControl.class.getSimpleName();
	private ServiceHelper serviceHelper;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.aodv_control);
		myNode = Node.getInstance();
		serviceHelper = ServiceHelper.getInstance();
		initUI();
		
		// Battery Monitor
		batteryReceiver = new BroadcastReceiver() {
	        int scale = -1;
	        int level = -1;
	        //int voltage = -1;
	        @Override
	        public void onReceive(Context context, Intent intent) {
	            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	            //voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
	            batteryPercentage = (double)level/scale;
	            //Log.e("BatteryManager", "level is "+level+"/"+scale+", temp is "+temp+", voltage is "+voltage);
	        }
	    };
	    filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(batteryReceiver);
	}
	
	private void initUI(){
		rreqDests = (EditText)findViewById(R.id.destIpTV);
		testFilesDests = (EditText)findViewById(R.id.testFileDestIP);
		rreqBtn = (Button)findViewById(R.id.rreqBtn);
		discoveryBtn = (Button)findViewById(R.id.routeDiscBtn);
		sendFilesBtn = (Button)findViewById(R.id.SendFilesBtn);
		pictureBtn = (Button)findViewById(R.id.pictureBtn);
		videoBtn = (Button)findViewById(R.id.videoBtn);
		selectBtn = (Button)findViewById(R.id.selectBtn);
		viewBtn = (Button)findViewById(R.id.viewBtn);
		deleteAllBtn = (Button)findViewById(R.id.deleteAllBtn);
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		
		rreqBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String str = rreqDests.getText().toString();
				String[] strArray = str.split(",");
				HashMap<Integer, Integer> dests = new HashMap<Integer, Integer>();
				for(int i=0; i < strArray.length; i++){
					if(MyTextUtils.isNumeric(strArray[i]))
						dests.put(Integer.parseInt(strArray[i]), Constants.UNKNOWN_SEQUENCE_NUMBER);
				}
				/*if(!dests.isEmpty())
					myNode.sendRREQ(dests);*/
			}
		});
		
		discoveryBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				serviceHelper.startTopologyDiscovery(new TopologyListener(){
					@Override
					public void onError(String msg) {
						Logger.e(TAG, msg);
					}
					@Override
					public void onComplete(List<NodeInfo> topList) {
						for(NodeInfo info:topList){
							Logger.v(TAG, "Receive NodeInfo from " + info.getSource());
						}
					}
				});
			}
		});
		
		sendFilesBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String str = testFilesDests.getText().toString();
				if(MyTextUtils.isNumeric(str)){
					//progressBar.setVisibility(View.VISIBLE);
					sendPacket(Integer.parseInt(str));
					//progressBar.setVisibility(View.INVISIBLE);
				}
			}
		});
		
		pictureBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				takePicture();
			}
		});
		
		videoBtn.setText("Simulate");
		videoBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				new Thread(new Runnable(){
					@Override
					public void run() {
						processingTimeTest();
					}
				}).start();
			}
		});
		
		selectBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				selectImages();
			}
		});
		
		viewBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse("file://" + "/sdcard/MDFS/decrypted/girl.jpg"), "image/*");
				startActivity(intent);
			}
		});
		
		deleteAllBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				deleteAllFiles();
			}
		});
	}
	
	private Uri imageUri;
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 0;
	private void takePicture(){
		 // create Intent to take a picture and return control to the calling application
	    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    imageUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); // set the image file name
	    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
	    
	    // start the image capture Intent
	    startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

	}
	
	private Uri videoUri;
	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 1;
	private void takeVideo(){
		//create new Intent
	    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
	    videoUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);  // create a file to save the video
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);  // set the image file name
	    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
	    intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
	    
	 // start the Video Capture Intent
	    startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);

	}
	
	private void processingTimeTest(){
		double kn_ratio = 0.6;
		int n1, n2, k1, k2, size;
		
		for(int s=5; s<50; s=s+2){
			size=s;
			n1 = (int) Math.ceil(size * kn_ratio);
			n2 = (int) Math.ceil(size * kn_ratio);
			k1 = (int) Math.round(n1 * kn_ratio);
			k2 = (int) Math.round(n2 * kn_ratio);
			Logger.v(TAG, "Simulating network size: " + size);
			PlacementHelper helper = new PlacementHelper(createGraph(size), n1, k1, n2, k2);
			helper.findOptimalLocations();
			String str = size + ",\t" + n1 + ",\t" + k1 + ",\t" + helper.getMCSimTime() + ",\t" + helper.getILPTime(); 
			Logger.i(TAG, str);
		}
	}	
	
	private HashSet<NodeInfo> createGraph(int size){
		Random rnd = new Random();
		boolean[][] distM = new boolean[size][size];
		for(int r=0; r<size; r++){
			for(int c=0; c<r+1; c++){
				if(r==c){
					distM[r][c]=false;
				}
				else{
					distM[r][c]=rnd.nextBoolean();
					distM[c][r]=distM[r][c];
				}
			}
		}
		HashSet<NodeInfo> set = new HashSet<NodeInfo>();				
		for(int i=0; i<size; i++){
			NodeInfo node = new NodeInfo();
			ArrayList<Integer> neighbors = new ArrayList<Integer>();
			for(int n=0; n<size; n++){
				if(distM[i][n])
					neighbors.add(n);				
			}
			node.setNeighborsList(neighbors);
			node.setFailureProbability((float) (0.5*rnd.nextDouble()));
			set.add(node);
		}		
		return set;
	}
	
	private void deleteAllFiles(){
		File rootDir = AndroidIOUtils.getExternalFile("MDFS");
		try {
			IOUtilities.deleteRecursively(rootDir);
		} catch (IOException e) {
			Logger.e(TAG, e.toString());
			e.printStackTrace();
		}
		ServiceHelper.getInstance().getDirectory().clearAll();
		rootDir = AndroidIOUtils.getExternalFile("AODVLog");
		try {
			IOUtilities.deleteRecursively(rootDir);
		} catch (IOException e) {
			Logger.e(TAG, e.toString());
			e.printStackTrace();
		}
		
	}
	
	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.
	
	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "MyCameraApp");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.
	
	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("MyCameraApp", "failed to create directory");
	            return null;
	        }
	    }
	
	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }
	
	    return mediaFile;
	}
	private static final int SELECT_PICTURE = 2;
	private void selectImages(){
		// in onCreate or any event where your want the user to
        // select a file
        /*Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,
                "Select Picture"), SELECT_PICTURE);*/
		
		Intent i = new Intent(Intent.ACTION_PICK,
	               android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, SELECT_PICTURE); 

	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
		    if (resultCode == RESULT_OK) {
		        //use imageUri here to access the image
		    	//File f = convertImageUriToFile(imageUri, this);
		    	//Logger.v(TAG, "Image File " + f.getName() + " is created");
		    } else if (resultCode == RESULT_CANCELED) {
		        Logger.v(TAG, "Picture was not taken");
		    } else {
		        Logger.v(TAG, "Picture was not taken");
		    }
		}
		else if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
			 if (resultCode == RESULT_OK) {
			        //use imageUri here to access the image
			    	Logger.v(TAG, "Video File " + " is created");
			    } else if (resultCode == RESULT_CANCELED) {
			        Logger.v(TAG, "Video was not taken");
			    } else {
			        Logger.v(TAG, "Video was not taken");
			    }
		}
	}
	
	public static File convertImageUriToFile (Uri imageUri, Activity activity)  {
		Cursor cursor = null;
		try {
		    String [] proj={MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID, MediaStore.Images.ImageColumns.ORIENTATION};
		    cursor = activity.managedQuery( imageUri,
		            proj, // Which columns to return
		            null,       // WHERE clause; which rows to return (all rows)
		            null,       // WHERE clause selection arguments (none)
		            null); // Order-by clause (ascending by name)
		    int file_ColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		    int orientation_ColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION);
		    if (cursor.moveToFirst()) {
		        String orientation =  cursor.getString(orientation_ColumnIndex);
		        return new File(cursor.getString(file_ColumnIndex));
		    }
		    return null;
		} finally {
		    if (cursor != null) {
		        cursor.close();
		    }
		}
	}

	
	private void sendPacket(int id){
		NodeInfo info = new NodeInfo();
		info.setSource(myNode.getNodeId());
		info.setRank((int)Math.round(Math.random()*3));
		info.setTimeSinceInaccessible((int)Math.round(Math.random()*1000));
		RouteTableManager manager = myNode.getRouteManager();
		for(ForwardRouteEntry entry : manager.getNeighbors()){
			info.addNeighbor(entry.getDestinationAddress());
		}
		info.setDest(id);
		myNode.sendAODVDataContainer(info);
	}
	
	private Handler uiHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			String str = (String)msg.obj;
			AndroidUIUtilities.showToast(getBaseContext(), str, true);
		}
	};
	
	private NodeInfo gatherNodeInfo(){
		NodeInfo info = new NodeInfo();
		info.setSource(myNode.getNodeId());
		info.setRank((int)Math.round(Math.random()*3));
		info.setTimeSinceInaccessible((int)Math.round(Math.random()*1000));
		info.setBatteryLevel((int)(batteryPercentage*100.0));
		
		RouteTableManager manager = myNode.getRouteManager();
		for(ForwardRouteEntry entry : manager.getNeighbors()){
			info.addNeighbor(entry.getDestinationAddress());
		}
		return info;
	}
	
}
