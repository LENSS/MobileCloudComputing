package edu.tamu.lenss.mdfs.activities;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import adhoc.etc.IOUtilities;
import adhoc.etc.Logger;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.MDFSFileCreator;
import edu.tamu.lenss.mdfs.MDFSFileCreator.MDFSFileCreatorListener;
import edu.tamu.lenss.mdfs.R;
import edu.tamu.lenss.mdfs.SingleMediaScanner;
import edu.tamu.lenss.mdfs.SingleMediaScanner.SingleFileSannerListener;
import edu.tamu.lenss.mdfs.comm.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.utils.AndroidUIUtilities;

public class HomeScreen extends Activity {
	private static final String TAG = HomeScreen.class.getSimpleName();
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int SELECT_IMAGE_REQUEST_CODE = 300;
	private Uri imageUri;
	
	private Button takePicBtn, selectPicBtn, dirListBtn, confirmBtn, cancelBtn, galleryBtn;
	private TextView statusTV;
	private EditText confirmET;
	private LinearLayout confirmLayout;
	private ImageView imageView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_screen);
		ServiceHelper.getInstance();
		initUI();
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ServiceHelper.releaseService();
	}
	
	private void initUI(){
		confirmET = (EditText)findViewById(R.id.confirmET);
		statusTV = (TextView)findViewById(R.id.statusTV);
		takePicBtn = (Button)findViewById(R.id.takePictureBtn);
		selectPicBtn = (Button)findViewById(R.id.selectPictureBtn);
		dirListBtn = (Button)findViewById(R.id.directoryBtn);
		confirmBtn = (Button)findViewById(R.id.confirmBtn);
		cancelBtn = (Button)findViewById(R.id.cancelBtn);
		galleryBtn = (Button)findViewById(R.id.galleryBtn);
		confirmLayout = (LinearLayout)findViewById(R.id.confirmlayout);
		imageView = (ImageView)findViewById(R.id.imageView);
		getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);		// Keep Screen On
		
		takePicBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				takePicture();
			}
		});
		selectPicBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				selectPicture();
			}
		});
		dirListBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(getBaseContext(), DirectoryList.class));
			}
		});
		galleryBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				openGallery();
			}
		});
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
	        if (resultCode == RESULT_OK) {
	            
	            showConfirmBar(imgFile);
	        } else if (resultCode == RESULT_CANCELED) {
	            // User canceled the image capture
	        } else {
	            // Image capture failed, advise user
	        }
	    }

	    if (requestCode == SELECT_IMAGE_REQUEST_CODE) {
	        if (resultCode == RESULT_OK) {
	        	Uri selectedImage = data.getData();
	            String[] filePathColumn = {MediaStore.Images.Media.DATA};

	            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
	            cursor.moveToFirst();

	            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	            String filePath = cursor.getString(columnIndex);
	            cursor.close();
	            Logger.v(TAG, "File path: " + filePath);
	            showConfirmBar(new File(filePath));
	        } else if (resultCode == RESULT_CANCELED) {
	            // User cancelled the video capture
	        } else {
	            // Video capture failed, advise user
	        }
	    }
	}
	
	private File imgFile, selectedFile;
	private void takePicture(){
		  // create Intent to take a picture and return control to the calling application
	    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    
	    imgFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
	    imageUri = Uri.fromFile(imgFile); // create a file to save the image
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); // set the image file name
	    //intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // set the video image quality to high
	    
	    // start the image capture Intent
	    startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}
	
	private void selectPicture(){
		Intent i = new Intent(Intent.ACTION_PICK,
	               android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, SELECT_IMAGE_REQUEST_CODE);
	}
	
	private void openGallery(){
		//sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
		File fPath = AndroidIOUtils.getExternalFile(Constants.DIR_DECRYPTED);
		File[] files = fPath.listFiles();
		if(!fPath.exists() || files == null || files.length < 1){
			AndroidUIUtilities.showToast(getBaseContext(), "No Files Currently");
			return;
		}
		
		new SingleMediaScanner(this, files[0], new SingleFileSannerListener(){
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
	
	
	private void sendFile(File file){
		MDFSFileCreator creator = new MDFSFileCreator(file, Constants.KEY_CODING_RATIO, Constants.FILE_CODING_RATIO, fileCreatorListener);
		creator.setDeleteFileWhenComplete(true);
		creator.start();
	}
	
	private static final int HANDLE_STATUS_MSG = 0;
	private static final int HANDLE_STATUS_BAR_HIDE = 1;
	private static final int HANDLE_STATUS_BAR_SHOW = 2;
	private Handler uiHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case HANDLE_STATUS_MSG:
				String str = (String)msg.obj;
				statusTV.setText(str + " ...");
				break;
			case HANDLE_STATUS_BAR_HIDE:
				confirmLayout.setVisibility(View.GONE);
				imageView.setImageResource(R.drawable.ic_launcher);
				break;
			case HANDLE_STATUS_BAR_SHOW:
				
				break;
			}
		}
	};
	
	private void updateStatusBar(String msg){
		Message m = new Message();
		m.what = HANDLE_STATUS_MSG;
		m.obj = msg;
		uiHandler.sendMessage(m);
	}
	
	private MDFSFileCreatorListener fileCreatorListener = new MDFSFileCreatorListener(){
		@Override
		public void statusUpdate(String status) {
			updateStatusBar(status);
		}

		@Override
		public void onError(String error) {
			updateStatusBar(error);
		}

		@Override
		public void onComplete() {
			updateStatusBar("File Creation Complete");
			Message m = new Message();
			m.what = HANDLE_STATUS_BAR_HIDE;
			uiHandler.sendMessage(m);
		}
	};
	
	private void showConfirmBar(final File file){
		Logger.v(TAG, "File Path: " + file.getAbsolutePath());
		if(!file.exists()){
			confirmLayout.setVisibility(View.GONE);
			imageView.setImageResource(android.R.id.icon);
			return;
		}
		confirmLayout.setVisibility(View.VISIBLE);
		updateStatusBar("Please name your picture and click Send when done");
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = 4;   // for 1/3 the image to be loaded. Used for imageView
		final Bitmap compressed = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
		imageView.setImageBitmap(compressed);
		
		confirmET.setText(file.getName());
		confirmET.setSelectAllOnFocus(true);
		confirmET.requestFocus();
		
		confirmBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				
				final StringBuilder str = new StringBuilder(confirmET.getText().toString());
				if(!str.toString().toLowerCase().contains(".jpg"))
					str.append(".jpg");
				
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(confirmET.getWindowToken(), 0);
				
				new Thread(new Runnable(){
					@Override
					public void run() {
						compressAndsendFile(file, compressed, str.toString());
					}
				}).start();
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				statusTV.setText("Compressing File ...");
			}
		});
		cancelBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				confirmLayout.setVisibility(View.GONE);
				imageView.setImageResource(android.R.id.icon);
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(confirmET.getWindowToken(), 0);
			}
		});
		confirmET.setOnEditorActionListener(new OnEditorActionListener() {
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_SEND) {
		            confirmBtn.performClick();
		            return true;
		        }
		        return false;
		    }
		});
	}
	
	private void compressAndsendFile(final File orgiginalFile, final Bitmap compressed, final String newFileName){
		final File renamedFile = new File(orgiginalFile.getParent()+ "/" + newFileName);
		orgiginalFile.renameTo(renamedFile);
		File tmpF = AndroidIOUtils.getExternalFile(Constants.DIR_CACHE);
		final File compressedFile = IOUtilities.createNewFile(tmpF, newFileName);
		
		// Compress the image file. Blocking call
		try {
	        FileOutputStream out = new FileOutputStream(compressedFile);
	        //BitmapFactory.decodeFile(renamedFile.getAbsolutePath()).compress(Bitmap.CompressFormat.JPEG, 90, out);
	        compressed.compress(Bitmap.CompressFormat.JPEG, 95, out);
		} catch (Exception e) {
		       e.printStackTrace();
		}
		
		this.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				sendFile(compressedFile);
				// delete the file
				orgiginalFile.delete();
				renamedFile.delete();
			}
		});
	}
	
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;

	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type){
	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "MyCameraApp");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.e(TAG, "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp); //  + ".jpg"
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp); // + ".mp4"
	    } else {
	        return null;
	    }
	    return mediaFile;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		menu.add(0, 0, 0, "Debugging Pages");
		menu.add(0, 1, 0, "Job Processing");		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
		case 0:
			startActivity(new Intent(this, AODVDisplay.class));
			break;
		case 1:
			startActivity(new Intent(this, JobProcessing.class));
			break;
		}
		return true;
	}	
}
