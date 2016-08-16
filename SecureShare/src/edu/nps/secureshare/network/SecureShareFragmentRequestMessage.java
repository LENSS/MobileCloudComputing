package edu.nps.secureshare.network;


public class SecureShareFragmentRequestMessage extends SecureShareMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8062960305863431465L;
	private String mFragHash;

	
	public SecureShareFragmentRequestMessage(String fragHash) {
		super(FRAG_REQ);
		this.mFragHash = fragHash;
	}
	
	public String getFragHash() {
		return mFragHash;
	}
}
