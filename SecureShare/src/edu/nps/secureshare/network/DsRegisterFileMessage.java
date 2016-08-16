package edu.nps.secureshare.network;

import edu.nps.secureshare.directoryservice.DataRecord;


public class DsRegisterFileMessage extends SecureShareMessage {
	

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1869258080050420704L;
	private DataRecord mRecord;
	
	public DsRegisterFileMessage(DataRecord record) {
		super(DS_REGISTER_FILE);
		mRecord = record;
	}
	
	public DataRecord getDatarecord() {
		return mRecord;
	}

}
