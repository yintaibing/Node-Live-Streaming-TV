package com.turtletv.android.util;

public class Constants {
	public static final int DATAGRAM_TOKEN_LEN = 10;
	public static final int DANMAKU_MAX_LEN = 20;
	public static final String DATAGRAM_TOKEN = "$DATAGRAM$",
			DES_KEY = "TurtleTV",
	
			KEY_OP = "op",
			KEY_USER_ID = "userId",
			KEY_RANDOM_USER_ID = "randomUserId",
			KEY_JSON = "json",
			KEY_STATUS = "status",
			KEY_MSG = "msg",
			KEY_ROOM_TITLE = "title",
			KEY_ROOM_ID = "roomId",
			KEY_AUDIENCE_NUM = "audienceNum",
			KEY_DANMAKU = "danmaku",
			KEY_QUERY = "query",
			KEY_CATEGORY_ID = "categoryId",

			OP_REGISTER = "register",
			OP_LOGIN = "login",
			OP_LOGOUT = "logout",
			OP_APPLY_PUBLISH = "applyPublish",
			OP_ADD_LIKE = "addLike",
			OP_CANCEL_LIKE = "cancelLike",
			OP_GET_ROOM_LIST = "getRoomList",
			OP_GET_CATEGORY_LIST = "getCategoryList",
			OP_ENTER_ROOM = "enterRoom",
			OP_LEAVE_ROOM = "leaveRoom",
			OP_DANMAKU = "danmaku",

			STATUS_OK = "ok",
			STATUS_ERROR = "error";
}
