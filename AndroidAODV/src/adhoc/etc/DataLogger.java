package adhoc.etc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import adhoc.etc.DataLogger.LogFileInfo.LogFileName;
import android.os.Environment;

public class DataLogger {
	private File parentDir;
	private static final String TAG = DataLogger.class.getSimpleName();
	
	public DataLogger(){
	}
	
	public boolean init(){
		boolean success = false;
		if(!IOUtilities.isExternalStorageAvailable()){
			Logger.e(TAG, "External Storage is not available");
			return false;
		}
		try{
			parentDir = new File(Environment.getExternalStorageDirectory(), 
					adhoc.aodv.Constants.DIR_LOG);
			if(!parentDir.exists())
				success = parentDir.mkdirs();
			else
				success = true;
		}
		catch(NullPointerException e){
			Logger.e(TAG, e.toString());
			success = false;
		}
		if(!success)
			Logger.e(TAG, "Fail to create directory");
		return success;
	}
	
	private Map<LogFileName, BufferedWriter> buffers = new HashMap<LogFileName, BufferedWriter>();
	public void appendSensorData(LogFileName t, String data){
		if(!buffers.containsKey(t)){
			File tmpFile = new File(parentDir, t.toString());
			if(tmpFile != null){
				try {
					FileWriter tmpWriter =  new FileWriter(tmpFile, true);
					buffers.put(t, new BufferedWriter(tmpWriter));
				} 
				catch (IOException e) {
					Logger.e(TAG, e.toString());
					e.printStackTrace();
					return;
				}
			}
		}
		try {	
			buffers.get(t).append(data);
			buffers.get(t).flush();
			//Logger.i(TAG, data);
		} catch (IOException e) {
			Logger.e(TAG, e.toString());
			e.printStackTrace();
		}
	}
	
	public void closeAllFiles(){
		Iterator<LogFileName>iter = buffers.keySet().iterator();
		while(iter.hasNext()){
			LogFileName t = iter.next();
			try {
				buffers.get(t).close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static boolean createRequiredFiles(){
		File tmp;
		File parentDir = new File(Environment.getExternalStorageDirectory(), 
				adhoc.aodv.Constants.DIR_LOG);
		
		for(LogFileName t : LogFileInfo.getAvailableSensors()){
			tmp = IOUtilities.createNewFile(parentDir, t.toString());
			if(tmp == null){
				Logger.e(TAG, "Create file " + t.toString() + " failed");
				return false;
			}
		}
		return true;
	}
		
	
	public static final class LogFileInfo {
		public static enum LogFileName{
			PACKET_SENT("packet_sent.csv"),
			PACKET_FORWARD("packet_forward.csv"),
			PACKET_RECEIVED("packet_received.csv"),
			FILE_CREATION("file_creation.csv"),
			FILE_RETRIEVAL("file_retrieval.csv"),
			NEIGHBORS("neighbors.csv"),
			TIMES("times.csv");
			
			private final String name;
			LogFileName(String n){
				this.name = n;
			}

			@Override
			public String toString() {
				return name;
			}
		}
		
		public static List<LogFileName> getAvailableSensors(){
			return Arrays.asList(LogFileName.values());
		}
	}
}
