package com.turtletv.android.bean;

public class Category extends BaseBean {
	public static final String COL_NAME = "name";
	public static final String COL_COVER_PATH = "coverPath";
	
	private String name;
	private String coverPath;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getCoverPath() {
		return coverPath;
	}
	
	public void setCoverPath(String coverPath) {
		this.coverPath = coverPath;
	}
}
