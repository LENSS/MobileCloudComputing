package edu.tamu.lenss.mdfs;

import java.io.File;
import java.util.List;

import adhoc.etc.Logger;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.FaceDetector;
import android.os.CountDownTimer;
import android.util.Log;
import edu.tamu.lenss.mdfs.MDFSFileRetriever.FileRetrieverListener;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.models.TaskResult;

public class DataAnalyzer {
	private final Context context;
	private static final String TAG = DataAnalyzer.class.getSimpleName();
	private final DataAnalyzerListener listener;
	private List<MDFSFileInfo> fileInfoList;
	public DataAnalyzer(Context c, DataAnalyzerListener lis){
		this.context = c;
		this.listener = lis;
	}
	
	
	/**
	 * Provide a list of files to be processed
	 * @param fileInfoList
	 */
	private int count = 0;
	private CountDownTimer timer; 
	public void downloadFile(final List<MDFSFileInfo> fileList){
		fileInfoList = fileList;
		// The timer click is not accurate when CPU is fully utilized. Need to allocate enough time
		if(timer != null){
			timer.cancel();
			timer = null;
		}
		timer = new CountDownTimer(10*fileInfoList.size()*10000+2000, 10000) {
			public void onTick(long millisUntilFinished) {
				if(count < fileInfoList.size()){
					retrieveOneFile();
				}
				else{
					this.cancel();
				}
			}

			public void onFinish() {
				Logger.v(TAG, "Timer finished");
			}
		}.start();
	}
	
	private void retrieveOneFile(){
		if(count < fileInfoList.size()){
			Logger.v(TAG, "Count=" + count + " Size=" + fileInfoList.size());
			MDFSFileInfo fInfo = fileInfoList.get(count);
			MDFSFileRetriever retriever = new MDFSFileRetriever(fInfo.getFileName(), fInfo.getCreatedTime());
			retriever.setListener(fileRetrieverListener);
			retriever.start();
			listener.statusUpdate("Downloading " + fInfo.getFileName());
			Logger.v(TAG, "Downloading " + fInfo.getFileName());
			count++;
		}
		else{
			timer.cancel();
		}
	}
	
	public void cancelTimer(){
		timer.cancel();
	}
	
	/**
	 * Blocking call
	 * @param a File handle 
	 * @return
	 */
	public int countFace(File f, MDFSFileInfo fileInfo){
		listener.statusUpdate("Analyzing " + f.getName());
		int NUMBER_OF_FACES = 10;
    	long startT, endT;
    	startT = System.currentTimeMillis();
    	BitmapFactory.Options bitmapFatoryOptions=new BitmapFactory.Options();
    	bitmapFatoryOptions.inPreferredConfig=Bitmap.Config.RGB_565;
    	Bitmap myBitmap = BitmapFactory.decodeFile(f.getPath());
    	if(myBitmap == null){
    		listener.onError("Null Image");
    		return -1;
    	}
    	
    	FaceDetector.Face[] detectedFaces = new FaceDetector.Face[NUMBER_OF_FACES];
    	FaceDetector faceDetector=new FaceDetector(myBitmap.getWidth(),myBitmap.getHeight(),NUMBER_OF_FACES);
    	int detectedCnt = faceDetector.findFaces(myBitmap, detectedFaces);
    	endT = System.currentTimeMillis();
    	
    	listener.statusUpdate(detectedCnt + " faces are detected in " + f.getName());
    	listener.onDetectedFace(new TaskResult(f.getName(), fileInfo.getCreatedTime(), detectedCnt));
    	Log.i(TAG, detectedCnt + " faces are detected");
    	Log.i(TAG, "Take " + (endT - startT) + " milli seconds");
    	return detectedCnt;
	}
	
	
	private FileRetrieverListener fileRetrieverListener = new FileRetrieverListener(){
		@Override
		public void onError(String error) {
			listener.onError(error);
		}

		@Override
		public void statusUpdate(String status) {
			listener.statusUpdate(status);
		}

		@Override
		public void onComplete(File decryptedFile, MDFSFileInfo fileInfo) {
			countFace(decryptedFile, fileInfo);
			retrieveOneFile();
		}
	};
	
	public interface DataAnalyzerListener {
		public void onError(String error);
		public void statusUpdate(String status);
		public void onDetectedFace(TaskResult result);
	}
}
