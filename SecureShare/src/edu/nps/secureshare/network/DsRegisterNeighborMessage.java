package edu.nps.secureshare.network;


public class DsRegisterNeighborMessage extends SecureShareMessage {
	

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7065031378744693903L;
	
	private String mNeighbor;
	
	public DsRegisterNeighborMessage(String neighbor) {
		super(DS_REGISTER_NEIGHBOR);
		mNeighbor = neighbor;
	}
	
	public String getNeighbor() {
		return mNeighbor;
	}

}
