package android.TextMessenger.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;

import adhoc.aodv.Node;
import android.TextMessenger.exceptions.ContactOfflineException;
import android.TextMessenger.model.pdu.Ack;
import android.TextMessenger.model.pdu.ChatRequest;
import android.TextMessenger.model.pdu.Msg;
import android.TextMessenger.model.pdu.NoSuchChat;
import android.TextMessenger.view.ObserverConst;

public class ChatManager extends Observable {
	private HashMap<Integer, Chat> chats;
	private Sender sender;
	private ContactManager contactManager;
	private int myContactID;
	private String myDisplayName;

	public ChatManager(String myDisplayName, int myContactID, Node node) {
		this.myContactID = myContactID;
		this.myDisplayName = myDisplayName;
		chats = new HashMap<Integer, Chat>();	
		contactManager = new ContactManager(myDisplayName, myContactID, this, node);
		sender = contactManager.getSender();
		
		ClassConstants classConstant = ClassConstants.getInstance();
		classConstant.setContactManager(contactManager);
		classConstant.setChatManager(this);
	}

	public void sendText(String text, int chatID) throws ContactOfflineException {
		Chat chat = chats.get(chatID);
		if (chat != null) {
			//adds the messages to its own chat
			Msg msg = new Msg(chat.getNextMessageNum(), myContactID, chatID, text);
			chat.addMsg(msg);
			//sends the message to all other contacts in the chat
			for (int contact : chat.getContacts().keySet()) {
				if (contactManager.isContactOnline(contact)) {
					sender.sendPDU(msg, contact);
				} else {
					//removes the chat
					removeChat(chatID);
					throw new ContactOfflineException();
				}
			}
		}
	}
	
	public void textReceived(Msg msg, int sourceContact){
		Chat chat = chats.get(msg.getChatID());
		Ack ack = new Ack(msg.getSequenceNumber());
		sender.resendPDU(ack, sourceContact);
		
		if(chat != null){
			if(chat.addMsg(msg)){
				setChanged();
				notifyObservers(new ObjToObsever(msg.getChatID(), ObserverConst.TEXT_RECIVED));
			}
		}
		else{
			sender.sendPDU(new NoSuchChat(msg.getChatID()), sourceContact);
		}
		
	}
	
	public boolean removeChat(int chatID){
		Chat c = null;
		synchronized (chats) {
			c = chats.remove(chatID);	
		}
		if( c != null){
			c.disableChat();
			setChanged();
			//Notify
			notifyObservers(new ObjToObsever(c, ObserverConst.REMOVE_CHAT));
			return true;
		}
		return false;
	}
	
	public boolean removeChatsWhereContactIsIn(int contactID) {
		synchronized (chats) {
			ArrayList<Integer> removeChatID = new ArrayList<Integer>();
			for (Chat c : chats.values()) {
				if (c.getContacts().containsKey(contactID)) {
					removeChatID.add(c.getID());
				}
			}
			if (removeChatID.isEmpty()) {
				return false;
			} else {
				for (int i : removeChatID) {
					removeChat(i);
				}
				return true;
			}
		}
	}

	/**FIXME the chats hashmap is in danger for cuncurentsy
	 * when creating a chat from GUI no need to notify because it is the one creating the chat
	 * @param contactIDs
	 * @return returns true if a chat were created, and false if the chat already exists
	 * @throws Exception If one of the Contacts is offline
	 */
	public boolean newChat(HashMap<Integer, String> contactIDs) {
		// Creates a chat ID
		contactIDs.put(myContactID, myDisplayName);
		int chatID = createChatID(contactIDs.keySet().toArray());

		Chat chat = new Chat(contactIDs, chatID, myContactID, myDisplayName);
		Object returnResult = null;
		synchronized (chats) {
			returnResult = chats.put(chatID, chat);
		}
		if (returnResult == null) {
			contactIDs.put(myContactID, myDisplayName);
			for (Integer contactID : contactIDs.keySet()) {
				if(contactID != myContactID){
				ChatRequest chatRequest = new ChatRequest(chatID, contactIDs);
				sender.sendPDU(chatRequest, contactID);
				}
			}
			setChanged();
			notifyObservers(new ObjToObsever(chat, ObserverConst.NEW_CHAT));
			contactIDs.remove(myContactID);
			return true;
		}
		contactIDs.remove(myContactID);
		return false;
	}
	
	public Chat getChat(int ID){
		return chats.get(ID);
	}

	/**
	 * 
	 * @param pdu
	 * @param sourceContact
	 */
	public void chatRequestReceived(ChatRequest pdu, int sourceContact) {
		Ack ack = new Ack(pdu.getSequenceNumber());
		sender.resendPDU(ack,sourceContact);
		HashMap<Integer, String> contacts = pdu.getFriends();
		contacts.remove(myContactID);
		Chat chat = new Chat(contacts, pdu.getChatID(), myContactID, myDisplayName);
		Object returnResult = null;
		synchronized (chats) {
			returnResult = chats.put(pdu.getChatID(), chat);
		}
		if (returnResult == null) {
			for(int c : contacts.keySet()){
				contactManager.addContact(c , pdu.getFriends().get(c), false);
			}
			//Notify
			setChanged();
			notifyObservers(new ObjToObsever(chat, ObserverConst.NEW_CHAT));
		}

	}

	public void notifyTextNotSent(Msg msg) {
		Chat chat = chats.get(msg.getChatID());
		if(chat != null){
			chat.notifyTextNotSent(msg);
			setChanged();
			notifyObservers(new ObjToObsever(msg, ObserverConst.TEXT_NOT_SENT));
		}
	}
/**
 * Creats a unique hash sum from a unique String, with user id's sortet with insert sort
 * @param contactIDs
 * @return Unique ID for chat with contacts
 */
	private int createChatID(Object contactIDs[]) {
		int n = contactIDs.length;
		for (int i = 1; i < n; i++) {
			int value = (Integer) contactIDs[i];
			int j = i;
			while ((j > 0) && (((Integer) contactIDs[j - 1]) > value)) {
				contactIDs[j] = contactIDs[j - 1];
				j--;
			}
			contactIDs[j] = value;
		}
		String chatIDString = contactIDs[0] + "";
		for (int i = 1; i < n; i++) {
			chatIDString += ";" + contactIDs[i];
		}
		return chatIDString.hashCode();
	}
	
	public boolean chatExists(int cahtID){
		return chats.containsKey(cahtID);
	}
	
	public void noSuchChatRecived(NoSuchChat noChat, int sourceContact){
		Ack ack = new Ack(noChat.getSequenceNumber());
		sender.resendPDU(ack, sourceContact);
		removeChat(noChat.getChatID());
	}
}
