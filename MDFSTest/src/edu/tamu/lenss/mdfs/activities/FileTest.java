package edu.tamu.lenss.mdfs.activities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import adhoc.etc.IOUtilities;
import adhoc.etc.Logger;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.MDFSDirectory;
import edu.tamu.lenss.mdfs.MDFSFileCreator;
import edu.tamu.lenss.mdfs.MDFSFileCreator.MDFSFileCreatorListener;
import edu.tamu.lenss.mdfs.MDFSFileRetriever;
import edu.tamu.lenss.mdfs.R;
import edu.tamu.lenss.mdfs.comm.ServiceHelper;
import edu.tamu.lenss.mdfs.crypto.FragmentInfo;
import edu.tamu.lenss.mdfs.crypto.FragmentInfo.KeyShareInfo;
import edu.tamu.lenss.mdfs.crypto.MDFSEncoder;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;

public class FileTest extends Activity {
	private EditText fileET, encodeET;
	private TextView messageTV;
	private Button aesEncryptBT, aesDecryptBT, shSplitBT, bcDirectory, clearCacheBtn, encodeBtn;
	private static final String TAG = FileTest.class.getSimpleName();
	
	private Handler uiHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			String str = (String)msg.obj;
			messageTV.setText(str + " ...");
		}
	};
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
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.file_test);
	    initUI();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}
	
	private void updateStatusBar(String msg){
		Message m = new Message();
		m.obj = msg;
		uiHandler.sendMessage(m);
	}

	private void initUI(){
		messageTV = (TextView)findViewById(R.id.messageTV);
		fileET = (EditText)findViewById(R.id.fileET);
		aesEncryptBT = (Button)findViewById(R.id.aesEncryptBtn);
		aesDecryptBT = (Button)findViewById(R.id.aesDecryptBtn);
		shSplitBT = (Button)findViewById(R.id.splitSecretBtn);
		bcDirectory = (Button)findViewById(R.id.rsEncodeBtn);
		clearCacheBtn = (Button)findViewById(R.id.clearCacheBtn);
		encodeBtn = (Button)findViewById(R.id.encodeBtn);
		encodeET = (EditText)findViewById(R.id.encodeET);
		
		aesEncryptBT.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				encryptFile();
			}
		});
		
		aesDecryptBT.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				decryptFile();
			}
		});
		
		shSplitBT.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				deleteFile();
			}
		});
		
		bcDirectory.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				ServiceHelper.getInstance().broadcastMyDirectory();
			}
		});
		
		encodeBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				encodeFile();
			}
		});
		
		clearCacheBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				File rootDir = AndroidIOUtils.getExternalFile("MDFS");
				File[] files = rootDir.listFiles();
				for(File f : files){
					if(!f.isDirectory())
						continue;
					try {
						IOUtilities.deleteRecursively(f);
					} catch (IOException e) {
						Logger.e(TAG, e.toString());
						e.printStackTrace();
					}
				}
				
				ServiceHelper.getInstance().getDirectory().syncLocal();
			}
		});
		
	}
	byte[] encryptedByteFile;
	private byte[] rawSecretKey;
	ArrayList<FragmentInfo> fragInfos;
	ArrayList<KeyShareInfo> keyShares;
	
	private void encryptFile(){
		File myFile = AndroidIOUtils.getExternalFile("/Albums");
		File[] files = myFile.listFiles();
		myFile = files[0];
		MDFSFileCreator creator = new MDFSFileCreator(myFile, 0.8, 0.8, fileCreatorListener);
		creator.start();
		
	}
	
	private void decryptFile(){
		File myFile = AndroidIOUtils.getExternalFile("/Albums");
		File[] files = myFile.listFiles();
		myFile = files[0];
		MDFSDirectory directory = ServiceHelper.getInstance().getDirectory();
		MDFSFileInfo fInfo = directory.getFileInfo(myFile.lastModified());
		if(fInfo == null)
			return;
		MDFSFileRetriever retriever = new MDFSFileRetriever(fInfo.getFileName(), fInfo.getCreatedTime());
		retriever.start();
	}
	
	private void encodeFile(){
		int n_par = 8;
		int k_par = Integer.parseInt(encodeET.getText().toString());
		 // Read a file
		File fileFragDir = AndroidIOUtils.getExternalFile(Constants.DIR_ROOT + "/" + 
				"testFile.jpg");
		
		if(fileFragDir == null || !fileFragDir.exists()){
			Logger.e(TAG, "File does not exist");
			return;
		}
		
		MDFSEncoder encoder = new MDFSEncoder(fileFragDir, n_par, n_par, k_par, k_par);
		if (!encoder.encode()) {
			Logger.e(TAG, "File Encryption Failed");
			return;
		}
		//List<FragmentInfo> fragInfos = encoder.getFileFragments();
		
		int fragSize = encoder.getFileFragments().get(0).getFragment().length;
		String str = "Fragment Size = " + fragSize;
		str += "\nOverhead = " + (fragSize * n_par - encoder.getEncryptedBytes().length);
		
		messageTV.setText(str);
		Logger.i(TAG, str);
		
	}
	
	private void deleteFile(){
		
	}
}
