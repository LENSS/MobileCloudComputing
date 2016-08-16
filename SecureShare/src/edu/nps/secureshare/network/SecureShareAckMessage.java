package edu.nps.secureshare.network;

public class SecureShareAckMessage extends SecureShareMessage {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5035303813863128060L;

	public SecureShareAckMessage() {
		super(ACK_MSG);
	}

}
