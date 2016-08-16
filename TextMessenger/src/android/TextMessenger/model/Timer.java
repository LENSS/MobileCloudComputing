package android.TextMessenger.model;

import java.util.HashMap;
import java.util.LinkedList;

import adhoc.aodv.Node;
import android.TextMessenger.model.pdu.ChatRequest;
import android.TextMessenger.model.pdu.Msg;
import android.TextMessenger.model.pdu.PduInterface;

public class Timer extends Thread {
	private ChatManager chatManager;
	private ContactManager contactManager;
	private Sender sender;
	private volatile boolean keepRunning = true;
	private HashMap<Integer, Integer> pduIdentifiers = new HashMap<Integer, Integer>();
	private LinkedList<PduInterface> aliveQueue = new LinkedList<PduInterface>();
	//TODO sync??
	//private final Object timerLock = new Integer(0);
	
	public Timer(Node node, String myDisplayName, int myContactID, ContactManager contactManager, ChatManager chatManager) {
		this.sender = new Sender(node, this);
		this.contactManager = contactManager;
		this.chatManager = chatManager;
		AODVObserver aodvobs = new AODVObserver(node, myDisplayName, myContactID, this, contactManager, chatManager);
		this.start();
	}
	
	public Sender getSender(){
		return sender;
	}
	
	public boolean setTimer(PduInterface pdu, int destContactID) {
		Integer pduExists;
		pdu.setTimer();
		
		synchronized (pduIdentifiers) {
			pduExists = pduIdentifiers.put(pdu.getSequenceNumber(), destContactID);
		if (pduExists != null){
			return false;
		}
		
		aliveQueue.addLast(pdu);
		}
		synchronized (aliveQueue) {
			aliveQueue.notify();
		}
		
		return true;
	}
	
	public void run(){
		while(keepRunning){
			try {
				synchronized (aliveQueue) {
					while(aliveQueue.isEmpty()){
						aliveQueue.wait();
					}	
				}
				
				PduInterface pdu = aliveQueue.peek();
				if(pdu != null){
				long timeToDie = pdu.getAliveTime();
				long sleepTime = timeToDie - System.currentTimeMillis();
				if(sleepTime > 0){
					sleep(sleepTime);
				}
				
					while(timeToDie <= System.currentTimeMillis()){
						synchronized (pduIdentifiers) {
							if(pduIdentifiers.containsKey(pdu.getSequenceNumber())){
								if(resetTimer(pdu)){
									aliveQueue.remove();
									sender.resendPDU(pdu,pduIdentifiers.get(pdu.getSequenceNumber()));
									pdu.setTimer();
									aliveQueue.addLast(pdu);
								}
						
								if(!pdu.resend()){
									if(pdu.getPduType() == Constants.PDU_MSG){
										contactManager.setContactOnlineStatus(pduIdentifiers.get(((Msg)pdu).getSequenceNumber()),false);
										chatManager.removeChatsWhereContactIsIn(pduIdentifiers.get(((Msg)pdu).getSequenceNumber()));
									}
									if(pdu.getPduType() == Constants.PDU_CHAT_REQUEST){
										chatManager.removeChatsWhereContactIsIn(pduIdentifiers.get(((Msg)pdu).getSequenceNumber()));
									}
									removePDU(pdu.getSequenceNumber());
								}
							} else {
								aliveQueue.remove();
							}
						}
						pdu = aliveQueue.peek();
						if(null == pdu){
							break;
						}
						timeToDie = pdu.getAliveTime();
					}
					}
			} catch (InterruptedException e) {
				//thread stopped
			}
		}
		
	}
	
	public void stopThread(){
		keepRunning = false;
		this.interrupt();
	}
	
	/**
	 * Method used whenever a message in the timer has received an corresponding ACK message
	 * @param sequenceNumber is the message to be removed from the timer
	 * @return returns true if the pdu successfully where removed as defined by the remove method of a HashSet
	 */
	public boolean removePDU(int sequenceNumber){
		synchronized (pduIdentifiers) {
			Integer pdu = pduIdentifiers.get(sequenceNumber);
			if (pdu != null){
				pduIdentifiers.remove(sequenceNumber);
				return true;
			}
		}
		 return false;
	}
	
	public void removeAllPDUForContact(int contactID){
		synchronized (pduIdentifiers) {
			pduIdentifiers.values().remove(contactID);
			/*
			HashMap<Integer, Integer> coppy = pduIdentifiers;
			for(int id: coppy.keySet()){
				if(pduIdentifiers.get(id) == contactID){
					pduIdentifiers.remove(id);
					
				}
			}*/
			int ii = 0;
			ii++;
		}
	}
	
	private boolean resendChatReq(ChatRequest chatReq){
		if(chatManager.chatExists(chatReq.getChatID())){
			for(int id: chatReq.getFriends().keySet()){
				 if(!contactManager.isContactOnline(id)){
					 return false;
				 }	
			}
			return true;
		}
		return false;
	}
	
	private boolean resendMsg(Msg msg){
		if(contactManager.isContactOnline(pduIdentifiers.get(msg.getSequenceNumber()))){
			return true;
		}
		return false;
	}
	
	private boolean resetTimer(PduInterface pdu){
		
		switch (pdu.getPduType()) {
		
		case Constants.PDU_CHAT_REQUEST:
			if(!resendChatReq((ChatRequest) pdu)){
				return false;
			}
			break;
			
		case Constants.PDU_HELLO:
			return true;
			
		case Constants.PDU_MSG:
			if(!resendMsg((Msg) pdu)){
				chatManager.notifyTextNotSent((Msg)pdu);
				return false;
			}
			break;
			
		default:
			return false;
			
		}
		return true;
	}
}
