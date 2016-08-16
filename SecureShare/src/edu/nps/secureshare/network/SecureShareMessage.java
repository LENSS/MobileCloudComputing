package edu.nps.secureshare.network;

import java.io.Serializable;

public abstract class SecureShareMessage implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5943757853622506363L;
	
	public static final int ACK_MSG = 0;
	public static final int FRAG_REQ = 1;
	public static final int STORE_FRAG_REQUEST = 2;
	public static final int TRANSMIT_FRAG = 3;
	public static final int DS_REGISTER_FILE = 4;
	public static final int DS_REGISTER_FRAG = 5;
	public static final int DS_LIST_FILES_QUERY = 6;
	public static final int DS_LIST_FILES_REPLY = 7;
	public static final int DS_FRAG_QUERY = 8;
	public static final int DS_FRAG_REPLY = 9;
	public static final int DS_REGISTER_NEIGHBOR = 10;
	public static final int DS_GET_NEIGHBORS_QUERY = 11;
	public static final int DS_GET_NEIGHBORS_REPLY = 12;
	public static final int DS_FRAG_HASH_NOT_FOUND = 13;
	
	private int type;
	
	public SecureShareMessage(int typeFlag) {
		super();
		type = typeFlag;
	}
	
	public int getType() {
		return type;
	}
}
