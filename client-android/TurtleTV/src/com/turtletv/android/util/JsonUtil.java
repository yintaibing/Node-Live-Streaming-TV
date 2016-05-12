package com.turtletv.android.util;

import com.google.gson.Gson;

public class JsonUtil {
	private static Gson sGson = new Gson();
	
	public static String toJson(Object obj) {
		return sGson.toJson(obj);
	}
	
	public static Object fromJson(String json, Class<?> clazz) {
		return sGson.fromJson(json, clazz);
	}
}
