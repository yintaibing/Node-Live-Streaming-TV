/*
 * @File:   RoomService.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/27
 */

var Util = require('util');
var Room = require('../bean/Room.js');
var BaseService = require('./BaseService.js');
var Constants = require('../myutil/Constants.js');

function RoomService(dao) {
	BaseService.call(this, dao);

	this.events = {
		addRoomOk: 'addRoomOk',
		updateRoomOk: 'updateRoomOk',
		getRoomOk: 'getRoomOk',
		getRoomListOk: 'getRoomListOk'
	};
	this.roomData = {};
}

Util.inherits(RoomService, BaseService);

RoomService.prototype.addRoom = function(callback) {
	this.setCallback(this.events.addRoomOk, callback);

	var room = new Room(null, this.roomData.title, this.roomData.publisherId);
	room.setCategoryId(this.roomData.categoryId);
	this.dao.create(this, this.events.addRoomOk, room);
};

RoomService.prototype.updateRoom = function(callback) {
	this.setCallback(this.events.updateRoomOk, callback);

	var room = new Room(this.roomData.id, this.roomData.title, this.roomData.publisherId);
	room.setCategoryId(this.roomData.categoryId);
	this.dao.update(this, this.events.updateRoomOk, room);
};

RoomService.prototype.getRoom = function(callback) {
	this.setCallback(this.events.getRoomOk, callback);

	var room = new Room(this.roomData.id, this.roomData.title, this.roomData.publisherId);
	this.dao.read(this, this.events.getRoomOk, room);
};

RoomService.prototype.getRoomList = function(callback) {
	this.setCallback(this.events.getRoomListOk, callback);

	var room = new Room(null, this.roomData.title, null);
	this.dao.read(this, this.events.getRoomListOk, room);
};

RoomService.prototype.applyPublish = function(socketHandler, json, userManager, roomManager) {
	var self = this;
	self.roomData = {};
	self.roomData.title = json[Constants.KEY_ROOM_TITLE];
	self.roomData.publisherId = json[Constants.KEY_USER_ID];
	self.roomData.categoryId = json[Constants.KEY_CATEGORY_ID];

	self.addRoom(function() {
		self.getRoom(function(result) {
			var res = {};
			res[Constants.KEY_OP] = Constants.OP_APPLY_PUBLISH;

			if (!result) {
				res[Constants.KEY_STATUS] = Constants.STATUS_ERROR;
				res[Constants.KEY_MSG] = '申请直播失败';
			} else {
				var roomHolder = {};
				roomHolder.room = result[0];
				roomHolder.rtmpConns = {};
				roomHolder.danmakuSockets = {};
				roomHolder.publisher = userManager[result[0].getPublisherId()];
				roomHolder.audiences = [];
				roomHolder.public = {};
				roomManager[result[0].getId()] = roomHolder;

				result[0].publisherName = roomHolder.publisher.getName();
				result[0].audienceNum = roomHolder.audiences.length;

				res[Constants.KEY_STATUS] = Constants.STATUS_OK;
				res[Constants.KEY_JSON] = result[0];
			}
			socketHandler.response(res);
		});
	});
};

// modify my room
// modifyRoom = function....

RoomService.prototype.queryRoomList = function(socketHandler, roomData, roomManager) {
	if (!roomData) {
		// no query condition, get all
		var res = {};
		res[Constants.KEY_OP] = Constants.OP_GET_ROOM_LIST;

		var rooms = [];
		for (var roomId in roomManager) {
			var room = {};
			var roomHolder = roomManager[roomId];
			room.id = roomHolder.room.getId();
			room.title = roomHolder.room.getTitle();
			room.publisherId = roomHolder.room.getPublisherId();
			room.categoryId = roomHolder.room.getCategoryId();
			room.isLiving = roomHolder.room.getIsLiving();
			room.publisherName = roomHolder.publisher.getName();
			room.audienceNum = roomHolder.audiences.length;

			rooms.push(room);
		}

		res[Constants.KEY_STATUS] = Constants.STATUS_OK;
		res[Constants.KEY_JSON] = rooms;
		socketHandler.response(res);
	} else {
		// query condition exist, make a query
		var self = this;
		self.roomData = {};
		self.roomData.title = roomData.title;
		self.getRoomList(function(result) {
			var res = {};
			res[Constants.KEY_OP] = Constants.OP_GET_ROOM_LIST;
		
			for (var i in result) {
				var room = result[i];
				var roomId = room.getId();
				room.pulisherName = roomManager[roomId].publisher.getName();
				room.audienceNum = roomManager[roomId].audiences.length;
			}

			res[Constants.KEY_STATUS] = Constants.STATUS_OK;
			res[Constants.KEY_JSON] = result;
			socketHandler.response(res);
		});
	}
};

module.exports = RoomService;