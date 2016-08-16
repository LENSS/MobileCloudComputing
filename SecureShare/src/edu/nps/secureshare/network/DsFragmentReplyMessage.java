package edu.nps.secureshare.network;



public class DsFragmentReplyMessage extends SecureShareMessage {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5396221554036515064L;
	private String mAddress;

	public DsFragmentReplyMessage(String address) {
		super(DS_FRAG_REPLY);
		mAddress = address;
	}
	
	public String getAddress() {
		return mAddress;
	}

}
