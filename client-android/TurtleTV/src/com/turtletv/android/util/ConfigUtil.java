package com.turtletv.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class ConfigUtil {
	private static final String CFG_FILE = "cfg";
	private static final String DEF_HOST = "192.168.1.104";
	private static final String DEF_SOCKET_PORT = "19350";
	public static final String CFG_HOST = "host";
	public static final String CFG_SOCKET_PORT = "socket_port";
	public static final String CFG_USER_JSON = "userJson";
	
	private static SharedPreferences sConfig;
	private static boolean sIsLoaded = false;
	
	public static void load(Context ctx) {
		sConfig = ctx.getSharedPreferences(CFG_FILE, Context.MODE_PRIVATE);
		if (sConfig.getString(CFG_HOST, null) == null) {
			edit(CFG_HOST, DEF_HOST);
		}
		if (sConfig.getString(CFG_SOCKET_PORT, null) == null) {
			edit(CFG_SOCKET_PORT, DEF_SOCKET_PORT);
		}
		sIsLoaded = true;
	}
	
	public static boolean isLoaded() {
		return sIsLoaded;
	}
	
	public static String get(String key) {
		return sConfig.getString(key, null);
	}
	
	public static void edit(String key, String val) {
		Editor editor = sConfig.edit();
		editor.putString(key, val);
		editor.commit();
	}
}
