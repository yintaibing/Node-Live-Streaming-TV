/*
 * @File:   SocketServer.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/27
 */

var net = require('net');
var uuid = require('node-uuid');
var SocketHandler = require('./SocketHandler.js');

var sockets = {};
var handlers = {};

// roomManager: see ../RoomManager.js, userManager: see ../UserManager.js
function SocketServer(userManager, roomManager, dao) {
	this.um = userManager;
	this.rm = roomManager;
	this.dao = dao;
}

SocketServer.prototype.run = function() {
	var self = this;
	this.server = net.createServer(function(socket) {
		var socketId = uuid.v4();
		sockets[socketId] = socket;
		handlers[socketId] = new SocketHandler(self.um, self.rm, self.dao, socketId, socket);

		console.log('socket connected, id=' + socketId);

		socket.setEncoding('utf8');
		socket.on('data', function(data) {
			console.log('receive data from ' + socketId + '\ndata=' + data);
			handlers[socketId].handle(JSON.parse(data));
		});
		socket.on('end', function() {
			console.log('socket disconnected, id=' + socketId);
			delete sockets[socketId];
			delete handlers[socketId];
		});
		socket.on('error', function(err) {
			console.log('socket ' + socketId + ' error:' + err);
		});
	});
	this.server.listen(19350, function() {
		console.log('socket server start listening, port=19350');
	});
	this.server.on('error', function(err) {
		console.log('socket server listen error:' + err);
	});
};

module.exports = SocketServer;