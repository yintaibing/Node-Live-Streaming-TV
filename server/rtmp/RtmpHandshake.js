/*
 * @File:   RtmpHandshake.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/16
 * @Remark: This file refers to: 
 *          https://github.com/illuspas/Node-Media-Server/blob/master/nm_rtmp_handshake.js
 */

 var Crypto = require('crypto');

 var RTMP_MSG_FORMAT_0 = 0;
 var RTMP_MSG_FROMAT_1 = 1;
 var RTMP_MSG_FROMAT_2 = 2;

 var RTMP_HANDSHAKE_MSG_SIZE = 1536;
 var SHA256DL = 32;

 var RandomCrud = new Buffer([
    0xf0, 0xee, 0xc2, 0x4a, 0x80, 0x68, 0xbe, 0xe8,
    0x2e, 0x00, 0xd0, 0xd1, 0x02, 0x9e, 0x7e, 0x57,
    0x6e, 0xec, 0x5d, 0x2d, 0x29, 0x80, 0x6f, 0xab,
    0x93, 0xb8, 0xe6, 0x36, 0xcf, 0xeb, 0x31, 0xae
]);

var FMSConst = "Genuine Adobe Flash Media Server 001";
var FMSConstCrud = Buffer.concat([new Buffer(FMSConst, 'utf8'), RandomCrud]);
var FPConst = "Genuine Adobe Flash Player 001";
var FPConstCrud = Buffer.concat([new Buffer(FPConst, 'utf8'), RandomCrud]);

function calculateHmac(data, key) {
	var hmac = Crypto.createHmac('sha256', key);
	hmac.update(data);
	return hmac.digest();
}

function getServerConstDigestOffset(buf) {
	return ((buf[0] + buf[1] + buf[2] + buf[3]) % 728) + 776;
}

function getClientConstDigestOffset(buf) {
	return ((buf[0] + buf[1] + buf[2] + buf[3]) % 728) + 12;
}

function hasSameBytes(buf1, buf2) {
	var length = buf1.length;
	for (var i = 0; i < length; i++) {
		if (buf1[i] !== buf2[i]) {
			return false;
		}
	}

	return true;
}

function detectClientMsgFormat(clientSig) {
	var computedSig, msg, providedSig, sdl;

	sdl = getServerConstDigestOffset(clientSig.slice(772, 776));
	msg = Buffer.concat([clientSig.slice(0, sdl), clientSig.slice(sdl + SHA256DL)], 1504);
	computedSig = calculateHmac(msg, FPConst);
	providedSig = clientSig.slice(sdl, sdl + SHA256DL);
	if (hasSameBytes(computedSig, providedSig)) {
		return RTMP_MSG_FROMAT_2;
	}

	sdl = getClientConstDigestOffset(clientSig.slice(8, 12));
	msg = Buffer.concat([clientSig.slice(0, sdl), clientSig.slice(sdl + SHA256DL)], 1504);
	computedSig = calculateHmac(msg, FPConst);
	providedSig = clientSig.slice(sdl, sdl + SHA256DL);
	if (hasSameBytes(computedSig, providedSig)) {
		return RTMP_MSG_FROMAT_1;
	}

	return RTMP_MSG_FORMAT_0;
}

function generateS1(msgFormat) {
	var randomBytes = Crypto.randomBytes(RTMP_HANDSHAKE_MSG_SIZE - 8);
	var handshakeBytes = Buffer.concat([new Buffer([0, 0, 0, 0, 1, 2, 3, 4]), randomBytes], 
		RTMP_HANDSHAKE_MSG_SIZE);

	var serverDigestOffset;
	if (msgFormat === 1) {
		serverDigestOffset = getClientConstDigestOffset(handshakeBytes.slice(8, 12));
	} else {
		serverDigestOffset = getServerConstDigestOffset(handshakeBytes.slice(772, 776));
	}

	var msg = Buffer.concat([handshakeBytes.slice(0, serverDigestOffset), 
		handshakeBytes.slice(serverDigestOffset + SHA256DL)], 
		RTMP_HANDSHAKE_MSG_SIZE - SHA256DL);
	var hash = calculateHmac(msg, FMSConst);
	hash.copy(handshakeBytes, serverDigestOffset, 0, 32);
	return handshakeBytes;
}

function generateS2(msgFormat, clientSig, callback) {
	var randomBytes = Crypto.randomBytes(RTMP_HANDSHAKE_MSG_SIZE - 32);
	
	var challengeKeyOffset;
	if (msgFormat === 1) {
		challengeKeyOffset = getClientConstDigestOffset(clientSig.slice(8, 12));
	} else {
		challengeKeyOffset = getServerConstDigestOffset(clientSig.slice(772, 776));
	}

	var challengeKey = clientSig.slice(challengeKeyOffset, challengeKeyOffset + 32);
	var hash = calculateHmac(challengeKey, FMSConstCrud);
	var sig = calculateHmac(randomBytes, hash);

	var s2Bytes = Buffer.concat([randomBytes, sig], RTMP_HANDSHAKE_MSG_SIZE);
	return s2Bytes;
}

function generateS0S1S2(clientSig, callback) {
	var clientType = clientSig[0];
	clientSig = clientSig.slice(1);

	var msgFormat = detectClientMsgFormat(clientSig);
	var allBytes;
	if (msgFormat === RTMP_MSG_FORMAT_0) {
		allBytes = Buffer.concat([new Buffer([clientType]), clientSig, clientSig]);
	} else {
		allBytes = Buffer.concat([new Buffer([clientType]), generateS1(msgFormat), generateS2(msgFormat, clientSig)]);
	}
	return allBytes;
}

module.exports = {
	generateS0S1S2: generateS0S1S2
};