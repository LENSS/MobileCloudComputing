package edu.nps.secureshare.network;

import java.util.List;

import edu.nps.secureshare.directoryservice.DataRecord;



public class DsListFilesReplyMessage extends SecureShareMessage {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8264988322860036479L;
	private List<DataRecord> mList;

	public DsListFilesReplyMessage(List<DataRecord> list) {
		super(DS_LIST_FILES_REPLY);
		mList = list;
	}
	
	public List<DataRecord> getRecords() {
		return mList;
	}

}
