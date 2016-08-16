package edu.nps.secureshare.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class MDFSCipher {
	private static MDFSCipher instance = null;
	
	/**
	 * 
	 */
	private MDFSCipher() {}
	
	public static MDFSCipher getInstance() {
		if (instance == null) {
			instance = new MDFSCipher();
		}
		return instance;
	}
	
	public SecretKey generateSecretKey() {
		// Get the KeyGenerator
		KeyGenerator kgen = null;
		try {
			kgen = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    kgen.init(128);
	    
	    // Generate the secret key specs.
	    SecretKey skey = kgen.generateKey();
	    //return skey.getEncoded();
	    return skey;
	}
	
	public byte[] encrypt(byte[] plainMessage, byte[] rawSecretKey) {
		byte[] encryptedMessage = null;
		
		
		SecretKeySpec skeySpec = new SecretKeySpec(rawSecretKey, "AES");
		System.out.println("Encode keyspec: " + skeySpec);
		// Instantiate the cipher
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			encryptedMessage = cipher.doFinal(plainMessage);
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Inside the encrypt function: " + encryptedMessage.length);
		
		return encryptedMessage;
	}
	
	public byte[] decrypt(byte[] encryptedMessage, byte[] rawSecretKey) {
		byte[] plainMessage = null;
		SecretKeySpec skeySpec = new SecretKeySpec(rawSecretKey, "AES");
		System.out.println("Decode keyspec: " + skeySpec);
		
		// Instantiate the cipher
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			plainMessage = cipher.doFinal(encryptedMessage);
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return plainMessage;
	}
}
