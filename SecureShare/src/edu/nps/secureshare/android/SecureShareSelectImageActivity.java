/* Much of the code for this class is adapter from the book 
 * "Android Wireless Application Development, Second Edition" and
 * http://mobiforge.com/designing/story/understanding-user-interface-android-part-3-more-views
 */

package edu.nps.secureshare.android;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class SecureShareSelectImageActivity extends Activity {
	private static final String DEBUG_TAG="IMAGE_SELECT";
	
	// result code
	private static final int ACTIVITY_SELECT_IMAGE = 0;
	
	private ImageView imageView;
	private Button select, cancel;
	String filePath;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_image);
	
		imageView = (ImageView)findViewById(R.id.picture_view);
		select = (Button)findViewById(R.id.select_picture_button);
		cancel = (Button)findViewById(R.id.cancel_select_button);
		
		select.setOnClickListener(mSelectButtonListener);
		cancel.setOnClickListener(mCancelButtonListener);
		
		Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, ACTIVITY_SELECT_IMAGE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ACTIVITY_SELECT_IMAGE:
			if (resultCode == RESULT_OK) {
				Uri selectedImage = data.getData();
				Log.i(DEBUG_TAG, "The uri is: " + selectedImage.toString());
				String[] projection = {MediaStore.Images.Media.DATA};
				
				Cursor cursor = managedQuery(selectedImage, projection, null, null, null);
				cursor.moveToFirst();
				
				int columnIndex = cursor.getColumnIndex(projection[0]);
				filePath = cursor.getString(columnIndex);
				Log.i(DEBUG_TAG, "The filepath is: " + filePath);
				       
				imageView.setImageBitmap(BitmapFactory.decodeFile(filePath));
			}
		}
	}
	
	private OnClickListener mSelectButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent i = getIntent();
			i.putExtra("filePath", filePath);
			setResult(RESULT_OK, i);
			finish();
		}
		
	};
	
	private OnClickListener mCancelButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			setResult(RESULT_CANCELED, getIntent());
			finish();
		}
		
	};
	
}
