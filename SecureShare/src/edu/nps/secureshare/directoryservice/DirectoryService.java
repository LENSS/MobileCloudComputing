package edu.nps.secureshare.directoryservice;

import java.io.IOException;
import java.util.List;

import edu.nps.secureshare.android.services.NetworkServerService;
import edu.nps.secureshare.network.DsFragmentQueryMessage;
import edu.nps.secureshare.network.DsFragmentReplyMessage;
import edu.nps.secureshare.network.DsGetNeighborsQueryMessage;
import edu.nps.secureshare.network.DsGetNeighborsReplyMessage;
import edu.nps.secureshare.network.DsListFilesQueryMessage;
import edu.nps.secureshare.network.DsListFilesReplyMessage;
import edu.nps.secureshare.network.DsRegisterFileMessage;
import edu.nps.secureshare.network.DsRegisterFragmentMessage;
import edu.nps.secureshare.network.DsRegisterNeighborMessage;
import edu.nps.secureshare.network.SecureShareMessage;
import edu.nps.secureshare.network.SecureShareNetworkClient;

public class DirectoryService {
 
	//private final DatabaseHelper mDbhelper; 
	private String mServerIpAddress;
	private int mServerPort;
	//private SecureShareNetworkClient mNetworkClient;
	
	public DirectoryService(String serverIpAddress){
		
		mServerIpAddress = serverIpAddress;
		mServerPort = NetworkServerService.SERVER_PORT;
		//mNetworkClient = new SecureShareNetworkClient(mServerIpAddress, mServerPort);
	}

	/*@Override
	protected void finalize() throws Throwable {
		try {
			mNetworkClient.close();
		} finally {
			super.finalize();
		}
	}*/

	public void registerFile(DataRecord record) throws IOException {
		// create a new message and send
		DsRegisterFileMessage message = new DsRegisterFileMessage(record);
		SecureShareNetworkClient client = new SecureShareNetworkClient(mServerIpAddress, mServerPort);
		client.sendMessage(message);
		//client.close();
		//mNetworkClient.sendMessage(message);
	}
	
	public void registerFragment(String fragHash, String address) throws IOException {
		DsRegisterFragmentMessage message = new DsRegisterFragmentMessage(fragHash, address);
		SecureShareNetworkClient client = new SecureShareNetworkClient(mServerIpAddress, mServerPort);
		client.sendMessage(message);
		//client.close();
		//mNetworkClient.sendMessage(message);
	}
	
	public List<DataRecord> list() throws IOException, ClassNotFoundException {
		DsListFilesQueryMessage message = new DsListFilesQueryMessage();
		SecureShareNetworkClient client = new SecureShareNetworkClient(mServerIpAddress, mServerPort);
		client.sendMessage(message);
		//mNetworkClient.sendMessage(message);
		
		Object o = client.receiveMessage();
		//Object o = mNetworkClient.receiveMessage();
		
		if (!(o instanceof SecureShareMessage))
			throw new IllegalArgumentException("Wanted SecureShareMessage, got " + o);
		
		DsListFilesReplyMessage replyMessage = (DsListFilesReplyMessage)o;
		List<DataRecord> records = replyMessage.getRecords();
		//client.close();
		return records;
	}
	
	public String whoHasFragment(String fragHash) throws IOException, ClassNotFoundException {
		DsFragmentQueryMessage message = new DsFragmentQueryMessage(fragHash);
		SecureShareNetworkClient client = new SecureShareNetworkClient(mServerIpAddress, mServerPort);
		client.sendMessage(message);
		//mNetworkClient.sendMessage(message);
		
		Object o = client.receiveMessage();
		//Object o = mNetworkClient.receiveMessage();
		
		if (!(o instanceof SecureShareMessage))
			throw new IllegalArgumentException("Wanted SecureShareMessage, got " + o);
		
		int type = ((SecureShareMessage)o).getType();
		
		String address = null;
		
		switch (type) {
		case SecureShareMessage.DS_FRAG_REPLY:
			DsFragmentReplyMessage replyMessage = (DsFragmentReplyMessage)o;
			address = replyMessage.getAddress();
			break;
		}
		//client.close();
		return address;
	}
	
	public void registerNeighbor(String neighbor) throws IOException {
		// create a new message and send
		DsRegisterNeighborMessage message = new DsRegisterNeighborMessage(neighbor);
		SecureShareNetworkClient client = new SecureShareNetworkClient(mServerIpAddress, mServerPort);
		client.sendMessage(message);
		//client.close();
		//mNetworkClient.sendMessage(message);
	}
	
	public String[] getNeighbors(int n) throws IOException, ClassNotFoundException {
		DsGetNeighborsQueryMessage message = new DsGetNeighborsQueryMessage(n);
		SecureShareNetworkClient client = new SecureShareNetworkClient(mServerIpAddress, mServerPort);
		client.sendMessage(message);
		//client.close();
		//mNetworkClient.sendMessage(message);
		
		Object o = client.receiveMessage();
		//Object o = mNetworkClient.receiveMessage();
		
		if (!(o instanceof SecureShareMessage))
			throw new IllegalArgumentException("Wanted SecureShareMessage, got " + o);
		
		DsGetNeighborsReplyMessage replyMessage = (DsGetNeighborsReplyMessage)o;
		String[] neighbors = replyMessage.getNeighbors();
		//client.close();
		return neighbors;
	}
}
