package com.turtletv.android.activity;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class BaseActivity extends Activity {
	protected static final long FINISH_INTERVAL = 2000L;
	protected static final int MSG_DISCONNECTED = -1;
	protected static final int MSG_STATUS_ERROR = -2;
	
	private static Map<String, BaseActivity> sActivitys = new HashMap<String, BaseActivity>();

	protected ImageView mImgBack;
	protected TextView mTxtBack;

	public static BaseActivity getActivity(Class<?> clazz) {
		return sActivitys.get(clazz.getSimpleName());
	}

	public static void closeActivity(Class<?> clazz) {
		String key = clazz.getSimpleName();
		BaseActivity activity = sActivitys.get(key);
		if (activity != null && !activity.isFinishing()) {
			activity.finish();
		}
	}

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		saveThis();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		sActivitys.remove(this);
	}
	
	protected void sendMsgToHandler(Handler handler, int arg1, Object extra) {
		sendMsgToHandler(handler, arg1, extra, 0L);
	}

	protected void sendMsgToHandler(Handler handler, int arg1, Object extra,
			long delay) {
		Message msg = handler.obtainMessage();
		msg.arg1 = arg1;
		msg.obj = extra;
		if (delay == 0L) {
			msg.sendToTarget();
		} else {
			handler.sendMessageDelayed(msg, delay);
		}
	}

	private void saveThis() {
		String key = this.getClass().getSimpleName();
		sActivitys.put(key, this);
	}
}
