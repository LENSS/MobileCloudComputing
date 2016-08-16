package edu.tamu.lenss.mdfs.activities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import adhoc.aodv.Node;
import adhoc.aodv.RouteTableManager;
import adhoc.etc.IOUtilities;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import edu.tamu.lenss.mdfs.R;
import edu.tamu.lenss.mdfs.models.NodeInfo;

public class AODVDisplay extends Activity {
	private EditText forwardTable, requestTable, topologyTable;
	private Button refreshBtn, controlBtn, settingBtn, fileTestBtn, fileListBtn; 
	private TextView ipTV;
	private String myIp;
	private Node myNode;
	public static HashMap<Integer, NodeInfo> networkInfo = new HashMap<Integer, NodeInfo>(); 
	private static final String TAG = AODVDisplay.class.getSimpleName();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.aodv_display);
	    myIp = IOUtilities.getLocalIpAddress();
	    myNode=Node.getInstance(); 
	    
	    initUI(); 
	}
	
	private void initUI(){
		forwardTable = (EditText)findViewById(R.id.forwardET);
		requestTable = (EditText)findViewById(R.id.reqET);
		topologyTable = (EditText)findViewById(R.id.topologyET);
		
		refreshBtn = (Button)findViewById(R.id.refreshBtn);
		controlBtn = (Button)findViewById(R.id.controlBtn);
		settingBtn = (Button)findViewById(R.id.settingBtn);
		fileTestBtn = (Button)findViewById(R.id.fileTestBtn);
		fileListBtn = (Button)findViewById(R.id.fileListBtn);
		ipTV = (TextView)findViewById(R.id.ipTV);
		ipTV.setText(myIp);
		getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);		// Keep Screen On
		
		refreshBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				refreshTable();
			}
		});
		
		controlBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(getBaseContext(), AODVControl.class));
			}
		});
		
		settingBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(getBaseContext(), AODVSetting.class));
			}
		});
		
		fileTestBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(getBaseContext(), FileTest.class));
			}
		});
		
		fileListBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				startActivity(new Intent(getBaseContext(), DirectoryList.class));
			}
		});
	}
	
	private void refreshTable(){
		RouteTableManager manager = myNode.getRouteManager();
		forwardTable.setText(manager.getForwardTableSummary());
		requestTable.setText(manager.getRequestTableSummary());
		topologyTable.setText(networkInfo());
	}
	
	private String networkInfo(){
		StringBuilder builder=new StringBuilder("");
		Iterator<Entry<Integer, NodeInfo>>  iter = networkInfo.entrySet().iterator();
		NodeInfo node;
		while(iter.hasNext()){
			node = iter.next().getValue();
			builder.append("Node " + node.getSource());
			builder.append(" Rank: " + node.getRank());
			builder.append(" Battery: " + node.getBatteryLevel() + "%");
			builder.append(" On Time: " + node.getTimeSinceInaccessible() + "\n");
			builder.append("      Neighbors: ");
			for(Integer i : node.getNeighborsList())
				builder.append(i + ", ");
			builder.append("\n");
		}
		return builder.toString();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//myNode.stopThread();
	}
	
}
