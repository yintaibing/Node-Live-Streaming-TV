package com.turtletv.android.datagram;

import java.util.HashMap;

import com.turtletv.android.util.JsonUtil;

public class Datagram extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;
	
	public Datagram() {
		super();
	}
	
	@Override
	public String toString() {
		return JsonUtil.toJson(this);
	}
}
