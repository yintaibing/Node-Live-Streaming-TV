/*
 * @File:   RtmpConn.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/16
 * @Remark: This file refers to: 
 *          https://github.com/illuspas/Node-Media-Server/blob/master/nm_rtmp_conn.js
 */

var QueueBuffer = require('QueueBuffer.js');
var AMF = require('RtmpAmf.js');
var RtmpHandshake = require('RtmpHandshake.js');
var User = require('../bean/User.js');
var myutil = require('../util/myutil.js');

var AAC_SAMPLE_RATE = [
	96000, 88200, 64000, 48000,
    44100, 32000, 24000, 22050,
    16000, 12000, 11025, 8000,
    7350,  0, 	  0, 	 0
];

function readUInt24BE(buf, off) {
	if (off === null) {
		off = 0;
	}
	return (buf[0 + off] << 16) + (buf[1 + off] << 8) + buf[2 +off];
}

function writeUInt24BE(buf, val, off) {
	if (off === null) {
		off = 0;
	}
	buf[offset] = (val >> 16) & 0xFF;
    buf[offset + 1] = (val >> 8) & 0xFF;
    buf[offset + 2] = val & 0xFF;
}

// my own RtmpConn, different to the reference above
// userManager: see ../UserManager.js
// roomManager: see ../RoomManager.js
function RtmpConn(streamId, userManager, roomManager, socket) {
	this.streamId = streamId;
	this.user = null;
	this.um = userManager;
	this.rm = roomManager;
	this.rh = null; // rh is roomHolder, which equals roomManager[roomId]
	this.socket = socket;
	this.rtmpStatus = 0;
	this.inChunkSize = 128;
	this.outChunkSize = 128;
	this.preChunkMsg = {};
	this.connCmdObj = null;
	this.isFirstAudioReceived = true;
	this.isFirstVideoReceived = true;
	this.lastAudioTimestamp = 0;
	this.lastVideoTimestamp = 0;
	this.isPublisherStream = false;

	this.qb = new QueueBuffer();
	this.sendBufferQueue = [];
	this.codec = {
		width: 0,
        height: 0,
        duration: 0,
        framerate: 0,
        videodatarate: 0,
        audiosamplerate: 0,
        audiosamplesize: 0,
        audiodatarate: 0,
        spsLen: 0,
        sps: null,
        ppsLen: 0,
        pps: null
	};
}

RtmpConn.prototype.run = function(self) {
	setInterval(self.handshake, 10, self);

	// self.rh.rtmpConns[self.streamId] = self;
};

RtmpConn.prototype.stop = function() {
	this.qb.end();

	this.user.setStreamId(null);
	var audiences = this.rh.audiences;
	if (this.isPublisherStream) {
		// if this stream is a publish stream, notify the audience the publisher is gone,
		// but do not delete the audiences' stream, so they can continue their danmaku,
		// you'd better delete the audiences' stream only when they leave the room
		this.rh.room.setIsLiving(false);
		for (var audienceId in audiences) {
			var audience = this.um[audienceId];
			this.rh.rtmpConns[audience.getStreamId()].sendStreamEOF();
		}
	} else {
		// if this stream is a play stream, delete this stream from connPool, and delete
		// this user from audiences of this room
		this.rh.audiences = myutil.removeFromAry(this.rh.audiences, this.user.getId());
	}
	delete this.rh.rtmpConns[this.streamId];
};

// there'll be serveral AMFCommand datagram to be transfered, so this function will be 
// called, which ensure this.rh would not be null when other function call it later
RtmpConn.prototype.initUserAndRoom = function(isPublish, streamStr) {
	var l = 'l', u = 'u', r = 'r';
	var params = myutil.parseQueryString(myutil.desDe(streamStr));
	var isLogined = params[l],
		userId = params[u],
		roomId = params[r];

	if (isPublish) {
		// unecessary to identify if publishers are logined or not, but necessary to check 
		// the userId and roomId is matched
		var roomHolder = this.rm[roomId];
		if (!roomHolder) {
			return false;
		}
		if (parseInt(userId) !== roomHolder.room.getPublisherId()) {
			return false;
		}
		this.isPublisherStream = true;
		this.rh = roomHolder;
		this.rh.publisher = this.um[userId];
		this.rh.room.setIsLiving(true);
		this.rh.rtmpConns[this.streamId] = this;
		if (!this.rh.public) {
			this.rh.public = {};
		}
		return true;
	} else {
		// necessary to identify if audiences are logined or not
		this.rh = this.rm[roomId];
		this.rh.rtmpConns[this.streamId] = this;
		if (isLogined) {
			// user is logined, get user obj from UserManager
			this.user = this.um[userId];
			this.user.setStreamId(this.streamId);
			if (!this.rh.audiences) {
				this.rh.audiences = [];
			}
			this.rh.audiences.push(this.user.getId());
		} else {
			// user is not logined, put a temp user obj into UserManager
			this.user = new User();
			this.user.setId(userId);
			this.user.setStreamId(this.streamId);
			this.um[userId] = this.user;
			this.rh.audiences.push(userId);
			if (!this.rh.public) {
				this.rh.public = {};
			}
		}
		return true;
	}
};

RtmpConn.prototype.getRealChunkSize = function(rtmpBodySize, chunkSize) {
	var size = rtmpBodySize + parseInt(rtmpBodySize / chunkSize);
	return (rtmpBodySize % chunkSize) ? size : (size - 1);
};

RtmpConn.prototype.handshake = function(self) {
	if (self.rtmpStatus === 0) {
		// the size of the first datagram of RTMP handshaking is 1537
		var c0c1 = self.qb.read(1537, true);
		if (!c0c1) {
			return;
		}
		var s0s1s2 = RtmpHandshake.generateS0S1S2(c0c1);
		self.socket.write(s0s1s2);
		self.rtmpStatus = 1;
	} else {
		var c2 = self.qb.read(1536, true);
		if (!c2) {
			return;
		}
		self.rtmpStatus = 2;
		clearInterval(this);
		self.parseRtmpMsg(self);
	}
};

RtmpConn.prototype.parseRtmpMsg = function(self) {
	var ret = 0;
	if (!self.rh || !self.rh.room || !self.rh.room.getIsLiving()) {
		return;
	}

	do {
		var msg = {},
			chunkMsgHeader = null,
			preChunk = null,
			pos = 0,
			chunkBasicHeader = self.qb.read(1),
			exStreamId = null;
		if (!chunkBasicHeader) {
			break;
		}

		msg.formatType = chunkBasicHeader[0] >> 6;
		msg.chunkStreamId = chunkBasicHeader[0] & 0x3F;
		if (msg.chunkStreamId === 0) {
			exStreamId = self.qb.read(1);
			if (!exStreamId) {
				break;
			}
			msg.chunkStreamId = exStreamId[0] + 64;
		} else if (msg.chunkStreamId === 1) {
			exStreamId = self.qb.read(2);
			if (!exStreamId) {
				break;
			}
			msg.chunkStreamId = (exStreamId[0] << 8) + exStreamId[1] + 64;
		}

		if (msg.formatType === 0) {
			// type 0: 11 bytes
			chunkMsgHeader = self.qb.read(11);
			if (!chunkMsgHeader) {
				break;
			}
			msg.timestamp = readUInt24BE(chunkMsgHeader, 0);
			msg.timestampDelta = 0;
			msg.msgLength = readUInt24BE(chunkMsgHeader, 3);
			msg.msgTypeId = chunkMsgHeader[6];
			msg.msgStreamId = chunkMsgHeader.readInt32LE(7);
		} else if (msg.formatType === 1) {
			// type 1: 7 bytes
			chunkMsgHeader = self.qb.read(7);
			if (!chunkMsgHeader) {
				break;
			}
			msg.timestampDelta = readUInt24BE(chunkMsgHeader, 0);
			msg.msgLength = readUInt24BE(chunkMsgHeader, 3);
			msg.msgTypeId = chunkMsgHeader[6];
			preChunk = self.preChunkMsg[msg.chunkStreamId];
			if (preChunk) {
				msg.timestamp = preChunk.timestamp;
				msg.msgStreamId = preChunk.msgStreamId;
			} else {
				throw 'Chunk reference error for type 1';
			}
		} else if (msg.formatType === 2) {
			// type 2: 3 bytes
			chunkMsgHeader = self.qb.read(3);
			if (!chunkMsgHeader) {
				break;
			}
			msg.timestampDelta = readUInt24BE(chunkMsgHeader, 0);
			preChunk = self.preChunkMsg[msg.chunkStreamId];
			if (preChunk) {
				msg.timestamp = preChunk.timestamp;
				msg.msgStreamId = preChunk.msgStreamId;
				msg.msgLength = preChunk.msgLength;
				msg.msgTypeId = preChunk.msgTypeId;
			} else {
				throw 'Chunk reference error for type 2';
			}
		} else if (msg.formatType === 3) {
			// type 3: 0 bytes
			preChunk = self.preChunkMsg[msg.chunkStreamId];
			if (preChunk) {
				msg.timestamp = preChunk.timestamp;
				msg.msgStreamId = preChunk.msgStreamId;
				msg.msgLength = preChunk.msgLength;
				msg.timestampDelta = preChunk.timestampDelta;
				msg.msgTypeId =preChunk.msgTypeId;
			} else {
				throw 'Chunk reference error for type 3';
			}
		} else {
			throw ('Unknow format type:' + msg.formatType);
		}

		// extended timestamp
		var chunkBodyHeader = null;
		if (msg.formatType === 0) {
			if (msg.timestamp === 0xFFFFFF) {
				chunkBodyHeader = self.qb.read(3);
				if (!chunkBodyHeader) {
					break;
				}
				msg.timestamp = (chunkBodyHeader[0] * Math.pow(256, 3)) + 
					(chunkBodyHeader[1] << 16) + (chunkBodyHeader[2] << 8) + 
					chunkBodyHeader[3];
			}
		} else if (msg.timestampDelta === 0xFFFFFF) {
			chunkBodyHeader = self.qb.read(4);
			if (!chunkBodyHeader) {
				break;
			}
			msg.timestampDelta = (chunkBodyHeader[0] * Math.pow(256, 3)) + 
				(chunkBodyHeader[1] << 16) + (chunkBodyHeader[2] << 8) + 
				chunkBodyHeader[3];
		}

		var rtmpBody = [],
			rtmpBodySize = msg.msgLength;
		var chunkBodySize = self.getRealChunkSize(rtmpBodySize, self.inChunkSize);
		var chunkBody = self.qb.read(chunkBodySize);
		var chunkBodyPos = 0;

		if (!chunkBody) {
			break;
		}

		do {
			if (rtmpBodySize > self.inChunkSize) {
				rtmpBody.push(chunkBody.slice(chunkBodyPos, chunkBodyPos + self.inChunkSize));
				rtmpBodySize -= self.inChunkSize;
				chunkBodyPos += self.inChunkSize;
				chunkBodyPos++;
			} else {
				rtmpBody.push(chunkBody.slice(chunkBodyPos, chunkBodyPos + rtmpBodySize));
				rtmpBodySize -= rtmpBodySize;
				chunkBodyPos += rtmpBodySize;
			}
		} while (rtmpBodySize > 0);

		msg.timestamp += msg.timestampDelta;
		self.preChunkMsg[msg.chunkStreamId] = msg;
		var rtmpBodyBuf = Buffer.concat(rtmpBody);
		self.handleRtmpMsg(msg, rtmpBodyBuf);
		self.qb.commit();
		ret = 1;
	} while (0);

	if (ret === 0) {
		setTimeout(self.parseRtmpMsg, 200, self);
	} else {
		setImmediate(self.parseRtmpMsg, self);
	}
};

RtmpConn.prototype.createRtmpMsg = function(rtmpHeader, rtmpBody) {
	var formatTypeId = 0;
	var rtmpBodySize = rtmpBody.length;

	if (!rtmpHeader.chunkStreamId || 
		!rtmpHeader.timestamp ||
		!rtmpHeader.msgTypeId ||
		!rtmpHeader.msgStreamId) {
		console.error('error creating RTMP msg');
	}

	var useExtendedTimestamp = false,
		timestamp;
	if (rtmpHeader.timestamp >= 0xFFFFFF) {
		useExtendedTimestamp = true;
		timestamp = [0xFF, 0xFF, 0xFF];
	} else {
		timestamp = [(rtmpHeader.timestamp >> 16) & 0xFF, 
			(rtmpHeader.timestamp >> 8) & 0xFF, 
			rtmpHeader.timestamp & 0xFF];
	}

	var bufs = new Buffer([(formatTypeId << 6) | rtmpHeader.chunkStreamId, 
			timestamp[0], timestamp[1], timestamp[2], 
			(rtmpBodySize >> 16) & 0xFF, (rtmpBodySize >> 8) & 0xFF], rtmpBodySize & 0xFF, 
			rtmpHeader.msgTypeId, rtmpHeader.msgStreamId & 0xFF, 
			(rtmpHeader.msgStreamId >>> 8) & 0xFF, (rtmpHeader.msgStreamId >>> 16) & 0xFF, 
			(rtmpHeader.msgStreamId >>> 24) & 0xFF);

	if (useExtendedTimestamp) {
		var extendedTimestamp = new Buffer([(rtmpHeader.timestamp >> 24) & 0xFF, 
			(rtmpHeader.timestamp >> 16) & 0xFF, (rtmpHeader.timestamp >> 8) & 0xFF, 
			rtmpHeader.timestamp & 0xFF]);
		bufs = Buffer.concat([bufs, extendedTimestamp]);
	}

	var rtmpBodyPos = 0,
		chunkBody = [];
	var chunkBodySize = this.getRealChunkSize(rtmpBodySize, this.outChunkSize);
	var type3Header = new Buffer([(3 << 6) | rtmpHeader.chunkStreamId]);

	do {
		if (rtmpBodySize > this.outChunkSize) {
			chunkBody.push(rtmpBody.slice(rtmpBodyPos, rtmpBodyPos + this.outChunkSize));
			rtmpBodySize -= this.outChunkSize;
			rtmpBodyPos += this.outChunkSize;
			chunkBody.push(type3Header);
		} else {
			chunkBody.push(rtmpBody.slice(rtmpBodyPos, rtmpBodyPos + rtmpBodySize));
			rtmpBodySize -= rtmpBodySize;
			rtmpBodyPos += rtmpBodySize;
		}
	} while (rtmpBodySize > 0);

	var chunkBodyBuf = Buffer.concat(chunkBody);
	bufs = Buffer.concat([bufs, chunkBodyBuf]);
	return bufs;
};

RtmpConn.prototype.handleRtmpMsg = function(rtmpHeader, rtmpBody) {
	switch (rtmpHeader.msgTypeId) {
		case 0x01: {
			this.inChunkSize = rtmpBody.readUInt24BE(0);
			break;
		}

		case 0x04: {
			var userControlMsg = this.parseUserControlMsg(rtmpBody);
			if (userControlMsg.eventType === 3) {
				var streamId = (userControlMsg.eventData[0] << 24) + 
					(userControlMsg.eventData[1] << 16) + (userControlMsg.eventData[2] << 8) + 
					userControlMsg.eventData[3];
				var bufLength = (userControlMsg.eventData[4] << 24) + 
					(userControlMsg.eventData[5] << 16) + (userControlMsg.eventData[6] << 8) + 
					userControlMsg.eventData[7];
			} else if (userControlMsg.eventType === 7) {
				var timestamp = (userControlMsg.eventData[0] << 24) + 
					(userControlMsg.eventData[1] << 16) + (userControlMsg.eventData[2] << 8) + 
					userControlMsg.eventData[3];
			} else {
				//
			}
			break;
		}

		case 0x08: {
			// audio data
			this.parseAudioMsg(rtmpHeader, rtmpBody);
			break;
		}

		case 0x09: {
			// video data
			this.parseVideoMsg(rtmpHeader, rtmpBody);
			break;
		}

		case 0x12: {
			// AMF0 data
			var cmd = AMF.decodeAmf0Cmd(rtmpBody);
			this.handleAMFDataMsg(cmd);
			break;
		}

		case 0x14: {
			// AMF0 command
			var cmd1 = AMF.decodeAmf0Cmd(rtmpBody);
			this.handleAMFCommandMsg(cmd1);
			break;
		}
	}
};

RtmpConn.prototype.handleAMFDataMsg = function(cmd) {
	if (cmd.cmd === '@setDataFrame') {
		this.receiveSetDataFrame(cmd.method, cmd.cmdObj);
	}
};

RtmpConn.prototype.handleAMFCommandMsg = function(cmd) {
	var streamStr = cmd.streamName;

	switch (cmd.cmd) {
		case 'connect': {
			this.connCmdObj = cmd.cmdObj;
			this.windowACK(2500000);
			this.setPeerBandwidth(2500000, 2);
			this.outChunkSize = 4000;
			this.setChunkSize(this.outChunkSize);
			this.respondConnect();
			break;
		}

		case 'createStream': {
			this.responsdCreateStream(cmd);
			break;
		}

		case 'play': {
			console.log('client want to play stream ' + streamStr);
			this.initUserAndRoom(false, streamStr);

			this.respondPlay();
			this.startPlay();
			break;
		}

		case 'publish': {
			console.log('publisher want to publish stream ' + streamStr);

			// when there are more than 1 publisher who want to publish the same streamName, 
			// give the later one a error response
			if (this.initUserAndRoom(true, streamStr)) {
				console.log('bad publish stream name ' + cmd.name);
				this.respondPublishError();
				return;
			}
			this.respondPublish();
			break;
		}

		case 'closeStream':
		case 'deleteStream':
		case 'releaseStream':
		case 'pause':
		case 'FCPublish':
		case 'FCUnpublish': {
			break;
		}
	}
};

RtmpConn.prototype.windowACK = function(size) {
	var rtmpBuffer = new Buffer('02000000000004050000000000000000', 'hex');
    rtmpBuffer.writeUInt32BE(size, 12);
    this.socket.write(rtmpBuffer);
};

RtmpConn.prototype.setPeerBandwidth = function(size, type) {
	var rtmpBuffer = new Buffer('0200000000000506000000000000000000', 'hex');
    rtmpBuffer.writeUInt32BE(size, 12);
    rtmpBuffer[16] = type;
    this.socket.write(rtmpBuffer);
};

RtmpConn.prototype.setChunkSize = function(size) {
    var rtmpBuffer = new Buffer('02000000000004010000000000000000', 'hex');
    rtmpBuffer.writeUInt32BE(size, 12);
    this.socket.write(rtmpBuffer);
};

RtmpConn.prototype.respondConnect = function() {
	var rtmpHeader = {
        chunkStreamID: 3,
        timestamp: 0,
        messageTypeID: 0x14,
        messageStreamID: 0
    };
    var opt = {
        cmd: '_result',
        transId: 1,
        cmdObj: {
            fmsVer: 'FMS/3,0,1,123',
            capabilities: 31
        },
        info: {
            level: 'status',
            code: 'NetConnection.Connect.Success',
            description: 'Connection succeeded.',
            objectEncoding: 0
        }
    };
    var rtmpBody = AMF.encodeAmf0Cmd(opt);
    var rtmpMessage = this.createRtmpMsg(rtmpHeader, rtmpBody);
    this.socket.write(rtmpMessage);
};

RtmpConn.prototype.respondCreateStream = function(cmd) {
    // //console.log(cmd);
    var rtmpHeader = {
        chunkStreamID: 3,
        timestamp: 0,
        messageTypeID: 0x14,
        messageStreamID: 0
    };
    var opt = {
        cmd: "_result",
        transId: cmd.transId,
        cmdObj: null,
        info: 1,
        objectEncoding: 0

    };
    var rtmpBody = AMF.encodeAmf0Cmd(opt);
    var rtmpMessage = this.createRtmpMsg(rtmpHeader, rtmpBody);
    this.socket.write(rtmpMessage);
};

RtmpConn.prototype.respondPlay = function() {
    var rtmpHeader = {
        chunkStreamID: 3,
        timestamp: 0,
        messageTypeID: 0x14,
        messageStreamID: 1
    };
    var opt = {
        cmd: 'onStatus',
        transId: 0,
        cmdObj: null,
        info: {
            level: 'status',
            code: 'NetStream.Play.Start',
            description: 'Start live'
        }
    };
    var rtmpBody = AMF.encodeAmf0Cmd(opt);
    var rtmpMessage = this.createRtmpMsg(rtmpHeader, rtmpBody);
    this.socket.write(rtmpMessage);

    rtmpHeader = {
        chunkStreamID: 5,
        timestamp: 0,
        messageTypeID: 0x12,
        messageStreamID: 1
    };
    opt = {
        cmd: '|RtmpSampleAccess',
        bool1: true,
        bool2: true
    };

    rtmpBody = AMF.encodeAmf0Cmd(opt);
    rtmpMessage = this.createRtmpMsg(rtmpHeader, rtmpBody);
    this.socket.write(rtmpMessage);
};

RtmpConn.prototype.startPlay = function() {
    var roomPublic = this.rh.public;
    if (!roomPublic.metaData ||
    	!roomPublic.cacheAudioSequenceBuffer ||
    	!roomPublic.cacheVideoSequenceBuffer) {
    	return;
    }

    var rtmpHeader = {
        chunkStreamID: 5,
        timestamp: 0,
        messageTypeID: 0x12,
        messageStreamID: 1
    };

    var opt = {
        cmd: 'onMetaData',
        cmdObj: roomPublic.metaData
    };

    var rtmpBody = AMF.encodeAmf0Cmd(opt);
    var metaDataRtmpMessage = this.createRtmpMsg(rtmpHeader, rtmpBody);


    rtmpHeader = {
        chunkStreamID: 4,
        timestamp: 0,
        messageTypeID: 0x08,
        messageStreamID: 1
    };
    var audioSequenceRtmpMessage = this.createRtmpMsg(rtmpHeader, 
    	roomPublic.cacheAudioSequenceBuffer);


    rtmpHeader = {
        chunkStreamID: 4,
        timestamp: 0,
        messageTypeID: 0x09,
        messageStreamID: 1
    };
    var videoSequenceRtmpMessage = this.createRtmpMsg(rtmpHeader, 
    	roomPublic.cacheVideoSequenceBuffer);

    var beginRtmpMessage = new Buffer("020000000000060400000000000000000001", 'hex');
    this.sendBufferQueue.push(beginRtmpMessage);
    this.sendBufferQueue.push(metaDataRtmpMessage);
    this.sendBufferQueue.push(audioSequenceRtmpMessage);
    this.sendBufferQueue.push(videoSequenceRtmpMessage);
    this.sendRtmpMsg(this);
};

RtmpConn.prototype.respondPublish = function() {
    var rtmpHeader = {
        chunkStreamID: 5,
        timestamp: 0,
        messageTypeID: 0x14,
        messageStreamID: 1
    };
    var opt = {
        cmd: 'onStatus',
        transId: 0,
        cmdObj: null,
        info: {
            level: 'status',
            code: 'NetStream.Publish.Start',
            description: 'Start publishing'
        }
    };
    var rtmpBody = AMF.encodeAmf0Cmd(opt);
    var rtmpMessage = this.createRtmpMsg(rtmpHeader, rtmpBody);
    this.socket.write(rtmpMessage);
};

RtmpConn.prototype.respondPublishError = function() {
    var rtmpHeader = {
        chunkStreamID: 5,
        timestamp: 0,
        messageTypeID: 0x14,
        messageStreamID: 1
    };
    var opt = {
        cmd: 'onStatus',
        transId: 0,
        cmdObj: null,
        info: {
            level: 'error',
            code: 'NetStream.Publish.BadName',
            description: 'Already publishing'
        }
    };
    var rtmpBody = AMF.encodeAmf0Cmd(opt);
    var rtmpMessage = this.createRtmpMsg(rtmpHeader, rtmpBody);
    this.socket.write(rtmpMessage);
};

RtmpConn.prototype.receiveSetDataFrame = function(method, obj) {
    if (method == 'onMetaData') {
        this.rh.public.metaData = obj;
    }
};

RtmpConn.prototype.parseUserControlMessage = function(buf) {
    var eventData, eventType;
    eventType = (buf[0] << 8) + buf[1];
    eventData = buf.slice(2);
    var message = {
        eventType: eventType,
        eventData: eventData
    };
    if (eventType === 3) {
        message.streamID = (eventData[0] << 24) + (eventData[1] << 16) + 
        	(eventData[2] << 8) + eventData[3];
        message.bufferLength = (eventData[4] << 24) + (eventData[5] << 16) + 
        	(eventData[6] << 8) + eventData[7];
    }
    return message;
};

RtmpConn.prototype.parseAudioMessage = function(rtmpHeader, rtmpBody) {
    var streamId, otherStream;
    if (this.isFirstAudioReceived) {
        var sound_format = rtmpBody[0];
        var sound_type = sound_format & 0x01;
        var sound_size = (sound_format >> 1) & 0x01;
        var sound_rate = (sound_format >> 2) & 0x03;
        sound_format = (sound_format >> 4) & 0x0f;
        if (sound_format != 10) {
            console.error("Only support audio aac codec. actual=" + sound_format);
            return -1;
        }
        var aac_packet_type = rtmpBody[1];
        if (aac_packet_type === 0) {
            this.codec.aac_profile = rtmpBody[2];
            this.codec.aac_sample_rate = rtmpBody[3];

            this.codec.aac_channels = (this.codec.aac_sample_rate >> 3) & 0x0f;
            this.codec.aac_sample_rate = ((this.codec.aac_profile << 1) & 0x0e) | 
            	((this.codec.aac_sample_rate >> 7) & 0x01);
            this.codec.aac_profile = (this.codec.aac_profile >> 3) & 0x1f;
            this.codec.audiosamplerate = AAC_SAMPLE_RATE[this.codec.aac_sample_rate];
            if (this.codec.aac_profile === 0 || this.codec.aac_profile === 0x1f) {
                console.error("Parse audio aac sequence header failed, adts object=" + 
                	this.codec.aac_profile + "invalid.");
                return -1;
            }
            this.codec.aac_profile--;
            this.isFirstAudioReceived = false;
            this.rh.public.cacheAudioSequenceBuffer = new Buffer(rtmpBody);
            if (this.isPublisherStream) {
            	for (streamId in this.rh.rtmpConns) {
            		otherStream = this.rh.rtmpConns[streamId];
            		if (!otherStream.isPublisherStream) {
            			otherStream.startPlay();
            		}
            	}
            }
        }
    } else {
        var sendRtmpHeader = {
            chunkStreamID: 4,
            timestamp: rtmpHeader.timestamp,
            messageTypeID: 0x08,
            messageStreamID: 1
        };
        var rtmpMessage = this.createRtmpMsg(sendRtmpHeader, rtmpBody);

        if (this.isPublisherStream) {
        	for (streamId in this.rh.rtmpConns) {
        		otherStream = this.rh.rtmpConns[streamId];
        		if (!otherStream.isPublisherStream) {
        			otherStream.sendBufferQueue.push(rtmpMessage);
        		}
        	}
        }
    }
};

RtmpConn.prototype.parseVideoMessage = function(rtmpHeader, rtmpBody) {
    var streamId, otherStream;
    var index = 0;
    var frame_type = rtmpBody[0];
    var codec_id = frame_type & 0x0f;
    frame_type = (frame_type >> 4) & 0x0f;
    // only support h.264/avc
    if (codec_id !== 7) {
        //console.error("Only support video h.264/avc codec. actual=" + codec_id);
        return -1;
    }
    var avc_packet_type = rtmpBody[1];
    var composition_time = readUInt24BE(rtmpBody, 2);
    //  printf("v composition_time %d\n",composition_time);

    if (avc_packet_type === 0) {
        if (this.isFirstVideoReceived) {
            //AVC sequence header
            var configurationVersion = rtmpBody[5];
            this.codec.avc_profile = rtmpBody[6];
            var profile_compatibility = rtmpBody[7];
            this.codec.avc_level = rtmpBody[8];
            var lengthSizeMinusOne = rtmpBody[9];
            lengthSizeMinusOne &= 0x03;
            this.codec.NAL_unit_length = lengthSizeMinusOne;

            //  sps
            var numOfSequenceParameterSets = rtmpBody[10];
            numOfSequenceParameterSets &= 0x1f;

            if (numOfSequenceParameterSets != 1) {
                //console.error("Decode video avc sequenc header sps failed.\n");
                return -1;
            }

            this.codec.spsLen = rtmpBody.readUInt16BE(11);

            index = 11 + 2;
            if (this.codec.spsLen > 0) {
                this.codec.sps = new Buffer(this.codec.spsLen);
                rtmpBody.copy(this.codec.sps, 0, 13, 13 + this.codec.spsLen);
            }
            // pps
            index += this.codec.spsLen;
            var numOfPictureParameterSets = rtmpBody[index];
            numOfPictureParameterSets &= 0x1f;
            if (numOfPictureParameterSets != 1) {
                //console.error("Decode video avc sequenc header pps failed.\n");
                return -1;
            }

            index++;
            this.codec.ppsLen = rtmpBody.readUInt16BE(index);
            index += 2;
            if (this.codec.ppsLen > 0) {
                this.codec.pps = new Buffer(this.codec.ppsLen);
                rtmpBody.copy(this.codec.pps, 0, index, index + this.codec.ppsLen);
            }
            this.isFirstVideoReceived = false;
            this.rh.public.cacheVideoSequenceBuffer = new Buffer(rtmpBody);
            if (this.isPublisherStream) {
            	for (streamId in this.rh.rtmpConns) {
            		otherStream = this.rh.rtmpConns[streamId];
            		if (!otherStream.isPublisherStream) {
            			otherStream.startPlay();
            		}
            	}
            }
        }
    } else if (avc_packet_type == 1) {
        var sendRtmpHeader = {
            chunkStreamID: 4,
            timestamp: rtmpHeader.timestamp,
            messageTypeID: 0x09,
            messageStreamID: 1
        };
        var rtmpMessage = this.createRtmpMessage(sendRtmpHeader, rtmpBody);

        if (this.isPublisherStream) {
        	for (streamId in this.rh.rtmpConns) {
        		otherStream = this.rh.rtmpConns[streamId];
        		if (!otherStream.isPublisherStream) {
        			otherStream.sendBufferQueue.push(rtmpMessage);
        		}
        	}
        }
    }
};

RtmpConn.prototype.sendStreamEOF = function() {
    var rtmpBuffer = new Buffer("020000000000060400000000000100000001", 'hex');
    this.socket.write(rtmpBuffer);
};

NMRtmpConn.prototype.sendRtmpMsg = function(self) {
    if (!self.rh.room.getIsLiving()) return;
    var len = self.sendBufferQueue.length;
    for (var i = 0; i < len; i++) {
        self.socket.write(self.sendBufferQueue.shift());
    }
    if (len === 0) {
        setTimeout(self.sendRtmpMsg, 200, self);
    } else {
        setImmediate(self.sendRtmpMsg, self);
    }
};

module.exports = RtmpConn;