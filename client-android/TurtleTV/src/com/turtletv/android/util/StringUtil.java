package com.turtletv.android.util;

import java.util.UUID;

public class StringUtil {
	public static String BLANK = "";
	
	public static boolean isBlank(String s) {
		return s == null || BLANK.equals(s.trim());
	}
	
	public static String uuid() {
		return UUID.randomUUID().toString();
	}
}
