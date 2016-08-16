package edu.nps.secureshare.mdfs;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import edu.nps.secureshare.shamir.ShamirSecret.ShareInfo;

public class FragmentContainer implements Serializable {
	public static final int DATA_TYPE = 0;
	public static final int CODING_TYPE = 1;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7279365137924269087L;
	private String _filename;
	private int _type;
	private long _filesize;
	private byte[] _fragment;
	private long _lastModifiedTS;
	private int _fragmentNumber;
	private int _kNumber;
	private int _nNumber;
	private ShareInfo _sKeyFragment;
	private String _fragmentHashString;
	private String _fileHashString;
	
	public FragmentContainer(String filename, int fragmentType, long filesize, 
			byte[] fragment, int fragmentNumber, int kNumber, int nNumber, ShareInfo sKeyFragment, 
			long lastModified) {
		super();
		_filename = filename;
		_fragment = fragment;
		_fragmentNumber = fragmentNumber;
		_kNumber = kNumber;
		_nNumber = nNumber;
		_sKeyFragment = sKeyFragment;
		_lastModifiedTS = lastModified;
		_type = fragmentType;
		_filesize = filesize;
		_fragmentHashString = _filename + _lastModifiedTS + _fragmentNumber;
		_fileHashString = _filename + _lastModifiedTS;
	}
	
	/**
	 * @return MD5 hash based on filename, creation timestamp, and fragment number.
	 * @throws NoSuchAlgorithmException
	 * 
	 * Can be used to determine if fragments from the same file are unique
	 */
	public String getFragmentHash() throws NoSuchAlgorithmException {
        return getHash(_fragmentHashString);	
	}
	
	/**
	 * @return MD5 hash based on filename and the creation timestamp
	 * @throws NoSuchAlgorithmException
	 * 
	 * Can be used to ensure fragments are from the same file.
	 */
	public String getFileHash() throws NoSuchAlgorithmException {
		return getHash(_fileHashString);
	}
	
	/**
	 * @param filename - the filename
	 * @param timestamp - the timestamp 
	 * @return the hash value of the file given the filename and timestamp
	 * @throws NoSuchAlgorithmException
	 */
	public static String generateFileHash(String filename, long timestamp) 
	throws NoSuchAlgorithmException {
		return getHash(filename + timestamp);
	}
	
	/**
	 * @param filename - the filename
	 * @param timestamp - the timestamp
	 * @param fragmentNum - the fragment number
	 * @return the hash value of the fragment given the filename, timestamp, and fragment number
	 * @throws NoSuchAlgorithmException
	 */
	public static String generateFragmentHash(String filename, long timestamp, int fragmentNum) 
	throws NoSuchAlgorithmException {
		return getHash(filename + timestamp + fragmentNum);
	}
	
	/**
	 * @param _hashString input string to perform hash on
	 * @return MD5 has based on the input string
	 * @throws NoSuchAlgorithmException
	 * 
	 * Private function that actually performs the MD5 hash for getFileHash() and
	 * getFragmentHash()
	 */
	private static String getHash(String _hashString) throws NoSuchAlgorithmException {
		MessageDigest md = null;
		md = MessageDigest.getInstance("MD5");
		md.update(_hashString.getBytes());
		byte byteData[] = md.digest();
		StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
        	sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();	
	}
	
	/**
	 * @return the k threshold value for the fragment
	 */
	public int getK() {
		return _kNumber;
	}
	
	/**
	 * @return the n value indicating total number of fragments generated
	 */
	public int getN() {
		return _nNumber;
	}
	
	/**
	 * @return the index value between 0 and n-1
	 */
	public int getFragmentNumber() {
		return _fragmentNumber;
	}
	
	/**
	 * @return the filename of the file
	 */
	public String getFilename() {
		return _filename;
	}
	
	/**
	 * @return the byte[] representation of this fragment
	 */
	public byte[] getFragment() {
		return _fragment;
	}
	
	/**
	 * @return class content of DATA_TYPE or CODING_TYPE
	 */
	public int getType() {
		return _type;
	}
	
	/**
	 * @return the file size of the encoded file
	 */
	public long getFilesize() {
		return _filesize;
	}
	
	/**
	 * @return the last modified timestamp of the file
	 */
	public long getTimestamp() {
		return _lastModifiedTS;
	}
	
	/**
	 * @return the fragment of the Shamir encoded private key for this fragment
	 */
	public ShareInfo getKeyFragment() {
		return _sKeyFragment;
	}

}
