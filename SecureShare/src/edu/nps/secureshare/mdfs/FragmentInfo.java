package edu.nps.secureshare.mdfs;

public class FragmentInfo {
	private String _fileName = "";
	private String _storedFilename = "";
	
	public FragmentInfo(String filename) {
		_fileName = filename;
	}
	
	/**
	 * @return the _fileName
	 */
	public String get_fileName() {
		return _fileName;
	}
	/**
	 * @param _storedFilename the _storedFilename to set
	 */
	public void set_storedFilename(String _storedFilename) {
		this._storedFilename = _storedFilename;
	}
	/**
	 * @return the _storedFilename
	 */
	public String get_storedFilename() {
		return _storedFilename;
	}
	
	public String toString() {
		return _fileName;
	}

}
