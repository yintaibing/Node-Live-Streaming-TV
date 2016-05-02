/*
 * @File:   RtmpQueueBuffer.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/16
 * @Remark: This file refers to: 
 *          https://github.com/illuspas/Node-Media-Server/blob/master/nm_queuebuffer.js
 */

var Readable = require('stream').Readable;

function QueueBuffer() {
	this.reader = new Readable();
	this.buf = [];

	this.reader.on('error', function(err) {
		// console.log('queuebuffer error');
		// console.error(err);
	});
}

// store the RTMP datagrams
QueueBuffer.prototype.push = function(data) {
	this.reader.push(data);
};

// read n-length datagram into the buffer
QueueBuffer.prototype.read = function(length, endQueue) {
	var tempBuffer = this.reader.read(length);
	if (tempBuffer === null) {
		this.rollback();
		return null;
	} else {
		this.buf.push(tempBuffer);
		if (endQueue) {
			this.commit();
		}
		return tempBuffer;
	}
};

QueueBuffer.prototype.rollback = function() {
	var length = this.buf.length;
	for (var i = 0; i < length; i++) {
		this.reader.unshift(this.buf.pop());
	}
};

QueueBuffer.prototype.commit = function() {
	var length = this.buf.length;
	for (var i = 0; i < length; i++) {
		this.buf.shift();
	}
};

QueueBuffer.prototype.end = function() {
	this.reader.read();
	this.commit();
};

module.exports = QueueBuffer;