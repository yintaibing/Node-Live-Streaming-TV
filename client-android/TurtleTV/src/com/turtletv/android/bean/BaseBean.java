package com.turtletv.android.bean;

public abstract class BaseBean {
	public static final String COL_ID = "id";
	
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
