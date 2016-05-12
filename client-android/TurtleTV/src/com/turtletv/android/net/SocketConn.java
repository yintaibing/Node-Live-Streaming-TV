package com.turtletv.android.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import com.turtletv.android.datagram.Datagram;
import com.turtletv.android.datagram.DatagramBuilder;
import com.turtletv.android.datagram.OnReceiveDatagramListener;
import com.turtletv.android.util.ConfigUtil;
import com.turtletv.android.util.Constants;
import com.turtletv.android.util.JsonUtil;
import com.turtletv.android.util.LogUtil;

public class SocketConn {
	private class ConnectThread extends Thread {
		private OnConnectListener mOnConnectListener;

		public ConnectThread(OnConnectListener l) {
			mOnConnectListener = l;
		}

		@Override
		public void run() {
			try {
				LogUtil.log(this, ConfigUtil.get(ConfigUtil.CFG_HOST));
				mSocket = new Socket(ConfigUtil.get(ConfigUtil.CFG_HOST),
						Integer.parseInt(ConfigUtil
								.get(ConfigUtil.CFG_SOCKET_PORT)));
				mInput = new BufferedReader(new InputStreamReader(
						mSocket.getInputStream()));
				mOutput = new OutputStreamWriter(mSocket.getOutputStream());

				// start listen
				new ListenThread().start();

				if (mOnConnectListener != null) {
					mOnConnectListener.onConnected();
				}
			} catch (IOException e) {
				mSocket = null;

				if (mOnConnectListener != null) {
					mOnConnectListener.onDisconnected();
				}

				e.printStackTrace();
			}
		}
	}

	private class ListenThread extends Thread {
		@Override
		public void run() {
			while (mSocket != null && !mSocket.isClosed()
					&& mSocket.isConnected()) {
				try {
					// receive
					String read = mInput.readLine();
					String json = read.substring(read
							.lastIndexOf(Constants.DATAGRAM_TOKEN)
							+ Constants.DATAGRAM_TOKEN_LEN);
					LogUtil.log(this, "接收报文:" + json);
					Datagram datagram = (Datagram) JsonUtil.fromJson(json,
							Datagram.class);

					ListIterator<OnReceiveDatagramListener> it = mListeners
							.listIterator(mListeners.size());
					while (it.hasPrevious()) {
						OnReceiveDatagramListener l = it.previous();
						if (datagram.get(Constants.KEY_OP).equals(l.getOp())) {
							Map<String, Class<?>> convertStrategy = l
									.getParseStrategy();
							if (convertStrategy != null) {
								DatagramBuilder.convert(datagram,
										convertStrategy);
							}

							l.onReceive(datagram);
							break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					try {
						if (mSocket != null) {
							mSocket.close();
						}
					} catch (IOException ee) {
						ee.printStackTrace();
					}
				}
			}
		}
	}

	private static SocketConn sInstance;

	public static SocketConn getInstance() {
		if (sInstance == null) {
			sInstance = new SocketConn();
		}
		return sInstance;
	}

	private Socket mSocket;
	// private List<Datagram> mDatagramQueue;
	public LinkedList<OnReceiveDatagramListener> mListeners;
	// private List<Datagram> mDatagramQueue;
	private BufferedReader mInput;
	private OutputStreamWriter mOutput;

	private SocketConn() {
		init();
	}

	private void init() {
		mListeners = new LinkedList<OnReceiveDatagramListener>();
		// mDatagramQueue = new LinkedList<Datagram>();
	}

	public void connect(OnConnectListener l) {
		new ConnectThread(l).start();
	}

	public boolean isConnected() {
		return mSocket != null && !mSocket.isClosed() && mSocket.isConnected();
	}

	public void close() {
		mListeners.clear();
		try {
			if (mSocket != null && !mSocket.isClosed() && mSocket.isConnected()) {
				mSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean send(Datagram req) {
		try {
			if (mSocket != null && !mSocket.isClosed() && mSocket.isConnected()) {
				StringBuilder sb = new StringBuilder(Constants.DATAGRAM_TOKEN);
				sb.append(req.toString());
				mOutput.write(sb.toString());
				mOutput.flush();
				LogUtil.log(this, "发送报文:" + sb.toString());
				return true;
			}
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void addListener(OnReceiveDatagramListener l) {
		if (!mListeners.contains(l)) {
			mListeners.add(l);
		}
	}

	public void removeListener(OnReceiveDatagramListener l) {
		mListeners.remove(l);
	}
}
