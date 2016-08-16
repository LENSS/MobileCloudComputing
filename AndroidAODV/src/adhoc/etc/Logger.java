package adhoc.etc;

import android.util.Log;

public final class Logger {

	public static final void d(String tag, String msg) {
		Log.d(tag, msg);
	}

	public static final void i(String tag, String msg) {
		Log.i(tag, msg);
	}

	public static final void e(String tag, String msg) {
		Log.e(tag, msg);
	}

	public static final void v(String tag, String msg) {
		Log.v(tag, msg);
	}

	public static final void w(String tag, String msg) {
		Log.w(tag, msg);
	}

	public static final void e(String tag, String msg, Throwable tr) {
		Log.e(tag, msg, tr);
	}
}
