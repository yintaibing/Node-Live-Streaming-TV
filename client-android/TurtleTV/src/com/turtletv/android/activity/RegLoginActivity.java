package com.turtletv.android.activity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.turtletv.android.R;
import com.turtletv.android.bean.User;
import com.turtletv.android.datagram.Datagram;
import com.turtletv.android.datagram.DatagramBuilder;
import com.turtletv.android.datagram.OnReceiveDatagramListener;
import com.turtletv.android.manager.UserManager;
import com.turtletv.android.net.SocketConn;
import com.turtletv.android.util.ConfigUtil;
import com.turtletv.android.util.Constants;
import com.turtletv.android.util.JsonUtil;
import com.turtletv.android.util.ToastUtil;

public class RegLoginActivity extends BaseActivity implements OnClickListener {
	private static final int MSG_REG_OK = 1;
	private static final int MSG_REG_ERROR = 2;
	private static final int MSG_LOGIN_OK = 3;
	private static final int MSG_LOGIN_ERROR = 4;

	private OnReceiveDatagramListener mOnRegListener = new OnReceiveDatagramListener() {
		@Override
		public String getOp() {
			return Constants.OP_REGISTER;
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
				sendMsgToHandler(mHandler, MSG_REG_OK,
						res.get(Constants.KEY_JSON));
			} else {
				sendMsgToHandler(mHandler, MSG_REG_ERROR,
						res.get(Constants.KEY_MSG));
			}
		}
	};
	private OnReceiveDatagramListener mOnLoginListener = new OnReceiveDatagramListener() {
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
				sendMsgToHandler(mHandler, MSG_LOGIN_OK,
						res.get(Constants.KEY_JSON));
			} else {
				sendMsgToHandler(mHandler, MSG_LOGIN_ERROR,
						res.get(Constants.KEY_MSG));
			}
		}
	};

	private static class RegLoginActivityHandler extends Handler {
		private WeakReference<RegLoginActivity> mActivity;

		public RegLoginActivityHandler(RegLoginActivity ctx) {
			super();
			mActivity = new WeakReference<RegLoginActivity>(ctx);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			RegLoginActivity ctx = mActivity.get();
			switch (msg.arg1) {
			case MSG_DISCONNECTED:
			case MSG_STATUS_ERROR:
			case MSG_REG_ERROR:
			case MSG_LOGIN_ERROR:
				ctx.mTxtErr.setText((String) msg.obj);
				ctx.mTxtErr.setVisibility(View.VISIBLE);
				break;

			case MSG_REG_OK:
			case MSG_LOGIN_OK:
				User user = (User) msg.obj;
				ctx.mUserManager.setCurrentUser(user);

				ConfigUtil
						.edit(ConfigUtil.CFG_USER_JSON, JsonUtil.toJson(user));

				String toast = msg.arg1 == MSG_REG_OK ? "注册成功" : "登录成功";
				ToastUtil.toast(ctx, toast);
				ctx.setResult(RESULT_OK);
				ctx.finish();
				break;

			default:
				break;

			}
		}
	}

	private ImageButton mImgBtnPortrait;
	private TextView mTxtPortraitHint;
	private EditText mEdtName;
	private EditText mEdtPsw;
	private TextView mTxtErr;
	private Button mBtnRegLogin;

	private String mOp;
	private SocketConn mSocketConn;
	private UserManager mUserManager;
	private RegLoginActivityHandler mHandler;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_reg_login);

		View layoutBack = findViewById(R.id.layoutBack);
		mImgBtnPortrait = (ImageButton) findViewById(R.id.imgBtnRegLoginPortrait);
		mTxtPortraitHint = (TextView) findViewById(R.id.txtRegLoginPortraitHint);
		mEdtName = (EditText) findViewById(R.id.edtRegLoginName);
		mEdtPsw = (EditText) findViewById(R.id.edtRegLoginPsw);
		mTxtErr = (TextView) findViewById(R.id.txtRegLoginErr);
		mBtnRegLogin = (Button) findViewById(R.id.btnRegLogin);

		layoutBack.setOnClickListener(this);
		mImgBtnPortrait.setOnClickListener(this);
		mBtnRegLogin.setOnClickListener(this);

		mHandler = new RegLoginActivityHandler(this);

		mOp = getIntent().getStringExtra(Constants.KEY_OP);
		mSocketConn = SocketConn.getInstance();
		mUserManager = UserManager.getInstance();

		if (Constants.OP_REGISTER.equals(mOp)) {
			initReg();
		} else if (Constants.OP_LOGIN.equals(mOp)) {
			initLogin();
		}
	}

	@Override
	protected void onDestroy() {
		mSocketConn.removeListener(mOnRegListener);
		mSocketConn.removeListener(mOnLoginListener);

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

		case R.id.imgBtnRegLoginPortrait:
			if (Constants.OP_REGISTER.equals(mOp)) {
				// it's clickable only when mOp==OP_REG, set my portrait
				ToastUtil.toast(this, "选取头像功能暂未实现");
			}
			break;

		case R.id.btnRegLogin:
			regOrLogin();
			break;

		default:
			break;
		}
	}

	private void initReg() {
		TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
		txtTitle.setText(R.string.register);
		mBtnRegLogin.setText(R.string.register);

		mSocketConn.addListener(mOnRegListener);
	}

	private void initLogin() {
		TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
		txtTitle.setText(R.string.login);
		mBtnRegLogin.setText(R.string.login);
		mImgBtnPortrait.setOnClickListener(null);
		mTxtPortraitHint.setVisibility(View.GONE);

		mSocketConn.addListener(mOnLoginListener);
	}

	private void regOrLogin() {
		if (mTxtErr.getVisibility() == View.VISIBLE) {
			mTxtErr.setVisibility(View.GONE);
		}

		User user = new User();
		user.setName(mEdtName.getText().toString());
		user.setPassword(mEdtPsw.getText().toString());
		Datagram req = DatagramBuilder.create().put(Constants.KEY_OP, mOp)
				.put(Constants.KEY_JSON, user)
				.put(Constants.KEY_RANDOM_USER_ID, mUserManager.getRandomId())
				.build();
		if (!mSocketConn.send(req)) {
			sendMsgToHandler(mHandler, MSG_DISCONNECTED, null);
		}
	}

	private void canceled() {
		setResult(RESULT_CANCELED);
		finish();
	}
}
