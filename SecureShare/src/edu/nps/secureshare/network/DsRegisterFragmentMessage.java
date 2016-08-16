package edu.nps.secureshare.network;



public class DsRegisterFragmentMessage extends SecureShareMessage {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7071528233578746596L;
	private String mFragHash;
	private String mAddress;
	
	public DsRegisterFragmentMessage(String fragHash, String address) {
		super(DS_REGISTER_FRAG);
		mFragHash = fragHash;
		mAddress = address;
	}
	
	public String getFragHash() {
		return mFragHash;
	}
	
	public String getAddress() {
		return mAddress;
	}

}
