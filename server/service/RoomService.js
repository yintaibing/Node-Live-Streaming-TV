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

RoomService.prototype.addRoom = function() {
	var room = new Room(null, this.roomData.title, this.roomData.publisherId);
	room.setCategoryId(this.roomData.categoryId);
	this.dao.create(this, this.events.addRoomOk, room);
};

RoomService.prototype.updateRoom = function() {
	var room = new Room(this.roomData.id, this.roomData.title, this.roomData.publisherId);
	room.setCategoryId(this.roomData.categoryId);
	this.dao.update(this, this.events.updateRoomOk, room);
};

RoomService.prototype.getRoom = function() {
	var room = new Room(this.roomData.id, this.roomData.title, this.roomData.publisherId);
	this.dao.read(this, this.events.getRoomOk, room);
};

RoomService.prototype.getRoomList = function() {
	var room = new Room(null, this.roomData.title, null);
	this.dao.read(this, this.events.getRoomListOk, room);
};

RoomService.prototype.applyPublish = function(socketHandler, json, userManager, roomManager) {
	var self = this;
	self.roomData = {};
	self.roomData.title = json[Constants.KEY_ROOM_TITLE];
	self.roomData.publisherId = json[Constants.KEY_USER_ID];
	self.roomData.categoryId = json[Constants.KEY_CATEGORY_ID];

	self.setCallback(self.events.addRoomOk, function() {
		self.setCallback(self.events.getRoomOk, function(result) {
			var res = {};
			res[Constants.KEY_OP] = Constants.OP_APPLY_PUBLISH;

			if (!result) {
				res[Constants.KEY_STATUS] = Constants.STATUS_ERROR;
				res[Constants.KEY_MSG] = '申请直播失败';
			} else {
				res[Constants.KEY_STATUS] = Constants.STATUS_OK;
				res[Constants.KEY_JSON] = result[0];

				roomManager[result[0].getId()] = {};
				var roomHolder = roomManager[result[0].getId()];
				roomHolder.room = result[0];
				roomHolder.publisher = userManager[result[0].getPublisherId()];
			}
			socketHandler.response(res);
		});
		self.getRoom();
	});
	self.addRoom();
};

// modify my room
// modifyRoom = function....

RoomService.prototype.queryRoomList = function(socketHandler, roomData, roomManager) {
	if (!roomData) {
		// no query condition, get all
		var res = {};
		res[KEY_OP] = Constants.OP_GET_ROOM_LIST;

		var roomHolders = [];
		for (var roomId in roomManager) {
			var holder = {};
			holder.room = roomManager[roomId].room;
			holder.audienceNum = roomManager[roomId].audiences.length;
			roomHolders.push(holder);
		}

		res[KET_STATUS] = Constants.STATUS_OK;
		res[Constants.KEY_JSON] = roomHolders;
		socketHandler.response(res);
	} else {
		// query condition exist, make a query
		var self = this;
		self.roomData = {};
		self.roomData.title = roomData.title;
		self.setCallback(self.events.getRoomListOk, function(result) {
			var res = {};
			res[KEY_OP] = Constants.OP_GET_ROOM_LIST;
		
			var roomHolders = [];
			for (var i in result) {
				var holder = {};
				var roomId = result[i].getId();
				holder.room = roomManager[roomId].room;
				holder.audienceNum = roomManager[roomId].audiences.length;
				roomHolders.push(roomHolder);
			}

			res[KET_STATUS] = Constants.STATUS_OK;
			res[Constants.KEY_JSON] = roomHolders;
			socketHandler.response(res);
		});
		self.getRoomList();
	}
};

module.exports = RoomService;