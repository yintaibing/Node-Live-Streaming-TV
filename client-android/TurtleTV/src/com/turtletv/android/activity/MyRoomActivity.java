package com.turtletv.android.activity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.turtletv.android.R;
import com.turtletv.android.bean.Room;
import com.turtletv.android.manager.RoomManager;
import com.turtletv.android.manager.UserManager;
import com.turtletv.android.util.UriUtil;

public class MyRoomActivity extends BaseActivity implements OnClickListener {
	private TextView mTxtStreamAddr;
	private TextView mTxtStreamName;

	private UserManager mUserManager;
	private RoomManager mRoomManager;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_my_room);

		LinearLayout layoutBack = (LinearLayout) findViewById(R.id.layoutBack);
		TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
		mTxtStreamAddr = (TextView) findViewById(R.id.txtStreamAddress);
		mTxtStreamName = (TextView) findViewById(R.id.txtStreamName);

		layoutBack.setOnClickListener(this);
		
		mUserManager = UserManager.getInstance();
		mRoomManager = RoomManager.getInstance();

		txtTitle.setText(R.string.my_publish);
		Room myRoom = mRoomManager.getRoomByPublisher(mUserManager
				.getCurrentUser().getId());
		mTxtStreamAddr.setText(UriUtil.makeStreamAddr());
		mTxtStreamName.setText(UriUtil.makeStreamName(myRoom));
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.layoutBack:
			finish();
			break;
			
		default:
		}
	}
}
