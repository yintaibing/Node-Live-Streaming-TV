/*
 * @File:   Constants.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/29
 */

module.exports = {
	DATAGRAM_TOKEN: '$DATAGRAM$',

	KEY_OP: 'op',
	KEY_USER_ID: 'userId',
	KEY_JSON: 'json',
	KEY_STATUS: 'status',
	KEY_MSG: 'msg',
	KEY_ROOM_TITLE: 'title',
	KEY_ROOM_ID: 'roomId',
	KEY_AUDIENCE_NUM: 'audienceNum',
	KEY_QUERY: 'query',
	KEY_CATEGORY_ID: 'categoryId',

	OP_REGISTR: 'register',
	OP_LOGIN: 'login',
	OP_LOGOUT: 'logout',
	OP_APPLY_PUBLISH: 'applyPublish',
	OP_ADD_LIKE: 'addLike',
	OP_CANCEL_LIKE: 'cancelLike',
	OP_GET_CATEGORY_LIST: 'getCategoryList',
	OP_GET_ROOM_LIST: 'getRoomList',
	OP_ENTER_ROOM: 'enterRoom',
	OP_LEAVE_ROOM: 'leaveRoom',
	OP_DANMAKU: 'danmaku',

	STATUS_OK: 'ok',
	STATUS_ERROR: 'error'
};