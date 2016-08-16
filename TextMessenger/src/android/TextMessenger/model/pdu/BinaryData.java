package android.TextMessenger.model.pdu;

import adhoc.aodv.exception.BadPduFormatException;
import android.TextMessenger.model.Constants;

public class BinaryData implements PduInterface {
	private int retries = Constants.MAX_RESENDS;
	private int sequenceNumber = -1;
	private long aliveTimeLeft;
	private byte type = Constants.PDU_MDFS_FILE;
	private byte[] myData;
	private String fileName;
	
	public BinaryData(){
		
	}
	
	public BinaryData(byte[] b, String fName){
		this.myData = b;
		this.fileName = fName;
	}
	
	public String getFileName(){
		return fileName;
	}
	
	public byte[] getData(){
		return myData;
	}
	
	@Override
	public byte[] toBytes() {
		byte[] header = (type+";"+sequenceNumber+";"+fileName+";").getBytes();
		byte[] combo = new byte[header.length+myData.length];
		System.arraycopy(header, 0, combo, 0, header.length);
		System.arraycopy(myData, 0, combo, header.length, myData.length); 		
		return combo;
	}

	@Override
	public void parseBytes(byte[] dataToParse) throws BadPduFormatException {
		String[] s = new String(dataToParse).split(";",4);
		if(s.length != 4){
			throw new BadPduFormatException(	"BinaryData: could not be split " +
												"the expected # of arguments from bytes. " +
												"Expecteded 3 args but were given "+s.length	);
		}
		try {
			sequenceNumber = Integer.parseInt(s[1]);
			fileName = s[2];
			myData = s[3].getBytes();
		} catch (NumberFormatException e) {
			throw new BadPduFormatException("Hello: falied parsing arguments to the desired types");
		}
	}

	@Override
	public long getAliveTime() {
		return aliveTimeLeft;
	}

	@Override
	public void setTimer() {
		aliveTimeLeft = 60000 + System.currentTimeMillis();
	}

	@Override
	public byte getPduType() {
		return type;
	}

	@Override
	public int getSequenceNumber() {
		return this.sequenceNumber ;
	}

	@Override
	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	@Override
	public boolean resend() {
		retries--;
		if(retries <= 0){
			return false;
		}
		return true;
	}

}
