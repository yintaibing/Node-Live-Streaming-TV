package com.turtletv.android.bean;

import java.util.List;

public class User extends BaseBean {
	public static final String COL_NAME = "name";
	public static final String COL_PASSWORD = "password";
	public static final String COL_PORTRAIT = "portrait";
	public static final String COL_CAN_PUBLISH = "canPublish";
	public static final String COL_LIKES = "likes";
	
	private String name;
	private String password;
	private byte[] portrait;
	private boolean canPublish;
	private List<Integer> likes;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public byte[] getPortrait() {
		return portrait;
	}
	
	public void setPortrait(byte[] portrait) {
		this.portrait = portrait;
	}
	
	public boolean getCanPublish() {
		return canPublish;
	}
	
	public void setCanPublish(boolean canPublish) {
		this.canPublish = canPublish;
	}
	
	public List<Integer> getLikes() {
		return likes;
	}
	
	public void setLikes(List<Integer> likes) {
		this.likes = likes;
	}
}
