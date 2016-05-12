package com.turtletv.android.activity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.turtletv.android.R;
import com.turtletv.android.util.ConfigUtil;
import com.turtletv.android.util.StringUtil;
import com.turtletv.android.util.ToastUtil;

public class SettingsActivity extends BaseActivity implements OnClickListener {
	private EditText mEdtHost;
	private EditText mEdtPort;
	private TextView mTxtErr;
	private Button mBtnConfirm;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_settings);

		LinearLayout layoutBack = (LinearLayout) findViewById(R.id.layoutBack);
		TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
		mEdtHost = (EditText) findViewById(R.id.edtHost);
		mEdtPort = (EditText) findViewById(R.id.edtSocketPort);
		mTxtErr = (TextView) findViewById(R.id.txtSettingsErr);
		mBtnConfirm = (Button) findViewById(R.id.btnSettingsConfirm);

		txtTitle.setText(R.string.settings);
		mEdtHost.setText(ConfigUtil.get(ConfigUtil.CFG_HOST));
		mEdtPort.setText(ConfigUtil.get(ConfigUtil.CFG_SOCKET_PORT));
		layoutBack.setOnClickListener(this);
		mBtnConfirm.setOnClickListener(this);
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

		case R.id.btnSettingsConfirm:
			confirm();
			break;

		default:
			break;
		}
	}

	private void confirm() {
		String host = mEdtHost.getText().toString();
		String port = mEdtPort.getText().toString();

		if (StringUtil.isBlank(host)) {
			mTxtErr.setText("服务器地址不能为空");
			mTxtErr.setVisibility(View.VISIBLE);
			return;
		}
		if (StringUtil.isBlank(port)) {
			mTxtErr.setText("服务器端口不能为空");
			mTxtErr.setVisibility(View.VISIBLE);
			return;
		}

		mTxtErr.setVisibility(View.GONE);
		ConfigUtil.edit(ConfigUtil.CFG_HOST, host);
		ConfigUtil.edit(ConfigUtil.CFG_SOCKET_PORT, port);
		ToastUtil.toast(this, "修改成功");
		setResult(RESULT_OK);
		finish();
	}
	
	private void canceled() {
		setResult(RESULT_CANCELED);
		finish();
	}
}
