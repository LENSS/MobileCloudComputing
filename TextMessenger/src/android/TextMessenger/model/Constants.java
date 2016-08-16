package android.TextMessenger.model;

public interface Constants {
	
	//valid sequence number interval
	public static final int MIN_VALID_SEQ_NUM = 0;
	public static final int MAX_VALID_SEQ_NUM = Integer.MAX_VALUE;
	public static final int MAX_RESENDS = 2;
	
	//the time to wait before a sent pdu is timed out
	public static final int MESSAGE_ALIVE_TIME = 5000;
	
	//pdu types
	public static final byte PDU_ACK = 1;
	public static final byte PDU_CHAT_REQUEST = 2;
	public static final byte PDU_HELLO = 3;
	public static final byte PDU_MSG = 4;
	public static final byte PDU_NO_SUCH_CHAT = 5;
	public static final byte PDU_MDFS_FILE = 6;
	
	//Time between hello to Offline
	public static final int CHECK_TIME = 2000;
	
	public static final String IP_PREFIX = "192.168.2.";
	
	public static final String DIR_PATH = "/AODVTest/";
}
