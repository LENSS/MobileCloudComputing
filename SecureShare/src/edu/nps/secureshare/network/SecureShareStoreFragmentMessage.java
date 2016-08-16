package edu.nps.secureshare.network;

import edu.nps.secureshare.mdfs.FragmentContainer;

public class SecureShareStoreFragmentMessage extends SecureShareMessage {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3829404967301850079L;
	private FragmentContainer fragment = null;
	
	public SecureShareStoreFragmentMessage(FragmentContainer fragment) {
		super(STORE_FRAG_REQUEST);
		this.fragment = fragment;
	}
	
	public FragmentContainer getContainer() {
		return fragment;
	}

}
