package com.turtletv.android.widget;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.turtletv.android.R;

public class PullRefreshListView extends LinearLayout implements
		OnTouchListener {
	private class RefreshTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			int topMargin = mHeaderMargin.topMargin;
			while (true) {
				topMargin += SPEED_SCROLL_BACK;
				if (topMargin <= 0) {
					topMargin = 0;
					break;
				}
				publishProgress(topMargin);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			mStatus = STATUS_REFRESHING;
			publishProgress(0);
			if (mOnRefreshListener != null) {
				mOnRefreshListener.onRefresh();
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			updateHeader();
			mHeaderMargin.topMargin = topMargin[0];
			mHeader.setLayoutParams(mHeaderMargin);
		}
	}

	private class HideHeaderTask extends AsyncTask<Void, Integer, Integer> {
		@Override
		protected Integer doInBackground(Void... params) {
			int topMargin = mHeaderMargin.topMargin;
			while (true) {
				topMargin += SPEED_SCROLL_BACK;
				if (topMargin <= -mHeaderHeight) {
					topMargin = -mHeaderHeight;
					break;
				}
				publishProgress(topMargin);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return topMargin;
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			mHeaderMargin.topMargin = topMargin[0];
			mHeader.setLayoutParams(mHeaderMargin);
		}

		@Override
		protected void onPostExecute(Integer topMargin) {
			mHeaderMargin.topMargin = topMargin;
			mHeader.setLayoutParams(mHeaderMargin);
			mListView.setLongClickable(true);
			mListView.setFocusable(true);
			mListView.setFocusableInTouchMode(true);
			mStatus = STATUS_REFRESH_FINISHED;
		}
	}

	private static final int STATUS_PULLING = 0; // 下拉
	private static final int STATUS_RELEASE_TO_REFRESH = 1; // 释放立即刷新
	private static final int STATUS_REFRESHING = 2; // 正在刷新
	private static final int STATUS_REFRESH_FINISHED = 3; // 刷新完成

	private static final int SPEED_SCROLL_BACK = -20; // 头部回滚速度

	private View mHeader; // 下拉头
	private ImageView mImgArrow; // 箭头图片
	private ProgressBar mProgressBar; // 刷新进度条
	private TextView mTxtStatus;
	private MarginLayoutParams mHeaderMargin; // 下拉头布局参数
	private int mHeaderHeight; // 下拉头高度
	private ListView mListView; // 列表
	private int mStatus = STATUS_REFRESH_FINISHED; // 当前状态
	private int mLastStatus = mStatus; // 上一次状态
	private float mYDown; // 手指按下时的纵坐标
	private int mTouchSlop; // 在被判断为滚动之前手指可以移动的最大距离
	private boolean mIsLoaded; // 用于判断仅加载一次
	private boolean mPullable; // 仅在滚动到列表顶部，才可以下拉
	private OnListRefreshListener mOnRefreshListener;

	public PullRefreshListView(Context ctx, AttributeSet attrSet) {
		super(ctx, attrSet);

		mHeader = LayoutInflater.from(ctx).inflate(
				R.layout.layout_pullrefreshlist_head, null, true);
		mImgArrow = (ImageView) mHeader.findViewById(R.id.imgArrow);
		mProgressBar = (ProgressBar) mHeader
				.findViewById(R.id.progressBarRefresh);
		mTxtStatus = (TextView) mHeader.findViewById(R.id.txtRefreshStatus);
		mTouchSlop = ViewConfiguration.get(ctx).getScaledTouchSlop();

		setOrientation(VERTICAL);
		addView(mHeader, 0);
	}
	
	public void finishRefresh() {
		mStatus = STATUS_REFRESH_FINISHED;
		new HideHeaderTask().execute();
	}
	
	public void setOnListRefreshListener(OnListRefreshListener l) {
		mOnRefreshListener = l;
	}
	
	// 下拉头是否被拉出来了
	public boolean isPulledOut() {
		return mStatus != STATUS_REFRESH_FINISHED;
	}
	
	public ListView getListView() {
		if (mListView == null) {
			return (ListView) getChildAt(1);
		}
		return mListView;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);

		if (changed && !mIsLoaded) {
			mHeaderHeight = mHeader.getHeight();
			mHeaderMargin = (MarginLayoutParams) mHeader.getLayoutParams();
			mHeaderMargin.topMargin = -mHeaderHeight;
			mListView = (ListView) getChildAt(1);
			mListView.setOnTouchListener(this);
			mIsLoaded = true;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent e) {
		setPullable(e);

		if (mPullable) {
			switch (e.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mYDown = e.getRawY();
				break;

			case MotionEvent.ACTION_MOVE:
				float yMove = e.getRawY();
				int distance = (int) (yMove - mYDown);
				// 若手指是下滑，且下拉头完全隐藏，则屏蔽下拉事件
				if (distance <= 0 && mHeaderMargin.topMargin <= -mHeaderHeight) {
					return false;
				}
				if (distance <= mTouchSlop) {
					return false;
				}
				if (mStatus != STATUS_REFRESHING) {
					if (mHeaderMargin.topMargin > 0) {
						mStatus = STATUS_RELEASE_TO_REFRESH;
					} else {
						mStatus = STATUS_PULLING;
					}
					// 改变下拉头的topMargin值来显示下拉效果
					mHeaderMargin.topMargin = (distance / 2) - mHeaderHeight;
					mHeader.setLayoutParams(mHeaderMargin);
				}
				break;

			case MotionEvent.ACTION_UP:
			default:
				if (mStatus == STATUS_RELEASE_TO_REFRESH) {
					// 松手时若是“释放立即刷新”状态，则触发刷新
					new RefreshTask().execute();
				} else if (mStatus == STATUS_PULLING) {
					// 松手时若依然是“继续下拉刷新”状态，则隐藏下拉头
					new HideHeaderTask().execute();
				}
				break;
			}

			// 最后更新下拉头中的信息
			if (mStatus == STATUS_PULLING
					|| mStatus == STATUS_RELEASE_TO_REFRESH) {
				updateHeader();
				// 取消ListView的焦点
				mListView.setPressed(false);
				mListView.setLongClickable(false);
				mListView.setFocusable(false);
				mListView.setFocusableInTouchMode(false);
				mLastStatus = mStatus;
				return true;
			}
		}

		return false;
	}

	// 根据ListView的状态来设定mPullable的值
	private void setPullable(MotionEvent e) {
		View firstChild = mListView.getChildAt(0);
		if (firstChild != null) {
			int firstVisiblePos = mListView.getFirstVisiblePosition();
			if (firstVisiblePos == 0 && firstChild.getTop() == 0) {
				// 若ListView第一个元素的上边界距离父布局的距离为0，则说明ListView滚动到了顶部
				if (!mPullable) {
					mYDown = e.getRawY();
				}
				mPullable = true;
			} else {
				if (mHeaderMargin.topMargin != -mHeaderHeight) {
					mHeaderMargin.topMargin = -mHeaderHeight;
					mHeader.setLayoutParams(mHeaderMargin);
				}
				mPullable = false;
			}
		} else {
			mPullable = false;
		}
	}

	// 更新下拉头中的信息
	private void updateHeader() {
		if (mStatus != mLastStatus) {
			if (mStatus == STATUS_PULLING) {
				mTxtStatus.setText("继续下拉刷新");
				mImgArrow.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.GONE);
				rotateArrow();
			} else if (mStatus == STATUS_RELEASE_TO_REFRESH) {
				mTxtStatus.setText("释放立即刷新");
				mImgArrow.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.GONE);
				rotateArrow();
			} else if (mStatus == STATUS_REFRESHING) {
				mTxtStatus.setText("正在刷新...");
				mImgArrow.clearAnimation();
				mImgArrow.setVisibility(View.GONE);
				mProgressBar.setVisibility(View.VISIBLE);
			}
		}
	}

	// 根据状态来旋转箭头
	private void rotateArrow() {
		float pivotX = mImgArrow.getWidth() / 2f;
		float pivotY = mImgArrow.getHeight() / 2f;
		float fromDegrees = 0f;
		float toDegrees = 0f;

		if (mStatus == STATUS_PULLING) {
			fromDegrees = 180f;
			toDegrees = 360f;
		} else if (mStatus == STATUS_RELEASE_TO_REFRESH) {
			fromDegrees = 0f;
			toDegrees = 180f;
		}

		RotateAnimation rotate = new RotateAnimation(fromDegrees, toDegrees,
				pivotX, pivotY);
		rotate.setDuration(100);
		rotate.setFillAfter(true);
		mImgArrow.startAnimation(rotate);
	}
}
