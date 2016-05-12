package com.turtletv.android.bean;

public class Room extends BaseBean {
	public static final String COL_TITLE = "title";
	public static final String COL_PUBLISHER_ID = "publisherId";
	public static final String COL_CATEGORY_ID = "categoryId";
	public static final String COL_IS_LIVING = "isLiving";
	public static final String COL_PUBLISHER_NAME = "publisherName";
	public static final String COL_AUDIENCE_NUM = "audienceNum";
	
	private String title;
	private int publisherId;
	private int categoryId;
	private boolean isLiving;
	
	private String publisherName;
	private int audienceNum;
	
	public String getPublisherName() {
		return publisherName;
	}

	public void setPublisherName(String publisherName) {
		this.publisherName = publisherName;
	}

	public int getAudienceNum() {
		return audienceNum;
	}

	public void setAudienceNum(int audienceNum) {
		this.audienceNum = audienceNum;
	}

	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public int getPublisherId() {
		return publisherId;
	}
	
	public void setPublisherId(int publisherId) {
		this.publisherId = publisherId;
	}
	
	public int getCategoryId() {
		return categoryId;
	}
	
	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}
	
	public boolean getIsLiving() {
		return isLiving;
	}
	
	public void setIsLiving(boolean isLiving) {
		this.isLiving = isLiving;
	}
}
