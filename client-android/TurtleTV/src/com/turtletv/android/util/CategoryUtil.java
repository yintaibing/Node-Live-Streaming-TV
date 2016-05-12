package com.turtletv.android.util;

import java.util.List;

import com.turtletv.android.R;
import com.turtletv.android.bean.Category;

public class CategoryUtil {
	public static int getCoverResId(int categoryId) {
		switch (categoryId) {
		case 2:
			return R.drawable.lol;
			
		case 3:
			return R.drawable.sc2;
			
		default:
			return R.drawable.cover_def;
		}
	}
	
	public static String[] toNameAry(List<Category> categories) {
		int size = categories.size();
		String[] names = new String[size];
		
		for (int i = 0; i < size; i++) {
			names[i] = categories.get(i).getName();
		}
		return names;
	}
}
