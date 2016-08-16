package android.TextMessenger.model.pdu;

import adhoc.aodv.exception.BadPduFormatException;
import android.TextMessenger.model.Constants;

public class Msg implements PduInterface {
	private int retries = Constants.MAX_RESENDS;
	private int sequenceNumber = -1, chatID, messageNumber, ownerContactID;
	private byte type = Constants.PDU_MSG;
	private String text, time;
	private boolean visible = false;
	private long aliveTimeLeft = Long.MAX_VALUE;
	
	public Msg(int ownerContactID){
		this.ownerContactID = ownerContactID;
	}
	
	public Msg(int messageNumber, int ownerContactID, int chatID, String text){
		this.messageNumber = messageNumber;
		this.ownerContactID = ownerContactID;
		this.text = text;
		this.chatID = chatID;
		//FIXME tjek at time ikke indeholder ";" eller "::" ellers kan det gå galt
		time = "17:45";
//		time = new android.text.format.Time().toString();
	}
	public void setVisible(){
		visible = true;
	}
	
	public int getChatID(){
		return chatID;
	}
	
	public int getMssageNumber(){
		return messageNumber;
	}
	
	public int getContactID(){
		return ownerContactID;
	}
	
	public String getText(){
		return text;
	}
	
	public String getTime(){
		return time;
	}

	@Override
	public void parseBytes(byte[] bytes) throws BadPduFormatException {
		String[] s = new String(bytes).split(";",6);
		if(s.length != 6){
			throw new BadPduFormatException(	"Msg: could not split " +
												"the expected # of arguments from bytes. " +
												"Expecteded 5 args but were given "+s.length	);
		}
		try {
			sequenceNumber = Integer.parseInt(s[1]);
			chatID = Integer.parseInt(s[2]);
			messageNumber =Integer.parseInt(s[3]);
			time = new String(s[4]);
			text = new String(s[5]);
		} catch (NumberFormatException e) {
			throw new BadPduFormatException("Msg: falied parsing arguments to the desired types");
		}
	}

	@Override
	public byte[] toBytes() {
		//FIXME sørg for at der ikke indeholdes ";" i nogen af følgende strenge
		return (type+";"+sequenceNumber+";"+chatID+";"+messageNumber+";"+time+";"+text).getBytes();
	}

	@Override
	public long getAliveTime() {
		return aliveTimeLeft;
	}

	@Override
	public void setTimer() {
		aliveTimeLeft = Constants.MESSAGE_ALIVE_TIME + System.currentTimeMillis();
	}
	
	@Override
	public byte getPduType() {
		return type;
	}
	
	@Override
	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
		
	}
	
	@Override
	public int getSequenceNumber() {
		return sequenceNumber;
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
