package com.turtletv.android.activity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.BaseCacheStuffer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SimpleTextCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PlayerCode;
import com.pili.pldroid.player.widget.VideoView;
import com.turtletv.android.R;
import com.turtletv.android.bean.Room;
import com.turtletv.android.bean.User;
import com.turtletv.android.datagram.Datagram;
import com.turtletv.android.datagram.DatagramBuilder;
import com.turtletv.android.datagram.OnReceiveDatagramListener;
import com.turtletv.android.manager.RoomManager;
import com.turtletv.android.manager.UserManager;
import com.turtletv.android.net.SocketConn;
import com.turtletv.android.util.Constants;
import com.turtletv.android.util.LogUtil;
import com.turtletv.android.util.StringUtil;
import com.turtletv.android.util.ToastUtil;

public class PlayerActivity extends BaseActivity implements OnClickListener,
		IjkMediaPlayer.OnCompletionListener, IjkMediaPlayer.OnInfoListener,
		IjkMediaPlayer.OnErrorListener, IjkMediaPlayer.OnPreparedListener {
	public static final String KEY_VIDEO_PATH = "videoPath";
	public static final String KEY_ROOM_ID = "roomId";
	private static final int MSG_NEW_DANMAKU = 1;
	private static final int MSG_HIDE_TITLEBAR = 2;
	private static final int MSG_ADD_LIKE_OK = 3;
	private static final int MSG_CANCEL_LIKE_OK = 4;
	private static final int REQ_DELAY_MILLIS = 3000;

	private OnReceiveDatagramListener mOnDanmakuListener = new OnReceiveDatagramListener() {
		@Override
		public String getOp() {
			return Constants.OP_DANMAKU;
		}

		@Override
		public Map<String, Class<?>> getParseStrategy() {
			Map<String, Class<?>> strategy = new HashMap<String, Class<?>>(1);
			strategy.put(Constants.KEY_JSON,
					com.turtletv.android.bean.Danmaku.class);
			return strategy;
		}

		@Override
		public void onReceive(Datagram res) {
			if (res != null) {
				com.turtletv.android.bean.Danmaku myDanmaku = (com.turtletv.android.bean.Danmaku) res
						.get(Constants.KEY_JSON);
				if (myDanmaku != null
						&& !StringUtil.isBlank(myDanmaku.getText())) {
					BaseDanmaku biliDanmaku = convertDanmaku(myDanmaku);
					sendMsgToHandler(mHandler, MSG_NEW_DANMAKU, biliDanmaku);
				}
			}
		}

		private BaseDanmaku convertDanmaku(
				com.turtletv.android.bean.Danmaku myDanmaku) {
			BaseDanmaku biliDanmaku = mDanmakuCtx.mDanmakuFactory
					.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
			biliDanmaku.text = myDanmaku.getText();
			biliDanmaku.isLive = true;
			biliDanmaku.time = mDanmakuView.getCurrentTime() + 1200;
			biliDanmaku.textSize = 25f;
			biliDanmaku.textColor = Color.RED;
			return biliDanmaku;
		}
	};
	private OnReceiveDatagramListener mOnAddLikeListener = new OnReceiveDatagramListener() {
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

	private OnReceiveDatagramListener mOnCancelLikeListener = new OnReceiveDatagramListener() {
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

	private static class PlayActivityHandler extends Handler {
		private WeakReference<PlayerActivity> mActivity;

		public PlayActivityHandler(PlayerActivity ctx) {
			mActivity = new WeakReference<PlayerActivity>(ctx);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			PlayerActivity ctx = mActivity.get();
			switch (msg.arg1) {
			case MSG_NEW_DANMAKU:
				ctx.mDanmakuView.addDanmaku((BaseDanmaku) msg.obj);
				break;

			case MSG_HIDE_TITLEBAR:
				// auto hide titlebar if they're visible and mEdtDanmaku doesn't
				// has focus
				if (ctx.mIsTitlebarVisible && !ctx.mEdtDanmaku.hasFocus()) {
					ctx.changeTitlebarVisibility();
				}
				break;

			case MSG_ADD_LIKE_OK:
				User user = ctx.mUserManager.getCurrentUser();
				if (user.getLikes() == null) {
					user.setLikes(new LinkedList<Integer>());
				}
				user.getLikes().add(
						Integer.valueOf(((Double) msg.obj).intValue()));
				ctx.mLikeChanged = true;
				ctx.mBtnLike.setText("取消收藏");
				ToastUtil.toast(ctx, "收藏成功");
				break;

			case MSG_CANCEL_LIKE_OK:
				User user1 = ctx.mUserManager.getCurrentUser();
				user1.getLikes().remove(
						Integer.valueOf(((Double) msg.obj).intValue()));
				ctx.mLikeChanged = true;
				ctx.mBtnLike.setText("收藏");
				ToastUtil.toast(ctx, "取消收藏成功");
				break;

			default:
				break;
			}
		}
	};

	private VideoView mVideoView;
	private IDanmakuView mDanmakuView;
	private View mLayoutTitlebar;
	private View mLayoutBottom;
	private TextView mTxtLoading;
	private TextView mTxtTitle;
	private ImageButton mImgBtnLeave;
	private ImageButton mImgBtnPlayPause;
	private Button mBtnLike;
	private EditText mEdtDanmaku;
	private Button mBtnSendDanmaku;
	private CheckBox mDanmakuSwitch;

	private long mLastBackPressed;
	private Room mRoom;
	private UserManager mUserManager;
	private String mVideoPath;
	private int mReqDelay = REQ_DELAY_MILLIS;
	private boolean mLikeChanged;
	private boolean mIsCompleted;
	private boolean mIsPaused;
	private boolean mIsTitlebarVisible = false;
	private Runnable mReconnect;
	private PlayActivityHandler mHandler;
	private DanmakuContext mDanmakuCtx;

	private SocketConn mSocketConn;

	@SuppressLint("UseSparseArrays")
	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_player);

		mSocketConn = SocketConn.getInstance();
		mSocketConn.addListener(mOnDanmakuListener);
		mSocketConn.addListener(mOnAddLikeListener);
		mSocketConn.addListener(mOnCancelLikeListener);
		mUserManager = UserManager.getInstance();

		Intent intent = getIntent();
		mRoom = RoomManager.getInstance().getRoomById(
				intent.getIntExtra(KEY_ROOM_ID, -1));
		mVideoPath = intent.getStringExtra(KEY_VIDEO_PATH);

		// mAspectLayout = (AspectLayout) findViewById(R.id.aspectLayout);
		mVideoView = (VideoView) findViewById(R.id.videoView);
		mDanmakuView = (IDanmakuView) findViewById(R.id.danmakuView);
		mLayoutTitlebar = findViewById(R.id.layoutPlayerTitlebar);
		mLayoutBottom = findViewById(R.id.layoutPlayerBottom);
		mTxtLoading = (TextView) findViewById(R.id.txtPlayerLoading);
		mTxtTitle = (TextView) findViewById(R.id.txtPlayerRoomTitle);
		mImgBtnLeave = (ImageButton) findViewById(R.id.imgBtnPlayerLeaveRoom);
		mImgBtnPlayPause = (ImageButton) findViewById(R.id.imgBtnPlayerPlayPause);
		mBtnLike = (Button) findViewById(R.id.btnPlayerLike);
		mEdtDanmaku = (EditText) findViewById(R.id.edtPlayerDanmaku);
		mBtnSendDanmaku = (Button) findViewById(R.id.btnPlayerSendDanmaku);
		mDanmakuSwitch = (CheckBox) findViewById(R.id.checkBoxPlayerDanmaku);

		mTxtTitle.setText(mRoom != null ? mRoom.getTitle() : StringUtil.BLANK);
		if (mUserManager.getCurrentUser().getLikes() != null
				&& mUserManager.getCurrentUser().getLikes()
						.contains(mRoom.getId())) {
			mBtnLike.setText("取消收藏");
		} else {
			mBtnLike.setText("收藏");
		}

		mImgBtnLeave.setOnClickListener(this);
		mImgBtnPlayPause.setOnClickListener(this);
		mBtnLike.setOnClickListener(this);
		mBtnSendDanmaku.setOnClickListener(this);
		mDanmakuSwitch
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton btn,
							boolean checked) {
						mDanmakuSwitch.setText(checked ? R.string.danmaku_on
								: R.string.danmaku_off);
						if (checked) {
							mDanmakuView.show();
						} else {
							mDanmakuView.hide();
						}
					}
				});

		// init video view
		mVideoView.setMediaBufferingIndicator(mTxtLoading);
		AVOptions options = new AVOptions();
		// 1 -> enable, 0 -> disable
		options.setInteger(AVOptions.KEY_MEDIACODEC, 0);
		// the unit of buffer time is ms
		options.setInteger(AVOptions.KEY_BUFFER_TIME, 1000);
		// the unit of timeout is ms
		options.setInteger(AVOptions.KEY_GET_AV_FRAME_TIMEOUT, 10 * 1000);
		// "nobuffer"
		options.setString(AVOptions.KEY_FFLAGS, AVOptions.VALUE_FFLAGS_NOBUFFER);
		options.setInteger(AVOptions.KEY_LIVE_STREAMING, 1);
		mVideoView.setAVOptions(options);
		mVideoView.setOnErrorListener(this);
		mVideoView.setOnCompletionListener(this);
		mVideoView.setOnInfoListener(this);
		mVideoView.setOnPreparedListener(this);
		mVideoView.requestFocus();
		mVideoView.setVideoPath(mVideoPath);

		// init danmaku view
		Map<Integer, Integer> maxLines = new HashMap<Integer, Integer>();
		maxLines.put(BaseDanmaku.TYPE_SCROLL_RL, 1); // 所有弹幕单行显示
		Map<Integer, Boolean> overlappingEnable = new HashMap<Integer, Boolean>();
		overlappingEnable.put(BaseDanmaku.TYPE_SCROLL_RL, true); // 允许弹幕重叠
		mDanmakuCtx = DanmakuContext.create();
		mDanmakuCtx
				.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 2)
				.setDuplicateMergingEnabled(false)
				.setScrollSpeedFactor(1.2f)
				.setScaleTextSize(1.0f)
				.setCacheStuffer(new SimpleTextCacheStuffer(),
						new BaseCacheStuffer.Proxy() {
							@Override
							public void releaseResource(BaseDanmaku danmaku) {
							}

							@Override
							public void prepareDrawing(BaseDanmaku danmaku,
									boolean fromWorkerThread) {
							}
						}).setMaximumLines(maxLines)
				.preventOverlapping(overlappingEnable);
		mDanmakuView.setCallback(new DrawHandler.Callback() {
			@Override
			public void updateTimer(DanmakuTimer timer) {
			}

			@Override
			public void prepared() {
				mDanmakuView.start();
			}

			@Override
			public void drawingFinished() {
			}

			@Override
			public void danmakuShown(BaseDanmaku danmaku) {
			}
		});
		mDanmakuView.prepare(new BaseDanmakuParser() {
			@Override
			public Danmakus parse() {
				return new Danmakus();
			}
		}, mDanmakuCtx);
		mDanmakuView.showFPS(true);
		mDanmakuView.enableDanmakuDrawingCache(true);

		// init handler
		mHandler = new PlayActivityHandler(this);

		// send datagrams
		enterRoom();
	}

	@Override
	protected void onPause() {
		if (mVideoView != null && !mIsPaused) {
			mVideoView.pause();
			mIsPaused = true;
		}
		super.onPause();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mReqDelay = REQ_DELAY_MILLIS;
		if (mVideoView != null && mIsPaused) {
			mVideoView.start();
		}
	}

	@Override
	protected void onDestroy() {
		leaveRoom();

		mSocketConn.removeListener(mOnAddLikeListener);
		mSocketConn.removeListener(mOnCancelLikeListener);
		mSocketConn.removeListener(mOnDanmakuListener);

		if (mDanmakuView != null && mDanmakuView.isShown()) {
			mDanmakuView.stop();
		}

		if (mVideoView != null && !mIsPaused) {
			mVideoView.pause();
		}

		super.onDestroy();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent e) {
		if (e.getAction() == MotionEvent.ACTION_UP) {
			// if e.getY() is between the titlebar and bottom layout,
			// change their visibility
			int y = (int) e.getY();
			if (y > mLayoutTitlebar.getBottom() && y < mLayoutBottom.getTop()) {
				if (mIsTitlebarVisible) {
					// hide them immediately
					changeTitlebarVisibility();
				} else {
					// show them immediately and
					// hide them 3 sec later if mEdtDanmaku doesn't have focus
					changeTitlebarVisibility();
					sendMsgToHandler(mHandler, MSG_HIDE_TITLEBAR, null, 3000L);
				}
				return true;
			}
		}
		return super.dispatchTouchEvent(e);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent e) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			changeTitlebarVisibility();
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			long now = System.currentTimeMillis();
			if (mLastBackPressed > 0L
					&& now - mLastBackPressed < FINISH_INTERVAL) {
				if (mLikeChanged) {
					setResult(RESULT_OK);
				}
				finish();
			} else {
				mLastBackPressed = now;
				ToastUtil.toast(this, "再次按返回键离开此直播间");
			}
			return true;
		}
		return super.onKeyDown(keyCode, e);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.imgBtnPlayerLeaveRoom:
			if (mLikeChanged) {
				setResult(RESULT_OK);
			}
			finish();
			break;

		case R.id.imgBtnPlayerPlayPause:
			if (mIsPaused) {
				mImgBtnPlayPause.setImageResource(R.drawable.pause);
				mVideoView.start();
			} else {
				mImgBtnPlayPause.setImageResource(R.drawable.play);
				mVideoView.pause();
			}
			mIsPaused = !mIsPaused;
			break;

		case R.id.btnPlayerLike:
			if (mUserManager.getCurrentUser().getLikes() != null
					&& mUserManager.getCurrentUser().getLikes()
							.contains(mRoom.getId())) {
				cancelLike();
			} else {
				addLike();
			}
			break;

		case R.id.btnPlayerSendDanmaku:
			sendDanmaku();
			break;

		default:
			break;
		}
	}

	@Override
	public void onCompletion(IMediaPlayer player) {
		LogUtil.log(this, "onCompletion");
		mIsCompleted = true;
		showLoading(false);
		mVideoView.start();
	}

	@Override
	public boolean onError(IMediaPlayer player, int what, int extra) {
		LogUtil.log(this, "onError what=" + what + ", extra=" + extra);

		if (what == -10000) {
			if (extra == PlayerCode.EXTRA_CODE_INVALID_URI
					|| extra == PlayerCode.EXTRA_CODE_EOF) {
				showLoading(false);
				return true;
			}
			if (mIsCompleted && extra == PlayerCode.EXTRA_CODE_EMPTY_PLAYLIST) {
				LogUtil.log(this, "video reconnecing...");
				mVideoView.removeCallbacks(mReconnect);
				mReconnect = new Runnable() {
					@Override
					public void run() {
						mVideoView.setVideoPath(mVideoPath);
					}
				};
				mVideoView.postDelayed(mReconnect, mReqDelay);
				mReqDelay += 200;
			} else if (extra == PlayerCode.EXTRA_CODE_404_NOT_FOUND) {
				LogUtil.log(this, "video 404");
				showLoading(false);
			} else if (extra == PlayerCode.EXTRA_CODE_IO_ERROR) {
				// no such RTMP stream
				LogUtil.log(this, "video I/O error");
				showLoading(false);
			}
		}
		return true;
	}

	@Override
	public boolean onInfo(IMediaPlayer player, int what, int extra) {
		LogUtil.log(this, "onInfo what=" + what + ", extra=" + extra);

		switch (what) {
		case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
			LogUtil.log(this, "onInfo MEDIA_INFO_BUFFERING_START");
			showLoading(true);
			break;

		case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
			LogUtil.log(this, "onInfo MEDIA_INFO_BUFFERING_END");
			showLoading(false);
			break;

		case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
			ToastUtil.toast(this, "Audio Start");
			break;

		case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
			ToastUtil.toast(this, "Video Start");
			break;

		default:
			break;
		}
		return true;
	}

	@Override
	public void onPrepared(IMediaPlayer player) {
		LogUtil.log(this, "onPrepared");
		showLoading(false);
		mReqDelay = REQ_DELAY_MILLIS;
	}

	private void showLoading(boolean visible) {
		if (mTxtLoading != null) {
			mTxtLoading.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	private void changeTitlebarVisibility() {
		mLayoutTitlebar.setVisibility(mIsTitlebarVisible ? View.INVISIBLE
				: View.VISIBLE);
		mLayoutBottom.setVisibility(mIsTitlebarVisible ? View.INVISIBLE
				: View.VISIBLE);
		mIsTitlebarVisible = !mIsTitlebarVisible;
	}

	private void enterRoom() {
		Datagram req = DatagramBuilder.create()
				.put(Constants.KEY_OP, Constants.OP_ENTER_ROOM)
				.put(Constants.KEY_ROOM_ID, mRoom.getId()).build();
		if (mSocketConn.send(req)) {
			sendMsgToHandler(mHandler, MSG_DISCONNECTED, null);
		}
	}

	private void leaveRoom() {
		Datagram req = DatagramBuilder.create()
				.put(Constants.KEY_OP, Constants.OP_LEAVE_ROOM)
				.put(Constants.KEY_ROOM_ID, mRoom.getId()).build();
		mSocketConn.send(req);
	}

	private void sendDanmaku() {
		String text = mEdtDanmaku.getText().toString();
		if (StringUtil.isBlank(text)) {
			ToastUtil.toast(this, "不能发送空弹幕");
			return;
		}
		if (text.length() > Constants.DANMAKU_MAX_LEN) {
			ToastUtil.toast(this, "弹幕不能超过20个字");
			return;
		}

		com.turtletv.android.bean.Danmaku myDanmaku = new com.turtletv.android.bean.Danmaku();
		myDanmaku.setText(text);
		Datagram req = DatagramBuilder.create()
				.put(Constants.KEY_OP, Constants.OP_DANMAKU)
				.put(Constants.KEY_ROOM_ID, mRoom.getId())
				.put(Constants.KEY_JSON, myDanmaku).build();
		if (mSocketConn.send(req)) {
			mEdtDanmaku.setText(StringUtil.BLANK);
			changeTitlebarVisibility();
			ToastUtil.toast(this, "发射成功");
		} else {
			ToastUtil.toast(this, "发射失败，请重试");
		}
	}

	private void cancelLike() {
		Datagram req = DatagramBuilder
				.create()
				.put(Constants.KEY_OP, Constants.OP_CANCEL_LIKE)
				.put(Constants.KEY_USER_ID,
						mUserManager.getCurrentUser().getId())
				.put(Constants.KEY_ROOM_ID, mRoom.getId()).build();
		if (!mSocketConn.send(req)) {
			sendMsgToHandler(mHandler, MSG_DISCONNECTED, null);
		}
	}

	private void addLike() {
		Datagram req = DatagramBuilder
				.create()
				.put(Constants.KEY_OP, Constants.OP_ADD_LIKE)
				.put(Constants.KEY_USER_ID,
						mUserManager.getCurrentUser().getId())
				.put(Constants.KEY_ROOM_ID, mRoom.getId()).build();
		if (!mSocketConn.send(req)) {
			sendMsgToHandler(mHandler, MSG_DISCONNECTED, null);
		}
	}
}
