/*
 * @File:   SocketHandler.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/27
 */

var Util = require('util');
var UserService = require('../service/UserService.js');
var CategoryService = require('../service/CategoryService.js');
var RoomService = require('../service/RoomService.js');
var Constants = require('../myutil/Constants.js');

function SocketHandler(userManager, roomManager, dao, socketId, socket) {
	this.um = userManager;
	this.rm = roomManager;
	this.us = new UserService(dao);
	this.cs = new CategoryService(dao);
	this.rs = new RoomService(dao);
	this.socketId = socketId;
	this.socket = socket;
}

SocketHandler.prototype.handle = function(data) {
	switch (data[Constants.KEY_OP]) {
	case Constants.OP_REGISTR: {
		this.us.register(this, data[Constants.KEY_JSON], this.um);
		break;
	}

	case Constants.OP_LOGIN: {
		this.us.login(this, data[Constants.KEY_JSON], this.um);
		break;
	}

	case Constants.OP_LOGOUT: {
		this.us.logout(this, data[Constants.KEY_USER_ID], this.um);
		break;
	}

	case Constants.OP_APPLY_PUBLISH: {
		this.us.applyPublish(this, data, this.um);
		break;
	}

	case Constants.OP_GET_CATEGORY_LIST: {
		this.cs.queryCategoryList(this, data[Constants.KEY_JSON]);
		break;
	}

	case Constants.OP_GET_ROOM_LIST: {
		this.rs.queryRoomList(this, data[Constants.KEY_JSON], this.rm);
		break;
	}

	case Constants.OP_ENTER_ROOM: {
		// add this socket into damakuSockets
		var roomId = data[Constants.KEY_ROOM_ID];
		var roomHolder = this.rm[roomId];
		roomHolder.danmakuSockets[this.socketId] = this.socket;
		break;
	}

	case Constants.OP_LEAVE_ROOM: {
		// remove this socket from damakuSockets
		var roomId = data[Constants.KEY_ROOM_ID];
		var roomHolder = this.rm[roomId];
		delete roomHolder.danmakuSockets[this.socketId];
	}

	case Constants.OP_DANMAKU: {
		// broadcast this danmaku to the other clients in the same room
		var roomId = data[Constants.KEY_ROOM_ID];
		var danmaku = data[Constants.KEY_JSON];
		var danmakuSockets = this.rm[roomId].danmakuSockets;
		for (var socketId in danmakuSockets) {
			var socket = danmakuSockets[socketId];
			var res = {};
			res[Constants.KEY_OP] = Constants.OP_DANMAKU;
			res[Constants.KEY_JSON] = danmaku;
			this.response(res);
		}
		break;
	}

	case Constants.OP_ADD_LIKE:

	case Constants.OP_CANCEL_LIKE:

	}
};

SocketHandler.prototype.response = function(res) {
	var resStr = Constants.DATAGRAM_TOKEN + JSON.stringify(res) + '\r\n';
	console.log('response=' + resStr);
	this.socket.write(resStr, 'utf8');
	this.socket.pipe(this.socket);
};

module.exports = SocketHandler;