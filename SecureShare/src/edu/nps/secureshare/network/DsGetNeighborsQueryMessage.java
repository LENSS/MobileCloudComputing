package edu.nps.secureshare.network;



public class DsGetNeighborsQueryMessage extends SecureShareMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -89256321488773196L;
	private int mNum;

	public DsGetNeighborsQueryMessage(int n) {
		super(DS_GET_NEIGHBORS_QUERY);
		mNum = n;
	}
	
	public int getN() {
		return mNum;
	}

}
