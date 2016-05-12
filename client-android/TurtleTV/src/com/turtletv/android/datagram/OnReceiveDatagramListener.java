package com.turtletv.android.datagram;

import java.util.Map;

public interface OnReceiveDatagramListener {
	public String getOp();
	
	public Map<String, Class<?>> getParseStrategy();
	
	public void onReceive(Datagram res);
}
