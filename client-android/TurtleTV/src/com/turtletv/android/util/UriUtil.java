package com.turtletv.android.util;

import android.net.Uri;

import com.turtletv.android.bean.Room;

public class UriUtil {
	public static boolean isUrlLocalFile(String path) {
		return getPathScheme(path) == null
				|| "file".equals(getPathScheme(path));
	}

	public static String getPathScheme(String path) {
		return Uri.parse(path).getScheme();
	}
	
	public static String makeUrl(int roomId, String userId, boolean isLogined) {
		StringBuilder sb = new StringBuilder("rtmp://");
		sb.append(ConfigUtil.get(ConfigUtil.CFG_HOST));
		sb.append(":1935/TurtleTV/");
		sb.append("r=");
		sb.append(roomId);
		sb.append("&u=");
		sb.append(userId);
		sb.append("&l=");
		sb.append(isLogined ? 1 : 0);
		return sb.toString();
	}
	
	public static String makeStreamAddr() {
		StringBuilder sb = new StringBuilder("rtmp://");
		sb.append(ConfigUtil.get(ConfigUtil.CFG_HOST));
		sb.append("/TurtleTV");
		return sb.toString();
	}
	
	public static String makeStreamName(Room room) {
		StringBuilder sb = new StringBuilder();
		sb.append("r=");
		sb.append(room.getId());
		sb.append("&u=");
		sb.append(room.getPublisherId());
		return sb.toString();
	}
}
