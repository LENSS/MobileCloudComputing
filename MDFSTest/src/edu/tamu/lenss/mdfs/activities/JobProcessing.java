package edu.tamu.lenss.mdfs.activities;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import adhoc.etc.Logger;
import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.DataAnalyzer;
import edu.tamu.lenss.mdfs.DataAnalyzer.DataAnalyzerListener;
import edu.tamu.lenss.mdfs.MDFSDirectory;
import edu.tamu.lenss.mdfs.MDFSFileCreator;
import edu.tamu.lenss.mdfs.MDFSFileCreator.MDFSFileCreatorListener;
import edu.tamu.lenss.mdfs.R;
import edu.tamu.lenss.mdfs.comm.ServiceHelper;
import edu.tamu.lenss.mdfs.comm.TopologyHandler.TopologyListener;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.models.NodeInfo;
import edu.tamu.lenss.mdfs.models.TaskResult;
import edu.tamu.lenss.mdfs.models.TaskSchedule;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;

public class JobProcessing extends Activity {
	private Button createFileBt, processFileBt;
	private TextView statusTV;
	private DataAnalyzer analyzer;
	private ServiceHelper serviceHelper;
	private JobProcessingLog jobLog = new JobProcessingLog();
	private static final double KN_RATIO = 0.5;
	private static final String TAG = JobProcessing.class.getSimpleName();
	
	public static final String DEFAULT_IMG_DIR = "df_images";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.processing_screen);		
		this.serviceHelper = ServiceHelper.getInstance();
		this.analyzer = new DataAnalyzer(this, dataListener);
		initUI();		
	}
	
	private void initUI(){
		createFileBt = (Button)findViewById(R.id.createJobsBtn);
		processFileBt = (Button)findViewById(R.id.processBtn);
		statusTV = (TextView)findViewById(R.id.statusTV);
		statusTV.setMovementMethod(new ScrollingMovementMethod());
		
		createFileBt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				distributeDefaultImgs();
			}
		});
		processFileBt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				//analyzeLocalData();
				topDiscover();
			}
		});
	}
	
	private void analyzeLocalData(){
		MDFSDirectory directory = serviceHelper.getDirectory();
		List<MDFSFileInfo> fileList = directory.getFileList();
		if(!fileList.isEmpty()){
			Logger.v(TAG, "Total " + fileList.size() + " files");
			analyzer.downloadFile(fileList);
		}
	}
	
	/**
	 * Send some pre-loaded data to the network
	 */
	private void distributeDefaultImgs(){
		// Read local file
		File dir = AndroidIOUtils.getExternalFile(DEFAULT_IMG_DIR + "/");
		final File[] files = dir.listFiles();
		
		if(files == null)
			return;
		
		// Retrieve a file every 3 seconds
		new CountDownTimer(files.length * 5000+1000, 5000) {
			int count = 0;

			public void onTick(long millisUntilFinished) {
				if(count < files.length){
					MDFSFileCreator creator = new MDFSFileCreator(files[count], Constants.KEY_CODING_RATIO, Constants.FILE_CODING_RATIO, fileCreatorListener);
					creator.setDeleteFileWhenComplete(false);
					creator.start();
					count++;
				}
			}

			public void onFinish() {
			}
		}.start();
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}
	
	/**
	 * Get the number of nodes in the network and the number of tasks
	 * Randomly assign task to nodes 
	 * Send out schedule
	 */
	private void topDiscover(){
		serviceHelper.startTopologyDiscovery(topologyListener);
		
	}
	
	private Set<Long> curTasks = new HashSet<Long>();
	private void scheduleJobs(List<NodeInfo> topList){
		MDFSDirectory directory = serviceHelper.getDirectory();
		List<MDFSFileInfo> fileList = directory.getFileList();
		TaskSchedule schedule = new TaskSchedule();
		
		int myNode = serviceHelper.getMyNode().getNodeId();
		int nodeCnt = topList.size();
		int selectedIdx, processorNode;
		int n_par = (int) Math.round(topList.size()*0.8);
		if(n_par == topList.size())
			n_par--;
		
		int k_par = (int) Math.round(n_par*KN_RATIO);
		Random rnd = new Random();
		curTasks.clear();
		// Replicate each file to n-k+1 different nodes except myself
		/*Set<Integer> inserted  = new HashSet<Integer>();
		for(MDFSFileInfo file:fileList){
			inserted.clear();
			while(true){
				selectedIdx = rnd.nextInt(nodeCnt);
				processorNode = topList.get(selectedIdx).getSource();
				if(processorNode != myNode && !inserted.contains(processorNode)){
					schedule.insertTask(processorNode, file);
					inserted.add(processorNode);
					Logger.v(TAG, "Assign task " + file.getFileName() + " to Node " + processorNode);
					updateStatusBar("Assign task " + file.getFileName() + " to Node " + processorNode);
					if(inserted.size() >= n_par-k_par+1)
						break;
				}
			}			
			//schedule.insertTask(topList.get(selectedNode).getSource(), file);
			//Logger.v(TAG, "Assign task " + file.getFileName() + " to Node " + topList.get(selectedNode).getSource());
			curTasks.add(file.getCreatedTime());
		}*/
		
		//for(int i=0; i<n_par-k_par+1; i++){
		Set<Integer> inserted  = new HashSet<Integer>();
		for(int i=0; i<2; i++){
			inserted.clear();
			for(MDFSFileInfo file:fileList){
				while(true){
					selectedIdx = rnd.nextInt(nodeCnt);
					processorNode = topList.get(selectedIdx).getSource();
					if(processorNode != myNode && !inserted.contains(processorNode)){
						schedule.insertTask(processorNode, file);
						updateStatusBar("Assign task " + file.getFileName() + " to Node " + processorNode);
						inserted.add(processorNode);
						if(inserted.size() >= topList.size()-1)
							inserted.clear();
						break;
					}
				}
			}
		}
		for(MDFSFileInfo file:fileList){
			curTasks.add(file.getCreatedTime());
		}
		//serviceHelper.broadcastJobSchedule(schedule, jobListener);
		jobLog.networkSize = topList.size();
		jobLog.myNode = myNode;
		jobLog.fileCount = fileList.size();
		jobLog.n_par = n_par;
		jobLog.k_par = k_par;
		jobLog.startT = System.currentTimeMillis();
		
	}
	
	private TopologyListener topologyListener = new TopologyListener() {
		@Override
		public void onError(String msg) {
			Logger.e(TAG, msg);
		}

		@Override
		public void onComplete(List<NodeInfo> topList) {
			Message m = new Message();
			m.what = HANDLE_TOPOLOGY_REQUEST;
			m.obj = topList;
			uiHandler.sendMessage(m);
		}
	};
	
	private void updateStatusBar(String msg){
		Message m = new Message();
		m.what = HANDLE_STATUS_MSG;
		m.obj = msg;
		uiHandler.sendMessage(m);
	}
	
	private static final int HANDLE_STATUS_MSG = 0;
	private static final int HANDLE_TOPOLOGY_REQUEST = 1;
	
	private StringBuilder strBuilder = new StringBuilder();
	private Handler uiHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case HANDLE_STATUS_MSG:
				strBuilder.append((String)msg.obj + "\n");
				statusTV.setText(strBuilder.toString() + "  ...");
				break;
			case HANDLE_TOPOLOGY_REQUEST:
				scheduleJobs((List<NodeInfo>)msg.obj );
				updateStatusBar("Complete Topology Discovery");
				break;
			}
		}
	};
	
	private DataAnalyzerListener dataListener = new DataAnalyzerListener(){
		@Override
		public void onError(String error) {
			updateStatusBar(error);
		}

		@Override
		public void statusUpdate(String status) {
			updateStatusBar(status);
		}

		@Override
		public void onDetectedFace(TaskResult result) {
			
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
	
	private JobManagerListener jobListener = new JobManagerListener(){
		@Override
		public void onNewResult(TaskResult result) {
			//Logger.i(TAG, result.getResult() + " faces in " + result.getTaskName() + " from node " + result.getSource());
			updateStatusBar(result.getResult() + " faces in " + result.getTaskName() + " from node " + result.getSource());
			curTasks.remove(result.getFileId());
			if(curTasks.size()<=0){
				serviceHelper.broadcastJobComplete();
				jobLog.endT=System.currentTimeMillis();
				Logger.i(TAG, "All tasks have been completed");				
				updateStatusBar("All tasks have been completed");
				writeLog();
			}
		}
	};
	
	private void writeLog(){
		//AndroidDataLogger dataLogger = ServiceHelper.getInstance().getDataLogger();
		StringBuilder str = new StringBuilder();
		str.append(jobLog.myNode + ", ");
		str.append("JobProcessing" + ", ");
		
		str.append(jobLog.fileCount + ", ");
		str.append(jobLog.n_par + ", ");
		str.append(jobLog.k_par+ ", ");
		str.append(jobLog.startT + ", ");
		str.append(jobLog.getDiff(jobLog.startT, jobLog.endT) + " \n\n");
		//dataLogger.appendSensorData(LogFileName.TIMES, str.toString());
	}
	
	/**
	 * Listen for the taskResult
	 * @author Jay
	 */
	public interface JobManagerListener {		
		public void onNewResult(TaskResult result);
	}
	
	private class JobProcessingLog{
		public long startT, endT;
		public int myNode, n_par, k_par, fileCount, networkSize;
		
		public String getDiff(long l1, long l2){
			return Long.toString(Math.abs(l2-l1));
		}
	}
}
