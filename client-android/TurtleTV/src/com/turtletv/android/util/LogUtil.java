package com.turtletv.android.util;

import android.util.Log;

public class LogUtil {
	private static final boolean isDebug = true;

	public static void log(Object obj, String info) {
		if (isDebug) {
			if (obj == null) {
				Log.e("TurtleTV", info);
			} else {
				Log.e(obj.getClass().getSimpleName(), info);
			}
		}
	}

	public static void log(Object obj, int lineNumber, String info) {
		log(obj, lineNumber + "行:" + info);
	}
}
