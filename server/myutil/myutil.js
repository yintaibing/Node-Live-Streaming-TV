/*
 * @File:   myutil.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/19
 */

var crypto = require('crypto');
var qs = require('querystring');

var KEY = 'TurtleTV';
exports.desEn = function(str) {
	var cipher = crypto.createCipher('des', KEY);
	var en = cipher.update(str, 'utf8', 'hex');
	en += cipher.final('hex');
	return en;
};

exports.desDe = function(str) {
	var decipher = crypto.createDecipher('des', KEY);
	var de = decipher.update(str, 'hex', 'utf8');
	de += decipher.final('utf8');
	return de;
};

exports.parseQueryString = function(str) {
	return qs.parse(str);
};

exports.removeFromAry = function(ary, target) {
	if (!ary) {
		return [];
	}

	var length = ary.length;
	for (var i = 0; i < length; i++) {
		if (ary[i] === target) {
			return ary.slice(0, i).concat(ary.slice(i + 1));
		}
	}
	return ary;
};

exports.isBlank = function(s) {
	return (s === '' || s === undefined || s === null);
};