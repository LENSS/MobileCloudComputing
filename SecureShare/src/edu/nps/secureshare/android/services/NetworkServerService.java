package edu.nps.secureshare.android.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import edu.nps.secureshare.android.DatabaseHelper;
import edu.nps.secureshare.android.R;
import edu.nps.secureshare.directoryservice.DataRecord;
import edu.nps.secureshare.mdfs.FragmentContainer;
import edu.nps.secureshare.network.DsFragmentHashNotFoundMessage;
import edu.nps.secureshare.network.DsFragmentQueryMessage;
import edu.nps.secureshare.network.DsFragmentReplyMessage;
import edu.nps.secureshare.network.DsGetNeighborsQueryMessage;
import edu.nps.secureshare.network.DsGetNeighborsReplyMessage;
import edu.nps.secureshare.network.DsListFilesQueryMessage;
import edu.nps.secureshare.network.DsListFilesReplyMessage;
import edu.nps.secureshare.network.DsRegisterFileMessage;
import edu.nps.secureshare.network.DsRegisterFragmentMessage;
import edu.nps.secureshare.network.DsRegisterNeighborMessage;
import edu.nps.secureshare.network.SecureShareFragmentRequestMessage;
import edu.nps.secureshare.network.SecureShareMessage;
import edu.nps.secureshare.network.SecureShareStoreFragmentMessage;
import edu.nps.secureshare.network.SecureShareTransmitFragmentMessage;

public final class NetworkServerService extends Service {
	private String DEBUG_TAG = "NetworkServerService";
	
	// Server Port
	public static final int SERVER_PORT = 8087;
	
	// Server IP address
	public static String SERVER_IP = null;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		SERVER_IP = getLocalIpAddress();
		Log.i(DEBUG_TAG, "Local IP Address is: " + SERVER_IP);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(DEBUG_TAG, "Recieved start id " + startId + ": " + intent);
		Thread serverThread = new Thread(new ServerThread());
		serverThread.start();
		return START_STICKY;
	}
	
	/**
	 * @return Gets the IP address of the client
	 */
	public static String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); 
				en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); 
					enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("Get IP Address", ex.toString());
		}
		return null;
	}


	public class ServerThread implements Runnable {
		ServerSocket server = null;

		@Override
		public void run() {
			createSocket(SERVER_PORT);
			while (true) {
				try {
					Socket client = server.accept();
					Log.i(DEBUG_TAG, "Connected from " + client.getInetAddress().toString());
					MessageHandler handler = new MessageHandler(getApplicationContext(), client);
					Thread thread = new Thread(handler);
					thread.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}		
		}	
		
		/*@Override
		protected void finalize() throws Throwable {
			try {
				server.close();
			} catch (IOException e) {
				Log.d(DEBUG_TAG, "Could not close server socket");
			} finally {
				super.finalize();
			}
		}*/


		public boolean createSocket(int port) {
			try {
				server = new ServerSocket(port);
				Log.i(DEBUG_TAG, "Listening on port: " + port);
			} catch (IOException e) {
				Log.d(DEBUG_TAG, "Could not listen on port: " + port);
				return false;
			}
			return true;
		}
	
	}
}

final class MessageHandler implements Runnable {
	private final static String DEBUG_TAG = "Message Handler";
	final static String CRLF = "\r\n";
	Socket mSocket;
	Context mctx;
	
	//ObjectInputStream mOis;
	//ObjectOutputStream mOos;
	
	// Storage Flags
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	
	// Directories
	private String mFragmentsDirectoryPath;
	
	// DatabaseHelper
	private DatabaseHelper mDbhelper;
	
	// Constructor
	public MessageHandler(Context context, Socket socket) throws Exception {
		this.mctx = context;
		this.mSocket = socket;
		this.mDbhelper = new DatabaseHelper(mctx);
		Log.i(DEBUG_TAG, "Message handler created");
		
		// Set External Storage Flags
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		// Set up fragments path
		mFragmentsDirectoryPath = mctx.getExternalFilesDir(null).toString() + "/" +
			mctx.getResources().getString((R.string.fragment_directory));
		
		//mOis = new ObjectInputStream(this.mSocket.getInputStream());;
		//mOos = new ObjectOutputStream(this.mSocket.getOutputStream());
		Log.i(DEBUG_TAG, "Created streams");
	}

	@Override
	public void run() {
		try {
			processMessage();
		} catch (Exception e) {
			Log.e(DEBUG_TAG, "Could not process message: " + e.toString());
			e.printStackTrace();
		}
	}
	
	private void processMessage() throws IOException, ClassNotFoundException {
		Log.i(DEBUG_TAG, "Processing message");
		InputStream is = this.mSocket.getInputStream();
		OutputStream os = this.mSocket.getOutputStream();
		
		ObjectInputStream ois = new ObjectInputStream(is);
		ObjectOutputStream oos = new ObjectOutputStream(os);
		Log.i(DEBUG_TAG, "Created streams");
		
		Object o = ois.readObject();
		if (!(o instanceof SecureShareMessage))
			throw new IllegalArgumentException("Wanted SecureShareMessage, got " + o);
			
		// Valid, so get type and process
		int type = ((SecureShareMessage)o).getType();
		
		switch (type) {
		case SecureShareMessage.ACK_MSG:
			Log.i(DEBUG_TAG, "Message is an ACK");
			// Do nothing for now
			break;
		case SecureShareMessage.STORE_FRAG_REQUEST:
			Log.i(DEBUG_TAG, "Message is a store request");
			storeFragment(((SecureShareStoreFragmentMessage)o));
			break;
		case SecureShareMessage.FRAG_REQ:
			Log.i(DEBUG_TAG, "Message is a fragment retrieve request");
			FragmentContainer fragment = retrieveFragment((SecureShareFragmentRequestMessage)o);
			SecureShareTransmitFragmentMessage fragMessage = new SecureShareTransmitFragmentMessage(fragment);
			oos.writeObject(fragMessage);
			oos.flush();
			break;
		case SecureShareMessage.DS_REGISTER_NEIGHBOR:
			Log.i(DEBUG_TAG, "Message is a register neighbor request");
			registerNeighbor((DsRegisterNeighborMessage)o);
			break;
		case SecureShareMessage.DS_REGISTER_FILE:
			Log.i(DEBUG_TAG, "Message is a register file");
			registerFile((DsRegisterFileMessage)o);
			break;
		case SecureShareMessage.DS_REGISTER_FRAG:
			Log.i(DEBUG_TAG, "Message is a register fragment");
			registerFragment((DsRegisterFragmentMessage)o);
			break;
		case SecureShareMessage.DS_LIST_FILES_QUERY:
			Log.i(DEBUG_TAG, "Message is a list files query");
			List<DataRecord> records = listFiles((DsListFilesQueryMessage)o);
			DsListFilesReplyMessage listFilesMessage = new DsListFilesReplyMessage(records);
			oos.writeObject(listFilesMessage);
			oos.flush();
			break;
		case SecureShareMessage.DS_FRAG_QUERY:
			Log.i(DEBUG_TAG, "Message is a frag query");
			String address = null;
			try {
				address = whoHasFrag((DsFragmentQueryMessage)o);
			} catch (CursorIndexOutOfBoundsException e) {
				DsFragmentHashNotFoundMessage hashNotFoundMessage = new DsFragmentHashNotFoundMessage();
				oos.writeObject(hashNotFoundMessage);
				oos.flush();
				break;
			}
			
			// Now send the reply back through the socket
			DsFragmentReplyMessage fragmentReplyMessage = new DsFragmentReplyMessage(address);
			oos.writeObject(fragmentReplyMessage);
			oos.flush();
			break;
		case SecureShareMessage.DS_GET_NEIGHBORS_QUERY:
			Log.i(DEBUG_TAG, "Message is a neighbors query");
			String[] neighbors = getNeighbors((DsGetNeighborsQueryMessage)o);
			// Now write neighbors to oos
			DsGetNeighborsReplyMessage neighborsReplyMessage = new DsGetNeighborsReplyMessage(neighbors);
			oos.writeObject(neighborsReplyMessage);
			oos.flush();
			break;
		} 
		ois.close();
		oos.close();
	}
	
	/*
	 * Helper functions specific to each message type
	 */

	private String[] getNeighbors(DsGetNeighborsQueryMessage message) {
		Cursor c = mDbhelper.getNeighbors();
		int n = message.getN();
		c.moveToFirst();
		
		String[] neighbors = new String[n];
		for (int i=0;i<n;i++) {
			neighbors[i] = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.NEIGHBORS_IP));
			if (!c.moveToNext()) {
				c.moveToFirst();
			}
		}
		
		return neighbors;
	}

	private String whoHasFrag(DsFragmentQueryMessage message) {
		String fragHash = message.getFragHash();
		Cursor c = mDbhelper.whoHasFragment(fragHash);
		c.moveToFirst();
		Log.i(DEBUG_TAG, "There were " + c.getCount() + " records returned");
		String result = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.FRAGMENT_LOCATION));
		c.close();
		return result;
	}

	private List<DataRecord> listFiles(DsListFilesQueryMessage message) {
		Cursor c = mDbhelper.list();
		//startManagingCurson(c);
		int numFiles = c.getCount();
		Log.d(DEBUG_TAG, "Query returned " + numFiles + " files");
		List<DataRecord> records = new ArrayList<DataRecord>();
		//DataRecord[] records = new DataRecord[numFiles];
		c.moveToFirst();
		for (int i=0;i<numFiles;i++) {
			String filename = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.FILE_NAME));
			long timestamp = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.FILE_TIME_STAMP));
			int n = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.FILE_N));
			int k = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.FILE_K));
			String source = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.FILE_SOURCE));
			records.add(new DataRecord(filename, timestamp, n, k, source));
			c.moveToNext();
		}
		c.close();
		return records;
		
		// Now send the reply back through the socket
		
	}

	private void registerFragment(DsRegisterFragmentMessage message) {
		mDbhelper.registerFragment(message.getFragHash(), message.getAddress());	
	}

	private void registerFile(DsRegisterFileMessage message) {
		mDbhelper.registerFile(message.getDatarecord());
	}

	private void storeFragment(SecureShareStoreFragmentMessage message) {
		FragmentContainer fragment = message.getContainer();
		Log.i(DEBUG_TAG, "Call to store fragments of size: " + fragment.getFragment().length);
		
		Log.i(DEBUG_TAG, "The fragments are stored in " + mFragmentsDirectoryPath);
		File fragmentsDirectory = new File(mFragmentsDirectoryPath);
		if (!fragmentsDirectory.exists()) {
			new File(mFragmentsDirectoryPath).mkdirs();
		}
		
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			File outFile = new File(mFragmentsDirectoryPath, fragment.getFragmentHash() + ".frag");
			fos = new FileOutputStream(outFile);
			out = new ObjectOutputStream(fos);
			out.writeObject(fragment);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private FragmentContainer retrieveFragment(SecureShareFragmentRequestMessage message) {
		FragmentContainer fragment = getFragment(message.getFragHash());
		return fragment;
	}
	
	private void registerNeighbor(DsRegisterNeighborMessage message) {
		String neighbor = message.getNeighbor();
		Log.i(DEBUG_TAG, "Registering neighbor: " + neighbor);
		// TODO: check to see if already in DB.  If so, do nothing, else register
		mDbhelper.registerNeighbor(neighbor);
	}
	
	
	// Helper function to get file from the filesystem given a hash
	private FragmentContainer getFragment(String fragHash) {
		Object o = null;
		
		// retrieve the fragment and send on the oos
		try {
			File inFile = new File(mFragmentsDirectoryPath, fragHash + ".frag");
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inFile));
			o = ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		if (!(o instanceof FragmentContainer))
			throw new IllegalArgumentException("Wanted FragmentContainer, got " + o);
		
		FragmentContainer fragment = (FragmentContainer)o;
		
		return fragment;
		
	}
	
}
