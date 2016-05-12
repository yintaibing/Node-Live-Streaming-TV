package com.turtletv.android.manager;

import java.util.ArrayList;
import java.util.List;

import com.turtletv.android.bean.Category;

public class CategoryManager {
	private static CategoryManager sInstance;
	
	public static CategoryManager getInstance() {
		if (sInstance == null) {
			sInstance = new CategoryManager();
		}
		return sInstance;
	}
	
	private List<Category> mCategories;
	
	private CategoryManager() {
		mCategories = new ArrayList<Category>(0);
	}
	
	public void load(List<Category> categories) {
		mCategories = categories;
	}
	
	public List<Category> getCategories() {
		return mCategories;
	}
	
	public Category getCategoryById(int categoryId) {
		for (Category c : mCategories) {
			if (categoryId == c.getId()) {
				return c;
			}
		}
		return null;
	}
	
	public Category getCategoryByName(String name) {
		for (Category c : mCategories) {
			if (name.equals(c.getName())) {
				return c;
			}
		}
		return null;
	}
	
	public void clear() {
		mCategories.clear();
	}
}
