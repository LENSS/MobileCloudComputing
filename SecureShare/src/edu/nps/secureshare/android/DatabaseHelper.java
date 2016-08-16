package edu.nps.secureshare.android;

import edu.nps.secureshare.directoryservice.DataRecord;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String DEBUG_TAG = "DSDBHelper";
	
	// Constants for Database Details
	public static final String DATABASE_NAME = "datastorage";
	public static final int    DATABASE_VERSION = 1;
	public static final String KEY_ID = "_id";
	
	// List of available files that haven't been downloaded
	public static final String FILE_TABLE_NAME = "files";
	public static final String FILE_NAME="filename";
	public static final String FILE_TIME_STAMP = "timestamp";
	public static final String FILE_KEYWORDS = "keywords";
	public static final String FILE_N = "n_value";
	public static final String FILE_K = "k_value";
	public static final String FILE_SOURCE = "source";
	public static final String FILE_EXPIRE = "expire";
	public static final String FILE_STORE_TIME = "store_time";
		
	// List of all fragments and where they are stored
	public static final String FRAGMENT_TABLE_NAME = "fragments";
	public static final String FRAGMENT_HASH = "fraghash";
	public static final String FRAGMENT_LOCATION = "location";
	public static final String FRAGMENT_STORE_TIME = "store_time";
	
	// list of downloaded files
	public static final String DOWNLOAD_TABLE_NAME = "downloads";
	public static final String DOWNLOAD_FILE_NAME="filename";
	public static final String DOWNLOAD_FILE_TIME_STAMP = "timestamp";
	public static final String DOWNLOAD_SOURCE = "source";
	public static final String DOWNLOAD_FILE_EXPIRE_TIME = "expire";
	public static final String DOWNLOAD_STORE_TIME = "store_time";
	
	// list of neighors
	public static final String NEIGHBORS_TABLE_NAME = "neighbors";
	public static final String NEIGHBORS_IP = "ip";
	public static final String NEIGHBORS_STORE_TIME = "store_time";
	
	private static final String CREATE_FILES_TABLE="create table " +
		FILE_TABLE_NAME+" ("+ 
		KEY_ID+" integer primary key autoincrement, " + 
		FILE_NAME+" text not null, " + 
		FILE_TIME_STAMP + " long not null, " + 
		FILE_N + " int not null, " +
		FILE_K + " int not null, " +
		FILE_SOURCE + " text not null, " +
		FILE_STORE_TIME + " long);";
	
	private static final String CREATE_FRAGMENTS_TABLE="create table " +
		FRAGMENT_TABLE_NAME + " ("+ 
		KEY_ID + " integer primary key autoincrement, " + 
		FRAGMENT_HASH + " text not null, " + 
		FRAGMENT_LOCATION + " text not null, " + 
		FRAGMENT_STORE_TIME + " long);";
	
	private static final String CREATE_DOWNLOADS_TABLE="create table " +
		DOWNLOAD_TABLE_NAME+" ("+ 
		KEY_ID+" integer primary key autoincrement, " + 
		DOWNLOAD_FILE_NAME+" text not null, " + 
		DOWNLOAD_FILE_TIME_STAMP + " long not null, " + 
		DOWNLOAD_FILE_EXPIRE_TIME + " long not null, " +
		DOWNLOAD_STORE_TIME + " long);";
	
	private static final String CREATE_NEIGHBORS_TABLE="create table " +
		NEIGHBORS_TABLE_NAME+" ("+ 
		KEY_ID+" integer primary key autoincrement, " + 
		NEIGHBORS_IP+" text not null, " + 
		NEIGHBORS_STORE_TIME + " long);";
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.v(DEBUG_TAG,"Creating all the tables"); 
		try {
			db.execSQL(CREATE_FILES_TABLE);
			db.execSQL(CREATE_FRAGMENTS_TABLE);
			db.execSQL(CREATE_DOWNLOADS_TABLE);
			db.execSQL(CREATE_NEIGHBORS_TABLE);
		} catch(SQLiteException ex) {
			Log.v("Create table exception", ex.getMessage());
		}
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w("TaskDBAdapter", "Upgrading from version " + oldVersion +
				" to " + newVersion +
				", which will destroy all old data");
		db.execSQL("drop table if exists "+FILE_TABLE_NAME);
		db.execSQL("drop table if exists "+FRAGMENT_TABLE_NAME);
		db.execSQL("drop table if exists "+DOWNLOAD_TABLE_NAME);
		db.execSQL("drop table if exists "+NEIGHBORS_TABLE_NAME);
		onCreate(db);
	}
	
	public void registerFile(DataRecord record) {
		ContentValues cv = new ContentValues(); 
		cv.put(FILE_NAME, record.filename()); 
		cv.put(FILE_TIME_STAMP, record.timestamp());
		cv.put(FILE_N, record.n());
		cv.put(FILE_K, record.k());
		cv.put(FILE_SOURCE, record.source());
		cv.put(FILE_STORE_TIME, java.lang.System.currentTimeMillis());
		
		getWritableDatabase().insert(FILE_TABLE_NAME, FILE_NAME, cv);
	}
	
	public void registerFragment(String fraghash, String address) {
		ContentValues cv = new ContentValues(); 
		cv.put(FRAGMENT_HASH, fraghash); 
		cv.put(FRAGMENT_LOCATION, address);
		cv.put(FRAGMENT_STORE_TIME, java.lang.System.currentTimeMillis());
		
		getWritableDatabase().insert(FRAGMENT_TABLE_NAME, FRAGMENT_HASH, cv);
	}
	
	public void registerNeighbor(String neighbor) {
		ContentValues cv = new ContentValues(); 
		cv.put(NEIGHBORS_IP, neighbor); 
		cv.put(NEIGHBORS_STORE_TIME, java.lang.System.currentTimeMillis());
		
		getWritableDatabase().insert(NEIGHBORS_TABLE_NAME, NEIGHBORS_IP, cv);
	}
	
	public Cursor getNeighbors() {
		return getReadableDatabase().query(NEIGHBORS_TABLE_NAME, null, null, null, null, null, null);
	}
	
	public void registerDownloadedFile(String filename, long timestamp) {
		ContentValues cv = new ContentValues();
		cv.put(DOWNLOAD_FILE_NAME, filename);
		cv.put(DOWNLOAD_FILE_TIME_STAMP, timestamp);
		cv.put(DOWNLOAD_STORE_TIME, java.lang.System.currentTimeMillis());
		cv.put(DOWNLOAD_FILE_EXPIRE_TIME, java.lang.System.currentTimeMillis()+(1000*60*60)); // one hour from now
		
		getWritableDatabase().insert(DOWNLOAD_TABLE_NAME, DOWNLOAD_FILE_NAME, cv);
	}
	
	
	public Cursor list() {
		return getReadableDatabase().query(FILE_TABLE_NAME, null, null, null, null, null, FILE_TIME_STAMP + " DESC");
	}
	
	public Cursor whoHasFragment(String fragHash) {
		String selection = FRAGMENT_HASH + " = \"" + fragHash + "\"";
		return getReadableDatabase().query(FRAGMENT_TABLE_NAME, null, 
				selection, null, null, null, null);
	}
	
	public Cursor listDownloadedFiles() {
		return getReadableDatabase().query(DOWNLOAD_TABLE_NAME, null, null, null, null, null, DOWNLOAD_FILE_EXPIRE_TIME + " DESC");
	}
	
	public Cursor getDownloadedFileDetails(String filename) {
		String selection = DOWNLOAD_FILE_NAME + " = \"" + filename + "\"";
		return getReadableDatabase().query(DOWNLOAD_TABLE_NAME, null, 
				selection, null, null, null, null);
	}
}
