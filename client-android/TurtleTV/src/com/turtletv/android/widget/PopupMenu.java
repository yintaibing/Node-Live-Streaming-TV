package com.turtletv.android.widget;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.turtletv.android.R;
import com.turtletv.android.bean.Category;
import com.turtletv.android.util.CategoryUtil;

public class PopupMenu extends PopupWindow {
	private static final float ALPHA_HALF = 0.5f;
	private static final float ALPHA_SOLID = 1.0f;

	private Activity mActivity;
	private LinearLayout mLayoutBtns;
	private LinearLayout mLayoutCategories;
	private List<Category> mCategories;

	public PopupMenu(Context ctx) {
		super(ctx);
	}

	public PopupMenu(Activity activity, View view, OnClickListener l) {
		super(view, view.getLayoutParams().width,
				view.getLayoutParams().height, false);
		mActivity = activity;
		setAnimationStyle(R.style.MainMenu);
		setOutsideTouchable(true);

		View contentView = getContentView();
		mLayoutBtns = (LinearLayout) contentView
				.findViewById(R.id.layoutMainMenuBtns);
		mLayoutCategories = (LinearLayout) contentView
				.findViewById(R.id.layoutMainMenuCategories);

		contentView.findViewById(R.id.txtLoginOrName).setOnClickListener(l);
		contentView.findViewById(R.id.txtRegisterOrLogout)
				.setOnClickListener(l);
		int count = mLayoutBtns.getChildCount();
		for (int i = 0; i < count; i++) {
			mLayoutBtns.getChildAt(i).setOnClickListener(l);
		}
	}

	@Override
	public void dismiss() {
		super.dismiss();
		setOutsideAlpha(ALPHA_SOLID);
	}

	public void slideOutFromLeft() {
		setOutsideAlpha(ALPHA_HALF);
		Rect rect = new Rect();
		mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
		setHeight(rect.height());
		showAtLocation(mActivity.findViewById(android.R.id.content),
				Gravity.LEFT | Gravity.TOP, 0, rect.top);
	}

	public void showCategories(List<Category> categories, OnClickListener l) {
		if (categories.equals(mCategories)) {
			mLayoutCategories.setVisibility(View.VISIBLE);
		} else {
			mCategories = categories;
			mLayoutCategories.removeAllViews();
			LayoutInflater inflater = LayoutInflater.from(mActivity);

			int size = categories.size();
			for (int i = 0; i < size; i++) {
				Category c = categories.get(i);
				LinearLayout item = (LinearLayout) inflater.inflate(
						R.layout.list_item_main_menu, null);
				ImageView imgCover = (ImageView) item
						.findViewById(R.id.imgPopupMenuListCategoryCover);
				TextView txtName = (TextView) item
						.findViewById(R.id.txtPopupMenuListCategoryName);
				imgCover.setImageResource(CategoryUtil.getCoverResId(c.getId()));
				txtName.setText(c.getName());

				item.setTag(c);
				item.setOnClickListener(l);

				mLayoutCategories.addView(item);
			}
			mLayoutCategories.setVisibility(View.VISIBLE);
		}
	}

	public void hideCategories() {
		mLayoutCategories.setVisibility(View.GONE);
	}

	public boolean isCategoriesShowing() {
		return mLayoutCategories.getVisibility() == View.VISIBLE;
	}

	private void setOutsideAlpha(float alpha) {
		WindowManager.LayoutParams params = mActivity.getWindow()
				.getAttributes();
		params.alpha = alpha;
		mActivity.getWindow().setAttributes(params);
	}
}
