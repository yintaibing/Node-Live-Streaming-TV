package com.turtletv.android.manager;

import com.turtletv.android.bean.User;
import com.turtletv.android.util.StringUtil;

public class UserManager {
	private static UserManager sInstance;
	public static UserManager getInstance() {
		if (sInstance == null) {
			sInstance = new UserManager();
		}
		return sInstance;
	}
	
	private User mCurrentUser;
	private boolean mIsLogined;
	private String mRandomId;
	
	private UserManager() {
		mIsLogined = false;
	}
	
	public User getCurrentUser() {
		return mCurrentUser;
	}
	
	public void setCurrentUser(User user) {
		mCurrentUser = user;
		mIsLogined = user != null;
	}
	
	public boolean isLogined() {
		return mIsLogined;
	}
	
	public String newRandomId() {
		mRandomId = StringUtil.uuid();
		return mRandomId;
	}
	
	public String getRandomId() {
		return mRandomId;
	}
	
	public void logout() {
		mCurrentUser = null;
		mIsLogined = false;
		mRandomId = StringUtil.uuid();
	}
}
