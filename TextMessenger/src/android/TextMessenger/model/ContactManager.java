package android.TextMessenger.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;

import adhoc.aodv.Node;
import adhoc.etc.Logger;
import android.TextMessenger.model.pdu.Ack;
import android.TextMessenger.model.pdu.BinaryData;
import android.TextMessenger.model.pdu.Hello;
import android.TextMessenger.view.ObserverConst;


/**
 * Responsible for handling all received error messages about contacts: new user discovered, link breakage with an active user, user goes offline, user goes online..
 * @author rabie
 *
 */
public class ContactManager extends Observable{
	private HashMap<Integer, Contact> contacts;
	private Sender sender;
	private Timer timer;
	private String myDisplayName;
	private ChatManager chatManager;
	private CheckOfflineStatus checkOfflineStatus;
	private boolean offlineExists;
	private int mData;
	public static final String TAG = ContactManager.class.getSimpleName();
	
	public ContactManager(String myDisplayName, int myContactID, ChatManager chatManager, Node node){
		this.myDisplayName = myDisplayName;
		this.chatManager = chatManager;
		
		contacts = new HashMap<Integer, Contact>();
		
		offlineExists = false;		
		checkOfflineStatus = new CheckOfflineStatus();
		
		timer = new Timer(node, myDisplayName, myContactID, this, chatManager);
		sender = timer.getSender();
		checkOfflineStatus.start();
	}
	
	public void stopThread(){
		checkOfflineStatus.stopCheckOfflineStatusThread();
	}
	
	public Sender getSender(){
		return sender;
	}
	
	public ArrayList<String> getOnlineContactsID(){
		ArrayList<String> onlineContact = new ArrayList<String>();
		for(Contact c : contacts.values()){
			if(c.isOnline()){
				onlineContact.add(c.getID()+"");
			}
		}
		return onlineContact;
	}
	
	
	/**
	 * Creates a new contact or sets an existing contact to online
	 * @param contactID
	 * @param displayName
	 * @return true if new contact was created and false if contact already exists 
	 */
	public boolean addContact(int contactID, String displayName, boolean sendHello){
		Contact contact = contacts.get(contactID);
		if(contact == null){
			contact = new Contact(contactID, displayName);
			synchronized (contacts) {
				contacts.put(contactID, contact);
			}
			if(sendHello){
				sender.sendPDU(new Hello(myDisplayName, true), contactID);
			}
			else{
				contact.setIsOnline(true);
			}
			setChanged();
			notifyObservers(new ObjToObsever(contact, ObserverConst.NEW_CONTACT));
			
			return true;
		}
		setContactOnlineStatus(contactID, true);
		return false;
	}
	
	public void setContactOnlineStatus(int contactID, boolean isOnline) {
		Contact c = contacts.get(contactID);
		if (c != null) {
			c.setIsOnline(isOnline);
			if (!isOnline) {
				offlineExists = true;
				synchronized (contacts) {
					contacts.notify();
				}
			}
			setChanged();
			notifyObservers(new ObjToObsever(contactID, ObserverConst.CONTACT_ONLINE_STATUS_CHANGED));
		}
	}
	
	public boolean removeContact(int contactID){
		//setContactOnlineStatus(contactID, false);
		chatManager.removeChatsWhereContactIsIn(contactID);
		timer.removeAllPDUForContact(contactID);
		//TODO Behover maaske ikke notifye observer hvis det kun er view der bruger denne funktion
		setChanged();
		notifyObservers(new ObjToObsever(contacts.get(contactID), ObserverConst.REMOVE_CONTACT));
		contacts.remove(contactID);
		return true;
	}

	public boolean isContactOnline(int contactID) {
		Contact c = contacts.get(contactID);
		if(c != null){
			return c.isOnline();
		}
		return false;
	}
	
	public void routeEstablishmentFailurRecived(int contactID){
		setContactOnlineStatus(contactID, false);
		chatManager.removeChatsWhereContactIsIn(contactID);
		timer.removeAllPDUForContact(contactID);
	}
	
	public void routeInvalidRecived(int contactID){
		Hello hello = new Hello(myDisplayName, false);
		sender.sendPDU(hello, contactID);
	}
	
	public void routeEstablishedRecived(int contactID){
		if(!contacts.containsKey(contactID) || !contacts.get(contactID).isOnline()){
			Hello hello = new Hello(myDisplayName, false);
			sender.sendPDU(hello, contactID);
			
		}
	}
	
	public void helloRecived(Hello hello, int sourceContactID){
		Ack ack = new Ack(hello.getSequenceNumber());
		sender.resendPDU(ack, sourceContactID);		
		addContact(sourceContactID, hello.getSourceDisplayName(), hello.replyThisMessage());
	}
	
	public String getContactDisplayName(int contactID){
		synchronized(contacts){
			Contact c = contacts.get(contactID); 
			if(c != null)
			return c.getDisplayName();
			else return "Contact removed";
		}
	}
	
	private void helloToOffline(){
		offlineExists = false;
		synchronized (contacts) {
			for(Contact c : contacts.values()){
				if(!c.isOnline()){
					Hello hello = new Hello(myDisplayName, true);
					sender.sendPDU(hello, c.getID());
				}
			}	
		}
	}
	
	/**
	 * Testing purpose
	 */
	public void sendFile(BinaryData bData){
		sender.sendPDU(bData, 164);	// Send to HTC
		//sender.sendPDU(bData, 185);	// Send to moto
		Logger.v(TAG, "File test is sent");
	}
	
	private class CheckOfflineStatus extends Thread{
		private volatile boolean keepChecking = true;		
		@Override
		public void run() {
			while (keepChecking) {
				try {
					sleep(Constants.CHECK_TIME);
					synchronized (contacts) {
						while (!offlineExists)
							contacts.wait();
					}
					helloToOffline();
				} catch (InterruptedException e) {
				}
			}
		}
		
		public void stopCheckOfflineStatusThread(){
			keepChecking = false;
			this.interrupt();
		}
		
	}

}
