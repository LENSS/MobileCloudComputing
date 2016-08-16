package edu.nps.secureshare.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;

import android.util.Log;

public class SecureShareNetworkClient {
	public static final String DEBUG_TAG = "Network Client";
	
	private Socket connection;
	
	public SecureShareNetworkClient(String host, int port) {
		try {
			connection = new Socket(host, port);
			Log.i(DEBUG_TAG, "Connected to " + connection.getInetAddress().toString());
		} catch (IOException e) {
			Log.e(DEBUG_TAG, "Could not connect to " + e.toString());
		}
	}
	
	public void close() {
		try {
			connection.close();
		} catch (IOException e) {
			Log.e(DEBUG_TAG, "Could not close connection " + e.toString());
		}
	}
	
	public void sendMessage(SecureShareMessage message) throws IOException {
		OutputStream os = connection.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(message);
		oos.flush();
	}
	
	public SecureShareMessage receiveMessage() throws StreamCorruptedException, IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
		Object o = ois.readObject();
		if (!(o instanceof SecureShareMessage)) {
			throw new IllegalArgumentException("Wanted SecureShareMessage, got " + o);
		}
		
		return (SecureShareMessage)o;
	}
}
