package com.turtletv.android.activity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.turtletv.android.R;
import com.turtletv.android.bean.Category;
import com.turtletv.android.bean.Room;
import com.turtletv.android.datagram.Datagram;
import com.turtletv.android.datagram.DatagramBuilder;
import com.turtletv.android.datagram.OnReceiveDatagramListener;
import com.turtletv.android.manager.CategoryManager;
import com.turtletv.android.manager.RoomManager;
import com.turtletv.android.manager.UserManager;
import com.turtletv.android.net.SocketConn;
import com.turtletv.android.util.CategoryUtil;
import com.turtletv.android.util.ConfigUtil;
import com.turtletv.android.util.Constants;
import com.turtletv.android.util.JsonUtil;
import com.turtletv.android.util.StringUtil;
import com.turtletv.android.util.ToastUtil;

public class ApplyPublishActivity extends BaseActivity implements
		OnClickListener {
	private static final int MSG_APPLY_PUBLISH_OK = 1;
	private static final int MSG_APPLY_PUBLISH_ERROR = 2;

	private OnReceiveDatagramListener mApplyPublishListener = new OnReceiveDatagramListener() {
		@Override
		public String getOp() {
			return Constants.OP_APPLY_PUBLISH;
		}

		@Override
		public Map<String, Class<?>> getParseStrategy() {
			Map<String, Class<?>> strategy = new HashMap<String, Class<?>>();
			strategy.put(Constants.KEY_JSON, Room.class);
			return strategy;
		}

		@Override
		public void onReceive(Datagram res) {
			if (Constants.STATUS_OK.equals(res.get(Constants.KEY_STATUS))) {
				sendMsgToHandler(mHandler, MSG_APPLY_PUBLISH_OK,
						res.get(Constants.KEY_JSON));
			} else {
				sendMsgToHandler(mHandler, MSG_APPLY_PUBLISH_ERROR,
						res.get(Constants.KEY_MSG));
			}
		}
	};

	private static class ApplyPublishActivityHandler extends Handler {
		private WeakReference<ApplyPublishActivity> mActivity;

		public ApplyPublishActivityHandler(ApplyPublishActivity ctx) {
			super();
			mActivity = new WeakReference<ApplyPublishActivity>(ctx);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			ApplyPublishActivity ctx = mActivity.get();
			switch (msg.arg1) {
			case MSG_DISCONNECTED:
				ToastUtil.toast(ctx, "连接服务器失败");
				break;

			case MSG_APPLY_PUBLISH_OK:
				ctx.mUserManager.getCurrentUser().setCanPublish(true);
				ConfigUtil.edit(ConfigUtil.CFG_USER_JSON,
						JsonUtil.toJson(ctx.mUserManager.getCurrentUser()));
				ctx.mRoomManager.add((Room) msg.obj);
				ToastUtil.toast(ctx, "恭喜你成为一名主播！");
				ctx.setResult(RESULT_OK);
				ctx.finish();
				break;

			case MSG_APPLY_PUBLISH_ERROR:
				ToastUtil.toast(ctx, (String) msg.obj);
				break;

			default:
				break;
			}
		}
	}

	private EditText mEdtRoomTitle;
	private TextView mTxtCategory;
	private Button mBtnChooseCategory;
	private Button mBtnApply;
	private AlertDialog mChoosedCategoryDialog;

	private ApplyPublishActivityHandler mHandler;
	private SocketConn mSocketConn;
	private UserManager mUserManager;
	private CategoryManager mCategoryManager;
	private RoomManager mRoomManager;
	private int mChoosedCategoryIndex = -1;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_apply_publish);

		LinearLayout layoutBack = (LinearLayout) findViewById(R.id.layoutBack);
		TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
		mEdtRoomTitle = (EditText) findViewById(R.id.edtApplyPublishRoomTitle);
		mTxtCategory = (TextView) findViewById(R.id.txtApplyPublishCategory);
		mBtnChooseCategory = (Button) findViewById(R.id.btnApplyPublishChooseCategory);
		mBtnApply = (Button) findViewById(R.id.btnApplyPublish);

		txtTitle.setText(R.string.apply_publish);
		mTxtCategory.setText("选择房间分类：");
		layoutBack.setOnClickListener(this);
		mBtnChooseCategory.setOnClickListener(this);
		mBtnApply.setOnClickListener(this);

		mHandler = new ApplyPublishActivityHandler(this);

		mSocketConn = SocketConn.getInstance();
		mUserManager = UserManager.getInstance();
		mCategoryManager = CategoryManager.getInstance();
		mRoomManager = RoomManager.getInstance();
		mSocketConn.addListener(mApplyPublishListener);
	}

	@Override
	protected void onDestroy() {
		mSocketConn.removeListener(mApplyPublishListener);

		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		canceled();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.layoutBack:
			canceled();
			break;

		case R.id.btnApplyPublishChooseCategory:
			showCategoryMenu();
			break;

		case R.id.btnApplyPublish:
			applyPublish();
			break;

		default:
		}
	}

	private void showCategoryMenu() {
		if (mChoosedCategoryDialog != null) {
			mChoosedCategoryDialog.show();
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		DialogInterface.OnClickListener onClick = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which >= 0) {
					// if the list items has been clicked, which >= 0
					mChoosedCategoryIndex = which;
				} else if (which == DialogInterface.BUTTON_POSITIVE) {
					dialog.dismiss();
					categoryChoosed();
				} else if (which == DialogInterface.BUTTON_NEGATIVE) {
					dialog.dismiss();
				}
			}
		};
		mChoosedCategoryDialog = builder
				.setTitle(R.string.choose_category)
				.setSingleChoiceItems(
						CategoryUtil
								.toNameAry(mCategoryManager.getCategories()),
						mChoosedCategoryIndex, onClick)
				.setPositiveButton(R.string.confirm, onClick)
				.setNegativeButton(R.string.cancel, onClick)
				.setCancelable(false).create();
		mChoosedCategoryDialog.show();
	}

	private void categoryChoosed() {
		Category c = mCategoryManager.getCategories()
				.get(mChoosedCategoryIndex);
		mTxtCategory.setText("已选择房间分类：" + c.getName());
	}

	private void applyPublish() {
		String roomTitle = mEdtRoomTitle.getText().toString();
		if (StringUtil.isBlank(roomTitle)) {
			ToastUtil.toast(this, "房间标题不能为空");
			return;
		}

		if (mChoosedCategoryIndex < 0) {
			ToastUtil.toast(this, "请选择一个房间分类");
			return;
		}

		Datagram req = DatagramBuilder
				.create()
				.put(Constants.KEY_OP, Constants.OP_APPLY_PUBLISH)
				.put(Constants.KEY_ROOM_TITLE, roomTitle)
				.put(Constants.KEY_USER_ID,
						mUserManager.getCurrentUser().getId())
				.put(Constants.KEY_CATEGORY_ID,
						mCategoryManager.getCategories()
								.get(mChoosedCategoryIndex).getId()).build();
		if (!mSocketConn.send(req)) {
			sendMsgToHandler(mHandler, MSG_DISCONNECTED, null);
		}
	}

	private void canceled() {
		setResult(RESULT_CANCELED);
		finish();
	}
}
