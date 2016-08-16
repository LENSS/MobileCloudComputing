package android.TextMessenger.exceptions;

public class ContactOfflineException extends Exception{

	private static final long serialVersionUID = 1L;
	
	public ContactOfflineException() {
	}
	
	public ContactOfflineException(String message) {
		super(message);
	}
}
