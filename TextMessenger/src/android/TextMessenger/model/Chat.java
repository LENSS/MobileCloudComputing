package android.TextMessenger.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;

import android.TextMessenger.model.pdu.Msg;
import android.TextMessenger.view.ObserverConst;

public class Chat extends Observable {
	private HashMap<Integer, String> contacts;
	private ArrayList<Msg> messages;
	private boolean newMsg, active;
	private int chatID, myContactID;
	private Integer messageNum;
	private String myDisplayName;
	private ArrayList<Msg> earlyMessages = new ArrayList<Msg>();

	public Chat(HashMap<Integer, String> contacts, int chatID, int myContactID, String myDisplayName) {
		this.chatID = chatID;
		this.contacts = contacts;
		this.myContactID = myContactID;
		this.myDisplayName = myDisplayName;
		messages = new ArrayList<Msg>();
		newMsg = false;
		messageNum = 0;
		active = true;
	}

	public HashMap<Integer, String> getContacts() {
		return new HashMap<Integer, String>(contacts);
	}

	private boolean addMsgToBuffer(Msg msg) {
		// adds the message to a sorted buffer list
		for (int x = 0; x < earlyMessages.size(); x++) {
			if (earlyMessages.get(x).getContactID() == msg.getContactID()) {

				if (earlyMessages.get(x).getMssageNumber() == msg.getMssageNumber()) {
					return false;
				}

				else if (earlyMessages.get(x).getMssageNumber() > msg.getMssageNumber()) {
					earlyMessages.add(x, msg);
					return false;
				}
			}
		}
		earlyMessages.add(msg);
		return false;
	}

	public void addBufferdMsg(Msg msg) {
		int lastMessageNumber = msg.getMssageNumber();
		ArrayList<Msg> removedMSG = new ArrayList<Msg>();
		for (Msg emsg : earlyMessages) {
			if (msg.getContactID() == emsg.getContactID() && lastMessageNumber == (emsg.getMssageNumber() - 1)) {
				messages.add(emsg);
				notTextToObserver(emsg);
				removedMSG.add(emsg);
				lastMessageNumber = emsg.getMssageNumber();
			} else {
				break;
			}
		}
		for (Msg emsg : removedMSG) {
			earlyMessages.remove(emsg);
		}
	}

	public boolean addMsg(Msg msg) {
		synchronized (messages) {
			Msg iMsg;
			for (int i = messages.size(); i > 0; i--) {
				iMsg = messages.get(i - 1);
				if (iMsg.getContactID() == msg.getContactID()) {
					//
					if (iMsg.getMssageNumber() == (msg.getMssageNumber() - 1)) {
						messages.add(msg);
						newMsg = true;
						addBufferdMsg(msg);
						notTextToObserver(msg);
						return true;
					}

					else if (iMsg.getMssageNumber() < (msg.getMssageNumber() - 1)) {
						return addMsgToBuffer(msg);
					} else if (iMsg.getMssageNumber() == msg.getMssageNumber()) {
						return false;
					}
				}
			}
			if (msg.getMssageNumber() != 1) {
				return addMsgToBuffer(msg);
			}
			messages.add(msg);
			newMsg = true;
			// Adds all the msg from earlyMessages that can be added
			addBufferdMsg(msg);

		}
		// Notifying the observer
		notTextToObserver(msg);
		return true;
	}

	private void notTextToObserver(Msg msg) {
		String message;
		if (msg.getContactID() == myContactID) {
			message = (myDisplayName + " writes at " + msg.getTime() + ":\n");
		} else {
			message = (contacts.get(msg.getContactID()) + " writes at " + msg.getTime() + ":\n");
		}
		message += (msg.getText() + "\n\n");
		setChanged();
		notifyObservers(new ObjToObsever(message, ObserverConst.TEXT_RECIVED));
	}

	/**
	 * Sets the hasBeanViewed when the Chat is displayed or closed.
	 * 
	 * @param newMsg
	 */
	public void setHaveBeenViewde() {
		newMsg = false;
	}

	public boolean isTherNewMsg() {
		return newMsg;
	}

	public void getTextHistory() {
		synchronized (messages) {
			for (Msg msg : messages) {
				notTextToObserver(msg);
			}
		}

	}

	public String getDisplayname(int contactID) {
		if (contactID == myContactID) {
			return myDisplayName;
		} else {
			return contacts.get(contactID);
		}
	}

	public void disableChat() {
		active = false;
		setChanged();
		notifyObservers(new ObjToObsever(null, ObserverConst.REMOVE_CHAT));
	}

	public int getNextMessageNum() {
		synchronized (messageNum) {
			messageNum++;
			return messageNum;
		}
	}

	public int getID() {
		return chatID;
	}

	public void notifyTextNotSent(Msg msg) {
		setChanged();
		notifyObservers(new ObjToObsever(msg, ObserverConst.TEXT_NOT_SENT));
	}
}
