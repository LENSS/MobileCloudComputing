package android.TextMessenger.view;

import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import adhoc.aodv.Node;
import adhoc.aodv.exception.InvalidNodeAddressException;
import adhoc.etc.Debug;
import adhoc.etc.IOUtilities;
import adhoc.setup.AdhocManager;
import adhoc.setup.PhoneType;
import android.TextMessenger.control.ButtonListner;
import android.TextMessenger.model.ChatManager;
import android.TextMessenger.model.Constants;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

public class Connect extends Activity {
	private Button connect;
	private ButtonListner listener;
	private int myContactID;
	private Node node;
	private ChatManager chatManager;
	AdhocManager adHoc;
	String ip;
	int phoneType;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect);
		listener = new ButtonListner(this);
		connect = (Button) findViewById(R.id.connectButton);
		connect.setOnClickListener(listener);
		

		EditText name = (EditText) findViewById(R.id.displayName);
		name.setText(IOUtilities.getLocalIpAddress());
	}
	
	private int getPhoneType(){
		String model = Build.MODEL;
		String loc_ip = IOUtilities.getLocalIpAddress();
		myContactID = Integer.parseInt(loc_ip.substring(loc_ip.lastIndexOf(".")+1));
		Log.v("JAY", "myContactID: " + myContactID);
		return 1;		
	}

	public static native int runCommand(String command);

	static {
		//System.loadLibrary("adhocsetup");
	}

	/**
	 * When connect is clicked, a ad-hoc network is startet
	 */
	public void clickConnect() {
		EditText name = (EditText) findViewById(R.id.displayName);
		String myDisplayName = name.getText().toString();
		if (myDisplayName == "") {
			return;
		}
		//try {
			//int myContactID = nameToID(myDisplayName);
		phoneType = getPhoneType();
		ip = Constants.IP_PREFIX + myContactID;
		if(phoneType == -1){
			Log.d("PHONE", "No such phoneType");
			return;
		}
		
		
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		adHoc = new AdhocManager(this, wifi);
		//Starting an ad-hoc network
		//int result = Connect.runCommand("su -c \""+" startstopadhoc start "+phoneType+" "+ip+"\"");
		//Log.d("RESULTAT", ""+result);
		//Starting the routing protocol
		node = Node.getInstance(); 
		Debug.setDebugStream(System.out);
		chatManager = new ChatManager(myDisplayName, myContactID, node);
		
		node.startThread();

	

		Intent i = new Intent(this, TabView.class);
		startActivity(i);

		/*} catch (BindException e) {
			e.printStackTrace();
		} catch (InvalidNodeAddressException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}*/
		Log.d("DEBUG", "Node started ");
	}

	// FIXME HVORDAN SKAL MAN Faa TILDELT ID
	private int nameToID(String displayName) {
		return 4; // (int)(Math.random()*100);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// FIXME go to tabView
	}

	@Override
	protected void onDestroy() {
		if(node != null){
			node.stopThread();
		}
		super.onDestroy();
		if (adHoc != null) {
			//runCommand("su -c \"" + " startstopadhoc stop " + phoneType + " " + ip + "\"");
		}
	}
}
