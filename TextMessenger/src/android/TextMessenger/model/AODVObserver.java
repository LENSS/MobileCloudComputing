package android.TextMessenger.model;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

import adhoc.aodv.Node;
import adhoc.aodv.Node.MessageToObserver;
import adhoc.aodv.Node.PacketToObserver;
import adhoc.aodv.ObserverConst;
import adhoc.aodv.exception.BadPduFormatException;
import adhoc.etc.IOUtilities;
import adhoc.etc.Logger;
import android.TextMessenger.model.pdu.BinaryData;
import android.TextMessenger.model.pdu.ChatRequest;
import android.TextMessenger.model.pdu.Hello;
import android.TextMessenger.model.pdu.Msg;
import android.TextMessenger.model.pdu.NoSuchChat;
import android.os.Environment;
import android.util.Log;

public class AODVObserver implements Observer {
	private Timer timer;
	private ContactManager contactManager;
	private ChatManager chatManager;
	public static final String TAG = AODVObserver.class.getSimpleName();
	
	public AODVObserver(Node node, String myDisplayName, int myContactID, Timer timer, ContactManager contactManager ,ChatManager chatManager) {
		this.chatManager = chatManager;
		this.timer = timer;
		this.contactManager = contactManager;
		node.addObserver(this);
	}

	@Override
	public void update(Observable o, Object arg) {
		MessageToObserver msg = (MessageToObserver)arg;
		int userPacketID, destination, type = msg.getMessageType();
		switch (type) {
		case ObserverConst.ROUTE_ESTABLISHMENT_FAILURE:
			int unreachableDestinationAddrerss  = (Integer)msg.getContainedData();
			contactManager.routeEstablishmentFailurRecived(unreachableDestinationAddrerss);
			break;
		case ObserverConst.DATA_RECEIVED:
			parseMessage(	(Integer)((PacketToObserver)msg).getSenderNodeAddress(),
							(byte[])msg.getContainedData()	);
			break;
		case ObserverConst.INVALID_DESTINATION_ADDRESS:
			userPacketID = (Integer)msg.getContainedData();
			//FIXME slet fra timer og Contacts
			break;
		case ObserverConst.DATA_SIZE_EXCEEDES_MAX:
			userPacketID = (Integer)msg.getContainedData();
			//FIXME slet fra timer
			break;
		case ObserverConst.ROUTE_INVALID:
			destination  = (Integer)msg.getContainedData();
			contactManager.routeInvalidRecived(destination);
			break;
		case ObserverConst.ROUTE_CREATED:
			destination = (Integer)msg.getContainedData();
			contactManager.routeEstablishedRecived(destination);
			break;
		default:
			break;
		}
	}
	
	private void parseMessage(int senderID, byte[] data){
		String[] split = new String(data).split(";",2);
		try {
			int type = Integer.parseInt(split[0]);
			switch (type) {
			
			case Constants.PDU_MSG:
				System.out.println("Recived: Msg");
				Msg msg = new Msg(senderID);
				msg.parseBytes(data);
				chatManager.textReceived(msg, senderID);
				break;
			case Constants.PDU_ACK:
				//System.out.println("Recived: ACK");
				timer.removePDU(Integer.parseInt(split[1]));
				break;
			case Constants.PDU_CHAT_REQUEST:
				
				ChatRequest chatReq = new ChatRequest();
				chatReq.parseBytes(data);
				//("Recived: Chat Req :  "+chatReq.getSequenceNumber());
				chatManager.chatRequestReceived(chatReq,senderID);
				break;
			case Constants.PDU_HELLO:
				Hello hello = new Hello();
				hello.parseBytes(data);
				System.out.println("TxtMsg - Reciver: Hello from ID: "+senderID+", Return: " + hello.replyThisMessage());
				Log.v(TAG, "Hello from " + senderID);
				contactManager.helloRecived(hello, senderID);
				break;
			case Constants.PDU_NO_SUCH_CHAT:
				//("Recived: No s Chat");
				NoSuchChat noSuchChat = new NoSuchChat();
				noSuchChat.parseBytes(data);
				chatManager.noSuchChatRecived(noSuchChat,senderID);
				break;
			case Constants.PDU_MDFS_FILE:
				
				BinaryData bData = new BinaryData();
				bData.parseBytes(data);
				Logger.i(TAG, "File Name:" + bData.getFileName());
				File tmp = new File(Environment.getExternalStorageDirectory(), "AODVTest");
				IOUtilities.byteToFile(bData.getData(),tmp, bData.getFileName());
				
				break;
			default:
				break;
			}
		} catch (NumberFormatException e) {
			//discard the message.
		} catch (BadPduFormatException e) {
			//discard the message
			// Message is in the domain of invalid messages
		}
	}

}
