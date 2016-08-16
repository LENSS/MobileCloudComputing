package edu.nps.secureshare.directoryservice;

import java.io.Serializable;

public class DataRecord implements Comparable<DataRecord>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4975354747795989402L;
	private String filename = null;
	private long timestamp;
	private String[] keywords = null;
	private int n;
	private int k;
	private String source = null;
	
	public DataRecord(String filename, long timestamp, int n, int k, String source) {
		this.filename = filename;
		this.timestamp = timestamp;
		this.n = n;
		this.k = k;
		this.source = source;
		keywords = null;
	}
	
	public DataRecord(String filename, long timestamp, int n, int k, String[] keywords) {
		this.filename = filename;
		this.timestamp = timestamp;
		this.n = n;
		this.k = k;
		this.keywords = keywords;
	}
	
	public String filename() {
		return filename;
	}
	
	public long timestamp() {
		return timestamp;
	}
	
	public int n() {
		return n;
	}
	
	public int k() {
		return k;
	}
	
	public String source() {
		return source;
	}
	
	public String[] keywords() {
		return keywords;
	}
	
	public int compareTo(DataRecord other) {
		return (int) ((this.timestamp) - (other.timestamp));
	}

}
