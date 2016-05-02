/*
 * @File:   RtmpServer.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/16
 */

var net = require('net');
var uuid = require('node-uuid');
var RtmpSocket = require('./RtmpSocket.js');

// roomManager: see ../RoomManager.js, userManager: see ../UserManager.js
function RtmpServer(userManager, roomManager) {
	this.um = userManager;
	this.rm = roomManager;
}

RtmpServer.prototype.run = function() {
	var self = this;
	this.server = net.createServer(function(socket) {
		var streamId = uuid.v4();
		var rtmpConn = new RtmpSocket(streamId, socket, self.um, self.rm);

		socket.on('data', function(data) {
			rtmpConn.qb.push(data);
		});
		socket.on('end', function() {
			rtmpConn.stop();
			console.log('client disconnected, id=' + streamId);
		});
		socket.on('error', function(err) {
			rtmpConn.stop();
			console.log('client ' + streamId + ' error:' + err);
		});

		rtmpConn.run(rtmpConn);
		console.log('client connected, id=' + streamId);
	});
	this.server.listen(1935, function() {
		console.log('rtmp server start listening, port=1935');
	});
	this.server.on('error', function(err) {
		console.log('rtmp server listen error:' + err);
	});
};

module.exports = RtmpServer;