package android.TextMessenger.model;

import java.util.Observable;

public class Contact extends Observable{
	private int ID;
	private volatile boolean isOnline = false;
	private String displayName;
	
	public Contact(int ID, String displayName){
	this.ID = ID;	
	//FIXME DET ER SUPER VIGTIGT AT EN displayName IKKE INDEHOLDER ";" eller "::"
	this.displayName = displayName;
	}
	
	public int getID(){
		return ID;
	}
	
	public String getDisplayName(){
		synchronized (displayName) {
			return displayName;	
		}
	}
	
	public void setIsOnline(boolean isOnline){
		//TODO notify
		this.isOnline = isOnline;
	}
	
	public boolean isOnline(){
		return isOnline;
	}

	public void setDisplayName(String displayName) {
		//TODO notify
		synchronized (this.displayName) {
			this.displayName = displayName;	
		}
	}

}
