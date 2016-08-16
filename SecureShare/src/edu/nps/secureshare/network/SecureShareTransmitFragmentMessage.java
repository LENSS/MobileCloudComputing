package edu.nps.secureshare.network;

import edu.nps.secureshare.mdfs.FragmentContainer;

public class SecureShareTransmitFragmentMessage extends SecureShareMessage {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2376286478775761863L;
	private FragmentContainer mFragment;
	
	public SecureShareTransmitFragmentMessage(FragmentContainer fragment) {
		super(TRANSMIT_FRAG);
		mFragment = fragment;
	}
	
	public FragmentContainer getFragment() {
		return mFragment;
	}
}
