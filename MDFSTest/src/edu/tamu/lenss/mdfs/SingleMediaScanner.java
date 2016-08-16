package edu.tamu.lenss.mdfs;

import java.io.File;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class SingleMediaScanner implements MediaScannerConnectionClient {

	private MediaScannerConnection mMs;
	private File mFile;
	private String FILE_TYPE = "*/*"; // "image/*"
	private SingleFileSannerListener listener;

	public SingleMediaScanner(Context cont, File f, SingleFileSannerListener listener) {
		mFile = f;
		mMs = new MediaScannerConnection(cont, this);
		mMs.connect();
		this.listener = listener;
	}

	@Override
	public void onMediaScannerConnected() {
		mMs.scanFile(mFile.getAbsolutePath(), FILE_TYPE);
	}

	@Override
	public void onScanCompleted(String path, Uri uri) {
		listener.onScanCompleted(path, uri);
		mMs.disconnect();
	}
	
	public static interface SingleFileSannerListener{
		public void onScanCompleted(String path, Uri uri);
	}
}
