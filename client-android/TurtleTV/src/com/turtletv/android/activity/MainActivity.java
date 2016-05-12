package com.turtletv.android.activity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.turtletv.android.R;
import com.turtletv.android.bean.Category;
import com.turtletv.android.bean.Room;
import com.turtletv.android.bean.User;
import com.turtletv.android.datagram.Datagram;
import com.turtletv.android.datagram.DatagramBuilder;
import com.turtletv.android.datagram.OnReceiveDatagramListener;
import com.turtletv.android.manager.CategoryManager;
import com.turtletv.android.manager.RoomManager;
import com.turtletv.android.manager.UserManager;
import com.turtletv.android.net.OnConnectListener;
import com.turtletv.android.net.SocketConn;
import com.turtletv.android.util.CategoryUtil;
import com.turtletv.android.util.ConfigUtil;
import com.turtletv.android.util.Constants;
import com.turtletv.android.util.JsonUtil;
import com.turtletv.android.util.LogUtil;
import com.turtletv.android.util.StringUtil;
import com.turtletv.android.util.ToastUtil;
import com.turtletv.android.util.UriUtil;
import com.turtletv.android.widget.MainListItemHolder;
import com.turtletv.android.widget.OnListRefreshListener;
import com.turtletv.android.widget.PopupMenu;
import com.turtletv.android.widget.PullRefreshListView;

public class MainActivity extends BaseActivity implements OnClickListener {
	private static final int REQ_CODE_REG = 1;
	private static final int REQ_CODE_LOGIN = 2;
	private static final int REQ_CODE_APPLY_PUBLISH = 3;
	private static final int REQ_CODE_SETTINGS = 4;
	private static final int REQ_CODE_LIKE_CHANGED_IN_ROOM = 5;

	private static final int MSG_AUTO_LOGIN_OK = 1;
	private static final int MSG_AUTO_LOGIN_ERROR = 2;
	private static final int MSG_GOT_CATEGORIES = 3;
	private static final int MSG_GOT_ROOMS = 4;
	private static final int MSG_SWITCH_CATEGORY = 5;
	private static final int MSG_ADD_LIKE_OK = 6;
	private static final int MSG_CANCEL_LIKE_OK = 7;

	private static final String KEY_CURRENT_CATEGORY_ID = "currentCategoryId";

	private long mLastBackPressed;
	private SocketConn mSocketConn;
	private UserManager mUserManager;
	private CategoryManager mCategoryManager;
	private RoomManager mRoomManager;
	private Category mCurrentCategory;
	private List<Room> mCurrentRooms;
	private boolean mIsMyLikes = false;
	private OnConnectListener mOnConnectListener = new OnConnectListener() {
		@Override
		public void onConnected() {
			if (!StringUtil.isBlank(ConfigUtil.get(ConfigUtil.CFG_USER_JSON))
					&& !mUserManager.isLogined()) {
				autoLogin();
			}
			getCategories(null);
			getRooms(null);
		}

		@Override
		public void onDisconnected() {
			if (mPullRefreshView != null && mPullRefreshView.isPulledOut()) {
				mPullRefreshView.finishRefresh();
			}
			sendMsgToHandler(mHandler, MSG_DISCONNECTED, null);
		}
	};
	private OnReceiveDatagramListener mAutoLoginListener = new OnReceiveDatagramListener() {
		@Override
		public String getOp() {
			return Constants.OP_LOGIN;
		}

		@Override
		public Map<String, Class<?>> getParseStrategy() {
			Map<String, Class<?>> strategy = new HashMap<String, Class<?>>();
			strategy.put(Constants.KEY_JSON, User.class);
			return strategy;
		}

		@Override
		public void onReceive(Datagram res) {
			if (Constants.STATUS_OK.equals(res.get(Constants.KEY_STATUS))) {
				sendMsgToHandler(mHandler, MSG_AUTO_LOGIN_OK,
						res.get(Constants.KEY_JSON));
			} else {
				sendMsgToHandler(mHandler, MSG_AUTO_LOGIN_ERROR,
						res.get(Constants.KEY_MSG));
			}
		}
	};
	private OnReceiveDatagramListener mGetCategoriesListener = new OnReceiveDatagramListener() {
		@Override
		public String getOp() {
			return Constants.OP_GET_CATEGORY_LIST;
		}

		@Override
		public Map<String, Class<?>> getParseStrategy() {
			Map<String, Class<?>> strategy = new HashMap<String, Class<?>>(1);
			strategy.put(Constants.KEY_JSON, Category.class);
			return strategy;
		}

		@Override
		public void onReceive(Datagram res) {
			if (Constants.STATUS_OK.equals(res.get(Constants.KEY_STATUS))) {
				sendMsgToHandler(mHandler, MSG_GOT_CATEGORIES,
						res.get(Constants.KEY_JSON));
			} else {
				sendMsgToHandler(mHandler, MSG_STATUS_ERROR,
						res.get(Constants.KEY_MSG));
			}
		}
	};
	private OnReceiveDatagramListener mGetRoomsListener = new OnReceiveDatagramListener() {
		@Override
		public String getOp() {
			return Constants.OP_GET_ROOM_LIST;
		}

		@Override
		public Map<String, Class<?>> getParseStrategy() {
			Map<String, Class<?>> strategy = new HashMap<String, Class<?>>(1);
			strategy.put(Constants.KEY_JSON, Room.class);
			return strategy;
		}

		@Override
		public void onReceive(Datagram res) {
			if (Constants.STATUS_OK.equals(res.get(Constants.KEY_STATUS))) {
				sendMsgToHandler(mHandler, MSG_GOT_ROOMS,
						res.get(Constants.KEY_JSON));
			} else {
				sendMsgToHandler(mHandler, MSG_STATUS_ERROR,
						res.get(Constants.KEY_MSG));
			}
		}
	};
	private OnReceiveDatagramListener mAddLikeListener = new OnReceiveDatagramListener() {
		@Override
		public String getOp() {
			return Constants.OP_ADD_LIKE;
		}

		@Override
		public Map<String, Class<?>> getParseStrategy() {
			return null;
		}

		@Override
		public void onReceive(Datagram res) {
			sendMsgToHandler(mHandler, MSG_ADD_LIKE_OK,
					res.get(Constants.KEY_ROOM_ID));
		}
	};
	private OnReceiveDatagramListener mCancelLikeListener = new OnReceiveDatagramListener() {
		@Override
		public String getOp() {
			return Constants.OP_CANCEL_LIKE;
		}

		@Override
		public Map<String, Class<?>> getParseStrategy() {
			return null;
		}

		@Override
		public void onReceive(Datagram res) {
			sendMsgToHandler(mHandler, MSG_CANCEL_LIKE_OK,
					res.get(Constants.KEY_ROOM_ID));
		}
	};

	private OnClickListener mOnCategoryClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			sendMsgToHandler(mHandler, MSG_SWITCH_CATEGORY, v.getTag());
		}
	};

	private PopupMenu mPopupMenu;

	private TextView mTxtCurrentCategory;
	private TextView mTxtCurrentRoomNum;

	private PullRefreshListView mPullRefreshView;
	private ListView mMainList;

	private class MainListAdapter extends BaseAdapter {
		private LayoutInflater mInflater;

		public MainListAdapter(Context ctx) {
			super();
			mInflater = LayoutInflater.from(ctx);
		}

		@Override
		public int getCount() {
			return mCurrentRooms == null ? 1 : mCurrentRooms.size() + 1;
		}

		@Override
		public Object getItem(int arg0) {
			return (mCurrentRooms == null && arg0 < mCurrentRooms.size()) ? null
					: mCurrentRooms.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup root) {
			if (mCurrentRooms != null && pos < mCurrentRooms.size()) {
				// normal room item
				MainListItemHolder item;
				if (convertView == null || convertView instanceof TextView) {
					// when pos==0, convertView is a old hint textview, rebuild
					// it
					convertView = mInflater.inflate(R.layout.list_item_main,
							null);
					item = new MainListItemHolder();
					item.imgRoomCover = (ImageView) convertView
							.findViewById(R.id.imgRoomCover);
					item.txtRoomTitle = (TextView) convertView
							.findViewById(R.id.txtRoomTitle);
					item.txtPublisher = (TextView) convertView
							.findViewById(R.id.txtRoomPublisher);
					item.txtAudienceNum = (TextView) convertView
							.findViewById(R.id.txtRoomAudienceNum);
					item.txtIsLiving = (TextView) convertView
							.findViewById(R.id.txtRoomIsLiving);
					convertView.setTag(item);
				} else {
					item = (MainListItemHolder) convertView.getTag();
				}

				Room room = mCurrentRooms.get(pos);
				item.imgRoomCover.setImageResource(CategoryUtil
						.getCoverResId(room.getCategoryId()));
				item.txtRoomTitle.setText(room.getTitle());
				item.txtPublisher.setText("主播：" + room.getPublisherName());
				item.txtAudienceNum
						.setText("观众：" + room.getAudienceNum() + "人");
				if (room.getIsLiving()) {
					item.txtIsLiving.setTextColor(getResources().getColor(
							R.color.blue));
					item.txtIsLiving.setText("正在直播");
				} else {
					item.txtIsLiving.setTextColor(getResources().getColor(
							android.R.color.darker_gray));
					item.txtIsLiving.setText("主播不在");
				}
			} else {
				// the "no_more_room" hint text
				if (convertView == null || !(convertView instanceof TextView)) {
					convertView = new TextView(MainActivity.this);
				}
				TextView tv = (TextView) convertView;
				AbsListView.LayoutParams params = new AbsListView.LayoutParams(
						AbsListView.LayoutParams.MATCH_PARENT, 48);
				tv.setLayoutParams(params);
				tv.setText(R.string.no_more_room_found);
				tv.setTextSize(16.0f);
				tv.setTextColor(Color.GRAY);
				tv.setGravity(Gravity.CENTER);
			}
			return convertView;
		}
	}

	private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int pos,
				long itemId) {
			if (mCurrentRooms != null && pos < mCurrentRooms.size()) {
				enterRoom(mCurrentRooms.get(pos));
			}
		}
	};
	private OnListRefreshListener mOnRefresListener = new OnListRefreshListener() {
		@Override
		public void onRefresh() {
			if (!mSocketConn.isConnected()) {
				mSocketConn.connect(mOnConnectListener);
			} else {
				getRooms(null);
			}
		}
	};

	private static class MainActivityHandler extends Handler {
		private WeakReference<MainActivity> mActivity;

		public MainActivityHandler(MainActivity ctx) {
			mActivity = new WeakReference<MainActivity>(ctx);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			MainActivity ctx = mActivity.get();
			switch (msg.arg1) {
			case MSG_DISCONNECTED:
				if (ctx.mPullRefreshView.isPulledOut()) {
					ctx.mPullRefreshView.finishRefresh();
				}
				ToastUtil.toast(ctx, "连接服务器失败");
				break;

			case MSG_STATUS_ERROR:
				ctx.mSocketConn.removeListener(ctx.mAutoLoginListener);
				ToastUtil.toast(ctx, (String) msg.obj);
				break;

			case MSG_AUTO_LOGIN_OK:
				ctx.mSocketConn.removeListener(ctx.mAutoLoginListener);
				ctx.mUserManager.setCurrentUser((User) msg.obj);
				ConfigUtil.edit(ConfigUtil.CFG_USER_JSON,
						JsonUtil.toJson((User) msg.obj));
				ctx.logined();
				break;

			case MSG_AUTO_LOGIN_ERROR:
				ToastUtil.toast(ctx, "自动登录失败");
				break;

			case MSG_GOT_CATEGORIES:
				ctx.mCategoryManager.load((List<Category>) msg.obj);
				if (ctx.mCurrentCategory != null) {
					ctx.mCurrentCategory = ctx.mCategoryManager
							.getCategoryById(ctx.mCurrentCategory.getId());
				}
				break;

			case MSG_GOT_ROOMS:
				ctx.mRoomManager.load((List<Room>) msg.obj);
				if (ctx.mIsMyLikes) {
					ctx.mCurrentRooms = ctx.mRoomManager
							.getLikeRooms(ctx.mUserManager.getCurrentUser()
									.getLikes());
					ctx.mTxtCurrentCategory.setText("我的收藏");
				} else if (ctx.mCurrentCategory == null) {
					ctx.mCurrentRooms = ctx.mRoomManager.getAllRooms();
					ctx.mTxtCurrentCategory.setText(R.string.all_rooms);
					ctx.mMainList.setPressed(false);
					ctx.mMainList.setFocusable(false);
					ctx.mMainList.setFocusableInTouchMode(false);
				} else {
					ctx.mCurrentRooms = ctx.mRoomManager
							.getRoomsByCategory(ctx.mCurrentCategory.getId());
					ctx.mTxtCurrentCategory.setText(ctx.mCurrentCategory
							.getName());
				}
				if (ctx.mCurrentRooms == null) {
					ctx.mTxtCurrentRoomNum.setText("共0个房间");
				} else {
					ctx.mTxtCurrentRoomNum.setText("共"
							+ ctx.mCurrentRooms.size() + "个房间");
				}

				((MainListAdapter) ctx.mMainList.getAdapter())
						.notifyDataSetChanged();
				if (ctx.mPullRefreshView.isPulledOut()) {
					ctx.mPullRefreshView.finishRefresh();
				}
				break;

			case MSG_SWITCH_CATEGORY:
				ctx.mIsMyLikes = false;
				ctx.mPopupMenu.dismiss();

				ctx.mCurrentCategory = (Category) msg.obj;
				if (ctx.mCurrentCategory == null) {
					ctx.mCurrentRooms = ctx.mRoomManager.getAllRooms();
					ctx.mTxtCurrentCategory.setText(R.string.all_rooms);
				} else {
					ctx.mCurrentRooms = ctx.mRoomManager
							.getRoomsByCategory(ctx.mCurrentCategory.getId());
					ctx.mTxtCurrentCategory.setText(ctx.mCurrentCategory
							.getName());
				}

				ctx.mTxtCurrentRoomNum.setText("共" + ctx.mCurrentRooms.size()
						+ "个房间");
				((MainListAdapter) ctx.mMainList.getAdapter())
						.notifyDataSetChanged();
				break;

			case MSG_ADD_LIKE_OK:
				User user = ctx.mUserManager.getCurrentUser();
				if (user.getLikes() == null) {
					user.setLikes(new LinkedList<Integer>());
				}
				user.getLikes().add(
						Integer.valueOf(((Double) msg.obj).intValue()));
				if (ctx.mIsMyLikes) {
					ctx.mCurrentRooms = ctx.mRoomManager.getLikeRooms(user
							.getLikes());
					((MainListAdapter) ctx.mMainList.getAdapter())
							.notifyDataSetChanged();
				}
				ToastUtil.toast(ctx, "收藏成功");
				break;

			case MSG_CANCEL_LIKE_OK:
				User user1 = ctx.mUserManager.getCurrentUser();
				user1.getLikes().remove(
						Integer.valueOf(((Double) msg.obj).intValue()));
				if (ctx.mIsMyLikes) {
					ctx.mCurrentRooms = ctx.mRoomManager.getLikeRooms(user1
							.getLikes());
					((MainListAdapter) ctx.mMainList.getAdapter())
							.notifyDataSetChanged();
				}
				ToastUtil.toast(ctx, "取消收藏成功");
				break;

			default:
				break;
			}
		}
	};

	private MainActivityHandler mHandler;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_main);

		// init views
		mImgBack = (ImageView) findViewById(R.id.imgBack);
		mTxtBack = (TextView) findViewById(R.id.txtBack);
		mTxtCurrentCategory = (TextView) findViewById(R.id.txtCurrentCategory);
		mTxtCurrentRoomNum = (TextView) findViewById(R.id.txtCurrentCategoryRoomNum);
		mPullRefreshView = (PullRefreshListView) findViewById(R.id.listMainPullRefresh);
		mMainList = mPullRefreshView.getListView();
		mTxtCurrentCategory.setText(R.string.all_rooms);
		mTxtCurrentRoomNum.setText("共0个房间");

		mTxtBack.setVisibility(View.GONE);
		mImgBack.setImageResource(R.drawable.menu);
		mImgBack.setOnClickListener(this);

		mMainList.setAdapter(new MainListAdapter(this));
		mMainList.setOnItemClickListener(mOnItemClickListener);
		mMainList
				.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
					@Override
					public void onCreateContextMenu(ContextMenu menu,
							View view, ContextMenuInfo mi) {
						AdapterContextMenuInfo info = (AdapterContextMenuInfo) mi;
						if (mUserManager.isLogined() && mCurrentRooms != null
								&& info.position < mCurrentRooms.size()) {
							menu.add(0, Menu.FIRST + 1, 1, "前往直播间");
							Room room = mCurrentRooms.get(info.position);
							User user = mUserManager.getCurrentUser();
							if (user.getLikes() != null
									&& user.getLikes().contains(room.getId())) {
								menu.add(0, Menu.FIRST + 2, 2, "取消收藏此房间");
							} else {
								menu.add(0, Menu.FIRST + 3, 3, "收藏此房间");
							}
						}
					}
				});
		mPullRefreshView.setOnListRefreshListener(mOnRefresListener);

		LinearLayout menuLayout = (LinearLayout) LayoutInflater.from(this)
				.inflate(R.layout.layout_main_menu, null);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(300,
				LinearLayout.LayoutParams.MATCH_PARENT);
		menuLayout.setLayoutParams(params);
		mPopupMenu = new PopupMenu(this, menuLayout, this);

		// init handler
		mHandler = new MainActivityHandler(this);

		// init config and managers
		ConfigUtil.load(getApplicationContext());
		mUserManager = (UserManager) UserManager.getInstance();
		mCategoryManager = (CategoryManager) CategoryManager.getInstance();
		mRoomManager = (RoomManager) RoomManager.getInstance();
		String userJson = ConfigUtil.get(ConfigUtil.CFG_USER_JSON);
		if (StringUtil.isBlank(userJson)) {
			mUserManager.newRandomId();
		}

		// init member data
		if (bundle != null) {
			int categoryId = bundle.getInt(KEY_CURRENT_CATEGORY_ID, -1);
			if (categoryId > 0) {
				mCurrentCategory = mCategoryManager.getCategoryById(categoryId);
				mCurrentRooms = mRoomManager.getRoomsByCategory(categoryId);
			}
		}

		// init socket
		mSocketConn = SocketConn.getInstance();
		mSocketConn.addListener(mAutoLoginListener);
		mSocketConn.addListener(mGetCategoriesListener);
		mSocketConn.addListener(mGetRoomsListener);
		mSocketConn.addListener(mAddLikeListener);
		mSocketConn.addListener(mCancelLikeListener);
		if (!mSocketConn.isConnected()) {
			mSocketConn.connect(mOnConnectListener);
			LogUtil.log(this, "连接socket");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mPopupMenu.isShowing()) {
			mPopupMenu.dismiss();
		}
	}

	@Override
	protected void onDestroy() {
		logout();

		mSocketConn.removeListener(mAutoLoginListener);
		mSocketConn.removeListener(mAddLikeListener);
		mSocketConn.removeListener(mCancelLikeListener);
		mSocketConn.removeListener(mGetCategoriesListener);
		mSocketConn.removeListener(mGetRoomsListener);
		mSocketConn.close();

		mRoomManager.clear();
		mCategoryManager.clear();

		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		bundle.putInt(KEY_CURRENT_CATEGORY_ID, mCurrentCategory == null ? -1
				: mCurrentCategory.getId());
	}

	@Override
	protected void onActivityResult(int reqCode, int resCode, Intent data) {
		switch (reqCode) {
		case REQ_CODE_REG:
		case REQ_CODE_LOGIN:
			if (resCode == RESULT_OK) {
				logined();
			}
			break;

		case REQ_CODE_APPLY_PUBLISH:
			if (resCode == RESULT_OK) {
				((MainListAdapter) mMainList.getAdapter())
						.notifyDataSetChanged();
				Button btnMainMenuApplyPublish = (Button) mPopupMenu
						.getContentView().findViewById(
								R.id.btnMainMenuApplyPublish);
				btnMainMenuApplyPublish.setText(R.string.my_publish);
			}
			break;

		case REQ_CODE_SETTINGS:
			if (resCode == RESULT_OK) {
				mSocketConn.connect(mOnConnectListener);
			}
			break;

		case REQ_CODE_LIKE_CHANGED_IN_ROOM:
			if (resCode == RESULT_OK && mIsMyLikes) {
				mCurrentRooms = mRoomManager.getLikeRooms(mUserManager
						.getCurrentUser().getLikes());
				mTxtCurrentRoomNum.setText("共" + mCurrentRooms.size() + "个房间");
				((MainListAdapter) mMainList.getAdapter())
						.notifyDataSetChanged();
			}
			break;

		default:
			break;
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent e) {
		if (mPopupMenu.isShowing()) {
			mPopupMenu.dismiss();
			return true;
		}
		return super.dispatchTouchEvent(e);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent e) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (mPopupMenu.isShowing()) {
				mPopupMenu.dismiss();
			} else {
				mPopupMenu.slideOutFromLeft();
			}
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mPopupMenu.isShowing()) {
				mPopupMenu.dismiss();
				return true;
			}

			long now = System.currentTimeMillis();
			if (mLastBackPressed > 0L
					&& now - mLastBackPressed < FINISH_INTERVAL) {
				finish();
			} else {
				mLastBackPressed = now;
				ToastUtil.toast(this, "再次按返回键退出");
			}
			return true;
		}
		return super.onKeyDown(keyCode, e);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		Room room = mCurrentRooms.get(info.position);

		switch (item.getItemId()) {
		case Menu.FIRST + 1:
			// enter room
			enterRoom(room);
			return true;

		case Menu.FIRST + 2:
			// cancel like
			cancelLike(room);
			return true;

		case Menu.FIRST + 3:
			// add like
			addLike(room);
			return true;

		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.imgBack:
			mPopupMenu.slideOutFromLeft();
			break;

		// main menu btns
		case R.id.txtLoginOrName:
			if (mUserManager.getCurrentUser() == null) {
				// goto login
				Intent intent = new Intent(this, RegLoginActivity.class);
				intent.putExtra(Constants.KEY_OP, Constants.OP_LOGIN);
				startActivityForResult(intent, REQ_CODE_LOGIN);
			} else {
				// goto my profile

			}
			break;

		case R.id.txtRegisterOrLogout:
			if (mUserManager.getCurrentUser() == null) {
				// goto register
				Intent intent = new Intent(this, RegLoginActivity.class);
				intent.putExtra(Constants.KEY_OP, Constants.OP_REGISTER);
				startActivityForResult(intent, REQ_CODE_REG);
			} else {
				// logout
				logout();
				ConfigUtil.edit(ConfigUtil.CFG_USER_JSON, null);
				ToastUtil.toast(this, "注销成功");
			}
			break;

		case R.id.btnMainMenuAllRooms:
			sendMsgToHandler(mHandler, MSG_SWITCH_CATEGORY, null);
			break;

		case R.id.btnMainMenuCategories:
			if (mPopupMenu.isCategoriesShowing()) {
				mPopupMenu.hideCategories();
			} else {
				mPopupMenu.showCategories(mCategoryManager.getCategories(),
						mOnCategoryClickListener);
			}
			break;

		case R.id.btnMainMenuMyLikes:
			if (mUserManager.isLogined()) {
				mPopupMenu.dismiss();
				mIsMyLikes = true;
				mCurrentCategory = null;
				mCurrentRooms = mRoomManager.getLikeRooms(mUserManager
						.getCurrentUser().getLikes());
				mTxtCurrentCategory.setText(R.string.my_likes);
				mTxtCurrentRoomNum.setText("共" + mCurrentRooms.size() + "个房间");
				((MainListAdapter) mMainList.getAdapter())
						.notifyDataSetChanged();
			} else {
				ToastUtil.toast(this, "请先登录");
			}
			break;

		case R.id.btnMainMenuApplyPublish:
			if (mUserManager.isLogined()) {
				Intent intent = new Intent();
				if (mUserManager.getCurrentUser().getCanPublish()) {
					// user is already a publisher, goto my room
					intent.setClass(this, MyRoomActivity.class);
					startActivity(intent);
				} else {
					// user is not a publisher, goto apply publish
					intent.setClass(this, ApplyPublishActivity.class);
					startActivityForResult(intent, REQ_CODE_APPLY_PUBLISH);
				}
			} else {
				ToastUtil.toast(this, "请先登录");
			}
			break;

		case R.id.btnMainMenuSettings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivityForResult(intent, REQ_CODE_SETTINGS);
			break;

		case R.id.btnMainMenuExit:
			mPopupMenu.dismiss();
			finish();
			break;

		default:
			break;
		}
	}

	private void getCategories(Category conditions) {
		Datagram req = DatagramBuilder.create()
				.put(Constants.KEY_OP, Constants.OP_GET_CATEGORY_LIST)
				.put(Constants.KEY_JSON, conditions).build();
		if (!mSocketConn.send(req)) {
			sendMsgToHandler(mHandler, MSG_DISCONNECTED, null);
		}
	}

	private void getRooms(Room conditions) {
		Datagram req = DatagramBuilder.create()
				.put(Constants.KEY_OP, Constants.OP_GET_ROOM_LIST)
				.put(Constants.KEY_JSON, conditions).build();
		if (!mSocketConn.send(req)) {
			sendMsgToHandler(mHandler, MSG_DISCONNECTED, null);
		}
	}

	private void enterRoom(Room room) {
		if (room.getIsLiving()) {

			String userId = null;
			if (mUserManager.isLogined()) {
				userId = String.valueOf(mUserManager.getCurrentUser().getId());
			} else {
				userId = mUserManager.getRandomId();
			}
			String videoPath = UriUtil.makeUrl(room.getId(), userId,
					mUserManager.isLogined());
			Intent intent = new Intent(this, PlayerActivity.class);
			intent.putExtra(PlayerActivity.KEY_VIDEO_PATH, videoPath);
			intent.putExtra(PlayerActivity.KEY_ROOM_ID, room.getId());
			startActivityForResult(intent, REQ_CODE_LIKE_CHANGED_IN_ROOM);
		} else {
			ToastUtil.toast(this, "该主播还在休息，请尝试刷新列表，或选择其他房间");
		}
	}

	private void autoLogin() {
		String userJson = ConfigUtil.get(ConfigUtil.CFG_USER_JSON);
		User user = (User) JsonUtil.fromJson(userJson, User.class);

		Datagram req = DatagramBuilder.create()
				.put(Constants.KEY_OP, Constants.OP_LOGIN)
				.put(Constants.KEY_JSON, user).build();
		mSocketConn.send(req);
	}

	private void logined() {
		User user = mUserManager.getCurrentUser();

		TextView txtUserName = (TextView) mPopupMenu.getContentView()
				.findViewById(R.id.txtLoginOrName);
		TextView txtLogout = (TextView) mPopupMenu.getContentView()
				.findViewById(R.id.txtRegisterOrLogout);
		Button btnMainMenuApplyPublish = (Button) mPopupMenu.getContentView()
				.findViewById(R.id.btnMainMenuApplyPublish);
		txtUserName.setText(user.getName());
		txtLogout.setText(R.string.logout);
		if (user.getCanPublish()) {
			btnMainMenuApplyPublish.setText(R.string.my_publish);
		} else {
			btnMainMenuApplyPublish.setText(R.string.apply_publish);
		}
	}

	private void logout() {
		if (mPopupMenu.isShowing()) {
			mPopupMenu.dismiss();
		}
		TextView txtLogin = (TextView) mPopupMenu.getContentView()
				.findViewById(R.id.txtLoginOrName);
		TextView txtReg = (TextView) mPopupMenu.getContentView().findViewById(
				R.id.txtRegisterOrLogout);
		Button btnMainMenuApplyPublish = (Button) mPopupMenu.getContentView()
				.findViewById(R.id.btnMainMenuApplyPublish);
		txtLogin.setText(R.string.login);
		txtReg.setText(R.string.register);
		btnMainMenuApplyPublish.setText(R.string.apply_publish);

		Datagram req = DatagramBuilder
				.create()
				.put(Constants.KEY_OP, Constants.OP_LOGOUT)
				.put(Constants.KEY_USER_ID,
						mUserManager.isLogined() ? mUserManager
								.getCurrentUser().getId() : mUserManager
								.getRandomId()).build();
		mSocketConn.send(req);

		mUserManager.logout();
	}

	private void cancelLike(Room room) {
		Datagram req = DatagramBuilder
				.create()
				.put(Constants.KEY_OP, Constants.OP_CANCEL_LIKE)
				.put(Constants.KEY_USER_ID,
						mUserManager.getCurrentUser().getId())
				.put(Constants.KEY_ROOM_ID, room.getId()).build();
		if (!mSocketConn.send(req)) {
			sendMsgToHandler(mHandler, MSG_DISCONNECTED, null);
		}
	}

	private void addLike(Room room) {
		Datagram req = DatagramBuilder
				.create()
				.put(Constants.KEY_OP, Constants.OP_ADD_LIKE)
				.put(Constants.KEY_USER_ID,
						mUserManager.getCurrentUser().getId())
				.put(Constants.KEY_ROOM_ID, room.getId()).build();
		if (!mSocketConn.send(req)) {
			sendMsgToHandler(mHandler, MSG_DISCONNECTED, null);
		}
	}
}
