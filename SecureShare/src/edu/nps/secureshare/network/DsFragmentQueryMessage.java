package edu.nps.secureshare.network;



public class DsFragmentQueryMessage extends SecureShareMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4064168948193861685L;
	private String mFragHash;

	public DsFragmentQueryMessage(String fragHash) {
		super(DS_FRAG_QUERY);
		mFragHash = fragHash;
	}
	
	public String getFragHash() {
		return mFragHash;
	}

}
