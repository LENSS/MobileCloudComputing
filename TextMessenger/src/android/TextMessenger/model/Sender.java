package android.TextMessenger.model;

import adhoc.aodv.Node;
import android.TextMessenger.model.pdu.PduInterface;

public class Sender {
	private Node node;
	private Timer timer;
	private volatile int sequenceNumber = Constants.MIN_VALID_SEQ_NUM;
	public Sender(Node node, Timer timer){
		this.node = node;
		this.timer = timer;
	}
	
	public synchronized void sendPDU(PduInterface pdu, int destinationContactID){
		pdu.setSequenceNumber(getNextSequenceNumber());
		if(timer.setTimer(pdu,destinationContactID)){
			node.sendData(pdu.getSequenceNumber(), destinationContactID, pdu.toBytes());	// Include the Hello Header
		} else {
			//TWO MESSAGES HAVE THE SAME SEQ NUMBER!
		}
		
	}
	
	/**
	 * Method used if a pdu by some reason didn't receive an ACK message. Resends a message with the same sequence number.
	 */
	public synchronized void resendPDU(PduInterface pdu, int destinationContactID){
			node.sendData(pdu.getSequenceNumber(), destinationContactID, pdu.toBytes());
	}

	private int getNextSequenceNumber(){
		if(sequenceNumber < Constants.MAX_VALID_SEQ_NUM){
			return (sequenceNumber++);
		}
		return (sequenceNumber = Constants.MIN_VALID_SEQ_NUM);
	}
}
