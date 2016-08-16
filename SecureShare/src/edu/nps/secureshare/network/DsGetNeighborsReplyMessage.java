package edu.nps.secureshare.network;



public class DsGetNeighborsReplyMessage extends SecureShareMessage {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3947364816384224532L;
	private String[] mNeighbors;

	public DsGetNeighborsReplyMessage(String[] neighbors) {
		super(DS_GET_NEIGHBORS_REPLY);
		mNeighbors = neighbors;
	}
	
	public String[] getNeighbors() {
		return mNeighbors;
	}

}
