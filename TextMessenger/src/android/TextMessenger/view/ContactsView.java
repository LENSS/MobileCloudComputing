package android.TextMessenger.view;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import adhoc.etc.IOUtilities;
import adhoc.etc.Logger;
import adhoc.tcp.TCPConnection;
import adhoc.tcp.TCPReceive.TCPReceiverData;
import adhoc.tcp.TCPSend;
import android.TextMessenger.control.ButtonListner;
import android.TextMessenger.control.ItemClickListener;
import android.TextMessenger.model.ClassConstants;
import android.TextMessenger.model.Contact;
import android.TextMessenger.model.ContactManager;
import android.TextMessenger.model.ObjToObsever;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactsView extends ListActivity implements Observer {
	private ContactManager contactManager;
	//String[] items = {};
	private ItemClickListener itemlisterner;
	private IconicAdapter ica ;
	private ArrayList<String> olga;
	private Button addContact, startServer;
	private ButtonListner listner;
	private Handler newContactHandler, removeContactHandler, updateContactHandler;
	private static final String TAG = ContactsView.class.getSimpleName();

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		olga = new ArrayList<String>();
		setContentView(R.layout.contacts);
		ica = new IconicAdapter();
		setListAdapter(ica);
		itemlisterner = new ItemClickListener(this,1);
		getListView().setOnItemLongClickListener(itemlisterner);
		addContact = (Button) findViewById(R.id.addcontact);
		listner = new ButtonListner(this);
		addContact.setOnClickListener(listner);
		
		startServer = (Button) findViewById(R.id.addcontact2);
		startServer.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				TCPConnection.getInstance();
			}
		});
		TCPConnection.getInstance().addObserver(this);
		
	   contactManager = ClassConstants.getInstance().getContactManager();
	   contactManager.addObserver(this);
	   
	   newContactHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				Bundle d = msg.getData();
				ica.add(d.getInt("CID")+"");
				ica.notifyDataSetChanged();
			}
		};
		
		removeContactHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				Bundle d = msg.getData();
				ica.remove(d.getInt("CID")+"");
				ica.notifyDataSetChanged();
			}
		};
		
		updateContactHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				ica.notifyDataSetChanged();
			}
		};
	    
//	   contactManager.addContact(22, "22olga", false);
//	   contactManager.addContact(33, "33olga", false);
//	   contactManager.addContact(44, "44olga", false);
//	   contactManager.addContact(55, "55olga", false);
//	   
//	   contactManager.setContactOnlineStatus(44, true);
//	   contactManager.setContactOnlineStatus(33, true);
	   
	}
	
	public void longPress(int position){
		int cID = Integer.parseInt(olga.get(position));
		contactManager.removeContact(cID);
	}

	/*
	 * This is a blocking call!!!
	 */
	@Override
	public void update(Observable observable, Object arg) {
		if(observable instanceof TCPConnection){
			TCPReceiverData data = (TCPReceiverData)arg;
			DataInputStream in = data.getDataInputStream();
			try {
				byte[] buffer = new byte[1024];
				File tmp0 = new File(Environment.getExternalStorageDirectory(), "AODVTest");
				File tmp = IOUtilities.createNewFile(tmp0, "1.jpg");
				FileOutputStream fos = new FileOutputStream(tmp);
				int readLen=0;
				while ((readLen = in.read(buffer)) >= 0) {
                    fos.write(buffer, 0, readLen);
				}
				Logger.v(TAG, "Finish reading data from InputStream");
				fos.close(); 
				data.close();
				
				/*data.getDataOutputStream().writeBytes("Returning message for forwarding test\r\n");
				data.getDataOutputStream().writeBytes("Returning message for forwarding test\r\n");
				data.close();*/
			} catch (IOException e) {
				e.printStackTrace();
			} finally{
				
			}
			
		}
		else{
			Contact c;
			Message m = new Message();
			Bundle b = new Bundle();
			ObjToObsever msg = (ObjToObsever) arg;
			int type = msg.getMessageType();
			switch (type) {

			case ObserverConst.CONTACT_ONLINE_STATUS_CHANGED:
				updateContactHandler.sendEmptyMessage(0);
				break;
			case ObserverConst.NEW_CONTACT:
				c = (Contact)msg.getContainedData();
				b.putInt("CID", c.getID());
				m.setData(b);		
				newContactHandler.sendMessage(m);	
				break;
			case ObserverConst.REMOVE_CONTACT:
				c = (Contact)msg.getContainedData();
				b.putInt("CID", c.getID());
				m.setData(b);		
				removeContactHandler.sendMessage(m);
				
				break;
			default:
				break;
			}
		}
	}
	
	public void addContact(){		
		//Intent i = new Intent(this, AddFriend.class);
		//startActivityForResult(i, 0);
		
		// File Transfer test
		TCPConnection conn = TCPConnection.getInstance();
		try {
			TCPSend send = conn.creatConnection("192.168.10.236");
			if(send == null){
				Logger.e(TAG, "Connection Failed");
				return;
			}
			DataOutputStream out = send.getOutputStream();
			
			File myFile = IOUtilities.getExternalFile("/Albums/1.jpg");
			
			byte [] mybytearray  = new byte [(int)myFile.length()];
			FileInputStream fis = new FileInputStream(myFile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			bis.read(mybytearray,0,mybytearray.length);
			out.write(mybytearray,0,mybytearray.length);
			Logger.v(TAG, "Finish writing data to OutStream");
			send.close();
			
			/*byte[] buf = new byte[1024];
			int len=0;
			while((len = send.getInputStream().read(buf)) >= 0){
				byte[] buf2 = new byte[len];
				System.arraycopy(buf, 0, buf2, 0, len);
				Logger.v(TAG, new String(buf2));
			}
			send.close();*/
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	// HTC
		
		/*File f = IOUtilities.getExternalFile("/Albums/android.jpg");
		RandomAccessFile f2;
		try {
			f2 = new RandomAccessFile(f, "r");
			byte[] b = new byte[(int)f2.length()];
			f2.read(b);
			ClassConstants.getInstance().getContactManager().sendFile(new BinaryData(b,"android.jpg"));
			f2.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/  
	}
	

	
	
	
	@SuppressWarnings("unchecked")
	class IconicAdapter extends ArrayAdapter {
		
		IconicAdapter() {
		//	super(ContactsView.this, R.layout.row, /*(En eller anden liste)*/ );
			super(ContactsView.this, R.layout.row, olga);
			
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.row, parent, false);
			//row.setOnClickListener(listener);
			TextView label = (TextView) row.findViewById(R.id.label);

			label.setText(contactManager.getContactDisplayName(Integer.parseInt(olga.get(position))));

			ImageView icon = (ImageView) row.findViewById(R.id.icon);

			if (contactManager.isContactOnline(Integer.parseInt(olga.get(position)))) {
				icon.setImageResource(R.drawable.svambe_bob);
			} else {
				icon.setImageResource(R.drawable.icon);
			}

			return (row);
		}
	}
}
