/*
 * @File:   app.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/27
 */

var Dao = require('./dao/Dao.js');
var SocketServer = require('./socket/SocketServer.js');
var RtmpServer = require('./rtmp/RtmpServer.js');
var UserService = require('./service/UserService.js');
var RoomService = require('./service/RoomService.js');

var userManager = {};
var roomManager = {};

var dao = new Dao();
dao.init();

setTimeout(function() {
	// read all publishers from database
	var us = new UserService(dao);
	us.setCallback(us.events.getUserOk, function(result) {
		for (var i in result) {
			var user = result[i];
			userManager[user.getId()] = user;
		}

		console.log('all publishers loaded ' + result.length);
	});
	us.userData.canPublish = true;
	us.getUser();

	// read all room from database
	var rs = new RoomService(dao);
	rs.setCallback(rs.events.getRoomListOk, function(result) {
		for (var i in result) {
			var room = result[i];
			var roomId = room.getId();

			roomManager[roomId] = {};
			roomManager[roomId].room = room;
			roomManager[roomId].rtmpConns = {};
			roomManager[roomId].danmakuSockets = {};
			roomManager[roomId].publisher = userManager[room.getPublisherId()];
			roomManager[roomId].audiences = [];
			roomManager[roomId].public = {};

			console.log('all room loaded ' + result.length);
		}
	});
	rs.getRoomList();
}, 2000);

// start SocketServer
var socketServer = new SocketServer(userManager, roomManager, dao);
socketServer.run();

// start RtmpServer
var rtmpServer = new RtmpServer(userManager, roomManager);
rtmpServer.run();